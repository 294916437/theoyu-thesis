import { io } from 'socket.io-client'
import { ref, computed } from 'vue'
import { useEventListener } from '@vueuse/core'

class SocketClient {
	constructor() {
		this.socket = null
		this.connected = ref(false)
		this.reconnecting = ref(false)
		this.eventHandlers = new Map()
	}

	connect(url, options = {}) {
		if (this.socket?.connected) {
			console.warn('Socket already connected')
			return Promise.resolve()
		}

		return new Promise((resolve, reject) => {
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
				resolve()
			})

			this.socket.on('disconnect', reason => {
				console.log('Socket disconnected', reason)
				this.connected.value = false

				if (reason === 'io server disconnect') {
					this.socket.connect()
				}
			})

			this.socket.on('reconnecting', attemptNumber => {
				console.log('Socket reconnecting attempt', attemptNumber)
				this.reconnecting.value = true
			})

			this.socket.on('reconnect_failed', () => {
				console.error('Socket reconnection failed')
				this.reconnecting.value = false
				reject(new Error('Reconnection failed'))
			})

			this.socket.on('error', error => {
				console.error('Socket error', error)
				reject(error)
			})
		})
	}

	disconnect() {
		if (this.socket) {
			this.socket.disconnect()
			this.socket = null
			this.connected.value = false
			this.eventHandlers.clear()
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

	once(event, handler) {
		this.socket?.once(event, handler)
	}
}

export const socketClient = new SocketClient()
