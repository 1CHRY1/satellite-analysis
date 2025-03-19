import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
	plugins: [vue(), tailwindcss()],
	resolve: {
		alias: {
			'@': resolve(__dirname, 'src'),
		},
	},
})
