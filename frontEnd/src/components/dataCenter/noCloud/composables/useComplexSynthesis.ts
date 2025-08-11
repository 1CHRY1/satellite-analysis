import { reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getNoCloud } from '@/api/http/satellite-data'
import { exploreData, taskStore, setCurrentPanel, additionalData, dataReconstruction, complexProgress, showComplexProgress, complexSynthesisLoading, hasComplexResult } from './shared'

/**
 * 复杂合成相关的组合式函数
 */

export const useComplexSynthesis = (allScenes: any) => {
    
    // 多源数据合成
    const multiSourceData = reactive({
        selectedBands: [],

        visualization: {
            red_band: '',    // 默认R通道显示R波段
            green_band: '',  // 默认G通道显示G波段  
            blue_band: ''    // 默认B通道显示B波段
        },

        // 可视化波段
        viz_bands: computed(() => {
            return [
                multiSourceData.visualization.red_band,
                multiSourceData.visualization.green_band,
                multiSourceData.visualization.blue_band
            ]
        }),

        // 波段类型
        sourceTypes: computed(() => {
            const types: string[] = []
            if (multiSourceData.selectedBands.includes('Red')) types.push('红波段')
            if (multiSourceData.selectedBands.includes('Blue')) types.push('蓝波段')
            if (multiSourceData.selectedBands.includes('Green')) types.push('绿波段')
            if (multiSourceData.selectedBands.includes('NIR')) types.push('近红外波段')
            if (multiSourceData.selectedBands.includes('NDVI')) types.push('归一化植被指数')
            if (multiSourceData.selectedBands.includes('EVI')) types.push('增强植被指数')
            return types.join('、') || '未选择'
        })
    })

    // 多时相数据合成
    const multiTemporalData = reactive({
        enabled: false,
        date1: null,
        date2: null,
        phases: [],
        totalBands: computed(() => multiTemporalData.phases.length * 3)
    })

    // 监听多源数据选择变化
    watch(() => multiSourceData.selectedBands, (newBands, oldBands) => {
        if (!newBands.length) {
            multiSourceData.visualization = { red_band: '', green_band: '', blue_band: '' }
            return
        }

        // 如果波段数量减少，清空所有通道（避免旧值占用）
        if (newBands.length < oldBands?.length) {
            multiSourceData.visualization = { red_band: '', green_band: '', blue_band: '' }
        }

        // 重新分配默认值（包括新增的波段）
        assignDefaultBands()
    }, { immediate: true, deep: true })

    // 分配默认波段
    const assignDefaultBands = () => {
        const { red_band, green_band, blue_band } = multiSourceData.visualization
        const channels = ['red_band', 'green_band', 'blue_band']
        const usedBands = [red_band, green_band, blue_band].filter(Boolean)
        let bandIndex = 0

        channels.forEach(channel => {
            // 如果当前通道已有值，跳过（避免覆盖用户手动选择）
            if (multiSourceData.visualization[channel]) return

            // 分配未被占用的波段（包括新增的波段）
            while (bandIndex < multiSourceData.selectedBands.length) {
                const band = multiSourceData.selectedBands[bandIndex]
                if (!usedBands.includes(band)) {
                    multiSourceData.visualization[channel] = band
                    usedBands.push(band) // 标记为已占用
                    break
                }
                bandIndex++
            }
        })
    }

    // 多源数据合成
    const handleMultiSourceData = async () => {
        // 检查是否选择了波段
        if (multiSourceData.selectedBands.length === 0) {
            ElMessage.warning('请选择至少一个波段')
            return
        }

        // 检查是否选择了可视化波段
        if (!multiSourceData.visualization.red_band || !multiSourceData.visualization.green_band || !multiSourceData.visualization.blue_band) {
            ElMessage.warning('请选择可视化波段')
            return
        }

        // 获取波段列表
        const bandList = multiSourceData.selectedBands

        taskStore.setIsInitialTaskPending(true)
        setCurrentPanel('history')

        // 根据勾选情况合并影像
        let addedImages = [...allScenes.value]
        if (dataReconstruction.value[0] === true) {
            addedImages = addedImages.concat(allScenes.value)
        }
        if (dataReconstruction.value[1] === true) {
            addedImages = addedImages.concat(allScenes.value)
        }
        if (dataReconstruction.value[2] === true) {
            addedImages = addedImages.concat(allScenes.value)
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
            bandList: bandList
        }

        // 发送请求
        console.log(getNoCloudParam, '发起请求')
        let startCalcRes = await getNoCloud(getNoCloudParam)
        if (startCalcRes.message !== 'success') {
            ElMessage.error('无云一版图计算失败')
            console.error(startCalcRes)
            return
        }
        
        // 更新任务，跳转至历史panel
        taskStore.setIsInitialTaskPending(false)
    }

    // 多时相数据合成
    const handleMultitTemporalData = async () => {
        console.log('多时相数据合成')
    }

    // 添加时相
    const addTimePhase = () => {
        if (multiTemporalData.date1 && multiTemporalData.date2) {
            multiTemporalData.phases.push({
                date1: multiTemporalData.date1,
                date2: multiTemporalData.date2,
                bands: '1-3, 4-6'
            })
            // 清空选择
            multiTemporalData.date1 = null
            multiTemporalData.date2 = null
            ElMessage.success('时相添加成功')
        } else {
            ElMessage.warning('请选择两个时相日期')
        }
    }

    // 控制复杂合成进度条
    const controlComplexProgress = (index: number) => {
        showComplexProgress.value[index] = true
        complexProgress.value[index] = 0
        
        const timer = setInterval(() => {
            if (complexProgress.value[index] < 95) {
                complexProgress.value[index] += 5
            } else {
                complexProgress.value[index] = 100
                clearInterval(timer)
                showComplexProgress.value[index] = false
            }
        }, 100)
    }

    // 监听多时相数据选择变化
    watch(() => multiTemporalData.enabled, (newVal) => {
        if (newVal) {
            controlComplexProgress(1)
        }
    })

    return {
        multiSourceData,
        multiTemporalData,
        handleMultiSourceData,
        handleMultitTemporalData,
        addTimePhase,
        controlComplexProgress
    }
}