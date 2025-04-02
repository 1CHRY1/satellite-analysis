import type { polygonGeometry } from '@/util/share.type'

///// Sensor API /////////////////////////
export namespace Sensor {
    export interface SensorResponse {
        sensorId: string
        sensorName: string
    }

    export interface SensorListResponse extends Array<SensorResponse> {}

    export interface SensorDetailResponse {
        platFormName: string | null
        description: string
    }
}

///// Product API /////////////////////////
export namespace Product {
    export interface ProductResponse {
        productId: string
        productName: string
    }

    export interface ProductListResponse extends Array<ProductResponse> {}

    export interface ProductDetailResponse {
        resolution: string
        period: string
        description: string
    }
}

export namespace Scene {}

///// Sensor Image API /////////////////////////
export namespace SensorImage {
    export interface SensorImageSearchRequest {
        sensorId: string
        productId: string
        startTime: string
        endTime: string
        geometry: polygonGeometry
    }

    export interface SensorImageSearchItemResponse {
        id: string
        type: 'Feature'
        properties: Record<string, any>
        geometry: polygonGeometry
    }

    export interface SensorImageSearchResponse {
        type: 'FeatureCollection'
        features: Array<SensorImageSearchItemResponse>
    }

    export interface SensorImageDetailResponse {
        sceneName: string
        sceneTime: string
        tileLevelNum: number
        tileLevels: string[]
        crs: string
        description: string
        bandNum: number
        bands: string[]
        cloud: string
    }

    export interface SensorImageBandResponse {
        imageId: string
        band: string
    }

    export interface SensorImageBandListResponse extends Array<SensorImageBandResponse> {}
}

///// Tile API /////////////////////////
export namespace ImageTile {
    export interface ImageTileItemResponse {
        id: string
        type: 'Feature'
        properties: Record<string, any>
        geometry: polygonGeometry
    }

    export interface ImageTilesResponse {
        type: 'FeatureCollection'
        features: Array<ImageTileItemResponse>
    }

    export interface ImageTileTifMergeRequest {
        imageId: string
        tiles: string[]
    }

    export interface ImageTileDetailResponse {
        tileLevel: string
        imageId: string
        columnId: number
        rowId: number
    }
}
