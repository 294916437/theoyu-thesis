import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore(
	'user',
	() => {
		// ========== State ==========
		// 用户Token
		const token = ref('')
		// 主页用户信息
		const profile = ref({})
		// 用户ID
		const userId = ref()

		// ========== Actions ==========
		// 设置用户信息
		const setUserId = newUserId => {
			userId.value = newUserId
		}

		const setProfile = newProfile => {
			profile.value = newProfile
		}

		const setToken = newToken => {
			token.value = newToken
		}

		// 退出登录
		const logout = () => {
			token.value = ''
			// 删除用户信息
			profile.value = {}
			userId.value = undefined
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
		key: 'thesis-user',
		storage: localStorage,
		persist: true,
	},
)
