import { Socket } from "socket.io"
import { Logger } from "../utils/logger"
import { GrpcClient } from "../utils/grpc-client"

export interface Session {
	userId: string
	username: string
	roomId: string
	socketId: string
	permissions: string[]
	createdAt: number
	lastActivityAt: number
}

export class SessionManager {
	private logger = new Logger("SessionManager")
	private sessions: Map<string, Session> = new Map() // userId -> Session
	private socketToUser: Map<string, string> = new Map() // socketId -> userId
	private grpcClient = GrpcClient.getInstance()

	async createSession(
		socket: Socket,
		data: {
			roomId: string
			userId: string
			username: string
			token: string
		},
	): Promise<Session> {
		const { roomId, userId, username, token } = data

		// 验证 Token
		const validation = await this.grpcClient.validateRoomAccess(roomId, userId, token)
		if (!validation.allowed) {
			throw new Error(`Access denied: ${validation.message}`)
		}

		// 创建会话
		const session: Session = {
			userId,
			username,
			roomId,
			socketId: socket.id,
			permissions: ["publish", "subscribe"],
			createdAt: Date.now(),
			lastActivityAt: Date.now(),
		}

		// 清理旧会话（如果存在）
		const oldSession = this.sessions.get(userId)
		if (oldSession) {
			this.socketToUser.delete(oldSession.socketId)
			this.logger.info(`Replacing old session for user ${userId}`)
		}

		this.sessions.set(userId, session)
		this.socketToUser.set(socket.id, userId)

		this.logger.info(`Session created for user ${username} (${userId}) in room ${roomId}`)
		return session
	}

	getSession(socketId: string): Session | undefined {
		const userId = this.socketToUser.get(socketId)
		return userId ? this.sessions.get(userId) : undefined
	}

	getSessionByUserId(userId: string): Session | undefined {
		return this.sessions.get(userId)
	}

	hasPermission(socketId: string, permission: string): boolean {
		const session = this.getSession(socketId)
		return session ? session.permissions.includes(permission) : false
	}

	updateActivity(socketId: string): void {
		const session = this.getSession(socketId)
		if (session) {
			session.lastActivityAt = Date.now()
		}
	}

	destroySession(socketId: string): void {
		const userId = this.socketToUser.get(socketId)
		if (userId) {
			this.sessions.delete(userId)
			this.socketToUser.delete(socketId)
			this.logger.info(`Session destroyed for user ${userId}`)
		}
	}

	// 获取房间内所有会话
	getSessionsByRoom(roomId: string): Session[] {
		return Array.from(this.sessions.values()).filter((s) => s.roomId === roomId)
	}
}
