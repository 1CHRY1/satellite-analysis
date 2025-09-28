import { clickHandler, selectedGrid } from '@/components/dataCenter/noCloud/composables/useBox'
import { mapManager } from '../mapManager'
import { ezStore } from '@/store'
import bus from '@/store/bus'
import { watch, watchEffect } from 'vue'

/**
 * 添加网格图层，同时为初始格网图层（未填充）所用
 * @param gridGeoJson grid的geojson
 */
export function map_addGridLayer(gridGeoJson: GeoJSON.FeatureCollection): void {
    const id = 'grid-layer'
    const fillId = id + '-fill'
    const lineId = id + '-line'
    const highlightId = id + '-highlight'
    const srcId = id + '-source'

    mapManager.withMap((m) => {
        ezStore.set('map', m)

        // Add a geojson source
        m.addSource(srcId, {
            type: 'geojson',
            data: gridGeoJson,
        })
        // Add a line layer for **grid line visualization**
        m.addLayer({
            id: lineId,
            type: 'line',
            source: srcId,
            paint: {
                'line-color': '#F00000',
                'line-width': 1,
                'line-opacity': 0.3,
            },
        })
        // Add a invisible fill layer for **grid picking**
        m.addLayer({
            id: fillId,
            type: 'fill',
            source: srcId,
            paint: {
                'fill-color': '#00FFFF',
                'fill-opacity': ['coalesce', ['to-number', ['get', 'opacity']], 0.01],
            },
        })

        // Add a filterable fill layer for **grid highlighting**
        // const nowSelectedGrids = Array.from(gridStore.selectedGrids) || ['']
        m.addLayer({
            id: highlightId,
            type: 'fill',
            source: srcId,
            paint: {
                // 'fill-color': '#FF9900',
                'fill-color': '#0000FF',
                'fill-opacity': 0.3,
            },
            // filter: ['in', 'id', ...nowSelectedGrids],
            filter: ['in', 'id', ''],
        })

        // 左键点击 fill 区域，更新高亮
        m.on('click', fillId, clickHandler)

        // Add a click event listener to the invisible fill layer
        m.on('contextmenu', fillId, () => {
            // TODO
        })

        // ezStore.set('grid-layer-cancel-watch', cancelWatch)
        ezStore.set('grid-layer-fill-id', fillId)
        ezStore.set('grid-layer-line-id', lineId)
        ezStore.set('grid-layer-highlight-id', highlightId)
        ezStore.set('grid-layer-source-id', srcId)
    })
}

/**
 * 删除网格图层
 */
export function map_destroyGridLayer(): void {
    const gridLayer = ezStore.get('grid-layer-fill-id')
    const gridLineLayer = ezStore.get('grid-layer-line-id')
    const gridHighlightLayer = ezStore.get('grid-layer-highlight-id')
    const gridSourceId = ezStore.get('grid-layer-source-id')
    const cancelWatch = ezStore.get('grid-layer-cancel-watch')

    mapManager.withMap((m) => {
        gridLayer && m.getLayer(gridLayer) && m.off('click', gridLayer, clickHandler)
        gridLayer && m.getLayer(gridLayer) && m.removeLayer(gridLayer)
        gridLineLayer && m.getLayer(gridLineLayer) && m.removeLayer(gridLineLayer)
        gridHighlightLayer && m.getLayer(gridHighlightLayer) && m.removeLayer(gridHighlightLayer)
        gridSourceId && m.getSource(gridSourceId) && m.removeSource(gridSourceId)
        cancelWatch && cancelWatch()
        ezStore.delete('grid-layer-fill-id')
        ezStore.delete('grid-layer-line-id')
        ezStore.delete('grid-layer-highlight-id')
        ezStore.delete('grid-layer-source-id')
        ezStore.delete('grid-layer-cancel-watch')
    })
}

/**
 * 添加立方体图层
 * @param gridGeoJson 立方体的GeoJSON
 * @param boxJson 立方体对应的时序立方体数据JSON
 */
export function map_add3DBoxLayer(gridGeoJson, boxJson): void {
    const id = '3d-box-layer'
    const srcId = id + '-source'
    const fillId = id + '-fill'
    mapManager.withMap((map) => {
        // 添加数据源
        map.addSource(srcId, {
            type: 'geojson',
            data: gridGeoJson,
        })
        const preset_colors = ['#845ec2', '#d65db1', '#ff6f91', '#ff9671', '#ffc75f', '#f9f871']
        for (const [i, date] of boxJson.Dimension_Dates.entries()) {
            map.addLayer({
                id: `${id}-date-${i}`,
                type: 'fill-extrusion',
                source: srcId,
                paint: {
                    'fill-extrusion-color': preset_colors[i % preset_colors.length],
                    'fill-extrusion-base': i * 100,        // 每层往上堆
                    'fill-extrusion-height': (i + 1) * 100, // 每层高度一样
                    'fill-extrusion-opacity': 0.45,
                },
            })
        }

        // 设置摄像机视角为 3D
        map.setPitch(60) // 倾斜角度
        map.setBearing(-20) // 旋转方向
        // map.setCenter([120.5, 30.5]) // 中心点
        // map.setZoom(13)
    })
}

/**
 * 删除立体网格图层
 */
export function map_destrod3DBoxLayer(): void {
    const gridLayer = ezStore.get('3d-box-layer')
    const gridSourceId = ezStore.get('3d-box-layer-source')

    mapManager.withMap((m) => {
        gridLayer && m.getLayer(gridLayer) && m.off('click', gridLayer, clickHandler)
        gridLayer && m.getLayer(gridLayer) && m.removeLayer(gridLayer)
        gridSourceId && m.getSource(gridSourceId) && m.removeSource(gridSourceId)
    })
}
