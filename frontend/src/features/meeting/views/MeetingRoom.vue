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

				<!-- 网络状态指示 -->
				<v-tooltip location="bottom">
					<template #activator="{ props }">
						<v-chip v-bind="props" :color="networkQuality.color" variant="tonal" size="small">
							<template #prepend>
								<v-icon :icon="networkQuality.icon" size="small"></v-icon>
							</template>
							{{ networkQuality.text }}
						</v-chip>
					</template>
					<div class="text-caption">
						<div>发送质量: {{ connectionQuality.send.quality }}</div>
						<div>接收质量: {{ connectionQuality.recv.quality }}</div>
						<div v-if="effectiveType">网络类型: {{ effectiveType }}</div>
					</div>
				</v-tooltip>

				<v-chip color="success" variant="flat" size="small" class="mr-4">
					<template #prepend>
						<v-icon icon="mdi-account-multiple" size="small"></v-icon>
					</template>
					<span class="font-weight-medium">{{ participantCount }} 人</span>
				</v-chip>

				<v-btn icon="mdi-cog" variant="text" size="small" @click="showSettings = true"></v-btn>
			</v-app-bar>

			<!-- 主视频区域 -->
			<v-container fluid class="video-container" :style="{ height: videoContainerHeight }">
				<v-row no-gutters class="fill-height">
					<!-- 视频网格 -->
					<v-col :cols="showSidebar ? 9 : 12" class="video-main">
						<VideoGrid
							:participants="participants"
							:screen-share="screenShare"
							:layout="videoLayout"
							:local-stream="localStream"
							:local-audio-enabled="audioEnabled"
							:local-video-enabled="videoEnabled"
							:show-connection-quality="true"
							@pin-participant="handlePinParticipant"
							@unpin-participant="handleUnpinParticipant"
						/>

						<!-- 网络断开提示 -->
						<v-fade-transition>
							<div v-if="!online" class="connection-overlay">
								<v-icon icon="mdi-wifi-off" size="64" color="primary"></v-icon>
								<div class="text-h6 mt-4 text-on-primary">网络连接已断开</div>
								<v-btn variant="flat" color="primary" class="mt-4" @click="handleReconnect">
									重新连接
								</v-btn>
							</div>
						</v-fade-transition>
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
								<!-- 参与者列表 -->
								<v-tabs-window-item value="participants" class="fill-height">
									<ParticipantsList
										:participants="participants"
										:current-user-id="currentUserId"
										:meeting-id="meetingInfo.roomId"
										@mute-participant="handleMuteParticipant"
										@remove-participant="handleRemoveParticipant"
										@pin-participant="handlePinParticipant"
										@spotlight-participant="handleSpotlightParticipant"
									/>
								</v-tabs-window-item>

								<!-- 聊天面板 -->
								<v-tabs-window-item value="chat" class="fill-height">
									<div class="chat-panel">
										<!-- 聊天标题栏 -->
										<div class="chat-header">
											<span class="text-subtitle-1 font-weight-medium">会议聊天</span>
											<v-menu>
												<template #activator="{ props }">
													<v-btn
														icon="mdi-dots-vertical"
														variant="text"
														size="small"
														v-bind="props"
													></v-btn>
												</template>
												<v-list density="compact" bg-color="surface">
													<v-list-item @click="handleSaveChat">
														<template #prepend>
															<v-icon icon="mdi-download"></v-icon>
														</template>
														<v-list-item-title>保存聊天记录</v-list-item-title>
													</v-list-item>
													<v-list-item @click="handleClearChat">
														<template #prepend>
															<v-icon icon="mdi-delete"></v-icon>
														</template>
														<v-list-item-title>清空聊天</v-list-item-title>
													</v-list-item>
												</v-list>
											</v-menu>
										</div>

										<v-divider></v-divider>

										<!-- 消息列表 -->
										<div ref="messageContainer" class="message-container">
											<!-- 加载更多按钮 -->
											<div v-if="hasMoreMessages" class="load-more-wrapper">
												<v-btn
													variant="text"
													size="small"
													:loading="loadingMore"
													@click="handleLoadMoreMessages"
												>
													加载更多消息
												</v-btn>
											</div>

											<!-- 消息分组 -->
											<div v-for="(group, date) in groupedMessages" :key="date" class="mb-4">
												<!-- 日期分隔符 -->
												<div class="date-divider">
													<span class="date-text">{{ formatDate(date) }}</span>
												</div>

												<!-- 消息列表 -->
												<div
													v-for="message in group"
													:key="message.id"
													class="message-wrapper"
													:class="{ 'd-flex justify-end': message.isOwn }"
												>
													<div class="message-content">
														<!-- 他人消息头部 -->
														<div
															v-if="!message.isOwn"
															class="d-flex align-center mb-2 px-1"
														>
															<v-avatar size="28" color="primary"
																><span class="text-caption">{{
																	getInitials(message.userName)
																}}</span></v-avatar
															>
															<span class="message-sender ml-2">{{
																message.userName
															}}</span>
															<span class="message-time ml-2">{{
																formatTime(message.timestamp)
															}}</span>
														</div>

														<div
															class="message-bubble"
															:class="{ 'message-own-bubble': message.isOwn }"
														>
															<!-- 文本消息 -->
															<div v-if="message.type === 'text'" class="message-text">
																{{ message.content }}
															</div>

															<!-- 文件消息 -->
															<div
																v-else-if="message.type === 'file'"
																class="message-file"
															>
																<v-icon left size="20">{{
																	getFileIcon(message.file.type)
																}}</v-icon>
																<div class="file-info">
																	<div class="file-name">{{ message.file.name }}</div>
																	<div class="file-size text-caption">
																		{{ formatFileSize(message.file.size) }}
																	</div>
																</div>
																<v-btn
																	icon="mdi-download"
																	size="x-small"
																	variant="text"
																	@click="downloadFile(message.file)"
																></v-btn>
															</div>

															<!-- 自己消息的时间 -->
															<div v-if="message.isOwn" class="message-time-own">
																{{ formatTime(message.timestamp) }}
															</div>
														</div>
													</div>
												</div>
											</div>

											<!-- 正在输入指示器 -->
											<div v-if="typingUsers.length > 0" class="typing-indicator">
												<v-avatar size="24" color="grey">
													<span class="typing-dots">...</span>
												</v-avatar>
												<span class="ml-2 text-caption">
													{{ typingUsers.join(', ') }} 正在输入...
												</span>
											</div>

											<!-- 空状态 -->
											<div
												v-if="chatMessages.length === 0"
												class="d-flex flex-column align-center justify-center fill-height text-on-surface-variant"
											>
												<v-icon
													icon="mdi-chat-outline"
													size="64"
													color="grey"
													class="mb-4"
												></v-icon>
												<div class="text-body-2 text-grey">暂无消息</div>
												<div class="text-caption text-grey-darken-1">发送消息开始聊天</div>
											</div>
										</div>

										<!-- 输入区域 -->
										<v-divider></v-divider>

										<div class="input-area">
											<!-- 文件上传进度 -->
											<v-progress-linear
												v-if="uploadProgress > 0"
												v-model="uploadProgress"
												color="primary"
												height="3"
												class="mb-2 rounded"
											></v-progress-linear>

											<!-- 消息输入框 -->
											<div class="d-flex align-end ga-2">
												<v-btn
													icon="mdi-emoticon-happy-outline"
													variant="text"
													size="small"
													class="emoji-btn"
												></v-btn>

												<v-textarea
													v-model="messageInput"
													variant="outlined"
													density="compact"
													placeholder="输入消息..."
													hide-details
													rows="1"
													auto-grow
													max-rows="4"
													class="message-input"
													@keydown.enter.exact.prevent="sendChatMessage"
													@keydown.shift.enter.exact="addNewLine"
													@input="handleTyping"
												></v-textarea>

												<input ref="fileInput" type="file" hidden @change="handleFileSelect" />

												<v-btn
													icon="mdi-paperclip"
													variant="text"
													size="small"
													class="attach-btn"
													@click="$refs.fileInput?.click()"
												></v-btn>

												<v-btn
													icon="mdi-send"
													variant="flat"
													color="primary"
													size="small"
													:disabled="!messageInput.trim()"
													class="send-btn"
													@click="sendChatMessage"
												></v-btn>
											</div>
										</div>
									</div>
								</v-tabs-window-item>
							</v-tabs-window>
						</div>
					</v-col>
				</v-row>
			</v-container>

			<!-- 底部控制栏 -->
			<div class="control-bar-wrapper" :class="{ collapsed: controlBarCollapsed }">
				<!-- 收起/展开按钮 -->
				<v-btn
					icon
					variant="elevated"
					color="primary"
					size="small"
					class="collapse-toggle"
					@click="controlBarCollapsed = !controlBarCollapsed"
				>
					<v-icon>{{ controlBarCollapsed ? 'mdi-chevron-up' : 'mdi-chevron-down' }}</v-icon>
				</v-btn>

				<!-- 控制栏主体 -->
				<transition name="slide-up">
					<div v-show="!controlBarCollapsed" class="control-bar">
						<v-container fluid class="pa-0">
							<v-row no-gutters align="center" justify="center">
								<!-- 左侧：连接状态 -->
								<v-col cols="auto" class="d-flex align-center">
									<v-chip
										:color="overallConnectionQuality.color"
										variant="flat"
										size="small"
										class="ml-4"
									>
										<template #prepend>
											<v-icon size="small">{{ overallConnectionQuality.icon }}</v-icon>
										</template>
										{{ overallConnectionQuality.text }}
									</v-chip>
								</v-col>

								<v-spacer></v-spacer>

								<!-- 中间：主要控制按钮 -->
								<v-col cols="auto">
									<div class="d-flex ga-3 align-center">
										<!-- 音频控制 -->
										<v-tooltip location="top">
											<template #activator="{ props }">
												<v-btn
													v-bind="props"
													:icon="audioEnabled ? 'mdi-microphone' : 'mdi-microphone-off'"
													:color="audioEnabled ? 'surface' : 'error'"
													:variant="audioEnabled ? 'elevated' : 'flat'"
													size="large"
													class="control-btn"
													@click="audioEnabled = !audioEnabled"
												></v-btn>
											</template>
											<span>{{ audioEnabled ? '静音' : '取消静音' }}</span>
										</v-tooltip>

										<!-- 视频控制 -->
										<v-tooltip location="top">
											<template #activator="{ props }">
												<v-btn
													v-bind="props"
													:icon="videoEnabled ? 'mdi-video' : 'mdi-video-off'"
													:color="videoEnabled ? 'surface' : 'error'"
													:variant="videoEnabled ? 'elevated' : 'flat'"
													size="large"
													class="control-btn"
													@click="videoEnabled = !videoEnabled"
												></v-btn>
											</template>
											<span>{{ videoEnabled ? '关闭摄像头' : '开启摄像头' }}</span>
										</v-tooltip>

										<!-- 屏幕共享 -->
										<v-tooltip location="top">
											<template #activator="{ props }">
												<v-btn
													v-bind="props"
													:icon="screenSharing ? 'mdi-monitor-off' : 'mdi-monitor-share'"
													:color="screenSharing ? 'success' : 'surface'"
													:variant="screenSharing ? 'flat' : 'elevated'"
													:disabled="!screenSharing && hasScreenShare"
													size="large"
													class="control-btn"
													@click="handleScreenShareToggle"
												></v-btn>
											</template>
											<span>
												{{
													screenSharing
														? '停止共享'
														: hasScreenShare
															? '已有人在共享'
															: '共享屏幕'
												}}
											</span>
										</v-tooltip>

										<!-- 录制 -->
										<v-tooltip location="top">
											<template #activator="{ props }">
												<v-btn
													v-bind="props"
													:icon="isRecording ? 'mdi-record-rec' : 'mdi-record-circle-outline'"
													:color="isRecording ? 'error' : 'surface'"
													:variant="isRecording ? 'flat' : 'elevated'"
													size="large"
													class="control-btn"
													@click="toggleRecording"
												></v-btn>
											</template>
											<span>{{ isRecording ? '停止录制' : '开始录制' }}</span>
										</v-tooltip>

										<!-- 更多选项 -->
										<v-menu location="top">
											<template #activator="{ props: menuProps }">
												<v-tooltip location="top">
													<template #activator="{ props: tooltipProps }">
														<v-btn
															v-bind="mergeProps(menuProps, tooltipProps)"
															icon="mdi-dots-horizontal"
															variant="elevated"
															color="surface"
															size="large"
															class="control-btn"
														></v-btn>
													</template>
													<span>更多选项</span>
												</v-tooltip>
											</template>

											<v-list density="compact" bg-color="surface">
												<v-list-item @click="toggleVideoLayout">
													<template #prepend>
														<v-icon icon="mdi-view-grid"></v-icon>
													</template>
													<v-list-item-title>切换布局</v-list-item-title>
												</v-list-item>

												<v-list-item @click="toggleHandRaise">
													<template #prepend>
														<v-icon
															:icon="
																handRaised
																	? 'mdi-hand-back-right'
																	: 'mdi-hand-back-right-outline'
															"
															:color="handRaised ? 'warning' : undefined"
														></v-icon>
													</template>
													<v-list-item-title>
														{{ handRaised ? '放下手' : '举手' }}
													</v-list-item-title>
												</v-list-item>

												<v-list-item @click="showSettings = true">
													<template #prepend>
														<v-icon icon="mdi-cog"></v-icon>
													</template>
													<v-list-item-title>设置</v-list-item-title>
												</v-list-item>

												<v-divider></v-divider>

												<v-list-item @click="toggleFullscreen">
													<template #prepend>
														<v-icon
															:icon="
																isFullscreen ? 'mdi-fullscreen-exit' : 'mdi-fullscreen'
															"
														></v-icon>
													</template>
													<v-list-item-title>
														{{ isFullscreen ? '退出全屏' : '全屏' }}
													</v-list-item-title>
												</v-list-item>
											</v-list>
										</v-menu>

										<!-- 离开会议 -->
										<v-tooltip location="top">
											<template #activator="{ props }">
												<v-btn
													v-bind="props"
													icon="mdi-phone-hangup"
													variant="flat"
													color="error"
													size="large"
													class="control-btn leave-btn"
													@click="handleLeaveMeeting"
												></v-btn>
											</template>
											<span>离开会议</span>
										</v-tooltip>
									</div>
								</v-col>

								<v-spacer></v-spacer>

								<!-- 右侧：侧边栏和聊天 -->
								<v-col cols="auto" class="d-flex align-center">
									<v-tooltip location="top">
										<template #activator="{ props }">
											<v-btn
												v-bind="props"
												:icon="showSidebar ? 'mdi-dock-right' : 'mdi-dock-left'"
												:color="showSidebar ? 'primary' : 'surface'"
												:variant="showSidebar ? 'flat' : 'elevated'"
												size="large"
												class="mr-2"
												@click="showSidebar = !showSidebar"
											></v-btn>
										</template>
										<span>{{ showSidebar ? '隐藏侧边栏' : '显示侧边栏' }}</span>
									</v-tooltip>

									<v-badge
										:content="unreadMessages"
										:model-value="unreadMessages > 0"
										color="error"
										overlap
										class="mr-4"
									>
										<v-tooltip location="top">
											<template #activator="{ props }">
												<v-btn
													v-bind="props"
													icon="mdi-chat"
													variant="elevated"
													color="surface"
													size="large"
													@click="toggleSidebarChat"
												></v-btn>
											</template>
											<span>聊天</span>
										</v-tooltip>
									</v-badge>
								</v-col>
							</v-row>
						</v-container>
					</div>
				</transition>
			</div>

			<!-- 设置对话框 -->
			<v-dialog v-model="showSettings" max-width="600" transition="dialog-bottom-transition">
				<v-card>
					<v-card-title class="d-flex align-center justify-space-between pa-4">
						<span class="text-h6 font-weight-medium">会议设置</span>
						<v-btn icon="mdi-close" variant="text" size="small" @click="showSettings = false"></v-btn>
					</v-card-title>

					<v-divider></v-divider>

					<v-card-text class="pa-6" style="height: 600px">
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
						<v-btn variant="flat" color="primary" @click="saveSettings"> 保存设置 </v-btn>
					</v-card-actions>
				</v-card>
			</v-dialog>

			<!-- 离开会议确认对话框 -->
			<v-dialog v-model="showLeaveConfirm" max-width="480" persistent transition="dialog-bottom-transition">
				<v-card>
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
							<p class="text-body-2 text-medium-emphasis">会议时长: {{ meetingDuration }}</p>
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
							variant="flat"
							color="error"
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
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick, mergeProps } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
	useIntervalFn,
	useDateFormat,
	useScroll,
	useThrottleFn,
	useFullscreen,
	useEventListener,
	useNetwork,
	useOnline,
	useDebounceFn,
} from '@vueuse/core'
import VideoGrid from '../components/VideoGrid.vue'
import ScreenShare from '../components/ScreenShare.vue'
import ParticipantsList from '../components/ParticipantsList.vue'
import LoadingOverlay from '@/components/common/LoadingOverlay.vue'
import { useMediaDevices } from '@/composables/useMediaDevices'
import { useMedia } from '@/composables/useMedia'
import { fetchMeetingDetail, fetchMeetingInfo } from '@/api/room'
import { $notify } from '@/plugins/notification'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ==================== 基础信息 ====================
const meetingInfo = ref({
	roomId: '',
	roomNo: route.params.roomNo,
	title: '',
	hostId: '',
	hostName: '',
	type: null,
	maxParticipants: null,
	currentParticipants: null,
	startTime: new Date(),
})

const currentUserId = computed(() => userStore.userId)
const currentUsername = computed(() => userStore.profile.nickname)
const authToken = computed(() => userStore.token)

// ==================== 媒体设备 ====================
const {
	cameras,
	microphones,
	speakers,
	selectedCamera,
	selectedMicrophone,
	selectedSpeaker,
	enumerateDevices,
	switchDevice,
} = useMediaDevices()

// 监听设备变化
useEventListener('devicechange', async () => {
	console.log('Media devices changed')
	await enumerateDevices()
	$notify.info('检测到设备变化')
})

// ==================== WebRTC媒体管理 ====================
const {
	roomId,
	peerId,
	localStream,
	participants,
	remoteParticipants,
	localParticipant,
	audioEnabled,
	videoEnabled,
	screenSharing,
	screenStream,
	hasScreenShare,
	getScreenSharingParticipant,
	connectionState,
	connectionQuality,
	stats,
	joinMeeting,
	leaveMeeting,
	toggleAudio,
	toggleVideo,
	startScreenShare,
	stopScreenShare,
	changeAudioDevice,
	changeVideoDevice,
} = useMedia()

// ==================== 网络状态监控 ====================
const online = useOnline()
const networkState = useNetwork()

// 安全地解构，提供默认值
const effectiveType = computed(() => networkState.effectiveType?.value || '4g')
const downlink = computed(() => networkState.downlink?.value || 10)

// 使用防抖优化网络状态变化的处理
const handleNetworkChange = useDebounceFn(online => {
	if (!online) {
		$notify.error('网络连接已断开')
	} else {
		$notify.success('网络连接已恢复')
		// 尝试重新连接
		if (connectionState.value === 'disconnected' && roomId.value) {
			handleReconnect()
		}
	}
}, 500)

// 监听网络状态 - 使用防抖函数
watch(online, handleNetworkChange)

// 网络质量计算
const networkQuality = computed(() => {
	if (!online) {
		return { text: '离线', color: 'error', icon: 'mdi-wifi-off' }
	}

	const type = effectiveType.value
	const speed = downlink.value

	if (type === '4g' && speed > 5) {
		return { text: '网络优秀', color: 'success', icon: 'mdi-wifi-strength-4' }
	} else if (type === '3g' || speed > 1) {
		return { text: '网络良好', color: 'info', icon: 'mdi-wifi-strength-3' }
	} else {
		return { text: '网络较差', color: 'warning', icon: 'mdi-wifi-strength-1' }
	}
})

// UI 状态
const showSidebar = ref(true)
const showSettings = ref(false)
const sidebarTab = ref('participants')
const videoLayout = ref('grid')
const isLoading = ref(false)
const loadingMessage = ref('')
const loadingProgress = ref(0)
const controlBarCollapsed = ref(false)
const showLeaveConfirm = ref(false)

// 视频设置
const videoQuality = ref(2)
const enableHD = ref(true)
const enableMirror = ref(false)

// ==================== 聊天功能 ====================
const messageContainer = ref(null)
const fileInput = ref(null)
const messageInput = ref('')
const chatMessages = ref([])
const unreadMessages = ref(0)
const typingUsers = ref([])
const hasMoreMessages = ref(false)
const loadingMore = ref(false)
const uploadProgress = ref(0)

// 滚动控制
const { arrivedState } = useScroll(messageContainer, {
	offset: { bottom: 50 },
})

// 自动滚动到底部
const scrollToBottom = async () => {
	await nextTick()
	if (messageContainer.value) {
		messageContainer.value.scrollTop = messageContainer.value.scrollHeight
	}
}

// 监听滚动到底部时标记已读
watch(
	() => arrivedState.bottom,
	isBottom => {
		if (isBottom && unreadMessages.value > 0) {
			unreadMessages.value = 0
		}
	},
)

// 监听新消息自动滚动
watch(
	() => chatMessages.value.length,
	() => {
		if (sidebarTab.value === 'chat' || arrivedState.bottom) {
			scrollToBottom()
		} else {
			unreadMessages.value++
		}
	},
)

// 按日期分组消息
const groupedMessages = computed(() => {
	const groups = {}
	chatMessages.value.forEach(msg => {
		const date = useDateFormat(msg.timestamp, 'YYYY-MM-DD').value
		if (!groups[date]) groups[date] = []
		groups[date].push({
			...msg,
			isOwn: msg.userId === currentUserId.value,
		})
	})
	return groups
})

// 输入节流
const handleTyping = useThrottleFn(() => {
	// TODO: 通知其他人正在输入
	console.log('User is typing...')
}, 1000)

// ==================== 会议控制 ====================
const isRecording = ref(false)
const handRaised = ref(false)
const { isFullscreen, toggle: toggleFullscreen } = useFullscreen()

// 会议时长统计
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

// ==================== 计算属性 ====================
const participantCount = computed(() => participants.value.length)

const videoContainerHeight = computed(() => {
	const topBarHeight = 48
	const controlBarHeight = controlBarCollapsed.value ? 0 : 88
	return `calc(100vh - ${topBarHeight}px - ${controlBarHeight}px)`
})

// 屏幕共享
const screenShare = computed(() => {
	const sharingPeer = remoteParticipants.value.find(p => p.streams?.screen)
	const localSharing = screenSharing.value && localParticipant.value?.streams?.screen
	const peer = sharingPeer || (localSharing ? localParticipant.value : null)

	if (peer?.streams?.screen) {
		return {
			active: true,
			stream: peer.streams.screen,
			presenter: {
				id: peer.peerId,
				name: peer.username,
				isLocal: peer.peerId === peerId.value,
			},
			isLocal: peer.peerId === peerId.value,
		}
	}

	return {
		active: false,
		stream: null,
		presenter: null,
		isLocal: false,
	}
})

const handleScreenShareToggle = async () => {
	try {
		if (screenSharing.value) {
			await stopScreenShare()
		} else {
			// 检查是否已经有人在共享
			if (hasScreenShare.value) {
				$notify.warning('已有参与者正在共享屏幕')
				return
			}
			await startScreenShare()
		}
	} catch (error) {
		console.error('Screen share toggle failed', error)
	}
}

// 综合连接质量
const overallConnectionQuality = computed(() => {
	const sendScore = connectionQuality.value.send.score
	const recvScore = connectionQuality.value.recv.score
	const avgScore = (sendScore + recvScore) / 2

	let color = 'success'
	let icon = 'mdi-wifi-strength-4'
	let text = '连接优秀'

	if (avgScore < 4) {
		color = 'error'
		icon = 'mdi-wifi-strength-1'
		text = '连接较差'
	} else if (avgScore < 6) {
		color = 'warning'
		icon = 'mdi-wifi-strength-2'
		text = '连接一般'
	} else if (avgScore < 8) {
		color = 'info'
		icon = 'mdi-wifi-strength-3'
		text = '连接良好'
	}

	return { color, icon, text }
})

// ==================== 工具函数 ====================
const formatDate = date => {
	const today = useDateFormat(new Date(), 'YYYY-MM-DD').value
	const yesterday = useDateFormat(new Date(Date.now() - 86400000), 'YYYY-MM-DD').value

	if (date === today) return '今天'
	if (date === yesterday) return '昨天'
	return useDateFormat(date, 'MM月DD日').value
}

const formatTime = timestamp => useDateFormat(timestamp, 'HH:mm').value

const getInitials = name => {
	return name
		.split(' ')
		.map(word => word[0])
		.join('')
		.toUpperCase()
		.slice(0, 2)
}

const getFileIcon = fileType => {
	const iconMap = {
		image: 'mdi-file-image',
		video: 'mdi-file-video',
		audio: 'mdi-file-music',
		pdf: 'mdi-file-pdf-box',
		document: 'mdi-file-document',
		archive: 'mdi-folder-zip',
	}
	return iconMap[fileType] || 'mdi-file'
}

const formatFileSize = bytes => {
	if (bytes === 0) return '0 B'
	const k = 1024
	const sizes = ['B', 'KB', 'MB', 'GB']
	const i = Math.floor(Math.log(bytes) / Math.log(k))
	return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

// ==================== 事件处理 ====================

// 音视频控制
watch(audioEnabled, async enabled => {
	console.log('Audio state changed', enabled)
	await toggleAudio()
})

watch(videoEnabled, async enabled => {
	console.log('Video state changed', enabled)
	await toggleVideo()
})

// 设备切换
watch(selectedCamera, async deviceId => {
	if (deviceId && localStream.value) {
		await changeVideoDevice(deviceId)
		$notify.success('已切换摄像头')
	}
})

watch(selectedMicrophone, async deviceId => {
	if (deviceId && localStream.value) {
		await changeAudioDevice(deviceId)
		$notify.success('已切换麦克风')
	}
})

// 连接状态监控
watch(connectionState, state => {
	if (state === 'failed') {
		$notify.error('连接失败，请检查网络')
	} else if (state === 'disconnected') {
		console.log('连接断开')
	} else if (state === 'connected') {
		console.log('连接已建立')
	}
})

// ==================== 聊天功能 ====================
const sendChatMessage = async () => {
	if (!messageInput.value.trim()) return

	const message = {
		id: Date.now(),
		userId: currentUserId.value,
		userName: currentUsername.value,
		content: messageInput.value.trim(),
		type: 'text',
		timestamp: new Date(),
	}

	chatMessages.value.push(message)
	messageInput.value = ''

	// TODO: 通过Socket.io发送给其他参与者
	console.log('Send message:', message)
}

const addNewLine = () => {
	messageInput.value += '\n'
}

const handleFileSelect = async event => {
	const file = event.target.files[0]
	if (!file) return

	if (file.size > 10 * 1024 * 1024) {
		$notify.error('文件大小不能超过10MB')
		return
	}

	try {
		uploadProgress.value = 0
		const uploadInterval = setInterval(() => {
			uploadProgress.value += 10
			if (uploadProgress.value >= 100) {
				clearInterval(uploadInterval)
				setTimeout(() => {
					uploadProgress.value = 0
					chatMessages.value.push({
						id: Date.now(),
						userId: currentUserId.value,
						userName: currentUsername.value,
						type: 'file',
						file: {
							name: file.name,
							size: file.size,
							type: file.type.split('/')[0],
							url: URL.createObjectURL(file),
						},
						timestamp: new Date(),
					})
				}, 500)
			}
		}, 200)
	} catch (error) {
		$notify.error('文件上传失败')
		uploadProgress.value = 0
	}

	fileInput.value.value = ''
}

const downloadFile = file => {
	window.open(file.url, '_blank')
}

const handleSaveChat = () => {
	const chatText = chatMessages.value
		.map(m => `[${formatTime(m.timestamp)}] ${m.userName}: ${m.content || '[文件]'}`)
		.join('\n')

	const blob = new Blob([chatText], { type: 'text/plain' })
	const url = URL.createObjectURL(blob)
	const a = document.createElement('a')
	a.href = url
	a.download = `chat-${Date.now()}.txt`
	a.click()
	URL.revokeObjectURL(url)
	$notify.success('聊天记录已保存')
}

const handleClearChat = () => {
	if (confirm('确定要清空所有聊天记录吗?')) {
		chatMessages.value = []
		$notify.success('聊天记录已清空')
	}
}

const handleLoadMoreMessages = async () => {
	loadingMore.value = true
	// TODO: 加载更多消息
	setTimeout(() => {
		loadingMore.value = false
		hasMoreMessages.value = false
	}, 1000)
}

// ==================== 会议控制 ====================
const toggleSidebarChat = () => {
	showSidebar.value = true
	sidebarTab.value = 'chat'
	unreadMessages.value = 0
}

const toggleVideoLayout = () => {
	const layouts = ['grid', 'spotlight', 'sidebar']
	const currentIndex = layouts.indexOf(videoLayout.value)
	videoLayout.value = layouts[(currentIndex + 1) % layouts.length]
	$notify.success(`已切换到${videoLayout.value}布局`)
}

const toggleRecording = () => {
	isRecording.value = !isRecording.value
	$notify.info(isRecording.value ? '开始录制' : '停止录制')
	// TODO: 实现录制功能
}

const toggleHandRaise = () => {
	handRaised.value = !handRaised.value
	$notify.info(handRaised.value ? '已举手' : '已放下手')
	// TODO: 通知其他参与者
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
	} catch (error) {
		console.error('Failed to leave meeting', error)
		$notify.error('离开会议失败')
		router.push('/')
	} finally {
		isLoading.value = false
	}
}

// ==================== 参与者管理 ====================
const handleMuteParticipant = async participantId => {
	console.log('Mute participant', participantId)
	$notify.info('该功能需要主持人权限')
	// TODO: 实现远程静音
}

const handleRemoveParticipant = async participantId => {
	console.log('Remove participant', participantId)
	$notify.info('该功能需要主持人权限')
	// TODO: 实现踢出参与者
}

const handlePinParticipant = participantId => {
	console.log('Pin participant', participantId)
	$notify.success('已固定参与者视图')
	// TODO: 实现固定视图
}
const handleUnpinParticipant = participantId => {
	console.log('Unpin participant', participantId)
	$notify.info('已取消固定')
	// TODO: 实现取消固定逻辑
}

const handleSpotlightParticipant = participantId => {
	console.log('Spotlight participant', participantId)
	videoLayout.value = 'spotlight'
	$notify.success('已切换到聚光灯模式')
	// TODO: 实现聚光灯
}

// ==================== 设置管理 ====================
const saveSettings = async () => {
	try {
		// 应用视频质量设置
		if (videoQuality.value !== 2) {
			console.log('Apply video quality:', videoQuality.value)
		}

		showSettings.value = false
		$notify.success('设置已保存')
	} catch (error) {
		console.error('Failed to save settings', error)
		$notify.error('保存设置失败')
	}
}

// ==================== 重连处理 ====================
const handleReconnect = async () => {
	if (!roomId.value || connectionState.value === 'connecting') return

	try {
		$notify.info('正在重新连接...')
		await joinMeeting(roomId.value, currentUserId.value, currentUsername.value, authToken.value)
		$notify.success('重连成功')
	} catch (error) {
		console.error('Reconnect failed', error)
		$notify.error('重连失败')
	}
}

// ==================== 生命周期 ====================
const loadMeetingDetail = async () => {
	try {
		const { data } = await fetchMeetingInfo(meetingInfo.value.roomNo)
		meetingInfo.value = data
	} catch (error) {
		console.error('Failed to load meeting detail', error)
		throw error
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
		await joinMeeting(meetingInfo.value.roomId, currentUserId.value, currentUsername.value, authToken.value)

		loadingProgress.value = 100
		meetingStartTime.value = Date.now()
	} catch (error) {
		console.error('Failed to join meeting', error)
		$notify.error(`加入会议失败: ${error.message}`)
	} finally {
		isLoading.value = false
		loadingProgress.value = 0
	}
})

onBeforeUnmount(async () => {
	await leaveMeeting()
})

// 监听页面刷新/关闭
useEventListener('beforeunload', e => {
	if (connectionState.value === 'connected') {
		e.preventDefault()
		e.returnValue = '确定要离开会议吗？'
	}
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
	padding: 0;
}

.video-main {
	height: 100%;
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

/* 聊天面板 */
.chat-panel {
	display: flex;
	flex-direction: column;
	height: 100%;
	background: transparent;
	overflow: hidden;
}

.chat-header {
	display: flex;
	flex-shrink: 0;
	align-items: center;
	justify-content: space-between;
	padding: 12px 16px;
	background: rgb(var(--v-theme-surface-variant));
}

.message-container {
	flex: 1;
	overflow-y: auto;
	overflow-x: hidden;
	padding: 16px;
	min-height: 0;
	max-height: 100%;
}

.message-container::-webkit-scrollbar {
	width: 6px;
}

.message-container::-webkit-scrollbar-track {
	background: rgb(var(--v-theme-surface-variant));
	border-radius: 3px;
}

.message-container::-webkit-scrollbar-thumb {
	background: rgb(var(--v-theme-primary));
	border-radius: 3px;
	opacity: 0.5;
}

.message-container::-webkit-scrollbar-thumb:hover {
	opacity: 0.8;
}

.load-more-wrapper {
	text-align: center;
	margin-bottom: 16px;
}

.date-divider {
	text-align: center;
	margin: 16px 0;
	position: relative;
}

.date-divider::before {
	content: '';
	position: absolute;
	top: 50%;
	left: 0;
	right: 0;
	height: 1px;
	background: rgb(var(--v-theme-border));
}

.date-text {
	position: relative;
	padding: 4px 12px;
	background: rgb(var(--v-theme-surface-variant));
	border-radius: 12px;
	font-size: 11px;
	color: rgb(var(--v-theme-on-surface-variant));
	font-weight: 500;
	letter-spacing: 0.5px;
}

.message-wrapper {
	margin-bottom: 12px;
	animation: fadeIn 0.2s ease-in;
}

@keyframes fadeIn {
	from {
		opacity: 0;
		transform: translateY(10px);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}

.message-content {
	max-width: 75%;
}

.message-sender {
	font-weight: 500;
	font-size: 13px;
	color: rgb(var(--v-theme-on-surface));
}

.message-time {
	font-size: 11px;
	color: rgb(var(--v-theme-on-surface-variant));
}

.message-bubble {
	padding: 10px 14px;
	border-radius: 16px;
	background: rgb(var(--v-theme-surface-variant));
	color: rgb(var(--v-theme-on-surface));
	word-wrap: break-word;
	box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
	transition: background-color 0.2s;
}

.message-bubble:hover {
	opacity: 0.9;
}

.message-own-bubble {
	background: rgb(var(--v-theme-primary));
	color: rgb(var(--v-theme-on-primary));
	border-bottom-right-radius: 4px;
}

.message-wrapper:not(.message-own) .message-bubble {
	border-bottom-left-radius: 4px;
}

.message-text {
	word-wrap: break-word;
	white-space: pre-wrap;
	line-height: 1.5;
	font-size: 14px;
}

.message-file {
	display: flex;
	align-items: center;
	gap: 10px;
	padding: 8px;
	background: rgba(var(--v-theme-surface), 0.5);
	border-radius: 10px;
}

.file-info {
	flex: 1;
	min-width: 0;
}

.file-name {
	font-size: 13px;
	font-weight: 500;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.file-size {
	opacity: 0.7;
	font-size: 11px;
	margin-top: 2px;
}

.message-time-own {
	font-size: 10px;
	color: rgba(var(--v-theme-on-primary), 0.7);
	text-align: right;
	margin-top: 4px;
}

.typing-indicator {
	display: flex;
	align-items: center;
	padding: 8px;
	color: rgb(var(--v-theme-on-surface-variant));
	font-size: 12px;
}

.typing-dots {
	animation: typing 1.4s infinite;
	font-size: 16px;
	font-weight: bold;
}

@keyframes typing {
	0%,
	60%,
	100% {
		opacity: 0.3;
	}
	30% {
		opacity: 1;
	}
}

.input-area {
	flex-shrink: 0;
	background: rgb(var(--v-theme-surface));
	border-top: 1px solid rgb(var(--v-theme-border));
	padding: 12px;
}

.message-input {
	flex: 1;
	min-width: 0;
}

.message-input :deep(.v-field) {
	border-radius: 20px;
}

.message-input :deep(.v-field__input) {
	padding: 8px 16px;
	max-height: 120px;
	overflow-y: auto;
}

.emoji-btn,
.attach-btn {
	flex-shrink: 0;
}

.send-btn {
	flex-shrink: 0;
}
/* 底部控制栏动画 */
.slide-up-enter-active,
.slide-up-leave-active {
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-up-enter-from,
.slide-up-leave-to {
	opacity: 0;
	transform: translateY(20px);
}

/* 底部控制栏 */
.control-bar-wrapper {
	position: sticky; /* 改为 sticky */
	bottom: 0;
	left: 0;
	right: 0;
	z-index: 10;
	background: rgb(var(--v-theme-surface));
}

.control-bar-wrapper.collapsed .collapse-toggle {
	top: -18px;
}

.collapse-toggle {
	position: absolute;
	left: 50%;
	transform: translateX(-50%);
	top: -18px;
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
	z-index: 1;
}

.control-bar {
	background: rgb(var(--v-theme-surface));
	backdrop-filter: blur(10px);
	border-top: 1px solid rgb(var(--v-theme-border));
	padding: 16px 0;
	box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.1);
}

.control-btn {
	transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.control-btn:hover {
	transform: translateY(-2px);
}

.control-btn:active {
	transform: translateY(0);
}

.leave-btn {
	margin-left: 8px;
}

/* 对话框 */
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
