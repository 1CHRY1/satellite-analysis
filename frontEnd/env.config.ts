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
    minio: string,
    modelServer: string,
}

export interface FixedConfig {
    basemap: string
    demtiles: string,
    mvtbasemap: string,
}

export const ENV_CONFIG: Record<string, EnvConfig> = {
    // 本地开发环境
    local: {
        api: 'http://localhost:8999',
        realtime: 'http://localhost:5001',
        websocket: 'http://localhost:9888/model/websocket',
        tiler: 'http://127.0.0.1:8000',
        proxymap: 'http://localhost:5003',
        minio: 'http://223.2.34.8:30900',
        modelServer: 'http://127.0.0.1:5001',
    },
    // 集群环境
    cluster: {
        api: 'http://223.2.34.8:31584',
        realtime: 'http://223.2.34.8:5001',
        websocket: 'http://223.2.34.8:30394/model/websocket',
        tiler: 'http://223.2.34.8:31800',
        proxymap: 'http://localhost:5003',
        minio: 'http://223.2.34.8:30900',
        modelServer: 'http://223.2.34.8:31500',
    },
    // HXF开发环境
    hxf: {
        api: 'http://223.2.43.238:8999',
        realtime: 'http://223.2.43.238:5001',
        websocket: 'http://223.2.43.238:9888/model/websocket',
        tiler: 'http://223.2.43.238:8000',
        proxymap: 'http://localhost:5003',
        minio: 'http://223.2.34.8:30900',
        modelServer: 'http://223.2.34.8:31500',
    },
    slk: {
        api: 'http://223.2.34.8:31584',
        realtime: 'http://223.2.34.8:5001',
        websocket: 'http://223.2.34.8:30394/model/websocket',
        tiler: 'http://127.0.0.1:8000',
        proxymap: 'http://localhost:5003',
        minio: 'http://223.2.34.8:30900',
        modelServer: 'http://223.2.34.8:31500',
    },
    zzw: {
        api: 'http://localhost:8999',
        realtime: 'http://localhost:5001',
        websocket: 'http://localhost:9888/model/websocket',
        tiler: 'http://127.0.0.1:8000',
        proxymap: 'http://localhost:5003',
        minio: 'http://223.2.34.8:30900',
        modelServer: 'http://localhost:5000',
    },
    xzy: {
        api: 'http://10.30.8.44:31584', // 或本地 http://localhost:8999
        realtime: 'http://localhost:5001',
        websocket: 'http://10.30.8.44:30394/model/websocket', // 或本地 http://localhost:9888/model/websocket
        tiler: 'http://10.30.8.44:31800',
        proxymap: 'http://localhost:5003',
        minio: 'http://223.2.34.8:30900',
        modelServer: 'http://223.2.34.8:31500',
    },
}

// 固定配置（不随环境变化）
export const FIXED_CONFIG: FixedConfig = {
    basemap: 'http://172.31.13.21:5001/tiles', // 北京影像底图
    mvtbasemap: 'http://172.31.13.21:5002/tiles', // 北京矢量底图
    demtiles: 'http://223.2.34.8:30504/tiles', // 北京内网5004端口
}