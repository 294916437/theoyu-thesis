import { Peer } from "./peer";
import { Room } from "../mediasoup/room";
import { MediasoupManager } from "../mediasoup/mediasoup-manager";
import { Logger } from "../utils/logger";

export class RoomManager {
	private static instance: RoomManager;
	private rooms: Map<string, Room> = new Map();
	private logger = new Logger("RoomManager");

	private constructor() {}

	public static getInstance(): RoomManager {
		if (!RoomManager.instance) {
			RoomManager.instance = new RoomManager();
		}
		return RoomManager.instance;
	}

	public async getOrCreateRoom(roomId: string): Promise<Room> {
		let room = this.rooms.get(roomId);

		if (!room) {
			const mediasoupManager = MediasoupManager.getInstance();
			const router = await mediasoupManager.createRouter(roomId);
			room = new Room(roomId, router);
			this.rooms.set(roomId, room);
			this.logger.info(`Room ${roomId} created`);
		}

		return room;
	}

	public getRoom(roomId: string): Room | undefined {
		return this.rooms.get(roomId);
	}

	public removeRoom(roomId: string): void {
		const room = this.rooms.get(roomId);
		if (room) {
			room.close();
			this.rooms.delete(roomId);
			this.logger.info(`Room ${roomId} removed`);
		}
	}

	public addPeerToRoom(roomId: string, peer: Peer): void {
		const room = this.rooms.get(roomId);
		if (room) {
			room.addPeer(peer);
		}
	}

	public removePeerFromRoom(roomId: string, peerId: string): void {
		const room = this.rooms.get(roomId);
		if (room) {
			room.removePeer(peerId);

			// 如果房间为空，关闭房间
			if (room.isEmpty()) {
				this.removeRoom(roomId);
			}
		}
	}

	public getAllRooms(): Room[] {
		return Array.from(this.rooms.values());
	}

	public getRoomStats(): any {
		const stats = {
			totalRooms: this.rooms.size,
			rooms: [] as any[],
		};

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
			});
		}

		return stats;
	}
}
