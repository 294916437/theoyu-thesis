import { ref } from 'vue'

const notifications = ref([])
let notificationId = 0

export function useNotification() {
	/**
	 * 显示通知
	 */
	const show = (message, type = 'info', duration = 3000) => {
		const id = ++notificationId

		notifications.value.push({
			id,
			message,
			type,
			visible: true,
		})

		if (duration > 0) {
			setTimeout(() => {
				hide(id)
			}, duration)
		}

		return id
	}

	/**
	 * 隐藏通知
	 */
	const hide = id => {
		const index = notifications.value.findIndex(n => n.id === id)
		if (index !== -1) {
			notifications.value.splice(index, 1)
		}
	}

	/**
	 * 显示成功消息
	 */
	const showSuccess = (message, duration) => {
		return show(message, 'success', duration)
	}

	/**
	 * 显示错误消息
	 */
	const showError = (message, duration) => {
		return show(message, 'error', duration)
	}

	/**
	 * 显示警告消息
	 */
	const showWarning = (message, duration) => {
		return show(message, 'warning', duration)
	}

	/**
	 * 显示信息消息
	 */
	const showInfo = (message, duration) => {
		return show(message, 'info', duration)
	}

	/**
	 * 清空所有通知
	 */
	const clearAll = () => {
		notifications.value = []
	}

	return {
		notifications,
		show,
		hide,
		showSuccess,
		showError,
		showWarning,
		showInfo,
		clearAll,
	}
}
