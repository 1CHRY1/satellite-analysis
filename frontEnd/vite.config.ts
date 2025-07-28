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
            '/api3': {
                // target: 'http://localhost:8999/api/v3', // 本地开发
                // target: 'http://223.2.34.8:31584/api/v3', // 集群环境
                target: 'http://192.168.1.127:8999/api/v3', // HXF开发环境
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api3/, ''),
            },
            '/api2': {
                // target: 'http://localhost:8999/api/v2', // 本地开发
                // target: 'http://223.2.34.8:31584/api/v2', // 集群环境
                target: 'http://192.168.1.127:8999/api/v2', // HXF开发环境
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api2/, ''),
            },
            // TODO: Improvement
            // 实时计算瓦片API专用代理（更具体的路径必须在前面）
            '/api/api/v1/realtime': {
                target: 'http://192.168.1.127:5001',
                changeOrigin: true,
            },
            // 实时计算会话API
            '/api/realtime': {
                target: 'http://192.168.1.127:5001',
                changeOrigin: true,
            },
            '/api': {
                // target: 'http://localhost:8999/api/v1', // 本地开发
                // target: 'http://223.2.34.8:31584/api/v1', // 集群环境
                target: 'http://192.168.1.127:8999/api/v1', // HXF开发环境
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, ''),
            },
            '/websocket': {
                // target: 'http://localhost:8999/model/websocket', // 本地开发
                // target: 'http://223.2.34.8:30394/model/websocket', // 集群环境
                target: 'http://192.168.1.127:8999/model/websocket', // HXF开发环境
                ws: true,
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/websocket/, ''),
            },
            '/tiler': {
                // target: 'http://127.0.0.1:8000', // 本地开发
                // target: 'http://223.2.34.8:31800', // 集群环境(稳定版)
                target: 'http://192.168.1.127:31800', // HXF开发环境
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
