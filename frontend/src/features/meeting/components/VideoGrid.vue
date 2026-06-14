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
				<div ref="localMediaWrapper" class="local-media-wrapper" :class="{ 'is-mirrored': localPreviewMirrored }">
					<canvas v-show="!props.effectCanvas" ref="localCanvasElement" class="video-element video-canvas"></canvas>
					<!-- effectCanvas 由 useMedia 动态创建：原始预览 WebGL canvas 或背景特效输出 canvas -->
				</div>

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
					'audio-only': !participant.videoEnabled && participant.audioEnabled,
					'is-screen-sharing': participant.isScreenSharing,
					...tileClass,
				}"
			>
				<!-- 视频元素：当视频启用时显示 -->
				<canvas v-show="participant.videoEnabled" :ref="el => setVideoRef(el, participant.peerId)" class="video-element video-canvas" />
				<!-- 占位符：当视频关闭但音频开启时显示 -->
				<div v-if="!participant.videoEnabled" class="video-placeholder">
					<v-avatar :size="avatarSize" color="secondary">
						<span :class="avatarTextClass">{{ getInitials(participant.name) }}</span>
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
								<!-- 屏幕共享优先显示 -->
								<v-icon v-if="participant.isScreenSharing" size="x-small"> mdi-monitor-share </v-icon>
								<!-- 音频关闭图标 -->
								<v-icon v-else-if="!participant.audioEnabled" size="x-small" color="error"> mdi-microphone-off </v-icon>
								<!-- 视频关闭但音频开启时显示麦克风图标 -->
								<v-icon v-else-if="!participant.videoEnabled && participant.audioEnabled" size="x-small" color="success"> mdi-microphone </v-icon>
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
					<v-btn v-if="isHost" icon size="x-small" variant="flat" color="warning" class="ml-1" @click="handleSetSpotlight(participant.peerId)">
						<v-icon size="small">mdi-spotlight</v-icon>
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
		<div v-if="screenShare?.active" class="spotlight-overlay screen-share-spotlight">
			<div class="spotlight-main">
				<canvas ref="screenShareCanvas" class="spotlight-main-video screen-share-video"></canvas>

				<div class="screen-share-info">
					<v-chip color="success" variant="flat" size="small">
						<template #prepend>
							<v-icon size="small">mdi-monitor-share</v-icon>
						</template>
						{{ screenShare.presenter?.name || screenShare.presenter }} 正在共享屏幕
					</v-chip>
				</div>
			</div>

			<div v-if="allParticipantsWithLocal.length" class="spotlight-filmstrip">
				<div class="spotlight-filmstrip-header">
					<v-icon icon="mdi-account-multiple" size="12" class="mr-1"></v-icon>
					{{ allParticipantsWithLocal.length }}
				</div>
				<div v-for="participant in allParticipantsWithLocal" :key="participant.id" class="filmstrip-tile" :class="{ 'is-local': participant.isLocal }">
					<canvas
						v-if="participant.videoEnabled"
						:ref="el => setThumbnailRef(el, participant.id)"
						class="filmstrip-video"
						:class="{ 'is-mirrored': participant.isLocal && localPreviewMirrored }"
					></canvas>

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

		<!-- 聚光灯覆盖层：Zoom 风格 —— 中央主视频 + 右侧纵向缩略图条 -->
		<div v-if="spotlightParticipant" class="spotlight-overlay">
			<!-- 主视频区 -->
			<div class="spotlight-main">
				<video
					v-show="spotlightParticipant.videoEnabled"
					ref="spotlightVideoRef"
					autoplay
					playsinline
					:muted="spotlightParticipant.isLocal"
					class="spotlight-main-video"
					:class="{ 'is-mirrored': spotlightParticipant.isLocal && localPreviewMirrored }"
				></video>
				<div v-if="!spotlightParticipant.videoEnabled" class="spotlight-main-placeholder">
					<v-avatar :size="120" color="primary">
						<span class="text-h3">{{ getInitials(spotlightParticipant.name) }}</span>
					</v-avatar>
				</div>

				<!-- 顶部渐变：聚光灯标识 + 主持人关闭按钮 -->
				<div class="spotlight-main-header">
					<v-chip size="small" color="warning" variant="flat" prepend-icon="mdi-spotlight" class="spotlight-badge"> 聚光灯模式 </v-chip>
					<v-btn
						v-if="isHost"
						icon="mdi-close-circle"
						size="small"
						variant="text"
						color="white"
						class="ml-auto"
						@click="emit('set-spotlight', { targetPeerId: null, active: false })"
					></v-btn>
				</div>

				<!-- 底部渐变：姓名 + 静音状态 -->
				<div class="spotlight-main-footer">
					<v-icon v-if="!spotlightParticipant.audioEnabled" icon="mdi-microphone-off" color="error" size="16" class="mr-1"></v-icon>
					<span class="spotlight-main-name">{{ spotlightParticipant.name }}</span>
				</div>
			</div>

			<!-- 右侧纵向缩略图条 -->
			<div v-if="otherParticipantsForSpotlight.length" class="spotlight-filmstrip">
				<div class="spotlight-filmstrip-header">
					<v-icon icon="mdi-account-multiple" size="12" class="mr-1"></v-icon>
					{{ otherParticipantsForSpotlight.length }}
				</div>
				<div v-for="p in otherParticipantsForSpotlight" :key="p.isLocal ? 'local' : p.peerId" class="filmstrip-tile">
					<video
						v-if="p.videoEnabled"
						:ref="el => setSpotlightThumbRef(el, p.isLocal ? 'local' : p.peerId)"
						autoplay
						playsinline
						:muted="p.isLocal"
						class="filmstrip-video"
						:class="{ 'is-mirrored': p.isLocal && localPreviewMirrored }"
					></video>
					<div v-else class="filmstrip-placeholder">
						<v-avatar size="36" color="primary">
							<span class="text-caption font-weight-bold">{{ getInitials(p.name) }}</span>
						</v-avatar>
					</div>
					<div class="filmstrip-name">
						<v-icon v-if="!p.audioEnabled" icon="mdi-microphone-off" color="error" size="10"></v-icon>
						{{ p.name }}
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup>
import { useDebounceFn } from '@vueuse/core'
import { getInitials } from '@/utils/common'
import { createVideoCanvasRenderer } from '@/utils/videoCanvasRenderer'
import { ref, shallowRef, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
const streamCache = new Map()
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
	localVideoMirrored: {
		type: Boolean,
		default: false,
	},
	showConnectionQuality: {
		type: Boolean,
		default: true,
	},
	spotlightPeerId: {
		type: String,
		default: null,
	},
	isHost: {
		type: Boolean,
		default: false,
	},
	localPeerId: {
		type: String,
		default: null,
	},
	effectCanvas: {
		type: Object,
		default: null,
	},
})

const emit = defineEmits(['pin-participant', 'unpin-participant', 'set-spotlight'])

// ==================== 响应式状态 ====================
const localCanvasElement = ref(null)
const localMediaWrapper = ref(null)
const screenShareCanvas = ref(null)
const videoRefs = new Map()
const thumbnailRefs = new Map()
const spotlightThumbRefs = new Map()
const spotlightVideoRef = ref(null)
const localCanvasRenderer = shallowRef(null)
const videoRenderers = new Map()
// 使用 WeakMap 缓存流对象，避免重复创建
const streamObjectCache = new WeakMap()

// ==================== 计算属性 ====================

// 格式化参与者数据
const formattedParticipants = computed(() => {
	return props.participants
		.filter(p => !p.isLocal)
		.map(p => {
			// 优先从缓存获取已存在的流对象
			let combinedStream = streamObjectCache.get(p)

			// 检查流是否需要更新
			const needsUpdate = !combinedStream || !combinedStream.getTracks().length || combinedStream.getTracks().some(t => t.readyState !== 'live')

			if (needsUpdate && p.streams) {
				// 创建新流
				combinedStream = new MediaStream()

				// 添加音频轨道
				if (p.streams.audio) {
					p.streams.audio.getAudioTracks().forEach(track => {
						if (track.readyState === 'live') {
							combinedStream.addTrack(track)
						}
					})
				}

				// 添加视频轨道
				if (p.streams.video) {
					p.streams.video.getVideoTracks().forEach(track => {
						if (track.readyState === 'live') {
							combinedStream.addTrack(track)
						}
					})
				}

				// 缓存新流
				streamObjectCache.set(p, combinedStream)
			}

			// 状态判断逻辑保持不变
			let audioEnabled = false
			let videoEnabled = false

			if (combinedStream) {
				const audioTracks = combinedStream.getAudioTracks()
				const videoTracks = combinedStream.getVideoTracks()

				const hasAudioTrack = audioTracks.some(t => t.readyState === 'live')
				const hasVideoTrack = videoTracks.some(t => t.readyState === 'live')

				const audioProducerActive = !p.producers?.audio?.paused
				const videoProducerActive = !p.producers?.video?.paused

				const audioTrackEnabled = audioTracks.some(t => t.enabled)
				const videoTrackEnabled = videoTracks.some(t => t.enabled)

				audioEnabled = hasAudioTrack && audioProducerActive && audioTrackEnabled
				videoEnabled = hasVideoTrack && videoProducerActive && videoTrackEnabled
			}

			return {
				id: p.peerId,
				peerId: p.peerId,
				name: p.username,
				stream: combinedStream, // 使用缓存的流对象
				audioEnabled,
				videoEnabled,
				isScreenSharing: p.producers?.video?.appData?.source === 'screen',
				isSpeaking: false,
				isLocal: false,
				connectionQuality: 'good',
			}
		})
		.filter(p => {
			if (!p.stream || p.stream.getTracks().length === 0) {
				return false
			}
			return p.audioEnabled || p.videoEnabled
		})
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

// 布局类名
const gridLayoutClass = computed(() => {
	return `layout-${props.layout}`
})

const localPreviewMirrored = computed(() => props.localVideoMirrored && !props.screenShare?.isLocal)

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

function bindCanvasRenderer(rendererMap, key, canvas, stream, options = {}) {
	if (!canvas) {
		const renderer = rendererMap.get(key)
		renderer?.dispose()
		rendererMap.delete(key)
		return
	}

	let renderer = rendererMap.get(key)
	if (!renderer || renderer.canvas !== canvas) {
		renderer?.dispose()
		renderer = createVideoCanvasRenderer(canvas, options)
		rendererMap.set(key, renderer)
	}

	renderer.setMuted(options.muted ?? true)
	renderer.setStream(stream)
}

function disposeRenderers(rendererMap) {
	rendererMap.forEach(renderer => renderer.dispose())
	rendererMap.clear()
}

function setLocalCanvasRenderer(canvas) {
	if (!canvas) {
		localCanvasRenderer.value?.dispose()
		localCanvasRenderer.value = null
		return
	}

	if (!localCanvasRenderer.value || localCanvasRenderer.value.canvas !== canvas) {
		localCanvasRenderer.value?.dispose()
		localCanvasRenderer.value = createVideoCanvasRenderer(canvas, { muted: true })
	}

	if (!props.effectCanvas) {
		localCanvasRenderer.value.setStream(props.localStream)
	}
}

// 设置聚光灯缩略图引用
function setSpotlightThumbRef(el, participantId) {
	if (el) {
		spotlightThumbRefs.set(participantId, el)
		if (participantId === 'local' && props.localStream) {
			el.srcObject = props.localStream
		} else {
			const participant = formattedParticipants.value.find(p => p.peerId === participantId)
			if (participant?.stream) {
				el.srcObject = participant.stream
			}
		}
	} else {
		spotlightThumbRefs.delete(participantId)
	}
}

// 参与者操作 tile 中的聚光灯设置按钮点击
function handleSetSpotlight(participantId) {
	emit('set-spotlight', { targetPeerId: participantId, active: true })
}

// ==================== 聚光灯计算属性 ====================

// 当前聚光灯的参与者对象
const spotlightParticipant = computed(() => {
	if (!props.spotlightPeerId) return null
	// 检查聚光灯是否为本地用户（支持 'local' 字符串和实际 peerId 两种形式）
	const isLocalSpotlit = props.spotlightPeerId === 'local' || (props.localPeerId && props.spotlightPeerId === props.localPeerId)
	if (isLocalSpotlit) {
		return {
			id: 'local',
			name: '我',
			stream: props.localStream,
			videoEnabled: props.localVideoEnabled,
			audioEnabled: props.localAudioEnabled,
			isLocal: true,
		}
	}
	return formattedParticipants.value.find(p => p.peerId === props.spotlightPeerId) || null
})

// 其他参与者（排除当前聚光灯的那位）用于展示在缩略图条
const otherParticipantsForSpotlight = computed(() => {
	if (!props.spotlightPeerId) return []
	// 检查是否为本地用户处于聚光灯
	const isLocalSpotlit = props.spotlightPeerId === 'local' || (props.localPeerId && props.spotlightPeerId === props.localPeerId)
	return allParticipantsWithLocal.value.filter(p => {
		if (isLocalSpotlit && p.isLocal) return false
		const id = p.isLocal ? 'local' : p.peerId
		return id !== props.spotlightPeerId
	})
})

// 监听聚光灯参与者变化，更新主视频元素 srcObject
watch(
	[spotlightVideoRef, spotlightParticipant],
	async ([el, participant]) => {
		await nextTick()
		if (el && participant) {
			if (participant.isLocal && props.localStream) {
				el.srcObject = props.localStream
			} else if (participant.stream) {
				el.srcObject = participant.stream
			}
		}
	},
	{ immediate: true },
)

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
		videoRefs.set(peerId, el)

		const participant = formattedParticipants.value.find(p => p.peerId === peerId)
		if (participant?.stream) {
			const newStreamId = participant.stream.id
			const cachedStreamId = streamCache.get(peerId)

			// 只在流 ID 真正变化且流对象不同时才更新
			const currentStream = videoRenderers.get(peerId)?.sourceVideo.srcObject
			const isSameStream = currentStream && currentStream.id === newStreamId

			if (!isSameStream && cachedStreamId !== newStreamId) {
				console.log(`Updating stream for ${peerId}:`, {
					oldStreamId: cachedStreamId,
					newStreamId: newStreamId,
					reason: 'Stream object changed',
				})

				// 设置新流
				bindCanvasRenderer(videoRenderers, peerId, el, participant.stream, { muted: false })
				streamCache.set(peerId, newStreamId)
			} else {
				bindCanvasRenderer(videoRenderers, peerId, el, participant.stream, { muted: false })
				console.log(`Stream ${newStreamId} already set for ${peerId}, skipping (same object)`)
			}
		}
	} else {
		videoRefs.delete(peerId)
		streamCache.delete(peerId)
		bindCanvasRenderer(videoRenderers, peerId, null)
	}
}

// 设置缩略图引用
function setThumbnailRef(el, participantId) {
	if (el) {
		thumbnailRefs.set(participantId, el)

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

// ==================== 监听器 ====================

// 监听本地流变化
watch(
	() => props.localStream,
	async newStream => {
		await nextTick()
		if (localCanvasRenderer.value && !props.effectCanvas) {
			console.log('Setting local canvas stream', newStream?.id)
			localCanvasRenderer.value.setStream(newStream)
		}
	},
	{ immediate: true },
)

// 监听特效 canvas 变化
watch(
	[() => props.effectCanvas, localMediaWrapper],
	([canvas, wrapper]) => {
		if (!wrapper) return
		// 移除旧的特效 canvas（如有）
		const prev = wrapper.querySelector('canvas.local-effect-canvas')
		if (prev) prev.remove()
		if (canvas) {
			canvas.classList.add('local-effect-canvas')
			// 用内联样式直接保证尺寸，绕过 scoped 约束
			canvas.style.display = 'block'
			canvas.style.width = '100%'
			canvas.style.height = '100%'
			canvas.style.objectFit = 'cover'
			wrapper.appendChild(canvas)
		}
	},
	{ immediate: true },
)

// 监听屏幕共享流变化，更新主画面 WebGL canvas
watch(
	[() => props.screenShare?.stream, screenShareCanvas],
	async ([newStream, canvas]) => {
		await nextTick()
		if (canvas && newStream) {
			console.log('Setting screen share stream', newStream.id)
		}
		bindCanvasRenderer(screenShareRenderers, 'main', canvas, newStream, { muted: true })
	},
	{ immediate: true },
)

// 使用防抖处理流更新
const handleParticipantsUpdate = useDebounceFn(newParticipants => {
	newParticipants.forEach(participant => {
		const renderer = videoRenderers.get(participant.peerId)

		if (renderer && participant.stream) {
			const currentStreamId = renderer.sourceVideo.srcObject?.id
			const newStreamId = participant.stream.id
			const currentStreamObj = renderer.sourceVideo.srcObject

			// 只有当流对象真正不同时才更新
			if (currentStreamObj !== participant.stream && currentStreamId !== newStreamId) {
				console.log(`Updating stream for ${participant.name} (handleUpdate):`, {
					oldStreamId: currentStreamId,
					newStreamId: newStreamId,
				})

				renderer.setStream(participant.stream)
				streamCache.set(participant.peerId, newStreamId)
			}
		}
	})
}, 150) // 防抖延迟

const participantStreamSignature = computed(() =>
	formattedParticipants.value
		.map(participant => {
			const tracks = participant.stream?.getTracks() || []
			const trackSignature = tracks.map(track => `${track.id}:${track.readyState}:${track.enabled}`).join(',')
			return `${participant.peerId}:${participant.stream?.id || ''}:${trackSignature}`
		})
		.join('|'),
)

// 监听参与者流变化，避免 deep watch 遍历 MediaStream/MediaStreamTrack 对象
watch(participantStreamSignature, () => handleParticipantsUpdate(formattedParticipants.value))

// ==================== 生命周期 ====================

onMounted(async () => {
	await nextTick()

	// 设置本地 canvas（特效未激活时）
	if (localCanvasElement.value && props.localStream && !props.effectCanvas) {
		setLocalCanvasRenderer(localCanvasElement.value)
	}

	// 设置屏幕共享 canvas
	if (screenShareCanvas.value && props.screenShare?.stream) {
		bindCanvasRenderer(screenShareRenderers, 'main', screenShareCanvas.value, props.screenShare.stream, { muted: true })
	}
})

onUnmounted(() => {
	// 清理视频引用
	localCanvasRenderer.value?.dispose()
	localCanvasRenderer.value = null
	disposeRenderers(videoRenderers)
	disposeRenderers(screenShareRenderers)
	videoRefs.clear()
	thumbnailRefs.clear()
	spotlightThumbRefs.clear()
	streamCache.clear()
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
.video-tile.audio-only {
	background: linear-gradient(135deg, rgba(var(--v-theme-secondary), 0.1) 0%, rgba(var(--v-theme-primary), 0.1) 100%);
}
.video-tile.audio-only .video-placeholder {
	background: linear-gradient(135deg, rgb(var(--v-theme-secondary)) 0%, rgb(var(--v-theme-primary)) 100%);
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
	transition: opacity 0.3s ease-in-out;
}

.local-media-wrapper {
	width: 100%;
	height: 100%;
}

.local-media-wrapper.is-mirrored,
.thumbnail-video.is-mirrored,
.spotlight-main-video.is-mirrored,
.filmstrip-video.is-mirrored {
	transform: scaleX(-1);
}

/* :deep() 确保 scoped 样式能匹配动态插入的外部 canvas 元素 */
.local-media-wrapper :deep(canvas.local-effect-canvas) {
	display: block;
	width: 100%;
	height: 100%;
	object-fit: cover;
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
	animation: fadeIn 0.3s ease-in-out;
}
@keyframes fadeIn {
	from {
		opacity: 0;
		transform: scale(0.95);
	}
	to {
		opacity: 1;
		transform: scale(1);
	}
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

/* ==================== 聚光灯 (Zoom 风格) ==================== */
.spotlight-overlay {
	position: absolute;
	inset: 0;
	z-index: 5;
	display: flex;
	flex-direction: row;
	gap: 8px;
	padding: 4px;
	background: rgb(var(--v-theme-background));
}

.screen-share-spotlight {
	z-index: 100;
}

/* 主视频区（与 video-tile 相同风格） */
.spotlight-main {
	flex: 1;
	position: relative;
	min-width: 0;
	overflow: hidden;
	background: rgb(var(--v-theme-surface));
	border-radius: 12px;
	border: 2px solid transparent;
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.spotlight-main-video {
	width: 100%;
	height: 100%;
	object-fit: cover;
	background: rgb(var(--v-theme-surface));
}

.spotlight-main-placeholder {
	position: absolute;
	inset: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, rgb(var(--v-theme-primary)) 0%, rgb(var(--v-theme-secondary)) 100%);
}

.spotlight-main-header {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	display: flex;
	align-items: center;
	padding: 12px 16px;
	background: linear-gradient(to bottom, rgba(0, 0, 0, 0.55), transparent);
}

.spotlight-badge {
	font-weight: 600;
	letter-spacing: 0.3px;
}

.spotlight-main-footer {
	position: absolute;
	bottom: 0;
	left: 0;
	right: 0;
	display: flex;
	align-items: center;
	padding: 12px 16px;
	background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
}

.spotlight-main-name {
	font-size: 14px;
	font-weight: 500;
	color: #fff;
	text-shadow: 0 1px 4px rgba(0, 0, 0, 0.6);
}

/* 右侧纵向缩略图条 (Filmstrip) */
.spotlight-filmstrip {
	width: 180px;
	flex-shrink: 0;
	display: flex;
	flex-direction: column;
	gap: 6px;
	padding: 4px;
	overflow-y: auto;
	background: rgb(var(--v-theme-background));
	scrollbar-width: thin;
	scrollbar-color: rgba(var(--v-border-color), 0.3) transparent;
}

.spotlight-filmstrip::-webkit-scrollbar {
	width: 4px;
}

.spotlight-filmstrip::-webkit-scrollbar-track {
	background: transparent;
}

.spotlight-filmstrip::-webkit-scrollbar-thumb {
	background: rgba(var(--v-border-color), 0.3);
	border-radius: 2px;
}

.spotlight-filmstrip-header {
	display: flex;
	align-items: center;
	padding: 2px 4px 8px;
	color: rgb(var(--v-theme-on-surface-variant));
	font-size: 11px;
	flex-shrink: 0;
	border-bottom: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
}

/* 缩略图 tile 与 thumbnail-tile 风格一致 */
.filmstrip-tile {
	width: 100%;
	aspect-ratio: 16 / 9;
	position: relative;
	border-radius: 8px;
	overflow: hidden;
	background: rgb(var(--v-theme-surface));
	flex-shrink: 0;
	border: 1.5px solid transparent;
	transition:
		border-color 0.15s,
		box-shadow 0.15s;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.filmstrip-tile:hover {
	border-color: rgb(var(--v-theme-primary));
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.filmstrip-video {
	width: 100%;
	height: 100%;
	object-fit: cover;
}

.filmstrip-placeholder {
	width: 100%;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, rgb(var(--v-theme-primary)) 0%, rgb(var(--v-theme-secondary)) 100%);
}

.filmstrip-name {
	position: absolute;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 4px 6px;
	background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
	font-size: 10px;
	color: rgba(255, 255, 255, 0.95);
	font-weight: 500;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
	display: flex;
	align-items: center;
	gap: 2px;
}

/* 移动端：切换为底部横向缩略图条 */
@media (max-width: 600px) {
	.spotlight-overlay {
		flex-direction: column;
		gap: 4px;
	}

	.spotlight-filmstrip {
		width: 100%;
		height: 90px;
		flex-direction: row;
		padding: 4px 8px;
		overflow-x: auto;
		overflow-y: hidden;
	}

	.filmstrip-tile {
		width: 120px;
		height: 68px;
		aspect-ratio: unset;
	}

	.spotlight-filmstrip-header {
		display: none;
	}
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

	.layout-grid {
		grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
		grid-auto-rows: minmax(120px, 1fr);
		gap: 4px;
		padding: 2px;
	}

	.spotlight-overlay {
		flex-direction: column;
		gap: 4px;
	}

	.spotlight-filmstrip {
		width: 100%;
		height: 90px;
		flex-direction: row;
		overflow-x: auto;
		overflow-y: hidden;
	}

	.filmstrip-tile {
		width: 120px;
		height: 68px;
		flex-shrink: 0;
	}
}
</style>
