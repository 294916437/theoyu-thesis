/**
 * WebSocket 消息聊天服务(基于Stomp协议)
 */
import { Client } from '@stomp/stompjs'
import { reactive, ref } from 'vue'

class MessageService {
	constructor() {
		this.client = null
		this.isConnected = ref(false)
		this.userId = null
		this.eventHandlers = reactive({
			'message-receive': [], // 新消息到达
			'message-sent': [], // 消息发送成功
			'message-error': [], // 消息发送失败
			'conversation-updated': [], // 会话信息更新
		})
		this.reconnectAttempts = 0
		this.maxReconnectAttempts = 5
	}

	/**
	 * 连接到消息服务器
	 * @param {string} url - HTTP 服务器地址
	 * @param {number} userId - 当前用户ID
	 */
	connect(url, userId) {
		this.userId = userId
		return new Promise((resolve, reject) => {
			// 将 HTTP URL 转换为 WebSocket URL
			const wsUrl = url.replace(/^http/, 'ws')

			// 创建 STOMP 客户端
			this.client = new Client({
				brokerURL: `${wsUrl}?userId=${userId}`,

				// 心跳配置（毫秒）
				heartbeatIncoming: 4000,
				heartbeatOutgoing: 4000,

				// 自动重连配置
				reconnectDelay: 5000,

				// 调试日志
				debug: str => {
					if (import.meta.env.DEV) {
						console.log('[消息服务 STOMP]', str)
					}
				},

				// 连接成功回调
				onConnect: frame => {
					this.isConnected.value = true
					this.reconnectAttempts = 0
					console.log('消息服务 WebSocket 连接成功')

					// 订阅个人消息队列
					this.subscribeToMessageQueues()

					resolve(frame)
				},

				// STOMP 错误回调
				onStompError: frame => {
					console.error('消息服务 STOMP 错误:', frame.headers['message'])
					this.isConnected.value = false
					reject(new Error(frame.headers['message'] || '消息服务连接失败'))
				},

				// WebSocket 错误回调
				onWebSocketError: event => {
					console.error('消息服务 WebSocket 错误:', event)
					this.isConnected.value = false
					reject(new Error('消息服务 WebSocket 连接失败'))
				},

				// 断开连接回调
				onDisconnect: () => {
					this.isConnected.value = false
					console.log('🔌 消息服务 WebSocket 连接断开')
				},

				// WebSocket 关闭回调
				onWebSocketClose: event => {
					console.log('🔒 消息服务 WebSocket 关闭:', event.code, event.reason)
				},
			})

			// 激活连接
			this.client.activate()
		})
	}

	/**
	 * 订阅用户消息队列
	 */
	subscribeToMessageQueues() {
		console.log('📡 ========== 开始订阅消息队列 ==========')
		console.log('当前用户ID:', this.userId)

		const subscriptions = [
			{ destination: '/user/queue/message-receive', type: 'message-receive' },
			{ destination: '/user/queue/message-sent', type: 'message-sent' },
			{ destination: '/user/queue/message-error', type: 'message-error' },
			{
				destination: '/user/queue/conversation-updated',
				type: 'conversation-updated',
			},
		]

		subscriptions.forEach(({ destination, type }) => {
			try {
				this.client.subscribe(destination, message => {
					let data
					try {
						data = typeof message.body === 'string' ? JSON.parse(message.body) : message.body
					} catch (e) {
						console.error('解析消息失败:', e)
						data = message.body
					}

					this.handleMessage({ type, data })
				})
			} catch (error) {
				console.error(`订阅失败: ${destination}`, error)
			}
		})
	}

	/**
	 * 处理接收到的消息
	 */
	handleMessage(message) {
		console.log(' ========== handleMessage 被调用 ==========')

		const handlers = this.eventHandlers[message.type] || []

		handlers.forEach((handler, index) => {
			try {
				handler(message.data)
			} catch (error) {
				console.error(`监听器 #${index + 1} 执行失败:`, error)
			}
		})
	}

	/**
	 * 注册事件监听器
	 */
	on(eventType, handler) {
		if (!this.eventHandlers[eventType]) {
			this.eventHandlers[eventType] = []
		}

		this.eventHandlers[eventType].push(handler)
		// 返回取消订阅函数
		return () => this.off(eventType, handler)
	}

	/**
	 * 移除事件监听器
	 */
	off(eventType, handler) {
		if (this.eventHandlers[eventType]) {
			const index = this.eventHandlers[eventType].indexOf(handler)
			if (index > -1) {
				this.eventHandlers[eventType].splice(index, 1)
				console.log(`移除监听器: ${eventType}`)
			}
		}
	}

	/**
	 * 发送消息到服务器
	 * @param {string} destination - 目的地
	 * @param {object} body - 消息体
	 */
	send(destination, body) {
		if (!this.isConnected.value || !this.client) {
			console.error('消息服务未连接')
			throw new Error('消息服务未连接')
		}

		try {
			this.client.publish({
				destination,
				body: JSON.stringify({
					...body,
					timestamp: Date.now(),
				}),
			})
		} catch (error) {
			console.error('发送消息失败:', error)
			throw error
		}
	}

	/**
	 * 发送文本消息
	 * @param {number} conversationId - 会话ID
	 * @param {string} content - 消息内容
	 */
	sendTextMessage(conversationId, content) {
		this.send(`/app/chat/${conversationId}/send`, {
			messageType: 1, // 文本消息
			content: content.trim(),
		})
	}

	/**
	 * 发送图片消息
	 * @param {number} conversationId - 会话ID
	 * @param {string[]} imgUris - 图片URL数组
	 */
	sendImageMessage(conversationId, imgUris) {
		this.send(`/app/chat/${conversationId}/send`, {
			messageType: 2, // 图片消息
			imgUris,
		})
	}

	/**
	 * 发送视频消息
	 * @param {number} conversationId - 会话ID
	 * @param {string} videoUri - 视频URL
	 */
	sendVideoMessage(conversationId, videoUri) {
		this.send(`/app/chat/${conversationId}/send`, {
			messageType: 4, // 视频消息
			videoUri,
		})
	}

	/**
	 * 发送文件消息
	 * @param {number} conversationId - 会话ID
	 * @param {string} videoUri - 视频URL
	 */
	sendFileMessage(conversationId, content) {
		this.send(`/app/chat/${conversationId}/send`, {
			messageType: 6, // 文件消息
			content,
		})
	}

	/**
	 * 断开连接
	 */
	disconnect() {
		if (this.client) {
			this.client.deactivate()
			this.client = null
			this.isConnected.value = false

			// 清空事件监听器
			Object.keys(this.eventHandlers).forEach(key => {
				this.eventHandlers[key] = []
			})
		}
	}

	/**
	 * 获取连接状态
	 */
	getConnectionState() {
		return this.isConnected.value
	}

	/**
	 * 获取连接类型
	 */
	getConnectionType() {
		return 'websocket-chat'
	}
}

export default new MessageService()
