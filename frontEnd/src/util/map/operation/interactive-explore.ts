import { mapManager, initMap, type Style } from '../mapManager'
import { ezStore, useGridStore } from '@/store'
import { Popup, GeoJSONSource, MapMouseEvent } from 'mapbox-gl'
import bus from '@/store/bus'
import { createApp, type ComponentInstance, ref, type Ref, reactive } from 'vue'
import PopContent from '@/components/feature/map/popContent/popContent.vue'
import Antd, { message } from 'ant-design-vue'
import type { Expression } from 'mapbox-gl'
// 定义回调函数的类型，方便组件传入业务逻辑
type DrawCallback = (feature: GeoJSON.Feature) => void

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
 * 开启多边形绘制模式
 */
export function draw_startPolygon(): void {
    mapManager.withDraw((d) => {
        // 1. 清除之前的绘制，避免混淆（可选，视需求而定）
        d.deleteAll()
        
        // 2. 切换到多边形绘制模式
        d.changeMode('draw_polygon')
        
        // 3. 给出提示
        message.info('请在地图上点击绘制多边形，双击结束绘制')
    })
}

/**
 * 绑定绘制完成的事件监听
 * @param onDrawCreate 当绘制完成时的回调
 */
export function bindDrawEvents(onDrawCreate: DrawCallback) {
    mapManager.withMap((map) => {
        // 移除旧的监听器以防重复绑定 (如果需要)
        // map.off('draw.create', ...) 
        
        // 监听绘制创建事件 (draw.create)
        map.on('draw.create', (e: any) => {
            const features = e.features
            if (features && features.length > 0) {
                const geometry = features[0]
                console.log('绘制的多边形数据:', geometry)
                
                // 执行回调，将数据传回组件
                onDrawCreate(geometry)
                
                // 交互优化：绘制完成后，通常切回简单选择模式
                // 也可以在这里调用 d.deleteAll() 然后用 map_addPolygonLayer 把它画成静态层
                setTimeout(() => {
                     mapManager.withDraw(d => d.changeMode('simple_select'))
                }, 0)
            }
        })

        // 监听更新事件 (如果允许用户修改绘制后的多边形)
        map.on('draw.update', (e: any) => {
             const features = e.features
             if (features && features.length > 0) {
                 onDrawCreate(features[0])
             }
        })
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
            metadata: {
                'user-label': '检索区' + '填充图层',
            },
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
            metadata: {
                'user-label': '检索区' + '线图层',
            },
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
 * 关闭网格弹窗并清除高亮
 */
function closeGridPopupAndClearHighlight(): void {
    // 关闭时间轴/年份月份选择弹窗
    bus.emit('closeTimeline')
    bus.emit('gridPopup:visible', false)
    const id = 'grid-layer'
    const highlightId = id + '-highlight'
    const map = ezStore.get('map')
    if (map && map.getLayer(highlightId)) {
        map.setFilter(highlightId, ['in', 'id', ''])
    }
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

        const id = 'grid-layer'
        const highlightId = id + '-highlight'
        ezStore.get('map').setFilter(highlightId, ['in', 'id', e.features![0].properties!.id])

        bus.emit('update:gridPopupData', gridInfo)
        bus.emit('gridPopup:visible', true)
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                bus.emit('gridPopup:reset-position')
            })
        })
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
            metadata: {
                'user-label': '格网边界图层',
            },
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
            metadata: {
                'user-label': '格网填充图层',
            },
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
            metadata: {
                'user-label': '格网高亮图层',
            },
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

        // Ensure left-click map hides popup
        m.on('click', () => {
            closeGridPopupAndClearHighlight()
        })

        // Ensure listening to "cancel button" event
        bus.on('gridPopup:closeByUser', () => {
            closeGridPopupAndClearHighlight()
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
    console.log('影像可视化Url：', url)
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
            metadata: {
                'user-label': '大范围可视化图层',
            },
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
    console.log('影像可视化Url：', url)
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
            metadata: {
                'user-label': 'Onthefly实时可视化图层',
            },
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
        .map(
            ([key, value]) => `
            <tr>
                <td class="attr-key">${key}</td>
                <td class="attr-value">${value ?? ''}</td>
            </tr>
        `,
        )
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
            className: 'vector-popup-container',
        })

        ezStore.set('vectorPopup', popup)
    }

    return popup
}
/**
 * 辅助函数：根据模式生成 Mapbox 样式表达式
 */
function getPaintColorExpression(
    mode: 'discrete' | 'continuous',
    field: string,
    attrList: { color: string; type: number | string | any }[]
): Expression {
    const defaultColor = 'rgba(0,0,0,0)'; // 默认透明

    if (!attrList || attrList.length === 0) {
        return defaultColor as any;
    }

    if (mode === 'continuous') {
        // --- 连续模式 (Continuous) ---
        // 使用 'case' 表达式处理区间判断
        // 格式要求 type 为 "min-max" (例如 "0-10")
        
        const expression: any[] = ['case'];

        attrList.forEach((item) => {
            const rangeStr = String(item.type);
            // 解析 "low-up" 字符串
            const parts = rangeStr.split('-');
            
            if (parts.length === 2) {
                const min = parseFloat(parts[0]);
                const max = parseFloat(parts[1]);

                if (!isNaN(min) && !isNaN(max)) {
                    // 构建判断条件: min <= value < max
                    // 注意：必须确保字段转为数字进行比较 ['to-number', ['get', field]]
                    const condition = [
                        'all',
                        ['>=', ['to-number', ['get', field]], min],
                        ['<', ['to-number', ['get', field]], max]
                    ];
                    
                    expression.push(condition, item.color);
                }
            }
        });

        // 添加默认颜色作为兜底
        expression.push(defaultColor);
        return expression as Expression;

    } else {
        // --- 离散模式 (Discrete) ---
        // 使用 'match' 表达式进行精确匹配
        
        return [
            'match',
            ['to-string', ['get', field]], // 强转 string 比较，兼容性更好
            ...attrList.flatMap((tc) => [String(tc.type), tc.color]),
            defaultColor,
        ] as Expression;
    }
}

/**
 * 添加 MVT 图层
 * @param mode 'discrete' (离散/分类) | 'continuous' (连续/区间)
 */
export function map_addMVTLayer(
    source_layer: string,
    url: string,
    attrList: { color: string; type: number | string | any }[],
    field: string = 'type',
    mode: 'discrete' | 'continuous' = 'discrete' // 新增参数，默认为离散
) {
    const baseId = `${source_layer}-mvt-layer`;
    const srcId = baseId + '-source';

    // 获取颜色表达式
    const matchColor = getPaintColorExpression(mode, field, attrList);

    console.log(`Layer Mode: ${mode}, Field: ${field}`);
    console.log(attrList);

    mapManager.withMap((m) => {
        // 移除旧图层逻辑(可选，视你业务逻辑是否需要保留)
        // ... 

        // 添加数据源
        if (!m.getSource(srcId)) {
             m.addSource(srcId, {
                type: 'vector',
                tiles: [url],
            });
        }

        // 添加面图层
        if (!m.getLayer(`${baseId}-fill`)) {
            m.addLayer({
                id: `${baseId}-fill`,
                type: 'fill',
                source: srcId,
                metadata: {
                    'user-label': `${source_layer}` + '矢量图层',
                },
                'source-layer': source_layer,
                filter: ['==', '$type', 'Polygon'],
                paint: {
                    'fill-color': matchColor,
                    'fill-outline-color': '#004499',
                },
            });
        }

        // 添加线图层
        if (!m.getLayer(`${baseId}-line`)) {
            m.addLayer({
                id: `${baseId}-line`,
                type: 'line',
                source: srcId,
                metadata: {
                    'user-label': `${source_layer}` + '矢量图层',
                },
                'source-layer': source_layer,
                filter: ['==', '$type', 'LineString'],
                paint: {
                    'line-color': matchColor, // 线也会应用此颜色逻辑
                    'line-width': 2,
                    'line-opacity': 0.8,
                },
            });
        }

        // 添加点图层
        if (!m.getLayer(`${baseId}-point`)) {
             m.addLayer({
                id: `${baseId}-point`,
                type: 'circle',
                source: srcId,
                metadata: {
                    'user-label': `${source_layer}` + '矢量图层',
                },
                'source-layer': source_layer,
                filter: ['==', '$type', 'Point'],
                paint: {
                    'circle-color': matchColor, // 点也会应用此颜色逻辑
                    'circle-radius': 6,
                    'circle-opacity': 0.8,
                    'circle-stroke-color': '#ffffff',
                    'circle-stroke-width': 2,
                },
            });
        }

        // --- 事件处理逻辑保持不变 ---
        const layerIds = [`${baseId}-fill`, `${baseId}-line`, `${baseId}-point`];
        
        layerIds.forEach((layerId) => {
            if (!m.getLayer(layerId)) return;

            m.off('click', layerId as any);
            m.on('click', layerId, (e) => {
                const features = m.queryRenderedFeatures(e.point, { layers: [layerId] });
                if (features.length > 0) {
                    const feature = features[0];
                    const properties = feature.properties || {};
                    const popup = getOrCreateVectorPopup();
                    const content = createVectorPopupContent(properties);
                    popup.setLngLat(e.lngLat).setHTML(content).addTo(m);
                }
            });

            m.on('mouseenter', layerId, () => { m.getCanvas().style.cursor = 'pointer'; });
            m.on('mouseleave', layerId, () => { m.getCanvas().style.cursor = ''; });
            m.on('contextmenu', layerId, (e) => {
                e.preventDefault();
                const existingPopup = ezStore.get('vectorPopup') as any;
                if (existingPopup) existingPopup.remove();
            });
        });
    });
}

/**
 * 更新矢量图层样式
 * @param mode 'discrete' (离散) | 'continuous' (连续)
 */
export function map_updateMVTLayerStyle(
    source_layer: string,
    attrList: { color: string; type: number | string | any }[],
    field: string = 'type',
    mode: 'discrete' | 'continuous' = 'discrete' // 新增参数
) {
    const baseId = `${source_layer}-mvt-layer`;
    
    // 使用统一的辅助函数生成颜色表达式
    const newPaintColor = getPaintColorExpression(mode, field, attrList);

    mapManager.withMap((m) => {
        const fillId = `${baseId}-fill`;
        const lineId = `${baseId}-line`;
        const pointId = `${baseId}-point`;

        const layers = [
            { id: fillId, prop: 'fill-color' },
            { id: lineId, prop: 'line-color' },
            { id: pointId, prop: 'circle-color' }
        ];

        layers.forEach(({ id, prop }) => {
            if (m.getLayer(id)) {
                try {
                    m.setPaintProperty(id, prop as any, newPaintColor);
                } catch (e) {
                    console.error(`更新图层样式失败 [${id}]:`, e);
                }
            }
        });
    });
}

/**
 * 删除矢量图层
 */
export function map_destroyMVTLayer() {
    mapManager.withMap((m) => {
        const style = m.getStyle()
        if (!style) return

        // 1. 删除所有匹配 `mvt-layer-*-fill/line/point` 的图层
        const layers = style.layers || []
        layers.forEach((layer) => {
            if (
                layer.id.includes('mvt-layer-') &&
                (layer.id.endsWith('-fill') ||
                    layer.id.endsWith('-line') ||
                    layer.id.endsWith('-point'))
            ) {
                // 移除事件监听器
                m.off('click', layer.id as any)
                m.off('mouseenter', layer.id as any)
                m.off('mouseleave', layer.id as any)
                m.off('contextmenu', layer.id as any)

                // 移除图层
                m.removeLayer(layer.id)
                console.log(`已移除图层: ${layer.id}`)
            }
        })

        // 2. 删除所有匹配 `mvt-layer-source` 的数据源
        const sources = Object.keys(style.sources || {})
        sources.forEach((sourceId) => {
            if (sourceId.includes('mvt-layer-source')) {
                m.removeSource(sourceId)
                console.log(`已移除数据源: ${sourceId}`)
            }
        })

        // 3. 关闭并移除矢量弹窗
        const vectorPopup = ezStore.get('vectorPopup') as Popup
        if (vectorPopup) {
            vectorPopup.remove()
            ezStore.set('vectorPopup', null)
        }

        // 重置鼠标光标
        m.getCanvas().style.cursor = ''
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
// ================= 2D DEM =================
export function map_add2DDEMLayer(url: string | string[]) {
    const baseId = '2d-dem-layer'
    const list = Array.isArray(url) ? url : [url]
    mapManager.withMap((m) => {
        // Fix: 加上 ?.layers
        m.getStyle()?.layers?.filter(l => l.id.startsWith(baseId)).forEach(l => {
            m.removeLayer(l.id); m.getSource(l.id) && m.removeSource(l.id)
        })
        list.forEach((u, i) => {
            const id = `${baseId}-${i}`
            m.addSource(id, { type: 'raster', tiles: [u], tileSize: 256 })
            m.addLayer({ id, type: 'raster', source: id, metadata: { 'user-label': 'DEM图层' }, paint: {} })
        })
    })
}

export function map_destroy2DDEMLayer() {
    const baseId = '2d-dem-layer'
    mapManager.withMap((m) => {
        // Fix: 加上 ?.layers
        m.getStyle()?.layers?.filter(l => l.id.startsWith(baseId)).forEach(l => {
            m.removeLayer(l.id); m.getSource(l.id) && m.removeSource(l.id)
        })
    })
}

// ================= NDVI / SVR =================
export function map_addNDVIOrSVRLayer(url: string | string[]) {
    const baseId = 'ndvi-layer'
    const list = Array.isArray(url) ? url : [url]
    mapManager.withMap((m) => {
        m.getStyle()?.layers?.filter(l => l.id.startsWith(baseId)).forEach(l => {
            m.removeLayer(l.id); m.getSource(l.id) && m.removeSource(l.id)
        })
        list.forEach((u, i) => {
            const id = `${baseId}-${i}`
            m.addSource(id, { type: 'raster', tiles: [u], tileSize: 256 })
            m.addLayer({ id, type: 'raster', source: id, metadata: { 'user-label': '图层' }, paint: {} })
        })
    })
}

export function map_destroyNDVIOrSVRLayer() {
    const baseId = 'ndvi-layer'
    mapManager.withMap((m) => {
        m.getStyle()?.layers?.filter(l => l.id.startsWith(baseId)).forEach(l => {
            m.removeLayer(l.id); m.getSource(l.id) && m.removeSource(l.id)
        })
    })
}

// ================= One Band =================
export function map_addOneBandLayer(url: string | string[]) {
    const baseId = 'oneband-layer'
    const list = Array.isArray(url) ? url : [url]
    mapManager.withMap((m) => {
        m.getStyle()?.layers?.filter(l => l.id.startsWith(baseId)).forEach(l => {
            m.removeLayer(l.id); m.getSource(l.id) && m.removeSource(l.id)
        })
        list.forEach((u, i) => {
            const id = `${baseId}-${i}`
            m.addSource(id, { type: 'raster', tiles: [u], tileSize: 256 })
            m.addLayer({ id, type: 'raster', source: id, metadata: { 'user-label': '指标数据图层' }, paint: {} })
        })
    })
}

export function map_destroyOneBandLayer() {
    const baseId = 'oneband-layer'
    mapManager.withMap((m) => {
        m.getStyle()?.layers?.filter(l => l.id.startsWith(baseId)).forEach(l => {
            m.removeLayer(l.id); m.getSource(l.id) && m.removeSource(l.id)
        })
    })
}

// ================= 3D Layer =================
export function map_add3DLayer(url: string | string[]) {
    const baseId = '3d-layer'
    const list = Array.isArray(url) ? url : [url]
    mapManager.withMap((m) => {
        m.getStyle()?.layers?.filter(l => l.id.startsWith(baseId)).forEach(l => {
            m.removeLayer(l.id); m.getSource(l.id) && m.removeSource(l.id)
        })
        list.forEach((u, i) => {
            const id = `${baseId}-${i}`
            m.addSource(id, { type: 'raster', tiles: [u], tileSize: 256 })
            m.addLayer({ id, type: 'raster', source: id, metadata: { 'user-label': baseId + '图层' }, paint: {} })
        })
    })
}

export function map_destroy3DLayer() {
    const baseId = '3d-layer'
    mapManager.withMap((m) => {
        m.getStyle()?.layers?.filter(l => l.id.startsWith(baseId)).forEach(l => {
            m.removeLayer(l.id); m.getSource(l.id) && m.removeSource(l.id)
        })
    })
}
