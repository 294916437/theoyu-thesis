import * as grpc from "@grpc/grpc-js"
import * as protoLoader from "@grpc/proto-loader"
import path from "path"
import config from "../config/config"
import { Logger } from "./logger"

interface RoomConfig {
	max_participants: number
	enable_recording: boolean
	allowed_codecs: string[]
}

interface RoomAccessRequest {
	room_id: string
	user_id: string
	token: string
}

interface RoomAccessResponse {
	allowed: boolean
	message: string
	config?: RoomConfig
}

interface TokenRequest {
	token: string
}

interface TokenResponse {
	valid: boolean
	user_id: string
	username: string
}

interface ParticipantEvent {
	room_id: string
	user_id: string
	username: string
	timestamp: number
}

interface AckResponse {
	success: boolean
	message: string
}

interface MediaStatsRequest {
	room_id: string
	peer_id: string
	stats: { [key: string]: string }
}

export class GrpcClient {
	private static instance: GrpcClient
	private client: any
	private logger = new Logger("GrpcClient")
	private connected = false

	private constructor() {}

	public static getInstance(): GrpcClient {
		if (!GrpcClient.instance) {
			GrpcClient.instance = new GrpcClient()
		}
		return GrpcClient.instance
	}

	public async init(): Promise<void> {
		try {
			const PROTO_PATH = path.join(__dirname, "../../proto/sfu-callback.proto")

			// 检查文件是否存在
			const fs = require("fs")
			if (!fs.existsSync(PROTO_PATH)) {
				throw new Error(`Proto file not found: ${PROTO_PATH}`)
			}

			const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
				keepCase: true,
				longs: String,
				enums: String,
				defaults: true,
				oneofs: true,
			})

			const protoDescriptor = grpc.loadPackageDefinition(packageDefinition) as any

			// 检查包是否正确加载
			if (!protoDescriptor.sfu || !protoDescriptor.sfu.callback || !protoDescriptor.sfu.callback.SFUCallbackService) {
				throw new Error("Failed to load SFUService from proto file")
			}

			const sfuService = protoDescriptor.sfu.SFUService

			this.client = new sfuService(`${config.grpc.host}:${config.grpc.port}`, grpc.credentials.createInsecure(), {
				"grpc.keepalive_time_ms": 30000,
				"grpc.keepalive_timeout_ms": 10000,
				"grpc.keepalive_permit_without_calls": 1,
				"grpc.http2.max_pings_without_data": 0,
				"grpc.http2.min_time_between_pings_ms": 10000,
				"grpc.http2.min_ping_interval_without_data_ms": 30000,
			})

			this.connected = true
			this.logger.info(`gRPC client initialized: ${config.grpc.host}:${config.grpc.port}`)
		} catch (error) {
			this.logger.error("Failed to initialize gRPC client", error)
			this.connected = false
			throw error
		}
	}

	private ensureConnected(): void {
		if (!this.connected || !this.client) {
			throw new Error("gRPC client not initialized or disconnected")
		}
	}

	public async validateRoomAccess(roomId: string, userId: string, token: string): Promise<RoomAccessResponse> {
		this.ensureConnected()

		return new Promise((resolve, reject) => {
			const request: RoomAccessRequest = {
				room_id: roomId,
				user_id: userId,
				token: token,
			}

			const deadline = new Date()
			deadline.setSeconds(deadline.getSeconds() + 5)

			this.client.ValidateRoomAccess(request, { deadline }, (error: grpc.ServiceError | null, response: RoomAccessResponse) => {
				if (error) {
					this.logger.error("ValidateRoomAccess failed", error)
					// 返回默认拒绝响应而不是抛出错误
					resolve({
						allowed: false,
						message: `gRPC error: ${error.message}`,
					})
				} else {
					resolve(response)
				}
			})
		})
	}

	public async validateUserToken(token: string): Promise<TokenResponse> {
		this.ensureConnected()

		return new Promise((resolve, reject) => {
			const request: TokenRequest = { token }

			const deadline = new Date()
			deadline.setSeconds(deadline.getSeconds() + 5)

			this.client.ValidateUserToken(request, { deadline }, (error: grpc.ServiceError | null, response: TokenResponse) => {
				if (error) {
					this.logger.error("ValidateUserToken failed", error)
					resolve({
						valid: false,
						user_id: "",
						username: "",
					})
				} else {
					resolve(response)
				}
			})
		})
	}

	public async notifyParticipantJoined(roomId: string, userId: string, username: string): Promise<AckResponse> {
		this.ensureConnected()

		return new Promise((resolve, reject) => {
			const request: ParticipantEvent = {
				room_id: roomId,
				user_id: userId,
				username: username,
				timestamp: Date.now(),
			}

			const deadline = new Date()
			deadline.setSeconds(deadline.getSeconds() + 5)

			this.client.NotifyParticipantJoined(request, { deadline }, (error: grpc.ServiceError | null, response: AckResponse) => {
				if (error) {
					this.logger.warn("NotifyParticipantJoined failed (non-critical)", error.message)
					resolve({ success: false, message: error.message })
				} else {
					resolve(response)
				}
			})
		})
	}

	public async notifyParticipantLeft(roomId: string, userId: string, username: string): Promise<AckResponse> {
		this.ensureConnected()

		return new Promise((resolve, reject) => {
			const request: ParticipantEvent = {
				room_id: roomId,
				user_id: userId,
				username: username,
				timestamp: Date.now(),
			}

			const deadline = new Date()
			deadline.setSeconds(deadline.getSeconds() + 5)

			this.client.NotifyParticipantLeft(request, { deadline }, (error: grpc.ServiceError | null, response: AckResponse) => {
				if (error) {
					this.logger.warn("NotifyParticipantLeft failed (non-critical)", error.message)
					resolve({ success: false, message: error.message })
				} else {
					resolve(response)
				}
			})
		})
	}

	public async reportMediaStats(roomId: string, peerId: string, stats: { [key: string]: string }): Promise<AckResponse> {
		this.ensureConnected()

		return new Promise((resolve, reject) => {
			const request: MediaStatsRequest = {
				room_id: roomId,
				peer_id: peerId,
				stats: stats,
			}

			const deadline = new Date()
			deadline.setSeconds(deadline.getSeconds() + 5)

			this.client.ReportMediaStats(request, { deadline }, (error: grpc.ServiceError | null, response: AckResponse) => {
				if (error) {
					this.logger.warn("ReportMediaStats failed (non-critical)", error.message)
					resolve({ success: false, message: error.message })
				} else {
					resolve(response)
				}
			})
		})
	}

	public isConnected(): boolean {
		return this.connected
	}

	public close(): void {
		if (this.client) {
			grpc.closeClient(this.client)
			this.connected = false
			this.logger.info("gRPC client closed")
		}
	}
}
