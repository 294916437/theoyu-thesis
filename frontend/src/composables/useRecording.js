import { ref, computed, shallowRef } from 'vue'
import { startRecording, stopRecording } from '@/api/media'
import { uploadFile } from '@/api/file'
import { $notify } from '@/plugins/notification'

/**
 * 录制阶段状态机
 *
 * idle         → 空闲，无任何录制
 * checking     → 正在请求后端，检查是否已有录制记录
 * exists       → 后端已有该 roomId+userId 的录制记录（展示已有结果）
 * starting     → 正在初始化本地 MediaRecorder + 调用后端 startRecording
 * recording    → 录制中
 * stopping     → 停止录制中（上传文件 + 调用后端 stopRecording）
 * done         → 录制完成，展示结果
 * error        → 发生错误
 */
export const RECORDING_PHASE = {
	IDLE: 'idle',
	CHECKING: 'checking',
	EXISTS: 'exists',
	STARTING: 'starting',
	RECORDING: 'recording',
	STOPPING: 'stopping',
	DONE: 'done',
	ERROR: 'error',
}

/**
 * 会议录制 Composable（状态机版）
 *
 * @param {import('vue').Ref<string|number>} roomId - 房间ID
 * @param {import('vue').Ref<string|number>} userId - 当前用户ID
 */
export function useRecording(roomId, userId) {
	// ==================== 状态机 ====================
	const phase = ref(RECORDING_PHASE.IDLE)
	const errorMessage = ref('')

	// ==================== 录制元数据 ====================
	const recordingFormat = ref('webm')
	const recordingDuration = ref(0)
	/** @type {import('vue').Ref<{fileUrl,fileSize,duration,endTime,format}|null>} */
	const recordingResult = ref(null)

	// ==================== 内部媒体对象（shallowRef 避免深层响应） ====================
	/** @type {import('vue').ShallowRef<MediaRecorder|null>} */
	const mediaRecorder = shallowRef(null)
	/** @type {import('vue').ShallowRef<MediaStream|null>} */
	const compositeStream = shallowRef(null)
	/** @type {import('vue').ShallowRef<HTMLCanvasElement|null>} */
	const captureCanvas = shallowRef(null)
	/** @type {import('vue').ShallowRef<CanvasRenderingContext2D|null>} */
	const canvasCtx = shallowRef(null)
	/** @type {import('vue').ShallowRef<AudioContext|null>} */
	const audioCtx = shallowRef(null)
	/** @type {import('vue').ShallowRef<MediaStreamAudioDestinationNode|null>} */
	const audioDestination = shallowRef(null)
	/** @type {import('vue').ShallowRef<Map<string,MediaStreamAudioSourceNode>>} */
	const audioSourceNodes = shallowRef(new Map())

	// 录制 Blob 缓冲（全量收集用于最终上传）
	/** @type {import('vue').Ref<Blob[]>} */
	const recordedChunks = ref([])

	// 上传进度 0~100
	const uploadProgress = ref(0)

	// Canvas rAF / 计时器
	let _animFrameId = null
	let _timerInterval = null

	// ==================== 计算属性 ====================
	const isRecording = computed(() => phase.value === RECORDING_PHASE.RECORDING)
	const isLoading = computed(() => [RECORDING_PHASE.CHECKING, RECORDING_PHASE.STARTING, RECORDING_PHASE.STOPPING].includes(phase.value))

	const formattedDuration = computed(() => {
		const s = recordingDuration.value
		const h = Math.floor(s / 3600)
		const m = Math.floor((s % 3600) / 60)
		const sec = s % 60
		if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
		return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
	})

	// ==================== 计时器 ====================
	const _startTimer = () => {
		_stopTimer()
		recordingDuration.value = 0
		_timerInterval = setInterval(() => recordingDuration.value++, 1000)
	}

	const _stopTimer = () => {
		if (_timerInterval) {
			clearInterval(_timerInterval)
			_timerInterval = null
		}
	}

	// ==================== Canvas 视频捕获 ====================
	const _collectVideoElements = () =>
		Array.from(document.querySelectorAll('.video-grid video, .video-tile video')).filter(
			v => v instanceof HTMLVideoElement && !v.paused && v.readyState >= 2 && v.videoWidth > 0,
		)

	const _calcGridLayout = (count, width, height) => {
		const cols = Math.ceil(Math.sqrt(count))
		const rows = Math.ceil(count / cols)
		return { cols, rows, cellW: Math.floor(width / cols), cellH: Math.floor(height / rows) }
	}

	const _createCanvasStream = (width = 1280, height = 720) => {
		const canvas = document.createElement('canvas')
		canvas.width = width
		canvas.height = height
		canvas.style.cssText = 'position:fixed;top:-9999px;left:-9999px;pointer-events:none;'
		document.body.appendChild(canvas)

		const ctx = canvas.getContext('2d')
		captureCanvas.value = canvas
		canvasCtx.value = ctx

		const renderFrame = () => {
			_animFrameId = requestAnimationFrame(renderFrame)
			ctx.fillStyle = '#1a1a2e'
			ctx.fillRect(0, 0, width, height)

			const videos = _collectVideoElements()
			if (!videos.length) return

			const { cols, cellW, cellH } = _calcGridLayout(videos.length, width, height)
			videos.forEach((video, index) => {
				const x = (index % cols) * cellW
				const y = Math.floor(index / cols) * cellH
				try {
					const vr = video.videoWidth / video.videoHeight
					const cr = cellW / cellH
					let sx = 0,
						sy = 0,
						sw = video.videoWidth,
						sh = video.videoHeight
					if (vr > cr) {
						sw = Math.round(video.videoHeight * cr)
						sx = Math.round((video.videoWidth - sw) / 2)
					} else {
						sh = Math.round(video.videoWidth / cr)
						sy = Math.round((video.videoHeight - sh) / 2)
					}
					ctx.drawImage(video, sx, sy, sw, sh, x, y, cellW, cellH)
				} catch (_) {
					ctx.fillStyle = '#2a2a3e'
					ctx.fillRect(x, y, cellW, cellH)
				}
			})
		}

		renderFrame()
		return canvas.captureStream(30)
	}

	const _destroyCanvas = () => {
		if (_animFrameId) {
			cancelAnimationFrame(_animFrameId)
			_animFrameId = null
		}
		captureCanvas.value?.remove()
		captureCanvas.value = null
		canvasCtx.value = null
	}

	// ==================== AudioContext 混音 ====================
	const _collectAudioStreams = () => {
		const streams = []
		document.querySelectorAll('.video-grid video, .video-tile video').forEach(video => {
			if (video instanceof HTMLVideoElement && video.srcObject instanceof MediaStream) {
				if (video.srcObject.getAudioTracks().some(t => t.readyState === 'live')) {
					streams.push(video.srcObject)
				}
			}
		})
		return streams
	}

	const _createAudioMixer = audioStreams => {
		const ctx = new AudioContext()
		const destination = ctx.createMediaStreamDestination()
		const nodes = new Map()
		audioStreams.forEach((stream, idx) => {
			try {
				const source = ctx.createMediaStreamSource(stream)
				source.connect(destination)
				nodes.set(`stream-${idx}`, source)
			} catch (e) {
				console.warn('[Recording] Failed to connect audio stream:', e)
			}
		})
		audioCtx.value = ctx
		audioDestination.value = destination
		audioSourceNodes.value = nodes
		return destination.stream
	}

	const _destroyAudioMixer = async () => {
		if (!audioCtx.value) return
		audioSourceNodes.value.forEach(node => {
			try {
				node.disconnect()
			} catch (e) {
				/* noop */
			}
		})
		audioSourceNodes.value.clear()
		try {
			await audioCtx.value.close()
		} catch (e) {
			/* noop */
		}
		audioCtx.value = null
		audioDestination.value = null
	}

	// ==================== 工具 ====================
	const _getSupportedMimeType = format => {
		const candidates = format === 'webm' ? ['video/webm;codecs=vp9,opus', 'video/webm;codecs=vp8,opus', 'video/webm'] : ['video/mp4;codecs=h264,aac', 'video/mp4']
		return candidates.find(t => MediaRecorder.isTypeSupported(t)) || null
	}

	const _cleanupMedia = async () => {
		_destroyCanvas()
		await _destroyAudioMixer()
		compositeStream.value?.getTracks().forEach(t => t.stop())
		compositeStream.value = null
		mediaRecorder.value = null
	}

	// ==================== 状态机动作 ====================

	/**
	 * 阶段一：打开对话框时调用
	 * 向后端请求检查是否已有 roomId+userId 的录制记录
	 */
	const checkAndOpen = async () => {
		// 录制中：直接返回，让父组件打开 Dialog 展示 RECORDING 阶段即可
		if (phase.value === RECORDING_PHASE.RECORDING) return

		phase.value = RECORDING_PHASE.CHECKING
		errorMessage.value = ''

		try {
			const { data } = await startRecording({
				roomId: roomId.value,
				userId: userId.value,
				format: recordingFormat.value,
			})

			if (data.exists && data.fileUrl) {
				recordingResult.value = {
					fileUrl: data.fileUrl,
					fileSize: data.fileSize,
					duration: data.duration,
					endTime: data.endTime ? new Date(data.endTime) : null,
					format: data.format || recordingFormat.value,
				}
				phase.value = RECORDING_PHASE.EXISTS
			} else {
				phase.value = RECORDING_PHASE.STARTING
			}
		} catch (error) {
			console.error('[Recording] checkAndOpen failed:', error)
			errorMessage.value = error.message || '检查录制状态失败'
			phase.value = RECORDING_PHASE.ERROR
		}
	}

	/**
	 * 阶段二：用户确认格式后，启动本地 MediaRecorder
	 * @param {string} format - 'mp4' | 'webm'
	 */
	const handleStartRecording = async format => {
		recordingFormat.value = format
		phase.value = RECORDING_PHASE.STARTING

		try {
			const mimeType = _getSupportedMimeType(format)
			if (!mimeType) throw new Error('当前浏览器不支持录制功能，请使用 Chrome 或 Edge')

			// 创建 Canvas + 音频混流
			const videoStream = _createCanvasStream(1280, 720)
			const audioStreams = _collectAudioStreams()
			let audioStream = null
			if (audioStreams.length > 0) {
				audioStream = _createAudioMixer(audioStreams)
			}

			const tracks = [...videoStream.getVideoTracks(), ...(audioStream ? audioStream.getAudioTracks() : [])]
			const composite = new MediaStream(tracks)
			compositeStream.value = composite

			recordedChunks.value = []
			const recorder = new MediaRecorder(composite, {
				mimeType,
				videoBitsPerSecond: 2_500_000,
				audioBitsPerSecond: 128_000,
			})

			recorder.ondataavailable = event => {
				if (event.data?.size > 0) {
					recordedChunks.value.push(event.data)
				}
			}

			recorder.onerror = event => {
				console.error('[Recording] MediaRecorder error:', event.error)
				$notify.error(`录制出错: ${event.error?.message || '未知错误'}`)
				handleStopRecording()
			}

			recorder.onstop = () => {
				_finishRecording()
			}

			// 每 5 秒切一个 chunk（保证数据实时性）
			recorder.start(5000)
			mediaRecorder.value = recorder
			_startTimer()

			phase.value = RECORDING_PHASE.RECORDING
			$notify.success('录制已开始')
		} catch (error) {
			console.error('[Recording] Start failed:', error)
			errorMessage.value = error.message
			phase.value = RECORDING_PHASE.ERROR
			await _cleanupMedia()
		}
	}

	/**
	 * 阶段三：用户点击停止录制
	 */
	const handleStopRecording = () => {
		const recorder = mediaRecorder.value
		if (!recorder || recorder.state === 'inactive') return

		_stopTimer()
		phase.value = RECORDING_PHASE.STOPPING
		recorder.stop()
	}

	/**
	 * 阶段四：MediaRecorder.onstop 触发后
	 * 1. 合并所有 Blob
	 * 2. 调用 uploadFile 上传
	 * 3. 调用 stopRecording 更新后端记录
	 */
	const _finishRecording = async () => {
		uploadProgress.value = 0
		let progressTimer = null

		try {
			// 合并所有录制分片为完整文件
			const mimeType = _getSupportedMimeType(recordingFormat.value) || `video/${recordingFormat.value}`
			const finalBlob = new Blob(recordedChunks.value, { type: mimeType })
			const fileSize = finalBlob.size

			if (fileSize === 0) throw new Error('录制文件为空，请重试')

			// Step 1: 上传文件到服务器
			const fileName = `recording-${roomId.value}-${userId.value}-${Date.now()}.${recordingFormat.value}`
			const formData = new FormData()
			formData.append('file', finalBlob, fileName)

			// 模拟上传进度
			progressTimer = setInterval(() => {
				if (uploadProgress.value < 85) uploadProgress.value += 5
			}, 200)

			const { data: fileUrl } = await uploadFile(formData)

			clearInterval(progressTimer)
			uploadProgress.value = 95

			// Step 2: 通知后端更新录制记录
			const { data } = await stopRecording({
				roomId: roomId.value,
				userId: userId.value,
				fileUrl,
				fileSize,
				duration: recordingDuration.value,
				format: recordingFormat.value,
			})

			uploadProgress.value = 100

			recordingResult.value = {
				fileUrl: data.fileUrl || fileUrl,
				fileSize: data.fileSize || fileSize,
				duration: data.duration || recordingDuration.value,
				endTime: data.endTime ? new Date(data.endTime) : new Date(),
				format: recordingFormat.value,
			}

			phase.value = RECORDING_PHASE.DONE
			$notify.success('录制已完成，文件已保存')
		} catch (error) {
			console.error('[Recording] Finish failed:', error)
			errorMessage.value = error.message || '录制文件保存失败'
			phase.value = RECORDING_PHASE.ERROR
		} finally {
			if (progressTimer) clearInterval(progressTimer)
			recordedChunks.value = []
			await _cleanupMedia()
		}
	}

	/**
	 * 重置到初始状态（关闭对话框时调用）
	 */
	const reset = async (minimized = false) => {
		// 录制中：触发停止流程（会进入 STOPPING）
		if (isRecording.value) {
			if (minimized) return
			handleStopRecording()
			return
		}
		// 上传保存中：禁止重置，等待完成
		if (phase.value === RECORDING_PHASE.STOPPING) {
			$notify.warning('正在保存录制文件，请稍候...')
			return
		}
		_stopTimer()
		await _cleanupMedia()
		recordingDuration.value = 0
		recordingResult.value = null
		uploadProgress.value = 0
		errorMessage.value = ''
		phase.value = RECORDING_PHASE.IDLE
	}

	/**
	 * 控制栏按钮：切换录制状态
	 * - 未录制时：打开对话框并检查已有记录
	 * - 录制中时：停止录制
	 */
	const toggleRecording = () => {
		checkAndOpen()
	}

	return {
		// 状态
		phase,
		isRecording,
		isLoading,
		errorMessage,
		recordingFormat,
		recordingDuration,
		formattedDuration,
		recordingResult,
		uploadProgress,
		// 操作
		checkAndOpen,
		handleStartRecording,
		handleStopRecording,
		toggleRecording,
		reset,
	}
}
