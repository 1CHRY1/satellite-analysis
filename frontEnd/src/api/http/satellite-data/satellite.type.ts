import type { polygonGeometry } from '@/util/share.type'

/**
 * Common API
 */
export namespace Common {
    export interface CommonResult<T> {
        status: number
        message: string
        data: T
    }

    export interface PageRequest {
        page: number
        pageSize: number,
        asc?: boolean,
        searchText?: string,
        sortField?: string
    }

    export interface Page<T> {
        records: Array<T>,
        total: number,
        size: number,
        current: number,
        pages: number
    }

    export interface PageResponse<T> extends CommonResult<Page<T>> { }
}

///// Sensor API /////////////////////////
export namespace Sensor {
    export interface SensorResponse {
        sensorId: string
        sensorName: string
    }

    export interface SensorListResponse extends Array<SensorResponse> { }

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

    export interface ProductListResponse extends Array<ProductResponse> { }

    export interface ProductDetailResponse {
        resolution: string
        period: string
        description: string
    }
}

export namespace Scene { }

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

    export interface SensorImageBandListResponse extends Array<SensorImageBandResponse> { }
}

type ModelStatus = 'COMPLETE' | 'RUNNING' | 'PENDING' | 'NONE' | 'ERROR'
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

    export interface ImageTileDetailResponse {
        tileLevel: string
        sceneId: string | null
        imageId: string
        cloud: string
        columnId: number
        rowId: number
    }

    export interface ImageTileTifMergeRequest {
        // sceneId: string
        tiles: Array<{
            columnId: string
            rowId: string
            sceneId: string
        }>
        bands: string[]
    }

    export interface ImageTileTifMergeResponse {
        status: number
        message: string
        data: ModelStatus
    }

    export interface ImageTileTifMergeStatusResponse {
        status: number
        message: string
        data: ModelStatus
    }

    export interface ImageTileQueryRequest {
        sensorId: string
        productId: string
        tileLevel: string
        rowId: string
        columnId: string
        band: string
    }

    export interface ImageTileQueryResponseItem {
        tilerUrl: string
        object: string
        tileId: string
        cloud: string
        sceneId: string
    }
    export interface ImageTileQueryResponse extends Array<ImageTileQueryResponseItem> { }
}

///// Project API /////////////////////////
export namespace Project {
    export interface ProjectActionRequest {
        projectId: string
        userId: string
        action: 'open' | 'close'
    }
    export interface ProjectActionResponse {
        status: number
        info: string
        projectId: string
    }
    export interface ImageTileUploadToProjectRequest {
        userId: string
        projectId: string
        object: string
    }
}

/**
 * Case API
 */
export namespace Case {
    export interface CasePageRequest extends Common.PageRequest {
    }

    export interface Case {
        caseId: string,
        caseName: string,
        resolution: string,
        sceneList: Array<string>,
        dataSet: string,
        status: string,
        result: {
            bucket: string,
            object_path: string
        },
        createTime: string
    }

    export interface CaseResponse extends Common.CommonResult<Case> { }

    export interface CasePageResponse extends Common.PageResponse<Case> {
    }
}