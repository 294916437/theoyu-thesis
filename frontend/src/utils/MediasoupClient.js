import * as mediasoupClient from 'mediasoup-client'
import { socketClient } from './SocketClient'

export class MediasoupClient {
	constructor() {
		this.device = null
		this.sendTransport = null
		this.recvTransport = null
		this.producers = new Map() // kind -> Producer
		this.consumers = new Map() // consumerId -> Consumer
	}

	async loadDevice(routerRtpCapabilities) {
		try {
			this.device = new mediasoupClient.Device()
			await this.device.load({ routerRtpCapabilities })
			return this.device
		} catch (error) {
			console.error('Failed to load device', error)
			throw error
		}
	}

	async createSendTransport(roomId) {
		if (this.sendTransport) {
			console.warn('Send transport already exists')
			return this.sendTransport
		}

		try {
			const transportInfo = await socketClient.emit('createWebRtcTransport', {
				roomId,
				producing: true,
				consuming: false,
			})

			this.sendTransport = this.device.createSendTransport(transportInfo)

			this.sendTransport.on('connect', async ({ dtlsParameters }, callback, errback) => {
				try {
					console.log('Send transport connecting...')

					await socketClient.emit('connectWebRtcTransport', {
						roomId,
						transportId: this.sendTransport.id,
						dtlsParameters,
					})

					console.log('Send transport connected successfully')
					callback()
				} catch (error) {
					console.error('Send transport connect failed:', error)
					errback(error)
				}
			})

			// 监听 produce 事件
			this.sendTransport.on('produce', async ({ kind, rtpParameters, appData }, callback, errback) => {
				try {
					const response = await socketClient.emit('produce', {
						roomId,
						transportId: this.sendTransport.id,
						kind,
						rtpParameters,
						appData,
					})

					callback({ id: response.id })
				} catch (error) {
					errback(error)
				}
			})

			// 添加详细的连接状态监听
			this.sendTransport.on('connectionstatechange', state => {
				console.log(`Send transport connection state: ${state}`)

				if (state === 'failed') {
					console.error('Send transport connection failed')
					// 触发重连逻辑
					this.handleTransportFailure('send')
				}

				if (state === 'disconnected') {
					console.warn('Send transport disconnected')
				}
			})

			// 添加 ICE 状态监听
			this.sendTransport.on('icestatechange', state => {
				console.log(`Send transport ICE state: ${state}`)
			})

			return this.sendTransport
		} catch (error) {
			console.error('Failed to create send transport', error)
			throw error
		}
	}

	async createRecvTransport(roomId) {
		if (this.recvTransport) {
			console.warn('Recv transport already exists')
			return this.recvTransport
		}

		try {
			const transportInfo = await socketClient.emit('createWebRtcTransport', {
				roomId,
				producing: false,
				consuming: true,
			})

			console.log('Creating recv transport', transportInfo)

			this.recvTransport = this.device.createRecvTransport({
				id: transportInfo.id,
				iceParameters: transportInfo.iceParameters,
				iceCandidates: transportInfo.iceCandidates,
				dtlsParameters: transportInfo.dtlsParameters,
				sctpParameters: transportInfo.sctpParameters,
			})

			this.recvTransport.on('connect', async ({ dtlsParameters }, callback, errback) => {
				try {
					console.log('Recv transport connecting', dtlsParameters)
					await socketClient.emit('connectWebRtcTransport', {
						roomId,
						transportId: this.recvTransport.id,
						dtlsParameters,
					})
					callback()
				} catch (error) {
					console.error('Recv transport connect failed', error)
					errback(error)
				}
			})

			this.recvTransport.on('connectionstatechange', state => {
				console.log('Recv transport connection state', state)
				if (state === 'failed' || state === 'closed') {
					this.recvTransport = null
				}
			})

			return this.recvTransport
		} catch (error) {
			console.error('Failed to create recv transport', error)
			throw error
		}
	}

	async produce(track, appData = {}) {
		if (!this.sendTransport) {
			throw new Error('Send transport not created')
		}

		try {
			const producer = await this.sendTransport.produce({
				track,
				...appData,
			})

			this.producers.set(track.kind, producer)

			producer.on('transportclose', () => {
				console.log('Producer transport closed', track.kind)
				this.producers.delete(track.kind)
			})

			producer.on('trackended', () => {
				console.log('Producer track ended', track.kind)
				this.closeProducer(track.kind)
			})

			console.log('Producer created', track.kind, producer.id)
			return producer
		} catch (error) {
			console.error('Failed to produce', error)
			throw error
		}
	}

	async consume(roomId, producerId, peerId) {
		if (!this.recvTransport) {
			throw new Error('Recv transport not created')
		}

		try {
			const { id, kind, rtpParameters } = await socketClient.emit('consume', {
				roomId,
				producerId,
				rtpCapabilities: this.device.rtpCapabilities,
			})

			const consumer = await this.recvTransport.consume({
				id,
				producerId,
				kind,
				rtpParameters,
			})

			this.consumers.set(id, { consumer, peerId, producerId })

			consumer.on('transportclose', () => {
				console.log('Consumer transport closed', id)
				this.consumers.delete(id)
			})

			consumer.on('trackended', () => {
				console.log('Consumer track ended', id)
				this.closeConsumer(id)
			})

			// Resume consumer
			await socketClient.emit('resumeConsumer', {
				roomId,
				consumerId: id,
			})

			console.log('Consumer created', kind, id)
			return consumer
		} catch (error) {
			console.error('Failed to consume', error)
			throw error
		}
	}

	async pauseProducer(kind) {
		const producer = this.producers.get(kind)
		if (producer && !producer.paused) {
			await producer.pause()
			console.log('Producer paused', kind)
		}
	}

	async resumeProducer(kind) {
		const producer = this.producers.get(kind)
		if (producer && producer.paused) {
			await producer.resume()
			console.log('Producer resumed', kind)
		}
	}

	async closeProducer(kind) {
		const producer = this.producers.get(kind)
		if (producer) {
			producer.close()
			this.producers.delete(kind)
			console.log('Producer closed', kind)
		}
	}

	async closeConsumer(consumerId) {
		const consumerData = this.consumers.get(consumerId)
		if (consumerData) {
			consumerData.consumer.close()
			this.consumers.delete(consumerId)
			console.log('Consumer closed', consumerId)
		}
	}

	close() {
		console.log('Closing mediasoup client')

		this.producers.forEach(producer => producer.close())
		this.producers.clear()

		this.consumers.forEach(({ consumer }) => consumer.close())
		this.consumers.clear()

		this.sendTransport?.close()
		this.recvTransport?.close()

		this.sendTransport = null
		this.recvTransport = null
		this.device = null
	}
}
