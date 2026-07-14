<template>
	<div class="hero-section">
		<!-- PixiJS 动画容器 -->
		<div ref="pixiContainer" class="pixi-background"></div>

		<v-container class="py-12 position-relative z-1">
			<v-row align="center" justify="center">
				<v-col cols="12" lg="10" xl="8">
					<!-- 欢迎横幅 -->
					<div class="welcome-content text-center mb-8">
						<v-icon size="80" color="white" class="welcome-icon mb-4"> mdi-video-account </v-icon>
						<h1 class="text-h3 text-md-h2 font-weight-bold text-white mb-3">{{ greeting }}, {{ userName }}!</h1>
						<p class="text-h6 text-white text-opacity-90 mb-4">创建或加入会议,与团队保持联系</p>
					</div>

					<!-- 快速操作 -->
					<v-row justify="center">
						<v-col cols="12" md="10" lg="8">
							<v-card elevation="12" rounded="xl" class="action-card pa-6">
								<v-row>
									<!-- 创建会议 -->
									<v-col cols="12" sm="6">
										<v-card elevation="0" class="action-item pa-6 text-center" color="primary" rounded="lg" @click="emit('create-meeting')">
											<v-icon size="56" color="white" class="mb-4"> mdi-video-plus </v-icon>
											<h3 class="text-h6 text-white font-weight-bold mb-2">创建会议</h3>
											<p class="text-body-2 text-white text-opacity-80">立即开始新的视频会议</p>
										</v-card>
									</v-col>

									<!-- 加入会议 -->
									<v-col cols="12" sm="6">
										<v-card elevation="0" class="action-item pa-6" color="surface-variant" rounded="lg">
											<div class="text-center mb-4">
												<v-icon size="56" color="primary" class="mb-4"> mdi-login </v-icon>
												<h3 class="text-h6 font-weight-bold mb-2">加入会议</h3>
											</div>

											<v-text-field
												v-model="meetingId"
												label="输入会议ID"
												variant="outlined"
												density="comfortable"
												color="primary"
												hide-details
												clearable
												@keyup.enter="handleJoin"
											>
												<template #prepend-inner>
													<v-icon size="20">mdi-pound</v-icon>
												</template>
												<template #append-inner>
													<v-btn icon size="small" color="primary" :disabled="!meetingId" @click="handleJoin">
														<v-icon>mdi-arrow-right</v-icon>
													</v-btn>
												</template>
											</v-text-field>
										</v-card>
									</v-col>
								</v-row>
							</v-card>
						</v-col>
					</v-row>
				</v-col>
			</v-row>
		</v-container>
	</div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useNow, useMouse, useWindowSize } from '@vueuse/core'
import { Application, Graphics, Container, BlurFilter } from 'pixi.js'

defineProps({
	userName: {
		type: String,
		default: '用户',
	},
})

const emit = defineEmits(['join-meeting', 'create-meeting'])

const now = useNow()
const meetingId = ref('')
const pixiContainer = ref(null)
let app = null

const { x: mouseX, y: mouseY } = useMouse()
const { width: windowWidth, height: windowHeight } = useWindowSize()

const greeting = computed(() => {
	const hour = now.value.getHours()
	if (hour < 12) return '早上好'
	if (hour < 18) return '下午好'
	return '晚上好'
})

const handleJoin = () => {
	if (meetingId.value && meetingId.value.trim()) {
		emit('join-meeting', meetingId.value.trim())
		meetingId.value = ''
	}
}

// 创建单朵云图形
function createCloudGraphic() {
	const cloud = new Graphics()
	cloud.ellipse(0, 0, 70, 32)
	cloud.ellipse(35, -18, 55, 38)
	cloud.ellipse(-28, -12, 48, 28)
	cloud.ellipse(45, 8, 42, 22)
	cloud.ellipse(-10, -28, 38, 22)
	cloud.fill({ color: 0xffffff, alpha: 0.55 + Math.random() * 0.35 })
	const blurFilter = new BlurFilter({ strength: 3, quality: 4 })
	cloud.filters = [blurFilter]
	return cloud
}

// 创建单只飞鸟图形
function createBirdGraphic() {
	const bird = new Graphics()
	bird.moveTo(-30, -15)
	bird.quadraticCurveTo(-15, -25, 0, 0)
	bird.quadraticCurveTo(15, -25, 30, -15)
	bird.stroke({ color: 0xffffff, alpha: 0.7 + Math.random() * 0.3, width: 3, cap: 'round', join: 'round' })
	return bird
}

const initPixiAnimation = async () => {
	app = new Application()

	await app.init({
		resizeTo: pixiContainer.value,
		backgroundAlpha: 0,
		antialias: true,
	})

	pixiContainer.value.appendChild(app.canvas)

	// 图层顺序：背景场景 → 天空元素(视差)
	const sceneContainer = new Container()
	const skyContainer = new Container()

	app.stage.addChild(sceneContainer)
	app.stage.addChild(skyContainer)

	// ── 图层拆分：静态/慢变/动态 分层管理 ──────────────────────────────
	// oceanBgGraphics   : 海洋色块      → 仅 resize 时重绘
	// waveGraphics      : 波纹 + 高光   → 每帧，但减少采样点
	// foamGraphics      : 破碎浪泡沫    → 每帧，步长加大
	// beachBgGraphics   : 沙滩色块      → 仅 resize 时重绘
	// transitionGraphics: 水沙过渡带    → 节流 ~20fps
	// beachLineGraphics : 沙滩纹理线    → 节流 ~12fps
	// beachDetailGraphics: 贝壳/脚印   → 一次性
	const oceanBgGraphics = new Graphics()
	const waveGraphics = new Graphics()
	const foamGraphics = new Graphics()
	const beachBgGraphics = new Graphics()
	const transitionGraphics = new Graphics()
	const beachLineGraphics = new Graphics()
	const beachDetailGraphics = new Graphics()

	sceneContainer.addChild(oceanBgGraphics)
	sceneContainer.addChild(waveGraphics)
	sceneContainer.addChild(foamGraphics)
	sceneContainer.addChild(beachBgGraphics)
	sceneContainer.addChild(transitionGraphics)
	sceneContainer.addChild(beachLineGraphics)
	sceneContainer.addChild(beachDetailGraphics)

	// 白云
	const clouds = []
	for (let i = 0; i < 10; i++) {
		const cloud = createCloudGraphic()
		cloud.x = Math.random() * app.screen.width
		cloud.y = Math.random() * (app.screen.height * 0.42)
		cloud.scale.set(0.5 + Math.random() * 0.9)
		const speed = 0.12 + Math.random() * 0.22
		skyContainer.addChild(cloud)
		clouds.push({ sprite: cloud, speed })
	}

	// 飞鸟
	const birds = []
	for (let i = 0; i < 7; i++) {
		const bird = createBirdGraphic()
		bird.x = Math.random() * app.screen.width
		bird.y = Math.random() * (app.screen.height * 0.38)
		const baseScale = 0.4 + Math.random() * 0.55
		bird.scale.set(baseScale)
		const speed = 0.7 + Math.random() * 1.6
		skyContainer.addChild(bird)
		birds.push({ sprite: bird, speed, baseScaleY: baseScale, timer: Math.random() * Math.PI * 2 })
	}

	let beachDetailInited = false
	let time = 0

	// 节流时间戳
	let lastTransitionRedraw = -999
	let lastBeachLineRedraw = -999
	const TRANSITION_INTERVAL = 0.05 // 水沙过渡带 ~20fps
	const BEACH_LINE_INTERVAL = 0.083 // 沙滩纹理线 ~12fps

	// 缓存尺寸，用于 resize 检测
	let cachedW = -1
	let cachedH = -1

	app.ticker.add(ticker => {
		const dt = ticker.deltaTime
		time += 0.016 * dt

		const w = app.screen.width
		const h = app.screen.height

		const horizonY = h * 0.52
		const shoreY = h * 0.76
		const beachTopY = shoreY

		// 1. 海洋色块
		const sizeChanged = w !== cachedW || h !== cachedH
		if (sizeChanged) {
			cachedW = w
			cachedH = h

			oceanBgGraphics.clear()

			// 远海（深蓝）
			oceanBgGraphics.moveTo(0, horizonY)
			oceanBgGraphics.lineTo(w, horizonY)
			oceanBgGraphics.lineTo(w, horizonY + (shoreY - horizonY) * 0.3)
			oceanBgGraphics.lineTo(0, horizonY + (shoreY - horizonY) * 0.3)
			oceanBgGraphics.fill({ color: 0x1a4a7a, alpha: 0.92 })

			// 中海（中蓝）
			oceanBgGraphics.moveTo(0, horizonY + (shoreY - horizonY) * 0.28)
			oceanBgGraphics.lineTo(w, horizonY + (shoreY - horizonY) * 0.28)
			oceanBgGraphics.lineTo(w, horizonY + (shoreY - horizonY) * 0.62)
			oceanBgGraphics.lineTo(0, horizonY + (shoreY - horizonY) * 0.62)
			oceanBgGraphics.fill({ color: 0x2e6daf, alpha: 0.9 })

			// 近海（浅蓝绿）
			oceanBgGraphics.moveTo(0, horizonY + (shoreY - horizonY) * 0.6)
			oceanBgGraphics.lineTo(w, horizonY + (shoreY - horizonY) * 0.6)
			oceanBgGraphics.lineTo(w, shoreY)
			oceanBgGraphics.lineTo(0, shoreY)
			oceanBgGraphics.fill({ color: 0x4d9fd6, alpha: 0.88 })

			// 沙滩色块也随尺寸一起重绘
			beachBgGraphics.clear()

			// 湿沙区
			beachBgGraphics.moveTo(-10, beachTopY + 8)
			for (let x = 0; x <= w + 20; x += 20) {
				beachBgGraphics.lineTo(x, beachTopY + 8 + Math.sin(x * 0.015) * 4)
			}
			beachBgGraphics.lineTo(w + 20, beachTopY + h * 0.09)
			beachBgGraphics.lineTo(-10, beachTopY + h * 0.09)
			beachBgGraphics.fill({ color: 0xc8b89a, alpha: 1 })

			// 干沙区
			beachBgGraphics.moveTo(-10, beachTopY + h * 0.07)
			beachBgGraphics.lineTo(w + 20, beachTopY + h * 0.07)
			beachBgGraphics.lineTo(w + 20, h + 10)
			beachBgGraphics.lineTo(-10, h + 10)
			beachBgGraphics.fill({ color: 0xe8d5b0, alpha: 1 })

			// resize 后需重新初始化贝壳细节
			beachDetailInited = false
			beachDetailGraphics.clear()
		}

		// 2. 波纹 + 高光
		waveGraphics.clear()

		const waveLayerCount = 10
		for (let i = 0; i < waveLayerCount; i++) {
			const t = i / (waveLayerCount - 1)
			const tNL = Math.pow(t, 1.6)
			const waveY = horizonY + tNL * (shoreY - horizonY)
			const amp = 2 + t * 14
			const freq = 0.012 - t * 0.007
			const alpha = 0.18 + t * 0.45
			const lw = 0.8 + t * 2.5

			waveGraphics.moveTo(0, waveY + Math.sin(time * (1.2 - t * 0.5)) * amp)
			for (let x = 18; x <= w; x += 18) {
				waveGraphics.lineTo(x, waveY + Math.sin(time * (1.2 - t * 0.5) + x * freq) * amp + Math.sin(time * 0.7 + x * freq * 1.7 + i) * amp * 0.4)
			}
			waveGraphics.stroke({ color: 0xaaddff, alpha, width: lw })
		}

		// 高光光斑
		const glintCount = 16
		for (let i = 0; i < glintCount; i++) {
			const gx = (Math.sin(i * 37.13) * 0.5 + 0.5) * w
			const gyRatio = Math.sin(i * 53.77) * 0.5 + 0.5
			const gy = horizonY + gyRatio * (shoreY - horizonY)
			const glintAlpha = (0.4 + Math.sin(time * 3.1 + i * 1.7) * 0.35) * gyRatio
			const glintSize = (1 + gyRatio * 3) * (0.6 + Math.sin(time * 2 + i) * 0.4)
			if (glintAlpha > 0.08 && glintSize > 0.5) {
				waveGraphics.ellipse(gx, gy, glintSize * 3, glintSize * 0.8)
				waveGraphics.fill({ color: 0xffffff, alpha: Math.min(glintAlpha, 0.7) })
			}
		}

		// 3. 破碎浪泡沫
		foamGraphics.clear()

		// 退潮渗透过渡层
		foamGraphics.moveTo(0, shoreY - 4)
		for (let x = 0; x <= w; x += 14) {
			foamGraphics.lineTo(x, shoreY - 4 + Math.sin(time * 1.0 + x * 0.016) * 5)
		}
		for (let x = w; x >= 0; x -= 14) {
			foamGraphics.lineTo(x, shoreY + 28 + Math.sin(time * 0.7 + x * 0.019) * 5)
		}
		foamGraphics.fill({ color: 0x7ec8e8, alpha: 0.32 })

		// 破碎浪主体
		foamGraphics.moveTo(0, shoreY + Math.sin(time * 1.1) * 5)
		for (let x = 0; x <= w; x += 14) {
			foamGraphics.lineTo(x, shoreY + Math.sin(time * 1.1 + x * 0.018) * 6 + Math.sin(time * 2.3 + x * 0.031) * 3)
		}
		for (let x = w; x >= 0; x -= 14) {
			foamGraphics.lineTo(x, shoreY + 14 + Math.sin(time * 0.9 + x * 0.022) * 4)
		}
		foamGraphics.fill({ color: 0xdff3ff, alpha: 0.72 })

		// 浪沫小泡
		for (let fi = 0; fi < 16; fi++) {
			const fx = (Math.sin(fi * 41.3 + time * 0.4) * 0.5 + 0.5) * w
			const fy = shoreY + 2 + (Math.sin(fi * 29.7 + time * 0.6) * 0.5 + 0.5) * 14
			const fr = 1.5 + Math.sin(fi + time * 2) * 1.2
			const fa = 0.5 + Math.sin(fi * 2.1 + time * 1.5) * 0.35
			if (fr > 0.3) {
				foamGraphics.circle(fx, fy, Math.max(fr, 0.3))
				foamGraphics.fill({ color: 0xffffff, alpha: Math.max(fa, 0) })
			}
		}

		// 4. 水沙过渡带
		if (time - lastTransitionRedraw > TRANSITION_INTERVAL) {
			lastTransitionRedraw = time
			transitionGraphics.clear()
			for (let ti = 0; ti < 6; ti++) {
				const tRatio = ti / 5
				const bandY = beachTopY + 4 + tRatio * 18
				transitionGraphics.moveTo(0, bandY + Math.sin(ti * 1.3 + time * 0.4) * 3)
				for (let x = 0; x <= w + 20; x += 24) {
					transitionGraphics.lineTo(x, bandY + Math.sin(x * 0.014 + ti * 1.3 + time * 0.4) * 4)
				}
				const bandColor = tRatio < 0.5 ? 0xa8cfe0 : 0xc0b090
				transitionGraphics.stroke({ color: bandColor, alpha: 0.28 - tRatio * 0.12, width: 4 + tRatio * 3 })
			}
		}

		// 5. 沙滩纹理线
		if (time - lastBeachLineRedraw > BEACH_LINE_INTERVAL) {
			lastBeachLineRedraw = time
			beachLineGraphics.clear()
			for (let si = 0; si < 12; si++) {
				const t2 = si / 11
				const sy = beachTopY + 12 + t2 * (h - beachTopY - 12)
				const lineAlpha = 0.08 + t2 * 0.1
				const lineWidth = 0.5 + t2 * 1.5
				beachLineGraphics.moveTo(-10, sy + Math.sin(sy * 0.03 + time * 0.15) * (2 + t2 * 6))
				for (let x = 0; x <= w + 20; x += 28) {
					beachLineGraphics.lineTo(x, sy + Math.sin(x * 0.012 + sy * 0.025 + time * 0.15) * (2 + t2 * 6))
				}
				beachLineGraphics.stroke({ color: 0xa08060, alpha: lineAlpha, width: lineWidth })
			}
		}

		// 6. 沙滩细节（贝壳、小石子、脚印）—— 一次性绘制
		if (!beachDetailInited && w > 0 && h > 0) {
			beachDetailInited = true
			beachDetailGraphics.clear()

			const shellCount = 22
			for (let si = 0; si < shellCount; si++) {
				const sx = 0.02 * w + Math.random() * w * 0.96
				const sy = beachTopY + 10 + Math.random() * (h - beachTopY - 20)
				const sr = 2 + Math.random() * 4
				const isShell = Math.random() > 0.45

				if (isShell) {
					beachDetailGraphics.ellipse(sx, sy, sr * 1.8, sr)
					beachDetailGraphics.fill({ color: 0xf5e6d0, alpha: 0.85 })
					beachDetailGraphics.ellipse(sx, sy, sr * 1.8, sr)
					beachDetailGraphics.stroke({ color: 0xc8a878, alpha: 0.5, width: 0.6 })
					for (let li = 1; li <= 3; li++) {
						const lx = sx - sr * 1.4 + (li / 4) * sr * 2.8
						beachDetailGraphics.moveTo(lx, sy - sr * 0.7)
						beachDetailGraphics.lineTo(lx, sy + sr * 0.7)
						beachDetailGraphics.stroke({ color: 0xb89060, alpha: 0.35, width: 0.5 })
					}
				} else {
					const stoneColor = [0xb0a090, 0x9a8878, 0xccc0b0][Math.floor(Math.random() * 3)]
					beachDetailGraphics.ellipse(sx, sy, sr * (0.8 + Math.random() * 0.5), sr * (0.6 + Math.random() * 0.5))
					beachDetailGraphics.fill({ color: stoneColor, alpha: 0.75 })
				}
			}

			const footprintCount = 6
			for (let fi = 0; fi < footprintCount; fi++) {
				const fpx = 0.05 * w + Math.random() * w * 0.9
				const fpy = beachTopY + 25 + Math.random() * (h - beachTopY - 50)
				const fSize = 3 + Math.random() * 3
				beachDetailGraphics.ellipse(fpx - fSize * 0.7, fpy, fSize * 0.65, fSize * 1.4)
				beachDetailGraphics.fill({ color: 0xc0a87a, alpha: 0.45 })
				beachDetailGraphics.ellipse(fpx + fSize * 0.7, fpy + fSize * 2, fSize * 0.65, fSize * 1.4)
				beachDetailGraphics.fill({ color: 0xc0a87a, alpha: 0.45 })
			}
		}

		// 7. 白云动画
		clouds.forEach(c => {
			c.sprite.x += c.speed * dt
			if (c.sprite.x - c.sprite.width * c.sprite.scale.x > app.screen.width) {
				c.sprite.x = -c.sprite.width * c.sprite.scale.x - 60
				c.sprite.y = Math.random() * (app.screen.height * 0.42)
				c.sprite.scale.set(0.5 + Math.random() * 0.9)
			}
		})

		// 8. 飞鸟动画
		birds.forEach(b => {
			b.sprite.x += b.speed * dt
			b.timer += 0.07 * dt
			b.sprite.y += Math.sin(b.timer * 0.45) * 0.55
			b.sprite.scale.y = b.baseScaleY * (1 + Math.sin(b.timer * 2.8) * 0.38)

			if (b.sprite.x - b.sprite.width * 1.5 > app.screen.width) {
				b.sprite.x = -b.sprite.width - 60
				b.sprite.y = Math.random() * (app.screen.height * 0.38)
			}
		})

		// 9. 视差效果
		const targetX = (mouseX.value / windowWidth.value - 0.5) * -45
		const targetY = (mouseY.value / windowHeight.value - 0.5) * -25
		skyContainer.x += (targetX - skyContainer.x) * 0.05
		skyContainer.y += (targetY - skyContainer.y) * 0.05
	})
}

onMounted(() => {
	initPixiAnimation()
})

onBeforeUnmount(() => {
	if (app) {
		app.destroy(true, { children: true, texture: true, baseTexture: true })
	}
})
</script>

<style scoped>
.hero-section {
	position: relative;
	/* 天空渐变：从天蓝顶部到接近地平线的浅蓝白 */
	background: linear-gradient(180deg, #5ba3d9 0%, #8dc8f0 40%, #b8dff5 70%, #d8eefc 100%);
	overflow: hidden;
	min-height: 100%;
}

.pixi-background {
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	z-index: 0;
	pointer-events: none;
}

.z-1 {
	z-index: 1;
}

.welcome-icon {
	animation: float 3s ease-in-out infinite;
}

@keyframes float {
	0%,
	100% {
		transform: translateY(0);
	}
	50% {
		transform: translateY(-12px);
	}
}

.action-card {
	background: rgba(255, 255, 255, 0.82);
	backdrop-filter: blur(18px);
}

.action-item {
	cursor: pointer;
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	height: 100%;
}

.action-item:hover {
	transform: translateY(-8px);
	box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15) !important;
}

.action-item:active {
	transform: translateY(-4px);
}

@media (max-width: 600px) {
	.hero-section {
		padding-top: 2rem;
		padding-bottom: 2rem;
	}

	.action-item {
		margin-bottom: 1rem;
	}
}
</style>
