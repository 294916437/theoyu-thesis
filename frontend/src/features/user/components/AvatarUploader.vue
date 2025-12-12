<template>
	<div class="avatar-uploader">
		<v-avatar :size="120" class="avatar-main" :class="{ 'avatar-editing': isEditing }">
			<v-img :src="avatar" cover>
				<template #placeholder>
					<v-row class="fill-height ma-0" align="center" justify="center">
						<v-progress-circular indeterminate color="primary" />
					</v-row>
				</template>
			</v-img>

			<!-- 编辑模式覆盖层 -->
			<v-overlay
				v-if="isEditing"
				:model-value="true"
				contained
				class="align-center justify-center avatar-overlay"
				@click="triggerUpload"
			>
				<div class="avatar-edit-content">
					<v-icon size="32" color="white">mdi-camera</v-icon>
					<div class="text-caption mt-1">更换头像</div>
				</div>
			</v-overlay>
		</v-avatar>

		<!-- 隐藏的文件上传输入 -->
		<ImageUploader ref="uploaderRef" accept="image/*" :max-size="1" @upload="handleUpload" />
	</div>
</template>

<script setup>
import { ref } from 'vue'
import ImageUploader from '@/components/common/ImageUploader.vue'

defineProps({
	avatar: {
		type: String,
		default: 'https://via.placeholder.com/150',
	},
	isEditing: {
		type: Boolean,
		default: false,
	},
})

const emit = defineEmits(['update:avatar'])

const uploaderRef = ref(null)

// 触发上传
const triggerUpload = () => {
	uploaderRef.value?.triggerUpload()
}

// 处理上传
const handleUpload = file => {
	// TODO: 上传到服务器并获取URL
	// const url = await uploadImage(file)
	const url = URL.createObjectURL(file)
	emit('update:avatar', url)
}
</script>

<style scoped>
.avatar-uploader {
	position: relative;
}

.avatar-main {
	border: 4px solid white;
	box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.avatar-editing {
	cursor: pointer;
}

.avatar-editing:hover {
	transform: scale(1.05);
	box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);
}

.avatar-overlay {
	background-color: rgba(0, 0, 0, 0.5);
	cursor: pointer;
	transition: background-color 0.3s;
}

.avatar-overlay:hover {
	background-color: rgba(0, 0, 0, 0.7);
}

.avatar-edit-content {
	display: flex;
	flex-direction: column;
	align-items: center;
	color: white;
	text-align: center;
}
</style>
