<template>
	<div :class="['message-bubble-wrapper', { 'message-bubble-wrapper--self': message.isSelf }]">
		<!-- 对方消息 -->
		<div v-if="!message.isSelf" class="d-flex align-start ga-2 mb-3">
			<v-avatar :image="message.senderAvatar" size="32" color="grey-lighten-2">
				<v-icon v-if="!message.senderAvatar" icon="mdi-account" size="20"></v-icon>
			</v-avatar>

			<div class="message-content">
				<div class="message-bubble message-bubble--incoming">
					<!-- 文本消息 -->
					<p v-if="message.messageType === 1" class="message-text">{{ message.content }}</p>

					<!-- 图片消息 -->
					<div v-else-if="message.messageType === 2" class="message-images">
						<v-img
							v-for="(url, idx) in message.imgUris"
							:key="idx"
							:src="url"
							width="180"
							cover
							class="message-image"
						></v-img>
					</div>

					<!-- 视频消息 -->
					<div v-else-if="message.messageType === 4" class="message-video">
						<video :src="message.videoUri" controls class="video-player"></video>
					</div>
				</div>

				<div class="message-meta mt-1">
					<span class="text-caption text-disabled">{{ formatTime(message.createdTime) }}</span>
				</div>
			</div>
		</div>

		<!-- 自己的消息 -->
		<div v-else class="d-flex align-start justify-end ga-2 mb-3">
			<div class="message-content text-right">
				<div v-if="message.messageType === 1" class="message-bubble message-bubble--outgoing">
					<!-- 文本消息 -->
					<p class="message-text">{{ message.content }}</p>
				</div>
				<!-- 图片消息 -->
				<div v-else-if="message.messageType === 2" class="message-images">
					<v-img
						v-for="(img, idx) in message.imgUris"
						:key="idx"
						:src="img"
						width="180"
						aspect-ratio="16/9"
						cover
						class="message-image"
					></v-img>
				</div>

				<!-- 视频消息 -->
				<div v-else-if="message.messageType === 4" class="message-video">
					<video :src="message.videoUri" controls class="video-player"></video>
				</div>

				<div class="message-meta mt-1 d-flex align-center justify-end ga-1">
					<span class="text-caption text-disabled">{{ formatTime(message.createdTime) }}</span>
					<v-icon v-if="!message.sending" icon="mdi-check-all" size="14" color="primary"></v-icon>
				</div>
			</div>

			<v-avatar :image="message.senderAvatar" size="32" color="grey-lighten-2">
				<v-icon v-if="!message.senderAvatar" icon="mdi-account" size="20"></v-icon>
			</v-avatar>
		</div>
	</div>
</template>

<script setup>
import { formatTime } from '@/utils/formatTime'

defineProps({
	message: {
		type: Object,
		required: true,
	},
	user: {
		type: Object,
		required: true,
	},
})
</script>
<style scoped>
.message-bubble-wrapper {
	max-width: 100%;
	animation: messageSlideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes messageSlideIn {
	from {
		opacity: 0;
		transform: translateY(10px);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}

.message-content {
	max-width: 70%;
}

.message-bubble {
	display: inline-block;
	padding: 10px 16px;
	border-radius: 16px;
	word-wrap: break-word;
	word-break: break-word;
	position: relative;
}

.message-bubble--incoming {
	background-color: rgb(var(--v-theme-surface));
	color: rgb(var(--v-theme-on-surface));
	border-bottom-left-radius: 4px;
}

.message-bubble--outgoing {
	background-color: rgb(var(--v-theme-primary));
	color: rgb(var(--v-theme-on-primary));
	border-bottom-right-radius: 4px;
}

.message-text {
	margin: 0;
	line-height: 1.5;
	font-size: 0.9375rem;
}

.message-images {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
}

.message-image {
	cursor: pointer;
	transition: transform 0.2s;
}

.message-image:hover {
	transform: scale(1.05);
}

.video-player {
	max-width: 300px;
	border-radius: 8px;
}

.message-meta {
	display: flex;
	align-items: center;
}
</style>
