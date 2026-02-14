import * as Minio from "minio"
import { Logger } from "./logger"
import config from "../config/config"

export class MinioClient {
	private static instance: MinioClient
	private client: Minio.Client
	private logger = new Logger("MinioClient")

	private constructor() {
		this.client = new Minio.Client({
			endPoint: config.minio.endPoint,
			port: config.minio.port,
			useSSL: config.minio.useSSL,
			accessKey: config.minio.accessKey,
			secretKey: config.minio.secretKey,
		})
	}

	public static getInstance(): MinioClient {
		if (!MinioClient.instance) {
			MinioClient.instance = new MinioClient()
		}
		return MinioClient.instance
	}

	public async uploadFile(bucketName: string, objectName: string, filePath: string): Promise<void> {
		try {
			// 确保 bucket 存在
			const bucketExists = await this.client.bucketExists(bucketName)
			if (!bucketExists) {
				await this.client.makeBucket(bucketName, "us-east-1")
				this.logger.info(`Bucket created: ${bucketName}`)
			}

			// 上传文件
			await this.client.fPutObject(bucketName, objectName, filePath, {
				"Content-Type": "video/mp4",
			})

			this.logger.info(`File uploaded successfully: ${bucketName}/${objectName}`)
		} catch (error) {
			this.logger.error(`Failed to upload file to MinIO`, error)
			throw error
		}
	}

	public async getFileUrl(bucketName: string, objectName: string): Promise<string> {
		try {
			// 生成预签名 URL（有效期 7 天）
			const url = await this.client.presignedGetObject(bucketName, objectName, 7 * 24 * 60 * 60)
			return url
		} catch (error) {
			this.logger.error(`Failed to get file URL from MinIO`, error)
			throw error
		}
	}
}
