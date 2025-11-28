<template>
	<div class="control-bar-wrapper" :class="{ collapsed: isCollapsed }">
		<!-- 收起/展开按钮 -->
		<v-btn class="collapse-toggle" icon size="small" color="white" variant="elevated" @click="toggleCollapse">
			<v-icon>{{ isCollapsed ? 'mdi-chevron-up' : 'mdi-chevron-down' }}</v-icon>
		</v-btn>

		<!-- 控制栏主体 -->
		<transition name="slide-up">
			<div v-show="!isCollapsed" class="control-bar">
				<v-container fluid class="pa-0">
					<v-row no-gutters align="center" justify="center">
						<!-- 左侧：连接状态 -->
						<v-col cols="auto" class="d-flex align-center">
							<v-chip :color="connectionQuality.color" variant="flat" size="small" class="ml-4">
								<v-icon left size="small">{{ connectionQuality.icon }}</v-icon>
								{{ connectionQuality.text }}
							</v-chip>
						</v-col>

						<v-spacer></v-spacer>

						<!-- 中间：主要控制按钮 -->
						<v-col cols="auto">
							<div class="control-buttons">
								<!-- 音频控制 -->
								<v-tooltip location="top">
									<template #activator="{ props }">
										<v-btn
											v-bind="props"
											:icon="audioEnabled ? 'mdi-microphone' : 'mdi-microphone-off'"
											:color="audioEnabled ? 'white' : 'error'"
											:variant="audioEnabled ? 'text' : 'flat'"
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
											:color="videoEnabled ? 'white' : 'error'"
											:variant="videoEnabled ? 'text' : 'flat'"
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
											:color="screenSharing ? 'success' : 'white'"
											:variant="screenSharing ? 'flat' : 'text'"
											size="large"
											class="control-btn"
											@click="toggleScreenShare"
										></v-btn>
									</template>
									<span>{{ screenSharing ? '停止共享' : '共享屏幕' }}</span>
								</v-tooltip>

								<!-- 录制 -->
								<v-tooltip location="top">
									<template #activator="{ props }">
										<v-btn
											v-bind="props"
											:icon="isRecording ? 'mdi-record-rec' : 'mdi-record-circle-outline'"
											:color="isRecording ? 'error' : 'white'"
											:variant="isRecording ? 'flat' : 'text'"
											size="large"
											class="control-btn"
											@click="toggleRecording"
										></v-btn>
									</template>
									<span>{{ isRecording ? '停止录制' : '开始录制' }}</span>
								</v-tooltip>

								<!-- 更多选项 -->
								<v-menu offset-y location="top">
									<template #activator="{ props: menuProps }">
										<v-tooltip location="top">
											<template #activator="{ props: tooltipProps }">
												<v-btn
													v-bind="mergeProps(menuProps, tooltipProps)"
													icon="mdi-dots-horizontal"
													color="white"
													variant="text"
													size="large"
													class="control-btn"
												></v-btn>
											</template>
											<span>更多选项</span>
										</v-tooltip>
									</template>

									<v-list density="compact" class="more-menu">
										<v-list-item @click="emit('toggle-layout')">
											<template #prepend>
												<v-icon>mdi-view-grid</v-icon>
											</template>
											<v-list-item-title>切换布局</v-list-item-title>
										</v-list-item>

										<v-list-item @click="toggleHandRaise">
											<template #prepend>
												<v-icon :color="handRaised ? 'warning' : ''">
													{{
														handRaised
															? 'mdi-hand-back-right'
															: 'mdi-hand-back-right-outline'
													}}
												</v-icon>
											</template>
											<v-list-item-title>
												{{ handRaised ? '放下手' : '举手' }}
											</v-list-item-title>
										</v-list-item>

										<v-list-item @click="emit('open-settings')">
											<template #prepend>
												<v-icon>mdi-cog</v-icon>
											</template>
											<v-list-item-title>设置</v-list-item-title>
										</v-list-item>

										<v-divider></v-divider>

										<v-list-item @click="toggleFullscreen">
											<template #prepend>
												<v-icon>
													{{ isFullscreen ? 'mdi-fullscreen-exit' : 'mdi-fullscreen' }}
												</v-icon>
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
											color="error"
											variant="flat"
											size="large"
											class="control-btn leave-btn"
											@click="emit('leave-meeting')"
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
										:color="showSidebar ? 'primary' : 'white'"
										:variant="showSidebar ? 'flat' : 'text'"
										size="large"
										class="mr-2"
										@click="emit('toggle-sidebar')"
									></v-btn>
								</template>
								<span>{{ showSidebar ? '隐藏侧边栏' : '显示侧边栏' }}</span>
							</v-tooltip>

							<v-badge
								:content="unreadCount"
								:model-value="unreadCount > 0"
								color="error"
								overlap
								class="mr-4"
							>
								<v-tooltip location="top">
									<template #activator="{ props }">
										<v-btn
											v-bind="props"
											icon="mdi-chat"
											color="white"
											variant="text"
											size="large"
											@click="emit('toggle-sidebar-chat')"
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
</template>

<script setup>
import { ref, computed, mergeProps } from 'vue'
import { useFullscreen } from '@vueuse/core'

const props = defineProps({
	audioEnabled: Boolean,
	videoEnabled: Boolean,
	screenSharing: Boolean,
	showSidebar: Boolean,
	collapsed: Boolean,
	unreadCount: {
		type: Number,
		default: 0,
	},
})

const emit = defineEmits([
	'update:audioEnabled',
	'update:videoEnabled',
	'update:screenSharing',
	'update:collapsed',
	'toggle-sidebar',
	'toggle-sidebar-chat',
	'leave-meeting',
	'toggle-layout',
	'open-settings',
])

const isCollapsed = ref(props.collapsed)
const isRecording = ref(false)
const handRaised = ref(false)

const { isFullscreen, toggle: toggleFullscreen } = useFullscreen()

// 连接质量状态
const connectionQuality = computed(() => {
	// 这里可以根据实际网络质量动态计算
	return {
		color: 'success',
		icon: 'mdi-wifi-strength-4',
		text: '连接良好',
	}
})

const toggleCollapse = () => {
	isCollapsed.value = !isCollapsed.value
	emit('update:collapsed', isCollapsed.value)
}

const toggleAudio = () => {
	emit('update:audioEnabled', !props.audioEnabled)
}

const toggleVideo = () => {
	emit('update:videoEnabled', !props.videoEnabled)
}

const toggleScreenShare = () => {
	emit('update:screenSharing', !props.screenSharing)
}

const toggleRecording = () => {
	isRecording.value = !isRecording.value
	// 预留API: 开始/停止录制
}

const toggleHandRaise = () => {
	handRaised.value = !handRaised.value
	// 预留API: 举手/放下手
}
</script>

<style scoped>
.control-bar-wrapper {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	z-index: 100;
}

.control-bar-wrapper.collapsed .collapse-toggle {
	top: -36px;
}

.control-bar-wrapper:not(.collapsed) .collapse-toggle {
	top: -36px;
}

.collapse-toggle {
	position: absolute;
	left: 50%;
	transform: translateX(-50%);
	box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.3);
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
	z-index: 1;
}

.control-bar {
	background: linear-gradient(
		to top,
		rgba(30, 30, 30, 0.98) 0%,
		rgba(30, 30, 30, 0.95) 50%,
		rgba(30, 30, 30, 0.85) 100%
	);
	backdrop-filter: blur(10px);
	border-top: 1px solid rgba(255, 255, 255, 0.1);
	padding: 16px 0;
	box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.4);
}

.control-buttons {
	display: flex;
	gap: 12px;
	align-items: center;
}

.control-btn {
	transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.control-btn:hover {
	transform: translateY(-2px);
	box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.control-btn:active {
	transform: translateY(0);
}

.leave-btn {
	margin-left: 8px;
	background: linear-gradient(135deg, #f44336 0%, #d32f2f 100%) !important;
}

.leave-btn:hover {
	background: linear-gradient(135deg, #e53935 0%, #c62828 100%) !important;
}

.more-menu {
	background-color: #2d2d2d;
	border: 1px solid rgba(255, 255, 255, 0.12);
}

.more-menu .v-list-item:hover {
	background-color: rgba(255, 255, 255, 0.08);
}

/* 动画 */
.slide-up-enter-active,
.slide-up-leave-active {
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-up-enter-from,
.slide-up-leave-to {
	opacity: 0;
	transform: translateY(20px);
}
</style>
