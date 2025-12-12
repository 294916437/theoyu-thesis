<template>
	<v-form ref="formRef" @submit.prevent>
		<!-- 昵称 -->
		<div class="info-field">
			<div class="field-label">
				<v-icon size="20" color="primary" class="mr-2">mdi-account</v-icon>
				<span class="text-subtitle-2 font-weight-medium">昵称</span>
			</div>
			<v-text-field
				v-if="isEditing"
				v-model="formData.nickname"
				placeholder="请输入昵称"
				:rules="[rules.required, rules.nickname]"
				counter="20"
				maxlength="20"
				density="comfortable"
			/>
			<div v-else class="field-value">{{ originalData.nickname }}</div>
		</div>

		<v-divider class="my-4" />

		<!-- 生日 -->
		<div class="info-field">
			<div class="field-label">
				<v-icon size="20" color="primary" class="mr-2">mdi-cake-variant</v-icon>
				<span class="text-subtitle-2 font-weight-medium">生日</span>
			</div>
			<v-text-field
				v-if="isEditing"
				v-model="formData.birthday"
				type="date"
				:rules="[rules.required]"
				density="comfortable"
			/>
			<div v-else class="field-value">{{ formatBirthday(originalData.birthday) }}</div>
		</div>

		<v-divider class="my-4" />

		<!-- 个人简介 -->
		<div class="info-field">
			<div class="field-label">
				<v-icon size="20" color="primary" class="mr-2">mdi-text</v-icon>
				<span class="text-subtitle-2 font-weight-medium">个人简介</span>
			</div>
			<v-textarea
				v-if="isEditing"
				v-model="formData.introduction"
				placeholder="请输入个人简介"
				:rules="[rules.introduction]"
				counter="200"
				maxlength="200"
				rows="4"
				auto-grow
				density="comfortable"
			/>
			<div v-else class="field-value multiline">
				{{ originalData.introduction || '暂无简介' }}
			</div>
		</div>
	</v-form>
</template>

<script setup>
import { computed } from 'vue'
import { useDateFormat } from '@vueuse/core'

const props = defineProps({
	modelValue: {
		type: Object,
		required: true,
	},
	isEditing: {
		type: Boolean,
		default: false,
	},
	originalData: {
		type: Object,
		required: true,
	},
})

const emit = defineEmits(['update:modelValue'])

const formData = computed({
	get: () => props.modelValue,
	set: value => emit('update:modelValue', value),
})

// 验证规则
const rules = {
	required: value => !!value || '此字段不能为空',
	nickname: value => {
		if (!value) return true
		if (value.length < 2) return '昵称至少2个字符'
		if (value.length > 20) return '昵称最多20个字符'
		return true
	},
	introduction: value => {
		if (!value) return true
		if (value.length > 200) return '简介最多200个字符'
		return true
	},
}

// 格式化生日
const formatBirthday = birthday => {
	if (!birthday) return '未设置'
	const formatted = useDateFormat(birthday, 'YYYY年MM月DD日')
	return formatted.value
}
</script>

<style scoped>
.info-field {
	margin-bottom: 8px;
}

.field-label {
	display: flex;
	align-items: center;
	margin-bottom: 8px;
	color: rgb(var(--v-theme-on-surface));
}

.field-value {
	padding: 12px 16px;
	background-color: rgb(var(--v-theme-surface-variant));
	border-radius: 8px;
	color: rgb(var(--v-theme-on-surface));
	min-height: 48px;
	display: flex;
	align-items: center;
}

.field-value.multiline {
	align-items: flex-start;
	white-space: pre-wrap;
	word-break: break-word;
}
</style>
