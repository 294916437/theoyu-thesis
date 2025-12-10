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

const notificationRef = ref(null)

onMounted(() => {
	// 注册通知实例
	if (notificationRef.value) {
		setNotificationInstance(notificationRef.value)
	}
})
</script>

<style></style>
