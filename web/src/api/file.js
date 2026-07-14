import axios from '@/utils/axios'

// 接口前缀
const API_PREFIX = '/file'

/**
 * 上传文件
 */
export function uploadFile(formData) {
	return axios.post(`${API_PREFIX}/file/upload`, formData, {
		headers: {
			'Content-Type': 'multipart/form-data',
		},
	})
}
