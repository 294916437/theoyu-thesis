import { ref, computed, watch, onUnmounted } from 'vue'
import { useStorage, useAsyncState } from '@vueuse/core'
import * as ort from 'onnxruntime-web'

const MASK_WIDTH = 256
const MASK_HEIGHT = 144
const VIDEO_WIDTH = 640
const VIDEO_HEIGHT = 480

export function useBackgroundEffect() {
	// ========== 状态管理 ==========
	const effectType = useStorage('meeting-effect-type', 'none') // none | blur | replace
	const selectedBackground = useStorage('meeting-selected-bg', '')
	const customBackgrounds = useStorage('meeting-custom-bgs', [])

	const isProcessing = ref(false)
	const isLoading = ref(false)
	const error = ref(null)

	// WASM 和模型实例
	let wasmModule = null
	let processor = null
	let session = null
	let originalVideoTrack = null
	let processedStream = null

	// Canvas 上下文
	let inputCanvas = null
	let outputCanvas = null
	let inputCtx = null
	let outputCtx = null
	let animationFrameId = null

	// ========== 预设背景列表 ==========
	const presetBackgrounds = ref([
		{
			id: 'office',
			name: '现代办公室',
			thumbnail: '/assets/backgrounds/thumbnails/office_break_room.jpg',
			url: '/assets/backgrounds/office_break_room.jpg',
		},
		{
			id: 'home',
			name: '家庭房间',
			thumbnail: '/assets/backgrounds/thumbnails/stylish_home_office.jpg',
			url: '/assets/backgrounds/stylish_home_office.jpg',
		},
		// 更多预设...
	])

	// 合并预设和自定义背景
	const allBackgrounds = computed(() => [...presetBackgrounds.value, ...customBackgrounds.value])

	// ========== 初始化 WASM 和模型 ==========
	const {
		state: initState,
		isReady,
		execute: initResources,
	} = useAsyncState(
		async () => {
			try {
				isLoading.value = true

				// 1. 加载 WASM 模块
				const wasmPath = '/wasm/meet_background_effect_bg.wasm'
				const { default: init, MeetProcessor, init_panic_hook } = await import('/wasm/meet_background_effect.js')

				wasmModule = await init(wasmPath)
				init_panic_hook()
				processor = MeetProcessor.new(VIDEO_WIDTH, VIDEO_HEIGHT)

				// 2. 加载 ONNX 模型
				session = await ort.InferenceSession.create('/models/segmentation_model.onnx', {
					executionProviders: ['wasm'],
					graphOptimizationLevel: 'all',
				})

				// 3. 创建离屏 Canvas
				inputCanvas = new OffscreenCanvas(MASK_WIDTH, MASK_HEIGHT)
				outputCanvas = new OffscreenCanvas(VIDEO_WIDTH, VIDEO_HEIGHT)
				inputCtx = inputCanvas.getContext('2d')
				outputCtx = outputCanvas.getContext('2d', { willReadFrequently: true })

				console.log('Background effect resources initialized')
				return true
			} catch (err) {
				console.error('Failed to initialize background effect:', err)
				error.value = err.message
				throw err
			} finally {
				isLoading.value = false
			}
		},
		null,
		{ immediate: false },
	)

	// ========== 加载背景图片到 WASM ==========
	async function loadBackgroundImage(url) {
		return new Promise((resolve, reject) => {
			const img = new Image()
			img.crossOrigin = 'Anonymous'
			img.src = url

			img.onload = () => {
				const canvas = new OffscreenCanvas(VIDEO_WIDTH, VIDEO_HEIGHT)
				const ctx = canvas.getContext('2d')

				// 保持宽高比裁剪
				const scale = Math.max(VIDEO_WIDTH / img.width, VIDEO_HEIGHT / img.height)
				const x = (VIDEO_WIDTH - img.width * scale) / 2
				const y = (VIDEO_HEIGHT - img.height * scale) / 2

				ctx.drawImage(img, x, y, img.width * scale, img.height * scale)
				const imageData = ctx.getImageData(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)

				// 上传到 WASM 内存
				const bgPtr = processor.background_ptr()
				const bgBuffer = new Uint8Array(wasmModule.memory.buffer, bgPtr, VIDEO_WIDTH * VIDEO_HEIGHT * 4)
				bgBuffer.set(imageData.data)

				resolve(imageData)
			}

			img.onerror = reject
		})
	}

	// ========== AI 推理（生成分割 Mask）==========
	async function inferSegmentationMask(videoElement) {
		// 1. 缩小到模型输入尺寸
		inputCtx.drawImage(videoElement, 0, 0, MASK_WIDTH, MASK_HEIGHT)
		const imgData = inputCtx.getImageData(0, 0, MASK_WIDTH, MASK_HEIGHT).data

		// 2. 归一化到 [0, 1]
		const float32Data = new Float32Array(MASK_WIDTH * MASK_HEIGHT * 3)
		for (let i = 0, j = 0; i < imgData.length; i += 4, j += 3) {
			float32Data[j] = imgData[i] / 255
			float32Data[j + 1] = imgData[i + 1] / 255
			float32Data[j + 2] = imgData[i + 2] / 255
		}

		// 3. 运行推理
		const inputTensor = new ort.Tensor('float32', float32Data, [1, MASK_HEIGHT, MASK_WIDTH, 3])
		const results = await session.run({ 'input_1:0': inputTensor })
		const maskData = results[Object.keys(results)[0]].data

		// 4. Softmax 归一化
		const mask = new Float32Array(MASK_WIDTH * MASK_HEIGHT)
		for (let i = 0; i < MASK_WIDTH * MASK_HEIGHT; i++) {
			const bg = maskData[i * 2]
			const fg = maskData[i * 2 + 1]
			const expSum = Math.exp(bg) + Math.exp(fg)
			mask[i] = Math.exp(fg) / expSum
		}

		return mask
	}

	// ========== 渲染循环 ==========
	async function processVideoFrame(videoElement) {
		if (!isProcessing.value || effectType.value === 'none') {
			animationFrameId = requestAnimationFrame(() => processVideoFrame(videoElement))
			return
		}

		try {
			// 1. 推理分割 Mask
			const mask = await inferSegmentationMask(videoElement)

			// 2. 获取原始视频帧
			outputCtx.drawImage(videoElement, 0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)
			const frameData = outputCtx.getImageData(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)

			// 3. 上传到 WASM
			const inputPtr = processor.input_ptr()
			const inputBuffer = new Uint8Array(wasmModule.memory.buffer, inputPtr, VIDEO_WIDTH * VIDEO_HEIGHT * 4)
			inputBuffer.set(frameData.data)

			const maskPtr = processor.mask_ptr()
			const maskBuffer = new Float32Array(wasmModule.memory.buffer, maskPtr, MASK_WIDTH * MASK_HEIGHT)
			maskBuffer.set(mask)

			// 4. WASM 处理
			processor.prepare_mask()

			if (effectType.value === 'blur') {
				processor.render_blur()
			} else if (effectType.value === 'replace') {
				processor.render_replace()
			}

			// 5. 读取处理后的数据
			const outputPtr = processor.output_ptr()
			const outputBuffer = new Uint8ClampedArray(wasmModule.memory.buffer, outputPtr, VIDEO_WIDTH * VIDEO_HEIGHT * 4)

			// 6. 渲染到 Canvas
			const processedData = new ImageData(outputBuffer.slice(), VIDEO_WIDTH, VIDEO_HEIGHT)
			outputCtx.putImageData(processedData, 0, 0)
		} catch (err) {
			console.error('Frame processing error:', err)
		}

		animationFrameId = requestAnimationFrame(() => processVideoFrame(videoElement))
	}

	// ========== 启动背景效果 ==========
	async function startEffect(videoTrack) {
		if (!isReady.value) {
			await initResources()
		}

		if (isProcessing.value) return

		try {
			originalVideoTrack = videoTrack
			isProcessing.value = true

			// 1. 如果是背景替换，加载背景图
			if (effectType.value === 'replace' && selectedBackground.value) {
				const bgUrl = allBackgrounds.value.find(bg => bg.id === selectedBackground.value)?.url

				if (bgUrl) {
					await loadBackgroundImage(bgUrl)
				}
			}

			// 2. 创建视频元素用于读帧
			const video = document.createElement('video')
			video.srcObject = new MediaStream([videoTrack])
			video.autoplay = true
			video.muted = true
			await video.play()

			// 3. 启动渲染循环
			processVideoFrame(video)

			// 4. 从 Canvas 创建新的视频流
			const canvasStream = outputCanvas.captureStream(30)
			processedStream = canvasStream

			return canvasStream.getVideoTracks()[0]
		} catch (err) {
			console.error('Failed to start effect:', err)
			isProcessing.value = false
			throw err
		}
	}

	// ========== 停止背景效果 ==========
	function stopEffect() {
		if (animationFrameId) {
			cancelAnimationFrame(animationFrameId)
			animationFrameId = null
		}

		if (processedStream) {
			processedStream.getTracks().forEach(track => track.stop())
			processedStream = null
		}

		isProcessing.value = false
	}

	// ========== 上传自定义背景 ==========
	async function uploadCustomBackground(file) {
		try {
			if (file.size > 5 * 1024 * 1024) {
				throw new Error('文件大小不能超过 5MB')
			}

			// 1. 读取文件
			const reader = new FileReader()
			const dataUrl = await new Promise((resolve, reject) => {
				reader.onload = e => resolve(e.target.result)
				reader.onerror = reject
				reader.readAsDataURL(file)
			})

			// 2. 生成缩略图
			const thumbnail = await generateThumbnail(dataUrl, 160, 90)

			// 3. 保存到 IndexedDB（或上传到服务器）
			const customBg = {
				id: `custom_${Date.now()}`,
				name: file.name,
				thumbnail,
				url: dataUrl,
				createdAt: new Date().toISOString(),
			}

			customBackgrounds.value.push(customBg)

			return customBg
		} catch (err) {
			console.error('Failed to upload background:', err)
			throw err
		}
	}

	// ========== 生成缩略图 ==========
	async function generateThumbnail(dataUrl, width, height) {
		return new Promise((resolve, reject) => {
			const img = new Image()
			img.src = dataUrl

			img.onload = () => {
				const canvas = document.createElement('canvas')
				canvas.width = width
				canvas.height = height
				const ctx = canvas.getContext('2d')

				const scale = Math.max(width / img.width, height / img.height)
				const x = (width - img.width * scale) / 2
				const y = (height - img.height * scale) / 2

				ctx.drawImage(img, x, y, img.width * scale, img.height * scale)
				resolve(canvas.toDataURL('image/jpeg', 0.7))
			}

			img.onerror = reject
		})
	}

	// ========== 切换效果类型 ==========
	watch(effectType, async (newType, oldType) => {
		if (newType === 'none') {
			stopEffect()
		} else if (oldType === 'none' && originalVideoTrack) {
			await startEffect(originalVideoTrack)
		} else if (newType === 'replace' && selectedBackground.value) {
			// 重新加载背景图
			const bgUrl = allBackgrounds.value.find(bg => bg.id === selectedBackground.value)?.url

			if (bgUrl) {
				await loadBackgroundImage(bgUrl)
			}
		}
	})

	// ========== 清理资源 ==========
	onUnmounted(() => {
		stopEffect()

		if (processor) {
			processor.free()
		}
	})

	return {
		// 状态
		effectType,
		selectedBackground,
		allBackgrounds,
		presetBackgrounds,
		customBackgrounds,
		isProcessing,
		isLoading,
		isReady,
		error,

		// 方法
		initResources,
		startEffect,
		stopEffect,
		uploadCustomBackground,
	}
}
