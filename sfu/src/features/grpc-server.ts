import * as grpc from "@grpc/grpc-js"
import * as protoLoader from "@grpc/proto-loader"
import path from "path"
import { Logger } from "../utils/logger"
import config from "../config/config"
import { RoomManager } from "../core/room-manager"
import { RecordingManager } from "../features/recording-manager"
import { MediasoupManager } from "../core/mediasoup-manager"

export class GrpcServer {
	private static instance: GrpcServer
	private server: grpc.Server
	private logger = new Logger("GrpcServer")
	private recordingManager = RecordingManager.getInstance()
	private mediasoupManager = MediasoupManager.getInstance()
	private roomManager = RoomManager.getInstance()

	private constructor() {
		this.server = new grpc.Server()
	}

	public static getInstance(): GrpcServer {
		if (!GrpcServer.instance) {
			GrpcServer.instance = new GrpcServer()
		}
		return GrpcServer.instance
	}

	public async init(): Promise<void> {
		const PROTO_PATH = path.join(__dirname, "../../proto/sfu-control.proto")

		const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
			keepCase: true,
			longs: String,
			enums: String,
			defaults: true,
			oneofs: true,
		})

		const protoDescriptor = grpc.loadPackageDefinition(packageDefinition) as any
		// 根据 proto package 定义: sfu.control
		const sfuService = protoDescriptor.sfu.control.SFUControlService

		this.server.addService(sfuService.service, {
			StartRecording: this.handleStartRecording.bind(this),
			StopRecording: this.handleStopRecording.bind(this),
			GetRecordingStatus: this.handleGetRecordingStatus.bind(this),
		})

		const bindAddress = `0.0.0.0:${config.grpc.serverPort || 50051}`

		await new Promise<void>((resolve, reject) => {
			this.server.bindAsync(bindAddress, grpc.ServerCredentials.createInsecure(), (error, port) => {
				if (error) {
					reject(error)
				} else {
					this.server.start()
					this.logger.info(`gRPC server listening on ${bindAddress}`)
					resolve()
				}
			})
		})
	}

	private async handleStartRecording(call: grpc.ServerUnaryCall<any, any>, callback: grpc.sendUnaryData<any>): Promise<void> {
		try {
			// Proto 变更: recording_id -> host_id
			const { room_id, host_id, config: recordingConfig } = call.request

			this.logger.info(`StartRecording request: room=${room_id}, host=${host_id}`)

			const router = this.mediasoupManager.getRouter(room_id)
			if (!router) {
				callback(null, {
					success: false,
					message: `Room ${room_id} not found`,
					host_id: host_id,
				})
				return
			}

			const producers = this.roomManager.getRoomProducers(room_id)

			// 验证是否有可录制的内容
			if (producers.length === 0) {
				this.logger.warn(`No producers found in room ${room_id}, cannot start recording`)
				callback(null, {
					success: false,
					message: `No active media streams in room ${room_id}`,
					host_id: host_id,
				})
				return
			}

			// 记录 Producers 信息（用于调试）
			const producersInfo = this.roomManager.getRoomProducersInfo(room_id)
			this.logger.info(`Found ${producers.length} producers in room ${room_id}:`, JSON.stringify(producersInfo, null, 2))

			const config = {
				videoWidth: recordingConfig?.video_width || 1280,
				videoHeight: recordingConfig?.video_height || 720,
				videoBitrate: recordingConfig?.video_bitrate || 3000,
				videoFramerate: recordingConfig?.video_framerate || 30,
				videoCodec: recordingConfig?.video_codec || "h264",
				audioBitrate: recordingConfig?.audio_bitrate || 128,
				audioCodec: recordingConfig?.audio_codec || "aac",
				format: recordingConfig?.format || "mp4",
			}

			// 传递 recordingId (即 host_id) 给 Manager
			await this.recordingManager.startRecording(room_id, host_id, router, producers, config)

			callback(null, {
				success: true,
				message: "Recording started successfully",
				host_id: host_id,
			})
		} catch (error: any) {
			this.logger.error("StartRecording error", error)
			callback(null, {
				success: false,
				message: error.message,
				host_id: call.request.host_id,
			})
		}
	}

	private async handleStopRecording(call: grpc.ServerUnaryCall<any, any>, callback: grpc.sendUnaryData<any>): Promise<void> {
		try {
			const { room_id, host_id } = call.request
			// 联合主键
			const recordingId = `${room_id}_${host_id}`

			this.logger.info(`StopRecording request: room=${room_id}, host=${host_id}`)

			const fileUrl = await this.recordingManager.stopRecording(recordingId)

			callback(null, {
				success: true,
				message: "Recording stopped successfully",
				file_url: fileUrl,
			})
		} catch (error: any) {
			this.logger.error("StopRecording error", error)
			callback(null, {
				success: false,
				message: error.message,
				file_url: null,
			})
		}
	}

	private async handleGetRecordingStatus(call: grpc.ServerUnaryCall<any, any>, callback: grpc.sendUnaryData<any>): Promise<void> {
		try {
			const { room_id, host_id } = call.request
			// 联合主键
			const recordingId = `${room_id}_${host_id}`

			const status = this.recordingManager.getRecordingStatus(recordingId)

			if (!status) {
				callback(null, {
					is_recording: false,
					duration_seconds: 0,
					file_size_bytes: 0,
				})
				return
			}

			callback(null, {
				is_recording: status.isRecording,
				duration_seconds: status.durationSeconds,
				file_size_bytes: status.fileSizeBytes,
			})
		} catch (error: any) {
			this.logger.error("GetRecordingStatus error", error)
			callback(null, {
				is_recording: false,
				duration_seconds: 0,
				file_size_bytes: 0,
			})
		}
	}

	public async close(): Promise<void> {
		return new Promise<void>((resolve) => {
			this.server.tryShutdown(() => {
				this.logger.info("gRPC server closed")
				resolve()
			})
		})
	}
}
