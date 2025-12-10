<template>
	<div
		class="message-item"
		:class="[active ? 'message-item--active' : '', hasUnread ? 'message-item--unread' : '']"
		@click="$emit('click')"
	>
		<!-- 头像 -->
		<img
			:src="conversation.user.avatar"
			:alt="conversation.user.name"
			class="w-12 h-12 rounded-full flex-shrink-0"
		/>

		<!-- 内容 -->
		<div class="flex-1 min-w-0">
			<div class="flex items-center gap-1 mb-1">
				<span class="font-semibold truncate" :class="hasUnread ? 'text-black' : 'text-gray-900'">
					{{ conversation.user.nickname }}
				</span>
				<span class="message-item__meta">·</span>
				<span class="message-item__meta flex-shrink-0">
					{{ formatTime(conversation.lastMessageTime) }}
				</span>
			</div>

			<p
				class="text-sm truncate"
				:class="hasUnread ? 'text-gray-900 font-medium' : 'text-gray-600'"
			>
				{{ conversation.lastMessageContent }}
			</p>
		</div>

		<!-- 未读数量徽章 -->
		<div v-if="hasUnread" class="flex-shrink-0 mt-2">
			<div
				v-if="unreadCount > 0"
				class="unread-badge"
				:class="unreadCount > 99 ? 'unread-badge--large' : ''"
			>
				{{ displayUnreadCount }}
			</div>
			<div v-else class="w-2 h-2 bg-blue-500 rounded-full"></div>
		</div>
	</div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
	conversation: {
		type: Object,
		required: true,
	},
	active: {
		type: Boolean,
		default: false,
	},
})

defineEmits(['click'])

// ==================== 计算属性 ====================

/**
 * 未读消息数量
 */
const unreadCount = computed(() => {
	return props.conversation.unreadCount || 0
})

/**
 * 是否有未读消息
 */
const hasUnread = computed(() => {
	return unreadCount.value > 0
})

/**
 * 显示的未读数量（超过99显示99+）
 */
const displayUnreadCount = computed(() => {
	const count = unreadCount.value
	if (count > 99) {
		return '99+'
	}
	return count
})

/**
 * 格式化时间显示
 * @param {string} timeString - ISO 时间字符串
 * @returns {string} 格式化后的时间
 */
const formatTime = timeString => {
	if (!timeString) {
		return ''
	}

	try {
		const date = new Date(timeString)

		// 验证日期是否有效
		if (isNaN(date.getTime())) {
			console.warn('无效的时间格式:', timeString)
			return ''
		}

		const now = new Date()
		const diffMs = now - date
		const diffMins = Math.floor(diffMs / 60000)
		const diffHours = Math.floor(diffMs / 3600000)
		const diffDays = Math.floor(diffMs / 86400000)

		// 今天以内
		if (diffMins < 1) return '刚刚'
		if (diffMins < 60) return `${diffMins}分钟前`
		if (diffHours < 24) {
			// 显示具体时间（如：14:30）
			return date.toLocaleTimeString('zh-CN', {
				hour: '2-digit',
				minute: '2-digit',
			})
		}

		// 昨天
		if (diffDays === 1) {
			return '昨天'
		}

		// 本周内
		if (diffDays < 7) {
			return `${diffDays}天前`
		}

		// 本年内
		if (date.getFullYear() === now.getFullYear()) {
			return date.toLocaleDateString('zh-CN', {
				month: 'short',
				day: 'numeric',
			})
		}

		// 跨年
		return date.toLocaleDateString('zh-CN', {
			year: 'numeric',
			month: 'short',
			day: 'numeric',
		})
	} catch (error) {
		console.error('格式化时间失败:', error)
		return ''
	}
}
</script>

<style scoped>
.message-item {
	display: flex;
	align-items: flex-start;
	gap: 0.75rem;
	padding: 0.75rem 1rem;
	cursor: pointer;
	transition: background-color 0.2s;
	border-bottom: 1px solid rgb(243 244 246);
}

.message-item:hover {
	background-color: rgb(249 250 251);
}

.message-item--active {
	background-color: rgb(243 244 246);
}

.message-item--active:hover {
	background-color: rgb(243 244 246);
}

.message-item--unread {
	background-color: rgb(239 246 255);
}

.message-item--unread:hover {
	background-color: rgb(229 238 255);
}

.message-item__meta {
	color: rgb(107 114 128);
	font-size: 0.875rem;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

/* 未读徽章样式 */
.unread-badge {
	display: flex;
	align-items: center;
	justify-content: center;
	min-width: 1.25rem;
	height: 1.25rem;
	padding: 0 0.375rem;
	background-color: rgb(239 68 68);
	color: white;
	font-size: 0.75rem;
	font-weight: 600;
	border-radius: 0.625rem;
	line-height: 1;
}

.unread-badge--large {
	min-width: 1.5rem;
	height: 1.5rem;
	padding: 0 0.5rem;
}

/* 头像容器 */
.message-item img {
	transition: opacity 0.2s;
}

.message-item:hover img {
	opacity: 0.9;
}
</style>
