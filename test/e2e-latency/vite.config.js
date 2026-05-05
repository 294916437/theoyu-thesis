import { defineConfig } from "vite"
import { resolve } from "path"

export default defineConfig({
	server: {
		port: 5200,
	},
	build: {
		rollupOptions: {
			input: {
				index: resolve(__dirname, "index.html"),
				"sender-main": resolve(__dirname, "sender-main.html"),
				sender: resolve(__dirname, "sender.html"),
				receiver: resolve(__dirname, "receiver.html"),
			},
		},
	},
})
