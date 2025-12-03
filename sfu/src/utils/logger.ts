export class Logger {
	constructor(private context: string) {}

	private formatMessage(level: string, message: string, data?: any): string {
		const timestamp = new Date().toISOString();
		let logMessage = `[${timestamp}] [${level}] [${this.context}] ${message}`;

		if (data) {
			if (data instanceof Error) {
				logMessage += `\n${data.stack}`;
			} else {
				logMessage += `\n${JSON.stringify(data, null, 2)}`;
			}
		}

		return logMessage;
	}

	info(message: string, data?: any): void {
		console.log(this.formatMessage("INFO", message, data));
	}

	warn(message: string, data?: any): void {
		console.warn(this.formatMessage("WARN", message, data));
	}

	error(message: string, error?: any): void {
		console.error(this.formatMessage("ERROR", message, error));
	}

	debug(message: string, data?: any): void {
		if (process.env.NODE_ENV === "development") {
			console.debug(this.formatMessage("DEBUG", message, data));
		}
	}
}
