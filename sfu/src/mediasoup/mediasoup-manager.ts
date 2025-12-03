import * as mediasoup from "mediasoup";
import type * as mediasoupTypes from "mediasoup/node/lib/types";
import config from "../config/config";
import { Logger } from "../utils/logger";

export class MediasoupManager {
	private static instance: MediasoupManager;
	private workers: mediasoupTypes.Worker[] = [];
	private routers: Map<string, mediasoupTypes.Router> = new Map();
	private nextWorkerIndex = 0;
	private logger = new Logger("MediasoupManager");

	private constructor() {}

	public static getInstance(): MediasoupManager {
		if (!MediasoupManager.instance) {
			MediasoupManager.instance = new MediasoupManager();
		}
		return MediasoupManager.instance;
	}

	public async init(): Promise<void> {
		this.logger.info(`Creating ${config.mediasoup.numWorkers} mediasoup workers...`);

		for (let i = 0; i < config.mediasoup.numWorkers; i++) {
			const worker = await mediasoup.createWorker({
				logLevel: config.mediasoup.workerSettings.logLevel,
				logTags: config.mediasoup.workerSettings.logTags,
				rtcMinPort: config.mediasoup.workerSettings.rtcMinPort,
				rtcMaxPort: config.mediasoup.workerSettings.rtcMaxPort,
			});

			worker.on("died", () => {
				this.logger.error(`Worker ${worker.pid} died, exiting in 2 seconds...`);
				setTimeout(() => process.exit(1), 2000);
			});

			this.workers.push(worker);
			this.logger.info(`Worker ${i + 1} created with PID: ${worker.pid}`);
		}

		this.logger.info("All mediasoup workers created successfully");
	}

	public async createRouter(roomId: string): Promise<mediasoupTypes.Router> {
		const worker = this.getNextWorker();
		const router = await worker.createRouter({
			mediaCodecs: config.mediasoup.routerOptions.mediaCodecs,
		});

		this.routers.set(roomId, router);
		this.logger.info(`Router created for room: ${roomId}`);

		return router;
	}

	public getRouter(roomId: string): mediasoupTypes.Router | undefined {
		return this.routers.get(roomId);
	}

	public async closeRouter(roomId: string): Promise<void> {
		const router = this.routers.get(roomId);
		if (router) {
			router.close();
			this.routers.delete(roomId);
			this.logger.info(`Router closed for room: ${roomId}`);
		}
	}

	private getNextWorker(): mediasoupTypes.Worker {
		const worker = this.workers[this.nextWorkerIndex];
		this.nextWorkerIndex = (this.nextWorkerIndex + 1) % this.workers.length;
		return worker;
	}

	public getWorkers(): mediasoupTypes.Worker[] {
		return this.workers;
	}

	public async close(): Promise<void> {
		this.logger.info("Closing all mediasoup workers...");

		for (const router of this.routers.values()) {
			router.close();
		}
		this.routers.clear();

		for (const worker of this.workers) {
			worker.close();
		}
		this.workers = [];

		this.logger.info("All mediasoup workers closed");
	}
}
