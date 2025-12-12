<template>
	<v-container fluid class="login-container">
		<v-row justify="center" align="center" class="fill-height">
			<v-col cols="12" sm="8" md="6" lg="4">
				<v-card class="login-card" elevation="8">
					<v-card-text class="pa-8">
						<!-- Logo 区域 -->
						<div class="text-center mb-8">
							<v-icon size="64" color="primary">mdi-account-circle</v-icon>
							<h1 class="text-h4 font-weight-bold mt-4 text-primary">欢迎</h1>
							<p class="text-body-2 text-medium-emphasis mt-2">无需注册，即可登陆</p>
						</div>

						<!-- 手机号输入 -->
						<PhoneInput v-model="phone" :error-messages="phoneError" class="mb-4" />

						<!-- 验证码输入 -->
						<VerificationCodeInput
							v-model="code"
							:phone="phone"
							:error-messages="codeError"
							:disabled="!isPhoneValid"
							class="mb-6"
							@send="handleSendCode"
						/>

						<!-- 登录按钮 -->
						<v-btn
							block
							size="large"
							:loading="loading"
							:disabled="!canSubmit"
							class="mb-4"
							@click="handleLogin"
						>
							登录
						</v-btn>

						<!-- 提示文字 -->
						<div class="text-center text-caption text-medium-emphasis">
							登录即表示同意用户协议和隐私政策
						</div>
					</v-card-text>
				</v-card>
			</v-col>
		</v-row>

		<!-- 提示消息 -->
		<v-snackbar v-model="snackbar.show" :color="snackbar.color" :timeout="3000">
			{{ snackbar.message }}
			<template #actions>
				<v-btn variant="text" @click="snackbar.show = false">关闭</v-btn>
			</template>
		</v-snackbar>
	</v-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import PhoneInput from '../components/PhoneInput.vue'
import VerificationCodeInput from '../components/VerificationCodeInput.vue'
import { $notify } from '@/plugins/notification'

const router = useRouter()

// 表单数据
const phone = ref('')
const code = ref('')
const phoneError = ref('')
const codeError = ref('')
const loading = ref(false)

// 提示消息
const snackbar = ref({
	show: false,
	message: '',
	color: 'success',
})

// 手机号验证
const isPhoneValid = computed(() => {
	const phoneRegex = /^1[3-9]\d{9}$/
	return phoneRegex.test(phone.value)
})

// 是否可以提交
const canSubmit = computed(() => {
	return isPhoneValid.value && code.value.length === 6
})

// 发送验证码
const handleSendCode = async () => {
	try {
		// TODO: 调用后端API发送验证码
		// await api.sendVerificationCode({ phone: phone.value })

		console.log('发送验证码到:', phone.value)
		$notify.success('验证码已发送')

		// 模拟API调用
		return new Promise(resolve => setTimeout(resolve, 1000))
	} catch (error) {
		$notify.error('发送验证码失败，请重试')
		throw error
	}
}

// 登录处理
const handleLogin = async () => {
	// 清除错误信息
	phoneError.value = ''
	codeError.value = ''

	// 验证手机号
	if (!isPhoneValid.value) {
		phoneError.value = '请输入正确的手机号'
		return
	}

	// 验证验证码
	if (code.value.length !== 6) {
		codeError.value = '请输入6位验证码'
		return
	}

	loading.value = true

	try {
		// TODO: 调用后端API进行登录/注册
		// const response = await api.loginWithCode({
		//   phone: phone.value,
		//   code: code.value,
		// })

		console.log('登录信息:', { phone: phone.value, code: code.value })

		// 模拟API调用
		await new Promise(resolve => setTimeout(resolve, 1500))

		// TODO: 保存token和用户信息
		// localStorage.setItem('token', response.token)
		// localStorage.setItem('user', JSON.stringify(response.user))

		$notify.success('登录成功')

		// 跳转到首页或profile页面
		setTimeout(() => {
			router.push('/')
		}, 500)
	} catch (error) {
		$notify.error(error.message || '登录失败，请重试')
	} finally {
		loading.value = false
	}
}
</script>

<style scoped>
.login-container {
	min-height: 100vh;
	background: linear-gradient(135deg, rgb(var(--v-theme-primary)) 0%, rgb(var(--v-theme-secondary)) 100%);
}

.login-card {
	backdrop-filter: blur(10px);
	background-color: rgba(var(--v-theme-surface), 0.95) !important;
}
</style>
