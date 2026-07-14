<template>
	<button
		:type="type"
		:disabled="disabled"
		:aria-label="ariaLabel"
		:class="buttonClasses"
		@click="handleClick"
	>
		<slot>
			<!-- 默认图标 -->
			<svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
				<path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" />
			</svg>
		</slot>
	</button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
	// 按钮类型
	type: {
		type: String,
		default: 'button',
		validator: value => ['button', 'submit', 'reset'].includes(value),
	},

	// 是否禁用
	disabled: {
		type: Boolean,
		default: false,
	},

	// 无障碍标签
	ariaLabel: {
		type: String,
		default: '',
	},

	// 尺寸
	size: {
		type: String,
		default: 'md',
		validator: value => ['sm', 'md', 'lg'].includes(value),
	},

	// 变体样式
	variant: {
		type: String,
		default: 'default',
		validator: value => ['default', 'primary', 'danger', 'ghost'].includes(value),
	},

	// 是否为圆形
	rounded: {
		type: Boolean,
		default: true,
	},

	// 自定义类名
	customClass: {
		type: String,
		default: '',
	},
})

const emit = defineEmits(['click'])

// 计算按钮类名
const buttonClasses = computed(() => {
	const classes = ['icon-button']

	// 尺寸类
	classes.push(`icon-button--${props.size}`)

	// 变体类
	classes.push(`icon-button--${props.variant}`)

	// 圆形类
	if (props.rounded) {
		classes.push('icon-button--rounded')
	}

	// 禁用类
	if (props.disabled) {
		classes.push('icon-button--disabled')
	}

	// 自定义类
	if (props.customClass) {
		classes.push(props.customClass)
	}

	return classes.join(' ')
})

// 处理点击事件
const handleClick = event => {
	if (!props.disabled) {
		emit('click', event)
	}
}
</script>

<style scoped>
/* 基础样式 */
.icon-button {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	border: none;
	background: transparent;
	cursor: pointer;
	transition: all 0.2s ease;
	color: inherit;
	outline: none;
	position: relative;
}

/* 圆形样式 */
.icon-button--rounded {
	border-radius: 9999px;
}

/* 尺寸变体 */
.icon-button--sm {
	padding: 0.375rem;
}

.icon-button--md {
	padding: 0.5rem;
}

.icon-button--lg {
	padding: 0.625rem;
}

/* 颜色变体 - 默认 */
.icon-button--default:hover:not(.icon-button--disabled) {
	background-color: var(--color-primary-hover, rgba(0, 0, 0, 0.05));
}

.icon-button--default:active:not(.icon-button--disabled) {
	background-color: var(--color-primary-active, rgba(0, 0, 0, 0.1));
	transform: scale(0.95);
}

/* 颜色变体 - 主色调 */
.icon-button--primary {
	color: var(--color-primary-active);
}

.icon-button--primary:hover:not(.icon-button--disabled) {
	background-color: rgba(29, 155, 240, 0.1);
}

.icon-button--primary:active:not(.icon-button--disabled) {
	background-color: rgba(29, 155, 240, 0.2);
	transform: scale(0.95);
}

/* 颜色变体 - 危险 */
.icon-button--danger {
	color: #f91880;
}

.icon-button--danger:hover:not(.icon-button--disabled) {
	background-color: rgba(249, 24, 128, 0.1);
}

.icon-button--danger:active:not(.icon-button--disabled) {
	background-color: rgba(249, 24, 128, 0.2);
	transform: scale(0.95);
}

/* 颜色变体 - 幽灵 */
.icon-button--ghost {
	color: #64748b;
}

.icon-button--ghost:hover:not(.icon-button--disabled) {
	background-color: rgba(100, 116, 139, 0.1);
	color: #0f172a;
}

/* 禁用状态 */
.icon-button--disabled {
	opacity: 0.5;
	cursor: not-allowed;
}

/* 聚焦状态 */
.icon-button:focus-visible {
	outline: 2px solid var(--color-primary-active);
	outline-offset: 2px;
}

/* 图标样式继承 */
.icon-button :deep(svg) {
	display: block;
}
</style>
