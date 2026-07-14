<template>
	<v-chip :size="compact ? 'small' : 'default'" color="success" variant="flat" class="raised-hand-status" :class="{ 'raised-hand-status--compact': compact }">
		<template #prepend>
			<span class="raised-hand-status__icon">
				<v-icon icon="mdi-hand-back-right" :size="compact ? 14 : 18"></v-icon>
			</span>
		</template>
		<span class="raised-hand-status__text">
			<slot>{{ label }}</slot>
		</span>
		<template v-if="count > 1" #append>
			<span class="raised-hand-status__count">+{{ count - 1 }}</span>
		</template>
	</v-chip>
</template>

<script setup>
defineProps({
	label: {
		type: String,
		default: '',
	},
	count: {
		type: Number,
		default: 1,
	},
	compact: {
		type: Boolean,
		default: false,
	},
})
</script>

<style scoped>
.raised-hand-status {
	max-width: 100%;
	border-radius: 999px;
	box-shadow: 0 2px 8px rgba(var(--v-theme-success), 0.24);
	font-weight: 600;
	letter-spacing: 0;
}

.raised-hand-status :deep(.v-chip__content) {
	min-width: 0;
}

.raised-hand-status__icon {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	width: 28px;
	height: 28px;
	margin-inline-start: -6px;
	margin-inline-end: 6px;
	border-radius: 50%;
	background: rgba(var(--v-theme-on-surface), 0.14);
	color: rgb(var(--v-theme-on-surface));
}

.raised-hand-status--compact .raised-hand-status__icon {
	width: 22px;
	height: 22px;
	margin-inline-start: -4px;
	margin-inline-end: 4px;
}

.raised-hand-status__text {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.raised-hand-status__count {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	min-width: 22px;
	height: 22px;
	padding: 0 6px;
	border-radius: 999px;
	background: rgba(var(--v-theme-on-surface), 0.12);
	font-size: 12px;
	font-weight: 700;
}
</style>
