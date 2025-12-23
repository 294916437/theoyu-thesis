<template>
	<v-overlay :model-value="true" class="incoming-call-overlay" persistent :scrim="true" :z-index="9998">
		<v-card class="incoming-call-card" elevation="24" rounded="xl" max-width="400" width="100%">
			<!-- 呼叫者信息 -->
			<v-card-text class="pa-8 text-center">
				<!-- 头像 -->
				<v-avatar color="primary" size="120" class="mb-6 avatar-ring">
					<v-icon size="64" color="white"> mdi-account </v-icon>
				</v-avatar>

				<!-- 呼叫者名称 -->
				<div class="text-h5 font-weight-bold mb-2 text-high-emphasis">
					{{ callerName }}
				</div>

				<!-- 呼叫类型 -->
				<v-chip color="primary" variant="tonal" size="default" class="mb-4">
					<v-icon start size="small"> mdi-video </v-icon>
					视频通话
				</v-chip>

				<!-- 状态提示 -->
				<div class="text-body-2 text-medium-emphasis mb-6">
					<v-icon size="small" class="mr-1 ringing-icon"> mdi-phone-ring </v-icon>
					正在呼叫中...
				</div>
			</v-card-text>

			<!-- 操作按钮 -->
			<v-card-actions class="pa-6 pt-0 d-flex justify-center ga-4">
				<!-- 拒绝按钮 -->
				<v-btn
					color="error"
					variant="flat"
					size="x-large"
					icon
					elevation="4"
					class="action-btn reject-btn"
					@click="$emit('reject')"
				>
					<v-icon size="32"> mdi-phone-hangup </v-icon>
					<v-tooltip activator="parent" location="bottom" text="拒绝" />
				</v-btn>

				<!-- 接受按钮 -->
				<v-btn
					color="success"
					variant="flat"
					size="x-large"
					icon
					elevation="4"
					class="action-btn accept-btn"
					@click="$emit('accept')"
				>
					<v-icon size="32"> mdi-video </v-icon>
					<v-tooltip activator="parent" location="bottom" text="接受" />
				</v-btn>
			</v-card-actions>

			<!-- 快捷操作 -->
			<v-divider class="mx-6 mb-4" />

			<v-card-actions class="pa-6 pt-0 d-flex justify-center ga-2">
				<v-btn variant="text" size="small" color="primary" prepend-icon="mdi-message-text"> 发送消息 </v-btn>
			</v-card-actions>
		</v-card>
	</v-overlay>
</template>

<script setup>
defineProps({
	callerName: {
		type: String,
		required: true,
	},
})

defineEmits(['accept', 'reject'])
</script>

<style scoped>
.incoming-call-overlay {
	display: flex;
	align-items: center;
	justify-content: center;
	backdrop-filter: blur(8px);
	animation: fadeIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes fadeIn {
	from {
		opacity: 0;
	}
	to {
		opacity: 1;
	}
}

.incoming-call-card {
	animation: slideUp 0.4s cubic-bezier(0.4, 0, 0.2, 1);
	background: rgb(var(--v-theme-surface));
	box-shadow: 0 24px 48px rgba(0, 0, 0, 0.24) !important;
}

@keyframes slideUp {
	from {
		transform: translateY(50px) scale(0.95);
		opacity: 0;
	}
	to {
		transform: translateY(0) scale(1);
		opacity: 1;
	}
}

/* 头像呼吸动画 */
.avatar-ring {
	position: relative;
	animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.avatar-ring::before {
	content: '';
	position: absolute;
	inset: -8px;
	border-radius: 50%;
	border: 3px solid rgb(var(--v-theme-primary));
	opacity: 0.3;
	animation: ripple 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.avatar-ring::after {
	content: '';
	position: absolute;
	inset: -16px;
	border-radius: 50%;
	border: 2px solid rgb(var(--v-theme-primary));
	opacity: 0.15;
	animation: ripple 2s cubic-bezier(0.4, 0, 0.6, 1) infinite 0.5s;
}

@keyframes pulse {
	0%,
	100% {
		transform: scale(1);
	}
	50% {
		transform: scale(1.05);
	}
}

@keyframes ripple {
	0% {
		transform: scale(1);
		opacity: 0.3;
	}
	100% {
		transform: scale(1.2);
		opacity: 0;
	}
}

/* 铃声图标动画 */
.ringing-icon {
	animation: ring 1s ease-in-out infinite;
	transform-origin: center top;
}

@keyframes ring {
	0%,
	100% {
		transform: rotate(0deg);
	}
	10%,
	30% {
		transform: rotate(-15deg);
	}
	20%,
	40% {
		transform: rotate(15deg);
	}
	50% {
		transform: rotate(0deg);
	}
}

/* 操作按钮 */
.action-btn {
	width: 80px;
	height: 80px;
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.action-btn:hover {
	transform: scale(1.1);
}

.action-btn:active {
	transform: scale(0.95);
}

/* 拒绝按钮特效 */
.reject-btn {
	box-shadow: 0 8px 16px rgba(var(--v-theme-error), 0.4) !important;
}

.reject-btn:hover {
	box-shadow: 0 12px 24px rgba(var(--v-theme-error), 0.5) !important;
}

/* 接受按钮特效 */
.accept-btn {
	box-shadow: 0 8px 16px rgba(var(--v-theme-success), 0.4) !important;
}

.accept-btn:hover {
	box-shadow: 0 12px 24px rgba(var(--v-theme-success), 0.5) !important;
}

/* 文字强调色 */
.text-high-emphasis {
	color: rgb(var(--v-theme-on-surface));
	opacity: 0.87;
}

.text-medium-emphasis {
	color: rgb(var(--v-theme-on-surface));
	opacity: 0.6;
}

/* 移动端适配 */
@media (max-width: 600px) {
	.incoming-call-card {
		margin: 16px;
		max-width: calc(100vw - 32px);
	}

	.action-btn {
		width: 72px;
		height: 72px;
	}
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
	.incoming-call-overlay {
		backdrop-filter: blur(12px);
	}

	.incoming-call-card {
		box-shadow: 0 24px 48px rgba(0, 0, 0, 0.6) !important;
	}
}
</style>
