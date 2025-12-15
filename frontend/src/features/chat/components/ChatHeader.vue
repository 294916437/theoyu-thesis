<script setup>
defineProps({
	user: {
		type: Object,
		required: true,
	},
})
</script>
<template>
	<v-sheet color="surface" class="chat-header px-4 py-3" elevation="1">
		<div class="d-flex align-center justify-space-between">
			<div class="d-flex align-center gap-3">
				<v-avatar :image="user.avatar" size="44" color="grey-lighten-2">
					<v-icon v-if="!user.avatar" icon="mdi-account" size="24"></v-icon>
				</v-avatar>

				<div>
					<h3 class="text-subtitle-1 font-weight-bold">
						{{ user.nickname }}
					</h3>
					<p class="text-caption text-medium-emphasis">在线</p>
				</div>
			</div>

			<!-- 修改按钮尺寸 -->
			<div class="d-flex gap-2">
				<v-btn icon="mdi-video-outline" variant="text" size="default" color="primary">
					<v-icon size="22"></v-icon>
				</v-btn>
				<v-btn icon="mdi-phone-outline" variant="text" size="default" color="primary">
					<v-icon size="22"></v-icon>
				</v-btn>
				<v-btn icon="mdi-information-outline" variant="text" size="default" color="primary">
					<v-icon size="22"></v-icon>
				</v-btn>
			</div>
		</div>
	</v-sheet>
</template>

<style scoped>
.chat-header {
	border-bottom: 1px solid rgba(var(--v-theme-on-surface), 0.08);
	min-height: 72px;
}

.gap-3 {
	gap: 12px;
}

.gap-2 {
	gap: 8px;
}
</style>
