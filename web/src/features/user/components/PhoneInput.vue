<template>
	<v-text-field
		:model-value="modelValue"
		label="手机号"
		placeholder="请输入手机号"
		type="tel"
		maxlength="11"
		:error-messages="errorMessages"
		:prepend-inner-icon="icon"
		clearable
		@update:model-value="handleInput"
		@blur="handleBlur"
	>
	</v-text-field>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
	modelValue: {
		type: String,
		default: '',
	},
	errorMessages: {
		type: [String, Array],
		default: '',
	},
})

const emit = defineEmits(['update:modelValue', 'blur'])

// 验证手机号
const isValid = computed(() => {
	const phoneRegex = /^1[3-9]\d{9}$/
	return phoneRegex.test(props.modelValue)
})

// 图标
const icon = computed(() => {
	if (!props.modelValue) return 'mdi-cellphone'
	return isValid.value ? 'mdi-check-circle' : 'mdi-cellphone'
})

// 处理输入
const handleInput = value => {
	// 只允许输入数字
	const cleaned = value.replace(/\D/g, '')
	emit('update:modelValue', cleaned)
}

// 处理失焦
const handleBlur = () => {
	emit('blur')
}
</script>
