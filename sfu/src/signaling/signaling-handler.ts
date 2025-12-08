import { Server, Socket } from "socket.io";
import { RoomManager } from "../core/room-manager";
import { Peer } from "../core/peer";
import { GrpcClient } from "../utils/grpc-client";
import { Logger } from "../utils/logger";
import config from "../config/config";
import type * as mediasoupTypes from "mediasoup/node/lib/types";

export class SignalingHandler {
	private io: Server;
	private roomManager: RoomManager;
	private grpcClient: GrpcClient;
	private logger = new Logger("SignalingHandler");
	private socketToPeerMap: Map<string, { roomId: string; userId: string }> = new Map();

	constructor(io: Server) {
		this.io = io;
		this.roomManager = RoomManager.getInstance();
		this.grpcClient = GrpcClient.getInstance();
		this.setupSocketHandlers();
	}

	private setupSocketHandlers(): void {
		this.io.on("connection", (socket: Socket) => {
			this.logger.info(`Client connected: ${socket.id}`);

			socket.on("joinRoom", (data, callback) => this.handleJoinRoom(socket, data, callback));
			socket.on("getRouterRtpCapabilities", (data, callback) =>
				this.handleGetRouterRtpCapabilities(socket, data, callback)
			);
			socket.on("createWebRtcTransport", (data, callback) =>
				this.handleCreateWebRtcTransport(socket, data, callback)
			);
			socket.on("connectWebRtcTransport", (data, callback) =>
				this.handleConnectWebRtcTransport(socket, data, callback)
			);
			socket.on("produce", (data, callback) => this.handleProduce(socket, data, callback));
			socket.on("consume", (data, callback) => this.handleConsume(socket, data, callback));
			socket.on("resumeConsumer", (data, callback) =>
				this.handleResumeConsumer(socket, data, callback)
			);
			socket.on("pauseProducer", (data, callback) =>
				this.handlePauseProducer(socket, data, callback)
			);
			socket.on("resumeProducer", (data, callback) =>
				this.handleResumeProducer(socket, data, callback)
			);
			socket.on("closeProducer", (data, callback) =>
				this.handleCloseProducer(socket, data, callback)
			);
			socket.on("leaveRoom", () => this.handleLeaveRoom(socket));
			socket.on("disconnect", () => this.handleDisconnect(socket));
		});
	}

	private async handleJoinRoom(
		socket: Socket,
		data: { roomId: string; userId: string; username: string; token: string },
		callback: Function
	): Promise<void> {
		try {
			const { roomId, userId, username, token } = data;

			// 验证房间访问权限（此时房间应该已经在 Spring Cloud 端创建）
			const validation = await this.grpcClient.validateRoomAccess(roomId, userId, token);
			if (!validation.allowed) {
				callback({ error: "Access denied: " + validation.message });
				return;
			}

			// 获取或创建房间
			let room = this.roomManager.getRoom(roomId);

			if (!room) {
				room = await this.roomManager.createRoomInternal(roomId);
				this.logger.info(`SFU Room ${roomId} created for first participant`);
			}

			// 检查用户是否已在房间中（处理重连）
			const existingPeer = room.getPeer(userId);
			if (existingPeer) {
				// 用户已在房间中，关闭旧连接
				existingPeer.socket.disconnect();
				room.removePeer(userId);
			}

			// 创建 Peer
			const peer = new Peer(
				{
					id: userId,
					userId,
					username,
					roomId,
				},
				socket
			);

			// 添加到房间
			room.addPeer(peer);
			// 映射 socket.id 到 peer 信息
			this.socketToPeerMap.set(socket.id, { roomId, userId });

			// 加入 Socket.io 房间
			socket.join(roomId);

			// 通知其他参与者
			socket.to(roomId).emit("newPeer", {
				peerId: peer.id,
				userId: peer.userId,
				username: peer.username,
			});

			// 通知 Spring Cloud
			await this.grpcClient.notifyParticipantJoined(roomId, userId, username);

			// 返回房间中的其他参与者
			const otherPeers = room.getPeersExcept(peer.id).map((p: Peer) => ({
				peerId: p.id,
				userId: p.userId,
				username: p.username,
			}));

			callback({
				peerId: peer.id,
				peers: otherPeers,
			});

			this.logger.info(`User ${username} (${userId}) joined room ${roomId}`);
		} catch (error: any) {
			this.logger.error("Error in joinRoom", error);
			callback({ error: error.message });
		}
	}

	private async handleGetRouterRtpCapabilities(
		socket: Socket,
		data: { roomId: string },
		callback: Function
	): Promise<void> {
		try {
			const { roomId } = data;
			const room = this.roomManager.getRoom(roomId);

			if (!room) {
				callback({ error: "Room not found" });
				return;
			}

			const rtpCapabilities = room.router.rtpCapabilities;
			callback({ rtpCapabilities });
		} catch (error: any) {
			this.logger.error("Error in getRouterRtpCapabilities", error);
			callback({ error: error.message });
		}
	}

	private async handleCreateWebRtcTransport(
		socket: Socket,
		data: { roomId: string; producing: boolean; consuming: boolean },
		callback: Function
	): Promise<void> {
		try {
			const { roomId, producing, consuming } = data;
			const room = this.roomManager.getRoom(roomId);

			const peerInfo = this.socketToPeerMap.get(socket.id);
			if (!peerInfo) {
				callback({ error: "Peer info not found" });
				return;
			}

			const peer = room?.getPeer(peerInfo.userId);

			if (!room || !peer) {
				callback({ error: "Room or peer not found" });
				return;
			}

			const transport = await room.router.createWebRtcTransport({
				...config.mediasoup.webRtcTransportOptions,
				appData: { producing, consuming },
			});

			if (producing) {
				peer.sendTransport = transport;
			}
			if (consuming) {
				peer.recvTransport = transport;
			}

			callback({
				id: transport.id,
				iceParameters: transport.iceParameters,
				iceCandidates: transport.iceCandidates,
				dtlsParameters: transport.dtlsParameters,
			});

			this.logger.info(`WebRTC transport created for peer ${peer.id}`);
		} catch (error: any) {
			this.logger.error("Error in createWebRtcTransport", error);
			callback({ error: error.message });
		}
	}

	private async handleConnectWebRtcTransport(
		socket: Socket,
		data: {
			roomId: string;
			transportId: string;
			dtlsParameters: mediasoupTypes.DtlsParameters;
		},
		callback: Function
	): Promise<void> {
		try {
			const { roomId, transportId, dtlsParameters } = data;
			const room = this.roomManager.getRoom(roomId);
			const peer = room?.getPeer(socket.id);

			if (!room || !peer) {
				callback({ error: "Room or peer not found" });
				return;
			}

			const transport =
				peer.sendTransport?.id === transportId ? peer.sendTransport : peer.recvTransport;

			if (!transport) {
				callback({ error: "Transport not found" });
				return;
			}

			await transport.connect({ dtlsParameters });
			callback({ success: true });

			this.logger.info(`Transport ${transportId} connected for peer ${peer.id}`);
		} catch (error: any) {
			this.logger.error("Error in connectWebRtcTransport", error);
			callback({ error: error.message });
		}
	}

	private async handleProduce(
		socket: Socket,
		data: {
			roomId: string;
			transportId: string;
			kind: mediasoupTypes.MediaKind;
			rtpParameters: mediasoupTypes.RtpParameters;
			appData?: any;
		},
		callback: Function
	): Promise<void> {
		try {
			const { roomId, transportId, kind, rtpParameters, appData } = data;
			const room = this.roomManager.getRoom(roomId);

			const peerInfo = this.socketToPeerMap.get(socket.id);
			if (!peerInfo) {
				callback({ error: "Peer info not found" });
				return;
			}
			const peer = room?.getPeer(peerInfo.userId);

			if (!room || !peer || !peer.sendTransport) {
				callback({ error: "Room, peer or transport not found" });
				return;
			}

			const producer = await peer.sendTransport.produce({
				kind,
				rtpParameters,
				appData: { ...appData, peerId: peer.id, username: peer.username },
			});

			peer.addProducer(producer);

			// 通知房间内其他参与者
			socket.to(roomId).emit("newProducer", {
				producerId: producer.id,
				peerId: peer.id,
				userId: peer.userId,
				username: peer.username,
				kind: producer.kind,
			});

			callback({ id: producer.id });

			this.logger.info(`Producer ${producer.id} created for peer ${peer.id} (${kind})`);
		} catch (error: any) {
			this.logger.error("Error in produce", error);
			callback({ error: error.message });
		}
	}

	private async handleConsume(
		socket: Socket,
		data: {
			roomId: string;
			producerId: string;
			rtpCapabilities: mediasoupTypes.RtpCapabilities;
		},
		callback: Function
	): Promise<void> {
		try {
			const { roomId, producerId, rtpCapabilities } = data;
			const room = this.roomManager.getRoom(roomId);

			const peerInfo = this.socketToPeerMap.get(socket.id);
			if (!peerInfo) {
				callback({ error: "Peer info not found" });
				return;
			}

			const peer = room?.getPeer(peerInfo.userId);

			if (!room || !peer || !peer.recvTransport) {
				callback({ error: "Room, peer or transport not found" });
				return;
			}

			// 检查是否可以消费
			if (!room.router.canConsume({ producerId, rtpCapabilities })) {
				callback({ error: "Cannot consume" });
				return;
			}

			const consumer = await peer.recvTransport.consume({
				producerId,
				rtpCapabilities,
				paused: true, // 初始暂停，等待客户端准备好
			});

			peer.addConsumer(consumer);

			callback({
				id: consumer.id,
				producerId: consumer.producerId,
				kind: consumer.kind,
				rtpParameters: consumer.rtpParameters,
			});

			this.logger.info(`Consumer ${consumer.id} created for peer ${peer.id}`);
		} catch (error: any) {
			this.logger.error("Error in consume", error);
			callback({ error: error.message });
		}
	}

	private async handleResumeConsumer(
		socket: Socket,
		data: { roomId: string; consumerId: string },
		callback: Function
	): Promise<void> {
		try {
			const { roomId, consumerId } = data;
			const room = this.roomManager.getRoom(roomId);
			const peer = room?.getPeer(socket.id);

			if (!room || !peer) {
				callback({ error: "Room or peer not found" });
				return;
			}

			const consumer = peer.consumers.get(consumerId);
			if (!consumer) {
				callback({ error: "Consumer not found" });
				return;
			}

			await consumer.resume();
			callback({ success: true });

			this.logger.info(`Consumer ${consumerId} resumed for peer ${peer.id}`);
		} catch (error: any) {
			this.logger.error("Error in resumeConsumer", error);
			callback({ error: error.message });
		}
	}

	private async handlePauseProducer(
		socket: Socket,
		data: { roomId: string; producerId: string },
		callback: Function
	): Promise<void> {
		try {
			const { roomId, producerId } = data;
			const room = this.roomManager.getRoom(roomId);
			const peer = room?.getPeer(socket.id);

			if (!room || !peer) {
				callback({ error: "Room or peer not found" });
				return;
			}

			const producer = peer.producers.get(producerId);
			if (!producer) {
				callback({ error: "Producer not found" });
				return;
			}

			await producer.pause();

			// 通知其他参与者
			socket.to(roomId).emit("producerPaused", { producerId, peerId: peer.id });

			callback({ success: true });
			this.logger.info(`Producer ${producerId} paused for peer ${peer.id}`);
		} catch (error: any) {
			this.logger.error("Error in pauseProducer", error);
			callback({ error: error.message });
		}
	}

	private async handleResumeProducer(
		socket: Socket,
		data: { roomId: string; producerId: string },
		callback: Function
	): Promise<void> {
		try {
			const { roomId, producerId } = data;
			const room = this.roomManager.getRoom(roomId);
			const peer = room?.getPeer(socket.id);

			if (!room || !peer) {
				callback({ error: "Room or peer not found" });
				return;
			}

			const producer = peer.producers.get(producerId);
			if (!producer) {
				callback({ error: "Producer not found" });
				return;
			}

			await producer.resume();

			// 通知其他参与者
			socket.to(roomId).emit("producerResumed", { producerId, peerId: peer.id });

			callback({ success: true });
			this.logger.info(`Producer ${producerId} resumed for peer ${peer.id}`);
		} catch (error: any) {
			this.logger.error("Error in resumeProducer", error);
			callback({ error: error.message });
		}
	}

	private async handleCloseProducer(
		socket: Socket,
		data: { roomId: string; producerId: string },
		callback: Function
	): Promise<void> {
		try {
			const { roomId, producerId } = data;
			const room = this.roomManager.getRoom(roomId);
			const peer = room?.getPeer(socket.id);

			if (!room || !peer) {
				callback({ error: "Room or peer not found" });
				return;
			}

			const producer = peer.producers.get(producerId);
			if (!producer) {
				callback({ error: "Producer not found" });
				return;
			}

			producer.close();
			peer.removeProducer(producerId);

			// 通知其他参与者
			socket.to(roomId).emit("producerClosed", { producerId, peerId: peer.id });

			callback({ success: true });
			this.logger.info(`Producer ${producerId} closed for peer ${peer.id}`);
		} catch (error: any) {
			this.logger.error("Error in closeProducer", error);
			callback({ error: error.message });
		}
	}

	private async handleLeaveRoom(socket: Socket): Promise<void> {
		try {
			const peerInfo = this.socketToPeerMap.get(socket.id);
			if (!peerInfo) {
				return;
			}

			const { roomId, userId } = peerInfo;
			const room = this.roomManager.getRoom(roomId);
			const peer = room?.getPeer(userId);

			if (room && peer) {
				// 通知其他参与者
				socket.to(roomId).emit("peerLeft", {
					peerId: peer.id,
					userId: peer.userId,
					username: peer.username,
				});

				// 通知 Spring Cloud
				await this.grpcClient.notifyParticipantLeft(roomId, peer.userId, peer.username);

				// 从房间移除
				this.roomManager.removePeerFromRoom(roomId, peer.id);

				// 离开 Socket.io 房间
				socket.leave(roomId);

				this.logger.info(`Peer ${peer.id} (${peer.username}) left room ${roomId}`);
			}
			this.socketToPeerMap.delete(socket.id);
		} catch (error) {
			this.logger.error("Error in leaveRoom", error);
		}
	}

	private async handleDisconnect(socket: Socket): Promise<void> {
		await this.handleLeaveRoom(socket);
		this.logger.info(`Client disconnected: ${socket.id}`);
	}
}
