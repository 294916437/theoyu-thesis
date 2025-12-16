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
export function updateUserProfile(data) {
	// 创建 FormData 对象用于文件上传
	const formData = new FormData()

	// 如果有头像文件，添加到表单
	if (data.avatar && data.avatar instanceof File) {
		formData.append('avatar', data.avatar)
	}

	// 添加其他字段
	if (data.nickname) formData.append('nickname', data.nickname)
	if (data.userId) formData.append('userId', data.userId)
	if (data.sex !== undefined && data.sex !== null) formData.append('sex', data.sex)
	if (data.birthday) formData.append('birthday', data.birthday)
	if (data.introduction) formData.append('introduction', data.introduction)

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
