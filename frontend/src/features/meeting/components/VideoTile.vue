<template>
	<div
		class="video-tile"
		:class="{
			'is-local': isLocal,
			'is-speaking': isSpeaking,
			'video-disabled': !videoEnabled,
		}"
	>
		<!-- 视频元素 -->
		<video ref="videoElement" autoplay playsinline :muted="isLocal" class="video-element"></video>

		<!-- 无视频时的占位符 -->
		<div v-if="!videoEnabled" class="video-placeholder">
			<v-avatar size="80" color="primary">
				<span class="text-h4 text-white">{{ userInitial }}</span>
			</v-avatar>
		</div>

		<!-- 用户信息覆盖层 -->
		<div class="video-overlay">
			<div class="user-info">
				<v-chip size="small" :color="isSpeaking ? 'success' : 'default'" class="user-name-chip">
					<v-icon v-if="!audioEnabled" size="small" left color="error"> mdi-microphone-off </v-icon>
					{{ userName }}
					<v-icon v-if="isLocal" size="small" right> mdi-account </v-icon>
				</v-chip>
			</div>

			<!-- 连接质量指示器 -->
			<div class="connection-quality">
				<v-icon size="small" :color="connectionColor">
					{{ connectionIcon }}
				</v-icon>
			</div>
		</div>

		<!-- 操作按钮(悬停时显示) -->
		<div v-if="!isLocal" class="tile-actions">
			<v-btn icon size="small" variant="text" color="white" @click="handlePin">
				<v-icon>{{ isPinned ? 'mdi-pin-off' : 'mdi-pin' }}</v-icon>
			</v-btn>
		</div>
	</div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useElementSize } from '@vueuse/core'

const props = defineProps({
	stream: {
		type: MediaStream,
		default: null,
	},
	userName: {
		type: String,
		required: true,
	},
	isLocal: {
		type: Boolean,
		default: false,
	},
	audioEnabled: {
		type: Boolean,
		default: true,
	},
	videoEnabled: {
		type: Boolean,
		default: true,
	},
	isSpeaking: {
		type: Boolean,
		default: false,
	},
	connectionQuality: {
		type: String,
		default: 'good',
		validator: value => ['excellent', 'good', 'poor', 'bad'].includes(value),
	},
})

const emit = defineEmits(['pin', 'unpin'])

const videoElement = ref(null)
const isPinned = ref(false)

// 用户名首字母
const userInitial = computed(() => {
	return props.userName.charAt(0).toUpperCase()
})

// 连接质量图标和颜色
const connectionIcon = computed(() => {
	const icons = {
		excellent: 'mdi-wifi-strength-4',
		good: 'mdi-wifi-strength-3',
		poor: 'mdi-wifi-strength-2',
		bad: 'mdi-wifi-strength-1',
	}
	return icons[props.connectionQuality]
})

const connectionColor = computed(() => {
	const colors = {
		excellent: 'success',
		good: 'success',
		poor: 'warning',
		bad: 'error',
	}
	return colors[props.connectionQuality]
})

// 设置视频流
watch(
	() => props.stream,
	newStream => {
		if (videoElement.value && newStream) {
			videoElement.value.srcObject = newStream
		}
	},
	{ immediate: true },
)

const handlePin = () => {
	isPinned.value = !isPinned.value
	emit(isPinned.value ? 'pin' : 'unpin')
}

onMounted(() => {
	if (videoElement.value && props.stream) {
		videoElement.value.srcObject = props.stream
	}
})
</script>

<style scoped>
.video-tile {
	position: relative;
	background-color: #1a1a1a;
	border-radius: 8px;
	overflow: hidden;
	transition: all 0.3s ease;
	border: 2px solid transparent;
}

.video-tile.is-speaking {
	border-color: #4caf50;
	box-shadow: 0 0 20px rgba(76, 175, 80, 0.5);
}

.video-tile.is-local {
	border-color: rgba(102, 126, 234, 0.5);
}

.video-element {
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
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.video-overlay {
	position: absolute;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 12px;
	background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
	display: flex;
	justify-content: space-between;
	align-items: flex-end;
}

.user-name-chip {
	font-weight: 500;
}

.connection-quality {
	display: flex;
	align-items: center;
}

.tile-actions {
	position: absolute;
	top: 8px;
	right: 8px;
	opacity: 0;
	transition: opacity 0.2s;
}

.video-tile:hover .tile-actions {
	opacity: 1;
}
</style>
