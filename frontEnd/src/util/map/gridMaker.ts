import type { polygonGeometry } from '@/util/share.type'
import { useGridStore } from '@/store/gridStore'

const EarthRadius = 6371008.8
const EarthCircumference = 2 * Math.PI * EarthRadius

export class GridMaker {
    private gridResolutionInMeter: number
    private gridNumX: number
    private gridNumY: number
    private areaLimitKm2: number

    constructor(gridResolutionInKilometer: number, areaLimitKm2?: number) {
        this.gridResolutionInMeter = gridResolutionInKilometer * 1000
        this.gridNumX = Math.ceil(EarthCircumference / this.gridResolutionInMeter)
        this.gridNumY = Math.ceil(EarthCircumference / 2.0 / this.gridResolutionInMeter)
        this.areaLimitKm2 = areaLimitKm2 || 2500
    }

    makeGrid(config: {
        polygon: polygonGeometry
        startCb: () => void
        endCb: () => void
        overboundCb: () => void
    }): GeoJSON.FeatureCollection | null {
        const { polygon, startCb, endCb, overboundCb } = config
        startCb && startCb()
        const startTime = new Date()
        const { topLeft, bottomRight } = calculateBbox(polygon)
        const [minLng, maxLat] = topLeft
        const [maxLng, minLat] = bottomRight
        const area = calculateGridArea(minLng, maxLat, maxLng, minLat) // 平方米
        if (area > this.areaLimitKm2 * 1000 * 1000) {
            console.warn(`💢 格网总面积：${area / 1000000} 平方公里了！`)
            overboundCb && overboundCb()
            return null
        }

        // 计算网格索引范围
        const startGridX = Math.floor(((minLng + 180) / 360) * this.gridNumX)
        const endGridX = Math.ceil(((maxLng + 180) / 360) * this.gridNumX)
        const startGridY = Math.floor(((90 - maxLat) / 180) * this.gridNumY)
        const endGridY = Math.ceil(((90 - minLat) / 180) * this.gridNumY)

        // 默认选中所有网格
        const gridIds = []

        const features = []
        for (let i = startGridX; i < endGridX; i++) {
            for (let j = startGridY; j < endGridY; j++) {
                const [leftLng, topLat] = grid2lnglat(i, j, this.gridNumX, this.gridNumY)
                const [rightLng, bottomLat] = grid2lnglat(
                    i + 1,
                    j + 1,
                    this.gridNumX,
                    this.gridNumY,
                )

                const gridPolygon: polygonGeometry = {
                    type: 'Polygon',
                    coordinates: [
                        [
                            [leftLng, topLat],
                            [rightLng, topLat],
                            [rightLng, bottomLat],
                            [leftLng, bottomLat],
                            [leftLng, topLat],
                        ],
                    ],
                }

                features.push({
                    type: 'Feature',
                    geometry: gridPolygon,
                    properties: {
                        columnId: i,
                        rowId: j,
                        id: `${i}-${j}`,
                    },
                    id: `${i}-${j}`,
                })
                gridIds.push(`${i}-${j}`)
            }
        }
        endCb && endCb()
        console.log(`格网生成完成，用时：${new Date().getTime() - startTime.getTime()}ms`)
        useGridStore().storeAllGrids(gridIds)//存储所有ids
        return {
            type: 'FeatureCollection',
            features: features,
        } as GeoJSON.FeatureCollection
    }
}

function calculateBbox(polygon: polygonGeometry) {
    const { coordinates } = polygon
    let minX = Infinity,
        minY = Infinity,
        maxX = -Infinity,
        maxY = -Infinity
    coordinates[0].forEach(([lng, lat]) => {
        if (lng < minX) minX = lng
        if (lng > maxX) maxX = lng
        if (lat < minY) minY = lat
        if (lat > maxY) maxY = lat
    })

    return {
        topLeft: [minX, maxY], // 左上角经纬度
        bottomRight: [maxX, minY], // 右下角经纬度
    }
}

function grid2lnglat(gridX: number, gridY: number, gridNumX: number, gridNumY: number) {
    const lng = (gridX / gridNumX) * 360.0 - 180.0
    const lat = 90.0 - (gridY / gridNumY) * 180.0
    return [lng, lat]
}

function calculateGridArea(
    leftLng: number,
    topLat: number,
    rightLng: number,
    bottomLat: number,
): number {
    // 使用Haversine公式计算网格的实际宽度和高度
    const width = calculateDistance(leftLng, topLat, rightLng, topLat);
    const height = calculateDistance(leftLng, topLat, leftLng, bottomLat);
    
    // 返回网格面积（平方米）
    return width * height;
}

// 使用Haversine公式计算两点间距离
function calculateDistance(lng1: number, lat1: number, lng2: number, lat2: number): number {
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLng = (lng2 - lng1) * Math.PI / 180;
    const a = 
        Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * 
        Math.sin(dLng/2) * Math.sin(dLng/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return EarthRadius * c;
}
