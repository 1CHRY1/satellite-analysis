import { getGridImage } from "@/api/http/satellite-data/visualize.api";
import { ezStore } from "@/store";
import { grid2Coordinates } from "./gridMaker";
async function qk_preview(
    rowId: number,
    columnId: number,
    resolution: number,
    tifPath: string,
    bucket: string,
): Promise<string> {

    const tifFullPath = `/${bucket}/${tifPath}`;

    const imgPath = await getGridImage({
        rowId,
        columnId,
        resolution,
        tifFullPath,
    })

    return imgPath
}

export async function qk_addLayer(rowId: number,
    columnId: number,
    resolution: number) {
    const layerId = 'layer' + Math.random()
    const srcId = layerId + '-src'
    const m = ezStore.get('map')

    const imgData = getGridData(columnId, rowId)

    const imageUrl = await qk_preview(rowId, columnId, resolution, imgData.tifPath, imgData.bucket)

    const gridCoords = grid2Coordinates(columnId, rowId, resolution)

    m.addSource(srcId, {
        type: 'image',
        url: imageUrl,
        coordinates: gridCoords,
    })
    m.addLayer({
        id: layerId,
        type: 'raster',
        source: srcId,
        paint: {
            'raster-opacity': 0.9,
        },
    })
}

let gridData: any = null
export function setGridData(data: any) {
    gridData = data['noCloud']['tiles']
}

export function getGridData(columnId: number, rowId: number) {
    return gridData!.find(item => item.columnId === columnId && item.rowId === rowId)
}

/*
紧急说明：

1. 无云一般图的响应结果调用setGridData存储
2. 在外部就直接调用qk_addLayer传入rowId,columnId和resolution直接加载图层
*/

// 把下面的函数绑定到格网图层的点击事件上
function noCloudLayerClickHandler(e) {
    const clickGrid = e.features[0]
    if (!clickGrid) return
    
    const columnId = clickGrid.properties.columnId
    const rowId = clickGrid.properties.rowId
    const resolution = clickGrid.properties.resolution

    qk_addLayer(rowId, columnId, resolution)
}