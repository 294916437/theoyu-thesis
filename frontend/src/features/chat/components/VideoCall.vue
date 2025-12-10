<template>
	<div v-if="isActive" class="video-call-overlay">
		<div class="video-call-container">
			<!-- 远程视频 -->
			<video ref="remoteVideo" autoplay playsinline class="remote-video" />

			<!-- 本地视频（画中画） -->
			<video ref="localVideo" autoplay playsinline muted class="local-video" />

			<!-- 通话信息 -->
			<div class="call-info">
				<div class="user-name">{{ remoteName }}</div>
				<div class="call-status" :class="callStatusClass">
					{{ callStatusText }}
				</div>
				<div v-if="showDuration" class="call-duration">{{ formattedDuration }}</div>
			</div>

			<!-- 控制按钮 -->
			<div class="controls">
				<button class="control-btn" :class="{ active: !isAudioMuted }" @click="toggleAudio">
					<svg v-if="!isAudioMuted" class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
						<path
							d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"
						/>
						<path
							d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"
						/>
					</svg>
					<svg v-else class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
						<path
							d="M19 11h-1.7c0 .74-.16 1.43-.43 2.05l1.23 1.23c.56-.98.9-2.09.9-3.28zm-4.02.17c0-.06.02-.11.02-.17V5c0-1.66-1.34-3-3-3S9 3.34 9 5v.18l5.98 5.99zM4.27 3L3 4.27l6.01 6.01V11c0 1.66 1.33 3 2.99 3 .22 0 .44-.03.65-.08l1.66 1.66c-.71.33-1.5.52-2.31.52-2.76 0-5.3-2.1-5.3-5.1H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c.91-.13 1.77-.45 2.54-.9L19.73 21 21 19.73 4.27 3z"
						/>
					</svg>
				</button>

				<button class="control-btn" :class="{ active: !isVideoMuted }" @click="toggleVideo">
					<svg v-if="!isVideoMuted" class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
						<path
							d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z"
						/>
					</svg>
					<svg v-else class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
						<path
							d="M21 6.5l-4 4V7c0-.55-.45-1-1-1H9.82L21 17.18V6.5zM3.27 2L2 3.27 4.73 6H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.21 0 .39-.08.54-.18L19.73 21 21 19.73 3.27 2z"
						/>
					</svg>
				</button>
				<button class="control-btn end-call" @click="endCall">
					<svg class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
						<path
							d="M12 9c-1.6 0-3.15.25-4.6.72v3.1c0 .39-.23.74-.56.9-.98.49-1.87 1.12-2.66 1.85-.18.18-.43.28-.7.28-.28 0-.53-.11-.71-.29L.29 13.08c-.18-.17-.29-.42-.29-.7 0-.28.11-.53.29-.71C3.34 8.78 7.46 7 12 7s8.66 1.78 11.71 4.67c.18.18.29.43.29.71 0 .28-.11.53-.29.71l-2.48 2.48c-.18.18-.43.29-.71.29-.27 0-.52-.11-.7-.28-.79-.74-1.68-1.36-2.66-1.85-.33-.16-.56-.5-.56-.9v-3.1C15.15 9.25 13.6 9 12 9z"
						/>
					</svg>
				</button>
			</div>
		</div>
	</div>
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

// 通话状态样式类
const callStatusClass = computed(() => {
	return {
		'status-calling': props.callState === 'calling',
		'status-connected': props.callState === 'connected',
		'status-ended': props.callState === 'ended',
	}
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
		// ✅ 等待 DOM 更新完成
		await nextTick()

		if (localVideo.value) {
			if (newStream) {
				localVideo.value.srcObject = newStream
				// ✅ 手动触发播放（某些浏览器需要）
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
.video-call-overlay {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: #000;
	z-index: 9999;
	display: flex;
	align-items: center;
	justify-content: center;
}

.video-call-container {
	position: relative;
	width: 100%;
	height: 100%;
}

.remote-video {
	width: 100%;
	height: 100%;
	object-fit: cover;
	background: #1a1a1a;
}

.local-video {
	position: absolute;
	top: 20px;
	right: 20px;
	width: 200px;
	height: 150px;
	object-fit: cover;
	border-radius: 8px;
	border: 2px solid #fff;
	box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
	background: #2a2a2a;
	z-index: 10;
}

/* 移动端适配 */
@media (max-width: 640px) {
	.local-video {
		width: 120px;
		height: 90px;
		top: 10px;
		right: 10px;
	}
}

.call-info {
	position: absolute;
	top: 20px;
	left: 20px;
	color: #fff;
	text-shadow: 0 2px 4px rgba(0, 0, 0, 0.8);
	z-index: 10;
}

.user-name {
	font-size: 1.5rem;
	font-weight: 600;
	margin-bottom: 4px;
}

.call-status {
	font-size: 0.9rem;
	opacity: 0.9;
	margin-bottom: 4px;
	padding: 2px 8px;
	border-radius: 4px;
	display: inline-block;
}

.status-calling,
.status-ringing {
	background: rgba(59, 130, 246, 0.3);
	animation: pulse 1.5s ease-in-out infinite;
}

.status-connected {
	background: rgba(34, 197, 94, 0.3);
}

.status-ended {
	background: rgba(239, 68, 68, 0.3);
}

@keyframes pulse {
	0%,
	100% {
		opacity: 1;
	}
	50% {
		opacity: 0.5;
	}
}

.call-duration {
	font-size: 1rem;
	font-weight: 500;
	opacity: 0.9;
}

.controls {
	position: absolute;
	bottom: 40px;
	left: 50%;
	transform: translateX(-50%);
	display: flex;
	gap: 20px;
	z-index: 10;
}

@media (max-width: 640px) {
	.controls {
		gap: 12px;
		bottom: 20px;
	}
}

.control-btn {
	width: 60px;
	height: 60px;
	border-radius: 50%;
	background: rgba(255, 255, 255, 0.2);
	backdrop-filter: blur(10px);
	border: 2px solid rgba(255, 255, 255, 0.3);
	color: #fff;
	display: flex;
	align-items: center;
	justify-content: center;
	cursor: pointer;
	transition: all 0.3s ease;
}

@media (max-width: 640px) {
	.control-btn {
		width: 50px;
		height: 50px;
	}
}

.control-btn:hover {
	background: rgba(255, 255, 255, 0.3);
	transform: scale(1.1);
}

.control-btn:active {
	transform: scale(0.95);
}

.control-btn.active {
	background: rgb(59 130 246);
	border-color: rgb(59 130 246);
}

.control-btn.end-call {
	background: rgb(239 68 68);
	border-color: rgb(239 68 68);
}

.control-btn.end-call:hover {
	background: rgb(220 38 38);
}

/* 禁用状态样式 */
.control-btn:disabled {
	opacity: 0.5;
	cursor: not-allowed;
}

.control-btn:disabled:hover {
	transform: none;
}
</style>
