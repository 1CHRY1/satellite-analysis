import { mapManager } from '../mapManager'
import { ezStore } from '@/store'
import type { GridData } from '@/type/interactive-explore/grid'


/**
 * 0. 公用函数/初始化等
 */
function uid() {
    return Math.random().toString(36).substring(2, 15)
}

/**
 * 1. 格网探查 - 格网可视化
 */
export function map_addGridDEMLayer(
    gridInfo: GridData,
    url: string,
    cb?: () => void,
) {
    const prefix = gridInfo.rowId + '_' + gridInfo.columnId
    const id = prefix + uid()
    const srcId = id + '-source'

    if (!ezStore.get('grid-dem-layer-map')) {
        ezStore.set('grid-dem-layer-map', new window.Map())
    }

    map_destroyGridDEMLayer(gridInfo)

    mapManager.withMap((m) => {
        const gridDEMLayerMap = ezStore.get('grid-dem-layer-map')
        for (let key of gridDEMLayerMap.keys()) {
            if (key.includes(prefix)) {
                const oldId = key
                const oldSrcId = oldId + '-source'
                if (m.getLayer(oldId) && m.getSource(oldSrcId)) {
                    m.removeLayer(oldId)
                    m.removeSource(oldSrcId)
                    m.setTerrain(null)
                }
            }
        }

        m.addSource(srcId, {
            type: 'raster-dem',
            tiles: [url],
            tileSize: 256,
        })
        m.setTerrain({ source: srcId, exaggeration: 4.0 })
        setTimeout(() => {
            cb && cb()
        }, 5000)
    })
}
export function map_destroyGridDEMLayer(gridInfo: GridData) {
    const prefix = gridInfo.rowId + '_' + gridInfo.columnId
    const gridDEMLayerMap = ezStore.get('grid-dem-layer-map')

    mapManager.withMap((m) => {
        for (let key of gridDEMLayerMap.keys()) {
            if (key.includes(prefix)) { 
                const oldId = key
                const oldSrcId = oldId + '-source'
                if (m.getLayer(oldId) && m.getSource(oldSrcId)) {
                    m.removeLayer(oldId)
                    m.removeSource(oldSrcId)
                    m.setTerrain(null)  
                }
            }
        }
    })
}

export function map_addGridSceneLayer(
    gridInfo: GridData,
    url: string,
    cb?: () => void,
) {
    return map_addGrid3DLayer(gridInfo, url, cb)
}

export function map_destroyGridSceneLayer(gridInfo: GridData) {
    return map_destroyGrid3DLayer(gridInfo)
}

export function map_addGrid3DLayer(
    gridInfo: GridData,
    url: string,
    cb?: () => void,
) {
    const prefix = gridInfo.rowId + '_' + gridInfo.columnId
    const id = prefix + uid()
    const srcId = id + '-source'

    if (!ezStore.get('grid-image-layer-map')) {
        ezStore.set('grid-image-layer-map', new window.Map())
    }

    mapManager.withMap((m) => {
        const gridImageLayerMap = ezStore.get('grid-image-layer-map')
        for (let key of gridImageLayerMap.keys()) {
            if (key.includes(prefix)) {
                const oldId = key
                const oldSrcId = oldId + '-source'
                if (m.getLayer(oldId) && m.getSource(oldSrcId)) {
                    m.removeLayer(oldId)
                    m.removeSource(oldSrcId)
                }
            }
        }

        m.addSource(srcId, {
            type: 'raster',
            tiles: [url],
        })
        m.addLayer({
            id: id,
            type: 'raster',
            source: srcId,
            paint: {
                'raster-opacity': (100 - (gridInfo.opacity || 0))*0.01   // 设置透明度，值范围 0-1
            }
        })

        gridImageLayerMap.set(id, {
            id: id,
            source: srcId,
        })

        setTimeout(() => {
            cb && cb()
        }, 3000)
    })
}
export function map_destroyGrid3DLayer(gridInfo: GridData) {
    const prefix = gridInfo.rowId + '_' + gridInfo.columnId
    const gridImageLayerMap = ezStore.get('grid-image-layer-map')

    mapManager.withMap((m) => {
        for (let key of gridImageLayerMap.keys()) {
            if (key.startsWith(prefix)) {
                const oldId = key
                const oldSrcId = oldId + '-source'
                if (m.getLayer(oldId) && m.getSource(oldSrcId)) {
                    m.removeLayer(oldId)
                    m.removeSource(oldSrcId)
                }
            }
        }
    })
}

export function map_addGridNDVIOrSVRLayer(
    gridInfo: GridData,
    url: string,
    cb?: () => void,
) {
    const prefix = gridInfo.rowId + '_' + gridInfo.columnId
    const id = prefix + uid()
    const srcId = id + '-source'
    console.log(prefix)

    if (!ezStore.get('grid-oneband-layer-map')) {
        ezStore.set('grid-oneband-layer-map', new window.Map())
    }

    map_destroyGridNDVIOrSVRLayer(gridInfo)

    mapManager.withMap((m) => {
        const gridOneBandLayerMap = ezStore.get('grid-oneband-layer-map')
        for (let key of gridOneBandLayerMap.keys()) {
            if (key.includes(prefix)) {
                const oldId = key
                const oldSrcId = oldId + '-source'
                if (m.getLayer(oldId) && m.getSource(oldSrcId)) {
                    m.removeLayer(oldId)
                    m.removeSource(oldSrcId)
                }
            }
        }
        

        m.addSource(srcId, {
            type: 'raster',
            tiles: [url],
        })

        m.addLayer({
            id: id,
            type: 'raster',
            source: srcId,
            paint: {
                'raster-opacity': (100 - (gridInfo.opacity || 0))*0.01   // 设置透明度，值范围 0-1
            }
        })
        gridOneBandLayerMap.set(id, {
            id: id,
            source: srcId,
        })

        setTimeout(() => {
            cb && cb()
        }, 3000)
    })
}

export function map_destroyGridNDVIOrSVRLayer(gridInfo: GridData) {
    const prefix = gridInfo.rowId + '_' + gridInfo.columnId
    const gridOneBandLayerMap = ezStore.get('grid-oneband-layer-map')

    mapManager.withMap((m) => {
        for (let key of gridOneBandLayerMap.keys()) {
            if (key.startsWith(prefix)) {
                const oldId = key
                const oldSrcId = oldId + '-source'
                if (m.getLayer(oldId) && m.getSource(oldSrcId)) {
                    m.removeLayer(oldId)
                    m.removeSource(oldSrcId)
                }
            }
        }
    })
}

export function map_addGridMVTLayer(source_layer: string, url: string, cb?: () => void) {
    const prefix = 'GridMVT'
    let layeridStore: any = null
    if (!ezStore.get('GridMVTLayerIds')) ezStore.set('GridMVTLayerIds', [])

    layeridStore = ezStore.get('GridMVTLayerIds')

    map_destroyGridMVTLayer()

    mapManager.withMap((m) => {
        const id = prefix + uid()
        const srcId = id + '-source'
        if (m.getLayer(id) && m.getSource(srcId)) {
            m.removeLayer(id)
            m.removeSource(srcId)
        }

        layeridStore.push(id)

        m.addSource(srcId, {
            type: 'vector',
            tiles: [url],
        })

        m.addLayer({
            id: id,
            type: 'fill',
            source: srcId,
            'source-layer': source_layer,
            paint: {
                'fill-color': '#0066cc',
                'fill-opacity': 0.5,
            }
        })

        setTimeout(() => {
            cb && cb()
        }, 3000)
    })
}
export function map_destroyGridMVTLayer() {
    if (!ezStore.get('GridMVTLayerIds')) return

    const layeridStore = ezStore.get('GridMVTLayerIds')
    console.log(layeridStore)
    mapManager.withMap((m) => {
        for (let i = 0; i < layeridStore.length; i++) {
            const id = layeridStore[i]
            m.getLayer(id) && m.removeLayer(id)
            m.getSource(id + '-source') && m.removeSource(id + '-source')
        }
    })
}