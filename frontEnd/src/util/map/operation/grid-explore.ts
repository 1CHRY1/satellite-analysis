import { mapManager } from '../mapManager'
import { ezStore } from '@/store'
import type { GridData } from '@/type/interactive-explore/grid'
import bus from '@/store/bus'
import type { Expression } from 'mapbox-gl'


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
        m.setTerrain(null)  
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
export function map_addGrid2DDEMLayer(
    gridInfo: GridData,
    url: string,
    cb?: () => void,
) {
    const prefix = gridInfo.rowId + '_' + gridInfo.columnId
    const id = prefix + uid()
    const srcId = id + '-source'

    if (!ezStore.get('grid-2dDem-layer-map')) {
        ezStore.set('grid-2dDem-layer-map', new window.Map())
    }

    mapManager.withMap((m) => {
        const gridImageLayerMap = ezStore.get('grid-2dDem-layer-map')
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
            metadata: {
                'user-label': id + '图层', 
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
export function map_destroyGrid2DDEMLayer(gridInfo: GridData) {
    const prefix = gridInfo.rowId + '_' + gridInfo.columnId
    const gridImageLayerMap = ezStore.get('grid-2dDem-layer-map')

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

export function map_addGridSceneLayer(
    gridInfo: GridData,
    url: string,
) {
    return map_addGrid3DLayer(gridInfo, url)
}

export function map_destroyGridSceneLayer(gridInfo: GridData) {
    return map_destroyGrid3DLayer(gridInfo)
}

export function map_addGrid3DLayer(
    gridInfo: GridData,
    url: string,
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
            metadata: {
                'user-label': id + '图层', 
            },
            paint: {
                'raster-opacity': (100 - (gridInfo.opacity || 0))*0.01   // 设置透明度，值范围 0-1
            }
        })

        gridImageLayerMap.set(id, {
            id: id,
            source: srcId,
        })
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
            metadata: {
                'user-label': id + '图层', 
            },
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
        // 解析 "low-up" 字符串 (如 "10-20") 并构建 case 表达式
        const expression: any[] = ['case'];

        attrList.forEach((item) => {
            const rangeStr = String(item.type);
            const parts = rangeStr.split('-');
            
            if (parts.length === 2) {
                const min = parseFloat(parts[0]);
                const max = parseFloat(parts[1]);

                if (!isNaN(min) && !isNaN(max)) {
                    // 构建判断条件: min <= value < max
                    const condition = [
                        'all',
                        ['>=', ['to-number', ['get', field]], min],
                        ['<', ['to-number', ['get', field]], max]
                    ];
                    expression.push(condition, item.color);
                }
            }
        });

        expression.push(defaultColor);
        return expression as Expression;
    } else {
        // --- 离散模式 (Discrete) ---
        return [
            'match',
            ['to-string', ['get', field]], // 强转 string 比较
            ...attrList.flatMap((tc) => [String(tc.type), tc.color]),
            defaultColor,
        ] as Expression;
    }
}

/**
 * 添加格网 MVT 图层
 */
export function map_addGridMVTLayer(
    source_layer: string,
    url: string,
    attrList: { color: string; type: number | string | any }[], // 更新类型定义
    field: string = 'type',
    cb?: () => void,
    gridInfo?: GridData,
    mode: 'discrete' | 'continuous' = 'discrete' // 新增参数
) {
    const prefix = `GridMVT`;
    
    // 使用辅助函数生成颜色表达式
    const matchColor = getPaintColorExpression(mode, field, attrList);

    let layeridStore: any = null;
    if (!ezStore.get('GridMVTLayerIds')) ezStore.set('GridMVTLayerIds', []);
    
    // 创建格网MVT图层映射表
    if (!ezStore.get('grid-mvt-layer-map')) {
        ezStore.set('grid-mvt-layer-map', new window.Map());
    }

    layeridStore = ezStore.get('GridMVTLayerIds');
    const gridMVTLayerMap = ezStore.get('grid-mvt-layer-map');

    mapManager.withMap((m) => {
        // 如果有格网信息，使用格网信息生成可识别的ID
        const gridPrefix = gridInfo ? `${gridInfo.rowId}_${gridInfo.columnId}_` : '';
        const id = prefix + gridPrefix + uid();
        const srcId = id + '-source';

        layeridStore.push(`${id}-fill`);
        layeridStore.push(`${id}-line`);
        layeridStore.push(`${id}-point`);
        
        // 如果有格网信息，将图层ID存储到映射表中
        if (gridInfo) {
            const gridKey = `${gridInfo.rowId}_${gridInfo.columnId}`;
            if (!gridMVTLayerMap.has(gridKey)) {
                gridMVTLayerMap.set(gridKey, []);
            }
            const gridLayers = gridMVTLayerMap.get(gridKey);
            gridLayers.push(`${id}-fill`, `${id}-line`, `${id}-point`);
        }

        m.addSource(srcId, {
            type: 'vector',
            tiles: [url],
        });

        // 添加面图层
        m.addLayer({
            id: `${id}-fill`,
            type: 'fill',
            source: srcId,
            metadata: {
                'user-label': id + '矢量图层', 
            },
            'source-layer': source_layer,
            filter: ['==', '$type', 'Polygon'], // 只显示面要素
            paint: {
                'fill-color': matchColor,
                'fill-outline-color': '#004499'
            }
        });
        
        // 添加线图层
        m.addLayer({
            id: `${id}-line`,
            type: 'line',
            source: srcId,
            metadata: {
                'user-label': id + '矢量图层', 
            },
            'source-layer': source_layer,
            filter: ['==', '$type', 'LineString'], // 只显示线要素
            paint: {
                'line-color': matchColor,
                'line-width': 2,
                'line-opacity': 0.8
            }
        });
        
        // 添加点图层
        m.addLayer({
            id: `${id}-point`,
            type: 'circle',
            source: srcId,
            metadata: {
                'user-label': id + '矢量图层', 
            },
            'source-layer': source_layer,
            filter: ['==', '$type', 'Point'], // 只显示点要素
            paint: {
                'circle-color': matchColor,
                'circle-radius': 6,
                'circle-opacity': 0.8,
                'circle-stroke-color': '#ffffff',
                'circle-stroke-width': 2
            }
        });

        setTimeout(() => {
            cb && cb();
        }, 3000);
    });
}

/**
 * 更新格网 MVT 图层样式
 */
export function map_updateGridMVTLayerStyle(
    attrList: { color: string; type: number | string | any }[],
    field: string = 'type',
    gridInfo?: GridData,
    mode: 'discrete' | 'continuous' = 'discrete' // 新增参数
) {
    // 1. 使用辅助函数生成新的颜色表达式
    const newMatchColor = getPaintColorExpression(mode, field, attrList);

    mapManager.withMap((m) => {
        // 获取映射表
        const gridMVTLayerMap = ezStore.get('grid-mvt-layer-map');
        if (!gridMVTLayerMap) return;

        let targetLayerIds: string[] = [];

        // 2. 确定要更新哪些图层 ID
        if (gridInfo) {
            // A. 只更新特定格网 (局部更新)
            const gridKey = `${gridInfo.rowId}_${gridInfo.columnId}`;
            if (gridMVTLayerMap.has(gridKey)) {
                targetLayerIds = gridMVTLayerMap.get(gridKey);
            }
        } else {
            // B. 更新所有格网 (全局更新 - 例如修改了图例颜色)
            gridMVTLayerMap.forEach((ids: string[]) => {
                targetLayerIds.push(...ids);
            });
        }

        // 3. 执行样式更新
        targetLayerIds.forEach((layerId) => {
            // 先检查图层是否存在，避免报错
            const layer = m.getLayer(layerId);
            if (layer) {
                try {
                    // 根据图层类型自动匹配属性
                    if (layer.type === 'fill') {
                        m.setPaintProperty(layerId, 'fill-color', newMatchColor);
                    } else if (layer.type === 'line') {
                        m.setPaintProperty(layerId, 'line-color', newMatchColor);
                    } else if (layer.type === 'circle') {
                        m.setPaintProperty(layerId, 'circle-color', newMatchColor);
                    }
                } catch (e) {
                    console.warn(`更新图层样式失败: ${layerId}`, e);
                }
            }
        });
        
        console.log(`已更新格网图层样式 (mode: ${mode})，涉及图层数量: ${targetLayerIds.length}`);
    });
}

export function map_destroyGridMVTLayer() {
    if (!ezStore.get('GridMVTLayerIds')) return

    const layeridStore = ezStore.get('GridMVTLayerIds')
    console.log(layeridStore)
    mapManager.withMap((m) => {
        const style = m.getStyle();
        if (!style) return;
        for (let i = 0; i < layeridStore.length; i++) {
            const id = layeridStore[i]
            m.getLayer(id) && m.removeLayer(id)
            console.log(`已移除图层：${id}`)
        }
        const sources = Object.keys(style.sources || {});
        sources.forEach(sourceId => {
            if (sourceId.includes('GridMVT') && sourceId.includes('-source')) {
                m.removeSource(sourceId);
                console.log(`已移除数据源: ${sourceId}`);
            }
        });
    })
    
    // 清理存储的图层 ID 列表
    ezStore.set('GridMVTLayerIds', [])
    // 清理格网MVT图层映射表
    ezStore.set('grid-mvt-layer-map', new window.Map())
}

// 删除特定格网的MVT图层
export function map_destroyGridMVTLayerByGrid(gridInfo: GridData) {
    // 确保映射表已初始化
    if (!ezStore.get('grid-mvt-layer-map')) {
        ezStore.set('grid-mvt-layer-map', new window.Map())
    }
    
    const gridMVTLayerMap = ezStore.get('grid-mvt-layer-map')
    if (!gridMVTLayerMap) return
    
    const gridKey = `${gridInfo.rowId}_${gridInfo.columnId}`
    const gridLayers = gridMVTLayerMap.get(gridKey)
    
    if (!gridLayers || gridLayers.length === 0) return
    
    mapManager.withMap((m) => {
        const style = m.getStyle();
        if (!style) return;
        
        // 移除该格网的所有图层
        gridLayers.forEach(layerId => {
            if (m.getLayer(layerId)) {
                m.removeLayer(layerId)
                console.log(`已移除格网 ${gridKey} 的图层：${layerId}`)
            }
        })
        
        // 移除对应的数据源
        const sources = Object.keys(style.sources || {});
        sources.forEach(sourceId => {
            if (sourceId.includes('GridMVT') && sourceId.includes(`${gridInfo.rowId}_${gridInfo.columnId}_`) && sourceId.includes('-source')) {
                m.removeSource(sourceId);
                console.log(`已移除格网 ${gridKey} 的数据源: ${sourceId}`);
            }
        });
    })
    
    // 从全局图层ID列表中移除该格网的图层ID
    const layeridStore = ezStore.get('GridMVTLayerIds') || []
    const updatedLayerIds = layeridStore.filter(id => !gridLayers.includes(id))
    ezStore.set('GridMVTLayerIds', updatedLayerIds)
    
    // 从映射表中移除该格网的记录
    gridMVTLayerMap.delete(gridKey)
    
    console.log(`成功删除格网 ${gridKey} 的所有MVT图层`)
}

export function map_destroySuperResolution(gridInfo: GridData) {
    const prefix = gridInfo.rowId + '_' + gridInfo.columnId
    const gridImageLayerMap = ezStore.get('grid-image-layer-map')

    mapManager.withMap((m) => {
        if (gridImageLayerMap) {
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
        }
    })
    
    // 通过总线事件重置超分增强状态，传递格网信息
    bus.emit('SuperResTimeLine', {
        data: { R: '', G: '', B: '' },
        gridInfo: {
            rowId: gridInfo.rowId,
            columnId: gridInfo.columnId,
            resolution: gridInfo.resolution
        }
    }, false)
}