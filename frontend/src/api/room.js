import axios from '@/utils/axios'

// 房间接口前缀
const API_PREFIX = '/media/room'

// 房间消息接口前缀

const MESSAGE_API_PREFIX = '/media/room/message'

// 房间参与者接口前缀
const PARTICIPANT_API_PREFIX = '/media/room/participants'

/**
 * 验证会议号
 */
export function validateMeetingNo(meetingNo) {
	return axios.post(`${API_PREFIX}/validate-no`, {
		meetingNo: meetingNo.trim(),
	})
}

/**
 * 获取会议信息
 */
export function fetchMeetingInfo(roomId) {
	return axios.get(`${API_PREFIX}/info/${roomId}`)
}

/**
 * 获取会议详情
 */
export function fetchMeetingDetail(roomId) {
	return axios.get(`${API_PREFIX}/detail/${roomId}`)
}

//

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
/**
 * 获取房间聊天历史
 */
export function fetchMessageHistory(roomId, page, size) {
	return axios.get(`${MESSAGE_API_PREFIX}/history?roomId=${roomId}&page=${page}&size=${size}`)
}
/**
 * 获取房间参与者列表
 */
export function fetchParticipantsList(roomId, status, page, size) {
	return axios.get(`${PARTICIPANT_API_PREFIX}?roomId=${roomId}&status=${status}&page=${page}&size=${size}`)
}
