/**
 * 交互式探索API V3版
 */

import http from '../../axiosClient/clientHttp3'
import type { CommonResponse } from '../common.type'
import type {
    POIFilterRequest,
    RegionFilterRequest,
    PolygonFilterRequest,
    SceneStats,
    ThemeStats,
    Theme,
} from './filter.type'

export async function getSceneStatsByRegionFilter(
    param: RegionFilterRequest,
): Promise<SceneStats.SceneStatsResponse> {
    return http.post<SceneStats.SceneStatsResponse>(`data/scene/time/region`, param)
}

export async function getSceneStatsByPOIFilter(
    param: POIFilterRequest,
): Promise<SceneStats.SceneStatsResponse> {
    return http.post<SceneStats.SceneStatsResponse>(`data/scene/time/location`, param)
}

export async function getSceneStatsByPolygonFilter(
    param: PolygonFilterRequest,
): Promise<SceneStats.SceneStatsResponse> {
    return http.post<SceneStats.SceneStatsResponse>(`data/scene/time/polygon`, param)
}

export async function getThemeStatsByRegionFilter(
    param: RegionFilterRequest,
): Promise<ThemeStats.ThemeStatsResponse> {
    return http.post<ThemeStats.ThemeStatsResponse>(`data/theme/time/region`, param)
}

export async function getThemeStatsByPOIFilter(
    param: POIFilterRequest,
): Promise<ThemeStats.ThemeStatsResponse> {
    return http.post<ThemeStats.ThemeStatsResponse>(`data/theme/time/location`, param)
}

export async function getThemeStatsByPolygonFilter(
    param: PolygonFilterRequest,
): Promise<ThemeStats.ThemeStatsResponse> {
    return http.post<ThemeStats.ThemeStatsResponse>(`data/theme/time/polygon`, param)
}

export async function getThemeByThemeName(
    themeName: string,
): Promise<CommonResponse<Theme.ThemeResponse>> {
    return http.get<CommonResponse<Theme.ThemeResponse>>(
        `modeling/example/theme/visualization/${themeName}`,
    )
}

/**
 * 获取覆盖率接口
 */
export async function getSceneCategoryCoverage(
    category?: string,
): Promise<SceneStats.SceneCategoryCoverageResponse> {
    if (category) return http.get<SceneStats.SceneCategoryCoverageResponse>(`data/scene/coverage/${category}`)
    else return http.get<SceneStats.SceneCategoryCoverageResponse>(`data/scene/coverage`)
}
