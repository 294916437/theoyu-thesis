import express from "express";
import { createServer } from "http";
import { Server } from "socket.io";
import cors from "cors";
import config from "./config/config";
import { MediasoupManager } from "./mediasoup/mediasoup-manager";
import { SignalingHandler } from "./signaling/signaling-handler";
import { NacosClient } from "./utils/nacos-client";
import { GrpcClient } from "./utils/grpc-client";
import { Logger } from "./utils/logger";
import { RoomManager } from "./core/room-manager";

const logger = new Logger("SFU-Server");

async function startServer() {
	try {
		// 初始化 Express
		const app = express();
		app.use(cors(config.server.cors));
		app.use(express.json());

		// 健康检查接口
		app.get("/health", (req, res) => {
			res.json({
				status: "healthy",
				timestamp: new Date().toISOString(),
				uptime: process.uptime(),
			});
		});

		// 房间统计接口
		app.get("/api/stats", (req, res) => {
			const roomManager = RoomManager.getInstance();
			res.json(roomManager.getRoomStats());
		});

		// 创建 HTTP 服务器
		const httpServer = createServer(app);

		// 初始化 Socket.io
		const io = new Server(httpServer, {
			cors: config.server.cors,
			pingTimeout: 60000,
			pingInterval: 25000,
		});

		// 初始化 Mediasoup
		logger.info("Initializing Mediasoup...");
		const mediasoupManager = MediasoupManager.getInstance();
		await mediasoupManager.init();

		// 初始化 gRPC 客户端
		logger.info("Initializing gRPC client...");
		const grpcClient = GrpcClient.getInstance();
		try {
			await grpcClient.init();
			logger.info("gRPC client initialized successfully");
		} catch (error) {
			logger.warn("gRPC client initialization failed, continuing without gRPC", error);
		}

		// 初始化信令处理
		logger.info("Initializing Signaling Handler...");
		new SignalingHandler(io);

		// 初始化 Nacos 客户端并注册服务
		logger.info("Initializing Nacos client...");
		const nacosClient = NacosClient.getInstance();
		try {
			await nacosClient.init();
			await nacosClient.registerService();
			logger.info("Nacos client initialized successfully");
		} catch (error) {
			logger.warn(
				"Nacos initialization failed, continuing without service registration",
				error
			);
		}

		// 启动服务器
		httpServer.listen(config.server.port, config.server.host, () => {
			logger.info(`SFU Server is running on ${config.server.host}:${config.server.port}`);
			logger.info(`Environment: ${process.env.NODE_ENV || "development"}`);
		});

		// 优雅关闭
		const shutdown = async (signal: string) => {
			logger.info(`${signal} received, shutting down gracefully...`);

			try {
				// 关闭 gRPC 客户端
				grpcClient.close();

				// 注销 Nacos 服务
				await nacosClient.close();

				// 关闭所有房间和 Mediasoup
				const roomManager = RoomManager.getInstance();
				for (const room of roomManager.getAllRooms()) {
					roomManager.removeRoom(room.id);
				}
				await mediasoupManager.close();

				// 关闭 Socket.io
				io.close(() => {
					logger.info("Socket.io server closed");
				});

				// 关闭 HTTP 服务器
				httpServer.close(() => {
					logger.info("HTTP server closed");
					process.exit(0);
				});

				// 强制退出超时
				setTimeout(() => {
					logger.error("Forced shutdown after timeout");
					process.exit(1);
				}, 10000);
			} catch (error) {
				logger.error("Error during shutdown", error);
				process.exit(1);
			}
		};

		process.on("SIGTERM", () => shutdown("SIGTERM"));
		process.on("SIGINT", () => shutdown("SIGINT"));

		// 未捕获异常处理
		process.on("uncaughtException", (error) => {
			logger.error("Uncaught Exception", error);
			process.exit(1);
		});

		process.on("unhandledRejection", (reason, promise) => {
			logger.error("Unhandled Rejection", { reason, promise });
			process.exit(1);
		});
	} catch (error) {
		logger.error("Failed to start server", error);
		process.exit(1);
	}
}

startServer();
