/**
 * SFU 并发压力测试脚本
 *
 * 测试方案：
 *   1. 启动 SFU 服务（需设置 SFU_TEST_MODE=true 跳过 gRPC 验证）
 *   2. 并行启动 2/4/8/12/16 个 Broadcaster 实例加入同一房间
 *   3. 每个实例执行完整信令流程：
 *      joinRoom → getRouterRtpCapabilities → createWebRtcTransport(send+recv)
 *      → connectWebRtcTransport(send+recv) → produce(video+audio) → consume(订阅其他所有人)
 *   4. 稳定期结束后通过 GET /metrics 采集指标
 *   5. 逐步增加并发直至出现明显性能瓶颈
 *
 * 启动方式：
 *   # 启动 SFU 服务（必须设置测试模式环境变量）
 *   SFU_TEST_MODE=true npm run dev --workspace=sfu
 *
 *   # 运行测试（另开终端）
 *   cd test && npm install && node sfu/concurrent-stress.js
 *
 *   # 快速测试
 *   STABLE_MS=10000 LEVELS=2,4 node sfu/concurrent-stress.js
 *
 * 环境变量：
 *   SFU_URL      SFU 服务地址（默认 http://localhost:3000）
 *   LEVELS       并发级别，逗号分隔（默认 2,4,8,12,16）
 *   STABLE_MS    每级稳定推流时长 ms（默认 180000，即 3 分钟）
 *   TOKEN        测试 token（默认 test-token）
 *   OUTPUT_FILE  结果输出文件（默认 sfu-stress-result.json）
 */

"use strict"

const { io } = require("socket.io-client")
const http = require("http")
const https = require("https")
const fs = require("fs")
const path = require("path")
const { EventEmitter } = require("events")

// ─────────────────────────── 配置 ───────────────────────────────────────────

const CONFIG = {
	sfuUrl: process.env.SFU_URL || "http://localhost:3000",
	levels: (process.env.LEVELS || "2,4,8,12,16").split(",").map(Number),
	stableMs: parseInt(process.env.STABLE_MS || "180000", 10),
	token: process.env.TOKEN || "test-token",
	outputFile: process.env.OUTPUT_FILE || path.join(__dirname, "sfu-stress-result.json"),
	connectTimeoutMs: 10000,
	signalingTimeoutMs: 15000,
	cooldownMs: 12000, // 每轮之间的冷却时间
}

// ─────────────────────────── 辅助函数 ───────────────────────────────────────

/** 生成一个合法格式的假 DTLS Fingerprint（测试用，不做真实 DTLS 握手） */
function fakeDtlsParameters() {
	const hex = () =>
		Array.from({ length: 32 }, () =>
			Math.floor(Math.random() * 256)
				.toString(16)
				.padStart(2, "0")
				.toUpperCase(),
		).join(":")
	return {
		role: "client",
		fingerprints: [{ algorithm: "sha-256", value: hex() }],
	}
}

/**
 * 从路由器 RTP 能力中提取 VP8 编解码器，构造 produce 用的 rtpParameters
 * 发送端侧的 payloadType 与路由器无需完全一致，mediasoup 会做映射
 */
function buildVideoRtpParameters(routerRtpCapabilities, ssrcBase) {
	const vp8 = routerRtpCapabilities.codecs.find((c) => c.mimeType.toLowerCase() === "video/vp8")
	if (!vp8) throw new Error("路由器不支持 VP8 编解码器")
	return {
		mid: "video",
		codecs: [
			{
				mimeType: vp8.mimeType,
				payloadType: 96,
				clockRate: vp8.clockRate,
				parameters: vp8.parameters || {},
				rtcpFeedback: vp8.rtcpFeedback || [],
			},
		],
		headerExtensions: [],
		encodings: [{ ssrc: ssrcBase, rtx: { ssrc: ssrcBase + 1 } }],
		rtcp: { cname: `test-cname-${ssrcBase}`, reducedSize: true },
	}
}

/** 从路由器 RTP 能力中提取 Opus 编解码器，构造 produce 用的 rtpParameters */
function buildAudioRtpParameters(routerRtpCapabilities, ssrcBase) {
	const opus = routerRtpCapabilities.codecs.find((c) => c.mimeType.toLowerCase() === "audio/opus")
	if (!opus) throw new Error("路由器不支持 Opus 编解码器")
	return {
		mid: "audio",
		codecs: [
			{
				mimeType: opus.mimeType,
				payloadType: 100,
				clockRate: opus.clockRate,
				channels: opus.channels || 2,
				parameters: opus.parameters || {},
				rtcpFeedback: opus.rtcpFeedback || [],
			},
		],
		headerExtensions: [],
		encodings: [{ ssrc: ssrcBase + 2 }],
		rtcp: { cname: `test-cname-${ssrcBase}`, reducedSize: true },
	}
}

/** 发起一次带超时的 HTTP GET 请求，返回解析后的 JSON 或 null */
function httpGet(url, timeoutMs = 5000) {
	return new Promise((resolve) => {
		const lib = url.startsWith("https") ? https : http
		const req = lib
			.get(url, { timeout: timeoutMs }, (res) => {
				let raw = ""
				res.on("data", (chunk) => (raw += chunk))
				res.on("end", () => {
					try {
						resolve(JSON.parse(raw))
					} catch {
						resolve(null)
					}
				})
			})
			.on("error", () => resolve(null))
			.on("timeout", () => {
				req.destroy()
				resolve(null)
			})
	})
}

/** 格式化毫秒为可读字符串 */
function fmtMs(ms) {
	if (ms == null) return "N/A"
	return `${ms}ms`
}

/** 打印分隔线 */
const sep = (char = "─", len = 70) => console.log(char.repeat(len))

// ─────────────────────────── TestBroadcaster ────────────────────────────────

/**
 * 模拟单个会议参与者，执行完整的 Mediasoup 信令流程
 */
class TestBroadcaster extends EventEmitter {
	constructor(index, roomId) {
		super()
		this.index = index
		this.roomId = roomId
		// 每个实例使用唯一的 userId（进程 PID + 索引 + 时间戳）
		this.userId = `stress-${process.pid}-${index}-${Date.now()}`
		this.username = `StressUser_${index}`
		this.socket = null
		this.routerRtpCapabilities = null
		this.sendTransportId = null
		this.recvTransportId = null
		this.producerIds = []
		this.consumerIds = []
		// 每个实例独占一段 SSRC 空间，避免冲突
		this.ssrcBase = (index + 1) * 100000 + Math.floor(Math.random() * 1000)
		// 各步骤延迟记录（ms）
		this.latency = {}
		// 非致命错误记录
		this.warnings = []
		this.connected = false
	}

	// ─── 建立 Socket.io 连接 ───────────────────────────────────────────────

	connect() {
		return new Promise((resolve, reject) => {
			const timer = setTimeout(() => reject(new Error("连接超时")), CONFIG.connectTimeoutMs)
			this.socket = io(CONFIG.sfuUrl, {
				auth: { token: CONFIG.token },
				transports: ["websocket"],
				reconnection: false,
				timeout: CONFIG.connectTimeoutMs,
			})
			this.socket.once("connect", () => {
				clearTimeout(timer)
				this.connected = true
				resolve()
			})
			this.socket.once("connect_error", (err) => {
				clearTimeout(timer)
				reject(err)
			})
		})
	}

	// ─── 带延迟测量的事件发送 ─────────────────────────────────────────────

	/**
	 * 发送一个 Socket.io 事件并等待 callback 响应，测量往返延迟
	 * @param {string} event 事件名
	 * @param {object} data 请求体
	 * @param {string} [latencyKey] 延迟统计用的 key（默认等于 event）
	 */
	emit_measured(event, data, latencyKey) {
		const key = latencyKey || event
		return new Promise((resolve, reject) => {
			const timer = setTimeout(() => reject(new Error(`${event} 响应超时`)), CONFIG.signalingTimeoutMs)
			const start = Date.now()
			this.socket.emit(event, data, (response) => {
				clearTimeout(timer)
				const duration = Date.now() - start
				// 累加延迟（同一 key 可能多次调用，取均值在外部处理）
				if (!this.latency[key]) {
					this.latency[key] = []
				}
				this.latency[key].push(duration)
				if (response && response.error) {
					reject(new Error(`[${event}] ${response.error} (code: ${response.code})`))
				} else {
					resolve(response)
				}
			})
		})
	}

	// ─── 完整信令流程 ─────────────────────────────────────────────────────

	/**
	 * 执行完整信令流程
	 * @param {string[]} existingProducerIds 已在房间内的其他 Producer ID 列表（用于消费）
	 */
	async runSignaling(existingProducerIds) {
		// 1. joinRoom
		const joinResult = await this.emit_measured("joinRoom", {
			roomId: this.roomId,
			userId: this.userId,
			username: this.username,
			token: CONFIG.token,
		})
		// 记录加入时已有的 peers（用于调试）
		this._peersOnJoin = joinResult.peers?.length ?? 0

		// 2. getRouterRtpCapabilities
		const capsResult = await this.emit_measured("getRouterRtpCapabilities", { roomId: this.roomId })
		this.routerRtpCapabilities = capsResult.rtpCapabilities

		// 3. createWebRtcTransport（发送端）
		const sendResult = await this.emit_measured("createWebRtcTransport", { roomId: this.roomId, producing: true, consuming: false }, "createWebRtcTransport_send")
		this.sendTransportId = sendResult.id

		// 4. createWebRtcTransport（接收端）
		const recvResult = await this.emit_measured("createWebRtcTransport", { roomId: this.roomId, producing: false, consuming: true }, "createWebRtcTransport_recv")
		this.recvTransportId = recvResult.id

		// 5. connectWebRtcTransport（发送端）
		await this.emit_measured(
			"connectWebRtcTransport",
			{ roomId: this.roomId, transportId: this.sendTransportId, dtlsParameters: fakeDtlsParameters() },
			"connectWebRtcTransport_send",
		)

		// 6. connectWebRtcTransport（接收端）
		await this.emit_measured(
			"connectWebRtcTransport",
			{ roomId: this.roomId, transportId: this.recvTransportId, dtlsParameters: fakeDtlsParameters() },
			"connectWebRtcTransport_recv",
		)

		// 7. produce 视频
		try {
			const videoRtp = buildVideoRtpParameters(this.routerRtpCapabilities, this.ssrcBase)
			const videoResult = await this.emit_measured(
				"produce",
				{
					roomId: this.roomId,
					transportId: this.sendTransportId,
					kind: "video",
					rtpParameters: videoRtp,
					appData: { label: "stress-video" },
				},
				"produce_video",
			)
			this.producerIds.push(videoResult.id)
		} catch (err) {
			this.warnings.push({ step: "produce_video", message: err.message })
		}

		// 8. produce 音频
		try {
			const audioRtp = buildAudioRtpParameters(this.routerRtpCapabilities, this.ssrcBase)
			const audioResult = await this.emit_measured(
				"produce",
				{
					roomId: this.roomId,
					transportId: this.sendTransportId,
					kind: "audio",
					rtpParameters: audioRtp,
					appData: { label: "stress-audio" },
				},
				"produce_audio",
			)
			this.producerIds.push(audioResult.id)
		} catch (err) {
			this.warnings.push({ step: "produce_audio", message: err.message })
		}

		// 9. consume 订阅其他参与者的 Producer
		for (const producerId of existingProducerIds) {
			try {
				const consumeResult = await this.emit_measured(
					"consume",
					{
						roomId: this.roomId,
						producerId,
						rtpCapabilities: this.routerRtpCapabilities,
					},
					"consume",
				)
				this.consumerIds.push(consumeResult.id)

				// 10. resumeConsumer（mediasoup 要求消费者先处于 paused 再 resume）
				await this.emit_measured(
					"resumeConsumer",
					{
						roomId: this.roomId,
						consumerId: consumeResult.id,
					},
					"resumeConsumer",
				)
			} catch (err) {
				this.warnings.push({ step: "consume", producerId, message: err.message })
			}
		}
	}

	// ─── 断开连接 ─────────────────────────────────────────────────────────

	disconnect() {
		if (this.socket && this.connected) {
			try {
				this.socket.disconnect()
			} catch {
				// ignore
			}
			this.connected = false
		}
	}

	// ─── 汇总该实例的延迟统计 ─────────────────────────────────────────────

	getLatencySummary() {
		const summary = {}
		for (const [key, values] of Object.entries(this.latency)) {
			if (!Array.isArray(values) || values.length === 0) continue
			summary[key] = {
				avg: Math.round(values.reduce((a, b) => a + b, 0) / values.length),
				min: Math.min(...values),
				max: Math.max(...values),
				count: values.length,
			}
		}
		return summary
	}
}

// ─────────────────────────── 并发级别测试 ───────────────────────────────────

/**
 * 运行单个并发级别的压力测试
 * @param {number} concurrency 并发 Broadcaster 数量
 * @returns {object} 测试结果
 */
async function runLevel(concurrency) {
	const roomId = `stress-room-${concurrency}-${Date.now()}`
	sep("═")
	console.log(`  并发级别: ${concurrency} 个 Broadcaster  |  房间: ${roomId}`)
	sep("═")

	const broadcasters = Array.from({ length: concurrency }, (_, i) => new TestBroadcaster(i, roomId))
	const result = {
		concurrency,
		roomId,
		startTime: new Date().toISOString(),
		phases: {},
		signaling: { success: 0, failed: 0, failedIndexes: [], warnings: [] },
		latencyByEvent: {},
		metricsAtStart: null,
		metricsAfterStable: null,
		roomStats: null,
	}

	// ── Phase 1: 连接 ────────────────────────────────────────────────────

	console.log(`\n[Phase 1] 并行连接 ${concurrency} 个客户端...`)
	const p1Start = Date.now()
	const connectResults = await Promise.allSettled(broadcasters.map((b) => b.connect()))
	result.phases.connect = Date.now() - p1Start

	const connected = connectResults.filter((r) => r.status === "fulfilled").length
	console.log(`  连接: ${connected}/${concurrency} 成功，耗时 ${result.phases.connect}ms`)

	if (connected === 0) {
		console.error("  全部连接失败，终止本轮测试")
		result.signaling.failed = concurrency
		return result
	}

	// ── Phase 2: 信令交互 ──────────────────────────────────────────────────

	console.log(`\n[Phase 2] 执行完整信令流程（joinRoom → produce → consume）...`)
	const p2Start = Date.now()

	// sharedProducerIds 在所有 Broadcaster 之间共享，先加入的 Producer 供后加入的 Broadcaster 消费
	const sharedProducerIds = []

	// 采用小幅错峰启动（每隔 50ms），避免服务器瞬时洪峰
	const signalingPromises = broadcasters.map(async (b, i) => {
		if (!b.connected) return
		await new Promise((r) => setTimeout(r, i * 50))
		// 在该实例加入时，捕获当前已知的 Producer 列表
		const producersSnapshot = [...sharedProducerIds]
		await b.runSignaling(producersSnapshot)
		// 将自身的 Producer 追加到共享列表
		sharedProducerIds.push(...b.producerIds)
	})

	const signalingResults = await Promise.allSettled(signalingPromises)
	result.phases.signaling = Date.now() - p2Start

	for (let i = 0; i < signalingResults.length; i++) {
		const r = signalingResults[i]
		if (r.status === "fulfilled") {
			result.signaling.success++
		} else {
			result.signaling.failed++
			result.signaling.failedIndexes.push(i)
			console.warn(`  Broadcaster[${i}] 信令失败: ${r.reason?.message}`)
		}
		// 收集非致命 warnings
		if (broadcasters[i].warnings.length > 0) {
			result.signaling.warnings.push(...broadcasters[i].warnings.map((w) => ({ index: i, ...w })))
		}
	}

	console.log(`  信令完成: ${result.signaling.success}/${concurrency} 成功，` + `共 ${sharedProducerIds.length} 个 Producer，耗时 ${result.phases.signaling}ms`)
	if (result.signaling.warnings.length > 0) {
		console.warn(`  非致命警告 ${result.signaling.warnings.length} 条（见结果文件）`)
	}

	// ── Phase 3: 初始指标采集 ──────────────────────────────────────────────

	result.metricsAtStart = await httpGet(`${CONFIG.sfuUrl}/metrics`)

	// ── Phase 4: 稳定期 ───────────────────────────────────────────────────

	const stableSec = CONFIG.stableMs / 1000
	console.log(`\n[Phase 3] 稳定推流 ${stableSec}s，每 30s 打印一次心跳...`)
	const p4Start = Date.now()

	// 定期打印心跳（每 30s 或稳定期 1/6 取较小值）
	const heartbeatInterval = Math.min(30000, Math.floor(CONFIG.stableMs / 6))
	let heartbeatCount = 0
	const heartbeatTimer = setInterval(async () => {
		heartbeatCount++
		const elapsed = Math.round((Date.now() - p4Start) / 1000)
		const m = await httpGet(`${CONFIG.sfuUrl}/metrics`)
		const conns = m?.connections?.current ?? "?"
		const producers = (m?.producers?.audio ?? "?") + (m?.producers?.video ?? 0)
		const consumers = m?.consumers ?? "?"
		console.log(`  [${elapsed}s] 活跃连接=${conns}  Producer总数=${producers}  Consumer总数=${consumers}`)
	}, heartbeatInterval)

	await new Promise((r) => setTimeout(r, CONFIG.stableMs))
	clearInterval(heartbeatTimer)
	result.phases.stable = Date.now() - p4Start

	// ── Phase 5: 最终指标采集 ─────────────────────────────────────────────

	console.log(`\n[Phase 4] 采集最终指标...`)
	result.metricsAfterStable = await httpGet(`${CONFIG.sfuUrl}/metrics`)
	result.roomStats = await httpGet(`${CONFIG.sfuUrl}/api/stats`)

	// ── Phase 6: 断开连接 ──────────────────────────────────────────────────

	console.log(`[Phase 5] 断开全部连接...`)
	broadcasters.forEach((b) => b.disconnect())
	await new Promise((r) => setTimeout(r, 1000))

	// ── 汇总延迟数据 ───────────────────────────────────────────────────────

	// 统计所有成功 Broadcaster 的延迟数据，按事件 key 聚合
	const aggregated = {}
	for (const b of broadcasters) {
		if (!b.connected && !b.warnings) continue
		const summary = b.getLatencySummary()
		for (const [key, stat] of Object.entries(summary)) {
			if (!aggregated[key]) aggregated[key] = []
			aggregated[key].push(stat.avg)
		}
	}
	for (const [key, avgs] of Object.entries(aggregated)) {
		result.latencyByEvent[key] = {
			avg: Math.round(avgs.reduce((a, b) => a + b, 0) / avgs.length),
			min: Math.min(...avgs),
			max: Math.max(...avgs),
		}
	}

	result.endTime = new Date().toISOString()
	return result
}

// ─────────────────────────── 结果打印 ───────────────────────────────────────

/** 打印单轮测试的关键结果 */
function printLevelResult(result) {
	const m = result.metricsAfterStable
	sep()
	console.log(`  并发 ${result.concurrency} 结果摘要`)
	sep()
	console.log(`  信令成功率: ${result.signaling.success}/${result.concurrency}`)
	console.log(`  连接阶段耗时: ${fmtMs(result.phases.connect)}`)
	console.log(`  信令阶段耗时: ${fmtMs(result.phases.signaling)}`)
	console.log(`  稳定期时长: ${fmtMs(result.phases.stable)}`)

	if (m) {
		console.log(`\n  /metrics 采集（稳定期结束）:`)
		console.log(`    当前连接数: ${m.connections?.current ?? "N/A"}`)
		console.log(`    累计连接数: ${m.connections?.total ?? "N/A"}`)
		console.log(`    Producer(audio/video): ${m.producers?.audio ?? 0}/${m.producers?.video ?? 0}`)
		console.log(`    Consumer 总数: ${m.consumers ?? "N/A"}`)
		if (m.messages && m.messages.length > 0) {
			console.log(`\n  事件处理耗时（均值）:`)
			for (const msg of m.messages) {
				if (msg.count > 0) {
					console.log(
						`    ${msg.event.padEnd(28)} count=${String(msg.count).padStart(5)}  avgDuration=${fmtMs(Math.round(msg.avgDuration))}  errorRate=${(msg.errorRate * 100).toFixed(2)}%`,
					)
				}
			}
		}
	} else {
		console.log(`  /metrics 无法访问`)
	}

	if (Object.keys(result.latencyByEvent).length > 0) {
		console.log(`\n  客户端侧信令往返延迟（均值）:`)
		for (const [key, stat] of Object.entries(result.latencyByEvent)) {
			console.log(`    ${key.padEnd(30)} avg=${fmtMs(stat.avg)}  min=${fmtMs(stat.min)}  max=${fmtMs(stat.max)}`)
		}
	}
}

/** 打印所有轮次的对比汇总表 */
function printSummary(allResults) {
	console.log(`\n`)
	sep("═")
	console.log("  SFU 并发压力测试汇总表")
	sep("═")

	const header = [
		"并发数".padStart(6),
		"信令成功".padStart(8),
		"连接耗时".padStart(10),
		"信令耗时".padStart(10),
		"活跃连接".padStart(10),
		"Producer".padStart(10),
		"Consumer".padStart(10),
		"joinRoom均值".padStart(14),
	]
	console.log(header.join(" | "))
	sep("-")

	for (const r of allResults) {
		const m = r.metricsAfterStable
		const row = [
			String(r.concurrency).padStart(6),
			`${r.signaling.success}/${r.concurrency}`.padStart(8),
			fmtMs(r.phases.connect).padStart(10),
			fmtMs(r.phases.signaling).padStart(10),
			String(m?.connections?.current ?? "N/A").padStart(10),
			String((m?.producers?.audio ?? 0) + (m?.producers?.video ?? 0)).padStart(10),
			String(m?.consumers ?? "N/A").padStart(10),
			fmtMs(r.latencyByEvent?.joinRoom?.avg).padStart(14),
		]
		console.log(row.join(" | "))
	}
	sep()
}

// ─────────────────────────── 健康检查 ───────────────────────────────────────

async function checkHealth() {
	const result = await httpGet(`${CONFIG.sfuUrl}/health`, 5000)
	return result?.status === "healthy"
}

// ─────────────────────────── 保存结果 ───────────────────────────────────────

function saveResults(allResults) {
	const output = {
		meta: {
			testDate: new Date().toISOString(),
			sfuUrl: CONFIG.sfuUrl,
			stableMs: CONFIG.stableMs,
			levels: CONFIG.levels,
		},
		results: allResults,
	}
	try {
		fs.writeFileSync(CONFIG.outputFile, JSON.stringify(output, null, 2), "utf-8")
		console.log(`\n✓ 测试结果已写入: ${CONFIG.outputFile}`)
	} catch (err) {
		console.error(`结果保存失败: ${err.message}`)
	}
}

// ─────────────────────────── 主流程 ─────────────────────────────────────────

async function main() {
	console.log("\n")
	console.log("╔══════════════════════════════════════════════════════════════╗")
	console.log("║          SFU Mediasoup 并发压力测试                           ║")
	console.log("╚══════════════════════════════════════════════════════════════╝")
	console.log(`  目标服务: ${CONFIG.sfuUrl}`)
	console.log(`  并发级别: ${CONFIG.levels.join(" → ")}`)
	console.log(`  每级稳定期: ${CONFIG.stableMs / 1000}s`)

	// 健康检查
	process.stdout.write("  检查 SFU 服务健康状态... ")
	const healthy = await checkHealth()
	if (!healthy) {
		console.log("✗")
		console.error("\n  SFU 服务不可达。请确认：")
		console.error(`    1. SFU 服务已启动（SFU_TEST_MODE=true npm run dev --workspace=sfu）`)
		console.error(`    2. 服务地址正确：${CONFIG.sfuUrl}（可通过 SFU_URL 环境变量修改）`)
		process.exit(1)
	}
	console.log("✓\n")

	const allResults = []

	for (let i = 0; i < CONFIG.levels.length; i++) {
		const level = CONFIG.levels[i]
		const result = await runLevel(level)
		allResults.push(result)
		printLevelResult(result)

		// 判断是否出现明显瓶颈（信令成功率低于 60% 则停止加压）
		const successRate = result.signaling.success / level
		if (successRate < 0.6 && i < CONFIG.levels.length - 1) {
			console.warn(`\n  信令成功率 ${(successRate * 100).toFixed(0)}% < 60%，检测到性能瓶颈，停止加压`)
			break
		}

		// 最后一轮不需要冷却
		if (i < CONFIG.levels.length - 1) {
			console.log(`\n[冷却期] 等待 ${CONFIG.cooldownMs / 1000}s 后进入下一级...`)
			await new Promise((r) => setTimeout(r, CONFIG.cooldownMs))
		}
	}

	printSummary(allResults)
	saveResults(allResults)
}

// 未捕获的 Promise 错误
process.on("unhandledRejection", (reason) => {
	console.error("UnhandledRejection:", reason)
})

main().catch((err) => {
	console.error("测试运行异常:", err)
	process.exit(1)
})
