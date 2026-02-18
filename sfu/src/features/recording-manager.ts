import { spawn, ChildProcess } from "child_process"
import * as fs from "fs"
import * as path from "path"
import type * as mediasoupTypes from "mediasoup/node/lib/types"
import { Logger } from "../utils/logger"
import { MinioClient } from "../utils/minio-client"

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

interface RecordingSession {
	recordingId: string
	roomId: string
	transport: mediasoupTypes.PlainTransport
	consumers: mediasoupTypes.Consumer[]
	ffmpegProcess: ChildProcess
	outputPath: string
	startTime: number
	config: RecordingConfig
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

	public async startRecording(roomId: string, hostId: string, router: mediasoupTypes.Router, producers: mediasoupTypes.Producer[], config: RecordingConfig): Promise<void> {
		// 联合主键
		const recordingId = `${roomId}_${hostId}`

		if (this.sessions.has(recordingId)) {
			throw new Error(`Recording ${recordingId} already exists`)
		}

		this.logger.info(`Starting recording ${recordingId} for room ${roomId}`)

		// 创建 PlainTransport
		const transport = await router.createPlainTransport({
			listenIp: { ip: "127.0.0.1", announcedIp: undefined },
			rtcpMux: false,
			comedia: true,
		})

		this.logger.info(`PlainTransport created: ${transport.tuple.localIp}:${transport.tuple.localPort}`)

		// 创建 Consumers
		const consumers: mediasoupTypes.Consumer[] = []
		const sdpLines: string[] = []

		sdpLines.push("v=0")
		sdpLines.push("o=- 0 0 IN IP4 127.0.0.1")
		sdpLines.push("s=Mediasoup Recording")
		sdpLines.push("c=IN IP4 127.0.0.1")
		sdpLines.push("t=0 0")

		let mediaIndex = 0

		for (const producer of producers) {
			try {
				const consumer = await transport.consume({
					producerId: producer.id,
					rtpCapabilities: router.rtpCapabilities,
					paused: false,
				})

				consumers.push(consumer)

				// 立即请求关键帧
				if (consumer.kind === "video") {
					await consumer.requestKeyFrame()
					this.logger.info(`Requested key frame for consumer ${consumer.id}`)
				}

				// 构建 SDP
				const codecPayloadType = consumer.rtpParameters.codecs[0].payloadType
				const codecName = consumer.rtpParameters.codecs[0].mimeType.split("/")[1]
				const clockRate = consumer.rtpParameters.codecs[0].clockRate

				sdpLines.push(`m=${consumer.kind} ${transport.tuple.localPort + mediaIndex} RTP/AVP ${codecPayloadType}`)
				sdpLines.push(`a=rtpmap:${codecPayloadType} ${codecName}/${clockRate}`)

				if (consumer.kind === "video") {
					sdpLines.push(`a=fmtp:${codecPayloadType} packetization-mode=1`)
				}

				mediaIndex++

				this.logger.info(`Consumer created for producer ${producer.id}, kind: ${consumer.kind}`)
			} catch (error) {
				this.logger.error(`Failed to create consumer for producer ${producer.id}`, error)
			}
		}

		if (consumers.length === 0) {
			transport.close()
			throw new Error("No consumers created, cannot start recording")
		}

		// 生成 SDP 文件
		const sdpPath = path.join(this.recordingsDir, `${recordingId}.sdp`)
		fs.writeFileSync(sdpPath, sdpLines.join("\n"))

		// 输出文件路径
		const outputPath = path.join(this.recordingsDir, `${recordingId}.${config.format}`)

		// 启动 FFmpeg
		const ffmpegProcess = this.spawnFFmpeg(sdpPath, outputPath, config)

		const session: RecordingSession = {
			recordingId,
			roomId,
			transport,
			consumers,
			ffmpegProcess,
			outputPath,
			startTime: Date.now(),
			config,
		}

		this.sessions.set(recordingId, session)

		// 监听 FFmpeg 进程退出
		ffmpegProcess.on("close", async (code) => {
			this.logger.info(`FFmpeg process exited with code ${code} for recording ${recordingId}`)
			await this.handleRecordingComplete(recordingId)
		})

		ffmpegProcess.on("error", (error) => {
			this.logger.error(`FFmpeg process error for recording ${recordingId}`, error)
		})

		// 记录 FFmpeg 输出
		ffmpegProcess.stderr?.on("data", (data) => {
			this.logger.debug(`FFmpeg stderr: ${data.toString()}`)
		})

		this.logger.info(`Recording started: ${recordingId}`)
	}

	private spawnFFmpeg(sdpPath: string, outputPath: string, config: RecordingConfig): ChildProcess {
		const isWebm = config.format === "webm"
		const args = [
			"-protocol_whitelist",
			"file,rtp,udp",
			"-i",
			sdpPath,
			"-c:v",
			isWebm ? "libvpx" : "libx264", // WebM 使用 VP8/VP9, MP4 使用 H.264
			"-preset",
			"ultrafast",
			"-b:v",
			`${config.videoBitrate}k`,
			"-s",
			`${config.videoWidth}x${config.videoHeight}`,
			"-r",
			config.videoFramerate.toString(),
			"-c:a",
			isWebm ? "libvorbis" : "aac", // WebM 使用 Vorbis/Opus, MP4 使用 AAC
			"-b:a",
			`${config.audioBitrate}k`,
		]

		if (!isWebm) {
			args.push("-movflags", "+faststart")
		}

		args.push("-y", outputPath)

		this.logger.info(`Spawning FFmpeg with args: ${args.join(" ")}`)

		return spawn("ffmpeg", args)
	}

	public async stopRecording(recordingId: string): Promise<string> {
		const session = this.sessions.get(recordingId)
		if (!session) {
			throw new Error(`Recording ${recordingId} not found`)
		}

		this.logger.info(`Stopping recording ${recordingId}`)

		// 发送 SIGTERM 给 FFmpeg
		if (session.ffmpegProcess && !session.ffmpegProcess.killed) {
			session.ffmpegProcess.kill("SIGTERM")
		}

		// 等待一段时间让 FFmpeg 完成
		await new Promise((resolve) => setTimeout(resolve, 2000))

		return session.outputPath
	}

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

			// 关闭 transport
			session.transport.close()

			// 上传到 MinIO
			const format = session.config.format
			const fileName = `${session.roomId}/${recordingId}.${format}`
			const bucketName = "room-record"

			if (fs.existsSync(session.outputPath)) {
				await this.minioClient.uploadFile(bucketName, fileName, session.outputPath)

				this.logger.info(`Recording uploaded to MinIO: ${fileName}`)

				// 删除本地文件
				fs.unlinkSync(session.outputPath)
				const sdpPath = session.outputPath.replace(`.${format}`, ".sdp")
				if (fs.existsSync(sdpPath)) {
					fs.unlinkSync(sdpPath)
				}

				this.logger.info(`Local recording files cleaned up for ${recordingId}`)
			}
		} catch (error) {
			this.logger.error(`Failed to handle recording complete for ${recordingId}`, error)
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

				session.transport.close()
			} catch (error) {
				this.logger.error(`Error cleaning up session ${recordingId}`, error)
			}
		}

		this.sessions.clear()
	}
}
