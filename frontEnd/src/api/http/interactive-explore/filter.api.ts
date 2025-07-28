/**
 * 交互式探索API V3版
 */

import http from '../../axiosClient/clientHttp3'
import type { POIFilterRequest, RegionFilterRequest, SceneStats, ThemeStats } from './filter.type'

export async function getSceneStatsByRegionFilter(param: RegionFilterRequest): Promise<SceneStats.SceneStatsResponse> {
    return http.post<SceneStats.SceneStatsResponse>(`data/scene/time/region`, param)
}

export async function getSceneStatsByPOIFilter(param: POIFilterRequest): Promise<SceneStats.SceneStatsResponse> {
    return http.post<SceneStats.SceneStatsResponse>(`data/scene/time/location`, param)
}

export async function getThemeStatsByRegionFilter(param: RegionFilterRequest): Promise<ThemeStats.ThemeStatsResponse> {
    return http.post<ThemeStats.ThemeStatsResponse>(`data/theme/time/region`, param)
}

export async function getThemeStatsByPOIFilter(param: POIFilterRequest): Promise<ThemeStats.ThemeStatsResponse> {
    return http.post<ThemeStats.ThemeStatsResponse>(`data/theme/time/location`, param)
}