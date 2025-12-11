<template>
	<v-app>
		<!-- 页面头部 -->
		<v-app-bar elevation="2" color="surface">
			<v-app-bar-title class="d-flex align-center">
				<v-icon icon="mdi-palette" class="mr-3" color="primary"></v-icon>
				<span class="text-h6 font-weight-bold">主题测试中心</span>
			</v-app-bar-title>

			<v-spacer></v-spacer>

			<!-- 主题信息 -->
			<v-chip :color="isDark ? 'grey-darken-3' : 'grey-lighten-2'" :prepend-icon="themeIcon" class="mr-4" label>
				{{ themeLabel }}
			</v-chip>

			<!-- 主题切换按钮 -->
			<ThemeToggle />
		</v-app-bar>

		<!-- 主内容 -->
		<v-main>
			<v-container fluid class="pa-6">
				<!-- 标签导航 -->
				<v-tabs v-model="currentTab" color="primary" class="mb-6">
					<v-tab value="colors">
						<v-icon start>mdi-palette</v-icon>
						颜色系统
					</v-tab>
					<v-tab value="components">
						<v-icon start>mdi-view-dashboard</v-icon>
						组件展示
					</v-tab>
					<v-tab value="forms">
						<v-icon start>mdi-form-textbox</v-icon>
						表单元素
					</v-tab>
					<v-tab value="layout">
						<v-icon start>mdi-page-layout-body</v-icon>
						布局排版
					</v-tab>
				</v-tabs>

				<!-- 标签内容 -->
				<v-window v-model="currentTab">
					<!-- Tab 1: 颜色系统 -->
					<v-window-item value="colors">
						<ColorsTab />
					</v-window-item>

					<!-- Tab 2: 组件展示 -->
					<v-window-item value="components">
						<ComponentsTab />
					</v-window-item>

					<!-- Tab 3: 表单元素 -->
					<v-window-item value="forms">
						<FormsTab />
					</v-window-item>

					<!-- Tab 4: 布局排版 -->
					<v-window-item value="layout">
						<LayoutTab />
					</v-window-item>
				</v-window>
			</v-container>
		</v-main>

		<!-- 页面底部 -->
		<v-footer color="surface" class="border-t">
			<v-row justify="center" no-gutters>
				<v-col class="text-center" cols="12">
					<span class="text-body-2 text-medium-emphasis"> Thesis Platform © 2025 - 主题系统测试页面 </span>
				</v-col>
			</v-row>
		</v-footer>
	</v-app>
</template>

<script setup>
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useThemeStore } from '@/stores/theme'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import ColorsTab from './ThemeTest/ColorsTab.vue'
import ComponentsTab from './ThemeTest/ComponentsTab.vue'
import FormsTab from './ThemeTest/FormsTab.vue'
import LayoutTab from './ThemeTest/LayoutTab.vue'

const themeStore = useThemeStore()
const { isDark, themeIcon, themeLabel } = storeToRefs(themeStore)

const currentTab = ref('colors')
</script>

<style scoped>
.border-t {
	border-top: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
}
</style>
