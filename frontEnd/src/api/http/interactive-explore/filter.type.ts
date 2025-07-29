export interface RegionFilterRequest {
    startTime: string,
    endTime: string,
    regionId: string,
    resolution: number,
}

export interface POIFilterRequest {
    startTime: string,
    endTime: string,
    locationId: string,
    resolution: number,
}

export namespace SceneStats {
    export type SceneCategory = 'subMeter' | 'twoMeter' | 'tenMeter' | 'thirtyMeter' | 'other'

    export type SceneCategoryStats = {
        total: number,
        coverage: string,
        resolution: number,
        label: string,
        dataList: Array<{
            sensorName: string,
            platformName: string
        }>,
    }

    export interface SceneStatsResponse {
        total: number,
        coverage: string,
        category: Array<SceneCategory>,
        dataset?: {
            [key in SceneCategory]: SceneCategoryStats
        },
    }
}

export namespace VectorStats {
    export type Vector = {
        tableName: string,
        vectorName: string,
        time: string,
    }

    export interface VectorStatsResponse extends Array<Vector> { }

}

export namespace ThemeStats {
    export type ThemeCategory = 'subMeter' | 'twoMeter' | 'tenMeter' | 'thirtyMeter' | 'other'

    export type ThemeCategoryStats = {
        total: number,
        label: string,
        dataList: Array<string>,
    }

    export interface ThemeStatsResponse {
        total: number,
        category: Array<ThemeCategory>,
        dataset?: {
            [key in ThemeCategory]: ThemeCategoryStats
        },
    }
}