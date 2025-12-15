<template>
	<v-sheet color="background" class="chat-panel fill-height d-flex flex-column" elevation="0">
		<!-- 聊天头部 -->
		<ChatHeader :user="conversation.user" />

		<!-- 消息列表 - 关键修改点 -->
		<div ref="messageListRef" class="message-list-container">
			<!-- 空状态 - 使用绝对定位 -->
			<div v-if="conversation.messages.length === 0" class="empty-message-state">
				<v-icon icon="mdi-message-outline" size="64" color="grey-lighten-1"></v-icon>
				<p class="text-body-1 text-disabled mt-4">暂无消息，开始聊天吧</p>
			</div>

			<!-- 加载更多按钮 -->
			<div v-else-if="conversation.hasMore" class="text-center py-3">
				<v-btn variant="text" size="small" color="primary" prepend-icon="mdi-chevron-up" @click="handleScroll">
					加载更多消息
				</v-btn>
			</div>

			<!-- 消息列表内容 -->
			<div v-if="conversation.messages.length > 0" class="message-list-content px-4 py-3">
				<div v-for="(item, index) in messagesWithDividers" :key="item.id || `divider-${index}`">
					<!-- 日期分隔线 -->
					<div v-if="item.isDivider" class="message-date-divider my-4">
						<v-divider></v-divider>
						<v-chip size="small" color="surface-variant" class="date-chip" label>
							{{ item.date }}
						</v-chip>
					</div>

					<!-- 消息气泡 -->
					<MessageBubble v-else :message="item" :user="conversation.user" />
				</div>
			</div>
		</div>

		<!-- 消息输入框 - 始终固定在底部 -->
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
	</v-sheet>
</template>

<script setup>
import { ref, nextTick, watch, computed, onMounted, onBeforeUnmount, defineAsyncComponent } from 'vue'
import { useUserStore } from '@/stores/user'
import ChatHeader from './ChatHeader.vue'
import MessageBubble from './MessageBubble.vue'
import MessageInput from './MessageInput.vue'
import { $notify } from '@/plugins/notification'
import { formatTime } from '@/utils/formatTime'

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
			console.log('触发加载更多消息...')
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
			console.error('加载更多消息失败:', error)
		} finally {
			isLoadingMore.value = false
		}
	}
}
// 添加日期分隔线的消息列表
const messagesWithDividers = computed(() => {
	const messages = props.conversation.messages || []
	const result = []
	let lastDate = null

	messages.forEach(message => {
		const messageDate = new Date(message.createdTime).toLocaleDateString()

		if (messageDate !== lastDate) {
			result.push({
				isDivider: true,
				date: formatTime(message.createdTime),
			})
			lastDate = messageDate
		}

		result.push(message)
	})

	return result
})
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
const scrollToBottom = (smooth = true) => {
	nextTick(() => {
		if (messagesContainer.value) {
			messagesContainer.value.scrollTo({
				top: messagesContainer.value.scrollHeight,
				behavior: smooth ? 'smooth' : 'auto',
			})
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
<style scoped>
.chat-panel {
	position: relative;
	height: 100%;
}

/* 消息列表容器*/
.message-list-container {
	flex: 1;
	min-height: 0;
	overflow-y: auto;
	overflow-x: hidden;
	position: relative;
	background-color: rgb(var(--v-theme-background));
}

/* 消息列表内容 */
.message-list-content {
	min-height: 100%;
	display: flex;
	flex-direction: column;
	justify-content: flex-end; /* 消息从底部开始排列 */
}

/* 空状态 */
.empty-message-state {
	position: absolute;
	top: 50%;
	left: 50%;
	transform: translate(-50%, -50%);
	display: flex;
	flex-direction: column;
	align-items: center;
	text-align: center;
	padding: 48px 24px;
	pointer-events: none;
}

/* 滚动条样式 */
.message-list-container::-webkit-scrollbar {
	width: 6px;
}

.message-list-container::-webkit-scrollbar-track {
	background: transparent;
}

.message-list-container::-webkit-scrollbar-thumb {
	background-color: rgba(var(--v-theme-on-surface), 0.15);
	border-radius: 3px;
	transition: background-color 0.2s;
}

.message-list-container::-webkit-scrollbar-thumb:hover {
	background-color: rgba(var(--v-theme-on-surface), 0.25);
}

/* 日期分隔线 */
.message-date-divider {
	position: relative;
	text-align: center;
	margin: 16px 0;
}

.message-date-divider .v-divider {
	position: absolute;
	top: 50%;
	left: 0;
	right: 0;
	transform: translateY(-50%);
}

.date-chip {
	position: relative;
	z-index: 1;
	font-size: 0.75rem;
}
</style>
