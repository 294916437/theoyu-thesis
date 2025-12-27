import axios from '@/utils/axios'

// 接口前缀
const API_PREFIX = '/media/media/room'

/**
 * 验证会议号
 */
export function validateMeetingNo(meetingNo) {
	return axios.post(`${API_PREFIX}/validate-no`, {
		meetingNo: meetingNo.trim(),
	})
}

/**
 * 获取会议详情
 */
export function fetchMeetingDetail(roomId) {
	return axios.get(`${API_PREFIX}/${roomId}`)
}

/**
 * 创建会议
 */
export function createMeeting(meetingData) {
	return axios.post(`${API_PREFIX}/create`, meetingData)
}

/**
 * 加入会议 (预验证)
 */
export function joinMeeting(joinData) {
	return axios.post(`${API_PREFIX}/join`, joinData)
}

/**
 * 获取即将开始的会议
 */
export function fetchUpcomingMeetings(page = 1, size = 5) {
	return axios.get(`${API_PREFIX}/upcoming`, {
		params: {
			page,
			size,
		},
	})
}

/**
 * 获取最近的会议
 */
export function fetchRecentMeetings(page = 1, size = 10) {
	return axios.get(`${API_PREFIX}/recent`, {
		params: {
			page,
			size,
		},
	})
}

/**
 * 更新会议
 */
export function updateMeeting(roomId, meetingData) {
	return axios.put(`${API_PREFIX}/${roomId}`, meetingData)
}

/**
 * 删除会议
 */
export function deleteMeeting(roomId) {
	return axios.delete(`${API_PREFIX}/${roomId}`)
}
