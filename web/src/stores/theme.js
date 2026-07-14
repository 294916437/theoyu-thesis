import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useThemeStore = defineStore(
	'theme',
	() => {
		// ==================== State ====================
		const themeName = ref('light')
		const followSystem = ref(false)

		// ==================== Getters ====================
		const isDark = computed(() => themeName.value === 'dark')

		const themeIcon = computed(() => {
			if (followSystem.value) return 'mdi-theme-light-dark'
			return isDark.value ? 'mdi-weather-night' : 'mdi-weather-sunny'
		})

		const themeLabel = computed(() => {
			if (followSystem.value) return '跟随系统'
			return isDark.value ? '深色模式' : '浅色模式'
		})

		// ==================== Actions ====================

		/**
		 * 设置主题
		 * @param {string} name - 'light' | 'dark'
		 * @param {boolean} isSystemTrigger - 是否由系统触发
		 */
		const setTheme = (name, isSystemTrigger = false) => {
			if (name !== 'light' && name !== 'dark') {
				console.warn(`无效的主题名称: ${name}`)
				return
			}

			themeName.value = name

			// 如果不是系统触发，则取消跟随系统
			if (!isSystemTrigger) {
				followSystem.value = false
			}
		}

		/**
		 * 切换主题
		 */
		const toggleTheme = () => {
			const newTheme = isDark.value ? 'light' : 'dark'
			setTheme(newTheme)
		}

		/**
		 * 设置跟随系统主题
		 */
		const setFollowSystem = () => {
			followSystem.value = true

			// 立即应用系统主题
			const systemTheme = getSystemTheme()
			themeName.value = systemTheme
		}

		/**
		 * 获取系统主题偏好
		 * @returns {string} 'light' | 'dark'
		 */
		const getSystemTheme = () => {
			const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
			return prefersDark ? 'dark' : 'light'
		}

		/**
		 * 重置主题到默认值
		 */
		const resetTheme = () => {
			themeName.value = 'light'
			followSystem.value = false
		}

		// ==================== Return ====================
		return {
			// State
			themeName,
			followSystem,

			// Getters
			isDark,
			themeIcon,
			themeLabel,

			// Actions
			setTheme,
			toggleTheme,
			setFollowSystem,
			getSystemTheme,
			resetTheme,
		}
	},
	{
		// 持久化配置
		persist: {
			key: 'theme',
			storage: localStorage,
			paths: ['themeName', 'followSystem'],
		},
	},
)
