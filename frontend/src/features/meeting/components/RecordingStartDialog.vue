<template>
	<v-dialog v-model="model" max-width="440" transition="dialog-bottom-transition" persistent>
		<v-card class="recording-start-dialog" rounded="xl" elevation="8">
			<!-- 顶部装饰条 -->
			<div class="dialog-accent-bar"></div>

			<v-card-title class="d-flex align-center pa-5 pb-3">
				<div class="record-icon-wrapper mr-3">
					<v-icon icon="mdi-record-circle" color="error" size="28"></v-icon>
				</div>
				<div>
					<div class="text-h6 font-weight-medium">开始录制</div>
					<div class="text-caption text-medium-emphasis">选择录制格式后开始</div>
				</div>
				<v-spacer></v-spacer>
				<v-btn icon="mdi-close" variant="text" size="small" :disabled="loading" @click="model = false"></v-btn>
			</v-card-title>

			<v-divider></v-divider>

			<v-card-text class="pa-5">
				<!-- 格式选择 -->
				<div class="text-subtitle-2 font-weight-medium mb-3">录制格式</div>
				<div class="format-grid">
					<div
						v-for="fmt in formats"
						:key="fmt.value"
						class="format-card"
						:class="{ 'format-card--active': selectedFormat === fmt.value }"
						@click="selectedFormat = fmt.value"
					>
						<v-icon :icon="fmt.icon" size="28" class="mb-2" :color="selectedFormat === fmt.value ? 'primary' : 'medium-emphasis'"> </v-icon>
						<div class="format-card__label">{{ fmt.label }}</div>
						<div class="format-card__desc text-caption text-medium-emphasis">
							{{ fmt.desc }}
						</div>
						<!-- 选中勾选 -->
						<v-icon v-if="selectedFormat === fmt.value" icon="mdi-check-circle" size="16" color="primary" class="format-card__check"></v-icon>
					</div>
				</div>

				<!-- 提示信息 -->
				<v-alert type="info" variant="tonal" density="compact" class="mt-4" rounded="lg">
					<div class="text-caption">
						<div class="font-weight-medium mb-1">录制说明</div>
						<div>• 录制内容包含所有参与者的音视频</div>
						<div>• 录制文件将在会议结束后可下载</div>
						<div>• 请确保有足够的存储空间</div>
					</div>
				</v-alert>
			</v-card-text>

			<v-divider></v-divider>

			<v-card-actions class="pa-4">
				<v-btn variant="text" :disabled="loading" @click="model = false">取消</v-btn>
				<v-spacer></v-spacer>
				<v-btn variant="flat" color="error" prepend-icon="mdi-record" :loading="loading" rounded="lg" @click="$emit('confirm', selectedFormat)"> 开始录制 </v-btn>
			</v-card-actions>
		</v-card>
	</v-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
	modelValue: Boolean,
	loading: Boolean,
	format: { type: String, default: 'mp4' },
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const model = defineModel()

const selectedFormat = ref(props.format)

watch(
	() => props.format,
	val => {
		selectedFormat.value = val
	},
)

const formats = [
	{
		value: 'mp4',
		label: 'MP4',
		icon: 'mdi-file-video',
		desc: '兼容性最佳\n推荐格式',
	},
	{
		value: 'webm',
		label: 'WebM',
		icon: 'mdi-web',
		desc: '体积更小\n现代浏览器',
	},
]
</script>

<style scoped>
.recording-start-dialog {
	overflow: hidden;
}

/* 顶部红色装饰条 */
.dialog-accent-bar {
	height: 4px;
	background: linear-gradient(90deg, rgb(var(--v-theme-error)), rgba(var(--v-theme-error), 0.4));
}

/* 录制图标背景 */
.record-icon-wrapper {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 44px;
	height: 44px;
	border-radius: 12px;
	background: rgba(var(--v-theme-error), 0.1);
	flex-shrink: 0;
}

/* 格式选择网格 */
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
	box-shadow: 0 4px 12px rgba(var(--v-theme-primary), 0.12);
}

.format-card--active {
	border-color: rgb(var(--v-theme-primary));
	background: rgba(var(--v-theme-primary), 0.08);
}

.format-card__label {
	font-size: 0.9rem;
	font-weight: 600;
	color: rgb(var(--v-theme-on-surface));
	margin-bottom: 4px;
	letter-spacing: 0.3px;
}

.format-card__desc {
	white-space: pre-line;
	line-height: 1.4;
	font-size: 0.72rem;
}

/* 选中角标 */
.format-card__check {
	position: absolute;
	top: 8px;
	right: 8px;
}
</style>
