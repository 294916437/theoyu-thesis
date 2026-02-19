import { ref, computed } from 'vue'
import { useIntervalFn } from '@vueuse/core'
import { startRecording, stopRecording, getRecordingStatus } from '@/api/media'
import { $notify } from '@/plugins/notification'

export function useRecording(roomId, hostId) {
	// ==================== 状态 ====================
	const isRecording = ref(false)
	const recordingFormat = ref('mp4')
	const recordingStartTime = ref(null)
	const recordingDuration = ref(0) // 实时时长（秒）
	const recordingFileSize = ref(0) // 实时文件大小（字节）
	const recordingLoading = ref(false) // API 请求中

	// Dialog 控制
	const showStartDialog = ref(false) // 开始前的格式选择 Dialog
	const showResultDialog = ref(false) // 完成后的结果 Dialog

	// 录制结果（停止后填充）
	const recordingResult = ref(null)
	// {fileUrl, fileSize, duration, endTime}

	// ==================== 轮询状态 ====================
	// 每 5 秒轮询一次实时状态
	const { pause: pausePoll, resume: resumePoll } = useIntervalFn(
		async () => {
			if (!isRecording.value || !roomId.value) return
			try {
				const { data } = await getRecordingStatus(roomId.value, hostId.value)
				if (data.isRecording) {
					recordingDuration.value = data.durationSeconds ?? 0
					recordingFileSize.value = data.fileSizeBytes ?? 0
				}
			} catch (e) {
				// 轮询失败静默处理，不影响主流程
				console.warn('[useRecording] 轮询状态失败', e)
			}
		},
		5000,
		{ immediate: false }, // 不立即执行，等开始录制后再启动
	)

	// ==================== 格式化工具 ====================
	const formattedDuration = computed(() => {
		const s = recordingDuration.value
		const h = Math.floor(s / 3600)
		const m = Math.floor((s % 3600) / 60)
		const sec = s % 60
		if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
		return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
	})

	const formattedFileSize = computed(() => {
		const bytes = recordingFileSize.value
		if (!bytes) return '计算中...'
		if (bytes < 1024) return `${bytes} B`
		if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
		return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
	})

	// ==================== 完整文件URL处理 ====================
	/**
	 * 处理后端返回的 fileUrl：
	 * - 若已是完整 URL（http/https 开头）直接返回
	 * - 若是相对路径，拼接 OSS/CDN base URL
	 */
	const resolveFileUrl = rawUrl => {
		if (!rawUrl) return ''
		if (/^https?:\/\//i.test(rawUrl)) return rawUrl
		const base = import.meta.env.VITE_OSS_BASE_URL?.replace(/\/$/, '') || ''
		return `${base}/${rawUrl.replace(/^\//, '')}`
	}

	// ==================== 开始录制 ====================
	const handleStartRecording = async () => {
		if (!roomId.value || !hostId.value) {
			$notify.error('房间信息不完整')
			return
		}
		recordingLoading.value = true
		try {
			await startRecording({
				roomId: roomId.value,
				hostId: hostId.value,
				format: recordingFormat.value,
			})
			isRecording.value = true
			recordingStartTime.value = new Date()
			recordingDuration.value = 0
			recordingFileSize.value = 0
			showStartDialog.value = false
			resumePoll() // 启动轮询
			$notify.success('录制已开始')
		} catch (e) {
			$notify.error(e?.response?.data?.message || '启动录制失败')
		} finally {
			recordingLoading.value = false
		}
	}

	// ==================== 停止录制 ====================
	const handleStopRecording = async () => {
		if (!isRecording.value) return
		recordingLoading.value = true
		try {
			pausePoll() // 立即停止轮询
			const { data } = await stopRecording({
				roomId: roomId.value,
				hostId: hostId.value,
			})
			isRecording.value = false
			// 处理完整文件 URL
			recordingResult.value = {
				...data,
				fileUrl: resolveFileUrl(data.fileUrl),
			}
			showResultDialog.value = true
			$notify.success('录制已完成')
		} catch (e) {
			$notify.error(e?.response?.data?.message || '停止录制失败')
			resumePoll() // 停止失败则继续轮询
		} finally {
			recordingLoading.value = false
		}
	}

	// ==================== 按钮点击入口 ====================
	const toggleRecording = () => {
		if (isRecording.value) {
			handleStopRecording()
		} else {
			showStartDialog.value = true
		}
	}

	return {
		// 状态
		isRecording,
		recordingFormat,
		recordingLoading,
		recordingDuration,
		recordingFileSize,
		formattedDuration,
		formattedFileSize,
		recordingResult,
		// Dialog 控制
		showStartDialog,
		showResultDialog,
		// 操作
		toggleRecording,
		handleStartRecording,
	}
}
