import { ref, computed, onUnmounted, watch } from 'vue'
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
                    producers: peer.producers || [],
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
            await mediasoupClient.createSendTransport(meetingId)
            await mediasoupClient.createRecvTransport(meetingId)

            // 7. 获取本地媒体流
            await getLocalStream()

            // 8. 发布本地媒体流
            if (localStream.value) {
                const audioTrack = localStream.value.getAudioTracks()[0]
                const videoTrack = localStream.value.getVideoTracks()[0]

                if (audioTrack) {
                    const producer = await mediasoupClient.produce(audioTrack, { kind: 'audio' })
                    updateLocalProducer('audio', producer)
                }

                if (videoTrack) {
                    const producer = await mediasoupClient.produce(videoTrack, { kind: 'video' })
                    updateLocalProducer('video', producer)
                }
            }

            // 9. 订阅现有参与者的媒体流
            for (const peer of joinResponse.peers) {
                if (peer.producers && peer.producers.length > 0) {
                    for (const producer of peer.producers) {
                        await consumeProducer(producer.id, peer.peerId)
                    }
                }
            }

            // 10. 监听事件
            setupSocketListeners()

            // 11. 启动统计信息收集
            startStatsCollection()

            connectionState.value = 'connected'
            $notify.success(`已加入会议: ${usernameParam}`)
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
            console.log('Consuming producer', producerId, remotePeerId)

            const consumer = await mediasoupClient.consume(roomId.value, producerId, remotePeerId)

            // 更新参与者流
            const participant = participants.value.find(p => p.peerId === remotePeerId)
            if (participant) {
                const kind = consumer.track.kind

                // 创建或更新媒体流
                if (!participant.streams[kind]) {
                    participant.streams[kind] = new MediaStream()
                }

                participant.streams[kind].addTrack(consumer.track)
                participant.consumers[producerId] = consumer

                // 监听消费者质量分数
                consumer.on('score', score => {
                    connectionQuality.value.recv = {
                        score: score.score,
                        quality: getQualityLevel(score.score),
                    }
                })

                console.log('Consumer track added', remotePeerId, kind)
            }

            return consumer
        } catch (error) {
            console.error('Failed to consume producer', error)
            throw error
        }
    }

    /**
     * 设置 Socket 事件监听
     */
    function setupSocketListeners() {
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
                    producers: [],
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
                $notify.info(`${data.username} 离开了会议`)
            }
        })

        // 新生产者
        socketClient.on('newProducer', async data => {
            console.log('New producer', data)

            // 不消费自己的生产者
            if (data.peerId !== peerId.value) {
                const participant = participants.value.find(p => p.peerId === data.peerId)
                if (participant) {
                    // 记录生产者信息
                    participant.producers.push({
                        id: data.producerId,
                        kind: data.kind,
                        paused: data.paused,
                    })

                    // 开始消费
                    await consumeProducer(data.producerId, data.peerId)
                }
            }
        })

        // 生产者关闭
        socketClient.on('producerClosed', data => {
            console.log('Producer closed', data)

            const participant = participants.value.find(p => p.peerId === data.peerId)
            if (participant && participant.consumers[data.producerId]) {
                const consumer = participant.consumers[data.producerId]
                const kind = consumer.track.kind

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

            const participant = participants.value.find(p => Object.values(p.consumers).some(c => c.id === data.consumerId))
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
    async function startScreenShare() {
        try {
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
            const videoTrack = stream.getVideoTracks()[0]

            // 生产屏幕共享流
            const producer = await mediasoupClient.produce(videoTrack, {
                kind: 'video',
                appData: { source: 'screen' },
            })

            // 更新本地参与者
            const localPeer = participants.value.find(p => p.peerId === peerId.value)
            if (localPeer) {
                localPeer.producers.screen = producer
                localPeer.streams.screen = stream
            }

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
            throw error
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

                screenProducer.close()
                mediasoupClient.producers.delete('screen')

                // 更新本地参与者
                const localPeer = participants.value.find(p => p.peerId === peerId.value)
                if (localPeer) {
                    delete localPeer.producers.screen
                    delete localPeer.streams.screen
                }
            }

            screenSharing.value = false
            console.log('Screen share stopped')
            $notify.info('已停止屏幕共享')
        } catch (error) {
            console.error('Failed to stop screen share', error)
        }
    }

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
        setInterval(async () => {
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

            // 1. 通知服务器离开
            if (socketClient.connected.value && roomId.value) {
                await socketClient.emit('leaveRoom', {
                    roomId: roomId.value,
                })
            }

            // 2. 停止所有本地流
            localStream.value?.getTracks().forEach(track => track.stop())
            screenStream.value?.getTracks().forEach(track => track.stop())

            // 3. 清理所有参与者的流和消费者
            participants.value.forEach(participant => {
                Object.values(participant.streams).forEach(stream => {
                    if (stream instanceof MediaStream) {
                        stream.getTracks().forEach(track => track.stop())
                    }
                })
                Object.values(participant.consumers).forEach(consumer => {
                    consumer.close()
                })
            })

            // 4. 清理 Mediasoup 客户端
            mediasoupClient?.close()
            mediasoupClient = null

            // 5. 断开 Socket 连接
            socketClient.disconnect()

            // 6. 重置所有状态
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

    // 组件卸载时清理
    onUnmounted(() => {
        leaveMeeting()
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

        // 方法
        joinMeeting,
        leaveMeeting,
        toggleAudio,
        toggleVideo,
        startScreenShare,
        stopScreenShare,
        setPreferredLayers,
        getStats,
        changeAudioDevice,
        changeVideoDevice,
    }
}