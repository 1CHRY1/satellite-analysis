import http from '~/apis/clients/adminClient'
import type { CommonResponse } from '../common.type'
import type { BackendCacheResponse, RedisCacheResponse, BackendCacheKeys, RedisCacheKeys } from './cache.type'

export async function getBackendCache(): Promise<CommonResponse<BackendCacheResponse>> {
    return http.get<CommonResponse<BackendCacheResponse>>(`cache/backend/get`)
}

export async function getRedisCache(): Promise<CommonResponse<RedisCacheResponse>> {
    return http.get<CommonResponse<RedisCacheResponse>>(`cache/redis/get`)
}

export async function batchDelBackendCache(cacheKeys: BackendCacheKeys): Promise<CommonResponse<any>> {
    return http.delete<CommonResponse<any>>(`cache/backend/delete`, {data: cacheKeys})
}

export async function delExpiredBackendCache(): Promise<CommonResponse<any>> {
    return http.delete<CommonResponse<any>>(`cache/backend/delete/expire`,)
}

export async function batchDelRedisCache(cacheKeys: RedisCacheKeys): Promise<CommonResponse<any>> {
    return http.delete<CommonResponse<any>>(`cache/redis/delete`, {data: cacheKeys})
}