import { ref } from 'vue'

// 全局通知实例引用
const notificationInstance = ref(null)

// 注册通知实例
export const setNotificationInstance = instance => {
	notificationInstance.value = instance
}

// 全局通知对象
export const $notify = {
	show(options) {
		if (notificationInstance.value) {
			notificationInstance.value.show(options)
		} else {
			console.warn('通知组件未初始化')
		}
	},

	success(message, options = {}) {
		if (notificationInstance.value) {
			notificationInstance.value.success(message, options)
		}
	},

	error(message, options = {}) {
		if (notificationInstance.value) {
			notificationInstance.value.error(message, options)
		}
	},

	warning(message, options = {}) {
		if (notificationInstance.value) {
			notificationInstance.value.warning(message, options)
		}
	},

	info(message, options = {}) {
		if (notificationInstance.value) {
			notificationInstance.value.info(message, options)
		}
	},

	close() {
		if (notificationInstance.value) {
			notificationInstance.value.close()
		}
	},
}

// Vue 插件安装函数
export default {
	install(app) {
		// 将 $notify 挂载到全局属性
		app.config.globalProperties.$notify = $notify
	},
}
