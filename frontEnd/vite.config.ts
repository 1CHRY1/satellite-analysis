import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import tailwindcss from '@tailwindcss/vite'
import { ENV_CONFIG, FIXED_CONFIG } from './env.config'

// 环境配置
// const ENV = process.env.NODE_ENV || 'development'



const ENV_TARGET = 'slk' // 一键切换配置环境，使用集群则改为cluster，使用本地则改为local


// 获取当前环境配置
const currentEnv = ENV_CONFIG[ENV_TARGET as keyof typeof ENV_CONFIG] || ENV_CONFIG.zzw

// 代理配置生成器
const createProxyConfig = () => {
    return {
        // API v3
        // 过程：把/api3/user重写成/user拼接到target后，changeOrigin让浏览器发出的请求头 Host 变成 target 的 host。
        '/api3': {
            target: `${currentEnv.api}/api/v3`,
            changeOrigin: true,
            rewrite: (path: string) => path.replace(/^\/api3/, ''),
        },
        // API v2
        '/api2': {
            target: `${currentEnv.api}/api/v2`,
            changeOrigin: true,
            rewrite: (path: string) => path.replace(/^\/api2/, ''),
        },
        // 实时计算瓦片API专用代理（更具体的路径必须在前面）
        '/api/api/v1/realtime': {
            target: currentEnv.realtime,
            changeOrigin: true,
        },
        // 实时计算会话API
        '/api/realtime': {
            target: currentEnv.realtime,
            changeOrigin: true,
        },
        // API v1
        '/api': {
            target: `${currentEnv.api}/api/v1`,
            changeOrigin: true,
            rewrite: (path: string) => path.replace(/^\/api/, ''),
        },
        // WebSocket
        '/websocket': {
            target: currentEnv.websocket,
            ws: true,
            changeOrigin: true,
            rewrite: (path: string) => path.replace(/^\/websocket/, ''),
        },
        // Tiler
        '/tiler': {
            target: currentEnv.tiler,
            changeOrigin: true,
            secure: false,
            rewrite: (path: string) => path.replace(/^\/tiler/, ''),
        },
        '/minio': {
            target: currentEnv.minio,
            changeOrigin: true,
            rewrite: (path: string) => path.replace(/^\/minio/, ''),
        },
        '/model': {
            target: currentEnv.modelServer,
            changeOrigin: true,
            rewrite: (path: string) => path.replace(/^\/model/, ''),
        },
        // 固定配置（不随环境变化）
        // '/hytemp': {
        //     target: currentEnv.hytemp,
        //     changeOrigin: true,
        //     rewrite: (path: string) => path.replace(/^\/hytemp/, ''),
        // },
        '/basemap': {
            target: FIXED_CONFIG.basemap,
            changeOrigin: true,
            rewrite: (path: string) => path.replace(/^\/basemap/, ''),
        },
        '/mvtbasemap': {
            target: FIXED_CONFIG.mvtbasemap,
            changeOrigin: true,
            rewrite: (path: string) => path.replace(/^\/mvtbasemap/, ''),
        },
        '/proxymap': {
            target: currentEnv.proxymap,
            changeOrigin: true,
            rewrite: (path: string) => path.replace(/^\/proxymap/, ''),
        },
        '/demtiles': {
            target: FIXED_CONFIG.demtiles,
            changeOrigin: true,
            rewrite: (path: string) => path.replace(/^\/demtiles/, ''),
        },
        // '/chry': {
        //     target: FIXED_CONFIG.chry,
        //     changeOrigin: true,
        //     rewrite: (path: string) => path.replace(/^\/chry/, ''),
        // },
    }
}

// 打印当前环境信息
console.log(`🚀 当前环境: ${ENV_TARGET.toUpperCase()}`)
console.log(`📡 API基础URL: ${currentEnv.api}`)

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
        proxy: createProxyConfig(),
    },
})
