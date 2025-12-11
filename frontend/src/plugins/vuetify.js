import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { aliases, mdi } from 'vuetify/iconsets/mdi'
import '@mdi/font/css/materialdesignicons.css'

// 基于图片的配色方案
const lightTheme = {
	dark: false,
	colors: {
		// 主色系
		primary: '#3966A2', // 主品牌色
		'primary-darken-1': '#2E5280',
		'primary-lighten-1': '#5180B8',

		// 次要色系
		secondary: '#6191D3', // 辅助色
		'secondary-darken-1': '#4D7AB5',
		'secondary-lighten-1': '#80A8DC',

		// 强调色
		accent: '#132843', // 强调色
		'accent-lighten-1': '#1F3D5F',
		'accent-lighten-2': '#2E5280',

		// 功能色
		success: '#4CAF50', // 成功
		error: '#F44336', // 错误
		warning: '#FF9800', // 警告
		info: '#2196F3', // 信息

		// 背景和表面
		background: '#F8F6F6', // Color 05 - 页面背景
		surface: '#FFFFFF', // 卡片/组件表面
		'surface-variant': '#D6DEEB', // Color 04 - 次要表面
		'surface-bright': '#FFFFFF',
		'surface-light': '#F5F5F5',

		// 文字颜色
		'on-primary': '#FFFFFF',
		'on-secondary': '#FFFFFF',
		'on-accent': '#FFFFFF',
		'on-background': '#132843',
		'on-surface': '#132843',
		'on-surface-variant': '#3966A2',

		// 边框和分割线
		border: '#E0E0E0',
		divider: '#E0E0E0',

		// 覆盖层
		overlay: 'rgba(19, 40, 67, 0.5)',
	},
	variables: {
		// 边框半径
		'border-radius-root': '8px',
		'border-radius-sm': '4px',
		'border-radius-md': '8px',
		'border-radius-lg': '12px',
		'border-radius-xl': '16px',

		// 阴影
		'shadow-key-umbra-opacity': 0.08,
		'shadow-key-penumbra-opacity': 0.05,
		'shadow-key-ambient-opacity': 0.03,

		// 过渡动画
		'transition-duration': '0.3s',
		'transition-timing-function': 'cubic-bezier(0.4, 0, 0.2, 1)',
	},
}

const darkTheme = {
	dark: true,
	colors: {
		// 主色系 - 在深色模式下稍微调亮
		primary: '#5180B8', // 比浅色模式更亮
		'primary-darken-1': '#3966A2',
		'primary-lighten-1': '#6D9BD0',

		// 次要色系
		secondary: '#80A8DC', // 更亮的辅助色
		'secondary-darken-1': '#6191D3',
		'secondary-lighten-1': '#9CBEE5',

		// 强调色
		accent: '#6191D3', // 深色模式使用更亮的强调色
		'accent-lighten-1': '#80A8DC',
		'accent-lighten-2': '#9CBEE5',

		// 功能色 - 深色模式适配
		success: '#66BB6A',
		error: '#EF5350',
		warning: '#FFA726',
		info: '#42A5F5',

		// 背景和表面 - 使用 Color 01 为基础
		background: '#0A1929', // 更深的背景
		surface: '#132843', // Color 01 - 卡片表面
		'surface-variant': '#1F3D5F', // 稍亮的表面
		'surface-bright': '#2E5280',
		'surface-light': '#1F3D5F',

		// 文字颜色
		'on-primary': '#FFFFFF',
		'on-secondary': '#FFFFFF',
		'on-accent': '#FFFFFF',
		'on-background': '#E0E0E0',
		'on-surface': '#E0E0E0',
		'on-surface-variant': '#B0BEC5',

		// 边框和分割线
		border: '#2E5280',
		divider: '#2E5280',

		// 覆盖层
		overlay: 'rgba(0, 0, 0, 0.7)',
	},
	variables: {
		'border-radius-root': '8px',
		'border-radius-sm': '4px',
		'border-radius-md': '8px',
		'border-radius-lg': '12px',
		'border-radius-xl': '16px',

		'shadow-key-umbra-opacity': 0.15,
		'shadow-key-penumbra-opacity': 0.1,
		'shadow-key-ambient-opacity': 0.08,

		'transition-duration': '0.3s',
		'transition-timing-function': 'cubic-bezier(0.4, 0, 0.2, 1)',
	},
}

export default createVuetify({
	components,
	directives,
	icons: {
		defaultSet: 'mdi',
		aliases,
		sets: {
			mdi,
		},
	},
	theme: {
		defaultTheme: 'light',
		themes: {
			light: lightTheme,
			dark: darkTheme,
		},
		variations: {
			colors: ['primary', 'secondary', 'accent'],
			lighten: 5,
			darken: 5,
		},
	},
	defaults: {
		// 全局组件默认属性
		VBtn: {
			color: 'primary',
			rounded: 'md',
			elevation: 2,
		},
		VCard: {
			elevation: 2,
			rounded: 'lg',
		},
		VTextField: {
			variant: 'outlined',
			color: 'primary',
		},
		VSelect: {
			variant: 'outlined',
			color: 'primary',
		},
		VTextarea: {
			variant: 'outlined',
			color: 'primary',
		},
		VSwitch: {
			color: 'primary',
		},
		VCheckbox: {
			color: 'primary',
		},
		VRadio: {
			color: 'primary',
		},
	},
})
