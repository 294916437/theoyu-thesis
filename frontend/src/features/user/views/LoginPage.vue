<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import PhoneInput from '../components/PhoneInput.vue'
import VerificationCodeInput from '../components/VerificationCodeInput.vue'
import { $notify } from '@/plugins/notification'
import { getVerificationCode, login } from '@/api/auth'
import { getUserProfile } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// 表单数据
const phone = ref('')
const code = ref('')
const phoneError = ref('')
const codeError = ref('')
const loading = ref(false)
const agreeTerms = ref(false)

// 倒计时
const countdown = ref(0)
let timer = null

// 手机号验证
const isPhoneValid = computed(() => {
	const phoneRegex = /^1[3-9]\d{9}$/
	return phoneRegex.test(phone.value)
})

// 是否可以提交
const canSubmit = computed(() => {
	return isPhoneValid.value && code.value.length === 6 && agreeTerms.value
})

// 发送验证码
const handleSendCode = async () => {
	// 清除错误信息
	phoneError.value = ''

	// 验证手机号
	if (!isPhoneValid.value) {
		phoneError.value = '请输入正确的手机号'
		$notify.warning('请输入正确的手机号')
		throw new Error('手机号格式不正确')
	}

	// 检查倒计时
	if (countdown.value > 0) {
		throw new Error('请稍后再试')
	}

	try {
		const response = await getVerificationCode(phone.value)

		if (!response.success) {
			$notify.info(response.notification || '发送验证码失败')
			throw new Error('发送失败')
		}

		$notify.success('验证码已发送')

		// 开始倒计时
		countdown.value = 180 // 3分钟
		timer = setInterval(() => {
			countdown.value--
			if (countdown.value <= 0) {
				clearInterval(timer)
				timer = null
			}
		}, 1000)
	} catch (error) {
		$notify.error(error.message || '发送验证码失败，请重试')
		throw error
	}
}

// 登录处理
const handleLogin = async () => {
	// 清除错误信息
	phoneError.value = ''
	codeError.value = ''

	// 检查是否同意协议
	if (!agreeTerms.value) {
		$notify.warning('请先同意用户协议和隐私政策')
		return
	}

	// 验证手机号
	if (!isPhoneValid.value) {
		phoneError.value = '请输入正确的手机号'
		$notify.warning('请输入正确的手机号')
		return
	}

	// 验证验证码
	if (code.value.length !== 6) {
		codeError.value = '请输入6位验证码'
		$notify.warning('请输入正确的验证码')
		return
	}

	loading.value = true

	try {
		// 调用登录接口
		const response = await login({
			phone: phone.value,
			code: code.value,
			type: 1, // 验证码登录类型
		})

		if (!response.success) {
			$notify.error('验证码错误')
			return
		}

		// 存储 token
		userStore.setToken(response.data.token)
		const userId = response.data.userId

		// 初始化用户ID
		userStore.setUserId(userId)

		// 获取用户信息
		try {
			const profileRes = await getUserProfile(userId)
			if (profileRes.success) {
				userStore.setProfile(profileRes.data)
			}
		} catch (error) {
			console.error('获取用户信息失败:', error)
		}

		$notify.success('登录成功')

		// 跳转到首页
		setTimeout(() => {
			router.push('/')
		}, 500)
	} catch (error) {
		console.error('登录失败:', error)
		$notify.error(error.message || '登录失败，请重试')
	} finally {
		loading.value = false
	}
}

// 组件卸载时清除定时器
onUnmounted(() => {
	if (timer) {
		clearInterval(timer)
		timer = null
	}
})
</script>

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
							:countdown="countdown"
							class="mb-4"
							@send="handleSendCode"
						/>

						<!-- 协议勾选 -->
						<div class="mb-6">
							<v-checkbox v-model="agreeTerms" density="compact" hide-details>
								<template #label>
									<span class="text-caption">
										我已阅读并同意
										<a href="#" class="text-primary" @click.prevent>《用户协议》</a>
										和
										<a href="#" class="text-primary" @click.prevent>《隐私政策》</a>
									</span>
								</template>
							</v-checkbox>
						</div>

						<!-- 登录按钮 -->
						<v-btn
							block
							size="large"
							color="primary"
							:loading="loading"
							:disabled="!canSubmit"
							class="mb-4"
							@click="handleLogin"
						>
							登录
						</v-btn>

						<!-- 提示文字 -->
						<div class="text-center text-caption text-medium-emphasis">新用户可直接登录</div>
					</v-card-text>
				</v-card>
			</v-col>
		</v-row>
	</v-container>
</template>

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
