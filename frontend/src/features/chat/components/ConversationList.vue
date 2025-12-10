<template>
	<div class="w-96 border-r border-gray-200 flex flex-col">
		<!-- 头部 -->
		<div class="px-4 py-3 border-b border-gray-200">
			<div class="flex items-center justify-between mb-3">
				<h1 class="text-xl font-bold">消息列表</h1>
				<div class="flex gap-2">
					<IconButton aria-label="Settings" @click="handleSettings">
						<svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
							<path
								d="M19.43 12.98c.04-.32.07-.64.07-.98 0-.34-.03-.66-.07-.98l2.11-1.65c.19-.15.24-.42.12-.64l-2-3.46c-.09-.16-.26-.25-.44-.25-.06 0-.12.01-.17.03l-2.49 1c-.52-.4-1.08-.73-1.69-.98l-.38-2.65C14.46 2.18 14.25 2 14 2h-4c-.25 0-.46.18-.49.42l-.38 2.65c-.61.25-1.17.59-1.69.98l-2.49-1c-.06-.02-.12-.03-.18-.03-.17 0-.34.09-.43.25l-2 3.46c-.13.22-.07.49.12.64l2.11 1.65c-.04.32-.07.65-.07.98 0 .33.03.66.07.98l-2.11 1.65c-.19.15-.24.42-.12.64l2 3.46c.09.16.26.25.44.25.06 0 .12-.01.17-.03l2.49-1c.52.4 1.08.73 1.69.98l.38 2.65c.03.24.24.42.49.42h4c.25 0 .46-.18.49-.42l.38-2.65c.61-.25 1.17-.59 1.69-.98l2.49 1c.06.02.12.03.18.03.17 0 .34-.09.43-.25l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.65zM12 15.5c-1.93 0-3.5-1.57-3.5-3.5s1.57-3.5 3.5-3.5 3.5 1.57 3.5 3.5-1.57 3.5-3.5 3.5z"
							/>
						</svg>
					</IconButton>
					<IconButton aria-label="New message" @click="handleNewMessage">
						<svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
							<path
								d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"
							/>
						</svg>
					</IconButton>
				</div>
			</div>

			<!-- 搜索框 -->
			<div class="relative">
				<input
					v-model="searchQuery"
					type="text"
					placeholder="直接搜索消息"
					class="w-full px-4 py-2 pl-10 bg-gray-100 rounded-full outline-none focus:bg-gray-200 transition-colors text-sm"
				/>
				<svg
					class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-500"
					fill="currentColor"
					viewBox="0 0 24 24"
				>
					<path
						d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"
					/>
				</svg>
			</div>
		</div>

		<!-- Chat 标签 -->
		<!-- <div class="px-4 py-3 border-b border-gray-200">
			<div class="flex items-center gap-2">
				<svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
					<path
						d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"
					/>
				</svg>
				<span class="font-medium">聊天</span>
				<span class="ml-auto px-2 py-0.5 bg-blue-500 text-white text-xs rounded-full">测试</span>
			</div>
		</div> -->

		<!-- 对话列表 -->
		<div class="flex-1 overflow-y-auto">
			<MessageListItem
				v-for="conversation in filteredConversations"
				:key="conversation.id"
				:conversation="conversation"
				:active="activeId === conversation.id"
				@click="$emit('select', conversation.id)"
			/>

			<div v-if="filteredConversations.length === 0" class="p-4 text-center text-gray-500">
				没有找到对话
			</div>
		</div>
	</div>
</template>

<script setup>
import { ref, computed } from 'vue'
import MessageListItem from './MessageListItem.vue'
import IconButton from '@/components/common/IconButton.vue'

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

defineEmits(['select'])

const searchQuery = ref('')
const handleSettings = () => {
	console.log('打开设置')
}
const handleNewMessage = () => {
	console.log('新建消息')
}
// 过滤对话列表
const filteredConversations = computed(() => {
	if (!searchQuery.value.trim()) {
		return props.conversations
	}

	const query = searchQuery.value.toLowerCase()
	return props.conversations.filter(
		conv =>
			conv.user.name.toLowerCase().includes(query) ||
			conv.user.username.toLowerCase().includes(query) ||
			conv.lastMessage.text.toLowerCase().includes(query),
	)
})
</script>
<style scoped>
.icon-button {
	padding: 0.5rem;
	border-radius: 9999px;
	transition: background-color 0.2s;

	&:hover {
		background-color: var(--color-primary-hover);
	}
}
</style>
