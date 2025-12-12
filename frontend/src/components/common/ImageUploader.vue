<template>
	<div class="image-uploader">
		<slot
			name="activator"
			:props="{
				onClick: triggerUpload,
			}"
		/>

		<!-- 隐藏的文件输入 -->
		<input ref="fileInputRef" type="file" :accept="accept" style="display: none" @change="handleFileChange" />

		<!-- 上传进度对话框 -->
		<v-dialog v-model="uploading" persistent max-width="400">
			<v-card>
				<v-card-title>上传中...</v-card-title>
				<v-card-text>
					<v-progress-linear :model-value="progress" color="primary" height="20">
						<template #default>
							<strong>{{ Math.ceil(progress) }}%</strong>
						</template>
					</v-progress-linear>
				</v-card-text>
			</v-card>
		</v-dialog>
	</div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
	accept: {
		type: String,
		default: 'image/*',
	},
	maxSize: {
		type: Number,
		default: 5, // MB
	},
})

const emit = defineEmits(['upload', 'error'])

const fileInputRef = ref(null)
const uploading = ref(false)
const progress = ref(0)
const error = ref({
	show: false,
	message: '',
})

// 触发上传
const triggerUpload = () => {
	fileInputRef.value?.click()
}

// 验证文件
const validateFile = file => {
	// 检查文件类型
	if (!file.type.startsWith('image/')) {
		error.value = {
			show: true,
			message: '请上传图片文件',
		}
		emit('error', error.value)
		return false
	}

	// 检查文件大小
	const maxSizeBytes = props.maxSize * 1024 * 1024
	if (file.size > maxSizeBytes) {
		error.value = {
			show: true,
			message: `文件大小不能超过 ${props.maxSize}MB`,
		}
		emit('error', error.value)
		return false
	}

	return true
}

// 处理文件选择
const handleFileChange = async event => {
	const file = event.target.files?.[0]
	if (!file) return

	// 验证文件
	if (!validateFile(file)) {
		event.target.value = ''
		return
	}

	// 开始上传
	uploading.value = true
	progress.value = 0

	try {
		// TODO: 实际上传到服务器
		// 模拟上传进度
		const interval = setInterval(() => {
			progress.value += 10
			if (progress.value >= 100) {
				clearInterval(interval)
			}
		}, 200)

		await new Promise(resolve => setTimeout(resolve, 2000))

		emit('upload', file)
	} catch (err) {
		error.value = {
			show: true,
			message: '上传失败，请重试',
		}
		emit('error', err)
	} finally {
		uploading.value = false
		progress.value = 0
		event.target.value = '' // 重置input
	}
}

// 暴露方法供父组件调用
defineExpose({
	triggerUpload,
})
</script>

<style scoped>
.image-uploader {
	display: inline-block;
}
</style>
