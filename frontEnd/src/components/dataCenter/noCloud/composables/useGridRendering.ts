import { ref, watch } from 'vue'
import * as MapOperation from '@/util/map/operation'
import { mapManager } from '@/util/map/mapManager'
import { exploreData, additionalData, dataReconstruction, gridRenderingData, renderedGrids, gridRenderingStatus } from './shared'
import type { FeatureCollection } from 'geojson'

/**
 * 格网渲染相关的组合式函数
 */

export const useGridRendering = (getGridStatsByType: (dataType: string) => any[]) => {
    
    // 生成格网唯一ID
    const generateGridId = (rowId: number, columnId: number, resolution: number): string => {
        return `${rowId}_${columnId}_${resolution}`
    }

    // 判断格网是否需要渲染
    const shouldRenderGrid = (gridId: string, gridData: any, dataType: string): boolean => {
        // 如果已经渲染过，不再渲染
        if (renderedGrids.value.has(gridId)) {
            return false
        }
        
        // 检查是否有数据覆盖
        return gridData && gridData.isOverlapped === true
    }

    // 创建格网FeatureCollection
    const createGridFeatureCollection = (grids: any[], dataType: string): FeatureCollection => {
        const features = grids.map((grid: any) => {
            const gridId = generateGridId(grid.rowId, grid.columnId, grid.resolution)
            
            return {
                type: 'Feature' as const,
                geometry: grid.boundary?.geometry || null,
                properties: {
                    id: gridId,
                    rowId: grid.rowId,
                    columnId: grid.columnId,
                    resolution: grid.resolution,
                    source: dataType,
                    opacity: 0.3,
                    color: gridRenderingData[dataType].color
                }
            }
        }).filter(feature => feature.geometry !== null)

        return {
            type: 'FeatureCollection',
            features
        }
    }

    // 检查数据类型是否启用
    const checkDataTypeEnabled = (dataType: string): boolean => {
        const dataTypeMap: Record<string, boolean> = {
            'demotic1m': additionalData.value[0] ?? false,
            'demotic2m': dataReconstruction.value[0] ?? false,
            'international': (additionalData.value[1] ?? false) || (dataReconstruction.value[1] ?? false),
            'radar': (additionalData.value[2] ?? false) || (dataReconstruction.value[2] ?? false),
        }

        return dataTypeMap[dataType] ?? false
    }

    // 清除特定类型的格网渲染
    const clearGridRenderingByType = (dataType: string) => {
        console.log(`开始清除${dataType}类型的格网渲染`)
        
        MapOperation.map_destroyGridLayerByType(dataType)
        
        // 从渲染状态中移除该类型的格网
        const gridsToRemove = Array.from(gridRenderingStatus.value.entries())
            .filter(([gridId, type]) => type === dataType)
            .map(([gridId]) => gridId)
        
        console.log(`找到 ${gridsToRemove.length} 个${dataType}类型的格网需要清除`)
        
        gridsToRemove.forEach(gridId => {
            renderedGrids.value.delete(gridId)
            gridRenderingStatus.value.delete(gridId)
        })
        
        // 重置该类型的渲染状态
        gridRenderingData[dataType].rendered = false
        gridRenderingData[dataType].grids = []
        
        console.log(`成功清除${dataType}类型的格网渲染，移除了${gridsToRemove.length}个格网`)
        console.log(`当前已渲染格网总数: ${renderedGrids.value.size}`)
    }

    // 清除所有格网渲染
    const clearAllGridRendering = () => {
        MapOperation.map_destroyAllGridLayers()
        renderedGrids.value.clear()
        gridRenderingStatus.value.clear()
        
        // 重置渲染状态
        Object.keys(gridRenderingData).forEach(key => {
            gridRenderingData[key].rendered = false
            gridRenderingData[key].grids = []
        })
    }

    // 渲染格网
    const renderGrids = (dataType: string) => {
        console.log(`开始渲染${dataType}类型格网`)
        
        // 检查对应的复选框是否启用
        const isEnabled = checkDataTypeEnabled(dataType)
        console.log(`${dataType}类型启用状态:`, isEnabled)
        if (!isEnabled) {
            console.log(`数据类型 ${dataType} 未启用，跳过渲染`)
            return
        }

        if (gridRenderingData[dataType].rendered) {
            console.log(`数据类型 ${dataType} 已经渲染过，跳过`)
            return
        }

        const grids = exploreData.grids || []
        if (grids.length === 0) {
            console.log('没有可用的格网数据')
            return
        }

        const gridStats = getGridStatsByType(dataType)
        console.log(`${dataType}类型的格网统计数据:`, gridStats)
        
        if (!gridStats || gridStats.length === 0) {
            console.log(`没有找到 ${dataType} 类型的格网统计数据`)
            return
        }

        console.log(`处理 ${grids.length} 个格网，类型: ${dataType}`)
        console.log(`可用的格网统计数据: ${gridStats.length} 个`)

        const gridsToRender = grids.filter((grid: any) => {
            const gridId = generateGridId(grid.rowId, grid.columnId, grid.resolution)
            const gridStat = gridStats.find((stat: any) => 
                stat.rowId === grid.rowId && 
                stat.columnId === grid.columnId && 
                stat.resolution === grid.resolution
            )
            
            const shouldRender = shouldRenderGrid(gridId, gridStat, dataType)
            if (shouldRender) {
                //格网监测
                // console.log(`格网 ${gridId} 将被渲染为 ${dataType} 类型`)
            }
            
            return shouldRender
        })

        console.log(`最终需要渲染的格网数量: ${gridsToRender.length}`)
        console.log(`要渲染的格网:`, gridsToRender)

        if (gridsToRender.length === 0) {
            console.log(`没有格网需要渲染，类型: ${dataType}`)
            return
        }

        console.log(`渲染 ${gridsToRender.length} 个格网，类型: ${dataType}`)

        // 创建FeatureCollection
        const gridFeature = createGridFeatureCollection(gridsToRender, dataType)
        console.log(`创建的FeatureCollection:`, gridFeature)
        
        // 添加到地图
        MapOperation.map_addGridLayer(gridFeature, dataType)
        
        // 更新渲染状态
        gridsToRender.forEach((grid: any) => {
            const gridId = generateGridId(grid.rowId, grid.columnId, grid.resolution)
            renderedGrids.value.add(gridId)
            gridRenderingStatus.value.set(gridId, dataType)
        })
        
        gridRenderingData[dataType].rendered = true
        gridRenderingData[dataType].grids = gridsToRender
        
        console.log(`成功渲染 ${gridsToRender.length} 个格网，类型: ${dataType}`)
        console.log(`当前已渲染格网总数: ${renderedGrids.value.size}`)
    }

    // 重新渲染所有格网
    const reRenderAllGrids = () => {
        clearAllGridRendering()
        
        // 按优先级渲染：国产1m -> 国产2m -> 国外 -> 雷达
        const renderOrder = ['demotic1m', 'demotic2m', 'international', 'radar']
        
        renderOrder.forEach(dataType => {
            if (checkDataTypeEnabled(dataType)) {
                renderGrids(dataType)
            } else {
                gridRenderingData[dataType].rendered = false
                gridRenderingData[dataType].grids = []
            }
        })
    }

    // 初始化格网渲染
    const initGridRendering = async () => {
        await mapManager.waitForInit()
        
        // 清除旧的格网图层
        MapOperation.map_destroyAllGridLayers()
        
        // 渲染所有格网
        reRenderAllGrids()
    }

    // 监听数据重构选择变化，触发格网重新渲染
    watch(() => dataReconstruction.value, (newVal) => {
        console.log('数据重构选择变化:', newVal)
        // 延迟执行，确保数据已更新
        setTimeout(() => {
            reRenderAllGrids()
        }, 100)
    }, { deep: true })

    // 监听额外数据选择变化，触发格网重新渲染
    watch(() => additionalData.value, (newVal) => {
        console.log('额外数据选择变化:', newVal)
        // 延迟执行，确保数据已更新
        setTimeout(() => {
            reRenderAllGrids()
        }, 100)
    }, { deep: true })

    return {
        generateGridId,
        shouldRenderGrid,
        createGridFeatureCollection,
        checkDataTypeEnabled,
        clearGridRenderingByType,
        clearAllGridRendering,
        renderGrids,
        reRenderAllGrids,
        initGridRendering
    }
}