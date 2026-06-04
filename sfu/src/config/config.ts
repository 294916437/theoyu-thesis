import dotenv from "dotenv"
import type * as mediasoupTypes from "mediasoup/node/lib/types"

dotenv.config()

const parsePort = (value: string | undefined, fallback: number, name: string): number => {
	const rawValue = value || String(fallback)
	if (!/^\d+$/.test(rawValue)) {
		throw new Error(`${name} must be a valid TCP/UDP port, got: ${value}`)
	}
	const port = parseInt(rawValue, 10)
	if (!Number.isInteger(port) || port <= 0 || port > 65535) {
		throw new Error(`${name} must be a valid TCP/UDP port, got: ${value}`)
	}
	return port
}

export interface MediasoupConfig {
	numWorkers: number
	workerSettings: {
		logLevel: "debug" | "warn" | "error"
		logTags: mediasoupTypes.WorkerLogTag[]
		rtcMinPort: number
		rtcMaxPort: number
	}
	routerOptions: {
		mediaCodecs: mediasoupTypes.RtpCodecCapability[]
	}
	webRtcTransportOptions: {
		listenInfos: mediasoupTypes.TransportListenInfo[]
		enableUdp: boolean
		enableTcp: boolean
		preferUdp: boolean
		initialAvailableOutgoingBitrate: number
	}
}

export interface ServerConfig {
	port: number
	host: string
	cors: {
		origin: string
		credentials: boolean
	}
}

export interface GrpcConfig {
	host: string
	port: number
	serviceName: string
}

export interface NacosConfig {
	serverList: string
	namespace: string
	serviceName: string
	ip: string
	port: number
	instanceId: string
	metadata: {
		httpPort: string
		instanceId: string
	}
}
export interface MinioConfig {
	endPoint: string
	port: number
	useSSL: boolean
	accessKey: string
	secretKey: string
	bucketName: string
}
export class TestModeConfig {
	public readonly enabled: boolean = process.env.SFU_TEST_MODE === "true"
}

export class Config {
	public readonly testMode: TestModeConfig = {
		enabled: process.env.SFU_TEST_MODE === "true",
	}
	public readonly minio: MinioConfig = {
		endPoint: process.env.MINIO_ENDPOINT || "127.0.0.1",
		port: parsePort(process.env.MINIO_PORT, 9000, "MINIO_PORT"),
		useSSL: process.env.MINIO_USE_SSL === "true",
		accessKey: process.env.MINIO_ACCESS_KEY || "test",
		secretKey: process.env.MINIO_SECRET_KEY || "test123456",
		bucketName: process.env.MINIO_BUCKET_NAME || "thesis",
	}

	public readonly server: ServerConfig = {
		port: parsePort(process.env.PORT, 3000, "PORT"),
		host: process.env.HOST || "0.0.0.0",
		cors: {
			origin: process.env.CORS_ORIGIN || "*",
			credentials: true,
		},
	}

	public readonly grpc: GrpcConfig = {
		host: process.env.GRPC_HOST || "localhost",
		port: parsePort(process.env.GRPC_PORT, 50051, "GRPC_PORT"),
		serviceName: process.env.GRPC_SERVICE_NAME || "video-conference-service",
	}

	public readonly nacos: NacosConfig = {
		serverList: process.env.NACOS_SERVER || "127.0.0.1:8848",
		namespace: process.env.NACOS_NAMESPACE || "public",
		serviceName: process.env.NACOS_SERVICE_NAME || "sfu-server",
		ip: process.env.NACOS_IP || "127.0.0.1",
		port: parsePort(process.env.NACOS_PORT, this.server.port, "NACOS_PORT"),
		instanceId:
			process.env.SFU_INSTANCE_ID ||
			process.env.NACOS_INSTANCE_ID ||
			`${process.env.NACOS_IP || "127.0.0.1"}:${parsePort(process.env.NACOS_PORT, this.server.port, "NACOS_PORT")}`,
		metadata: {
			httpPort: String(this.server.port),
			instanceId:
				process.env.SFU_INSTANCE_ID ||
				process.env.NACOS_INSTANCE_ID ||
				`${process.env.NACOS_IP || "127.0.0.1"}:${parsePort(process.env.NACOS_PORT, this.server.port, "NACOS_PORT")}`,
		},
	}

	public readonly mediasoup: MediasoupConfig = {
		numWorkers: parseInt(process.env.MEDIASOUP_WORKERS || "4", 10),
		workerSettings: {
			logLevel: (process.env.MEDIASOUP_LOG_LEVEL as any) || "warn",
			logTags: ["ice", "dtls", "bwe", "score"],
			rtcMinPort: parsePort(process.env.RTC_MIN_PORT, 40000, "RTC_MIN_PORT"),
			rtcMaxPort: parsePort(process.env.RTC_MAX_PORT, 49999, "RTC_MAX_PORT"),
		},
		routerOptions: {
			mediaCodecs: [
				// Audio: Opus (PT 111 是标准值)
				{
					kind: "audio",
					mimeType: "audio/opus",
					clockRate: 48000,
					channels: 2,
					preferredPayloadType: 111,
					parameters: {
						// 启用不连续传输：静音时停止发包，降低带宽与 CPU
						usedtx: 1,
						// 启用前向纠错：在轻微丢包时无需重传即可恢复音频
						useinbandfec: 1,
					},
				},
				// Video: VP8 (PT 96 是标准值)
				{
					kind: "video",
					mimeType: "video/VP8",
					clockRate: 90000,
					parameters: {
						"x-google-start-bitrate": 1000,
					},
					preferredPayloadType: 96,
				},
				// Video: VP9 (PT 98)
				{
					kind: "video",
					mimeType: "video/VP9",
					clockRate: 90000,
					parameters: {
						"profile-id": 2,
						"x-google-start-bitrate": 1000,
					},
					preferredPayloadType: 98,
				},
				// Video: h264 Constrained Baseline (最兼容)
				{
					kind: "video",
					mimeType: "video/h264",
					clockRate: 90000,
					parameters: {
						"packetization-mode": 1,
						"profile-level-id": "42e01f",
						"level-asymmetry-allowed": 1,
						"x-google-start-bitrate": 1000,
					},
					preferredPayloadType: 102,
				},
			],
		},
		webRtcTransportOptions: {
			listenInfos: [
				{
					protocol: "udp",
					ip: process.env.MEDIASOUP_LISTEN_IP || "0.0.0.0",
					announcedAddress: process.env.MEDIASOUP_ANNOUNCED_IP,
				},
				{
					protocol: "tcp",
					ip: process.env.MEDIASOUP_LISTEN_IP || "0.0.0.0",
					announcedAddress: process.env.MEDIASOUP_ANNOUNCED_IP,
				},
			],
			enableUdp: true,
			enableTcp: true,
			preferUdp: true,
			initialAvailableOutgoingBitrate: 1000000,
		},
	}

	constructor() {
		const { rtcMinPort, rtcMaxPort } = this.mediasoup.workerSettings
		if (rtcMinPort > rtcMaxPort) {
			throw new Error(`RTC_MIN_PORT must be less than or equal to RTC_MAX_PORT, got ${rtcMinPort}-${rtcMaxPort}`)
		}
	}
}

export default new Config()
