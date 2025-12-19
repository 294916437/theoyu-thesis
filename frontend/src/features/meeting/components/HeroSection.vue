<template>
	<div class="hero-section">
		<v-container class="py-12">
			<v-row align="center" justify="center">
				<v-col cols="12" lg="10" xl="8">
					<!-- 欢迎横幅 -->
					<div class="welcome-content text-center mb-8">
						<v-icon size="80" color="white" class="welcome-icon mb-4"> mdi-video-account </v-icon>
						<h1 class="text-h3 text-md-h2 font-weight-bold text-white mb-3">
							{{ greeting }}, {{ userName }}!
						</h1>
						<p class="text-h6 text-white text-opacity-90 mb-2">
							{{ currentDate }}
						</p>
						<p class="text-subtitle-1 text-white text-opacity-80">开始或加入会议,与团队保持联系</p>
					</div>

					<!-- 快速操作 -->
					<v-row justify="center">
						<v-col cols="12" md="10" lg="8">
							<v-card elevation="12" rounded="xl" class="action-card pa-6">
								<v-row>
									<!-- 创建会议 -->
									<v-col cols="12" sm="6">
										<v-card
											elevation="0"
											class="action-item pa-6 text-center"
											color="primary"
											rounded="lg"
											@click="emit('create-meeting')"
										>
											<v-icon size="56" color="white" class="mb-4"> mdi-video-plus </v-icon>
											<h3 class="text-h6 text-white font-weight-bold mb-2">创建会议</h3>
											<p class="text-body-2 text-white text-opacity-80">立即开始新的视频会议</p>
										</v-card>
									</v-col>

									<!-- 加入会议 -->
									<v-col cols="12" sm="6">
										<v-card
											elevation="0"
											class="action-item pa-6"
											color="surface-variant"
											rounded="lg"
										>
											<div class="text-center mb-4">
												<v-icon size="56" color="primary" class="mb-4"> mdi-login </v-icon>
												<h3 class="text-h6 font-weight-bold mb-2">加入会议</h3>
											</div>

											<v-text-field
												v-model="meetingId"
												label="输入会议ID"
												variant="outlined"
												density="comfortable"
												color="primary"
												hide-details
												clearable
												@keyup.enter="handleJoin"
											>
												<template #prepend-inner>
													<v-icon size="20">mdi-pound</v-icon>
												</template>
												<template #append-inner>
													<v-btn
														icon
														size="small"
														color="primary"
														:disabled="!meetingId"
														@click="handleJoin"
													>
														<v-icon>mdi-arrow-right</v-icon>
													</v-btn>
												</template>
											</v-text-field>
										</v-card>
									</v-col>
								</v-row>
							</v-card>
						</v-col>
					</v-row>
				</v-col>
			</v-row>
		</v-container>

		<!-- 装饰性背景 -->
		<div class="hero-bg-decoration"></div>
	</div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useDateFormat, useNow } from '@vueuse/core'

const props = defineProps({
	userName: {
		type: String,
		default: '用户',
	},
})

const emit = defineEmits(['join-meeting', 'create-meeting'])

const now = useNow()
const meetingId = ref('')

// 当前日期
const currentDate = useDateFormat(now, 'YYYY年MM月DD日 dddd', { locales: 'zh-CN' })

// 问候语
const greeting = computed(() => {
	const hour = now.value.getHours()
	if (hour < 12) return '早上好'
	if (hour < 18) return '下午好'
	return '晚上好'
})

// 加入会议
const handleJoin = () => {
	if (meetingId.value && meetingId.value.trim()) {
		emit('join-meeting', meetingId.value.trim())
		meetingId.value = ''
	}
}
</script>

<style scoped>
.hero-section {
	position: relative;
	background: linear-gradient(135deg, rgb(var(--v-theme-primary)) 0%, rgb(var(--v-theme-secondary)) 100%);
	overflow: hidden;
}

.hero-bg-decoration {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background-image:
		radial-gradient(circle at 20% 50%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
		radial-gradient(circle at 80% 80%, rgba(255, 255, 255, 0.1) 0%, transparent 50%);
	pointer-events: none;
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
		transform: translateY(-12px);
	}
}

.action-card {
	background: rgb(var(--v-theme-surface));
	backdrop-filter: blur(10px);
}

.action-item {
	cursor: pointer;
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	height: 100%;
}

.action-item:hover {
	transform: translateY(-8px);
	box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2) !important;
}

.action-item:active {
	transform: translateY(-4px);
}

/* 响应式调整 */
@media (max-width: 600px) {
	.hero-section {
		padding-top: 2rem;
		padding-bottom: 2rem;
	}

	.action-item {
		margin-bottom: 1rem;
	}
}
</style>
