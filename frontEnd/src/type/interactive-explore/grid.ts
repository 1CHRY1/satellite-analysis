import type { Grid } from "@/api/http/interactive-explore/grid.type"
import type { VectorStats } from "@/api/http/interactive-explore/filter.type"

export type PopupTab = 'scene' | 'vector' | 'theme'


export type GridData = {
    rowId: number,
    columnId: number,
    resolution: number,
    opacity?: number,
    normalize_level?: number,
    sceneRes: Grid.SceneDetailStatsResponse,
    vectors: VectorStats.VectorStatsResponse,
    themeRes: Grid.ThemeDetailStatsResponse,
}

export type SceneMethod = 'rgb' | 'superresolution'

export type ImageInfoType = {
    sceneId: string
    time: string
    tifFullPath: string
    nodata: number 
}

export type MultiImageInfoType = {
    sceneId: string
    time: string
    sensorName: string
    productName: string
    dataType: string
    redPath: string
    greenPath: string
    bluePath: string
    images?: any[],
    bandMapper?: any,
    nodata: number
    cloudPath?: string  // 云掩膜完整路径
}