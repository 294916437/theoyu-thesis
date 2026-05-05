/**
 * LatencyCollector — SFU 端到端延迟采集服务（仅用于论文测试，不影响业务逻辑）
 *
 * 测量原理
 * ──────────────────────────────────────────────────────────────────────────
 * 在 SFU 架构中，媒体包的传播路径为：
 *
 *   发送方浏览器 ──RTP──▶ SFU (Producer) ──RTP──▶ SFU (Consumer) ──▶ 接收方浏览器
 *
 * 延迟由两段构成：
 *   ① sender_one_way = producerRTT / 2   （发送端↔SFU RTCP RTT 的一半）
 *   ② receiver_one_way = consumerRTT / 2 （SFU↔接收端 RTCP RTT 的一半）
 *
 * 因此估算 E2E 单向延迟：
 *   E2E ≈ producerRTT/2 + consumerRTT/2
 *
 * RTT 数据来源：
 *   - producerRTT：mediasoup Worker 基于 RTCP XR/DLRR（RFC 3611）测量
 *     由 Producer.getStats() 中 roundTripTime 字段暴露（单位：秒）
 *     注意：Simulcast 有多个 SSRC，取「活跃 SSRC 中的最小值」排除 stale 层
 *   - consumerRTT：mediasoup 向接收端发送 RTCP SR，接收端回复 RTCP RR（标准机制）
 *     由 Consumer.getStats() 中 roundTripTime 字段暴露（单位：秒）
 *     比 producerRTT 更可靠（不依赖 RTCP XR 扩展）
 *
 * 性能影响
 * ──────────────────────────────────────────────────────────────────────────
 * - getStats() 调用仅读取 Worker 内部的 RTC stats 缓存，不产生额外网络交互
 * - 采集间隔默认 5 秒，定时器 unref() 不阻塞进程退出
 * - 所有数据存储于内存滑动窗口，不写入数据库
 * - 本模块仅在测试模式或显式开启时启动
 */

import * as fs from "fs"
import * as path from "path"
import { RoomManager } from "../core/room-manager"
import { Logger } from "../utils/logger"

// ─── 数据结构 ──────────────────────────────────────────────────────────────

/** 单次 E2E 延迟采样 */
interface E2eSample {
	/** 采样时间戳 (ms) */
	timestamp: number
	roomId: string
	/** Producer 所属 peerId（发送方） */
	senderPeerId: string
	/** Consumer 所属 peerId（接收方） */
	receiverPeerId: string
	producerKind: "audio" | "video"
	/** RTCP RTT：发送端↔SFU，单位 ms */
	senderRttMs: number
	/** RTCP RTT：SFU↔接收端，单位 ms */
	receiverRttMs: number
	/** E2E 单向延迟估算，单位 ms */
	e2eMs: number
}

/** 统计汇总 */
export interface LatencyReport {
	collectedAt: string
	collectionDurationSec: number
	totalSamples: number
	/** 全局 E2E 统计（所有房间所有 peer 对） */
	global: LatencyStats
	/** 按房间划分的统计 */
	byRoom: Record<string, RoomLatencyStats>
	/** 最近 N 条原始样本（供调试） */
	recentSamples: E2eSample[]
}

interface LatencyStats {
	sampleCount: number
	/** E2E 延迟均值 (ms) */
	e2eMeanMs: number
	/** E2E 延迟最大值 (ms) */
	e2eMaxMs: number
	/** E2E 延迟最小值 (ms) */
	e2eMinMs: number
	/** E2E 延迟 P50 (ms) */
	e2eP50Ms: number
	/** E2E 延迟 P95 (ms) */
	e2eP95Ms: number
	/** RTCP RTT 均值：发送端↔SFU (ms) */
	senderRttMeanMs: number
	/** RTCP RTT 均值：SFU↔接收端 (ms) */
	receiverRttMeanMs: number
	/** RTCP RTT 最大值：发送端↔SFU (ms) */
	senderRttMaxMs: number
	/** RTCP RTT 最大值：SFU↔接收端 (ms) */
	receiverRttMaxMs: number
}

interface RoomLatencyStats extends LatencyStats {
	roomId: string
	activePeerPairs: number
}

// ─── LatencyCollector ──────────────────────────────────────────────────────

export class LatencyCollector {
	private static instance: LatencyCollector

	private readonly logger = new Logger("LatencyCollector")
	private readonly roomManager = RoomManager.getInstance()

	/** 滑动窗口：最多保留最近 WINDOW_SIZE 条样本 */
	private readonly WINDOW_SIZE = 2000
	/** 采集间隔（ms） */
	private readonly COLLECT_INTERVAL_MS = 5000
	/** 将报告写入文件的间隔（ms），0 = 不写文件 */
	private readonly DUMP_INTERVAL_MS = 30000

	private samples: E2eSample[] = []
	private collectTimer: NodeJS.Timeout | null = null
	private dumpTimer: NodeJS.Timeout | null = null
	private startTime = Date.now()
	private outputPath: string

	private constructor() {
		this.outputPath = path.resolve(process.cwd(), "latency-report.json")
	}

	public static getInstance(): LatencyCollector {
		if (!LatencyCollector.instance) {
			LatencyCollector.instance = new LatencyCollector()
		}
		return LatencyCollector.instance
	}

	// ─── 生命周期 ──────────────────────────────────────────────────────────

	/** 启动采集 */
	public start(outputPath?: string): void {
		if (this.collectTimer) return

		if (outputPath) this.outputPath = outputPath
		this.startTime = Date.now()
		this.samples = []

		this.logger.info(`[LatencyCollector] Started. interval=${this.COLLECT_INTERVAL_MS}ms, output=${this.outputPath}`)

		const collectTick = () => {
			this.collectOnce().catch((err) => this.logger.error("[LatencyCollector] collect error", err))
			this.collectTimer = setTimeout(collectTick, this.COLLECT_INTERVAL_MS)
			this.collectTimer.unref()
		}
		this.collectTimer = setTimeout(collectTick, this.COLLECT_INTERVAL_MS)
		this.collectTimer.unref()

		if (this.DUMP_INTERVAL_MS > 0) {
			const dumpTick = () => {
				this.dumpToFile().catch((err) => this.logger.error("[LatencyCollector] dump error", err))
				this.dumpTimer = setTimeout(dumpTick, this.DUMP_INTERVAL_MS)
				this.dumpTimer?.unref()
			}
			this.dumpTimer = setTimeout(dumpTick, this.DUMP_INTERVAL_MS)
			this.dumpTimer?.unref()
		}
	}

	/** 停止采集并写最终报告 */
	public async stop(): Promise<LatencyReport> {
		if (this.collectTimer) {
			clearTimeout(this.collectTimer)
			this.collectTimer = null
		}
		if (this.dumpTimer) {
			clearTimeout(this.dumpTimer)
			this.dumpTimer = null
		}

		const report = this.buildReport()
		await this.dumpToFile(report)
		this.logger.info(`[LatencyCollector] Stopped. totalSamples=${report.totalSamples}`)
		return report
	}

	// ─── 核心采集逻辑 ──────────────────────────────────────────────────────

	/**
	 * 遍历所有房间的 Producer/Consumer，读取 RTCP RTT stats，计算 E2E 延迟。
	 *
	 * 关键流程：
	 * 1. 对每个 Room 的每个 Peer，遍历其 producers
	 * 2. 调用 producer.getStats() → 提取 roundTripTime（秒）
	 * 3. 遍历同一 Room 内其他所有 Peer 的 consumers，
	 *    找到 consumer.producerId === producer.id 的 consumer
	 * 4. 调用 consumer.getStats() → 提取 roundTripTime（秒）
	 * 5. E2E(ms) = (producerRTT + consumerRTT) / 2 * 1000
	 */
	private async collectOnce(): Promise<void> {
		const rooms = this.roomManager.getAllRooms()
		const now = Date.now()
		const newSamples: E2eSample[] = []

		for (const room of rooms) {
			const peers = room.getAllPeers()
			if (peers.length < 2) continue // 至少需要 2 个 peer 才有 E2E 意义

			// 收集所有 producer RTT：Map<producerId, { rttMs, senderPeerId, kind }>
			const producerRttMap = new Map<string, { rttMs: number; senderPeerId: string; kind: "audio" | "video" }>()

			for (const peer of peers) {
				for (const producer of peer.producers.values()) {
					if (producer.closed) continue
					try {
						const stats = await producer.getStats()
						const rttSec = this.extractRoundTripTime(stats)
						if (rttSec !== null && rttSec > 0) {
							producerRttMap.set(producer.id, {
								rttMs: rttSec * 1000,
								senderPeerId: peer.id,
								kind: producer.kind as "audio" | "video",
							})
						}
					} catch {
						// producer 可能已关闭，忽略
					}
				}
			}

			if (producerRttMap.size === 0) continue

			// 遍历所有 consumer，匹配对应的 producer RTT
			for (const peer of peers) {
				for (const consumer of peer.consumers.values()) {
					if (consumer.closed) continue

					const producerInfo = producerRttMap.get(consumer.producerId)
					if (!producerInfo) continue
					// 发送方与接收方不能是同一 peer
					if (producerInfo.senderPeerId === peer.id) continue

					try {
						const stats = await consumer.getStats()
						const rttSec = this.extractRoundTripTime(stats)
						if (rttSec !== null && rttSec > 0) {
							const receiverRttMs = rttSec * 1000
							const senderRttMs = producerInfo.rttMs
							const e2eMs = (senderRttMs + receiverRttMs) / 2

							newSamples.push({
								timestamp: now,
								roomId: room.id,
								senderPeerId: producerInfo.senderPeerId,
								receiverPeerId: peer.id,
								producerKind: producerInfo.kind,
								senderRttMs,
								receiverRttMs,
								e2eMs,
							})
						}
					} catch {
						// consumer 可能已关闭，忽略
					}
				}
			}
		}

		if (newSamples.length > 0) {
			this.samples.push(...newSamples)
			// 维持滑动窗口大小
			if (this.samples.length > this.WINDOW_SIZE) {
				this.samples = this.samples.slice(this.samples.length - this.WINDOW_SIZE)
			}
			// 输出新增数据
			this.logger.info(`[LatencyCollector] +${newSamples.length} samples`)
		}
	}

	/**
	 * 从 mediasoup getStats() 返回的数组中提取 roundTripTime（秒）。
	 *
	 * ── 数据来源 ──────────────────────────────────────────────────────────────
	 * mediasoup getStats() 返回每个 SSRC 的独立统计记录（ProducerStat / ConsumerStat）。
	 * 单层（LATENCY_TEST_MODE）模式下视频有 1 个主 SSRC + 1 个低频 RTX SSRC；
	 * 音频为 1 条。
	 *
	 * ── 选"流量最大"的 SSRC ───────────────────────────────────────────────────
	 * 策略：取 packetsReceived + packetsSent 之和最大的 SSRC 的 RTT。
	 *
	 * 理由：
	 * 1. 主流 SSRC（高包量）拥有持续稳定的 RTCP 反馈，其 roundTripTime 始终
	 *    反映最新网络状态，是最可信、也是最接近真实值的 RTT 来源。
	 * 2. RTX SSRC 仅在丢包重传时发包，包量极低，RTCP 测量周期长，
	 *    其 RTT 可能停留在旧时刻的高抖动值，直接选主流可完全规避此干扰。
	 * 3. 相较于先前的 MIN 策略（在多 SSRC 中取随机最小值），
	 *    选主流语义更明确，在连续采样间不会因为"碰巧选到不同 SSRC"而产生跳变。
	 *
	 * 回退规则：若所有 SSRC 的包量均为 0（连接建立初期），
	 * 取所有有效 RTT 的最小值作为兜底估算。
	 *
	 * 硬性过滤：RTT > 10s 视为 RTCP 未初始化或溢出，直接丢弃。
	 */
	private extractRoundTripTime(stats: any[]): number | null {
		const MAX_VALID_RTT_SEC = 10 // 超过 10s 视为无效值（RTCP 未初始化或溢出）

		let primaryRtt: number | null = null // 流量最大 SSRC 的 RTT（主流）
		let primaryPackets = 0
		const fallbackRtts: number[] = [] // 包量为 0 时的兜底候选

		for (const stat of stats) {
			const rtt = stat.roundTripTime
			if (typeof rtt !== "number" || rtt <= 0 || rtt > MAX_VALID_RTT_SEC) continue

			fallbackRtts.push(rtt)

			// 累计包量：Producer stats → packetsReceived；Consumer stats → packetsSent
			const packetCount = (stat.packetsReceived ?? 0) + (stat.packetsSent ?? 0)

			if (packetCount > primaryPackets) {
				primaryPackets = packetCount
				primaryRtt = rtt
			}
		}

		// 优先使用主流 SSRC（包量最大，RTCP 反馈最新鲜）
		// 兜底：取所有有效 RTT 中的最小值（连接初期无包量统计时）
		if (primaryPackets > 0) return primaryRtt
		return fallbackRtts.length > 0 ? Math.min(...fallbackRtts) : null
	}

	// ─── 报告构建 ──────────────────────────────────────────────────────────

	/** 实时获取当前采集报告（供 HTTP 接口调用） */
	public getReport(): LatencyReport {
		return this.buildReport()
	}

	private buildReport(): LatencyReport {
		const now = new Date().toISOString()
		const durationSec = Math.round((Date.now() - this.startTime) / 1000)

		const globalStats = this.computeStats(this.samples)

		// 按房间分组
		const byRoomMap = new Map<string, E2eSample[]>()
		for (const s of this.samples) {
			if (!byRoomMap.has(s.roomId)) byRoomMap.set(s.roomId, [])
			byRoomMap.get(s.roomId)!.push(s)
		}

		const byRoom: Record<string, RoomLatencyStats> = {}
		for (const [roomId, roomSamples] of byRoomMap.entries()) {
			const activePairs = new Set(roomSamples.map((s) => `${s.senderPeerId}→${s.receiverPeerId}`)).size
			byRoom[roomId] = {
				roomId,
				activePeerPairs: activePairs,
				...this.computeStats(roomSamples),
			}
		}

		// 最近 20 条原始样本供调试
		const recentSamples = this.samples.slice(-20)

		return {
			collectedAt: now,
			collectionDurationSec: durationSec,
			totalSamples: this.samples.length,
			global: globalStats,
			byRoom,
			recentSamples,
		}
	}

	private computeStats(samples: E2eSample[]): LatencyStats {
		if (samples.length === 0) {
			return {
				sampleCount: 0,
				e2eMeanMs: 0,
				e2eMaxMs: 0,
				e2eMinMs: 0,
				e2eP50Ms: 0,
				e2eP95Ms: 0,
				senderRttMeanMs: 0,
				receiverRttMeanMs: 0,
				senderRttMaxMs: 0,
				receiverRttMaxMs: 0,
			}
		}

		const e2eValues = samples.map((s) => s.e2eMs).sort((a, b) => a - b)
		const senderRtts = samples.map((s) => s.senderRttMs)
		const receiverRtts = samples.map((s) => s.receiverRttMs)

		const mean = (arr: number[]) => arr.reduce((a, b) => a + b, 0) / arr.length
		const max = (arr: number[]) => Math.max(...arr)
		const min = (arr: number[]) => Math.min(...arr)
		const percentile = (sorted: number[], p: number) => {
			const idx = Math.ceil((p / 100) * sorted.length) - 1
			return sorted[Math.max(0, idx)]
		}
		const round2 = (v: number) => Math.round(v * 100) / 100

		return {
			sampleCount: samples.length,
			e2eMeanMs: round2(mean(e2eValues)),
			e2eMaxMs: round2(max(e2eValues)),
			e2eMinMs: round2(min(e2eValues)),
			e2eP50Ms: round2(percentile(e2eValues, 50)),
			e2eP95Ms: round2(percentile(e2eValues, 95)),
			senderRttMeanMs: round2(mean(senderRtts)),
			receiverRttMeanMs: round2(mean(receiverRtts)),
			senderRttMaxMs: round2(max(senderRtts)),
			receiverRttMaxMs: round2(max(receiverRtts)),
		}
	}

	// ─── 文件持久化 ────────────────────────────────────────────────────────

	private async dumpToFile(report?: LatencyReport): Promise<void> {
		const r = report ?? this.buildReport()
		try {
			await fs.promises.writeFile(this.outputPath, JSON.stringify(r, null, 2), "utf-8")
			this.logger.info(`[LatencyCollector] Report dumped to ${this.outputPath}`)
		} catch (err) {
			this.logger.error("[LatencyCollector] Failed to dump report", err)
		}
	}
}
