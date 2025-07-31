export namespace Grid {
    export interface GridRequest {
        columnId: number,
        rowId: number,
        resolution: number,
    }

    export type SceneCategory = 'subMeter' | 'twoMeter' | 'tenMeter' | 'thirtyMeter' | 'other'

    export type SceneDetail = {
        sceneId: string,
        sceneTime: string,
        sensorName: string,
        productName: string,
        images: Array<{
            bucket: string,
            tifPath: string,
            band: number,
        }>,
        noData: number,
        bandMapper: {
            Red: number,
            Green: number,
            Blue: number,
        },
    }

    export interface SceneDetailStatsResponse {
        total: number,
        category: Array<SceneCategory>,
        dataset?: {
            [key in SceneCategory]: {
                total: number,
                label: string,
                dataList: Array<SceneDetail>
            }
        }
    }

    export type ThemeCategory = 'dem' | 'dsm' | 'ndvi' | 'svr' | '3d' | 'other'

    export type ThemeDetail = {
        sceneId: string,
        sceneTime: string,
        sensorName: string,
        productName: string,
        dataType: ThemeCategory,
        images: Array<{
            bucket: string,
            tifPath: string,
            band: number,
        }>,
        noData: number,
        bandMapper: {
            Red: number,
            Green: number,
            Blue: number,
        },
    }

    export interface ThemeDetailStatsResponse {
        total: number,
        category: Array<ThemeCategory>,
        dataset?: {
            [key in ThemeCategory]: {
                total: number,
                label: string,
                dataList: Array<ThemeDetail>
            }
        }
    }
}