import { ref, computed, watch } from 'vue'
import { socketClient } from '@/utils/SocketClient'
import { MediasoupClient } from '@/utils/MediasoupClient'
import { $notify } from '@/plugins/notification'

export function useMedia() {
	// 状态管理
	const roomId = ref(null)
	const peerId = ref(null)
	const userId = ref(null)
	const username = ref(null)
	const localStream = ref(null)
	const participants = ref([])
	const audioEnabled = ref(true)
	const videoEnabled = ref(true)
	const screenSharing = ref(false)
	const screenStream = ref(null)
	const originalVideoTrack = ref(null)
	const connectionState = ref('disconnected') // disconnected | connecting | connected | failed
	const connectionQuality = ref({
		send: { score: 10, quality: 'excellent' },
		recv: { score: 10, quality: 'excellent' },
	})

	// Mediasoup 客户端实例
	let mediasoupClient = null

	// 统计信息
	const stats = ref({
		audio: null,
		video: null,
		screen: null,
	})
	let statsIntervalId = null

	// 获取远程参与者列表
	const remoteParticipants = computed(() => participants.value.filter(p => p.peerId !== peerId.value))

	// 获取本地参与者
	const localParticipant = computed(() => {
		const local = participants.value.find(p => p.peerId === peerId.value)
		if (local && localStream.value) {
			local.streams = {
				audio: new MediaStream(localStream.value.getAudioTracks()),
				video: new MediaStream(localStream.value.getVideoTracks()),
			}
		}
		return local
	})

	/**
	 * 加入房间
	 */
	async function joinMeeting(meetingId, userIdParam, usernameParam, token) {
		try {
			connectionState.value = 'connecting'
			console.log('Joining meeting', meetingId, userIdParam)

			// 保存用户信息
			userId.value = userIdParam
			username.value = usernameParam

			// 1. 连接 Socket.io
			await socketClient.connect(import.meta.env.VITE_SFU_URL || 'http://localhost:3000', {
				auth: { token },
			})

			// 2. 加入房间
			const joinResponse = await socketClient.emit('joinRoom', {
				roomId: meetingId,
				userId: userIdParam,
				username: usernameParam,
				token,
			})

			roomId.value = meetingId
			peerId.value = joinResponse.peerId
			console.log('Joined room', joinResponse)

			// 3. 设置现有参与者（包括自己）
			participants.value = [
				{
					peerId: joinResponse.peerId,
					userId: userIdParam,
					username: usernameParam,
					streams: {},
					producers: {},
					consumers: {},
					isLocal: true,
				},
				...joinResponse.peers.map(peer => ({
					peerId: peer.peerId,
					userId: peer.userId,
					username: peer.username,
					streams: {},
					producers: {},
					consumers: {},
					isLocal: false,
				})),
			]

			// 4. 获取路由器 RTP 能力
			const { rtpCapabilities } = await socketClient.emit('getRouterRtpCapabilities', {
				roomId: meetingId,
			})

			// 5. 初始化 Mediasoup Device
			mediasoupClient = new MediasoupClient()
			await mediasoupClient.loadDevice(rtpCapabilities)

			// 6. 创建传输层
			try {
				await mediasoupClient.createSendTransport(meetingId)
				await mediasoupClient.createRecvTransport(meetingId)
			} catch (error) {
				console.error('Failed to create transports:', error)
				throw new Error('传输层创建失败，请检查网络连接')
			}

			// 7. 获取本地媒体流
			await getLocalStream()

			// 8. 发布本地媒体流
			if (localStream.value) {
				const audioTrack = localStream.value.getAudioTracks()[0]
				const videoTrack = localStream.value.getVideoTracks()[0]

				// 添加发布重试逻辑
				const publishWithTimeout = async (track, kind, timeout = 15000) => {
					return Promise.race([
						mediasoupClient.produce(track, { kind }),
						new Promise((_, reject) =>
							setTimeout(() => reject(new Error(`Publish ${kind} timeout`)), timeout),
						),
					])
				}

				try {
					// 串行发布，确保 transport 连接已建立
					if (audioTrack) {
						console.log('Publishing audio track...')
						const audioProducer = await publishWithTimeout(audioTrack, 'audio')
						updateLocalProducer('audio', audioProducer)
						console.log('Audio published successfully')
					}

					if (videoTrack) {
						console.log('Publishing video track...')
						const videoProducer = await publishWithTimeout(videoTrack, 'video')
						updateLocalProducer('video', videoProducer)
						console.log('Video published successfully')
					}
				} catch (error) {
					console.error('Failed to publish media streams:', error)

					// 如果是超时错误，给出更明确的提示
					if (error.message.includes('timeout')) {
						$notify.error('媒体流发布超时，请检查网络或防火墙设置')
					} else {
						$notify.error('发布媒体流失败，但仍可以接收其他人的视频')
					}

					// 不抛出错误，允许用户继续观看
				}
			}
			// 9. 监听事件
			setupSocketListeners()

			// 10. 订阅现有参与者的媒体流
			for (const peer of joinResponse.peers) {
				if (peer.producers && peer.producers.length > 0) {
					for (const producer of peer.producers) {
						await consumeProducer(producer.id, peer.peerId)
					}
				}
			}

			// 11. 启动统计信息收集
			// startStatsCollection()

			connectionState.value = 'connected'
		} catch (error) {
			console.error('Failed to join meeting', error)
			connectionState.value = 'failed'
			$notify.error(`加入会议失败: ${error.message}`)
			// 关闭统计数据收集
			// stopStatsCollection()
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
					sampleRate: 48000,
				},
				video: {
					width: { ideal: 1280, max: 1920 },
					height: { ideal: 720, max: 1080 },
					frameRate: { ideal: 30, max: 60 },
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
	 * 更新本地生产者
	 */
	function updateLocalProducer(kind, producer) {
		const localPeer = participants.value.find(p => p.peerId === peerId.value)
		if (localPeer) {
			localPeer.producers[kind] = producer

			// 监听生产者质量分数
			producer.on('score', score => {
				connectionQuality.value.send = {
					score: score.score,
					quality: getQualityLevel(score.score),
				}
			})
		}
	}

	/**
	 * 订阅远程生产者
	 */
	async function consumeProducer(producerId, remotePeerId) {
		try {
			console.log(`Consuming producer ${producerId} ${remotePeerId}`)

			const consumer = await mediasoupClient.consume(roomId.value, producerId, remotePeerId)

			// 找到对应的参与者
			const participant = participants.value.find(p => p.peerId === remotePeerId)
			if (!participant) {
				console.error(`Participant ${remotePeerId} not found`)
				return
			}

			if (!participant.streams) participant.streams = {}
			if (!participant.consumers) participant.consumers = {}
			if (!participant.producers) participant.producers = {}

			// 创建或更新对应类型的 MediaStream(screen/audio/video)
			const kind = consumer.track.kind
			const isScreenShare = consumer.appData?.source === 'screen'

			// 创建或更新流
			if (!participant.streams[kind]) {
				participant.streams[kind] = new MediaStream([consumer.track])
				console.log(`Created new ${kind} stream for peer ${remotePeerId}`)
			} else {
				const existingStream = participant.streams[kind]
				const existingTracks = existingStream.getTracks()

				existingTracks.forEach(track => {
					if (track.kind === kind) {
						existingStream.removeTrack(track)
						track.stop()
					}
				})

				existingStream.addTrack(consumer.track)
				console.log(`Updated ${kind} stream for peer ${remotePeerId}`)
			}

			// 记录到 producers（用于 UI 判断状态）
			participant.producers[kind] = {
				id: producerId,
				kind: kind,
				paused: false,
				appData: consumer.appData || {},
			}
			participant.consumers[consumer.id] = consumer

			// 监听 track 状态
			consumer.track.onended = () => {
				console.log(`Consumer track ended: ${consumer.id}`)
				// 清理流
				if (participant.streams[kind]) {
					participant.streams[kind].removeTrack(consumer.track)
				}
			}
			// 如果是屏幕共享，发送通知
			if (isScreenShare) {
				$notify.info(`${participant.username} 开始共享屏幕`)
			}

			console.log(`Consumer track added ${remotePeerId} ${kind}`)

			// 强制触发响应式更新
			participants.value = [...participants.value]
		} catch (error) {
			console.error('Failed to consume producer', producerId, error)
		}
	}

	/**
	 * 设置 Socket 事件监听
	 */
	function setupSocketListeners() {
		// 首先注册心跳监听
		socketClient.on('ping', data => {
			console.log('Received ping from server', data)
			// 立即响应 pong
			socketClient.socket.emit('pong', { timestamp: data.timestamp })
			console.log('Pong sent to server')
		})

		// 监听 RTT
		socketClient.on('rtt', data => {
			console.log(`RTT: ${data.rtt}ms`)
		})

		// 新参与者加入
		socketClient.on('newPeer', async data => {
			console.log('New peer joined', data)

			const existingPeer = participants.value.find(p => p.peerId === data.peerId)
			if (!existingPeer) {
				participants.value.push({
					peerId: data.peerId,
					userId: data.userId,
					username: data.username,
					streams: {},
					producers: {},
					consumers: {},
					isLocal: false,
				})

				$notify.info(`${data.username} 加入了会议`)
			}
		})

		// 参与者离开
		socketClient.on('peerLeft', data => {
			console.log('Peer left', data)

			const index = participants.value.findIndex(p => p.peerId === data.peerId)
			if (index !== -1) {
				const participant = participants.value[index]

				// 清理流
				Object.values(participant.streams).forEach(stream => {
					if (stream instanceof MediaStream) {
						stream.getTracks().forEach(track => track.stop())
					}
				})

				// 清理消费者
				Object.values(participant.consumers).forEach(consumer => {
					consumer.close()
				})

				participants.value.splice(index, 1)
			}
		})

		// 新生产者
		socketClient.on('newProducer', async data => {
			console.log('New producer', data)

			const { producerId, peerId: remotePeerId, userId: remoteUserId, username, kind } = data

			// 确保参与者存在
			let participant = participants.value.find(p => p.peerId === remotePeerId)

			if (!participant) {
				// 如果参与者不存在（理论上不应该发生），创建一个
				participant = {
					peerId: remotePeerId,
					userId: remoteUserId,
					username: username,
					streams: {},
					producers: {},
					consumers: {},
					isLocal: false,
				}
				participants.value.push(participant)
				console.log(`Added new participant ${remotePeerId} (${username})`)
			}

			// 记录生产者信息
			if (!participant.producers) {
				participant.producers = {}
			}
			participant.producers[producerId] = {
				id: producerId,
				kind: kind,
				paused: data.paused || false,
			}

			// 订阅这个新的生产者
			await consumeProducer(producerId, remotePeerId)
		})

		// 生产者关闭
		socketClient.on('producerClosed', data => {
			console.log('Producer closed', data)

			const participant = participants.value.find(p => p.peerId === data.peerId)
			if (participant && participant.consumers[data.producerId]) {
				const consumer = participant.consumers[data.producerId]
				const kind = consumer.track.kind
				const isScreenShare = consumer.appData?.source === 'screen'

				// 关闭消费者
				consumer.close()
				delete participant.consumers[data.producerId]

				// 从流中移除轨道
				if (participant.streams[kind]) {
					const stream = participant.streams[kind]
					stream.getTracks().forEach(track => {
						if (track.id === consumer.track.id) {
							track.stop()
							stream.removeTrack(track)
						}
					})

					// 如果流为空，删除流
					if (stream.getTracks().length === 0) {
						delete participant.streams[kind]
					}
				}
				if (isScreenShare) {
					$notify.info(`${participant.username} 停止了屏幕共享`)
				}

				// 从生产者列表中移除
				participant.producers = participant.producers.filter(p => p.id !== data.producerId)
			}
		})

		// 生产者暂停
		socketClient.on('producerPaused', data => {
			console.log('Producer paused', data)
			updateProducerState(data.peerId, data.producerId, true)
		})

		// 生产者恢复
		socketClient.on('producerResumed', data => {
			console.log('Producer resumed', data)
			updateProducerState(data.peerId, data.producerId, false)
		})

		// 消费者关闭
		socketClient.on('consumerClosed', data => {
			console.log('Consumer closed', data)

			const participant = participants.value.find(p =>
				Object.values(p.consumers).some(c => c.id === data.consumerId),
			)
			if (participant) {
				const consumer = participant.consumers[data.consumerId]
				if (consumer) {
					consumer.close()
					delete participant.consumers[data.consumerId]
				}
			}
		})
	}

	/**
	 * 更新生产者状态
	 */
	function updateProducerState(remotePeerId, producerId, paused) {
		const participant = participants.value.find(p => p.peerId === remotePeerId)
		if (participant && participant.consumers[producerId]) {
			const consumer = participant.consumers[producerId]
			if (paused) {
				consumer.pause()
			} else {
				consumer.resume()
			}

			// 更新生产者列表状态
			const producer = participant.producers.find(p => p.id === producerId)
			if (producer) {
				producer.paused = paused
			}
		}
	}

	/**
	 * 切换音频
	 */
	async function toggleAudio() {
		try {
			const audioProducer = mediasoupClient.producers.get('audio')
			if (!audioProducer) {
				console.warn('No audio producer found')
				return
			}

			if (audioEnabled.value) {
				// 暂停音频
				await mediasoupClient.pauseProducer('audio')
				localStream.value?.getAudioTracks().forEach(track => (track.enabled = false))

				await socketClient.emit('pauseProducer', {
					roomId: roomId.value,
					producerId: audioProducer.id,
				})

				audioEnabled.value = false
			} else {
				// 恢复音频
				await mediasoupClient.resumeProducer('audio')
				localStream.value?.getAudioTracks().forEach(track => (track.enabled = true))

				await socketClient.emit('resumeProducer', {
					roomId: roomId.value,
					producerId: audioProducer.id,
				})

				audioEnabled.value = true
			}

			console.log('Audio toggled', audioEnabled.value)
		} catch (error) {
			console.error('Failed to toggle audio', error)
			$notify.error('切换音频失败')
			throw error
		}
	}

	/**
	 * 切换视频
	 */
	async function toggleVideo() {
		try {
			const videoProducer = mediasoupClient.producers.get('video')
			if (!videoProducer) {
				console.warn('No video producer found')
				return
			}

			if (videoEnabled.value) {
				// 暂停视频
				await mediasoupClient.pauseProducer('video')
				localStream.value?.getVideoTracks().forEach(track => (track.enabled = false))

				await socketClient.emit('pauseProducer', {
					roomId: roomId.value,
					producerId: videoProducer.id,
				})

				videoEnabled.value = false
			} else {
				// 恢复视频
				await mediasoupClient.resumeProducer('video')
				localStream.value?.getVideoTracks().forEach(track => (track.enabled = true))

				await socketClient.emit('resumeProducer', {
					roomId: roomId.value,
					producerId: videoProducer.id,
				})

				videoEnabled.value = true
			}

			console.log('Video toggled', videoEnabled.value)
		} catch (error) {
			console.error('Failed to toggle video', error)
			$notify.error('切换视频失败')
			throw error
		}
	}

	/**
	 * 开始屏幕共享
	 */
	/**
	 * 开始屏幕共享 - 替换视频流方案
	 */
	async function startScreenShare() {
		try {
			// 1. 检查是否已经在共享
			if (screenSharing.value) {
				$notify.warning('您已在共享屏幕')
				return
			}

			// 2. 检查是否有其他人在共享
			if (hasScreenShare.value && !screenShareInfo.value.presenter.isLocal) {
				$notify.warning('已有参与者正在共享屏幕')
				return
			}

			// 3. 获取屏幕共享流
			const stream = await navigator.mediaDevices.getDisplayMedia({
				video: {
					cursor: 'always',
					displaySurface: 'monitor',
					width: { max: 1920 },
					height: { max: 1080 },
					frameRate: { max: 30 },
				},
				audio: false,
			})

			screenStream.value = stream
			const screenVideoTrack = stream.getVideoTracks()[0]

			// 4. 保存原始摄像头轨道
			const currentVideoProducer = mediasoupClient.producers.get('video')
			if (currentVideoProducer) {
				originalVideoTrack.value = currentVideoProducer.track

				// 暂停原摄像头 producer（不关闭，保留连接）
				await mediasoupClient.pauseProducer('video')
				await socketClient.emit('pauseProducer', {
					roomId: roomId.value,
					producerId: currentVideoProducer.id,
				})

				console.log('Original camera paused')
			} else {
				console.warn('No existing video producer to pause')
			}

			// 5. 创建新的屏幕共享 producer（使用 video kind）
			const screenProducer = await mediasoupClient.produce(screenVideoTrack, {
				kind: 'video',
				appData: {
					source: 'screen',
					shareType: 'display',
					originalProducerId: currentVideoProducer?.id,
				},
			})

			// 使用 'screen' 作为 key 存储（便于查找）
			mediasoupClient.producers.set('screen', screenProducer)

			// 6. 更新本地参与者状态
			const localPeer = participants.value.find(p => p.peerId === peerId.value)
			if (localPeer) {
				// 标记当前 video producer 为屏幕共享
				localPeer.producers.video = {
					id: screenProducer.id,
					kind: 'video',
					paused: false,
					appData: { source: 'screen' },
				}

				// 更新流（将屏幕轨道替换到 video 流中）
				if (localPeer.streams.video) {
					const videoStream = localPeer.streams.video
					const oldTracks = videoStream.getVideoTracks()
					oldTracks.forEach(track => videoStream.removeTrack(track))
					videoStream.addTrack(screenVideoTrack)
				} else {
					localPeer.streams.video = new MediaStream([screenVideoTrack])
				}
			}

			screenSharing.value = true

			// 7. 监听用户主动停止共享（浏览器按钮）
			screenVideoTrack.onended = async () => {
				console.log('Screen share ended by user via browser button')
				await stopScreenShare()
			}

			// 8. 监听 producer 关闭
			screenProducer.on('transportclose', () => {
				console.log('Screen share transport closed')
				stopScreenShare()
			})

			$notify.success('已开始屏幕共享')
			console.log('Screen share started, producer ID:', screenProducer.id)

			return screenProducer
		} catch (error) {
			screenSharing.value = false

			if (error.name === 'NotAllowedError') {
				console.log('User cancelled screen share')
				// 用户取消，不显示错误
			} else {
				console.error('Failed to start screen share:', error)
				$notify.error('屏幕共享失败')
			}

			// 恢复原摄像头（如果之前暂停了）
			await restoreCameraVideo()

			throw error
		}
	}

	/**
	 * 停止屏幕共享 - 修复版
	 */
	async function stopScreenShare() {
		try {
			if (!screenSharing.value && !screenStream.value) {
				console.log('Screen share already stopped')
				return
			}

			console.log('Stopping screen share...')

			// 1. 停止屏幕共享流的所有轨道
			if (screenStream.value) {
				screenStream.value.getTracks().forEach(track => {
					track.stop()
					console.log('Stopped screen track:', track.id)
				})
				screenStream.value = null
			}

			// 2. 关闭屏幕共享 producer
			const screenProducer = mediasoupClient.producers.get('screen')
			if (screenProducer) {
				try {
					// 通知服务器关闭
					await socketClient.emit('closeProducer', {
						roomId: roomId.value,
						producerId: screenProducer.id,
					})
					console.log('Server notified about screen producer closure')
				} catch (error) {
					console.error('Failed to notify server:', error)
				}

				// 关闭本地 producer
				screenProducer.close()
				mediasoupClient.producers.delete('screen')
			}

			// 3. 恢复原摄像头视频流
			await restoreCameraVideo()

			// 4. 更新状态
			screenSharing.value = false
			originalVideoTrack.value = null

			$notify.info('已停止屏幕共享')
			console.log('Screen share stopped successfully')
		} catch (error) {
			console.error('Failed to stop screen share:', error)
			screenSharing.value = false
			screenStream.value = null

			// 尝试恢复摄像头
			await restoreCameraVideo()
		}
	}
	/**
	 * 恢复摄像头视频流（辅助函数）
	 */
	async function restoreCameraVideo() {
		try {
			const originalProducer = mediasoupClient.producers.get('video')

			if (originalProducer && originalVideoTrack.value) {
				// 如果原 producer 还存在，直接恢复
				console.log('Restoring original camera producer...')

				// 恢复轨道
				await originalProducer.replaceTrack({ track: originalVideoTrack.value })

				// 恢复 producer
				await mediasoupClient.resumeProducer('video')
				await socketClient.emit('resumeProducer', {
					roomId: roomId.value,
					producerId: originalProducer.id,
				})

				// 更新本地参与者状态
				const localPeer = participants.value.find(p => p.peerId === peerId.value)
				if (localPeer) {
					localPeer.producers.video = {
						id: originalProducer.id,
						kind: 'video',
						paused: false,
						appData: { source: 'camera' }, // 恢复为摄像头
					}

					// 更新流
					if (localPeer.streams.video) {
						const videoStream = localPeer.streams.video
						const oldTracks = videoStream.getVideoTracks()
						oldTracks.forEach(track => videoStream.removeTrack(track))
						videoStream.addTrack(originalVideoTrack.value)
					}
				}

				console.log('Camera video restored')
			} else if (originalVideoTrack.value) {
				// 原 producer 丢失，重新创建
				console.log('Recreating camera producer...')

				const newProducer = await mediasoupClient.produce(originalVideoTrack.value, {
					kind: 'video',
					appData: { source: 'camera' },
				})

				updateLocalProducer('video', newProducer)

				console.log('New camera producer created')
			} else {
				console.warn('No original video track to restore')
			}

			// 强制更新 UI
			participants.value = [...participants.value]
		} catch (error) {
			console.error('Failed to restore camera video:', error)
			$notify.warning('无法恢复摄像头，请重新打开')
		}
	}

	/**
	 * 获取当前正在共享屏幕的参与者
	 */
	function getScreenSharingParticipant() {
		return participants.value.find(p => p.producers?.screen || p.streams?.screen)
	}

	/**
	 * 检查是否有人在共享屏幕
	 */
	const hasScreenShare = computed(() => {
		return participants.value.some(p => p.producers?.video?.appData?.source === 'screen')
	})
	/**
	 * 获取屏幕共享信息
	 */
	const screenShareInfo = computed(() => {
		const sharingPeer = participants.value.find(p => p.producers?.video?.appData?.source === 'screen')

		if (!sharingPeer) {
			return { active: false, presenter: null }
		}

		return {
			active: true,
			presenter: {
				id: sharingPeer.peerId,
				name: sharingPeer.username,
				isLocal: sharingPeer.peerId === peerId.value,
			},
		}
	})

	/**
	 * 设置首选层（Simulcast/SVC）
	 */
	async function setPreferredLayers(consumerId, spatialLayer, temporalLayer = 2) {
		try {
			await socketClient.emit('setPreferredLayers', {
				roomId: roomId.value,
				consumerId,
				spatialLayer,
				temporalLayer,
			})

			console.log(`Set preferred layers for consumer ${consumerId}`, { spatialLayer, temporalLayer })
		} catch (error) {
			console.error('Failed to set preferred layers', error)
		}
	}

	/**
	 * 获取统计信息
	 */
	async function getStats(type = 'all') {
		try {
			const localPeer = participants.value.find(p => p.peerId === peerId.value)
			if (!localPeer) return null

			const results = {}

			// 获取音频统计
			if ((type === 'all' || type === 'audio') && localPeer.producers.audio) {
				const audioStats = await socketClient.emit('getStats', {
					roomId: roomId.value,
					producerId: localPeer.producers.audio.id,
				})
				results.audio = audioStats.stats
			}

			// 获取视频统计
			if ((type === 'all' || type === 'video') && localPeer.producers.video) {
				const videoStats = await socketClient.emit('getStats', {
					roomId: roomId.value,
					producerId: localPeer.producers.video.id,
				})
				results.video = videoStats.stats
			}

			// 获取屏幕共享统计
			if ((type === 'all' || type === 'screen') && localPeer.producers.screen) {
				const screenStats = await socketClient.emit('getStats', {
					roomId: roomId.value,
					producerId: localPeer.producers.screen.id,
				})
				results.screen = screenStats.stats
			}

			stats.value = results
			return results
		} catch (error) {
			console.error('Failed to get stats', error)
			return null
		}
	}

	/**
	 * 启动统计信息收集
	 */
	function startStatsCollection() {
		// 清理旧的定时器
		if (statsIntervalId) {
			clearInterval(statsIntervalId)
			statsIntervalId = null
		}
		statsIntervalId = setInterval(async () => {
			if (connectionState.value === 'connected') {
				try {
					await getStats()
				} catch (error) {
					console.error('Failed to collect stats:', error)
					stopStatsCollection()
				}
			}
		}, 10000) // 每10秒收集一次统计信息
	}
	/**
	 * 停止统计信息收集
	 */
	function stopStatsCollection() {
		if (statsIntervalId) {
			clearInterval(statsIntervalId)
			statsIntervalId = null
			console.log('Stats collection stopped')
		}
	}

	/**
	 * 获取质量等级
	 */
	function getQualityLevel(score) {
		if (score >= 8) return 'excellent'
		if (score >= 6) return 'good'
		if (score >= 4) return 'fair'
		if (score >= 2) return 'poor'
		return 'bad'
	}

	/**
	 * 离开会议
	 */
	async function leaveMeeting() {
		try {
			console.log('Leaving meeting', roomId.value)

			// 1. 停止统计信息收集
			stopStatsCollection()

			// 2. 通知服务器离开
			if (socketClient.connected.value && roomId.value) {
				try {
					await socketClient.emit('leaveRoom', {
						roomId: roomId.value,
					})
				} catch (error) {
					console.error('Error notifying server:', error)
				}
			}

			// 3. 停止所有本地流
			localStream.value?.getTracks().forEach(track => track.stop())
			screenStream.value?.getTracks().forEach(track => track.stop())

			// 4. 清理所有参与者的流和消费者
			participants.value.forEach(participant => {
				Object.values(participant.streams || {}).forEach(stream => {
					if (stream instanceof MediaStream) {
						stream.getTracks().forEach(track => track.stop())
					}
				})
				Object.values(participant.consumers || {}).forEach(consumer => {
					consumer.close()
				})
			})

			// 5. 清理 Mediasoup 客户端
			mediasoupClient?.close()
			mediasoupClient = null

			// 6. 断开 Socket 连接
			socketClient.disconnect()

			// 7. 重置所有状态
			roomId.value = null
			peerId.value = null
			userId.value = null
			username.value = null
			localStream.value = null
			screenStream.value = null
			participants.value = []
			audioEnabled.value = true
			videoEnabled.value = true
			screenSharing.value = false
			connectionState.value = 'disconnected'
			connectionQuality.value = {
				send: { score: 10, quality: 'excellent' },
				recv: { score: 10, quality: 'excellent' },
			}
			stats.value = {
				audio: null,
				video: null,
				screen: null,
			}

			console.log('Left meeting successfully')
		} catch (error) {
			console.error('Failed to leave meeting', error)
			throw error
		}
	}

	/**
	 * 更换音频设备
	 */
	async function changeAudioDevice(deviceId) {
		try {
			const stream = await navigator.mediaDevices.getUserMedia({
				audio: { deviceId: { exact: deviceId } },
			})

			const audioTrack = stream.getAudioTracks()[0]
			const audioProducer = mediasoupClient.producers.get('audio')

			if (audioProducer) {
				await audioProducer.replaceTrack({ track: audioTrack })
				console.log('Audio device changed', deviceId)
			}

			// 更新本地流
			const oldAudioTrack = localStream.value.getAudioTracks()[0]
			oldAudioTrack?.stop()
			localStream.value.removeTrack(oldAudioTrack)
			localStream.value.addTrack(audioTrack)
		} catch (error) {
			console.error('Failed to change audio device', error)
			$notify.error('更换音频设备失败')
		}
	}

	/**
	 * 更换视频设备
	 */
	async function changeVideoDevice(deviceId) {
		try {
			const stream = await navigator.mediaDevices.getUserMedia({
				video: { deviceId: { exact: deviceId } },
			})

			const videoTrack = stream.getVideoTracks()[0]
			const videoProducer = mediasoupClient.producers.get('video')

			if (videoProducer) {
				await videoProducer.replaceTrack({ track: videoTrack })
				console.log('Video device changed', deviceId)
			}

			// 更新本地流
			const oldVideoTrack = localStream.value.getVideoTracks()[0]
			oldVideoTrack?.stop()
			localStream.value.removeTrack(oldVideoTrack)
			localStream.value.addTrack(videoTrack)
		} catch (error) {
			console.error('Failed to change video device', error)
			$notify.error('更换视频设备失败')
		}
	}

	// 监听连接质量变化
	watch(connectionQuality, quality => {
		if (quality.send.quality === 'poor' || quality.send.quality === 'bad') {
			console.warn('Poor send quality detected', quality.send)
		}
		if (quality.recv.quality === 'poor' || quality.recv.quality === 'bad') {
			console.warn('Poor recv quality detected', quality.recv)
		}
	})

	return {
		// 状态
		roomId,
		peerId,
		userId,
		username,
		localStream,
		participants,
		remoteParticipants,
		localParticipant,
		audioEnabled,
		videoEnabled,
		screenSharing,
		screenStream,
		connectionState,
		connectionQuality,
		stats,
		hasScreenShare,

		// 方法
		joinMeeting,
		leaveMeeting,
		toggleAudio,
		toggleVideo,
		startScreenShare,
		stopScreenShare,
		stopStatsCollection,
		setPreferredLayers,
		getStats,
		changeAudioDevice,
		changeVideoDevice,
		getScreenSharingParticipant,
	}
}
