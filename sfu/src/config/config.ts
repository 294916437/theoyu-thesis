import dotenv from "dotenv"
import type * as mediasoupTypes from "mediasoup/node/lib/types"

dotenv.config()

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
	serverPort: number
	serviceName: string
}

export interface NacosConfig {
	serverList: string
	namespace: string
	serviceName: string
	ip: string
	port: number
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
		port: parseInt(process.env.MINIO_PORT || "9000", 10),
		useSSL: process.env.MINIO_USE_SSL === "true",
		accessKey: process.env.MINIO_ACCESS_KEY || "test",
		secretKey: process.env.MINIO_SECRET_KEY || "test123456",
		bucketName: process.env.MINIO_BUCKET_NAME || "thesis",
	}

	public readonly server: ServerConfig = {
		port: parseInt(process.env.PORT || "3000", 10),
		host: process.env.HOST || "0.0.0.0",
		cors: {
			origin: process.env.CORS_ORIGIN || "*",
			credentials: true,
		},
	}

	public readonly grpc: GrpcConfig = {
		host: process.env.GRPC_HOST || "localhost",
		port: parseInt(process.env.GRPC_PORT || "50051", 10),
		serverPort: parseInt(process.env.GRPC_SERVER_PORT || "50052", 10),
		serviceName: process.env.GRPC_SERVICE_NAME || "video-conference-service",
	}

	public readonly nacos: NacosConfig = {
		serverList: process.env.NACOS_SERVER || "127.0.0.1:8848",
		namespace: process.env.NACOS_NAMESPACE || "public",
		serviceName: process.env.NACOS_SERVICE_NAME || "sfu-server",
		ip: process.env.NACOS_IP || "127.0.0.1",
		port: parseInt(process.env.NACOS_PORT || "3000", 10),
	}

	public readonly mediasoup: MediasoupConfig = {
		numWorkers: parseInt(process.env.MEDIASOUP_WORKERS || "4", 10),
		workerSettings: {
			logLevel: (process.env.MEDIASOUP_LOG_LEVEL as any) || "warn",
			logTags: ["info", "ice", "dtls", "rtp", "srtp", "rtcp", "rtx", "bwe", "score", "simulcast", "svc"],
			rtcMinPort: parseInt(process.env.RTC_MIN_PORT || "40000", 10),
			rtcMaxPort: parseInt(process.env.RTC_MAX_PORT || "49999", 10),
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
}

export default new Config()
