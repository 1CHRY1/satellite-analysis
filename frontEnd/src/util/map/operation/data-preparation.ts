import { clickHandler, selectedGrid } from '@/components/dataCenter/noCloud/composables/useBox'
import { mapManager } from '../mapManager'
import { ezStore } from '@/store'
import bus from '@/store/bus'
import { watch, watchEffect } from 'vue'
import * as turf from '@turf/turf'

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
export function map_add3DBoxLayer(gridGeoJson, cubeSceneListByDate, boxJson): void {
    const id = '3d-box-layer'
    const srcId = id + '-source'
    const fillId = id + '-fill'
    console.log(gridGeoJson)
    const gridBbox = turf.bbox(gridGeoJson)
    const labelPosition = [gridBbox[2], gridBbox[1]] // 右下角 [maxLng, minLat]
    mapManager.withMap((map) => {
        const preset_colors = ['#845ec2', '#d65db1', '#ff6f91', '#ff9671', '#ffc75f', '#f9f871']
        console.log(cubeSceneListByDate)
        for (const [i, date] of boxJson.dimensionDates.entries()) {
            const levelSrcId = `${srcId}-${date}`
            map.addSource(levelSrcId, {
                type: 'geojson',
                data: {
                    type: 'FeatureCollection',
                    features: cubeSceneListByDate[date].map((scene) =>
                        turf.intersect(turf.featureCollection([scene.boundingBox, gridGeoJson])),
                    ),
                },
            })
            map.addLayer({
                id: `${id}-date-${i}`,
                type: 'fill-extrusion',
                source: levelSrcId,
                paint: {
                    'fill-extrusion-color': preset_colors[i % preset_colors.length],
                    'fill-extrusion-base': (i + 1) * 500, // 每层往上堆
                    'fill-extrusion-height': (i + 2) * 100, // 每层高度一样
                    'fill-extrusion-opacity': 0.35,
                },
            })

            // 添加标签图层的数据源
            const labelSrcId = `${srcId}-label-${date}`
            map.addSource(labelSrcId, {
                type: 'geojson',
                data: {
                    type: 'FeatureCollection',
                    features: [
                        {
                            type: 'Feature',
                            geometry: {
                                type: 'Point',
                                coordinates: labelPosition,
                            },
                            properties: {
                                date: date,
                                level: i,
                            },
                        },
                    ],
                },
            })

            // 添加标签图层 - 使用垂直偏移错开
            map.addLayer({
                id: `${id}-label-${i}`,
                type: 'symbol',
                source: labelSrcId,
                layout: {
                    'text-field': ['get', 'date'],
                    'text-font': ['Open Sans Bold', 'Arial Unicode MS Bold'],
                    'text-size': 12,
                    'text-anchor': 'bottom-left',
                    'text-offset': [0.5, -i * 1.5], // 垂直方向错开，每层向上偏移
                    'text-allow-overlap': true, // 允许重叠显示
                    'icon-allow-overlap': true,
                },
                paint: {
                    'text-color': preset_colors[i % preset_colors.length],
                    'text-halo-color': '#ffffff',
                    'text-halo-width': 2,
                    'text-opacity': 0.9,
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
    mapManager.withMap((m) => {
        const style = m.getStyle()
        if (!style) return
        const layers = style.layers || []
        layers.forEach((layer) => {
            if (layer.id.includes('3d-box-layer')) {
                m.getLayer(layer.id) && m.off('click', layer.id, clickHandler)
                m.getLayer(layer.id) && m.removeLayer(layer.id)
            }
        })
        const sources = Object.keys(style.sources || {})
        sources.forEach((sourceId) => {
            if (sourceId.includes('3d-box-layer-source')) {
                m.removeSource(sourceId)
                console.log(`已移除数据源: ${sourceId}`)
            }
        })
    })
}
