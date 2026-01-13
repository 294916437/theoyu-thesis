<template>
	<v-container class="meeting-detail-page">
		<!-- 加载状态 -->
		<v-row v-if="loading">
			<v-col cols="12" class="text-center py-12">
				<v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
				<div class="mt-4 text-body-1 text-grey">加载会议详情中...</div>
			</v-col>
		</v-row>

		<!-- 错误状态 -->
		<v-row v-else-if="error">
			<v-col cols="12">
				<v-alert type="error" variant="tonal" prominent>
					<template #prepend>
						<v-icon>mdi-alert-circle</v-icon>
					</template>
					<div class="text-h6">加载失败</div>
					<div class="mt-2">{{ error }}</div>
					<div class="mt-4">
						<v-btn color="primary" @click="loadMeetingDetail">重新加载</v-btn>
						<v-btn class="ml-2" variant="text" @click="goBack">返回</v-btn>
					</div>
				</v-alert>
			</v-col>
		</v-row>

		<!-- 主内容 -->
		<template v-else-if="meetingDetail">
			<v-row>
				<v-col cols="12">
					<v-btn variant="text" prepend-icon="mdi-arrow-left" @click="goBack"> 返回 </v-btn>
				</v-col>
			</v-row>

			<v-row>
				<!-- 左侧详情 -->
				<v-col cols="12" md="8">
					<v-card elevation="2" class="mb-4">
						<v-card-title class="d-flex align-center justify-space-between pa-6">
							<div class="d-flex align-center">
								<v-icon color="primary" size="32" class="mr-3">mdi-video</v-icon>
								<h2 class="text-h5">{{ meetingDetail.title }}</h2>
							</div>

							<v-chip :color="statusColor" variant="elevated" size="large">
								<v-icon start size="small">{{ statusIcon }}</v-icon>
								{{ statusText }}
							</v-chip>
						</v-card-title>

						<v-divider></v-divider>

						<v-card-text class="pa-6">
							<!-- 会议信息 -->
							<v-row>
								<v-col cols="12" md="6">
									<div class="info-item">
										<v-icon color="grey-darken-1">mdi-calendar</v-icon>
										<div>
											<div class="text-caption text-grey-darken-1">开始时间</div>
											<div class="text-body-1 font-weight-medium">
												{{ formattedStartTime }}
											</div>
										</div>
									</div>
								</v-col>

								<v-col cols="12" md="6">
									<div class="info-item">
										<v-icon color="grey-darken-1">mdi-clock-outline</v-icon>
										<div>
											<div class="text-caption text-grey-darken-1">持续时间</div>
											<div class="text-body-1 font-weight-medium">
												{{ durationText }}
											</div>
										</div>
									</div>
								</v-col>

								<v-col cols="12" md="6">
									<div class="info-item">
										<v-icon color="grey-darken-1">mdi-account</v-icon>
										<div>
											<div class="text-caption text-grey-darken-1">主持人</div>
											<div class="d-flex align-center mt-1">
												<v-avatar size="24" class="mr-2">
													<v-img
														v-if="meetingDetail.host.avatar"
														:src="meetingDetail.host.avatar"
													></v-img>
													<v-icon v-else size="16">mdi-account</v-icon>
												</v-avatar>
												<span class="text-body-1 font-weight-medium">
													{{ meetingDetail.host.nickName }}
												</span>
											</div>
										</div>
									</div>
								</v-col>

								<v-col cols="12" md="6">
									<div class="info-item">
										<v-icon color="grey-darken-1">mdi-account-multiple</v-icon>
										<div>
											<div class="text-caption text-grey-darken-1">参与人数</div>
											<div class="text-body-1 font-weight-medium">
												{{ meetingDetail.participantCount }} 人
											</div>
										</div>
									</div>
								</v-col>

								<v-col v-if="parsedDescription" cols="12">
									<div class="info-item align-start">
										<v-icon color="grey-darken-1">mdi-cog</v-icon>
										<div class="flex-grow-1">
											<div class="text-caption text-grey-darken-1 mb-2">会议设置</div>
											<div class="d-flex flex-wrap gap-2">
												<v-chip
													v-if="parsedDescription.enableRecording"
													size="small"
													color="primary"
													variant="tonal"
												>
													<v-icon start size="16">mdi-record-circle</v-icon>
													启用录制
												</v-chip>
												<v-chip
													v-if="parsedDescription.enableWhiteboard"
													size="small"
													color="secondary"
													variant="tonal"
												>
													<v-icon start size="16">mdi-drawing</v-icon>
													启用白板
												</v-chip>
												<v-chip
													v-if="parsedDescription.enableScreenShare"
													size="small"
													color="accent"
													variant="tonal"
												>
													<v-icon start size="16">mdi-monitor-share</v-icon>
													启用屏幕共享
												</v-chip>
											</div>
										</div>
									</div>
								</v-col>
							</v-row>

							<!-- 会议链接 -->
							<v-card variant="outlined" class="mt-6" color="surface-variant">
								<v-card-text class="pa-4">
									<div class="text-subtitle-2 mb-3 text-grey-darken-2">会议链接</div>
									<v-text-field
										:model-value="meetingLink"
										readonly
										variant="outlined"
										density="comfortable"
										hide-details
										bg-color="surface"
									>
										<template #append-inner>
											<v-btn
												icon="mdi-content-copy"
												size="small"
												variant="text"
												color="primary"
												@click="copyLink"
											></v-btn>
										</template>
									</v-text-field>
								</v-card-text>
							</v-card>

							<!-- 操作按钮 -->
							<div class="d-flex flex-wrap gap-3 mt-6 align-center">
								<v-btn
									v-if="canJoinMeeting"
									color="primary"
									size="large"
									prepend-icon="mdi-video"
									elevation="2"
									@click="joinMeeting"
								>
									加入会议
								</v-btn>

								<v-spacer></v-spacer>

								<v-btn
									v-if="canEditMeeting"
									variant="outlined"
									color="primary"
									prepend-icon="mdi-pencil"
									@click="editMeeting"
								>
									编辑
								</v-btn>

								<v-btn
									variant="outlined"
									color="secondary"
									prepend-icon="mdi-share-variant"
									@click="shareMeeting"
								>
									分享
								</v-btn>

								<v-menu>
									<template #activator="{ props }">
										<v-btn
											icon="mdi-dots-vertical"
											variant="text"
											color="grey-darken-1"
											v-bind="props"
										></v-btn>
									</template>
									<v-list>
										<v-list-item
											v-if="meetingDetail.recording?.available"
											@click="downloadRecording"
										>
											<template #prepend>
												<v-icon color="primary">mdi-download</v-icon>
											</template>
											<v-list-item-title>下载录像</v-list-item-title>
										</v-list-item>
										<v-list-item
											v-if="meetingDetail.transcript?.available"
											@click="exportTranscript"
										>
											<template #prepend>
												<v-icon color="primary">mdi-file-document</v-icon>
											</template>
											<v-list-item-title>导出记录</v-list-item-title>
										</v-list-item>
										<v-divider v-if="canDeleteMeeting"></v-divider>
										<v-list-item v-if="canDeleteMeeting" @click="confirmDelete">
											<template #prepend>
												<v-icon color="error">mdi-delete</v-icon>
											</template>
											<v-list-item-title class="text-error">删除会议</v-list-item-title>
										</v-list-item>
									</v-list>
								</v-menu>
							</div>
						</v-card-text>
					</v-card>

					<!-- 参与者列表 -->
					<v-card elevation="2">
						<v-card-title class="pa-6">
							<v-icon color="primary" class="mr-2">mdi-account-multiple</v-icon>
							参与者 ({{ meetingDetail.participants.length }})
						</v-card-title>
						<v-divider></v-divider>
						<v-card-text class="pa-0">
							<v-list v-if="meetingDetail.participants.length > 0">
								<template
									v-for="(participant, index) in meetingDetail.participants"
									:key="participant.userId"
								>
									<v-list-item class="px-6 py-3">
										<template #prepend>
											<v-badge
												:color="getParticipantStatusColor(participant.status)"
												dot
												location="bottom right"
												offset-x="4"
												offset-y="4"
											>
												<v-avatar color="primary" size="40">
													<v-img v-if="participant.avatar" :src="participant.avatar"></v-img>
													<v-icon v-else icon="mdi-account" size="20"></v-icon>
												</v-avatar>
											</v-badge>
										</template>

										<v-list-item-title class="d-flex align-center">
											<span class="font-weight-medium">{{ participant.userName }}</span>
											<v-chip
												v-if="participant.role === 2"
												size="x-small"
												color="warning"
												variant="flat"
												class="ml-2"
											>
												主持人
											</v-chip>
											<v-chip
												v-else-if="participant.role === 1"
												size="x-small"
												color="info"
												variant="tonal"
												class="ml-2"
											>
												成员
											</v-chip>
										</v-list-item-title>

										<v-list-item-subtitle class="d-flex align-center mt-1">
											<v-icon v-if="participant.audioMuted" size="16" color="error" class="mr-1">
												mdi-microphone-off
											</v-icon>
											<v-icon v-else size="16" color="success" class="mr-1">
												mdi-microphone
											</v-icon>

											<v-icon v-if="participant.videoMuted" size="16" color="error" class="mr-2">
												mdi-video-off
											</v-icon>
											<v-icon v-else size="16" color="success" class="mr-2"> mdi-video </v-icon>

											<span class="text-caption text-grey-darken-1">
												{{ getParticipantStatusText(participant) }}
											</span>
										</v-list-item-subtitle>

										<template #append>
											<div class="text-caption text-grey-darken-1 text-right">
												<div>{{ formatJoinTime(participant.joinedAt) }}</div>
												<div v-if="participant.leftAt" class="mt-1">
													{{ formatJoinTime(participant.leftAt) }}
												</div>
											</div>
										</template>
									</v-list-item>
									<v-divider v-if="index < meetingDetail.participants.length - 1"></v-divider>
								</template>
							</v-list>
							<div v-else class="pa-8 text-center text-grey-darken-1">
								<v-icon size="48" color="grey-lighten-1">mdi-account-off</v-icon>
								<div class="mt-4">暂无参与者</div>
							</div>
						</v-card-text>
					</v-card>
				</v-col>

				<!-- 右侧信息 -->
				<v-col cols="12" md="4">
					<!-- 会议统计 -->
					<v-card elevation="2" class="mb-4">
						<v-card-title class="pa-6">
							<v-icon color="primary" class="mr-2">mdi-chart-box</v-icon>
							会议统计
						</v-card-title>
						<v-divider></v-divider>
						<v-card-text class="pa-6">
							<MeetingStatistics :statistics="meetingStatistics" />
						</v-card-text>
					</v-card>

					<!-- 录像与记录 -->
					<v-card
						v-if="meetingDetail.recording?.available || meetingDetail.transcript?.available"
						elevation="2"
						class="mb-4"
					>
						<v-card-title class="pa-6">
							<v-icon color="primary" class="mr-2">mdi-file-video</v-icon>
							录像与记录
						</v-card-title>
						<v-divider></v-divider>
						<v-list>
							<v-list-item v-if="meetingDetail.recording?.available" @click="playRecording" class="px-6">
								<template #prepend>
									<v-icon color="primary">mdi-play-circle</v-icon>
								</template>
								<v-list-item-title>观看录像</v-list-item-title>
								<v-list-item-subtitle>
									{{ formatFileSize(meetingDetail.recording.size) }} ·
									{{ meetingDetail.recording.duration }} 分钟
								</v-list-item-subtitle>
							</v-list-item>

							<v-divider
								v-if="meetingDetail.recording?.available && meetingDetail.transcript?.available"
							></v-divider>

							<v-list-item
								v-if="meetingDetail.transcript?.available"
								class="px-6"
								@click="viewTranscript"
							>
								<template #prepend>
									<v-icon color="secondary">mdi-text-box</v-icon>
								</template>
								<v-list-item-title>查看记录</v-list-item-title>
							</v-list-item>
						</v-list>
					</v-card>

					<!-- 相关会议 -->
					<v-card v-if="relatedMeetings.length > 0" elevation="2">
						<v-card-title class="pa-6">
							<v-icon color="primary" class="mr-2">mdi-link-variant</v-icon>
							相关会议
						</v-card-title>
						<v-divider></v-divider>
						<v-list>
							<template v-for="(related, index) in relatedMeetings" :key="related.roomNo">
								<v-list-item @click="navigateToMeeting(related.roomNo)" class="px-6">
									<template #prepend>
										<v-icon color="secondary">mdi-video</v-icon>
									</template>
									<v-list-item-title>{{ related.title }}</v-list-item-title>
									<v-list-item-subtitle>
										{{ formatDate(related.startTime) }}
									</v-list-item-subtitle>
								</v-list-item>
								<v-divider v-if="index < relatedMeetings.length - 1"></v-divider>
							</template>
						</v-list>
					</v-card>
				</v-col>
			</v-row>
		</template>

		<!-- 编辑会议对话框 -->
		<v-dialog v-model="showEditDialog" max-width="600">
			<MeetingForm :meeting="meetingDetail" @save="handleSave" @cancel="showEditDialog = false" />
		</v-dialog>

		<!-- 删除确认对话框 -->
		<v-dialog v-model="showDeleteDialog" max-width="400">
			<v-card>
				<v-card-title class="pa-6">
					<v-icon color="error" class="mr-2">mdi-alert-circle</v-icon>
					删除会议
				</v-card-title>
				<v-divider></v-divider>
				<v-card-text class="pa-6"> 确定要删除此会议吗？此操作无法撤销。 </v-card-text>
				<v-card-actions class="pa-6 pt-0">
					<v-spacer></v-spacer>
					<v-btn variant="text" @click="showDeleteDialog = false">取消</v-btn>
					<v-btn color="error" variant="flat" @click="handleDelete">删除</v-btn>
				</v-card-actions>
			</v-card>
		</v-dialog>
	</v-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDateFormat, useClipboard } from '@vueuse/core'
import MeetingStatistics from '../components/MeetingStatistics.vue'
import MeetingForm from '../components/MeetingForm.vue'
import { $notify } from '@/plugins/notification'
import { fetchMeetingDetail, updateMeeting, deleteMeeting } from '@/api/room'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const { copy, copied } = useClipboard()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.user?.id)

// 响应式数据
const loading = ref(false)
const error = ref(null)
const meetingDetail = ref(null)
const relatedMeetings = ref([])
const showEditDialog = ref(false)
const showDeleteDialog = ref(false)

// 会议链接
const meetingLink = computed(() => {
	if (!meetingDetail.value) return ''
	return `${window.location.origin}/meeting/room/${meetingDetail.value.roomNo}`
})

// 解析会议设置
const parsedDescription = computed(() => {
	if (!meetingDetail.value?.description) return null
	try {
		return JSON.parse(meetingDetail.value.description)
	} catch {
		return null
	}
})

// 会议状态相关
const statusMap = {
	0: { text: '待开始', color: 'warning', icon: 'mdi-clock-outline' },
	1: { text: '进行中', color: 'success', icon: 'mdi-record-circle' },
	2: { text: '已结束', color: 'grey', icon: 'mdi-check-circle' },
	3: { text: '已取消', color: 'error', icon: 'mdi-close-circle' },
}

const statusColor = computed(() => {
	return statusMap[meetingDetail.value?.status]?.color || 'grey'
})

const statusIcon = computed(() => {
	return statusMap[meetingDetail.value?.status]?.icon || 'mdi-help-circle'
})

const statusText = computed(() => {
	return statusMap[meetingDetail.value?.status]?.text || '未知'
})

// 格式化时间
const formattedStartTime = computed(() => {
	if (!meetingDetail.value?.startTime) return '-'
	return useDateFormat(meetingDetail.value.startTime, 'YYYY年MM月DD日 HH:mm').value
})

const durationText = computed(() => {
	const duration = meetingDetail.value?.duration || 0
	if (duration === 0) return '进行中'
	if (duration < 60) return `${duration} 分钟`
	const hours = Math.floor(duration / 60)
	const minutes = duration % 60
	return `${hours} 小时 ${minutes} 分钟`
})

// 格式化加入时间
const formatJoinTime = time => {
	if (!time) return '-'
	return useDateFormat(time, 'HH:mm').value
}

const formatDate = time => {
	if (!time) return '-'
	return useDateFormat(time, 'MM-DD HH:mm').value
}

// 格式化文件大小
const formatFileSize = bytes => {
	if (!bytes || bytes === 0) return '0 B'
	const num = Number(bytes)
	if (isNaN(num)) return '0 B'
	const k = 1024
	const sizes = ['B', 'KB', 'MB', 'GB']
	const i = Math.floor(Math.log(num) / Math.log(k))
	return Math.round((num / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

// 参与者状态
const getParticipantStatusColor = status => {
	const colors = {
		1: 'success', // 在线
		2: 'grey', // 离线
	}
	return colors[status] || 'grey'
}

const getParticipantStatusText = participant => {
	if (participant.status === 1) {
		return '在线中'
	} else if (participant.leftAt) {
		return `已离开`
	}
	return '已离开'
}

// 权限判断
const canJoinMeeting = computed(() => {
	return meetingDetail.value?.status === 0 || meetingDetail.value?.status === 1
})

const canEditMeeting = computed(() => {
	return meetingDetail.value?.host?.id === currentUserId.value
})

const canDeleteMeeting = computed(() => {
	return meetingDetail.value?.host?.id === currentUserId.value
})

// 会议统计数据
const meetingStatistics = computed(() => {
	if (!meetingDetail.value) return {}

	return {
		totalParticipants: meetingDetail.value.participantCount || 0,
		currentOnline: meetingDetail.value.participants.filter(p => p.status === 1).length,
		duration: meetingDetail.value.duration || 0,
		hasRecording: meetingDetail.value.recording?.available || false,
		hasTranscript: meetingDetail.value.transcript?.available || false,
	}
})

// 方法
const loadMeetingDetail = async () => {
	loading.value = true
	error.value = null

	try {
		const roomIdOrNo = route.params.roomNo
		const { data } = await fetchMeetingDetail(roomIdOrNo)
		meetingDetail.value = data
	} catch (err) {
		console.error('加载会议详情失败:', err)
		error.value = err.message || '加载会议详情失败，请稍后重试'
	} finally {
		loading.value = false
	}
}

const copyLink = async () => {
	try {
		await copy(meetingLink.value)
		if (copied.value) {
			$notify.success('会议链接已复制到剪贴板')
		}
	} catch (error) {
		$notify.error('复制失败，请手动复制')
	}
}

const joinMeeting = () => {
	router.push(`/meeting/room/${meetingDetail.value.roomNo}`)
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
			if (error.name !== 'AbortError') {
				await copyLink()
			}
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
		await updateMeeting(meetingDetail.value.roomId, updatedMeeting)
		Object.assign(meetingDetail.value, updatedMeeting)
		showEditDialog.value = false
		$notify.success('会议更新成功')
	} catch (error) {
		console.error('更新会议失败:', error)
		$notify.error('会议更新失败，请稍后重试')
	}
}

const handleDelete = async () => {
	try {
		await deleteMeeting(meetingDetail.value.roomId)
		$notify.success('会议已删除')
		showDeleteDialog.value = false
		router.push('/')
	} catch (error) {
		console.error('删除会议失败:', error)
		$notify.error('删除失败，请稍后重试')
	}
}

const downloadRecording = () => {
	if (meetingDetail.value.recording?.url) {
		window.open(meetingDetail.value.recording.url, '_blank')
	} else {
		$notify.warning('录像文件不可用')
	}
}

const exportTranscript = () => {
	if (meetingDetail.value.transcript?.url) {
		window.open(meetingDetail.value.transcript.url, '_blank')
	} else {
		$notify.warning('记录文件不可用')
	}
}

const playRecording = () => {
	if (meetingDetail.value.recording?.url) {
		window.open(meetingDetail.value.recording.url, '_blank')
	} else {
		$notify.warning('录像文件不可用')
	}
}

const viewTranscript = () => {
	if (meetingDetail.value.transcript?.url) {
		window.open(meetingDetail.value.transcript.url, '_blank')
	} else {
		$notify.warning('记录文件不可用')
	}
}

const navigateToMeeting = roomNo => {
	router.push(`/meeting/detail/${roomNo}`)
	// 重新加载数据
	loadMeetingDetail()
}

const goBack = () => {
	router.back()
}

// 初始化
loadMeetingDetail()
</script>

<style scoped>
.meeting-detail-page {
	padding-top: 12px;
	padding-bottom: 12px;
	max-width: 1400px;
}

.info-item {
	display: flex;
	align-items: flex-start;
	gap: 16px;
	padding: 12px 0;
}

.info-item > .v-icon {
	margin-top: 2px;
}
</style>
