export type CacheType = "eOCubeCache" | "sceneCache" | "themeCache" | "regionInfoCache"

export type BackendCache = {
    cacheExpiry: number
    cacheTime: number
}

export type RedisCache = {
    size: number,
    type: string,
    ttl: number,
    key: string
}

export type BackendCacheResponse = {
    [K in CacheType]: Record<string, BackendCache>;
}

export type RedisCacheResponse = RedisCache[]

export interface BackendCacheKeys {
    cacheKeys: {
        type: CacheType,
        cacheKey: string
    }[]
}

export interface RedisCacheKeys {
    keys: string[]
}