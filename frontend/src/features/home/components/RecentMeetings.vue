<template>
	<v-card elevation="0" rounded="xl" class="meeting-card">
		<v-card-title class="d-flex align-center justify-space-between px-6 pt-6">
			<div class="d-flex align-center">
				<v-icon color="primary" size="28" class="mr-3"> mdi-history </v-icon>
				<span class="text-h6 font-weight-bold">最近的会议</span>
			</div>

			<div class="d-flex align-center">
				<v-btn variant="text" size="small" @click="showAll = !showAll">
					{{ showAll ? '收起' : '查看全部' }}
					<v-icon end>{{ showAll ? 'mdi-chevron-up' : 'mdi-chevron-down' }}</v-icon>
				</v-btn>

				<v-btn icon size="small" variant="text" :loading="loading" @click="emit('refresh')">
					<v-icon>mdi-refresh</v-icon>
				</v-btn>
			</div>
		</v-card-title>

		<v-divider class="mx-6 my-4"></v-divider>

		<v-card-text class="px-6 pb-6">
			<!-- 加载骨架屏 -->
			<v-skeleton-loader v-if="loading" type="list-item-avatar-three-line@5"></v-skeleton-loader>

			<!-- 会议列表 -->
			<v-list v-else-if="displayMeetings.length > 0" class="py-0">
				<v-list-item
					v-for="(meeting, index) in displayMeetings"
					:key="meeting.id"
					class="meeting-item"
					rounded="lg"
					@click="emit('view-detail', meeting.id)"
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
</template>

<script setup>
import { computed, ref } from 'vue'
import { useDateFormat } from '@vueuse/core'

const props = defineProps({
	meetings: {
		type: Array,
		default: () => [],
	},
	loading: {
		type: Boolean,
		default: false,
	},
})

const emit = defineEmits(['view-detail', 'refresh'])

const showAll = ref(false)

// 显示的会议列表
const displayMeetings = computed(() => {
	return showAll.value ? props.meetings : props.meetings.slice(0, 5)
})

// 格式化日期时间
const formatDateTime = time => {
	return useDateFormat(time, 'MM-DD HH:mm').value
}

// 获取状态颜色
const getStatusColor = status => {
	const colorMap = {
		completed: 'success',
		ongoing: 'primary',
		cancelled: 'error',
		scheduled: 'warning',
	}
	return colorMap[status] || 'grey'
}

// 获取状态文本
const getStatusText = status => {
	const textMap = {
		completed: '已结束',
		ongoing: '进行中',
		cancelled: '已取消',
		scheduled: '已安排',
	}
	return textMap[status] || '未知'
}

// 获取会议颜色
const getMeetingColor = index => {
	const colors = ['primary', 'secondary', 'accent', 'success', 'info']
	return colors[index % colors.length]
}
</script>

<style scoped>
.meeting-card {
	background: rgb(var(--v-theme-surface));
	border: 1px solid rgba(var(--v-theme-primary), 0.12);
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
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
