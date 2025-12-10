<template>
	<div class="flex gap-2" :class="message.isSelf ? 'justify-end' : 'justify-start'">
		<div
			class="max-w-md px-4 py-3 rounded-3xl"
			:class="
				message.isSelf
					? 'bg-blue-500 text-white rounded-br-sm'
					: 'bg-gray-200 text-gray-900 rounded-bl-sm'
			"
		>
			<p class="text-sm whitespace-pre-wrap break-words">{{ message.content }}</p>
		</div>
	</div>

	<!-- 时间戳 -->
	<div class="text-xs text-gray-500 mt-1" :class="message.isSelf ? 'text-right mr-2' : 'text-left'">
		{{ formattedTime }} · {{ message.isSelf ? '发送' : '接受' }}
	</div>
</template>

<script setup>
import { computed } from 'vue'
import { formatTime } from '@/utils/formatTime'

const props = defineProps({
	message: {
		type: Object,
		required: true,
	},
	user: {
		type: Object,
		required: true,
	},
})
const formattedTime = computed(() => {
	return formatTime(props.message.createdTime)
})
</script>
