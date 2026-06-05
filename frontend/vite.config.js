import { defineConfig, loadEnv } from 'vite'
import { createRequire } from 'node:module'
import { dirname, join } from 'node:path'
import { fileURLToPath, URL } from 'node:url'
import vuetify from 'vite-plugin-vuetify'
import vue from '@vitejs/plugin-vue'
import basicSsl from '@vitejs/plugin-basic-ssl'

const require = createRequire(import.meta.url)
const onnxRuntimeWebDistDir = dirname(require.resolve('onnxruntime-web/wasm'))

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
	const env = loadEnv(mode, process.cwd(), '')
	const isProd = mode === 'production'

	const localIp = env.VITE_LOCAL_IP || '127.0.0.1'

	return {
		plugins: [vue(), vuetify({ autoImport: true }), basicSsl()],
		resolve: {
			alias: {
				'@': fileURLToPath(new URL('./src', import.meta.url)),
				'@ort-wasm-url': `${join(onnxRuntimeWebDistDir, 'ort-wasm-simd-threaded.wasm')}?url`,
			},
		},
		server: {
			host: true, // 监听 0.0.0.0，局域网可访问
			https: true, // 启用 HTTPS，保证局域网访问时 navigator.mediaDevices 可用（安全上下文）
			proxy: {
				'/api': {
					target: `http://${localIp}:8000`,
					changeOrigin: true,
					rewrite: path => path.replace(/^\/api/, ''),
				},
				// 将 socket.io WebSocket 请求代理到 SFU 服务
				// 浏览器只看到 wss://frontend，Vite 内部转发到 ws://SFU，避免混合内容错误
				'/socket.io': {
					target: `http://${localIp}:3000`,
					changeOrigin: true,
					ws: true, // 开启 WebSocket 代理
				},
				// Spring Cloud Gateway 的 WebSocket 路径代理
				// 覆盖 message/ws/* 和 media/ws/* 三条 STOMP/WS 连接
				'/message/ws': {
					target: `http://${localIp}:8000`,
					changeOrigin: true,
					ws: true,
				},
				'/media/ws': {
					target: `http://${localIp}:8000`,
					changeOrigin: true,
					ws: true,
				},
			},
		},
		build: {
			// 针对支持 ESM 的现代浏览器，减少 polyfill 体积
			target: 'es2020',
			// 关闭 gzip 体积报告，加快构建速度
			reportCompressedSize: false,
			// chunk 超过 1MB 才警告
			chunkSizeWarningLimit: 1000,
			rollupOptions: {
				output: {
					// 将大型第三方库拆分为独立 chunk，利用浏览器并行加载与长期缓存
					manualChunks: id => {
						// WebRTC / 媒体相关
						if (id.includes('mediasoup-client') || id.includes('socket.io-client')) {
							return 'vendor-media'
						}
						// Canvas 渲染
						if (id.includes('pixi.js') || id.includes('@pixi/')) {
							return 'vendor-pixi'
						}
						// UI 框架
						if (id.includes('vuetify') || id.includes('@mdi/font')) {
							return 'vendor-vuetify'
						}
						// Vue 核心生态
						if (id.includes('node_modules/vue') || id.includes('node_modules/pinia') || id.includes('vue-router') || id.includes('@vueuse/')) {
							return 'vendor-vue'
						}
						// 其余 node_modules 合并为公共 vendor
						if (id.includes('node_modules')) {
							return 'vendor-misc'
						}
					},
				},
			},
			// 确保 WASM 文件被正确处理，禁止内联
			assetsInlineLimit: isProd ? 4096 : 0,
		},
	}
})
