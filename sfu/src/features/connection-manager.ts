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
    private heartbeatTimers: Map<string, NodeJS.Timeout> = new Map()
    
    // 调整时间参数
    private pingInterval = 20000 // 20秒心跳
    private connectionTimeout = 60000 // 60秒超时（给予充足的缓冲）
    private maxReconnects = 3

    setupConnection(socket: Socket): void {
        const metrics: ConnectionMetrics = {
            connectedAt: Date.now(),
            lastPingAt: Date.now(),
            reconnectCount: 0,
            messageCount: 0,
        }

        this.connections.set(socket.id, metrics)

        // 监听所有消息并更新活动时间
        socket.onAny(() => {
            this.updateActivity(socket.id)
        })

        // 监听重连
        socket.on("reconnect", (attemptNumber: number) => {
            this.handleReconnect(socket, attemptNumber)
        })

        // 监听断开，清理资源
        socket.on("disconnect", () => {
            this.cleanupConnection(socket.id)
        })

        // 启动心跳检测
        this.startHeartbeat(socket)

        this.logger.info(`Connection setup for ${socket.id}`)
    }

    /**
     * 更新连接活动时间
     */
    updateActivity(socketId: string): void {
        const metrics = this.connections.get(socketId)
        if (metrics) {
            metrics.lastPingAt = Date.now()
            metrics.messageCount++
        }
    }

    private startHeartbeat(socket: Socket): void {
        // 注册 pong 监听器（只注册一次）
        socket.once("pong", (data: { timestamp: number }) => {
            this.handlePong(socket, data)
        })

        // 定期发送 ping 和检查超时
        const timerId = setInterval(() => {
            const metrics = this.connections.get(socket.id)
            
            // 如果连接已被清理，停止心跳
            if (!metrics) {
                clearInterval(timerId)
                this.heartbeatTimers.delete(socket.id)
                return
            }

            const now = Date.now()
            const timeSinceLastActivity = now - metrics.lastPingAt

            // 检查是否超时（在发送新 ping 之前检查）
            if (timeSinceLastActivity > this.connectionTimeout) {
                this.logger.warn(
                    `Connection timeout for ${socket.id} (inactive for ${timeSinceLastActivity}ms)`
                )
                socket.disconnect(true)
                clearInterval(timerId)
                this.heartbeatTimers.delete(socket.id)
                return
            }

            // 发送 ping
            socket.emit("ping", { timestamp: now })
            
            // 记录 ping 发送时间（但不更新 lastPingAt，等待 pong）
            this.logger.debug(`Ping sent to ${socket.id}`)
            
        }, this.pingInterval)

        // 保存定时器引用
        this.heartbeatTimers.set(socket.id, timerId)
    }

    private handlePong(socket: Socket, data: { timestamp: number }): void {
        const metrics = this.connections.get(socket.id)
        if (!metrics) return

        const now = Date.now()
        const rtt = now - data.timestamp

        // 更新最后活动时间
        metrics.lastPingAt = now

        // 发送 RTT 给客户端
        socket.emit("rtt", { rtt, timestamp: now })

        this.logger.debug(`Pong received from ${socket.id}, RTT: ${rtt}ms`)

        // 重新注册下一次 pong 监听
        socket.once("pong", (nextData: { timestamp: number }) => {
            this.handlePong(socket, nextData)
        })
    }

    private handleReconnect(socket: Socket, attemptNumber: number): void {
        const metrics = this.connections.get(socket.id)
        if (!metrics) return

        metrics.reconnectCount++
        metrics.lastPingAt = Date.now() // 重置活动时间

        this.logger.info(
            `Socket ${socket.id} reconnected (attempt ${attemptNumber}, total reconnects: ${metrics.reconnectCount})`
        )

        if (metrics.reconnectCount > this.maxReconnects) {
            this.logger.warn(`Max reconnects (${this.maxReconnects}) exceeded for ${socket.id}`)
            socket.disconnect(true)
        }
    }

    /**
     * 清理连接资源
     */
    private cleanupConnection(socketId: string): void {
        // 清理心跳定时器
        const timer = this.heartbeatTimers.get(socketId)
        if (timer) {
            clearInterval(timer)
            this.heartbeatTimers.delete(socketId)
        }

        // 清理连接指标
        this.connections.delete(socketId)

        this.logger.info(`Connection cleaned up for ${socketId}`)
    }

    /**
     * 手动断开连接
     */
    disconnectSocket(socketId: string, reason: string = "Manual disconnect"): void {
        const metrics = this.connections.get(socketId)
        if (!metrics) return

        this.logger.info(`Manually disconnecting ${socketId}: ${reason}`)
        
        // 获取 socket 实例并断开（需要从外部传入或保存引用）
        // 这里只清理资源
        this.cleanupConnection(socketId)
    }

    getMetrics(socketId: string): ConnectionMetrics | undefined {
        return this.connections.get(socketId)
    }

    getAllMetrics(): Map<string, ConnectionMetrics> {
        return this.connections
    }

    /**
     * 获取连接健康状态
     */
    getConnectionHealth(socketId: string): {
        healthy: boolean
        timeSinceLastActivity: number
        messageRate: number
    } | null {
        const metrics = this.connections.get(socketId)
        if (!metrics) return null

        const now = Date.now()
        const timeSinceLastActivity = now - metrics.lastPingAt
        const connectionAge = now - metrics.connectedAt
        const messageRate = connectionAge > 0 ? (metrics.messageCount / connectionAge) * 1000 : 0

        return {
            healthy: timeSinceLastActivity < this.connectionTimeout / 2,
            timeSinceLastActivity,
            messageRate,
        }
    }

    /**
     * 清理所有连接（用于关闭服务器）
     */
    cleanup(): void {
        this.logger.info(`Cleaning up ${this.connections.size} connections`)

        // 清理所有定时器
        for (const timer of this.heartbeatTimers.values()) {
            clearInterval(timer)
        }

        this.heartbeatTimers.clear()
        this.connections.clear()
    }
}