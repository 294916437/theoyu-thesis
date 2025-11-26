import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore(
	'user',
	() => {
		// ========== State ==========
		const token = ref('')
		// 主页用户信息
		const profile = ref({})
		// ========== Getters ==========
		// 用户ID
		const userId = ref()

		// ========== Actions ==========
		// 设置用户信息
		const setUserId = newUserId => {
			userId.value = newUserId
		}

		const setProfile = newProfile => {
			profile.value = newProfile
			console.log('用户Profile:', profile.value)
		}

		const setToken = newToken => {
			token.value = newToken
		}

		// 退出登录
		const logout = () => {
			token.value = ''
			// 删除用户信息
			profile.value = {}
		}

		return {
			userId,
			token,
			profile,
			setProfile,
			setUserId,
			setToken,
			logout,
		}
	},
	{
		// 开启持久化
		key: 'oursphere-user',
		storage: localStorage,
		persist: true,
	},
)
