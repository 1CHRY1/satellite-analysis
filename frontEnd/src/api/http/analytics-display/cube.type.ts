import type { Dayjs } from "dayjs";

export interface CubeRequest {
    cubeId: string,
    dimensionSensors: string[],
    dimensionDates: Dayjs[],
    dimensionBands: string[],
    dimensionScenes: Scene[],
}

interface Scene {
    sceneId: string,
    sceneTime: Dayjs,
    sensorName: string,
    noData: number,
    platformName: string,
    productName: string,
    images: Image[],
    bandMapper: {
        Red: string,
        Green: string,
        Blue: string,
        NIR: string,
    },
    boundingBox: any,
}

interface Image {
    bucket: string,
    tifPath: string,
    band: string,
}

export interface GridRequest {
    rowId: number,
    columnId: number,
    resolution: number
}

export interface CubeItem {
    cubeId: string
    cacheTime: number
    dimensionSensors: any[]
    dimensionScenes: any[]
    dimensionDates: any[]
}

export interface CubeDisplayItem extends CubeItem {
    cacheKey: string
    isSelect: boolean
    isShow: boolean
    gridGeoJson: any
}

export type CubeObj = Record<string, CubeItem>