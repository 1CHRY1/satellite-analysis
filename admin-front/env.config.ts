// 环境配置文件
// 使用方法：
// 1. 设置环境变量 VITE_ENV_TARGET=local|cluster|hxf
// 2. 或者在 package.json 中配置不同的启动脚本

export interface EnvConfig {
    api: string
    minio: string,
    minio_console: string,
    log: string,
}

export const ENV_CONFIG: Record<string, EnvConfig> = {
    // 本地开发环境
    local: {
        api: 'http://localhost:8999',
        minio: 'http://223.2.34.8:30901',
        minio_console: 'http://223.2.34.8:30900',
        log: 'ws://223.2.34.8:9888',
    },
    // 集群环境
    cluster: {
        api: 'http://223.2.34.8:31584',
        minio: 'http://223.2.34.8:30901',
        minio_console: 'http://223.2.34.8:30900',
        log: 'ws://223.2.34.8:9888',
    },
    // HXF开发环境
    hxf: {
        api: 'http://223.2.43.238:8999',
        minio: 'http://223.2.34.8:30901',
        minio_console: 'http://223.2.34.8:30900',
        log: 'ws://223.2.34.8:9888',
    },
    slk: {
        api: 'http://223.2.34.8:31584',
        minio: 'http://223.2.34.8:30901',
        minio_console: 'http://223.2.34.8:30900',
        log: 'ws://223.2.34.8:9888',
    },
    zzw: {
        api: 'http://localhost:8999',
        minio: 'http://223.2.34.8:30901',
        minio_console: 'http://223.2.34.8:30900',
        log: 'ws://223.2.34.8:9888',
    }
}