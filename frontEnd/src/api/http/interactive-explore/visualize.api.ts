/**
 * Visualize API: 获取可视化服务URL
 */
import { ezStore } from "@/store"
import type { VectorUrlParam } from "./visualize.type"

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
    const { landId, source_layer, spatialFilterMethod } = vectorUrlParam
    let baseUrl = ''
    switch (spatialFilterMethod) {
        case 'region':
            baseUrl = `http://${window.location.hostname}:${window.location.port}${backProxyEndPoint}/data/vector/region/${landId}/${source_layer}/{z}/{x}/{y}`
            break
        case 'poi':
            baseUrl = `${backProxyEndPoint}/data/vector/poi/${landId}/${source_layer}/{z}/{x}/{y}`
            break
        default:
            baseUrl = `${backProxyEndPoint}/data/vector/region/${landId}/${source_layer}/{z}/{x}/{y}`
            break
    }
    const fullUrl = baseUrl
    console.log("Vector URL: ", fullUrl)
    return fullUrl
}

export const getDEMUrl = (themeName: string, gridsBoundary: any) => {
    let baseUrl = `${titilerProxyEndPoint}/terrain/terrainRGB/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    requestParams.append('themeName', themeName)
    requestParams.append('grids_boundary', JSON.stringify(gridsBoundary))
    const fullUrl = baseUrl + '?' + requestParams.toString()
    console.log("DEM URL: ", fullUrl)
    return fullUrl
}