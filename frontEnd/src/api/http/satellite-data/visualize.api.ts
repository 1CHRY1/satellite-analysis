import { ezStore } from "@/store";
import { grid2bbox } from "@/util/map/gridMaker";

const titilerEndPoint = ezStore.get('conf')['titiler']
const minioEndPoint = ezStore.get('conf')['minioIpAndPort']

type GridImageParams = {
    rowId: number
    columnId: number
    resolution: number
    tifFullPath: string
}

// 返回可以作为layer source 的 url
export async function getGridImage(params: GridImageParams): Promise<string> {


    const statisticsJson = await getImgStatistics(params.tifFullPath)
    console.log(statisticsJson)

    const percentile_2 = statisticsJson.b1 ? statisticsJson.b1.min : 0;
    const percentile_98 = statisticsJson.b1 ? statisticsJson.b1.max : 20000;


    const bbox = grid2bbox(params.columnId, params.rowId, params.resolution)
    let url = `${titilerEndPoint}/bbox/${bbox.join(',')}.png`

    const requestParams = new URLSearchParams()
    requestParams.append('url', minioEndPoint + params.tifFullPath)
    requestParams.append('rescale', percentile_2 + ',' + percentile_98)
    requestParams.append('max_size', '512')
    requestParams.append('return_mask', 'true')
    url += '?' + requestParams.toString()

    return url
}

export async function getImgStatistics(tifFullPath: string): Promise<any> {

    let url = `${titilerEndPoint}/statistics`

    const requestParams = new URLSearchParams()
    requestParams.append('url', minioEndPoint + tifFullPath)
    url += '?' + requestParams.toString()

    const response = await fetch(url)
    const json = await response.json()

    return json
}



// 获取一张tif的统计信息
async function getTifStatistic(tifFullPath: string) {
    let url = `${titilerEndPoint}/statistics`

    const requestParams = new URLSearchParams()
    requestParams.append('url', tifFullPath)
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
export async function getTifPreviewUrl(tifFullPath: string) {

    const rescale = await getTifScaleParam(tifFullPath)

    let url = `${titilerEndPoint}/preview`
    const requestParams = new URLSearchParams()
    requestParams.append('url', tifFullPath)
    requestParams.append('format', 'png')
    requestParams.append('max_size', '512')
    requestParams.append('rescale', rescale)
    requestParams.append('return_mask', 'true')
    url += '?' + requestParams.toString()

    return url
}

// 获取一个格网的tif的预览图路径(已加入拉伸参数)
export async function getGridPreviewUrl(params: GridImageParams) {
    return getGridImage(params)
}
