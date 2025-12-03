import type * as mediasoupTypes from "mediasoup/node/lib/types";
import { Socket } from "socket.io";

export interface PeerInfo {
	id: string;
	userId: string;
	username: string;
	roomId: string;
}

export class Peer {
	public id: string;
	public userId: string;
	public username: string;
	public roomId: string;
	public socket: Socket;
	public sendTransport: mediasoupTypes.Transport | null = null;
	public recvTransport: mediasoupTypes.Transport | null = null;
	public producers: Map<string, mediasoupTypes.Producer> = new Map();
	public consumers: Map<string, mediasoupTypes.Consumer> = new Map();

	constructor(info: PeerInfo, socket: Socket) {
		this.id = info.id;
		this.userId = info.userId;
		this.username = info.username;
		this.roomId = info.roomId;
		this.socket = socket;
	}

	public addProducer(producer: mediasoupTypes.Producer): void {
		this.producers.set(producer.id, producer);
	}

	public removeProducer(producerId: string): void {
		this.producers.delete(producerId);
	}

	public addConsumer(consumer: mediasoupTypes.Consumer): void {
		this.consumers.set(consumer.id, consumer);
	}

	public removeConsumer(consumerId: string): void {
		this.consumers.delete(consumerId);
	}

	public close(): void {
		// Close all transports
		if (this.sendTransport) {
			this.sendTransport.close();
			this.sendTransport = null;
		}

		if (this.recvTransport) {
			this.recvTransport.close();
			this.recvTransport = null;
		}

		// Clear producers and consumers
		this.producers.clear();
		this.consumers.clear();
	}

	public getInfo(): PeerInfo {
		return {
			id: this.id,
			userId: this.userId,
			username: this.username,
			roomId: this.roomId,
		};
	}
}
