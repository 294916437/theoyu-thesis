import axios from '@/utils/axios'

// 会议录制接口前缀
const API_PREFIX = '/media/record'

/**
 * 开始录制
 * @param {Object} params - 请求参数
 * @param {number} params.roomId - 房间 ID
 * @param {number} params.hostId - 主持人 ID
 * @param {string} params.format - 录制格式 (如 "mp4")
 * @returns {Promise<{
 *   roomId: number,
 *   hostId: number,
 *   status: number,       // 0: 录制中
 *   format: string,
 *   startTime: string,    // ISO 格式时间
 *   message: string
 * }>}
 */
export function startRecording(params) {
	return axios.post(`${API_PREFIX}/start`, {
		roomId: params.roomId,
		hostId: params.hostId,
		format: params.format,
	})
}

/**
 * 停止录制
 * @param {Object} params - 请求参数
 * @param {number} params.roomId - 房间 ID
 * @param {number} params.hostId - 主持人 ID
 * @returns {Promise<{
 *   roomId: number,
 *   hostId: number,
 *   status: number,       // 2: 已完成
 *   fileUrl: string,      // 录制文件下载地址
 *   fileSize: number,     // 文件大小（字节）
 *   duration: number,     // 录制时长（秒）
 *   endTime: string       // ISO 格式时间
 * }>}
 */
export function stopRecording(params) {
	return axios.post(`${API_PREFIX}/stop`, {
		roomId: params.roomId,
		hostId: params.hostId,
	})
}

/**
 * 获取录制状态
 * @param {number} roomId - 房间 ID
 * @param {number} hostId - 主持人 ID
 * @returns {Promise<{
 *   roomId: number,
 *   hostId: number,
 *   isRecording: boolean, // 是否正在录制
 *   status: number,       // 0: 录制中,2: 已完成,3: 录制失败
 *   startTime: string,    // 录制开始时间(ISO 格式)
 *   format: string,       // 录制格式
 *   durationSeconds: number,  // 当前录制时长（秒，实时计算）
 *   fileSizeBytes: number     // 当前文件大小（字节）
 * }>}
 */
export function getRecordingStatus(roomId, hostId) {
	return axios.get(`${API_PREFIX}/status`, {
		params: { roomId, hostId },
	})
}
