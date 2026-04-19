import { Logger } from "../utils/logger"
import * as os from "os"

interface MessageMetrics {
	count: number
	totalDuration: number
	errors: number
}

export class MonitoringService {
	private static instance: MonitoringService

	public static getInstance(): MonitoringService {
		if (!MonitoringService.instance) {
			MonitoringService.instance = new MonitoringService()
		}
		return MonitoringService.instance
	}

	private logger = new Logger("MonitoringService")
	private readonly startTime = Date.now()

	// CPU 采样：每 5s 在后台采样一次，getMetrics 直接读取结果
	private cpuPercent = 0
	private lastCpuUsage = process.cpuUsage()
	private lastCpuSampleTime = Date.now()

	private metrics = {
		connections: 0,
		disconnections: 0,
		totalConnections: 0,
		messages: new Map<string, MessageMetrics>(),
		roomJoins: new Map<string, number>(),
		roomLeaves: new Map<string, number>(),
		producersCreated: { audio: 0, video: 0 },
		producersClosed: { audio: 0, video: 0 },
		consumersCreated: 0,
		consumersClosed: 0,
		producerScores: new Map<string, number[]>(),
		consumerScores: new Map<string, number[]>(),
	}

	private constructor() {
		this.scheduleCpuSampling()
	}

	/** 周期采样 CPU 使用率，不阻塞进程退出 */
	private scheduleCpuSampling(): void {
		const interval = setInterval(() => {
			const now = Date.now()
			const elapsedUs = (now - this.lastCpuSampleTime) * 1000 // ms → µs
			if (elapsedUs > 0) {
				const delta = process.cpuUsage(this.lastCpuUsage)
				this.cpuPercent = parseFloat((((delta.user + delta.system) / elapsedUs) * 100).toFixed(2))
			}
			this.lastCpuUsage = process.cpuUsage()
			this.lastCpuSampleTime = now
		}, 5000)
		interval.unref()
	}

	// ─── 连接 ────────────────────────────────────────────────────────────────

	recordConnection(): void {
		this.metrics.connections++
		this.metrics.totalConnections++
	}

	recordDisconnection(): void {
		this.metrics.disconnections++
		this.metrics.connections = Math.max(0, this.metrics.connections - 1)
	}

	// ─── 信令事件 ────────────────────────────────────────────────────────────

	recordMessage(eventName: string): void {
		if (!this.metrics.messages.has(eventName)) {
			this.metrics.messages.set(eventName, { count: 0, totalDuration: 0, errors: 0 })
		}
		this.metrics.messages.get(eventName)!.count++
	}

	recordMessageDuration(eventName: string, duration: number): void {
		const metric = this.metrics.messages.get(eventName)
		if (metric) {
			metric.totalDuration += duration
		}
	}

	recordError(eventName: string): void {
		const metric = this.metrics.messages.get(eventName)
		if (metric) {
			metric.errors++
		}
	}

	// ─── 房间 ────────────────────────────────────────────────────────────────

	recordRoomJoin(roomId: string): void {
		this.metrics.roomJoins.set(roomId, (this.metrics.roomJoins.get(roomId) || 0) + 1)
	}

	recordRoomLeave(roomId: string): void {
		this.metrics.roomLeaves.set(roomId, (this.metrics.roomLeaves.get(roomId) || 0) + 1)
	}

	// ─── Producer ────────────────────────────────────────────────────────────

	recordProducerCreated(roomId: string, kind: "audio" | "video"): void {
		this.metrics.producersCreated[kind]++
	}

	recordProducerClosed(kind: "audio" | "video"): void {
		this.metrics.producersClosed[kind]++
	}

	recordProducerScore(producerId: string, score: any): void {
		if (!this.metrics.producerScores.has(producerId)) {
			this.metrics.producerScores.set(producerId, [])
		}
		this.metrics.producerScores.get(producerId)!.push(score.score)
	}

	// ─── Consumer ────────────────────────────────────────────────────────────

	recordConsumerCreated(roomId: string): void {
		this.metrics.consumersCreated++
	}

	recordConsumerClosed(): void {
		this.metrics.consumersClosed++
	}

	recordConsumerScore(consumerId: string, score: any): void {
		if (!this.metrics.consumerScores.has(consumerId)) {
			this.metrics.consumerScores.set(consumerId, [])
		}
		this.metrics.consumerScores.get(consumerId)!.push(score.score)
	}

	// ─── 指标快照 ────────────────────────────────────────────────────────────

	getMetrics() {
		const mem = process.memoryUsage()
		const toMB = (bytes: number) => parseFloat((bytes / 1024 / 1024).toFixed(2))

		const activeAudio = Math.max(0, this.metrics.producersCreated.audio - this.metrics.producersClosed.audio)
		const activeVideo = Math.max(0, this.metrics.producersCreated.video - this.metrics.producersClosed.video)
		const activeConsumers = Math.max(0, this.metrics.consumersCreated - this.metrics.consumersClosed)

		const messageStats = Array.from(this.metrics.messages.entries()).map(([event, metric]) => ({
			event,
			count: metric.count,
			avgDuration: metric.count > 0 ? Math.round(metric.totalDuration / metric.count) : 0,
			errorRate: metric.count > 0 ? parseFloat((metric.errors / metric.count).toFixed(4)) : 0,
			errors: metric.errors,
		}))

		return {
			uptime: Math.floor((Date.now() - this.startTime) / 1000),
			cpu: {
				usagePercent: this.cpuPercent,
				cores: os.cpus().length,
			},
			memory: {
				rssMB: toMB(mem.rss),
				heapUsedMB: toMB(mem.heapUsed),
				heapTotalMB: toMB(mem.heapTotal),
				externalMB: toMB(mem.external),
				systemTotalMB: toMB(os.totalmem()),
				systemFreeMB: toMB(os.freemem()),
			},
			connections: {
				current: this.metrics.connections,
				total: this.metrics.totalConnections,
				disconnections: this.metrics.disconnections,
			},
			producers: {
				audio: {
					total: this.metrics.producersCreated.audio,
					closed: this.metrics.producersClosed.audio,
					active: activeAudio,
				},
				video: {
					total: this.metrics.producersCreated.video,
					closed: this.metrics.producersClosed.video,
					active: activeVideo,
				},
				totalActive: activeAudio + activeVideo,
			},
			consumers: {
				total: this.metrics.consumersCreated,
				closed: this.metrics.consumersClosed,
				active: activeConsumers,
			},
			messages: messageStats,
			rooms: {
				joins: Object.fromEntries(this.metrics.roomJoins),
				leaves: Object.fromEntries(this.metrics.roomLeaves),
			},
		}
	}

	// 定期输出统计日志（保留接口，按需启用）
	startPeriodicLogging(intervalMs: number = 60000): void {
		const interval = setInterval(() => {
			const s = this.getMetrics()
			this.logger.info(
				`[Metrics] uptime=${s.uptime}s cpu=${s.cpu.usagePercent}% ` +
					`mem=${s.memory.heapUsedMB}MB/${s.memory.rssMB}MB ` +
					`conn=${s.connections.current} ` +
					`producers(active)=${s.producers.totalActive} consumers(active)=${s.consumers.active}`,
			)
		}, intervalMs)
		interval.unref()
	}
}
