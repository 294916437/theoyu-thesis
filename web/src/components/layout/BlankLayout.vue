<template>
	<v-app>
		<v-main>
			<router-view />
		</v-main>

		<!-- 全局通知（即使在空白布局也需要） -->
		<GlobalNotification ref="notificationRef" />
	</v-app>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import GlobalNotification from '@/components/common/GlobalNotification.vue'
import { setNotificationInstance } from '@/plugins/notification'
import { useTheme } from '@/composables/useTheme'

const notificationRef = ref(null)
const { initTheme } = useTheme()

onMounted(() => {
	if (notificationRef.value) {
		setNotificationInstance(notificationRef.value)
	}
	initTheme()
})
</script>
