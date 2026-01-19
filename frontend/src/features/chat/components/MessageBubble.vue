<template>
	<div class="message-bubble-wrapper">
		<!-- 对方消息 -->
		<div v-if="!message.isSelf" class="d-flex align-start justify-start ga-2 mb-3">
			<v-avatar size="28" color="primary">
				<v-img v-if="!message.senderAvatar" :src="message.senderAvatar" :alt="message.senderNickname" size="32">
					<template #error>
						<v-icon icon="mdi-account" size="20"></v-icon>
					</template>
				</v-img>
				<v-icon v-else icon="mdi-account" size="20"></v-icon>
			</v-avatar>

			<div class="message-content text-left">
				<!-- 文本消息 -->
				<div v-if="message.messageType === 1" class="message-bubble message-bubble--incoming">
					<!-- 文本消息 -->
					<p class="message-text">{{ message.content }}</p>
				</div>

				<!-- 图片消息 -->
				<div v-else-if="message.messageType === 2" class="message-images">
					<v-img
						v-for="(url, idx) in message.imgUris"
						:key="idx"
						:src="url"
						width="180"
						aspect-ratio="16/9"
						cover
						class="message-image"
						@click="emit('preview-file', { url: imgUrl, fileName: 'default.jpg', type: 'image' })"
						>></v-img
					>
				</div>

				<!-- 视频消息 -->
				<div v-else-if="message.messageType === 4" class="message-video">
					<div class="video-thumbnail clickable" @click="emit('preview-file', { url: message.videoUri, fileName: 'default.mp4' })">
						<v-icon icon="mdi-play-circle" size="48" color="white"></v-icon>
						<video :src="message.videoUri" class="video-preview" @click.stop></video>
					</div>
				</div>

				<!-- 文件消息 -->
				<div v-else-if="message.messageType === 6 && fileInfo" class="message-file">
					<v-card class="file-card clickable" elevation="2" @click="handleFileClick(fileInfo.fileUrl, fileInfo.fileName, getFileType(fileInfo.fileName))">
						<div class="d-flex align-center pa-3 ga-3">
							<!-- 文件图标 -->
							<v-avatar size="40" color="surface-variant">
								<v-icon :icon="getEnhancedFileIcon(fileInfo.fileType)" size="24" color="primary"></v-icon>
							</v-avatar>

							<!-- 文件信息 -->
							<div class="flex-1 text-truncate">
								<div class="text-body-2 font-weight-medium text-truncate">
									{{ fileInfo.fileName }}
								</div>
								<div class="text-caption text-grey">
									{{ fileInfo.fileSize }}
								</div>
							</div>

							<!-- 下载图标 -->
							<v-icon icon="mdi-download" size="20" color="grey-darken-1"></v-icon>
						</div>
					</v-card>
				</div>

				<div class="message-meta mt-1 d-flex align-center justify-start ga-1">
					<span class="text-caption text-disabled">{{ formatTime(message.createdTime) }}</span>
					<v-icon v-if="!message.sending" icon="mdi-check-all" size="14" color="primary"></v-icon>
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
				<div v-if="message.messageType === 2 && message.imgUris?.length" class="message-images">
					<v-img
						v-for="(imgUrl, index) in message.imgUris"
						:key="index"
						:src="imgUrl"
						width="180"
						aspect-ratio="16/9"
						cover
						class="message-image clickable"
						@click="emit('preview-file', { url: imgUrl, fileName: 'default.jpg', type: 'image' })"
					>
						<template #placeholder>
							<div class="d-flex align-center justify-center fill-height">
								<v-progress-circular indeterminate size="24"></v-progress-circular>
							</div>
						</template>
					</v-img>
				</div>

				<!-- 视频消息 -->
				<div v-else-if="message.messageType === 4 && message.videoUri" class="message-video">
					<div class="video-thumbnail clickable" @click="emit('preview-file', { url: message.videoUri, fileName: 'default.mp4' })">
						<v-icon icon="mdi-play-circle" size="48" color="white"></v-icon>
						<video :src="message.videoUri" class="video-preview" @click.stop></video>
					</div>
				</div>

				<!-- 文件消息 -->
				<div v-else-if="message.messageType === 6 && fileInfo" class="message-file">
					<v-card class="file-card clickable" color="primary-lighten-4" elevation="2" @click="handleFileClick(fileInfo.fileUrl, fileInfo.fileName, fileInfo.fileType)">
						<div class="d-flex align-center pa-3 ga-3">
							<!-- 文件图标 -->
							<v-avatar size="40" color="primary">
								<v-icon :icon="getFileIcon(fileInfo.fileType)" size="24" color="white"></v-icon>
							</v-avatar>

							<!-- 文件信息 -->
							<div class="flex-1 text-truncate">
								<div class="text-body-2 font-weight-medium text-truncate">
									{{ fileInfo.fileName }}
								</div>
								<div class="text-caption text-primary-darken-2">
									{{ fileInfo.fileSize }}
								</div>
							</div>

							<!-- 下载图标 -->
							<v-icon icon="mdi-download" size="20" color="primary-darken-2"></v-icon>
						</div>
					</v-card>
				</div>

				<div class="message-meta mt-1 d-flex align-center justify-end ga-1">
					<span class="text-caption text-disabled">{{ formatTime(message.createdTime) }}</span>
					<v-icon v-if="!message.sending" icon="mdi-check-all" size="14" color="primary"></v-icon>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { computed } from 'vue'
import { formatTime } from '@/utils/formatTime'
import { useFilePreview } from '@/composables/useFilePreview'
const { getFileType, getFileIcon } = useFilePreview()
const props = defineProps({
	message: {
		type: Object,
		required: true,
	},
	user: {
		type: Object,
		required: true,
	},
})
const emit = defineEmits(['preview-file', 'download-file'])
// 处理文件点击
const handleFileClick = (url, fileName, type) => {
	if (['image', 'video', 'audio', 'pdf'].includes(type)) {
		// 可预览的文件,触发预览
		emit('preview-file', { url, fileName, type })
	} else {
		// 不可预览的文件,直接下载
		emit('download-file', { url, fileName })
	}
}
// 解析文件消息内容 (格式: "name|type|size|url")
const parseFileContent = content => {
	if (!content || typeof content !== 'string') {
		return null
	}

	const parts = content.split('|')
	if (parts.length !== 4) {
		console.warn('文件消息格式不正确:', content)
		return null
	}

	return {
		fileName: parts[0],
		fileType: parts[1],
		fileSize: parts[2],
		fileUrl: parts[3],
	}
}

// 计算文件信息
const fileInfo = computed(() => {
	if (props.message.messageType !== 6) {
		return null
	}
	return parseFileContent(props.message.content)
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
	box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}
.message-video {
	margin-top: 8px;
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
.clickable {
	cursor: pointer;
	transition: opacity 0.2s;
}

.clickable:hover {
	opacity: 0.9;
}

.message-file {
	margin-top: 8px;
}

.file-card {
	max-width: 280px;
	min-width: 240px;
	background: rgb(var(--v-theme-surface));
}
.file-card:hover {
	transform: translateY(-2px);
	box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15) !important;
	transition: all 0.2s;
}

.video-thumbnail {
	position: relative;
	width: 200px;
	height: 150px;
	background: #000;
	border-radius: 8px;
	overflow: hidden;
	display: flex;
	align-items: center;
	justify-content: center;
}

.video-thumbnail .v-icon {
	position: absolute;
	z-index: 2;
}
.video-thumbnail:hover .v-icon {
	transform: scale(1.1);
	transition: transform 0.2s;
}

.video-preview {
	width: 100%;
	height: 100%;
	object-fit: cover;
	pointer-events: none;
}
</style>
