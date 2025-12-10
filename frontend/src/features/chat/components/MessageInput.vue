<template>
	<div class="px-4 py-3 border-t border-gray-200">
		<div class="flex items-end gap-2">
			<!-- 工具按钮 -->
			<div class="flex gap-1 mb-2">
				<button class="tool-button" aria-label="Video call" @click="handleVideoCall">
					<svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
						<path
							d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z"
						/>
					</svg>
				</button>
				<button class="tool-button" aria-label="Add media" @click="handleAddMedia">
					<svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
						<path
							d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"
						/>
					</svg>
				</button>
				<button class="tool-button" aria-label="Add emoji">
					<svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
						<path
							d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"
						/>
					</svg>
				</button>
			</div>

			<!-- 输入框 -->
			<div class="flex-1 relative">
				<textarea
					ref="textareaRef"
					v-model="messageText"
					placeholder="发送一条消息..."
					rows="1"
					class="message-textarea"
					@keydown.enter.exact.prevent="handleSend"
					@input="adjustHeight"
				/>
			</div>

			<!-- 发送按钮 -->
			<button
				class="send-button"
				:class="{ 'send-button--active': canSend, 'send-button--disabled': !canSend }"
				:disabled="!canSend"
				aria-label="Send message"
				@click="handleSend"
			>
				<svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
					<path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
				</svg>
			</button>
		</div>
	</div>
</template>

<script setup>
import { ref, computed } from 'vue'

const emit = defineEmits(['send', 'video-call'])

const messageText = ref('')
const textareaRef = ref(null)

// 是否可以发送
const canSend = computed(() => {
	return messageText.value.trim().length > 0
})

// 发送消息
const handleSend = () => {
	if (!canSend.value) return

	emit('send', messageText.value)
	messageText.value = ''

	// 重置高度
	if (textareaRef.value) {
		textareaRef.value.style.height = 'auto'
	}
}

// 自动调整高度
const adjustHeight = () => {
	if (textareaRef.value) {
		textareaRef.value.style.height = 'auto'
		textareaRef.value.style.height = textareaRef.value.scrollHeight + 'px'
	}
}

// 添加媒体
const handleAddMedia = () => {
	// TODO: 实现文件上传
	console.log('Add media')
}
// 发起视频通话
const handleVideoCall = () => {
	emit('video-call')
}
</script>

<style scoped>
.tool-button {
	padding: 0.5rem;
	color: rgb(59 130 246);
	border-radius: 9999px;
	transition: background-color 0.2s;
	cursor: pointer;
}

.tool-button:hover {
	background-color: rgb(239 246 255);
}

.message-textarea {
	width: 100%;
	padding: 0.5rem 2.5rem 0.5rem 1rem;
	background-color: rgb(243 244 246);
	border-radius: 1.5rem;
	resize: none;
	outline: none;
	font-size: 0.875rem;
	max-height: 8rem;
	overflow-y: auto;
	transition: background-color 0.2s;
}

.message-textarea:focus {
	background-color: rgb(229 231 235);
}

.send-button {
	padding: 0.5rem;
	margin-bottom: 0.25rem;
	border-radius: 9999px;
	transition: all 0.2s;
}

.send-button--active {
	background-color: rgb(59 130 246);
	color: white;
}

.send-button--active:hover {
	background-color: rgb(37 99 235);
}

.send-button--disabled {
	background-color: rgb(209 213 219);
	color: rgb(107 114 128);
	cursor: not-allowed;
}
</style>
