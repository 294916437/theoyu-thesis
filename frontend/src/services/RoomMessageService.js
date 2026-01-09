/**
 * WebSocket 房间消息服务(基于Stomp协议)
 * 用于直播间、聊天室等多人实时通信场景
 */
import { Client } from '@stomp/stompjs'
import { reactive, ref } from 'vue'

class RoomMessageService {
	constructor() {
		this.client = null
		this.isConnected = ref(false)
		this.userId = null
		this.currentRoomId = null
		this.roomSubscription = null // 当前房间订阅对象

		// 事件处理器
		this.eventHandlers = reactive({
			'room-message': [], // 收到房间消息
			'room-joined': [], // 加入房间成功
			'room-left': [], // 离开房间
			'connection-error': [], // 连接错误
		})

		this.reconnectAttempts = 0
		this.maxReconnectAttempts = 5
	}

	/**
	 * 连接到房间消息服务器
	 * @param {string} url - HTTP 服务器地址
	 * @param {number} userId - 当前用户ID
	 * @param {number} roomId - 房间ID
	 */
	connect(url, userId, roomId) {
		this.userId = userId
		this.currentRoomId = roomId

		return new Promise((resolve, reject) => {
			// 将 HTTP URL 转换为 WebSocket URL
			const wsUrl = url.replace(/^http/, 'ws')

			// 创建 STOMP 客户端
			this.client = new Client({
				brokerURL: `${wsUrl}?userId=${userId}&roomId=${roomId}`,

				// 心跳配置（毫秒）
				heartbeatIncoming: 4000,
				heartbeatOutgoing: 4000,

				// 自动重连配置
				reconnectDelay: 5000,

				// 调试日志
				debug: str => {
					if (import.meta.env.DEV) {
						console.log('[房间消息服务 STOMP]', str)
					}
				},

				// 连接成功回调
				onConnect: frame => {
					this.isConnected.value = true
					this.reconnectAttempts = 0
					console.log('房间消息服务 WebSocket 连接成功')
					console.log('用户ID:', userId, '| 房间ID:', roomId)

					// 订阅房间消息
					this.subscribeToRoom(roomId)

					// 触发加入房间事件
					this.triggerEvent('room-joined', { roomId, userId })

					resolve(frame)
				},

				// STOMP 错误回调
				onStompError: frame => {
					console.error('房间消息服务 STOMP 错误:', frame.headers['message'])
					this.isConnected.value = false
					this.triggerEvent('connection-error', {
						type: 'stomp',
						message: frame.headers['message'],
					})
					reject(new Error(frame.headers['message'] || '房间消息服务连接失败'))
				},

				// WebSocket 错误回调
				onWebSocketError: event => {
					console.error('房间消息服务 WebSocket 错误:', event)
					this.isConnected.value = false
					this.triggerEvent('connection-error', { type: 'websocket', event })
					reject(new Error('房间消息服务 WebSocket 连接失败'))
				},

				// 断开连接回调
				onDisconnect: () => {
					this.isConnected.value = false
					console.log('房间消息服务 WebSocket 连接断开')
					if (this.currentRoomId) {
						this.triggerEvent('room-left', {
							roomId: this.currentRoomId,
							userId: this.userId,
						})
					}
				},

				// WebSocket 关闭回调
				onWebSocketClose: event => {
					console.log('房间消息服务 WebSocket 关闭:', event.code, event.reason)
				},
			})

			// 激活连接
			this.client.activate()
		})
	}

	/**
	 * 订阅房间消息
	 * @param {number} roomId - 房间ID
	 */
	subscribeToRoom(roomId) {
		if (!this.client || !this.isConnected.value) {
			console.error('客户端未连接,无法订阅房间')
			return
		}

		// 如果已有订阅,先取消
		if (this.roomSubscription) {
			this.roomSubscription.unsubscribe()
			console.log(`取消订阅房间: ${this.currentRoomId}`)
		}

		// 订阅新房间
		const destination = `/topic/room/${roomId}`
		console.log('========== 开始订阅房间消息 ==========')
		console.log('订阅地址:', destination)

		try {
			this.roomSubscription = this.client.subscribe(destination, message => {
				console.log('收到房间消息:', message)

				let data
				try {
					data = typeof message.body === 'string' ? JSON.parse(message.body) : message.body
					console.log('解析后的消息数据:', data)
				} catch (e) {
					console.error('解析消息失败:', e)
					data = message.body
				}

				// 触发消息接收事件
				this.triggerEvent('room-message', data)
			})

			this.currentRoomId = roomId
			console.log(`订阅房间成功: ${roomId}`)
		} catch (error) {
			console.error(`订阅房间失败: ${roomId}`, error)
		}
	}

	/**
	 * 切换房间
	 * @param {number} newRoomId - 新房间ID
	 */
	switchRoom(newRoomId) {
		console.log(`切换房间: ${this.currentRoomId} → ${newRoomId}`)

		if (this.currentRoomId) {
			this.triggerEvent('room-left', {
				roomId: this.currentRoomId,
				userId: this.userId,
			})
		}

		this.subscribeToRoom(newRoomId)

		this.triggerEvent('room-joined', {
			roomId: newRoomId,
			userId: this.userId,
		})
	}

	/**
	 * 触发事件
	 * @param {string} eventType - 事件类型
	 * @param {any} data - 事件数据
	 */
	triggerEvent(eventType, data) {
		const handlers = this.eventHandlers[eventType] || []

		if (handlers.length === 0) {
			console.warn(`没有为 ${eventType} 注册监听器`)
			return
		}

		console.log(`触发事件: ${eventType}, 监听器数量: ${handlers.length}`)

		handlers.forEach((handler, index) => {
			try {
				handler(data)
			} catch (error) {
				console.error(`监听器 #${index + 1} 执行失败:`, error)
			}
		})
	}

	/**
	 * 注册事件监听器
	 * @param {string} eventType - 事件类型
	 * @param {Function} handler - 处理函数
	 * @returns {Function} 取消订阅函数
	 */
	on(eventType, handler) {
		console.log('注册事件监听器:', eventType)

		if (!this.eventHandlers[eventType]) {
			this.eventHandlers[eventType] = []
		}

		this.eventHandlers[eventType].push(handler)
		console.log(`监听器已注册 - 总数: ${this.eventHandlers[eventType].length}`)

		// 返回取消订阅函数
		return () => this.off(eventType, handler)
	}

	/**
	 * 移除事件监听器
	 * @param {string} eventType - 事件类型
	 * @param {Function} handler - 处理函数
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
			console.error('房间消息服务未连接')
			throw new Error('房间消息服务未连接')
		}

		try {
			console.log('发送消息:', { destination, body })

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
	 * 发送文本消息到当前房间
	 * @param {string} content - 消息内容
	 * @param {number} contentType - 内容类型 (默认1-文本)
	 */
	sendTextMessage(content, contentType = 1) {
		if (!this.currentRoomId) {
			console.error('未加入任何房间')
			throw new Error('未加入任何房间')
		}

		this.send('/app/room/sendMessage', {
			roomId: this.currentRoomId,
			content: content.trim(),
			messageType: 2, // 2-用户消息
			contentType: contentType, // 1-文本
		})
	}

	/**
	 * 发送图片消息到当前房间
	 * @param {string} imageUrl - 图片URL
	 */
	sendImageMessage(imageUrl) {
		if (!this.currentRoomId) {
			console.error('未加入任何房间')
			throw new Error('未加入任何房间')
		}

		this.send('/app/room/sendMessage', {
			roomId: this.currentRoomId,
			content: imageUrl,
			messageType: 2, // 2-用户消息
			contentType: 2, // 2-图片
		})
	}

	/**
	 * 发送文件消息到当前房间
	 * @param {string} fileUrl - 文件URL
	 */
	sendFileMessage(fileUrl) {
		if (!this.currentRoomId) {
			console.error('未加入任何房间')
			throw new Error('未加入任何房间')
		}

		this.send('/app/room/sendMessage', {
			roomId: this.currentRoomId,
			content: fileUrl,
			messageType: 2, // 2-用户消息
			contentType: 3, // 3-文件
		})
	}

	/**
	 * 发送自定义消息
	 * @param {object} messageData - 完整消息数据
	 */
	sendCustomMessage(messageData) {
		this.send('/app/room/sendMessage', {
			roomId: this.currentRoomId,
			...messageData,
		})
	}

	/**
	 * 断开连接
	 */
	disconnect() {
		if (this.client) {
			console.log('断开房间消息服务连接')

			// 取消房间订阅
			if (this.roomSubscription) {
				this.roomSubscription.unsubscribe()
				this.roomSubscription = null
			}

			// 触发离开房间事件
			if (this.currentRoomId) {
				this.triggerEvent('room-left', {
					roomId: this.currentRoomId,
					userId: this.userId,
				})
			}

			// 停用客户端
			this.client.deactivate()
			this.client = null
			this.isConnected.value = false
			this.currentRoomId = null

			// 清空事件监听器
			Object.keys(this.eventHandlers).forEach(key => {
				this.eventHandlers[key] = []
			})

			console.log('房间消息服务已断开')
		}
	}

	/**
	 * 获取连接状态
	 * @returns {boolean}
	 */
	getConnectionState() {
		return this.isConnected.value
	}

	/**
	 * 获取当前房间ID
	 * @returns {number|null}
	 */
	getCurrentRoomId() {
		return this.currentRoomId
	}

	/**
	 * 获取用户ID
	 * @returns {number|null}
	 */
	getUserId() {
		return this.userId
	}

	/**
	 * 获取连接类型
	 * @returns {string}
	 */
	getConnectionType() {
		return 'websocket-room'
	}
}

// 导出单例
export default new RoomMessageService()
