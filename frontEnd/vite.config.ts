import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
    plugins: [
        vue(),
        tailwindcss(),
    ],
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src'),
        },
    },
    server: {
        host: '0.0.0.0',
        proxy: {
            '/api': {
                target: 'http://223.2.47.202:8999/api/v1',
                // target: 'http://223.2.43.228:30535/api/v1',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, ''),
            },
            '/websocket': {
                target: 'ws://223.2.47.202:9000/model/websocket',
                // target: 'http://223.2.43.228:30535/api/v1',
                ws: true,
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/websocket/, ''),
            },
            '/basemap': {
                target: 'http://172.31.13.21:5001/tiles',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/basemap/, ''),
            },
        },
    },
})
