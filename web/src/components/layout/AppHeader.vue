<template>
	<v-app-bar :elevation="2" class="app-header">
		<v-app-bar-nav-icon v-if="$vuetify.display.mobile" @click="emit('toggle-drawer')"></v-app-bar-nav-icon>

		<!-- Logo -->
		<v-toolbar-title class="d-flex align-center" @click="router.push('/')">
			<span class="logo-text">蔚蓝天空</span>
		</v-toolbar-title>

		<v-spacer></v-spacer>

		<!-- 主题切换 -->
		<ThemeToggle />

		<!-- 私聊页面入口 -->
		<v-btn icon class="mr-2" @click="router.push('/chat')">
			<v-icon>mdi-chat</v-icon>
		</v-btn>

		<!-- 通知 -->
		<v-menu offset-y>
			<template #activator="{ props }">
				<v-btn icon v-bind="props" class="mr-2">
					<v-badge :content="unreadNotifications" :model-value="unreadNotifications > 0" color="error" overlap class="theme-badge">
						<v-icon>mdi-bell</v-icon>
					</v-badge>
				</v-btn>
			</template>

			<v-card max-width="400" max-height="500" class="notification-card">
				<v-card-title class="d-flex align-center justify-space-between">
					<span>通知</span>
					<v-btn v-if="notifications.length > 0" variant="text" size="small" @click="markAllAsRead"> 全部已读 </v-btn>
				</v-card-title>

				<v-divider></v-divider>

				<v-list v-if="notifications.length > 0" class="notification-list">
					<v-list-item
						v-for="notification in notifications"
						:key="notification.id"
						:class="{ unread: !notification.read }"
						@click="handleNotificationClick(notification)"
					>
						<template #prepend>
							<v-avatar :color="getNotificationColor(notification.type)">
								<v-icon color="white">{{ getNotificationIcon(notification.type) }}</v-icon>
							</v-avatar>
						</template>

						<v-list-item-title>{{ notification.title }}</v-list-item-title>
						<v-list-item-subtitle>
							{{ notification.message }}
						</v-list-item-subtitle>
						<v-list-item-subtitle class="text-caption">
							{{ formatTime(notification.time) }}
						</v-list-item-subtitle>
					</v-list-item>
				</v-list>

				<v-card-text v-else class="text-center text-grey py-8">
					<v-icon size="48" color="grey-lighten-2" class="mb-2">mdi-bell-off</v-icon>
					<div>暂无通知</div>
				</v-card-text>
			</v-card>
		</v-menu>

		<!-- 用户菜单 -->
		<v-menu offset-y>
			<template #activator="{ props }">
				<v-btn v-bind="props" variant="text" class="user-menu-btn">
					<v-avatar size="32" color="primary">
						<v-img v-if="userStore.profile.avatar" :src="userStore.profile.avatar"></v-img>
						<v-img v-else src="@/assets/image/default-avatar.png"></v-img>
					</v-avatar>
					<span v-if="!$vuetify.display.mobile" class="ml-2">{{ userStore.profile.nickname }}</span>
					<v-icon v-if="!$vuetify.display.mobile" right>mdi-chevron-down</v-icon>
				</v-btn>
			</template>

			<v-list>
				<v-list-item @click="goToProfile">
					<template #prepend>
						<v-icon>mdi-account</v-icon>
					</template>
					<v-list-item-title>个人资料</v-list-item-title>
				</v-list-item>

				<v-list-item @click="goToSettings">
					<template #prepend>
						<v-icon>mdi-cog</v-icon>
					</template>
					<v-list-item-title>设置</v-list-item-title>
				</v-list-item>

				<v-divider></v-divider>

				<v-list-item @click="handleLogout">
					<template #prepend>
						<v-icon color="error">mdi-logout</v-icon>
					</template>
					<v-list-item-title class="text-error">退出登录</v-list-item-title>
				</v-list-item>
			</v-list>
		</v-menu>
	</v-app-bar>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTimeAgo } from '@vueuse/core'
import { useUserStore } from '@/stores/user'
import { $notify } from '@/plugins/notification'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { logout } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const emit = defineEmits(['toggle-drawer', 'search'])

const notifications = ref([
	{
		id: 1,
		type: 'meeting',
		title: '会议提醒',
		message: '您的会议将在15分钟后开始',
		time: new Date(Date.now() - 600000),
		read: false,
	},
	{
		id: 2,
		type: 'message',
		title: '新消息',
		message: '李四在会议中@了你',
		time: new Date(Date.now() - 3600000),
		read: false,
	},
])

const unreadNotifications = computed(() => {
	return notifications.value.filter(n => !n.read).length
})

const formatTime = time => {
	return useTimeAgo(time).value
}

const getNotificationColor = type => {
	const colors = {
		meeting: 'primary',
		message: 'success',
		warning: 'warning',
		error: 'error',
	}
	return colors[type] || 'grey'
}

const getNotificationIcon = type => {
	const icons = {
		meeting: 'mdi-video',
		message: 'mdi-message',
		warning: 'mdi-alert',
		error: 'mdi-alert-circle',
	}
	return icons[type] || 'mdi-bell'
}

const handleNotificationClick = notification => {
	notification.read = true
	// 根据通知类型跳转
	if (notification.type === 'meeting') {
		router.push(`/meeting/${notification.meetingId}`)
	}
}

const markAllAsRead = () => {
	notifications.value.forEach(n => (n.read = true))
	$notify.success('所有通知已标记为已读')
}

const goToProfile = () => {
	router.push('/user/profile')
}

const goToSettings = () => {
	router.push('/user/settings')
}

const handleLogout = async () => {
	const res = await logout()
	if (res.success) {
		userStore.logout()
		// 刷新页面
		router.push('/login')
		window.location.reload()
	}
}
</script>

<style scoped>
.app-header {
	background: rgb(var(--v-theme-surface));
	border-bottom: 1px solid rgb(var(--v-theme-border));
	transition:
		background-color 0.3s ease,
		border-color 0.3s ease;
}

.title-text {
	-webkit-background-clip: text;
	-webkit-text-fill-color: transparent;
	background-clip: text;
	font-weight: bold;
	font-size: large;
	transition: background-position 0.5s ease;
	cursor: pointer;
}
.logo-text {
	background: linear-gradient(135deg, rgb(var(--v-theme-secondary)) 0%, rgb(var(--v-theme-primary)) 50%, rgb(var(--v-theme-primary-darken-1)) 100%);
	/* 文字裁剪效果 */
	-webkit-text-fill-color: transparent;
	background-clip: text;

	font-weight: bold;
	font-size: 1.25rem; /* 或 large */

	background-position: 0 0;
	transition: background-position 0.5s cubic-bezier(0.4, 0, 0.2, 1);

	cursor: pointer;
}

.v-theme--dark .logo-text {
	background: linear-gradient(135deg, rgb(var(--v-theme-secondary)) 0%, rgb(var(--v-theme-primary)) 50%, rgb(var(--v-theme-primary-lighten-1)) 100%);
	-webkit-background-clip: text;
	-webkit-text-fill-color: transparent;
	background-clip: text;
}
.logo-text:hover {
	background-position: 100% 0;
}

.notification-list {
	max-height: 400px;
	overflow-y: auto;
}
.notification-list .v-list-item.unread {
	background-color: rgba(var(--v-theme-primary), 0.05);
}
.notification-card {
	background: rgb(var(--v-theme-surface));
	color: rgb(var(--v-theme-on-surface));
}

.notification-card :deep(.v-icon) {
	color: rgb(var(--v-theme-on-surface-variant));
}

.v-theme--dark .notification-list .v-list-item.unread {
	background-color: rgba(var(--v-theme-primary), 0.15);
}

.user-menu-btn {
	text-transform: none;
	color: rgb(var(--v-theme-on-surface)) !important;
}

.app-header :deep(.v-btn) {
	color: rgb(var(--v-theme-on-surface));
}

.app-header :deep(.v-toolbar-title) {
	color: rgb(var(--v-theme-on-surface));
}
.app-header :deep(.v-btn--icon) {
	color: rgb(var(--v-theme-on-surface));
}

.v-theme--dark .app-header :deep(.v-badge__badge) {
	background: rgb(var(--v-theme-error)) !important;
	color: rgb(var(--v-theme-on-error));
}

.v-theme--dark :deep(.v-list-item:hover) {
	background: rgba(var(--v-theme-primary), 0.1);
}

.app-header :deep(.v-divider) {
	border-color: rgb(var(--v-theme-divider));
}
.theme-badge :deep(.v-badge__badge) {
	font-weight: 600;
	border: 2px solid rgb(var(--v-theme-surface));
}
</style>
