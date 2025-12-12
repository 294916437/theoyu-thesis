<template>
	<div class="profile-header">
		<!-- 背景图 -->
		<div class="background-wrapper">
			<v-img :src="backgroundImg" cover height="300" class="background-image">
				<div class="background-content">
					<!-- 背景图编辑按钮 -->
					<transition name="fade">
						<div v-if="isEditing" class="background-edit-btn">
							<ImageUploader accept="image/*" :max-size="5" @upload="handleBackgroundUpload">
								<template #activator="{ props: uploaderProps }">
									<v-btn
										v-bind="uploaderProps"
										icon="mdi-camera"
										color="white"
										size="large"
										elevation="4"
										class="edit-btn"
									/>
								</template>
							</ImageUploader>
						</div>
					</transition>
				</div>

				<!-- 加载占位符 -->
				<template #placeholder>
					<v-row class="fill-height ma-0" align="center" justify="center">
						<v-progress-circular indeterminate color="primary" />
					</v-row>
				</template>
			</v-img>
		</div>

		<!-- 头像和昵称 -->
		<v-container class="avatar-container">
			<div class="avatar-wrapper">
				<AvatarUploader
					:avatar="avatar"
					:is-editing="isEditing"
					@update:avatar="$emit('update:avatar', $event)"
				/>
				<div class="nickname-wrapper">
					<h2 class="text-h4 font-weight-bold text-white nickname">
						{{ nickname }}
					</h2>
				</div>
			</div>
		</v-container>
	</div>
</template>

<script setup>
import AvatarUploader from './AvatarUploader.vue'
import ImageUploader from '@/components/common/ImageUploader.vue'

defineProps({
	backgroundImg: {
		type: String,
		default: 'https://via.placeholder.com/1920x400',
	},
	avatar: {
		type: String,
		default: 'https://via.placeholder.com/150',
	},
	nickname: {
		type: String,
		default: '用户昵称',
	},
	isEditing: {
		type: Boolean,
		default: false,
	},
})

const emit = defineEmits(['update:background-img', 'update:avatar'])

// 处理背景图上传
const handleBackgroundUpload = file => {
	// TODO: 上传到服务器并获取URL
	// const url = await uploadImage(file)
	const url = URL.createObjectURL(file)
	emit('update:background-img', url)
}
</script>

<style scoped>
.profile-header {
	position: relative;
	width: 100%;
	max-width: 960px;
	margin: 0 auto;
}

.background-wrapper {
	position: relative;
	width: 100%;
}

.background-image {
	width: 100%;
	position: relative;
}

/* 背景图内容容器 - 覆盖整个背景图 */
.background-content {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	pointer-events: none;
	z-index: 1;
}

/* 背景图编辑按钮容器 */
.background-edit-btn {
	position: absolute;
	top: 16px;
	right: 16px;
	z-index: 100;
	pointer-events: auto;
}

/* 编辑按钮样式增强 */
.edit-btn {
	backdrop-filter: blur(8px);
	background-color: rgba(255, 255, 255, 0.95) !important;
	transition: all 0.3s ease;
}

.edit-btn:hover {
	transform: scale(1.1);
	background-color: rgba(255, 255, 255, 1) !important;
}

/* 渐入渐出动画 */
.fade-enter-active,
.fade-leave-active {
	transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
	opacity: 0;
}

/* 头像容器 */
.avatar-container {
	position: relative;
	margin-top: -80px;
	z-index: 2;
}

.avatar-wrapper {
	display: flex;
	align-items: flex-end;
	gap: 24px;
}

.nickname-wrapper {
	padding-bottom: 12px;
}

.nickname {
	text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}
</style>
