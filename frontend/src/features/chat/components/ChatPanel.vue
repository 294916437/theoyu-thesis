<script setup>
import { ref, nextTick, watch, computed, onMounted, onBeforeUnmount, defineAsyncComponent, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import MessageBubble from './MessageBubble.vue'
import { $notify } from '@/plugins/notification'
import { formatTime } from '@/utils/formatTime'
import { uploadFile } from '@/api/file'
import { useFilePreview } from '@/composables/useFilePreview'
import FilePreview from '@/components/common/FilePreview.vue'
import { getUserOnlineStatus, setUserOnlineStatus, setUserOfflineStatus } from '@/api/user'

// 延迟加载视频通话相关组件
const VideoCall = defineAsyncComponent(() => import('./VideoCall.vue'))
const IncomingCallNotification = defineAsyncComponent(() => import('./IncomingCallNotification.vue'))
const props = defineProps({
	conversation: {
		type: Object,
		required: true,
	},
})

// 对方用户在线状态
const targetUserOnlineStatus = ref('未知状态')
const emit = defineEmits(['send-message', 'load-more'])
const messageText = ref('')
const userStore = useUserStore()
// 图片选择器
const imageInputRef = ref(null)
const uploadingImage = ref(false)

// 文件选择器
const fileInputRef = ref(null)
const uploadingFile = ref(false)

// 视频选择器
const videoInputRef = ref(null)
const uploadingVideo = ref(false)
// 消息列表引用
const messagesContainer = ref(null)
const isLoadingMore = ref(false)
const scrollThreshold = 100
// 视频通话状态
const isVideoCallActive = ref(false)
const localStream = ref(null)
const remoteStream = ref(null)
const incomingCall = ref(null)
const videoCallReady = ref(false)
const callState = ref('idle')

let videoCallManager = null
let signalingService = null

// 视频配置
const VIDEO_CONFIG = {
	maxSize: 50 * 1024 * 1024, // 50MB
	maxDuration: 300, // 5分钟
	acceptTypes: ['video/mp4', 'video/webm', 'video/ogg'],
}
// 文件配置
const FILE_CONFIG = {
	maxSize: 100 * 1024 * 1024, // 10MB
	acceptTypes: [
		// 文档
		'application/pdf',
		'application/msword',
		'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
		'application/vnd.ms-excel',
		'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
		'application/vnd.ms-powerpoint',
		'application/vnd.openxmlformats-officedocument.presentationml.presentation',
		// 压缩包
		'application/zip',
		'application/x-rar-compressed',
		'application/x-7z-compressed',
		// 文本
		'text/plain',
		'text/csv',
		// 其他
		'application/json',
	],
}

// 计算用户在线状态显示文本
const userOnlineStatus = computed(() => {
	return targetUserOnlineStatus.value ? '在线' : '离线'
})

// 计算在线状态颜色
const userOnlineStatusColor = computed(() => {
	return targetUserOnlineStatus.value ? 'success' : 'grey'
})

const currentUserId = userStore.userId
// 文件预览
const {
	visible: filePreviewVisible,
	fileUrl: previewFileUrl,
	fileName: previewFileName,
	fileType: previewFileType,
	downloadProgress: fileDownloadProgress,
	openPreview,
	closePreview,
	downloadFile,
} = useFilePreview()

// 处理文件预览
const handleFilePreview = ({ url, fileName }) => {
	openPreview(url, fileName)
}
// 打开通用文件选择器
const handleAddFile = () => {
	if (uploadingFile.value) {
		$notify.warning('文件正在上传中，请稍候')
		return
	}
	fileInputRef.value?.click()
}

// 处理文件下载
const handleFileDownload = ({ url, fileName }) => {
	console.log('下载文件:', { url, fileName })
	downloadFile({ url, name: fileName })
}
// 处理通用文件选择
const handleGeneralFileChange = async event => {
	const file = event.target.files?.[0]

	if (!file) {
		return
	}

	// 验证文件大小
	if (file.size > FILE_CONFIG.maxSize) {
		$notify.error('文件大小不能超过 100MB')
		event.target.value = ''
		return
	}

	// 开始上传
	await handleFileUpload(file)

	// 清空文件选择器
	event.target.value = ''
}

// 上传通用文件
const handleFileUpload = async file => {
	uploadingFile.value = true

	try {
		// 创建 FormData
		const formData = new FormData()
		formData.append('file', file)

		console.log('开始上传文件:', file.name)

		// 调用上传接口
		const response = await uploadFile(formData)

		if (response.success) {
			const fileUrl = response.data

			console.log('文件上传成功:', fileUrl)

			// 格式化文件信息
			const fileSize = formatFileSize(file.size)
			const fileType = file.type
			const fileName = file.name

			// 构造 content: "name|type|size|url"
			const content = `${fileName}|${fileType}|${fileSize}|${fileUrl}`

			// 发送文件消息
			await handleSendFileMessage(content)
		} else {
			throw new Error(response.message || '上传失败')
		}
	} catch (error) {
		console.error('文件上传失败:', error)
		$notify.error('文件上传失败: ' + error.message)
	} finally {
		uploadingFile.value = false
	}
}
// 格式化文件大小
const formatFileSize = bytes => {
	if (bytes === 0) return '0 B'
	const k = 1024
	const sizes = ['B', 'KB', 'MB', 'GB']
	const i = Math.floor(Math.log(bytes) / Math.log(k))
	return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

// 发送文件消息
const handleSendFileMessage = async content => {
	if (!content) {
		console.warn('文件内容为空')
		return
	}

	emit('send-message', {
		type: 'file',
		content: content,
	})

	nextTick(() => {
		scrollToBottom()
	})
}
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

	const { scrollTop, scrollHeight } = messagesContainer.value

	// 当滚动到接近顶部时，加载更多历史消息
	if (scrollTop < scrollThreshold && props.conversation.hasMore) {
		isLoadingMore.value = true

		// 记录当前滚动位置
		const oldScrollHeight = scrollHeight
		const oldScrollTop = scrollTop

		try {
			console.log('触发加载更多消息...')
			emit('load-more')

			// 等待 DOM 更新后恢复滚动位置
			await nextTick()

			if (messagesContainer.value) {
				const newScrollHeight = messagesContainer.value.scrollHeight
				const heightDiff = newScrollHeight - oldScrollHeight
				// 恢复滚动位置，防止跳动
				messagesContainer.value.scrollTop = oldScrollTop + heightDiff
			}
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
// 打开文件选择器
const handleAddMedia = () => {
	if (uploadingImage.value) {
		$notify.warning('图片正在上传中，请稍候')
		return
	}
	imageInputRef.value?.click()
}
// 处理文件选择
const handleFileChange = async event => {
	const file = event.target.files?.[0]

	if (!file) {
		return
	}

	// 验证文件类型
	if (!file.type.startsWith('image/')) {
		$notify.error('请选择图片文件')
		event.target.value = ''
		return
	}

	// 验证文件大小 (限制 10MB)
	const maxSize = 10 * 1024 * 1024
	if (file.size > maxSize) {
		$notify.error('图片大小不能超过 10MB')
		event.target.value = ''
		return
	}

	// 开始上传
	await handleImageUpload(file)

	// 清空文件选择器
	event.target.value = ''
}
// 上传图片
const handleImageUpload = async file => {
	uploadingImage.value = true

	try {
		// 创建 FormData
		const formData = new FormData()
		formData.append('file', file)

		console.log('开始上传图片:', file.name)

		// 调用上传接口
		const response = await uploadFile(formData)

		if (response.success && response.data) {
			const imageUrl = response.data

			console.log('图片上传成功:', imageUrl)

			// 发送图片消息
			await handleSendImageMessage(imageUrl)

			$notify.success('图片发送成功')
		} else {
			throw new Error(response.message || '上传失败')
		}
	} catch (error) {
		console.error('图片上传失败:', error)
		$notify.error('图片上传失败: ' + error.message)
	} finally {
		uploadingImage.value = false
	}
}
// 打开视频选择器
const handleAddVideo = () => {
	if (uploadingVideo.value) {
		$notify.warning('视频正在上传中，请稍候')
		return
	}
	videoInputRef.value?.click()
}

// 处理视频文件选择
const handleVideoFileChange = async event => {
	const file = event.target.files?.[0]

	if (!file) {
		return
	}

	// 验证文件类型
	if (!VIDEO_CONFIG.acceptTypes.includes(file.type)) {
		$notify.error('请选择支持的视频格式 (MP4/WebM/OGG)')
		event.target.value = ''
		return
	}

	// 验证文件大小
	if (file.size > VIDEO_CONFIG.maxSize) {
		$notify.error('视频大小不能超过 50MB')
		event.target.value = ''
		return
	}

	// 开始上传
	await handleVideoUpload(file)

	// 清空文件选择器
	event.target.value = ''
}

// 上传视频
const handleVideoUpload = async file => {
	uploadingVideo.value = true

	try {
		// 创建 FormData
		const formData = new FormData()
		formData.append('file', file)

		console.log('开始上传视频:', file.name)

		// 调用上传接口
		const response = await uploadFile(formData)

		if (response.success) {
			const videoUrl = response.data

			console.log('视频上传成功:', videoUrl)

			// 发送视频消息
			await handleSendVideoMessage(videoUrl)

			$notify.success('视频发送成功')
		} else {
			throw new Error(response.message || '上传失败')
		}
	} catch (error) {
		console.error('视频上传失败:', error)
		$notify.error('视频上传失败: ' + error.message)
	} finally {
		uploadingVideo.value = false
	}
}

// 发送视频消息
const handleSendVideoMessage = async videoUrl => {
	if (!videoUrl) {
		console.warn('视频URL为空')
		return
	}

	emit('send-message', {
		type: 'video',
		videoUri: videoUrl,
	})

	nextTick(() => {
		scrollToBottom()
	})
}
// 发送消息
const handleSendTextMessage = () => {
	if (!messageText.value.trim()) return
	emit('send-message', {
		type: 'text',
		content: messageText.value,
	})
	messageText.value = ''
	nextTick(() => {
		scrollToBottom()
	})
}
// 发送图片消息
const handleSendImageMessage = async imageUrl => {
	if (!imageUrl) {
		console.warn('图片URL为空')
		return
	}

	emit('send-message', {
		type: 'image',
		imageUris: [imageUrl],
	})

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
		console.log('发起视频通话')
		callState.value = 'calling'
		const result = await videoCallManager.startCall(props.conversation.user.userId)
		localStream.value = result.localStream
		isVideoCallActive.value = true
		console.log('Local stream tracks:', localStream.value?.getTracks())
		watchRemoteStream()
		watchCallState()
	} catch (error) {
		console.error('视频通话失败:', error)
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
const fetchUserOnlineStatus = async () => {
	try {
		const res = await getUserOnlineStatus(props.conversation.user.userId)
		if (res.success) {
			return res.data.online
		}
		return false
	} catch (error) {
		console.error('获取用户在线状态异常:', error)
		return false
	}
}
// 组件挂载时立即初始化视频通话功能
onMounted(async () => {
	// 初始化视频通话功能
	await initVideoCall()
	// 设置自己的状态为在线
	setUserOnlineStatus(userStore.userId)
	// 获取对方用户在线状态
	targetUserOnlineStatus.value = await fetchUserOnlineStatus()
})
onUnmounted(() => {
	// 设置自己的状态为离线
	setUserOfflineStatus(userStore.userId)
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
<template>
	<v-sheet color="background" class="chat-panel" elevation="0">
		<!-- 聊天头部 -->
		<v-sheet color="surface" class="chat-header px-4 py-3" elevation="1">
			<div class="d-flex align-center justify-space-between">
				<div class="d-flex align-center ga-3">
					<v-avatar size="42" color="primary">
						<v-img v-if="conversation.user.avatar" :src="conversation.user.avatar" :alt="conversation.user.userName">
							<template #error>
								<v-icon icon="mdi-account" size="16"></v-icon>
							</template>
						</v-img>
						<v-icon v-else icon="mdi-account" size="16"></v-icon>
					</v-avatar>

					<div>
						<h3 class="text-subtitle-1 font-weight-bold">
							{{ conversation.user.nickname }}
						</h3>
						<!-- 在线状态文本 -->
						<div class="d-flex align-center ga-1">
							<v-icon :icon="targetUserOnlineStatus ? 'mdi-circle' : 'mdi-circle-outline'" :color="userOnlineStatusColor" size="12"></v-icon>
							<p class="text-caption text-medium-emphasis">{{ userOnlineStatus }}</p>
						</div>
					</div>
				</div>

				<div class="d-flex ga-2">
					<v-btn icon="mdi-video-outline" variant="text" size="default" color="primary" @click="handleVideoCall">
						<v-icon size="22"></v-icon>
					</v-btn>
					<v-btn icon="mdi-information-outline" variant="text" size="default" color="primary">
						<v-icon size="22"></v-icon>
					</v-btn>
				</div>
			</div>
		</v-sheet>

		<!-- 消息列表 -->
		<div ref="messagesContainer" class="message-list-container" @scroll="handleScroll">
			<!-- 空状态 -->
			<div v-if="conversation.messages.length === 0" class="empty-message-state">
				<v-icon icon="mdi-message-outline" size="64" color="grey-lighten-1"></v-icon>
				<p class="text-body-1 text-disabled mt-4">暂无消息，开始聊天吧</p>
			</div>

			<!-- 消息列表内容 -->
			<div v-else class="message-list-content px-4 py-3">
				<!-- 加载更多按钮 -->
				<div v-if="conversation.hasMore" class="text-center py-3">
					<v-btn variant="text" size="small" color="primary" prepend-icon="mdi-chevron-up" :loading="isLoadingMore" @click="handleScroll"> 加载更多消息 </v-btn>
				</div>
				<div v-for="(item, index) in messagesWithDividers" :key="item.id || `divider-${index}`">
					<!-- 日期分隔线 -->
					<div v-if="item.isDivider" class="message-date-divider my-4">
						<v-divider></v-divider>
						<v-chip size="small" color="surface-variant" class="date-chip" label>
							{{ item.date }}
						</v-chip>
					</div>

					<!-- 消息气泡 -->
					<MessageBubble v-else :message="item" :user="conversation.user" @preview-file="handleFilePreview" @download-file="handleFileDownload" />
				</div>
			</div>
		</div>

		<!-- 消息输入框 - 始终固定在底部 -->
		<v-sheet color="surface" class="message-input-container" elevation="2">
			<!-- 隐藏的图片选择器 -->
			<input ref="imageInputRef" type="file" accept="image/*" style="display: none" @change="handleFileChange" />

			<!-- 隐藏的视频选择器 -->
			<input ref="videoInputRef" type="file" accept="video/mp4,video/webm,video/ogg" style="display: none" @change="handleVideoFileChange" />

			<!-- 隐藏的通用文件选择器 -->
			<input ref="fileInputRef" type="file" :accept="FILE_CONFIG.acceptTypes.join(',')" style="display: none" @change="handleGeneralFileChange" />
			<div class="input-wrapper">
				<!-- 左侧功能按钮 -->
				<!-- 左侧功能按钮 -->
				<div class="input-actions">
					<!-- 视频按钮 -->
					<v-tooltip text="发送视频" location="top">
						<template #activator="{ props }">
							<v-btn
								v-bind="props"
								icon="mdi-video-image"
								variant="text"
								size="default"
								color="primary"
								:loading="uploadingVideo"
								:disabled="uploadingVideo"
								@click="handleAddVideo"
							>
								<v-icon size="22"></v-icon>
							</v-btn>
						</template>
					</v-tooltip>

					<!-- 图片按钮 -->
					<v-tooltip text="发送图片" location="top">
						<template #activator="{ props }">
							<v-btn
								v-bind="props"
								icon="mdi-image"
								variant="text"
								size="default"
								color="primary"
								:loading="uploadingImage"
								:disabled="uploadingImage"
								@click="handleAddMedia"
							>
								<v-icon size="22"></v-icon>
							</v-btn>
						</template>
					</v-tooltip>

					<!-- 文件按钮 (新增) -->
					<v-tooltip text="发送文件" location="top">
						<template #activator="{ props }">
							<v-btn
								v-bind="props"
								icon="mdi-file-document"
								variant="text"
								size="default"
								color="primary"
								:loading="uploadingFile"
								:disabled="uploadingFile"
								@click="handleAddFile"
							>
								<v-icon size="22"></v-icon>
							</v-btn>
						</template>
					</v-tooltip>
				</div>

				<!-- 输入框 -->
				<div class="input-field-wrapper">
					<v-textarea
						v-model="messageText"
						placeholder="发送一条消息..."
						variant="solo"
						flat
						hide-details
						auto-grow
						rows="1"
						max-rows="4"
						bg-color="surface-variant"
						rounded="xl"
						class="flex-1"
						@keydown.enter.exact.prevent="handleSendTextMessage"
					></v-textarea>
				</div>

				<!-- 发送按钮 -->
				<v-btn icon size="large" color="primary" elevation="0" class="send-btn" :disabled="!messageText.trim()" @click="handleSendTextMessage">
					<v-icon size="24">mdi-send</v-icon>
				</v-btn>
			</div>
		</v-sheet>

		<!-- 视频对话框 -->
		<VideoCall
			v-model:is-active="isVideoCallActive"
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
		<IncomingCallNotification v-if="incomingCall" :caller-name="incomingCall.callerName" @accept="acceptCall" @reject="rejectCall" />
		<!-- 文件预览对话框 -->
		<FilePreview
			v-model="filePreviewVisible"
			:file-url="previewFileUrl"
			:file-name="previewFileName"
			:file-type="previewFileType"
			:download-progress="fileDownloadProgress"
			@download="downloadFile"
			@close="closePreview"
		/>
	</v-sheet>
</template>

<style scoped>
.chat-panel {
	display: flex;
	flex-direction: column;
	position: relative;
	height: calc(100vh - 64px); /* 减去顶部导航栏 */
}
/* 聊天头部样式 */
.chat-header {
	border-bottom: 1px solid rgba(var(--v-theme-on-surface), 0.08);
	min-height: 72px;
	flex-shrink: 0;
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
	overflow-y: scroll;
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
/* 消息输入容器 */
.message-input-container {
	border-top: 1px solid rgba(var(--v-theme-on-surface), 0.08);
	padding: 12px 16px;
	background-color: rgb(var(--v-theme-surface));
	flex-shrink: 0;
}

.input-wrapper {
	display: flex;
	align-items: flex-end;
	gap: 12px;
	max-width: 1200px;
	margin: 0 auto;
}

.input-actions {
	display: flex;
	gap: 4px;
	flex-shrink: 0;
}

.input-field-wrapper {
	flex: 1;
	min-width: 0;
}

.send-btn {
	flex-shrink: 0;
	border-radius: 50%;
	transition: all 0.2s;
}

.send-btn:not(:disabled):hover {
	transform: scale(1.05);
}

.send-btn:disabled {
	opacity: 0.5;
}

@media (max-width: 960px) {
	.input-wrapper {
		padding: 0;
	}

	.input-actions {
		gap: 0;
	}
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
