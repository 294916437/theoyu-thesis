import { Server, Socket } from "socket.io"
import { RoomManager } from "../core/room-manager"
import { Peer } from "../core/peer"
import { GrpcClient } from "../utils/grpc-client"
import { Logger } from "../utils/logger"
import { ConnectionManager } from "./connection-manager"
import { SessionManager, Session } from "./session-manager"
import { TransportManager } from "./transport-manager"
import { MonitoringService } from "./monitoring-service"
import config from "../config/config"
import type * as mediasoupTypes from "mediasoup/node/lib/types"

export class SocketHandler {
	private io: Server
	private roomManager: RoomManager
	private grpcClient: GrpcClient
	private connectionManager: ConnectionManager
	private sessionManager: SessionManager
	private transportManager: TransportManager
	private monitoring: MonitoringService
	private logger = new Logger("SocketHandler")

	constructor(io: Server) {
		this.io = io
		this.roomManager = RoomManager.getInstance()
		this.grpcClient = GrpcClient.getInstance()
		this.connectionManager = new ConnectionManager()
		this.sessionManager = new SessionManager()
		this.transportManager = new TransportManager()
		this.monitoring = MonitoringService.getInstance()

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
		socket.on("closeRoom", (data, callback) => this.withErrorHandling(socket, "closeRoom", data, callback, this.handleCloseRoom))
		socket.on("removeParticipant", (data, callback) => this.withErrorHandling(socket, "removeParticipant", data, callback, this.handleRemoveParticipant))

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
		socket.on("hostMuteAll", (data, callback) => this.withErrorHandling(socket, "hostMuteAll", data, callback, this.handleHostMuteAll))
		socket.on("hostDisableAllVideo", (data, callback) => this.withErrorHandling(socket, "hostDisableAllVideo", data, callback, this.handleHostDisableAllVideo))

		// Simulcast/SVC 支持
		socket.on("setPreferredLayers", (data, callback) => this.withErrorHandling(socket, "setPreferredLayers", data, callback, this.handleSetPreferredLayers))

		// 聚光灯模式
		socket.on("requestSpotlight", (data, callback) => this.withErrorHandling(socket, "requestSpotlight", data, callback, this.handleRequestSpotlight))
		socket.on("setSpotlight", (data, callback) => this.withErrorHandling(socket, "setSpotlight", data, callback, this.handleSetSpotlight))

		// 统计和监控
		socket.on("getStats", (data, callback) => this.withErrorHandling(socket, "getStats", data, callback, this.handleGetStats))

		// 心跳响应处理
		socket.on("ping", (data: { timestamp: number }) => {
			// 更新活动时间
			this.sessionManager.updateActivity(socket.id)
			this.connectionManager.updateActivity(socket.id)
			// 立即响应 pong
			socket.emit("pong", { timestamp: data?.timestamp || Date.now() })
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

		// 通知业务系统，测试模式下跳过通知
		if (!config.testMode.enabled) {
			await this.grpcClient.notifyParticipantJoined(roomId, userId, username)
		}

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
			this.monitoring.recordProducerClosed(producer.kind as "audio" | "video")
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
			this.monitoring.recordConsumerClosed()
		})

		consumer.on("producerclose", () => {
			// 通知客户端生产者已关闭
			socket.emit("consumerClosed", { consumerId: consumer.id })
			peer.removeConsumer(consumer.id)
			this.monitoring.recordConsumerClosed()
		})

		consumer.on("score", (score) => {
			this.monitoring.recordConsumerScore(consumer.id, score)
		})

		// 监听 Simulcast 层级变化，实现码率自适应通知
		consumer.on("layerschange", (layers) => {
			this.logger.debug(`Consumer ${consumer.id} layers changed: ${JSON.stringify(layers)}`)
			socket.emit("consumerLayersChanged", {
				consumerId: consumer.id,
				producerId: consumer.producerId,
				spatialLayer: layers ? layers.spatialLayer : null,
				temporalLayer: layers ? layers.temporalLayer : null,
			})
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
		this.monitoring.recordProducerClosed(producer.kind as "audio" | "video")

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
	// 主持人一键静音所有参与者（暂时性）
	private async handleHostMuteAll(socket: Socket, data: { roomId: string }, callback: Function): Promise<void> {
		const { roomId } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		if (!room) {
			throw new Error("Room not found")
		}

		// TODO: 服务端验证是否为主持人

		// 获取房间内所有参与者（排除主持人自己）
		const allPeers = room.getPeersExcept(session.userId)

		let mutedCount = 0
		const failedPeers: string[] = []

		// 遍历所有参与者，静音他们的音频
		for (const targetPeer of allPeers) {
			// 找到目标的 audio producer
			const audioProducer = Array.from(targetPeer.producers.values()).find((p) => p.kind === "audio")

			if (!audioProducer) {
				// 该参与者没有音频流，跳过
				continue
			}

			try {
				// 强制暂停音频
				await audioProducer.pause()

				// 广播状态变化到所有人（包括被控制者本人）
				this.io.to(roomId).emit("producerStateChanged", {
					producerId: audioProducer.id,
					peerId: targetPeer.id,
					kind: "audio",
					paused: true,
					reason: "host_forced",
				})

				mutedCount++
				this.logger.info(`Host ${session.userId} muted peer ${targetPeer.id}`)
			} catch (error: any) {
				this.logger.error(`Failed to mute peer ${targetPeer.id}:`, error)
				failedPeers.push(targetPeer.id)
			}
		}

		// 返回操作结果
		callback({
			success: true,
			mutedCount,
			totalCount: allPeers.length,
			failedPeers: failedPeers.length > 0 ? failedPeers : undefined,
		})

		this.logger.info(`Host ${session.userId} muted ${mutedCount}/${allPeers.length} participants`)
	}

	// 主持人一键关闭所有参与者视频（暂时性）
	private async handleHostDisableAllVideo(socket: Socket, data: { roomId: string }, callback: Function): Promise<void> {
		const { roomId } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		if (!room) {
			throw new Error("Room not found")
		}

		// TODO: 服务端验证是否为主持人

		// 获取房间内所有参与者（排除主持人自己）
		const allPeers = room.getPeersExcept(session.userId)

		let disabledCount = 0
		const failedPeers: string[] = []

		// 遍历所有参与者，关闭他们的视频
		for (const targetPeer of allPeers) {
			// 只处理普通成员（role === 1），跳过其他主持人
			// 注: 如果需要角色判断，可从 session 或 peer.appData 获取

			// 找到目标的 video producer
			const videoProducer = Array.from(targetPeer.producers.values()).find((p) => p.kind === "video")

			if (!videoProducer) {
				// 该参与者没有视频流，跳过
				continue
			}

			try {
				// 强制暂停视频
				await videoProducer.pause()

				// 广播状态变化到所有人（包括被控制者本人）
				this.io.to(roomId).emit("producerStateChanged", {
					producerId: videoProducer.id,
					peerId: targetPeer.id,
					kind: "video",
					paused: true,
					reason: "host_forced",
				})

				disabledCount++
				this.logger.info(`Host ${session.userId} disabled video for peer ${targetPeer.id}`)
			} catch (error: any) {
				this.logger.error(`Failed to disable video for peer ${targetPeer.id}:`, error)
				failedPeers.push(targetPeer.id)
			}
		}

		// 返回操作结果
		callback({
			success: true,
			disabledCount,
			totalCount: allPeers.length,
			failedPeers: failedPeers.length > 0 ? failedPeers : undefined,
		})

		this.logger.info(`Host ${session.userId} disabled video for ${disabledCount}/${allPeers.length} participants`)
	}

	// 普通参与者申请聚光灯模式
	private async handleRequestSpotlight(socket: Socket, data: { roomId: string; requesterId: string; requesterUsername: string }, callback: Function): Promise<void> {
		const { roomId, requesterId, requesterUsername } = data

		const room = this.roomManager.getRoom(roomId)
		if (!room) {
			throw new Error("Room not found")
		}

		// 将申请广播给房间内所有人，由前端判断是否为主持人来决定是否弹窗
		socket.to(roomId).emit("spotlightRequest", {
			requesterId,
			requesterUsername,
		})

		callback({ success: true })
		this.logger.info(`Peer ${requesterId} requested spotlight in room ${roomId}`)
	}

	// 设置聚光灯（主持人操作）
	private async handleSetSpotlight(socket: Socket, data: { roomId: string; targetPeerId: string | null; active: boolean }, callback: Function): Promise<void> {
		const { roomId, targetPeerId, active } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const room = this.roomManager.getRoom(roomId)
		if (!room) {
			throw new Error("Room not found")
		}

		// 广播聚光灯状态变化给房间内所有人（含发送者）
		this.io.to(roomId).emit("spotlightChanged", {
			targetPeerId: active ? targetPeerId : null,
			active,
			setBy: session.userId,
		})

		callback({ success: true })
		this.logger.info(`Peer ${session.userId} ${active ? "enabled" : "disabled"} spotlight for ${targetPeerId} in room ${roomId}`)
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

	private async handleLeaveRoom(
		socket: Socket,
		data: {
			targetUserId?: string
			reason?: string
		},
		callback: Function,
	): Promise<void> {
		// 1. 确定操作目标
		let targetSocketId: string
		let targetSession: Session | undefined

		if (data.targetUserId) {
			// 主持人踢人场景：找到目标用户的 session
			targetSession = this.sessionManager.getSessionByUserId(data.targetUserId)
			if (!targetSession) {
				this.logger.warn(`Target user ${data.targetUserId} session not found`)
				callback({ success: false, error: "Target user not found" })
				return
			}
			targetSocketId = targetSession.socketId
		} else {
			// 普通离开场景：操作自己
			targetSocketId = socket.id
			targetSession = this.sessionManager.getSession(targetSocketId)
		}

		if (!targetSession) {
			this.logger.warn(`Session not found for socket ${targetSocketId}`)
			callback({ success: false })
			return
		}

		const { roomId, userId } = targetSession
		const room = this.roomManager.getRoom(roomId)
		const peer = room?.getPeer(userId)

		if (room && peer) {
			const reason = data.reason || "self_leave"

			// 广播离开事件
			socket.to(roomId).emit("peerLeft", {
				peerId: peer.id,
				userId: peer.userId,
				username: peer.username,
				reason,
			})

			// 如果是被踢出，给目标用户发送特殊事件
			if (reason === "removed_by_host") {
				this.io.to(targetSocketId).emit("removedFromRoom", {
					roomId,
					reason: "Host removed you from the meeting",
				})
			}

			// 通知业务系统
			if (!config.testMode.enabled) {
				await this.grpcClient.notifyParticipantLeft(roomId, peer.userId, peer.username)
			}

			// 清理房间资源
			this.roomManager.removePeerFromRoom(roomId, peer.id)

			// 获取目标用户的 socket 实例
			const targetSocket = this.io.sockets.sockets.get(targetSocketId)
			if (targetSocket) {
				targetSocket.leave(roomId)

				// 如果是被踢出，主动断开连接
				if (reason === "removed_by_host") {
					setTimeout(() => {
						targetSocket.disconnect(true)
					}, 500)
				}
			}

			this.monitoring.recordRoomLeave(roomId)
			this.logger.info(`Peer ${peer.id} left room ${roomId} (reason: ${reason})`)
		}

		this.sessionManager.destroySession(targetSocketId)
		callback({ success: true })
	}

	private async handleRemoveParticipant(socket: Socket, data: { targetPeerId: string }, callback: Function): Promise<void> {
		const { targetPeerId } = data

		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		// 1. 验证主持人权限
		// TODO: 从业务系统验证是否为主持人

		// 复用 handleLeaveRoom，传入特殊参数
		await this.handleLeaveRoom(
			socket,
			{
				targetUserId: targetPeerId,
				reason: "removed_by_host",
			},
			callback,
		)

		// 5. 通知操作者
		callback({
			success: true,
			message: `Participant ${targetPeerId} has been removed`,
		})
	}

	private async handleCloseRoom(socket: Socket, data: { roomId?: string; reason?: string }, callback: Function): Promise<void> {
		const session = this.sessionManager.getSession(socket.id)
		if (!session) {
			throw new Error("Session not found")
		}

		const roomId = data.roomId || session.roomId
		if (roomId !== session.roomId) {
			const error: any = new Error("Cannot close a room that the user has not joined")
			error.code = "FORBIDDEN"
			throw error
		}

		// TODO: 从业务系统验证是否为主持人
		const room = this.roomManager.getRoom(roomId)
		const sessions = this.sessionManager.getSessionsByRoom(roomId)
		const reason = data.reason || "host_closed"

		socket.to(roomId).emit("roomClosed", {
			roomId,
			closedBy: session.userId,
			username: session.username,
			reason,
			timestamp: Date.now(),
		})

		if (room) {
			this.roomManager.removeRoom(roomId)
		}

		for (const roomSession of sessions) {
			const roomSocket = this.io.sockets.sockets.get(roomSession.socketId)
			if (roomSocket) {
				roomSocket.leave(roomId)
			}
			this.sessionManager.destroySession(roomSession.socketId)
		}

		this.logger.info(`Room ${roomId} closed by ${session.userId} (reason: ${reason})`)
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
