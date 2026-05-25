<template>
	<v-card>
		<v-card-title>{{ isEdit ? '编辑会议' : '创建会议' }}</v-card-title>

		<v-divider></v-divider>

		<v-card-text class="pa-6">
			<v-form ref="formRef" v-model="valid">
				<v-text-field
					v-model="formData.title"
					label="会议主题"
					:rules="[rules.required]"
					variant="outlined"
					prepend-inner-icon="mdi-text"
					counter="100"
					maxlength="100"
				></v-text-field>

				<v-row class="mt-4">
					<v-col cols="12" md="6">
						<v-text-field
							v-model="formData.startDate"
							label="开始日期"
							type="date"
							:rules="[rules.required]"
							variant="outlined"
							prepend-inner-icon="mdi-calendar"
						></v-text-field>
					</v-col>

					<v-col cols="12" md="6">
						<v-text-field
							v-model="formData.startTime"
							label="开始时间"
							type="time"
							:rules="[rules.required]"
							variant="outlined"
							prepend-inner-icon="mdi-clock"
						></v-text-field>
					</v-col>
				</v-row>

				<v-select v-model="formData.duration" :items="durationOptions" label="持续时间" variant="outlined" prepend-inner-icon="mdi-timer" class="mt-4"></v-select>

				<v-divider class="mt-6 mb-4"></v-divider>
				<div class="text-subtitle-2 text-grey-darken-2 mb-3">会议设置</div>

				<v-switch v-model="formData.enableWaitingRoom" label="启用等候室" color="primary" hide-details density="comfortable"></v-switch>

				<v-switch v-model="formData.enableRecording" label="自动录制" color="primary" hide-details density="comfortable" class="mt-1"></v-switch>

				<v-switch v-model="formData.disableCamera" label="默认关闭摄像头" color="primary" hide-details density="comfortable" class="mt-1"></v-switch>
			</v-form>
		</v-card-text>

		<v-divider></v-divider>

		<v-card-actions class="pa-4">
			<v-spacer></v-spacer>
			<v-btn @click="emit('cancel')">取消</v-btn>
			<v-btn color="primary" :disabled="!valid" :loading="saving" @click="handleSave">
				{{ isEdit ? '保存' : '创建' }}
			</v-btn>
		</v-card-actions>
	</v-card>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useDateFormat } from '@vueuse/core'

const props = defineProps({
	meeting: {
		type: Object,
		default: null,
	},
})

const emit = defineEmits(['save', 'cancel'])

const formRef = ref(null)
const valid = ref(false)
const saving = ref(false)

// 保留原始 description 中的其他字段（如 allowedCodecs），防止编辑时丢失
const originalDescriptionExtra = ref({})

const isEdit = computed(() => !!props.meeting?.roomId)

const formData = ref({
	title: '',
	startDate: useDateFormat(new Date(), 'YYYY-MM-DD').value,
	startTime: useDateFormat(new Date(), 'HH:mm').value,
	duration: 60,
	enableWaitingRoom: false,
	enableRecording: false,
	disableCamera: false,
})

const rules = {
	required: value => !!value || '此字段为必填项',
}

const durationOptions = [
	{ title: '15分钟', value: 15 },
	{ title: '30分钟', value: 30 },
	{ title: '45分钟', value: 45 },
	{ title: '1小时', value: 60 },
	{ title: '1.5小时', value: 90 },
	{ title: '2小时', value: 120 },
	{ title: '3小时', value: 180 },
]

const handleSave = async () => {
	const { valid: isValid } = await formRef.value.validate()
	if (!isValid) return

	saving.value = true
	try {
		const description = JSON.stringify({
			...originalDescriptionExtra.value,
			enableWaitingRoom: formData.value.enableWaitingRoom,
			enableRecording: formData.value.enableRecording,
			disableCamera: formData.value.disableCamera,
		})

		const meetingData = {
			title: formData.value.title,
			description,
			startTime: new Date(`${formData.value.startDate} ${formData.value.startTime}`).toISOString(),
			duration: formData.value.duration,
		}

		emit('save', meetingData)
	} finally {
		saving.value = false
	}
}

// 如果是编辑模式，填充表单数据
watch(
	() => props.meeting,
	meeting => {
		if (meeting) {
			const startDate = new Date(meeting.startTime)

			let parsedDesc = {}
			try {
				parsedDesc = JSON.parse(meeting.description || '{}')
			} catch {
				parsedDesc = {}
			}

			// 提取已知字段，其余保留为 extra 防止丢失
			const { enableWaitingRoom, enableRecording, disableCamera, ...extra } = parsedDesc
			originalDescriptionExtra.value = extra

			formData.value = {
				title: meeting.title || '',
				startDate: useDateFormat(startDate, 'YYYY-MM-DD').value,
				startTime: useDateFormat(startDate, 'HH:mm').value,
				duration: meeting.duration || 60,
				enableWaitingRoom: enableWaitingRoom ?? false,
				enableRecording: enableRecording ?? false,
				disableCamera: disableCamera ?? false,
			}
		}
	},
	{ immediate: true },
)
</script>
