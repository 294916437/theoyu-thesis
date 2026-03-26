<template>
	<v-container fluid class="home-page pa-0">
		<!-- 顶部英雄区域 -->
		<HeroSection :user-name="userName" @join-meeting="handleJoinMeeting" @create-meeting="openCreateDialog" />

		<v-container class="py-8">
			<v-row>
				<!-- 即将开始的会议 -->
				<v-col cols="12" lg="4" order="2" order-lg="1">
					<v-card elevation="0" class="meeting-card" rounded="xl">
						<v-card-title class="d-flex align-center px-6 pt-6">
							<v-icon color="primary" size="28" class="mr-3"> mdi-calendar-clock </v-icon>
							<span class="text-h6 font-weight-bold">即将开始</span>
						</v-card-title>

						<v-divider class="mx-6 my-4"></v-divider>

						<v-card-text class="px-6 pb-6">
							<v-list v-if="!upcomingLoading && upcomingMeetings.length > 0" class="py-0">
								<v-list-item
									v-for="(meeting, index) in upcomingMeetings"
									:key="meeting.id"
									class="upcoming-item"
									rounded="lg"
									@click="goToMeetingInfo(meeting.roomNo)"
								>
									<template #prepend>
										<v-avatar :color="getMeetingColor(index)" size="44" class="mr-4">
											<v-icon color="white" size="24">mdi-video</v-icon>
										</v-avatar>
									</template>

									<v-list-item-title class="font-weight-medium mb-1">
										{{ meeting.title }}
									</v-list-item-title>

									<v-list-item-subtitle class="d-flex align-center">
										<v-icon size="16" class="mr-1">mdi-clock-outline</v-icon>
										{{ formatTime(meeting.startTime) }}
									</v-list-item-subtitle>

									<template #append>
										<v-chip size="small" variant="flat" color="primary-lighten-1" class="font-weight-medium">
											{{ getTimeUntil(meeting.startTime) }}
										</v-chip>
									</template>
								</v-list-item>
							</v-list>

							<v-skeleton-loader v-else-if="upcomingLoading" type="list-item-avatar-two-line@3"></v-skeleton-loader>

							<div v-else class="text-center py-8">
								<v-icon size="64" color="grey-lighten-2" class="mb-4"> mdi-calendar-blank </v-icon>
								<p class="text-body-2 text-medium-emphasis">暂无即将开始的会议</p>
							</div>
						</v-card-text>
					</v-card>
				</v-col>

				<!-- 最近的会议 -->
				<v-col cols="12" lg="8" order="1" order-lg="2">
					<v-card elevation="0" class="meeting-card" rounded="xl">
						<v-card-title class="d-flex align-center justify-space-between px-6 pt-6">
							<div class="d-flex align-center">
								<v-icon color="primary" size="28" class="mr-3"> mdi-history </v-icon>
								<span class="text-h6 font-weight-bold">最近的会议</span>
							</div>

							<div class="d-flex align-center">
								<v-btn variant="text" size="small" @click="showAllRecent = !showAllRecent">
									{{ showAllRecent ? '收起' : '查看全部' }}
									<v-icon end>{{ showAllRecent ? 'mdi-chevron-up' : 'mdi-chevron-down' }}</v-icon>
								</v-btn>

								<v-btn icon size="small" variant="text" :loading="recentLoading" @click="loadRecentMeetings">
									<v-icon>mdi-refresh</v-icon>
								</v-btn>
							</div>
						</v-card-title>

						<v-divider class="mx-6 my-4"></v-divider>

						<v-card-text class="px-6 pb-6">
							<!-- 加载骨架屏 -->
							<v-skeleton-loader v-if="recentLoading" type="list-item-avatar-three-line@5"></v-skeleton-loader>

							<!-- 会议列表 -->
							<v-list v-else-if="recentMeetings.length > 0" class="py-0">
								<v-list-item
									v-for="(meeting, index) in displayRecentMeetings"
									:key="meeting.roomId"
									class="meeting-item"
									rounded="lg"
									@click="goToMeetingInfo(meeting.roomNo)"
								>
									<template #prepend>
										<v-avatar :color="getMeetingColor(index)" size="56" class="mr-4">
											<v-icon color="white" size="28">mdi-video</v-icon>
										</v-avatar>
									</template>

									<v-list-item-title class="font-weight-bold text-h6 mb-1">
										{{ meeting.title }}
									</v-list-item-title>

									<v-list-item-subtitle class="d-flex align-center mb-1">
										<v-icon size="16" class="mr-1">mdi-clock-outline</v-icon>
										{{ formatDateTime(meeting.startTime) }}
										<v-chip size="small" :color="getStatusColor(meeting.status)" variant="flat" class="ml-3">
											{{ getStatusText(meeting.status) }}
										</v-chip>
									</v-list-item-subtitle>

									<v-list-item-subtitle class="d-flex align-center text-medium-emphasis">
										<v-icon size="16" class="mr-1">mdi-account-multiple</v-icon>
										{{ meeting.participantCount || 0 }} 位参与者
										<span class="mx-2">·</span>
										<v-icon size="16" class="mr-1">mdi-timer-outline</v-icon>
										{{ meeting.duration || 0 }} 分钟
									</v-list-item-subtitle>

									<template #append>
										<v-btn icon="mdi-chevron-right" variant="text" size="small"></v-btn>
									</template>
								</v-list-item>
							</v-list>

							<!-- 空状态 -->
							<div v-else class="text-center py-12">
								<v-icon size="80" color="grey-lighten-2" class="mb-4"> mdi-video-off-outline </v-icon>
								<p class="text-h6 text-medium-emphasis mb-2">暂无会议记录</p>
								<p class="text-body-2 text-medium-emphasis">创建你的第一个会议开始使用吧</p>
							</div>
						</v-card-text>
					</v-card>
				</v-col>
			</v-row>
		</v-container>

		<!-- 创建会议对话框 -->
		<CreateMeetingDialog v-model="showCreateDialog" :user-name="userName" @submit="handleCreateMeeting" />
	</v-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAsyncState, useDateFormat, useNow } from '@vueuse/core'
import HeroSection from '../components/HeroSection.vue'
import CreateMeetingDialog from '../components/CreateMeetingDialog.vue'
import { $notify } from '@/plugins/notification'
import { fetchUpcomingMeetings, fetchRecentMeetings, createMeeting } from '@/api/room'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const now = useNow({ interval: 60000 }) // 每分钟更新一次

// 用户信息
const userStore = useUserStore()
const userName = userStore.profile.nickname

// 创建会议对话框状态
const showCreateDialog = ref(false)

// 最近会议显示状态
const showAllRecent = ref(false)

// 即将开始的会议
const {
	state: upcomingMeetings,
	isLoading: upcomingLoading,
	execute: loadUpcomingMeetings,
} = useAsyncState(
	async () => {
		try {
			const res = await fetchUpcomingMeetings()
			return res?.data || []
		} catch (error) {
			console.error('Failed to load upcoming meetings:', error)
			return []
		}
	},
	[],
	{ immediate: true },
)

// 最近的会议
const {
	state: recentMeetings,
	isLoading: recentLoading,
	execute: loadRecentMeetings,
} = useAsyncState(
	async () => {
		try {
			const res = await fetchRecentMeetings()
			return res?.data || []
		} catch (error) {
			console.error('Failed to load recent meetings:', error)
			return []
		}
	},
	[],
	{ immediate: true },
)

// 显示的最近会议列表
const displayRecentMeetings = computed(() => {
	return showAllRecent.value ? recentMeetings.value : recentMeetings.value.slice(0, 5)
})

// 格式化时间
const formatTime = time => {
	return useDateFormat(time, 'MM-DD HH:mm').value
}

// 格式化日期时间
const formatDateTime = time => {
	return useDateFormat(time, 'MM-DD HH:mm').value
}

// 获取距离会议开始的时间
const getTimeUntil = startTime => {
	const diff = new Date(startTime) - now.value
	const minutes = Math.floor(diff / 60000)
	const hours = Math.floor(minutes / 60)
	const days = Math.floor(hours / 24)

	if (days > 0) return `${days}天后`
	if (hours > 0) return `${hours}小时后`
	if (minutes > 0) return `${minutes}分钟后`
	return '即将开始'
}

// 获取会议颜色
const getMeetingColor = index => {
	const colors = ['primary', 'secondary', 'accent', 'success', 'info']
	return colors[index % colors.length]
}

// 获取状态颜色
const getStatusColor = status => {
	const colorMap = {
		completed: 'success',
		ongoing: 'primary',
		cancelled: 'warning',
		scheduled: 'error',
	}
	return colorMap[status] || 'grey'
}

// 获取状态文本
const getStatusText = status => {
	const textMap = {
		completed: '预约中',
		ongoing: '进行中',
		cancelled: '已结束',
		scheduled: '已取消',
	}
	return textMap[status] || '未知'
}

// 打开创建会议对话框
const openCreateDialog = () => {
	showCreateDialog.value = true
}

// 加入会议(默认先查看会议详情)
const handleJoinMeeting = async roomNo => {
	if (!roomNo || !roomNo.trim()) {
		$notify.warning('请输入会议号D')
		return
	}
	router.push(`/meeting/info/${roomNo.trim()}`)
}

// 创建会议
const handleCreateMeeting = async meetingData => {
	try {
		// 调用后端API创建会议
		const { success, data } = await createMeeting(meetingData)
		// 打印表单数据
		console.log('用户创建会议提交的表单数据:' + meetingData)

		if (success) {
			$notify.success('会议创建成功')
			// 刷新会议列表
			await Promise.all([loadUpcomingMeetings(), loadRecentMeetings()])
			// 根据会议类型决定跳转路径
			if (meetingData.type === 1) {
				// 即时会议，直接进入房间
				router.push(`/meeting/room/${data.roomNo}`)
			} else {
				// 预约会议，跳转到详情页
				router.push(`/meeting/info/${data.roomNo}`)
			}
		} else {
			$notify.error('创建会议失败')
			return
		}
	} catch (error) {
		$notify.error('创建会议失败')
		console.error('Create meeting error:', error)
	}
}

// 查看会议详情
const goToMeetingInfo = roomNo => {
	router.push(`/meeting/info/${roomNo}`)
}
</script>

<style scoped>
.home-page {
	min-height: 100vh;
	background: rgb(var(--v-theme-background));
}

.meeting-card {
	background: rgb(var(--v-theme-surface));
	border: 1px solid rgba(var(--v-theme-primary), 0.12);
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	overflow: hidden;
}

.meeting-card:hover {
	box-shadow: 0 8px 24px rgba(var(--v-theme-primary), 0.15);
	transform: translateY(-2px);
}

.upcoming-item {
	cursor: pointer;
	transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
	padding: 12px;
	margin-right: 4px;
	margin-bottom: 0.75rem;
}

.upcoming-item:hover {
	background: rgba(var(--v-theme-primary), 0.08);
	transform: translateX(4px);
}

.meeting-item {
	cursor: pointer;
	transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
	border: 1px solid transparent;
	padding: 0 1rem;
	margin-right: 4px;
	margin-bottom: 0.75rem;
}

.meeting-item:hover {
	background: rgba(var(--v-theme-primary), 0.08);
	border-color: rgba(var(--v-theme-primary), 0.2);
	transform: translateX(4px);
}
</style>
