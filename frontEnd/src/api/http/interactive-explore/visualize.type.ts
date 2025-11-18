import type { SpatialFilterMethod } from '@/type/interactive-explore/filter'
import type { BandMapper } from './filter.type'

export type VectorUrlParam = {
    landId: string
    source_layer: string
    spatialFilterMethod: SpatialFilterMethod
    resolution?: number
    type?: number[]
}

export type RGBCompositeParams = {
    redPath: string
    greenPath: string
    bluePath: string
    r_min: number
    r_max: number
    g_min: number
    g_max: number
    b_min: number
    b_max: number
    stretch_method?: 'linear' | 'standard' | 'gamma' | ''
    normalize_level?: number
    nodata?: number
    std_config?: string
}

export type OneBandColorLayerParam = {
    fullTifPath: string
    min?: number
    max?: number
    gridsBoundary?: any
    nodata?: number
    normalize_level?: number
}

export type LargeScaleSceneParam = {
    startTime: string
    endTime: string
    sensorName: string
    regionId: string
}

interface ScenePath {
    [key: string]: string // band_1, band_2, ...
}
export type ScenesInfo = {
    bandMapper: BandMapper
    scenesConfig: {
        sceneId: string
        sensorName: string
        bucket: string
        path: ScenePath
    }[]
}
