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
	<div class="conversation-skeleton-container">
		<div
			v-for="i in count"
			:key="i"
			class="conversation-skeleton-item"
			:style="{ animationDelay: `${i * 50}ms` }"
		>
			<!-- 头像骨架 -->
			<div class="skeleton-avatar"></div>

			<!-- 内容骨架 -->
			<div class="skeleton-content">
				<div class="skeleton-header">
					<div class="skeleton-name"></div>
					<div class="skeleton-time"></div>
				</div>
				<div class="skeleton-message" :style="{ width: `${60 + (i % 3) * 15}%` }"></div>
			</div>
		</div>
	</div>
</template>

<style scoped>
.conversation-skeleton-container {
	padding: 0;
}

.conversation-skeleton-item {
	display: flex;
	gap: 12px;
	padding: 12px 16px;
	border-bottom: 1px solid #f0f0f0;
	animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
	from {
		opacity: 0;
		transform: translateY(-10px);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}

.skeleton-avatar {
	width: 48px;
	height: 48px;
	border-radius: 50%;
	background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
	background-size: 200% 100%;
	animation: shimmer 1.5s infinite;
	flex-shrink: 0;
}

.skeleton-content {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 8px;
}

.skeleton-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	gap: 12px;
}

.skeleton-name {
	height: 16px;
	width: 120px;
	border-radius: 4px;
	background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
	background-size: 200% 100%;
	animation: shimmer 1.5s infinite;
}

.skeleton-time {
	height: 12px;
	width: 60px;
	border-radius: 4px;
	background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
	background-size: 200% 100%;
	animation: shimmer 1.5s infinite;
	flex-shrink: 0;
}

.skeleton-message {
	height: 14px;
	border-radius: 4px;
	background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
	background-size: 200% 100%;
	animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
	0% {
		background-position: 200% 0;
	}
	100% {
		background-position: -200% 0;
	}
}

/* 响应式优化 */
@media (max-width: 768px) {
	.conversation-skeleton-item {
		padding: 10px 12px;
	}

	.skeleton-avatar {
		width: 44px;
		height: 44px;
	}

	.skeleton-name {
		width: 100px;
	}
}
</style>
