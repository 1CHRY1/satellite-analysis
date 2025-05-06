import { mapManager, initMap, type Style } from './mapManager'
import { Popup, GeoJSONSource, MapMouseEvent } from 'mapbox-gl'
import { CN_Bounds } from './constant'
// import { type Image } from '@/types/satellite'
// import { type polygonGeometry } from '@/types/sharing'
import { watch } from 'vue'
import type { polygonGeometry } from '../share.type'
import { ezStore, useGridStore } from '@/store'

////////////////////////////////////////////////////////
/////// Map Operation //////////////////////////////////

const gridStore = useGridStore()
let resizeObserver: ResizeObserver | null = null

export async function map_initiliaze(
    id: string,
    style: Style = 'local',
    proj: 'mercator' | 'globe' = 'mercator',
) {
    // return initMap(id)
    setTimeout(() => {
        initMap(id, style, proj).then((m) => {
            m.resize()
            const container = document.getElementById(id)
            if (container) {
                resizeObserver = new ResizeObserver(() => {
                    m.resize()
                })
                resizeObserver.observe(container)
            }
            m.fitBounds(CN_Bounds, {
                linear: true,
                animate: true,
                duration: 1000,
            })
        })
    }, 0)
}

export async function map_destroy() {
    if (resizeObserver) {
        resizeObserver.disconnect()
        resizeObserver = null
    }
    mapManager.destroy()
}

export function map_checkoutStyle(s: Style): void {
    mapManager.withMap((m) => {
        m.setStyle(s)
    })
}

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

export function map_fitViewToFeature(feature: polygonGeometry): void {
    console.log(feature)
    const coordinates = feature.coordinates[0]
    const bbox = coordinates.reduce(
        (acc, coord) => {
            acc[0] = Math.min(acc[0], coord[0])
            acc[1] = Math.min(acc[1], coord[1])
            acc[2] = Math.max(acc[2], coord[0])
            acc[3] = Math.max(acc[3], coord[1])
            return acc
        },
        [Infinity, Infinity, -Infinity, -Infinity],
    )

    mapManager.withMap((m) => {
        m.fitBounds([bbox[0], bbox[1], bbox[2], bbox[3]], {
            padding: 100,
            duration: 900,
        })
    })
}

export function map_zoomIn(): void {
    mapManager.withMap((m) => {
        m.zoomIn()
    })
}

export function map_zoomOut(): void {
    mapManager.withMap((m) => {
        m.zoomOut()
    })
}

export function map_flyTo([lng, lat]: [number, number]): void {
    mapManager.withMap((m) => {
        m.flyTo({
            center: [lng, lat],
            zoom: 8,
            animate: true,
        })
    })
}

export function removeRasterLayer(layerId: string = 'raster-layer'): void {
    mapManager.withMap((m) => {
        if (m.getLayer(layerId)) {
            m.removeLayer(layerId)
        }
        if (m.getSource(layerId)) {
            m.removeSource(layerId)
        }
    })
}

////////////////////////////////////////////////////////
/////// Draw Operation //////////////////////////////////
export function draw_deleteAll(): void {
    mapManager.withDraw((d) => {
        d.deleteAll()
        d.changeMode('simple_select')
    })
}

export function draw_polygonMode(): void {
    mapManager.withDraw((d) => {
        d.deleteAll()
        d.changeMode('draw_polygon')
    })
}

export function draw_pointMode(): void {
    mapManager.withDraw((d) => {
        d.deleteAll()
        d.changeMode('draw_point')
    })
}

export function getCurrentGeometry(): polygonGeometry {
    if (ezStore.get('polygonFeature')) {
        return ezStore.get('polygonFeature') as polygonGeometry
    }
    return {
        type: 'Polygon',
        coordinates: [
            [
                [0, 85],
                [0, -85],
                [180, -85],
                [180, 85],
                [0, 85],
            ],
        ],
    }
}

////////////////////////////////////////////////////////
/////// Layer Operation ////////////////////////////////

export function addRasterLayerFromUrl(url: string, layerId: string = 'raster-layer'): void {
    mapManager.withMap((m) => {
        // 检查是否已经存在同名图层，避免重复添加
        if (m.getLayer(layerId)) {
            console.warn(`图层 "${layerId}" 已存在，跳过添加。`)
            return
        }

        // 添加栅格数据源
        m.addSource(layerId, {
            type: 'raster',
            tiles: [url], // 瓦片服务的 URL 模板
            tileSize: 256, // 瓦片尺寸，默认为 256x256
            crossOrigin: 'anonymous',
        })

        // 添加栅格图层
        m.addLayer({
            id: layerId,
            type: 'raster',
            source: layerId,
            paint: {}, // 可以在这里自定义渲染样式
        })

        console.log(`图层 "${layerId}" 已成功添加到地图。`)
    })
}

// Data-View:: image-polygon-layer
export function map_showImagePolygon(geoFeature: polygonGeometry): void {
    let id = 'image-polygon'
    let srcId = id + '-source'
    let polygonGeojson = {
        type: 'Feature',
        geometry: geoFeature,
        properties: {},
    }

    mapManager.withMap((m) => {
        if (m.getLayer(id) && m.getSource(srcId)) {
            let source = m.getSource(srcId) as GeoJSONSource
            source.setData(geoFeature)
        } else {
            m.addSource(srcId, {
                type: 'geojson',
                data: polygonGeojson as any,
            })

            m.addLayer({
                id: id,
                type: 'fill',
                source: srcId,
                paint: {
                    'fill-color': '#FFFF00',
                    'fill-opacity': 0.3,
                    'fill-outline-color': '#000000',
                },
            })
            ezStore.set('image-polygon-layer', id)
            ezStore.set('image-polygon-source', srcId)
        }
    })
}
export function map_destroyImagePolygon(): void {
    const id = ezStore.get('image-polygon-layer')
    const srcId = ezStore.get('image-polygon-source')
    mapManager.withMap((m) => {
        id && m.getLayer(id) && m.removeLayer(id)
        srcId && m.getSource(srcId) && m.removeSource(srcId)
        ezStore.delete('image-polygon-layer')
        ezStore.delete('image-polygon-source')
    })
}

// Data-View:: image-preview-layer
type ImageLayerProp = {
    imageUrl: string
    boxCoordinates: [[number, number], [number, number], [number, number], [number, number]]
}
export function map_addImagePreviewLayer(props: ImageLayerProp): void {
    const id = 'image-preview-layer'
    const srcId = id + '-source'
    mapManager.withMap((m) => {
        if (m.getLayer(id) && m.getSource(srcId)) {
            m.removeLayer(id)
            m.removeSource(srcId)
        }

        m.addSource(srcId, {
            type: 'image',
            url: props.imageUrl,
            coordinates: props.boxCoordinates,
        })
        m.addLayer({
            id: id,
            type: 'raster',
            source: srcId,
            paint: {
                'raster-opacity': 0.9,
            },
        })
        ezStore.set('image-preview-layer', id)
        ezStore.set('image-preview-source', srcId)
    })
}
export function map_destroyImagePreviewLayer(): void {
    const id = ezStore.get('image-preview-layer')
    const srcId = ezStore.get('image-preview-source')
    mapManager.withMap((m) => {
        id && m.getLayer(id) && m.removeLayer(id)
        srcId && m.getSource(srcId) && m.removeSource(srcId)
        ezStore.delete('image-preview-layer')
        ezStore.delete('image-preview-source')
    })
}

function grid_fill_click_handler(e: MapMouseEvent): void {
    const features = e.features!
    if (features.length) {
        const featureId = features[0].properties?.id
        // popup.setLngLat(e.lngLat).setText(text).addTo(m)

        // Toggle highlight
        if (gridStore.selectedGrids.includes(featureId)) {
            gridStore.removeGrid(featureId)
        } else {
            gridStore.addGrid(featureId)
        }
    }
}

// Data-View:: grid-layer
export function map_addGridLayer(gridGeoJson: GeoJSON.FeatureCollection): void {
    const id = 'grid-layer'
    const fillId = id + '-fill'
    const lineId = id + '-line'
    const highlightId = id + '-highlight'
    const srcId = id + '-source'

    mapManager.withMap((m) => {
        // Add a popup to show grid info
        // const popup = new mapboxgl.Popup({
        //     closeButton: false,
        //     closeOnMove: true,
        // })
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
                'fill-color': '#FF0000',
                'fill-opacity': 0.01,
            },
        })
        // Add a filterable fill layer for **grid highlighting**
        const nowSelectedGrids = Array.from(gridStore.selectedGrids) || ['']
        m.addLayer({
            id: highlightId,
            type: 'fill',
            source: srcId,
            paint: {
                'fill-color': '#FF9900',
                'fill-opacity': 0.3,
            },
            filter: ['in', 'id', ...nowSelectedGrids],
        })

        // Add a click event listener to the invisible fill layer
        m.on('click', fillId, grid_fill_click_handler)

        // Keep Watching gridStore.selectedGrids and update the highlight layer
        const cancelWatch = watch(
            () => gridStore.selectedGrids,
            () => {
                const selectedGrids = Array.from(gridStore.selectedGrids) || ['']
                m.setFilter(highlightId, ['in', 'id', ...selectedGrids])
            },
        )

        ezStore.set('grid-layer-cancel-watch', cancelWatch)
        ezStore.set('grid-layer-fill-id', fillId)
        ezStore.set('grid-layer-line-id', lineId)
        ezStore.set('grid-layer-highlight-id', highlightId)
        ezStore.set('grid-layer-source-id', srcId)
    })
}

// Data-View:: grid-layer
export function map_destroyGridLayer(): void {
    const gridLayer = ezStore.get('grid-layer-fill-id')
    const gridLineLayer = ezStore.get('grid-layer-line-id')
    const gridHighlightLayer = ezStore.get('grid-layer-highlight-id')
    const gridSourceId = ezStore.get('grid-layer-source-id')
    const cancelWatch = ezStore.get('grid-layer-cancel-watch')

    mapManager.withMap((m) => {
        gridLayer && m.getLayer(gridLayer) && m.off('click', gridLayer, grid_fill_click_handler)
        gridLayer && m.getLayer(gridLayer) && m.removeLayer(gridLayer)
        gridLineLayer && m.getLayer(gridLineLayer) && m.removeLayer(gridLineLayer)
        gridHighlightLayer && m.getLayer(gridHighlightLayer) && m.removeLayer(gridHighlightLayer)
        gridSourceId && m.getSource(gridSourceId) && m.removeSource(gridSourceId)
        cancelWatch && cancelWatch()
        gridStore.cleadAllGrids()
        ezStore.delete('grid-layer-fill-id')
        ezStore.delete('grid-layer-line-id')
        ezStore.delete('grid-layer-highlight-id')
        ezStore.delete('grid-layer-source-id')
        ezStore.delete('grid-layer-cancel-watch')
    })
}
