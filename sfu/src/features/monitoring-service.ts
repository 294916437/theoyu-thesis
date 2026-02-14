import { Logger } from "../utils/logger"

interface MessageMetrics {
	count: number
	totalDuration: number
	errors: number
}

export class MonitoringService {
	private logger = new Logger("MonitoringService")
	private metrics = {
		connections: 0,
		disconnections: 0,
		totalConnections: 0,
		messages: new Map<string, MessageMetrics>(),
		roomJoins: new Map<string, number>(),
		roomLeaves: new Map<string, number>(),
		producersCreated: { audio: 0, video: 0 },
		consumersCreated: 0,
		producerScores: new Map<string, number[]>(),
		consumerScores: new Map<string, number[]>(),
	}

	recordConnection(): void {
		this.metrics.connections++
		this.metrics.totalConnections++
	}

	recordDisconnection(): void {
		this.metrics.disconnections++
		this.metrics.connections = Math.max(0, this.metrics.connections - 1)
	}

	recordMessage(eventName: string): void {
		if (!this.metrics.messages.has(eventName)) {
			this.metrics.messages.set(eventName, {
				count: 0,
				totalDuration: 0,
				errors: 0,
			})
		}
		const metric = this.metrics.messages.get(eventName)!
		metric.count++
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

	recordRoomJoin(roomId: string): void {
		const count = this.metrics.roomJoins.get(roomId) || 0
		this.metrics.roomJoins.set(roomId, count + 1)
	}

	recordRoomLeave(roomId: string): void {
		const count = this.metrics.roomLeaves.get(roomId) || 0
		this.metrics.roomLeaves.set(roomId, count + 1)
	}

	recordProducerCreated(roomId: string, kind: "audio" | "video"): void {
		this.metrics.producersCreated[kind]++
	}

	recordConsumerCreated(roomId: string): void {
		this.metrics.consumersCreated++
	}

	recordProducerScore(producerId: string, score: any): void {
		if (!this.metrics.producerScores.has(producerId)) {
			this.metrics.producerScores.set(producerId, [])
		}
		this.metrics.producerScores.get(producerId)!.push(score.score)
	}

	recordConsumerScore(consumerId: string, score: any): void {
		if (!this.metrics.consumerScores.has(consumerId)) {
			this.metrics.consumerScores.set(consumerId, [])
		}
		this.metrics.consumerScores.get(consumerId)!.push(score.score)
	}

	getMetrics() {
		const messageStats = Array.from(this.metrics.messages.entries()).map(([event, metric]) => ({
			event,
			count: metric.count,
			avgDuration: metric.count > 0 ? metric.totalDuration / metric.count : 0,
			errorRate: metric.count > 0 ? metric.errors / metric.count : 0,
		}))

		return {
			connections: {
				current: this.metrics.connections,
				total: this.metrics.totalConnections,
			},
			messages: messageStats,
			producers: this.metrics.producersCreated,
			consumers: this.metrics.consumersCreated,
			rooms: {
				joins: Object.fromEntries(this.metrics.roomJoins),
				leaves: Object.fromEntries(this.metrics.roomLeaves),
			},
		}
	}

	// 定期输出统计信息
	startPeriodicLogging(intervalMs: number = 60000): void {
		// setInterval(() => {
		// 	const stats = this.getMetrics()
		// 	this.logger.info("=== Monitoring Stats ===")
		// 	this.logger.info(`Active Connections: ${stats.connections.current}`)
		// 	this.logger.info(`Total Connections: ${stats.connections.total}`)
		// 	this.logger.info(`Audio Producers: ${stats.producers.audio}`)
		// 	this.logger.info(`Video Producers: ${stats.producers.video}`)
		// 	this.logger.info(`Consumers: ${stats.consumers}`)
		// }, intervalMs)
	}
}
