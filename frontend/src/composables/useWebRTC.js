import { ref, computed, onUnmounted } from 'vue'
import { socketClient } from '@/utils/SocketClient'
import { MediasoupClient } from '@/utils/MediasoupClient'
import { $notify } from '@/plugins/notification'

export function useWebRTC() {
	// 状态管理
	const roomId = ref(null)
	const peerId = ref(null)
	const localStream = ref(null)
	const participants = ref([])
	const audioEnabled = ref(true)
	const videoEnabled = ref(true)
	const screenSharing = ref(false)
	const screenStream = ref(null)
	const connectionState = ref('disconnected') // disconnected | connecting | connected | failed

	// Mediasoup 客户端实例
	let mediasoupClient = null

	// 获取远程参与者列表
	const remoteParticipants = computed(() => participants.value.filter(p => p.peerId !== peerId.value))

	// 获取本地参与者
	const localParticipant = computed(() => participants.value.find(p => p.peerId === peerId.value))

	/**
	 * 加入房间
	 */
	async function joinMeeting(meetingId, userId, username, token) {
		try {
			connectionState.value = 'connecting'
			console.log('Joining meeting', meetingId, userId)

			// 1. 连接 Socket.io
			await socketClient.connect(import.meta.env.VITE_SFU_URL || 'http://localhost:3000', {
				auth: { token },
			})

			// 2. 加入房间
			const joinResponse = await socketClient.emit('joinRoom', {
				roomId: meetingId,
				userId,
				username,
				token,
			})

			roomId.value = meetingId
			peerId.value = joinResponse.peerId
			console.log('Joined room', joinResponse)

			// 3. 设置现有参与者
			participants.value = joinResponse.peers.map(peer => ({
				peerId: peer.id,
				userId: peer.userId,
				username: peer.username,
				streams: {},
				producers: {},
			}))

			// 4. 获取路由器 RTP 能力
			const { rtpCapabilities } = await socketClient.emit('getRouterRtpCapabilities', {
				roomId: meetingId,
			})

			// 5. 初始化 Mediasoup Device
			mediasoupClient = new MediasoupClient()
			await mediasoupClient.loadDevice(rtpCapabilities)

			// 6. 创建传输层
			await mediasoupClient.createSendTransport(meetingId)
			await mediasoupClient.createRecvTransport(meetingId)

			// 7. 获取本地媒体流
			await getLocalStream()

			// 8. 发布本地媒体流
			if (localStream.value) {
				const audioTrack = localStream.value.getAudioTracks()[0]
				const videoTrack = localStream.value.getVideoTracks()[0]

				if (audioTrack) {
					await mediasoupClient.produce(audioTrack, { kind: 'audio' })
				}

				if (videoTrack) {
					await mediasoupClient.produce(videoTrack, { kind: 'video' })
				}
			}

			// 9. 订阅现有参与者的媒体流
			for (const peer of joinResponse.peers) {
				for (const producer of peer.producers || []) {
					await consumeProducer(producer.id, peer.id)
				}
			}

			// 10. 监听事件
			setupSocketListeners()

			connectionState.value = 'connected'
			$notify.success(`已加入会议: ${username}`)
		} catch (error) {
			console.error('Failed to join meeting', error)
			connectionState.value = 'failed'
			$notify.error(`加入会议失败: ${error.message}`)
			throw error
		}
	}

	/**
	 * 获取本地媒体流
	 */
	async function getLocalStream() {
		try {
			const stream = await navigator.mediaDevices.getUserMedia({
				audio: {
					echoCancellation: true,
					noiseSuppression: true,
					autoGainControl: true,
				},
				video: {
					width: { ideal: 1280 },
					height: { ideal: 720 },
					frameRate: { ideal: 30 },
				},
			})

			localStream.value = stream
			console.log('Local stream acquired', stream.id)

			// 更新本地参与者流
			const localPeer = participants.value.find(p => p.peerId === peerId.value)
			if (localPeer) {
				localPeer.streams.local = stream
			}

			return stream
		} catch (error) {
			console.error('Failed to get local stream', error)
			$notify.error('无法获取摄像头/麦克风权限')
			throw error
		}
	}

	/**
	 * 订阅远程生产者
	 */
	async function consumeProducer(producerId, remotePeerId) {
		try {
			console.log('Consuming producer', producerId, remotePeerId)

			const consumer = await mediasoupClient.consume(roomId.value, producerId, remotePeerId)

			// 更新参与者流
			const participant = participants.value.find(p => p.peerId === remotePeerId)
			if (participant) {
				const kind = consumer.track.kind

				if (!participant.streams[kind]) {
					participant.streams[kind] = new MediaStream()
				}

				participant.streams[kind].addTrack(consumer.track)
				participant.producers[producerId] = consumer

				console.log('Consumer track added', remotePeerId, kind)
			}

			return consumer
		} catch (error) {
			console.error('Failed to consume producer', error)
		}
	}

	/**
	 * 设置 Socket 事件监听
	 */
	function setupSocketListeners() {
		// 新参与者加入
		socketClient.on('newPeer', async data => {
			console.log('New peer joined', data)

			participants.value.push({
				peerId: data.peerId,
				userId: data.userId,
				username: data.username,
				streams: {},
				producers: {},
			})

			$notify.info(`${data.username} 加入了会议`)
		})

		// 参与者离开
		socketClient.on('peerLeft', data => {
			console.log('Peer left', data)

			const index = participants.value.findIndex(p => p.peerId === data.peerId)
			if (index !== -1) {
				const participant = participants.value[index]

				// 清理流
				Object.values(participant.streams).forEach(stream => {
					stream.getTracks().forEach(track => track.stop())
				})

				participants.value.splice(index, 1)
				$notify.info(`${data.username} 离开了会议`)
			}
		})

		// 新生产者
		socketClient.on('newProducer', async data => {
			console.log('New producer', data)

			if (data.peerId !== peerId.value) {
				await consumeProducer(data.producerId, data.peerId)
			}
		})

		// 生产者关闭
		socketClient.on('producerClosed', data => {
			console.log('Producer closed', data)

			const participant = participants.value.find(p => p.peerId === data.peerId)
			if (participant && participant.producers[data.producerId]) {
				const consumer = participant.producers[data.producerId]
				consumer.close()
				delete participant.producers[data.producerId]

				// 从流中移除轨道
				const kind = data.kind
				if (participant.streams[kind]) {
					participant.streams[kind].getTracks().forEach(track => {
						if (track.id === consumer.track.id) {
							track.stop()
							participant.streams[kind].removeTrack(track)
						}
					})
				}
			}
		})

		// 生产者暂停/恢复
		socketClient.on('producerPaused', data => {
			console.log('Producer paused', data)
			updateProducerState(data.peerId, data.producerId, true)
		})

		socketClient.on('producerResumed', data => {
			console.log('Producer resumed', data)
			updateProducerState(data.peerId, data.producerId, false)
		})
	}

	/**
	 * 更新生产者状态
	 */
	function updateProducerState(remotePeerId, producerId, paused) {
		const participant = participants.value.find(p => p.peerId === remotePeerId)
		if (participant && participant.producers[producerId]) {
			const consumer = participant.producers[producerId]
			if (paused) {
				consumer.pause()
			} else {
				consumer.resume()
			}
		}
	}

	/**
	 * 切换音频
	 */
	async function toggleAudio() {
		try {
			if (audioEnabled.value) {
				await mediasoupClient.pauseProducer('audio')
				localStream.value?.getAudioTracks().forEach(track => (track.enabled = false))

				await socketClient.emit('pauseProducer', {
					roomId: roomId.value,
					producerId: mediasoupClient.producers.get('audio')?.id,
				})
			} else {
				await mediasoupClient.resumeProducer('audio')
				localStream.value?.getAudioTracks().forEach(track => (track.enabled = true))

				await socketClient.emit('resumeProducer', {
					roomId: roomId.value,
					producerId: mediasoupClient.producers.get('audio')?.id,
				})
			}

			audioEnabled.value = !audioEnabled.value
			console.log('Audio toggled', audioEnabled.value)
		} catch (error) {
			console.error('Failed to toggle audio', error)
		}
	}

	/**
	 * 切换视频
	 */
	async function toggleVideo() {
		try {
			if (videoEnabled.value) {
				await mediasoupClient.pauseProducer('video')
				localStream.value?.getVideoTracks().forEach(track => (track.enabled = false))

				await socketClient.emit('pauseProducer', {
					roomId: roomId.value,
					producerId: mediasoupClient.producers.get('video')?.id,
				})
			} else {
				await mediasoupClient.resumeProducer('video')
				localStream.value?.getVideoTracks().forEach(track => (track.enabled = true))

				await socketClient.emit('resumeProducer', {
					roomId: roomId.value,
					producerId: mediasoupClient.producers.get('video')?.id,
				})
			}

			videoEnabled.value = !videoEnabled.value
			console.log('Video toggled', videoEnabled.value)
		} catch (error) {
			console.error('Failed to toggle video', error)
		}
	}

	/**
	 * 开始屏幕共享
	 */
	async function startScreenShare() {
		try {
			const stream = await navigator.mediaDevices.getDisplayMedia({
				video: {
					cursor: 'always',
					displaySurface: 'monitor',
				},
				audio: false,
			})

			screenStream.value = stream
			const videoTrack = stream.getVideoTracks()[0]

			// 生产屏幕共享流
			await mediasoupClient.produce(videoTrack, {
				kind: 'video',
				appData: { source: 'screen' },
			})

			screenSharing.value = true
			console.log('Screen share started')

			// 监听用户停止共享
			videoTrack.onended = () => {
				stopScreenShare()
			}

			$notify.success('已开始屏幕共享')
		} catch (error) {
			console.error('Failed to start screen share', error)
			$notify.error('屏幕共享失败')
		}
	}

	/**
	 * 停止屏幕共享
	 */
	async function stopScreenShare() {
		try {
			if (screenStream.value) {
				screenStream.value.getTracks().forEach(track => track.stop())
				screenStream.value = null
			}

			// 关闭屏幕共享生产者
			const screenProducer = Array.from(mediasoupClient.producers.values()).find(
				p => p.appData?.source === 'screen',
			)

			if (screenProducer) {
				await socketClient.emit('closeProducer', {
					roomId: roomId.value,
					producerId: screenProducer.id,
				})

				mediasoupClient.closeProducer('screen')
			}

			screenSharing.value = false
			console.log('Screen share stopped')
			$notify.info('已停止屏幕共享')
		} catch (error) {
			console.error('Failed to stop screen share', error)
		}
	}

	/**
	 * 离开会议
	 */
	async function leaveMeeting() {
		try {
			console.log('Leaving meeting', roomId.value)

			// 1. 通知服务器离开
			if (socketClient.connected.value) {
				await socketClient.emit('leaveRoom', {
					roomId: roomId.value,
				})
			}

			// 2. 停止本地流
			localStream.value?.getTracks().forEach(track => track.stop())
			screenStream.value?.getTracks().forEach(track => track.stop())

			// 3. 清理 Mediasoup 客户端
			mediasoupClient?.close()
			mediasoupClient = null

			// 4. 断开 Socket 连接
			socketClient.disconnect()

			// 5. 重置状态
			roomId.value = null
			peerId.value = null
			localStream.value = null
			screenStream.value = null
			participants.value = []
			audioEnabled.value = true
			videoEnabled.value = true
			screenSharing.value = false
			connectionState.value = 'disconnected'

			console.log('Left meeting successfully')
		} catch (error) {
			console.error('Failed to leave meeting', error)
			throw error
		}
	}

	// 组件卸载时清理
	onUnmounted(() => {
		leaveMeeting()
	})

	return {
		// 状态
		roomId,
		peerId,
		localStream,
		participants,
		remoteParticipants,
		localParticipant,
		audioEnabled,
		videoEnabled,
		screenSharing,
		screenStream,
		connectionState,

		// 方法
		joinMeeting,
		leaveMeeting,
		toggleAudio,
		toggleVideo,
		startScreenShare,
		stopScreenShare,
	}
}
