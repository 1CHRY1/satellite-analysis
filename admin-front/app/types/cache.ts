export type CacheType = "eOCubeCache" | "sceneCache" | "themeCache" | "regionInfoCache"


export type BackendCache = {
    cacheExpiry: number
    cacheTime: number
    type?: CacheType
    cacheKey?: string
    id: number
}

export type RedisCache = {
    size: number,
    type: string,
    ttl: number,
    key: string
}