import { ezStore } from '@/store'
import { grid2bbox } from '@/util/map/gridMaker'

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
export async function getGridImage(
    params: GridImageParams,
    imgResolution: number = 1,
): Promise<string> {
    const statisticsJson = await getImgStatistics(params.tifFullPath)
    // console.log(statisticsJson)

    const percentile_2 = statisticsJson.b1 ? statisticsJson.b1.min : 0
    const percentile_98 = statisticsJson.b1 ? statisticsJson.b1.max : 20000

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

export async function getTifbandMinMax(tifFullPath: string) {
    const statisticsJson = await getTifStatistic(tifFullPath)
    const min = statisticsJson.b1 ? statisticsJson.b1.min : 0
    const max = statisticsJson.b1 ? statisticsJson.b1.max : 20000
    return [min, max]
}

// 获取一张tif的预览图路径(已加入拉伸参数)
export async function getTifPreviewUrl(
    tifFullPath: string,
    resolution: number = 10,
    gridSize = 20000,
) {
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
    nodata?: number
}
export function getSceneRGBCompositeTileUrl(param: RGBTileLayerParams) {
    let baseUrl = `${titilerEndPoint}/rgb/tiles/{z}/{x}/{y}.png`

    const requestParams = new URLSearchParams()
    requestParams.append('url_r', minioEndPoint + '/' + param.redPath)
    requestParams.append('url_g', minioEndPoint + '/' + param.greenPath)
    requestParams.append('url_b', minioEndPoint + '/' + param.bluePath)
    requestParams.append('min_r', param.r_min.toString())
    requestParams.append('max_r', param.r_max.toString())
    requestParams.append('min_g', param.g_min.toString())
    requestParams.append('max_g', param.g_max.toString())
    requestParams.append('min_b', param.b_min.toString())
    requestParams.append('max_b', param.b_max.toString())
    // if (param.nodata) requestParams.append('nodata', param.nodata.toString())
    if (param.nodata !== undefined && param.nodata !== null) {
        requestParams.append('nodata', param.nodata.toString())
    }

    return baseUrl + '?' + requestParams.toString()
}

export function getGridRGBCompositeUrl(grid: GridInfoType, param: RGBTileLayerParams) {
    let baseUrl = `${titilerEndPoint}/rgb/box/{z}/{x}/{y}.png`

    const bbox = grid2bbox(grid.columnId, grid.rowId, grid.resolution)

    const requestParams = new URLSearchParams()
    requestParams.append('bbox', bbox.join(','))
    requestParams.append('url_r', minioEndPoint + '/' + param.redPath)
    requestParams.append('url_g', minioEndPoint + '/' + param.greenPath)
    requestParams.append('url_b', minioEndPoint + '/' + param.bluePath)
    requestParams.append('min_r', param.r_min.toString())
    requestParams.append('max_r', param.r_max.toString())
    requestParams.append('min_g', param.g_min.toString())
    requestParams.append('max_g', param.g_max.toString())
    requestParams.append('min_b', param.b_min.toString())
    requestParams.append('max_b', param.b_max.toString())
    if (param.nodata) requestParams.append('nodata', param.nodata.toString())

    return baseUrl + '?' + requestParams.toString()
}

// 获取一个tif的geojson
type BaseImageType = {
    bucket: string
    tifPath: string
    [key: string]: any
}

type BaseSceneType = {
    images: BaseImageType[]
    [key: string]: any
}
export async function getSceneGeojson(scene: BaseSceneType) {
    // const oneImgUrl = minioEndPoint + '/' + scene.images[0]
    const oneBandImage = scene.images[0]
    const oneImgFullPath = minioEndPoint + '/' + oneBandImage.bucket + '/' + oneBandImage.tifPath

    let baseUrl = `${titilerEndPoint}/info.geojson`
    const requestParams = new URLSearchParams()
    requestParams.append('url', oneImgFullPath)

    const httpUrl = baseUrl + '?' + requestParams.toString()

    const response = await fetch(httpUrl)
    const json = await response.json()

    return json
}

// 地形瓦片，实时构建的terrainRGB
/**
 *
 * @param terrainTifPath bucket + '/' + tifPath
 */
export function getTerrainRGBUrl(terrainTifPath: string) {
    // demo: /tiler/terrain/terrainRGB/{z}/{x}/{y}.png
    let baseUrl = `${titilerEndPoint}/terrain/terrainRGB/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    requestParams.append('url', minioEndPoint + '/' + terrainTifPath)

    const fullUrl = baseUrl + '?' + requestParams.toString()
    return fullUrl
}

// 单波段彩色产品

export function getOneBandColorUrl(oneBandColorTifPath: string) {
    let baseUrl = `${titilerEndPoint}/oneband/colorband/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    requestParams.append('url', minioEndPoint + '/' + oneBandColorTifPath)

    const fullUrl = baseUrl + '?' + requestParams.toString()
    return fullUrl
}

// 三波段的无云一版图

export async function getNoCloudScaleParam(noCloudTifPath: string) {
    const statistice = await getTifStatistic(noCloudTifPath)
    console.log(statistice)

    const band1Scale = statistice.b1.percentile_2 + ',' + statistice.b1.percentile_98
    const band2Scale = statistice.b2.percentile_2 + ',' + statistice.b2.percentile_98
    const band3Scale = statistice.b3.percentile_2 + ',' + statistice.b3.percentile_98

    return {
        band1Scale,
        band2Scale,
        band3Scale,
    }
}

type NoCloudInfoParam = {
    fullTifPath: string
    band1Scale: string
    band2Scale: string
    band3Scale: string
}
export function getNoCloudUrl(param: NoCloudInfoParam) {
    // http://localhost:8000/tiles/WebMercatorQuad/9/427/200?scale=1&format=png&url=http%3A%2F%2F223.2.32.166%3A30900%2Ftemp-files%2Faa0594b6-f6a1-4c08-a148-21dfb5ed193e%2FnoCloud_merge.tif&bidx=1&bidx=2&bidx=3&unscale=false&rescale=0%2C13000&rescale=0%2C13000&rescale=0%2C13000&return_mask=true

    // /tiles/{tileMatrixSetId}/{z}/{x}/{y}
    let baseUrl = `${titilerEndPoint}/tiles/WebMercatorQuad/{z}/{x}/{y}`

    const requestParams = new URLSearchParams()
    requestParams.append('url', minioEndPoint + '/' + param.fullTifPath)
    requestParams.append('scale', '1')
    requestParams.append('format', 'png')
    requestParams.append('bidx', '1')
    requestParams.append('bidx', '2')
    requestParams.append('bidx', '3')
    requestParams.append('unscale', 'false')
    requestParams.append('rescale', param.band1Scale)
    requestParams.append('rescale', param.band2Scale)
    requestParams.append('rescale', param.band3Scale)
    requestParams.append('return_mask', 'true')
    requestParams.append('nodata', '0')

    const fullUrl = baseUrl + '?' + requestParams.toString()

    return fullUrl
}

type MosaicTileParam = {
    mosaicJsonPath: string
}
export function getNoCloudUrl4MosaicJson(param: MosaicTileParam) {
    let baseUrl = `${titilerEndPoint}/mosaic/mosaictile/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    const mosaicUrl = minioEndPoint + '/' + param.mosaicJsonPath
    requestParams.append('mosaic_url', mosaicUrl)
    const fullUrl = baseUrl + '?' + requestParams.toString()
    return fullUrl
}
