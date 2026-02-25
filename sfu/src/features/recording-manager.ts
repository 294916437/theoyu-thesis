import { spawn, ChildProcess, execSync } from "child_process"
import * as fs from "fs"
import * as path from "path"
import * as net from "net"
import * as dgram from "dgram"
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

const getFreePort = async (): Promise<number> => {
	return new Promise((resolve, reject) => {
		const socket = dgram.createSocket("udp4")
		socket.bind(0, "127.0.0.1", () => {
			const port = socket.address().port
			socket.close(() => {
				resolve(port)
			})
		})
		socket.on("error", reject)
	})
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

	// 等待 FFmpeg 绑定端口就绪的辅助函数
	private waitForFFmpegReady(ffmpegProcess: ChildProcess, timeoutMs: number = 5000): Promise<void> {
		return new Promise((resolve) => {
			let resolved = false

			const doResolve = () => {
				if (!resolved) {
					resolved = true
					resolve()
				}
			}

			const timer = setTimeout(() => {
				this.logger.warn("FFmpeg ready timeout, proceeding anyway")
				doResolve()
			}, timeoutMs)

			const onData = (data: Buffer) => {
				const text = data.toString()
				// FFmpeg 输出这些关键词说明已完成端口绑定和流探测
				if (
					text.includes("Press [q]") ||
					text.includes("Output #0") ||
					text.includes("Stream mapping") ||
					// 收到 RTP 数据后 FFmpeg 会输出此行（analyzeduration 阶段完成）
					text.includes("Input #0")
				) {
					clearTimeout(timer)
					// 关键修复：监听到就绪信号后，移除此临时监听器，避免重复 resolve
					ffmpegProcess.stderr?.off("data", onData)
					this.logger.info("FFmpeg ready signal detected")
					doResolve()
				}
			}

			// 若 FFmpeg 在等待期间就提前退出，立即 resolve（后续会处理错误）
			const onClose = (code: number | null) => {
				this.logger.warn(`FFmpeg exited with code ${code} during ready wait`)
				clearTimeout(timer)
				ffmpegProcess.stderr?.off("data", onData)
				doResolve()
			}

			ffmpegProcess.stderr?.on("data", onData)
			ffmpegProcess.once("close", onClose)
		})
	}

	public async startRecording(roomId: string, hostId: string, router: mediasoupTypes.Router, producers: mediasoupTypes.Producer[], recordConfig: RecordingConfig): Promise<void> {
		const recordingId = `${roomId}-${hostId}`

		if (this.sessions.has(recordingId)) {
			throw new Error(`Recording ${recordingId} already exists`)
		}

		this.logger.info(`Starting recording ${recordingId} for room ${roomId}`)

		// ===== 阶段一：准备资源，构建 SDP，但暂不激活推流 =====

		interface PreparedTrack {
			transport: mediasoupTypes.PlainTransport
			consumer: mediasoupTypes.Consumer
			remotePort: number
		}

		const preparedTracks: PreparedTrack[] = []
		const transports: mediasoupTypes.PlainTransport[] = []
		const consumers: mediasoupTypes.Consumer[] = []
		const sdpLines: string[] = []

		sdpLines.push("v=0")
		sdpLines.push("o=- 0 0 IN IP4 127.0.0.1")
		sdpLines.push("s=Mediasoup Recording")
		sdpLines.push("c=IN IP4 127.0.0.1")
		sdpLines.push("t=0 0")
		sdpLines.push("a=tool:libavformat")

		for (const producer of producers) {
			try {
				// 1. 为 FFmpeg 动态获取一个空闲 UDP 端口
				const remotePort = await getFreePort()

				// 2. 创建 PlainTransport，rtcpMux: true（RTP/RTCP 复用同一端口）
				//    comedia: false，需要显式调用 connect() 激活推流方向
				//    此时不调用 connect()，Mediasoup 不会推送任何 RTP 包
				const transport = await router.createPlainTransport({
					listenIp: { ip: "127.0.0.1", announcedIp: undefined },
					rtcpMux: true,
					comedia: false,
				})
				transports.push(transport)

				this.logger.info(`[Phase1] PlainTransport created for ${producer.kind}: ` + `Mediasoup local port=${transport.tuple.localPort}, FFmpeg target port=${remotePort}`)

				// 3. 创建 Consumer，paused: true
				//    先暂停消费，避免数据在 FFmpeg 未就绪时堆积丢失
				const consumer = await transport.consume({
					producerId: producer.id,
					rtpCapabilities: router.rtpCapabilities,
					paused: true,
				})
				consumers.push(consumer)

				// 4. 暂存待激活信息，供阶段三使用
				preparedTracks.push({ transport, consumer, remotePort })

				// 5. 根据 consumer 的 RTP 参数生成 SDP
				const codecPayloadType = consumer.rtpParameters.codecs[0].payloadType
				const codecName = consumer.rtpParameters.codecs[0].mimeType.split("/")[1]
				const clockRate = consumer.rtpParameters.codecs[0].clockRate
				const ssrc = consumer.rtpParameters.encodings?.[0]?.ssrc

				// m= 行端口写 remotePort（FFmpeg 监听的端口）
				sdpLines.push(`m=${consumer.kind} ${remotePort} RTP/AVP ${codecPayloadType}`)
				sdpLines.push(`c=IN IP4 127.0.0.1`)
				// rtcpMux 模式下 RTCP 端口与 RTP 端口相同
				sdpLines.push(`a=rtcp:${remotePort}`)
				sdpLines.push(`a=rtcp-mux`)
				sdpLines.push(`a=rtpmap:${codecPayloadType} ${codecName}/${clockRate}`)

				if (consumer.kind === "audio" && codecName.toLowerCase() === "opus") {
					// Opus 需声明双声道
					sdpLines.push(`a=rtpmap:${codecPayloadType} ${codecName}/${clockRate}/2`)
				}

				if (consumer.kind === "video") {
					const width = recordConfig.videoWidth
					const height = recordConfig.videoHeight
					if (codecName.toLowerCase() === "h264") {
						const profileLevelId = consumer.rtpParameters.codecs[0].parameters?.["profile-level-id"] || "42e01f"
						sdpLines.push(`a=fmtp:${codecPayloadType} packetization-mode=1;profile-level-id=${profileLevelId}`)
					}
					sdpLines.push(`a=framesize:${codecPayloadType} ${width}-${height}`)
				}

				if (ssrc) {
					sdpLines.push(`a=ssrc:${ssrc} cname:recording`)
				}

				// FFmpeg 作为接收端，标记为 recvonly
				sdpLines.push("a=recvonly")

				this.logger.info(`[Phase1] Consumer prepared for producer ${producer.id}, kind: ${consumer.kind}`)
			} catch (error) {
				this.logger.error(`[Phase1] Failed to prepare consumer for producer ${producer.id}`, error)
			}
		}

		// 若没有任何 consumer 创建成功，清理并抛出异常
		if (consumers.length === 0) {
			for (const t of transports) t.close()
			throw new Error("No consumers created, cannot start recording")
		}

		// ===== 阶段二：写入 SDP，启动 FFmpeg 并等待其端口就绪 =====

		const sdpPath = path.join(this.recordingsDir, `${recordingId}.sdp`)
		fs.writeFileSync(sdpPath, sdpLines.join("\r\n"))
		this.logger.info(`[Phase2] SDP file written to ${sdpPath}:\n${sdpLines.join("\n")}`)

		// 从 consumers 中检测视频编解码器，用于 FFmpeg 转码策略
		let detectedVideoCodec: string | undefined
		for (const consumer of consumers) {
			if (consumer.kind === "video") {
				detectedVideoCodec = consumer.rtpParameters.codecs[0].mimeType.split("/")[1].toLowerCase()
				break
			}
		}
		this.logger.info(`[Phase2] Detected video codec from RTP: ${detectedVideoCodec}`)

		const outputPath = path.join(this.recordingsDir, `${recordingId}.${recordConfig.format}`)

		// 先启动 FFmpeg：FFmpeg 会立即解析 SDP 并绑定 SDP 中定义的 UDP 端口，开始监听
		// 此时 Mediasoup 尚未 connect，不会有 RTP 包发出，FFmpeg 处于等待状态
		const ffmpegProcess = this.spawnFFmpeg(sdpPath, outputPath, recordConfig, detectedVideoCodec)

		// 等待 FFmpeg 完成端口绑定并准备好接收数据
		// waitForFFmpegReady 通过监听 stderr 中的关键字（"Output #0" / "Press [q]"）来判断就绪
		// 超时兜底：8 秒后无论如何都继续（FFmpeg 可能正在等待第一个 RTP 包）
		this.logger.info(`[Phase2] Waiting for FFmpeg to bind ports and become ready...`)
		await this.waitForFFmpegReady(ffmpegProcess, 8000)
		this.logger.info(`[Phase2] FFmpeg is ready, activating Mediasoup transports...`)

		// ===== 阶段三：FFmpeg 就绪后，激活所有 Transport，开始推流 =====

		for (const track of preparedTracks) {
			try {
				// connect：告知 Mediasoup 将 RTP 包推送到 FFmpeg 监听的端口
				// rtcpMux: true，只传 ip + port，不传 rtcpPort（两者互斥）
				await track.transport.connect({
					ip: "127.0.0.1",
					port: track.remotePort,
				})

				// resume：解除 Consumer 暂停，开始消费 Producer 的流并转发给 FFmpeg
				await track.consumer.resume()

				// 对视频流请求关键帧，确保 FFmpeg 能立即解码，避免等待下一个 I 帧
				if (track.consumer.kind === "video") {
					await track.consumer.requestKeyFrame()
					this.logger.info(`[Phase3] Key frame requested for consumer ${track.consumer.id}`)
				}

				this.logger.info(`[Phase3] Transport activated for ${track.consumer.kind}, ` + `pushing RTP to 127.0.0.1:${track.remotePort}`)
			} catch (err) {
				this.logger.error(`[Phase3] Failed to activate transport for ${track.consumer.kind} on port ${track.remotePort}`, err)
			}
		}

		// ===== Session 管理：存储会话，注册 FFmpeg 事件处理 =====

		// 初始化上传完成 Promise，供 stopRecording 等待上传结果
		let resolveUpload: (result: RecordingUploadResult) => void
		let rejectUpload: (error: Error) => void
		const uploadCompletionPromise = new Promise<RecordingUploadResult>((resolve, reject) => {
			resolveUpload = resolve
			rejectUpload = reject
		})
		// 挂载 catch handler，防止未处理的 Promise 拒绝导致 Node.js 进程崩溃
		uploadCompletionPromise.catch((err) => {
			this.logger.warn(`Recording upload promise rejected (expected during failure): ${err.message}`)
		})

		const session: RecordingSession = {
			recordingId,
			roomId,
			transports,
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

		// FFmpeg 进程正常/异常退出时，触发资源清理和 MinIO 上传
		ffmpegProcess.on("close", async (code) => {
			this.logger.info(`FFmpeg process exited with code ${code} for recording ${recordingId}`)
			await this.handleRecordingComplete(recordingId)
		})

		// FFmpeg 启动失败（如找不到可执行文件）时，reject 等待中的 stopRecording
		ffmpegProcess.on("error", (error) => {
			this.logger.error(`FFmpeg process error for recording ${recordingId}`, error)
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
			this.logger.warn(`FFmpeg process has no PID for ${recordingId}`)
			return
		}

		// Windows 上 detached: true 的子进程必须用 taskkill
		// process.kill(pid) 在 Windows 上对 detached 进程会报 ESRCH（找不到进程）
		if (process.platform === "win32") {
			try {
				this.logger.info(`[Win32] taskkill /f /t /pid ${pid} for recording ${recordingId}`)
				// /t: 同时终止子进程树，/f: 强制终止
				execSync(`taskkill /f /t /pid ${pid}`, { stdio: "ignore" })
			} catch (e: any) {
				// 进程可能已退出，忽略错误
				this.logger.warn(`taskkill failed for ${recordingId}: ${e.message}`)
			}
		} else {
			// Linux/Mac：SIGINT 触发 FFmpeg 优雅停止（写入文件尾部）
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
	}

	/**
	 * 停止录制
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

	private async handleRecordingComplete(recordingId: string): Promise<void> {
		const session = this.sessions.get(recordingId)
		if (!session) {
			return
		}

		this.logger.info(`Handling recording complete for ${recordingId}`)

		try {
			// 清理 consumers 和 transports
			for (const consumer of session.consumers) {
				try {
					consumer.close()
				} catch (_) {}
			}
			for (const transport of session.transports) {
				try {
					transport.close()
				} catch (_) {}
			}

			const format = session.recordConfig.format
			const fileName = `recordings-${recordingId}.${format}`
			const bucketName = config.minio.bucketName

			// 文件不存在时不直接 throw，而是视为录制异常（如 FFmpeg 从未成功写入）
			// 此时 reject uploadPromise，让 stopRecording 感知到失败并正确返回错误
			if (!fs.existsSync(session.outputPath)) {
				this.logger.warn(`Output file not found for ${recordingId}, FFmpeg may have failed`)
				throw new Error(`Recording failed: output file not found (${session.outputPath})`)
			}

			// 检查文件大小，防止上传空文件
			const fileStats = fs.statSync(session.outputPath)
			if (fileStats.size === 0) {
				throw new Error(`Recording failed: output file is empty (${session.outputPath})`)
			}

			this.logger.info(`Uploading recording to MinIO: ${fileName} (${fileStats.size} bytes)`)
			await this.minioClient.uploadFile(bucketName, fileName, session.outputPath)
			this.logger.info(`Recording uploaded to MinIO: bucket=${bucketName}, key=${fileName}`)

			// 删除本地临时文件
			try {
				fs.unlinkSync(session.outputPath)
				const sdpPath = session.outputPath.replace(`.${format}`, ".sdp")
				if (fs.existsSync(sdpPath)) {
					fs.unlinkSync(sdpPath)
				}
				this.logger.info(`Local recording files cleaned up for ${recordingId}`)
			} catch (cleanupErr) {
				// 清理失败不影响主流程
				this.logger.warn(`Failed to cleanup local files for ${recordingId}: ${cleanupErr}`)
			}

			const protocol = config.minio.useSSL ? "https" : "http"
			const fileUrl = `${protocol}://${config.minio.endPoint}:${config.minio.port}/${bucketName}/${fileName}`

			if (session._resolveUpload) {
				session._resolveUpload({ fileUrl })
			}
		} catch (error: any) {
			this.logger.error(`Failed to handle recording complete for ${recordingId}`, error)
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
