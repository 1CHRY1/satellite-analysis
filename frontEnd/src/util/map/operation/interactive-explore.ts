import { mapManager, initMap, type Style } from '../mapManager'
import { ezStore, useGridStore } from '@/store'
import { Popup, GeoJSONSource, MapMouseEvent } from 'mapbox-gl'
import bus from '@/store/bus'
import { createApp, type ComponentInstance, ref, type Ref, reactive } from 'vue'
import PopContent from '@/components/feature/map/popContent/popContent.vue'
import Antd from 'ant-design-vue'
import type { Expression } from 'mapbox-gl'

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
export function map_addLargeSceneLayer(url: string) {
    console.log("影像可视化Url：", url)
    const id = 'large-scene-layer'
    const srcId = id + '-source'
    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(srcId) && m.removeSource(srcId)

        m.addSource(srcId, {
            type: 'raster',
            tiles: [url],
            tileSize: 256,
            minzoom: 1,
            maxzoom: 8, // 数据源最大 8 级，不会请求更高
        })
        
        m.addLayer({
            id,
            type: 'raster',
            source: srcId,
            paint: {},
            maxzoom: 8, // 图层最大 8 级，>8 就不渲染
        })
    })
}

export function map_destroyLargeSceneLayer() {
    const id = 'large-scene-layer'
    const srcId = id + '-source'
    mapManager.withMap((m) => {
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(srcId) && m.removeSource(srcId)
    })
}
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
            minzoom: 9,
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

export function map_fitViewToTargetZoom(zoom: number) {
    mapManager.withMap((m) => {
        m.zoomTo(zoom)
    })
}
/**
 * 4. 交互探索 - 矢量可视化
 */

/**
 * 创建矢量属性弹窗
 * @param properties 属性对象
 * @returns HTML字符串
 */
function createVectorPopupContent(properties: Record<string, any>): string {
    const hasProperties = Object.keys(properties).length > 0

    if (!hasProperties) {
        return `
            <div class="vector-popup-content">
                <div class="popup-header">
                    <h4>属性信息</h4>
                </div>
                <div class="popup-body">
                    <p class="no-data">无属性信息</p>
                </div>
            </div>
        `
    }

    const rows = Object.entries(properties)
        .map(([key, value]) => `
            <tr>
                <td class="attr-key">${key}</td>
                <td class="attr-value">${value ?? ''}</td>
            </tr>
        `)
        .join('')

    return `
        <div class="vector-popup-content">
            <div class="popup-header">
                <h4>要素属性</h4>
            </div>
            <div class="popup-body">
                <table class="attributes-table">
                    ${rows}
                </table>
            </div>
        </div>
    `
}

/**
 * 获取或创建矢量弹窗实例
 * @returns Popup实例
 */
function getOrCreateVectorPopup(): Popup {
    let popup = ezStore.get('vectorPopup') as Popup

    if (!popup) {
        popup = new Popup({
            closeButton: true,
            closeOnClick: true,
            closeOnMove: false,
            maxWidth: '320px',
            className: 'vector-popup-container'
        })

        ezStore.set('vectorPopup', popup)
    }

    return popup
}

/**
 * 添加矢量图层
 * @param source_layer 矢量图层名称
 * @param landId 行政区id
 * @param cb 回调函数
 */
export function map_addMVTLayer(source_layer: string, url: string, attrList: {color: string, type: number}[]) {
    const baseId = `${source_layer}-mvt-layer`
    const srcId = baseId + '-source'
    const matchColor: Expression = [
        'match',
        ['get', 'type'], // MVT属性字段
        ...attrList.flatMap(tc => [tc.type, tc.color]),
        'rgba(0,0,0,0)' // 默认颜色
    ]
    
    mapManager.withMap((m) => {
    //   // 移除已存在的图层和数据源
    //   const layerIds = [
    //     `${baseId}-fill`,
    //     `${baseId}-line`, 
    //     `${baseId}-point`
    //   ]
      
    //   layerIds.forEach(layerId => {
    //     if (m.getLayer(layerId)) {
    //       m.removeLayer(layerId)
    //     }
    //   })
      
    //   if (m.getSource(srcId)) {
    //     m.removeSource(srcId)
    //   }
      
      // 添加数据源
      m.addSource(srcId, {
        type: 'vector',
        tiles: [url],
      })
      
      // 添加面图层
      m.addLayer({
        id: `${baseId}-fill`,
        type: 'fill',
        source: srcId,
        'source-layer': source_layer,
        filter: ['==', '$type', 'Polygon'], // 只显示面要素
        paint: {
        //   'fill-color': '#0066cc',
          'fill-color': matchColor,
        //   'fill-opacity': 0.5,
          'fill-outline-color': '#004499'
        }
      })
      
      // 添加线图层
      m.addLayer({
        id: `${baseId}-line`,
        type: 'line',
        source: srcId,
        'source-layer': source_layer,
        filter: ['==', '$type', 'LineString'], // 只显示线要素
        paint: {
          'line-color': matchColor,
          'line-width': 2,
          'line-opacity': 0.8
        }
      })
      
      // 添加点图层
      m.addLayer({
        id: `${baseId}-point`,
        type: 'circle',
        source: srcId,
        'source-layer': source_layer,
        filter: ['==', '$type', 'Point'], // 只显示点要素
        paint: {
          'circle-color': matchColor,
          'circle-radius': 6,
          'circle-opacity': 0.8,
          'circle-stroke-color': '#ffffff',
          'circle-stroke-width': 2
        }
      })

      // 为所有图层添加点击事件处理器
      const layerIds = [`${baseId}-fill`, `${baseId}-line`, `${baseId}-point`]

      layerIds.forEach(layerId => {
        // 移除已存在的点击事件监听器（如果有）
        m.off('click', layerId)

        // 添加新的点击事件监听器
        m.on('click', layerId, (e) => {
          const features = m.queryRenderedFeatures(e.point, {
            layers: [layerId]
          })

          if (features.length > 0) {
            const feature = features[0]
            const properties = feature.properties || {}

            // 获取或创建弹窗实例
            const popup = getOrCreateVectorPopup()

            // 创建弹窗内容
            const content = createVectorPopupContent(properties)

            // 显示弹窗
            popup
              .setLngLat(e.lngLat)
              .setHTML(content)
              .addTo(m)

            // 保留控制台输出用于调试
            console.log('Clicked on layer:', layerId)
            console.log('MVT Source Layer:', feature.sourceLayer)
            console.log('Feature properties:', properties)
          }
        })

        // 鼠标悬停时显示手型光标
        m.on('mouseenter', layerId, () => {
          m.getCanvas().style.cursor = 'pointer'
        })

        m.on('mouseleave', layerId, () => {
          m.getCanvas().style.cursor = ''
        })
      })
    })
  }

/**
 * 删除矢量图层
 */
export function map_destroyMVTLayer() {
    mapManager.withMap((m) => {
        const style = m.getStyle();
        if (!style) return;

        // 1. 删除所有匹配 `mvt-layer-*-fill/line/point` 的图层
        const layers = style.layers || [];
        layers.forEach(layer => {
            if (layer.id.includes('mvt-layer-') &&
                (layer.id.endsWith('-fill') ||
                 layer.id.endsWith('-line') ||
                 layer.id.endsWith('-point'))) {
                // 移除事件监听器
                m.off('click', layer.id);
                m.off('mouseenter', layer.id);
                m.off('mouseleave', layer.id);

                // 移除图层
                m.removeLayer(layer.id);
                console.log(`已移除图层: ${layer.id}`);
            }
        });

        // 2. 删除所有匹配 `mvt-layer-source` 的数据源
        const sources = Object.keys(style.sources || {});
        sources.forEach(sourceId => {
            if (sourceId.includes('mvt-layer-source')) {
                m.removeSource(sourceId);
                console.log(`已移除数据源: ${sourceId}`);
            }
        });

        // 3. 关闭并移除矢量弹窗
        const vectorPopup = ezStore.get('vectorPopup') as Popup;
        if (vectorPopup) {
            vectorPopup.remove();
            ezStore.set('vectorPopup', null);
        }

        // 重置鼠标光标
        m.getCanvas().style.cursor = '';
    });
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