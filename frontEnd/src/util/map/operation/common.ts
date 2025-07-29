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