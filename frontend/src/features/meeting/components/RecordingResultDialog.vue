<!-- filepath: d:\git\monorepo\theoyu-thesis\frontend\src\features\meeting\components\RecordingResultDialog.vue -->
<template>
	<v-dialog v-model="model" max-width="460" transition="dialog-bottom-transition">
		<v-card class="recording-result-dialog" rounded="xl" elevation="8">
			<!-- 顶部绿色装饰条 -->
			<div class="dialog-accent-bar--success"></div>

			<v-card-title class="d-flex align-center pa-5 pb-3">
				<div class="success-icon-wrapper mr-3">
					<v-icon icon="mdi-check-circle" color="success" size="28"></v-icon>
				</div>
				<div>
					<div class="text-h6 font-weight-medium">录制完成</div>
					<div class="text-caption text-medium-emphasis">文件已生成，可下载或预览</div>
				</div>
				<v-spacer></v-spacer>
				<v-btn icon="mdi-close" variant="text" size="small" @click="model = false"></v-btn>
			</v-card-title>

			<v-divider></v-divider>

			<v-card-text class="pa-5">
				<!-- 录制信息卡片 -->
				<v-card variant="tonal" color="success" rounded="lg" class="mb-4 pa-4">
					<div class="d-flex align-center justify-space-between">
						<div class="d-flex flex-column ga-2">
							<div class="d-flex align-center">
								<v-icon icon="mdi-clock-outline" size="16" class="mr-2" color="success"></v-icon>
								<span class="text-body-2">
									时长：<strong>{{ formattedDuration }}</strong>
								</span>
							</div>
							<div class="d-flex align-center">
								<v-icon icon="mdi-database-outline" size="16" class="mr-2" color="success"></v-icon>
								<span class="text-body-2">
									大小：<strong>{{ formattedSize }}</strong>
								</span>
							</div>
							<div class="d-flex align-center">
								<v-icon icon="mdi-file-video-outline" size="16" class="mr-2" color="success"></v-icon>
								<span class="text-body-2">
									格式：<strong>{{ fileExtension.toUpperCase() }}</strong>
								</span>
							</div>
						</div>
						<!-- 格式徽标 -->
						<div class="format-badge">
							<span>{{ fileExtension.toUpperCase() }}</span>
						</div>
					</div>
				</v-card>

				<!-- 文件 URL 展示（可复制） -->
				<div class="url-copy-box" @click="copyUrl">
					<v-icon icon="mdi-link-variant" size="16" class="mr-2 flex-shrink-0" color="primary"></v-icon>
					<span class="url-text text-caption">{{ result?.fileUrl }}</span>
					<v-icon :icon="copied ? 'mdi-check' : 'mdi-content-copy'" size="16" :color="copied ? 'success' : 'medium-emphasis'" class="ml-2 flex-shrink-0"></v-icon>
				</div>
			</v-card-text>

			<v-divider></v-divider>

			<v-card-actions class="pa-4 ga-2">
				<v-btn variant="text" @click="model = false">关闭</v-btn>
				<v-spacer></v-spacer>
				<!-- 预览（仅 mp4/webm 支持） -->
				<v-btn v-if="canPreview" variant="tonal" color="primary" prepend-icon="mdi-play-circle-outline" rounded="lg" @click="$emit('preview', result.fileUrl)">
					预览
				</v-btn>
				<!-- 下载 -->
				<v-btn variant="flat" color="primary" prepend-icon="mdi-download" rounded="lg" @click="$emit('download', { url: result.fileUrl, name: fileName })">
					下载录制
				</v-btn>
			</v-card-actions>
		</v-card>
	</v-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { useClipboard } from '@vueuse/core'

const props = defineProps({
	modelValue: Boolean,
	result: Object,
	// result: { fileUrl, fileSize, duration, endTime }
})

const emit = defineEmits(['update:modelValue', 'download', 'preview'])

const model = defineModel()

// 使用 vueuse 的剪贴板工具
const { copy, copied } = useClipboard({ legacy: true })

const copyUrl = () => copy(props.result?.fileUrl || '')

// 从 URL 中提取文件扩展名
const fileExtension = computed(() => {
	const url = props.result?.fileUrl || ''
	return url.split('.').pop()?.split('?')[0]?.toLowerCase() || 'mp4'
})

const canPreview = computed(() => ['mp4', 'webm'].includes(fileExtension.value))

// 文件名：从 URL 提取 或 生成
const fileName = computed(() => {
	const url = props.result?.fileUrl || ''
	const fromUrl = url.split('/').pop()?.split('?')[0]
	return fromUrl || `recording-${Date.now()}.${fileExtension.value}`
})

const formattedDuration = computed(() => {
	const s = props.result?.duration ?? 0
	const h = Math.floor(s / 3600)
	const m = Math.floor((s % 3600) / 60)
	const sec = s % 60
	if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
	return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
})

const formattedSize = computed(() => {
	const bytes = props.result?.fileSize ?? 0
	if (!bytes) return '未知'
	if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
	return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
})
</script>

<style scoped>
.recording-result-dialog {
	overflow: hidden;
}

.dialog-accent-bar--success {
	height: 4px;
	background: linear-gradient(90deg, rgb(var(--v-theme-success)), rgba(var(--v-theme-success), 0.3));
}

.success-icon-wrapper {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 44px;
	height: 44px;
	border-radius: 12px;
	background: rgba(var(--v-theme-success), 0.1);
	flex-shrink: 0;
}

/* 格式徽标 */
.format-badge {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 52px;
	height: 52px;
	border-radius: 12px;
	background: rgba(var(--v-theme-success), 0.15);
	border: 2px solid rgba(var(--v-theme-success), 0.3);
	font-size: 0.75rem;
	font-weight: 700;
	color: rgb(var(--v-theme-success));
	letter-spacing: 0.5px;
	flex-shrink: 0;
}

/* URL 复制框 */
.url-copy-box {
	display: flex;
	align-items: center;
	padding: 10px 12px;
	border-radius: 8px;
	border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
	background: rgba(var(--v-theme-surface-variant), 0.6);
	cursor: pointer;
	transition: background 0.2s;
	overflow: hidden;
}

.url-copy-box:hover {
	background: rgba(var(--v-theme-primary), 0.06);
	border-color: rgba(var(--v-theme-primary), 0.3);
}

.url-text {
	flex: 1;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	color: rgb(var(--v-theme-on-surface-variant));
	font-family: monospace;
	font-size: 0.72rem;
}
</style>
