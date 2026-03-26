<template>
	<v-overlay :model-value="phase !== 'hidden'" class="meeting-entry-overlay align-center justify-center" persistent scrim="#000" :opacity="1" :transition="false">
		<!-- ========== Consent 阶段 ========== -->
		<transition name="phase-fade" mode="out-in">
			<!-- ========== Not Found 阶段 (会议不存在) ========== -->
			<div v-if="phase === 'not-found'" key="not-found" class="phase-content">
				<v-card class="md-card text-center pa-8" max-width="420" elevation="0">
					<div class="md-icon-wrapper error-wrapper mb-6">
						<v-icon icon="mdi-alert-circle-outline" size="48" color="error"></v-icon>
					</div>
					<v-card-title class="text-h5 font-weight-bold px-0 mb-2">会议不存在</v-card-title>
					<v-card-text class="pa-0 mb-8 text-body-1 text-medium-emphasis"> 该会议房间可能不存在或已被结束，请检查会议号是否正确。 </v-card-text>
					<v-card-actions class="px-0 justify-center">
						<v-btn variant="flat" color="primary" size="large" rounded="pill" block @click="handleExitWaiting"> 返回首页 </v-btn>
					</v-card-actions>
				</v-card>
			</div>
			<!-- ========== Waiting 阶段 (等候室) ========== -->
			<div v-else-if="phase === 'waiting'" key="waiting" class="phase-content">
				<v-card class="consent-card text-center pa-6" max-width="480" elevation="0">
					<div class="d-flex justify-center mb-4">
						<v-icon icon="mdi-calendar-clock" size="64" color="primary"></v-icon>
					</div>
					<v-card-title class="text-h5 font-weight-bold px-0 mb-2">会议尚未开始</v-card-title>
					<v-card-text class="pa-0 mb-6">
						<div class="text-body-1 text-medium-emphasis mb-4">会议在开始前 30 分钟内方可加入房间。</div>
						<div class="pa-4 bg-surface-variant rounded-lg">
							<div class="text-caption text-medium-emphasis mb-1">距离可加入时间还有</div>
							<div class="text-h4 font-weight-bold text-primary" style="font-variant-numeric: tabular-nums">
								{{ waitingCountdownStr }}
							</div>
						</div>
					</v-card-text>
					<v-card-actions class="px-0 ga-3 justify-center">
						<v-btn variant="tonal" color="error" size="large" rounded="lg" class="flex-1-1" @click="handleExitWaiting"> 离开房间 </v-btn>
						<v-btn variant="flat" color="primary" size="large" rounded="lg" class="flex-1-1" :loading="isRetrying" @click="handleRetryJoin"> 重新加入 </v-btn>
					</v-card-actions>
				</v-card>
			</div>
			<div v-else-if="phase === 'consent'" key="consent" class="phase-content">
				<v-card class="consent-card text-center pa-6" max-width="560" elevation="0">
					<div class="d-flex justify-center mb-4">
						<v-img src="/audioorvideo.svg" width="220" height="160" contain></v-img>
					</div>

					<v-card-title class="text-h6 font-weight-medium px-4 text-wrap"> 你想让他人在会议中看到你并听到你的声音吗？ </v-card-title>

					<v-card-text class="text-body-2 text-medium-emphasis pb-4"> 你仍可以在会议期间随时关闭麦克风和摄像头。 </v-card-text>

					<v-card-actions class="flex-column ga-3 px-4 pb-4">
						<v-btn color="primary" variant="flat" block size="large" rounded="pill" prepend-icon="mdi-monitor-share" @click="requestAndJoin">
							使用麦克风和摄像头
						</v-btn>

						<v-btn variant="text" color="primary" block size="default" @click="joinAsListener"> 在不使用麦克风和摄像头的情况下继续 </v-btn>
					</v-card-actions>
				</v-card>
			</div>

			<!-- ========== Loading 阶段 ========== -->
			<div v-else-if="phase === 'loading'" key="loading" class="phase-content">
				<v-progress-circular :indeterminate="!loadingProgress" :model-value="loadingProgress" :size="80" :width="6" color="primary">
					<span v-if="loadingProgress" class="text-h6">{{ loadingProgress }}%</span>
				</v-progress-circular>

				<div class="text-h6 text-white mt-6">{{ loadingMessage }}</div>

				<!-- 离开场景无进度时的补充提示 -->
				<div v-if="!loadingProgress" class="text-body-2 text-white mt-2" style="opacity: 0.6">请稍候...</div>
			</div>
		</transition>
	</v-overlay>
</template>

<script setup>
import { ref, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
const props = defineProps({
	phase: {
		type: String,
		default: 'loading', //  'loading' |'consent' | 'hidden' | 'waiting' | 'not-found'
	},
	loadingMessage: {
		type: String,
		default: '正在初始化...',
	},
	loadingProgress: {
		type: Number,
		default: 0,
	},
	meetingInfo: {
		type: Object,
		default: () => ({}),
	},
	isRetrying: {
		type: Boolean,
		default: false,
	},
})

const emit = defineEmits(['confirm', 'retry', 'timeup'])

const router = useRouter()
const waitingCountdownStr = ref('00:00:00')
const WAITING_THRESHOLD_MS = 30 * 60 * 1000 // 30分钟
let waitingTimer = null

// 等候室计时逻辑
const startWaitingTimer = () => {
	if (waitingTimer) clearInterval(waitingTimer)
	if (!props.meetingInfo?.startTime) return

	const updateTimer = () => {
		const diff = new Date(props.meetingInfo.startTime).getTime() - Date.now() - WAITING_THRESHOLD_MS
		// 如果倒计时结束自然到时间，直接通知父组件放行进入consent阶段
		if (diff <= 0) {
			clearInterval(waitingTimer)
			emit('timeup')
			return
		}
		const d = Math.floor(diff / (1000 * 60 * 60 * 24))
		const h = Math.floor((diff / (1000 * 60 * 60)) % 24)
		const m = Math.floor((diff / 1000 / 60) % 60)
		const s = Math.floor((diff / 1000) % 60)
		const timeParts = [h.toString().padStart(2, '0'), m.toString().padStart(2, '0'), s.toString().padStart(2, '0')]
		waitingCountdownStr.value = d > 0 ? `${d}天 ${timeParts.join(':')}` : timeParts.join(':')
	}
	updateTimer()
	waitingTimer = setInterval(updateTimer, 1000)
}

// 监听状态，只有在waiting阶段才走计时
watch(
	() => props.phase,
	newPhase => {
		if (newPhase === 'waiting') {
			startWaitingTimer()
		} else if (waitingTimer) {
			clearInterval(waitingTimer)
			waitingTimer = null
		}
	},
	{ immediate: true },
)

onBeforeUnmount(() => {
	if (waitingTimer) clearInterval(waitingTimer)
})

const requestAndJoin = () => {
	emit('confirm', { withMedia: true })
}
const handleExitWaiting = () => router.push('/')
const handleRetryJoin = () => emit('retry')

const joinAsListener = () => {
	emit('confirm', { withMedia: false })
}
</script>

<style scoped>
.meeting-entry-overlay {
	z-index: 9999;
}

.phase-content {
	display: flex;
	flex-direction: column;
	align-items: center;
}

.consent-card {
	border-radius: 16px !important;
	background: rgb(var(--v-theme-surface)) !important;
}
.text-wrap {
	white-space: normal;
	line-height: 1.5;
}
.md-card {
	border-radius: 24px !important;
	background: rgb(var(--v-theme-surface)) !important;
	box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12) !important;
	border: 1px solid rgba(var(--v-theme-border), 0.5);
}

.md-icon-wrapper {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	width: 88px;
	height: 88px;
	border-radius: 50%;
	margin: 0 auto;
	transition: transform 0.3s ease;
}

.md-icon-wrapper:hover {
	transform: scale(1.05);
}

.md-icon-wrapper.primary-wrapper {
	background: rgba(var(--v-theme-primary), 0.08);
	border: 2px solid rgba(var(--v-theme-primary), 0.15);
}

.md-icon-wrapper.error-wrapper {
	background: rgba(var(--v-theme-error), 0.08);
	border: 2px solid rgba(var(--v-theme-error), 0.15);
}
/* 等候室倒计时区块容器 */
.countdown-container {
	background: rgba(var(--v-theme-surface-variant), 0.5);
	border-radius: 16px;
	padding: 24px 20px;
	position: relative;
	overflow: hidden;
}

.countdown-container::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	height: 4px;
	background: rgb(var(--v-theme-primary));
	opacity: 0.8;
}

.countdown-label {
	display: block;
	font-size: 0.85rem;
	color: rgb(var(--v-theme-on-surface-variant));
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 1px;
	margin-bottom: 8px;
}

.countdown-value {
	font-size: 2.75rem;
	font-weight: 700;
	font-variant-numeric: tabular-nums;
	letter-spacing: 2px;
	line-height: 1.2;
	text-shadow: 0 4px 12px rgba(var(--v-theme-primary), 0.15);
}

/* consent → loading 切换的淡入淡出 */
.phase-fade-enter-active,
.phase-fade-leave-active {
	transition: opacity 0.3s ease;
}

.phase-fade-enter-from,
.phase-fade-leave-to {
	opacity: 0;
}
</style>
