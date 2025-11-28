<template>
	<div class="chat-panel">
		<!-- 标题栏 -->
		<div class="chat-header">
			<span class="text-subtitle-1 font-weight-medium">会议聊天</span>
			<v-menu>
				<template #activator="{ props }">
					<v-btn icon="mdi-dots-vertical" size="small" variant="text" v-bind="props"></v-btn>
				</template>
				<v-list density="compact" class="chat-menu">
					<v-list-item @click="handleSaveChat">
						<template #prepend>
							<v-icon>mdi-download</v-icon>
						</template>
						<v-list-item-title>保存聊天记录</v-list-item-title>
					</v-list-item>
					<v-list-item @click="handleClearChat">
						<template #prepend>
							<v-icon>mdi-delete</v-icon>
						</template>
						<v-list-item-title>清空聊天</v-list-item-title>
					</v-list-item>
				</v-list>
			</v-menu>
		</div>

		<v-divider></v-divider>

		<!-- 消息列表 -->
		<div ref="messageContainer" class="message-container">
			<!-- 加载更多按钮 -->
			<div v-if="hasMoreMessages" class="load-more-wrapper">
				<v-btn
					variant="text"
					size="small"
					:loading="loadingMore"
					class="load-more-btn"
					@click="loadMoreMessages"
				>
					加载更多消息
				</v-btn>
			</div>

			<div v-for="(group, date) in groupedMessages" :key="date" class="message-date-group">
				<!-- 日期分隔符 -->
				<div class="date-divider">
					<span class="date-text">{{ formatDate(date) }}</span>
				</div>

				<!-- 消息 -->
				<div
					v-for="message in group"
					:key="message.id"
					class="message-wrapper"
					:class="{ 'message-own': message.isOwn }"
				>
					<div class="message-content">
						<!-- 他人消息头部 -->
						<div v-if="!message.isOwn" class="message-header">
							<v-avatar size="28" :color="message.avatarColor || 'primary'">
								<span class="text-caption text-white">{{ getInitials(message.userName) }}</span>
							</v-avatar>
							<span class="message-sender ml-2">{{ message.userName }}</span>
							<span class="message-time ml-2">{{ formatTime(message.timestamp) }}</span>
						</div>

						<div class="message-bubble" :class="{ 'message-own-bubble': message.isOwn }">
							<!-- 文本消息 -->
							<div v-if="message.type === 'text'" class="message-text">
								{{ message.content }}
							</div>

							<!-- 文件消息 -->
							<div v-else-if="message.type === 'file'" class="message-file">
								<v-icon left size="20">{{ getFileIcon(message.file.type) }}</v-icon>
								<div class="file-info">
									<div class="file-name">{{ message.file.name }}</div>
									<div class="file-size text-caption">
										{{ formatFileSize(message.file.size) }}
									</div>
								</div>
								<v-btn
									icon="mdi-download"
									size="x-small"
									variant="text"
									@click="downloadFile(message.file)"
								></v-btn>
							</div>

							<!-- 系统消息 -->
							<div v-else-if="message.type === 'system'" class="message-system">
								<v-icon size="small" class="mr-1">{{ message.icon }}</v-icon>
								{{ message.content }}
							</div>

							<!-- 自己消息的时间 -->
							<div v-if="message.isOwn" class="message-time-own">
								{{ formatTime(message.timestamp) }}
							</div>
						</div>
					</div>
				</div>
			</div>

			<!-- 正在输入指示器 -->
			<div v-if="typingUsers.length > 0" class="typing-indicator">
				<v-avatar size="24" color="grey-darken-1">
					<span class="typing-dots text-white">...</span>
				</v-avatar>
				<span class="ml-2 text-caption"> {{ typingUsers.join(', ') }} 正在输入... </span>
			</div>

			<!-- 空状态 -->
			<div v-if="props.messages.length === 0" class="empty-state">
				<v-icon size="64" color="grey-darken-1" class="mb-4">mdi-chat-outline</v-icon>
				<div class="text-body-2 text-grey">暂无消息</div>
				<div class="text-caption text-grey-darken-1">发送消息开始聊天</div>
			</div>
		</div>

		<!-- 输入区域 -->
		<v-divider></v-divider>

		<div class="input-area">
			<!-- 文件上传进度 -->
			<v-progress-linear
				v-if="uploadProgress > 0"
				:model-value="uploadProgress"
				color="primary"
				height="3"
				class="upload-progress"
			></v-progress-linear>

			<!-- 回复消息预览 -->
			<v-sheet v-if="replyingTo" color="surface-variant" class="reply-preview">
				<div class="d-flex justify-space-between align-center">
					<div class="text-caption d-flex align-center">
						<v-icon size="small" class="mr-1">mdi-reply</v-icon>
						回复 <span class="font-weight-medium ml-1">{{ replyingTo.userName }}</span>
					</div>
					<v-btn icon="mdi-close" size="x-small" variant="text" @click="replyingTo = null"></v-btn>
				</div>
				<div class="text-caption text-medium-emphasis mt-1 reply-content">
					{{ replyingTo.content }}
				</div>
			</v-sheet>

			<!-- 消息输入框 -->
			<div class="input-wrapper">
				<v-menu location="top">
					<template #activator="{ props }">
						<v-btn
							icon="mdi-emoticon-happy-outline"
							variant="text"
							size="small"
							v-bind="props"
							class="emoji-btn"
						></v-btn>
					</template>
					<EmojiPicker @select="insertEmoji" />
				</v-menu>

				<v-textarea
					v-model="messageInput"
					variant="outlined"
					density="compact"
					placeholder="输入消息..."
					hide-details
					rows="1"
					auto-grow
					max-rows="4"
					class="message-input"
					@keydown.enter.exact.prevent="sendMessage"
					@keydown.shift.enter.exact="addNewLine"
					@input="handleTyping"
				></v-textarea>

				<input ref="fileInput" type="file" hidden @change="handleFileSelect" />

				<v-btn
					icon="mdi-paperclip"
					variant="text"
					size="small"
					class="attach-btn"
					@click="$refs.fileInput.click()"
				></v-btn>

				<v-btn
					icon="mdi-send"
					color="primary"
					size="small"
					:disabled="!messageInput.trim()"
					class="send-btn"
					@click="sendMessage"
				></v-btn>
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { useDateFormat, useScroll, useThrottleFn } from '@vueuse/core'
import { useNotification } from '@/composables/useNotification'
import EmojiPicker from './EmojiPicker.vue'

const props = defineProps({
	messages: {
		type: Array,
		default: () => [],
	},
	currentUserId: {
		type: String,
		required: true,
	},
})

const emit = defineEmits(['send-message', 'message-read', 'load-more', 'file-upload'])

const { showSuccess, showError } = useNotification()

const messageContainer = ref(null)
const fileInput = ref(null)
const messageInput = ref('')
const replyingTo = ref(null)
const typingUsers = ref([])
const hasMoreMessages = ref(false)
const loadingMore = ref(false)
const uploadProgress = ref(0)

const { arrivedState } = useScroll(messageContainer, {
	offset: { bottom: 50 },
})

// 按日期分组消息
const groupedMessages = computed(() => {
	const groups = {}

	props.messages.forEach(msg => {
		const date = useDateFormat(msg.timestamp, 'YYYY-MM-DD').value
		if (!groups[date]) {
			groups[date] = []
		}
		groups[date].push({
			...msg,
			isOwn: msg.userId === props.currentUserId,
		})
	})

	return groups
})

const formatDate = date => {
	const today = useDateFormat(new Date(), 'YYYY-MM-DD').value
	const yesterday = useDateFormat(new Date(Date.now() - 86400000), 'YYYY-MM-DD').value

	if (date === today) return '今天'
	if (date === yesterday) return '昨天'
	return useDateFormat(date, 'MM月DD日').value
}

const formatTime = timestamp => {
	return useDateFormat(timestamp, 'HH:mm').value
}

const getInitials = name => {
	return name
		.split(' ')
		.map(word => word[0])
		.join('')
		.toUpperCase()
		.slice(0, 2)
}

const getFileIcon = fileType => {
	const iconMap = {
		image: 'mdi-file-image',
		video: 'mdi-file-video',
		audio: 'mdi-file-music',
		pdf: 'mdi-file-pdf-box',
		document: 'mdi-file-document',
		archive: 'mdi-folder-zip',
	}
	return iconMap[fileType] || 'mdi-file'
}

const formatFileSize = bytes => {
	if (bytes === 0) return '0 B'
	const k = 1024
	const sizes = ['B', 'KB', 'MB', 'GB']
	const i = Math.floor(Math.log(bytes) / Math.log(k))
	return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

const scrollToBottom = async () => {
	await nextTick()
	if (messageContainer.value) {
		messageContainer.value.scrollTop = messageContainer.value.scrollHeight
	}
}

const sendMessage = async () => {
	if (!messageInput.value.trim()) return

	const message = {
		type: 'text',
		content: messageInput.value.trim(),
		replyTo: replyingTo.value?.id,
	}

	emit('send-message', message)
	messageInput.value = ''
	replyingTo.value = null
	await scrollToBottom()
}

const addNewLine = () => {
	messageInput.value += '\n'
}

const insertEmoji = emoji => {
	messageInput.value += emoji
}

const handleFileSelect = async event => {
	const file = event.target.files[0]
	if (!file) return

	// 文件大小限制 10MB
	if (file.size > 10 * 1024 * 1024) {
		showError('文件大小不能超过10MB')
		return
	}

	try {
		uploadProgress.value = 0

		// 预留文件上传API
		// 模拟上传进度
		const uploadInterval = setInterval(() => {
			uploadProgress.value += 10
			if (uploadProgress.value >= 100) {
				clearInterval(uploadInterval)
				setTimeout(() => {
					uploadProgress.value = 0
					emit('file-upload', {
						type: 'file',
						file: {
							name: file.name,
							size: file.size,
							type: file.type.split('/')[0],
							url: URL.createObjectURL(file),
						},
					})
				}, 500)
			}
		}, 200)
	} catch (error) {
		showError('文件上传失败')
		uploadProgress.value = 0
	}

	fileInput.value.value = ''
}

const downloadFile = async file => {
	// 预留文件下载API
	window.open(file.url, '_blank')
}

const handleTyping = useThrottleFn(() => {
	// 预留API: 发送正在输入状态
	console.log('User is typing...')
}, 1000)

const loadMoreMessages = async () => {
	loadingMore.value = true
	try {
		// 预留加载更多消息API
		await emit('load-more')
	} finally {
		loadingMore.value = false
	}
}

const handleSaveChat = async () => {
	// 预留保存聊天记录API
	const chatText = props.messages.map(m => `[${formatTime(m.timestamp)}] ${m.userName}: ${m.content}`).join('\n')

	const blob = new Blob([chatText], { type: 'text/plain' })
	const url = URL.createObjectURL(blob)
	const a = document.createElement('a')
	a.href = url
	a.download = `chat-${Date.now()}.txt`
	a.click()
	URL.revokeObjectURL(url)
	showSuccess('聊天记录已保存')
}

const handleClearChat = () => {
	// 预留清空聊天API
	if (confirm('确定要清空所有聊天记录吗?')) {
		console.log('Clear chat')
	}
}

// 监听滚动到底部时标记消息已读
watch(
	() => arrivedState.bottom,
	isBottom => {
		if (isBottom) {
			emit('message-read')
		}
	},
)

// 新消息时自动滚动
watch(
	() => props.messages.length,
	() => {
		scrollToBottom()
	},
)
</script>

<style scoped>
.chat-panel {
	display: flex;
	flex-direction: column;
	height: 100%;
	background: transparent;
	overflow: hidden;
}

.chat-header {
	display: flex;
	flex-shrink: 0;
	align-items: center;
	justify-content: space-between;
	padding: 12px 16px;
	background: rgba(255, 255, 255, 0.02);
	border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.chat-menu {
	background-color: rgba(40, 40, 58, 0.98);
	border: 1px solid rgba(255, 255, 255, 0.12);
}

.chat-menu :deep(.v-list-item) {
	color: rgba(255, 255, 255, 0.87);
}

.chat-menu :deep(.v-list-item:hover) {
	background-color: rgba(255, 255, 255, 0.08);
}

.message-container {
	flex: 1;
	overflow-y: auto;
	overflow-x: hidden;
	padding: 16px;
	min-height: 0;
	max-height: 100%;
}

.message-container::-webkit-scrollbar {
	width: 6px;
}

.message-container::-webkit-scrollbar-track {
	background: rgba(255, 255, 255, 0.05);
	border-radius: 3px;
}

.message-container::-webkit-scrollbar-thumb {
	background: rgba(255, 255, 255, 0.2);
	border-radius: 3px;
}

.message-container::-webkit-scrollbar-thumb:hover {
	background: rgba(255, 255, 255, 0.3);
}

.load-more-wrapper {
	text-align: center;
	margin-bottom: 16px;
}

.load-more-btn {
	color: rgba(255, 255, 255, 0.7);
	text-transform: none;
}

.message-date-group {
	margin-bottom: 16px;
}

.date-divider {
	text-align: center;
	margin: 16px 0;
	position: relative;
}

.date-divider::before {
	content: '';
	position: absolute;
	top: 50%;
	left: 0;
	right: 0;
	height: 1px;
	background: linear-gradient(
		to right,
		transparent,
		rgba(255, 255, 255, 0.1) 20%,
		rgba(255, 255, 255, 0.1) 80%,
		transparent
	);
}

.date-text {
	position: relative;
	padding: 4px 12px;
	background: rgba(255, 255, 255, 0.05);
	border-radius: 12px;
	font-size: 11px;
	color: rgba(255, 255, 255, 0.6);
	font-weight: 500;
	letter-spacing: 0.5px;
}

.message-wrapper {
	margin-bottom: 12px;
	animation: fadeIn 0.2s ease-in;
}

@keyframes fadeIn {
	from {
		opacity: 0;
		transform: translateY(10px);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}

.message-wrapper.message-own {
	display: flex;
	justify-content: flex-end;
}

.message-content {
	max-width: 75%;
}

.message-header {
	display: flex;
	align-items: center;
	margin-bottom: 6px;
	padding: 0 4px;
}

.message-sender {
	font-weight: 500;
	font-size: 13px;
	color: rgba(255, 255, 255, 0.9);
}

.message-time {
	font-size: 11px;
	color: rgba(255, 255, 255, 0.5);
}

.message-bubble {
	padding: 10px 14px;
	border-radius: 16px;
	background: rgba(255, 255, 255, 0.08);
	color: rgba(255, 255, 255, 0.95);
	word-wrap: break-word;
	box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
	transition: background-color 0.2s;
}

.message-bubble:hover {
	background: rgba(255, 255, 255, 0.12);
}

.message-own-bubble {
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	border-bottom-right-radius: 4px;
	box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.message-own-bubble:hover {
	background: linear-gradient(135deg, #7b8fef 0%, #8859b5 100%);
}

.message-wrapper:not(.message-own) .message-bubble {
	border-bottom-left-radius: 4px;
}

.message-text {
	word-wrap: break-word;
	white-space: pre-wrap;
	line-height: 1.5;
	font-size: 14px;
}

.message-file {
	display: flex;
	align-items: center;
	gap: 10px;
	padding: 8px;
	background: rgba(0, 0, 0, 0.2);
	border-radius: 10px;
}

.file-info {
	flex: 1;
	min-width: 0;
}

.file-name {
	font-size: 13px;
	font-weight: 500;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.file-size {
	opacity: 0.7;
	font-size: 11px;
	margin-top: 2px;
}

.message-system {
	text-align: center;
	font-size: 12px;
	color: rgba(255, 255, 255, 0.6);
	font-style: italic;
	padding: 4px 8px;
}

.message-time-own {
	font-size: 10px;
	color: rgba(255, 255, 255, 0.7);
	text-align: right;
	margin-top: 4px;
}

.typing-indicator {
	display: flex;
	align-items: center;
	padding: 8px;
	color: rgba(255, 255, 255, 0.6);
	font-size: 12px;
}

.typing-dots {
	animation: typing 1.4s infinite;
	font-size: 16px;
	font-weight: bold;
}

@keyframes typing {
	0%,
	60%,
	100% {
		opacity: 0.3;
	}
	30% {
		opacity: 1;
	}
}

.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	height: 100%;
	color: rgba(255, 255, 255, 0.5);
}

.input-area {
	flex-shrink: 0;
	background: rgba(255, 255, 255, 0.02);
	border-top: 1px solid rgba(255, 255, 255, 0.08);
	padding: 12px;
}

.upload-progress {
	margin-bottom: 8px;
	border-radius: 2px;
}

.reply-preview {
	padding: 8px 12px;
	margin-bottom: 8px;
	border-radius: 8px;
	border-left: 3px solid rgb(102, 126, 234);
}

.reply-content {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	max-width: 100%;
}

.input-wrapper {
	display: flex;
	align-items: flex-end;
	gap: 8px;
}

.message-input {
	flex: 1;
	min-width: 0;
}

.message-input :deep(.v-field) {
	border-radius: 20px;
	background-color: rgba(255, 255, 255, 0.06);
	border-color: rgba(255, 255, 255, 0.12);
}

.message-input :deep(.v-field--focused) {
	background-color: rgba(255, 255, 255, 0.08);
	border-color: rgba(102, 126, 234, 0.5);
}

.message-input :deep(.v-field__input) {
	color: rgba(255, 255, 255, 0.95);
	padding: 8px 16px;
	max-height: 120px;
	overflow-y: auto;
}

.message-input :deep(textarea::placeholder) {
	color: rgba(255, 255, 255, 0.4);
}

.emoji-btn,
.attach-btn {
	flex-shrink: 0;
	color: rgba(255, 255, 255, 0.7);
}

.emoji-btn:hover,
.attach-btn:hover {
	color: rgba(255, 255, 255, 0.9);
	background-color: rgba(255, 255, 255, 0.08);
}

.send-btn {
	flex-shrink: 0;
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.send-btn:hover {
	box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.send-btn:disabled {
	background: rgba(255, 255, 255, 0.1);
	box-shadow: none;
}
</style>
