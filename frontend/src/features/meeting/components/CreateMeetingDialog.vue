<template>
	<v-dialog v-model="isOpen" max-width="600" persistent scrollable>
		<v-card rounded="xl" elevation="8">
			<!-- 对话框标题 -->
			<v-card-title class="d-flex align-center px-6 pt-6 pb-4">
				<v-icon color="primary" size="32" class="mr-3">mdi-video-plus</v-icon>
				<span class="text-h5 font-weight-bold">创建会议</span>
				<v-spacer></v-spacer>
				<v-btn icon="mdi-close" variant="text" size="small" @click="handleClose"></v-btn>
			</v-card-title>

			<v-divider></v-divider>

			<!-- 对话框内容 -->
			<v-card-text class="px-6 py-6">
				<v-form ref="formRef" v-model="formValid" @submit.prevent="handleSubmit">
					<!-- 会议标题 -->
					<v-text-field
						v-model="formData.title"
						label="会议标题"
						placeholder="请输入会议标题"
						variant="outlined"
						density="comfortable"
						color="primary"
						:rules="titleRules"
						prepend-inner-icon="mdi-text"
						clearable
						autofocus
						counter="50"
						maxlength="50"
						class="mb-4"
					></v-text-field>

					<!-- 会议类型 -->
					<v-select
						v-model="formData.type"
						label="会议类型"
						:items="meetingTypes"
						item-title="text"
						item-value="value"
						variant="outlined"
						density="comfortable"
						color="primary"
						prepend-inner-icon="mdi-form-select"
						class="mb-4"
					></v-select>

					<!-- 开始时间（仅预约会议显示） -->
					<v-text-field
						v-show="formData.type === 2"
						v-model="formData.startTime"
						label="开始时间"
						type="datetime-local"
						variant="outlined"
						density="comfortable"
						color="primary"
						prepend-inner-icon="mdi-clock-outline"
						class="mb-4"
						:rules="startTimeRules"
					></v-text-field>

					<!-- 最大参与者数量 -->
					<v-text-field
						v-model.number="formData.maxParticipants"
						label="最大参与者数量"
						type="number"
						variant="outlined"
						density="comfortable"
						color="primary"
						:rules="maxParticipantsRules"
						prepend-inner-icon="mdi-account-multiple"
						hint="建议不超过50人"
						persistent-hint
						class="mb-4"
					></v-text-field>

					<!-- 高级设置折叠面板 -->
					<v-expansion-panels variant="accordion" class="mb-4">
						<v-expansion-panel rounded="lg" elevation="0" bg-color="surface-variant">
							<v-expansion-panel-title>
								<div class="d-flex align-center">
									<v-icon color="primary" class="mr-2">mdi-cog</v-icon>
									<span class="font-weight-medium">高级设置</span>
									<v-chip size="small" color="primary" variant="flat" class="ml-2">可选</v-chip>
								</div>
							</v-expansion-panel-title>

							<v-expansion-panel-text class="pt-4">
								<!-- SFU节点ID -->
								<v-text-field
									v-model.number="formData.sfuNodeId"
									label="SFU节点ID"
									type="number"
									variant="outlined"
									density="comfortable"
									color="primary"
									prepend-inner-icon="mdi-server"
									hint="默认0表示自动分配"
									persistent-hint
									clearable
									class="mb-4"
								></v-text-field>

								<!-- 会议设置 -->
								<div class="mb-2">
									<v-label class="text-body-2 font-weight-medium mb-2">会议设置</v-label>

									<!-- 启用录制 -->
									<v-switch v-model="settings.enableRecording" label="启用录制" color="primary" hide-details density="comfortable" class="mb-2">
										<template #prepend>
											<v-icon color="primary">mdi-record-rec</v-icon>
										</template>
									</v-switch>

									<!-- 允许的编解码器 -->
									<v-select
										v-model="settings.allowedCodecs"
										label="允许的编解码器"
										:items="codecOptions"
										variant="outlined"
										density="comfortable"
										color="primary"
										multiple
										chips
										closable-chips
										prepend-inner-icon="mdi-video-wireless"
										hint="选择支持的音视频编解码器"
										persistent-hint
										class="mb-2"
									></v-select>

									<!-- 等候室 -->
									<v-switch v-model="settings.enableWaitingRoom" label="启用等候室" color="primary" hide-details density="comfortable" class="mb-2">
										<template #prepend>
											<v-icon color="primary">mdi-door-open</v-icon>
										</template>
									</v-switch>

									<!-- 禁用摄像头 -->
									<v-switch v-model="settings.disableCamera" label="默认关闭摄像头" color="primary" hide-details density="comfortable">
										<template #prepend>
											<v-icon color="primary">mdi-camera-off</v-icon>
										</template>
									</v-switch>
								</div>
							</v-expansion-panel-text>
						</v-expansion-panel>
					</v-expansion-panels>
				</v-form>
			</v-card-text>

			<v-divider></v-divider>

			<!-- 对话框操作 -->
			<v-card-actions class="px-6 py-4">
				<v-spacer></v-spacer>
				<v-btn variant="text" color="grey-darken-1" :disabled="loading" @click="handleClose"> 取消 </v-btn>
				<v-btn variant="flat" color="primary" :loading="loading" :disabled="!formValid" @click="handleSubmit">
					<v-icon left>mdi-check</v-icon>
					创建会议
				</v-btn>
			</v-card-actions>
		</v-card>
	</v-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { $notify } from '@/plugins/notification'
import { useDateFormat } from '@vueuse/core'

const props = defineProps({
	modelValue: {
		type: Boolean,
		default: false,
	},
	userName: {
		type: String,
		default: '',
	},
})

const emit = defineEmits(['update:modelValue', 'submit'])

// 对话框状态
const isOpen = ref(props.modelValue)

watch(
	() => props.modelValue,
	val => {
		isOpen.value = val
	},
)

watch(isOpen, val => {
	emit('update:modelValue', val)
})

// 表单引用和验证状态
const formRef = ref(null)
const formValid = ref(false)
const loading = ref(false)

// 表单数据
const formData = reactive({
	title: `${props.userName}的会议`,
	type: 2, // 默认为预约会议
	maxParticipants: 15,
	sfuNodeId: 0,
	startTime: '', // 添加开始时间
})

// 会议设置
const settings = reactive({
	enableRecording: false,
	allowedCodecs: ['opus', 'VP8'],
	enableWaitingRoom: false,
	disableCamera: false,
})

// 会议类型选项
const meetingTypes = [
	{ text: '即时会议', value: 1 },
	{ text: '预约会议', value: 2 },
]

// 编解码器选项
const codecOptions = ['opus', 'VP8', 'VP9', 'H264', 'H265', 'AV1']

// 表单验证规则
const titleRules = [v => !!v || '会议标题不能为空', v => (v && v.length >= 2) || '会议标题至少2个字符', v => (v && v.length <= 50) || '会议标题不能超过50个字符']

const maxParticipantsRules = [v => !!v || '参与者数量不能为空', v => v > 0 || '参与者数量必须大于0', v => v <= 100 || '参与者数量不能超过100']

const startTimeRules = [v => formData.type !== 2 || !!v || '请选择开始时间', v => formData.type !== 2 || !v || new Date(v).getTime() > Date.now() || '开始时间必须晚于当前时间']
// 关闭对话框
const handleClose = () => {
	if (!loading.value) {
		isOpen.value = false
		resetForm()
	}
}

// 重置表单
const resetForm = () => {
	formData.title = ''
	formData.type = 1
	formData.maxParticipants = 15
	formData.sfuNodeId = 0
	formData.startTime = ''

	settings.enableRecording = false
	settings.allowedCodecs = ['opus', 'VP8']
	settings.enableWaitingRoom = false
	settings.disableCamera = false

	formRef.value?.resetValidation()
}

// 提交表单
const handleSubmit = async () => {
	// 验证表单
	const { valid } = await formRef.value.validate()
	if (!valid) {
		$notify.warning('请填写完整的会议信息')
		return
	}

	try {
		loading.value = true

		// 处理时间格式化为后端要求的 "YYYY-MM-DD HH:mm:ss"
		const rawTime = formData.type === 2 ? formData.startTime : new Date()
		const formattedStartTime = useDateFormat(rawTime, 'YYYY-MM-DD HH:mm:ss').value

		// 构建提交数据
		const submitData = {
			title: formData.title.trim(),
			type: formData.type,
			maxParticipants: formData.maxParticipants,
			sfuNodeId: formData.sfuNodeId,
			startTime: formattedStartTime,
			settings: JSON.stringify(settings),
		}

		// 触发提交事件，父组件处理API调用
		emit('submit', submitData)

		// 成功后关闭对话框
		isOpen.value = false
		resetForm()
	} catch (error) {
		console.error('Submit meeting error:', error)
		$notify.error('创建会议失败，请重试')
	} finally {
		loading.value = false
	}
}
</script>

<style scoped>
/* 对话框动画 */
.v-dialog {
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 表单项间距优化 */
:deep(.v-input) {
	margin-bottom: 0;
}

/* 折叠面板样式 */
:deep(.v-expansion-panel) {
	border: 1px solid rgba(var(--v-theme-primary), 0.12);
}

:deep(.v-expansion-panel-title) {
	padding: 16px;
}

:deep(.v-expansion-panel-text__wrapper) {
	padding: 0 16px 16px;
}

/* Switch 样式优化 */
:deep(.v-switch) {
	margin-bottom: 8px;
}

:deep(.v-switch .v-label) {
	font-size: 0.875rem;
	opacity: 0.87;
}
</style>
