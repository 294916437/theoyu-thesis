<template>
	<div class="background-panel">
		<!-- 头部 -->
		<div class="panel-header">
			<span class="text-subtitle-1 font-weight-medium">背景效果</span>

			<v-btn v-if="effectType !== 'none'" variant="text" size="small" prepend-icon="mdi-close" @click="handleDisableEffect"> 关闭效果 </v-btn>
		</div>

		<v-divider></v-divider>

		<!-- 加载状态 -->
		<div v-if="isLoading" class="loading-wrapper">
			<v-progress-circular indeterminate color="primary"></v-progress-circular>
			<p class="text-body-2 mt-4">正在加载资源...</p>
		</div>

		<!-- 主内容 -->
		<div v-else class="panel-content">
			<!-- 效果类型选择 -->
			<div class="effect-types mb-4">
				<v-chip-group v-model="effectType" mandatory color="primary" class="effect-chips">
					<v-chip value="none" filter variant="outlined">
						<template #prepend>
							<v-icon icon="mdi-cancel"></v-icon>
						</template>
						无效果
					</v-chip>

					<v-chip value="blur" filter variant="outlined">
						<template #prepend>
							<v-icon icon="mdi-blur"></v-icon>
						</template>
						背景模糊
					</v-chip>

					<v-chip value="replace" filter variant="outlined">
						<template #prepend>
							<v-icon icon="mdi-image"></v-icon>
						</template>
						背景替换
					</v-chip>
				</v-chip-group>
			</div>

			<!-- 背景图选择（仅替换模式） -->
			<v-expand-transition>
				<div v-show="effectType === 'replace'" class="backgrounds-section">
					<div class="section-header">
						<span class="text-body-2 font-weight-medium">选择背景</span>

						<v-btn variant="text" size="small" prepend-icon="mdi-upload" @click="handleUploadClick"> 上传 </v-btn>
					</div>

					<!-- 背景网格 -->
					<div class="backgrounds-grid">
						<div
							v-for="bg in allBackgrounds"
							:key="bg.id"
							class="background-item"
							:class="{ active: selectedBackground === bg.id }"
							@click="handleSelectBackground(bg.id)"
						>
							<v-img :src="bg.thumbnail" :alt="bg.name" aspect-ratio="16/9" cover class="background-thumbnail">
								<template #placeholder>
									<div class="d-flex align-center justify-center fill-height">
										<v-progress-circular indeterminate size="24" width="2" color="primary"></v-progress-circular>
									</div>
								</template>
							</v-img>

							<div class="background-name">
								<span class="text-caption">{{ bg.name }}</span>
							</div>

							<!-- 选中标记 -->
							<v-fade-transition>
								<div v-show="selectedBackground === bg.id" class="selected-overlay">
									<v-icon icon="mdi-check-circle" color="primary" size="32"></v-icon>
								</div>
							</v-fade-transition>

							<!-- 自定义背景删除按钮 -->
							<v-btn
								v-if="bg.id.startsWith('custom_')"
								icon="mdi-close"
								variant="text"
								size="x-small"
								color="error"
								class="delete-btn"
								@click.stop="handleDeleteBackground(bg.id)"
							></v-btn>
						</div>
					</div>
				</div>
			</v-expand-transition>

			<!-- 错误提示 -->
			<v-alert v-if="error" type="error" variant="tonal" density="compact" closable class="mt-4" @click:close="error = null">
				{{ error }}
			</v-alert>
		</div>

		<!-- 隐藏的文件上传 -->
		<input ref="fileInput" type="file" accept="image/jpeg,image/png,image/webp" hidden @change="handleFileUpload" />
	</div>
</template>

<script setup>
import { ref, watch } from 'vue'
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

const { effectType, selectedBackground, allBackgrounds, isProcessing, isLoading, error, startEffect, stopEffect, uploadCustomBackground } = useBackgroundEffect()

// ========== 事件处理 ==========
async function handleDisableEffect() {
	effectType.value = 'none'
	stopEffect()

	// 恢复原始轨道
	emit('track-updated', props.videoTrack)
	$notify.success('已关闭背景效果')
}

async function handleSelectBackground(bgId) {
	selectedBackground.value = bgId

	if (effectType.value === 'replace' && props.videoTrack) {
		try {
			const newTrack = await startEffect(props.videoTrack)
			emit('track-updated', newTrack)
			$notify.success('背景已更换')
		} catch (err) {
			$notify.error('更换背景失败')
		}
	}
}

function handleUploadClick() {
	fileInput.value?.click()
}

async function handleFileUpload(event) {
	const file = event.target.files?.[0]
	if (!file) return

	try {
		const customBg = await uploadCustomBackground(file)
		selectedBackground.value = customBg.id
		$notify.success('背景已上传')
	} catch (err) {
		$notify.error(err.message || '上传失败')
	} finally {
		fileInput.value.value = ''
	}
}

function handleDeleteBackground(bgId) {
	// TODO: 从 customBackgrounds 中删除
	$notify.success('已删除背景')
}

// ========== 监听效果类型变化 ==========
watch(effectType, async newType => {
	if (newType === 'none') {
		return
	}

	if (!props.videoTrack) {
		$notify.warning('未检测到视频流')
		return
	}

	try {
		const newTrack = await startEffect(props.videoTrack)
		emit('track-updated', newTrack)

		const typeName = newType === 'blur' ? '背景模糊' : '背景替换'
		$notify.success(`已启用${typeName}`)
	} catch (err) {
		$notify.error('启用效果失败')
	}
})
</script>

<style scoped>
.background-panel {
	display: flex;
	flex-direction: column;
	height: 100%;
	overflow: hidden;
}

.panel-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 12px 16px;
	background: rgb(var(--v-theme-surface-variant));
}

.loading-wrapper {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	flex: 1;
	padding: 32px;
}

.panel-content {
	flex: 1;
	overflow-y: auto;
	padding: 16px;
}

.effect-types {
	padding: 8px 0;
}

.effect-chips {
	gap: 8px;
}

.backgrounds-section {
	margin-top: 16px;
}

.section-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 12px;
}

.backgrounds-grid {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
	gap: 12px;
}

.background-item {
	position: relative;
	cursor: pointer;
	border-radius: 8px;
	overflow: hidden;
	transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
	border: 2px solid transparent;
}

.background-item:hover {
	transform: translateY(-2px);
	box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.background-item.active {
	border-color: rgb(var(--v-theme-primary));
}

.background-thumbnail {
	width: 100%;
	aspect-ratio: 16/9;
}

.background-name {
	position: absolute;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 6px 8px;
	background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
	color: white;
	text-align: center;
}

.selected-overlay {
	position: absolute;
	inset: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(var(--v-theme-primary), 0.2);
}

.delete-btn {
	position: absolute;
	top: 4px;
	right: 4px;
	opacity: 0;
	transition: opacity 0.2s;
}

.background-item:hover .delete-btn {
	opacity: 1;
}
</style>
