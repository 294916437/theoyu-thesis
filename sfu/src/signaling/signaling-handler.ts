import { Server, Socket } from "socket.io"
import { RoomManager } from "../core/room-manager"
import { Peer } from "../core/peer"
import { GrpcClient } from "../utils/grpc-client"
import { Logger } from "../utils/logger"
import { ConnectionManager } from "./connection-manager"
import { SessionManager } from "./session-manager"
import { TransportManager } from "./transport-manager"
import { MonitoringService } from "./monitoring-service"
import config from "../config/config"
import type * as mediasoupTypes from "mediasoup/node/lib/types"

export class SignalingHandler {
	private io: Server
	private roomManager: RoomManager
	private grpcClient: GrpcClient
	private connectionManager: ConnectionManager
	private sessionManager: SessionManager
	private transportManager: TransportManager
	private monitoring: MonitoringService
	private logger = new Logger("SignalingHandler")

	constructor(io: Server) {
		this.io = io
		this.roomManager = RoomManager.getInstance()
		this.grpcClient = GrpcClient.getInstance()
		this.connectionManager = new ConnectionManager()
		this.sessionManager = new SessionManager()
		this.transportManager = new TransportManager()
		this.monitoring = new MonitoringService()

		this.setupMiddleware()
		this.setupSocketHandlers()
	}

	private setupMiddleware(): void {
		// 连接认证中间件
		this.io.use(async (socket, next) => {
			try {
				const token = socket.handshake.auth.token
				if (!token) {
					return next(new Error("Authentication token required"))
				}

				// 这里可以做预验证
				next()
			} catch (error: any) {
				next(new Error("Authentication failed"))
			}
		})
	}

	private setupSocketHandlers(): void {
		this.io.on("connection", (socket: Socket) => {
			this.logger.info(`Client connected: ${socket.id}`)

			// 初始化连接管理
			this.connectionManager.setupConnection(socket)
			this.monitoring.recordConnection()

			// 注册所有事件处理器
			this.registerHandlers(socket)

			socket.on("disconnect", (reason) => {
				this.handleDisconnect(socket, reason)
			})
		})
	}

	private registerHandlers(socket: Socket): void {
		// 房间相关
		socket.on("joinRoom", (data, callback) => this.withErrorHandling(socket, "joinRoom", data, callback, this.handleJoinRoom))
		socket.on("leaveRoom", (data, callback) => this.withErrorHandling(socket, "leaveRoom", data, callback, this.handleLeaveRoom))

		// 媒体协商相关
		socket.on("getRouterRtpCapabilities", (data, callback) => this.withErrorHandling(socket, "getRouterRtpCapabilities", data, callback, this.handleGetRouterRtpCapabilities))
		socket.on("createWebRtcTransport", (data, callback) => this.withErrorHandling(socket, "createWebRtcTransport", data, callback, this.handleCreateWebRtcTransport))
		socket.on("connectWebRtcTransport", (data, callback) => this.withErrorHandling(socket, "connectWebRtcTransport", data, callback, this.handleConnectWebRtcTransport))

		// 生产者相关
		socket.on("produce", (data, callback) => this.withErrorHandling(socket, "produce", data, callback, this.handleProduce))
		socket.on("pauseProducer", (data, callback) => this.withErrorHandling(socket, "pauseProducer", data, callback, this.handlePauseProducer))
		socket.on("resumeProducer", (data, callback) => this.withErrorHandling(socket, "resumeProducer", data, callback, this.handleResumeProducer))
		socket.on("closeProducer", (data, callback) => this.withErrorHandling(socket, "closeProducer", data, callback, this.handleCloseProducer))

		// 消费者相关
		socket.on("consume", (data, callback) => this.withErrorHandling(socket, "consume", data, callback, this.handleConsume))
		socket.on("resumeConsumer", (data, callback) => this.withErrorHandling(socket, "resumeConsumer", data, callback, this.handleResumeConsumer))
		socket.on("pauseConsumer", (data, callback) => this.withErrorHandling(socket, "pauseConsumer", data, callback, this.handlePauseConsumer))

		// 音视频控制相关
		socket.on("hostToggleAudio", (data, callback) => this.withErrorHandling(socket, "hostToggleAudio", data, callback, this.handleHostToggleAudio))
		socket.on("hostToggleVideo", (data, callback) => this.withErrorHandling(socket, "hostToggleVideo", data, callback, this.handleHostToggleVideo))
		socket.on("toggleAudio", (data, callback) => this.withErrorHandling(socket, "toggleAudio", data, callback, this.handleToggleAudio))
		socket.on("toggleVideo", (data, callback) => this.withErrorHandling(socket, "toggleVideo", data, callback, this.handleToggleVideo))

		// Simulcast/SVC 支持
		socket.on("setPreferredLayers", (data, callback) => this.withErrorHandling(socket, "setPreferredLayers", data, callback, this.handleSetPreferredLayers))

		// 统计和监控
		socket.on("getStats", (data, callback) => this.withErrorHandling(socket, "getStats", data, callback, this.handleGetStats))

		// 心跳响应处理
		socket.on("ping", (data: { timestamp: number }) => {
			// 更新活动时间
			this.sessionManager.updateActivity(socket.id)
			this.connectionManager.updateActivity(socket.id)

			// 立即响应 pong
			socket.emit("pong", { timestamp: data.timestamp })
		})
	}

	// 错误处理包装器
	private async withErrorHandling(
		socket: Socket,
		eventName: string,
		data: any,
		callback: Function,
		handler: (socket: Socket, data: any, callback: Function) => Promise<void>,
	): Promise<void> {
		const startTime = Date.now()
		try {
			// 同时更新两个管理器的活动时间
			this.sessionManager.updateActivity(socket.id)
			this.connectionManager.updateActivity(socket.id)

			this.monitoring.recordMessage(eventName)

			await handler.call(this, socket, data, callback)

			const duration = Date.now() - startTime
			this.monitoring.recordMessageDuration(eventName, duration)
		} catch (error: any) {
			this.logger.error(`Error in ${eventName}`, error)
			this.monitoring.recordError(eventName)

			callback({
				error: error.message,
				code: error.code || "INTERNAL_ERROR",
			})
		}
	}

	private async handleJoinRoom(socket: Socket, data: { roomId: string; userId: string; username: string; token: string }, callback: Function): Promise<void> {
		const { roomId, userId, username, token } = data

		// 创建会话
		const session = await this.sessionManager.createSession(socket, data)

		// 获取或创建房间
		let room = this.roomManager.getRoom(roomId)
		if (!room) {
			room = await this.roomManager.createRoomInternal(roomId)
			this.logger.info(`Room ${roomId} created`)
		}

		// 处理重连（移除旧 Peer）
		const existingPeer = room.getPeer(userId)
		if (existingPeer) {
			existingPeer.socket.disconnect()
			room.removePeer(userId)
		}

		// 创建 Peer
		const peer = new Peer(
			{
				id: userId,
				userId,
				username,
				roomId,
			},
			socket,
		)

		room.addPeer(peer)
		socket.join(roomId)

		// 通知其他参与者
		socket.to(roomId).emit("newPeer", {
			peerId: peer.id,
			userId: peer.userId,
			username: peer.username,
		})

		// 通知业务系统
		await this.grpcClient.notifyParticipantJoined(roomId, userId, username)

		// 获取现有参与者和他们的生产者
		const otherPeers = room.getPeersExcept(peer.id).map((p: Peer) => ({
			peerId: p.id,
			userId: p.userId,
			username: p.username,
			producers: Array.from(p.producers.values()).map((producer) => ({
				id: producer.id,
				kind: producer.kind,
				paused: producer.paused,
			})),
		}))

		callback({
			peerId: peer.id,
			peers: otherPeers,
		})

		this.monitoring.recordRoomJoin(roomId)
		this.logger.info(`User ${username} joined room ${roomId}`)
	}

	private async handleGetRouterRtpCapabilities(socket: Socket, data: { roomId: string }, callback: Function): Promise<void> {
		const { roomId } = data
		const room = this.roomManager.getRoom(roomId)

		if (!room) {
			throw new Error("Room not found")
		}

		callback({ rtpCapabilities: room.router.rtpCapabilities })
	}

	private async handleCreateWebRtcTransport(
		socket: Socket,
		data: {
			roomId: string
			producing: boolean
			consuming: boolean
			sctpCapabilities?: mediasoupTypes.SctpCapabilities
		},
		callback: Function,
	): Promise<void> {
		const { roomId, producing, consuming, sctpCapabilities } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(session.userId)

		if (!room || !peer) {
			throw new Error("Room or peer not found")
		}

		// 权限检查
		if (producing && !this.sessionManager.hasPermission(socket.id, "publish")) {
			throw new Error("No permission to publish")
		}
		if (consuming && !this.sessionManager.hasPermission(socket.id, "subscribe")) {
			throw new Error("No permission to subscribe")
		}

		const transportOptions: mediasoupTypes.WebRtcTransportOptions = {
			...config.mediasoup.webRtcTransportOptions,
			enableSctp: !!sctpCapabilities,
			numSctpStreams: sctpCapabilities?.numStreams,
			appData: { producing, consuming, peerId: peer.id },
		}

		const transport = await room.router.createWebRtcTransport(transportOptions)

		// 注册到传输管理器
		const direction = producing ? "send" : "recv"
		this.transportManager.registerTransport(transport, direction, peer.id)

		if (producing) {
			peer.sendTransport = transport
		}
		if (consuming) {
			peer.recvTransport = transport
		}

		callback({
			id: transport.id,
			iceParameters: transport.iceParameters,
			iceCandidates: transport.iceCandidates,
			dtlsParameters: transport.dtlsParameters,
			sctpParameters: transport.sctpParameters,
		})

		this.logger.info(`WebRTC transport ${transport.id} created for peer ${peer.id}`)
	}

	private async handleConnectWebRtcTransport(
		socket: Socket,
		data: {
			roomId: string
			transportId: string
			dtlsParameters: mediasoupTypes.DtlsParameters
		},
		callback: Function,
	): Promise<void> {
		const { roomId, transportId, dtlsParameters } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(session.userId)

		if (!room || !peer) {
			throw new Error("Room or peer not found")
		}

		const transport = peer.sendTransport?.id === transportId ? peer.sendTransport : peer.recvTransport

		if (!transport) {
			throw new Error("Transport not found")
		}

		await this.transportManager.connectTransport(transport, dtlsParameters)

		callback({ success: true })
		this.logger.info(`Transport ${transportId} connected`)
	}

	private async handleProduce(
		socket: Socket,
		data: {
			roomId: string
			transportId: string
			kind: mediasoupTypes.MediaKind
			rtpParameters: mediasoupTypes.RtpParameters
			appData?: any
		},
		callback: Function,
	): Promise<void> {
		const { roomId, transportId, kind, rtpParameters, appData } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(session.userId)

		if (!room || !peer || !peer.sendTransport) {
			throw new Error("Room, peer or transport not found")
		}

		const producer = await peer.sendTransport.produce({
			kind,
			rtpParameters,
			appData: {
				...appData,
				peerId: peer.id,
				username: peer.username,
				userId: session.userId,
			},
		})

		peer.addProducer(producer)

		// 监听生产者事件
		producer.on("transportclose", () => {
			this.logger.info(`Producer ${producer.id} transport closed`)
		})

		producer.on("score", (score) => {
			// 可以用于监控质量
			this.monitoring.recordProducerScore(producer.id, score)
		})

		// 通知房间内其他参与者有新的生产者
		socket.to(roomId).emit("newProducer", {
			producerId: producer.id,
			peerId: peer.id,
			userId: session.userId,
			username: peer.username,
			kind: producer.kind,
			paused: producer.paused,
		})

		callback({ id: producer.id })

		this.monitoring.recordProducerCreated(roomId, kind)
		this.logger.info(`Producer ${producer.id} created (${kind})`)
	}

	private async handleConsume(
		socket: Socket,
		data: {
			roomId: string
			producerId: string
			rtpCapabilities: mediasoupTypes.RtpCapabilities
		},
		callback: Function,
	): Promise<void> {
		const { roomId, producerId, rtpCapabilities } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(session.userId)

		if (!room || !peer || !peer.recvTransport) {
			throw new Error("Room, peer or transport not found")
		}

		// 检查是否可以消费
		if (!room.router.canConsume({ producerId, rtpCapabilities })) {
			throw new Error("Cannot consume this producer")
		}

		const consumer = await peer.recvTransport.consume({
			producerId,
			rtpCapabilities,
			paused: true, // 初始暂停
		})

		peer.addConsumer(consumer)

		// 监听消费者事件
		consumer.on("transportclose", () => {
			this.logger.info(`Consumer ${consumer.id} transport closed`)
		})

		consumer.on("producerclose", () => {
			// 通知客户端生产者已关闭
			socket.emit("consumerClosed", { consumerId: consumer.id })
			peer.removeConsumer(consumer.id)
		})

		consumer.on("score", (score) => {
			this.monitoring.recordConsumerScore(consumer.id, score)
		})

		callback({
			id: consumer.id,
			producerId: consumer.producerId,
			kind: consumer.kind,
			rtpParameters: consumer.rtpParameters,
			type: consumer.type,
			producerPaused: consumer.producerPaused,
		})

		this.monitoring.recordConsumerCreated(roomId)
		this.logger.info(`Consumer ${consumer.id} created`)
	}

	private async handleResumeConsumer(socket: Socket, data: { roomId: string; consumerId: string }, callback: Function): Promise<void> {
		const { roomId, consumerId } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(session.userId)

		if (!room || !peer) {
			throw new Error("Room or peer not found")
		}

		const consumer = peer.consumers.get(consumerId)
		if (!consumer) {
			throw new Error("Consumer not found")
		}

		await consumer.resume()
		callback({ success: true })

		this.logger.info(`Consumer ${consumerId} resumed`)
	}

	private async handlePauseConsumer(socket: Socket, data: { roomId: string; consumerId: string }, callback: Function): Promise<void> {
		const { roomId, consumerId } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(session.userId)

		if (!room || !peer) {
			throw new Error("Room or peer not found")
		}

		const consumer = peer.consumers.get(consumerId)
		if (!consumer) {
			throw new Error("Consumer not found")
		}

		await consumer.pause()
		callback({ success: true })

		this.logger.info(`Consumer ${consumerId} paused`)
	}

	private async handlePauseProducer(socket: Socket, data: { roomId: string; producerId: string }, callback: Function): Promise<void> {
		const { roomId, producerId } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(session.userId)

		if (!room || !peer) {
			throw new Error("Room or peer not found")
		}

		const producer = peer.producers.get(producerId)
		if (!producer) {
			throw new Error("Producer not found")
		}

		await producer.pause()

		socket.to(roomId).emit("producerPaused", {
			producerId,
			peerId: peer.id,
		})

		callback({ success: true })
		this.logger.info(`Producer ${producerId} paused`)
	}

	private async handleResumeProducer(socket: Socket, data: { roomId: string; producerId: string }, callback: Function): Promise<void> {
		const { roomId, producerId } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(session.userId)

		if (!room || !peer) {
			throw new Error("Room or peer not found")
		}

		const producer = peer.producers.get(producerId)
		if (!producer) {
			throw new Error("Producer not found")
		}

		await producer.resume()

		socket.to(roomId).emit("producerResumed", {
			producerId,
			peerId: peer.id,
		})

		callback({ success: true })
		this.logger.info(`Producer ${producerId} resumed`)
	}

	private async handleCloseProducer(socket: Socket, data: { roomId: string; producerId: string }, callback: Function): Promise<void> {
		const { roomId, producerId } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(session.userId)

		if (!room || !peer) {
			throw new Error("Room or peer not found")
		}

		const producer = peer.producers.get(producerId)
		if (!producer) {
			throw new Error("Producer not found")
		}

		producer.close()
		peer.removeProducer(producerId)

		socket.to(roomId).emit("producerClosed", {
			producerId,
			peerId: peer.id,
		})

		callback({ success: true })
		this.logger.info(`Producer ${producerId} closed`)
	}

	private async handleSetPreferredLayers(
		socket: Socket,
		data: {
			roomId: string
			consumerId: string
			spatialLayer: number
			temporalLayer?: number
		},
		callback: Function,
	): Promise<void> {
		const { roomId, consumerId, spatialLayer, temporalLayer } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(session.userId)

		if (!room || !peer) {
			throw new Error("Room or peer not found")
		}

		const consumer = peer.consumers.get(consumerId)
		if (!consumer) {
			throw new Error("Consumer not found")
		}

		await consumer.setPreferredLayers({ spatialLayer, temporalLayer })

		callback({ success: true })
		this.logger.info(`Consumer ${consumerId} preferred layers set: spatial=${spatialLayer}, temporal=${temporalLayer}`)
	}

	// 主持人静音参与者（暂时性关闭）
	private async handleHostToggleAudio(socket: Socket, data: { roomId: string; targetPeerId: string; enabled: boolean }, callback: Function): Promise<void> {
		const { roomId, targetPeerId, enabled } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		if (!room) {
			throw new Error("Room not found")
		}

		// TODO:服务端验证是否为主持人

		// 找到目标参与者
		const targetPeer = room.getPeer(targetPeerId)
		if (!targetPeer) {
			throw new Error("Target peer not found")
		}

		// 找到目标的 audio producer
		const audioProducer = Array.from(targetPeer.producers.values()).find((p) => p.kind === "audio")

		if (!audioProducer) {
			throw new Error("Target has no audio producer")
		}

		// 主持人控制
		if (enabled) {
			await audioProducer.resume()
		} else {
			await audioProducer.pause()
		}

		// 广播状态变化
		this.io.to(roomId).emit("producerStateChanged", {
			producerId: audioProducer.id,
			peerId: targetPeerId,
			kind: "audio",
			paused: !enabled,
			reason: "host_forced",
		})

		callback({ success: true, paused: enabled, producerId: audioProducer.id })
		this.logger.info(`Host ${session.userId} ${enabled ? "enabled" : "disabled"} peer ${targetPeerId}`)
	}

	// 主持人关闭参与者视频（临时性）
	private async handleHostToggleVideo(socket: Socket, data: { roomId: string; targetPeerId: string; enabled: boolean }, callback: Function): Promise<void> {
		const { roomId, targetPeerId, enabled } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		if (!room) {
			throw new Error("Room not found")
		}

		// TODO:服务端验证是否为主持人

		// 找到目标参与者
		const targetPeer = room.getPeer(targetPeerId)
		if (!targetPeer) {
			throw new Error("Target peer not found")
		}

		// 找到目标的 video producer
		const videoProducer = Array.from(targetPeer.producers.values()).find((p) => p.kind === "video")

		if (!videoProducer) {
			throw new Error("Target has no video producer")
		}

		// 主持人控制
		if (enabled) {
			await videoProducer.resume()
		} else {
			await videoProducer.pause()
		}

		// 广播状态变化
		this.io.to(roomId).emit("producerStateChanged", {
			producerId: videoProducer.id,
			peerId: targetPeerId,
			kind: "video",
			paused: !enabled,
			reason: "host_forced",
		})

		callback({ success: true, enabled: enabled, producerId: videoProducer.id })
		this.logger.info(`Host ${session.userId} ${enabled ? "enabled" : "disabled"} video for peer ${targetPeerId}`)
	}

	// 参与者自主控制音频
	private async handleToggleAudio(socket: Socket, data: { roomId: string; enabled: boolean }, callback: Function): Promise<void> {
		const { roomId, enabled } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(session.userId)

		if (!room || !peer) {
			throw new Error("Room or peer not found")
		}

		const audioProducer = Array.from(peer.producers.values()).find((p) => p.kind === "audio")

		if (!audioProducer) {
			throw new Error("No audio producer found")
		}

		// 参与者自主控制
		if (enabled) {
			await audioProducer.resume()
		} else {
			await audioProducer.pause()
		}

		// 广播状态
		this.io.to(roomId).emit("producerStateChanged", {
			producerId: audioProducer.id,
			peerId: peer.id,
			kind: "audio",
			paused: !enabled,
			reason: "self_control",
		})

		callback({
			success: true,
			enabled: enabled,
			producerId: audioProducer.id,
		})
		this.logger.info(`Peer ${peer.id} ${enabled ? "enabled" : "disabled"} audio`)
	}
	// 参与者自主控制视频
	private async handleToggleVideo(socket: Socket, data: { roomId: string; enabled: boolean }, callback: Function): Promise<void> {
		const { roomId, enabled } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(session.userId)

		if (!room || !peer) {
			throw new Error("Room or peer not found")
		}

		const videoProducer = Array.from(peer.producers.values()).find((p) => p.kind === "video")

		if (!videoProducer) {
			throw new Error("No video producer found")
		}

		// 参与者自主控制
		if (enabled) {
			await videoProducer.resume()
		} else {
			await videoProducer.pause()
		}

		// 广播状态
		this.io.to(roomId).emit("producerStateChanged", {
			producerId: videoProducer.id,
			peerId: peer.id,
			kind: "video",
			paused: !enabled,
			reason: "self_control",
		})

		callback({
			success: true,
			enabled: enabled,
			producerId: videoProducer.id,
		})
		this.logger.info(`Peer ${peer.id} ${enabled ? "enabled" : "disabled"} video`)
	}

	private async handleGetStats(socket: Socket, data: { roomId: string; producerId?: string; consumerId?: string }, callback: Function): Promise<void> {
		const { roomId, producerId, consumerId } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(session.userId)

		if (!room || !peer) {
			throw new Error("Room or peer not found")
		}

		let stats
		if (producerId) {
			const producer = peer.producers.get(producerId)
			if (producer) {
				stats = await producer.getStats()
			}
		} else if (consumerId) {
			const consumer = peer.consumers.get(consumerId)
			if (consumer) {
				stats = await consumer.getStats()
			}
		}

		callback({ stats })
	}

	private async handleLeaveRoom(socket: Socket, data: any, callback: Function): Promise<void> {
		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			callback({ success: true })
			return
		}

		const { roomId, userId } = session
		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(userId)

		if (room && peer) {
			socket.to(roomId).emit("peerLeft", {
				peerId: peer.id,
				userId: peer.userId,
				username: peer.username,
			})

			await this.grpcClient.notifyParticipantLeft(roomId, peer.userId, peer.username)

			this.roomManager.removePeerFromRoom(roomId, peer.id)
			socket.leave(roomId)

			this.monitoring.recordRoomLeave(roomId)
			this.logger.info(`Peer ${peer.id} left room ${roomId}`)
		}

		this.sessionManager.destroySession(socket.id)
		callback({ success: true })
	}

	private async handleDisconnect(socket: Socket, reason: string): Promise<void> {
		const session = this.sessionManager.getSession(socket.id)
		if (session) {
			await this.handleLeaveRoom(socket, {}, () => {})
		}

		this.monitoring.recordDisconnection()
		this.logger.info(`Client disconnected: ${socket.id} (${reason})`)
	}
}
