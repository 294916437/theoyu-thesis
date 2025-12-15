<template>
	<v-sheet color="surface" class="message-input-container" elevation="2">
		<div class="input-wrapper">
			<!-- 左侧功能按钮 -->
			<div class="input-actions">
				<v-btn icon="mdi-video" variant="text" size="default" color="primary" @click="handleVideoCall">
					<v-icon size="22"></v-icon>
				</v-btn>
				<v-btn icon="mdi-image" variant="text" size="default" color="primary" @click="handleAddMedia">
					<v-icon size="22"></v-icon>
				</v-btn>
				<v-btn icon="mdi-emoticon-happy-outline" variant="text" size="default" color="primary">
					<v-icon size="22"></v-icon>
				</v-btn>
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
					@keydown.enter.exact.prevent="handleSend"
				></v-textarea>
			</div>

			<!-- 发送按钮 -->
			<v-btn
				icon
				size="large"
				color="primary"
				elevation="0"
				class="send-btn"
				:disabled="!messageText.trim()"
				@click="handleSend"
			>
				<v-icon size="24">mdi-send</v-icon>
			</v-btn>
		</div>
	</v-sheet>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['send', 'video-call'])

const messageText = ref('')

const handleSend = () => {
	if (!messageText.value.trim()) return
	emit('send', messageText.value)
	messageText.value = ''
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
	border-top: 1px solid rgba(var(--v-theme-on-surface), 0.08);
	padding: 12px 16px;
	background-color: rgb(var(--v-theme-surface));
}

.input-wrapper {
	display: flex;
	align-items: flex-end;
	gap: 12px;
	max-width: 1200px; /* 限制最大宽度 */
	margin: 0 auto;
}

.input-actions {
	display: flex;
	gap: 4px;
	flex-shrink: 0;
}

.input-field-wrapper {
	flex: 1;
	min-width: 0; /* 防止 flex 溢出 */
}

.message-textarea {
	border-radius: 24px !important;
	background-color: rgb(var(--v-theme-background));
}

.message-textarea :deep(.v-field) {
	border-radius: 24px;
	padding: 8px 16px;
}

.message-textarea :deep(.v-field__input) {
	padding: 0;
	min-height: 40px;
	line-height: 1.5;
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

/* 响应式调整 */
@media (max-width: 960px) {
	.input-wrapper {
		padding: 0;
	}

	.input-actions {
		gap: 0;
	}
}
</style>
