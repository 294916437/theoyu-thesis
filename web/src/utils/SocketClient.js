import { io } from 'socket.io-client'
import { ref } from 'vue'

class SocketClient {
	constructor() {
		this.socket = null
		this.connected = ref(false)
		this.reconnecting = ref(false)
		this.eventHandlers = new Map()
		this.lifecycleHandlers = new Map()
	}

	connect(url, options = {}) {
		if (this.socket?.connected) {
			console.warn('Socket already connected')
			return Promise.resolve()
		}
		if (this.socket) {
			this.disconnect()
		}

		return new Promise((resolve, reject) => {
			let settled = false
			const resolveOnce = value => {
				if (settled) return
				settled = true
				resolve(value)
			}
			const rejectOnce = error => {
				if (settled) return
				settled = true
				reject(error)
			}

			this.socket = io(url, {
				transports: ['websocket', 'polling'],
				reconnection: true,
				reconnectionDelay: 1000,
				reconnectionDelayMax: 5000,
				reconnectionAttempts: 5,
				...options,
			})

			this.socket.on('connect', () => {
				console.log('Socket connected', this.socket.id)
				this.connected.value = true
				this.reconnecting.value = false
				resolveOnce()
			})

			this.socket.on('connect_error', error => {
				console.error('Socket connect error', error)
				this.connected.value = false
				rejectOnce(error)
			})

			this.socket.on('disconnect', reason => {
				console.log('Socket disconnected', reason)
				this.connected.value = false

				if (reason === 'io server disconnect') {
					this.socket.connect()
				}
			})

			this.socket.io.on('reconnect_attempt', attemptNumber => {
				console.log('Socket reconnecting attempt', attemptNumber)
				this.reconnecting.value = true
			})

			this.socket.io.on('reconnect', attemptNumber => {
				console.log('Socket reconnected', attemptNumber)
				this.connected.value = true
				this.reconnecting.value = false
			})

			this.socket.io.on('reconnect_error', error => {
				console.error('Socket reconnect error', error)
				this.connected.value = false
				this.reconnecting.value = true
			})

			this.socket.io.on('reconnect_failed', () => {
				console.error('Socket reconnection failed')
				this.reconnecting.value = false
			})

			this.socket.on('error', error => {
				console.error('Socket error', error)
				rejectOnce(error)
			})
		})
	}

	disconnect() {
		if (this.socket) {
			this.socket.disconnect()
			this.socket = null
			this.connected.value = false
			this.reconnecting.value = false
			this.eventHandlers.clear()
			this.lifecycleHandlers.clear()
		}
	}

	emit(event, data) {
		return new Promise((resolve, reject) => {
			if (!this.socket?.connected) {
				reject(new Error('Socket not connected'))
				return
			}

			const timeout = setTimeout(() => {
				reject(new Error(`Event ${event} timeout`))
			}, 10000)

			this.socket.emit(event, data, response => {
				clearTimeout(timeout)

				if (response?.error) {
					reject(new Error(response.error))
				} else {
					resolve(response)
				}
			})
		})
	}

	on(event, handler) {
		if (!this.eventHandlers.has(event)) {
			this.eventHandlers.set(event, new Set())
		}

		this.eventHandlers.get(event).add(handler)
		this.socket?.on(event, handler)

		return () => this.off(event, handler)
	}

	off(event, handler) {
		const handlers = this.eventHandlers.get(event)
		if (handlers) {
			handlers.delete(handler)
			if (handlers.size === 0) {
				this.eventHandlers.delete(event)
			}
		}
		this.socket?.off(event, handler)
	}

	onManager(event, handler) {
		if (!this.lifecycleHandlers.has(event)) {
			this.lifecycleHandlers.set(event, new Set())
		}

		this.lifecycleHandlers.get(event).add(handler)
		this.socket?.io?.on(event, handler)

		return () => this.offManager(event, handler)
	}

	offManager(event, handler) {
		const handlers = this.lifecycleHandlers.get(event)
		if (handlers) {
			handlers.delete(handler)
			if (handlers.size === 0) {
				this.lifecycleHandlers.delete(event)
			}
		}
		this.socket?.io?.off(event, handler)
	}

	once(event, handler) {
		this.socket?.once(event, handler)
	}
}

export const socketClient = new SocketClient()
