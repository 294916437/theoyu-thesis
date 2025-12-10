<template>
	<div class="incoming-call-overlay">
		<div class="incoming-call-card">
			<div class="caller-info">
				<div class="avatar-placeholder">
					<svg class="w-12 h-12" fill="currentColor" viewBox="0 0 24 24">
						<path
							d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
						/>
					</svg>
				</div>
				<h3 class="caller-name">{{ callerName }}</h3>
				<p class="call-type">视频通话</p>
			</div>

			<div class="call-actions">
				<button class="action-btn reject-btn" @click="$emit('reject')">
					<svg class="w-10 h-10" fill="currentColor" viewBox="0 0 24 24">
						<path
							d="M12 9c-1.6 0-3.15.25-4.6.72v3.1c0 .39-.23.74-.56.9-.98.49-1.87 1.12-2.66 1.85-.18.18-.43.28-.7.28-.28 0-.53-.11-.71-.29L.29 13.08c-.18-.17-.29-.42-.29-.7 0-.28.11-.53.29-.71C3.34 8.78 7.46 7 12 7s8.66 1.78 11.71 4.67c.18.18.29.43.29.71 0 .28-.11.53-.29.71l-2.48 2.48c-.18.18-.43.29-.71.29-.27 0-.52-.11-.7-.28-.79-.74-1.68-1.36-2.66-1.85-.33-.16-.56-.5-.56-.9v-3.1C15.15 9.25 13.6 9 12 9z"
						/>
					</svg>
					拒绝
				</button>

				<button class="action-btn accept-btn" @click="$emit('accept')">
					<svg class="w-10 h-10" fill="currentColor" viewBox="0 0 24 24">
						<path
							d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z"
						/>
					</svg>
					接受
				</button>
			</div>
		</div>
	</div>
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
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.7);
	backdrop-filter: blur(5px);
	z-index: 9998;
	display: flex;
	align-items: center;
	justify-content: center;
	animation: fadeIn 0.3s ease-in-out;
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
	background: white;
	border-radius: 16px;
	padding: 2rem;
	box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
	min-width: 300px;
	animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
	from {
		transform: translateY(50px);
		opacity: 0;
	}
	to {
		transform: translateY(0);
		opacity: 1;
	}
}

.caller-info {
	text-align: center;
	margin-bottom: 2rem;
}

.avatar-placeholder {
	width: 80px;
	height: 80px;
	border-radius: 50%;
	background: linear-gradient(135deg, rgb(59 130 246), rgb(147 51 234));
	color: white;
	display: flex;
	align-items: center;
	justify-content: center;
	margin: 0 auto 1rem;
}

.caller-name {
	font-size: 1.5rem;
	font-weight: 600;
	margin-bottom: 0.5rem;
	color: #1f2937;
}

.call-type {
	color: #6b7280;
	font-size: 0.875rem;
}

.call-actions {
	display: flex;
	gap: 1rem;
	justify-content: center;
}

.action-btn {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 0.5rem;
	padding: 1rem;
	border-radius: 50%;
	cursor: pointer;
	transition: all 0.2s;
	font-size: 0.875rem;
	font-weight: 500;
	min-width: 100px;
}

.reject-btn {
	background: rgb(239 68 68);
	color: white;
}

.reject-btn:hover {
	background: rgb(220 38 38);
	transform: scale(1.05);
}

.accept-btn {
	background: rgb(34 197 94);
	color: white;
}

.accept-btn:hover {
	background: rgb(22 163 74);
	transform: scale(1.05);
}
</style>
