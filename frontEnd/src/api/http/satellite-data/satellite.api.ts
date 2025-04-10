import http from '../clientHttp'
import type { Sensor, Product, SensorImage, ImageTile, Project } from './satellite.type'
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
    return http.get<Product.ProductDetailResponse>(
        `/data/product/description/productId/${productId}`,
    )
}

export async function searchSensorImage(
    filterParams: SensorImage.SensorImageSearchRequest,
): Promise<SensorImage.SensorImageSearchResponse> {
    return http.post<SensorImage.SensorImageSearchResponse>(
        `/data/scene/sensorId/productId/time/area`,
        filterParams,
    )
}

export async function getSensorImageDetail(
    sensorImageId: string,
): Promise<SensorImage.SensorImageDetailResponse> {
    return http.get<SensorImage.SensorImageDetailResponse>(
        `/data/scene/description/sceneId/${sensorImageId}`,
    )
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

export async function getSensorImageBands(
    sensorImageId: string,
): Promise<SensorImage.SensorImageBandListResponse> {
    return http.get<SensorImage.SensorImageBandListResponse>(`/data/image/sceneId/${sensorImageId}`)
}

export async function getImageTilesGeojson(
    sceneId: string,
    tileLevel: string,
): Promise<ImageTile.ImageTilesResponse> {
    return http.get<ImageTile.ImageTilesResponse>(
        `/data/tile/sceneId/${sceneId}/tileLevel/${tileLevel}`,
    )
}

export function getImageTilesGeojsonURL(sceneId: string, tileLevel: string): string {
    const lowcase = sceneId.toLowerCase()
    // @ts-ignore Property 'instance' is private and only accessible within class 'HttpClient'
    return `${http.instance.defaults.baseURL}/data/tile/sceneId/${lowcase}/tileLevel/${tileLevel}`
}

export async function getImageTileDetail(
    sceneId: string,
    tileId: string,
): Promise<ImageTile.ImageTileDetailResponse> {
    return http.get<ImageTile.ImageTileDetailResponse>(
        `/data/tile/description/sceneId/${sceneId}/tileId/${tileId}`,
    )
}

/// 合并tif (模型)
export async function startMergeImageTiles(
    params: ImageTile.ImageTileTifMergeRequest,
): Promise<ImageTile.ImageTileTifMergeStatusResponse> {
    return http.post<ImageTile.ImageTileTifMergeStatusResponse>(`/data/tile/tif/tiles`, params)
}
/// 查询模型状态
export async function getMergeImageTilesStatus(
    caseId: string,
): Promise<ImageTile.ImageTileTifMergeStatusResponse> {
    return http.get<ImageTile.ImageTileTifMergeStatusResponse>(
        `/model/case/status/caseId/${caseId}`,
    )
}
/// 合并结果
export async function getMergeImageTilesResult(caseId: string): Promise<Blob> {
    return http.get<Blob>(`/model/case/data/tif/caseId/${caseId}`, { responseType: 'blob' })
}
/// 下载结果
export async function downloadMergeImageTilesResult(caseId: string, name?: string): Promise<void> {
    const blob = await getMergeImageTilesResult(caseId)
    blobDownload(blob, name ?? `${caseId}.tif`)
}
/// 操作项目
export async function operateProject(
    params: Project.ProjectActionRequest,
): Promise<Project.ProjectActionResponse> {
    return http.post<Project.ProjectActionResponse>(`/coding/project/operating`, params)
}
/// 上传瓦片至项目
export async function uploadImageTilesToProject(
    params: Project.ImageTileUploadToProjectRequest,
): Promise<void> {
    return http.post<void>(`/coding/project/file/tiles`, params)
}

/// 基于产品和行列号查询一个瓦片的信息
export async function queryTileByXY(params: ImageTile.ImageTileQueryRequest): Promise<ImageTile.ImageTileQueryResponse> {
    return http.post<ImageTile.ImageTileQueryResponse>(`/data/tile/tiler/tiles`, params)
}