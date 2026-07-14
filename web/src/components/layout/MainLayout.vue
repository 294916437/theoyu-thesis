<template>
	<v-app>
		<!-- 头部栏 -->
		<AppHeader v-if="showHeader" @toggle-drawer="drawerOpen = !drawerOpen" />

		<!-- 侧边栏 -->
		<v-navigation-drawer
			v-if="showDrawer"
			v-model="drawerOpen"
			:temporary="$vuetify.display.mobile"
			:permanent="!$vuetify.display.mobile && showDrawer"
		>
			<AppSidebar />
		</v-navigation-drawer>

		<!-- 主内容区域 -->
		<v-main>
			<router-view v-slot="{ Component }">
				<transition name="fade" mode="out-in">
					<component :is="Component" />
				</transition>
			</router-view>
		</v-main>

		<!-- 全局通知 -->
		<GlobalNotification ref="notificationRef" />
	</v-app>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
import GlobalNotification from '@/components/common/GlobalNotification.vue'
import { setNotificationInstance } from '@/plugins/notification'
import { useTheme } from '@/composables/useTheme'

const route = useRoute()
const drawerOpen = ref(false)
const notificationRef = ref(null)
const { initTheme } = useTheme()

// 根据路由 meta 控制是否显示 Header, 默认显示
const showHeader = computed(() => {
	if (route.meta.showHeader === false) {
		return false
	}
	return route.meta.showHeader !== false
})

// 根据路由 meta 控制是否显示 Siderbar, 默认不显示
const showDrawer = computed(() => {
	return route.meta.showDrawer === true
})

onMounted(() => {
	if (notificationRef.value) {
		setNotificationInstance(notificationRef.value)
	}
	initTheme()
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
	transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
	opacity: 0;
}
</style>
