<template>
	<div class="url-copy-box" @click="$emit('copy-url')">
		<v-icon icon="mdi-link-variant" size="16" class="mr-2 flex-shrink-0" color="primary"></v-icon>
		<span class="url-text text-caption">{{ result?.fileUrl }}</span>
		<v-icon :icon="copied ? 'mdi-check' : 'mdi-content-copy'" size="16" :color="copied ? 'success' : 'medium-emphasis'" class="ml-2 flex-shrink-0"></v-icon>
	</div>
</template>

<script setup>
defineProps({
	result: Object,
	copied: Boolean,
})

defineEmits(['copy-url'])
</script>

<style scoped>
.url-copy-box {
	display: flex;
	align-items: center;
	padding: 10px 12px;
	border-radius: 8px;
	border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
	background: rgba(var(--v-theme-surface-variant), 0.6);
	cursor: pointer;
	transition: background 0.2s;
	overflow: hidden;
}
.url-copy-box:hover {
	background: rgba(var(--v-theme-primary), 0.06);
	border-color: rgba(var(--v-theme-primary), 0.3);
}
.url-text {
	flex: 1;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	color: rgb(var(--v-theme-on-surface-variant));
	font-family: monospace;
	font-size: 0.72rem;
}
</style>
