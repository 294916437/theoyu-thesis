/* eslint-disable no-undef */
import { ref, computed, watch, nextTick } from 'vue'
import { socketClient } from '@/utils/SocketClient'
import { MediasoupClient } from '@/utils/MediasoupClient'
import init, { MeetProcessor, init_panic_hook } from '@/libs/meet-effect/meet_background_effect.js'
import wasmUrl from '@/libs/meet-effect/meet_background_effect_bg.wasm?url'
import { $notify } from '@/plugins/notification'
import router from '@/router'

const MASK_WIDTH = 256
const MASK_HEIGHT = 144
const VIDEO_WIDTH = 640
const VIDEO_HEIGHT = 480

export function useMedia() {
	// 状态管理
	const roomId = ref(null)
	const peerId = ref(null)
	const userId = ref(null)
	const username = ref(null)
	const localStream = ref(null)
	const participants = ref([])
	const audioEnabled = ref(true)
	const videoEnabled = ref(true)
	const screenSharing = ref(false)
	const screenStream = ref(null)
	const originalVideoTrack = ref(null)
	const connectionState = ref('disconnected') // disconnected | connecting | connected | failed
	const connectionQuality = ref({
		send: { score: 10, quality: 'excellent' },
		recv: { score: 10, quality: 'excellent' },
	})
	// ========== 背景特效状态 ==========
	const effectStream = ref(null) // 特效流
	const originalCameraTrack = ref(null) // 保存原始摄像头轨道
	const effectProducerActive = ref(false) // 特效 producer 是否激活
	const effectType = ref('none') // 'none' | 'blur' | 'replace'
	const selectedBackground = ref(null)
	const customBackgrounds = ref([])
	const effectLoading = ref(false)
	const effectError = ref(null)

	// 预设背景列表
	const presetBackgrounds = ref([
		{
			id: 'office-modern',
			name: '现代办公室',
			thumbnail: '/backgrounds/stylish_home_office.jpg',
			url: '/backgrounds/stylish_home_office.jpg',
		},
		{
			id: 'office-break',
			name: '咖啡休息室',
			thumbnail: '/backgrounds/office_break_room.jpg',
			url: '/backgrounds/office_break_room.jpg',
		},
	])

	const allBackgrounds = computed(() => [...presetBackgrounds.value, ...customBackgrounds.value])

	// WASM 和推理相关
	let wasmModule = null
	let effectProcessor = null
	let onnxSession = null
	let effectCanvas = null
	let effectCtx = null
	let maskCanvas = null
	let maskCtx = null
	let currentMask = null
	let isInferring = false
	let effectAnimationId = null
	let inferenceTimeoutId = null
	let sourceVideoElement = null
	const float32Data = new Float32Array(MASK_WIDTH * MASK_HEIGHT * 3)

	// Mediasoup 客户端实例
	let mediasoupClient = null

	// 统计信息
	const stats = ref({
		audio: null,
		video: null,
		screen: null,
	})
	let statsIntervalId = null

	/**
	 * 初始化背景特效资源
	 */
	async function initBackgroundEffect() {
		if (effectProcessor && onnxSession) {
			console.log('[BackgroundEffect] Resources already initialized')
			return true
		}

		try {
			effectLoading.value = true

			// 检查 ONNX Runtime
			if (typeof ort === 'undefined') {
				throw new Error('ONNX Runtime 未加载，请检查 CDN 连接')
			}

			// 1. 初始化 WASM
			console.log('[BackgroundEffect] Initializing WASM...')
			wasmModule = await init({ module_or_path: wasmUrl })
			init_panic_hook()
			effectProcessor = MeetProcessor.new(VIDEO_WIDTH, VIDEO_HEIGHT)
			console.log('[BackgroundEffect] WASM initialized successfully')

			// 2. 配置 ONNX Runtime WASM 路径
			ort.env.wasm.wasmPaths = 'https://cdn.jsdelivr.net/npm/onnxruntime-web@1.23.2/dist/'
			ort.env.wasm.numThreads = 2

			// 3. 加载 ONNX 模型
			console.log('[BackgroundEffect] Loading ONNX model...')
			onnxSession = await ort.InferenceSession.create('/models/model_float32_opt.onnx', {
				executionProviders: ['wasm'],
				graphOptimizationLevel: 'all',
			})
			console.log('[BackgroundEffect] ONNX session created')

			// 4. 创建 Canvas
			maskCanvas = new OffscreenCanvas(MASK_WIDTH, MASK_HEIGHT)
			maskCtx = maskCanvas.getContext('2d', { willReadFrequently: true, alpha: false })

			effectCanvas = document.createElement('canvas')
			effectCanvas.width = VIDEO_WIDTH
			effectCanvas.height = VIDEO_HEIGHT
			effectCtx = effectCanvas.getContext('2d', { willReadFrequently: true, alpha: false })

			console.log('[BackgroundEffect] Canvas initialized')
			return true
		} catch (error) {
			console.error('[BackgroundEffect] Initialization failed:', error)
			effectError.value = `初始化失败: ${error.message}`
			$notify.error(effectError.value)
			throw error
		} finally {
			effectLoading.value = false
		}
	}
	/**
	 * 推理循环 - 生成分割 mask
	 */
	async function inferenceLoop(sourceVideo) {
		// 检查是否应该停止
		if (!sourceVideo || effectType.value === 'none' || !sourceVideoElement) {
			console.log('[BackgroundEffect] Inference loop stopped')
			return
		}

		if (isInferring) {
			inferenceTimeoutId = setTimeout(() => inferenceLoop(sourceVideo), 30)
			return
		}

		try {
			isInferring = true

			// 检查视频是否准备好
			if (sourceVideo.readyState < 2) {
				isInferring = false
				inferenceTimeoutId = setTimeout(() => inferenceLoop(sourceVideo), 30)
				return
			}

			// 绘制到小尺寸 canvas
			maskCtx.drawImage(sourceVideo, 0, 0, MASK_WIDTH, MASK_HEIGHT)
			const imgData = maskCtx.getImageData(0, 0, MASK_WIDTH, MASK_HEIGHT).data

			// 归一化
			for (let i = 0, j = 0; i < imgData.length; i += 4, j += 3) {
				float32Data[j] = imgData[i] / 255
				float32Data[j + 1] = imgData[i + 1] / 255
				float32Data[j + 2] = imgData[i + 2] / 255
			}

			// 推理
			const results = await onnxSession.run({
				'input_1:0': new ort.Tensor('float32', float32Data, [1, MASK_HEIGHT, MASK_WIDTH, 3]),
			})

			const maskData = results[Object.keys(results)[0]].data
			const mask = new Float32Array(MASK_WIDTH * MASK_HEIGHT)

			// Sigmoid 处理
			for (let i = 0; i < MASK_WIDTH * MASK_HEIGHT; i++) {
				const bg = maskData[i * 2]
				const fg = maskData[i * 2 + 1]
				mask[i] = 1.0 / (1.0 + Math.exp(bg - fg))
			}

			currentMask = mask
		} catch (error) {
			console.error('[BackgroundEffect] Inference error:', error)
		} finally {
			isInferring = false

			// 继续下一次推理
			if (sourceVideoElement && effectType.value !== 'none') {
				inferenceTimeoutId = setTimeout(() => inferenceLoop(sourceVideo), 30)
			}
		}
	}

	/**
	 * 渲染循环 - 应用特效（改为定时器驱动，增强稳定性）
	 */
	function renderLoop() {
		// 使用闭包或全局引用 sourceVideoElement
		const videoEl = sourceVideoElement

		// 检查停止条件
		if (!videoEl || effectType.value === 'none' || !effectProducerActive.value) {
			return
		}

		try {
			// 只有当视频准备好且有宽/高时才绘制
			if (videoEl.readyState >= 2 && videoEl.videoWidth > 0 && videoEl.videoHeight > 0) {
				// 1. 绘制源视频到 Canvas
				effectCtx.drawImage(videoEl, 0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)

				// 2. 应用特效 (WASM/Mask)
				if (currentMask && effectProcessor && wasmModule) {
					try {
						const frameData = effectCtx.getImageData(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)

						// 写入输入帧
						const inputPtr = effectProcessor.input_ptr()
						const inputBuffer = new Uint8Array(wasmModule.memory.buffer, inputPtr, VIDEO_WIDTH * VIDEO_HEIGHT * 4)
						inputBuffer.set(frameData.data)

						// 写入 mask
						const maskPtr = effectProcessor.mask_ptr()
						const maskBuffer = new Float32Array(wasmModule.memory.buffer, maskPtr, MASK_WIDTH * MASK_HEIGHT)
						maskBuffer.set(currentMask)

						// 处理
						effectProcessor.prepare_mask()

						if (effectType.value === 'blur') {
							effectProcessor.render_blur()
						} else if (effectType.value === 'replace') {
							const bgPtr = effectProcessor.background_ptr()
							const bgBuffer = new Uint8Array(wasmModule.memory.buffer, bgPtr, VIDEO_WIDTH * VIDEO_HEIGHT * 4)
							// 简单的非零检查，确认背景已加载
							if (bgBuffer.some(p => p !== 0)) {
								effectProcessor.render_replace()
							} else {
								effectProcessor.render_blur()
							}
						}

						// 读取输出并绘回
						const outputPtr = effectProcessor.output_ptr()
						const outputBuffer = new Uint8ClampedArray(wasmModule.memory.buffer, outputPtr, VIDEO_WIDTH * VIDEO_HEIGHT * 4)
						effectCtx.putImageData(new ImageData(outputBuffer.slice(), VIDEO_WIDTH, VIDEO_HEIGHT), 0, 0)
					} catch (err) {
						// 忽略单帧处理错误，防止循环中断
						console.warn('Effect processing error:', err)
					}
				}
			}
		} catch (error) {
			console.error('[BackgroundEffect] Render error:', error)
		}

		// 强制 30FPS 循环 (约33ms)
		effectAnimationId = setTimeout(renderLoop, 33)
	}
	/**
	 * 启动背景特效流
	 */
	async function startEffectStream() {
		try {
			if (effectProducerActive.value) return

			await initBackgroundEffect()

			const currentVideoProducer = mediasoupClient.producers.get('video')
			if (!currentVideoProducer) throw new Error('No camera producer found')

			// 1. 克隆轨道
			originalCameraTrack.value = currentVideoProducer.track
			const clonedTrack = currentVideoProducer.track.clone()

			// 2. 创建源视频元素
			// [关键修复] 将视频元素挂载到 DOM，防止浏览器停止解码
			sourceVideoElement = document.createElement('video')
			sourceVideoElement.id = 'effect-source-hidden'
			sourceVideoElement.autoplay = true
			sourceVideoElement.muted = true
			sourceVideoElement.playsInline = true
			sourceVideoElement.width = VIDEO_WIDTH
			sourceVideoElement.height = VIDEO_HEIGHT

			// 样式设置：不可见但占据布局（避免 display:none）
			sourceVideoElement.style.position = 'absolute'
			sourceVideoElement.style.top = '-9999px'
			sourceVideoElement.style.left = '-9999px'
			sourceVideoElement.style.width = '1px'
			sourceVideoElement.style.height = '1px'
			sourceVideoElement.style.opacity = '0'
			sourceVideoElement.style.pointerEvents = 'none'
			document.body.appendChild(sourceVideoElement)

			sourceVideoElement.srcObject = new MediaStream([clonedTrack])

			// 3. 等待播放
			await new Promise((resolve, reject) => {
				const timeout = setTimeout(() => reject(new Error('Video load timeout')), 3000)
				sourceVideoElement.onloadedmetadata = () => {
					sourceVideoElement
						.play()
						.then(() => {
							clearTimeout(timeout)
							resolve()
						})
						.catch(reject)
				}
			})

			// 4. 加载背景
			if (effectType.value === 'replace' && selectedBackground.value) {
				await loadBackgroundImage(selectedBackground.value)
			}

			// 5. 启动循环
			effectProducerActive.value = true
			inferenceLoop(sourceVideoElement)
			renderLoop() // 不传参，使用闭包变量

			// 6. 等待稳定
			await new Promise(resolve => setTimeout(resolve, 500))

			// 7. 捕获流
			effectStream.value = effectCanvas.captureStream(30)
			const effectVideoTrack = effectStream.value.getVideoTracks()[0]
			effectVideoTrack.contentHint = 'motion'

			// 8. 关闭旧 Producer
			await socketClient.emit('closeProducer', {
				roomId: roomId.value,
				producerId: currentVideoProducer.id,
			})
			currentVideoProducer.close()
			mediasoupClient.producers.delete('video')

			// 9. 创建新 Producer
			const effectProducer = await mediasoupClient.produce(effectVideoTrack, {
				kind: 'video',
				appData: {
					source: 'effect',
					effectType: effectType.value,
				},
			})

			effectVideoTrack.onended = () => {
				stopEffectStream()
			}
			mediasoupClient.producers.set('video', effectProducer)

			// 10. 更新本地状态
			const localPeer = participants.value.find(p => p.peerId === peerId.value)
			if (localPeer) {
				localPeer.producers.video = {
					id: effectProducer.id,
					kind: 'video',
					paused: false,
					appData: { source: 'effect', effectType: effectType.value },
				}

				// 更新显示
				if (localPeer.streams.video) {
					const videoStream = localPeer.streams.video
					videoStream.getTracks().forEach(t => videoStream.removeTrack(t))
					videoStream.addTrack(effectVideoTrack)
				}

				if (localStream.value) {
					localStream.value.getTracks().forEach(t => {
						if (t.kind === 'video') localStream.value.removeTrack(t)
					})
					localStream.value.addTrack(effectVideoTrack)
				}
			}

			return effectProducer
		} catch (error) {
			console.error('Failed to start effect:', error)
			await stopEffectStream()
			throw error
		}
	}
	/**
	 * 停止背景特效流
	 */
	async function stopEffectStream() {
		if (!effectProducerActive.value) return

		// 1. 清理 DOM 元素
		if (sourceVideoElement) {
			if (sourceVideoElement.srcObject) {
				sourceVideoElement.srcObject.getTracks().forEach(t => t.stop())
			}
			if (sourceVideoElement.parentNode) {
				sourceVideoElement.parentNode.removeChild(sourceVideoElement)
			}
			sourceVideoElement = null
		}

		// 2. 停止定时器
		if (effectAnimationId) {
			clearTimeout(effectAnimationId)
			effectAnimationId = null
		}
		if (inferenceTimeoutId) {
			clearTimeout(inferenceTimeoutId)
			inferenceTimeoutId = null
		}

		isInferring = false

		// 3. 停止流
		if (effectStream.value) {
			effectStream.value.getTracks().forEach(t => t.stop())
			effectStream.value = null
		}

		// 4. 关闭服务器 producer
		const effectProducer = mediasoupClient.producers.get('video')
		if (effectProducer && effectProducer.appData?.source === 'effect') {
			try {
				await socketClient.emit('closeProducer', {
					roomId: roomId.value,
					producerId: effectProducer.id,
				})
				effectProducer.close()
				mediasoupClient.producers.delete('video')
			} catch (e) {
				console.warn(e)
			}
		}

		// 5. 恢复原始摄像头
		if (originalCameraTrack.value && originalCameraTrack.value.readyState === 'live') {
			const newProducer = await mediasoupClient.produce(originalCameraTrack.value, {
				kind: 'video',
				appData: { source: 'camera' },
			})
			mediasoupClient.producers.set('video', newProducer)
			updateLocalProducer('video', newProducer)

			// 刷新 Track 到本地流
			const localPeer = participants.value.find(p => p.peerId === peerId.value)
			if (localPeer && localPeer.streams.video) {
				localPeer.streams.video.getTracks().forEach(t => localPeer.streams.video.removeTrack(t))
				localPeer.streams.video.addTrack(originalCameraTrack.value)
			}
			if (localStream.value) {
				localStream.value.getTracks().forEach(t => {
					if (t.kind === 'video') localStream.value.removeTrack(t)
				})
				localStream.value.addTrack(originalCameraTrack.value)
			}
		} else {
			// 重新请求摄像头
			const stream = await navigator.mediaDevices.getUserMedia({
				video: { width: 1280, height: 720 },
			})
			const newTrack = stream.getVideoTracks()[0]
			const newProducer = await mediasoupClient.produce(newTrack, {
				kind: 'video',
				appData: { source: 'camera' },
			})
			mediasoupClient.producers.set('video', newProducer)
			updateLocalProducer('video', newProducer)
			// 刷新通过 newTrack 更新 localStream...
			const localPeer = participants.value.find(p => p.peerId === peerId.value)
			if (localPeer && localPeer.streams.video) {
				localPeer.streams.video.getTracks().forEach(t => t.stop()) // kill old
				localPeer.streams.video.addTrack(newTrack)
			}
			if (localStream.value) {
				localStream.value.addTrack(newTrack)
			}
		}

		// 6. 重置状态
		effectProducerActive.value = false
		originalCameraTrack.value = null
	}

	/**
	 * 替换视频轨道（用于特效切换）
	 */
	async function replaceVideoTrack(newTrack) {
		const videoProducer = mediasoupClient.producers.get('video')
		if (!videoProducer) {
			console.warn('[BackgroundEffect] No video producer found')
			return
		}

		try {
			// 替换 producer 轨道
			await videoProducer.replaceTrack({ track: newTrack })

			// 更新本地流时，清理所有旧的视频轨道
			const oldTracks = localStream.value.getVideoTracks()
			oldTracks.forEach(track => {
				track.stop()
				localStream.value.removeTrack(track)
			})

			localStream.value.addTrack(newTrack)

			console.log('[BackgroundEffect] Video track replaced:', newTrack.id)
		} catch (error) {
			console.error('[BackgroundEffect] Failed to replace track:', error)
			throw error
		}
	}

	/**
	 * 加载背景图片到 WASM 内存
	 */
	async function loadBackgroundImage(bgId) {
		const bg = allBackgrounds.value.find(b => b.id === bgId)
		if (!bg) return

		return new Promise((resolve, reject) => {
			const img = new Image()
			img.crossOrigin = 'Anonymous'
			img.src = bg.url
			img.onload = () => {
				const canvas = new OffscreenCanvas(VIDEO_WIDTH, VIDEO_HEIGHT)
				const ctx = canvas.getContext('2d')

				// Cover 模式
				const scale = Math.max(VIDEO_WIDTH / img.width, VIDEO_HEIGHT / img.height)
				const x = (VIDEO_WIDTH - img.width * scale) / 2
				const y = (VIDEO_HEIGHT - img.height * scale) / 2
				ctx.drawImage(img, x, y, img.width * scale, img.height * scale)

				const imageData = ctx.getImageData(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)

				// 写入 WASM 背景缓冲区
				if (effectProcessor && wasmModule) {
					const bgPtr = effectProcessor.background_ptr()
					const bgBuffer = new Uint8Array(wasmModule.memory.buffer, bgPtr, VIDEO_WIDTH * VIDEO_HEIGHT * 4)
					bgBuffer.set(imageData.data)
				}

				resolve()
			}
			img.onerror = reject
		})
	}

	/**
	 * 上传自定义背景
	 */
	async function uploadCustomBackground(file) {
		return new Promise((resolve, reject) => {
			const reader = new FileReader()
			reader.onload = e => {
				const id = 'custom-' + Date.now()
				const newBg = {
					id,
					name: file.name,
					thumbnail: e.target.result,
					url: e.target.result,
				}
				customBackgrounds.value.push(newBg)
				resolve(newBg)
			}
			reader.onerror = reject
			reader.readAsDataURL(file)
		})
	}

	/**
	 * 主持人：远程控制参与者音频
	 */
	async function hostToggleAudio(targetPeerId, enabled) {
		try {
			await socketClient.emit('hostToggleAudio', {
				roomId: roomId.value,
				targetPeerId: targetPeerId,
				enabled: enabled,
			})

			console.log(`Host ${enabled ? 'unmuted' : 'muted'} participant ${targetPeerId}`)
		} catch (error) {
			console.error('Failed to toggle participant audio:', error)
			throw error
		}
	}

	/**
	 * 主持人：远程控制参与者视频
	 */
	async function hostToggleVideo(targetPeerId, enabled) {
		try {
			await socketClient.emit('hostToggleVideo', {
				roomId: roomId.value,
				targetPeerId: targetPeerId,
				enabled: enabled,
			})

			console.log(`Host ${enabled ? 'enabled' : 'disabled'} video for ${targetPeerId}`)
		} catch (error) {
			console.error('Failed to toggle participant video:', error)
			throw error
		}
	}
	/**
	 * 主持人：全体静音（排除主持人自己）
	 */
	async function muteAll() {
		try {
			const response = await socketClient.emit('hostMuteAll', {
				roomId: roomId.value,
			})

			if (response.success) {
				console.log(`[Host] Muted ${response.mutedCount} participants`)
			}
		} catch (error) {
			console.error('[Host] Failed to mute all:', error)
			$notify.error('全体静音失败')
			throw error
		}
	}

	/**
	 * 主持人：关闭全体视频（排除主持人自己）
	 */
	async function disableAllVideo() {
		try {
			const response = await socketClient.emit('hostDisableAllVideo', {
				roomId: roomId.value,
			})

			if (response.success) {
				console.log(`[Host] Disabled video for ${response.disabledCount} participants`)
			}
		} catch (error) {
			console.error('[Host] Failed to disable all video:', error)
			$notify.error('关闭全体摄像头失败')
			throw error
		}
	}
	/**
	 * 主持人：踢出参与者
	 */
	async function removeParticipant(targetPeerId) {
		try {
			const response = await socketClient.emit('removeParticipant', {
				targetPeerId,
			})

			if (response.success) {
				console.log(`[Host] Removed participant ${targetPeerId}`)

				// 从本地列表中移除
				const index = participants.value.findIndex(p => p.peerId === targetPeerId)
				if (index !== -1) {
					participants.value.splice(index, 1)
				}
			}
		} catch (error) {
			console.error('[Host] Failed to remove participant:', error)
			$notify.error('移除参与者失败')
			throw error
		}
	}

	// 获取远程参与者列表
	const remoteParticipants = computed(() => participants.value.filter(p => p.peerId !== peerId.value))

	// 获取本地参与者
	const localParticipant = computed(() => {
		const local = participants.value.find(p => p.peerId === peerId.value)
		if (local && localStream.value) {
			local.streams = {
				audio: new MediaStream(localStream.value.getAudioTracks()),
				video: new MediaStream(localStream.value.getVideoTracks()),
			}
			local.isLocal = true
		}
		return local
	})

	/**
	 * 加入房间
	 */
	async function joinMeeting(meetingId, userIdParam, usernameParam, token) {
		try {
			connectionState.value = 'connecting'

			// 保存用户信息
			userId.value = userIdParam
			username.value = usernameParam
			roomId.value = meetingId

			// 1. 连接 Socket.io
			await socketClient.connect(import.meta.env.VITE_SFU_URL || 'http://localhost:3000', {
				auth: { token },
			})

			// 2. 加入房间
			const joinResponse = await socketClient.emit('joinRoom', {
				roomId: roomId.value,
				userId: userIdParam,
				username: usernameParam,
				token,
			})

			peerId.value = joinResponse.peerId
			console.log(`Joined room ${roomId.value}`, joinResponse)

			// 3. 设置现有参与者（包括自己）
			participants.value = [
				{
					peerId: joinResponse.peerId,
					userId: userIdParam,
					username: usernameParam,
					streams: {},
					producers: {},
					consumers: {},
					isLocal: true,
				},
				...joinResponse.peers.map(peer => ({
					peerId: peer.peerId,
					userId: peer.userId,
					username: peer.username,
					streams: {},
					producers: {},
					consumers: {},
					isLocal: false,
				})),
			]

			// 4. 获取路由器 RTP 能力
			const { rtpCapabilities } = await socketClient.emit('getRouterRtpCapabilities', {
				roomId: meetingId,
			})

			// 5. 初始化 Mediasoup Device
			mediasoupClient = new MediasoupClient()
			await mediasoupClient.loadDevice(rtpCapabilities)

			// 6. 创建传输层
			try {
				await mediasoupClient.createSendTransport(meetingId)
				await mediasoupClient.createRecvTransport(meetingId)
			} catch (error) {
				console.error('Failed to create transports:', error)
				throw new Error('传输层创建失败，请检查网络连接')
			}

			// 7. 获取本地媒体流
			await getLocalStream()

			// 8. 发布本地媒体流
			if (localStream.value) {
				const audioTrack = localStream.value.getAudioTracks()[0]
				const videoTrack = localStream.value.getVideoTracks()[0]

				// 添加发布重试逻辑
				const publishWithTimeout = async (track, kind, timeout = 15000) => {
					return Promise.race([
						mediasoupClient.produce(track, { kind }),
						new Promise((_, reject) => setTimeout(() => reject(new Error(`Publish ${kind} timeout`)), timeout)),
					])
				}

				try {
					// 串行发布，确保 transport 连接已建立
					if (audioTrack) {
						console.log('Publishing audio track...')
						const audioProducer = await publishWithTimeout(audioTrack, 'audio')
						updateLocalProducer('audio', audioProducer)
						console.log('Audio published successfully')
					}

					if (videoTrack) {
						console.log('Publishing video track...')
						const videoProducer = await publishWithTimeout(videoTrack, 'video')
						updateLocalProducer('video', videoProducer)
						console.log('Video published successfully')
					}
				} catch (error) {
					console.error('Failed to publish media streams:', error)

					// 如果是超时错误，给出更明确的提示
					if (error.message.includes('timeout')) {
						$notify.error('媒体流发布超时，请检查网络或防火墙设置')
					} else {
						$notify.error('发布媒体流失败，但仍可以接收其他人的视频')
					}

					// 不抛出错误，允许用户继续观看
				}
			}
			// 9. 监听事件
			setupSocketListeners()
			// 10. 监听特效相关状态变化
			watch([effectType, selectedBackground], async ([newType, newBg], [oldType, oldBg]) => {
				if (!localStream.value) return

				try {
					// 情况1: 关闭所有效果
					if (newType === 'none') {
						// 停止特效
						await stopEffectStream()

						// 重新获取原始摄像头轨道
						const stream = await navigator.mediaDevices.getUserMedia({
							video: {
								width: { ideal: 1280, max: 1920 },
								height: { ideal: 720, max: 1080 },
								frameRate: { ideal: 30, max: 60 },
							},
						})

						const newVideoTrack = stream.getVideoTracks()[0]
						await replaceVideoTrack(newVideoTrack)

						console.log('[BackgroundEffect] Original camera restored')
						return
					}

					// 情况2: 仅背景图片变化（replace -> replace）
					if (newType === 'replace' && oldType === 'replace' && newBg !== oldBg) {
						// 只需重新加载背景，无需重建流
						await loadBackgroundImage(newBg)
						return
					}

					// 情况3: 效果类型变化或首次启用
					if (newType !== oldType || (newType === 'replace' && !oldBg)) {
						// 先停止旧效果（如果有）
						if (effectProducerActive.value) {
							await stopEffectStream()
							await new Promise(resolve => setTimeout(resolve, 200))
						}

						// 启动新效果
						await startEffectStream()
					}
				} catch (error) {
					console.error('[BackgroundEffect] Failed to switch effect:', error)
					effectType.value = 'none'
				}
			})

			// 11. 订阅现有参与者的媒体流
			for (const peer of joinResponse.peers) {
				if (peer.producers && peer.producers.length > 0) {
					for (const producer of peer.producers) {
						await consumeProducer(producer.id, peer.peerId)
					}
				}
			}

			connectionState.value = 'connected'
		} catch (error) {
			console.error('Failed to join meeting', error)
			connectionState.value = 'failed'
			$notify.error(`加入会议失败: ${error.message}`)
			throw error
		}
	}

	/**
	 * 获取本地媒体流
	 */
	async function getLocalStream() {
		try {
			const stream = await navigator.mediaDevices.getUserMedia({
				audio: {
					echoCancellation: true,
					noiseSuppression: true,
					autoGainControl: true,
					sampleRate: 48000,
				},
				video: {
					width: { ideal: 1280, max: 1920 },
					height: { ideal: 720, max: 1080 },
					frameRate: { ideal: 30, max: 60 },
				},
			})

			localStream.value = stream
			console.log('Local stream acquired', stream.id)

			// 更新本地参与者流
			const localPeer = participants.value.find(p => p.peerId === peerId.value)
			if (localPeer) {
				localPeer.streams.local = stream
			}

			return stream
		} catch (error) {
			console.error('Failed to get local stream', error)
			$notify.error('无法获取摄像头/麦克风权限')
			throw error
		}
	}

	/**
	 * 更新本地生产者
	 */
	function updateLocalProducer(kind, producer) {
		const localPeer = participants.value.find(p => p.peerId === peerId.value)
		if (localPeer) {
			localPeer.producers[kind] = producer

			// 监听生产者质量分数
			producer.on('score', score => {
				connectionQuality.value.send = {
					score: score.score,
					quality: getQualityLevel(score.score),
				}
			})
		}
	}

	/**
	 * 订阅远程生产者
	 */
	async function consumeProducer(producerId, remotePeerId) {
		try {
			console.log(`Consuming producer ${producerId} ${remotePeerId}`)

			const consumer = await mediasoupClient.consume(roomId.value, producerId, remotePeerId)

			// 找到对应的参与者
			const participant = participants.value.find(p => p.peerId === remotePeerId)
			if (!participant) {
				console.error(`Participant ${remotePeerId} not found`)
				return
			}

			if (!participant.streams) participant.streams = {}
			if (!participant.consumers) participant.consumers = {}
			if (!participant.producers) participant.producers = {}

			// 创建或更新对应类型的 MediaStream(screen/audio/camera/effect)
			const kind = consumer.track.kind

			// 创建或更新流
			if (!participant.streams[kind]) {
				participant.streams[kind] = new MediaStream([consumer.track])
				console.log(`Created new ${kind} stream for peer ${remotePeerId}`)
			} else {
				const existingStream = participant.streams[kind]
				const existingTracks = existingStream.getTracks()

				existingTracks.forEach(track => {
					if (track.kind === kind) {
						existingStream.removeTrack(track)
						track.stop()
					}
				})

				existingStream.addTrack(consumer.track)
				console.log(`Updated ${kind} stream for peer ${remotePeerId}`)
			}
			if (kind === 'effect') {
				setTimeout(() => {
					// 通过 socket 请求关键帧 (需要后端支持 requestKeyframe 事件，或者简单的暂停再恢复)
					// 如果后端没有 requestKeyframe，可以忽略此步，通常 resume 已经足够

					// 前端 hack: 强制 Video 元素重载
					const newStreamId = participant.streams[kind].id
					// 触发 VideoGrid 里的 update
				}, 500)
			}

			// 记录到 producers（用于 UI 判断状态）
			participant.producers[kind] = {
				id: producerId,
				kind: kind,
				paused: false,
				appData: consumer.appData || {},
			}
			participant.consumers[consumer.id] = consumer

			// 监听 track 状态
			consumer.track.onended = () => {
				console.log(`Consumer track ended: ${consumer.id}`)
				// 清理流
				if (participant.streams[kind]) {
					participant.streams[kind].removeTrack(consumer.track)
				}
			}

			console.log(`Consumer track added ${remotePeerId} ${kind}`)

			// 强制触发响应式更新
			participants.value = [...participants.value]
		} catch (error) {
			console.error('Failed to consume producer', producerId, error)
		}
	}

	/**
	 * 设置 Socket 事件监听
	 */
	function setupSocketListeners() {
		// 注册心跳监听(测试暂时关闭)
		// socketClient.on('ping', data => {
		// 	console.log('Received ping from server', data)
		// 	// 立即响应 pong
		// 	socketClient.socket.emit('pong', { timestamp: data.timestamp })
		// 	console.log('Pong sent to server')
		// })

		// // 监听 RTT
		// socketClient.on('rtt', data => {
		// 	console.log(`RTT: ${data.rtt}ms`)
		// })

		// 新参与者加入
		socketClient.on('newPeer', async data => {
			console.log('New peer joined', data)

			const existingPeer = participants.value.find(p => p.peerId === data.peerId)
			if (!existingPeer) {
				participants.value.push({
					peerId: data.peerId,
					userId: data.userId,
					username: data.username,
					streams: {},
					producers: {},
					consumers: {},
					isLocal: false,
				})
			}
		})

		// 参与者离开
		socketClient.on('peerLeft', data => {
			console.log('Peer left', data)

			const index = participants.value.findIndex(p => p.peerId === data.peerId)
			if (index !== -1) {
				const participant = participants.value[index]

				// 清理流
				Object.values(participant.streams).forEach(stream => {
					if (stream instanceof MediaStream) {
						stream.getTracks().forEach(track => track.stop())
					}
				})

				// 清理消费者
				Object.values(participant.consumers).forEach(consumer => {
					consumer.close()
				})

				participants.value.splice(index, 1)
			}
		})
		// 监听被踢出事件
		socketClient.on('removedFromRoom', async data => {
			$notify.error('您已被主持人移出会议', {
				timeout: 3000,
			})

			// 自动离开会议
			await leaveMeeting({ reason: 'removed_by_host' })

			// 跳转到首页
			setTimeout(() => {
				router.push('/')
			}, 3000)
		})

		// 新生产者
		socketClient.on('newProducer', async data => {
			console.log('New producer', data)

			const { producerId, peerId: remotePeerId, userId: remoteUserId, username, kind } = data

			// 确保参与者存在
			let participant = participants.value.find(p => p.peerId === remotePeerId)

			if (!participant) {
				// 如果参与者不存在（理论上不应该发生），创建一个
				participant = {
					peerId: remotePeerId,
					userId: remoteUserId,
					username: username,
					streams: {},
					producers: {},
					consumers: {},
					isLocal: false,
				}
				participants.value.push(participant)
			}

			// 记录生产者信息
			if (!participant.producers) {
				participant.producers = {}
			}
			participant.producers[producerId] = {
				id: producerId,
				kind: kind,
				paused: data.paused || false,
			}

			// 订阅这个新的生产者
			await consumeProducer(producerId, remotePeerId)
		})

		// 生产者关闭
		socketClient.on('producerClosed', data => {
			console.log('Producer closed', data)

			const participant = participants.value.find(p => p.peerId === data.peerId)
			if (participant && participant.consumers[data.producerId]) {
				const consumer = participant.consumers[data.producerId]
				const kind = consumer.track.kind

				// 关闭消费者
				consumer.close()
				delete participant.consumers[data.producerId]

				// 从流中移除轨道
				if (participant.streams[kind]) {
					const stream = participant.streams[kind]
					stream.getTracks().forEach(track => {
						if (track.id === consumer.track.id) {
							track.stop()
							stream.removeTrack(track)
						}
					})

					// 如果流为空，删除流
					if (stream.getTracks().length === 0) {
						delete participant.streams[kind]
					}
				}

				// 从生产者列表中移除
				participant.producers = participant.producers.filter(p => p.id !== data.producerId)
			}
		})

		// 消费者关闭
		socketClient.on('consumerClosed', data => {
			console.log('Consumer closed', data)

			const participant = participants.value.find(p => Object.values(p.consumers).some(c => c.id === data.consumerId))
			if (participant) {
				const consumer = participant.consumers[data.consumerId]
				if (consumer) {
					consumer.close()
					delete participant.consumers[data.consumerId]
				}
			}
		})
		// 添加统一状态变化监听
		socketClient.on('producerStateChanged', data => {
			const { producerId, peerId: remotePeerId, kind, paused, reason } = data

			// 1. 更新参与者的 producers 状态
			updateProducerState(remotePeerId, producerId, paused, reason)

			// 3. 强制触发响应式更新
			participants.value = [...participants.value]
		})
	}

	/**
	 * 更新生产者状态
	 */
	function updateProducerState(remotePeerId, producerId, paused, reason) {
		const participant = participants.value.find(p => p.peerId === remotePeerId)

		if (!participant) {
			console.warn(`[Media] Participant ${remotePeerId} not found`)
			return
		}

		const isLocalUser = participant.peerId === peerId.value
		let targetKind = null

		// 1. 更新 producers 记录
		if (participant.producers) {
			for (const [kind, producer] of Object.entries(participant.producers)) {
				if (producer && producer.id === producerId) {
					targetKind = kind
					if (isLocalUser) {
						// 本地用户：创建一个包装对象，不直接修改 Producer 实例
						participant.producers[kind] = {
							...producer,
							paused: paused,
						}
					} else {
						// 远程用户：直接修改普通对象
						producer.paused = paused
					}

					console.log(`[Media] Updated ${kind} producer state to paused=${paused} (isLocal: ${isLocalUser})`)
					break
				}
			}
		}

		// 2. 同步 Consumer 状态（仅远程参与者）
		if (!isLocalUser && participant.consumers && targetKind) {
			for (const [consumerId, consumer] of Object.entries(participant.consumers)) {
				if (consumer.producerId === producerId) {
					try {
						// 暂停/恢复 Consumer
						if (paused) {
							consumer.pause()
						} else {
							consumer.resume()
						}

						// 同步轨道状态
						if (consumer.track) {
							consumer.track.enabled = !paused
						}

						// 同步流中的轨道
						if (participant.streams && participant.streams[targetKind]) {
							const stream = participant.streams[targetKind]
							stream.getTracks().forEach(track => {
								if (track.kind === targetKind && track.id === consumer.track.id) {
									track.enabled = !paused
								}
							})
						}
					} catch (error) {
						console.error(`[Media] Failed to update consumer:`, error)
					}
					break
				}
			}
		}

		// 3. 如果是本地用户被主持人控制
		if (isLocalUser) {
			// 当且仅当 reason 是 host_control 时才显示通知，避免误报其他原因导致的状态变化
			if (reason === 'host_control') {
				$notify.warning(`主持人${paused ? '关闭了' : '开启了'}您的${targetKind === 'audio' ? '麦克风' : '摄像头'}`, { timeout: 3000 })
			}

			// 更新状态变量
			if (targetKind === 'audio') {
				audioEnabled.value = !paused
			} else if (targetKind === 'video') {
				videoEnabled.value = !paused
			}

			// 获取当前 Producer 并控制轨道
			const currentProducer = mediasoupClient.producers.get(targetKind)
			if (currentProducer && currentProducer.track) {
				currentProducer.track.enabled = !paused

				// 同步到 localStream
				if (localStream.value) {
					const tracks = targetKind === 'audio' ? localStream.value.getAudioTracks() : localStream.value.getVideoTracks()

					tracks.forEach(track => {
						track.enabled = !paused
					})
				}
			}
		}

		// 4. 使用 nextTick 优化响应式更新
		nextTick(() => {
			// 触发最小化的响应式更新
			participants.value = [...participants.value]
		})
	}

	/**
	 * 切换音频
	 */
	async function toggleAudio() {
		try {
			const willEnable = !audioEnabled.value

			// 1. 通知服务器（等待响应）
			const response = await socketClient.emit('toggleAudio', {
				roomId: roomId.value,
				enabled: willEnable,
			})

			// 根据服务器返回的状态更新
			if (response.success) {
				// 2. 本地轨道同步
				localStream.value?.getAudioTracks().forEach(track => {
					track.enabled = response.enabled
				})

				// 3. 更新状态
				audioEnabled.value = response.enabled

				console.log('Audio toggled to:', response.enabled)
			}
		} catch (error) {
			console.error('Failed to toggle audio', error)
			// 失败时回滚状态
			audioEnabled.value = !audioEnabled.value
			$notify.error('切换音频失败')
			throw error
		}
	}

	/**
	 * 切换视频
	 */
	async function toggleVideo() {
		try {
			const willEnable = !videoEnabled.value

			const response = await socketClient.emit('toggleVideo', {
				roomId: roomId.value,
				enabled: willEnable,
			})

			if (response.success) {
				localStream.value?.getVideoTracks().forEach(track => {
					track.enabled = response.enabled
				})

				videoEnabled.value = response.enabled

				console.log('Video toggled to:', response.enabled)
			}
		} catch (error) {
			console.error('Failed to toggle video', error)
			videoEnabled.value = !videoEnabled.value
			$notify.error('切换视频失败')
			throw error
		}
	}

	/**
	 * 开始屏幕共享,思路如下：
	 * 1. 关闭原 camera video producer
	 * 2. 创建新的 screen video producer
	 * 3. 更新本地流状态
	 */
	async function startScreenShare() {
		try {
			if (screenSharing.value) {
				$notify.warning('您已在共享屏幕')
				return
			}

			if (hasScreenShare.value && !screenShareInfo.value.presenter.isLocal) {
				$notify.warning('已有参与者正在共享屏幕')
				return
			}

			// 1. 获取屏幕共享流
			const stream = await navigator.mediaDevices.getDisplayMedia({
				video: {
					cursor: 'always',
					displaySurface: 'monitor',
					width: { max: 1920 },
					height: { max: 1080 },
					frameRate: { max: 30 },
				},
				audio: false,
			})

			screenStream.value = stream
			const screenVideoTrack = stream.getVideoTracks()[0]

			// 2. 保存并关闭原 camera producer
			const currentVideoProducer = mediasoupClient.producers.get('video')
			if (currentVideoProducer) {
				// 保存原始轨道（用于恢复）
				originalVideoTrack.value = currentVideoProducer.track

				// 通知服务器关闭原 producer
				await socketClient.emit('closeProducer', {
					roomId: roomId.value,
					producerId: currentVideoProducer.id,
				})

				// 关闭本地 producer
				currentVideoProducer.close()
				mediasoupClient.producers.delete('video')

				console.log('Original camera producer closed')
			}

			// 3. 创建新的屏幕共享 producer（使用 video kind）
			const screenProducer = await mediasoupClient.produce(screenVideoTrack, {
				kind: 'video',
				appData: {
					source: 'screen',
					shareType: 'display',
				},
			})

			// 存储为 video producer（替代原来的摄像头）
			mediasoupClient.producers.set('video', screenProducer)

			// 4. 更新本地参与者状态
			const localPeer = participants.value.find(p => p.peerId === peerId.value)
			if (localPeer) {
				// 更新 producers 标记
				localPeer.producers.video = {
					id: screenProducer.id,
					kind: 'video',
					paused: false,
					appData: { source: 'screen' },
				}

				// 替换本地流中的视频轨道
				if (localPeer.streams.video) {
					const videoStream = localPeer.streams.video
					const oldTracks = videoStream.getVideoTracks()
					oldTracks.forEach(track => {
						track.stop()
						videoStream.removeTrack(track)
					})
					videoStream.addTrack(screenVideoTrack)
				} else {
					localPeer.streams.video = new MediaStream([screenVideoTrack])
				}

				// 同步到 localStream
				if (localStream.value) {
					const oldTracks = localStream.value.getVideoTracks()
					oldTracks.forEach(track => {
						track.stop()
						localStream.value.removeTrack(track)
					})
					localStream.value.addTrack(screenVideoTrack)
				}
			}

			screenSharing.value = true

			// 5. 监听用户主动停止共享
			screenVideoTrack.onended = async () => {
				console.log('Screen share ended by user')
				await stopScreenShare()
			}

			console.log('Screen share started, producer ID:', screenProducer.id)

			return screenProducer
		} catch (error) {
			screenSharing.value = false

			if (error.name === 'NotAllowedError') {
				console.log('User cancelled screen share')
			} else {
				console.error('Failed to start screen share:', error)
				$notify.error('屏幕共享失败')
			}

			throw error
		}
	}

	/**
	 * 停止屏幕共享
	 * 1. 关闭 screen producer
	 * 2. 重新创建 camera video producer
	 * 3. 恢复本地流状态
	 */
	async function stopScreenShare() {
		try {
			if (!screenSharing.value && !screenStream.value) {
				console.log('Screen share already stopped')
				return
			}

			console.log('Stopping screen share...')

			// 1. 停止屏幕共享流
			if (screenStream.value) {
				screenStream.value.getTracks().forEach(track => {
					track.stop()
					console.log('Stopped screen track:', track.id)
				})
				screenStream.value = null
			}

			// 2. 关闭屏幕共享 producer
			const screenProducer = mediasoupClient.producers.get('video')
			if (screenProducer && screenProducer.appData?.source === 'screen') {
				await socketClient.emit('closeProducer', {
					roomId: roomId.value,
					producerId: screenProducer.id,
				})

				screenProducer.close()
				mediasoupClient.producers.delete('video')
				console.log('Screen producer closed')
			}

			// 3. 重新创建摄像头 producer
			if (originalVideoTrack.value && originalVideoTrack.value.readyState === 'live') {
				// 如果原轨道还活着，直接复用
				const newProducer = await mediasoupClient.produce(originalVideoTrack.value, {
					kind: 'video',
					appData: { source: 'camera' },
				})

				mediasoupClient.producers.set('video', newProducer)
				updateLocalProducer('video', newProducer)

				// 更新本地流
				const localPeer = participants.value.find(p => p.peerId === peerId.value)
				if (localPeer) {
					if (localPeer.streams.video) {
						const videoStream = localPeer.streams.video
						const oldTracks = videoStream.getVideoTracks()
						oldTracks.forEach(track => videoStream.removeTrack(track))
						videoStream.addTrack(originalVideoTrack.value)
					}

					if (localStream.value) {
						const oldTracks = localStream.value.getVideoTracks()
						oldTracks.forEach(track => localStream.value.removeTrack(track))
						localStream.value.addTrack(originalVideoTrack.value)
					}
				}

				console.log('Camera video restored from original track')
			} else {
				// 原轨道失效，重新获取摄像头
				console.log('Original track invalid, getting new camera stream...')

				const stream = await navigator.mediaDevices.getUserMedia({
					video: {
						width: { ideal: 1280, max: 1920 },
						height: { ideal: 720, max: 1080 },
						frameRate: { ideal: 30, max: 60 },
					},
				})

				const newVideoTrack = stream.getVideoTracks()[0]

				const newProducer = await mediasoupClient.produce(newVideoTrack, {
					kind: 'video',
					appData: { source: 'camera' },
				})

				mediasoupClient.producers.set('video', newProducer)
				updateLocalProducer('video', newProducer)

				// 更新本地流
				const localPeer = participants.value.find(p => p.peerId === peerId.value)
				if (localPeer) {
					if (localPeer.streams.video) {
						const videoStream = localPeer.streams.video
						const oldTracks = videoStream.getVideoTracks()
						oldTracks.forEach(track => {
							track.stop()
							videoStream.removeTrack(track)
						})
						videoStream.addTrack(newVideoTrack)
					}

					if (localStream.value) {
						const oldTracks = localStream.value.getVideoTracks()
						oldTracks.forEach(track => {
							track.stop()
							localStream.value.removeTrack(track)
						})
						localStream.value.addTrack(newVideoTrack)
					}
				}

				console.log('New camera stream created')
			}

			// 4. 更新状态
			screenSharing.value = false
			originalVideoTrack.value = null

			// 强制更新 UI
			participants.value = [...participants.value]

			console.log('Screen share stopped successfully')
		} catch (error) {
			console.error('Failed to stop screen share:', error)
			screenSharing.value = false
			screenStream.value = null
			$notify.error('停止屏幕共享失败')
		}
	}

	/**
	 * 获取当前正在共享屏幕的参与者
	 */
	function getScreenSharingParticipant() {
		return participants.value.find(p => p.producers?.screen || p.streams?.screen)
	}

	/**
	 * 检查是否有人在共享屏幕
	 */
	const hasScreenShare = computed(() => {
		return participants.value.some(p => {
			return p.producers?.video?.appData?.source === 'screen'
		})
	})
	/**
	 * 获取屏幕共享信息
	 */
	const screenShareInfo = computed(() => {
		const sharingPeer = participants.value.find(p => p.producers?.video?.appData?.source === 'screen')

		if (!sharingPeer) {
			return { active: false, presenter: null }
		}

		return {
			active: true,
			presenter: {
				id: sharingPeer.peerId,
				name: sharingPeer.username,
				isLocal: sharingPeer.peerId === peerId.value,
			},
		}
	})

	/**
	 * 设置首选层（Simulcast/SVC）
	 */
	async function setPreferredLayers(consumerId, spatialLayer, temporalLayer = 2) {
		try {
			await socketClient.emit('setPreferredLayers', {
				roomId: roomId.value,
				consumerId,
				spatialLayer,
				temporalLayer,
			})

			console.log(`Set preferred layers for consumer ${consumerId}`, { spatialLayer, temporalLayer })
		} catch (error) {
			console.error('Failed to set preferred layers', error)
		}
	}

	/**
	 * 获取统计信息
	 */
	async function getStats(type = 'all') {
		try {
			const localPeer = participants.value.find(p => p.peerId === peerId.value)
			if (!localPeer) return null

			const results = {}

			// 获取音频统计
			if ((type === 'all' || type === 'audio') && localPeer.producers.audio) {
				const audioStats = await socketClient.emit('getStats', {
					roomId: roomId.value,
					producerId: localPeer.producers.audio.id,
				})
				results.audio = audioStats.stats
			}

			// 获取视频统计
			if ((type === 'all' || type === 'video') && localPeer.producers.video) {
				const videoStats = await socketClient.emit('getStats', {
					roomId: roomId.value,
					producerId: localPeer.producers.video.id,
				})
				results.video = videoStats.stats
			}

			// 获取屏幕共享统计
			if ((type === 'all' || type === 'screen') && localPeer.producers.screen) {
				const screenStats = await socketClient.emit('getStats', {
					roomId: roomId.value,
					producerId: localPeer.producers.screen.id,
				})
				results.screen = screenStats.stats
			}

			stats.value = results
			return results
		} catch (error) {
			console.error('Failed to get stats', error)
			return null
		}
	}

	/**
	 * 启动统计信息收集
	 */
	function startStatsCollection() {
		// 清理旧的定时器
		if (statsIntervalId) {
			clearInterval(statsIntervalId)
			statsIntervalId = null
		}
		statsIntervalId = setInterval(async () => {
			if (connectionState.value === 'connected') {
				try {
					await getStats()
				} catch (error) {
					console.error('Failed to collect stats:', error)
					stopStatsCollection()
				}
			}
		}, 10000) // 每10秒收集一次统计信息
	}
	/**
	 * 停止统计信息收集
	 */
	function stopStatsCollection() {
		if (statsIntervalId) {
			clearInterval(statsIntervalId)
			statsIntervalId = null
			console.log('Stats collection stopped')
		}
	}

	/**
	 * 获取质量等级
	 */
	function getQualityLevel(score) {
		if (score >= 8) return 'excellent'
		if (score >= 6) return 'good'
		if (score >= 4) return 'fair'
		if (score >= 2) return 'poor'
		return 'bad'
	}

	/**
	 * 离开会议
	 */
	async function leaveMeeting(options = {}) {
		const { reason = 'self_leave' } = options
		try {
			// 1. 先同步停止背景特效
			await stopEffectStream()

			// 2. 通知服务器离开(只有非强制离开才需要主动通知)
			if (reason !== 'removed_by_host' && socketClient.connected.value) {
				try {
					await socketClient.emit('leaveRoom', {
						roomId: roomId.value,
						reason,
					})
				} catch (error) {
					console.error('Error notifying server:', error)
				}
			}

			// 3. 停止所有本地流
			localStream.value?.getTracks().forEach(track => track.stop())
			screenStream.value?.getTracks().forEach(track => track.stop())

			// 4. 清理所有参与者的流和消费者
			participants.value.forEach(participant => {
				Object.values(participant.streams || {}).forEach(stream => {
					if (stream instanceof MediaStream) {
						stream.getTracks().forEach(track => track.stop())
					}
				})
				Object.values(participant.consumers || {}).forEach(consumer => {
					consumer.close()
				})
			})

			// 5. 清理 Mediasoup 客户端
			mediasoupClient?.close()
			mediasoupClient = null

			// 6. 断开 Socket 连接
			socketClient.disconnect()

			// 7. 清理 WASM 和 ONNX（放到最后，确保循环已停止）
			if (onnxSession) {
				try {
					await onnxSession.release()
					onnxSession = null
					console.log('[BackgroundEffect] ONNX session released')
				} catch (error) {
					console.error('[BackgroundEffect] Failed to release ONNX session:', error)
				}
			}

			if (effectProcessor) {
				effectProcessor.free()
				effectProcessor = null
			}

			// 8. 销毁 Canvas（确保循环已停止）
			if (effectCanvas) {
				effectCtx = null
				effectCanvas.width = 0
				effectCanvas.height = 0
				effectCanvas = null
			}

			if (maskCanvas) {
				maskCtx = null
				maskCanvas = null
			}

			wasmModule = null

			// 9. 重置所有状态
			roomId.value = null
			peerId.value = null
			userId.value = null
			username.value = null
			localStream.value = null
			screenStream.value = null
			participants.value = []
			audioEnabled.value = true
			videoEnabled.value = true
			screenSharing.value = false
			connectionState.value = 'disconnected'
			connectionQuality.value = {
				send: { score: 10, quality: 'excellent' },
				recv: { score: 10, quality: 'excellent' },
			}

			console.log(`Left meeting successfully (reason: ${reason})`)
		} catch (error) {
			console.error('Failed to leave meeting', error)
			throw error
		}
	}

	/**
	 * 更换音频设备
	 */
	async function changeAudioDevice(deviceId) {
		try {
			const stream = await navigator.mediaDevices.getUserMedia({
				audio: { deviceId: { exact: deviceId } },
			})

			const audioTrack = stream.getAudioTracks()[0]
			const audioProducer = mediasoupClient.producers.get('audio')

			if (audioProducer) {
				await audioProducer.replaceTrack({ track: audioTrack })
				console.log('Audio device changed', deviceId)
			}

			// 更新本地流
			const oldAudioTrack = localStream.value.getAudioTracks()[0]
			oldAudioTrack?.stop()
			localStream.value.removeTrack(oldAudioTrack)
			localStream.value.addTrack(audioTrack)
		} catch (error) {
			console.error('Failed to change audio device', error)
			$notify.error('更换音频设备失败')
		}
	}

	/**
	 * 更换视频设备
	 */
	async function changeVideoDevice(deviceId) {
		try {
			const stream = await navigator.mediaDevices.getUserMedia({
				video: { deviceId: { exact: deviceId } },
			})

			const videoTrack = stream.getVideoTracks()[0]
			const videoProducer = mediasoupClient.producers.get('video')

			if (videoProducer) {
				await videoProducer.replaceTrack({ track: videoTrack })
				console.log('Video device changed', deviceId)
			}

			// 更新本地流
			const oldVideoTrack = localStream.value.getVideoTracks()[0]
			oldVideoTrack?.stop()
			localStream.value.removeTrack(oldVideoTrack)
			localStream.value.addTrack(videoTrack)
		} catch (error) {
			console.error('Failed to change video device', error)
			$notify.error('更换视频设备失败')
		}
	}

	return {
		// 状态
		roomId,
		peerId,
		userId,
		username,
		localStream,
		participants,
		remoteParticipants,
		localParticipant,
		audioEnabled,
		videoEnabled,
		screenSharing,
		screenStream,
		connectionState,
		connectionQuality,
		stats,
		hasScreenShare,
		effectType,
		effectProducerActive,
		selectedBackground,
		allBackgrounds,
		effectLoading,
		effectError,

		// 方法
		joinMeeting,
		leaveMeeting,
		removeParticipant,
		toggleAudio,
		toggleVideo,
		hostToggleAudio,
		hostToggleVideo,
		muteAll,
		disableAllVideo,
		startScreenShare,
		stopScreenShare,
		stopStatsCollection,
		setPreferredLayers,
		getStats,
		changeAudioDevice,
		changeVideoDevice,
		getScreenSharingParticipant,
		uploadCustomBackground,
	}
}
