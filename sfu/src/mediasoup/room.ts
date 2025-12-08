import type * as mediasoupTypes from "mediasoup/node/lib/types";
import { Peer } from "../core/peer";
import { Logger } from "../utils/logger";

export class Room {
	public id: string;
	public router: mediasoupTypes.Router;
	public peers: Map<string, Peer> = new Map();
	private logger: Logger;

	constructor(id: string, router: mediasoupTypes.Router) {
		this.id = id;
		this.router = router;
		this.logger = new Logger(`Room:${id}`);
	}

	public addPeer(peer: Peer): void {
		this.peers.set(peer.id, peer);
		this.logger.info(
			`Peer ${peer.id} (userId: ${peer.userId}, username: ${peer.username}) joined. Total peers: ${this.peers.size}`
		);
	}

	public removePeer(peerId: string): Peer | undefined {
		const peer = this.peers.get(peerId);
		if (peer) {
			peer.close();
			this.peers.delete(peerId);
			this.logger.info(
				`Peer ${peerId} (${peer.username}) left. Total peers: ${this.peers.size}`
			);
		}
		return peer;
	}

	public getPeer(peerId: string): Peer | undefined {
		return this.peers.get(peerId);
	}

	public getAllPeers(): Peer[] {
		return Array.from(this.peers.values());
	}

	public getPeersExcept(peerId: string): Peer[] {
		return this.getAllPeers().filter((peer) => peer.id !== peerId);
	}

	public isEmpty(): boolean {
		return this.peers.size === 0;
	}

	public close(): void {
		this.logger.info(`Closing room ${this.id}`);

		// Close all peers
		for (const peer of this.peers.values()) {
			peer.close();
		}
		this.peers.clear();

		// Close router
		this.router.close();
	}
}
