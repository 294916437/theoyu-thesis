import axios from '@/utils/axios'

// 接口前缀
const API_PREFIX = '/user/user'

/**
 * 获取用户个人资料
 */
export function getUserProfile(userId) {
	return axios.post(`${API_PREFIX}/profile`, { userId: userId })
}

/**
 * 更新用户资料
 */
export function updateUserProfile(formData) {
	return axios.post(`${API_PREFIX}/update`, formData, {
		headers: {
			'Content-Type': 'multipart/form-data',
		},
	})
}
/**
 * 获取用户的在线状态
 */
export function getUserOnlineStatus(userId) {
	return axios.post(`${API_PREFIX}/online/check`, { userId: userId })
}
/**
 * 设置用户的在线状态
 */
export function setUserOnlineStatus(userId) {
	console.log(`用户${userId}上线`)

	return axios.post(`${API_PREFIX}/online/set`, { userId: userId })
}
/**
 * 设置用户的离线状态
 */
export function setUserOfflineStatus(userId) {
	console.log(`用户${userId}离线`)
	return axios.post(`${API_PREFIX}/offline/set`, { userId: userId })
}
