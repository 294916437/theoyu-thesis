import { defineConfig, loadEnv } from 'vite'
import { fileURLToPath, URL } from 'node:url'
import vueDevTools from 'vite-plugin-vue-devtools'
import vuetify from 'vite-plugin-vuetify'
import vue from '@vitejs/plugin-vue'
import basicSsl from '@vitejs/plugin-basic-ssl'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
	const env = loadEnv(mode, process.cwd(), '')

	const localIp = env.VITE_LOCAL_IP || '127.0.0.1'

	return {
		plugins: [vue(), vueDevTools(), vuetify({ autoImport: true }), basicSsl()],
		resolve: {
			alias: {
				'@': fileURLToPath(new URL('./src', import.meta.url)),
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
			// 确保 WASM 文件被正确处理
			rollupOptions: {
				external: [],
			},
			// 增加资源内联限制，防止 WASM 被内联
			assetsInlineLimit: 0,
		},
	}
})
