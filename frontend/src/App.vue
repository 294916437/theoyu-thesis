<template>
	<v-app>
		<router-view />

		<!-- 全局通知 -->
		<NotificationContainer />
	</v-app>
</template>

<script setup>
import NotificationContainer from './features/shared/NotificationContainer.vue'
</script>

<style></style>
