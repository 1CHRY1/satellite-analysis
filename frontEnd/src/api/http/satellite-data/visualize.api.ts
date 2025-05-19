import { ezStore } from "@/store";
import { grid2bbox } from "@/util/map/gridMaker";

const titilerEndPoint = ezStore.get('conf')['titiler']
const minioEndPoint = ezStore.get('conf')['minioIpAndPort']

// 在外部不拼接 minioEndPoint ， 此处统一拼接
type GridInfoType = {
    rowId: number
    columnId: number
    resolution: number
}

type GridImageParams = {
    tifFullPath: string
} & GridInfoType

// 返回可以作为layer source 的 url
export async function getGridImage(params: GridImageParams, imgResolution: number = 1): Promise<string> {

    const statisticsJson = await getImgStatistics(params.tifFullPath)
    // console.log(statisticsJson)

    const percentile_2 = statisticsJson.b1 ? statisticsJson.b1.min : 0;
    const percentile_98 = statisticsJson.b1 ? statisticsJson.b1.max : 20000;

    // const size = '' + gridSize / resolution
    // const width = params.resolution * 1000 / imgResolution
    // const height = params.resolution * 1000 / imgResolution
    const max_size = 1024

    const bbox = grid2bbox(params.columnId, params.rowId, params.resolution)
    let url = `${titilerEndPoint}/bbox/${bbox.join(',')}.png`

    const requestParams = new URLSearchParams()
    requestParams.append('url', minioEndPoint + '/' + params.tifFullPath)
    requestParams.append('rescale', percentile_2 + ',' + percentile_98)
    requestParams.append('max_size', '' + max_size)
    requestParams.append('return_mask', 'true')
    url += '?' + requestParams.toString()

    return url
}

export async function getImgStatistics(tifFullPath: string): Promise<any> {

    let url = `${titilerEndPoint}/statistics`

    const requestParams = new URLSearchParams()
    requestParams.append('url', minioEndPoint + '/' + tifFullPath)
    url += '?' + requestParams.toString()

    const response = await fetch(url)
    const json = await response.json()

    return json
}



// 获取一张tif的统计信息
async function getTifStatistic(tifFullPath: string) {
    let url = `${titilerEndPoint}/statistics`

    const requestParams = new URLSearchParams()
    requestParams.append('url', minioEndPoint + '/' + tifFullPath)
    url += '?' + requestParams.toString()

    const response = await fetch(url)
    const json = await response.json()

    return json
}

// 获取一张tif的拉伸参数(基于统计信息)
async function getTifScaleParam(tifFullPath: string) {
    const statisticsJson = await getTifStatistic(tifFullPath)
    const percentile_2 = statisticsJson.b1 ? statisticsJson.b1.percentile_2 : 0
    const percentile_98 = statisticsJson.b1 ? statisticsJson.b1.percentile_98 : 20000
    return percentile_2 + ',' + percentile_98
}

// 获取一张tif的预览图路径(已加入拉伸参数)
export async function getTifPreviewUrl(tifFullPath: string, resolution: number = 10, gridSize = 20000) {

    const rescale = await getTifScaleParam(tifFullPath)

    let url = `${titilerEndPoint}/preview`
    const requestParams = new URLSearchParams()
    requestParams.append('url', minioEndPoint + '/' + tifFullPath)
    requestParams.append('format', 'png')

    const size = '' + gridSize / resolution
    requestParams.append('max_size', size)
    requestParams.append('rescale', rescale)
    requestParams.append('return_mask', 'true')
    url += '?' + requestParams.toString()

    return url
}

// 获取一个格网的tif的预览图路径(已加入拉伸参数)
export async function getGridPreviewUrl(params: GridImageParams) {
    return getGridImage(params)
}


// 新的接口, RGB合成接口
// 这里主要提供一些URL合成方法, 交给mapbox
type RGBTileLayerParams = {
    redPath: string
    greenPath: string
    bluePath: string
    r_min: number
    r_max: number
    g_min: number
    g_max: number
    b_min: number
    b_max: number
}
export function getSceneRGBCompositeTileUrl(param: RGBTileLayerParams) {
    let baseUrl = `${titilerEndPoint}/rgb/tiles/{z}/{x}/{y}.png`

    const requestParams = new URLSearchParams()
    requestParams.append('url_r', minioEndPoint + '/' + param.redPath)
    requestParams.append('url_g', minioEndPoint + '/' + param.greenPath)
    requestParams.append('url_b', minioEndPoint + '/' + param.bluePath)
    requestParams.append('r_min', param.r_min.toString())
    requestParams.append('r_max', param.r_max.toString())
    requestParams.append('g_min', param.g_min.toString())
    requestParams.append('g_max', param.g_max.toString())
    requestParams.append('b_min', param.b_min.toString())
    requestParams.append('b_max', param.b_max.toString())

    return baseUrl + '?' + requestParams.toString()
}

export function getGridRGBCompositeUrl(grid: GridInfoType, param: RGBTileLayerParams) {
    let baseUrl = `${titilerEndPoint}/rgb/tiles/{z}/{x}/{y}.png`

    const bbox = grid2bbox(grid.columnId, grid.rowId, grid.resolution)

    const requestParams = new URLSearchParams()
    requestParams.append('bbox', bbox.join(','))
    requestParams.append('url_r', minioEndPoint + '/' + param.redPath)
    requestParams.append('url_g', minioEndPoint + '/' + param.greenPath)
    requestParams.append('url_b', minioEndPoint + '/' + param.bluePath)
    requestParams.append('r_min', param.r_min.toString())
    requestParams.append('r_max', param.r_max.toString())
    requestParams.append('g_min', param.g_min.toString())
    requestParams.append('g_max', param.g_max.toString())
    requestParams.append('b_min', param.b_min.toString())
    requestParams.append('b_max', param.b_max.toString())

    return baseUrl + '?' + requestParams.toString()
}
