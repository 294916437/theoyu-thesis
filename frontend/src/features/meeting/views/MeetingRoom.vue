<template>
	<v-app>
		<v-main class="meeting-room">
			<!-- 顶部信息栏 -->
			<v-app-bar density="compact" flat elevation="0" color="surface" class="meeting-header">
				<v-toolbar-title class="d-flex align-center">
					<v-icon icon="mdi-video" color="primary" class="mr-2"></v-icon>
					<span class="text-h6 font-weight-medium">{{ meetingInfo.title }}</span>
				</v-toolbar-title>

				<v-spacer></v-spacer>

				<div class="d-flex align-center mr-4">
					<v-icon icon="mdi-clock-outline" size="small" class="mr-1"></v-icon>
					<span class="text-body-2 font-weight-medium">{{ meetingDuration }}</span>
				</div>

				<v-chip color="success" variant="flat" size="small" class="mr-4">
					<template #prepend>
						<v-icon icon="mdi-account-multiple" size="small"></v-icon>
					</template>
					<span class="font-weight-medium">{{ participantCount }} 人</span>
				</v-chip>

				<v-btn icon="mdi-cog" variant="text" size="small" @click="showSettings = true"></v-btn>
			</v-app-bar>

			<!-- 主视频区域 -->
			<v-container fluid class="video-container pa-0" :style="{ height: videoContainerHeight }">
				<v-row no-gutters class="fill-height">
					<!-- 视频网格 -->
					<v-col :cols="showSidebar ? 9 : 12" class="video-main">
						<VideoGrid :participants="participants" :screen-share="screenShare" :layout="videoLayout" />

						<!-- 屏幕共享覆盖层 -->
						<ScreenShare
							v-if="screenShare.active"
							:stream="screenShare.stream"
							:presenter="screenShare.presenter"
							:participants="participants"
						/>
					</v-col>

					<!-- 侧边栏 -->
					<v-col v-if="showSidebar" cols="3" class="sidebar-container">
						<div class="sidebar">
							<v-tabs
								v-model="sidebarTab"
								bg-color="surface"
								color="primary"
								density="compact"
								class="sidebar-tabs"
							>
								<v-tab value="participants">
									<v-icon icon="mdi-account-multiple" size="small" class="mr-1"></v-icon>
									<span class="text-caption">参与者</span>
								</v-tab>

								<v-tab value="chat">
									<v-badge
										:content="unreadMessages"
										:model-value="unreadMessages > 0"
										color="error"
										inline
									>
										<v-icon icon="mdi-chat" size="small" class="mr-1"></v-icon>
										<span class="text-caption">聊天</span>
									</v-badge>
								</v-tab>
							</v-tabs>

							<v-tabs-window v-model="sidebarTab" class="sidebar-content">
								<v-tabs-window-item value="participants" class="h-100">
									<ParticipantsList
										:participants="participants"
										:current-user-id="currentUserId"
										:meeting-id="meetingInfo.id"
										@mute-participant="handleMuteParticipant"
										@remove-participant="handleRemoveParticipant"
										@pin-participant="handlePinParticipant"
										@spotlight-participant="handleSpotlightParticipant"
									/>
								</v-tabs-window-item>

								<v-tabs-window-item value="chat" class="h-100">
									<ChatPanel
										:messages="chatMessages"
										:current-user-id="currentUserId"
										@send-message="handleSendMessage"
										@message-read="handleMessageRead"
										@load-more="handleLoadMoreMessages"
										@file-upload="handleFileUpload"
									/>
								</v-tabs-window-item>
							</v-tabs-window>
						</div>
					</v-col>
				</v-row>
			</v-container>

			<!-- 底部控制栏 -->
			<ControlBar
				v-model:audio-enabled="audioEnabled"
				v-model:video-enabled="videoEnabled"
				v-model:screen-sharing="screenSharing"
				v-model:collapsed="controlBarCollapsed"
				:show-sidebar="showSidebar"
				:unread-count="unreadMessages"
				@toggle-sidebar="showSidebar = !showSidebar"
				@toggle-sidebar-chat="toggleSidebarChat()"
				@leave-meeting="handleLeaveMeeting"
				@toggle-layout="toggleVideoLayout"
				@open-settings="showSettings = true"
			/>

			<!-- 设置对话框 -->
			<v-dialog v-model="showSettings" max-width="600" transition="dialog-bottom-transition">
				<v-card class="settings-dialog">
					<v-card-title class="d-flex align-center justify-space-between pa-4">
						<span class="text-h6 font-weight-medium">会议设置</span>
						<v-btn icon="mdi-close" variant="text" size="small" @click="showSettings = false"></v-btn>
					</v-card-title>

					<v-divider></v-divider>

					<v-card-text class="pa-6">
						<!-- 音视频设备 -->
						<div class="mb-6">
							<h3 class="text-subtitle-1 font-weight-medium mb-4">音视频设备</h3>

							<v-select
								v-model="selectedCamera"
								:items="cameras"
								label="摄像头"
								item-title="label"
								item-value="deviceId"
								variant="outlined"
								density="comfortable"
								prepend-inner-icon="mdi-camera"
								color="primary"
								class="mb-4"
							></v-select>

							<v-select
								v-model="selectedMicrophone"
								:items="microphones"
								label="麦克风"
								item-title="label"
								item-value="deviceId"
								variant="outlined"
								density="comfortable"
								prepend-inner-icon="mdi-microphone"
								color="primary"
								class="mb-4"
							></v-select>

							<v-select
								v-model="selectedSpeaker"
								:items="speakers"
								label="扬声器"
								item-title="label"
								item-value="deviceId"
								variant="outlined"
								density="comfortable"
								prepend-inner-icon="mdi-volume-high"
								color="primary"
							></v-select>
						</div>

						<v-divider class="my-6"></v-divider>

						<!-- 视频设置 -->
						<div>
							<h3 class="text-subtitle-1 font-weight-medium mb-4">视频设置</h3>

							<v-slider
								v-model="videoQuality"
								:min="1"
								:max="3"
								:step="1"
								:ticks="{ 1: '流畅', 2: '标清', 3: '高清' }"
								show-ticks="always"
								tick-size="4"
								color="primary"
								class="mb-6"
							>
								<template #prepend>
									<v-icon icon="mdi-quality-low"></v-icon>
								</template>
								<template #append>
									<v-icon icon="mdi-quality-high"></v-icon>
								</template>
							</v-slider>

							<v-switch
								v-model="enableHD"
								label="启用高清视频"
								color="primary"
								hide-details
								class="mb-3"
							></v-switch>

							<v-switch
								v-model="enableMirror"
								label="镜像我的视频"
								color="primary"
								hide-details
							></v-switch>
						</div>
					</v-card-text>

					<v-divider></v-divider>

					<v-card-actions class="pa-4">
						<v-spacer></v-spacer>
						<v-btn variant="text" @click="showSettings = false"> 取消 </v-btn>
						<v-btn color="primary" variant="flat" @click="saveSettings"> 保存设置 </v-btn>
					</v-card-actions>
				</v-card>
			</v-dialog>

			<!-- 离开会议确认对话框 -->
			<v-dialog v-model="showLeaveConfirm" max-width="480" persistent transition="dialog-bottom-transition">
				<v-card class="leave-confirm-dialog">
					<!-- 警告图标 -->
					<v-card-title class="d-flex align-center pa-6">
						<div class="warning-icon-wrapper">
							<v-icon icon="mdi-exit-to-app" color="warning" size="40"></v-icon>
						</div>
					</v-card-title>

					<v-card-text class="px-6 pb-6">
						<!-- 主要提示 -->
						<div class="mb-4">
							<h3 class="text-h6 font-weight-medium mb-2">确认离开会议？</h3>
							<p class="text-body-2">会议时长: {{ meetingDuration }}</p>
						</div>

						<!-- 信息提示卡片 -->
						<v-alert type="info" variant="tonal" density="compact" class="mb-0">
							<div class="text-body-2">
								<div class="font-weight-medium mb-2">温馨提示</div>
								<div>
									• 会议记录和聊天内容将会保留<br />
									• 您的离开不会结束整个会议<br />
									• 其他参与者将收到您离开的通知
								</div>
							</div>
						</v-alert>
					</v-card-text>

					<v-divider></v-divider>

					<v-card-actions class="pa-4">
						<v-btn variant="text" prepend-icon="mdi-arrow-left" @click="showLeaveConfirm = false">
							留在会议
						</v-btn>

						<v-spacer></v-spacer>

						<v-btn
							color="error"
							variant="flat"
							prepend-icon="mdi-exit-to-app"
							class="leave-btn"
							@click="confirmLeaveMeeting"
						>
							确认离开
						</v-btn>
					</v-card-actions>
				</v-card>
			</v-dialog>

			<!-- 加载覆盖层 -->
			<LoadingOverlay :visible="isLoading" :message="loadingMessage" :progress="loadingProgress" />
		</v-main>
	</v-app>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useIntervalFn } from '@vueuse/core'
import VideoGrid from '../components/VideoGrid.vue'
import ScreenShare from '../components/ScreenShare.vue'
import ParticipantsList from '../components/ParticipantsList.vue'
import ChatPanel from '../components/ChatPanel.vue'
import ControlBar from '../components/ControlBar.vue'
import LoadingOverlay from '@/components/common/LoadingOverlay.vue'
import { useMediaDevices } from '@/composables/useMediaDevices'
import { useWebRTC } from '@/composables/useWebRTC'
import { fetchMeetingDetail } from '@/api/room'

import { $notify } from '@/plugins/notification'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 会议信息
const meetingInfo = ref({
	roomNo: route.params.roomNo,
	title: '视频会议',
	startTime: new Date(),
})

// 用户信息
const currentUserId = userStore.userId
const currentUsername = userStore.profile.username
const authToken = userStore.token

// 媒体设备
const { cameras, microphones, speakers, selectedCamera, selectedMicrophone, selectedSpeaker, enumerateDevices } =
	useMediaDevices()

// WebRTC
const {
	roomId,
	peerId,
	localStream,
	participants,
	remoteParticipants,
	audioEnabled,
	videoEnabled,
	screenSharing,
	screenStream,
	connectionState,
	joinMeeting,
	leaveMeeting,
	toggleAudio,
	toggleVideo,
	startScreenShare,
	stopScreenShare,
} = useWebRTC()

// UI 状态
const showSidebar = ref(true)
const showSettings = ref(false)
const sidebarTab = ref('participants')
const videoLayout = ref('grid')
const chatMessages = ref([])
const unreadMessages = ref(0)
const isLoading = ref(false)
const loadingMessage = ref('')
const loadingProgress = ref(0)
const controlBarCollapsed = ref(false)
const showLeaveConfirm = ref(false)

// 视频设置
const videoQuality = ref(2)
const enableHD = ref(true)
const enableMirror = ref(false)

// 计算属性
const participantCount = computed(() => participants.value.length)

const videoContainerHeight = computed(() => {
	const topBarHeight = 48
	const controlBarHeight = controlBarCollapsed.value ? 0 : 88
	return `calc(100vh - ${topBarHeight}px - ${controlBarHeight}px)`
})

// 屏幕共享信息
const screenShare = computed(() => {
	const sharingPeer = participants.value.find(p =>
		Object.values(p.producers).some(producer => producer.appData?.source === 'screen'),
	)

	if (sharingPeer) {
		const screenProducer = Object.values(sharingPeer.producers).find(
			producer => producer.appData?.source === 'screen',
		)

		return {
			active: true,
			stream: new MediaStream([screenProducer.track]),
			presenter: sharingPeer.username,
		}
	}

	return { active: false, stream: null, presenter: null }
})

// 会议时长
const meetingStartTime = ref(Date.now())
const meetingDuration = ref('00:00')

useIntervalFn(() => {
	const duration = Math.floor((Date.now() - meetingStartTime.value) / 1000)
	const hours = Math.floor(duration / 3600)
	const minutes = Math.floor((duration % 3600) / 60)
	const seconds = duration % 60

	if (hours > 0) {
		meetingDuration.value = `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
	} else {
		meetingDuration.value = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
	}
}, 1000)

// 监听音视频状态变化
watch(audioEnabled, enabled => {
	console.log('Audio state changed', enabled)
})

watch(videoEnabled, enabled => {
	console.log('Video state changed', enabled)
})

watch(screenSharing, sharing => {
	if (sharing) {
		startScreenShare()
	} else {
		stopScreenShare()
	}
})

// 监听连接状态
watch(connectionState, state => {
	if (state === 'failed') {
		$notify.error('连接失败,请重试')
	} else if (state === 'disconnected') {
		console.log('Connection disconnected')
	}
})

// 方法
const toggleSidebarChat = () => {
	showSidebar.value = true
	sidebarTab.value = 'chat'
}

const toggleVideoLayout = () => {
	const layouts = ['grid', 'spotlight', 'sidebar']
	const currentIndex = layouts.indexOf(videoLayout.value)
	videoLayout.value = layouts[(currentIndex + 1) % layouts.length]
	$notify.success(`已切换到${layouts[(currentIndex + 1) % layouts.length]}布局`)
}

const handleLeaveMeeting = () => {
	showLeaveConfirm.value = true
}

const confirmLeaveMeeting = async () => {
	showLeaveConfirm.value = false

	try {
		isLoading.value = true
		loadingMessage.value = '正在离开会议...'

		await leaveMeeting()

		router.push('/')
		$notify.success('已离开会议')
	} catch (error) {
		console.error('Failed to leave meeting', error)
		$notify.error('离开会议失败')
		router.push('/')
	} finally {
		isLoading.value = false
	}
}

const handleSendMessage = async message => {
	chatMessages.value.push({
		id: Date.now(),
		userId: currentUserId.value,
		userName: currentUsername.value,
		content: message.content,
		type: message.type || 'text',
		timestamp: new Date(),
		isOwn: true,
	})

	// TODO: 通过 Socket.io 发送消息给其他参与者
}

const handleMessageRead = () => {
	unreadMessages.value = 0
}

const handleLoadMoreMessages = async () => {
	console.log('Load more messages')
}

const handleFileUpload = async fileData => {
	chatMessages.value.push({
		id: Date.now(),
		userId: currentUserId.value,
		userName: currentUsername.value,
		type: 'file',
		file: fileData.file,
		timestamp: new Date(),
		isOwn: true,
	})
}

const handleMuteParticipant = async participantId => {
	console.log('Mute participant', participantId)
	// TODO: 实现远程静音功能 (需要后端支持)
}

const handleRemoveParticipant = async participantId => {
	console.log('Remove participant', participantId)
	// TODO: 实现踢出参与者功能 (需要后端支持)
}

const handlePinParticipant = participantId => {
	console.log('Pin participant', participantId)
	// TODO: 实现固定参与者视图
}

const handleSpotlightParticipant = participantId => {
	console.log('Spotlight participant', participantId)
	// TODO: 实现聚光灯模式
}

const saveSettings = () => {
	showSettings.value = false
	$notify.success('设置已保存')
	// TODO: 应用设备更改
}

// 加载会议详情
async function loadMeetingDetail() {
	try {
		const detail = await fetchMeetingDetail(meetingInfo.value.roomNo)
		meetingInfo.value = {
			...meetingInfo.value,
			...detail.meeting,
		}
	} catch (error) {
		console.error('Failed to load meeting detail', error)
	}
}

onMounted(async () => {
	isLoading.value = true
	loadingMessage.value = '正在初始化...'
	loadingProgress.value = 20

	try {
		// 1. 枚举媒体设备
		await enumerateDevices()
		loadingProgress.value = 40

		// 2. 加载会议详情
		await loadMeetingDetail()
		loadingProgress.value = 60

		// 3. 加入会议房间
		loadingMessage.value = '正在加入会议...'
		await joinMeeting(meetingInfo.value.id, currentUserId.value, currentUsername.value, authToken.value)

		loadingProgress.value = 100
		$notify.success('已成功加入会议')
	} catch (error) {
		console.error('Failed to join meeting', error)
		$notify.error(`加入会议失败: ${error.message}`)
	} finally {
		isLoading.value = false
		loadingProgress.value = 0
	}
})

onBeforeUnmount(() => {
	leaveMeeting()
})
</script>

<style scoped>
.meeting-room {
	background: rgb(var(--v-theme-background));
	height: 100vh;
	overflow: hidden;
}

.meeting-header {
	border-bottom: 1px solid rgb(var(--v-theme-border));
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.video-container {
	transition: height 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.video-main {
	position: relative;
	background: #000;
}

/* 侧边栏 */
.sidebar-container {
	display: flex;
	flex-direction: column;
	height: 100%;
}

.sidebar {
	background: rgb(var(--v-theme-surface));
	border-left: 1px solid rgb(var(--v-theme-border));
	display: flex;
	flex-direction: column;
	height: 100%;
	overflow: hidden;
}

.sidebar-tabs {
	flex-shrink: 0;
	border-bottom: 1px solid rgb(var(--v-theme-border));
}

.sidebar-content {
	flex: 1;
	min-height: 0;
	overflow: hidden;
}

.sidebar-content :deep(.v-window__container) {
	height: 100%;
}

.sidebar-content :deep(.v-window-item) {
	height: 100%;
}

/* 设置对话框 */
.settings-dialog {
	background: rgb(var(--v-theme-surface));
}

/* 离开会议对话框 */
.leave-confirm-dialog {
	background: rgb(var(--v-theme-surface));
	border: 1px solid rgb(var(--v-theme-border));
	overflow: hidden;
}

.warning-icon-wrapper {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 64px;
	height: 64px;
	margin: 0 auto;
	border-radius: 50%;
	background: rgba(var(--v-theme-warning), 0.1);
	border: 2px solid rgba(var(--v-theme-warning), 0.3);
}

.leave-btn {
	font-weight: 600;
	letter-spacing: 0.5px;
}

/* 工具类 */
.fill-height {
	height: 100%;
}

.h-100 {
	height: 100%;
}

/* 响应式调整 */
@media (max-width: 960px) {
	.sidebar-container {
		display: none;
	}

	.video-main {
		width: 100% !important;
	}
}

@media (max-width: 600px) {
	.warning-icon-wrapper {
		width: 56px;
		height: 56px;
	}
}
</style>
