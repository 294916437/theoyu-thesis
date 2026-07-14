<template>
	<v-dialog v-model="visible" :max-width="maxWidth" @update:model-value="handleClose">
		<v-card class="file-preview-card">
			<!-- 工具栏 -->
			<v-toolbar density="compact" color="transparent" flat>
				<v-toolbar-title class="text-subtitle-1">
					{{ title }}
				</v-toolbar-title>
				<v-spacer></v-spacer>

				<!-- 操作按钮 -->
				<v-btn icon="mdi-download" variant="text" @click="handleDownload"></v-btn>

				<v-btn icon="mdi-close" variant="text" @click="handleClose"></v-btn>
			</v-toolbar>

			<!-- 内容区域 -->
			<v-card-text class="pa-0 preview-content">
				<!-- 图片预览 -->
				<div v-if="fileType === 'image'" class="image-preview">
					<v-img :src="fileUrl" contain max-height="80vh">
						<template #placeholder>
							<div class="d-flex align-center justify-center fill-height">
								<v-progress-circular indeterminate color="primary"></v-progress-circular>
							</div>
						</template>
						<template #error>
							<div class="d-flex flex-column align-center justify-center fill-height">
								<v-icon icon="mdi-image-broken" size="64" color="error"></v-icon>
								<span class="text-caption mt-2">图片加载失败</span>
							</div>
						</template>
					</v-img>
				</div>

				<!-- 视频预览 -->
				<div v-else-if="fileType === 'video'" class="video-preview">
					<video
						ref="videoRef"
						:src="fileUrl"
						controls
						controlslist="nodownload"
						class="preview-video"
						@error="handleVideoError"
					>
						您的浏览器不支持视频播放
					</video>
				</div>

				<!-- 音频预览 -->
				<div v-else-if="fileType === 'audio'" class="audio-preview">
					<div class="audio-wrapper">
						<v-icon icon="mdi-music" size="64" color="primary" class="mb-4"></v-icon>
						<div class="text-h6 mb-4">{{ fileName }}</div>
						<audio
							ref="audioRef"
							:src="fileUrl"
							controls
							controlslist="nodownload"
							class="preview-audio"
							@error="handleAudioError"
						>
							您的浏览器不支持音频播放
						</audio>
					</div>
				</div>

				<!-- PDF 预览 (预留) -->
				<div v-else-if="fileType === 'pdf'" class="pdf-preview">
					<iframe :src="fileUrl" frameborder="0" class="preview-iframe"></iframe>
				</div>

				<!-- 文本文件预览 (预留) -->
				<div v-else-if="fileType === 'text'" class="text-preview">
					<pre class="preview-text">{{ textContent }}</pre>
				</div>

				<!-- 不支持的文件类型 -->
				<div v-else class="unsupported-preview">
					<v-icon icon="mdi-file-question" size="64" color="grey" class="mb-4"></v-icon>
					<div class="text-h6 mb-2">无法预览此文件</div>
					<div class="text-body-2 text-grey mb-4">{{ fileName }}</div>
					<v-btn variant="flat" color="primary" prepend-icon="mdi-download" @click="handleDownload">
						下载文件
					</v-btn>
				</div>

				<!-- 加载状态 -->
				<div v-if="loading" class="loading-overlay">
					<v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
					<div class="text-body-2 mt-4">加载中...</div>
				</div>

				<!-- 错误状态 -->
				<div v-if="error" class="error-overlay">
					<v-icon icon="mdi-alert-circle" size="64" color="error" class="mb-4"></v-icon>
					<div class="text-h6 mb-2">加载失败</div>
					<div class="text-body-2 text-grey mb-4">{{ error }}</div>
					<v-btn variant="flat" color="primary" @click="handleRetry"> 重试 </v-btn>
				</div>
			</v-card-text>

			<!-- 下载进度条 -->
			<v-progress-linear
				v-if="props.downloadProgress > 0 && props.downloadProgress < 100"
				:model-value="props.downloadProgress"
				color="primary"
				height="4"
			></v-progress-linear>
		</v-card>
	</v-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
	modelValue: {
		type: Boolean,
		default: false,
	},
	fileUrl: {
		type: String,
		required: true,
	},
	fileName: {
		type: String,
		default: '',
	},
	fileType: {
		type: String,
		required: true,
		validator: value => {
			return ['image', 'video', 'audio', 'pdf', 'text', 'unknown'].includes(value)
		},
	},
	title: {
		type: String,
		default: '文件预览',
	},
	downloadProgress: {
		type: Number,
		default: 0,
	},
})

const emit = defineEmits(['update:modelValue', 'download', 'close'])

// 响应式数据
const videoRef = ref(null)
const audioRef = ref(null)
const loading = ref(false)
const error = ref(null)
const textContent = ref('')

// 计算属性
const visible = computed({
	get: () => props.modelValue,
	set: value => emit('update:modelValue', value),
})

const maxWidth = computed(() => {
	switch (props.fileType) {
		case 'image':
		case 'video':
			return '90vw'
		case 'pdf':
			return '95vw'
		default:
			return '600px'
	}
})

// 方法
const handleClose = () => {
	// 停止媒体播放
	if (videoRef.value) {
		videoRef.value.pause()
		videoRef.value.currentTime = 0
	}
	if (audioRef.value) {
		audioRef.value.pause()
		audioRef.value.currentTime = 0
	}

	visible.value = false
	emit('close')
}

const handleDownload = () => {
	emit('download', {
		url: props.fileUrl,
		name: props.fileName,
	})
}

const handleRetry = () => {
	error.value = null
	loading.value = true
	// 触发重新加载
	setTimeout(() => {
		loading.value = false
	}, 500)
}

const handleVideoError = e => {
	console.error('Video load error:', e)
	error.value = '视频加载失败，请检查文件格式或网络连接'
}

const handleAudioError = e => {
	console.error('Audio load error:', e)
	error.value = '音频加载失败，请检查文件格式或网络连接'
}

// 监听文件变化，加载文本内容
watch(
	() => props.fileUrl,
	async newUrl => {
		if (props.fileType === 'text' && newUrl) {
			try {
				loading.value = true
				error.value = null

				const response = await fetch(newUrl)
				if (!response.ok) throw new Error('加载失败')

				textContent.value = await response.text()
			} catch (e) {
				error.value = '文本文件加载失败'
				console.error('Text load error:', e)
			} finally {
				loading.value = false
			}
		}
	},
	{ immediate: true },
)
</script>

<style scoped>
.file-preview-card {
	background: rgb(var(--v-theme-surface));
}

.preview-content {
	position: relative;
	min-height: 300px;
	background: rgb(var(--v-theme-background));
}

/* 图片预览 */
.image-preview {
	width: 100%;
	min-height: 400px;
}

/* 视频预览 */
.video-preview {
	display: flex;
	align-items: center;
	justify-content: center;
	background: #000;
}

.preview-video {
	max-width: 100%;
	max-height: 80vh;
	border-radius: 8px;
}

/* 音频预览 */
.audio-preview {
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 48px;
	min-height: 300px;
}

.audio-wrapper {
	display: flex;
	flex-direction: column;
	align-items: center;
	text-align: center;
}

.preview-audio {
	width: 100%;
	max-width: 400px;
	margin-top: 16px;
}

/* PDF 预览 */
.pdf-preview {
	height: 80vh;
}

.preview-iframe {
	width: 100%;
	height: 100%;
}

/* 文本预览 */
.text-preview {
	padding: 24px;
	max-height: 70vh;
	overflow-y: auto;
}

.preview-text {
	font-family: 'Courier New', monospace;
	font-size: 14px;
	line-height: 1.6;
	white-space: pre-wrap;
	word-wrap: break-word;
	color: rgb(var(--v-theme-on-background));
}

/* 不支持的文件类型 */
.unsupported-preview {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: 48px;
	min-height: 300px;
}

/* 加载状态 */
.loading-overlay {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	background: rgba(var(--v-theme-background), 0.9);
	z-index: 10;
}

/* 错误状态 */
.error-overlay {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	background: rgba(var(--v-theme-background), 0.95);
	z-index: 10;
}

/* 滚动条样式 */
.text-preview::-webkit-scrollbar {
	width: 8px;
}

.text-preview::-webkit-scrollbar-track {
	background: rgb(var(--v-theme-surface-variant));
	border-radius: 4px;
}

.text-preview::-webkit-scrollbar-thumb {
	background: rgb(var(--v-theme-primary));
	border-radius: 4px;
}

.text-preview::-webkit-scrollbar-thumb:hover {
	background: rgb(var(--v-theme-primary-darken-1));
}
</style>
