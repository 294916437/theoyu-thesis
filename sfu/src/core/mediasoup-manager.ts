import * as mediasoup from "mediasoup"
import type * as mediasoupTypes from "mediasoup/node/lib/types"
import config from "../config/config"
import { Logger } from "../utils/logger"

export class MediasoupManager {
	private static instance: MediasoupManager
	private workers: mediasoupTypes.Worker[] = []
	private routers: Map<string, mediasoupTypes.Router> = new Map()
	/** 记录每个 Worker 当前承载的 Router 数量，用于 Least-Load 调度 */
	private workerRouterCount: Map<number, number> = new Map()
	private logger = new Logger("MediasoupManager")

	private constructor() {}

	public static getInstance(): MediasoupManager {
		if (!MediasoupManager.instance) {
			MediasoupManager.instance = new MediasoupManager()
		}
		return MediasoupManager.instance
	}

	public async init(): Promise<void> {
		this.logger.info(`Creating ${config.mediasoup.numWorkers} mediasoup workers...`)

		for (let i = 0; i < config.mediasoup.numWorkers; i++) {
			const worker = await mediasoup.createWorker({
				logLevel: config.mediasoup.workerSettings.logLevel,
				logTags: config.mediasoup.workerSettings.logTags,
				rtcMinPort: config.mediasoup.workerSettings.rtcMinPort,
				rtcMaxPort: config.mediasoup.workerSettings.rtcMaxPort,
			})

			worker.on("died", () => {
				this.logger.error(`Worker ${worker.pid} died, exiting in 2 seconds...`)
				setTimeout(() => process.exit(1), 2000)
			})

			this.workers.push(worker)
			this.workerRouterCount.set(worker.pid, 0)
			this.logger.info(`Worker ${i + 1} created with PID: ${worker.pid}`)
		}

		this.logger.info("All mediasoup workers created successfully")
	}

	public async createRouter(roomId: string): Promise<mediasoupTypes.Router> {
		const worker = this.getNextWorker()

		const router = await worker.createRouter({
			mediaCodecs: config.mediasoup.routerOptions.mediaCodecs,
		})

		this.routers.set(roomId, router)
		this.workerRouterCount.set(worker.pid, (this.workerRouterCount.get(worker.pid) ?? 0) + 1)
		this.logger.info(`Router created for room: ${roomId}`)

		return router
	}

	public getRouter(roomId: string): mediasoupTypes.Router | undefined {
		return this.routers.get(roomId)
	}

	public async closeRouter(roomId: string): Promise<void> {
		const router = this.routers.get(roomId)
		if (router) {
			// 找到该 Router 所属的 Worker 并减少计数
			for (const worker of this.workers) {
				if ((worker as any).appData?.routerId === router.id) {
					this.workerRouterCount.set(worker.pid, Math.max(0, (this.workerRouterCount.get(worker.pid) ?? 1) - 1))
					break
				}
			}
			router.close()
			this.routers.delete(roomId)
			this.logger.info(`Router closed for room: ${roomId}`)
		}
	}

	/**
	 * Least-Load 调度：选取当前承载 Router 数量最少的 Worker。
	 * 相比 Round-Robin，能保证各 Worker 的实际负载均衡，
	 * 避免房间数量不是 Worker 数整数倍时出现的负载热点。
	 */
	private getNextWorker(): mediasoupTypes.Worker {
		let selectedWorker = this.workers[0]
		let minCount = this.workerRouterCount.get(selectedWorker.pid) ?? 0

		for (const worker of this.workers) {
			const count = this.workerRouterCount.get(worker.pid) ?? 0
			if (count < minCount) {
				minCount = count
				selectedWorker = worker
			}
		}
		return selectedWorker
	}

	public getWorkers(): mediasoupTypes.Worker[] {
		return this.workers
	}

	public async close(): Promise<void> {
		this.logger.info("Closing all mediasoup workers...")

		for (const router of this.routers.values()) {
			router.close()
		}
		this.routers.clear()

		for (const worker of this.workers) {
			worker.close()
		}
		this.workers = []

		this.logger.info("All mediasoup workers closed")
	}
}
