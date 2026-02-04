import { ref, computed, watch, onUnmounted } from 'vue'
import { useAsyncState } from '@vueuse/core'
import * as ort from 'onnxruntime-web'
import init, { MeetProcessor, init_panic_hook } from '@/libs/meet-effect/meet_background_effect.js'
import wasmUrl from '@/libs/meet-effect/meet_background_effect_bg.wasm?url'

const MASK_WIDTH = 256
const MASK_HEIGHT = 144
const VIDEO_WIDTH = 640
const VIDEO_HEIGHT = 480

export function useBackgroundEffect() {
	// ========== 状态管理 ==========
	const effectType = ref('none') // 'none' | 'blur' | 'replace'
	const selectedBackground = ref(null) // 预设或自定义背景 ID
	const customBackgrounds = ref([]) // 用户上传的背景列表

	const isProcessing = ref(false)
	const isLoading = ref(false)
	const error = ref(null)

	// 资源引用
	let wasmModule = null // 保存 init 返回的 module 实例
	let processor = null
	let session = null
	let processedStream = null
	let currentMask = null
	let isInferring = false

	// Canvas 上下文
	let sourceVideo = null
	let inputCanvas = null
	let outputCanvas = null
	let inputCtx = null
	let outputCtx = null

	let animationFrameId = null
	let inferenceTimeoutId = null

	// 内存缓存
	const float32Data = new Float32Array(MASK_WIDTH * MASK_HEIGHT * 3)
	let inputTensor = null

	// ========== 预设背景列表 ==========
	const presetBackgrounds = ref([
		{
			id: 'office',
			name: '现代办公室',
			// 使用 public 目录下的绝对路径
			thumbnail: '/backgrounds/stylish_home_office.jpg',
			url: '/backgrounds/stylish_home_office.jpg',
		},
		// 可以添加更多预设
	])

	const allBackgrounds = computed(() => [...presetBackgrounds.value, ...customBackgrounds.value])

	// ========== 初始化资源 ==========
	const {
		state: initState,
		isReady,
		execute: initResources,
	} = useAsyncState(
		async () => {
			console.log('[BackgroundEffect] Starting initialization...')

			if (processor && session) {
				return true
			}

			try {
				isLoading.value = true

				// 1. 初始化 WASM
				// init 函数直接接受 URL 字符串
				console.log('[BackgroundEffect] Initializing WASM from:', wasmUrl)
				wasmModule = await init(wasmUrl)
				init_panic_hook()

				processor = MeetProcessor.new(VIDEO_WIDTH, VIDEO_HEIGHT)
				console.log('[BackgroundEffect] WASM processor created')

				// 2. 加载 ONNX 模型
				// 对应 public/models/model_float32_opt.onnx
				const modelPath = '/models/model_float32_opt.onnx'
				console.log('[BackgroundEffect] Loading ONNX model:', modelPath)

				session = await ort.InferenceSession.create(modelPath, {
					executionProviders: ['wasm'], // 确保 node_modules 中的 onnxruntime wasm 文件已复制到 public
					graphOptimizationLevel: 'all',
				})
				console.log('[BackgroundEffect] ONNX session created')

				// 3. 创建 Canvas
				inputCanvas = new OffscreenCanvas(MASK_WIDTH, MASK_HEIGHT)
				outputCanvas = document.createElement('canvas')
				outputCanvas.width = VIDEO_WIDTH
				outputCanvas.height = VIDEO_HEIGHT

				inputCtx = inputCanvas.getContext('2d', { willReadFrequently: true, alpha: false })
				outputCtx = outputCanvas.getContext('2d', { willReadFrequently: true, alpha: false })

				return true
			} catch (err) {
				console.error('[BackgroundEffect] Initialization failed:', err)
				error.value = err.message || '特效组件初始化失败'
				throw err
			} finally {
				isLoading.value = false
			}
		},
		null,
		{ immediate: false },
	)

	// ========== 核心逻辑 (保持不变) ==========

	async function loopInference() {
		if (!isProcessing.value || !sourceVideo) return
		if (isInferring) {
			inferenceTimeoutId = setTimeout(loopInference, 30)
			return
		}

		try {
			isInferring = true

			// 绘制并获取数据
			inputCtx.drawImage(sourceVideo, 0, 0, MASK_WIDTH, MASK_HEIGHT)
			const imgData = inputCtx.getImageData(0, 0, MASK_WIDTH, MASK_HEIGHT).data

			// 归一化
			for (let i = 0, j = 0; i < imgData.length; i += 4, j += 3) {
				float32Data[j] = imgData[i] / 255
				float32Data[j + 1] = imgData[i + 1] / 255
				float32Data[j + 2] = imgData[i + 2] / 255
			}

			if (!inputTensor) {
				inputTensor = new ort.Tensor('float32', float32Data, [1, MASK_HEIGHT, MASK_WIDTH, 3])
			}

			// 运行推理
			const results = await session.run({ 'input_1:0': new ort.Tensor('float32', float32Data, [1, MASK_HEIGHT, MASK_WIDTH, 3]) })
			const maskData = results[Object.keys(results)[0]].data

			// 处理 Mask (Sigmoid/Softmax logic)
			const mask = new Float32Array(MASK_WIDTH * MASK_HEIGHT)
			for (let i = 0; i < MASK_WIDTH * MASK_HEIGHT; i++) {
				const bg = maskData[i * 2]
				const fg = maskData[i * 2 + 1]
				mask[i] = 1.0 / (1.0 + Math.exp(bg - fg))
			}
			currentMask = mask
		} catch (err) {
			console.error('Inference Error', err)
		} finally {
			isInferring = false
			inferenceTimeoutId = setTimeout(loopInference, 30)
		}
	}

	async function loadBackgroundImage(url) {
		return new Promise((resolve, reject) => {
			const img = new Image()
			img.crossOrigin = 'Anonymous'
			img.src = url
			img.onload = () => {
				const canvas = new OffscreenCanvas(VIDEO_WIDTH, VIDEO_HEIGHT)
				const ctx = canvas.getContext('2d')
				// Cover 模式填充
				const scale = Math.max(VIDEO_WIDTH / img.width, VIDEO_HEIGHT / img.height)
				const x = (VIDEO_WIDTH - img.width * scale) / 2
				const y = (VIDEO_HEIGHT - img.height * scale) / 2
				ctx.drawImage(img, x, y, img.width * scale, img.height * scale)

				const imageData = ctx.getImageData(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)

				if (processor && wasmModule) {
					const bgPtr = processor.background_ptr()
					const bgBuffer = new Uint8Array(wasmModule.memory.buffer, bgPtr, VIDEO_WIDTH * VIDEO_HEIGHT * 4)
					bgBuffer.set(imageData.data)
				}
				resolve(imageData)
			}
			img.onerror = reject
		})
	}

	function loopRender() {
		if (!isProcessing.value || !sourceVideo) return

		const type = effectType.value

		if (type === 'none' || !currentMask || !processor) {
			outputCtx.drawImage(sourceVideo, 0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)
		} else {
			try {
				outputCtx.drawImage(sourceVideo, 0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)
				const frameData = outputCtx.getImageData(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)

				// WASM 交互
				const inputPtr = processor.input_ptr()
				const inputBuffer = new Uint8Array(wasmModule.memory.buffer, inputPtr, VIDEO_WIDTH * VIDEO_HEIGHT * 4)
				inputBuffer.set(frameData.data)

				const maskPtr = processor.mask_ptr()
				const maskBuffer = new Float32Array(wasmModule.memory.buffer, maskPtr, MASK_WIDTH * MASK_HEIGHT)
				maskBuffer.set(currentMask)

				processor.prepare_mask()
				if (type === 'blur') processor.render_blur()
				else if (type === 'replace') processor.render_replace()

				const outputPtr = processor.output_ptr()
				const outputBuffer = new Uint8ClampedArray(wasmModule.memory.buffer, outputPtr, VIDEO_WIDTH * VIDEO_HEIGHT * 4)

				outputCtx.putImageData(new ImageData(outputBuffer.slice(), VIDEO_WIDTH, VIDEO_HEIGHT), 0, 0)
			} catch (e) {
				console.error('Render loop error', e)
			}
		}

		animationFrameId = requestAnimationFrame(loopRender)
	}

	// ========== 对外接口 ==========

	async function startEffect(videoTrack) {
		if (!isReady.value) {
			await initResources()
		}

		stopEffect(false)

		try {
			isProcessing.value = true

			sourceVideo = document.createElement('video')
			sourceVideo.autoplay = true
			sourceVideo.muted = true
			sourceVideo.playsInline = true
			sourceVideo.width = VIDEO_WIDTH
			sourceVideo.height = VIDEO_HEIGHT
			sourceVideo.srcObject = new MediaStream([videoTrack.clone()])

			await new Promise(resolve => {
				sourceVideo.onloadedmetadata = () => {
					sourceVideo.play()
					resolve()
				}
			})

			// 预加载当前选中的背景
			if (effectType.value === 'replace' && selectedBackground.value) {
				const bgItem = allBackgrounds.value.find(bg => bg.id === selectedBackground.value)
				if (bgItem) await loadBackgroundImage(bgItem.url)
			}

			loopInference()
			loopRender()

			processedStream = outputCanvas.captureStream(30)
			return processedStream.getVideoTracks()[0]
		} catch (err) {
			console.error('Failed to start effect:', err)
			isProcessing.value = false
			throw err
		}
	}

	function stopEffect(resetState = true) {
		if (animationFrameId) cancelAnimationFrame(animationFrameId)
		if (inferenceTimeoutId) clearTimeout(inferenceTimeoutId)

		if (sourceVideo) {
			sourceVideo.srcObject?.getTracks().forEach(t => t.stop())
			sourceVideo = null
		}
		if (resetState) isProcessing.value = false
	}

	watch(selectedBackground, async newVal => {
		if (effectType.value === 'replace' && newVal && isProcessing.value) {
			const bgItem = allBackgrounds.value.find(bg => bg.id === newVal)
			if (bgItem) await loadBackgroundImage(bgItem.url)
		}
	})

	async function uploadCustomBackground(file) {
		return new Promise((resolve, reject) => {
			const reader = new FileReader()
			reader.onload = async e => {
				const url = e.target.result
				const id = 'custom-' + Date.now()
				const newBg = {
					id,
					name: file.name,
					thumbnail: url, // 小图直接用原图 Base64
					url: url,
				}
				customBackgrounds.value.push(newBg)
				resolve(newBg)
			}
			reader.onerror = reject
			reader.readAsDataURL(file)
		})
	}

	onUnmounted(() => {
		stopEffect()
		if (processor) processor.free()
	})

	return {
		effectType,
		selectedBackground,
		allBackgrounds,
		isProcessing,
		isLoading,
		isReady,
		error,
		initResources,
		startEffect,
		stopEffect,
		uploadCustomBackground,
	}
}
