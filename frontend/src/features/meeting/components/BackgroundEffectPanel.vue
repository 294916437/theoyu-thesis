<template>
	<div class="effect-panel d-flex flex-column fill-height bg-surface">
		<!-- 顶部 -->
		<div class="pa-4 pb-2 d-flex align-center justify-space-between border-b">
			<span class="text-subtitle-1 font-weight-medium">背景与特效</span>
			<v-chip v-if="isLoading" size="x-small" color="info" variant="flat" class="ml-2">
				<v-progress-circular indeterminate size="12" width="2" class="mr-1"></v-progress-circular>
				加载资源中
			</v-chip>
		</div>

		<!-- 滚动区域 -->
		<div class="flex-grow-1 overflow-y-auto pa-4 scroll-container">
			<!-- 预览卡片 -->
			<v-card class="mb-6 rounded-lg overflow-hidden position-relative preview-card" elevation="0" border>
				<div ref="previewContainer" class="preview-video-container fill-height"></div>
				<div class="preview-badge">实时预览</div>
			</v-card>

			<!-- 错误提示 -->
			<v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4" closable @click:close="error = null">
				{{ error }}
			</v-alert>

			<!-- 效果选择 -->
			<div class="mb-5">
				<div class="text-caption font-weight-bold text-medium-emphasis mb-2">基础设置</div>
				<div class="effects-grid">
					<v-card
						v-ripple
						class="effect-item d-flex align-center justify-center py-3"
						:class="{ active: effectType === 'none' }"
						variant="outlined"
						@click="changeEffect('none')"
					>
						<v-icon icon="mdi-block-helper" class="mb-1"></v-icon>
						<span class="text-caption">无效果</span>
					</v-card>

					<v-card
						v-ripple
						class="effect-item d-flex align-center justify-center py-3"
						:class="{ active: effectType === 'blur' }"
						variant="outlined"
						@click="changeEffect('blur')"
					>
						<v-icon icon="mdi-blur" class="mb-1"></v-icon>
						<span class="text-caption">背景虚化</span>
					</v-card>
				</div>
			</div>

			<!-- 背景图选择 -->
			<div>
				<div class="d-flex align-center justify-space-between mb-2">
					<span class="text-caption font-weight-bold text-medium-emphasis">虚拟背景</span>
					<v-btn prepend-icon="mdi-upload" variant="text" density="compact" size="small" color="primary" @click="fileInput?.click()"> 自定义 </v-btn>
				</div>

				<div class="effects-grid images-grid">
					<v-card
						v-for="bg in allBackgrounds"
						:key="bg.id"
						v-ripple
						class="background-item"
						:class="{ active: effectType === 'replace' && selectedBackground === bg.id }"
						@click="changeBackground(bg.id)"
						elevation="0"
					>
						<v-img :src="bg.thumbnail" cover aspect-ratio="1.6" class="bg-image">
							<template #placeholder>
								<div class="d-flex align-center justify-center fill-height bg-grey-lighten-4">
									<v-icon icon="mdi-image-outline" color="grey"></v-icon>
								</div>
							</template>
							<!-- 选中遮罩 -->
							<div v-if="effectType === 'replace' && selectedBackground === bg.id" class="active-overlay">
								<v-icon icon="mdi-check-circle" color="white" size="24"></v-icon>
							</div>
						</v-img>
					</v-card>
				</div>
			</div>
		</div>

		<!-- 隐形输入框 -->
		<input ref="fileInput" type="file" accept="image/*" hidden @change="handleFileUpload" />
	</div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { useBackgroundEffect } from '@/composables/useBackgroundEffect'
import { $notify } from '@/plugins/notification'

const props = defineProps({
	videoTrack: MediaStreamTrack,
})

const emit = defineEmits(['track-updated'])
const fileInput = ref(null)
const previewContainer = ref(null)
let previewVideo = null

const { effectType, selectedBackground, allBackgrounds, error, isLoading, startEffect, uploadCustomBackground } = useBackgroundEffect()

/** UI 辅助：初始化预览 Video */
function initPreview(track) {
	if (!track || !previewContainer.value) return

	if (!previewVideo) {
		previewVideo = document.createElement('video')
		previewVideo.muted = true
		previewVideo.autoplay = true
		previewVideo.playsInline = true
		previewVideo.style.cssText = 'width: 100%; height: 100%; object-fit: cover;'
		previewContainer.value.appendChild(previewVideo)
	}

	const stream = new MediaStream([track])
	previewVideo.srcObject = stream
}

async function changeEffect(type) {
	effectType.value = type
	await applyEffect()
}

async function changeBackground(bgId) {
	selectedBackground.value = bgId
	effectType.value = 'replace'
	await applyEffect()
}

async function applyEffect() {
	if (!props.videoTrack) {
		$notify.warning('无法获取摄像头画面')
		return
	}

	try {
		const newTrack = await startEffect(props.videoTrack)
		emit('track-updated', newTrack)
		initPreview(newTrack)
	} catch (err) {
		console.error(err)
		$notify.error('特效应用失败: ' + err.message)
	}
}

async function handleFileUpload(event) {
	const file = event.target.files?.[0]
	if (!file) return

	try {
		const bg = await uploadCustomBackground(file)
		await changeBackground(bg.id)
		$notify.success('背景已添加')
	} catch (err) {
		$notify.error('上传失败')
	} finally {
		if (fileInput.value) fileInput.value.value = ''
	}
}

watch(
	() => props.videoTrack,
	async newTrack => {
		if (newTrack) {
			if (effectType.value !== 'none') {
				await applyEffect()
			} else {
				initPreview(newTrack)
			}
		} else if (previewVideo) {
			previewVideo.srcObject = null
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
	border-left: 1px solid rgba(var(--v-theme-border), 0.5);
}

.preview-card {
	aspect-ratio: 16/9;
	background-color: #000;
	border-color: rgba(var(--v-theme-border), 0.5);
}

.preview-badge {
	position: absolute;
	top: 8px;
	left: 8px;
	background: rgba(0, 0, 0, 0.6);
	backdrop-filter: blur(4px);
	color: white;
	padding: 4px 8px;
	border-radius: 4px;
	font-size: 11px;
	font-weight: 500;
}

.effects-grid {
	display: grid;
	grid-template-columns: repeat(2, 1fr);
	gap: 12px;
}

.images-grid {
	grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
}

.effect-item:hover {
	border-color: rgb(var(--v-theme-primary));
	background-color: rgba(var(--v-theme-primary), 0.05);
}

.effect-item.active {
	border-color: rgb(var(--v-theme-primary));
	background-color: rgba(var(--v-theme-primary), 0.1);
	color: rgb(var(--v-theme-primary));
}

.background-item {
	border-radius: 8px;
	overflow: hidden;
	cursor: pointer;
	border: 2px solid transparent;
	transition: all 0.2s;
}

.background-item:hover {
	transform: translateY(-2px);
	box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.background-item.active {
	border-color: rgb(var(--v-theme-primary));
}

.active-overlay {
	position: absolute;
	inset: 0;
	background-color: rgba(var(--v-theme-primary), 0.3);
	display: flex;
	align-items: center;
	justify-content: center;
	backdrop-filter: blur(1px);
}
</style>
