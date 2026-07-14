<script setup>
import { defineProps } from 'vue'

defineProps({
	count: {
		type: Number,
		default: 8,
	},
})
</script>

<template>
	<v-list class="pa-0" bg-color="transparent">
		<v-list-item v-for="i in count" :key="i" class="py-3">
			<template #prepend>
				<v-skeleton-loader type="avatar" width="48" height="48"></v-skeleton-loader>
			</template>

			<v-skeleton-loader type="list-item-two-line"></v-skeleton-loader>

			<template #append>
				<v-skeleton-loader type="chip" width="60"></v-skeleton-loader>
			</template>
		</v-list-item>
	</v-list>
</template>

<style scoped>
/* 骨架屏默认样式已足够，无需额外样式 */
</style>
