import axios from '@/utils/axios'

const RECORDING_API = '/media/media/recording'

/**
 * 开始录制
 *
 * @param {Object} params
 * @param {string|number} params.roomId  - 房间ID
 * @param {string|number} params.userId  - 录制人ID
 * @param {string}        params.format  - 录制格式 'mp4' | 'webm'
 * @returns {Promise<{
 * 	 success: boolean,
 *   message: string,
 *   data: {
 *     roomId: string,        // 房间ID
 *     userId: string,        // 录制人ID
 *     exists: boolean,       // true=已有录制记录，false=新建
 *     fileUrl: string|null,  // 已有录制时返回URL，新建时为null
 *     fileSize: number|null, // 已有录制时返回文件大小
 *     duration: number|null, // 已有录制时返回时长（秒）
 *     startTime: string,     // 录制开始时间（ISO）
 *     endTime: string|null,  // 已有录制时返回结束时间
 *     format: string         // 录制格式
 *   }
 * }>}
 */
export function startRecording(params) {
	return axios.post(`${RECORDING_API}/start`, {
		roomId: params.roomId,
		userId: params.userId,
		format: params.format,
	})
}

/**
 * 停止录制
 *
 * @param {Object} params
 * @param {string|number} params.roomId    - 房间ID
 * @param {string|number} params.userId    - 录制人ID
 * @param {string}        params.fileUrl   - 上传完成的文件访问 URL
 * @param {number}        params.fileSize  - 文件大小（字节）
 * @param {number}        params.duration  - 录制时长（秒）
 * @param {string}        params.format    - 录制格式
 * @returns {Promise<{
 *   success: boolean,
 *   message: string,
 *   data: {
 *     fileUrl: string,
 *     fileSize: number,
 *     duration: number,
 *     endTime: string
 *   }
 * }>}
 */
export function stopRecording(params) {
	return axios.post(`${RECORDING_API}/stop`, {
		roomId: params.roomId,
		userId: params.userId,
		fileUrl: params.fileUrl,
		fileSize: params.fileSize,
		duration: params.duration,
		format: params.format,
	})
}
