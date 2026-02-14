import type * as mediasoupTypes from "mediasoup/node/lib/types"
import { Logger } from "../utils/logger"

interface TransportInfo {
	id: string
	direction: "send" | "recv"
	peerId: string
	createdAt: number
	connected: boolean
	iceState?: string
	dtlsState?: string
}

export class TransportManager {
	private logger = new Logger("TransportManager")
	private transports: Map<string, TransportInfo> = new Map()

	registerTransport(
		transport: mediasoupTypes.WebRtcTransport,
		direction: "send" | "recv",
		peerId: string
	): void {
		const info: TransportInfo = {
			id: transport.id,
			direction,
			peerId,
			createdAt: Date.now(),
			connected: false,
		}

		this.transports.set(transport.id, info)

		// 监听状态变化
		transport.on("icestatechange", (iceState) => {
			info.iceState = iceState
			this.logger.debug(`Transport ${transport.id} ICE state: ${iceState}`)
		})

		transport.on("dtlsstatechange", (dtlsState) => {
			info.dtlsState = dtlsState
			if (dtlsState === "connected") {
				info.connected = true
			}
			this.logger.debug(`Transport ${transport.id} DTLS state: ${dtlsState}`)
		})

		transport.on("sctpstatechange", (sctpState) => {
			this.logger.debug(`Transport ${transport.id} SCTP state: ${sctpState}`)
		})

		this.logger.info(`Transport ${transport.id} registered for peer ${peerId} (${direction})`)
	}

	async connectTransport(
		transport: mediasoupTypes.WebRtcTransport,
		dtlsParameters: mediasoupTypes.DtlsParameters
	): Promise<void> {
		try {
			await transport.connect({ dtlsParameters })
			const info = this.transports.get(transport.id)
			if (info) {
				info.connected = true
			}
			this.logger.info(`Transport ${transport.id} connected`)
		} catch (error) {
			this.logger.error(`Failed to connect transport ${transport.id}`, error)
			throw error
		}
	}

	getTransportInfo(transportId: string): TransportInfo | undefined {
		return this.transports.get(transportId)
	}

	removeTransport(transportId: string): void {
		this.transports.delete(transportId)
		this.logger.info(`Transport ${transportId} removed`)
	}

	getTransportsByPeer(peerId: string): TransportInfo[] {
		return Array.from(this.transports.values()).filter((t) => t.peerId === peerId)
	}
}
