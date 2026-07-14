<template>
	<div>
		<!-- 主色系 -->
		<v-card class="mb-6">
			<v-card-title class="d-flex align-center">
				<v-icon icon="mdi-palette-swatch" class="mr-2"></v-icon>
				主色系
			</v-card-title>
			<v-card-text>
				<v-row>
					<v-col v-for="color in primaryColors" :key="color.name" cols="12" sm="6" md="4" lg="3">
						<ColorCard :color="color" />
					</v-col>
				</v-row>
			</v-card-text>
		</v-card>

		<!-- 功能色 -->
		<v-card class="mb-6">
			<v-card-title class="d-flex align-center">
				<v-icon icon="mdi-check-circle" class="mr-2"></v-icon>
				功能色
			</v-card-title>
			<v-card-text>
				<v-row>
					<v-col v-for="color in functionalColors" :key="color.name" cols="12" sm="6" md="4" lg="3">
						<ColorCard :color="color" />
					</v-col>
				</v-row>
			</v-card-text>
		</v-card>

		<!-- 背景和表面 -->
		<v-card class="mb-6">
			<v-card-title class="d-flex align-center">
				<v-icon icon="mdi-layers" class="mr-2"></v-icon>
				背景和表面
			</v-card-title>
			<v-card-text>
				<v-row>
					<v-col v-for="color in surfaceColors" :key="color.name" cols="12" sm="6" md="4" lg="3">
						<ColorCard :color="color" :show-border="true" />
					</v-col>
				</v-row>
			</v-card-text>
		</v-card>

		<!-- 颜色变体 -->
		<v-card>
			<v-card-title class="d-flex align-center">
				<v-icon icon="mdi-gradient-horizontal" class="mr-2"></v-icon>
				颜色变体（Lighten/Darken）
			</v-card-title>
			<v-card-text>
				<v-row>
					<v-col cols="12" md="6">
						<h4 class="mb-3">Primary 变体</h4>
						<div class="d-flex flex-wrap gap-2">
							<v-chip color="primary-lighten-2">Lighten 2</v-chip>
							<v-chip color="primary-lighten-1">Lighten 1</v-chip>
							<v-chip color="primary">Primary</v-chip>
							<v-chip color="primary-darken-1">Darken 1</v-chip>
							<v-chip color="primary-darken-2">Darken 2</v-chip>
						</div>
					</v-col>
					<v-col cols="12" md="6">
						<h4 class="mb-3">Secondary 变体</h4>
						<div class="d-flex flex-wrap gap-2">
							<v-chip color="secondary-lighten-2">Lighten 2</v-chip>
							<v-chip color="secondary-lighten-1">Lighten 1</v-chip>
							<v-chip color="secondary">Secondary</v-chip>
							<v-chip color="secondary-darken-1">Darken 1</v-chip>
							<v-chip color="secondary-darken-2">Darken 2</v-chip>
						</div>
					</v-col>
				</v-row>
			</v-card-text>
		</v-card>
	</div>
</template>

<script setup>
import { computed } from 'vue'
import { useTheme } from '@/composables/useTheme'
import ColorCard from './ColorCard.vue'

const { colors } = useTheme()

const primaryColors = computed(() => [
	{ name: 'Primary', key: 'primary', description: '主品牌色' },
	{ name: 'Secondary', key: 'secondary', description: '辅助色' },
	{ name: 'Accent', key: 'accent', description: '强调色' },
])

const functionalColors = computed(() => [
	{ name: 'Success', key: 'success', description: '成功状态' },
	{ name: 'Error', key: 'error', description: '错误状态' },
	{ name: 'Warning', key: 'warning', description: '警告状态' },
	{ name: 'Info', key: 'info', description: '信息提示' },
])

const surfaceColors = computed(() => [
	{ name: 'Background', key: 'background', description: '页面背景' },
	{ name: 'Surface', key: 'surface', description: '卡片表面' },
	{ name: 'Surface Variant', key: 'surface-variant', description: '次要表面' },
	{ name: 'Surface Bright', key: 'surface-bright', description: '明亮表面' },
])
</script>
