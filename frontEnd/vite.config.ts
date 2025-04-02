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
    server: {
        host: '0.0.0.0',
        // proxy: {
        //     '/tiles': {
        //         target: 'http://223.2.32.242:8079',
        //         changeOrigin: true,
        //         rewrite: (path) => path.replace(/^\/tiles/, ''),
        //     },
        // },
        proxy: {
            '/api': {
                // target: 'http://223.2.47.202:8999/api/v1',
                target: 'http://223.2.47.202:9888/api/v1',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, ''),
            },
        },
    },
})
