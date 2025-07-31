/**
 * Visualize API: 获取可视化服务URL
 */
import { ezStore } from "@/store"
import type { VectorUrlParam } from "./visualize.type"
import { getThemeByThemeName } from "./filter.api"
import http from "@/api/axiosClient/tilerHttp"
import { message } from "ant-design-vue"

const titilerProxyEndPoint = ezStore.get('conf')['titiler']
const minioEndPoint = ezStore.get('conf')['minioIpAndPort']
const backProxyEndPoint = ezStore.get('conf')['back_app']

export const getSceneUrl = (sensorName: string) => {
    let baseUrl = `${titilerProxyEndPoint}/image_visualization/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    requestParams.append('sensorName', sensorName)
    const fullUrl = baseUrl + '?' + requestParams.toString()
    console.log("Scene URL: ", fullUrl)
    return fullUrl
}

export const getVectorUrl = (vectorUrlParam: VectorUrlParam) => {
    const { landId, source_layer, spatialFilterMethod, resolution } = vectorUrlParam
    let baseUrl = ''
    switch (spatialFilterMethod) {
        case 'region':
            baseUrl = `http://${window.location.hostname}:${window.location.port}${backProxyEndPoint}/data/vector/region/${landId}/${source_layer}/{z}/{x}/{y}`
            break
        case 'poi':
            baseUrl = `http://${window.location.hostname}:${window.location.port}${backProxyEndPoint}/data/vector/location/${landId}/${resolution}/${source_layer}/{z}/{x}/{y}`
            break
        default:
            baseUrl = `http://${window.location.hostname}:${window.location.port}${backProxyEndPoint}/data/vector/region/${landId}/${source_layer}/{z}/{x}/{y}`
            break
    }
    const fullUrl = baseUrl
    console.log("Vector URL: ", fullUrl)
    return fullUrl
}

export const getDEMUrl = async (themeName: string, gridsBoundary: any) => {
    let baseUrl = `${titilerProxyEndPoint}/terrain/terrainRGB/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    const themeRes = await getThemeByThemeName(themeName)
    const url = `${minioEndPoint}/${themeRes.data.images[0].bucket}/${themeRes.data.images[0].tifPath}`
    requestParams.append('url', url)
    requestParams.append('grids_boundary', JSON.stringify(gridsBoundary))
    requestParams.append('scale_factor', '0.5')
    const fullUrl = baseUrl + '?' + requestParams.toString()
    console.log("DEM URL: ", fullUrl)
    return fullUrl
}

export const getNDVIOrSVRUrl = async (themeName: string, gridsBoundary: any) => {
    let baseUrl = `${titilerProxyEndPoint}/oneband/colorband/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    const themeRes = await getThemeByThemeName(themeName)
    const url = `${minioEndPoint}/${themeRes.data.images[0].bucket}/${themeRes.data.images[0].tifPath}`
    requestParams.append('url', url)
    requestParams.append('grids_boundary', JSON.stringify(gridsBoundary))
    requestParams.append('nodata', themeRes.data.noData.toString())
    const fullUrl = baseUrl + '?' + requestParams.toString()
    console.log("NDVI or SVR URL: ", fullUrl)
    return fullUrl
}

export async function getImgStats(url: string): Promise<any> {
    return http.get(`/statistics?url=${url}`)
}

export const get3DUrl = async (themeName: string, gridsBoundary: any) => {
    const stopLoading = message.loading('正在加载，请稍后...', 0)
    setTimeout(() => {
        stopLoading()
    }, 5000)
    let baseUrl = `${titilerProxyEndPoint}/rgb/tiles/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    const themeRes = await getThemeByThemeName(themeName)
    const image_r = themeRes.data.images.find((item: any) => item.band === themeRes.data.bandMapper.Red)
    const image_g = themeRes.data.images.find((item: any) => item.band === themeRes.data.bandMapper.Green)
    const image_b = themeRes.data.images.find((item: any) => item.band === themeRes.data.bandMapper.Blue)
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