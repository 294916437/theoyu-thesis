<template>
	<v-app>
		<router-view />

		<!-- 全局通知 -->
		<GlobalNotification ref="notificationRef" />
	</v-app>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import GlobalNotification from './components/common/GlobalNotification.vue'
import { setNotificationInstance } from '@/plugins/notification'
import { useTheme } from '@/composables/useTheme'

const notificationRef = ref(null)
const { initTheme } = useTheme()

onMounted(() => {
	// 注册通知实例
	if (notificationRef.value) {
		setNotificationInstance(notificationRef.value)
	}
	// 初始化主题（从持久化存储恢复并应用到 Vuetify）
	initTheme()
})
</script>

<style></style>
