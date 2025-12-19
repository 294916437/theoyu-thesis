import { io } from 'socket.io-client'

/**
 * Socket.IO 客户端封装类
 * 提供统一的 WebSocket 连接管理、事件处理和错误处理机制
 */
class SocketClient {
	/**
	 * 构造函数
	 * @param {Object} options - 配置选项
	 * @param {string} options.url - Socket.IO 服务器地址
	 * @param {Object} options.socketOptions - socket.io-client 原生配置
	 * @param {number} options.timeout - 请求超时时间(毫秒),默认 10000
	 * @param {boolean} options.autoConnect - 是否自动连接,默认 true
	 * @param {Function} options.onConnect - 连接成功回调
	 * @param {Function} options.onDisconnect - 断开连接回调
	 * @param {Function} options.onError - 错误回调
	 * @param {boolean} options.debug - 是否开启调试日志,默认 false
	 */
	constructor(options = {}) {
		this.url = options.url || 'http://localhost:3000'
		this.timeout = options.timeout || 10000
		this.debug = options.debug || false
		this.socket = null
		this.connected = false
		this.eventHandlers = new Map() // 存储事件处理器
		this.pendingRequests = new Map() // 存储待响应的请求
		this.requestIdCounter = 0

		// 回调函数
		this.onConnectCallback = options.onConnect || null
		this.onDisconnectCallback = options.onDisconnect || null
		this.onErrorCallback = options.onError || null

		// socket.io-client 配置
		this.socketOptions = {
			transports: ['websocket', 'polling'],
			reconnection: true,
			reconnectionAttempts: 5,
			reconnectionDelay: 1000,
			reconnectionDelayMax: 5000,
			timeout: 20000,
			...options.socketOptions,
		}

		// 自动连接
		if (options.autoConnect !== false) {
			this.connect()
		}
	}

	/**
	 * 连接到 Socket.IO 服务器
	 * @returns {Promise<void>}
	 */
	connect() {
		return new Promise((resolve, reject) => {
			try {
				this._log('正在连接到服务器:', this.url)

				this.socket = io(this.url, this.socketOptions)

				// 连接成功
				this.socket.on('connect', () => {
					this.connected = true
					this._log('连接成功, Socket ID:', this.socket.id)

					if (this.onConnectCallback) {
						this.onConnectCallback(this.socket.id)
					}

					resolve()
				})

				// 连接错误
				this.socket.on('connect_error', error => {
					this._log('连接错误:', error.message)

					if (this.onErrorCallback) {
						this.onErrorCallback(error)
					}

					reject(error)
				})

				// 断开连接
				this.socket.on('disconnect', reason => {
					this.connected = false
					this._log('连接断开:', reason)

					if (this.onDisconnectCallback) {
						this.onDisconnectCallback(reason)
					}

					// 清理待响应的请求
					this.pendingRequests.forEach(request => {
						clearTimeout(request.timeoutId)
						request.reject(new Error('连接已断开'))
					})
					this.pendingRequests.clear()
				})

				// 重连尝试
				this.socket.io.on('reconnect_attempt', attempt => {
					this._log(`重连尝试 ${attempt}/${this.socketOptions.reconnectionAttempts}`)
				})

				// 重连成功
				this.socket.io.on('reconnect', attempt => {
					this._log('重连成功,尝试次数:', attempt)
				})

				// 重连失败
				this.socket.io.on('reconnect_failed', () => {
					this._log('重连失败,已达到最大尝试次数')
					reject(new Error('重连失败'))
				})
			} catch (error) {
				this._log('连接异常:', error)
				reject(error)
			}
		})
	}

	/**
	 * 断开连接
	 */
	disconnect() {
		if (this.socket) {
			this._log('手动断开连接')
			this.socket.disconnect()
			this.connected = false
		}
	}

	/**
	 * 监听服务器事件
	 * @param {string} event - 事件名称
	 * @param {Function} handler - 事件处理函数
	 * @returns {Function} 返回取消监听的函数
	 */
	on(event, handler) {
		if (!this.socket) {
			throw new Error('Socket 未初始化')
		}

		this._log('注册事件监听:', event)

		// 包装处理函数以便于管理
		const wrappedHandler = (...args) => {
			this._log('收到事件:', event, args)
			handler(...args)
		}

		this.socket.on(event, wrappedHandler)

		// 存储处理器引用
		if (!this.eventHandlers.has(event)) {
			this.eventHandlers.set(event, [])
		}
		this.eventHandlers.get(event).push(wrappedHandler)

		// 返回取消监听函数
		return () => this.off(event, wrappedHandler)
	}

	/**
	 * 监听一次性事件
	 * @param {string} event - 事件名称
	 * @param {Function} handler - 事件处理函数
	 */
	once(event, handler) {
		if (!this.socket) {
			throw new Error('Socket 未初始化')
		}

		this._log('注册一次性事件监听:', event)

		const wrappedHandler = (...args) => {
			this._log('收到一次性事件:', event, args)
			handler(...args)
		}

		this.socket.once(event, wrappedHandler)
	}

	/**
	 * 取消事件监听
	 * @param {string} event - 事件名称
	 * @param {Function} handler - 要移除的处理函数,不传则移除所有
	 */
	off(event, handler) {
		if (!this.socket) {
			return
		}

		if (handler) {
			this._log('移除事件监听:', event)
			this.socket.off(event, handler)

			// 从存储中移除
			if (this.eventHandlers.has(event)) {
				const handlers = this.eventHandlers.get(event)
				const index = handlers.indexOf(handler)
				if (index > -1) {
					handlers.splice(index, 1)
				}
				if (handlers.length === 0) {
					this.eventHandlers.delete(event)
				}
			}
		} else {
			this._log('移除所有事件监听:', event)
			this.socket.off(event)
			this.eventHandlers.delete(event)
		}
	}

	/**
	 * 发送事件(不等待响应)
	 * @param {string} event - 事件名称
	 * @param {...any} args - 事件参数
	 */
	emit(event, ...args) {
		if (!this.socket) {
			throw new Error('Socket 未初始化')
		}

		if (!this.connected) {
			throw new Error('Socket 未连接')
		}

		this._log('发送事件:', event, args)
		this.socket.emit(event, ...args)
	}

	/**
	 * 发送请求并等待响应(Promise 模式)
	 * @param {string} event - 事件名称
	 * @param {any} data - 请求数据
	 * @param {number} timeout - 超时时间,默认使用实例配置
	 * @returns {Promise<any>} 响应数据
	 */
	request(event, data = {}, timeout = this.timeout) {
		return new Promise((resolve, reject) => {
			if (!this.socket) {
				return reject(new Error('Socket 未初始化'))
			}

			if (!this.connected) {
				return reject(new Error('Socket 未连接'))
			}

			// 生成唯一请求 ID
			const requestId = `req_${++this.requestIdCounter}_${Date.now()}`
			const responseEvent = `${event}_response_${requestId}`

			this._log('发送请求:', event, { requestId, data })

			// 设置超时
			const timeoutId = setTimeout(() => {
				this.socket.off(responseEvent)
				this.pendingRequests.delete(requestId)
				reject(new Error(`请求超时: ${event}`))
			}, timeout)

			// 存储待响应的请求
			this.pendingRequests.set(requestId, { resolve, reject, timeoutId })

			// 监听响应
			this.socket.once(responseEvent, response => {
				clearTimeout(timeoutId)
				this.pendingRequests.delete(requestId)

				this._log('收到响应:', event, response)

				if (response.error) {
					reject(new Error(response.error))
				} else {
					resolve(response.data)
				}
			})

			// 发送请求
			this.socket.emit(event, {
				requestId,
				data,
			})
		})
	}

	/**
	 * 使用 socket.io 原生的 emit with acknowledgment
	 * @param {string} event - 事件名称
	 * @param {any} data - 请求数据
	 * @param {number} timeout - 超时时间
	 * @returns {Promise<any>} 响应数据
	 */
	emitWithAck(event, data, timeout = this.timeout) {
		return new Promise((resolve, reject) => {
			if (!this.socket) {
				return reject(new Error('Socket 未初始化'))
			}

			if (!this.connected) {
				return reject(new Error('Socket 未连接'))
			}

			this._log('发送 ACK 请求:', event, data)

			// 设置超时
			const timeoutId = setTimeout(() => {
				reject(new Error(`请求超时: ${event}`))
			}, timeout)

			// 使用 socket.io 的 emit with callback
			this.socket.emit(event, data, response => {
				clearTimeout(timeoutId)

				this._log('收到 ACK 响应:', event, response)

				if (response && response.error) {
					reject(new Error(response.error))
				} else {
					resolve(response)
				}
			})
		})
	}

	/**
	 * 获取连接状态
	 * @returns {boolean}
	 */
	isConnected() {
		return this.connected && this.socket && this.socket.connected
	}

	/**
	 * 获取 Socket ID
	 * @returns {string|null}
	 */
	getSocketId() {
		return this.socket ? this.socket.id : null
	}

	/**
	 * 重新连接
	 * @returns {Promise<void>}
	 */
	reconnect() {
		this.disconnect()
		return this.connect()
	}

	/**
	 * 调试日志
	 * @private
	 */
	_log(...args) {
		if (this.debug) {
			console.log('[SocketClient]', ...args)
		}
	}

	/**
	 * 销毁实例,清理所有资源
	 */
	destroy() {
		this._log('销毁 Socket 实例')

		// 清理所有事件监听
		this.eventHandlers.forEach((handlers, event) => {
			this.off(event)
		})
		this.eventHandlers.clear()

		// 清理待响应的请求
		this.pendingRequests.forEach(request => {
			clearTimeout(request.timeoutId)
			request.reject(new Error('Socket 实例已销毁'))
		})
		this.pendingRequests.clear()

		// 断开连接
		this.disconnect()

		this.socket = null
		this.connected = false
	}
}

/**
 * 创建 Socket 客户端实例
 * @param {Object} options - 配置选项
 * @returns {SocketClient}
 */
export function createSocketClient(options) {
	return new SocketClient(options)
}

export default SocketClient
