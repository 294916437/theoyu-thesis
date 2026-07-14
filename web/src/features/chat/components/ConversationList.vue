<template>
	<v-list class="pa-0" bg-color="transparent">
		<v-list-item
			v-for="conversation in conversations"
			:key="conversation.id"
			:class="['conversation-item', { 'conversation-item--active': activeId === conversation.id }]"
			:ripple="true"
			lines="two"
			@click="$emit('select', conversation.id)"
		>
			<template #prepend>
				<v-badge
					:content="conversation.unreadCount > 99 ? '99+' : conversation.unreadCount"
					:model-value="conversation.unreadCount > 0"
					color="error"
					max="99"
					offset-x="4"
					offset-y="4"
				>
					<v-avatar size="48" color="primary" class="elevation-1">
						<v-img v-if="conversation.user?.avatar" :src="conversation.user.avatar" :alt="conversation.user.nickname">
							<template #error>
								<v-icon icon="mdi-account" size="24" color="white"></v-icon>
							</template>
						</v-img>
						<v-icon v-else icon="mdi-account" size="24" color="white"></v-icon>
					</v-avatar>
				</v-badge>
			</template>

			<v-list-item-title class="text-subtitle-1 font-weight-medium mb-1">
				{{ conversation.user?.nickname || '未知用户' }}
			</v-list-item-title>

			<v-list-item-subtitle class="text-body-2 text-medium-emphasis text-truncate">
				{{ getLastMessagePreview(conversation) }}
			</v-list-item-subtitle>

			<template #append>
				<div class="conversation-action-area d-flex flex-column align-end justify-space-between py-1">
					<span class="text-caption text-disabled time-text">
						{{ conversation.lastMessageTime ? formatTime(conversation.lastMessageTime) : '' }}
					</span>
					
					<v-btn
						icon="mdi-delete-outline"
						variant="text"
						size="small"
						color="error"
						class="delete-btn"
						@click.stop="emit('delete', conversation.id)"
						aria-label="Delete conversation"
					/>
				</div>
			</template>
		</v-list-item>

		<v-list-item v-if="conversations.length === 0" class="text-center py-8">
			<v-list-item-title class="text-body-2 text-medium-emphasis"> 暂无对话 </v-list-item-title>
		</v-list-item>
	</v-list>
</template>

<script setup>
import { formatTime } from '@/utils/formatTime'

const props = defineProps({
	conversations: {
		type: Array,
		required: true,
	},
	activeId: {
		type: String,
		default: null,
	},
})

const emit = defineEmits(['select', 'delete'])

// 获取最后一条消息预览
const getLastMessagePreview = conversation => {
	const content = conversation.lastMessageContent
	if (!content && !conversation.lastMessageType) return '暂无消息'

	if (conversation.lastMessageType === 2) return '[图片]'
	if (conversation.lastMessageType === 3 || conversation.lastMessageType === 4) return '[视频]'
	if (conversation.lastMessageType === 6) return '[文件]'

	if (!content) return '暂无消息'
	return content.length > 30 ? content.substring(0, 30) + '...' : content
}
</script>

<style scoped>
.conversation-item {
	transition: background-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
	border-bottom: 1px solid rgba(var(--v-theme-on-surface), 0.08);
	cursor: pointer;
	padding-inline-end: 16px !important;
}

.conversation-item:hover {
	background-color: rgba(var(--v-theme-primary), 0.04);
}

.conversation-item--active {
	background-color: rgba(var(--v-theme-primary), 0.08);
}

.conversation-item--active:hover {
	background-color: rgba(var(--v-theme-primary), 0.12);
}

.conversation-item:deep(.v-list-item__prepend) {
	align-self: center;
	margin-inline-end: 16px !important;
}

.conversation-action-area {
	min-width: 65px;
	height: 100%;
	position: relative;
}

.time-text {
	transition: opacity 0.2s ease, transform 0.2s ease;
	white-space: nowrap;
	line-height: 1.2;
}

.delete-btn {
	position: absolute;
	right: -8px;
	top: 50%;
	transform: translateY(-50%) scale(0.8);
	opacity: 0;
	pointer-events: none;
	transition: opacity 0.2s ease, transform 0.2s ease;
}

.conversation-item:hover .time-text {
	opacity: 0;
	transform: translateY(-4px);
}

.conversation-item:hover .delete-btn {
	opacity: 1;
	pointer-events: auto;
	transform: translateY(-50%) scale(1);
}
</style>
