import http from '../clientHttp'
import type { Sensor, Product, SensorImage, ImageTile, Project, Case, Common } from './satellite.type'
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
        `/modeling/case/status/caseId/${caseId}`,
    )
}
/// 合并结果
export async function getMergeImageTilesResult(caseId: string): Promise<Blob> {
    return http.get<Blob>(`/modeling/case/data/tif/caseId/${caseId}`, { responseType: 'blob' })
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
export async function queryTileByXY(
    params: ImageTile.ImageTileQueryRequest,
): Promise<ImageTile.ImageTileQueryResponse> {
    return http.post<ImageTile.ImageTileQueryResponse>(`/data/tile/tiler/tiles`, params)
}

//----------------------------- v2版本 API -----------------------------//
export async function getGridByRegionAndResolution(
    region: number | string,
    resolution: number,
): Promise<any> {
    return http.get<any>(`data/grid/grids/region/${region}/resolution/${resolution}`)
}
export async function getGridByPOIAndResolution(poiId: string, resolution: number): Promise<any> {
    return http.get<any>(`data/grid/grids/location/${poiId}/resolution/${resolution}`)
}

export async function getBoundary(region: number | string): Promise<any> {
    return http.get<any>(`data/region/boundary/${region}`)
}

export async function getAddress(region: number | string): Promise<Common.CommonResult<string>> {
    return http.get<Common.CommonResult<string>>(`data/region/address/${region}`)
}

export async function getRegionPosition(region: number | string): Promise<any> {
    return http.get<any>(`data/region/window/region/${region}`)
}
export async function getPOIPosition(region: number | string, resolution: number): Promise<any> {
    return http.get<any>(`data/location/window/location/${region}/resolution/${resolution}`)
}

export async function getSceneGrids(param: any): Promise<any> {
    return http.post<any>(`data/grid/scene/grids`, param)
}

export async function getVectorGrids(param: any): Promise<any> {
    return http.post<any>(`data/grid/vector/grids`, param)
}

export async function getNoCloud(param: any): Promise<any> {
    return http.post<any>(`modeling/example/noCloud`, param)
}

export async function getNdviPoint(param: any): Promise<any> {
    return http.post<any>(`modeling/example/ndvi/point`, param)
}

export async function getSpectrum(param: any): Promise<any> {
    return http.post<any>(`modeling/example/spectrum/point`, param)
}

export async function getCaseStatus(taskId: string): Promise<any> {
    return http.get<any>(`modeling/case/status/caseId/${taskId}`)
}

export async function getCaseTifResult(taskId: string): Promise<any> {
    return http.get<any>(`modeling/case/result/tif/caseId/${taskId}`)
}

export async function getCaseResult(taskId: string): Promise<any> {
    return http.get<any>(`modeling/case/result/caseId/${taskId}`)
}

export const pollStatus = async (taskId: string) => {
    const interval = 1000 // 每秒轮询一次
    return new Promise<void>((resolve, reject) => {
        const timer = setInterval(async () => {
            try {
                const res = await getCaseStatus(taskId)!
                console.log('状态:', res.data)

                if (res.data === 'COMPLETE') {
                    clearInterval(timer)
                    resolve()
                }
                if (res.data === 'FAILED' || res.data === 'ERROR') {
                    console.log(res, res.data)
                    clearInterval(timer)
                    reject(new Error('任务失败'))
                }
            } catch (err) {
                clearInterval(timer)
                console.log('错误', err)
                reject(err)
            }
        }, interval)
    })
}

export async function getRasterPoints(param: any): Promise<any> {
    return http.post<any>(`modeling/example/raster/point`, param)
}

export async function getRasterLine(param: any): Promise<any> {
    return http.post<any>(`modeling/example/raster/line`, param)
}

export async function getPoiInfo(query: string): Promise<any> {
    return http.get<any>(`data/location/name/${query}`)
}

/**
 * Case历史记录
 */
export async function getCasePage(param: Case.CasePageRequest): Promise<Case.CasePageResponse> {
    return http.post<Case.CasePageResponse>(`data/case/page`, param)
}

export async function getCurrentTime(): Promise<any> {
    return http.get<any>(`time/current`)
}

export async function getCaseById(caseId: string): Promise<Case.CaseResponse> {
    return http.get<Case.CaseResponse>(`data/case/${caseId}`)
}
// 注意与getCaseResult的区别
export async function getResultByCaseId(caseId: string): Promise<any> {
    return http.get<any>(`data/case/result/${caseId}`)
}

// 获取矢量数据
export async function getVectorsByConfig(param: any): Promise<any> {
    return http.post<any>(`data/vector/time/region`, param)
}
export async function getVectorsByPOIConfig(param: any): Promise<any> {
    return http.post<any>(`data/vector/time/location`, param)
}
