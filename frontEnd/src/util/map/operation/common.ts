import { mapManager } from '@/util/map/mapManager'
import { CN_Bounds } from '@/util/map/constant'

export function map_fitViewToCN(): void {
    mapManager.withMap((m) => {
        m.fitBounds(CN_Bounds, {
            duration: 700,
        })
    })
}

export function map_fitView(bounds: any): void {
    mapManager.withMap((m) => {
        m.fitBounds(bounds, {
            padding: 50,
            duration: 700,
        })
    })
}

export function getBoundsFromFeature(feature) {
    let geometry = feature.geometry
    let coordinates = []

    // 支持 Polygon 和 MultiPolygon
    if (geometry.type === 'Polygon') {
        coordinates = geometry.coordinates.flat(2)
    } else if (geometry.type === 'MultiPolygon') {
        coordinates = geometry.coordinates.flat(3)
    } else {
        console.warn('Unsupported geometry type:', geometry.type)
        return null
    }

    // 提取经纬度范围
    const lons = coordinates.map(coord => coord[0])
    const lats = coordinates.map(coord => coord[1])

    const minLng = Math.min(...lons)
    const minLat = Math.min(...lats)
    const maxLng = Math.max(...lons)
    const maxLat = Math.max(...lats)

    return [
        [minLng, minLat],
        [maxLng, maxLat]
    ]
}
