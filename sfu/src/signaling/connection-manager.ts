import { Socket } from "socket.io"
import { Logger } from "../utils/logger"

interface ConnectionMetrics {
	connectedAt: number
	lastPingAt: number
	reconnectCount: number
	messageCount: number
}

export class ConnectionManager {
	private logger = new Logger("ConnectionManager")
	private connections: Map<string, ConnectionMetrics> = new Map()
	private pingInterval = 15000 // 15秒心跳
	private pingTimeout = 5000 // 5秒超时
	private maxReconnects = 3

	setupConnection(socket: Socket): void {
		const metrics: ConnectionMetrics = {
			connectedAt: Date.now(),
			lastPingAt: Date.now(),
			reconnectCount: 0,
			messageCount: 0,
		}

		this.connections.set(socket.id, metrics)

		// 启动心跳检测
		this.startHeartbeat(socket)

		// 监听重连
		socket.on("reconnect", (attemptNumber: number) => {
			this.handleReconnect(socket, attemptNumber)
		})

		// 监听消息计数
		socket.onAny(() => {
			const m = this.connections.get(socket.id)
			if (m) m.messageCount++
		})
	}

	private startHeartbeat(socket: Socket): void {
		const interval = setInterval(() => {
			const metrics = this.connections.get(socket.id)
			if (!metrics) {
				clearInterval(interval)
				return
			}

			const timeSinceLastPing = Date.now() - metrics.lastPingAt
			if (timeSinceLastPing > this.pingInterval + this.pingTimeout) {
				this.logger.warn(`Connection timeout for ${socket.id}`)
				socket.disconnect(true)
				clearInterval(interval)
				return
			}

			socket.emit("ping", { timestamp: Date.now() })
		}, this.pingInterval)

		socket.on("pong", (data: { timestamp: number }) => {
			const metrics = this.connections.get(socket.id)
			if (metrics) {
				metrics.lastPingAt = Date.now()
				const rtt = Date.now() - data.timestamp
				socket.emit("rtt", { rtt })
			}
		})

		socket.on("disconnect", () => {
			clearInterval(interval)
			this.connections.delete(socket.id)
		})
	}

	private handleReconnect(socket: Socket, attemptNumber: number): void {
		const metrics = this.connections.get(socket.id)
		if (!metrics) return

		metrics.reconnectCount++
		this.logger.info(`Socket ${socket.id} reconnected (attempt ${attemptNumber})`)

		if (metrics.reconnectCount > this.maxReconnects) {
			this.logger.warn(`Max reconnects exceeded for ${socket.id}`)
			socket.disconnect(true)
		}
	}

	getMetrics(socketId: string): ConnectionMetrics | undefined {
		return this.connections.get(socketId)
	}

	getAllMetrics(): Map<string, ConnectionMetrics> {
		return this.connections
	}
}
