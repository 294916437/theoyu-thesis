/**
 * WebRTC 信令服务(基于Stomp协议)
 */
import { Client } from '@stomp/stompjs'
import { reactive, ref } from 'vue'

class SignalingService {
	constructor() {
		this.client = null
		this.isConnected = ref(false)
		this.userId = null
		this.eventHandlers = reactive({
			'call-offer': [],
			'call-answer': [],
			'ice-candidate': [],
			'call-end': [],
			error: [],
		})
		this.reconnectAttempts = 0
		this.maxReconnectAttempts = 5
	}

	/**
	 * 连接到信令服务器（原生 WebSocket）
	 * @param {string} url - HTTP 服务器地址
	 * @param {number} userId - 当前用户ID
	 */
	connect(url, userId) {
		this.userId = userId
		return new Promise((resolve, reject) => {
			// 将 URL 转换为 WebSocket URL
			// 相对路径（/开头）：根据当前页面协议构建，走 Vite proxy 避免混合内容
			// 绝对路径（http/https）：直接替换协议前缀
			let wsUrl
			if (url.startsWith('/')) {
				const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
				wsUrl = `${protocol}//${window.location.host}${url}`
			} else {
				wsUrl = url.replace(/^https/, 'wss').replace(/^http(?!s)/, 'ws')
			}

			// 创建 STOMP 客户端
			this.client = new Client({
				// 使用原生 WebSocket
				brokerURL: `${wsUrl}?userId=${userId}`,

				// 心跳配置（毫秒）
				heartbeatIncoming: 4000,
				heartbeatOutgoing: 4000,

				// 自动重连配置
				reconnectDelay: 5000,

				// 调试日志
				debug: str => {
					if (import.meta.env.DEV) {
						console.log('[信令服务 STOMP]', str)
					}
				},

				// 连接成功回调
				onConnect: frame => {
					this.isConnected.value = true
					this.reconnectAttempts = 0
					console.log('原生 WebSocket 连接成功')

					// 订阅个人消息队列
					this.subscribeToUserQueues()

					resolve(frame)
				},

				// STOMP 错误回调
				onStompError: frame => {
					console.error('STOMP 错误:', frame.headers['message'])
					console.error('详细信息:', frame.body)
					this.isConnected.value = false
					reject(new Error(frame.headers['message'] || '信令服务器连接失败'))
				},

				// WebSocket 错误回调
				onWebSocketError: event => {
					console.error('WebSocket 错误:', event)
					this.isConnected.value = false
					reject(new Error('WebSocket 连接失败'))
				},

				// 断开连接回调
				onDisconnect: () => {
					this.isConnected.value = false
					console.log('WebSocket 连接断开')
				},

				// WebSocket 关闭回调
				onWebSocketClose: event => {
					console.log('WebSocket 关闭:', event.code, event.reason)
				},
			})

			// 激活连接
			this.client.activate()
		})
	}

	/**
	 * 订阅用户消息队列
	 */
	subscribeToUserQueues() {
		console.log('========== 开始订阅用户消息队列 ==========')
		console.log('当前用户ID:', this.userId)
		console.log('STOMP 客户端状态:', this.client?.connected)

		const subscriptions = [
			{ destination: '/user/queue/call-offer', type: 'call-offer' },
			{ destination: '/user/queue/call-answer', type: 'call-answer' },
			{ destination: '/user/queue/ice-candidate', type: 'ice-candidate' },
			{ destination: '/user/queue/call-end', type: 'call-end' },
			{ destination: '/user/queue/error', type: 'error' },
		]

		subscriptions.forEach(({ destination, type }) => {
			try {
				this.client.subscribe(destination, message => {
					console.log(`========== 收到原始消息 ==========`)

					let data
					try {
						data = typeof message.body === 'string' ? JSON.parse(message.body) : message.body
					} catch (e) {
						console.error('解析消息失败:', e)
						data = message.body
					}

					this.handleMessage({ type, data })
				})

				console.log(`订阅成功: ${destination}`)
			} catch (error) {
				console.error(`订阅失败: ${destination}`, error)
			}
		})
	}

	/**
	 * 处理接收到的消息
	 */
	handleMessage(message) {
		const handlers = this.eventHandlers[message.type] || []

		if (handlers.length === 0) {
			console.warn(`没有为 ${message.type} 注册监听器！`)
		}

		handlers.forEach((handler, index) => {
			try {
				console.log(`执行监听器 #${index + 1}`)
				handler(message.data)
				console.log(`监听器 #${index + 1} 执行成功`)
			} catch (error) {
				console.error(`监听器 #${index + 1} 执行失败:`, error)
			}
		})
	}

	/**
	 * 注册事件监听器
	 */
	on(eventType, handler) {
		console.log('========== 注册事件监听器 ==========')

		if (!this.eventHandlers[eventType]) {
			this.eventHandlers[eventType] = []
			console.log(`创建新的事件类型: ${eventType}`)
		}

		this.eventHandlers[eventType].push(handler)
		console.log(`监听器已注册 - 总数: ${this.eventHandlers[eventType].length}`)
	}

	/**
	 * 移除事件监听器
	 */
	off(eventType, handler) {
		if (this.eventHandlers[eventType]) {
			const index = this.eventHandlers[eventType].indexOf(handler)
			if (index > -1) {
				this.eventHandlers[eventType].splice(index, 1)
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
			throw new Error('信令服务器未连接')
		}

		try {
			this.client.publish({
				destination,
				body: JSON.stringify({
					...body,
					timestamp: Date.now(),
				}),
			})
			console.log(`📤 发送消息到 ${destination}`)
		} catch (error) {
			console.error('发送消息失败:', error)
			throw error
		}
	}

	/**
	 * 发送通话邀请
	 */
	sendCallOffer({ toUserId, offer }) {
		const callId = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`

		this.send('/app/call-offer', {
			type: 'call-offer',
			callId,
			toUserId,
			offer,
		})

		return callId
	}

	/**
	 * 发送通话应答
	 */
	sendCallAnswer({ callId, toUserId, answer }) {
		this.send('/app/call-answer', {
			type: 'call-answer',
			callId,
			toUserId,
			answer,
		})
	}

	/**
	 * 发送 ICE 候选
	 */
	sendIceCandidate({ callId, toUserId, candidate }) {
		this.send('/app/ice-candidate', {
			type: 'ice-candidate',
			callId,
			toUserId,
			candidate,
		})
	}

	/**
	 * 发送结束通话
	 */
	sendCallEnd({ callId, toUserId, reason = 'user-hangup' }) {
		this.send('/app/call-end', {
			type: 'call-end',
			callId,
			toUserId,
			reason,
		})
	}

	/**
	 * 断开连接
	 */
	disconnect() {
		if (this.client) {
			console.log('断开信令服务器连接')
			this.client.deactivate()
			this.client = null
			this.isConnected.value = false
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
		return 'websocket-signal'
	}
}

export default SignalingService
