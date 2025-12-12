<template>
	<v-text-field
		:model-value="modelValue"
		label="验证码"
		placeholder="请输入6位验证码"
		type="text"
		maxlength="6"
		:error-messages="errorMessages"
		:disabled="disabled"
		@update:model-value="handleInput"
	>
		<template #prepend-inner>
			<v-icon>mdi-shield-lock</v-icon>
		</template>
		<template #append-inner>
			<v-btn
				:disabled="disabled || countdown > 0 || sending"
				:loading="sending"
				variant="text"
				color="primary"
				size="small"
				class="text-none"
				@click="handleSend"
			>
				{{ buttonText }}
			</v-btn>
		</template>
	</v-text-field>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useIntervalFn } from '@vueuse/core'

const props = defineProps({
	modelValue: {
		type: String,
		default: '',
	},
	phone: {
		type: String,
		required: true,
	},
	errorMessages: {
		type: [String, Array],
		default: '',
	},
	disabled: {
		type: Boolean,
		default: false,
	},
})

const emit = defineEmits(['update:modelValue', 'send'])

// 倒计时
const countdown = ref(0)
const sending = ref(false)

// 倒计时定时器
const { pause, resume } = useIntervalFn(
	() => {
		if (countdown.value > 0) {
			countdown.value--
		} else {
			pause()
		}
	},
	1000,
	{ immediate: false },
)

// 按钮文字
const buttonText = computed(() => {
	if (countdown.value > 0) {
		return `${countdown.value}秒后重试`
	}
	return '获取验证码'
})

// 处理输入
const handleInput = value => {
	// 只允许输入数字
	const cleaned = value.replace(/\D/g, '')
	emit('update:modelValue', cleaned)
}

// 发送验证码
const handleSend = async () => {
	sending.value = true
	try {
		await emit('send')
		// 开始倒计时
		countdown.value = 60
		resume()
	} catch (error) {
		console.error('发送验证码失败:', error)
	} finally {
		sending.value = false
	}
}
</script>
