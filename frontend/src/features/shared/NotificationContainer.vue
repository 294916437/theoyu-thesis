<template>
	<div class="notification-container">
		<transition-group name="notification">
			<v-snackbar
				v-for="notification in notifications"
				:key="notification.id"
				v-model="notification.visible"
				:color="getColor(notification.type)"
				:timeout="3000"
				location="top right"
				multi-line
			>
				<div class="d-flex align-center">
					<v-icon left>{{ getIcon(notification.type) }}</v-icon>
					{{ notification.message }}
				</div>

				<template #actions>
					<v-btn variant="text" @click="hide(notification.id)"> 关闭 </v-btn>
				</template>
			</v-snackbar>
		</transition-group>
	</div>
</template>

<script setup>
import { useNotification } from '@/composables/useNotification'

const { notifications, hide } = useNotification()

const getColor = type => {
	const colors = {
		success: 'success',
		error: 'error',
		warning: 'warning',
		info: 'info',
	}
	return colors[type] || 'info'
}

const getIcon = type => {
	const icons = {
		success: 'mdi-check-circle',
		error: 'mdi-alert-circle',
		warning: 'mdi-alert',
		info: 'mdi-information',
	}
	return icons[type] || 'mdi-information'
}
</script>

<style scoped>
.notification-container {
	position: fixed;
	top: 80px;
	right: 16px;
	z-index: 10000;
	display: flex;
	flex-direction: column;
	gap: 8px;
}

.notification-enter-active,
.notification-leave-active {
	transition: all 0.3s ease;
}

.notification-enter-from {
	opacity: 0;
	transform: translateX(100%);
}

.notification-leave-to {
	opacity: 0;
	transform: translateX(100%);
}
</style>
