<template>
	<div class="effect-panel d-flex flex-column fill-height bg-surface">
		<!-- 顶部：标题与开关 -->
		<div class="pa-4 pb-2 d-flex align-center justify-space-between border-b">
			<span class="text-subtitle-1 font-weight-medium">背景与特效</span>
		</div>

		<!-- 资源加载动画 -->
		<v-fade-transition v-if="isLoading">
			<div class="d-flex flex-column align-center justify-center py-8">
				<v-progress-circular indeterminate color="primary" size="32"></v-progress-circular>
				<span class="text-caption text-medium-emphasis mt-2">正在初始化资源...</span>
			</div>
		</v-fade-transition>

		<!-- 主内容区域 (滚动) -->
		<div class="flex-grow-1 overflow-y-auto pa-4 scroll-container">
			<!-- 实时预览区 -->
			<v-card class="mb-6 rounded-lg overflow-hidden position-relative preview-card" elevation="2" color="black">
				<!-- 用一个 Video 元素回显处理后的流，或者回显其中的 Canvas -->
				<div ref="previewContainer" class="preview-video-container">
					<!-- 动态插入 video or canvas -->
				</div>
				<div class="preview-badge">预览</div>
			</v-card>

			<!-- 错误提示 -->
			<v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4" closable @click:close="error = null">
				{{ error }}
			</v-alert>

			<!-- 1. 基础效果 (无/模糊) -->
			<div class="mb-4">
				<div class="text-caption font-weight-bold text-medium-emphasis mb-2 ml-1">效果</div>
				<div class="effects-grid">
					<!-- 无效果 -->
					<v-card
						v-ripple
						class="effect-item d-flex align-center justify-center"
						:class="{ active: effectType === 'none' }"
						variant="outlined"
						@click="changeEffect('none')"
					>
						<v-icon icon="mdi-block-helper" size="24"></v-icon>
						<span class="text-caption mt-1">无</span>
					</v-card>

					<!-- 模糊 -->
					<v-card
						v-ripple
						class="effect-item d-flex align-center justify-center"
						:class="{ active: effectType === 'blur' }"
						variant="outlined"
						@click="changeEffect('blur')"
					>
						<v-icon icon="mdi-blur" size="24"></v-icon>
						<span class="text-caption mt-1">模糊</span>
					</v-card>
				</div>
			</div>

			<!-- 2. 背景图片 (替换) -->
			<div>
				<div class="d-flex align-center justify-space-between mb-2 ml-1">
					<span class="text-caption font-weight-bold text-medium-emphasis">背景图片</span>
					<v-btn prepend-icon="mdi-plus" variant="text" density="compact" size="small" color="primary" @click="fileInput?.click()"> 添加 </v-btn>
				</div>

				<div class="effects-grid images-grid">
					<v-card
						v-for="bg in allBackgrounds"
						:key="bg.id"
						v-ripple
						class="background-item"
						:class="{ active: effectType === 'replace' && selectedBackground === bg.id }"
						@click="changeBackground(bg.id)"
					>
						<v-img :src="bg.thumbnail" cover aspect-ratio="1.6" class="bg-image">
							<template #placeholder>
								<div class="d-flex align-center justify-center fill-height bg-grey-lighten-4">
									<v-progress-circular indeterminate size="20" width="2" color="grey"></v-progress-circular>
								</div>
							</template>
							<!-- 选中遮罩 -->
							<div v-if="effectType === 'replace' && selectedBackground === bg.id" class="active-overlay">
								<v-icon icon="mdi-check" color="white"></v-icon>
							</div>
						</v-img>
					</v-card>
				</div>
			</div>
		</div>

		<!-- 隐藏的上传 -->
		<input ref="fileInput" type="file" accept="image/jpeg,image/png,image/webp" hidden @change="handleFileUpload" />
	</div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { useBackgroundEffect } from '@/composables/useBackgroundEffect'
import { $notify } from '@/plugins/notification'

const props = defineProps({
	videoTrack: {
		type: MediaStreamTrack,
		default: null,
	},
})

const emit = defineEmits(['track-updated'])
const fileInput = ref(null)
const previewContainer = ref(null)
let previewVideo = null

const { isReady, effectType, selectedBackground, allBackgrounds, isLoading, error, initResources, startEffect, uploadCustomBackground } = useBackgroundEffect()

/**
 * 初始化预览视频
 * 在面板上方显示一个小视频，直接播放当前的 localStream (经过效果处理后的)
 */
function initPreview(track) {
	if (!track || !previewContainer.value) return

	if (!previewVideo) {
		previewVideo = document.createElement('video')
		previewVideo.muted = true
		previewVideo.autoplay = true
		previewVideo.playsInline = true
		previewVideo.style.width = '100%'
		previewVideo.style.height = '100%'
		previewVideo.style.objectFit = 'cover'
		previewContainer.value.appendChild(previewVideo)
	}

	const stream = new MediaStream([track])
	previewVideo.srcObject = stream
}

/**
 * 切换效果类型 (None / Blur)
 */
async function changeEffect(type) {
	effectType.value = type
	await applyEffect()
}

/**
 * 切换背景图 (自动切到 Replace 模式)
 */
async function changeBackground(bgId) {
	selectedBackground.value = bgId
	effectType.value = 'replace'
	await applyEffect()
}

/**
 * 应用效果流程
 */
async function applyEffect() {
	if (!props.videoTrack) {
		$notify.warning('请先开启摄像头')
		return
	}

	try {
		// startEffect 内部会根据 effectType 和 selectedBackground 处理
		// 如果 type 是 none，startEffect 仍会返回一个流（直通）
		const newTrack = await startEffect(props.videoTrack)

		// 1. 发射事件给父组件更新主画面和推流
		emit('track-updated', newTrack)

		// 2. 更新面板顶部的小预览
		initPreview(newTrack)
	} catch (err) {
		console.error(err)
		$notify.error('应用效果失败')
	}
}

async function handleFileUpload(event) {
	const file = event.target.files?.[0]
	if (!file) return

	try {
		const bg = await uploadCustomBackground(file)
		// 自动选中新上传的
		await changeBackground(bg.id)
		$notify.success('背景已上传')
	} catch (err) {
		$notify.error(err.message || '上传失败')
	} finally {
		if (fileInput.value) fileInput.value.value = ''
	}
}

// 监听传入的轨道变化（例如用户切换了摄像头）
watch(
	() => props.videoTrack,
	async newTrack => {
		if (newTrack) {
			// 如果当前开启了效果，需要重新应用到新轨道
			if (effectType.value !== 'none') {
				await applyEffect()
			} else {
				// 无效果，直接预览原轨道
				initPreview(newTrack)
			}
		} else {
			// 清空预览
			if (previewVideo) {
				previewVideo.srcObject = null
			}
		}
	},
	{ immediate: true },
)

onUnmounted(() => {
	if (previewVideo) {
		previewVideo.srcObject = null
		previewVideo = null
	}
})
</script>

<style scoped>
.effect-panel {
	background-color: rgb(var(--v-theme-surface));
}

.scroll-container::-webkit-scrollbar {
	width: 6px;
}
.scroll-container::-webkit-scrollbar-thumb {
	background-color: rgba(var(--v-theme-on-surface), 0.1);
	border-radius: 4px;
}

/* 预览卡片 */
.preview-card {
	aspect-ratio: 16/9;
	background-color: #202124;
}
.preview-badge {
	position: absolute;
	bottom: 8px;
	left: 8px;
	background: rgba(0, 0, 0, 0.6);
	color: white;
	padding: 2px 6px;
	border-radius: 4px;
	font-size: 10px;
	pointer-events: none;
}

/* 效果网格 */
.effects-grid {
	display: grid;
	grid-template-columns: repeat(2, 1fr); /* 默认两列 */
	gap: 12px;
}

.images-grid {
	/* 图片根据宽度自适应，最小 80px */
	grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
}

/* 选项卡片样式 */
.effect-item {
	height: 60px;
	flex-direction: column;
	cursor: pointer;
	border-color: rgba(var(--v-theme-border), 0.5);
	transition: all 0.2s ease;
	border-width: 2px;
}

.effect-item:hover {
	background-color: rgba(var(--v-theme-primary), 0.05);
}

.effect-item.active {
	border-color: rgb(var(--v-theme-primary));
	background-color: rgba(var(--v-theme-primary), 0.05);
	color: rgb(var(--v-theme-primary));
}

/* 背景图项 */
.background-item {
	aspect-ratio: 1.6; /* 缩略图比例 */
	border-radius: 8px;
	cursor: pointer;
	overflow: hidden;
	position: relative;
	border: 2px solid transparent;
	transition: all 0.2s;
}

.background-item.active {
	border-color: rgb(var(--v-theme-primary));
}

.bg-image {
	transition: transform 0.3s;
}

.background-item:hover .bg-image {
	transform: scale(1.1);
}

.active-overlay {
	position: absolute;
	inset: 0;
	background-color: rgba(var(--v-theme-primary), 0.4);
	display: flex;
	align-items: center;
	justify-content: center;
	backdrop-filter: blur(1px);
}
</style>
