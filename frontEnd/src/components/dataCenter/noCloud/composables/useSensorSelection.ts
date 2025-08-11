import { message } from 'ant-design-vue'
import { getCoverRegionSensorScenes, getCoverPOISensorScenes } from '@/api/http/satellite-data'
import { getRGBTileLayerParamFromSceneObject } from '@/util/visualizeHelper/index'
import * as MapOperation from '@/util/map/operation'
import { exploreData, selectnation, selectinternation, selectsar, additionalData, dataReconstruction, gridRenderingData } from './shared'
import type { platformType } from './shared'
import {useGridRendering} from './useGridRendering'

/**
 * 传感器选择相关的组合式函数
 */

export const useSensorSelection = () => {
    
    // 处理传感器影像显示
    const handleShowSensorImage = async (selectedSensor: platformType | null) => {
        console.log(selectedSensor, '选择')
        const sceneIds = selectedSensor?.sceneId || []
        console.log('选中的景ids', sceneIds)
        const sensorName = selectedSensor?.sensorName || []

        console.log('匹配的sensorName', sensorName)
        console.log('对应LandId', exploreData.regionCode)
        console.log(exploreData.searchtab)

        const stopLoading = message.loading('正在加载传感器影像...')

        let coverScenes
        if (exploreData.searchtab === 'region') {
            const params = {
                sensorName,
                sceneIds,
                regionId: exploreData.regionCode,
            }
            coverScenes = await getCoverRegionSensorScenes(params)
        } else if (exploreData.searchtab === 'poi') {
            const params = {
                sensorName,
                sceneIds,
                locationId: exploreData.regionCode,
                resolution: exploreData.gridResolution,
            }
            coverScenes = await getCoverPOISensorScenes(params)
        }
        console.log(coverScenes, '接口返回：覆盖的景们')

        const promises: Promise<any>[] = []

        for (let scene of coverScenes) {
            promises.push(getRGBTileLayerParamFromSceneObject(scene))
        }

        const rgbTileLayerParamList = await Promise.all(promises)

        console.log('可视化参数们', rgbTileLayerParamList)

        MapOperation.map_addMultiRGBImageTileLayer(rgbTileLayerParamList, stopLoading)
    }

    // 添加国产1m影像
    const add1mDemoticImage = async (reRenderAllGrids: () => void, clearGridRenderingByType: (type: string) => void) => {
        const isChecked = additionalData.value[0]

        if (isChecked) {
            // 启用国产1m影像渲染
            console.log('启用国产1m影像渲染')
            // 清除之前的渲染状态
            gridRenderingData.demotic1m.rendered = false
            
            reRenderAllGrids()
        } else {
            console.log('禁用国产1m影像渲染')
            clearGridRenderingByType('demotic1m')
        }
    }

    // 添加国产2m影像
    const add2mDemoticImages = async (renderGrids: (type: string) => void, clearGridRenderingByType: (type: string) => void) => {
        const isChecked = dataReconstruction.value[0]
        
        if (isChecked) {
            console.log('启用国产2m影像渲染')
            gridRenderingData.demotic2m.rendered = false
            renderGrids('demotic2m')
        } else {
            console.log('禁用国产2m影像渲染')
            clearGridRenderingByType('demotic2m')
        }
    }

    // 添加国外影像
    const addAbroadImages = async (renderGrids: (type: string) => void, clearGridRenderingByType: (type: string) => void) => {
        const isChecked = additionalData.value[1]
        
        if (isChecked) {
            console.log('启用国外影像渲染')
            gridRenderingData.international.rendered = false
            renderGrids('international')
        } else {
            console.log('禁用国外影像渲染')
            clearGridRenderingByType('international')
        }
    }

    // 添加雷达影像
    const addRadarImages = async (renderGrids: (type: string) => void, clearGridRenderingByType: (type: string) => void) => {
        const isChecked = additionalData.value[2]
        
        if (isChecked) {
            console.log('启用雷达影像渲染')
            gridRenderingData.radar.rendered = false
            renderGrids('radar')
        } else {
            console.log('禁用雷达影像渲染')
            clearGridRenderingByType('radar')
        }
    }

    // 处理数据重构复选框变化
    const handleDataReconstructionChange = (index: number, reRenderAllGrids: () => void, clearGridRenderingByType: (type: string) => void) => {
        const isChecked = dataReconstruction.value[index]
        console.log(`数据重构复选框${index}变化:`, isChecked)
        
        // 根据索引确定对应的数据类型
        let dataType = ''
        switch (index) {
            case 0: // 国产2m超分影像
                dataType = 'demotic2m'
                break
            case 1: // 国外影像超分数据
                dataType = 'international'
                break
            case 2: // SAR色彩转换数据
                dataType = 'radar'
                break
            default:
                console.warn(`未知的数据重构索引: ${index}`)
                return
        }
        
        if (isChecked) {
            console.log(`启用${dataType}数据重构`)
            // 确保对应的预览也启用
            if (index === 1) additionalData.value[1] = true 
            if (index === 2) additionalData.value[2] = true 
            
            // 重新渲染所有格网
            setTimeout(() => {
                reRenderAllGrids()
            }, 100)
        } else {
            console.log(`禁用${dataType}数据重构`)
            // 数据重构禁用时，清除对应的渲染
            clearGridRenderingByType(dataType)
            
            // 如果禁用后面的选项，也要禁用依赖的选项
            if (index === 0) {
                dataReconstruction.value[1] = false
                dataReconstruction.value[2] = false
                additionalData.value[1] = false
                additionalData.value[2] = false
            } else if (index === 1) {
                dataReconstruction.value[2] = false
                additionalData.value[2] = false
            }
        }
    }

    return {
        handleShowSensorImage,
        add1mDemoticImage,
        add2mDemoticImages,
        addAbroadImages,
        addRadarImages,
        handleDataReconstructionChange
    }
}