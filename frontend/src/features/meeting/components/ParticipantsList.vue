<script setup>
import { $notify } from '@/plugins/notification'
import { getInitials } from '@/utils/common'
import { ref, computed } from 'vue'

const props = defineProps({
	participants: {
		type: Array,
		default: () => [],
	},
	isHost: {
		type: Boolean,
		default: false,
	},
	currentUserId: {
		type: String,
		required: true,
	},
	meetingId: {
		type: String,
		required: true,
	},
	meetingNo: {
		type: String,
		required: true,
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

const emit = defineEmits([
	'mute-all',
	'unmute-all',
	'disable-all-video',
	'host-toggle-audio',
	'host-toggle-video',
	'remove-participant',
	'pin-participant',
	'spotlight-participant',
	'toggle-audio',
	'toggle-video',
])

// 按角色分组
const participantsByRole = computed(() => {
	const members = props.participants.filter(p => p.role === 1 && p.status === 1)
	const hosts = props.participants.filter(p => p.role === 2 && p.status === 1)
	console.log('Participants by role:', { hosts, members })
	return { hosts, members }
})

const showInviteDialog = ref(false)

/**
 * 检查是否可以控制目标参与者（主持人操作其他参与者）
 */
const canControlOtherParticipant = targetParticipant => {
	// 主持人可以控制普通成员（非自己）
	return props.isHost && targetParticipant.role === 1 && targetParticipant.userId !== props.currentUserId
}

/**
 * 检查是否为当前用户自己
 */
const isCurrentUser = targetParticipant => {
	return targetParticipant.userId === props.currentUserId
}

const meetingLink = computed(() => {
	return window.location.href
})

const onlineParticipants = computed(() => props.participants.filter(p => p.status === 1))

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

const copyMeetingNo = async () => {
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
/**
 * 获取参与者的媒体状态（统一判断逻辑）
 */
const getMediaState = (participant, kind) => {
	// 如果是本地用户
	if (isCurrentUser(participant)) {
		return {
			enabled: kind === 'audio' ? props.localAudioEnabled : props.localVideoEnabled,
		}
	}

	// 如果是远程用户
	const producer = participant.producers?.[kind]
	if (!producer) {
		return { enabled: false }
	}

	// 判断 producer 是否被暂停
	return {
		enabled: !producer.paused,
	}
}

// ==================== 主持人操作 ====================

const handleMuteAll = () => {
	if (!props.isHost) {
		$notify.warning('仅主持人可执行此操作')
		return
	}

	emit('mute-all')
	console.log('Mute all participants')
	$notify.success('已请求全体静音')
}

const handleUnmuteAll = () => {
	if (!props.isHost) {
		$notify.warning('仅主持人可执行此操作')
		return
	}

	emit('unmute-all')
	console.log('Unmute all participants')
	$notify.success('已解除全体静音')
}

const handleDisableAllVideo = () => {
	if (!props.isHost) {
		$notify.warning('仅主持人可执行此操作')
		return
	}

	emit('disable-all-video')
	console.log('Disable all video')
	$notify.success('已请求关闭全体摄像头')
}

const handleHostToggleAudio = async participant => {
	console.log('主持人控制音频', participant.peerId, participant.producers?.audio?.paused)

	emit('host-toggle-audio', participant.peerId, participant.producers?.audio?.paused)
}

const handleHostToggleVideo = async participant => {
	console.log('主持人控制视频', participant.peerId, participant.producers?.video?.paused)
	emit('host-toggle-video', participant.peerId, participant.producers?.video?.paused)
}

const handleRemoveParticipant = participant => {
	if (!canControlOtherParticipant(participant)) {
		$notify.warning('您没有权限移除该参与者')
		return
	}

	emit('remove-participant', participant.peerId)
}
</script>

<template>
	<div class="participants-panel">
		<!-- 顶部操作栏 -->
		<div class="participants-header">
			<div class="d-flex align-center">
				<v-icon size="small" color="primary" class="mr-2">mdi-account-group</v-icon>
				<span class="text-subtitle-2 font-weight-medium"> 参与者 ({{ onlineParticipants.length }}) </span>
			</div>

			<div class="d-flex align-center ga-2">
				<!-- 邀请按钮 -->
				<v-tooltip location="bottom">
					<template #activator="{ props }">
						<v-btn v-bind="props" icon="mdi-account-plus" size="small" variant="text" color="primary" @click="showInviteDialog = true"></v-btn>
					</template>
					<span>邀请参与者</span>
				</v-tooltip>

				<!-- 主持人批量操作 -->
				<v-menu v-if="isHost">
					<template #activator="{ props }">
						<v-btn v-bind="props" icon="mdi-dots-vertical" size="small" variant="text" color="primary"></v-btn>
					</template>

					<v-list density="compact" class="menu-list">
						<v-list-item @click="handleMuteAll">
							<template #prepend>
								<v-icon color="warning">mdi-microphone-off</v-icon>
							</template>
							<v-list-item-title>全体静音</v-list-item-title>
						</v-list-item>

						<v-list-item @click="handleUnmuteAll">
							<template #prepend>
								<v-icon color="success">mdi-microphone</v-icon>
							</template>
							<v-list-item-title>解除全体静音</v-list-item-title>
						</v-list-item>

						<v-divider class="my-1"></v-divider>

						<v-list-item @click="handleDisableAllVideo">
							<template #prepend>
								<v-icon color="warning">mdi-video-off</v-icon>
							</template>
							<v-list-item-title>关闭全体摄像头</v-list-item-title>
						</v-list-item>
					</v-list>
				</v-menu>
			</div>
		</div>

		<v-divider></v-divider>

		<!-- 参与者列表 -->
		<div class="participants-list">
			<!-- 主持人列表 -->
			<div v-if="participantsByRole.hosts.length > 0" class="participants-section">
				<div class="section-title">
					<v-icon size="small" color="primary">mdi-shield-star</v-icon>
					<span>主持人 ({{ participantsByRole.hosts.length }})</span>
				</div>

				<div
					v-for="participant in participantsByRole.hosts"
					:key="participant.userId"
					class="participant-item"
					:class="{
						'current-user': isCurrentUser(participant),
					}"
				>
					<!-- 头像 -->
					<v-avatar size="36" :image="participant.avatar" color="primary-lighten-1">
						<template #placeholder>
							<v-progress-circular indeterminate size="20" color="primary"></v-progress-circular>
						</template>
						<span v-if="!participant.avatar" class="text-white text-caption font-weight-bold">
							{{ getInitials(participant.username) }}
						</span>
					</v-avatar>

					<!-- 参与者信息 -->
					<div class="participant-info">
						<div class="participant-name">
							<span class="font-weight-medium">
								{{ participant.username }}
								<span v-if="isCurrentUser(participant)" class="text-caption text-primary"> （我） </span>
							</span>
							<v-chip size="x-small" color="primary" variant="flat" class="ml-2"> 主持人 </v-chip>
							<v-icon v-if="participant.handRaised" size="small" color="warning" class="ml-1"> mdi-hand-back-right </v-icon>
						</div>
						<div class="participant-status">
							<v-icon size="small" :color="getMediaState(participant, 'audio').enabled ? 'success' : 'error'">
								{{ getMediaState(participant, 'audio').enabled ? 'mdi-microphone' : 'mdi-microphone-off' }}
							</v-icon>
							<v-icon size="small" :color="getMediaState(participant, 'video').enabled ? 'success' : 'error'" class="ml-1">
								{{ getMediaState(participant, 'video').enabled ? 'mdi-video' : 'mdi-video-off' }}
							</v-icon>
							<v-icon v-if="participant.connectionQuality" size="small" :color="getConnectionColor(participant.connectionQuality)" class="ml-2">
								{{ getConnectionIcon(participant.connectionQuality) }}
							</v-icon>
						</div>
					</div>
				</div>
			</div>

			<!-- 普通参与者列表 -->
			<div v-if="participantsByRole.members.length > 0" class="participants-section">
				<div class="section-title">
					<v-icon size="small" color="secondary">mdi-account-group</v-icon>
					<span>参与者 ({{ participantsByRole.members.length }})</span>
				</div>

				<div
					v-for="participant in participantsByRole.members"
					:key="participant.userId"
					class="participant-item"
					:class="{
						'current-user': isCurrentUser(participant),
					}"
				>
					<!-- 头像 -->
					<v-avatar size="36" :image="participant.avatar" color="secondary-lighten-1">
						<template #placeholder>
							<v-progress-circular indeterminate size="20" color="secondary"></v-progress-circular>
						</template>
						<span v-if="!participant.avatar" class="text-white text-caption font-weight-bold">
							{{ getInitials(participant.username) }}
						</span>
					</v-avatar>

					<!-- 参与者信息 -->
					<div class="participant-info">
						<div class="participant-name">
							<span class="font-weight-medium">
								{{ participant.username }}
								<span v-if="isCurrentUser(participant)" class="text-caption text-primary"> （我） </span>
							</span>
							<v-icon v-if="participant.handRaised" size="small" color="warning" class="ml-2"> mdi-hand-back-right </v-icon>
						</div>
						<div class="participant-status">
							<v-icon size="small" :color="participant.audioEnabled ? 'success' : 'error'">
								{{ participant.audioEnabled ? 'mdi-microphone' : 'mdi-microphone-off' }}
							</v-icon>
							<v-icon size="small" :color="participant.videoEnabled ? 'success' : 'error'" class="ml-1">
								{{ participant.videoEnabled ? 'mdi-video' : 'mdi-video-off' }}
							</v-icon>
							<v-icon v-if="participant.connectionQuality" size="small" :color="getConnectionColor(participant.connectionQuality)" class="ml-2">
								{{ getConnectionIcon(participant.connectionQuality) }}
							</v-icon>
						</div>
					</div>

					<!-- 操作菜单（仅主持人显示） -->
					<v-menu v-if="props.isHost">
						<template #activator="{ props }">
							<v-btn v-bind="props" icon="mdi-dots-vertical" size="small" variant="text" color="primary"></v-btn>
						</template>

						<v-list density="compact" class="menu-list">
							<!-- 主持人对其他参与者的控制 -->
							<template v-if="canControlOtherParticipant(participant)">
								<v-list-item @click="handleHostToggleAudio(participant)">
									<template #prepend>
										<v-icon color="warning">mdi-microphone-off</v-icon>
									</template>
									<v-list-item-title>
										{{ participant.producers?.audio?.paused ? '取消静音' : '静音' }}
									</v-list-item-title>
								</v-list-item>

								<v-list-item @click="handleHostToggleVideo(participant)">
									<template #prepend>
										<v-icon color="warning">mdi-video-off</v-icon>
									</template>
									<v-list-item-title>
										{{ participant.producers?.video?.paused ? '开启摄像头' : '关闭摄像头' }}
									</v-list-item-title>
								</v-list-item>

								<v-divider class="my-1"></v-divider>
							</template>

							<!-- 通用操作 -->
							<v-list-item @click="emit('pin-participant', participant.peerId)">
								<template #prepend>
									<v-icon color="info">mdi-pin</v-icon>
								</template>
								<v-list-item-title>固定视频</v-list-item-title>
							</v-list-item>

							<v-list-item @click="emit('spotlight-participant', participant.peerId)">
								<template #prepend>
									<v-icon color="secondary">mdi-spotlight-beam</v-icon>
								</template>
								<v-list-item-title>聚焦参与者</v-list-item-title>
							</v-list-item>

							<!-- 移除参与者 -->
							<template v-if="canControlOtherParticipant(participant)">
								<v-divider class="my-1"></v-divider>

								<v-list-item class="text-error" @click="handleRemoveParticipant(participant)">
									<template #prepend>
										<v-icon color="error">mdi-account-remove</v-icon>
									</template>
									<v-list-item-title>移除参与者</v-list-item-title>
								</v-list-item>
							</template>
						</v-list>
					</v-menu>
				</div>
			</div>

			<!-- 空状态 -->
			<div v-if="onlineParticipants.length === 0" class="empty-state">
				<v-icon size="64" color="primary-lighten-1">mdi-account-group-outline</v-icon>
				<div class="text-body-2 text-medium-emphasis mt-4">暂无其他参与者</div>
				<v-btn color="primary" variant="elevated" prepend-icon="mdi-account-plus" class="mt-4" @click="showInviteDialog = true"> 邀请参与者 </v-btn>
			</div>
		</div>

		<!-- 邀请对话框 -->
		<v-dialog v-model="showInviteDialog" max-width="500">
			<v-card class="invite-dialog">
				<v-card-title class="d-flex align-center justify-space-between bg-primary">
					<span class="text-h6 text-white">邀请参与者</span>
					<v-btn icon="mdi-close" variant="text" size="small" color="white" @click="showInviteDialog = false"></v-btn>
				</v-card-title>

				<v-divider></v-divider>

				<v-card-text class="pa-6">
					<div class="mb-4">
						<label class="text-subtitle-2 mb-2 d-block text-primary">会议链接</label>
						<v-text-field :model-value="meetingLink" readonly variant="outlined" density="comfortable" color="primary" hide-details>
							<template #append-inner>
								<v-btn icon="mdi-content-copy" size="small" variant="text" color="primary" @click="copyMeetingLink"></v-btn>
							</template>
						</v-text-field>
					</div>

					<div>
						<label class="text-subtitle-2 mb-2 d-block text-primary">会议号</label>
						<v-text-field :model-value="meetingNo" readonly variant="outlined" density="comfortable" color="primary" hide-details>
							<template #append-inner>
								<v-btn icon="mdi-content-copy" size="small" variant="text" color="primary" @click="copyMeetingNo"></v-btn>
							</template>
						</v-text-field>
					</div>

					<v-divider class="my-4"></v-divider>

					<div class="text-caption text-medium-emphasis d-flex align-center">
						<v-icon size="small" color="info" class="mr-2">mdi-information</v-icon>
						将会议链接或 ID 分享给他人即可邀请参与
					</div>
				</v-card-text>

				<v-divider></v-divider>

				<v-card-actions class="pa-4 bg-surface-variant">
					<v-spacer></v-spacer>
					<v-btn variant="text" color="primary" @click="showInviteDialog = false">关闭</v-btn>
					<v-btn color="primary" variant="elevated" @click="shareInvite">分享邀请</v-btn>
				</v-card-actions>
			</v-card>
		</v-dialog>
	</div>
</template>

<style scoped>
.participants-panel {
	display: flex;
	flex-direction: column;
	height: 100%;
	background: rgb(var(--v-theme-background));
	overflow: hidden;
}

.participants-header {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 12px 16px;
	background: rgb(var(--v-theme-surface));
	border-bottom: 1px solid rgb(var(--v-theme-border));
}

.participants-list {
	flex: 1;
	overflow-y: auto;
	overflow-x: hidden;
	padding: 8px;
	min-height: 0;
	background: rgb(var(--v-theme-background));
}

.participants-list::-webkit-scrollbar {
	width: 6px;
}

.participants-list::-webkit-scrollbar-track {
	background: rgb(var(--v-theme-surface-variant));
	border-radius: 3px;
}

.participants-list::-webkit-scrollbar-thumb {
	background: rgb(var(--v-theme-primary));
	opacity: 0.5;
	border-radius: 3px;
}

.participants-list::-webkit-scrollbar-thumb:hover {
	background: rgb(var(--v-theme-primary-darken-1));
	opacity: 0.8;
}

/* 分组区域 */
.participants-section {
	margin-bottom: 16px;
}

.participants-section:last-child {
	margin-bottom: 0;
}

.section-title {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 8px 12px;
	margin-bottom: 4px;
	font-size: 12px;
	font-weight: 600;
	color: rgb(var(--v-theme-on-surface-variant));
	text-transform: uppercase;
	letter-spacing: 0.5px;
	background: rgb(var(--v-theme-surface-variant));
	border-radius: 8px;
}

/* 参与者卡片 */
.participant-item {
	display: flex;
	align-items: center;
	padding: 12px;
	border-radius: 8px;
	margin-bottom: 4px;
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	cursor: pointer;
	background: rgb(var(--v-theme-surface));
	border: 1px solid transparent;
}

.participant-item:hover {
	background: rgb(var(--v-theme-surface-variant));
	border-color: rgb(var(--v-theme-primary));
	transform: translateX(4px);
	box-shadow: 0 2px 8px rgba(var(--v-theme-primary), 0.15);
}

.participant-item.current-user {
	background: rgba(var(--v-theme-primary), 0.1);
	border: 1px solid rgb(var(--v-theme-primary));
}

.participant-item.current-user:hover {
	background: rgba(var(--v-theme-primary), 0.15);
	box-shadow: 0 2px 12px rgba(var(--v-theme-primary), 0.25);
}

/* 头像 */
.participant-item :deep(.v-avatar) {
	flex-shrink: 0;
	border: 2px solid rgb(var(--v-theme-border));
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.participant-item:hover :deep(.v-avatar) {
	border-color: rgb(var(--v-theme-primary));
	box-shadow: 0 2px 8px rgba(var(--v-theme-primary), 0.3);
}

/* 参与者信息 */
.participant-info {
	flex: 1;
	margin-left: 12px;
	min-width: 0;
	overflow: hidden;
}

.participant-name {
	display: flex;
	align-items: center;
	font-size: 14px;
	color: rgb(var(--v-theme-on-surface));
	margin-bottom: 4px;
	white-space: nowrap;
	overflow: hidden;
}

.participant-name > span:first-child {
	overflow: hidden;
	text-overflow: ellipsis;
}

.participant-status {
	display: flex;
	align-items: center;
	font-size: 12px;
	color: rgb(var(--v-theme-on-surface-variant));
}

/* 空状态 */
.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	height: 100%;
	padding: 40px 20px;
	color: rgb(var(--v-theme-on-surface-variant));
}

/* 菜单样式 */
.menu-list {
	background-color: rgb(var(--v-theme-surface));
	border: 1px solid rgb(var(--v-theme-border));
	backdrop-filter: blur(10px);
}

.menu-list .v-list-item {
	border-radius: 4px;
	margin: 2px 4px;
	transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.menu-list .v-list-item:hover {
	background-color: rgb(var(--v-theme-surface-variant));
}

.menu-list .v-list-item.text-error:hover {
	background-color: rgba(var(--v-theme-error), 0.1);
}

/* 邀请对话框 */
.invite-dialog {
	border: 1px solid rgb(var(--v-theme-border));
}

.invite-dialog :deep(.v-field) {
	background-color: rgb(var(--v-theme-surface-variant));
}

.invite-dialog :deep(.v-field__input) {
	color: rgb(var(--v-theme-on-surface));
}

.invite-dialog :deep(.v-field:hover) {
	background-color: rgb(var(--v-theme-surface-bright));
}

/* 响应式调整 */
@media (max-width: 960px) {
	.participant-item {
		padding: 10px;
	}

	.participant-name {
		font-size: 13px;
	}

	.participant-status {
		font-size: 11px;
	}

	.section-title {
		padding: 6px 10px;
		font-size: 11px;
	}
}

/* 深色模式优化 */
.v-theme--dark .participants-panel {
	background: rgb(var(--v-theme-background));
}

.v-theme--dark .participant-item {
	background: rgb(var(--v-theme-surface));
}

.v-theme--dark .participant-item:hover {
	background: rgb(var(--v-theme-surface-bright));
}

.v-theme--dark .menu-list {
	background-color: rgb(var(--v-theme-surface));
	box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
}
</style>
