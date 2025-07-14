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
        proxy: {
            '/api2': {
                // target: 'http://223.2.47.202:8999/api/v1',
                // target: 'http://223.2.47.202:8999/api/v2',
                target: 'http://192.168.1.111:8999/api/v2',
                // target: 'http://223.2.43.228:30899/api/v1',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api2/, ''),
            },
            // 实时计算瓦片API专用代理（更具体的路径必须在前面）
            '/api/api/v1/realtime': {
                target: 'http://localhost:5001',
                changeOrigin: true,
            },
            // 实时计算会话API
            '/api/realtime': {
                target: 'http://localhost:5001',
                changeOrigin: true,
            },
            '/api': {
                // target: 'http://223.2.47.202:8999/api/v1',
                // target: 'http://223.2.47.202:8999/api/v1',
                target: 'http://192.168.1.111:8999/api/v1',
                // target: 'http://223.2.43.228:30899/api/v1',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, ''),
            },
            '/websocket': {
                // target: 'ws://223.2.47.202:8899/model/websocket',
                // target: 'http://223.2.43.228:30535/api/v1',
                target: 'http://192.168.1.111:8999/model/websocket',
                ws: true,
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/websocket/, ''),
            },
            '/tiler': {
                target: 'http://192.168.1.111:8000',
                // target: 'http://localhost:8000',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/tiler/, ''),
            },
            '/hytemp': {
                target: 'http://localhost:8000',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/hytemp/, ''),
            },
            '/basemap': {
                target: 'http://172.31.13.21:5001/tiles',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/basemap/, ''),
            },
            '/mvtbasemap': {
                target: 'http://172.31.13.21:5002/tiles',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/mvtbasemap/, ''),
            },
            '/proxymap': {
                target: 'http://localhost:5003',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/proxymap/, ''),
            },
            '/chry': {
                target: 'http://223.2.47.202:8999/api/v1/geo/vector/tiles',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/chry/, ''),
            },
        },
    },
})
