<template>
	<v-app>
		<v-main class="meeting-room">
			<!-- 顶部信息栏 -->
			<v-app-bar class="meeting-header" density="compact" flat elevation="0">
				<v-toolbar-title class="d-flex align-center">
					<v-icon class="mr-2 text-primary">mdi-video</v-icon>
					<span class="text-h6 font-weight-medium">{{ meetingInfo.title }}</span>
				</v-toolbar-title>

				<v-spacer></v-spacer>

				<div class="d-flex align-center mr-4">
					<v-icon size="small" class="mr-1 text-medium-emphasis">mdi-clock-outline</v-icon>
					<span class="text-body-2 font-weight-medium">{{ meetingDuration }}</span>
				</div>

				<v-chip color="success" variant="flat" class="mr-4" size="small">
					<v-icon left size="small">mdi-account-multiple</v-icon>
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
								bg-color="surface-variant"
								color="primary"
								class="sidebar-tabs"
							>
								<v-tab value="participants">
									<v-icon left size="small">mdi-account-multiple</v-icon>
									<span class="text-caption">参与者</span>
								</v-tab>
								<v-tab value="chat">
									<v-badge
										:content="unreadMessages"
										:model-value="unreadMessages > 0"
										color="error"
										inline
									>
										<v-icon left size="small">mdi-chat</v-icon>
										<span class="text-caption ml-1">聊天</span>
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
					<v-card-title class="d-flex align-center justify-space-between">
						<span class="text-h6">会议设置</span>
						<v-btn icon="mdi-close" variant="text" size="small" @click="showSettings = false"></v-btn>
					</v-card-title>

					<v-divider></v-divider>

					<v-card-text class="pa-6">
						<h3 class="text-subtitle-1 mb-4 text-medium-emphasis">音视频设备</h3>

						<v-select
							v-model="selectedCamera"
							:items="cameras"
							label="摄像头"
							item-title="label"
							item-value="deviceId"
							prepend-inner-icon="mdi-camera"
							variant="outlined"
							density="comfortable"
							class="mb-4"
						></v-select>

						<v-select
							v-model="selectedMicrophone"
							:items="microphones"
							label="麦克风"
							item-title="label"
							item-value="deviceId"
							prepend-inner-icon="mdi-microphone"
							variant="outlined"
							density="comfortable"
							class="mb-4"
						></v-select>

						<v-select
							v-model="selectedSpeaker"
							:items="speakers"
							label="扬声器"
							item-title="label"
							item-value="deviceId"
							prepend-inner-icon="mdi-volume-high"
							variant="outlined"
							density="comfortable"
						></v-select>

						<v-divider class="my-6"></v-divider>

						<h3 class="text-subtitle-1 mb-4 text-medium-emphasis">视频设置</h3>

						<v-slider
							v-model="videoQuality"
							:min="1"
							:max="3"
							:step="1"
							:ticks="{ 1: '流畅', 2: '标清', 3: '高清' }"
							show-ticks="always"
							tick-size="4"
							class="mb-4"
						>
							<template #prepend>
								<v-icon>mdi-quality-low</v-icon>
							</template>
							<template #append>
								<v-icon>mdi-quality-high</v-icon>
							</template>
						</v-slider>

						<v-switch
							v-model="enableHD"
							label="启用高清视频"
							color="primary"
							hide-details
							class="mb-2"
						></v-switch>

						<v-switch v-model="enableMirror" label="镜像我的视频" color="primary" hide-details></v-switch>
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
					<!-- 顶部警告条 -->
					<div class="warning-stripe"></div>

					<v-card-title class="px-6">
						<div class="warning-icon-wrapper">
							<v-icon color="warning" size="32">mdi-exit-to-app</v-icon>
						</div>
					</v-card-title>

					<v-divider></v-divider>

					<v-card-text class="pa-6">
						<!-- 主要提示 -->
						<div class="mb-4">
							<p class="text-body-1 font-weight-medium mb-2">
								您确定要离开当前会议吗？会议时长: {{ meetingDuration }} 分钟
							</p>
						</div>

						<!-- 信息提示卡片 -->
						<v-card variant="tonal" color="info" class="info-card mb-4">
							<v-card-text class="pa-3">
								<div class="d-flex align-center">
									<v-icon size="20" class="mr-2">mdi-information-outline</v-icon>
									<div>
										<div class="font-weight-medium mb-1">温馨提示</div>
										<div>
											• 会议记录和聊天内容将会保留<br />
											• 您的离开不会结束整个会议<br />
											• 其他参与者将收到您离开的通知
										</div>
									</div>
								</div>
							</v-card-text>
						</v-card>
					</v-card-text>

					<v-divider></v-divider>

					<v-card-actions class="pa-4">
						<v-btn
							variant="text"
							color="default"
							prepend-icon="mdi-arrow-left"
							@click="showLeaveConfirm = false"
						>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useIntervalFn } from '@vueuse/core'
import VideoGrid from '../components/VideoGrid.vue'
import ScreenShare from '../components/ScreenShare.vue'
import ParticipantsList from '../components/ParticipantsList.vue'
import ChatPanel from '../components/ChatPanel.vue'
import ControlBar from '../components/ControlBar.vue'
import LoadingOverlay from '@/features/shared/LoadingOverlay.vue'
import { useMediaDevices } from '@/composables/useMediaDevices'
import { useWebRTC } from '@/composables/useWebRTC'
import { useNotification } from '@/composables/useNotification'

const route = useRoute()
const router = useRouter()
const { showSuccess, showError } = useNotification()

// 会议信息
const meetingInfo = ref({
	id: route.params.id,
	title: '视频会议',
	startTime: new Date(),
})

// 媒体设备
const { cameras, microphones, speakers, selectedCamera, selectedMicrophone, selectedSpeaker, enumerateDevices } =
	useMediaDevices()

// WebRTC
const { localStream, participants, screenShare, audioEnabled, videoEnabled, screenSharing, joinMeeting, leaveMeeting } =
	useWebRTC()

// UI状态
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
const participantCount = computed(() => participants.value.length + 1)
const currentUserId = ref('current-user-id')

// 根据控制栏状态动态计算视频容器高度
const videoContainerHeight = computed(() => {
	// 顶部栏: 48px
	// 控制栏展开: 88px, 收起: 0px
	const topBarHeight = 48
	const controlBarHeight = controlBarCollapsed.value ? 0 : 88
	return `calc(100vh - ${topBarHeight}px - ${controlBarHeight}px)`
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

// 方法
const toggleSidebarChat = () => {
	showSidebar.value = true
	sidebarTab.value = 'chat'
}

const toggleVideoLayout = () => {
	const layouts = ['grid', 'spotlight', 'sidebar']
	const currentIndex = layouts.indexOf(videoLayout.value)
	videoLayout.value = layouts[(currentIndex + 1) % layouts.length]
	showSuccess(`已切换到${layouts[(currentIndex + 1) % layouts.length]}布局`)
}

// 显示离开确认对话框
const handleLeaveMeeting = () => {
	showLeaveConfirm.value = true
}

const confirmLeaveMeeting = async () => {
	showLeaveConfirm.value = false

	try {
		// 清理WebRTC连接和媒体流
		await leaveMeeting()

		router.push('/')

		showSuccess('已离开会议')
	} catch (error) {
		console.error('Failed to leave meeting:', error)
		showError('离开会议失败')

		// 即使出错也尝试跳转
		router.push({
			path: `/meeting/detail/${meetingInfo.value.id}`,
		})
	}
}

const handleSendMessage = async message => {
	chatMessages.value.push({
		id: Date.now(),
		userId: currentUserId.value,
		userName: '我',
		content: message.content,
		type: message.type || 'text',
		timestamp: new Date(),
		isOwn: true,
	})
}

const handleMessageRead = () => {
	unreadMessages.value = 0
}

const handleLoadMoreMessages = async () => {
	// 预留加载更多消息API
	console.log('Load more messages')
}

const handleFileUpload = async fileData => {
	chatMessages.value.push({
		id: Date.now(),
		userId: currentUserId.value,
		userName: '我',
		type: 'file',
		file: fileData.file,
		timestamp: new Date(),
		isOwn: true,
	})
}

const handleMuteParticipant = async participantId => {
	console.log('Mute participant:', participantId)
}

const handleRemoveParticipant = async participantId => {
	console.log('Remove participant:', participantId)
}

const handlePinParticipant = participantId => {
	console.log('Pin participant:', participantId)
}

const handleSpotlightParticipant = participantId => {
	console.log('Spotlight participant:', participantId)
}

const saveSettings = () => {
	// 预留保存设置API
	showSettings.value = false
	showSuccess('设置已保存')
}

onMounted(async () => {
	isLoading.value = true
	loadingMessage.value = '正在加入会议...'

	try {
		await enumerateDevices()
		await joinMeeting(meetingInfo.value.id)
		showSuccess('已加入会议')
	} catch (error) {
		console.error('Failed to join meeting:', error)
		showError('加入会议失败')
	} finally {
		isLoading.value = false
	}
})

onUnmounted(() => {
	leaveMeeting()
})
</script>

<style scoped>
.meeting-room {
	background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
	height: 100vh;
	overflow: hidden;
}

.meeting-header {
	background: linear-gradient(to bottom, rgba(30, 30, 46, 0.95) 0%, rgba(30, 30, 46, 0.85) 100%) !important;
	backdrop-filter: blur(10px);
	border-bottom: 1px solid rgba(255, 255, 255, 0.08);
	color: rgba(255, 255, 255, 0.95);
}

.video-container {
	transition: height 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.video-main {
	position: relative;
	background: #000;
}

.sidebar-container {
	display: flex;
	flex-direction: column;
	height: 100%;
}

.sidebar {
	background: linear-gradient(to bottom, rgba(40, 40, 58, 0.98) 0%, rgba(35, 35, 51, 0.98) 100%);
	backdrop-filter: blur(10px);
	border-left: 1px solid rgba(255, 255, 255, 0.08);
	display: flex;
	flex-direction: column;
	height: 100%;
	box-shadow: -4px 0 24px rgba(0, 0, 0, 0.3);
	overflow: hidden;
}

.sidebar-tabs {
	flex-shrink: 0;
	border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar-tabs :deep(.v-tab) {
	color: rgba(255, 255, 255, 0.7);
	text-transform: none;
	letter-spacing: 0.5px;
	min-height: 48px;
}

.sidebar-tabs :deep(.v-tab--selected) {
	color: rgba(255, 255, 255, 0.95);
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

.settings-dialog {
	background: linear-gradient(to bottom, rgba(40, 40, 58, 0.98) 0%, rgba(35, 35, 51, 0.98) 100%);
	backdrop-filter: blur(10px);
	color: rgba(255, 255, 255, 0.95);
}

.fill-height {
	height: 100%;
}

.h-100 {
	height: 100%;
}
/* 离开会议确认对话框样式 */
.leave-confirm-dialog {
	backdrop-filter: blur(20px);
	border: 1px solid rgba(255, 255, 255, 0.08);
	box-shadow: 0 24px 48px rgba(0, 0, 0, 0.5);
	overflow: hidden;
}

.warning-stripe {
	height: 4px;
	background: linear-gradient(90deg, #ff9800 0%, #f44336 100%);
}

.warning-icon-wrapper {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 56px;
	height: 56px;
	border-radius: 12px;
	background: linear-gradient(135deg, rgba(255, 152, 0, 0.15) 0%, rgba(244, 67, 54, 0.15) 100%);
	border: 2px solid rgba(255, 152, 0, 0.3);
}

.info-card {
	background: rgba(33, 150, 243, 0.1) !important;
	border: 1px solid rgba(33, 150, 243, 0.3);
	border-radius: 8px;
}
.stats-grid {
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 12px;
	padding: 12px;
	border-radius: 8px;
	border: 1px solid rgba(255, 255, 255, 0.05);
}

.stat-item {
	display: flex;
	align-items: center;
	padding: 8px 12px;
	background: rgba(255, 255, 255, 0.02);
	border-radius: 6px;
	transition: all 0.2s;
}

.stat-item:hover {
	background: rgba(255, 255, 255, 0.05);
	transform: translateY(-1px);
}

.leave-btn {
	background: linear-gradient(135deg, #f44336 0%, #d32f2f 100%) !important;
	box-shadow: 0 4px 12px rgba(244, 67, 54, 0.4);
	font-weight: 600;
	letter-spacing: 0.5px;
}

.leave-btn:hover {
	background: linear-gradient(135deg, #e53935 0%, #c62828 100%) !important;
	box-shadow: 0 6px 16px rgba(244, 67, 54, 0.5);
	transform: translateY(-2px);
}

.leave-btn:active {
	transform: translateY(0);
}

/* 响应式调整 */
@media (max-width: 600px) {
	.stats-grid {
		grid-template-columns: 1fr;
	}

	.warning-icon-wrapper {
		width: 48px;
		height: 48px;
	}
}

/* 提升文本对比度 */
:deep(.v-card-title),
:deep(.v-list-item-title) {
	color: rgba(255, 255, 255, 0.95) !important;
}

:deep(.v-list-item-subtitle),
:deep(.text-caption) {
	color: rgba(255, 255, 255, 0.7) !important;
}

:deep(.text-medium-emphasis) {
	color: rgba(255, 255, 255, 0.6) !important;
}

/* 输入框优化 */
:deep(.v-field) {
	background-color: rgba(255, 255, 255, 0.05);
	border-color: rgba(255, 255, 255, 0.12);
}

:deep(.v-field--focused) {
	background-color: rgba(255, 255, 255, 0.08);
}

:deep(.v-field__input) {
	color: rgba(255, 255, 255, 0.95);
}

/* 滑块优化 */
:deep(.v-slider__tick-label) {
	color: rgba(255, 255, 255, 0.7);
}
</style>
