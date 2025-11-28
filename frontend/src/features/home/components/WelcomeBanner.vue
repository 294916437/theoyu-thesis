<template>
	<v-card elevation="8" class="welcome-banner" :style="bannerStyle">
		<v-card-text class="pa-8">
			<v-row align="center">
				<v-col cols="12" md="8">
					<h1 class="text-h3 font-weight-bold text-white mb-4">{{ greeting }}, {{ userName }}!</h1>
					<p class="text-h6 text-white opacity-90">
						{{ currentDate }}
					</p>
					<p class="text-subtitle-1 text-white opacity-80 mt-2">开始或加入会议，与团队保持联系</p>
				</v-col>
				<v-col cols="12" md="4" class="text-center">
					<v-icon size="120" color="white" class="welcome-icon"> mdi-video-account </v-icon>
				</v-col>
			</v-row>
		</v-card-text>
	</v-card>
</template>

<script setup>
import { computed } from 'vue'
import { useDateFormat, useNow } from '@vueuse/core'

const props = defineProps({
	userName: {
		type: String,
		default: '用户',
	},
})

const now = useNow()
const currentDate = useDateFormat(now, 'YYYY年MM月DD日 dddd')

const greeting = computed(() => {
	const hour = new Date().getHours()
	if (hour < 12) return '早上好'
	if (hour < 18) return '下午好'
	return '晚上好'
})

const bannerStyle = computed(() => ({
	background: 'linear-gradient(120deg, #667eea 0%, #764ba2 100%)',
	borderRadius: '16px',
}))
</script>

<style scoped>
.welcome-banner {
	overflow: hidden;
}

.welcome-icon {
	animation: float 3s ease-in-out infinite;
}

@keyframes float {
	0%,
	100% {
		transform: translateY(0);
	}
	50% {
		transform: translateY(-10px);
	}
}

.opacity-90 {
	opacity: 0.9;
}

.opacity-80 {
	opacity: 0.8;
}
</style>
