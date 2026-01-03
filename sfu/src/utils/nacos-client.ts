import { NacosNamingClient } from "nacos-naming";
import config from "../config/config";
import { Logger } from "./logger";

export class NacosClient {
	private static instance: NacosClient;
	private client: NacosNamingClient | null = null;
	private logger = new Logger("NacosClient");
	private registered = false;

	private constructor() {}

	public static getInstance(): NacosClient {
		if (!NacosClient.instance) {
			NacosClient.instance = new NacosClient();
		}
		return NacosClient.instance;
	}

	public async init(): Promise<void> {
		try {
			this.client = new NacosNamingClient({
				serverList: config.nacos.serverList,
				namespace: config.nacos.namespace,
				logger: console,
			});

			await this.client.ready();
			this.logger.info("Nacos client initialized successfully");
		} catch (error) {
			this.logger.error("Failed to initialize Nacos client", error);
			throw error;
		}
	}

	public async registerService(): Promise<void> {
		if (!this.client) {
			throw new Error("Nacos client not initialized");
		}

		try {
			await this.client.registerInstance(config.nacos.serviceName, {
				ip: config.nacos.ip,
				port: config.nacos.port,
				instanceId: `${config.nacos.ip}:${config.nacos.port}`,
				healthy: true,
				enabled: true,
			});

			this.registered = true;
			this.logger.info(
				`Service registered: ${config.nacos.serviceName} at ${config.nacos.ip}:${config.nacos.port}`
			);

			// 定期发送心跳
			this.startHeartbeat();
		} catch (error) {
			this.logger.error("Failed to register service", error);
			throw error;
		}
	}

	private startHeartbeat(): void {
		setInterval(async () => {
			if (this.client && this.registered) {
				try {
					await this.client.registerInstance(config.nacos.serviceName, {
						ip: config.nacos.ip,
						port: config.nacos.port,
						instanceId: `${config.nacos.ip}:${config.nacos.port}`,
						healthy: true,
						enabled: true,
					});
					this.logger.debug("Heartbeat sent to Nacos");
				} catch (error) {
					this.logger.error("Failed to send heartbeat", error);
				}
			}
		}, 180000); // 每180s发送一次心跳
	}

	public async deregisterService(): Promise<void> {
		if (!this.client || !this.registered) {
			return;
		}

		try {
			await this.client.deregisterInstance(config.nacos.serviceName, {
				ip: config.nacos.ip,
				port: config.nacos.port,
				instanceId: `${config.nacos.ip}:${config.nacos.port}`,
				healthy: true,
				enabled: true,
			});

			this.registered = false;
			this.logger.info("Service deregistered from Nacos");
		} catch (error) {
			this.logger.error("Failed to deregister service", error);
			throw error;
		}
	}

	public async close(): Promise<void> {
		await this.deregisterService();
		if (this.client) {
			this.client.unSubscribe(config.nacos.serviceName, () => {});
			this.logger.info("Nacos client closed");
		}
	}
}
