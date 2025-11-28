<template>
	<v-card elevation="2">
		<v-card-title class="d-flex align-center justify-space-between">
			<span class="text-h6">
				<v-icon left>mdi-history</v-icon>
				最近的会议
			</span>
			<v-btn variant="text" size="small" @click="showAll = !showAll">
				{{ showAll ? '收起' : '查看全部' }}
			</v-btn>
		</v-card-title>

		<v-divider></v-divider>

		<v-card-text v-if="displayMeetings.length > 0">
			<v-list>
				<v-list-item
					v-for="meeting in displayMeetings"
					:key="meeting.id"
					class="meeting-list-item"
					@click="emit('view-detail', meeting.id)"
				>
					<template v-slot:prepend>
						<v-avatar :color="meeting.color || 'primary'" size="48">
							<v-icon color="white">mdi-video</v-icon>
						</v-avatar>
					</template>

					<v-list-item-title class="font-weight-medium">
						{{ meeting.title }}
					</v-list-item-title>

					<v-list-item-subtitle>
						<v-icon size="small" class="mr-1">mdi-clock-outline</v-icon>
						{{ formatDateTime(meeting.startTime) }}
						<v-chip size="small" :color="getStatusColor(meeting.status)" class="ml-2">
							{{ getStatusText(meeting.status) }}
						</v-chip>
					</v-list-item-subtitle>

					<v-list-item-subtitle class="mt-1">
						<v-icon size="small" class="mr-1">mdi-account-multiple</v-icon>
						{{ meeting.participantCount || 0 }} 位参与者
						<span class="mx-2">·</span>
						<v-icon size="small" class="mr-1">mdi-timer-outline</v-icon>
						{{ meeting.duration || 0 }} 分钟
					</v-list-item-subtitle>

					<template v-slot:append>
						<v-btn icon="mdi-chevron-right" variant="text" size="small"></v-btn>
					</template>
				</v-list-item>
			</v-list>
		</v-card-text>

		<v-card-text v-else class="text-center text-grey pa-8">
			<v-icon size="64" color="grey-lighten-2" class="mb-4"> mdi-video-off </v-icon>
			<p>暂无会议记录</p>
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
})

const emit = defineEmits(['view-detail'])

const showAll = ref(false)

const displayMeetings = computed(() => {
	return showAll.value ? props.meetings : props.meetings.slice(0, 5)
})

const formatDateTime = time => {
	return useDateFormat(time, 'MM-DD HH:mm').value
}

const getStatusColor = status => {
	const colors = {
		completed: 'success',
		ongoing: 'primary',
		cancelled: 'error',
		scheduled: 'warning',
	}
	return colors[status] || 'grey'
}

const getStatusText = status => {
	const texts = {
		completed: '已结束',
		ongoing: '进行中',
		cancelled: '已取消',
		scheduled: '已安排',
	}
	return texts[status] || '未知'
}
</script>

<style scoped>
.meeting-list-item {
	cursor: pointer;
	transition: background-color 0.2s;
	border-radius: 8px;
	margin-bottom: 8px;
}

.meeting-list-item:hover {
	background-color: rgba(0, 0, 0, 0.04);
}
</style>
