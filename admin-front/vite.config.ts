import { reactRouter } from "@react-router/dev/vite";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "vite";
import tsconfigPaths from "vite-tsconfig-paths";
import { ENV_CONFIG } from './env.config'

const ENV_TARGET = 'hxf' // 一键切换配置环境，使用集群则改为cluster，使用本地则改为local
// 获取当前环境配置
const currentEnv = ENV_CONFIG[ENV_TARGET as keyof typeof ENV_CONFIG] || ENV_CONFIG.zzw

// 代理配置生成器
const createProxyConfig = () => {
  return {
      // API v1
      '/api': {
          target: `${currentEnv.api}/api/v1`,
          changeOrigin: true,
          rewrite: (path: string) => path.replace(/^\/api/, ''),
      },
      // Admin API
      '/admin': {
        target: `${currentEnv.api}/admin/api/v1`,
        changeOrigin: true,
        rewrite: (path: string) => path.replace(/^\/admin/, ''),
    },
  }
}

export default defineConfig({
  plugins: [tailwindcss(), reactRouter(), tsconfigPaths()],
  server: {
    host: '0.0.0.0',
    proxy: createProxyConfig(),
},
});
