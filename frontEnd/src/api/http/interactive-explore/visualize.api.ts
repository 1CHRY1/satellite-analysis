/**
 * Visualize API: 获取可视化服务URL
 */
import { ezStore } from '@/store'
import type {
    LargeScaleSceneParam,
    OneBandColorLayerParam,
    RGBCompositeParams,
    ScenesInfo,
    VectorUrlParam,
} from './visualize.type'
import { getThemeByThemeName } from './filter.api'
import http from '@/api/axiosClient/tilerHttp'
import { message } from 'ant-design-vue'
import type { GridData } from '@/type/interactive-explore/grid'
import { grid2bbox } from '@/util/map/gridMaker'
import httpV3 from '../../axiosClient/clientHttp3'
import type { CommonResponse } from '../common.type'

const titilerProxyEndPoint = ezStore.get('conf')['titiler']
const minioEndPoint = ezStore.get('conf')['minioIpAndPort']
const backProxyEndPoint = ezStore.get('conf')['back_app']

/**
 * 0. 公共函数
 */
/**
 * 获取MinIOUrl
 */
export const getMinIOUrl = (path: string, bucket?: string) => {
    if (bucket) {
        return `${minioEndPoint}/${bucket}/${path}`
    } else {
        return `${minioEndPoint}/${path}`
    }
}

/**
 * 获取影像统计信息
 */
export async function getImgStats(url: string): Promise<any> {
    return http.get(`/statistics?url=${url}`)
}

/**
 * 1. 交互探索 - 可视化Url
 */

/**
 * 遥感影像Url - 小范围OnTheFly
 */
export const getSceneUrl = (sensorName: string) => {
    let baseUrl = `${titilerProxyEndPoint}/image_visualization/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    requestParams.append('sensorName', sensorName)
    const fullUrl = baseUrl + '?' + requestParams.toString()
    console.log('Scene URL: ', fullUrl)
    return fullUrl
}
export const getLargeSceneUrl = (mosaicUrl: string) => {
    let baseUrl = `${titilerProxyEndPoint}/mosaic/mosaictile/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    requestParams.append('mosaic_url', mosaicUrl)
    const fullUrl = baseUrl + '?' + requestParams.toString()
    return fullUrl
}
export async function getLargeSceneMosaicUrl(param: LargeScaleSceneParam): Promise<any> {
    return httpV3.post<any>(`modeling/example/scenes/visualization/lowLevel`, param)
}
export async function getScenesInfo(
    sensorName: string,
    points: number[],
): Promise<CommonResponse<ScenesInfo>> {
    return httpV3.post<CommonResponse<ScenesInfo>>('modeling/example/scenes/visualization', {
        sensorName,
        points,
    })
}

/**
 * 矢量Url
 */
export const getVectorUrl = (vectorUrlParam: VectorUrlParam) => {
    const { landId, source_layer, field, spatialFilterMethod, resolution, type } = vectorUrlParam
    const requestParams = new URLSearchParams()
    if (type && type.length) {
        for (const t of type) {
            // requestParams.append('type', t.toString())
            requestParams.append('value', t.toString())
        }
    }
    const qs = requestParams.toString()
    const types = qs ? '?' + qs : ''
    let baseUrl = ''
    // types = ''
    switch (spatialFilterMethod) {
        case 'region':
            baseUrl = `http://${window.location.hostname}:${window.location.port}${backProxyEndPoint}/data/vector/region/${landId}/${source_layer}/${field}/{z}/{x}/{y}${types}`
            break
        case 'poi':
            baseUrl = `http://${window.location.hostname}:${window.location.port}${backProxyEndPoint}/data/vector/location/${landId}/${resolution}/${source_layer}/${field}/{z}/{x}/{y}${types}`
            break
        default:
            baseUrl = `http://${window.location.hostname}:${window.location.port}${backProxyEndPoint}/data/vector/region/${landId}/${source_layer}/${field}/{z}/{x}/{y}${types}`
            break
    }
    const fullUrl = baseUrl
    console.log('Vector URL: ', fullUrl)
    return fullUrl
}

/**
 * 地形图Url
 */
export const getDEMUrl = async (themeName: string, gridsBoundary: any) => {
    let baseUrl = `${titilerProxyEndPoint}/terrain/terrainRGB/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    const themeRes = await getThemeByThemeName(themeName)
    const url = `${minioEndPoint}/${themeRes.data.images[0].bucket}/${themeRes.data.images[0].tifPath}`
    requestParams.append('url', url)
    requestParams.append('grids_boundary', JSON.stringify(gridsBoundary))
    requestParams.append('scale_factor', '0.5')
    const fullUrl = baseUrl + '?' + requestParams.toString()
    console.log('DEM URL: ', fullUrl)
    return fullUrl
}

/**
 * 二维地形图Url
 */
export const get2DDEMUrl = async (themeName: string, gridsBoundary: any) => {
    let baseUrl = `${titilerProxyEndPoint}/terrain/2dTerrainRGB/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    const themeRes = await getThemeByThemeName(themeName)
    const url = `${minioEndPoint}/${themeRes.data.images[0].bucket}/${themeRes.data.images[0].tifPath}`
    requestParams.append('url', url)
    requestParams.append('grids_boundary', JSON.stringify(gridsBoundary))
    requestParams.append('scale_factor', '0.5')
    const fullUrl = baseUrl + '?' + requestParams.toString()
    console.log('DEM URL: ', fullUrl)
    return fullUrl
}

/**
 * 单波段图Url - NDVI或SVR
 */
export const getNDVIOrSVRUrl = async (themeName: string, gridsBoundary: any) => {
    let baseUrl = `${titilerProxyEndPoint}/oneband/colorband/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    const themeRes = await getThemeByThemeName(themeName)
    const url = `${minioEndPoint}/${themeRes.data.images[0].bucket}/${themeRes.data.images[0].tifPath}`
    requestParams.append('url', url)
    requestParams.append('grids_boundary', JSON.stringify(gridsBoundary))
    requestParams.append('nodata', themeRes.data.noData.toString())
    const fullUrl = baseUrl + '?' + requestParams.toString()
    console.log('NDVI or SVR URL: ', fullUrl)
    return fullUrl
}

/**
 * 红绿立体影像Url
 */
export const get3DUrl = async (themeName: string, gridsBoundary: any) => {
    
    // setTimeout(() => {
    //     stopLoading()
    // }, 5000)
    let baseUrl = `${titilerProxyEndPoint}/rgb/tiles/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    const themeRes = await getThemeByThemeName(themeName)
    const image_r = themeRes.data.images.find(
        (item: any) => item.band === themeRes.data.bandMapper.Red,
    )
    const image_g = themeRes.data.images.find(
        (item: any) => item.band === themeRes.data.bandMapper.Green,
    )
    const image_b = themeRes.data.images.find(
        (item: any) => item.band === themeRes.data.bandMapper.Blue,
    )
    const url_r = `${minioEndPoint}/${image_r?.bucket}/${image_r?.tifPath}`
    const url_g = `${minioEndPoint}/${image_g?.bucket}/${image_g?.tifPath}`
    const url_b = `${minioEndPoint}/${image_b?.bucket}/${image_b?.tifPath}`
    requestParams.append('url_r', url_r)
    requestParams.append('url_g', url_g)
    requestParams.append('url_b', url_b)
    const stats_r = await getImgStats(url_r)
    const stats_g = await getImgStats(url_g)
    const stats_b = await getImgStats(url_b)
    requestParams.append('min_r', stats_r.b1.min)
    requestParams.append('max_r', stats_r.b1.max)
    requestParams.append('min_g', stats_g.b1.min)
    requestParams.append('max_g', stats_g.b1.max)
    requestParams.append('min_b', stats_b.b1.min)
    requestParams.append('max_b', stats_b.b1.max)
    requestParams.append('grids_boundary', JSON.stringify(gridsBoundary))
    const fullUrl = baseUrl + '?' + requestParams.toString()
    return fullUrl
}

/**
 * 2. 格网探查 - 格网可视化Url
 */

/**
 * 格网遥感影像Url
 */
export const getGridSceneUrl = (grid: GridData, param: RGBCompositeParams) => {
    return getGrid3DUrl(grid, param)
}

/**
 * 格网矢量Url
 */
export const getGridVectorUrl = (grid: GridData, source_layer: string, field: string = 'type', type?: any[]) => {
    const requestParams = new URLSearchParams()
    if (type && type.length) {
        for (const t of type) {
            requestParams.append('value', t.toString())
        }
    }
    const qs = requestParams.toString()
    const types = qs ? '?' + qs : ''
    return `http://${window.location.hostname}:${window.location.port}${backProxyEndPoint}/data/vector/grid/${grid.columnId}/${grid.rowId}/${grid.resolution}/${source_layer}/${field}/{z}/{x}/{y}${types}`
}

/**
 * 格网地形图Url
 */
export const getGridDEMUrl = (grid: GridData, bandPath: string) => {
    let baseUrl = `${titilerProxyEndPoint}/terrain/box/{z}/{x}/{y}.png`
    const bbox = grid2bbox(grid.columnId, grid.rowId, grid.resolution)
    const requestParams = new URLSearchParams()
    requestParams.append('bbox', bbox.join(','))
    requestParams.append('url', getMinIOUrl(bandPath))
    requestParams.append('scale_factor', '0.5')
    const fullUrl = baseUrl + '?' + requestParams.toString()
    console.log('DEM URL: ', fullUrl)
    return fullUrl
}
export const getGrid2DDEMUrl = (grid: GridData, bandPath: string) => {
    let baseUrl = `${titilerProxyEndPoint}/terrain/2dBox/{z}/{x}/{y}.png`
    const bbox = grid2bbox(grid.columnId, grid.rowId, grid.resolution)
    const requestParams = new URLSearchParams()
    requestParams.append('bbox', bbox.join(','))
    requestParams.append('url', getMinIOUrl(bandPath))
    requestParams.append('scale_factor', '0.5')
    const fullUrl = baseUrl + '?' + requestParams.toString()
    console.log('DEM URL: ', fullUrl)
    return fullUrl
}

/**
 * 格网红绿立体影像Url
 */
export function getGrid3DUrl(grid: GridData, param: RGBCompositeParams) {
    let baseUrl = `${titilerProxyEndPoint}/rgb/box/{z}/{x}/{y}.png`

    const bbox = grid2bbox(grid.columnId, grid.rowId, grid.resolution)

    const requestParams = new URLSearchParams()
    requestParams.append('bbox', bbox.join(','))
    requestParams.append('url_r', getMinIOUrl(param.redPath))
    requestParams.append('url_g', getMinIOUrl(param.greenPath))
    requestParams.append('url_b', getMinIOUrl(param.bluePath))
    requestParams.append('min_r', param.r_min.toString())
    requestParams.append('max_r', param.r_max.toString())
    requestParams.append('min_g', param.g_min.toString())
    requestParams.append('max_g', param.g_max.toString())
    requestParams.append('min_b', param.b_min.toString())
    requestParams.append('max_b', param.b_max.toString())
    if (param.normalize_level)
        requestParams.append('normalize_level', param.normalize_level.toString())
    if (param.stretch_method)
        requestParams.append('stretch_method', param.stretch_method.toString())
    if (param.nodata) requestParams.append('nodata', param.nodata.toString())
    if (param.std_config) requestParams.append('std_config', param.std_config.toString())
    // if (grid.opacity) requestParams.append('normalize_level', grid.opacity.toString())
    return baseUrl + '?' + requestParams.toString()
}

/**
 * 格网单波段图Url - NDVI或SVR
 */
export function getGridNDVIOrSVRUrl(grid: GridData, param: OneBandColorLayerParam) {
    let baseUrl = `${titilerProxyEndPoint}/oneband/box/{z}/{x}/{y}.png`

    const bbox = grid2bbox(grid.columnId, grid.rowId, grid.resolution)

    const requestParams = new URLSearchParams()
    requestParams.append('bbox', bbox.join(','))
    requestParams.append('url', getMinIOUrl(param.fullTifPath))
    requestParams.append('b_min', param?.min?.toString() || '0')
    requestParams.append('b_max', param?.max?.toString() || '255')
    if (param.nodata !== undefined && param.nodata !== null) {
        requestParams.append('nodata', param.nodata.toString())
    }
    if (grid.normalize_level)
        requestParams.append('normalize_level', grid.normalize_level.toString())

    return baseUrl + '?' + requestParams.toString()
}
