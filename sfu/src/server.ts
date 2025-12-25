import express, { Request, Response, NextFunction } from "express"
import { createServer } from "http"
import { Server } from "socket.io"
import cors from "cors"
import config from "./config/config"
import { MediasoupManager } from "./mediasoup/mediasoup-manager"
import { SignalingHandler } from "./signaling/signaling-handler"
import { NacosClient } from "./utils/nacos-client"
import { GrpcClient } from "./utils/grpc-client"
import { Logger } from "./utils/logger"
import { RoomManager } from "./core/room-manager"
import { GlobalErrorHandler } from "./utils/error-handler"

const logger = new Logger("SFU-Server")
const app = express()
const httpServer = createServer(app)

// 初始化 Socket.io
const io = new Server(httpServer, {
	cors: config.server.cors,
	transports: ["websocket", "polling"],
	pingTimeout: 60000,
	pingInterval: 25000,
	maxHttpBufferSize: 1e8,
})

async function startServer() {
	try {
		// 初始化全局错误处理器
		GlobalErrorHandler.getInstance()
		// 初始化 Express
		app.use(cors(config.server.cors))
		app.use(express.json())

		// Express 错误处理中间件
		app.use((err: Error, req: Request, res: Response, next: NextFunction) => {
			logger.error("Express error:", err)
			res.status(500).json({ error: "Internal Server Error" })
		})

		// 健康检查接口
		app.get("/health", (req, res) => {
			res.json({
				status: "healthy",
				timestamp: new Date().toISOString(),
				uptime: process.uptime(),
			})
		})
		// 统计端点
		app.get("/metrics", (req, res) => {
			const handler = (httpServer as any).signalingHandler as SignalingHandler
			if (handler && handler["monitoring"]) {
				res.json(handler["monitoring"].getMetrics())
			} else {
				res.status(503).json({ error: "Metrics not available" })
			}
		})

		// 房间统计接口
		app.get("/api/stats", (req, res) => {
			const roomManager = RoomManager.getInstance()
			res.json(roomManager.getRoomStats())
		})

		// 404 处理
		app.use((req, res) => {
			res.status(404).json({ error: "Not Found" })
		})

		// 初始化 Mediasoup
		logger.info("Initializing Mediasoup...")
		const mediasoupManager = MediasoupManager.getInstance()
		mediasoupManager.init()

		// 初始化 gRPC 客户端
		logger.info("Initializing gRPC client...")
		const grpcClient = GrpcClient.getInstance()
		try {
			await grpcClient.init()
			logger.info("gRPC client initialized successfully")
		} catch (error) {
			logger.warn("gRPC client initialization failed, continuing without gRPC", error)
		}

		// 初始化信令处理器
		logger.info("Initializing Signaling Handler...")
		const signalingHandler = new SignalingHandler(io)

		signalingHandler["monitoring"].startPeriodicLogging(60000)

		// 初始化 Nacos 客户端并注册服务
		logger.info("Initializing Nacos client...")
		const nacosClient = NacosClient.getInstance()
		try {
			await nacosClient.init()
			await nacosClient.registerService()
			logger.info("Nacos client initialized successfully")
		} catch (error) {
			logger.warn("Nacos initialization failed, continuing without service registration", error)
		}

		// 启动服务器
		httpServer.listen(config.server.port, config.server.host, () => {
			logger.info(`SFU Server is running on ${config.server.host}:${config.server.port}`)
			logger.info(`WebSocket endpoint: ws://localhost:${config.server.port}`)
			logger.info(`Environment: ${process.env.NODE_ENV || "development"}`)
			logger.info(`Metrics: http://localhost:${config.server.port}/metrics`)
		})

		// 优雅关闭
		const shutdown = async (signal: string) => {
			logger.info(`${signal} received, shutting down gracefully...`)

			try {
				// 关闭 gRPC 客户端
				grpcClient.close()

				// 注销 Nacos 服务
				await nacosClient.close()

				// 关闭所有房间和 Mediasoup
				const roomManager = RoomManager.getInstance()
				for (const room of roomManager.getAllRooms()) {
					roomManager.removeRoom(room.id)
				}
				await mediasoupManager.close()

				// 关闭 Socket.io
				io.close(() => {
					logger.info("Socket.io server closed")
				})

				// 关闭 HTTP 服务器
				httpServer.close(() => {
					logger.info("HTTP server closed")
					process.exit(0)
				})

				// 强制退出超时
				setTimeout(() => {
					logger.error("Forced shutdown after timeout")
					process.exit(1)
				}, 10000)
			} catch (error) {
				logger.error("Error during shutdown", error)
				process.exit(1)
			}
		}

		process.on("SIGTERM", () => shutdown("SIGTERM"))
		process.on("SIGINT", () => shutdown("SIGINT"))
	} catch (error) {
		logger.error("Failed to start server", error)
		process.exit(1)
	}
}

startServer()
