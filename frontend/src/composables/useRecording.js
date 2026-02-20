import { ref, computed } from 'vue'
import { startRecording, stopRecording } from '@/api/media'
import { $notify } from '@/plugins/notification'

export function useRecording(roomId, hostId) {
	// ==================== 状态 ====================
	const isRecording = ref(false)
	const recordingFormat = ref('mp4')
	const recordingStartTime = ref(null)
	const recordingDuration = ref(0)
	const recordingLoading = ref(false)

	// Dialog 控制
	const showStartDialog = ref(false)
	const showResultDialog = ref(false)

	// 录制结果（停止后填充）
	const recordingResult = ref(null)
	// {fileUrl, fileSize, duration, endTime}

	// ==================== 本地计时器 ====================
	let _timerInterval = null

	const _startTimer = () => {
		_stopTimer()
		recordingDuration.value = 0
		_timerInterval = setInterval(() => {
			recordingDuration.value++
		}, 1000)
	}

	const _stopTimer = () => {
		if (_timerInterval) {
			clearInterval(_timerInterval)
			_timerInterval = null
		}
	}

	// ==================== 格式化工具 ====================
	const formattedDuration = computed(() => {
		const s = recordingDuration.value
		const h = Math.floor(s / 3600)
		const m = Math.floor((s % 3600) / 60)
		const sec = s % 60
		if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
		return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
	})

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
			showStartDialog.value = false
			_startTimer()
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
			_stopTimer()
			const { data } = await stopRecording({
				roomId: roomId.value,
				hostId: hostId.value,
			})
			isRecording.value = false
			recordingResult.value = {
				...data,
				fileUrl: data.fileUrl,
			}
			showResultDialog.value = true
			$notify.success('录制已完成')
		} catch (e) {
			$notify.error(e?.response?.data?.message || '停止录制失败')
			_startTimer() // 停止失败则恢复本地计时
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
		formattedDuration,
		recordingResult,
		// Dialog 控制
		showStartDialog,
		showResultDialog,
		// 操作
		toggleRecording,
		handleStartRecording,
	}
}
