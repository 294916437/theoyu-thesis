<template>
	<!--
        单一全屏 Overlay，通过 phase 控制内容：
        - consent  : 媒体权限询问
        - loading  : 初始化加载中
        - hidden   : 隐藏（进入会议完成）
    -->
	<v-overlay :model-value="phase !== 'hidden'" class="meeting-entry-overlay align-center justify-center" persistent scrim="#000" :opacity="1" :transition="false">
		<!-- ========== Consent 阶段 ========== -->
		<transition name="phase-fade" mode="out-in">
			<div v-if="phase === 'consent'" key="consent" class="phase-content">
				<v-card class="consent-card text-center pa-6" max-width="480" elevation="0">
					<div class="d-flex justify-center mb-4">
						<v-img src="/audioorvideo.svg" width="220" height="160" contain></v-img>
					</div>

					<v-card-title class="text-h6 font-weight-medium px-4 text-wrap"> 你想让他人在会议中看到你并听到你的声音吗？ </v-card-title>

					<v-card-text class="text-body-2 text-medium-emphasis pb-4"> 你仍可以在会议期间随时关闭麦克风和摄像头。 </v-card-text>

					<v-alert v-if="permissionDenied" type="warning" variant="tonal" density="compact" icon="mdi-alert" class="mx-4 mb-4 text-left">
						权限被拒绝，请选择下方「在不使用麦克风和摄像头的情况下继续」
					</v-alert>

					<v-card-actions class="flex-column ga-3 px-4 pb-4">
						<v-btn
							color="primary"
							variant="flat"
							block
							size="large"
							rounded="pill"
							:loading="requestingPermission"
							prepend-icon="mdi-monitor-share"
							@click="requestAndJoin"
						>
							使用麦克风和摄像头
						</v-btn>

						<v-btn variant="text" color="primary" block size="default" @click="joinAsListener"> 在不使用麦克风和摄像头的情况下继续 </v-btn>
					</v-card-actions>
				</v-card>
			</div>

			<!-- ========== Loading 阶段 ========== -->
			<div v-else-if="phase === 'loading'" key="loading" class="phase-content">
				<v-progress-circular :indeterminate="!loadingProgress" :model-value="loadingProgress" :size="80" :width="6" color="primary">
					<span v-if="loadingProgress" class="text-h6">{{ loadingProgress }}%</span>
				</v-progress-circular>

				<div class="text-h6 text-white mt-6">{{ loadingMessage }}</div>

				<!-- 离开场景无进度时的补充提示 -->
				<div v-if="!loadingProgress" class="text-body-2 text-white mt-2" style="opacity: 0.6">请稍候...</div>
			</div>
		</transition>
	</v-overlay>
</template>

<script setup>
import { ref } from 'vue'
import { $notify } from '@/plugins/notification'

const props = defineProps({
	phase: {
		type: String,
		default: 'consent', // 'consent' | 'loading' | 'hidden'
	},
	loadingMessage: {
		type: String,
		default: '正在初始化...',
	},
	loadingProgress: {
		type: Number,
		default: 0,
	},
})

const emit = defineEmits(['confirm'])

const permissionDenied = ref(false)
const requestingPermission = ref(false)

const requestAndJoin = async () => {
	requestingPermission.value = true
	permissionDenied.value = false
	try {
		const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
		emit('confirm', { withMedia: true, stream })
	} catch (err) {
		if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
			permissionDenied.value = true
			$notify.warning('未获得摄像头/麦克风权限，请选择下方链接继续')
		} else {
			$notify.error(`设备访问失败: ${err.message}`)
		}
	} finally {
		requestingPermission.value = false
	}
}

const joinAsListener = () => {
	emit('confirm', { withMedia: false, stream: null })
}
</script>

<style scoped>
.meeting-entry-overlay {
	z-index: 9999;
}

.phase-content {
	display: flex;
	flex-direction: column;
	align-items: center;
}

.consent-card {
	border-radius: 16px !important;
	background: rgb(var(--v-theme-surface)) !important;
}

.text-wrap {
	white-space: normal;
	line-height: 1.5;
}

/* consent → loading 切换的淡入淡出 */
.phase-fade-enter-active,
.phase-fade-leave-active {
	transition: opacity 0.3s ease;
}

.phase-fade-enter-from,
.phase-fade-leave-to {
	opacity: 0;
}
</style>
