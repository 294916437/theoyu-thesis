<template>
	<div class="video-grid-container">
		<div class="video-grid" :class="gridLayoutClass">
			<!-- 本地视频 -->
			<div
				class="video-tile"
				:class="{
					'is-local': true,
					'video-disabled': !localVideoEnabled,
					...tileClass,
				}"
			>
				<!-- 视频元素 -->
				<video ref="localVideoElement" autoplay playsinline muted class="video-element"></video>

				<!-- 无视频时的占位符 -->
				<div v-if="!localVideoEnabled" class="video-placeholder">
					<v-avatar :size="avatarSize" color="primary">
						<span :class="avatarTextClass">我</span>
					</v-avatar>
				</div>

				<!-- 用户信息覆盖层 -->
				<div class="video-overlay">
					<div class="user-info">
						<v-chip size="small" color="primary" variant="flat" class="user-name-chip">
							<template #prepend>
								<v-icon v-if="!localAudioEnabled" size="x-small" color="error"> mdi-microphone-off </v-icon>
							</template>
							我 (本地)
						</v-chip>
					</div>

					<!-- 连接质量指示器 -->
					<div v-if="showConnectionQuality" class="connection-quality">
						<v-tooltip location="top">
							<template #activator="{ props: tooltipProps }">
								<v-icon v-bind="tooltipProps" size="small" :color="localConnectionQuality.color">
									{{ localConnectionQuality.icon }}
								</v-icon>
							</template>
							<span>{{ localConnectionQuality.text }}</span>
						</v-tooltip>
					</div>
				</div>
			</div>

			<!-- 远程参与者视频 -->
			<div
				v-for="participant in visibleParticipants"
				:key="participant.id"
				class="video-tile"
				:class="{
					'is-speaking': participant.isSpeaking,
					'video-disabled': !participant.videoEnabled,
					'is-screen-sharing': participant.isScreenSharing,
					...tileClass,
				}"
			>
				<!-- 视频元素 -->
				<video :ref="el => setVideoRef(el, participant.peerId)" autoplay playsinline :muted="false" class="video-element" />

				<!-- 无视频时的占位符 -->
				<div v-if="!hasValidVideoTrack(participant)" class="video-placeholder">
					<v-avatar :size="avatarSize" color="secondary">
						<span :class="avatarTextClass">{{ participant.name }}</span>
					</v-avatar>
				</div>

				<!-- 用户信息覆盖层 -->
				<div class="video-overlay">
					<div class="user-info">
						<v-chip
							size="small"
							:color="participant.isSpeaking ? 'success' : 'surface-variant'"
							:variant="participant.isSpeaking ? 'flat' : 'tonal'"
							class="user-name-chip"
						>
							<template #prepend>
								<v-icon v-if="participant.isScreenSharing" size="x-small"> mdi-monitor-share </v-icon>
								<v-icon v-else-if="!participant.audioEnabled" size="x-small" color="error"> mdi-microphone-off </v-icon>
							</template>
							{{ participant.name }}
							<span v-if="participant.isScreenSharing" class="ml-1 text-caption"> - 屏幕共享 </span>
						</v-chip>
					</div>

					<!-- 连接质量指示器 -->
					<div v-if="showConnectionQuality" class="connection-quality">
						<v-tooltip location="top">
							<template #activator="{ props: tooltipProps }">
								<v-icon v-bind="tooltipProps" size="small" :color="getConnectionQuality(participant).color">
									{{ getConnectionQuality(participant).icon }}
								</v-icon>
							</template>
							<span>{{ getConnectionQuality(participant).text }}</span>
						</v-tooltip>
					</div>
				</div>

				<!-- 操作按钮(悬停时显示) -->
				<div class="tile-actions">
					<v-btn icon size="x-small" variant="flat" color="surface" @click="handlePin(participant.id)">
						<v-icon size="small">
							{{ pinnedParticipantId === participant.id ? 'mdi-pin-off' : 'mdi-pin' }}
						</v-icon>
					</v-btn>
				</div>
			</div>

			<!-- 更多参与者指示器 -->
			<div v-if="hiddenParticipantCount > 0" class="video-tile more-participants" :class="tileClass">
				<v-avatar size="64" color="primary" variant="tonal">
					<v-icon size="48">mdi-account-multiple</v-icon>
				</v-avatar>
				<div class="text-h6 mt-4" style="color: rgb(var(--v-theme-on-surface))">+{{ hiddenParticipantCount }} 更多</div>
			</div>
		</div>

		<!-- 屏幕共享覆盖层 -->
		<div v-if="screenShare?.active" class="screen-share-overlay">
			<div class="screen-share-container">
				<video ref="screenShareVideo" autoplay playsinline class="screen-share-video"></video>

				<div class="screen-share-info">
					<v-chip color="success" variant="flat" size="small">
						<template #prepend>
							<v-icon size="small">mdi-monitor-share</v-icon>
						</template>
						{{ screenShare.presenter }} 正在共享屏幕
					</v-chip>
				</div>
			</div>

			<!-- 参与者缩略图栏 -->
			<div class="participants-thumbnails">
				<div v-for="participant in allParticipantsWithLocal" :key="participant.id" class="thumbnail-tile" :class="{ 'is-local': participant.isLocal }">
					<video :ref="el => setThumbnailRef(el, participant.id)" autoplay playsinline :muted="participant.isLocal" class="thumbnail-video"></video>

					<div v-if="!participant.videoEnabled" class="thumbnail-placeholder">
						<v-avatar size="32" color="primary">
							<span class="text-caption">{{ participant.name }}</span>
						</v-avatar>
					</div>

					<div class="thumbnail-name">
						<span class="text-caption">{{ participant.name }}</span>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { useDebounceFn } from '@vueuse/core'
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
const streamCache = ref(new Map())
const props = defineProps({
	participants: {
		type: Array,
		default: () => [],
	},
	screenShare: {
		type: Object,
		default: () => ({ active: false, stream: null, presenter: null }),
	},
	layout: {
		type: String,
		default: 'grid',
		validator: value => ['grid', 'spotlight', 'sidebar'].includes(value),
	},
	localStream: {
		type: MediaStream,
		default: null,
	},
	localAudioEnabled: {
		type: Boolean,
		default: true,
	},
	localVideoEnabled: {
		type: Boolean,
		default: true,
	},
	showConnectionQuality: {
		type: Boolean,
		default: true,
	},
})

const emit = defineEmits(['pin-participant', 'unpin-participant'])

// ==================== 响应式状态 ====================
const localVideoElement = ref(null)
const screenShareVideo = ref(null)
const videoRefs = ref(new Map())
const thumbnailRefs = ref(new Map())
const pinnedParticipantId = ref(null)

// ==================== 计算属性 ====================

// 格式化参与者数据
const formattedParticipants = computed(() => {
	return props.participants
		.filter(p => !p.isLocal) // 过滤掉本地参与者
		.map(p => {
			// 合并音视频流
			let combinedStream = null

			if (p.streams) {
				combinedStream = new MediaStream()

				// 添加音频轨道
				if (p.streams.audio && p.streams.audio.getTracks) {
					const audioTracks = p.streams.audio.getAudioTracks()
					audioTracks.forEach(track => {
						if (track.readyState === 'live') {
							combinedStream.addTrack(track)
						}
					})
				}

				// 添加视频轨道
				if (p.streams.video && p.streams.video.getTracks) {
					const videoTracks = p.streams.video.getVideoTracks()
					videoTracks.forEach(track => {
						if (track.readyState === 'live') {
							combinedStream.addTrack(track)
						}
					})
				}
			}

			// 判断音视频状态
			let audioEnabled = false
			let videoEnabled = false

			if (combinedStream) {
				const audioTracks = combinedStream.getAudioTracks()
				const videoTracks = combinedStream.getVideoTracks()

				audioEnabled = audioTracks.length > 0 && audioTracks.some(t => t.readyState === 'live' && t.enabled)
				videoEnabled = videoTracks.length > 0 && videoTracks.some(t => t.readyState === 'live' && t.enabled)
			}

			const isScreenSharing = p.producers?.video?.appData?.source === 'screen'

			return {
				id: p.peerId,
				peerId: p.peerId,
				name: p.username,
				stream: combinedStream,
				audioEnabled,
				videoEnabled,
				isScreenSharing,
				isSpeaking: false,
				isLocal: false, // 确保标记为远程参与者
				connectionQuality: 'good',
			}
		})
		.filter(p => p.stream && p.stream.getTracks().length > 0) // 过滤无效流
})

// 所有参与者(包含本地)
const allParticipantsWithLocal = computed(() => {
	// 确保本地参与者数据正确
	const local = {
		id: 'local',
		name: '我',
		stream: props.localStream,
		videoEnabled: props.localVideoEnabled,
		audioEnabled: props.localAudioEnabled,
		isLocal: true,
	}

	// 只包含远程参与者（已通过 formattedParticipants 过滤）
	return [local, ...formattedParticipants.value].filter(p => p.stream)
})
const hasValidVideoTrack = participant => {
	if (!participant.stream) return false
	const videoTracks = participant.stream.getVideoTracks()
	return videoTracks.length > 0 && videoTracks.some(t => t.readyState === 'live' && t.enabled)
}

// 布局类名
const gridLayoutClass = computed(() => {
	return `layout-${props.layout}`
})

// 视频块样式类
const tileClass = computed(() => {
	const total = formattedParticipants.value.length + 1 // +1 包含本地

	if (props.layout === 'spotlight') {
		return { 'tile-spotlight': true }
	}

	if (props.layout === 'sidebar') {
		return { 'tile-sidebar': true }
	}

	// Grid layout
	if (total <= 1) return { 'tile-single': true }
	if (total <= 2) return { 'tile-two': true }
	if (total <= 4) return { 'tile-four': true }
	if (total <= 6) return { 'tile-six': true }
	if (total <= 9) return { 'tile-nine': true }
	return { 'tile-many': true }
})

// 头像大小
const avatarSize = computed(() => {
	const total = formattedParticipants.value.length + 1
	if (total <= 2) return 120
	if (total <= 4) return 96
	if (total <= 9) return 80
	return 64
})

// 头像文字样式
const avatarTextClass = computed(() => {
	const total = formattedParticipants.value.length + 1
	if (total <= 2) return 'text-h2'
	if (total <= 4) return 'text-h3'
	if (total <= 9) return 'text-h4'
	return 'text-h5'
})

// 最大显示数量
const maxVisible = computed(() => {
	if (props.layout === 'spotlight') return 1
	if (props.layout === 'sidebar') return 4
	return 16
})

// 可见参与者
const visibleParticipants = computed(() => {
	if (pinnedParticipantId.value) {
		const pinned = formattedParticipants.value.find(p => p.id === pinnedParticipantId.value)
		if (pinned) {
			return [pinned]
		}
	}
	return formattedParticipants.value.slice(0, maxVisible.value)
})

// 隐藏的参与者数量
const hiddenParticipantCount = computed(() => {
	return Math.max(0, formattedParticipants.value.length - maxVisible.value)
})

// 本地连接质量
const localConnectionQuality = computed(() => {
	return {
		icon: 'mdi-wifi-strength-4',
		color: 'success',
		text: '连接优秀',
	}
})

// ==================== 方法 ====================

// 获取连接质量
function getConnectionQuality(participant) {
	const quality = participant.connectionQuality || 'good'

	const qualityMap = {
		excellent: {
			icon: 'mdi-wifi-strength-4',
			color: 'success',
			text: '连接优秀',
		},
		good: {
			icon: 'mdi-wifi-strength-3',
			color: 'success',
			text: '连接良好',
		},
		fair: {
			icon: 'mdi-wifi-strength-2',
			color: 'warning',
			text: '连接一般',
		},
		poor: {
			icon: 'mdi-wifi-strength-1',
			color: 'error',
			text: '连接较差',
		},
	}

	return qualityMap[quality] || qualityMap.good
}

// 设置视频引用
function setVideoRef(el, peerId) {
	if (el) {
		videoRefs.value.set(peerId, el)

		const participant = formattedParticipants.value.find(p => p.peerId === peerId)
		if (participant?.stream) {
			const newStreamId = participant.stream.id
			const cachedStreamId = streamCache.value.get(peerId)

			// ========== 只在流 ID 变化时才更新 srcObject ==========
			if (cachedStreamId !== newStreamId) {
				console.log(`Setting stream for ${peerId}:`, {
					oldStreamId: cachedStreamId,
					newStreamId: newStreamId,
				})

				// 先暂停旧视频
				if (el.srcObject && el.srcObject !== participant.stream) {
					el.pause()
					el.srcObject = null
				}

				// 设置新流
				el.srcObject = participant.stream
				streamCache.value.set(peerId, newStreamId)

				// 延迟播放，确保流已准备好
				nextTick(() => {
					el.play().catch(err => {
						// 忽略 AbortError，其他错误才报警
						if (err.name !== 'AbortError') {
							console.error(`Failed to play video for ${peerId}:`, err)
						}
					})
				})
			} else {
				console.log(`Stream ${newStreamId} already set for ${peerId}, skipping`)
			}
		} else {
			console.warn(`No stream found for peer ${peerId}`)
		}
	} else {
		videoRefs.value.delete(peerId)
		streamCache.value.delete(peerId)
	}
}

// 设置缩略图引用
function setThumbnailRef(el, participantId) {
	if (el) {
		thumbnailRefs.value.set(participantId, el)

		if (participantId === 'local' && props.localStream) {
			el.srcObject = props.localStream
		} else {
			const participant = formattedParticipants.value.find(p => p.id === participantId)
			if (participant?.stream) {
				el.srcObject = participant.stream
			}
		}
	}
}

// 固定/取消固定参与者
function handlePin(participantId) {
	if (pinnedParticipantId.value === participantId) {
		pinnedParticipantId.value = null
		emit('unpin-participant', participantId)
	} else {
		pinnedParticipantId.value = participantId
		emit('pin-participant', participantId)
	}
}

// ==================== 监听器 ====================

// 监听本地流变化
watch(
	() => props.localStream,
	async newStream => {
		await nextTick()
		if (localVideoElement.value && newStream) {
			console.log('Setting local stream', newStream.id)
			localVideoElement.value.srcObject = newStream
		}
	},
	{ immediate: true },
)

// 监听屏幕共享流变化
watch(
	() => props.screenShare?.stream,
	async newStream => {
		await nextTick()
		if (screenShareVideo.value && newStream) {
			console.log('Setting screen share stream', newStream.id)
			screenShareVideo.value.srcObject = newStream
		}
	},
	{ immediate: true },
)

// 使用防抖处理流更新
const handleParticipantsUpdate = useDebounceFn(newParticipants => {
	newParticipants.forEach(participant => {
		const videoEl = videoRefs.value.get(participant.peerId)

		if (videoEl && participant.stream) {
			const currentStreamId = videoEl.srcObject?.id
			const newStreamId = participant.stream.id

			// 只有当流 ID 改变时才更新
			if (currentStreamId !== newStreamId) {
				console.log(`Updating stream for ${participant.name}:`, {
					oldStreamId: currentStreamId,
					newStreamId: newStreamId,
				})

				videoEl.pause()
				videoEl.srcObject = participant.stream
				streamCache.value.set(participant.peerId, newStreamId)

				videoEl.play().catch(err => {
					if (err.name !== 'AbortError') {
						console.error(`Play error for ${participant.name}:`, err)
					}
				})
			}
		}
	})
}, 100) // 100ms 防抖

// 监听参与者流变化
watch(() => formattedParticipants.value, handleParticipantsUpdate, { deep: true })

// ==================== 生命周期 ====================

onMounted(async () => {
	await nextTick()

	// 设置本地视频
	if (localVideoElement.value && props.localStream) {
		localVideoElement.value.srcObject = props.localStream
	}

	// 设置屏幕共享视频
	if (screenShareVideo.value && props.screenShare?.stream) {
		screenShareVideo.value.srcObject = props.screenShare.stream
	}
})

onUnmounted(() => {
	// 清理视频引用
	videoRefs.value.clear()
	thumbnailRefs.value.clear()
})
</script>

<style scoped>
/* ==================== 容器布局 ==================== */
.video-grid-container {
	position: relative;
	width: 100%;
	height: 100%;
	background: rgb(var(--v-theme-background));
}

.video-grid {
	display: grid;
	gap: 8px;
	padding: 4px;
	width: 100%;
	height: 100%;
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ==================== 布局模式 ==================== */
.layout-grid {
	grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
	grid-auto-rows: minmax(200px, 1fr);
}

.layout-spotlight {
	grid-template-columns: 1fr;
	grid-template-rows: 1fr;
}

.layout-sidebar {
	grid-template-columns: 3fr 1fr;
}

/* ==================== 视频块 ==================== */
.video-tile {
	position: relative;
	background: rgb(var(--v-theme-surface));
	border-radius: 12px;
	overflow: hidden;
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	border: 2px solid transparent;
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.video-tile.is-speaking {
	border-color: rgb(var(--v-theme-success));
	box-shadow: 0 0 20px rgba(var(--v-theme-success), 0.3);
}
.video-tile.video-disabled {
	background: rgba(var(--v-theme-surface-variant), 0.5);
}
.video-tile.is-screen-sharing {
	border-color: rgb(var(--v-theme-success));
	box-shadow: 0 0 20px rgba(var(--v-theme-success), 0.5);
}

.video-tile.is-local {
	border-color: rgba(var(--v-theme-primary), 0.5);
}

.video-tile:hover {
	box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

/* ==================== 视频元素 ==================== */
.video-element {
	width: 100%;
	height: 100%;
	object-fit: cover;
	background: rgb(var(--v-theme-surface));
}

.video-placeholder {
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, rgb(var(--v-theme-primary)) 0%, rgb(var(--v-theme-secondary)) 100%);
}

/* ==================== 覆盖层 ==================== */
.video-overlay {
	position: absolute;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 12px;
	background: linear-gradient(to top, rgba(var(--v-theme-surface), 0.9), transparent);
	display: flex;
	justify-content: space-between;
	align-items: flex-end;
	transition: opacity 0.3s;
}

.user-info {
	flex: 1;
	min-width: 0;
}

.user-name-chip {
	font-weight: 500;
	backdrop-filter: blur(8px);
}

.connection-quality {
	display: flex;
	align-items: center;
	margin-left: 8px;
}

/* ==================== 操作按钮 ==================== */
.tile-actions {
	position: absolute;
	top: 8px;
	right: 8px;
	opacity: 0;
	transition: opacity 0.2s;
	z-index: 10;
}

.video-tile:hover .tile-actions {
	opacity: 1;
}

/* ==================== 视频块尺寸 ==================== */
.tile-single {
	grid-column: 1 / -1;
	grid-row: 1 / -1;
}

.tile-two {
	aspect-ratio: 16 / 9;
}

.tile-four {
	aspect-ratio: 4 / 3;
}

.tile-six,
.tile-nine {
	aspect-ratio: 16 / 9;
}

.tile-many {
	aspect-ratio: 4 / 3;
	max-width: 320px;
}

.tile-spotlight {
	grid-column: 1 / -1;
	grid-row: 1 / -1;
	aspect-ratio: 16 / 9;
}

.tile-sidebar {
	aspect-ratio: 16 / 9;
}

/* ==================== 更多参与者指示器 ==================== */
.more-participants {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, rgba(var(--v-theme-primary), 0.2) 0%, rgba(var(--v-theme-secondary), 0.2) 100%);
	cursor: pointer;
}

.more-participants:hover {
	background: linear-gradient(135deg, rgba(var(--v-theme-primary), 0.3) 0%, rgba(var(--v-theme-secondary), 0.3) 100%);
}

/* ==================== 屏幕共享 ==================== */
.screen-share-overlay {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(var(--v-theme-surface), 0.98);
	z-index: 100;
	display: flex;
	flex-direction: column;
	backdrop-filter: blur(10px);
}

.screen-share-container {
	flex: 1;
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 16px;
}

.screen-share-video {
	max-width: 100%;
	max-height: 100%;
	border-radius: 8px;
	box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.screen-share-info {
	position: absolute;
	top: 24px;
	left: 24px;
	z-index: 10;
}

/* ==================== 参与者缩略图 ==================== */
.participants-thumbnails {
	display: flex;
	gap: 8px;
	padding: 12px 16px;
	overflow-x: auto;
	background: rgba(var(--v-theme-surface-variant), 0.8);
	border-top: 1px solid rgb(var(--v-theme-border));
	scrollbar-width: thin;
	scrollbar-color: rgb(var(--v-theme-primary)) transparent;
}

.participants-thumbnails::-webkit-scrollbar {
	height: 6px;
}

.participants-thumbnails::-webkit-scrollbar-track {
	background: transparent;
}

.participants-thumbnails::-webkit-scrollbar-thumb {
	background: rgb(var(--v-theme-primary));
	border-radius: 3px;
}

.thumbnail-tile {
	position: relative;
	flex-shrink: 0;
	width: 120px;
	height: 90px;
	border-radius: 8px;
	overflow: hidden;
	background: rgb(var(--v-theme-surface));
	border: 2px solid transparent;
	transition: all 0.2s;
	cursor: pointer;
}

.thumbnail-tile.is-local {
	border-color: rgba(var(--v-theme-primary), 0.5);
}

.thumbnail-tile:hover {
	border-color: rgb(var(--v-theme-primary));
	transform: scale(1.05);
}

.thumbnail-video {
	width: 100%;
	height: 100%;
	object-fit: cover;
}

.thumbnail-placeholder {
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, rgb(var(--v-theme-primary)) 0%, rgb(var(--v-theme-secondary)) 100%);
}

.thumbnail-name {
	position: absolute;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 4px 8px;
	background: linear-gradient(to top, rgba(var(--v-theme-surface), 0.95), transparent);
	text-align: center;
	color: rgb(var(--v-theme-on-surface));
	font-weight: 500;
	font-size: 11px;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

/* ==================== 响应式调整 ==================== */
@media (max-width: 960px) {
	.video-grid {
		gap: 4px;
		padding: 4px;
	}

	.layout-grid {
		grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
	}

	.thumbnail-tile {
		width: 100px;
		height: 75px;
	}
}

@media (max-width: 600px) {
	.video-tile {
		border-radius: 8px;
	}

	.video-overlay {
		padding: 8px;
	}

	.user-name-chip {
		font-size: 11px;
	}

	.thumbnail-tile {
		width: 80px;
		height: 60px;
	}

	.participants-thumbnails {
		padding: 8px;
	}
}
</style>
