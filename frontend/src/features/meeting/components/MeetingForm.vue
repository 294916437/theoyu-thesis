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

				<v-textarea
					v-model="formData.description"
					label="会议描述(可选)"
					variant="outlined"
					prepend-inner-icon="mdi-text-box"
					rows="3"
					counter="500"
					maxlength="500"
					class="mt-4"
				></v-textarea>

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

				<v-select
					v-model="formData.duration"
					:items="durationOptions"
					label="持续时间"
					variant="outlined"
					prepend-inner-icon="mdi-timer"
					class="mt-4"
				></v-select>

				<v-combobox
					v-model="formData.participants"
					:items="availableParticipants"
					label="参与者(可选)"
					variant="outlined"
					prepend-inner-icon="mdi-account-multiple"
					multiple
					chips
					closable-chips
					class="mt-4"
				>
					<template #chip="{ props, item }">
						<v-chip v-bind="props" :text="item.raw.name || item.raw"></v-chip>
					</template>
				</v-combobox>

				<v-expansion-panels class="mt-4">
					<v-expansion-panel>
						<v-expansion-panel-title>
							<v-icon left>mdi-cog</v-icon>
							高级设置
						</v-expansion-panel-title>
						<v-expansion-panel-text>
							<v-switch
								v-model="formData.enableWaitingRoom"
								label="启用等候室"
								color="primary"
								hide-details
							></v-switch>

							<v-switch
								v-model="formData.enableRecording"
								label="自动录制"
								color="primary"
								hide-details
								class="mt-2"
							></v-switch>

							<v-switch
								v-model="formData.muteOnEntry"
								label="加入时静音"
								color="primary"
								hide-details
								class="mt-2"
							></v-switch>

							<v-text-field
								v-model="formData.password"
								label="会议密码(可选)"
								type="password"
								variant="outlined"
								prepend-inner-icon="mdi-lock"
								class="mt-4"
							></v-text-field>

							<v-select
								v-model="formData.maxParticipants"
								:items="maxParticipantsOptions"
								label="最大参与人数"
								variant="outlined"
								prepend-inner-icon="mdi-account-group"
								class="mt-4"
							></v-select>
						</v-expansion-panel-text>
					</v-expansion-panel>
				</v-expansion-panels>
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

const isEdit = computed(() => !!props.meeting?.id)

const formData = ref({
	title: '',
	description: '',
	startDate: useDateFormat(new Date(), 'YYYY-MM-DD').value,
	startTime: useDateFormat(new Date(), 'HH:mm').value,
	duration: 60,
	participants: [],
	enableWaitingRoom: false,
	enableRecording: false,
	muteOnEntry: true,
	password: '',
	maxParticipants: 100,
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

const maxParticipantsOptions = [
	{ title: '10人', value: 10 },
	{ title: '25人', value: 25 },
	{ title: '50人', value: 50 },
	{ title: '100人', value: 100 },
	{ title: '250人', value: 250 },
	{ title: '无限制', value: -1 },
]

const availableParticipants = ref([
	{ name: '李四', email: 'lisi@example.com' },
	{ name: '王五', email: 'wangwu@example.com' },
	{ name: '赵六', email: 'zhaoliu@example.com' },
])

const handleSave = async () => {
	const { valid: isValid } = await formRef.value.validate()
	if (!isValid) return

	saving.value = true
	try {
		const meetingData = {
			...formData.value,
			startTime: new Date(`${formData.value.startDate} ${formData.value.startTime}`).toISOString(),
		}

		emit('save', meetingData)
	} finally {
		saving.value = false
	}
}

// 如果是编辑模式,填充表单数据
watch(
	() => props.meeting,
	meeting => {
		if (meeting) {
			const startDate = new Date(meeting.startTime)
			formData.value = {
				...meeting,
				startDate: useDateFormat(startDate, 'YYYY-MM-DD').value,
				startTime: useDateFormat(startDate, 'HH:mm').value,
			}
		}
	},
	{ immediate: true },
)
</script>
