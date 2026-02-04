import { ref, computed, watch, onUnmounted } from 'vue'
import { useStorage, useAsyncState } from '@vueuse/core'
import * as ort from 'onnxruntime-web'

const MASK_WIDTH = 256
const MASK_HEIGHT = 144
const VIDEO_WIDTH = 640
const VIDEO_HEIGHT = 480

/**
 * 动态加载 WASM 模块
 */
async function loadWasmModule() {
	return new Promise((resolve, reject) => {
		// 检查全局是否已存在
		if (window.wasm_bindgen) {
			resolve(window.wasm_bindgen)
			return
		}
		const script = document.createElement('script')
		script.src = '/wasm/meet_background_effect.js'
		script.type = 'module'

		script.onload = () => {
			const checkModule = setInterval(() => {
				if (window.wasm_bindgen) {
					clearInterval(checkModule)
					resolve(window.wasm_bindgen)
				}
			}, 50)
		}
		script.onerror = () => reject(new Error('Failed to load WASM module'))
		document.head.appendChild(script)
	})
}

export function useBackgroundEffect() {
	// ========== 状态管理 ==========
	const effectType = useStorage('meeting-effect-type', 'none') // none | blur | replace
	const selectedBackground = useStorage('meeting-selected-bg', '')
	const customBackgrounds = useStorage('meeting-custom-bgs', [])

	const isProcessing = ref(false)
	const isLoading = ref(false)
	const error = ref(null)

	// 资源
	let wasmModule = null
	let processor = null
	let session = null
	let processedStream = null
	let currentMask = null
	let isInferring = false

	// Canvas 上下文
	let sourceVideo = null
	let inputCanvas = null // 用于缩放给 AI
	let outputCanvas = null // 用于最终输出
	let inputCtx = null
	let outputCtx = null
	let animationFrameId = null
	let inferenceTimeoutId = null

	// 缓存内存以减少 GC
	const float32Data = new Float32Array(MASK_WIDTH * MASK_HEIGHT * 3)
	let inputTensor = null

	// ========== 预设背景列表 ==========
	const presetBackgrounds = ref([
		{
			id: 'office',
			name: '办公室',
			thumbnail: '/assets/backgrounds/thumbnails/office_break_room.jpg',
			url: '/assets/backgrounds/office_break_room.jpg',
		},
		{
			id: 'home',
			name: '温馨家庭',
			thumbnail: '/assets/backgrounds/thumbnails/stylish_home_office.jpg',
			url: '/assets/backgrounds/stylish_home_office.jpg',
		},
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
				console.log('[BackgroundEffect] Resources already initialized')
				return true
			}

			try {
				isLoading.value = true
				console.log('[BackgroundEffect] Loading WASM module...')

				// 1. 加载 WASM
				const wasmBindgen = await loadWasmModule()
				console.log('[BackgroundEffect] WASM module loaded:', !!wasmBindgen)

				const { default: init, MeetProcessor, init_panic_hook } = wasmBindgen

				console.log('[BackgroundEffect] Initializing WASM...')
				await init('/wasm/meet_background_effect_bg.wasm')
				init_panic_hook()
				processor = MeetProcessor.new(VIDEO_WIDTH, VIDEO_HEIGHT)
				console.log('[BackgroundEffect] WASM processor created')

				// 2. 加载 ONNX 模型
				console.log('[BackgroundEffect] Loading ONNX model...')
				session = await ort.InferenceSession.create('/models/segmentation_model.onnx', {
					executionProviders: ['wasm'],
					graphOptimizationLevel: 'all',
				})
				console.log('[BackgroundEffect] ONNX session created')

				// 3. 创建 Canvas
				console.log('[BackgroundEffect] Creating canvas contexts...')
				inputCanvas = new OffscreenCanvas(MASK_WIDTH, MASK_HEIGHT)
				outputCanvas = document.createElement('canvas')
				outputCanvas.width = VIDEO_WIDTH
				outputCanvas.height = VIDEO_HEIGHT

				inputCtx = inputCanvas.getContext('2d', { willReadFrequently: true, alpha: false })
				outputCtx = outputCanvas.getContext('2d', { willReadFrequently: true, alpha: false })

				console.log('[BackgroundEffect] All resources initialized successfully')
				return true
			} catch (err) {
				console.error('[BackgroundEffect] Initialization failed:', err)
				console.error('[BackgroundEffect] Error stack:', err.stack)
				error.value = err.message || '初始化失败'
				throw err
			} finally {
				isLoading.value = false
			}
		},
		null,
		{ immediate: false }, // 保持 false，由组件显式调用
	)

	// ========== 核心逻辑 ==========

	// 1. 推理循环 (独立于渲染)
	async function loopInference() {
		if (!isProcessing.value || !sourceVideo) return
		if (isInferring) {
			inferenceTimeoutId = setTimeout(loopInference, 30)
			return
		}

		try {
			isInferring = true

			// 绘制小图
			inputCtx.drawImage(sourceVideo, 0, 0, MASK_WIDTH, MASK_HEIGHT)
			const imgData = inputCtx.getImageData(0, 0, MASK_WIDTH, MASK_HEIGHT).data

			// 归一化 (手动循环比 map 快)
			for (let i = 0, j = 0; i < imgData.length; i += 4, j += 3) {
				float32Data[j] = imgData[i] / 255
				float32Data[j + 1] = imgData[i + 1] / 255
				float32Data[j + 2] = imgData[i + 2] / 255
			}

			// 创建 Tensor (复用数据结构)
			if (!inputTensor) {
				inputTensor = new ort.Tensor('float32', float32Data, [1, MASK_HEIGHT, MASK_WIDTH, 3])
			}
			// 注意：ORT 目前可能不支持原地复用 Tensor 数据，这里简化处理，实际每次 new 开销尚可

			const results = await session.run({ 'input_1:0': new ort.Tensor('float32', float32Data, [1, MASK_HEIGHT, MASK_WIDTH, 3]) })
			const maskData = results[Object.keys(results)[0]].data

			// Softmax & 生成 Mask
			// 注意：WASM 那边接收的是 Float32Array
			// 这里我们可以在 JS 做 Softmax，也可以移到 Rust 做。这里保持原有 JS 逻辑但优化
			const mask = new Float32Array(MASK_WIDTH * MASK_HEIGHT)
			for (let i = 0; i < MASK_WIDTH * MASK_HEIGHT; i++) {
				const bg = maskData[i * 2]
				const fg = maskData[i * 2 + 1]
				// 简化 softmax: e^fg / (e^bg + e^fg) = 1 / (1 + e^(bg-fg))
				mask[i] = 1.0 / (1.0 + Math.exp(bg - fg))
			}
			currentMask = mask
		} catch (err) {
			console.error('Inference Error', err)
		} finally {
			isInferring = false
			inferenceTimeoutId = setTimeout(loopInference, 40) // ~25 FPS 推理
		}
	}

	// 2. 加载背景图到 WASM
	async function loadBackgroundImage(url) {
		return new Promise((resolve, reject) => {
			const img = new Image()
			img.crossOrigin = 'Anonymous'
			img.src = url
			img.onload = () => {
				const canvas = new OffscreenCanvas(VIDEO_WIDTH, VIDEO_HEIGHT)
				const ctx = canvas.getContext('2d')
				// 简单缩放填充
				ctx.drawImage(img, 0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)
				const imageData = ctx.getImageData(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)

				// 确保 processor 存在
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

	// 3. 渲染循环
	function loopRender() {
		if (!isProcessing.value || !sourceVideo) return

		const type = effectType.value

		if (type === 'none') {
			// 直通模式
			outputCtx.drawImage(sourceVideo, 0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)
		} else if (currentMask && processor) {
			// 有特效且 Mask 已就绪
			try {
				// 1. 获取原图
				outputCtx.drawImage(sourceVideo, 0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)
				const frameData = outputCtx.getImageData(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)

				// 2. 传入 WASM
				const inputPtr = processor.input_ptr()
				const inputBuffer = new Uint8Array(wasmModule.memory.buffer, inputPtr, VIDEO_WIDTH * VIDEO_HEIGHT * 4)
				inputBuffer.set(frameData.data)

				const maskPtr = processor.mask_ptr()
				const maskBuffer = new Float32Array(wasmModule.memory.buffer, maskPtr, MASK_WIDTH * MASK_HEIGHT)
				maskBuffer.set(currentMask)

				// 3. 处理
				processor.prepare_mask()
				if (type === 'blur') processor.render_blur()
				else if (type === 'replace') processor.render_replace()

				// 4.读取
				const outputPtr = processor.output_ptr()
				const outputBuffer = new Uint8ClampedArray(wasmModule.memory.buffer, outputPtr, VIDEO_WIDTH * VIDEO_HEIGHT * 4)

				outputCtx.putImageData(new ImageData(outputBuffer.slice(), VIDEO_WIDTH, VIDEO_HEIGHT), 0, 0)
			} catch (e) {
				console.error('Render loop error', e)
			}
		} else {
			// 降级：如果还在加载或出错，显示原图
			outputCtx.drawImage(sourceVideo, 0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)
		}

		animationFrameId = requestAnimationFrame(loopRender)
	}

	// ========== 对外接口 ==========
	async function startEffect(videoTrack) {
		console.log('[BackgroundEffect] startEffect called, isReady:', isReady.value)

		if (!isReady.value) {
			console.log('[BackgroundEffect] Initializing resources first...')
			await initResources()
		}

		// 检查初始化是否成功
		if (!isReady.value) {
			const errMsg = '资源初始化失败，无法启动效果'
			console.error('[BackgroundEffect]', errMsg)
			error.value = errMsg
			throw new Error(errMsg)
		}

		console.log('[BackgroundEffect] Getting WASM module reference...')
		wasmModule = await loadWasmModule()

		// 停止之前的
		stopEffect(false) // false = keep state

		try {
			isProcessing.value = true

			// 创建隐藏的 Video 元素作为源
			sourceVideo = document.createElement('video')
			sourceVideo.autoplay = true
			sourceVideo.muted = true
			sourceVideo.playsInline = true
			// 关键：必须设置宽高等待元数据
			sourceVideo.width = VIDEO_WIDTH
			sourceVideo.height = VIDEO_HEIGHT
			sourceVideo.srcObject = new MediaStream([videoTrack.clone()]) // clone track 防止被 stop

			await new Promise(resolve => {
				sourceVideo.onloadedmetadata = () => {
					sourceVideo.play()
					resolve()
				}
			})

			// 如果是替换，加载背景
			if (effectType.value === 'replace' && selectedBackground.value) {
				const bgItem = allBackgrounds.value.find(bg => bg.id === selectedBackground.value)
				if (bgItem) await loadBackgroundImage(bgItem.url)
			}

			// 启动循环
			loopInference()
			loopRender()

			// 生成流 (FPS 30)
			processedStream = outputCanvas.captureStream(30)

			// 处理原音轨（如果需要合并的话，但这里只处理视频）
			// 注意：captureStream 出来的 track id 会变，上一层业务需要处理替换

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

		// 我们不在这里 stop processedStream，因为这个 track 可能已经交给 MediaSoup 了
		// 应该由业务层决定何时停止使用这个 track

		if (resetState) isProcessing.value = false
	}

	// 监听类型变化自动切换背景图
	watch(selectedBackground, async newVal => {
		if (effectType.value === 'replace' && newVal && isProcessing.value) {
			const bgItem = allBackgrounds.value.find(bg => bg.id === newVal)
			if (bgItem) await loadBackgroundImage(bgItem.url)
		}
	})

	// ... uploadCustomBackground 等辅助函数保持不变 ...
	async function uploadCustomBackground(file) {
		// ... (保持原样) ...
		return { id: 'mock', url: '' } // 仅占位，需补全
	}

	onUnmounted(() => {
		stopEffect()
		if (processor) processor.free()
		// session 释放...
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
