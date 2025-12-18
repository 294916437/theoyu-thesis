<template>
	<v-container class="meeting-detail-page">
		<v-row>
			<v-col cols="12">
				<v-btn variant="text" prepend-icon="mdi-arrow-left" @click="goBack"> 返回 </v-btn>
			</v-col>
		</v-row>

		<v-row>
			<!-- 左侧详情 -->
			<v-col cols="12" md="8">
				<v-card elevation="2" class="mb-4">
					<v-card-title class="d-flex align-center justify-space-between">
						<div class="d-flex align-center">
							<v-icon left color="primary" size="large">mdi-video</v-icon>
							<h2 class="text-h5">{{ meetingDetail.title }}</h2>
						</div>

						<v-chip :color="getStatusColor(meetingDetail.status)" variant="elevated">
							<v-icon left size="small">{{ getStatusIcon(meetingDetail.status) }}</v-icon>
							{{ getStatusText(meetingDetail.status) }}
						</v-chip>
					</v-card-title>

					<v-divider></v-divider>

					<v-card-text class="pa-6">
						<!-- 会议信息 -->
						<v-row>
							<v-col cols="12" md="6">
								<div class="info-item">
									<v-icon left color="grey-darken-1">mdi-calendar</v-icon>
									<div>
										<div class="text-caption text-grey">开始时间</div>
										<div class="text-body-1">{{ formatDateTime(meetingDetail.startTime) }}</div>
									</div>
								</div>
							</v-col>

							<v-col cols="12" md="6">
								<div class="info-item">
									<v-icon left color="grey-darken-1">mdi-clock-outline</v-icon>
									<div>
										<div class="text-caption text-grey">持续时间</div>
										<div class="text-body-1">{{ meetingDetail.duration }} 分钟</div>
									</div>
								</div>
							</v-col>

							<v-col cols="12" md="6">
								<div class="info-item">
									<v-icon left color="grey-darken-1">mdi-account</v-icon>
									<div>
										<div class="text-caption text-grey">主持人</div>
										<div class="text-body-1">{{ meetingDetail.host?.name }}</div>
									</div>
								</div>
							</v-col>

							<v-col cols="12" md="6">
								<div class="info-item">
									<v-icon left color="grey-darken-1">mdi-account-multiple</v-icon>
									<div>
										<div class="text-caption text-grey">参与人数</div>
										<div class="text-body-1">{{ meetingDetail.participantCount }} 人</div>
									</div>
								</div>
							</v-col>

							<v-col v-if="meetingDetail.description" cols="12">
								<div class="info-item">
									<v-icon left color="grey-darken-1">mdi-text</v-icon>
									<div>
										<div class="text-caption text-grey">会议描述</div>
										<div class="text-body-1 mt-2">{{ meetingDetail.description }}</div>
									</div>
								</div>
							</v-col>
						</v-row>

						<!-- 会议链接 -->
						<v-card variant="outlined" class="mt-4" color="blue-lighten-5">
							<v-card-text>
								<div class="text-subtitle-2 mb-2">加入链接</div>
								<v-text-field
									:model-value="meetingLink"
									readonly
									variant="outlined"
									density="compact"
									hide-details
								>
									<template #append-inner>
										<v-btn
											icon="mdi-content-copy"
											size="small"
											variant="text"
											@click="copyLink"
										></v-btn>
									</template>
								</v-text-field>
							</v-card-text>
						</v-card>

						<!-- 操作按钮 -->
						<div class="d-flex gap-2 mt-6 align-center">
							<v-btn
								v-if="canJoinMeeting"
								color="primary"
								size="large"
								prepend-icon="mdi-video"
								@click="joinMeeting"
							>
								加入会议
							</v-btn>
							<v-spacer></v-spacer>

							<v-btn
								v-if="canEditMeeting"
								variant="outlined"
								prepend-icon="mdi-pencil"
								@click="editMeeting"
							>
								编辑
							</v-btn>

							<v-btn variant="outlined" prepend-icon="mdi-share-variant" @click="shareMeeting">
								分享
							</v-btn>

							<v-menu>
								<template #activator="{ props }">
									<v-btn icon="mdi-dots-vertical" variant="text" v-bind="props"></v-btn>
								</template>
								<v-list>
									<v-list-item v-if="hasRecording" @click="downloadRecording">
										<template #prepend>
											<v-icon>mdi-download</v-icon>
										</template>
										<v-list-item-title>下载录像</v-list-item-title>
									</v-list-item>
									<v-list-item v-if="hasTranscript" @click="exportTranscript">
										<template #prepend>
											<v-icon>mdi-file-document</v-icon>
										</template>
										<v-list-item-title>导出记录</v-list-item-title>
									</v-list-item>
									<v-divider></v-divider>
									<v-list-item v-if="canDeleteMeeting" class="text-error" @click="confirmDelete">
										<template #prepend>
											<v-icon color="error">mdi-delete</v-icon>
										</template>
										<v-list-item-title>删除会议</v-list-item-title>
									</v-list-item>
								</v-list>
							</v-menu>
						</div>
					</v-card-text>
				</v-card>

				<!-- 参与者列表 -->
				<v-card elevation="2">
					<v-card-title>
						<v-icon left>mdi-account-multiple</v-icon>
						参与者 ({{ participants.length }})
					</v-card-title>
					<v-divider></v-divider>
					<v-card-text>
						<v-list>
							<v-list-item v-for="participant in participants" :key="participant.id">
								<template #prepend>
									<v-avatar :color="participant.avatarColor || 'primary'">
										<v-img v-if="participant.avatar" :src="participant.avatar"></v-img>
										<span v-else class="text-white">
											{{ getInitials(participant.name) }}
										</span>
									</v-avatar>
								</template>

								<v-list-item-title>
									{{ participant.name }}
									<v-chip v-if="participant.isHost" size="x-small" color="warning" class="ml-2">
										主持人
									</v-chip>
								</v-list-item-title>

								<v-list-item-subtitle>
									{{ participant.email }}
								</v-list-item-subtitle>

								<template #append>
									<div class="text-caption text-grey">
										{{ formatJoinTime(participant.joinTime) }}
									</div>
								</template>
							</v-list-item>
						</v-list>
					</v-card-text>
				</v-card>
			</v-col>

			<!-- 右侧信息 -->
			<v-col cols="12" md="4">
				<!-- 会议统计 -->
				<v-card elevation="2" class="mb-4">
					<v-card-title>会议统计</v-card-title>
					<v-divider></v-divider>
					<v-card-text>
						<MeetingStatistics :statistics="meetingStatistics" />
					</v-card-text>
				</v-card>

				<!-- 录像与记录 -->
				<v-card v-if="hasRecording || hasTranscript" elevation="2" class="mb-4">
					<v-card-title>录像与记录</v-card-title>
					<v-divider></v-divider>
					<v-list>
						<v-list-item v-if="hasRecording" @click="playRecording">
							<template #prepend>
								<v-icon color="primary">mdi-play-circle</v-icon>
							</template>
							<v-list-item-title>观看录像</v-list-item-title>
							<v-list-item-subtitle>
								{{ formatFileSize(recordingSize) }}
							</v-list-item-subtitle>
						</v-list-item>

						<v-list-item v-if="hasTranscript" @click="viewTranscript">
							<template #prepend>
								<v-icon color="primary">mdi-text-box</v-icon>
							</template>
							<v-list-item-title>查看记录</v-list-item-title>
						</v-list-item>
					</v-list>
				</v-card>

				<!-- 相关会议 -->
				<v-card elevation="2">
					<v-card-title>相关会议</v-card-title>
					<v-divider></v-divider>
					<v-list>
						<v-list-item
							v-for="related in relatedMeetings"
							:key="related.id"
							@click="navigateToMeeting(related.id)"
						>
							<template #prepend>
								<v-icon>mdi-video</v-icon>
							</template>
							<v-list-item-title>{{ related.title }}</v-list-item-title>
							<v-list-item-subtitle>
								{{ formatDate(related.startTime) }}
							</v-list-item-subtitle>
						</v-list-item>
					</v-list>
				</v-card>
			</v-col>
		</v-row>

		<!-- 编辑会议对话框 -->
		<v-dialog v-model="showEditDialog" max-width="600">
			<MeetingForm :meeting="meetingDetail" @save="handleSave" @cancel="showEditDialog = false" />
		</v-dialog>

		<!-- 删除确认对话框 -->
		<v-dialog v-model="showDeleteDialog" max-width="400">
			<v-card>
				<v-card-title>删除会议</v-card-title>
				<v-card-text> 确定要删除此会议吗?此操作无法撤销。 </v-card-text>
				<v-card-actions>
					<v-spacer></v-spacer>
					<v-btn @click="showDeleteDialog = false">取消</v-btn>
					<v-btn color="error" @click="handleDelete">删除</v-btn>
				</v-card-actions>
			</v-card>
		</v-dialog>
	</v-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDateFormat, useClipboard } from '@vueuse/core'
import { useMeetingApi } from '@/composables/useMeetingApi'
import MeetingStatistics from '../components/MeetingStatistics.vue'
import MeetingForm from '../components/MeetingForm.vue'
import { $notify } from '@/plugins/notification'

const route = useRoute()
const router = useRouter()
const { copy } = useClipboard()
const { fetchMeetingDetail, updateMeeting, deleteMeeting } = useMeetingApi()

const meetingDetail = ref({
	id: '',
	title: '',
	description: '',
	startTime: '',
	duration: 0,
	status: 'scheduled',
	host: { name: '' },
	participantCount: 0,
})

const participants = ref([])
const relatedMeetings = ref([])
const meetingStatistics = ref({})
const showEditDialog = ref(false)
const showDeleteDialog = ref(false)

const hasRecording = ref(false)
const hasTranscript = ref(false)
const recordingSize = ref(0)

const meetingLink = computed(() => {
	return `${window.location.origin}/meeting/${meetingDetail.value.id}`
})

const canJoinMeeting = computed(() => {
	return meetingDetail.value.status === 'ongoing' || meetingDetail.value.status === 'scheduled'
})

const canEditMeeting = computed(() => {
	// 假设当前用户是主持人
	return true
})

const canDeleteMeeting = computed(() => {
	return true
})

const getStatusColor = status => {
	const colors = {
		scheduled: 'warning',
		ongoing: 'success',
		completed: 'grey',
		cancelled: 'error',
	}
	return colors[status] || 'grey'
}

const getStatusIcon = status => {
	const icons = {
		scheduled: 'mdi-clock-outline',
		ongoing: 'mdi-record-circle',
		completed: 'mdi-check-circle',
		cancelled: 'mdi-close-circle',
	}
	return icons[status] || 'mdi-help-circle'
}

const getStatusText = status => {
	const texts = {
		scheduled: '已安排',
		ongoing: '进行中',
		completed: '已结束',
		cancelled: '已取消',
	}
	return texts[status] || '未知'
}

const formatDateTime = time => {
	return useDateFormat(time, 'YYYY年MM月DD日 HH:mm').value
}

const formatDate = time => {
	return useDateFormat(time, 'MM-DD HH:mm').value
}

const formatJoinTime = time => {
	return useDateFormat(time, 'HH:mm').value
}

const formatFileSize = bytes => {
	if (bytes === 0) return '0 B'
	const k = 1024
	const sizes = ['B', 'KB', 'MB', 'GB']
	const i = Math.floor(Math.log(bytes) / Math.log(k))
	return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

const getInitials = name => {
	return name
		.split(' ')
		.map(word => word[0])
		.join('')
		.toUpperCase()
		.slice(0, 2)
}

const copyLink = async () => {
	try {
		await copy(meetingLink.value)
		$notify('会议链接已复制')
	} catch (error) {
		$notify('复制失败')
	}
}

const joinMeeting = () => {
	router.push(`/meeting/${meetingDetail.value.id}`)
}

const editMeeting = () => {
	showEditDialog.value = true
}

const shareMeeting = async () => {
	if (navigator.share) {
		try {
			await navigator.share({
				title: meetingDetail.value.title,
				text: `加入我的会议: ${meetingDetail.value.title}`,
				url: meetingLink.value,
			})
		} catch (error) {
			await copyLink()
		}
	} else {
		await copyLink()
	}
}

const confirmDelete = () => {
	showDeleteDialog.value = true
}

const handleSave = async updatedMeeting => {
	try {
		// 预留API调用
		await updateMeeting(meetingDetail.value.id, updatedMeeting)
		Object.assign(meetingDetail.value, updatedMeeting)
		showEditDialog.value = false
		$notify('会议更新成功')
	} catch (error) {
		$notify('会议更新失败')
	}
}

const handleDelete = async () => {
	try {
		// 预留API调用
		await deleteMeeting(meetingDetail.value.id)
		$notify('会议已删除')
		showDeleteDialog.value = false
		router.push('/')
	} catch (error) {
		$notify('删除失败')
	}
}

const downloadRecording = () => {
	// 预留下载录像API
	console.log('Download recording')
}

const exportTranscript = () => {
	// 预留导出记录API
	console.log('Export transcript')
}

const playRecording = () => {
	// 预留播放录像功能
	console.log('Play recording')
}

const viewTranscript = () => {
	// 预留查看记录功能
	console.log('View transcript')
}

const navigateToMeeting = id => {
	router.push(`/meeting/detail/${id}`)
}

const goBack = () => {
	router.back()
}

onMounted(async () => {
	try {
		// 预留API调用
		const data = await fetchMeetingDetail(route.params.id)
		meetingDetail.value = data.meeting
		participants.value = data.participants
		relatedMeetings.value = data.relatedMeetings
		meetingStatistics.value = data.statistics
		hasRecording.value = data.hasRecording
		hasTranscript.value = data.hasTranscript
		recordingSize.value = data.recordingSize
	} catch (error) {
		$notify('加载会议详情失败')
	}
})
</script>

<style scoped>
.meeting-detail-page {
	padding-top: 12px;
	padding-bottom: 12px;
}

.info-item {
	display: flex;
	align-items: flex-start;
	gap: 12px;
	padding: 8px 0;
}
</style>
