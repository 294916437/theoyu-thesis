<template>
	<v-overlay :model-value="visible" class="loading-overlay align-center justify-center" :persistent="persistent" scrim="overlay" opacity="1">
		<div class="loading-content">
			<v-progress-circular :indeterminate="!progress" :model-value="progress" :size="80" :width="6" color="primary">
				<span v-if="progress" class="text-h6">{{ progress }}%</span>
			</v-progress-circular>

			<div v-if="message" class="text-h6 text-white mt-6">
				{{ message }}
			</div>

			<div v-if="description" class="text-body-2 text-white mt-2 text-center">
				{{ description }}
			</div>

			<v-btn v-if="cancellable" variant="outlined" color="white" class="mt-6" @click="emit('cancel')"> 取消 </v-btn>
		</div>
	</v-overlay>
</template>

<script setup>
defineProps({
	visible: {
		type: Boolean,
		default: false,
	},
	message: {
		type: String,
		default: '',
	},
	description: {
		type: String,
		default: '',
	},
	progress: {
		type: Number,
		default: 0,
	},
	persistent: {
		type: Boolean,
		default: true,
	},
	cancellable: {
		type: Boolean,
		default: false,
	},
})

const emit = defineEmits(['cancel'])
</script>

<style scoped>
.loading-overlay {
	z-index: 9999;
}

.loading-content {
	display: flex;
	flex-direction: column;
	align-items: center;
}
</style>
