
/////// Scene API //////////////////////////////////
// Note: Scene是一景影像，可能包含多个GeoTIFF

export interface SceneSearchRequest {
    sensorId: string
    productId: string
    startTime: string
    endTime: string
    geometry: {
        type: 'Polygon'
        coordinates: number[][][]
    }
}

export interface SceneSearchResponse {
    type: 'FeatureCollection'
    features: Array<{
        id: string
        type: 'Feature'
        properties: Record<string, any>
        geometry: {
            type: 'Polygon'
            coordinates: number[][][]
        }
    }>
}

export interface SceneDetailResponse {
    sceneTime: string
    tileLevelNum: number
    tileLevels: string[]
    crs: string
    description: string | null
    bandNum: number
    bands: string[]
}


/////// Image API //////////////////////////////////
// Note: Image 隶属于Scene, 是明确的单个GeoTIFF
export interface ImageResponse {
    imageId: string
    band: string
}

export type SceneImageListResponse = Array<ImageResponse>
