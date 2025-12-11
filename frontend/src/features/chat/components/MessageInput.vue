<template>
	<v-sheet color="surface" class="message-input-container px-4 py-3" elevation="2">
		<div class="d-flex align-center gap-2">
			<!-- 视频通话按钮 -->
			<v-btn
				icon="mdi-video-outline"
				variant="text"
				size="small"
				color="primary"
				density="comfortable"
				@click="handleVideoCall"
			></v-btn>
			<!-- 图片上传 -->
			<v-btn
				icon="mdi-image-outline"
				variant="text"
				size="small"
				color="primary"
				density="comfortable"
				@click="handleAddMedia"
			></v-btn>
			<!-- 表情按钮 -->
			<v-btn
				icon="mdi-emoticon-outline"
				variant="text"
				size="small"
				color="primary"
				density="comfortable"
			></v-btn>

			<!-- 输入框 -->
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
				class="message-textarea flex-1"
				@keydown.enter.exact.prevent="handleSend"
			></v-textarea>

			<!-- 发送按钮 -->
			<v-btn icon="mdi-send" color="primary" size="small" :disabled="!canSend" @click="handleSend"></v-btn>
		</div>
	</v-sheet>
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

// 添加媒体
const handleAddMedia = () => {
	// TODO: 实现文件上传
}
// 发起视频通话
const handleVideoCall = () => {
	emit('video-call')
}
</script>

<style scoped>
.message-input-container {
	border-top: 1px solid rgb(var(--v-theme-border));
}

.message-textarea:deep(.v-field) {
	box-shadow: none !important;
}

.message-textarea:deep(.v-field__input) {
	padding-top: 8px;
	padding-bottom: 8px;
}

.gap-2 {
	gap: 8px;
}
</style>
