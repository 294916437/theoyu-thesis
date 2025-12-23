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
					:content="conversation.unreadCount"
					:model-value="conversation.unreadCount > 0"
					color="error"
					offset-x="-8"
					offset-y="-2"
				>
					<v-avatar :image="conversation.targetUser?.avatar" size="48" color="grey-lighten-2">
						<v-icon v-if="!conversation.targetUser?.avatar" icon="mdi-account" size="32"></v-icon>
					</v-avatar>
				</v-badge>
			</template>

			<v-list-item-title class="text-subtitle-2 font-weight-medium mb-1">
				{{ conversation.targetUser?.nickName || '未知用户' }}
			</v-list-item-title>

			<v-list-item-subtitle class="text-caption text-truncate">
				{{ getLastMessagePreview(conversation) }}
			</v-list-item-subtitle>

			<template #append>
				<div class="d-flex flex-column align-end" style="min-width: 60px">
					<span class="text-caption text-disabled">{{ formatTime(conversation.lastMessageTime) }}</span>
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
// 获取最后一条消息预览
const getLastMessagePreview = conversation => {
	const content = conversation.lastMessageContent
	if (!content) return '暂无消息'

	if (conversation.lastMessageType === 2) return '[图片]'
	if (conversation.lastMessageType === 3) return '[视频]'

	return content.length > 30 ? content.substring(0, 30) + '...' : content
}

defineEmits(['select'])
</script>
<style scoped>
.conversation-item {
	transition: background-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
	border-bottom: 1px solid rgb(var(--v-theme-divider));
	cursor: pointer;
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
}
</style>
