<template>
	<div class="screen-share-overlay">
		<v-card class="screen-share-card" elevation="8">
			<!-- 共享者信息栏 -->
			<div class="share-header">
				<v-chip color="success" variant="elevated">
					<v-icon left size="small">mdi-monitor-share</v-icon>
					{{ presenter.name }} 正在共享屏幕
				</v-chip>

				<div class="share-controls">
					<v-btn icon="mdi-fullscreen" variant="text" color="white" size="small" @click="toggleFullscreen">
						<v-tooltip activator="parent" location="bottom">
							{{ isFullscreen ? '退出全屏' : '全屏' }}
						</v-tooltip>
					</v-btn>

					<v-btn icon="mdi-close" variant="text" color="white" size="small" @click="handleClose">
						<v-tooltip activator="parent" location="bottom"> 关闭屏幕共享 </v-tooltip>
					</v-btn>
				</div>
			</div>

			<!-- 共享屏幕视频 -->
			<div ref="shareContainer" class="share-container">
				<video
					ref="shareVideo"
					autoplay
					playsinline
					class="share-video"
					:class="{ 'video-contain': videoFit === 'contain', 'video-cover': videoFit === 'cover' }"
				></video>

				<!-- 视频控制覆盖层 -->
				<div class="share-overlay-controls">
					<v-btn-group density="compact" variant="outlined" color="white">
						<v-btn :variant="videoFit === 'contain' ? 'flat' : 'outlined'" @click="videoFit = 'contain'">
							<v-icon>mdi-fit-to-screen</v-icon>
							<v-tooltip activator="parent" location="top"> 适应屏幕 </v-tooltip>
						</v-btn>

						<v-btn :variant="videoFit === 'cover' ? 'flat' : 'outlined'" @click="videoFit = 'cover'">
							<v-icon>mdi-arrow-expand-all</v-icon>
							<v-tooltip activator="parent" location="top"> 填充屏幕 </v-tooltip>
						</v-btn>
					</v-btn-group>

					<!-- 画质选择 -->
					<v-menu offset-y>
						<template #activator="{ props }">
							<v-btn variant="outlined" color="white" size="small" v-bind="props" class="ml-2">
								<v-icon left size="small">mdi-quality-high</v-icon>
								{{ currentQuality }}
							</v-btn>
						</template>
						<v-list>
							<v-list-item
								v-for="quality in qualityOptions"
								:key="quality.value"
								@click="changeQuality(quality.value)"
							>
								<v-list-item-title>
									{{ quality.label }}
									<v-icon v-if="currentQuality === quality.label" right size="small" color="success">
										mdi-check
									</v-icon>
								</v-list-item-title>
							</v-list-item>
						</v-list>
					</v-menu>
				</div>

				<!-- 加载状态 -->
				<v-overlay v-model="loading" contained class="align-center justify-center">
					<v-progress-circular indeterminate size="64" color="primary"></v-progress-circular>
					<div class="text-white mt-4">正在加载屏幕共享...</div>
				</v-overlay>

				<!-- 连接质量提示 -->
				<v-snackbar v-model="showQualityWarning" :timeout="5000" color="warning" location="bottom">
					<v-icon left>mdi-wifi-strength-1</v-icon>
					网络连接不稳定,可能影响共享质量
					<template #actions>
						<v-btn variant="text" @click="showQualityWarning = false"> 关闭 </v-btn>
					</template>
				</v-snackbar>
			</div>

			<!-- 底部参与者缩略图 -->
			<div v-if="showThumbnails" class="thumbnails-container">
				<div class="thumbnails-scroll">
					<div
						v-for="participant in participants"
						:key="participant.id"
						class="thumbnail-item"
						@click="handleThumbnailClick(participant)"
					>
						<video
							v-if="participant.stream"
							:ref="el => setThumbnailRef(el, participant.id)"
							autoplay
							playsinline
							muted
							class="thumbnail-video"
						></video>
						<div v-else class="thumbnail-placeholder">
							<v-avatar size="32" :color="participant.avatarColor || 'primary'">
								<span class="text-caption">{{ getInitials(participant.name) }}</span>
							</v-avatar>
						</div>
						<div class="thumbnail-name">{{ participant.name }}</div>
					</div>
				</div>
			</div>
		</v-card>
	</div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useFullscreen, useResizeObserver } from '@vueuse/core'

const props = defineProps({
	stream: {
		type: MediaStream,
		required: true,
	},
	presenter: {
		type: Object,
		required: true,
	},
	participants: {
		type: Array,
		default: () => [],
	},
})

const emit = defineEmits(['close', 'switch-presenter'])

const shareVideo = ref(null)
const shareContainer = ref(null)
const thumbnailRefs = new Map()

const loading = ref(true)
const videoFit = ref('contain')
const currentQuality = ref('高清')
const showQualityWarning = ref(false)
const showThumbnails = ref(true)

const { isFullscreen, toggle: toggleFullscreen } = useFullscreen(shareContainer)

const qualityOptions = [
	{ label: '超清', value: 'hd' },
	{ label: '高清', value: 'high' },
	{ label: '标清', value: 'medium' },
	{ label: '流畅', value: 'low' },
]

const setThumbnailRef = (el, id) => {
	if (el) {
		thumbnailRefs.set(id, el)
		// 设置视频流
		const participant = props.participants.find(p => p.id === id)
		if (participant?.stream && el.srcObject !== participant.stream) {
			el.srcObject = participant.stream
		}
	}
}

const getInitials = name => {
	return name
		.split(' ')
		.map(word => word[0])
		.join('')
		.toUpperCase()
		.slice(0, 2)
}

const changeQuality = quality => {
	const qualityMap = {
		hd: '超清',
		high: '高清',
		medium: '标清',
		low: '流畅',
	}
	currentQuality.value = qualityMap[quality]

	// 预留API: 调整视频质量
	console.log('Change quality to:', quality)
}

const handleClose = () => {
	emit('close')
}

const handleThumbnailClick = participant => {
	emit('switch-presenter', participant.id)
}

// 监听屏幕共享流变化
watch(
	() => props.stream,
	async newStream => {
		if (shareVideo.value && newStream) {
			loading.value = true
			shareVideo.value.srcObject = newStream

			// 等待视频加载
			await nextTick()
			shareVideo.value.onloadedmetadata = () => {
				loading.value = false
			}

			// 监听视频错误
			shareVideo.value.onerror = () => {
				loading.value = false
				showQualityWarning.value = true
			}
		}
	},
	{ immediate: true },
)

// 监听容器大小变化
useResizeObserver(shareContainer, () => {
	// 可以在这里处理视频缩放逻辑
})

// 监听参与者流变化
watch(
	() => props.participants,
	() => {
		props.participants.forEach(participant => {
			const videoEl = thumbnailRefs.get(participant.id)
			if (videoEl && participant.stream && videoEl.srcObject !== participant.stream) {
				videoEl.srcObject = participant.stream
			}
		})
	},
	{ deep: true },
)

onMounted(() => {
	// 键盘快捷键
	const handleKeydown = e => {
		if (e.key === 'Escape' && isFullscreen.value) {
			toggleFullscreen()
		} else if (e.key === 'f' || e.key === 'F') {
			toggleFullscreen()
		}
	}

	document.addEventListener('keydown', handleKeydown)

	onUnmounted(() => {
		document.removeEventListener('keydown', handleKeydown)
	})
})
</script>

<style scoped>
.screen-share-overlay {
	position: fixed;
	top: 48px;
	left: 0;
	right: 0;
	bottom: 80px;
	z-index: 10;
	background-color: rgba(0, 0, 0, 0.95);
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 16px;
}

.screen-share-card {
	width: 100%;
	height: 100%;
	background-color: #000;
	display: flex;
	flex-direction: column;
	border-radius: 8px;
	overflow: hidden;
}

.share-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 12px 16px;
	background: linear-gradient(to bottom, rgba(0, 0, 0, 0.8), transparent);
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	z-index: 2;
}

.share-controls {
	display: flex;
	gap: 8px;
}

.share-container {
	flex: 1;
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
	background-color: #000;
}

.share-video {
	max-width: 100%;
	max-height: 100%;
	width: auto;
	height: auto;
}

.video-contain {
	object-fit: contain;
}

.video-cover {
	object-fit: cover;
	width: 100%;
	height: 100%;
}

.share-overlay-controls {
	position: absolute;
	bottom: 16px;
	left: 50%;
	transform: translateX(-50%);
	display: flex;
	gap: 8px;
	opacity: 0;
	transition: opacity 0.3s;
}

.share-container:hover .share-overlay-controls {
	opacity: 1;
}

.thumbnails-container {
	background-color: rgba(0, 0, 0, 0.9);
	border-top: 1px solid rgba(255, 255, 255, 0.1);
	padding: 8px;
	max-height: 120px;
}

.thumbnails-scroll {
	display: flex;
	gap: 8px;
	overflow-x: auto;
	overflow-y: hidden;
	padding: 4px;
}

.thumbnails-scroll::-webkit-scrollbar {
	height: 6px;
}

.thumbnails-scroll::-webkit-scrollbar-track {
	background: rgba(255, 255, 255, 0.05);
	border-radius: 3px;
}

.thumbnails-scroll::-webkit-scrollbar-thumb {
	background: rgba(255, 255, 255, 0.2);
	border-radius: 3px;
}

.thumbnail-item {
	position: relative;
	min-width: 100px;
	height: 80px;
	border-radius: 8px;
	overflow: hidden;
	cursor: pointer;
	border: 2px solid transparent;
	transition: border-color 0.2s;
}

.thumbnail-item:hover {
	border-color: #667eea;
}

.thumbnail-video {
	width: 100%;
	height: 100%;
	object-fit: cover;
}

.thumbnail-placeholder {
	width: 100%;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.thumbnail-name {
	position: absolute;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 4px;
	background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
	color: white;
	font-size: 11px;
	text-align: center;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}
</style>
