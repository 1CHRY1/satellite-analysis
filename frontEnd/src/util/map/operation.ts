import { mapManager, initMap, type Style } from './mapManager'
import mapboxgl from 'mapbox-gl'
import { Popup, GeoJSONSource, MapMouseEvent } from 'mapbox-gl'
import { CN_Bounds } from './constant'
// import { type Image } from '@/types/satellite'
// import { type polygonGeometry } from '@/types/sharing'
// import { watch } from 'vue'
import type { polygonGeometry } from '../share.type'
import { ezStore, useGridStore } from '@/store'
import Antd from 'ant-design-vue'
import { createApp, type ComponentInstance, ref, type Ref, reactive } from 'vue'
import PopoverContent, { type GridData } from '@/components/feature/map/popoverContent.vue'
import bus from '@/store/bus'
import {
    getSceneRGBCompositeTileUrl,
    getGridRGBCompositeUrl,
    getTerrainRGBUrl,
    getOneBandColorUrl,
    getNoCloudUrl,
} from '@/api/http/satellite-data/visualize.api'

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

export function draw_lineMode(): void {
    mapManager.withDraw((d) => {
        d.deleteAll()
        d.changeMode('draw_line_string')
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
/////// Grid Popup ////////////////////////////////

function createPopoverContent() {
    const div = document.createElement('div')
    div.id = 'popover-content'
    document.body.appendChild(div)

    const app = createApp(PopoverContent, {
        // gridData: gridDataRef,
    }).use(Antd)
    app.mount('#popover-content') as ComponentInstance<typeof PopoverContent>
    return div
}

////////////////////////////////////////////////////////
/////// Layer Operation ////////////////////////////////

// 添加一个矢量图层，用来显示选中的行政区
export function map_addPolygonLayer(options: {
    geoJson: GeoJSON.FeatureCollection | any
    id: string
    showFill?: boolean
    lineColor?: string
    fillColor?: string
    fillOpacity?: number
    onClick?: (feature: GeoJSON.Feature) => void
}) {
    const {
        geoJson,
        id,
        lineColor = '#00FFFF',
        fillColor = '#00FFFF',
        fillOpacity = 0.05,
        onClick,
    } = options

    const fillId = `${id}-fill`
    const lineId = `${id}-line`
    const sourceId = `${id}-source`

    mapManager.withMap((map) => {
        // 👉 移除已存在的图层和数据源
        if (map.getLayer(fillId)) map.removeLayer(fillId)
        if (map.getLayer(lineId)) map.removeLayer(lineId)
        if (map.getSource(sourceId)) map.removeSource(sourceId)

        // 添加新的 source
        map.addSource(sourceId, {
            type: 'geojson',
            data: geoJson,
        })

        // if (options.showFill)
        // 添加填充层
        map.addLayer({
            id: fillId,
            type: 'fill',
            source: sourceId,
            paint: {
                'fill-color': fillColor,
                'fill-opacity': fillOpacity,
            },
        })

        // 添加边界线层
        map.addLayer({
            id: lineId,
            type: 'line',
            source: sourceId,
            paint: {
                'line-color': lineColor,
                'line-width': 4,
            },
        })

        // 绑定点击事件
        // if (onClick) {
        //     map.on('click', fillId, (e) => {
        //         const features = map.queryRenderedFeatures(e.point, {
        //             layers: [fillId],
        //         })
        //         if (features.length > 0) {
        //             onClick(features[0])
        //         }
        //     })
        // }
    })
}

// export function map_addPointLayer(
//     coord: [number, number], // [lon, lat]
// ) {
//     const sourceId = 'uniquePOI-source'
//     const layerId = 'uniquePOI-layer'
//     const geoJson = {
//         type: 'FeatureCollection',
//         features: [
//             {
//                 type: 'Feature',
//                 geometry: {
//                     type: 'Point',
//                     coordinates: coord,
//                 },
//                 properties: {},
//             },
//         ],
//     }

//     mapManager.withMap((map) => {
//         // 👉 清理旧图层和数据源
//         if (map.getLayer(layerId)) map.removeLayer(layerId)
//         if (map.getSource(sourceId)) map.removeSource(sourceId)

//         addPOIPoint(map, 120.123456, 36.123456)

//         // 👉 添加数据源
//         map.addSource(sourceId, {
//             type: 'geojson',
//             data: geoJson,
//         })

//         // 👉 添加图层：五角星样式（symbol layer）
//         map.addLayer({
//             id: layerId,
//             type: 'symbol',
//             source: sourceId,
//             layout: {
//                 'icon-image': 'satellite-icon', // 使用 mapbox 内置五角星图标
//                 'icon-size': 1.5,
//                 'icon-allow-overlap': true,
//             },
//             paint: {
//                 'icon-color': '#FF0000',
//             },
//         })
//     })
// }
export function addPOIPoint(map: mapboxgl.Map, lng: number, lat: number) {
    const iconId = 'satellite-icon'

    // 1. 加载 SVG 图像
    const img = new Image(30, 30)
    img.onload = () => {
        if (!map.hasImage(iconId)) {
            map.addImage(iconId, img)
        }

        // 2. 添加数据源
        if (!map.getSource('poi-source')) {
            map.addSource('poi-source', {
                type: 'geojson',
                data: {
                    type: 'FeatureCollection',
                    features: [
                        {
                            type: 'Feature',
                            geometry: {
                                type: 'Point',
                                coordinates: [lng, lat],
                            },
                            properties: {},
                        },
                    ],
                },
            })
        }

        // 3. 添加图层（点图标）
        if (!map.getLayer('uniquePOI')) {
            map.addLayer({
                id: 'uniquePOI',
                type: 'symbol',
                source: 'poi-source',
                layout: {
                    'icon-image': iconId,
                    'icon-size': 1,
                    'icon-anchor': 'bottom',
                },
            })
        }
    }

    // 4. 设置 SVG 图标路径（来自 public 目录）
    img.src = '/satelite.svg' // ⚠️ 路径以 / 开头
}
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
type GridInfoType = {
    rowId: number
    columnId: number
    resolution: number
}
type RGBTileLayerParams = {
    redPath: string
    greenPath: string
    bluePath: string
    r_min: number
    r_max: number
    g_min: number
    g_max: number
    b_min: number
    b_max: number
    nodata?: number
}
export function map_addRGBImageTileLayer(param: RGBTileLayerParams, cb?: () => void) {
    const id = 'rgb-image-tile-layer'
    const srcId = id + '-source'

    mapManager.withMap((m) => {
        if (m.getLayer(id) && m.getSource(srcId)) {
            m.removeLayer(id)
            m.removeSource(srcId)
        }

        const tileUrl = getSceneRGBCompositeTileUrl(param)

        m.addSource(srcId, {
            type: 'raster',
            tiles: [tileUrl],
        })
        m.addLayer({
            id: id,
            type: 'raster',
            source: srcId,
        })

        setTimeout(() => {
            cb && cb()
        }, 1000)
    })
}
export function map_destroyRGBImageTileLayer() {
    const id = 'rgb-image-tile-layer'
    const srcId = id + '-source'
    mapManager.withMap((m) => {
        if (m.getLayer(id) && m.getSource(srcId)) {
            m.removeLayer(id)
            m.removeSource(srcId)
        }
    })
}

export function map_addMultiRGBImageTileLayer(params: RGBTileLayerParams[], cb?: () => void) {
    const prefix = 'MultiRGB'
    let layeridStore: any = null
    if (!ezStore.get('MultiRGBLayerIds')) ezStore.set('MultiRGBLayerIds', [])

    layeridStore = ezStore.get('MultiRGBLayerIds')

    map_destroyMultiRGBImageTileLayer()

    mapManager.withMap((m) => {
        for (let i = 0; i < params.length; i++) {
            const id = prefix + uid()
            const srcId = id + '-source'
            if (m.getLayer(id) && m.getSource(srcId)) {
                m.removeLayer(id)
                m.removeSource(srcId)
            }

            layeridStore.push(id)

            const tileUrl = getSceneRGBCompositeTileUrl(params[i])

            m.addSource(srcId, {
                type: 'raster',
                tiles: [tileUrl],
            })
            m.addLayer({
                id: id,
                type: 'raster',
                source: srcId,
            })
        }

        setTimeout(() => {
            cb && cb()
        }, 3000)
    })
}
export function map_destroyMultiRGBImageTileLayer() {
    if (!ezStore.get('MultiRGBLayerIds')) return

    const layeridStore = ezStore.get('MultiRGBLayerIds')

    mapManager.withMap((m) => {
        for (let i = 0; i < layeridStore.length; i++) {
            const id = layeridStore[i]
            m.getLayer(id) && m.removeLayer(id)
            m.getSource(id + '-source') && m.removeSource(id + '-source')
        }
    })
}

export function map_addGridRGBImageTileLayer(
    gridInfo: GridInfoType,
    param: RGBTileLayerParams,
    cb?: () => void,
) {
    const prefix = '' + gridInfo.rowId + gridInfo.columnId
    const id = prefix + uid()
    const srcId = id + '-source'
    console.log(prefix)

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

        const tileUrl = getGridRGBCompositeUrl(gridInfo, param)

        m.addSource(srcId, {
            type: 'raster',
            tiles: [tileUrl],
        })
        m.addLayer({
            id: id,
            type: 'raster',
            source: srcId,
        })

        gridImageLayerMap.set(id, {
            id: id,
            source: srcId,
        })

        setTimeout(() => {
            cb && cb()
        }, 2000)
    })
}
export function map_destroyGridRGBImageTileLayer(gridInfo: GridInfoType) {
    const prefix = '' + gridInfo.rowId + gridInfo.columnId
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

function uid() {
    return Math.random().toString(36).substring(2, 15)
}

export function map_addGridPreviewLayer(img: string, coords: number[][], prefix: string) {
    const gridPreviewID = prefix + uid()
    const gridPreviewSourceID = gridPreviewID + '-source'

    if (!ezStore.get('grid-preview-layer-map')) {
        ezStore.set('grid-preview-layer-map', new window.Map())
    }

    mapManager.withMap((m) => {
        // if (m.getLayer(gridPreviewID)) {
        //     m.removeLayer(gridPreviewID)
        //     m.removeSource(gridPreviewSourceID)
        // }
        m.addSource(gridPreviewSourceID, {
            type: 'image',
            url: img,
            coordinates: coords as [
                [number, number],
                [number, number],
                [number, number],
                [number, number],
            ],
        })
        m.addLayer({
            id: gridPreviewID,
            type: 'raster',
            source: gridPreviewSourceID,
            paint: {
                'raster-opacity': 0.9,
            },
        })

        const grid_preview_layer_map = ezStore.get('grid-preview-layer-map') as Map<string, any>
        grid_preview_layer_map.set(gridPreviewID, {
            id: gridPreviewID,
            source: gridPreviewSourceID,
        })
    })
}

export function map_removeNocloudGridPreviewLayer() {
    const grid_preview_layer_map = ezStore.get('grid-preview-layer-map') as Map<string, any>
    const map = ezStore.get('map')

    if (!grid_preview_layer_map) return

    for (let key of grid_preview_layer_map.keys()) {
        if (key.indexOf('nocloud') != -1 && map.getLayer(key)) {
            map.removeLayer(grid_preview_layer_map.get(key).id)
        }
    }
}

export function map_removeGridPreviewLayer(pre: string) {
    const grid_preview_layer_map = ezStore.get('grid-preview-layer-map') as Map<string, any>
    const map = ezStore.get('map') as mapboxgl.Map

    if (!grid_preview_layer_map) return

    for (let [key, value] of grid_preview_layer_map) {
        if (key === 'all') {
            if (map.getLayer(value.id)) map.removeLayer(value.id)
            if (map.getSource(value.id)) map.removeSource(value.id)
        } else if (key.indexOf(pre) != -1) {
            if (map.getLayer(value.id)) map.removeLayer(value.id)
            if (map.getSource(value.id)) map.removeSource(value.id)
        }
    }
}

function grid_fill_click_handler(e: MapMouseEvent): void {
    const features = e.features!

    if (features.length && features[0].properties && features[0].properties.flag) {
        console.log(features[0].properties)
        const sceneGridsRes = ezStore.get('sceneGridsRes')

        const gridInfo = sceneGridsRes.find((item: any) => {
            return (
                item.rowId === features[0].properties!.rowId &&
                item.columnId === features[0].properties?.columnId
            )
        })
        bus.emit('update:gridPopupData', gridInfo)

        const popup = ezStore.get('gridPopup') as Popup
        popup.setLngLat(e.lngLat).addTo(ezStore.get('map'))

        const id = 'grid-layer'
        const highlightId = id + '-highlight'
        ezStore.get('map').setFilter(highlightId, ['in', 'id', e.features![0].properties!.id])
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
        ezStore.set('map', m)
        // Add a popup to show grid info
        if (!ezStore.get('gridPopup')) {
            const popup = new Popup({
                closeButton: false,
                closeOnMove: false,
                closeOnClick: true,
            })
            popup.on('close', () => {
                bus.emit('closeTimeline')
                const id = 'grid-layer'
                const highlightId = id + '-highlight'
                ezStore.get('map').setFilter(highlightId, ['in', 'id', ''])
            })
            const dom = createPopoverContent()
            popup.setDOMContent(dom).addTo(m)

            ezStore.set('gridPopup', popup)
        }

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
        // 这是之前的绘制方案，我先注释，确定没问题就可以删除了
        // 绘制的效果是有数据就半透明，没数据就透明
        // m.addLayer({
        //     id: fillId,
        //     type: 'fill',
        //     source: srcId,
        //     paint: {
        //         'fill-color': '#00FFFF',
        //         'fill-opacity': ['coalesce', ['to-number', ['get', 'opacity']], 0.01],
        //     },
        // })
        m.addLayer({
            id: fillId,
            type: 'fill',
            source: srcId,
            paint: {
                'fill-color': [
                    'match',
                    ['get', 'source'],
                    'demotic1m',
                    '#00FFFF',
                    'demotic2m',
                    // '#FFFF00',黄色
                    '#00FF00',
                    'international',
                    '#FFA500',
                    'radar',
                    '#FF0000',
                    /* default */ 'rgba(0,0,0,0)',
                ],
                'fill-opacity': 0.3,
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

        // // Add a click event listener to the invisible fill layer
        m.on('contextmenu', fillId, grid_fill_click_handler)
        // // Keep Watching gridStore.selectedGrids and update the highlight layer
        // const cancelWatch = watch(
        //     () => gridStore.selectedGrids,
        //     () => {
        //         const selectedGrids = Array.from(gridStore.selectedGrids) || ['']
        //         m.setFilter(highlightId, ['in', 'id', ...selectedGrids])
        //     },
        // )

        // ezStore.set('grid-layer-cancel-watch', cancelWatch)
        ezStore.set('grid-layer-fill-id', fillId)
        ezStore.set('grid-layer-line-id', lineId)
        ezStore.set('grid-layer-highlight-id', highlightId)
        ezStore.set('grid-layer-source-id', srcId)
    })
}

export function map_addGridLayer_coverOpacity(gridGeoJson: GeoJSON.FeatureCollection): void {
    const id = 'grid-layer'
    const fillId = id + '-fill'
    const lineId = id + '-line'
    const highlightId = id + '-highlight'
    const srcId = id + '-source'

    mapManager.withMap((m) => {
        ezStore.set('map', m)
        // Add a popup to show grid info
        if (!ezStore.get('gridPopup')) {
            const popup = new Popup({
                closeButton: false,
                closeOnMove: false,
                closeOnClick: true,
            })
            popup.on('close', () => {
                bus.emit('closeTimeline')
                const id = 'grid-layer'
                const highlightId = id + '-highlight'
                ezStore.get('map').setFilter(highlightId, ['in', 'id', ''])
            })
            const dom = createPopoverContent()
            popup.setDOMContent(dom).addTo(m)

            ezStore.set('gridPopup', popup)
        }

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

        // Add a click event listener to the invisible fill layer
        m.on('contextmenu', fillId, grid_fill_click_handler)

        // ezStore.set('grid-layer-cancel-watch', cancelWatch)
        ezStore.set('grid-layer-fill-id', fillId)
        ezStore.set('grid-layer-line-id', lineId)
        ezStore.set('grid-layer-highlight-id', highlightId)
        ezStore.set('grid-layer-source-id', srcId)
    })
}

export function map_addSceneBoxLayer(sceneBoxGeojson): void {
    const id = 'scene-box-layer'
    const source = id + '-source'

    const bbox = sceneBoxGeojson.bbox
    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(source) && m.removeSource(source)

        m.addSource(source, {
            type: 'geojson',
            data: sceneBoxGeojson,
        })
        m.addLayer({
            id: id,
            type: 'line',
            source: source,
            paint: {
                'line-color': '#ff6506',
                'line-width': 3,
            },
        })

        if (bbox) {
            m.fitBounds(
                [
                    [bbox[0], bbox[1]],
                    [bbox[2], bbox[3]],
                ],
                {
                    padding: 50,
                    duration: 1000,
                },
            )
        }
    })
}

export function map_destroySceneBoxLayer(): void {
    const id = 'scene-box-layer'
    const source = id + '-source'
    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(source) && m.removeSource(source)
    })
}

//////////// 无云一版图
export function map_addNoCloudLayer(url: string) {
    const id = 'no-cloud-layer'
    const source = id + '-source'

    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(source) && m.removeSource(source)

        m.addSource(source, {
            type: 'raster',
            tiles: [url],
            tileSize: 256,
            minzoom: 0,
            maxzoom: 22,
        })

        m.addLayer({
            id,
            type: 'raster',
            source: source,
            paint: {},
        })
    })
}
export function map_destroyNoCloudLayer() {
    const id = 'no-cloud-layer'
    const source = id + '-source'
    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(source) && m.removeSource(source)
    })
}
///// 爱分开加是吧
type OneNoCloudGrid = {
    bucket: string
    tifPath: string
    grid: any
}
type AllStrechParam = {
    min_r: number
    max_r: number
    min_g: number
    max_g: number
    min_b: number
    max_b: number
    nodata?: number
}
export function map_addMultiNoCloudLayer(gridsInfo: OneNoCloudGrid[], sparam: AllStrechParam) {
    if (!ezStore.get('noCloudGridsLayerIdList')) ezStore.set('noCloudGridsLayerIdList', [])
    const layerIdList = ezStore.get('noCloudGridsLayerIdList') as string[]
    mapManager.withMap((m) => {
        for (let i = 0; i < gridsInfo.length; i++) {
            const gridInfo = gridsInfo[i]
            const fullPath = gridInfo.bucket + '/' + gridInfo.tifPath
            const url = getNoCloudUrl({
                fullTifPath: gridInfo.bucket + '/' + gridInfo.tifPath,
                band1Scale: sparam.min_r + ',' + sparam.max_r,
                band2Scale: sparam.min_g + ',' + sparam.max_g,
                band3Scale: sparam.min_b + ',' + sparam.max_b,
            })

            const id = uid()
            layerIdList.push(id)
            const source = id + '-source'

            m.getLayer(id) && m.removeLayer(id)
            m.getSource(source) && m.removeSource(source)

            m.addSource(source, {
                type: 'raster',
                tiles: [url],
                tileSize: 256,
            })

            m.addLayer({
                id,
                type: 'raster',
                source: source,
                paint: {},
            })
        }
    })
}
export function map_destroyMultiNoCloudLayer() {
    const layerIdList = ezStore.get('noCloudGridsLayerIdList') as string[]
    mapManager.withMap((m) => {
        for (let i = 0; i < layerIdList.length; i++) {
            const id = layerIdList[i]
            const source = id + '-source'
            m.getLayer(id) && m.removeLayer(id)
            m.getSource(source) && m.removeSource(source)
        }
    })
}

//////////// 地形
type TerrainLayerParam = {
    fullTifPath: string
    gridsBoundary?: any
}
export function map_addTerrain(param: TerrainLayerParam): void {
    const terrainSourceUrl = getTerrainRGBUrl(param.fullTifPath)
    console.log(terrainSourceUrl)
    const onlySourceId = 'terrain-source'
    mapManager.withMap((map) => {
        map.setTerrain(null)
        map.getSource(onlySourceId) && map.removeSource(onlySourceId)

        map.addSource(onlySourceId, {
            type: 'raster-dem',
            tiles: [terrainSourceUrl],
            tileSize: 256,
            // 'maxzoom': 14
        })
        map.setTerrain({ source: onlySourceId, exaggeration: 4.0 })
    })
}
export function map_destroyTerrain() {
    mapManager.withMap(async (map) => {
        map.setTerrain(null)
        map.removeSource('terrain-rgb')
    })
}

/**
 * 遥感产品可视化
 */
export function map_addMultiTerrainTileLayer(params: TerrainLayerParam[], cb?: () => void) {
    const prefix = 'MultiTerrain'
    let layeridStore: any = null
    if (!ezStore.get('MultiTerrainLayerIds')) ezStore.set('MultiTerrainLayerIds', [])

    layeridStore = ezStore.get('MultiTerrainLayerIds')

    map_destroyMultiTerrainTileLayer()

    mapManager.withMap((m) => {
        for (let i = 0; i < params.length; i++) {
            const id = prefix + uid()
            const srcId = id + '-source'
            if (m.getLayer(id) && m.getSource(srcId)) {
                m.removeLayer(id)
                m.removeSource(srcId)
            }

            layeridStore.push(id)

            const tileUrl = getTerrainRGBUrl(params[i].fullTifPath, params[i].gridsBoundary, 0.5)

            m.setTerrain(null)
            m.addSource(srcId, {
                type: 'raster-dem',
                tiles: [tileUrl],
                tileSize: 256,
            })
            m.setTerrain({ source: srcId, exaggeration: 4.0 })
        }

        setTimeout(() => {
            cb && cb()
        }, 3000)
    })
}
export function map_destroyMultiTerrainTileLayer() {
    if (!ezStore.get('MultiTerrainLayerIds')) return

    const layeridStore = ezStore.get('MultiTerrainLayerIds')
    console.log(layeridStore)
    mapManager.withMap((m) => {
        for (let i = 0; i < layeridStore.length; i++) {
            const id = layeridStore[i]
            m.setTerrain(null)
            m.getLayer(id) && m.removeLayer(id)
            m.getSource(id + '-source') && m.removeSource(id + '-source')
        }
    })
}

export function map_addMultiOneBandColorLayer(params: OneBandColorLayerParam[], cb?: () => void) {
    const prefix = 'MultiOneBandColor'
    let layeridStore: any = null
    if (!ezStore.get('MultiOneBandColorLayerIds')) ezStore.set('MultiOneBandColorLayerIds', [])

    layeridStore = ezStore.get('MultiOneBandColorLayerIds')

    map_destroyMultiOneBandColorLayer()

    mapManager.withMap((m) => {
        for (let i = 0; i < params.length; i++) {
            const id = prefix + uid()
            const srcId = id + '-source'
            if (m.getLayer(id) && m.getSource(srcId)) {
                m.removeLayer(id)
                m.removeSource(srcId)
            }

            layeridStore.push(id)
            const nodata = params[i].nodata
            const tileUrl = getOneBandColorUrl(params[i].fullTifPath, params[i].gridsBoundary, nodata)

            m.addSource(srcId, {
                type: 'raster',
                tiles: [tileUrl],
            })

            m.addLayer({
                id: id,
                type: 'raster',
                source: srcId,
            })
        }

        setTimeout(() => {
            cb && cb()
        }, 3000)
    })
}
export function map_destroyMultiOneBandColorLayer() {
    if (!ezStore.get('MultiOneBandColorLayerIds')) return

    const layeridStore = ezStore.get('MultiOneBandColorLayerIds')
    console.log(layeridStore)
    mapManager.withMap((m) => {
        for (let i = 0; i < layeridStore.length; i++) {
            const id = layeridStore[i]
            m.getLayer(id) && m.removeLayer(id)
            m.getSource(id + '-source') && m.removeSource(id + '-source')
        }
    })
}

/**
 * 矢量图层
 */
export function map_addMVTLayer(source_layer: string, landId: string, cb?: () => void) {
    const prefix = 'MVT'
    let layeridStore: any = null
    if (!ezStore.get('MVTLayerIds')) ezStore.set('MVTLayerIds', [])

    layeridStore = ezStore.get('MVTLayerIds')

    map_destroyMVTLayer()

    mapManager.withMap((m) => {
        const id = prefix + uid()
        const srcId = id + '-source'
        if (m.getLayer(id) && m.getSource(srcId)) {
            m.removeLayer(id)
            m.removeSource(srcId)
        }

        layeridStore.push(id)
        const tileUrl = `http://${window.location.host}/api/data/vector/region/${landId}/${source_layer}/{z}/{x}/{y}`

        m.addSource(srcId, {
            type: 'vector',
            tiles: [tileUrl],
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
export function map_destroyMVTLayer() {
    if (!ezStore.get('MVTLayerIds')) return

    const layeridStore = ezStore.get('MVTLayerIds')
    console.log(layeridStore)
    mapManager.withMap((m) => {
        for (let i = 0; i < layeridStore.length; i++) {
            const id = layeridStore[i]
            m.getLayer(id) && m.removeLayer(id)
            m.getSource(id + '-source') && m.removeSource(id + '-source')
        }
    })
}

//////////// 单波段彩色产品 （形变速率）
type OneBandColorLayerParam = {
    fullTifPath: string,
    gridsBoundary?: any,
    nodata?: number
}
export function map_addOneBandColorLayer(param: OneBandColorLayerParam): void {
    const sourceUrl = getOneBandColorUrl(param.fullTifPath)
    const onlyId = 'one-band-color-layer'
    const onlySrcId = onlyId + '-source'

    mapManager.withMap((map) => {
        map.getLayer(onlyId) && map.removeLayer(onlyId)
        map.getSource(onlySrcId) && map.removeSource(onlySrcId)

        map.addSource(onlySrcId, {
            type: 'raster',
            tiles: [sourceUrl],
        })

        map.addLayer({
            id: onlyId,
            type: 'raster',
            source: onlySrcId,
        })
    })
}

export function map_destroyOneBandColorLayer() {
    const onlyId = 'one-band-color-layer'
    const onlySrcId = onlyId + '-source'
    mapManager.withMap((map) => {
        map.getLayer(onlyId) && map.removeLayer(onlyId)
        map.getSource(onlySrcId) && map.removeSource(onlySrcId)
    })
}

// export function map_addGridCoverLayer(gridGeoJson: GeoJSON.FeatureCollection){
//     const id = 'grid-layer'
//     const fillId = id + '-fill'
//     const lineId = id + '-line'
//     const highlightId = id + '-highlight'
//     const srcId = id + '-source'

//     mapManager.withMap((m) => {
//         ezStore.set('map', m)
//         // Add a popup to show grid info
//         if (!ezStore.get('gridPopup')) {
//             const popup = new Popup({
//                 closeButton: false,
//                 closeOnMove: false,
//                 closeOnClick: true,
//             })
//             popup.on('close', () => {
//                 bus.emit('closeTimeline')
//                 const id = 'grid-layer'
//                 const highlightId = id + '-highlight'
//                 ezStore.get('map').setFilter(highlightId, ['in', 'id', ''])
//             })
//             const dom = createPopoverContent()
//             popup.setDOMContent(dom).addTo(m)

//             ezStore.set('gridPopup', popup)
//         }

//         // Add a geojson source
//         m.addSource(srcId, {
//             type: 'geojson',
//             data: gridGeoJson,
//         })
//         // Add a line layer for **grid line visualization**
//         m.addLayer({
//             id: lineId,
//             type: 'line',
//             source: srcId,
//             paint: {
//                 'line-color': '#F00000',
//                 'line-width': 1,
//                 'line-opacity': 0.3,
//             },
//         })
//         // Add a invisible fill layer for **grid picking**
//         // 这是之前的绘制方案，我先注释，确定没问题就可以删除了
//         // 绘制的效果是有数据就半透明，没数据就透明
//         // m.addLayer({
//         //     id: fillId,
//         //     type: 'fill',
//         //     source: srcId,
//         //     paint: {
//         //         'fill-color': '#00FFFF',
//         //         'fill-opacity': ['coalesce', ['to-number', ['get', 'opacity']], 0.01],
//         //     },
//         // })
//         m.addLayer({
//             id: fillId,
//             type: 'fill',
//             source: srcId,
//             paint: {
//                 'fill-color': [
//                     'match',
//                     ['get', 'source'],
//                     'demotic1m',
//                     '#00FFFF',
//                     'demotic2m',
//                     // '#FFFF00',黄色
//                     '#00FF00',
//                     'international',
//                     '#FFA500',
//                     'radar',
//                     '#FF0000',
//                     /* default */ 'rgba(0,0,0,0)',
//                 ],
//                 // 'fill-opacity': 0.3,
//                 'fill-opacity': ['coalesce', ['to-number', ['get', 'opacity']], 0.01],
//             },
//         })

//         // Add a filterable fill layer for **grid highlighting**
//         // const nowSelectedGrids = Array.from(gridStore.selectedGrids) || ['']
//         m.addLayer({
//             id: highlightId,
//             type: 'fill',
//             source: srcId,
//             paint: {
//                 // 'fill-color': '#FF9900',
//                 'fill-color': '#0000FF',
//                 'fill-opacity': 0.3,
//             },
//             // filter: ['in', 'id', ...nowSelectedGrids],
//             filter: ['in', 'id', ''],
//         })

//         // // Add a click event listener to the invisible fill layer
//         m.on('contextmenu', fillId, grid_fill_click_handler)
//         // // Keep Watching gridStore.selectedGrids and update the highlight layer
//         // const cancelWatch = watch(
//         //     () => gridStore.selectedGrids,
//         //     () => {
//         //         const selectedGrids = Array.from(gridStore.selectedGrids) || ['']
//         //         m.setFilter(highlightId, ['in', 'id', ...selectedGrids])
//         //     },
//         // )

//         // ezStore.set('grid-layer-cancel-watch', cancelWatch)
//         ezStore.set('grid-layer-fill-id', fillId)
//         ezStore.set('grid-layer-line-id', lineId)
//         ezStore.set('grid-layer-highlight-id', highlightId)
//         ezStore.set('grid-layer-source-id', srcId)
//     })

// }

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
