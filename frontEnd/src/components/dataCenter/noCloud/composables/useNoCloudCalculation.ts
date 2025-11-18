import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { getNoCloud } from '@/api/http/satellite-data'
import { getNoCloudUrl4MosaicJson } from '@/api/http/satellite-data/visualize.api'
import * as MapOperation from '@/util/map/operation'
import { exploreData, taskStore, setCurrentPanel, noCloudLoading, calTask, additionalData, dataReconstruction, progress, showProgress } from './shared'
import {demotic1mImages,demotic2mImages,internationalImages,radarImages} from './useDataPreparation'
import { bandList } from './useComplexSynthesis'

/**
 * 无云一版图计算相关的组合式函数
 */

export const useNoCloudCalculation = (allScenes: any) => {
    
    // 计算图像结果
    const calImage = ref<any[]>([])
    
    // 进度条定时器
    let progressTimer: ReturnType<typeof setInterval> | null = null

    // 图像拉伸参数
    const showingImageStrech = reactive({
        r_min: 0,
        r_max: 5000,
        g_min: 0,
        g_max: 5000,
        b_min: 0,
        b_max: 5000,
    })

    // 控制进度条
    const progressControl = (index: number) => {
        if (calTask.value.calState === 'pending') return
        progress.value[index] = 0
        calTask.value.calState = 'pending'
        progressTimer = setInterval(() => {
            if (calTask.value.calState === 'success' || calTask.value.calState === 'failed') {
                progress.value[index] = 100
                clearInterval(progressTimer!)
                progressTimer = null
            } else if (progress.value[index] < 95) {
                progress.value[index] += 1
            } else {
                progress.value[index] = 95
            }
        }, 100)
    }

    // 操控进度条
    const controlProgress = (index: number) => {
        // 取消勾选要把后面的选项全部取消勾选。2、取消勾选隐藏进度条
        let overTask = cancelCheckbox('dataReconstruction', index)
        if (overTask) return

        // 只显示当前进度条
        showProgress.value = showProgress.value.map((_progress, i: number) => {
            return index === i ? true : false
        })

        progressControl(index)

        // 轮询运行状态，直到运行完成
        setTimeout(() => {
            calTask.value.calState = 'success'
        }, 500) // 假操作进度条统一时间
    }

    // 假操作进度条统一时间
    const mockProgressTime = 500

    // 现在的问题是，国外和SAR的勾选框有两个，取消一个都要取消后面的勾选框，所以作为一个单独的方法
    const cancelCheckbox = (type: string, index: number) => {
        // 第一种情况，取消勾选格网填补
        if (type === 'grid' && additionalData.value[index] === true) {
            while (index < additionalData.value.length - 1) {
                additionalData.value[index + 1] = false
                dataReconstruction.value[index + 1] = false
                showProgress.value[index + 1] = false
                index++
            }
            return true
        }
        // 第二种情况，取消勾选数据重构
        if (type === 'dataReconstruction' && dataReconstruction.value[index] === true) {
            showProgress.value[index] = false
            while (index < dataReconstruction.value.length - 1) {
                additionalData.value[index + 1] = false
                dataReconstruction.value[index + 1] = false
                showProgress.value[index + 1] = false
                index++
            }
            return true
        }
    }

    // 开始计算
    const calNoClouds = async () => {
        // 因为从后端拿到taskId需要一定时间，所以先向任务store推送一个初始化任务状态
        taskStore.setIsInitialTaskPending(true)
        setCurrentPanel('history')

        // 根据勾选情况合并影像
        let addedImages = [...demotic1mImages.value]
        if (dataReconstruction.value[0] === true) {
            addedImages = addedImages.concat(demotic2mImages.value)
        }
        if (dataReconstruction.value[1] === true) {
            addedImages = addedImages.concat(internationalImages.value)
        }
        if (dataReconstruction.value[2] === true) {
            addedImages = addedImages.concat(radarImages.value)
        }
        
        let dataSet = [
            '国产亚米影像',
            dataReconstruction.value[0] ? '国产2m超分影像' : null,
            dataReconstruction.value[1] ? '国外影像超分数据' : null,
            dataReconstruction.value[2] ? 'SAR色彩转换数据' : null,
        ].filter(Boolean).join('、')

        let getNoCloudParam = {
            regionId: exploreData.regionCode,
            cloud: exploreData.cloud,
            resolution: exploreData.gridResolution,
            sceneIds: addedImages.map((image) => image.sceneId),
            dataSet: dataSet,
            bandList: bandList.value
        }

        // 发送请求
        console.log(getNoCloudParam, '发起请求')
        let startCalcRes = await getNoCloud(getNoCloudParam)
        if (startCalcRes.message !== 'success') {
            message.error('无云一版图计算失败')
            console.error(startCalcRes)
            return
        }
        
        // 更新任务，跳转至历史panel
        calTask.value.taskId = startCalcRes.data
        taskStore.setTaskStatus(calTask.value.taskId, 'PENDING')
        taskStore.setIsInitialTaskPending(false)
    }

    // 预览无云一版图
    const previewNoCloud = async (data: any) => {
        const stopLoading = message.loading('正在加载无云一版图...', 0)
        
        // 清除旧图层
        MapOperation.map_removeNocloudGridPreviewLayer()
        MapOperation.map_destroyNoCloudLayer()

        // 新版无云一版图（MosaicJson）展示逻辑
        const mosaicJsonPath = data.bucket + '/' + data.object_path
        const url4MosaicJson = getNoCloudUrl4MosaicJson({
            mosaicJsonPath: mosaicJsonPath
        })
        MapOperation.map_addNoCloudLayer(url4MosaicJson)

        setTimeout(() => {
            stopLoading()
        }, 5000)
    }

    // 创建无云一版图瓦片
    const handleCreateNoCloudTiles = async () => {
        try {
            // 1. 准备参数
            const sceneIds = allScenes.value.map((item: any) => item.sceneId)
            const param = {
                sceneIds: sceneIds,
            }

            console.log('创建无云一版图配置参数:', param)

            // 2. 创建配置
            const response = await fetch('/api/modeling/example/noCloud/createNoCloudConfig', {
                method: 'POST',
                body: JSON.stringify(param),
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + localStorage.getItem('token'),
                },
            })
            const result = await response.json()
            const jsonUrl = result.data  // 从CommonResultVO中获取data字段
            
            console.log('获取到的jsonUrl:', jsonUrl)
            
            // 3. 添加瓦片图层
            const tileUrl = `http://localhost:8000/no_cloud/{z}/{x}/{y}?jsonUrl=${encodeURIComponent(jsonUrl)}`
            
            console.log('瓦片URL模板:', tileUrl)
            
            // 清除旧的无云图层
            MapOperation.map_destroyNoCloudLayer()
            
            // 添加新的瓦片图层
            MapOperation.map_addNoCloudLayer(tileUrl)
            
            console.log('无云一版图瓦片图层已添加到地图')
            
        } catch (error) {
            console.error('创建无云一版图瓦片失败:', error)
        }
    }

    return {
        calImage,
        showingImageStrech,
        progressControl,
        controlProgress,
        cancelCheckbox,
        calNoClouds,
        previewNoCloud,
        handleCreateNoCloudTiles
    }
}