import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import tailwindcss from '@tailwindcss/vite'
// import { visualizer } from 'rollup-plugin-visualizer'

// https://vite.dev/config/
export default defineConfig({
    plugins: [
        vue(),
        tailwindcss(),
        // visualizer({
        //     open: true,
        //     gzipSize: true,
        //     brotliSize: true,
        //     filename: 'dist/bundle.html',
        //     title: 'Vite Bundle Visualizer',
        //     sourcemap: true,
        // })
    ],
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src'),
        },
    },
    server: {
        host: '0.0.0.0',
        proxy: {
            '/tiles': {
                target: 'http://223.2.32.242:8079',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/tiles/, ''),
            },
            '/api': {
                target: 'http://223.2.47.202:9888/api/v1',
                // target: 'http://223.2.43.228:30535/api/v1',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, ''),
            },
            '/model/websocket': {
                target: 'http://223.2.47.202:9001/model/websocket',
                // target: 'http://223.2.43.228:30535/api/v1',
                ws: true,
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/model\/websocket/, ''),
            },
            '/basemap': {
                target: 'http://172.31.13.21:5001/tiles',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/basemap/, ''),
            },
        },
    },
})
