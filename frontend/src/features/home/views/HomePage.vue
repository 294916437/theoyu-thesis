<template>
	<v-container fluid class="home-page">
		<AppHeader />

		<v-row class="mt-16">
			<v-col cols="12">
				<WelcomeBanner :user-name="userName" />
			</v-col>
		</v-row>

		<v-row class="mt-8">
			<v-col cols="12" md="8">
				<QuickActions @join-meeting="handleJoinMeeting" @create-meeting="handleCreateMeeting" />
			</v-col>

			<v-col cols="12" md="4">
				<v-card elevation="2" class="pa-4">
					<v-card-title class="text-h6">
						<v-icon left>mdi-calendar-clock</v-icon>
						即将开始的会议
					</v-card-title>
					<v-divider class="my-2"></v-divider>
					<v-card-text>
						<v-list v-if="upcomingMeetings.length > 0">
							<v-list-item
								v-for="meeting in upcomingMeetings"
								:key="meeting.id"
								class="meeting-item"
								@click="goToMeetingDetail(meeting.id)"
							>
								<template #prepend>
									<v-icon>mdi-video</v-icon>
								</template>
								<v-list-item-title>{{ meeting.title }}</v-list-item-title>
								<v-list-item-subtitle>
									{{ formatMeetingTime(meeting.startTime) }}
								</v-list-item-subtitle>
							</v-list-item>
						</v-list>
						<div v-else class="text-center text-grey pa-4">暂无即将开始的会议</div>
					</v-card-text>
				</v-card>
			</v-col>
		</v-row>

		<v-row class="mt-4">
			<v-col cols="12">
				<RecentMeetings :meetings="recentMeetings" @view-detail="goToMeetingDetail" />
			</v-col>
		</v-row>
	</v-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import WelcomeBanner from '../components/WelcomeBanner.vue'
import QuickActions from '../components/QuickActions.vue'
import RecentMeetings from '../components/RecentMeetings.vue'
import { $notify } from '@/plugins/notification'
import { useMeetingApi } from '@/composables/useMeetingApi'
import { formatTime } from '@/utils/formatTime'

const router = useRouter()
const { fetchUpcomingMeetings, fetchRecentMeetings, createMeeting } = useMeetingApi()

const userName = ref('用户')
const upcomingMeetings = ref([])
const recentMeetings = ref([])

const formatMeetingTime = time => {
	return formatTime(time)
}

const handleJoinMeeting = async meetingId => {
	if (!meetingId) {
		$notify.warning('请输入会议ID')
		return
	}
	router.push(`/meeting/${meetingId}`)
}

const handleCreateMeeting = async () => {
	try {
		// 预留API调用位置
		const meeting = await createMeeting({
			title: '即时会议',
			startTime: new Date().toISOString(),
		})
		$notify.success('会议创建成功')
		router.push(`/meeting/${meeting.id}`)
	} catch (error) {
		$notify.error('创建会议失败')
	}
}

const goToMeetingDetail = meetingId => {
	router.push(`/meeting/detail/${meetingId}`)
}

onMounted(async () => {
	try {
		// 预留API调用位置
		upcomingMeetings.value = await fetchUpcomingMeetings()
		recentMeetings.value = await fetchRecentMeetings()
	} catch (error) {
		console.error('Failed to load meetings:', error)
	}
})
</script>

<style scoped>
.home-page {
	min-height: 100vh;
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	padding-bottom: 2rem;
}

.meeting-item {
	cursor: pointer;
	transition: background-color 0.2s;
}

.meeting-item:hover {
	background-color: rgba(0, 0, 0, 0.04);
}
</style>
