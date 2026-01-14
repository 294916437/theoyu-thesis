<template>
	<v-menu location="bottom" :close-on-content-click="false">
		<template #activator="{ props }">
			<v-btn v-bind="props" :icon="themeStore.themeIcon" variant="text" :ripple="false" class="theme-toggle-btn">
			</v-btn>
		</template>

		<v-card min-width="240" class="theme-menu">
			<v-list density="compact">
				<v-list-subheader class="d-flex align-center">
					<v-icon icon="mdi-palette" size="small" class="mr-2"></v-icon>
					选择主题
				</v-list-subheader>

				<!-- 浅色模式 -->
				<v-list-item
					:active="!themeStore.followSystem && themeStore.themeName === 'light'"
					class="theme-list-item"
					@click="handleThemeChange('light')"
				>
					<template #prepend>
						<v-icon icon="mdi-weather-sunny" color="amber"></v-icon>
					</template>
					<v-list-item-title>浅色模式</v-list-item-title>
					<template #append>
						<v-icon
							v-if="!themeStore.followSystem && themeStore.themeName === 'light'"
							icon="mdi-check"
							color="primary"
						></v-icon>
					</template>
				</v-list-item>

				<!-- 深色模式 -->
				<v-list-item
					:active="!themeStore.followSystem && themeStore.themeName === 'dark'"
					class="theme-list-item"
					@click="handleThemeChange('dark')"
				>
					<template #prepend>
						<v-icon icon="mdi-weather-night" color="indigo"></v-icon>
					</template>
					<v-list-item-title>深色模式</v-list-item-title>
					<template #append>
						<v-icon
							v-if="!themeStore.followSystem && themeStore.themeName === 'dark'"
							icon="mdi-check"
							color="primary"
						></v-icon>
					</template>
				</v-list-item>

				<v-divider class="my-2"></v-divider>

				<!-- 跟随系统 -->
				<v-list-item :active="themeStore.followSystem" class="theme-list-item" @click="handleFollowSystem">
					<template #prepend>
						<v-icon icon="mdi-theme-light-dark" color="primary"></v-icon>
					</template>
					<v-list-item-title>
						跟随系统
						<span v-if="themeStore.followSystem" class="text-caption text-medium-emphasis">
							(当前: {{ systemThemeLabel }})
						</span>
					</v-list-item-title>
					<template #append>
						<v-icon v-if="themeStore.followSystem" icon="mdi-check" color="primary"></v-icon>
					</template>
				</v-list-item>
			</v-list>

			<!-- 主题预览 -->
			<v-divider></v-divider>
			<v-card-text class="pa-3">
				<div class="text-caption text-medium-emphasis mb-2">当前主题</div>
				<v-chip
					:color="themeStore.isDark ? 'grey-lighten-3' : 'grey-darken-3'"
					:prepend-icon="themeStore.themeIcon"
					size="small"
					label
				>
					{{ themeStore.themeLabel }}
				</v-chip>
			</v-card-text>
		</v-card>
	</v-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

// 使用 Pinia Store
const themeStore = useThemeStore()

// 系统主题标签
const systemThemeLabel = computed(() => {
	const systemTheme = themeStore.getSystemTheme()
	return systemTheme === 'dark' ? '深色' : '浅色'
})

/**
 * 处理主题切换
 * @param {string} theme - 'light' | 'dark'
 */
const handleThemeChange = theme => {
	themeStore.setTheme(theme)
}

/**
 * 处理跟随系统
 */
const handleFollowSystem = () => {
	themeStore.setFollowSystem()
}
</script>

<style scoped>
.theme-toggle-btn {
	transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.theme-toggle-btn:hover {
	transform: rotate(20deg);
}

.theme-menu {
	margin-top: 8px;
}

.theme-list-item {
	cursor: pointer;
	transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.theme-list-item:hover {
	background-color: rgba(var(--v-theme-primary), 0.08);
}

/* 平滑过渡动画 */
:deep(.v-btn__overlay) {
	transition: opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.v-list-item__prepend) {
	transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.theme-list-item:hover :deep(.v-list-item__prepend) {
	transform: scale(1.1);
}
</style>
