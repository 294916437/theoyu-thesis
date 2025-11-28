<template>
	<v-card elevation="2" class="pa-6">
		<v-card-title class="text-h5 mb-4"> 快速操作 </v-card-title>

		<v-row>
			<v-col cols="12" md="6">
				<v-card
					elevation="4"
					class="action-card pa-6 text-center"
					color="primary"
					@click="emit('create-meeting')"
				>
					<v-icon size="64" color="white" class="mb-4"> mdi-video-plus </v-icon>
					<h3 class="text-h6 text-white mb-2">创建会议</h3>
					<p class="text-white opacity-80">立即开始新的视频会议</p>
				</v-card>
			</v-col>

			<v-col cols="12" md="6">
				<v-card elevation="4" class="action-card pa-6" color="secondary">
					<div class="text-center mb-4">
						<v-icon size="64" color="white" class="mb-4"> mdi-login </v-icon>
						<h3 class="text-h6 text-white mb-2">加入会议</h3>
					</div>

					<v-text-field
						v-model="meetingId"
						label="输入会议ID"
						variant="outlined"
						density="comfortable"
						bg-color="white"
						hide-details
						@keyup.enter="handleJoin"
					>
						<template #append-inner>
							<v-btn icon="mdi-arrow-right" size="small" color="secondary" @click="handleJoin"></v-btn>
						</template>
					</v-text-field>
				</v-card>
			</v-col>
		</v-row>
	</v-card>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['join-meeting', 'create-meeting'])

const meetingId = ref('')

const handleJoin = () => {
	if (meetingId.value.trim()) {
		emit('join-meeting', meetingId.value.trim())
		meetingId.value = ''
	}
}
</script>

<style scoped>
.action-card {
	cursor: pointer;
	transition:
		transform 0.2s,
		box-shadow 0.2s;
	border-radius: 12px;
}

.action-card:hover {
	transform: translateY(-4px);
	box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2) !important;
}

.opacity-80 {
	opacity: 0.8;
}
</style>
