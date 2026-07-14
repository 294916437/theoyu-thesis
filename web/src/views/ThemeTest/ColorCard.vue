<template>
	<v-card
		:color="color.key"
		class="color-card"
		:class="{ 'with-border': showBorder }"
		elevation="3"
		@click="copyColor"
	>
		<v-card-text class="pa-4">
			<div class="color-info">
				<div class="color-name">{{ color.name }}</div>
				<div class="color-description">{{ color.description }}</div>
				<div class="color-value">{{ colorValue }}</div>
				<div class="color-rgb">{{ colorRGB }}</div>
			</div>
		</v-card-text>

		<v-tooltip activator="parent" location="top"> 点击复制颜色值 </v-tooltip>
	</v-card>
</template>

<script setup>
import { computed } from 'vue'
import { useTheme } from '@/composables/useTheme'
import { $notify } from '@/plugins/notification'

const props = defineProps({
	color: {
		type: Object,
		required: true,
	},
	showBorder: {
		type: Boolean,
		default: false,
	},
})

const { getColor, getColorRGB } = useTheme()

const colorValue = computed(() => getColor(props.color.key))

const colorRGB = computed(() => {
	const rgb = getColorRGB(props.color.key)
	return `RGB(${rgb.r}, ${rgb.g}, ${rgb.b})`
})

const copyColor = async () => {
	try {
		await navigator.clipboard.writeText(colorValue.value)
		$notify.success(`已复制: ${colorValue.value}`, {
			timeout: 1500,
		})
	} catch (error) {
		console.error('复制失败:', error)
	}
}
</script>

<style scoped>
.color-card {
	min-height: 140px;
	cursor: pointer;
	transition:
		transform 0.2s,
		box-shadow 0.2s;
}

.color-card:hover {
	transform: translateY(-4px);
	box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2) !important;
}

.color-card.with-border {
	border: 2px solid rgba(var(--v-border-color), 0.3);
}

.color-info {
	text-align: center;
	color: white;
	text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.color-name {
	font-size: 18px;
	font-weight: bold;
	margin-bottom: 4px;
}

.color-description {
	font-size: 13px;
	opacity: 0.9;
	margin-bottom: 8px;
}

.color-value {
	font-family: 'Courier New', monospace;
	font-size: 14px;
	font-weight: bold;
	margin-top: 8px;
}

.color-rgb {
	font-family: 'Courier New', monospace;
	font-size: 12px;
	opacity: 0.8;
	margin-top: 4px;
}
</style>
