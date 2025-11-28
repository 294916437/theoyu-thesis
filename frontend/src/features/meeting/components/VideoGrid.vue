<template>
	<div class="video-grid" :class="`layout-${layout}`">
		<!-- 本地视频 -->
		<VideoTile
			:stream="localStream"
			:user-name="'我'"
			:is-local="true"
			:audio-enabled="localAudioEnabled"
			:video-enabled="localVideoEnabled"
			:class="tileClass"
		/>

		<!-- 远程视频 -->
		<VideoTile
			v-for="participant in visibleParticipants"
			:key="participant.id"
			:stream="participant.stream"
			:user-name="participant.name"
			:is-local="false"
			:audio-enabled="participant.audioEnabled"
			:video-enabled="participant.videoEnabled"
			:is-speaking="participant.isSpeaking"
			:class="tileClass"
		/>

		<!-- 更多参与者指示器 -->
		<div v-if="hiddenParticipantCount > 0" class="more-participants" :class="tileClass">
			<v-icon size="48" color="white">mdi-account-multiple</v-icon>
			<div class="text-h6 text-white mt-2">+{{ hiddenParticipantCount }} 更多</div>
		</div>
	</div>
</template>

<script setup>
import { computed } from 'vue'
import VideoTile from './VideoTile.vue'

const props = defineProps({
	participants: {
		type: Array,
		default: () => [],
	},
	screenShare: {
		type: Object,
		default: () => ({ active: false }),
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
})

// 计算每个视频块的样式类
const tileClass = computed(() => {
	const total = props.participants.length + 1

	if (props.layout === 'spotlight') {
		return 'tile-spotlight'
	}

	if (props.layout === 'sidebar') {
		return 'tile-sidebar'
	}

	// Grid layout
	if (total <= 1) return 'tile-single'
	if (total <= 2) return 'tile-two'
	if (total <= 4) return 'tile-four'
	if (total <= 6) return 'tile-six'
	if (total <= 9) return 'tile-nine'
	return 'tile-many'
})

// 最大显示数量
const maxVisible = computed(() => {
	if (props.layout === 'spotlight') return 1
	if (props.layout === 'sidebar') return 4
	return 16
})

// 可见参与者
const visibleParticipants = computed(() => {
	return props.participants.slice(0, maxVisible.value)
})

// 隐藏的参与者数量
const hiddenParticipantCount = computed(() => {
	return Math.max(0, props.participants.length - maxVisible.value)
})
</script>

<style scoped>
.video-grid {
	display: grid;
	gap: 8px;
	padding: 8px;
	width: 100%;
	height: 100%;
}

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
	max-width: 300px;
}

.more-participants {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	border-radius: 8px;
}
</style>
