import { ref, type Ref } from 'vue'
import { gridData, superResCache } from './shared'
import { getCaseStatus, getCaseBandsResult } from '@/api/http/satellite-data'
import bus from '@/store/bus'
import { GetSuperResolution, GetSuperResolutionV2 } from '@/api/http/satellite-data/visualize.api'
import {
    visualLoad,
    selectedSensor,
    selectedRBand,
    selectedGBand,
    selectedBBand,
} from './useGridScene'
import { message } from 'ant-design-vue'
import { currentScene } from '@/components/feature/map/popContent/shared.ts'
import { ezStore } from '@/store'
import { getGridSuperResolutionUrlV2 } from '@/api/http/interactive-explore'
import * as GridExploreMapOps from '@/util/map/operation/grid-explore'

const minioIpAndPort = ezStore.get('conf')['minioIpAndPort']

export const useSuperResolution = () => {
    //超分
    type band_path = {
        R: string
        G: string
        B: string
        NIR?: string
    }
    const calTask: Ref<any> = ref({
        calState: 'start',
        taskId: '',
    })

    const isSuperRes = ref(false)

    const handleSuperResolution = async () => {
        if (!gridData.value.sceneRes.dataset) {
            message.warning('请稍后')
            return
        }
        // 缓存命中
        console.log(superResCache)
        if (superResCache.get(currentScene.value?.sceneId as string)) {
            if (!currentScene.value) {
                message.error('未找到对应的数据')
                return
            }
            console.log('缓存命中')
            const data = superResCache.get(currentScene.value?.sceneId as string)
            bus.emit(
                'SuperResTimeLine',
                {
                    data: data,
                    gridInfo: {
                        rowId: gridData.value.rowId,
                        columnId: gridData.value.columnId,
                        resolution: gridData.value.resolution,
                    },
                    sceneId: currentScene.value?.sceneId,
                    time: currentScene.value?.time,
                },
                true,
            )
            return
        }
        if (visualLoad.value) {
            isSuperRes.value = true

            if (!currentScene.value) {
                bus.emit(
                    'SuperResTimeLine',
                    {
                        data: { R: '', G: '', B: '' },
                        gridInfo: {
                            rowId: gridData.value.rowId,
                            columnId: gridData.value.columnId,
                            resolution: gridData.value.resolution,
                        },
                    },
                    false,
                )
                message.warning('SuperResTimeLine')
                return
            }

            try {
                console.log(currentScene.value)
                if (!currentScene.value) {
                    message.error('未找到对应的数据')
                    return
                }

                const bands: band_path = {
                    R: currentScene.value.redPath,
                    G: currentScene.value.greenPath,
                    B: currentScene.value.bluePath,
                }

                const result = await GetSuperResolution({
                    columnId: gridData.value.columnId,
                    rowId: gridData.value.rowId,
                    resolution: gridData.value.resolution,
                    band: bands,
                })

                calTask.value.taskId = result.data
                // 2、轮询运行状态，直到运行完成
                // ✅ 轮询函数，直到 data === 'COMPLETE'
                const pollStatus = async (taskId: string) => {
                    console.log('查询报错')
                    const interval = 500
                    return new Promise<void>((resolve, reject) => {
                        const timer = setInterval(async () => {
                            try {
                                const res = await getCaseStatus(taskId)
                                console.log('轮询结果:', res)

                                if (res?.data === 'COMPLETE') {
                                    clearInterval(timer)
                                    resolve()
                                } else if (res?.data === 'ERROR') {
                                    console.log(res, res.data, 15616)

                                    clearInterval(timer)
                                    reject(new Error('任务失败'))
                                }
                            } catch (err) {
                                clearInterval(timer)
                                reject(err)
                            }
                        }, interval)
                    })
                }

                try {
                    console.log('开始')
                    message.info('超分处理中，请等候')
                    await pollStatus(calTask.value.taskId)
                    // ✅ 成功后设置状态
                    calTask.value.calState = 'success'
                    // 添加1000毫秒延迟
                    await new Promise((resolve) => setTimeout(resolve, 500))
                    let bandres = await getCaseBandsResult(calTask.value.taskId)
                    console.log(bandres, '结果')
                    console.log('超分返回数据', bandres.data)
                    // console.log(result.value)
                    bus.emit(
                        'SuperResTimeLine',
                        {
                            data: bandres.data,
                            gridInfo: {
                                rowId: gridData.value.rowId,
                                columnId: gridData.value.columnId,
                                resolution: gridData.value.resolution,
                            },
                            sceneId: currentScene.value.sceneId,
                            time: currentScene.value.time,
                        },
                        true,
                    )
                } catch (error) {
                    calTask.value.calState = 'failed'

                    console.error('有问题')
                    console.error('问题double')
                }
                message.success('超分处理成功')
            } catch {
                message.error('超分处理失败')
            }
        } else {
            message.error('请先完成立方体可视化')
        }
    }

    const handlePreSuperResolution = async () => {
        if (!gridData.value.sceneRes.dataset) {
            message.warning('请稍后')
            return
        }
        if (visualLoad.value) {
            isSuperRes.value = true

            if (!currentScene.value) {
                bus.emit(
                    'SuperResTimeLine',
                    {
                        data: { R: '', G: '', B: '' },
                        gridInfo: {
                            rowId: gridData.value.rowId,
                            columnId: gridData.value.columnId,
                            resolution: gridData.value.resolution,
                        },
                    },
                    false,
                )
                message.warning('SuperResTimeLine')
                return
            }

            try {
                console.log(currentScene.value)
                if (!currentScene.value) {
                    message.error('未找到对应的数据')
                    message.warning('未找到对应的数据')
                    return
                }

                const bands: band_path = {
                    R: currentScene.value.redPath,
                    G: currentScene.value.greenPath,
                    B: currentScene.value.bluePath,
                }

                const result = await GetSuperResolution({
                    columnId: gridData.value.columnId,
                    rowId: gridData.value.rowId,
                    resolution: gridData.value.resolution,
                    band: bands,
                })

                calTask.value.taskId = result.data
                // 2、轮询运行状态，直到运行完成
                // ✅ 轮询函数，直到 data === 'COMPLETE'
                const pollStatus = async (taskId: string) => {
                    console.log('查询报错')
                    const interval = 500
                    return new Promise<void>((resolve, reject) => {
                        const timer = setInterval(async () => {
                            try {
                                const res = await getCaseStatus(taskId)
                                console.log('轮询结果:', res)

                                if (res?.data === 'COMPLETE') {
                                    clearInterval(timer)
                                    resolve()
                                } else if (res?.data === 'ERROR') {
                                    console.log(res, res.data, 15616)

                                    clearInterval(timer)
                                    reject(new Error('任务失败'))
                                }
                            } catch (err) {
                                clearInterval(timer)
                                reject(err)
                            }
                        }, interval)
                    })
                }

                try {
                    console.log('开始')
                    await pollStatus(calTask.value.taskId)
                    // ✅ 成功后设置状态
                    calTask.value.calState = 'success'
                    // 添加1000毫秒延迟
                    await new Promise((resolve) => setTimeout(resolve, 500))
                    let bandres = await getCaseBandsResult(calTask.value.taskId)
                    console.log(bandres, '结果')
                    console.log('超分返回数据', bandres.data)
                    // console.log(result.value)
                    // 设立缓存
                    superResCache.set(currentScene.value.sceneId, bandres.data)
                    console.log(superResCache)
                } catch (error) {
                    calTask.value.calState = 'failed'

                    console.error('有问题')
                    console.error('问题double')
                }
                // message.success('超分处理成功')
            } catch {
                // message.error('超分处理失败');
            }
        } else {
            message.error('请先完成立方体可视化')
        }
    }

    const handleSuperResolutionV2 = async () => {
        if (!gridData.value.sceneRes.dataset) {
            message.warning('请稍后')
            return
        }
        if (visualLoad.value) {
            isSuperRes.value = true

            if (!currentScene.value) {
                bus.emit(
                    'SuperResTimeLine',
                    {
                        data: { R: '', G: '', B: '' },
                        gridInfo: {
                            rowId: gridData.value.rowId,
                            columnId: gridData.value.columnId,
                            resolution: gridData.value.resolution,
                        },
                    },
                    false,
                )
                message.warning('SuperResTimeLine')
                return
            }

            try {
                console.log(currentScene.value)
                if (!currentScene.value) {
                    message.error('未找到对应的数据')
                    return
                }
                const getImagePath = (currentScene: any, bandType: string) => {
                    const img = currentScene.images?.find(i => i.band === currentScene?.bandMapper[bandType]);
                    return img ? `${minioIpAndPort}/${img.bucket}/${img.tifPath}` : '';
                };

                const bands: band_path = {
                    R: getImagePath(currentScene.value, 'Red'),
                    G: getImagePath(currentScene.value, 'Green'),
                    B: getImagePath(currentScene.value, 'Blue'),
                    NIR: getImagePath(currentScene.value, 'NIR'),
                };

                const result = await GetSuperResolutionV2({
                    columnId: gridData.value.columnId,
                    rowId: gridData.value.rowId,
                    resolution: gridData.value.resolution,
                    band: bands,
                })

                calTask.value.taskId = result.data
                // 2、轮询运行状态，直到运行完成
                // ✅ 轮询函数，直到 data === 'COMPLETE'
                const pollStatus = async (taskId: string) => {
                    console.log('查询报错')
                    const interval = 500
                    return new Promise<void>((resolve, reject) => {
                        const timer = setInterval(async () => {
                            try {
                                const res = await getCaseStatus(taskId)
                                console.log('轮询结果:', res)

                                if (res?.data === 'COMPLETE') {
                                    clearInterval(timer)
                                    resolve()
                                } else if (res?.data === 'ERROR') {
                                    console.log(res, res.data, 15616)

                                    clearInterval(timer)
                                    reject(new Error('任务失败'))
                                }
                            } catch (err) {
                                clearInterval(timer)
                                reject(err)
                            }
                        }, interval)
                    })
                }

                try {
                    console.log('开始')
                    message.info('语义分割处理中，请等候')
                    await pollStatus(calTask.value.taskId)
                    // ✅ 成功后设置状态
                    calTask.value.calState = 'success'
                    // 添加1000毫秒延迟
                    await new Promise((resolve) => setTimeout(resolve, 500))
                    let bandres = await getCaseBandsResult(calTask.value.taskId)
                    console.log(bandres, '结果')
                    console.log('超分V2返回数据', bandres.data)
                    // console.log(result.value)
                    const url = getGridSuperResolutionUrlV2(calTask.value.taskId)
                    GridExploreMapOps.map_addGridSuperResolutionLayerV2(gridData.value, url)
                    // bus.emit(
                    //     'SuperResTimeLine',
                    //     {
                    //         data: bandres.data,
                    //         gridInfo: {
                    //             rowId: gridData.value.rowId,
                    //             columnId: gridData.value.columnId,
                    //             resolution: gridData.value.resolution,
                    //         },
                    //         sceneId: currentScene.value.sceneId,
                    //         time: currentScene.value.time,
                    //     },
                    //     true,
                    // )
                } catch (error) {
                    calTask.value.calState = 'failed'

                    console.error('有问题')
                    console.error('问题double')
                }
                message.success('处理成功')
            } catch {
                message.error('处理失败')
            }
        } else {
            message.error('请先完成立方体可视化')
        }
    }

    return {
        handleSuperResolution,
        handlePreSuperResolution,
        handleSuperResolutionV2,
        isSuperRes,
    }
}
