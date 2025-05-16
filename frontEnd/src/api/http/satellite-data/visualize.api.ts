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

export async function getGridImage(params: GridImageParams): Promise<string> {

    const bbox = grid2bbox(params.columnId, params.rowId, params.resolution)
    let url = `${titilerEndPoint}/bbox/${bbox.join(',')}.png`

    const requestParams = new URLSearchParams()
    requestParams.append('url', minioEndPoint + params.tifFullPath)
    // requestParams.append('colormap_name', 'cool')
    url += '?' + requestParams.toString()


    const response = await fetch(url)
    const blob = await response.blob()

    return URL.createObjectURL(blob)
}


export async function getImgStatistics(tifFullPath: string): Promise<any> {

    let url = `${titilerEndPoint}/statistics`

    const requestParams = new URLSearchParams()
    requestParams.append('url', minioEndPoint + tifFullPath)
    url += '?' + requestParams.toString()


    const response = await fetch(url)
    const blob = await response.blob()

    return blob

}