import { mapManager, initMap, type Style } from '../mapManager'
import { ezStore, useGridStore } from '@/store'
import { Popup, GeoJSONSource, MapMouseEvent } from 'mapbox-gl'
import bus from '@/store/bus'
import { createApp, type ComponentInstance, ref, type Ref, reactive } from 'vue'
import PopoverContent, { type GridData } from '@/components/feature/map/popoverContent.vue'
import PopContent from '@/components/feature/map/popContent.vue'
import Antd from 'ant-design-vue'


/**
 * 0. 公用函数/初始化等
 */
const gridStore = useGridStore()
function uid() {
    return Math.random().toString(36).substring(2, 15)
}
/**
 * 删除所有绘制
 */
export function draw_deleteAll(): void {
    mapManager.withDraw((d) => {
        d.deleteAll()
        d.changeMode('simple_select')
    })
}

/**
 * 1. 数据检索 - 获取格网
 */
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

export function map_destroyUniqueLayer() {
    const id = 'UniqueLayer'
    const fillId = `${id}-fill`
    const lineId = `${id}-line`
    const srcId = `${id}-source`
    mapManager.withMap((m) => {
        if (m.getLayer(lineId)) m.removeLayer(lineId)
        if (m.getLayer(fillId)) m.removeLayer(fillId)
        if (m.getSource(srcId)) m.removeSource(srcId)
    })
}

/**
 * 2. 数据检索 - 检索后
 */

/**
 * 创建网格信息弹窗
 * @returns 网格信息弹窗DOM
 */
function createPopoverContent() {
    const div = document.createElement('div')
    div.id = 'popover-content'
    document.body.appendChild(div)

    const app = createApp(PopContent, {
        // gridData: gridDataRef,
    }).use(Antd)
    app.mount('#popover-content') as ComponentInstance<typeof PopContent>
    return div
}

/**
 * 网格点击事件
 * @param e 地图点击事件
 */
function grid_fill_click_handler(e: MapMouseEvent): void {
    const features = e.features!

    if (features.length && features[0].properties && features[0].properties.flag) {
        console.log(features[0].properties)
        const gridInfo = {
            rowId: features[0].properties!.rowId,
            columnId: features[0].properties!.columnId,
            resolution: features[0].properties!.resolution,
        }
        bus.emit('update:gridPopupData', gridInfo)

        const popup = ezStore.get('gridPopup') as Popup
        popup.setLngLat(e.lngLat).addTo(ezStore.get('map'))

        const id = 'grid-layer'
        const highlightId = id + '-highlight'
        ezStore.get('map').setFilter(highlightId, ['in', 'id', e.features![0].properties!.id])
    }
}

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
                // 取消高亮个别网格
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

/**
 * 3. 交互探索 - 遥感影像可视化
 */
export function map_addSceneLayer(url: string) {
    console.log("影像可视化Url：", url)
    const id = 'scene-layer'
    const srcId = id + '-source'
    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(srcId) && m.removeSource(srcId)

        m.addSource(srcId, {
            type: 'raster',
            tiles: [url],
            tileSize: 256,
            minzoom: 6,
            maxzoom: 22,
        })

        m.addLayer({
            id,
            type: 'raster',
            source: srcId,
            paint: {},
        })
    })
}

export function map_destroySceneLayer() {
    const id = 'scene-layer'
    const srcId = id + '-source'
    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(srcId) && m.removeSource(srcId)
    })
}

/**
 * 4. 交互探索 - 矢量可视化
 */

/**
 * 添加矢量图层
 * @param source_layer 矢量图层名称
 * @param landId 行政区id
 * @param cb 回调函数
 */
export function map_addMVTLayer(source_layer: string, url: string) {
    const id = 'mvt-layer'
    const srcId = id + '-source'
    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(srcId) && m.removeSource(srcId)


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
    })
}

/**
 * 删除矢量图层
 */
export function map_destroyMVTLayer() {
    const id = 'mvt-layer'
    const srcId = id + '-source'
    
    mapManager.withMap((m) => {
        console.log(m.getLayer(id))
        console.log(m.getSource(srcId))
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(srcId) && m.removeSource(srcId)
    })
}

/**
 * 5. 交互探索 - 栅格专题产品可视化
 */
export function map_addDEMLayer(url: string) {
    const id = 'dem-layer'
    const srcId = id + '-source'
    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(srcId) && m.removeSource(srcId)

        m.setTerrain(null)

        m.addSource(srcId, {
            type: 'raster-dem',
            tiles: [url],
            tileSize: 256,
        })

        m.setTerrain({ source: srcId, exaggeration: 4.0 })
    })
}
export function map_destroyDEMLayer() {
    const id = 'dem-layer'
    const srcId = id + '-source'
    mapManager.withMap((m) => {
        m.setTerrain(null)
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(srcId) && m.removeSource(srcId)
    })
}
export function map_addNDVIOrSVRLayer(url: string) {
    const id = 'ndvi-layer'
    const srcId = id + '-source'
    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(srcId) && m.removeSource(srcId)

        m.addSource(srcId, {
            type: 'raster',
            tiles: [url],
            tileSize: 256,
        })

        m.addLayer({
            id,
            type: 'raster',
            source: srcId,
            paint: {},
        })
    })
}
export function map_destroyNDVIOrSVRLayer() {
    const id = 'ndvi-layer'
    const srcId = id + '-source'
    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(srcId) && m.removeSource(srcId)
    })
}
export function map_add3DLayer(url: string) {
    const id = '3d-layer'
    const srcId = id + '-source'
    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(srcId) && m.removeSource(srcId)

        m.addSource(srcId, {
            type: 'raster',
            tiles: [url],
            tileSize: 256,
        })

        m.addLayer({
            id,
            type: 'raster',
            source: srcId,
            paint: {},
        })
    })
}
export function map_destroy3DLayer() {
    const id = '3d-layer'
    const srcId = id + '-source'
    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(srcId) && m.removeSource(srcId)
    })
}