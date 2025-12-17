<template>
	<v-container fluid class="chat-page pa-0 fill-height">
		<v-row no-gutters class="fill-height">
			<!-- 左侧会话列表 -->
			<v-col cols="12" md="4" lg="3" class="conversation-sidebar">
				<v-sheet color="surface" class="fill-height d-flex flex-column" elevation="2">
					<!-- 头部 -->
					<div class="conversation-header px-4 py-3 border-b">
						<div class="d-flex align-center justify-space-between">
							<h2 class="text-h6 font-weight-bold text-primary">私信</h2>
							<div class="d-flex gap-2">
								<v-btn icon="mdi-magnify" variant="text" size="large" density="comfortable"></v-btn>
								<v-btn
									icon="mdi-plus"
									variant="text"
									size="large"
									density="comfortable"
									color="primary"
								></v-btn>
							</div>
						</div>
					</div>

					<!-- 会话列表区域 -->
					<div class="flex-1 overflow-y-auto">
						<ConversationListSkeleton v-if="initialLoadingConversations" :count="5" />

						<ConversationList
							v-else
							:conversations="conversations"
							:active-id="activeConversationId"
							@select="handleSelectConversation"
						/>
					</div>
				</v-sheet>
			</v-col>

			<!-- 右侧聊天面板 -->
			<v-col cols="12" md="8" lg="9" class="chat-main">
				<ChatPanel
					v-if="activeConversation"
					:conversation="activeConversation"
					@send-message="handleSendMessage"
					@load-more="handleLoadMoreMessages"
				/>

				<!-- 空状态 -->
				<v-sheet v-else color="background" class="fill-height d-flex align-center justify-center">
					<div class="empty-state text-center pa-8">
						<v-icon icon="mdi-message-text-outline" size="80" color="grey-lighten-1"></v-icon>
						<h3 class="text-h6 mt-4 text-medium-emphasis">选择一个对话开始聊天</h3>
						<p class="text-body-2 text-disabled mt-2">或者创建新的对话</p>
						<v-btn
							color="primary"
							variant="elevated"
							class="mt-6"
							prepend-icon="mdi-plus"
							rounded="lg"
							@click="handleCreateConversation"
						>
							新建对话
						</v-btn>
					</div>
				</v-sheet>
			</v-col>
		</v-row>
	</v-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import ConversationList from '@/features/chat/components/ConversationList.vue'
import ChatPanel from '@/features/chat/components/ChatPanel.vue'
import ConversationListSkeleton from '../components/ConversationListSkeleton.vue'
import { fetchConversations, fetchConversationMessages, createConversation } from '@/api/chat'
import { useUserStore } from '@/stores/user'
import messageService from '@/services/MessageService'
import { $notify } from '@/plugins/notification'

// ==================== 状态管理 ====================

const userStore = useUserStore()
const currentUserId = userStore.userId
const activeConversationId = ref(null)
const conversations = ref([])
// 消息聊天的 WebSocket 连接状态
const isWebSocketConnected = computed(() => messageService.isConnected.value)
// 存储每个会话的消息列表
const conversationMessagesMap = ref(new Map())

// 存储每个会话的消息游标
const conversationCursorsMap = ref(new Map())

// 存储每个会话是否还有更多消息
const conversationHasMoreMap = ref(new Map())

// 加载状态
const loadingConversations = ref(false)
const initialLoadingConversations = ref(true)
const loadingMessages = ref(false)

// 骨架屏最小显示时间(毫秒)
const SKELETON_MIN_DURATION = 600

// ==================== 计算属性 ====================
const activeConversation = computed(() => {
	if (!activeConversationId.value) {
		return null
	}

	// 从会话列表中找到当前会话
	const conversation = conversations.value.find(c => c.id === activeConversationId.value)

	if (!conversation) {
		console.warn('未找到会话信息:', activeConversationId.value)
		return null
	}

	// 获取消息列表
	const messages = conversationMessagesMap.value.get(activeConversationId.value) || []

	// 构造符合 ChatPanel prop 要求的对象
	return {
		...conversation,
		messages,
	}
})
// ==================== WebSocket 初始化 ====================

/**
 * 初始化 WebSocket 连接
 */
const initWebSocket = async () => {
	try {
		const wsUrl = import.meta.env.VITE_WS_MESSAGE_SERVER

		await messageService.connect(wsUrl, currentUserId)

		console.log('消息服务连接成功')

		// 注册消息监听器
		setupMessageListeners()
	} catch (error) {
		console.error('消息服务连接失败:', error)
		$notify.error('消息服务连接失败，请刷新页面重试')
	}
}

/**
 * 设置消息监听器
 */
const setupMessageListeners = () => {
	// 监听新消息
	messageService.on('message-receive', handleNewMessage)

	// 监听消息发送成功
	messageService.on('message-sent', handleMessageSent)

	// 监听消息发送失败
	messageService.on('message-error', handleMessageError)

	// 监听会话更新
	messageService.on('conversation-updated', handleConversationUpdated)
}

/**
 * 处理新消息到达
 */
const handleNewMessage = messageData => {
	console.log('========== 收到新消息 ==========')
	console.log('消息数据:', messageData)

	const {
		conversationId,
		id,
		senderId,
		senderNickname,
		senderAvatar,
		messageType,
		content,
		imgUris,
		videoUri,
		createdTime,
		isSelf,
	} = messageData

	// 构建消息对象
	const newMessage = {
		id,
		conversationId,
		senderId,
		senderNickname,
		senderAvatar,
		messageType,
		content,
		imgUris,
		videoUri,
		createdTime,
		isSelf,
	}

	// 添加到对应会话的消息列表
	const messages = conversationMessagesMap.value.get(conversationId) || []

	// 检查消息是否已存在(去重)
	const exists = messages.some(msg => msg.id === id)
	if (!exists) {
		messages.push(newMessage)
		conversationMessagesMap.value.set(conversationId, messages)
		console.log('消息已添加到会话:', conversationId)
	} else {
		console.log('消息已存在，跳过:', id)
	}

	// 更新会话列表
	const conversation = conversations.value.find(c => c.id === conversationId)
	if (conversation) {
		conversation.lastMessageContent = content
		conversation.lastMessageTime = createdTime

		// 如果不是当前会话，增加未读数
		if (conversationId !== activeConversationId.value) {
			conversation.unreadCount = (conversation.unreadCount || 0) + 1
		}

		// 将该会话移到列表顶部
		const index = conversations.value.indexOf(conversation)
		if (index > 0) {
			conversations.value.splice(index, 1)
			conversations.value.unshift(conversation)
		}

		console.log('会话列表已更新')
	} else {
		console.warn('未找到会话:', conversationId)
	}
}
/**
 * 处理消息发送成功
 */
const handleMessageSent = messageData => {
	console.log('消息发送成功:', messageData)
}

/**
 * 处理消息发送失败
 */
const handleMessageError = errorData => {
	console.error('消息发送失败:', errorData)
	$notify.error('消息发送失败: ' + (errorData.message || '未知错误'))
}

/**
 * 处理会话更新
 */
const handleConversationUpdated = conversationData => {
	console.log('会话更新:', conversationData)
	// 可以在这里更新会话信息
}

// ==================== 数据加载方法 ====================

/**
 * 加载会话列表
 */
const loadConversations = async () => {
	if (loadingConversations.value) {
		console.log('会话列表正在加载中...')
		return
	}

	loadingConversations.value = true

	try {
		// 创建最小延迟 Promise
		const minDelayPromise = new Promise(resolve => {
			if (initialLoadingConversations.value) {
				setTimeout(resolve, SKELETON_MIN_DURATION)
			} else {
				resolve()
			}
		})

		const dataPromise = await fetchConversations({
			userId: currentUserId,
			cursor: null,
		})

		// 等待两个 Promise 都完成
		const [result] = await Promise.all([dataPromise, minDelayPromise])

		if (result.success && result.data) {
			const { conversations: conversationList } = result.data
			conversations.value = conversationList || []
		} else {
			conversations.value = []
		}
	} catch (error) {
		console.error('加载会话列表失败:', error)
		$notify.error('加载会话列表失败')
		conversations.value = []
	} finally {
		loadingConversations.value = false
		initialLoadingConversations.value = false
	}
}

/**
 * 加载会话消息列表（支持游标分页）
 */
const loadMessages = async (conversationId, isLoadMore = false) => {
	if (loadingMessages.value) {
		console.log('消息列表正在加载中...')
		return
	}

	loadingMessages.value = true

	try {
		// 获取当前游标
		const cursor = isLoadMore ? conversationCursorsMap.value.get(conversationId) : null

		const result = await fetchConversationMessages(conversationId, {
			cursor,
			limit: 20,
		})

		if (result.success && result.data) {
			const { messages, nextCursor, hasMore } = result.data

			if (isLoadMore) {
				// 加载更多：将新消息插入到数组开头（历史消息在前）
				const currentMessages = conversationMessagesMap.value.get(conversationId) || []
				conversationMessagesMap.value.set(conversationId, [...messages, ...currentMessages])
			} else {
				// 初次加载：直接替换
				conversationMessagesMap.value.set(conversationId, messages || [])
			}

			// 更新游标和是否有更多消息的标志
			conversationCursorsMap.value.set(conversationId, nextCursor)
			conversationHasMoreMap.value.set(conversationId, hasMore)
		} else {
			console.warn('消息列表为空')
			conversationMessagesMap.value.set(conversationId, [])
		}
	} catch (error) {
		console.error('加载消息失败:', error)
		$notify.error('加载消息失败')
	} finally {
		loadingMessages.value = false
	}
}

// ==================== 事件处理方法 ====================

/**
 * 选择会话
 */
const handleSelectConversation = async id => {
	if (activeConversationId.value === id) {
		console.log('已选中该会话，跳过')
		return
	}

	console.log('切换到会话:', id)
	activeConversationId.value = id

	await loadMessages(id, false)
	// 重置未读数
	const conversation = conversations.value.find(c => c.id === id)
	if (conversation) {
		conversation.unreadCount = 0
	}
}

/**
 * 发送消息（通过 WebSocket）
 */
const handleSendMessage = async messageData => {
	if (!activeConversationId.value) {
		console.warn('会话ID为空')
		return
	}

	if (!isWebSocketConnected.value) {
		$notify.error('消息服务未连接，请稍后再试')
		return
	}

	try {
		const { type, content, imageUris, videoUri } = messageData

		// 根据类型发送不同的消息
		if (type === 'text') {
			messageService.sendTextMessage(activeConversationId.value, content)
		} else if (type === 'image') {
			messageService.sendImageMessage(activeConversationId.value, imageUris)
		} else if (type === 'video') {
			messageService.sendVideoMessage(activeConversationId.value, videoUri)
		} else {
			console.warn('未知的消息类型:', type)
			return
		}

		// 乐观更新UI
		const tempMessage = {
			id: Date.now(),
			conversationId: activeConversationId.value,
			senderId: currentUserId,
			senderNickname: userStore.profile.nickname,
			senderAvatar: userStore.profile.avatar,
			messageType: type === 'text' ? 1 : type === 'image' ? 2 : 4, // 1=文本, 2=图片, 3=视频
			content: type === 'text' ? content : '',
			imgUris: type === 'image' ? imageUris : [],
			videoUri: type === 'video' ? videoUri : '',
			createdTime: new Date().toISOString(),
			isSelf: true,
		}

		const messages = conversationMessagesMap.value.get(activeConversationId.value) || []
		messages.push(tempMessage)
		conversationMessagesMap.value.set(activeConversationId.value, messages)

		// 更新会话列表
		const conversation = conversations.value.find(c => c.id === activeConversationId.value)
		if (conversation) {
			conversation.lastMessageContent = type === 'text' ? content : type === 'image' ? '[图片]' : '[视频]'
			conversation.lastMessageTime = new Date().toISOString()

			const index = conversations.value.indexOf(conversation)
			if (index > 0) {
				conversations.value.splice(index, 1)
				conversations.value.unshift(conversation)
			}
		}
	} catch (error) {
		console.error('发送消息失败:', error)
		$notify.error('发送消息失败: ' + error.message)
	}
}

/**
 * 加载更多消息（用户向上滚动时触发）
 */
const handleLoadMoreMessages = async () => {
	if (!activeConversationId.value) {
		return
	}

	const hasMore = conversationHasMoreMap.value.get(activeConversationId.value)

	if (!hasMore) {
		console.log('没有更多历史消息了')
		return
	}

	console.log('加载更多历史消息...')
	await loadMessages(activeConversationId.value, true)
}

/**
 * 创建新会话
 * @param {number} targetUserId - 目标用户ID
 */
const handleCreateConversation = async targetUserId => {
	try {
		console.log('创建新会话, targetUserId:', targetUserId)

		const result = await createConversation({ targetUserId })

		if (result.success) {
			console.log('会话创建成功')

			// 重新加载会话列表
			await loadConversations()

			// 自动选中新创建的会话
			if (result.data?.conversationId) {
				await handleSelectConversation(result.data.conversationId)
			}
		} else {
			throw new Error(result.message || '创建失败')
		}
	} catch (error) {
		console.error(' 创建会话失败:', error)
		$notify.error('创建会话失败: ' + error.message)
	}
}

// ==================== 生命周期 ====================

onMounted(async () => {
	// 1. 初始化 WebSocket 连接
	await initWebSocket()

	// 2. 加载会话列表
	await loadConversations()
})
onUnmounted(() => {
	// 断开 WebSocket 连接
	messageService.disconnect()
})
</script>
<style scoped>
.chat-page {
	height: calc(100vh - 64px);
	background-color: rgb(var(--v-theme-background));
}

.conversation-sidebar {
	border-right: 1px solid rgb(var(--v-theme-border));
}

.conversation-header {
	border-bottom: 1px solid rgb(var(--v-theme-border));
}

.border-b {
	border-bottom: 1px solid rgb(var(--v-theme-border));
}

.gap-2 {
	gap: 8px;
}

.overflow-y-auto {
	overflow-y: auto;
	overflow-x: hidden;
}

.overflow-y-auto::-webkit-scrollbar {
	width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
	background: transparent;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
	background-color: rgba(var(--v-theme-on-surface), 0.2);
	border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
	background-color: rgba(var(--v-theme-on-surface), 0.3);
}

.empty-state {
	animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
	from {
		opacity: 0;
		transform: translateY(20px);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}
</style>
