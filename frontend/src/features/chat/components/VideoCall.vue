<template>
	<v-dialog v-model="isActive" fullscreen persistent :scrim="false" transition="dialog-bottom-transition">
		<v-card class="video-call-card" elevation="0">
			<!-- 远程视频区域 -->
			<div class="video-container">
				<video ref="remoteVideo" autoplay playsinline class="remote-video" />

				<!-- 远程视频占位符 -->
				<v-overlay v-if="!remoteStream" :model-value="true" contained class="video-placeholder" persistent>
					<div class="d-flex flex-column align-center">
						<v-avatar color="primary" size="120" class="mb-4">
							<v-icon size="64" color="white"> mdi-account </v-icon>
						</v-avatar>
						<v-chip color="primary" variant="flat" size="large">
							{{ callStatusText }}
						</v-chip>
					</div>
				</v-overlay>

				<!-- 本地视频(画中画) -->
				<v-card class="local-video-card" elevation="8" rounded="lg">
					<video ref="localVideo" autoplay playsinline muted class="local-video" />

					<!-- 本地视频占位符 -->
					<v-overlay
						v-if="!localStream || isVideoMuted"
						:model-value="true"
						contained
						class="local-placeholder"
					>
						<v-avatar color="secondary" size="48">
							<v-icon size="24" color="white"> mdi-account-outline </v-icon>
						</v-avatar>
					</v-overlay>
				</v-card>

				<!-- 通话信息面板 -->
				<v-card class="call-info-card" elevation="4" rounded="lg">
					<v-card-text class="pa-4">
						<div class="text-h6 font-weight-bold text-white mb-1">
							{{ remoteName }}
						</div>
						<v-chip :color="getStatusColor()" variant="flat" size="small" class="mb-2">
							<v-icon start :icon="getStatusIcon()" size="small" />
							{{ callStatusText }}
						</v-chip>
						<div v-if="showDuration && props.callState === 'connected'" class="text-body-2 text-white">
							<v-icon size="small" class="mr-1">mdi-clock-outline</v-icon>
							{{ formattedDuration }}
						</div>
					</v-card-text>
				</v-card>

				<!-- 控制按钮栏 -->
				<div class="controls-wrapper">
					<v-card class="controls-card" elevation="8" rounded="pill">
						<v-card-text class="pa-2 d-flex align-center justify-center ga-3">
							<!-- 音频控制 -->
							<v-tooltip location="top" :text="isAudioMuted ? '开启麦克风' : '关闭麦克风'">
								<template #activator="{ props: tooltipProps }">
									<v-btn
										v-bind="tooltipProps"
										:color="isAudioMuted ? 'error' : 'primary'"
										:variant="isAudioMuted ? 'flat' : 'tonal'"
										icon
										size="large"
										@click="toggleAudio"
									>
										<v-icon>
											{{ isAudioMuted ? 'mdi-microphone-off' : 'mdi-microphone' }}
										</v-icon>
									</v-btn>
								</template>
							</v-tooltip>

							<!-- 视频控制 -->
							<v-tooltip location="top" :text="isVideoMuted ? '开启摄像头' : '关闭摄像头'">
								<template #activator="{ props: tooltipProps }">
									<v-btn
										v-bind="tooltipProps"
										:color="isVideoMuted ? 'error' : 'primary'"
										:variant="isVideoMuted ? 'flat' : 'tonal'"
										icon
										size="large"
										@click="toggleVideo"
									>
										<v-icon>
											{{ isVideoMuted ? 'mdi-video-off' : 'mdi-video' }}
										</v-icon>
									</v-btn>
								</template>
							</v-tooltip>

							<!-- 结束通话 -->
							<v-tooltip location="top" text="结束通话">
								<template #activator="{ props: tooltipProps }">
									<v-btn
										v-bind="tooltipProps"
										color="error"
										variant="flat"
										icon
										size="x-large"
										class="end-call-btn"
										@click="endCall"
									>
										<v-icon size="large"> mdi-phone-hangup </v-icon>
									</v-btn>
								</template>
							</v-tooltip>
						</v-card-text>
					</v-card>
				</div>
			</div>
		</v-card>
	</v-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'

/**
 * Props 定义
 */
const props = defineProps({
	// 是否激活视频通话
	isActive: {
		type: Boolean,
		default: false,
	},
	// 本地媒体流
	localStream: {
		type: [MediaStream, null],
		default: null,
	},
	// 远程媒体流
	remoteStream: {
		type: [MediaStream, null],
		default: null,
	},
	// 远程用户名称
	remoteName: {
		type: String,
		default: '用户',
	},
	callState: {
		type: String,
		default: 'idle',
		validator: value => ['idle', 'calling', 'connected', 'ended'].includes(value),
	},
	// 是否显示通话时长
	showDuration: {
		type: Boolean,
		default: true,
	},
	// 初始音频状态（静音/非静音）
	initialAudioMuted: {
		type: Boolean,
		default: false,
	},
	// 初始视频状态（关闭/开启）
	initialVideoMuted: {
		type: Boolean,
		default: false,
	},
})

/**
 * Emits 定义
 */
const emit = defineEmits({
	// 结束通话
	end: null,
	// 切换音频（参数：是否启用音频）
	'toggle-audio': enabled => typeof enabled === 'boolean',
	// 切换视频（参数：是否启用视频）
	'toggle-video': enabled => typeof enabled === 'boolean',
	// 通话状态变化
	'state-change': state => typeof state === 'string',
	// 错误事件
	error: error => error instanceof Error || typeof error === 'string',
})

/**
 * 响应式数据
 */
const localVideo = ref(null)
const remoteVideo = ref(null)
const isAudioMuted = ref(props.initialAudioMuted)
const isVideoMuted = ref(props.initialVideoMuted)
const callDuration = ref(0)
const durationInterval = ref(null)

// 格式化通话时长
const formattedDuration = computed(() => {
	const hours = Math.floor(callDuration.value / 3600)
	const minutes = Math.floor((callDuration.value % 3600) / 60)
	const seconds = callDuration.value % 60

	if (hours > 0) {
		return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
	}
	return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

// 获取状态图标
const getStatusIcon = () => {
	const iconMap = {
		idle: 'mdi-clock-outline',
		calling: 'mdi-phone-ring',
		connected: 'mdi-phone-in-talk',
		ended: 'mdi-phone-hangup',
	}
	return iconMap[props.callState] || 'mdi-help-circle'
}

// 获取状态颜色
const getStatusColor = () => {
	const colorMap = {
		idle: 'grey',
		calling: 'info',
		connected: 'success',
		ended: 'error',
	}
	return colorMap[props.callState] || 'grey'
}

// 通话状态文本
const callStatusText = computed(() => {
	const statusMap = {
		idle: '准备中...',
		calling: '呼叫中...',
		connected: '通话中',
		ended: '已结束',
	}
	return statusMap[props.callState] || '未知状态'
})

/**
 * 方法
 */

// 切换音频
const toggleAudio = () => {
	isAudioMuted.value = !isAudioMuted.value
	emit('toggle-audio', !isAudioMuted.value)
}

// 切换视频
const toggleVideo = () => {
	isVideoMuted.value = !isVideoMuted.value
	emit('toggle-video', !isVideoMuted.value)
}

// 结束通话
const endCall = () => {
	emit('end')
}

// 开始计时
const startDurationTimer = () => {
	if (durationInterval.value) {
		stopDurationTimer()
	}
	durationInterval.value = setInterval(() => {
		callDuration.value++
	}, 1000)
}

// 停止计时
const stopDurationTimer = () => {
	if (durationInterval.value) {
		clearInterval(durationInterval.value)
		durationInterval.value = null
	}
	callDuration.value = 0
}

/**
 * 监听器
 */

// 监听本地视频流变化
watch(
	() => props.localStream,
	async newStream => {
		// 等待 DOM 更新完成
		await nextTick()

		if (localVideo.value) {
			if (newStream) {
				localVideo.value.srcObject = newStream
				// 手动触发播放（某些浏览器需要）
				await localVideo.value.play()
			} else {
				localVideo.value.srcObject = null
			}
		}
	},
	{ immediate: true },
)

// 监听远程视频流变化
watch(
	() => props.remoteStream,
	newStream => {
		if (remoteVideo.value) {
			if (newStream) {
				remoteVideo.value.srcObject = newStream
				console.log('远程视频流已设置')
			} else {
				remoteVideo.value.srcObject = null
			}
		}
	},
	{ immediate: true },
)

// 监听通话状态变化
watch(
	() => props.callState,
	(newState, oldState) => {
		console.log(`通话状态变化: ${oldState} -> ${newState}`)
		emit('state-change', newState)

		// 仅在连接状态时开始计时
		if (newState === 'connected') {
			startDurationTimer()
		} else if (newState === 'ended' || newState === 'idle') {
			stopDurationTimer()
		}
	},
)

// 监听激活状态
watch(
	() => props.isActive,
	isActive => {
		if (!isActive) {
			stopDurationTimer()
			// 清理视频元素
			if (localVideo.value) {
				localVideo.value.srcObject = null
			}
			if (remoteVideo.value) {
				remoteVideo.value.srcObject = null
			}
		}
	},
)

/**
 * 生命周期钩子
 */
onMounted(() => {
	console.log('VideoCall 组件已挂载')

	// 处理视频元素加载错误
	if (localVideo.value) {
		localVideo.value.onerror = error => {
			console.error('本地视频加载错误:', error)
			emit('error', new Error('本地视频加载失败'))
		}
	}

	if (remoteVideo.value) {
		remoteVideo.value.onerror = error => {
			console.error('远程视频加载错误:', error)
			emit('error', new Error('远程视频加载失败'))
		}
	}

	// 如果已激活且为连接状态，开始计时
	if (props.isActive && props.callState === 'connected') {
		startDurationTimer()
	}
})

onBeforeUnmount(() => {
	console.log('VideoCall 组件即将卸载')
	stopDurationTimer()

	// 清理视频流
	if (localVideo.value && localVideo.value.srcObject) {
		const stream = localVideo.value.srcObject
		stream.getTracks().forEach(track => track.stop())
		localVideo.value.srcObject = null
	}

	if (remoteVideo.value && remoteVideo.value.srcObject) {
		remoteVideo.value.srcObject = null
	}
})
</script>

<style scoped>
.video-call-card {
	background: #000;
	width: 100%;
	height: 100%;
}

.video-container {
	position: relative;
	width: 100%;
	height: 100%;
	overflow: hidden;
}

/* 远程视频 */
.remote-video {
	width: 100%;
	height: 100%;
	object-fit: cover;
	background: rgb(var(--v-theme-surface-variant));
}

/* 视频占位符 */
.video-placeholder {
	background: linear-gradient(135deg, rgb(var(--v-theme-primary)) 0%, rgb(var(--v-theme-secondary)) 100%);
}

/* 本地视频卡片 */
.local-video-card {
	position: absolute;
	top: 24px;
	right: 24px;
	width: 240px;
	height: 180px;
	overflow: hidden;
	z-index: 10;
	background: rgb(var(--v-theme-surface-variant));
	border: 2px solid rgba(255, 255, 255, 0.2);
}

.local-video {
	width: 100%;
	height: 100%;
	object-fit: cover;
}

.local-placeholder {
	background: rgba(var(--v-theme-surface-variant), 0.8);
	backdrop-filter: blur(8px);
}

/* 移动端适配 */
@media (max-width: 600px) {
	.local-video-card {
		width: 120px;
		height: 90px;
		top: 16px;
		right: 16px;
	}
}

/* 通话信息卡片 */
.call-info-card {
	position: absolute;
	top: 24px;
	left: 24px;
	background: rgba(var(--v-theme-surface), 0.15);
	backdrop-filter: blur(16px);
	border: 1px solid rgba(255, 255, 255, 0.1);
	z-index: 10;
	max-width: 280px;
}

@media (max-width: 600px) {
	.call-info-card {
		top: 16px;
		left: 16px;
		max-width: calc(100% - 160px);
	}
}

/* 控制按钮包装器 */
.controls-wrapper {
	position: absolute;
	bottom: 48px;
	left: 50%;
	transform: translateX(-50%);
	z-index: 10;
}

.controls-card {
	background: rgba(var(--v-theme-surface), 0.15);
	backdrop-filter: blur(20px);
	border: 1px solid rgba(255, 255, 255, 0.1);
	padding: 4px;
}

@media (max-width: 600px) {
	.controls-wrapper {
		bottom: 24px;
	}
}

/* 结束通话按钮特殊样式 */
.end-call-btn {
	box-shadow: 0 4px 12px rgba(var(--v-theme-error), 0.4) !important;
}

.end-call-btn:hover {
	box-shadow: 0 6px 16px rgba(var(--v-theme-error), 0.5) !important;
}

/* 动画效果 */
.call-info-card,
.local-video-card,
.controls-card {
	animation: slideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes slideIn {
	from {
		opacity: 0;
		transform: translateY(20px);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}

/* 脉动动画(呼叫中状态) */
@keyframes pulse {
	0%,
	100% {
		opacity: 1;
	}
	50% {
		opacity: 0.6;
	}
}
</style>
