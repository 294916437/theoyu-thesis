<template>
	<v-app>
		<v-main class="meeting-room">
			<!-- 初始化遮罩层 -->
			<MeetingEntryOverlay
				:phase="entryPhase"
				:loading-message="loadingMessage"
				:loading-progress="loadingProgress"
				:meeting-info="meetingInfo"
				:is-retrying="isRetrying"
				@confirm="handleMediaConsent"
				@retry="handleRetryJoin"
				@timeup="handleTimeup"
			/>
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
				<!-- 会议人数显示 -->
				<v-chip color="success" variant="flat" size="small" class="mr-4">
					<template #prepend>
						<v-icon icon="mdi-account-multiple" size="small"></v-icon>
					</template>
					<span class="font-weight-medium">{{ participantCount }} 人</span>
				</v-chip>
				<!-- 视频特效应用 -->
				<v-tooltip location="bottom">
					<template #activator="{ props }">
						<v-btn
							v-bind="props"
							icon="mdi-image-filter-hdr"
							variant="text"
							size="small"
							:color="effectProducerActive ? 'success' : undefined"
							:loading="effectLoading"
							class="mr-2"
							@click="toggleBackgroundPanel"
						>
							<!-- 显示激活状态指示器 -->
							<v-badge v-if="effectProducerActive" dot color="success" location="top end">
								<v-icon>mdi-image-filter-hdr</v-icon>
							</v-badge>
							<v-icon v-else>mdi-image-filter-hdr</v-icon>
						</v-btn>
					</template>
					<div>
						<div>背景特效</div>
						<div v-if="effectProducerActive" class="text-caption">当前: {{ effectType === 'blur' ? '背景虚化' : '虚拟背景' }}</div>
						<div v-if="effectLoading" class="text-caption text-warning">加载中...</div>
					</div>
				</v-tooltip>

				<v-btn icon="mdi-cog" variant="text" size="small" @click="showSettings = true"></v-btn>
			</v-app-bar>

			<!-- 主视频区域 -->
			<v-container fluid class="video-container" :style="{ height: videoContainerHeight }">
				<v-row no-gutters class="fill-height">
					<!-- 视频网格 -->
					<v-col :cols="showSidebar ? 9 : 12" class="video-main">
						<VideoGrid
							:participants="onlineParticipants"
							:screen-share="screenShare"
							:layout="videoLayout"
							:local-stream="localStream"
							:local-audio-enabled="audioEnabled"
							:local-video-enabled="videoEnabled"
							:show-connection-quality="true"
							:spotlight-peer-id="spotlightPeerId"
							:is-host="isHost"
							:local-peer-id="peerId"
							@pin-participant="handlePinParticipant"
							@unpin-participant="handleUnpinParticipant"
							@set-spotlight="handleSetSpotlight"
						/>

						<!-- 网络断开提示 -->
						<v-fade-transition>
							<div v-if="!online" class="connection-overlay">
								<v-icon icon="mdi-wifi-off" size="64" color="primary"></v-icon>
								<div class="text-h6 mt-4 text-on-primary">网络连接已断开</div>
								<v-btn variant="flat" color="primary" class="mt-4" @click="handleReconnect"> 重新连接 </v-btn>
							</div>
						</v-fade-transition>
					</v-col>

					<!-- 侧边栏 -->
					<v-col v-if="showSidebar" cols="3" class="sidebar-container">
						<div class="sidebar">
							<v-tabs v-model="sidebarTab" bg-color="surface" color="primary" density="compact" class="sidebar-tabs">
								<v-tab value="participants">
									<v-icon icon="mdi-account-multiple" size="small" class="mr-1"></v-icon>
									<span class="text-caption">参与者</span>
								</v-tab>

								<v-tab value="chat">
									<v-badge :content="unreadMessages" :model-value="unreadMessages > 0" color="error" inline>
										<v-icon icon="mdi-chat" size="small" class="mr-1"></v-icon>
										<span class="text-caption">聊天</span>
									</v-badge>
								</v-tab>
								<v-tab value="background">
									<v-icon icon="mdi-image-filter-hdr" size="small" class="mr-1"></v-icon>
									<span class="text-caption">背景</span>
								</v-tab>
							</v-tabs>

							<v-tabs-window v-model="sidebarTab" class="sidebar-content">
								<!-- 参与者列表 -->
								<v-tabs-window-item value="participants" class="fill-height">
									<ParticipantsList
										:participants="onlineParticipants"
										:is-host="isHost"
										:host-id="meetingInfo.hostId"
										:current-user-id="currentUserId"
										:meeting-no="meetingInfo.roomNo"
										:meeting-id="meetingInfo.roomId"
										:local-audio-enabled="audioEnabled"
										:local-video-enabled="videoEnabled"
										:spotlight-peer-id="spotlightPeerId"
										@host-toggle-audio="hostToggleAudio"
										@host-toggle-video="hostToggleVideo"
										@mute-all="muteAll"
										@disable-all-video="disableAllVideo"
										@remove-participant="removeParticipant"
										@set-spotlight="handleSetSpotlight"
									/>
								</v-tabs-window-item>

								<!-- 聊天面板 -->
								<v-tabs-window-item value="chat" class="fill-height">
									<div v-if="!chatMessages.length && loadingMore" class="d-flex align-center justify-center fill-height">
										<v-progress-circular indeterminate size="48" color="primary"></v-progress-circular>
										<span class="ml-4">加载聊天记录...</span>
									</div>
									<div class="chat-panel">
										<!-- 聊天标题栏 -->
										<div class="chat-header">
											<span class="text-subtitle-1 font-weight-medium">会议聊天</span>
											<v-menu>
												<template #activator="{ props }">
													<v-btn icon="mdi-dots-vertical" variant="text" size="small" v-bind="props"></v-btn>
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

										<!-- 消息容器 -->
										<div ref="messageContainer" class="message-container">
											<!-- 加载更多触发器 (无限滚动) -->
											<div v-if="hasMoreMessages" ref="loadMoreTrigger" class="load-more-trigger">
												<v-progress-circular v-if="loadingMore" indeterminate size="24" width="2" color="primary"></v-progress-circular>
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
													:class="{
														'system-message': message.messageType === 1,
														'user-message': !message.isOwn && message.messageType === 2,
														'user-own-message': message.isOwn && message.messageType === 2,
													}"
												>
													<!-- 系统消息 (messageType === 1) -->
													<div v-if="message.messageType === 1" class="system-message-content">
														<v-icon icon="mdi-information" size="small" class="mr-1"></v-icon>
														<span class="text-caption">{{ message.content }}</span>
													</div>

													<!-- 用户消息 (messageType === 2) -->
													<div v-else class="message-content">
														<!-- 他人消息头部：左对齐-->
														<div v-if="!message.isOwn" class="d-flex align-center mb-2">
															<v-avatar size="28" color="primary">
																<v-img v-if="message.avatar" :src="message.avatar" :alt="message.userName">
																	<template #error>
																		<v-icon icon="mdi-account" size="16"></v-icon>
																	</template>
																</v-img>
																<v-icon v-else icon="mdi-account" size="16"></v-icon>
															</v-avatar>
															<span class="message-sender ml-2">{{ message.userName }}</span>
															<span class="message-time ml-2">{{ formatTime(message.timestamp) }}</span>
														</div>

														<!-- 自己消息头部：右对齐-->
														<div v-else class="d-flex align-center mb-2 justify-end">
															<span class="message-time mr-2">{{ formatTime(message.timestamp) }}</span>
															<span class="message-sender mr-2">{{ message.userName }}</span>
															<v-avatar size="28" color="primary">
																<v-img v-if="message.avatar" :src="message.avatar" :alt="message.userName">
																	<template #error>
																		<v-icon icon="mdi-account" size="16"></v-icon>
																	</template>
																</v-img>
																<v-icon v-else icon="mdi-account" size="16"></v-icon>
															</v-avatar>
														</div>

														<!-- 消息气泡 -->
														<div v-if="message.contentType == 1" class="message-bubble" :class="{ 'message-own': message.isOwn }">
															<!-- 文本消息 (contentType === 1) -->
															<div class="message-text">
																{{ message.content }}
															</div>
														</div>

														<!-- 图片消息 (contentType === 2)-->
														<div v-if="message.contentType == 2" class="message-image-wrapper">
															<v-img
																:src="message.content"
																:alt="message.userName"
																max-width="240"
																class="message-image rounded-lg"
																@click="openPreview(message.content)"
															>
																<template #placeholder>
																	<div class="d-flex align-center justify-center fill-height">
																		<v-progress-circular indeterminate color="primary"></v-progress-circular>
																	</div>
																</template>
																<template #error>
																	<div class="d-flex flex-column align-center justify-center fill-height">
																		<v-icon icon="mdi-image-broken" size="48" color="error"></v-icon>
																		<span class="text-caption mt-2">图片加载失败</span>
																	</div>
																</template>
															</v-img>
														</div>
														<!-- 文件消息 (contentType === 3) -->
														<div v-else-if="message.contentType === 3" class="message-file-wrapper">
															<div class="message-file" @click="handleFileClick(message.file)">
																<v-icon :icon="getFileIcon(message.file.type)" size="20"></v-icon>
																<div class="file-info">
																	<div class="file-name">{{ message.file.name }}</div>
																	<div class="file-size text-caption">
																		{{ formatFileSize(message.file.size) }}
																	</div>
																</div>
															</div>
														</div>
													</div>
												</div>
											</div>

											<!-- 连接状态提示 -->
											<v-alert v-if="!roomMessageConnected" type="warning" variant="tonal" density="compact" class="mx-4"> 聊天服务未连接 </v-alert>

											<!-- 空状态 -->
											<div
												v-if="chatMessages.length === 0 && !loadingMore"
												class="d-flex flex-column align-center justify-center fill-height text-on-surface-variant"
											>
												<v-icon icon="mdi-chat-outline" size="64" color="grey" class="mb-4"></v-icon>
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

												<v-btn icon="mdi-paperclip" variant="text" size="small" class="attach-btn" @click="$refs.fileInput?.click()"></v-btn>

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
								<!-- 背景效果面板 -->
								<v-tabs-window-item value="background" class="fill-height">
									<div class="effect-panel">
										<!-- 顶部标题 -->
										<div class="effect-panel__header">
											<!-- 加载状态 -->
											<v-chip v-if="effectLoading" size="x-small" color="info" variant="flat">
												<v-progress-circular indeterminate size="12" width="2" class="mr-1"></v-progress-circular>
												加载资源中
											</v-chip>

											<!-- 激活状态 -->
											<v-chip v-else-if="effectProducerActive" size="x-small" color="success" variant="flat">
												<v-icon icon="mdi-check-circle" size="12" class="mr-1"></v-icon>
												{{ effectType === 'blur' ? '虚化生效' : '背景替换中' }}
											</v-chip>
										</div>

										<!-- 滚动区域 -->
										<div class="effect-panel__content">
											<!-- 错误提示 -->
											<v-alert v-if="effectError" type="error" variant="tonal" density="compact" closable class="mb-4" @click:close="effectError = null">
												{{ effectError }}
											</v-alert>

											<!-- 效果选择 -->
											<div class="mb-5">
												<div class="effect-section-title">基础设置</div>
												<div class="effects-grid">
													<v-card
														v-ripple
														class="effect-card"
														:class="{
															'effect-card--active': effectType === 'none',
															'effect-card--loading': effectLoading,
														}"
														variant="outlined"
														:disabled="effectLoading"
														@click="changeEffect('none')"
													>
														<v-icon icon="mdi-block-helper" size="20" class="mb-1"></v-icon>
														<span class="text-caption">无效果</span>
													</v-card>

													<v-card
														v-ripple
														class="effect-card"
														:class="{
															'effect-card--active': effectType === 'blur',
															'effect-card--loading': effectLoading,
														}"
														variant="outlined"
														:disabled="effectLoading"
														@click="changeEffect('blur')"
													>
														<v-icon icon="mdi-blur" size="20" class="mb-1"></v-icon>
														<span class="text-caption">背景虚化</span>
													</v-card>
												</div>
											</div>

											<!-- 虚拟背景选择 -->
											<div>
												<div class="d-flex align-center justify-space-between mb-2">
													<span class="effect-section-title">虚拟背景</span>
													<v-btn
														prepend-icon="mdi-upload"
														variant="text"
														density="compact"
														size="small"
														color="primary"
														:disabled="effectLoading"
														@click="bgFileInput?.click()"
													>
														自定义
													</v-btn>
												</div>

												<!-- 背景网格 -->
												<div class="backgrounds-grid">
													<v-card
														v-for="bg in allBackgrounds"
														:key="bg.id"
														v-ripple
														elevation="0"
														class="background-card"
														:class="{
															'background-card--active': effectType === 'replace' && selectedBackground === bg.id,
															'background-card--loading': effectLoading,
														}"
														:disabled="effectLoading"
														@click="changeBackground(bg.id)"
													>
														<v-img :src="bg.thumbnail" cover aspect-ratio="1.6">
															<template #placeholder>
																<div class="d-flex align-center justify-center fill-height bg-grey-lighten-4">
																	<v-icon icon="mdi-image-outline" color="grey"></v-icon>
																</div>
															</template>

															<!-- 选中遮罩 -->
															<div v-if="effectType === 'replace' && selectedBackground === bg.id" class="background-card__overlay">
																<v-icon icon="mdi-check-circle" color="white" size="24"></v-icon>
															</div>

															<!-- 加载遮罩 -->
															<div v-if="effectLoading && effectType === 'replace' && selectedBackground === bg.id" class="background-card__loading">
																<v-progress-circular indeterminate size="24" width="3" color="white"></v-progress-circular>
															</div>
														</v-img>
													</v-card>
												</div>
											</div>
										</div>

										<!-- 隐藏的文件输入 -->
										<input ref="bgFileInput" type="file" accept="image/jpeg,image/png,image/webp" hidden @change="handleBgFileUpload" />
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
				<v-btn icon variant="elevated" color="primary" size="small" class="collapse-toggle" @click="controlBarCollapsed = !controlBarCollapsed">
					<v-icon>{{ controlBarCollapsed ? 'mdi-chevron-up' : 'mdi-chevron-down' }}</v-icon>
				</v-btn>

				<!-- 控制栏主体 -->
				<transition name="slide-up">
					<div v-show="!controlBarCollapsed" class="control-bar">
						<v-container fluid class="pa-0">
							<v-row no-gutters align="center" justify="center">
								<!-- 左侧：连接状态 -->
								<v-col cols="auto" class="d-flex align-center">
									<v-chip :color="overallConnectionQuality.color" variant="flat" size="small" class="ml-4">
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
													@click="toggleAudio"
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
													@click="toggleVideo"
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
												{{ screenSharing ? '停止共享' : hasScreenShare ? '已有人在共享' : '共享屏幕' }}
											</span>
										</v-tooltip>

										<!-- 会议录制 -->
										<v-tooltip v-if="isHost" location="top">
											<template #activator="{ props }">
												<v-btn
													v-bind="props"
													:icon="isRecording ? 'mdi-stop-circle' : 'mdi-record-circle-outline'"
													:color="isRecording ? 'error' : 'surface'"
													:variant="isRecording ? 'flat' : 'elevated'"
													:loading="recordingLoading"
													size="large"
													class="control-btn"
													@click="toggleRecording"
												>
													<v-badge v-if="isRecording" dot color="error" location="top end">
														<v-icon>mdi-stop-circle</v-icon>
													</v-badge>
													<v-icon v-else>mdi-record-circle-outline</v-icon>
												</v-btn>
											</template>
											<div>
												<div>{{ isRecording ? '查看录制' : '开始录制' }}</div>
												<div v-if="isRecording" class="text-caption">{{ recordingFormattedDuration }}</div>
											</div>
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
												<v-list-item :disabled="effectLoading" @click="toggleBackgroundPanel">
													<template #prepend>
														<v-icon icon="mdi-image-filter-hdr" :color="effectProducerActive ? 'success' : undefined"></v-icon>
													</template>
													<v-list-item-title>背景特效</v-list-item-title>

													<!-- 显示当前效果状态 -->
													<template v-if="effectProducerActive" #append>
														<v-chip size="x-small" color="success" variant="flat">
															{{ effectType === 'blur' ? '虚化' : '替换' }}
														</v-chip>
													</template>

													<!-- 加载状态 -->
													<template v-else-if="effectLoading" #append>
														<v-progress-circular indeterminate size="16" width="2" color="primary"></v-progress-circular>
													</template>
												</v-list-item>
												<v-list-item @click="toggleVideoLayout">
													<template #prepend>
														<v-icon icon="mdi-view-grid"></v-icon>
													</template>
													<v-list-item-title>切换布局</v-list-item-title>
												</v-list-item>

												<v-list-item @click="toggleHandRaise">
													<template #prepend>
														<v-icon
															:icon="handRaised ? 'mdi-hand-back-right' : 'mdi-hand-back-right-outline'"
															:color="handRaised ? 'warning' : undefined"
														></v-icon>
													</template>
													<v-list-item-title>
														{{ handRaised ? '放下手' : '举手' }}
													</v-list-item-title>
												</v-list-item>

												<!-- 聚光灯模式 -->
												<v-list-item v-if="!isHost" @click="handleRequestSpotlight">
													<template #prepend>
														<v-icon icon="mdi-spotlight" color="warning"></v-icon>
													</template>
													<v-list-item-title>申请聚光灯</v-list-item-title>
												</v-list-item>

												<v-list-item v-if="spotlightPeerId" @click="handleSetSpotlight({ targetPeerId: null, active: false })">
													<template #prepend>
														<v-icon icon="mdi-spotlight-off" color="on-surface-variant"></v-icon>
													</template>
													<v-list-item-title>关闭聚光灯</v-list-item-title>
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
														<v-icon :icon="isFullscreen ? 'mdi-fullscreen-exit' : 'mdi-fullscreen'"></v-icon>
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

									<v-badge :content="unreadMessages" :model-value="unreadMessages > 0" color="error" overlap class="mr-4">
										<v-tooltip location="top">
											<template #activator="{ props }">
												<v-btn v-bind="props" icon="mdi-chat" variant="elevated" color="surface" size="large" @click="toggleSidebarChat"></v-btn>
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

							<v-switch v-model="enableHD" label="启用高清视频" color="primary" hide-details class="mb-3"></v-switch>

							<v-switch v-model="enableMirror" label="镜像我的视频" color="primary" hide-details></v-switch>
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
			<v-dialog v-model="showLeaveConfirm" max-width="420" persistent transition="dialog-bottom-transition">
				<v-card rounded="xl" elevation="8">
					<!-- 顶部图标区域 -->
					<div class="leave-dialog-header">
						<div class="leave-dialog-icon">
							<v-icon icon="mdi-exit-to-app" color="primary" size="32"></v-icon>
						</div>
						<div class="leave-dialog-title">
							<span class="text-h6 font-weight-semibold">离开会议</span>
							<span class="text-caption text-medium-emphasis mt-1">会议时长：{{ meetingDuration }}</span>
						</div>
						<v-btn icon="mdi-close" variant="text" size="small" density="comfortable" class="leave-dialog-close" @click="showLeaveConfirm = false"></v-btn>
					</div>

					<v-divider></v-divider>

					<v-card-text class="pa-5">
						<!-- 提示信息列表 -->
						<v-list density="compact" bg-color="transparent" class="leave-tip-list">
							<v-list-item v-for="tip in leaveTips" :key="tip.text" :prepend-icon="tip.icon" :base-color="tip.color" density="compact" class="px-0 rounded-lg">
								<v-list-item-title class="text-body-2">{{ tip.text }}</v-list-item-title>
							</v-list-item>
						</v-list>
					</v-card-text>

					<v-card-actions class="px-5 pb-5 pt-0 ga-3">
						<v-btn variant="tonal" color="primary" size="large" rounded="lg" class="flex-1-1" prepend-icon="mdi-arrow-left" @click="showLeaveConfirm = false">
							留在会议
						</v-btn>
						<v-btn variant="outlined" color="on-surface-variant" size="large" rounded="lg" class="flex-1-1" prepend-icon="mdi-exit-to-app" @click="confirmLeaveMeeting">
							确认离开
						</v-btn>
					</v-card-actions>
				</v-card>
			</v-dialog>

			<!-- 聚光灯申请对话框（仅主持人可见） -->
			<v-dialog v-model="showSpotlightRequestDialog" max-width="400" persistent>
				<v-card rounded="xl" elevation="8">
					<v-card-title class="d-flex align-center pa-4 ga-2">
						<v-icon icon="mdi-spotlight" color="warning" size="28"></v-icon>
						<span class="text-h6 font-weight-semibold">聚光灯申请</span>
					</v-card-title>
					<v-divider></v-divider>
					<v-card-text class="pa-5">
						<div class="d-flex align-center ga-3 mb-2">
							<v-avatar color="primary" size="40">
								<span class="text-body-1 font-weight-bold">{{ spotlightRequest?.requesterUsername?.charAt(0)?.toUpperCase() }}</span>
							</v-avatar>
							<div>
								<div class="text-body-1 font-weight-medium">{{ spotlightRequest?.requesterUsername }}</div>
								<div class="text-caption text-medium-emphasis">申请开启聚光灯模式</div>
							</div>
						</div>
						<v-alert type="info" variant="tonal" density="compact" class="mt-3 text-body-2"> 开启后，该参与者的视频将占据主屏幕中央，其他人缩小至底部缩略图。 </v-alert>
					</v-card-text>
					<v-card-actions class="px-5 pb-5 pt-0 ga-3">
						<v-btn variant="tonal" color="error" size="large" rounded="lg" class="flex-1-1" prepend-icon="mdi-close-circle" @click="denySpotlight"> 拒绝 </v-btn>
						<v-btn variant="flat" color="warning" size="large" rounded="lg" class="flex-1-1" prepend-icon="mdi-spotlight" @click="approveSpotlight"> 同意 </v-btn>
					</v-card-actions>
				</v-card>
			</v-dialog>

			<!-- 图片预览对话框 -->
			<FilePreview
				v-model="previewVisible"
				:file-url="previewFileUrl"
				:file-name="previewFileName"
				:file-type="previewFileType"
				:download-progress="downloadProgress"
				@download="handleDownloadFromPreview"
				@close="closePreview"
			/>

			<RecordingDialog
				v-model="recordingDialogVisible"
				:phase="recordingPhase"
				:recording-format="recordingFormat"
				:formatted-duration="recordingFormattedDuration"
				:recording-result="recordingResult"
				:upload-progress="recordingUploadProgress"
				:error-message="recordingErrorMessage"
				@start="onStartRecording"
				@stop="handleStopRecording"
				@close="onDialogClose"
				@minimize="onDialogMinimize"
				@download="handleDownloadFromPreview"
				@preview="url => openPreview(url, 'record.mp4')"
			/>
		</v-main>
	</v-app>
</template>

<script setup>
import { ref, computed, onBeforeUnmount, watch, nextTick, onMounted, mergeProps } from 'vue'
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
	useIntersectionObserver,
} from '@vueuse/core'
import VideoGrid from '../components/VideoGrid.vue'
import ParticipantsList from '../components/ParticipantsList.vue'
import FilePreview from '@/components/common/FilePreview.vue'
import RecordingDialog from '../components/RecordingDialog.vue'
import MeetingEntryOverlay from '../components/MeetingEntryOverlay.vue'
import { useRecording, RECORDING_PHASE } from '@/composables/useRecording'
import { useFilePreview } from '@/composables/useFilePreview'
import { useParticipants } from '@/composables/useParticipants'
import { useMediaDevices } from '@/composables/useMediaDevices'
import RoomMessageService from '@/services/RoomMessageService'
import { useMedia } from '@/composables/useMedia'
import { fetchMeetingInfo } from '@/api/room'
import { uploadFile } from '@/api/file'
import { fetchMessageHistory } from '@/api/room'
import { $notify } from '@/plugins/notification'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
// ==================== 离开会议 ====================
const leaveTips = [
	{ icon: 'mdi-chat-outline', color: 'primary', text: '会议记录和聊天内容将会保留' },
	{ icon: 'mdi-account-multiple-outline', color: 'primary', text: '您的离开不会结束整个会议' },
	{ icon: 'mdi-clock-outline', color: 'primary', text: '可随时使用会议号重新加入' },
]
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
const { cameras, microphones, speakers, selectedCamera, selectedMicrophone, selectedSpeaker, enumerateDevices, switchDevice } = useMediaDevices()

// 监听设备变化
useEventListener('devicechange', async () => {
	console.log('Media devices changed')
	await enumerateDevices()
})
// ==================== 文件预览 ====================
const {
	visible: previewVisible,
	fileUrl: previewFileUrl,
	fileName: previewFileName,
	fileType: previewFileType,
	downloadProgress,
	openPreview,
	closePreview,
	downloadFile,
	getFileType,
	getFileIcon,
} = useFilePreview()
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
	effectType,
	selectedBackground,
	allBackgrounds,
	effectLoading,
	effectError,
	effectProducerActive,
	currentSpatialLayer,
	joinMeeting,
	leaveMeeting,
	toggleAudio,
	toggleVideo,
	startScreenShare,
	stopScreenShare,
	changeAudioDevice,
	changeVideoDevice,
	uploadCustomBackground,
	hostToggleAudio,
	hostToggleVideo,
	muteAll,
	disableAllVideo,
	removeParticipant,
	setAllConsumersPreferredLayers,
	spotlightPeerId,
	spotlightRequest,
	requestSpotlight,
	setSpotlight,
} = useMedia()
// 处理背景替换文件上传处理

const bgFileInput = ref(null)
const handleBgFileUpload = async event => {
	const file = event.target.files?.[0]
	if (!file) return

	// 文件大小限制 (5MB)
	const maxSize = 5 * 1024 * 1024
	if (file.size > maxSize) {
		$notify.error('背景图片不能超过5MB')
		return
	}

	// 文件类型校验
	if (!file.type.startsWith('image/')) {
		$notify.error('仅支持图片格式')
		return
	}

	try {
		effectLoading.value = true
		const bg = await uploadCustomBackground(file)

		// 自动应用新背景
		selectedBackground.value = bg.id
		effectType.value = 'replace'
	} catch (error) {
		console.error('[MeetingRoom] Upload background failed:', error)
		$notify.error(`上传失败: ${error.message}`)
	} finally {
		effectLoading.value = false
		// 清空文件输入
		if (bgFileInput.value) {
			bgFileInput.value.value = ''
		}
	}
}
const changeEffect = async type => {
	effectType.value = type
}

const changeBackground = async bgId => {
	selectedBackground.value = bgId
	effectType.value = 'replace'
}
// ==================== 房间参与者 ====================
const { onlineParticipants, loadParticipants } = useParticipants(roomId, participants)
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

// 网络质量计算
const networkQuality = computed(() => {
	if (!online) {
		return { text: '离线', color: 'error', icon: 'mdi-wifi-off' }
	}

	const type = effectiveType.value
	const speed = downlink.value

	if (type === '5g' && speed > 5) {
		return { text: '网络优秀', color: 'success', icon: 'mdi-wifi-strength-4' }
	} else if (type === '4g' || speed > 1) {
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
const entryPhase = ref('loading')
const loadingMessage = ref('')
const loadingProgress = ref(0)
const controlBarCollapsed = ref(false)
const showLeaveConfirm = ref(false)

// 视频设置 (初始化时与底层状态同步: layer 0->1, 1->2, 2->3)
const videoQuality = ref(currentSpatialLayer.value + 1)
const enableHD = ref(true)
const enableMirror = ref(false)

// 等候室相关状态

const isRetrying = ref(false)
const WAITING_THRESHOLD_MS = 30 * 60 * 1000 // 30分钟

// 检查会议时间拦截规则
const checkMeetingEligibility = () => {
	const diff = new Date(meetingInfo.value.startTime).getTime() - Date.now()
	// 当预约会议 (type === 2) 且离开始时间大于30分钟时触发等候室 phase 状态
	if (meetingInfo.value.type === 2 && diff > WAITING_THRESHOLD_MS) {
		entryPhase.value = 'waiting'
		return false
	}
	return true
}

const handleRetryJoin = async () => {
	isRetrying.value = true
	try {
		await loadMeetingDetail()
		if (checkMeetingEligibility()) {
			entryPhase.value = 'consent'
		} else {
			$notify.warning('还未到可加入时间，请稍候')
		}
	} catch (e) {
		console.log(e)
		$notify.error('检测状态失败，请重试')
	} finally {
		isRetrying.value = false
	}
}

// 时间自然到达放行逻辑
const handleTimeup = () => {
	if (entryPhase.value === 'waiting') {
		entryPhase.value = 'consent'
	}
}
// 页面挂载先执行会议拉取判断时间拦截
onMounted(async () => {
	try {
		loadingMessage.value = '正在加载会议信息...'
		loadingProgress.value = 20
		await loadMeetingDetail()
		if (checkMeetingEligibility()) {
			entryPhase.value = 'consent'
		}
	} catch (error) {
		console.log(error)
		entryPhase.value = 'not-found'
	}
})

// 用户选择媒体权限后的处理
const handleMediaConsent = async ({ withMedia }) => {
	entryPhase.value = 'loading'

	try {
		loadingProgress.value = 10
		loadingMessage.value = '正在检测设备...'
		await enumerateDevices()
		loadingProgress.value = 30

		loadingMessage.value = '正在加载会议信息...'
		await loadMeetingDetail()
		loadingProgress.value = 40

		loadingMessage.value = '正在加载参与者...'
		await loadParticipants()
		loadingProgress.value = 60

		loadingMessage.value = '正在加入会议...'
		await joinMeeting(meetingInfo.value.roomId, currentUserId.value, currentUsername.value, authToken.value, { withMedia })
		loadingProgress.value = 80
		meetingStartTime.value = Date.now()

		loadingMessage.value = '正在连接聊天服务...'
		await initRoomMessageService()
		loadingProgress.value = 100

		// 短暂停留后隐藏，避免闪烁
		await new Promise(resolve => setTimeout(resolve, 300))
		entryPhase.value = 'hidden'
	} catch (error) {
		console.error('Failed to initialize meeting', error)
		$notify.error(`加入会议失败: ${error.message}`)
		entryPhase.value = 'hidden'
	}
}

// ==================== 聊天功能 ====================
const roomMessageConnected = ref(false)
const roomMessageError = ref(null)
const messageContainer = ref(null)
const fileInput = ref(null)
const messageInput = ref('')
const chatMessages = ref([])
const unreadMessages = ref(0)
const hasMoreMessages = ref(false)
const loadingMore = ref(false)
const uploadProgress = ref(0)
// 历史消息分页
const currentPage = ref(1)
const pageSize = 20
const loadMoreTrigger = ref(null)
// 滚动控制
const { arrivedState } = useScroll(messageContainer, {
	offset: { bottom: 50 },
})

// 自动滚动到底部
const scrollToBottom = async (smooth = false) => {
	const container = messageContainer.value
	if (!container) {
		console.warn('[Chat] Message container not available, skipping scroll')
		return
	}
	// 等待 DOM 完全更新
	await nextTick()

	// 再等待一帧，确保布局计算完成
	await new Promise(resolve => requestAnimationFrame(resolve))
	// 强制滚动到最底部
	container.scrollTo({
		top: container.scrollHeight,
		behavior: smooth ? 'smooth' : 'auto',
	})

	// console.log('[Chat] Scrolled to bottom:', {
	// 	scrollTop: container.scrollTop,
	// 	scrollHeight: container.scrollHeight,
	// 	clientHeight: container.clientHeight,
	// })
}

// 按日期分组消息
const groupedMessages = computed(() => {
	const groups = {}
	chatMessages.value.forEach(msg => {
		const date = useDateFormat(msg.timestamp, 'YYYY-MM-DD').value
		if (!groups[date]) groups[date] = []
		groups[date].push(msg)
	})
	return groups
})
/**
 * 将后端 RoomMessageResVO 转换为前端消息格式
 */
const transformMessage = resVO => {
	const baseMessage = {
		id: resVO.messageId,
		userId: resVO.senderId,
		userName: resVO.senderNickname,
		avatar: resVO.senderAvatar,
		messageType: resVO.messageType, // 1:系统消息，2:用户消息
		contentType: resVO.contentType, // 1:文本，2:图片，3:文件
		timestamp: new Date(resVO.sendTime),
		isOwn: resVO.senderId === currentUserId.value,
	}

	// 根据 contentType 转换消息类型
	switch (resVO.contentType) {
		case 1: // 文本消息
			return {
				...baseMessage,
				content: resVO.content,
			}

		case 2: // 图片消息
			return {
				...baseMessage,
				content: resVO.content, // 图片URL
			}

		case 3: {
			// 从 content 中解析文件信息 (格式为 "name|type|size|url") // 文件消息
			const [name, type, size, url] = resVO.content.split('|')
			return {
				...baseMessage,
				file: {
					name: name,
					size: parseInt(size),
					type: getFileType(type),
					url: url,
				},
			}
		}

		default:
			return {
				...baseMessage,
				content: '消息解析错误',
			}
	}
}
/**
 * 发送文本消息
 */
const sendChatMessage = async () => {
	const content = messageInput.value.trim()
	if (!content) return

	if (!roomMessageConnected.value) {
		$notify.error('聊天服务未连接')
		return
	}

	try {
		// 发送消息
		RoomMessageService.sendTextMessage(content)

		// 清空输入框和草稿
		messageInput.value = ''
	} catch (error) {
		console.error('发送消息失败:', error)
		$notify.error('发送失败,请重试')
	}
}

/**
 * 处理文件上传并发送
 */
const handleFileSelect = async event => {
	const file = event.target.files[0]
	if (!file) return

	// 文件大小限制
	const maxSize = 10 * 1024 * 1024 // 10MB
	if (file.size > maxSize) {
		$notify.error('文件大小不能超过10MB')
		return
	}

	try {
		uploadProgress.value = 0
		const formData = new FormData()
		formData.append('file', file)

		// 上传文件到服务器,获取文件URL
		const { data } = await uploadFile(formData)

		// 模拟上传进度
		const uploadInterval = setInterval(() => {
			uploadProgress.value += 10
			if (uploadProgress.value >= 100) {
				clearInterval(uploadInterval)

				// 根据文件类型发送不同消息
				const fileType = file.type.split('/')[0]

				if (fileType === 'image') {
					// 图片直接上传URL
					RoomMessageService.sendImageMessage(data)
				} else {
					// 其他文件格式发送 "filename|type|size|url"
					const content = `${file.name}|${fileType}|${file.size}|${data}`
					RoomMessageService.sendFileMessage(content)
				}

				uploadProgress.value = 0
			}
		}, 100)
	} catch (error) {
		console.error('文件上传失败:', error)
		uploadProgress.value = 0
	} finally {
		fileInput.value.value = ''
	}
}
/**
 * 加载历史消息
 */
const loadMessageHistory = async (page = 1) => {
	if (loadingMore.value) return

	try {
		loadingMore.value = true

		const { data } = await fetchMessageHistory(roomId.value, page, pageSize)

		if (data.length > 0) {
			// 转换消息格式
			const transformedMessages = data.map(transformMessage)
			// 记录加载前的滚动高度（用于加载更多时保持位置）
			const containerEl = messageContainer.value
			const oldScrollHeight = containerEl?.scrollHeight || 0

			// 如果是第一页，直接替换
			if (page === 1) {
				chatMessages.value = transformedMessages

				// 首次加载，等待 DOM 更新后滚动到底部
				await nextTick()
				await new Promise(resolve => setTimeout(resolve, 500)) // 额外等待渲染
				scrollToBottom()
			} else {
				// 追加到顶部（加载更多历史）
				chatMessages.value.unshift(...transformedMessages)

				// 恢复滚动位置（避免跳动）
				await nextTick()
				if (containerEl) {
					const newScrollHeight = containerEl.scrollHeight
					containerEl.scrollTop = newScrollHeight - oldScrollHeight
				}
			}

			// 判断是否还有更多
			hasMoreMessages.value = data.length === pageSize.value
			currentPage.value = page
		} else {
			hasMoreMessages.value = false
		}
	} catch (error) {
		console.error('加载历史消息失败:', error)
	} finally {
		loadingMore.value = false
	}
}
/**
 * 加载更多消息 (点击按钮)
 */
const handleLoadMoreMessages = async () => {
	await loadMessageHistory(currentPage.value + 1)
}
// 使用 Intersection Observer 实现无限滚动
useIntersectionObserver(
	loadMoreTrigger,
	([{ isIntersecting }]) => {
		if (isIntersecting && hasMoreMessages.value && !loadingMore.value) {
			handleLoadMoreMessages()
		}
	},
	{ threshold: 0.5 },
)
/**
 * 初始化房间消息服务
 */
const initRoomMessageService = async () => {
	try {
		// 1. 连接 WebSocket
		await RoomMessageService.connect(import.meta.env.VITE_WS_ROOM_MESSAGE_SERVER, currentUserId.value, meetingInfo.value.roomId)

		roomMessageConnected.value = true

		// 2. 监听房间消息
		RoomMessageService.on('room-message', data => {
			// 转换消息格式并添加到列表
			const message = transformMessage(data)
			chatMessages.value.push(message)

			// 如果不在聊天标签页，增加未读计数
			if (sidebarTab.value === 'chat' && messageContainer.value) {
				nextTick(() => scrollToBottom(true))
			} else {
				unreadMessages.value++
			}
		})
		// 3. 监听连接错误
		RoomMessageService.on('connection-error', error => {
			console.error('房间消息服务连接错误:', error)
			roomMessageError.value = error
		})

		// 4. 暂时不加载历史消息，等切换到聊天时再加载
		// await loadMessageHistory(1)
		hasMoreMessages.value = true
	} catch (error) {
		console.error('初始化房间消息服务失败:', error)
		roomMessageError.value = error
	}
}

// 输入节流
const handleTyping = useThrottleFn(() => {
	// TODO: 通知其他人正在输入
	console.log('User is typing...')
}, 1000)

// ==================== 文件下载&预览 ====================
const handleFileClick = file => {
	const fileType = getFileType(file.name)
	const canPreview = ['image', 'video', 'audio', 'pdf'].includes(fileType)

	if (canPreview) {
		// 可预览的文件，打开预览
		openPreview(file.url, file.name)
	} else {
		// 不可预览的文件，直接下载
		downloadFile(file.url, file.name)
	}
}

/**
 * 统一的文件下载处理（从预览组件触发）
 */
const handleDownloadFromPreview = async ({ url, name }) => {
	try {
		// 方案 1: 直接使用 Fetch API（推荐）
		await downloadFile({ url, name })

		// 方案 2: 如果需要进度显示，使用 downloadFileWithProgress
		// await downloadFileWithProgress(url, name)

		// 方案 3: 如果跨域问题无法解决，使用后端代理
		// await downloadViaProxy({ url, name })
	} catch (error) {
		console.error('Download failed:', error)
		$notify.error('下载失败，请重试')
	}
}

// ==================== 会议录制功能 ====================
const {
	phase: recordingPhase,
	isRecording,
	isLoading: recordingLoading,
	recordingFormat,
	formattedDuration: recordingFormattedDuration,
	recordingResult,
	uploadProgress: recordingUploadProgress,
	errorMessage: recordingErrorMessage,
	checkAndOpen,
	handleStartRecording,
	handleStopRecording,
	reset: resetRecording,
} = useRecording(roomId, currentUserId)

// Dialog 显示控制（独立 ref，与 phase 解耦）
const recordingDialogVisible = ref(false)

// 控制栏录制按钮点击：录制中直接打开 Dialog，未录制时检查并打开
const toggleRecording = () => {
	recordingDialogVisible.value = true
	if (!isRecording.value) {
		checkAndOpen()
	}
	// 录制中时 phase 已是 RECORDING，Dialog 直接展示录制中状态
}

// 开始录制：成功进入 RECORDING 后自动关闭 Dialog
const onStartRecording = async format => {
	await handleStartRecording(format)
	if (recordingPhase.value === RECORDING_PHASE.RECORDING) {
		recordingDialogVisible.value = false
	}
}

// 真正关闭（非录制中）：重置状态、关闭 Dialog
const onDialogClose = () => {
	resetRecording(false)
	recordingDialogVisible.value = false
}

// 最小化（录制中）：仅关闭 Dialog，不停止录制
const onDialogMinimize = () => {
	recordingDialogVisible.value = false
	// resetRecording(true) 已在 RecordingDialog 的 minimize emit 中处理，此处无需调用
}

// ==================== 会议控制功能 ====================
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
const participantCount = computed(() => onlineParticipants.value.length)

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

const formatFileSize = bytes => {
	if (bytes === 0) return '0 B'
	const k = 1024
	const sizes = ['B', 'KB', 'MB', 'GB']
	const i = Math.floor(Math.log(bytes) / Math.log(k))
	return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

// ==================== 状态与事件监听 ====================
// 监听网络状态 - 使用防抖函数
watch(online, handleNetworkChange)
// 监听滚动到底部时标记已读
watch(
	() => arrivedState.bottom,
	isBottom => {
		if (isBottom && unreadMessages.value > 0) {
			unreadMessages.value = 0
		}
	},
)
// ========== 监听标签切换到聊天时自动滚动 ==========
watch(sidebarTab, async (newTab, oldTab) => {
	if (newTab === 'chat' && oldTab !== 'chat') {
		// 重置未读消息
		unreadMessages.value = 0

		// 首次切换到聊天时加载历史
		if (chatMessages.value.length === 0 && hasMoreMessages.value) {
			await loadMessageHistory(1)
		} else {
			// 已有消息，直接滚动
			await nextTick()
			await new Promise(resolve => setTimeout(resolve, 200))
			scrollToBottom(true)
		}
	}
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

const addNewLine = () => {
	messageInput.value += '\n'
}

const handleSaveChat = () => {
	const chatText = chatMessages.value.map(m => `[${formatTime(m.timestamp)}] ${m.userName}: ${m.content || '[文件]'}`).join('\n')

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

// ==================== 会议控制 ====================
const toggleSidebarChat = async () => {
	showSidebar.value = true
	sidebarTab.value = 'chat'
	unreadMessages.value = 0
}
const toggleBackgroundPanel = () => {
	showSidebar.value = true
	sidebarTab.value = 'background'
}

const toggleVideoLayout = () => {
	const layouts = ['grid', 'spotlight', 'sidebar']
	const currentIndex = layouts.indexOf(videoLayout.value)
	videoLayout.value = layouts[(currentIndex + 1) % layouts.length]
	$notify.success(`已切换到${videoLayout.value}布局`)
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

	entryPhase.value = 'loading'
	loadingProgress.value = 0
	loadingMessage.value = '正在离开会议...'

	try {
		loadingProgress.value = 50
		await leaveMeeting()
		loadingProgress.value = 80

		RoomMessageService.disconnect()
		loadingProgress.value = 100

		await new Promise(resolve => setTimeout(resolve, 300))
		router.push('/')
	} catch (error) {
		console.error('Failed to leave meeting', error)
		router.push('/')
	}
}
// 判断当前用户是否为主持人
const isHost = computed(() => currentUserId.value === meetingInfo.value.hostId)

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

// ==================== 聚光灯功能 ====================
const showSpotlightRequestDialog = ref(false)

// 主持人处理 VideoGrid 中的 set-spotlight 事件（直接设置，无需弹窗）
const handleSetSpotlight = async ({ targetPeerId, active }) => {
	await setSpotlight(targetPeerId, active)
	if (active) {
		$notify.success('已开启聚光灯模式')
	} else {
		$notify.info('已关闭聚光灯模式')
	}
}

// 主持人同意聚光灯申请
const approveSpotlight = async () => {
	showSpotlightRequestDialog.value = false
	if (spotlightRequest.value) {
		await setSpotlight(spotlightRequest.value.requesterId, true)
		spotlightRequest.value = null
		$notify.success('已开启聚光灯模式')
	}
}

// 主持人拒绝聚光灯申请
const denySpotlight = async () => {
	showSpotlightRequestDialog.value = false
	// 通知申请者被拒绝：通过 setSpotlight active=false 广播，前端自行过滤
	// 这里只需关闭弹窗，对方不会收到任何通知（可根据需求扩展）
	spotlightRequest.value = null
	$notify.info('已拒绝聚光灯申请')
}

// 非主持人申请聚光灯
const handleRequestSpotlight = async () => {
	await requestSpotlight()
}

// 监听收到新的聚光灯申请：主持人弹窗，非主持人丢弃防止状态污染
watch(spotlightRequest, newRequest => {
	if (!newRequest) return
	if (isHost.value) {
		showSpotlightRequestDialog.value = true
	} else {
		spotlightRequest.value = null
	}
})

// ==================== 设置管理 ====================
const saveSettings = async () => {
	try {
		// 应用视频质量设置 (1: 流畅/Layer 0, 2: 标清/Layer 1, 3: 高清/Layer 2)
		if (videoQuality.value) {
			const spatialLayer = videoQuality.value - 1
			// 只有当设置发生变化时才发送请求
			if (spatialLayer !== currentSpatialLayer.value) {
				console.log(`Apply video quality: ${videoQuality.value}, mapping to spatial layer: ${spatialLayer}`)
				await setAllConsumersPreferredLayers(spatialLayer)
			}
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
const loadMeetingDetail = async () => {
	try {
		const { data } = await fetchMeetingInfo(meetingInfo.value.roomNo)
		meetingInfo.value = data
	} catch (error) {
		console.error('Failed to load meeting detail', error)
		throw error
	}
}

// ==================== 生命周期 ====================

onBeforeUnmount(async () => {
	// 离开会议
	await leaveMeeting()
	// 断开房间消息服务
	RoomMessageService.disconnect()
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

/* 离开会议对话框 */
.leave-dialog-header {
	display: flex;
	align-items: center;
	gap: 12px;
	padding: 20px 20px 16px;
	position: relative;
}

.leave-dialog-icon {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 52px;
	height: 52px;
	border-radius: 16px;
	background: rgba(var(--v-theme-primary), 0.1);
	flex-shrink: 0;
}

.leave-dialog-title {
	display: flex;
	flex-direction: column;
	flex: 1;
	min-width: 0;
}

.leave-dialog-close {
	position: absolute;
	top: 12px;
	right: 12px;
}

.leave-tip-list :deep(.v-list-item__prepend .v-icon) {
	opacity: 0.75;
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
	padding: 6px 8px;
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

.message-wrapper {
	display: flex;
	margin-bottom: 12px;
	animation: fadeIn 0.2s ease-in;
}
.system-message {
	justify-content: center !important;
	margin: 8px 0;
}
.user-message {
	justify-content: flex-start !important;
}
.user-own-message {
	justify-content: flex-end !important;
}

.system-message-content {
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 6px 12px;
	background: rgba(var(--v-theme-info), 0.1);
	border-radius: 12px;
	color: rgb(var(--v-theme-info));
	font-size: 13px;
}
/* 系统消息居中 */
.message-wrapper.system-message {
	justify-content: center;
	margin: 8px 0;
}
/* 图片消息样式 */
.message-image-wrapper {
	min-width: 160px;
	max-width: 240px;
	margin: 4px 0;
}
.message-image {
	cursor: pointer;
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	border-radius: 12px;
	overflow: hidden;
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.message-image:hover {
	transform: scale(1.1);
	box-shadow: 0 4px 16px rgba(var(--v-theme-primary), 0.2);
}

.message-image :deep(.v-img) {
	border-radius: 12px;
	overflow: hidden;
}

/* 加载更多触发器 */
.load-more-trigger {
	display: flex;
	justify-content: center;
	padding: 12px;
	min-height: 48px;
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

/* 连接状态动画 */
@keyframes pulse {
	0%,
	100% {
		opacity: 1;
	}
	50% {
		opacity: 0.5;
	}
}

.v-alert {
	animation: pulse 2s infinite;
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
	display: flex;
	flex-direction: column;
	max-width: 75%;
}
.message-time-own-image {
	font-size: 10px;
	color: rgb(var(--v-theme-on-surface-variant));
	text-align: right;
	margin-top: 4px;
}
/* 自己发送的图片靠右 */
.message-wrapper.user-own-message .message-image-wrapper {
	margin-left: auto;
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
	border-bottom-left-radius: 4px;
	max-width: 100%;
}

.message-bubble:hover {
	opacity: 0.9;
}

.message-own {
	background: rgb(var(--v-theme-primary));
	color: rgb(var(--v-theme-on-primary));
	border-bottom-right-radius: 4px;
	border-bottom-left-radius: 16px;
}
/* 文本消息 */
.message-text {
	word-wrap: break-word;
	white-space: pre-wrap;
	line-height: 1.5;
	font-size: 14px;
}

.message-file {
	display: flex;
	align-items: center;
	gap: 12px;
	padding: 12px;
	background: rgba(var(--v-theme-surface-light), 0.5);
	border-radius: 12px;
	cursor: pointer;
	transition: background-color 0.2s;
}
.message-file:hover {
	background: rgba(var(--v-theme-surface-light), 0.8);
}

.file-info {
	flex: 1;
	min-width: 0;
}

.file-name {
	font-size: 14px;
	font-weight: 500;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	color: rgb(var(--v-theme-on-surface));
}

.file-size {
	opacity: 0.7;
	font-size: 12px;
	margin-top: 2px;
	color: rgb(var(--v-theme-on-surface-variant));
}
/* 图片预览对话框 */
.image-preview-card {
	background: rgb(var(--v-theme-surface));
}

.image-preview-card :deep(.v-img) {
	background: rgb(var(--v-theme-background));
}

/* 下载对话框动画 */
.v-dialog > .v-overlay__content {
	animation: dialogSlideUp 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
@keyframes dialogSlideUp {
	from {
		opacity: 0;
		transform: translateY(20px);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
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
/* 背景特效面板样式 */
/* ==================== 背景特效面板 ==================== */

/* 面板容器 */
.effect-panel {
	display: flex;
	flex-direction: column;
	height: 100%;
	background: rgb(var(--v-theme-surface));
	border-left: 1px solid rgba(var(--v-theme-border), 0.5);
}

/* 面板头部 */
.effect-panel__header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 16px;
	padding-bottom: 8px;
	border-bottom: 1px solid rgba(var(--v-theme-border), 0.3);
	flex-shrink: 0;
}

/* 面板内容区 */
.effect-panel__content {
	flex: 1;
	overflow-y: auto;
	padding: 16px;
	min-height: 0;
}

/* 自定义滚动条 */
.effect-panel__content::-webkit-scrollbar {
	width: 6px;
}

.effect-panel__content::-webkit-scrollbar-track {
	background: rgba(var(--v-theme-surface-variant), 0.3);
	border-radius: 3px;
}

.effect-panel__content::-webkit-scrollbar-thumb {
	background: rgba(var(--v-theme-primary), 0.3);
	border-radius: 3px;
	transition: background 0.2s;
}

.effect-panel__content::-webkit-scrollbar-thumb:hover {
	background: rgba(var(--v-theme-primary), 0.5);
}

/* 分区标题 */
.effect-section-title {
	font-size: 0.75rem;
	font-weight: 700;
	color: rgb(var(--v-theme-on-surface-variant));
	text-transform: uppercase;
	letter-spacing: 0.5px;
}

/* ==================== 效果选择网格 ==================== */

.effects-grid {
	display: grid;
	grid-template-columns: repeat(2, 1fr);
	gap: 12px;
}

/* 效果卡片 */
.effect-card {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: 16px 12px;
	min-height: 80px;
	cursor: pointer;
	transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
	border-width: 2px;
	border-style: solid;
	border-color: rgba(var(--v-theme-border), 0.5);
}

.effect-card:hover {
	border-color: rgb(var(--v-theme-primary));
	background: rgba(var(--v-theme-primary), 0.05);
	transform: translateY(-2px);
	box-shadow: 0 4px 12px rgba(var(--v-theme-primary), 0.15);
}

.effect-card--active {
	border-color: rgb(var(--v-theme-primary));
	background: linear-gradient(135deg, rgba(var(--v-theme-primary), 0.12) 0%, rgba(var(--v-theme-primary), 0.08) 100%);
	color: rgb(var(--v-theme-primary));
	font-weight: 600;
}

.effect-card--active:hover {
	background: linear-gradient(135deg, rgba(var(--v-theme-primary), 0.18) 0%, rgba(var(--v-theme-primary), 0.12) 100%);
}

/* ==================== 背景图网格 ==================== */

.backgrounds-grid {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
	gap: 12px;
}

/* 背景卡片 */
.background-card {
	position: relative;
	border-radius: 8px;
	overflow: hidden;
	cursor: pointer;
	border: 2px solid transparent;
	transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.background-card:hover {
	transform: translateY(-3px) scale(1.02);
	box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
	border-color: rgba(var(--v-theme-primary), 0.3);
}

.background-card--active {
	border-color: rgb(var(--v-theme-primary));
	box-shadow: 0 4px 16px rgba(var(--v-theme-primary), 0.3);
}
.background-card--loading {
	opacity: 0.6;
	pointer-events: none;
}
.background-card__loading {
	position: absolute;
	inset: 0;
	background: rgba(0, 0, 0, 0.5);
	display: flex;
	align-items: center;
	justify-content: center;
	backdrop-filter: blur(2px);
	animation: overlayFadeIn 0.2s ease-out;
}

.background-card--active:hover {
	transform: translateY(-3px) scale(1.02);
	box-shadow: 0 8px 24px rgba(var(--v-theme-primary), 0.4);
}

/* 背景选中遮罩 */
.background-card__overlay {
	position: absolute;
	inset: 0;
	background: linear-gradient(135deg, rgba(var(--v-theme-primary), 0.4) 0%, rgba(var(--v-theme-primary), 0.25) 100%);
	display: flex;
	align-items: center;
	justify-content: center;
	backdrop-filter: blur(2px);
	animation: overlayFadeIn 0.2s ease-out;
}
/* 效果卡片禁用状态 */
.effect-card:disabled,
.effect-card--loading {
	opacity: 0.5;
	pointer-events: none;
	cursor: not-allowed;
}

/* 过渡动画优化 */
.effect-card,
.background-card {
	transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
/* 过渡动画优化 */
.effect-card,
.background-card {
	transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 成功状态动画 */
@keyframes successPulse {
	0%,
	100% {
		box-shadow: 0 0 0 0 rgba(var(--v-theme-success), 0.4);
	}
	50% {
		box-shadow: 0 0 0 8px rgba(var(--v-theme-success), 0);
	}
}

.effect-card--active {
	animation: successPulse 2s infinite;
}

@keyframes overlayFadeIn {
	from {
		opacity: 0;
		transform: scale(0.9);
	}
	to {
		opacity: 1;
		transform: scale(1);
	}
}

/* ==================== 响应式调整 ==================== */

@media (max-width: 1280px) {
	.backgrounds-grid {
		grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
		gap: 10px;
	}
}

@media (max-width: 960px) {
	.effect-panel__header {
		padding: 12px;
	}

	.effect-panel__content {
		padding: 12px;
	}

	.effects-grid {
		gap: 10px;
	}

	.effect-card {
		min-height: 70px;
		padding: 12px 8px;
	}

	.backgrounds-grid {
		grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
		gap: 8px;
	}
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
