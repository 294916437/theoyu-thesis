<template>
	<div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
		<div class="flex items-center gap-3">
			<img :src="user.avatar" :alt="user.nickname" class="w-8 h-8 rounded-full" />
			<div>
				<div class="flex items-center gap-1">
					<span class="font-bold text-sm">{{ user.nickname }}</span>
				</div>
			</div>
		</div>

		<button class="p-2 hover:bg-gray-100 rounded-full transition-colors" aria-label="More options">
			<svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
				<path
					d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"
				/>
			</svg>
		</button>
	</div>
</template>

<script setup>
defineProps({
	user: {
		type: Object,
		required: true,
	},
})
</script>
