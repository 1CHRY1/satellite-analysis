// 环境配置文件
// 使用方法：
// 1. 设置环境变量 VITE_ENV_TARGET=local|cluster|hxf
// 2. 或者在 package.json 中配置不同的启动脚本

export interface EnvConfig {
    api: string
    realtime: string
    websocket: string
    tiler: string
    proxymap: string,
}

export const ENV_CONFIG: Record<string, EnvConfig> = {
    // 本地开发环境
    local: {
        api: 'http://localhost:8999',
        realtime: 'http://localhost:5001',
        websocket: 'http://localhost:9888/model/websocket',
        tiler: 'http://127.0.0.1:8000',
        proxymap: 'http://localhost:5003',
    },
    // 集群环境
    cluster: {
        api: 'http://223.2.34.8:31584',
        realtime: 'http://223.2.34.8:5001',
        websocket: 'http://223.2.34.8:30394/model/websocket',
        tiler: 'http://223.2.34.8:31800',
        proxymap: 'http://localhost:5003',
    },
    // HXF开发环境
    hxf: {
        api: 'http://223.2.43.238:8999',
        realtime: 'http://223.2.43.238:5001',
        websocket: 'http://223.2.43.238:9000/model/websocket',
        tiler: 'http://223.2.43.238:8000',
        proxymap: 'http://localhost:5003',
    },
    slk: {
        api: 'http://223.2.34.8:31584',
        realtime: 'http://223.2.34.8:5001',
        websocket: 'http://223.2.34.8:30394/model/websocket',
        tiler: 'http://223.2.34.174:8000',
        proxymap: 'http://localhost:5003',
    },
    zzw: {
        api: 'http://localhost:8999',
        realtime: 'http://localhost:5001',
        websocket: 'http://localhost:9888/model/websocket',
        tiler: 'http://223.2.34.8:31800',
        proxymap: 'http://localhost:5003',
    }
}