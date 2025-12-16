<template>
	<v-card class="video-player-card" :elevation="elevation" rounded="lg">
		<!-- 视频容器 -->
		<div
			ref="videoContainer"
			class="video-container"
			@mouseenter="showControls = true"
			@mouseleave="handleMouseLeave"
		>
			<!-- 视频元素 -->
			<video ref="videoRef" :src="src" :poster="poster" class="video-element" @click="togglePlay"></video>

			<!-- 播放按钮覆盖层 -->
			<v-fade-transition>
				<div v-if="!playing || showControls" class="video-overlay" @click="togglePlay">
					<v-btn
						:icon="playing ? 'mdi-pause' : 'mdi-play'"
						size="x-large"
						color="white"
						elevation="6"
						class="play-btn"
					></v-btn>
				</div>
			</v-fade-transition>

			<!-- 控制条 -->
			<v-fade-transition>
				<div v-if="showControls" class="video-controls">
					<!-- 进度条 -->
					<v-progress-linear
						:model-value="progress"
						height="4"
						color="primary"
						class="progress-bar"
						@click="handleSeek"
					></v-progress-linear>

					<!-- 控制按钮 -->
					<div class="controls-bar">
						<div class="controls-left">
							<!-- 播放/暂停 -->
							<v-btn
								:icon="playing ? 'mdi-pause' : 'mdi-play'"
								variant="text"
								size="small"
								color="white"
								@click="togglePlay"
							></v-btn>

							<!-- 音量 -->
							<v-btn
								:icon="muted ? 'mdi-volume-off' : 'mdi-volume-high'"
								variant="text"
								size="small"
								color="white"
								@click="toggleMute"
							></v-btn>

							<!-- 时间显示 -->
							<span class="time-display">
								{{ formatTime(currentTime) }} / {{ formatTime(duration) }}
							</span>
						</div>

						<div class="controls-right">
							<!-- 倍速 -->
							<v-menu offset-y>
								<template #activator="{ props }">
									<v-btn v-bind="props" variant="text" size="small" color="white">
										{{ rate }}x
									</v-btn>
								</template>
								<v-list density="compact">
									<v-list-item
										v-for="speed in [0.5, 0.75, 1, 1.25, 1.5, 2]"
										:key="speed"
										@click="rate = speed"
									>
										<v-list-item-title>{{ speed }}x</v-list-item-title>
									</v-list-item>
								</v-list>
							</v-menu>

							<!-- 全屏 -->
							<v-btn
								:icon="isFullscreen ? 'mdi-fullscreen-exit' : 'mdi-fullscreen'"
								variant="text"
								size="small"
								color="white"
								@click="toggleFullscreen"
							></v-btn>
						</div>
					</div>
				</div>
			</v-fade-transition>
		</div>

		<!-- 底部信息(可选) -->
		<v-card-text v-if="showInfo" class="pa-2">
			<div class="d-flex align-center justify-space-between">
				<span class="text-caption text-medium-emphasis">{{ title }}</span>
				<v-chip size="x-small" color="primary" label>
					{{ formatDuration(duration) }}
				</v-chip>
			</div>
		</v-card-text>
	</v-card>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useMediaControls, useFullscreen, useToggle } from '@vueuse/core'

const props = defineProps({
	src: {
		type: String,
		required: true,
	},
	poster: {
		type: String,
		default: '',
	},
	title: {
		type: String,
		default: '',
	},
	elevation: {
		type: Number,
		default: 2,
	},
	showInfo: {
		type: Boolean,
		default: false,
	},
	autoplay: {
		type: Boolean,
		default: false,
	},
})

// 视频元素引用
const videoRef = ref(null)
const videoContainer = ref(null)

// 使用 VueUse 的 useMediaControls
const { playing, currentTime, duration, volume, muted, rate } = useMediaControls(videoRef, {
	src: props.src,
})

// 使用 VueUse 的 useFullscreen
const { isFullscreen, toggle: toggleFullscreen } = useFullscreen(videoContainer)

// 控制条显示状态
const [showControls, toggleControls] = useToggle(false)
let hideControlsTimer = null

// 播放进度百分比
const progress = computed(() => {
	if (!duration.value) return 0
	return (currentTime.value / duration.value) * 100
})

// 切换播放/暂停
const togglePlay = () => {
	playing.value = !playing.value
	showControls.value = true
	resetHideControlsTimer()
}

// 切换静音
const toggleMute = () => {
	muted.value = !muted.value
}

// 处理进度条点击
const handleSeek = event => {
	const rect = event.currentTarget.getBoundingClientRect()
	const percent = (event.clientX - rect.left) / rect.width
	currentTime.value = percent * duration.value
}

// 格式化时间显示
const formatTime = seconds => {
	if (!seconds || isNaN(seconds)) return '00:00'
	const mins = Math.floor(seconds / 60)
	const secs = Math.floor(seconds % 60)
	return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 格式化时长
const formatDuration = seconds => {
	if (!seconds) return '0秒'
	const mins = Math.floor(seconds / 60)
	if (mins > 0) {
		return `${mins}分${Math.floor(seconds % 60)}秒`
	}
	return `${Math.floor(seconds)}秒`
}

// 鼠标离开时隐藏控制条
const handleMouseLeave = () => {
	if (playing.value) {
		hideControlsTimer = setTimeout(() => {
			showControls.value = false
		}, 2000)
	}
}

// 重置隐藏控制条计时器
const resetHideControlsTimer = () => {
	if (hideControlsTimer) {
		clearTimeout(hideControlsTimer)
	}
	if (playing.value) {
		hideControlsTimer = setTimeout(() => {
			showControls.value = false
		}, 3000)
	}
}

// 监听播放状态
watch(playing, newValue => {
	if (newValue) {
		resetHideControlsTimer()
	} else {
		showControls.value = true
		if (hideControlsTimer) {
			clearTimeout(hideControlsTimer)
		}
	}
})

// 自动播放
if (props.autoplay) {
	playing.value = true
}
</script>

<style scoped>
.video-player-card {
	position: relative;
	overflow: hidden;
	background-color: rgb(var(--v-theme-surface));
}

.video-container {
	position: relative;
	width: 100%;
	background-color: #000;
	cursor: pointer;
}

.video-element {
	width: 100%;
	height: auto;
	display: block;
	max-height: 400px;
	object-fit: contain;
}

.video-overlay {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	background-color: rgba(0, 0, 0, 0.3);
	transition: background-color 0.3s;
}

.play-btn {
	opacity: 0.9;
	transition: all 0.3s;
}

.play-btn:hover {
	opacity: 1;
	transform: scale(1.1);
}

.video-controls {
	position: absolute;
	bottom: 0;
	left: 0;
	right: 0;
	background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
	padding: 8px;
}

.progress-bar {
	cursor: pointer;
	margin-bottom: 8px;
}

.controls-bar {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 4px;
}

.controls-left,
.controls-right {
	display: flex;
	align-items: center;
	gap: 4px;
}

.time-display {
	color: white;
	font-size: 12px;
	margin: 0 8px;
	user-select: none;
}

@media (max-width: 600px) {
	.time-display {
		font-size: 10px;
		margin: 0 4px;
	}
}
</style>
