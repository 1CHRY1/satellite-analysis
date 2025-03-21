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

    /////// UI interface
    // export interface SensorView {
    //     id: string
    //     name: string
    //     description: string
    //     color: string
    //     products: Array<Product.ProductView>
    // }
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

    // export interface ProductView {
    //     id: string
    //     sensorId: string
    //     name: string
    //     description: string
    //     resolution: string
    //     period: string
    // }
}

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
        name: string
        cloud: number
        sceneTime: string
        tileLevelNum: number
        tileLevels: string[]
        crs: string
        description: string | null
        bandNum: number
        bands: string[]
    }

    export interface SensorImageBandResponse {
        imageId: string
        band: string
    }

    export interface SensorImageBandListResponse extends Array<SensorImageBandResponse> {}

    // export interface BandView {
    //     id: string
    //     name: string
    //     description: string
    //     wavelength: string
    //     resolution: string
    // }

    // export interface SensorImageView {
    //     id: string
    //     name: string
    //     date: string
    //     resolution: string
    //     cloudCover: number
    //     thumbnail: string
    //     color: string
    //     bands: Array<BandView>
    // }
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
