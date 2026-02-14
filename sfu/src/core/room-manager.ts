import type * as mediasoupTypes from "mediasoup/node/lib/types"
import { Peer } from "./peer"
import { Room } from "../core/room"
import { MediasoupManager } from "./mediasoup-manager"
import { Logger } from "../utils/logger"

export class RoomManager {
	private static instance: RoomManager
	private rooms: Map<string, Room> = new Map()
	private logger = new Logger("RoomManager")

	private constructor() {}

	public static getInstance(): RoomManager {
		if (!RoomManager.instance) {
			RoomManager.instance = new RoomManager()
		}
		return RoomManager.instance
	}
	// 必须在先通过 Spring Cloud 验证/创建房间后才能调用此方法
	public async createRoomInternal(roomId: string): Promise<Room> {
		if (this.rooms.has(roomId)) {
			throw new Error(`Room ${roomId} already exists`)
		}

		const mediasoupManager = MediasoupManager.getInstance()
		const router = await mediasoupManager.createRouter(roomId)
		const room = new Room(roomId, router)
		this.rooms.set(roomId, room)
		this.logger.info(`Room ${roomId} created internally`)

		return room
	}

	public getRoom(roomId: string): Room | undefined {
		return this.rooms.get(roomId)
	}
	// 获取房间内所有的 媒体Producers
	public getRoomProducers(roomId: string): mediasoupTypes.Producer[] {
		const room = this.rooms.get(roomId)
		if (!room) {
			this.logger.warn(`Room ${roomId} not found when getting producers`)
			return []
		}
		return room.getAllProducers()
	}
	// 获取房间内特定类型的 Producers
	public getRoomProducersByKind(roomId: string, kind: "video" | "audio"): mediasoupTypes.Producer[] {
		const room = this.rooms.get(roomId)
		if (!room) {
			return []
		}
		return room.getProducersByKind(kind)
	}

	// 获取房间内 Producers 的详细信息（用于日志和调试）
	public getRoomProducersInfo(roomId: string): Array<any> {
		const room = this.rooms.get(roomId)
		if (!room) {
			return []
		}
		return room.getProducersInfo()
	}

	public removeRoom(roomId: string): void {
		const room = this.rooms.get(roomId)
		if (room) {
			room.close()
			this.rooms.delete(roomId)
			this.logger.info(`Room ${roomId} removed`)
		}
	}

	public addPeerToRoom(roomId: string, peer: Peer): void {
		const room = this.rooms.get(roomId)
		if (room) {
			room.addPeer(peer)
		}
	}

	public removePeerFromRoom(roomId: string, peerId: string): void {
		const room = this.rooms.get(roomId)
		if (room) {
			room.removePeer(peerId) // peerId 是 userId

			// 如果房间为空，关闭房间
			if (room.isEmpty()) {
				this.removeRoom(roomId)
			}
		}
	}

	public getAllRooms(): Room[] {
		return Array.from(this.rooms.values())
	}

	public getRoomStats(): any {
		const stats = {
			totalRooms: this.rooms.size,
			rooms: [] as any[],
		}

		for (const room of this.rooms.values()) {
			stats.rooms.push({
				id: room.id,
				participants: room.peers.size,
				peers: Array.from(room.peers.values()).map((p: Peer) => ({
					id: p.id,
					userId: p.userId,
					username: p.username,
					producers: p.producers.size,
					consumers: p.consumers.size,
				})),
			})
		}

		return stats
	}
}
