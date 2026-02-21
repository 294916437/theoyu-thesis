import { spawn, ChildProcess } from "child_process"
import * as fs from "fs"
import * as path from "path"
import type * as mediasoupTypes from "mediasoup/node/lib/types"
import { Logger } from "../utils/logger"
import { MinioClient } from "../utils/minio-client"
import config from "../config/config"

interface RecordingConfig {
	videoWidth: number
	videoHeight: number
	videoBitrate: number
	videoFramerate: number
	videoCodec: string
	audioBitrate: number
	audioCodec: string
	format: string
}

// 上传完成后的结果
interface RecordingUploadResult {
	fileUrl: string
}

interface RecordingSession {
	recordingId: string
	roomId: string
	transports: mediasoupTypes.PlainTransport[]
	consumers: mediasoupTypes.Consumer[]
	ffmpegProcess: ChildProcess
	outputPath: string
	startTime: number
	recordConfig: RecordingConfig
	uploadCompletionPromise?: Promise<RecordingUploadResult>
	_resolveUpload?: (result: RecordingUploadResult) => void
	_rejectUpload?: (error: Error) => void
}

export class RecordingManager {
	private static instance: RecordingManager
	private sessions: Map<string, RecordingSession> = new Map()
	private logger = new Logger("RecordingManager")
	private recordingsDir: string
	private minioClient: MinioClient

	private constructor() {
		this.recordingsDir = path.join(__dirname, "../../recordings")
		if (!fs.existsSync(this.recordingsDir)) {
			fs.mkdirSync(this.recordingsDir, { recursive: true })
		}
		this.minioClient = MinioClient.getInstance()
	}

	public static getInstance(): RecordingManager {
		if (!RecordingManager.instance) {
			RecordingManager.instance = new RecordingManager()
		}
		return RecordingManager.instance
	}

	public async startRecording(roomId: string, hostId: string, router: mediasoupTypes.Router, producers: mediasoupTypes.Producer[], recordConfig: RecordingConfig): Promise<void> {
		const recordingId = `${roomId}-${hostId}`

		if (this.sessions.has(recordingId)) {
			throw new Error(`Recording ${recordingId} already exists`)
		}

		this.logger.info(`Starting recording ${recordingId} for room ${roomId}`)

		const consumers: mediasoupTypes.Consumer[] = []
		const transports: mediasoupTypes.PlainTransport[] = []
		const sdpLines: string[] = []

		sdpLines.push("v=0")
		sdpLines.push("o=- 0 0 IN IP4 127.0.0.1")
		sdpLines.push("s=Mediasoup Recording")
		sdpLines.push("c=IN IP4 127.0.0.1")
		sdpLines.push("t=0 0")

		for (const producer of producers) {
			try {
				// 每个 producer 使用独立的 transport，避免端口冲突
				const transport = await router.createPlainTransport({
					listenIp: { ip: "127.0.0.1", announcedIp: undefined },
					rtcpMux: true,
					comedia: true,
				})
				transports.push(transport)

				this.logger.info(`PlainTransport created for ${producer.kind}: ${transport.tuple.localIp}:${transport.tuple.localPort}`)

				const consumer = await transport.consume({
					producerId: producer.id,
					rtpCapabilities: router.rtpCapabilities,
					paused: false,
				})

				consumers.push(consumer)

				if (consumer.kind === "video") {
					await consumer.requestKeyFrame()
					this.logger.info(`Requested key frame for consumer ${consumer.id}`)
				}

				const codecPayloadType = consumer.rtpParameters.codecs[0].payloadType
				const codecName = consumer.rtpParameters.codecs[0].mimeType.split("/")[1]
				const clockRate = consumer.rtpParameters.codecs[0].clockRate
				const ssrc = consumer.rtpParameters.encodings?.[0]?.ssrc

				// 每个 media line 使用各自 transport 的独立端口
				sdpLines.push(`m=${consumer.kind} ${transport.tuple.localPort} RTP/AVP ${codecPayloadType}`)
				sdpLines.push(`c=IN IP4 127.0.0.1`)
				sdpLines.push(`a=rtpmap:${codecPayloadType} ${codecName}/${clockRate}`)

				if (consumer.kind === "audio" && codecName.toLowerCase() === "opus") {
					sdpLines.push(`a=rtpmap:${codecPayloadType} ${codecName}/${clockRate}/2`)
				}

				if (consumer.kind === "video") {
					await consumer.requestKeyFrame()
					// 从 rtpParameters 获取视频宽高
					const encoding = consumer.rtpParameters.encodings?.[0] as any
					const width = encoding?.scalabilityMode ? recordConfig.videoWidth : recordConfig.videoWidth
					const height = recordConfig.videoHeight
					// VP8 不需要 packetization-mode，H264 才需要
					if (codecName.toLowerCase() === "h264") {
						sdpLines.push(`a=fmtp:${codecPayloadType} packetization-mode=1`)
					}
					// 添加视频尺寸信息
					sdpLines.push(`a=framesize:${codecPayloadType} ${width}-${height}`)
				}

				if (ssrc) {
					sdpLines.push(`a=ssrc:${ssrc} cname:recording`)
				}

				sdpLines.push("a=recvonly")

				this.logger.info(`Consumer created for producer ${producer.id}, kind: ${consumer.kind}`)
			} catch (error) {
				this.logger.error(`Failed to create consumer for producer ${producer.id}`, error)
			}
		}

		if (consumers.length === 0) {
			// 清理已创建的 transports
			for (const t of transports) t.close()
			throw new Error("No consumers created, cannot start recording")
		}

		const sdpPath = path.join(this.recordingsDir, `${recordingId}.sdp`)
		fs.writeFileSync(sdpPath, sdpLines.join("\r\n"))
		this.logger.info(`SDP file written to ${sdpPath}:\n${sdpLines.join("\n")}`)

		const outputPath = path.join(this.recordingsDir, `${recordingId}.${recordConfig.format}`)

		const ffmpegProcess = this.spawnFFmpeg(sdpPath, outputPath, recordConfig)

		const session: RecordingSession = {
			recordingId,
			roomId,
			transports, // 存储所有 transports
			consumers,
			ffmpegProcess,
			outputPath,
			startTime: Date.now(),
			recordConfig,
		}

		this.sessions.set(recordingId, session)

		// 监听 FFmpeg 进程退出，触发上传
		ffmpegProcess.on("close", async (code) => {
			this.logger.info(`FFmpeg process exited with code ${code} for recording ${recordingId}`)
			await this.handleRecordingComplete(recordingId)
		})

		ffmpegProcess.on("error", (error) => {
			this.logger.error(`FFmpeg process error for recording ${recordingId}`, error)
			// FFmpeg 启动失败时，也要 reject 等待中的 stopRecording
			const s = this.sessions.get(recordingId)
			if (s?._rejectUpload) {
				s._rejectUpload(new Error(`FFmpeg process error: ${error.message}`))
			}
		})

		ffmpegProcess.stderr?.on("data", (data) => {
			this.logger.debug(`FFmpeg stderr: ${data.toString()}`)
		})

		this.logger.info(`Recording started: ${recordingId}`)
	}

	private spawnFFmpeg(sdpPath: string, outputPath: string, recordConfig: RecordingConfig): ChildProcess {
		const isWebm = recordConfig.format === "webm"
		const args = [
			"-protocol_whitelist",
			"file,rtp,udp",
			"-i",
			sdpPath,
			"-c:v",
			isWebm ? "copy" : "libx264", // 如果源是VP8+mp4，转码为H264
			"-preset",
			"ultrafast",
			"-b:v",
			`${recordConfig.videoBitrate}k`,
			"-s",
			`${recordConfig.videoWidth}x${recordConfig.videoHeight}`,
			"-r",
			recordConfig.videoFramerate.toString(),
			"-c:a",
			isWebm ? "copy" : "aac",
			"-b:a",
			`${recordConfig.audioBitrate}k`,
		]

		if (!isWebm) {
			args.push("-movflags", "+faststart")
		}

		args.push("-y", outputPath)

		this.logger.info(`Spawning FFmpeg with args: ${args.join(" ")}`)

		return spawn("ffmpeg", args)
	}

	/**
	 * 停止录制
	 * 发送 SIGTERM 给 FFmpeg，并等待 handleRecordingComplete 中上传 MinIO 完成后返回 URL
	 * 超时时间 60 秒
	 */
	public async stopRecording(recordingId: string): Promise<string> {
		const session = this.sessions.get(recordingId)
		if (!session) {
			throw new Error(`Recording ${recordingId} not found`)
		}

		this.logger.info(`Stopping recording ${recordingId}`)

		// 在发送 SIGTERM 之前，先注册 uploadCompletionPromise
		// 避免 FFmpeg 退出极快时，Promise 还没注册就触发了 handleRecordingComplete
		const uploadCompletionPromise = new Promise<RecordingUploadResult>((resolve, reject) => {
			session._resolveUpload = resolve
			session._rejectUpload = reject
		})
		session.uploadCompletionPromise = uploadCompletionPromise

		// 发送 SIGTERM 给 FFmpeg，触发正常退出（moov atom 会被写入）
		if (session.ffmpegProcess && !session.ffmpegProcess.killed) {
			session.ffmpegProcess.kill("SIGTERM")
		}

		// 设置超时，防止死锁
		const timeoutPromise = new Promise<never>((_, reject) => setTimeout(() => reject(new Error(`Stop recording timeout after 60s: ${recordingId}`)), 60_000))

		// 等待上传完成或超时
		const result = await Promise.race([uploadCompletionPromise, timeoutPromise])

		this.logger.info(`Recording stopped and uploaded: ${recordingId}, url: ${result.fileUrl}`)
		return result.fileUrl
	}

	/**
	 * FFmpeg 进程退出后的处理：清理资源 -> 上传 MinIO -> 通知 stopRecording
	 */
	private async handleRecordingComplete(recordingId: string): Promise<void> {
		const session = this.sessions.get(recordingId)
		if (!session) {
			return
		}

		this.logger.info(`Handling recording complete for ${recordingId}`)

		try {
			// 清理 consumers
			for (const consumer of session.consumers) {
				consumer.close()
			}
			// 关闭所有 transports
			for (const transport of session.transports) {
				transport.close()
			}

			// 上传到 MinIO
			const format = session.recordConfig.format
			const fileName = `recordings-${recordingId}.${format}`
			const bucketName = config.minio.bucketName

			if (!fs.existsSync(session.outputPath)) {
				throw new Error(`Output file not found: ${session.outputPath}`)
			}

			await this.minioClient.uploadFile(bucketName, fileName, session.outputPath)
			this.logger.info(`Recording uploaded to MinIO: bucket=${bucketName}, key=${fileName}`)

			// 删除本地临时文件
			fs.unlinkSync(session.outputPath)
			const sdpPath = session.outputPath.replace(`.${format}`, ".sdp")
			if (fs.existsSync(sdpPath)) {
				fs.unlinkSync(sdpPath)
			}
			this.logger.info(`Local recording files cleaned up for ${recordingId}`)

			// 拼接完整 MinIO 访问 URL
			const protocol = config.minio.useSSL ? "https" : "http"
			const fileUrl = `${protocol}://${config.minio.endPoint}:${config.minio.port}/${bucketName}/${fileName}`

			// 通知 stopRecording 成功
			if (session._resolveUpload) {
				session._resolveUpload({ fileUrl })
			}
		} catch (error: any) {
			this.logger.error(`Failed to handle recording complete for ${recordingId}`, error)
			// 通知 stopRecording 失败
			if (session._rejectUpload) {
				session._rejectUpload(error instanceof Error ? error : new Error(String(error)))
			}
		} finally {
			this.sessions.delete(recordingId)
		}
	}

	public getRecordingStatus(recordingId: string) {
		const session = this.sessions.get(recordingId)
		if (!session) {
			return null
		}

		const durationSeconds = Math.floor((Date.now() - session.startTime) / 1000)
		let fileSizeBytes = 0

		if (fs.existsSync(session.outputPath)) {
			const stats = fs.statSync(session.outputPath)
			fileSizeBytes = stats.size
		}

		return {
			isRecording: true,
			durationSeconds,
			fileSizeBytes,
		}
	}

	public async cleanup(): Promise<void> {
		this.logger.info("Cleaning up all recording sessions")

		for (const [recordingId, session] of this.sessions.entries()) {
			try {
				if (session.ffmpegProcess && !session.ffmpegProcess.killed) {
					session.ffmpegProcess.kill("SIGTERM")
				}
				for (const consumer of session.consumers) {
					consumer.close()
				}
				// 清理所有 transports
				for (const transport of session.transports) {
					transport.close()
				}
				if (session._rejectUpload) {
					session._rejectUpload(new Error("Server is shutting down"))
				}
			} catch (error) {
				this.logger.error(`Error cleaning up session ${recordingId}`, error)
			}
		}

		this.sessions.clear()
	}
}
