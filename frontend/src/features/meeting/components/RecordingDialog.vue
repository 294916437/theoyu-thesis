<template>
	<v-dialog :model-value="modelValue" max-width="460" transition="dialog-bottom-transition" :persistent="isActiveRecording || isLoading" @update:model-value="handleDialogUpdate">
		<v-card class="recording-dialog" rounded="xl" elevation="8">
			<!-- 顶部装饰条（随阶段变色） -->
			<div class="dialog-accent-bar" :class="accentBarClass"></div>

			<!-- 标题栏 -->
			<v-card-title class="d-flex align-center pa-5 pb-3">
				<div class="icon-wrapper mr-3" :class="iconWrapperClass">
					<v-icon :icon="titleIcon" :color="titleIconColor" size="28"></v-icon>
				</div>
				<div>
					<div class="text-h6 font-weight-medium">{{ titleText }}</div>
					<div class="text-caption text-medium-emphasis">{{ subtitleText }}</div>
				</div>
				<v-spacer></v-spacer>
				<v-btn v-if="!isActiveRecording && !isLoading" icon="mdi-close" variant="text" size="small" @click="handleClose"></v-btn>
			</v-card-title>

			<v-divider></v-divider>

			<!-- ===== 阶段：checking ===== -->
			<template v-if="phase === PHASE.CHECKING">
				<v-card-text class="pa-8 d-flex flex-column align-center ga-4">
					<v-progress-circular indeterminate size="56" width="4" color="primary"></v-progress-circular>
					<div class="text-body-2 text-medium-emphasis">正在检查录制状态...</div>
				</v-card-text>
			</template>

			<!-- ===== 阶段：已有录制记录 ===== -->
			<template v-else-if="phase === PHASE.EXISTS">
				<v-card-text class="pa-5">
					<v-alert type="info" variant="tonal" density="compact" rounded="lg" class="mb-4">
						<div class="text-caption font-weight-medium">该会议已存在录制文件</div>
					</v-alert>
					<RecordingResultCard :result="recordingResult" @copy-url="onCopyUrl" :copied="copied" />
				</v-card-text>
				<v-divider></v-divider>
				<v-card-actions class="pa-4 ga-2">
					<v-btn variant="text" @click="handleClose">关闭</v-btn>
					<v-spacer></v-spacer>
					<v-btn v-if="canPreview" variant="tonal" color="primary" prepend-icon="mdi-play-circle-outline" rounded="lg" @click="$emit('preview', recordingResult.fileUrl)">
						预览
					</v-btn>
					<v-btn
						variant="flat"
						color="primary"
						prepend-icon="mdi-download"
						rounded="lg"
						@click="$emit('download', { url: recordingResult.fileUrl, name: downloadFileName })"
					>
						下载录制
					</v-btn>
				</v-card-actions>
			</template>

			<!-- ===== 阶段：starting（选择格式）===== -->
			<template v-else-if="phase === PHASE.STARTING">
				<v-card-text class="pa-5">
					<div class="text-subtitle-2 font-weight-medium mb-3">录制格式</div>
					<div class="format-grid">
						<div
							v-for="fmt in formats"
							:key="fmt.value"
							class="format-card"
							:class="{ 'format-card--active': selectedFormat === fmt.value }"
							@click="selectedFormat = fmt.value"
						>
							<v-icon :icon="fmt.icon" size="28" class="mb-2" :color="selectedFormat === fmt.value ? 'primary' : 'medium-emphasis'"></v-icon>
							<div class="format-card__label">{{ fmt.label }}</div>
							<div class="format-card__desc text-caption text-medium-emphasis">{{ fmt.desc }}</div>
							<v-icon v-if="selectedFormat === fmt.value" icon="mdi-check-circle" size="16" color="primary" class="format-card__check"></v-icon>
						</div>
					</div>

					<v-alert type="info" variant="tonal" density="compact" class="mt-4" rounded="lg">
						<div class="text-caption">
							<div class="font-weight-medium mb-1">录制说明</div>
							<div>• 录制内容包含所有参与者的音视频</div>
							<div>• 录制文件将在停止后可下载</div>
							<div>• 请确保有足够的网络带宽</div>
						</div>
					</v-alert>
				</v-card-text>
				<v-divider></v-divider>
				<v-card-actions class="pa-4">
					<v-btn variant="text" @click="handleClose">取消</v-btn>
					<v-spacer></v-spacer>
					<v-btn variant="flat" color="error" prepend-icon="mdi-record" rounded="lg" @click="$emit('start', selectedFormat)"> 开始录制 </v-btn>
				</v-card-actions>
			</template>

			<!-- ===== 阶段：recording（录制中）===== -->
			<template v-else-if="phase === PHASE.RECORDING">
				<v-card-text class="pa-5">
					<v-card variant="tonal" color="error" rounded="lg" class="pa-4 mb-4">
						<div class="d-flex align-center justify-space-between">
							<div class="d-flex flex-column ga-2">
								<div class="d-flex align-center">
									<span class="recording-dot mr-2"></span>
									<span class="text-body-2 font-weight-medium">录制中</span>
								</div>
								<div class="d-flex align-center">
									<v-icon icon="mdi-clock-outline" size="16" class="mr-2" color="error"></v-icon>
									<span class="text-body-2">
										时长：<strong>{{ formattedDuration }}</strong>
									</span>
								</div>
								<div class="d-flex align-center">
									<v-icon icon="mdi-file-video-outline" size="16" class="mr-2" color="error"></v-icon>
									<span class="text-body-2">
										格式：<strong>{{ recordingFormat.toUpperCase() }}</strong>
									</span>
								</div>
							</div>
							<div class="format-badge format-badge--recording">
								<span>{{ recordingFormat.toUpperCase() }}</span>
							</div>
						</div>
					</v-card>

					<v-alert type="warning" variant="tonal" density="compact" rounded="lg">
						<div class="text-caption">关闭此窗口不会停止录制，请使用下方停止按钮</div>
					</v-alert>
				</v-card-text>
				<v-divider></v-divider>
				<v-card-actions class="pa-4">
					<v-btn variant="text" @click="handleClose">最小化</v-btn>
					<v-spacer></v-spacer>
					<v-btn variant="flat" color="error" prepend-icon="mdi-stop-circle" rounded="lg" @click="$emit('stop')"> 停止录制 </v-btn>
				</v-card-actions>
			</template>

			<!-- ===== 阶段：stopping（上传 + 保存）===== -->
			<template v-else-if="phase === PHASE.STOPPING">
				<v-card-text class="pa-5 d-flex flex-column ga-4">
					<div class="d-flex align-center ga-3">
						<v-progress-circular indeterminate size="40" width="3" color="primary"></v-progress-circular>
						<div>
							<div class="text-body-2 font-weight-medium">正在保存录制文件</div>
							<div class="text-caption text-medium-emphasis">请勿关闭页面</div>
						</div>
					</div>
					<div>
						<div class="d-flex justify-space-between text-caption mb-1">
							<span>上传进度</span>
							<span>{{ uploadProgress }}%</span>
						</div>
						<v-progress-linear :model-value="uploadProgress" color="primary" height="6" rounded></v-progress-linear>
					</div>
				</v-card-text>
			</template>

			<!-- ===== 阶段：done（录制完成）===== -->
			<template v-else-if="phase === PHASE.DONE">
				<v-card-text class="pa-5">
					<v-card variant="tonal" color="success" rounded="lg" class="mb-4 pa-4">
						<div class="d-flex align-center justify-space-between">
							<div class="d-flex flex-column ga-2">
								<div class="d-flex align-center">
									<v-icon icon="mdi-clock-outline" size="16" class="mr-2" color="success"></v-icon>
									<span class="text-body-2">
										时长：<strong>{{ formattedDurationFromResult }}</strong>
									</span>
								</div>
								<div class="d-flex align-center">
									<v-icon icon="mdi-database-outline" size="16" class="mr-2" color="success"></v-icon>
									<span class="text-body-2">
										大小：<strong>{{ formattedFileSize }}</strong>
									</span>
								</div>
								<div class="d-flex align-center">
									<v-icon icon="mdi-file-video-outline" size="16" class="mr-2" color="success"></v-icon>
									<span class="text-body-2">
										格式：<strong>{{ fileExtension.toUpperCase() }}</strong>
									</span>
								</div>
							</div>
							<div class="format-badge format-badge--success">
								<span>{{ fileExtension.toUpperCase() }}</span>
							</div>
						</div>
					</v-card>

					<RecordingResultCard :result="recordingResult" @copy-url="onCopyUrl" :copied="copied" />
				</v-card-text>
				<v-divider></v-divider>
				<v-card-actions class="pa-4 ga-2">
					<v-btn variant="text" @click="handleClose">关闭</v-btn>
					<v-spacer></v-spacer>
					<v-btn v-if="canPreview" variant="tonal" color="primary" prepend-icon="mdi-play-circle-outline" rounded="lg" @click="$emit('preview', recordingResult.fileUrl)">
						预览
					</v-btn>
					<v-btn
						variant="flat"
						color="primary"
						prepend-icon="mdi-download"
						rounded="lg"
						@click="$emit('download', { url: recordingResult.fileUrl, name: downloadFileName })"
					>
						下载录制
					</v-btn>
				</v-card-actions>
			</template>

			<!-- ===== 阶段：error ===== -->
			<template v-else-if="phase === PHASE.ERROR">
				<v-card-text class="pa-5">
					<v-alert type="error" variant="tonal" rounded="lg">
						<div class="text-body-2 font-weight-medium mb-1">操作失败</div>
						<div class="text-caption">{{ errorMessage }}</div>
					</v-alert>
				</v-card-text>
				<v-divider></v-divider>
				<v-card-actions class="pa-4">
					<v-spacer></v-spacer>
					<v-btn variant="flat" color="error" @click="handleClose">关闭</v-btn>
				</v-card-actions>
			</template>
		</v-card>
	</v-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useClipboard } from '@vueuse/core'
import { RECORDING_PHASE } from '@/composables/useRecording'

// ==================== 子组件：URL 复制卡片 ====================
const RecordingResultCard = {
	name: 'RecordingResultCard',
	props: { result: Object, copied: Boolean },
	emits: ['copy-url'],
	template: `
        <div class="url-copy-box" @click="$emit('copy-url')">
            <v-icon icon="mdi-link-variant" size="16" class="mr-2 flex-shrink-0" color="primary"></v-icon>
            <span class="url-text text-caption">{{ result?.fileUrl }}</span>
            <v-icon :icon="copied ? 'mdi-check' : 'mdi-content-copy'" size="16"
                :color="copied ? 'success' : 'medium-emphasis'" class="ml-2 flex-shrink-0"></v-icon>
        </div>
    `,
}

// ==================== Props & Emits ====================
const props = defineProps({
	modelValue: Boolean,
	phase: {
		type: String,
		default: RECORDING_PHASE.IDLE,
	},
	recordingFormat: {
		type: String,
		default: 'webm',
	},
	formattedDuration: {
		type: String,
		default: '00:00',
	},
	recordingResult: {
		type: Object,
		default: null,
	},
	uploadProgress: {
		type: Number,
		default: 0,
	},
	errorMessage: {
		type: String,
		default: '',
	},
})

const emit = defineEmits(['update:modelValue', 'start', 'stop', 'close', 'download', 'preview'])

const PHASE = RECORDING_PHASE

// ==================== 本地状态 ====================
const selectedFormat = ref(props.recordingFormat)
const { copy, copied } = useClipboard({ legacy: true })

// ==================== 计算属性 ====================
const isActiveRecording = computed(() => props.phase === PHASE.RECORDING)
const isLoading = computed(() => [PHASE.CHECKING, PHASE.STARTING, PHASE.STOPPING].includes(props.phase))

const fileExtension = computed(() => {
	const url = props.recordingResult?.fileUrl || ''
	return url.split('.').pop()?.split('?')[0]?.toLowerCase() || props.recordingFormat
})

const canPreview = computed(() => ['mp4', 'webm'].includes(fileExtension.value))

const downloadFileName = computed(() => {
	const url = props.recordingResult?.fileUrl || ''
	const fromUrl = url.split('/').pop()?.split('?')[0]
	return fromUrl || `recording-${Date.now()}.${fileExtension.value}`
})

const formattedDurationFromResult = computed(() => {
	const s = props.recordingResult?.duration ?? 0
	const h = Math.floor(s / 3600)
	const m = Math.floor((s % 3600) / 60)
	const sec = s % 60
	if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
	return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
})

const formattedFileSize = computed(() => {
	const bytes = props.recordingResult?.fileSize ?? 0
	if (!bytes) return '未知'
	if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
	return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
})

// ==================== 标题栏动态样式 ====================
const titleText = computed(() => {
	const map = {
		[PHASE.IDLE]: '会议录制',
		[PHASE.CHECKING]: '检查录制状态',
		[PHASE.EXISTS]: '已有录制记录',
		[PHASE.STARTING]: '开始录制',
		[PHASE.RECORDING]: '录制中',
		[PHASE.STOPPING]: '正在保存',
		[PHASE.DONE]: '录制完成',
		[PHASE.ERROR]: '操作失败',
	}
	return map[props.phase] || '会议录制'
})

const subtitleText = computed(() => {
	const map = {
		[PHASE.CHECKING]: '请稍候...',
		[PHASE.EXISTS]: '以下为本次会议的录制文件',
		[PHASE.STARTING]: '选择录制格式后开始',
		[PHASE.RECORDING]: '正在录制音视频内容',
		[PHASE.STOPPING]: '上传完成后自动关闭',
		[PHASE.DONE]: '文件已生成，可下载或预览',
		[PHASE.ERROR]: '请关闭后重试',
	}
	return map[props.phase] || ''
})

const titleIcon = computed(() => {
	const map = {
		[PHASE.CHECKING]: 'mdi-loading',
		[PHASE.EXISTS]: 'mdi-file-video',
		[PHASE.STARTING]: 'mdi-record-circle',
		[PHASE.RECORDING]: 'mdi-record-circle',
		[PHASE.STOPPING]: 'mdi-cloud-upload',
		[PHASE.DONE]: 'mdi-check-circle',
		[PHASE.ERROR]: 'mdi-alert-circle',
	}
	return map[props.phase] || 'mdi-record-circle-outline'
})

const titleIconColor = computed(() => {
	const map = {
		[PHASE.CHECKING]: 'primary',
		[PHASE.EXISTS]: 'info',
		[PHASE.STARTING]: 'error',
		[PHASE.RECORDING]: 'error',
		[PHASE.STOPPING]: 'primary',
		[PHASE.DONE]: 'success',
		[PHASE.ERROR]: 'error',
	}
	return map[props.phase] || 'primary'
})

const accentBarClass = computed(() => ({
	'dialog-accent-bar--error': [PHASE.STARTING, PHASE.RECORDING].includes(props.phase),
	'dialog-accent-bar--success': [PHASE.DONE, PHASE.EXISTS].includes(props.phase),
	'dialog-accent-bar--primary': [PHASE.CHECKING, PHASE.STOPPING].includes(props.phase),
	'dialog-accent-bar--warning': props.phase === PHASE.ERROR,
}))

const iconWrapperClass = computed(() => ({
	'icon-wrapper--error': [PHASE.STARTING, PHASE.RECORDING].includes(props.phase),
	'icon-wrapper--success': [PHASE.DONE, PHASE.EXISTS].includes(props.phase),
	'icon-wrapper--primary': [PHASE.CHECKING, PHASE.STOPPING].includes(props.phase),
	'icon-wrapper--warning': props.phase === PHASE.ERROR,
}))

// ==================== 事件处理 ====================
const onCopyUrl = () => copy(props.recordingResult?.fileUrl || '')

const handleClose = () => {
	emit('close')
	emit('update:modelValue', false)
}

const handleDialogUpdate = val => {
	// 录制中或加载中不允许通过点击遮罩关闭
	if (!val && !isActiveRecording.value && !isLoading.value) {
		handleClose()
	}
}

// ==================== 格式列表 ====================
const formats = [
	{ value: 'mp4', label: 'MP4', icon: 'mdi-file-video', desc: '兼容性最佳\n推荐格式' },
	{ value: 'webm', label: 'WebM', icon: 'mdi-web', desc: '体积更小\n现代浏览器' },
]
</script>

<style scoped>
.recording-dialog {
	overflow: hidden;
}

/* 顶部装饰条 */
.dialog-accent-bar {
	height: 4px;
}
.dialog-accent-bar--error {
	background: linear-gradient(90deg, rgb(var(--v-theme-error)), rgba(var(--v-theme-error), 0.3));
}
.dialog-accent-bar--success {
	background: linear-gradient(90deg, rgb(var(--v-theme-success)), rgba(var(--v-theme-success), 0.3));
}
.dialog-accent-bar--primary {
	background: linear-gradient(90deg, rgb(var(--v-theme-primary)), rgba(var(--v-theme-primary), 0.3));
}
.dialog-accent-bar--warning {
	background: linear-gradient(90deg, rgb(var(--v-theme-warning)), rgba(var(--v-theme-warning), 0.3));
}

/* 图标包装器 */
.icon-wrapper {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 44px;
	height: 44px;
	border-radius: 12px;
	flex-shrink: 0;
}
.icon-wrapper--error {
	background: rgba(var(--v-theme-error), 0.1);
}
.icon-wrapper--success {
	background: rgba(var(--v-theme-success), 0.1);
}
.icon-wrapper--primary {
	background: rgba(var(--v-theme-primary), 0.1);
}
.icon-wrapper--warning {
	background: rgba(var(--v-theme-warning), 0.1);
}

/* 格式选择 */
.format-grid {
	display: grid;
	grid-template-columns: repeat(2, 1fr);
	gap: 12px;
}

.format-card {
	position: relative;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: 20px 12px 16px;
	border-radius: 12px;
	border: 2px solid rgba(var(--v-border-color), var(--v-border-opacity));
	cursor: pointer;
	transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
	background: rgb(var(--v-theme-surface));
	text-align: center;
	min-height: 110px;
}

.format-card:hover {
	border-color: rgba(var(--v-theme-primary), 0.5);
	background: rgba(var(--v-theme-primary), 0.04);
	transform: translateY(-2px);
}

.format-card--active {
	border-color: rgb(var(--v-theme-primary));
	background: rgba(var(--v-theme-primary), 0.08);
}

.format-card__label {
	font-size: 0.9rem;
	font-weight: 600;
	margin-bottom: 4px;
}

.format-card__desc {
	white-space: pre-line;
	line-height: 1.4;
	font-size: 0.72rem;
}

.format-card__check {
	position: absolute;
	top: 8px;
	right: 8px;
}

/* 格式徽标 */
.format-badge {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 52px;
	height: 52px;
	border-radius: 12px;
	font-size: 0.75rem;
	font-weight: 700;
	letter-spacing: 0.5px;
	flex-shrink: 0;
}
.format-badge--recording {
	background: rgba(var(--v-theme-error), 0.15);
	border: 2px solid rgba(var(--v-theme-error), 0.3);
	color: rgb(var(--v-theme-error));
}
.format-badge--success {
	background: rgba(var(--v-theme-success), 0.15);
	border: 2px solid rgba(var(--v-theme-success), 0.3);
	color: rgb(var(--v-theme-success));
}

/* 录制中红点脉冲 */
.recording-dot {
	display: inline-block;
	width: 10px;
	height: 10px;
	border-radius: 50%;
	background: rgb(var(--v-theme-error));
	animation: recordPulse 1.2s ease-in-out infinite;
	flex-shrink: 0;
}
@keyframes recordPulse {
	0%,
	100% {
		opacity: 1;
		transform: scale(1);
	}
	50% {
		opacity: 0.4;
		transform: scale(0.75);
	}
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
