<template>
	<v-snackbar
		v-model="isVisible"
		:color="notificationConfig.type"
		:timeout="notificationConfig.timeout"
		:location="notificationConfig.location"
		:vertical="notificationConfig.vertical"
		:multi-line="notificationConfig.multiLine"
		@update:model-value="onClose"
	>
		<div class="d-flex align-center">
			<v-icon v-if="notificationConfig.icon" class="mr-3">
				{{ notificationConfig.icon }}
			</v-icon>
			<div class="flex-grow-1">
				<div v-if="notificationConfig.title" class="font-weight-bold mb-1">
					{{ notificationConfig.title }}
				</div>
				<div>{{ notificationConfig.message }}</div>
			</div>
		</div>

		<template #actions>
			<v-btn v-if="notificationConfig.action" variant="text" size="small" @click="handleAction">
				{{ notificationConfig.action.text }}
			</v-btn>
			<v-btn
				v-if="notificationConfig.closable"
				icon="mdi-close"
				variant="text"
				size="small"
				@click="close"
			></v-btn>
		</template>
	</v-snackbar>
</template>

<script setup>
import { ref, computed } from 'vue'

const queue = ref([])
const currentNotification = ref(null)
const isVisible = ref(false)
const closeTimer = ref(null) // 用于清除自动关闭的定时器

// 默认配置
const defaultConfig = {
	type: 'info',
	timeout: 3000,
	location: 'bottom right',
	vertical: false,
	multiLine: false,
	closable: true,
	icon: null,
	title: null,
	action: null,
	message: '',
	immediate: true, // 新增：是否立即显示，默认 true
	clearQueue: true, // 新增：是否清空队列，默认 true
	priority: 0, // 新增：优先级，数字越大优先级越高
}

// 使用计算属性提供安全的默认值
const notificationConfig = computed(() => {
	return currentNotification.value || defaultConfig
})

// 类型图标映射
const typeIconMap = {
	success: 'mdi-check-circle',
	error: 'mdi-alert-circle',
	warning: 'mdi-alert',
	info: 'mdi-information',
}

// 显示通知（核心改进）
const show = options => {
	const notification = {
		...defaultConfig,
		...options,
		id: Date.now() + Math.random(),
	}

	// 如果没有指定图标，使用类型默认图标
	if (!notification.icon && typeIconMap[notification.type]) {
		notification.icon = typeIconMap[notification.type]
	}

	// 清空队列（如果配置了 clearQueue）
	if (notification.clearQueue) {
		queue.value = []
	}

	// 立即显示模式
	if (notification.immediate) {
		// 如果当前有通知正在显示
		if (isVisible.value && currentNotification.value) {
			// 检查优先级
			const currentPriority = currentNotification.value.priority || 0
			const newPriority = notification.priority || 0

			// 新通知优先级更高或相同，立即替换
			if (newPriority >= currentPriority) {
				// 清除当前的自动关闭定时器
				clearCloseTimer()

				// 立即替换当前通知
				currentNotification.value = notification
				isVisible.value = true
			} else {
				// 新通知优先级较低，加入队列
				queue.value.push(notification)
			}
		} else {
			// 当前没有通知显示，直接显示
			currentNotification.value = notification
			isVisible.value = true
		}
	} else {
		// 队列模式：添加到队列末尾
		queue.value.push(notification)

		// 如果当前没有通知显示，显示下一个
		if (!isVisible.value) {
			showNext()
		}
	}
}

// 显示下一个通知
const showNext = () => {
	if (queue.value.length === 0) {
		isVisible.value = false
		currentNotification.value = null
		clearCloseTimer()
		return
	}

	currentNotification.value = queue.value.shift()
	isVisible.value = true
}

// 关闭当前通知
const close = () => {
	isVisible.value = false
	clearCloseTimer()
}

// 清除自动关闭定时器
const clearCloseTimer = () => {
	if (closeTimer.value) {
		clearTimeout(closeTimer.value)
		closeTimer.value = null
	}
}

// 关闭回调
const onClose = value => {
	if (!value) {
		// 延迟显示下一个通知，等待关闭动画完成
		closeTimer.value = setTimeout(() => {
			showNext()
		}, 300)
	}
}

// 处理操作按钮点击
const handleAction = () => {
	if (currentNotification.value?.action?.callback) {
		currentNotification.value.action.callback()
	}
	close()
}

// 清空所有队列中的通知
const clearAll = () => {
	queue.value = []
	close()
}

// 快捷方法
const success = (message, options = {}) => {
	show({
		...options,
		message,
		type: 'success',
		immediate: options.immediate !== undefined ? options.immediate : true,
		clearQueue: options.clearQueue !== undefined ? options.clearQueue : true,
	})
}

const error = (message, options = {}) => {
	show({
		...options,
		message,
		type: 'error',
		timeout: 5000,
		immediate: options.immediate !== undefined ? options.immediate : true,
		clearQueue: options.clearQueue !== undefined ? options.clearQueue : true,
		priority: 10, // 错误通知默认高优先级
	})
}

const warning = (message, options = {}) => {
	show({
		...options,
		message,
		type: 'warning',
		timeout: 4000,
		immediate: options.immediate !== undefined ? options.immediate : true,
		clearQueue: options.clearQueue !== undefined ? options.clearQueue : true,
		priority: 5, // 警告通知中等优先级
	})
}

const info = (message, options = {}) => {
	show({
		...options,
		message,
		type: 'info',
		immediate: options.immediate !== undefined ? options.immediate : true,
		clearQueue: options.clearQueue !== undefined ? options.clearQueue : true,
	})
}

// 暴露方法供外部调用
defineExpose({
	show,
	success,
	error,
	warning,
	info,
	close,
	clearAll,
})
</script>

<style scoped>
.v-snackbar :deep(.v-snackbar__wrapper) {
	min-width: 300px;
	max-width: 600px;
}
</style>
