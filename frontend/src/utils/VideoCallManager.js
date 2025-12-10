/**
 * WebRTC 视频通话管理器
 * 负责处理 P2P 连接、媒体流管理、信令交换
 */
import { ref } from 'vue'

export class VideoCallManager {
	constructor(signalingService) {
		console.log('========== VideoCallManager 初始化 ==========')

		this.signalingService = signalingService
		this.peerConnection = null
		this.localStream = ref(null)
		this.remoteStream = ref(null)
		this.callId = null
		this.targetUserId = null
		this.callState = ref('idle') // idle, calling, connected, ended

		this.onIncomingCall = null
		this.onCallAnswer = null
		this.onIceCandidate = null
		this.onCallEnd = null
		this.onError = null

		// ICE 服务器配置（包含 Google STUN 服务器）
		this.iceServers = {
			iceServers: [
				{ urls: 'stun:stun.l.google.com:19302' },
				{ urls: 'stun:stun1.l.google.com:19302' },
				{ urls: 'stun:stun2.l.google.com:19302' },
				{ urls: 'stun:stun3.l.google.com:19302' },
				{ urls: 'stun:stun4.l.google.com:19302' },
			],
			iceCandidatePoolSize: 10, // 预生成 ICE 候选数量
		}

		// ICE 候选缓存队列
		this.pendingCandidates = []

		// 连接质量统计
		this.stats = {
			bytesReceived: 0,
			bytesSent: 0,
			packetsLost: 0,
		}

		this.setupSignalingHandlers()
	}

	/**
	 * 设置信令处理器
	 */
	setupSignalingHandlers() {
		// 接收到通话邀请
		this.signalingService.on('call-offer', async data => {
			console.log('========== 收到 call-offer 事件 ==========')
			const { callId, offer, fromUserId } = data
			console.log('收到通话邀请:', { callId, fromUserId })

			this.callId = callId
			this.targetUserId = fromUserId
			this.callState.value = 'calling'

			// 触发来电事件（由组件处理 UI）
			this.onIncomingCall && this.onIncomingCall({ callId, fromUserId, offer })
		})

		// 接收到通话应答
		this.signalingService.on('call-answer', data => {
			console.log('========== 收到 call-answer 事件 ==========')
			const { answer, callId } = data
			if (this.callId !== callId) {
				console.warn(`callId 不匹配,忽略此 answer (期望: ${this.callId}, 收到: ${callId})`)
				return
			}
			console.log('收到通话应答')
			this.handleAnswer(answer)
		})

		// 接收到 ICE 候选
		this.signalingService.on('ice-candidate', data => {
			console.log('========== 收到 ice-candidate 事件 ==========')
			const { candidate } = data
			this.handleIceCandidate(candidate)
		})

		// 通话结束
		this.signalingService.on('call-end', data => {
			console.log('========== 收到 call-end 事件 ==========')
			const { reason, fromUserId } = data
			console.log('对方结束通话, 原因:', reason, '来自用户:', fromUserId)

			// 先触发回调（通知组件）
			if (this.onCallEnd) {
				console.log('触发 onCallEnd 回调')
				this.onCallEnd({ reason, fromUserId })
			}

			// 然后清理本地资源（不发送信令）
			this.endCall(false)
		})

		// 错误处理
		this.signalingService.on('error', errorMsg => {
			console.error('信令错误:', errorMsg)
			this.onError && this.onError(errorMsg)
		})
	}

	/**
	 * 初始化本地媒体流
	 */
	async initLocalStream(constraints = { video: true, audio: true }) {
		try {
			// 优化的媒体约束
			const optimizedConstraints = {
				video: constraints.video
					? {
							width: { ideal: 1280, max: 1920 },
							height: { ideal: 720, max: 1080 },
							frameRate: { ideal: 30, max: 30 },
							facingMode: 'user',
						}
					: false,
				audio: constraints.audio
					? {
							echoCancellation: true,
							noiseSuppression: true,
							autoGainControl: true,
						}
					: false,
			}

			this.localStream.value = await navigator.mediaDevices.getUserMedia(optimizedConstraints)
			console.log('本地流轨道:', this.localStream.value.getTracks())
			return this.localStream.value
		} catch (error) {
			console.error('获取本地媒体流失败:', error)

			// 提供更详细的错误信息
			let errorMessage = '无法访问摄像头或麦克风'
			if (error.name === 'NotAllowedError') {
				errorMessage = '用户拒绝了摄像头/麦克风权限'
			} else if (error.name === 'NotFoundError') {
				errorMessage = '未找到摄像头或麦克风设备'
			} else if (error.name === 'NotReadableError') {
				errorMessage = '设备正在被其他应用使用'
			} else if (error.name === 'OverconstrainedError') {
				errorMessage = '设备不支持请求的约束条件'
			}

			throw new Error(errorMessage + ': ' + error.message)
		}
	}

	/**
	 * 创建 RTCPeerConnection
	 */
	createPeerConnection() {
		console.log('创建 RTCPeerConnection')
		this.peerConnection = new RTCPeerConnection(this.iceServers)

		// 添加本地流到连接
		this.localStream.value.getTracks().forEach(track => {
			this.peerConnection.addTrack(track, this.localStream.value)
			console.log('添加本地轨道:', track.kind)
		})

		// 处理远程流
		this.peerConnection.ontrack = event => {
			console.log('收到远程流轨道:', event.track.kind)
			if (!this.remoteStream.value) {
				this.remoteStream.value = new MediaStream()
			}
			this.remoteStream.value.addTrack(event.track)

			// 监听轨道结束
			event.track.onended = () => {
				console.log('远程轨道结束:', event.track.kind)
			}
		}

		// 处理 ICE 候选
		this.peerConnection.onicecandidate = event => {
			if (event.candidate) {
				console.log('发送 ICE 候选')
				this.signalingService.sendIceCandidate({
					callId: this.callId,
					toUserId: this.targetUserId,
					candidate: event.candidate,
				})
			} else {
				console.log('ICE 候选收集完成')
			}
		}

		// ICE 连接状态变化
		this.peerConnection.oniceconnectionstatechange = () => {
			const state = this.peerConnection.iceConnectionState
			console.log('ICE 连接状态:', state)

			switch (state) {
				case 'connected':
				case 'completed':
					this.callState.value = 'connected'
					console.log('P2P 连接建立成功')
					// 开始统计
					this.startStatsCollection()
					break
				case 'failed':
					console.error('ICE 连接失败')
					this.peerConnection.restartIce()
					break
				case 'disconnected':
					console.warn('ICE 连接断开')
					break
				case 'closed':
					console.log('ICE 连接关闭')
					break
			}
		}

		// ICE 收集状态变化
		this.peerConnection.onicegatheringstatechange = () => {
			console.log('ICE 收集状态:', this.peerConnection.iceGatheringState)
		}

		// 连接状态变化
		this.peerConnection.onconnectionstatechange = () => {
			const state = this.peerConnection.connectionState
			console.log('连接状态:', state)

			switch (state) {
				case 'connected':
					console.log('连接建立成功')
					break
				case 'disconnected':
					console.warn('连接断开')
					break
				case 'failed':
					console.error('连接失败')
					this.endCall()
					break
				case 'closed':
					console.log('连接关闭')
					break
			}
		}

		// 协商需要重新进行
		this.peerConnection.onnegotiationneeded = async () => {
			console.log('需要重新协商')
		}

		// 信令状态变化
		this.peerConnection.onsignalingstatechange = () => {
			console.log('信令状态:', this.peerConnection.signalingState)
		}

		return this.peerConnection
	}

	/**
	 * 发起通话（主呼方）
	 */
	async startCall(targetUserId) {
		try {
			console.log('发起通话 - 目标用户:', targetUserId)
			this.targetUserId = targetUserId
			this.callState.value = 'calling'

			// 1. 获取本地媒体流
			await this.initLocalStream()

			// 2. 创建 PeerConnection
			this.createPeerConnection()

			// 3. 创建 Offer
			const offer = await this.peerConnection.createOffer({
				offerToReceiveAudio: true,
				offerToReceiveVideo: true,
			})

			// 4. 设置本地描述
			await this.peerConnection.setLocalDescription(offer)
			console.log('设置本地描述成功')

			// 5. 通过信令服务器发送 Offer
			const callId = this.signalingService.sendCallOffer({
				toUserId: targetUserId,
				offer: offer,
			})

			this.callId = callId
			console.log('通话发起成功 - callId:', callId)

			return { localStream: this.localStream.value, callId }
		} catch (error) {
			console.error('发起通话失败:', error)
			this.endCall()
			throw error
		}
	}

	/**
	 * 接受通话（被呼方）
	 */
	async acceptCall(offer) {
		try {
			console.log('接受通话')

			// 1. 获取本地媒体流
			await this.initLocalStream()

			// 2. 创建 PeerConnection
			this.createPeerConnection()

			// 3. 设置远程描述（Offer）
			await this.peerConnection.setRemoteDescription(new RTCSessionDescription(offer))
			console.log('设置远程描述成功')

			// 4. 创建 Answer
			const answer = await this.peerConnection.createAnswer()

			// 5. 设置本地描述（Answer）
			await this.peerConnection.setLocalDescription(answer)
			console.log('设置本地描述成功')

			// 6. 发送 Answer
			this.signalingService.sendCallAnswer({
				callId: this.callId,
				toUserId: this.targetUserId,
				answer: answer,
			})

			// 7. 处理缓存的 ICE 候选
			await this.processPendingCandidates()

			console.log('通话接受成功')
			return { localStream: this.localStream.value, remoteStream: this.remoteStream.value }
		} catch (error) {
			console.error('接受通话失败:', error)
			throw error
		}
	}

	/**
	 * 处理收到的 Answer
	 */
	async handleAnswer(answer) {
		try {
			if (!this.peerConnection) {
				throw new Error('PeerConnection 未初始化')
			}
			const signalingState = this.peerConnection.signalingState

			if (signalingState !== 'have-local-offer') {
				console.warn('信令状态不正确,跳过设置远程描述')
				return
			}

			if (this.peerConnection.remoteDescription) {
				console.warn('远程描述已设置,跳过重复设置')
				return
			}

			await this.peerConnection.setRemoteDescription(new RTCSessionDescription(answer))
			console.log('设置远程描述成功')

			// 处理缓存的 ICE 候选
			await this.processPendingCandidates()
		} catch (error) {
			console.error('处理 Answer 失败:', error)
			throw error
		}
	}

	/**
	 * 处理 ICE 候选
	 */
	async handleIceCandidate(candidate) {
		try {
			if (!this.peerConnection) {
				console.warn('PeerConnection 未初始化，缓存 ICE 候选')
				this.pendingCandidates.push(candidate)
				return
			}

			// 检查远程描述是否已设置
			if (!this.peerConnection.remoteDescription) {
				console.warn('远程描述未设置，缓存 ICE 候选')
				this.pendingCandidates.push(candidate)
				return
			}

			await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate))
			console.log('ICE 候选添加成功')
		} catch (error) {
			console.error('添加 ICE 候选失败:', error)
		}
	}

	/**
	 * 处理缓存的 ICE 候选
	 */
	async processPendingCandidates() {
		if (this.pendingCandidates.length > 0) {
			for (const candidate of this.pendingCandidates) {
				try {
					await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate))
					console.log('添加缓存的 ICE 候选成功')
				} catch (error) {
					console.error('添加缓存的 ICE 候选失败:', error)
				}
			}

			this.pendingCandidates = []
		}
	}

	/**
	 * 开始收集连接统计信息
	 */
	startStatsCollection() {
		if (this.statsInterval) {
			clearInterval(this.statsInterval)
		}

		this.statsInterval = setInterval(async () => {
			if (!this.peerConnection) return

			try {
				const stats = await this.peerConnection.getStats()
				stats.forEach(report => {
					if (report.type === 'inbound-rtp' && report.kind === 'video') {
						this.stats.bytesReceived = report.bytesReceived
						this.stats.packetsLost = report.packetsLost
					}
					if (report.type === 'outbound-rtp' && report.kind === 'video') {
						this.stats.bytesSent = report.bytesSent
					}
				})
			} catch (error) {
				console.error('获取统计信息失败:', error)
			}
		}, 1000)
	}

	/**
	 * 切换麦克风状态
	 */
	toggleAudio(enabled) {
		if (this.localStream.value) {
			this.localStream.value.getAudioTracks().forEach(track => {
				track.enabled = enabled
			})
			console.log('音频状态:', enabled ? '开启' : '关闭')
			return true
		}
		return false
	}

	/**
	 * 切换摄像头状态
	 */
	toggleVideo(enabled) {
		if (this.localStream.value) {
			this.localStream.value.getVideoTracks().forEach(track => {
				track.enabled = enabled
			})
			console.log('视频状态:', enabled ? '开启' : '关闭')
			return true
		}
		return false
	}

	/**
	 * 取消呼叫（主呼方在对方接听前取消）
	 */
	cancelCall() {
		console.log('取消呼叫')

		if (!this.callId || !this.targetUserId) {
			console.warn('没有活跃的呼叫可以取消')
			return
		}

		// 发送取消信令
		try {
			this.signalingService.sendCallEnd({
				callId: this.callId,
				toUserId: this.targetUserId,
				reason: 'user-cancelled',
			})
			console.log('已发送取消呼叫信令')
		} catch (error) {
			console.error('发送取消信令失败:', error)
		}

		// 清理资源
		this.endCall(false) // 不再发送信令（已经发送过了）
	}

	/**
	 * 结束通话
	 */
	endCall(sendSignal = true) {
		console.log('结束通话')

		// 停止统计
		if (this.statsInterval) {
			clearInterval(this.statsInterval)
			this.statsInterval = null
		}

		// 停止本地流
		this.localStream.value = null

		// 关闭 PeerConnection
		if (this.peerConnection) {
			this.peerConnection.close()
			this.peerConnection = null
		}

		// 清空远程流
		this.remoteStream.value = null

		// 通知对方结束通话
		if (sendSignal && this.callId && this.targetUserId) {
			try {
				this.signalingService.sendCallEnd({
					callId: this.callId,
					toUserId: this.targetUserId,
					reason: 'user-hangup',
				})
			} catch (error) {
				console.error('发送结束通话信令失败:', error)
			}
		}

		this.callId = null
		this.targetUserId = null
		this.pendingCandidates = []
		this.callState.value = 'ended'
	}

	/**
	 * 获取远程流
	 */
	getRemoteStream() {
		return this.remoteStream.value
	}

	/**
	 * 获取本地流
	 */
	getLocalStream() {
		return this.localStream.value
	}

	/**
	 * 获取通话状态
	 */
	getCallState() {
		return this.callState.value
	}

	/**
	 * 获取连接统计信息
	 */
	getStats() {
		return this.stats
	}
}
