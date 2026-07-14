import { computed, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useThemeStore } from '@/stores/theme'
import { useTheme as useVuetifyTheme } from 'vuetify'

/**
 * 主题切换组合式函数
 * 这是 useThemeStore 的轻量级封装，提供更简洁的 API
 * 并负责与 Vuetify 主题系统的集成
 * @returns {Object} 主题相关的属性和方法
 */
export function useTheme() {
	const themeStore = useThemeStore()
	const vuetifyTheme = useVuetifyTheme()

	// 使用 storeToRefs 保持响应性
	const { themeName, isDark, followSystem, themeIcon, themeLabel } = storeToRefs(themeStore)

	// 当前主题的颜色对象（来自 Vuetify）
	const colors = computed(() => vuetifyTheme.global.current.value.colors)

	// 当前主题对象
	const currentTheme = computed(() => vuetifyTheme.global.current.value)

	// ==================== Vuetify 集成 ====================

	/**
	 * 应用主题到 Vuetify
	 * 使用最新的 Vuetify 3 API
	 */
	const applyToVuetify = () => {
		if (vuetifyTheme?.global) {
			// 使用推荐的方式设置主题
			vuetifyTheme.change(themeName.value)
		}
	}

	/**
	 * 初始化主题
	 * 从持久化存储恢复主题并应用
	 */
	const initTheme = () => {
		// 如果设置了跟随系统
		if (followSystem.value) {
			const systemTheme = themeStore.getSystemTheme()
			themeStore.setTheme(systemTheme, true)
		}

		// 立即应用主题到 Vuetify
		applyToVuetify()

		// 设置系统主题监听
		setupSystemThemeListener()
	}

	/**
	 * 监听系统主题变化
	 */
	const setupSystemThemeListener = () => {
		const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')

		const handleChange = e => {
			// 只有在跟随系统时才自动切换
			if (followSystem.value) {
				const newTheme = e.matches ? 'dark' : 'light'
				themeStore.setTheme(newTheme, true)
			}
		}

		// 添加监听器
		mediaQuery.addEventListener('change', handleChange)

		// 返回清理函数（可选）
		return () => {
			mediaQuery.removeEventListener('change', handleChange)
		}
	}

	// ==================== 响应式同步 ====================

	// 监听 Store 中的主题变化，自动同步到 Vuetify
	watch(
		themeName,
		newTheme => {
			if (vuetifyTheme?.global) {
				vuetifyTheme.change(newTheme)
			}
		},
		{ immediate: true }, // 立即执行一次
	)

	// ==================== 颜色辅助方法 ====================

	/**
	 * 获取指定颜色
	 * @param {string} colorName - 颜色名称
	 * @returns {string} 颜色值
	 */
	const getColor = colorName => {
		return colors.value[colorName] || ''
	}

	/**
	 * 获取颜色的 RGB 值
	 * @param {string} colorName - 颜色名称
	 * @returns {Object} { r, g, b }
	 */
	const getColorRGB = colorName => {
		const color = getColor(colorName)
		if (!color) return { r: 0, g: 0, b: 0 }

		// Hex 转 RGB
		const hex = color.replace('#', '')
		const bigint = parseInt(hex, 16)
		return {
			r: (bigint >> 16) & 255,
			g: (bigint >> 8) & 255,
			b: bigint & 255,
		}
	}

	/**
	 * 获取颜色的 RGBA 字符串
	 * @param {string} colorName - 颜色名称
	 * @param {number} alpha - 透明度 (0-1)
	 * @returns {string} rgba 字符串
	 */
	const getColorRGBA = (colorName, alpha = 1) => {
		const { r, g, b } = getColorRGB(colorName)
		return `rgba(${r}, ${g}, ${b}, ${alpha})`
	}

	/**
	 * 检查是否为浅色主题
	 * @returns {boolean}
	 */
	const isLight = computed(() => !isDark.value)

	// ==================== Return ====================

	return {
		// Store 的响应式引用
		themeName,
		isDark,
		isLight,
		followSystem,
		themeIcon,
		themeLabel,

		// Vuetify 主题
		colors,
		currentTheme,

		// Store 方法（直接暴露）
		setTheme: themeStore.setTheme,
		toggleTheme: themeStore.toggleTheme,
		setFollowSystem: themeStore.setFollowSystem,
		getSystemTheme: themeStore.getSystemTheme,
		resetTheme: themeStore.resetTheme,

		// 初始化方法
		initTheme,

		// 辅助方法
		getColor,
		getColorRGB,
		getColorRGBA,

		// 原始 Store（供高级用法）
		themeStore,
	}
}
