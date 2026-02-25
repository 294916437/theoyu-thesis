import axios from '@/utils/axios'

// ==================== 旧接口（gRPC方案，已废弃，保留备用） ====================
const LEGACY_API_PREFIX = '/media/media/record'

/** @deprecated 旧方案，已由 MediaRecorder 方案替代 */
export function startRecording(params) {
	return axios.post(`${LEGACY_API_PREFIX}/start`, params)
}
/** @deprecated 旧方案，已由 MediaRecorder 方案替代 */
export function stopRecording(params) {
	return axios.post(`${LEGACY_API_PREFIX}/stop`, params)
}
/** @deprecated 旧方案，已由 MediaRecorder 方案替代 */
export function getRecordingStatus(roomId, hostId) {
	return axios.get(`${LEGACY_API_PREFIX}/status`, { params: { roomId, hostId } })
}

// ==================== 新接口：MediaRecorder 分片上传方案 ====================
const RECORDING_API = '/media/recording/upload'

/**
 * 初始化 MinIO Multipart Upload
 * 后端创建上传任务，返回 uploadId 供后续分片使用
 *
 * @param {Object} params
 * @param {string|number} params.roomId - 房间ID
 * @param {string} params.key          - MinIO 存储路径，如 recordings/123/123-456-1700000000.webm
 * @returns {Promise<{
 *   code: number,
 *   data: {
 *     uploadId: string,  // MinIO Multipart Upload ID
 *     key: string        // 确认后的存储路径
 *   }
 * }>}
 */
export function initRecordingUpload(params) {
	return axios.post(`${RECORDING_API}/init`, {
		roomId: params.roomId,
		key: params.key,
	})
}

/**
 * 上传单个录制分片
 * 每次 MediaRecorder.ondataavailable 回调产生一个分片时调用
 *
 * @param {Object} params
 * @param {Blob}   params.chunk       - 分片数据（MediaRecorder 产生的 Blob）
 * @param {string} params.uploadId    - initRecordingUpload 返回的 uploadId
 * @param {string} params.key         - MinIO 存储路径
 * @param {number} params.partNumber  - 分片序号（从 1 开始，连续递增）
 * @param {Function} [params.onUploadProgress] - 上传进度回调 (progressEvent) => void
 * @returns {Promise<{
 *   code: number,
 *   data: {
 *     partNumber: number, // 已上传的分片序号
 *     etag: string        // MinIO 返回的 ETag，complete 时需要回传
 *   }
 * }>}
 */
export function uploadRecordingChunk(params) {
	const formData = new FormData()
	formData.append('chunk', params.chunk, `part-${params.partNumber}.webm`)
	formData.append('uploadId', params.uploadId)
	formData.append('key', params.key)
	formData.append('partNumber', String(params.partNumber))

	return axios.post(`${RECORDING_API}/chunk`, formData, {
		headers: { 'Content-Type': 'multipart/form-data' },
		timeout: 60000, // 分片上传允许较长超时
		onUploadProgress: params.onUploadProgress,
	})
}

/**
 * 完成 Multipart Upload（通知 MinIO 合并所有分片）
 * 所有分片上传完成后调用，MinIO 合并生成最终文件
 *
 * @param {Object} params
 * @param {string} params.uploadId           - Multipart Upload ID
 * @param {string} params.key                - MinIO 存储路径
 * @param {string|number} params.roomId      - 房间ID（用于业务记录）
 * @param {number} params.duration           - 录制时长（秒）
 * @param {Array<{partNumber: number, etag: string}>} params.parts - 所有已上传分片信息
 * @returns {Promise<{
 *   code: number,
 *   data: {
 *     fileUrl: string,  // 最终录制文件的访问 URL
 *     key: string,      // MinIO 存储路径
 *     duration: number  // 录制时长（秒）
 *   }
 * }>}
 */
export function completeRecordingUpload(params) {
	return axios.post(`${RECORDING_API}/complete`, {
		uploadId: params.uploadId,
		key: params.key,
		roomId: params.roomId,
		duration: params.duration,
		parts: params.parts, // [{partNumber: 1, etag: "xxx"}, ...]
	})
}

/**
 * 中止 Multipart Upload（录制失败时清理 MinIO 临时数据）
 * 未完成的 Multipart Upload 会持续占用 MinIO 存储，必须显式中止
 *
 * @param {Object} params
 * @param {string} params.uploadId - Multipart Upload ID
 * @param {string} params.key      - MinIO 存储路径
 * @returns {Promise<{ code: number, data: null }>}
 */
export function abortRecordingUpload(params) {
	return axios.post(`${RECORDING_API}/abort`, {
		uploadId: params.uploadId,
		key: params.key,
	})
}
