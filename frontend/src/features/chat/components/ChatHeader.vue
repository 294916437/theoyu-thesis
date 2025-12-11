<template>
	<v-sheet color="surface" class="chat-header px-4 py-3" elevation="1">
		<div class="d-flex align-center justify-space-between">
			<div class="d-flex align-center gap-3">
				<v-avatar :image="conversation.targetUser?.avatar" size="40" color="grey-lighten-2">
					<v-icon v-if="!conversation.targetUser?.avatar" icon="mdi-account"></v-icon>
				</v-avatar>

				<div>
					<h3 class="text-subtitle-1 font-weight-bold">
						{{ conversation.targetUser?.nickName || '未知用户' }}
					</h3>
					<p class="text-caption text-disabled">在线</p>
				</div>
			</div>

			<div class="d-flex gap-2">
				<v-btn
					icon="mdi-video-outline"
					variant="text"
					size="small"
					color="primary"
					density="comfortable"
				></v-btn>
				<v-btn
					icon="mdi-phone-outline"
					variant="text"
					size="small"
					color="primary"
					density="comfortable"
				></v-btn>
				<v-btn
					icon="mdi-information-outline"
					variant="text"
					size="small"
					color="primary"
					density="comfortable"
				></v-btn>
			</div>
		</div>
	</v-sheet>
</template>

<script setup>
defineProps({
	user: {
		type: Object,
		required: true,
	},
})
</script>
<style scoped>
.chat-header {
	border-bottom: 1px solid rgb(var(--v-theme-border));
}

.gap-3 {
	gap: 12px;
}

.gap-2 {
	gap: 8px;
}
</style>
