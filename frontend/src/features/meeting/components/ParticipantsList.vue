<template>
	<div class="participants-panel">
		<!-- 顶部操作栏 -->
		<div class="participants-header">
			<div class="d-flex align-center">
				<span class="text-subtitle-2 font-weight-medium">参与者 ({{ participants.length + 1 }})</span>
			</div>

			<div class="d-flex align-center gap-2">
				<!-- 邀请按钮 - 修复图标 -->
				<v-tooltip location="bottom">
					<template #activator="{ props }">
						<v-btn
							v-bind="props"
							icon="mdi-account-plus"
							size="small"
							variant="text"
							@click="showInviteDialog = true"
						></v-btn>
					</template>
					<span>邀请参与者</span>
				</v-tooltip>

				<!-- 更多选项 -->
				<v-menu>
					<template #activator="{ props }">
						<v-btn v-bind="props" icon="mdi-dots-vertical" size="small" variant="text"></v-btn>
					</template>

					<v-list density="compact" class="menu-list">
						<v-list-item @click="handleMuteAll">
							<template #prepend>
								<v-icon>mdi-microphone-off</v-icon>
							</template>
							<v-list-item-title>全体静音</v-list-item-title>
						</v-list-item>

						<v-list-item @click="handleUnmuteAll">
							<template #prepend>
								<v-icon>mdi-microphone</v-icon>
							</template>
							<v-list-item-title>解除全体静音</v-list-item-title>
						</v-list-item>

						<v-divider></v-divider>

						<v-list-item @click="handleLockMeeting">
							<template #prepend>
								<v-icon>{{ isMeetingLocked ? 'mdi-lock-open' : 'mdi-lock' }}</v-icon>
							</template>
							<v-list-item-title>
								{{ isMeetingLocked ? '解锁会议' : '锁定会议' }}
							</v-list-item-title>
						</v-list-item>
					</v-list>
				</v-menu>
			</div>
		</div>

		<v-divider></v-divider>

		<!-- 参与者列表 -->
		<div class="participants-list">
			<!-- 当前用户 -->
			<div class="participant-item current-user">
				<v-avatar size="36" color="primary">
					<v-icon>mdi-account</v-icon>
				</v-avatar>

				<div class="participant-info">
					<div class="participant-name">
						<span class="font-weight-medium">我</span>
						<v-chip size="x-small" color="primary" variant="flat" class="ml-2">主持人</v-chip>
					</div>
					<div class="participant-status">
						<v-icon size="small" :color="audioEnabled ? 'success' : 'error'">
							{{ audioEnabled ? 'mdi-microphone' : 'mdi-microphone-off' }}
						</v-icon>
						<v-icon size="small" :color="videoEnabled ? 'success' : 'error'" class="ml-1">
							{{ videoEnabled ? 'mdi-video' : 'mdi-video-off' }}
						</v-icon>
					</div>
				</div>

				<v-menu>
					<template #activator="{ props }">
						<v-btn v-bind="props" icon="mdi-dots-vertical" size="small" variant="text"></v-btn>
					</template>

					<v-list density="compact" class="menu-list">
						<v-list-item @click="toggleAudio">
							<template #prepend>
								<v-icon>{{ audioEnabled ? 'mdi-microphone-off' : 'mdi-microphone' }}</v-icon>
							</template>
							<v-list-item-title>
								{{ audioEnabled ? '静音' : '取消静音' }}
							</v-list-item-title>
						</v-list-item>

						<v-list-item @click="toggleVideo">
							<template #prepend>
								<v-icon>{{ videoEnabled ? 'mdi-video-off' : 'mdi-video' }}</v-icon>
							</template>
							<v-list-item-title>
								{{ videoEnabled ? '关闭摄像头' : '开启摄像头' }}
							</v-list-item-title>
						</v-list-item>
					</v-list>
				</v-menu>
			</div>

			<!-- 其他参与者 -->
			<div
				v-for="participant in participants"
				:key="participant.id"
				class="participant-item"
				:class="{ 'is-speaking': participant.isSpeaking }"
			>
				<v-avatar size="36" :color="participant.avatarColor || 'grey'">
					<span class="text-white">{{ getInitials(participant.name) }}</span>
				</v-avatar>

				<div class="participant-info">
					<div class="participant-name">
						<span class="font-weight-medium">{{ participant.name }}</span>
						<v-chip v-if="participant.isHost" size="x-small" color="primary" variant="flat" class="ml-2">
							主持人
						</v-chip>
						<v-icon v-if="participant.handRaised" size="small" color="warning" class="ml-2">
							mdi-hand-back-right
						</v-icon>
					</div>
					<div class="participant-status">
						<v-icon size="small" :color="participant.audioEnabled ? 'success' : 'error'">
							{{ participant.audioEnabled ? 'mdi-microphone' : 'mdi-microphone-off' }}
						</v-icon>
						<v-icon size="small" :color="participant.videoEnabled ? 'success' : 'error'" class="ml-1">
							{{ participant.videoEnabled ? 'mdi-video' : 'mdi-video-off' }}
						</v-icon>
						<span v-if="participant.connectionQuality" class="ml-2">
							<v-icon size="small" :color="getConnectionColor(participant.connectionQuality)">
								{{ getConnectionIcon(participant.connectionQuality) }}
							</v-icon>
						</span>
					</div>
				</div>

				<v-menu>
					<template #activator="{ props }">
						<v-btn v-bind="props" icon="mdi-dots-vertical" size="small" variant="text"></v-btn>
					</template>

					<v-list density="compact" class="menu-list">
						<v-list-item @click="emit('pin-participant', participant.id)">
							<template #prepend>
								<v-icon>mdi-pin</v-icon>
							</template>
							<v-list-item-title>固定视频</v-list-item-title>
						</v-list-item>

						<v-list-item @click="emit('spotlight-participant', participant.id)">
							<template #prepend>
								<v-icon>mdi-spotlight-beam</v-icon>
							</template>
							<v-list-item-title>聚焦参与者</v-list-item-title>
						</v-list-item>

						<v-divider></v-divider>

						<v-list-item @click="emit('mute-participant', participant.id)">
							<template #prepend>
								<v-icon>mdi-microphone-off</v-icon>
							</template>
							<v-list-item-title>静音</v-list-item-title>
						</v-list-item>

						<v-list-item @click="emit('remove-participant', participant.id)" class="text-error">
							<template #prepend>
								<v-icon color="error">mdi-account-remove</v-icon>
							</template>
							<v-list-item-title>移除参与者</v-list-item-title>
						</v-list-item>
					</v-list>
				</v-menu>
			</div>

			<!-- 空状态 -->
			<div v-if="participants.length === 0" class="empty-state">
				<v-icon size="64" color="grey-darken-1">mdi-account-group-outline</v-icon>
				<div class="text-body-2 text-grey mt-4">暂无其他参与者</div>
				<v-btn
					color="primary"
					variant="flat"
					prepend-icon="mdi-account-plus"
					class="mt-4"
					@click="showInviteDialog = true"
				>
					邀请参与者
				</v-btn>
			</div>
		</div>

		<!-- 邀请对话框 -->
		<v-dialog v-model="showInviteDialog" max-width="500">
			<v-card class="invite-dialog">
				<v-card-title class="d-flex align-center justify-space-between">
					<span class="text-h6">邀请参与者</span>
					<v-btn icon="mdi-close" variant="text" size="small" @click="showInviteDialog = false"></v-btn>
				</v-card-title>

				<v-divider></v-divider>

				<v-card-text class="pa-6">
					<div class="mb-4">
						<label class="text-subtitle-2 mb-2 d-block">会议链接</label>
						<v-text-field
							:model-value="meetingLink"
							readonly
							variant="outlined"
							density="comfortable"
							hide-details
						>
							<template #append-inner>
								<v-btn
									icon="mdi-content-copy"
									size="small"
									variant="text"
									@click="copyMeetingLink"
								></v-btn>
							</template>
						</v-text-field>
					</div>

					<div>
						<label class="text-subtitle-2 mb-2 d-block">会议 ID</label>
						<v-text-field
							:model-value="meetingId"
							readonly
							variant="outlined"
							density="comfortable"
							hide-details
						>
							<template #append-inner>
								<v-btn
									icon="mdi-content-copy"
									size="small"
									variant="text"
									@click="copyMeetingId"
								></v-btn>
							</template>
						</v-text-field>
					</div>

					<v-divider class="my-4"></v-divider>

					<div class="text-caption text-medium-emphasis">将会议链接或 ID 分享给他人即可邀请参与</div>
				</v-card-text>

				<v-divider></v-divider>

				<v-card-actions class="pa-4">
					<v-spacer></v-spacer>
					<v-btn variant="text" @click="showInviteDialog = false">关闭</v-btn>
					<v-btn color="primary" variant="flat" @click="shareInvite">分享邀请</v-btn>
				</v-card-actions>
			</v-card>
		</v-dialog>
	</div>
</template>

<script setup>
import { $notify } from '@/plugins/notification'
import { ref, computed } from 'vue'

const props = defineProps({
	participants: {
		type: Array,
		default: () => [],
	},
	currentUserId: {
		type: String,
		required: true,
	},
	meetingId: {
		type: String,
		required: true,
	},
})

const emit = defineEmits(['mute-participant', 'remove-participant', 'pin-participant', 'spotlight-participant'])

const showInviteDialog = ref(false)
const isMeetingLocked = ref(false)
const audioEnabled = ref(true)
const videoEnabled = ref(true)

const meetingLink = computed(() => {
	return `${window.location.origin}/meeting/${props.meetingId}`
})

const getInitials = name => {
	return name
		.split(' ')
		.map(word => word[0])
		.join('')
		.toUpperCase()
		.slice(0, 2)
}

const getConnectionColor = quality => {
	const colors = {
		excellent: 'success',
		good: 'success',
		fair: 'warning',
		poor: 'error',
	}
	return colors[quality] || 'grey'
}

const getConnectionIcon = quality => {
	const icons = {
		excellent: 'mdi-wifi-strength-4',
		good: 'mdi-wifi-strength-3',
		fair: 'mdi-wifi-strength-2',
		poor: 'mdi-wifi-strength-1',
	}
	return icons[quality] || 'mdi-wifi-strength-outline'
}

const copyMeetingLink = async () => {
	try {
		await navigator.clipboard.writeText(meetingLink.value)
		$notify.success('会议链接已复制')
	} catch (error) {
		$notify.error('复制失败')
	}
}

const copyMeetingId = async () => {
	try {
		await navigator.clipboard.writeText(props.meetingId)
		$notify.success('会议 ID 已复制')
	} catch (error) {
		$notify.error('复制失败')
	}
}

const shareInvite = () => {
	if (navigator.share) {
		navigator.share({
			title: '加入会议',
			text: `邀请您加入会议\n会议 ID: ${props.meetingId}`,
			url: meetingLink.value,
		})
	} else {
		copyMeetingLink()
	}
}

const handleMuteAll = () => {
	// 预留API: 全体静音
	console.log('Mute all participants')
	$notify.success('已全体静音')
}

const handleUnmuteAll = () => {
	// 预留API: 解除全体静音
	console.log('Unmute all participants')
	$notify.success('已解除全体静音')
}

const handleLockMeeting = () => {
	isMeetingLocked.value = !isMeetingLocked.value
	$notify.success(isMeetingLocked.value ? '会议已锁定' : '会议已解锁')
}

const toggleAudio = () => {
	audioEnabled.value = !audioEnabled.value
}

const toggleVideo = () => {
	videoEnabled.value = !videoEnabled.value
}
</script>

<style scoped>
.participants-panel {
	display: flex;
	flex-direction: column;
	height: 100%;
	background: transparent;
	overflow: hidden;
}

.participants-header {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 12px 16px;
	background: rgba(255, 255, 255, 0.02);
	border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.gap-2 {
	gap: 8px;
}

.participants-list {
	flex: 1;
	overflow-y: auto;
	overflow-x: hidden;
	padding: 8px;
	min-height: 0;
}

.participants-list::-webkit-scrollbar {
	width: 6px;
}

.participants-list::-webkit-scrollbar-track {
	background: rgba(255, 255, 255, 0.05);
	border-radius: 3px;
}

.participants-list::-webkit-scrollbar-thumb {
	background: rgba(255, 255, 255, 0.2);
	border-radius: 3px;
}

.participants-list::-webkit-scrollbar-thumb:hover {
	background: rgba(255, 255, 255, 0.3);
}

.participant-item {
	display: flex;
	align-items: center;
	padding: 12px;
	border-radius: 8px;
	margin-bottom: 4px;
	transition: all 0.2s;
	cursor: pointer;
}

.participant-item:hover {
	background: rgba(255, 255, 255, 0.05);
}

.participant-item.current-user {
	background: rgba(102, 126, 234, 0.1);
	border: 1px solid rgba(102, 126, 234, 0.3);
}

.participant-item.is-speaking {
	background: rgba(76, 175, 80, 0.1);
	border: 2px solid rgba(76, 175, 80, 0.5);
	animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
	0%,
	100% {
		border-color: rgba(76, 175, 80, 0.5);
	}
	50% {
		border-color: rgba(76, 175, 80, 0.8);
	}
}

.participant-info {
	flex: 1;
	margin-left: 12px;
	min-width: 0;
}

.participant-name {
	display: flex;
	align-items: center;
	font-size: 14px;
	color: rgba(255, 255, 255, 0.95);
	margin-bottom: 4px;
}

.participant-status {
	display: flex;
	align-items: center;
	font-size: 12px;
	color: rgba(255, 255, 255, 0.6);
}

.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	height: 100%;
	padding: 40px 20px;
	color: rgba(255, 255, 255, 0.5);
}

.menu-list {
	background-color: rgba(40, 40, 58, 0.98);
	border: 1px solid rgba(255, 255, 255, 0.12);
}

.menu-list .v-list-item:hover {
	background-color: rgba(255, 255, 255, 0.08);
}

.invite-dialog {
	background: linear-gradient(to bottom, rgba(40, 40, 58, 0.98) 0%, rgba(35, 35, 51, 0.98) 100%);
	backdrop-filter: blur(10px);
	color: rgba(255, 255, 255, 0.95);
}

.invite-dialog :deep(.v-field) {
	background-color: rgba(255, 255, 255, 0.05);
	border-color: rgba(255, 255, 255, 0.12);
}

.invite-dialog :deep(.v-field__input) {
	color: rgba(255, 255, 255, 0.95);
}
</style>
