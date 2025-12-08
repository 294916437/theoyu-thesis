import { Logger } from "./logger";

/**
 * 全局错误处理器
 */
export class GlobalErrorHandler {
	private static instance: GlobalErrorHandler;
	private logger = new Logger("GlobalErrorHandler");

	private constructor() {
		this.setupProcessErrorHandlers();
	}

	public static getInstance(): GlobalErrorHandler {
		if (!GlobalErrorHandler.instance) {
			GlobalErrorHandler.instance = new GlobalErrorHandler();
		}
		return GlobalErrorHandler.instance;
	}

	/**
	 * 设置进程级错误处理器
	 */
	private setupProcessErrorHandlers(): void {
		// 捕获未处理的 Promise rejection
		process.on("unhandledRejection", (reason: any, promise: Promise<any>) => {
			this.logger.error("Unhandled Promise Rejection:", {
				reason: reason,
				promise: promise,
				stack: reason?.stack,
			});
		});

		// 捕获未捕获的异常
		process.on("uncaughtException", (error: Error) => {
			this.logger.error("Uncaught Exception:", {
				message: error.message,
				stack: error.stack,
				name: error.name,
			});

			// 给进程一些时间来记录错误，然后退出
			// 注意：uncaughtException 后应该重启进程
			setTimeout(() => {
				this.logger.error("Process will exit due to uncaught exception");
				process.exit(1);
			}, 1000);
		});

		// 捕获警告
		process.on("warning", (warning: Error) => {
			this.logger.warn("Process Warning:", {
				name: warning.name,
				message: warning.message,
				stack: warning.stack,
			});
		});

		// 捕获 SIGTERM 信号（优雅关闭）
		process.on("SIGTERM", () => {
			this.logger.info("SIGTERM received, preparing for graceful shutdown");
		});

		// 捕获 SIGINT 信号（Ctrl+C）
		process.on("SIGINT", () => {
			this.logger.info("SIGINT received, preparing for graceful shutdown");
		});

		this.logger.info("Global error handlers initialized");
	}

	/**
	 * 异步错误包装器
	 */
	public static wrapAsync<T>(
		fn: (...args: any[]) => Promise<T>
	): (...args: any[]) => Promise<T> {
		return async (...args: any[]): Promise<T> => {
			try {
				return await fn(...args);
			} catch (error) {
				const logger = new Logger("AsyncWrapper");
				logger.error("Async function error:", error);
				throw error;
			}
		};
	}

	/**
	 * 同步错误包装器
	 */
	public static wrapSync<T>(fn: (...args: any[]) => T): (...args: any[]) => T {
		return (...args: any[]): T => {
			try {
				return fn(...args);
			} catch (error) {
				const logger = new Logger("SyncWrapper");
				logger.error("Sync function error:", error);
				throw error;
			}
		};
	}

	/**
	 * 安全执行异步函数
	 */
	public static async safeExecute<T>(
		fn: () => Promise<T>,
		fallback?: T,
		errorHandler?: (error: any) => void
	): Promise<T | undefined> {
		try {
			return await fn();
		} catch (error) {
			const logger = new Logger("SafeExecute");
			logger.error("Safe execute error:", error);

			if (errorHandler) {
				errorHandler(error);
			}

			return fallback;
		}
	}
}
