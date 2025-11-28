import { ref } from 'vue'
import { useMediaDevices } from './useMediaDevices'

export function useWebRTC() {
	const { getUserMedia } = useMediaDevices()

	const localStream = ref(null)
	const participants = ref([])
	const screenShare = ref({
		active: false,
		stream: null,
		presenter: null,
	})

	const audioEnabled = ref(true)
	const videoEnabled = ref(true)
	const screenSharing = ref(false)

	const peerConnections = new Map()
	const dataChannels = new Map()

	const iceServers = [{ urls: 'stun:stun.l.google.com:19302' }, { urls: 'stun:stun1.l.google.com:19302' }]

	/**
	 * 加入会议
	 */
	const joinMeeting = async meetingId => {
		try {
			// 获取本地媒体流
			localStream.value = await getUserMedia()

			// 预留: 连接到信令服务器
			// await connectToSignalingServer(meetingId)

			// 预留: 获取房间内的其他参与者
			// const existingParticipants = await getParticipants(meetingId)

			// 模拟添加参与者
			participants.value = [
				{
					id: 'participant-1',
					name: '参与者1',
					stream: null,
					audioEnabled: true,
					videoEnabled: true,
					isSpeaking: false,
					isHost: false,
				},
			]

			return true
		} catch (error) {
			console.error('Failed to join meeting:', error)
			throw error
		}
	}

	/**
	 * 离开会议
	 */
	const leaveMeeting = async () => {
		try {
			// 停止所有本地媒体轨道
			if (localStream.value) {
				localStream.value.getTracks().forEach(track => track.stop())
				localStream.value = null
			}

			// 停止屏幕共享
			if (screenShare.value.stream) {
				screenShare.value.stream.getTracks().forEach(track => track.stop())
				screenShare.value = { active: false, stream: null, presenter: null }
			}

			// 关闭所有对等连接
			peerConnections.forEach(pc => pc.close())
			peerConnections.clear()

			// 关闭所有数据通道
			dataChannels.forEach(dc => dc.close())
			dataChannels.clear()

			// 清空参与者
			participants.value = []

			// 预留: 断开信令服务器连接
			// disconnectFromSignalingServer()

			return true
		} catch (error) {
			console.error('Failed to leave meeting:', error)
			throw error
		}
	}

	/**
	 * 创建对等连接
	 */
	const createPeerConnection = participantId => {
		const pc = new RTCPeerConnection({ iceServers })

		// 添加本地流
		if (localStream.value) {
			localStream.value.getTracks().forEach(track => {
				pc.addTrack(track, localStream.value)
			})
		}

		// ICE候选
		pc.onicecandidate = event => {
			if (event.candidate) {
				// 预留: 发送ICE候选到信令服务器
				// sendIceCandidate(participantId, event.candidate)
			}
		}

		// 接收远程流
		pc.ontrack = event => {
			const participant = participants.value.find(p => p.id === participantId)
			if (participant) {
				participant.stream = event.streams[0]
			}
		}

		// 连接状态变化
		pc.onconnectionstatechange = () => {
			console.log('Connection state:', pc.connectionState)
			if (pc.connectionState === 'disconnected' || pc.connectionState === 'failed') {
				handleParticipantDisconnect(participantId)
			}
		}

		peerConnections.set(participantId, pc)
		return pc
	}

	/**
	 * 创建数据通道
	 */
	const createDataChannel = participantId => {
		const pc = peerConnections.get(participantId)
		if (!pc) return null

		const dc = pc.createDataChannel('chat')

		dc.onopen = () => {
			console.log('Data channel opened:', participantId)
		}

		dc.onmessage = event => {
			// 处理接收到的消息
			console.log('Received message:', event.data)
		}

		dataChannels.set(participantId, dc)
		return dc
	}

	/**
	 * 切换音频
	 */
	const toggleAudio = () => {
		if (localStream.value) {
			const audioTrack = localStream.value.getAudioTracks()[0]
			if (audioTrack) {
				audioTrack.enabled = !audioTrack.enabled
				audioEnabled.value = audioTrack.enabled

				// 预留: 通知其他参与者音频状态变化
				// notifyAudioStatusChange(audioEnabled.value)
			}
		}
		return audioEnabled.value
	}

	/**
	 * 切换视频
	 */
	const toggleVideo = () => {
		if (localStream.value) {
			const videoTrack = localStream.value.getVideoTracks()[0]
			if (videoTrack) {
				videoTrack.enabled = !videoTrack.enabled
				videoEnabled.value = videoTrack.enabled

				// 预留: 通知其他参与者视频状态变化
				// notifyVideoStatusChange(videoEnabled.value)
			}
		}
		return videoEnabled.value
	}

	/**
	 * 开始屏幕共享
	 */
	const startScreenShare = async () => {
		try {
			const stream = await navigator.mediaDevices.getDisplayMedia({
				video: {
					cursor: 'always',
				},
				audio: false,
			})

			// 监听用户停止共享
			stream.getVideoTracks()[0].onended = () => {
				stopScreenShare()
			}

			// 替换视频轨道
			if (localStream.value) {
				const videoTrack = stream.getVideoTracks()[0]
				const sender = Array.from(peerConnections.values())
					.flatMap(pc => pc.getSenders())
					.find(s => s.track?.kind === 'video')

				if (sender) {
					await sender.replaceTrack(videoTrack)
				}
			}

			screenShare.value = {
				active: true,
				stream,
				presenter: { id: 'current-user', name: '我' },
			}

			screenSharing.value = true

			// 预留: 通知其他参与者开始屏幕共享
			// notifyScreenShareStart()

			return stream
		} catch (error) {
			console.error('Failed to start screen share:', error)
			throw error
		}
	}

	/**
	 * 停止屏幕共享
	 */
	const stopScreenShare = async () => {
		try {
			if (screenShare.value.stream) {
				screenShare.value.stream.getTracks().forEach(track => track.stop())

				// 恢复摄像头视频
				if (localStream.value) {
					const videoTrack = localStream.value.getVideoTracks()[0]
					const sender = Array.from(peerConnections.values())
						.flatMap(pc => pc.getSenders())
						.find(s => s.track?.kind === 'video')

					if (sender && videoTrack) {
						await sender.replaceTrack(videoTrack)
					}
				}
			}

			screenShare.value = { active: false, stream: null, presenter: null }
			screenSharing.value = false

			// 预留: 通知其他参与者停止屏幕共享
			// notifyScreenShareStop()

			return true
		} catch (error) {
			console.error('Failed to stop screen share:', error)
			throw error
		}
	}

	/**
	 * 处理参与者断开连接
	 */
	const handleParticipantDisconnect = participantId => {
		const index = participants.value.findIndex(p => p.id === participantId)
		if (index !== -1) {
			participants.value.splice(index, 1)
		}

		const pc = peerConnections.get(participantId)
		if (pc) {
			pc.close()
			peerConnections.delete(participantId)
		}

		const dc = dataChannels.get(participantId)
		if (dc) {
			dc.close()
			dataChannels.delete(participantId)
		}
	}

	/**
	 * 发送聊天消息
	 */
	const sendChatMessage = message => {
		dataChannels.forEach(dc => {
			if (dc.readyState === 'open') {
				dc.send(
					JSON.stringify({
						type: 'chat',
						message,
						timestamp: Date.now(),
					}),
				)
			}
		})
	}

	return {
		localStream,
		participants,
		screenShare,
		audioEnabled,
		videoEnabled,
		screenSharing,
		joinMeeting,
		leaveMeeting,
		createPeerConnection,
		createDataChannel,
		toggleAudio,
		toggleVideo,
		startScreenShare,
		stopScreenShare,
		sendChatMessage,
	}
}
