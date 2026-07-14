import { ref } from 'vue'
import { $notify } from '@/plugins/notification'

/**
 * 文件预览
 */
export function useFilePreview() {
	const visible = ref(false)
	const fileUrl = ref('')
	const fileName = ref('')
	const fileType = ref('unknown')
	const downloadProgress = ref(0)

	/**
	 * 根据文件扩展名或 MIME 类型判断文件类型
	 */
	const getFileType = fileNameOrType => {
		const typeMap = {
			// 图片
			jpg: 'image',
			jpeg: 'image',
			png: 'image',
			gif: 'image',
			webp: 'image',
			svg: 'image',
			bmp: 'image',
			// 视频
			mp4: 'video',
			webm: 'video',
			ogg: 'video',
			avi: 'video',
			mov: 'video',
			mkv: 'video',
			flv: 'video',
			// 音频
			mp3: 'audio',
			wav: 'audio',
			flac: 'audio',
			aac: 'audio',
			m4a: 'audio',
			// PDF
			pdf: 'pdf',
			// 文本
			txt: 'text',
			md: 'text',
			json: 'text',
			xml: 'text',
			csv: 'text',
			log: 'text',
		}

		// 提取扩展名
		const ext = fileNameOrType.split('.').pop().toLowerCase()
		return typeMap[ext] || 'unknown'
	}
	// 获取文件图标
	const getFileIcon = fileType => {
		const iconMap = {
			// 图片
			image: 'mdi-file-image-outline',
			// 视频
			video: 'mdi-file-video-outline',
			// 音频
			audio: 'mdi-file-music-outline',
			// PDF
			pdf: 'mdi-file-pdf-box',
			// 文档
			document: 'mdi-file-document-outline',
			word: 'mdi-file-word-box',
			excel: 'mdi-file-excel-box',
			powerpoint: 'mdi-file-powerpoint-box',
			// 压缩包
			archive: 'mdi-folder-zip-outline',
			zip: 'mdi-folder-zip-outline',
			rar: 'mdi-folder-zip-outline',
			// 代码
			code: 'mdi-file-code-outline',
			// 默认
			file: 'mdi-file-outline',
		}
		return iconMap[fileType] || iconMap.file
	}

	/**
	 * 打开预览
	 */
	const openPreview = (url, name = 'default.jpg') => {
		if (!url) {
			$notify.error('文件地址无效')
			return
		}

		fileUrl.value = url
		fileName.value = name || url.split('/').pop()
		fileType.value = getFileType(fileName.value)
		visible.value = true

		console.log('[FilePreview] Open preview:', {
			url: fileUrl.value,
			name: fileName.value,
			type: fileType.value,
		})
	}

	/**
	 * 关闭预览
	 */
	const closePreview = () => {
		visible.value = false
		// 延迟清空数据，避免闪烁
		setTimeout(() => {
			fileUrl.value = ''
			fileName.value = ''
			fileType.value = 'unknown'
		}, 300)
	}

	/**
	 * 下载文件
	 */
	const downloadFile = async ({ url, name }) => {
		try {
			downloadProgress.value = 1

			// 使用 fetch 获取文件
			const response = await fetch(url, {
				method: 'GET',
				// 如果需要携带认证信息
				// credentials: 'include',
			})

			if (!response.ok) {
				throw new Error(`下载失败: ${response.statusText}`)
			}

			// 获取文件大小
			const contentLength = response.headers.get('content-length')
			const total = contentLength ? parseInt(contentLength, 10) : 0

			// 如果文件较小或无法获取大小，直接下载
			if (total === 0 || total < 1024 * 1024) {
				// 小于 1MB，直接获取 Blob
				const blob = await response.blob()
				downloadProgress.value = 50

				triggerDownload(blob, name)
				downloadProgress.value = 100

				setTimeout(() => {
					downloadProgress.value = 0
				}, 500)
			} else {
				// 大文件，显示进度
				await downloadWithProgress(response, total, name)
			}
		} catch (error) {
			console.error('Download failed:', error)
			downloadProgress.value = 0
			$notify.error(`下载失败: ${error.message}`)
		}
	}

	const downloadWithProgress = async (response, total, filename) => {
		let loaded = 0
		const reader = response.body.getReader()
		const chunks = []

		while (true) {
			const { done, value } = await reader.read()

			if (done) break

			chunks.push(value)
			loaded += value.length

			// 更新进度
			if (total > 0) {
				downloadProgress.value = Math.round((loaded / total) * 100)
			}
		}

		// 合并数据
		const blob = new Blob(chunks)
		triggerDownload(blob, filename)

		// 重置进度
		setTimeout(() => {
			downloadProgress.value = 0
		}, 500)
	}

	/**
	 * 触发浏览器下载（使用 Blob URL）
	 */
	const triggerDownload = (blob, filename) => {
		const blobUrl = URL.createObjectURL(blob)

		const link = document.createElement('a')
		link.href = blobUrl
		link.download = filename
		link.style.display = 'none'

		document.body.appendChild(link)
		link.click()

		// 延迟清理，确保下载开始
		setTimeout(() => {
			document.body.removeChild(link)
			URL.revokeObjectURL(blobUrl)
		}, 100)
	}

	return {
		// 状态
		visible,
		fileUrl,
		fileName,
		fileType,
		downloadProgress,

		// 方法
		getFileType,
		getFileIcon,
		openPreview,
		closePreview,
		downloadFile,
	}
}
