import { spawn, ChildProcess, execSync } from "child_process"
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
	detectedVideoCodec?: string
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
						sdpLines.push(`a=fmtp:${codecPayloadType} packetization-mode=1;profile-level-id=${consumer.rtpParameters.codecs[0].parameters?.["profile-level-id"] || "42e01f"}`)
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

		let detectedVideoCodec: string | undefined
		for (const consumer of consumers) {
			if (consumer.kind === "video") {
				detectedVideoCodec = consumer.rtpParameters.codecs[0].mimeType.split("/")[1].toLowerCase()
				break
			}
		}

		this.logger.info(`Detected video codec from RTP: ${detectedVideoCodec}`)

		const outputPath = path.join(this.recordingsDir, `${recordingId}.${recordConfig.format}`)

		// 延迟启动 FFmpeg，确保 Keyframe 已发出且 RTP 端口有数据 ===
		await new Promise((resolve) => setTimeout(resolve, 1000))
		// 再请求一次 KeyFrame 确保万无一失
		for (const consumer of consumers) {
			if (consumer.kind === "video") {
				await consumer.requestKeyFrame().catch(() => {})
			}
		}
		const ffmpegProcess = this.spawnFFmpeg(sdpPath, outputPath, recordConfig, detectedVideoCodec)

		// 初始化上传完成 Promise，供 stopRecording 等待
		let resolveUpload: (result: RecordingUploadResult) => void
		let rejectUpload: (error: Error) => void
		const uploadCompletionPromise = new Promise<RecordingUploadResult>((resolve, reject) => {
			resolveUpload = resolve
			rejectUpload = reject
		})

		const session: RecordingSession = {
			recordingId,
			roomId,
			transports, // 存储所有 transports
			consumers,
			ffmpegProcess,
			outputPath,
			startTime: Date.now(),
			recordConfig,
			detectedVideoCodec,
			uploadCompletionPromise,
			_resolveUpload: resolveUpload!,
			_rejectUpload: rejectUpload!,
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

	private spawnFFmpeg(sdpPath: string, outputPath: string, recordConfig: RecordingConfig, detectedVideoCodec?: string): ChildProcess {
		const outputFormat = recordConfig.format // "mp4" or "webm"
		const sourceVideoCodec = (detectedVideoCodec || recordConfig.videoCodec).toLowerCase()

		// 判断是否需要转码（源码流格式与容器兼容性）
		// mp4 容器原生支持 H264/H265，不支持 VP8/VP9（需转码）
		// webm 容器原生支持 VP8/VP9，不支持 H264（需转码）
		const mp4NativeCodecs = ["h264", "h265", "aac", "mp3"]
		const webmNativeCodecs = ["vp8", "vp9", "opus"]

		let videoEncoder: string
		if (outputFormat === "mp4") {
			videoEncoder = mp4NativeCodecs.includes(sourceVideoCodec) ? "copy" : "libx264"
		} else {
			// webm
			videoEncoder = webmNativeCodecs.includes(sourceVideoCodec) ? "copy" : "libvpx"
		}

		// Opus 在 mp4 中需要转为 AAC，在 webm 中可以 copy
		const audioEncoder = outputFormat === "mp4" ? "aac" : "copy"

		this.logger.info(`FFmpeg codec strategy: sourceVideo=${sourceVideoCodec}, outputFormat=${outputFormat}, videoEncoder=${videoEncoder}, audioEncoder=${audioEncoder}`)

		const args = [
			"-nostdin",
			"-analyzeduration",
			"10000000", // 10秒
			"-probesize",
			"10000000",
			"-protocol_whitelist",
			"file,rtp,udp",
			// 增加 max_delay，帮助 FFmpeg 更有耐心地缓存 RTP 包以探测头部
			"-max_delay",
			"1000000",
			"-i",
			sdpPath,
			// 明确映射所有流，防止视频流被跳过
			"-map",
			"0:a?",
			"-map",
			"0:v?",
			"-c:v",
			videoEncoder,
		]

		// VP8 转码到 H264 时，需要强制指定输入尺寸
		if (videoEncoder !== "copy") {
			args.push("-vf", `scale=${recordConfig.videoWidth}:${recordConfig.videoHeight}`)
			args.push("-preset", "ultrafast")
			args.push("-b:v", `${recordConfig.videoBitrate}k`)
			args.push("-r", recordConfig.videoFramerate.toString())
		}

		args.push("-c:a", audioEncoder)
		if (audioEncoder !== "copy") {
			args.push("-b:a", `${recordConfig.audioBitrate}k`)
		}

		if (outputFormat === "mp4") {
			args.push("-movflags", "+frag_keyframe+empty_moov")
		}

		args.push("-y", outputPath)

		this.logger.info(`Spawning FFmpeg with args: ${args.join(" ")}`)
		return spawn("ffmpeg", args, {
			// detached: true 在 Windows 上为 FFmpeg 创建独立的进程组
			// 这样后续发送 SIGINT 时不会广播到 Node.js 主进程
			detached: true,
			stdio: ["ignore", "pipe", "pipe"],
		})
	}

	private killFFmpegGracefully(session: RecordingSession, recordingId: string): void {
		const pid = session.ffmpegProcess.pid
		if (!pid) {
			this.logger.warn(`FFmpeg process has no PID for ${recordingId}, using kill()`)
			session.ffmpegProcess.kill("SIGTERM")
			return
		}

		// Windows + Linux/Mac 统一使用 SIGINT
		// detached: true 已确保 FFmpeg 在独立进程组中
		// SIGINT 只会发送给 FFmpeg 自身的进程组，不影响 Node.js 主进程
		try {
			this.logger.info(`Sending SIGINT to FFmpeg PID ${pid} for recording ${recordingId}`)
			process.kill(pid, "SIGINT")
		} catch (e: any) {
			this.logger.warn(`SIGINT failed for ${recordingId}: ${e.message}, falling back to SIGTERM`)
			try {
				process.kill(pid, "SIGTERM")
			} catch (e2) {
				// 进程可能已退出
			}
		}
	}

	/**
	 * 停止录制
	 * 发送 SIGTERM 给 FFmpeg，并等待 handleRecordingComplete 中上传 MinIO 完成后返回 URL
	 */
	public async stopRecording(recordingId: string): Promise<string> {
		const session = this.sessions.get(recordingId)
		if (!session) {
			throw new Error(`Recording session not found: ${recordingId}`)
		}

		this.logger.info(`Stopping recording ${recordingId}`)

		const uploadPromise = session.uploadCompletionPromise
		if (!uploadPromise) {
			throw new Error(`No upload promise found for recording ${recordingId}`)
		}

		return new Promise<string>((resolve, reject) => {
			const forceKillTimer = setTimeout(() => {
				this.logger.warn(`Force killing FFmpeg for recording ${recordingId}`)
				try {
					if (process.platform === "win32") {
						execSync(`taskkill /f /pid ${session.ffmpegProcess.pid}`)
					} else {
						session.ffmpegProcess.kill("SIGKILL")
					}
				} catch (e) {
					/* 进程可能已退出 */
				}
			}, 30000)

			uploadPromise
				.then((result) => {
					clearTimeout(forceKillTimer)
					resolve(result.fileUrl)
				})
				.catch((err) => {
					clearTimeout(forceKillTimer)
					reject(err)
				})

			// 调用平台感知的优雅终止
			this.killFFmpegGracefully(session, recordingId)
		})
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
					const pid = session.ffmpegProcess.pid
					if (pid) {
						process.kill(pid, "SIGINT")
					}
				}
				// 清理所有 consumers
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
