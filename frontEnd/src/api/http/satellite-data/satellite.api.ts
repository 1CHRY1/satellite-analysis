import http from '../clientHttp'
import type { Sensor, Product, SensorImage, ImageTile, Scene } from './satellite.type'
import { blobDownload } from '../../util'

export async function getSensorList(): Promise<Sensor.SensorListResponse> {
    return http.get<Sensor.SensorListResponse>(`/data/sensor`)
}

export async function getSensorDetail(sensorId: string): Promise<Sensor.SensorDetailResponse> {
    return http.get<Sensor.SensorDetailResponse>(`/data/sensor/description/sensorId/${sensorId}`)
}

export async function getProductList(sensorId: string): Promise<Product.ProductListResponse> {
    return http.get<Product.ProductListResponse>(`/data/product/sensorId/${sensorId}`)
}

export async function getProductDetail(productId: string): Promise<Product.ProductDetailResponse> {
    return http.get<Product.ProductDetailResponse>(`/data/product/description/productId/${productId}`)
}

export async function searchSensorImage(filterParams: SensorImage.SensorImageSearchRequest): Promise<SensorImage.SensorImageSearchResponse> {
    return http.post<SensorImage.SensorImageSearchResponse>(`/data/scene/sensorId/productId/time/area`, filterParams)
}

export async function getSensorImageDetail(sensorImageId: string): Promise<SensorImage.SensorImageDetailResponse> {
    return http.get<SensorImage.SensorImageDetailResponse>(`/data/scene/description/sceneId/${sensorImageId}`)
}

export async function getSensorImagePreview(sensorImageId: string): Promise<Blob> {
    return http.get<Blob>(`/data/scene/png/sceneId/${sensorImageId}`, { responseType: 'blob' })
}

export function getSensorImagePreviewUrl(sensorImageId: string): string {
    // @ts-ignore Property 'instance' is private and only accessible within class 'HttpClient'
    return `${http.instance.defaults.baseURL}/data/scene/png/sceneId/${sensorImageId}`
}

export async function getSensorImagePreviewPng(sensorImageId: string): Promise<string> {
    const blob = await http.get<Blob>(`/data/scene/png/sceneId/${sensorImageId}`, {
        responseType: 'blob',
    })
    return URL.createObjectURL(blob)
}

export async function getSensorImageBands(sensorImageId: string): Promise<SensorImage.SensorImageBandListResponse> {
    return http.get<SensorImage.SensorImageBandListResponse>(`/data/image/sceneId/${sensorImageId}`)
}

// export async function getImageTiles(bandImageId: string, tileLevel: string): Promise<ImageTile.ImageTilesResponse> {
//     return http.get<ImageTile.ImageTilesResponse>(`/data/tile/sceneId/${bandImageId}/tileLevel/${tileLevel}`)
// }

export function getImageTilesGeojsonURL(bandImageId: string, tileLevel: string): string {
    const lowcase = bandImageId.toLowerCase()
    // @ts-ignore Property 'instance' is private and only accessible within class 'HttpClient'
    return `${http.instance.defaults.baseURL}/data/tile/sceneId/${lowcase}/tileLevel/${tileLevel}`
}

async function getImageTileTif(bandImageId: string, tileId: string): Promise<Blob> {
    return http.get<Blob>(`/data/tile/tif/imageId/${bandImageId}/tileId/${tileId}`, {
        responseType: 'blob',
    })
}

export async function downloadImageTileTif(bandImageId: string, tileId: string, name?: string): Promise<void> {
    const blob = await getImageTileTif(bandImageId, tileId)
    blobDownload(blob, name ?? `${bandImageId}_${tileId}.tif`)
}

async function mergeImageTileTifs(params: ImageTile.ImageTileTifMergeRequest): Promise<Blob> {
    return http.post<Blob>(`/data/tile/tif/tileIds`, params, { responseType: 'blob' })
}

export async function downloadImageTileTifMerge(params: ImageTile.ImageTileTifMergeRequest, name?: string): Promise<void> {
    const blob = await mergeImageTileTifs(params)
    blobDownload(blob, name ?? `${params.imageId}_merged.tif`)
}

export async function getImageTileDetail(bandImageId: string, tileId: string): Promise<ImageTile.ImageTileDetailResponse> {
    return http.get<ImageTile.ImageTileDetailResponse>(`/data/tile/description/imageId/${bandImageId}/tileId/${tileId}`)
}
