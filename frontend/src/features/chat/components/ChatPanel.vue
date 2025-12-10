<template>
	<div class="flex-1 flex flex-col">
		<!-- 聊天头部 -->
		<ChatHeader :user="conversation.user" />

		<!-- 消息区域 -->
		<div ref="messagesContainer" class="flex-1 overflow-y-auto px-4 py-4" @scroll="handleScroll">
			<!-- 加载更多指示器 -->
			<div v-if="isLoadingMore" class="text-center py-2">
				<span class="text-sm text-gray-500">加载中...</span>
			</div>

			<!-- 没有更多消息提示 -->
			<div v-else-if="!conversation.hasMore && conversation.messages.length > 0" class="text-center py-2">
				<span class="text-sm text-gray-400">没有更多消息了</span>
			</div>

			<!-- 消息列表 -->
			<div class="space-y-2">
				<MessageBubble
					v-for="message in conversation.messages"
					:key="message.id"
					:message="message"
					:user="conversation.user"
				/>
			</div>
		</div>

		<!-- 消息输入框 -->
		<MessageInput @send="handleSend" @video-call="handleVideoCall" />

		<!-- 视频对话框 -->
		<VideoCall
			:is-active="isVideoCallActive"
			:local-stream="localStream"
			:remote-stream="remoteStream"
			:remote-name="conversation.user.name"
			:call-state="callState"
			:show-duration="true"
			:initial-audio-muted="false"
			:initial-video-muted="false"
			@end="endVideoCall"
			@toggle-audio="handleToggleAudio"
			@toggle-video="handleToggleVideo"
			@state-change="handleStateChange"
			@error="handleVideoError"
		/>

		<!-- 来电通知 -->
		<IncomingCallNotification
			v-if="incomingCall"
			:caller-name="incomingCall.callerName"
			@accept="acceptCall"
			@reject="rejectCall"
		/>
	</div>
</template>

<script setup>
import { ref, nextTick, watch, onMounted, onBeforeUnmount, defineAsyncComponent } from 'vue'
import { useUserStore } from '@/stores/user'
import ChatHeader from './ChatHeader.vue'
import MessageBubble from './MessageBubble.vue'
import MessageInput from './MessageInput.vue'
import { $notify } from '@/plugins/notification'

// 延迟加载视频通话相关组件
const VideoCall = defineAsyncComponent(() => import('./VideoCall.vue'))
const IncomingCallNotification = defineAsyncComponent(() => import('./IncomingCallNotification.vue'))
const props = defineProps({
	conversation: {
		type: Object,
		required: true,
	},
})

const emit = defineEmits(['send-message', 'load-more'])
const userStore = useUserStore()
const messagesContainer = ref(null)
const isLoadingMore = ref(false)
const scrollThreshold = 100
const isVideoCallActive = ref(false)
const localStream = ref(null)
const remoteStream = ref(null)
const incomingCall = ref(null)
const videoCallReady = ref(false)
const callState = ref('idle')

let videoCallManager = null
let signalingService = null

const currentUserId = userStore.userId

// 初始化视频通话功能（延迟加载）
const initVideoCall = async () => {
	if (videoCallReady.value) return

	try {
		console.log('正在加载视频通话功能...')

		// 创建新实例
		const [{ default: SignalingServiceClass }, { VideoCallManager: VideoCallManagerClass }] = await Promise.all([
			import('@/services/SignalingService'),
			import('@/utils/VideoCallManager'),
		])

		// 每个组件独立实例
		signalingService = new SignalingServiceClass()

		const signalingServerUrl = import.meta.env.VITE_WS_SIGNALING_SERVER
		await signalingService.connect(signalingServerUrl, currentUserId)

		videoCallManager = new VideoCallManagerClass(signalingService)

		// 设置回调
		videoCallManager.onIncomingCall = ({ callId, fromUserId, offer }) => {
			// 匹配是否是当前会话用户的来电
			if (fromUserId === props.conversation.user.userId) {
				incomingCall.value = {
					callId,
					fromUserId,
					offer,
					callerName: props.conversation.user.nickname,
				}
			}
		}

		// 监听远程结束通话事件
		videoCallManager.onCallEnd = ({ reason }) => {
			console.log('收到远程结束通话信令:', reason)
			handleRemoteCallEnd(reason)
		}

		videoCallReady.value = true
		console.log('视频通话功能已就绪')
	} catch (error) {
		console.error('初始化视频通话失败:', error)
	}
}
// 处理对方结束通话
const handleRemoteCallEnd = reason => {
	// 显示通知
	const reasonText =
		{
			'user-hangup': '对方已挂断',
			'user-rejected': '对方拒绝了通话',
			'user-cancelled': '对方取消了呼叫',
			busy: '对方忙线中',
			timeout: '通话超时',
		}[reason] || '通话已结束'

	$notify.info(reasonText)

	// 关闭视频通话界面
	isVideoCallActive.value = false
	// 清理媒体流
	localStream.value = null
	remoteStream.value = null

	// 关闭来电通知
	if (incomingCall.value) {
		incomingCall.value = null
	}

	// 更新通话状态
	callState.value = 'ended'

	// 1秒后重置状态
	setTimeout(() => {
		callState.value = 'idle'
	}, 1000)
}
const handleVideoError = errorMsg => {
	console.error('视频通话错误:', errorMsg)
}
/**
 * 处理滚动事件（向上滚动到顶部时加载更多）
 */
const handleScroll = async () => {
	if (!messagesContainer.value || isLoadingMore.value) {
		return
	}

	const { scrollTop } = messagesContainer.value

	// 当滚动到接近顶部时，加载更多历史消息
	if (scrollTop < scrollThreshold && props.conversation.hasMore) {
		isLoadingMore.value = true

		// 记录当前滚动位置和内容高度
		const oldScrollHeight = messagesContainer.value.scrollHeight
		const oldScrollTop = messagesContainer.value.scrollTop

		try {
			console.log('📜 触发加载更多消息...')
			emit('load-more')

			// 加载完成后，恢复滚动位置（防止跳动）
			nextTick(() => {
				if (messagesContainer.value) {
					const newScrollHeight = messagesContainer.value.scrollHeight
					const heightDiff = newScrollHeight - oldScrollHeight
					messagesContainer.value.scrollTop = oldScrollTop + heightDiff
				}
			})
		} catch (error) {
			console.error('❌ 加载更多消息失败:', error)
		} finally {
			isLoadingMore.value = false
		}
	}
}
// 清理
onBeforeUnmount(() => {
	if (videoCallManager) {
		videoCallManager.endCall()
	}
	if (signalingService) {
		signalingService.disconnect()
	}
})

// 发送消息
const handleSend = text => {
	emit('send-message', text)
	nextTick(() => {
		scrollToBottom()
	})
}

// 处理视频通话请求
const handleVideoCall = async () => {
	// 确保视频通话功能已初始化
	if (!videoCallReady.value) {
		await initVideoCall()
	}

	if (!videoCallReady.value) {
		$notify.error('视频通话功能初始化失败，请刷新页面重试')
		return
	}

	try {
		console.log('📞 发起视频通话')
		callState.value = 'calling'
		const result = await videoCallManager.startCall(props.conversation.user.userId)
		localStream.value = result.localStream
		isVideoCallActive.value = true
		console.log('Local stream tracks:', localStream.value?.getTracks())
		watchRemoteStream()
		watchCallState()
	} catch (error) {
		console.error('❌ 视频通话失败:', error)
		callState.value = 'ended'
		$notify.error('无法发起视频通话: ' + error.message)
	}
}

// 接受来电
const acceptCall = async () => {
	try {
		if (!videoCallManager) {
			throw new Error('视频通话管理器未初始化')
		}

		if (!incomingCall.value) {
			throw new Error('没有来电信息')
		}

		console.log('接受来电')

		// 接受通话
		const result = await videoCallManager.acceptCall(incomingCall.value.offer)
		localStream.value = result.localStream
		isVideoCallActive.value = true

		// 监听远程流
		watchRemoteStream()

		// 监听通话状态
		watchCallState()

		incomingCall.value = null
	} catch (error) {
		$notify.error('无法接受通话: ' + error.message)
	}
}

// 拒绝来电
const rejectCall = () => {
	if (incomingCall.value) {
		console.log('拒绝来电')

		signalingService.sendCallEnd({
			callId: incomingCall.value.callId,
			fromUserId: currentUserId,
			toUserId: incomingCall.value.fromUserId,
			reason: 'user-rejected',
		})
		isVideoCallActive.value = false
		incomingCall.value = null
	}
}

// 监听远程流变化
const watchRemoteStream = () => {
	let attempts = 0
	const maxAttempts = 100 // 10秒

	const checkInterval = setInterval(() => {
		attempts++

		const stream = videoCallManager.getRemoteStream()
		if (stream && stream.getTracks().length > 0) {
			remoteStream.value = stream
			clearInterval(checkInterval)
			console.log('远程流已建立')
		}

		if (attempts >= maxAttempts) {
			clearInterval(checkInterval)
			console.warn('远程流建立超时')
		}
	}, 100)
}

// 监听通话状态
const watchCallState = () => {
	const stateCheckInterval = setInterval(() => {
		if (videoCallManager) {
			const currentState = videoCallManager.getCallState()
			if (currentState !== callState.value) {
				callState.value = currentState
			}
			if (currentState === 'ended') {
				clearInterval(stateCheckInterval)
			}
		} else {
			clearInterval(stateCheckInterval)
		}
	}, 500)
}

// 切换音频
const handleToggleAudio = enabled => {
	videoCallManager?.toggleAudio(enabled)
}

// 切换视频
const handleToggleVideo = enabled => {
	videoCallManager?.toggleVideo(enabled)
}
const handleStateChange = state => {
	console.log('通话状态变化:', state)
}
// 结束通话
const endVideoCall = () => {
	// 区分呼叫中取消和通话中挂断
	if (callState.value === 'calling') {
		// 呼叫中取消
		videoCallManager?.cancelCall()
	} else {
		// 通话中挂断
		videoCallManager?.endCall(true)
	}

	// 关闭 UI
	isVideoCallActive.value = false
	localStream.value = null
	remoteStream.value = null
	callState.value = 'ended'

	setTimeout(() => {
		callState.value = 'idle'
	}, 1000)
}

// 滚动到底部
const scrollToBottom = () => {
	nextTick(() => {
		if (messagesContainer.value) {
			messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
		}
	})
}
// 组件挂载时立即初始化视频通话功能
onMounted(async () => {
	await initVideoCall()
	console.log('成功初始化视频通话功能')
})

// 监听消息变化，自动滚动
watch(
	() => props.conversation?.messages.length,
	() => {
		scrollToBottom()
	},
	{ immediate: true },
)
</script>
