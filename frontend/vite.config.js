import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'
import vueDevTools from 'vite-plugin-vue-devtools'
import vuetify from 'vite-plugin-vuetify'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
	plugins: [vue(), vueDevTools(), vuetify({ autoImport: true })],
	resolve: {
		alias: {
			'@': fileURLToPath(new URL('./src', import.meta.url)),
		},
	},
	server: {
		proxy: {
			'/api': {
				target: 'http://127.0.0.1:8000',
				changeOrigin: true,
				rewrite: path => path.replace(/^\/api/, ''),
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
})
