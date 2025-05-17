<template>
    <div class="custom-panel px-2">
        <dv-border-box12 class="!h-[calc(100vh-56px-48px-32px-8px)]">
            <div class="main-container">
                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <CloudIcon :size="18" />
                        </div>
                        <h2 class="section-title">无云一版图重构</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">

                            <div class="config-item">
                                <div class="config-label relative">
                                    <BoltIcon :size="16" class="config-icon" />
                                    <span>国产光学影像</span>
                                </div>
                                <div class="config-control flex-col !items-start">
                                    <div class="flex flex-col gap-2 w-full">
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="additionalData[0]" disabled
                                                class="w-4 h-4 rounded">
                                            国产亚米级影像
                                        </label>
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="dataReconstruction[0]"
                                                @click="add2mDemoticImages" class="w-4 h-4 rounded">
                                            使用国产2m级影像超分重建亚米级数据
                                        </label>
                                        <div v-if="showProgress[0]"
                                            class="w-full  bg-[#1e293b] rounded-lg overflow-hidden border border-[#2c3e50]">
                                            <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                                :style="{ width: `${progress[0]}%` }"></div>
                                        </div>
                                    </div>
                                    <div class="result-info-container">
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <MapIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">行政区划编码</div>
                                                <div class="result-info-value">{{ props.regionConfig.regionCode }}
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <ImageIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">格网分辨率</div>
                                                <div class="result-info-value">{{ props.regionConfig.space }}km </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CalendarIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">涵盖时间范围</div>
                                                <div class="result-info-value date-range">
                                                    <div class="date-item">{{
                                                        formatTime(props.regionConfig.dataRange[0], 'day')
                                                    }}~
                                                        {{ formatTime(props.regionConfig.dataRange[1], 'day')
                                                        }}</div>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">云量范围</div>
                                                <div class="result-info-value">0 ~ {{ props.regionConfig.cloud }}%</div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">研究区亚米级国产影像</div>
                                                <div class="result-info-value">{{ demotic }}景影像</div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">亚米级国产影像覆盖率</div>
                                                <div class="result-info-value">{{ coverageRate.demotic1m != 'NaN%'
                                                    ? coverageRate.demotic1m : '待计算' }}</div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">研究区国产影像（2m超分后）</div>
                                                <div class="result-info-value">{{ demotic2mImages.length }}景影像</div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">影像覆盖率（2m超分后）</div>
                                                <div class="result-info-value">{{ coverageRate.demotic2m != 'NaN%'
                                                    ? coverageRate.demotic2m : '待计算' }}</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                            </div>
                            <div class="config-item">
                                <div class="config-label relative">
                                    <CalendarIcon :size="16" class="config-icon" />
                                    <span>融合国外光学影像</span>
                                    <el-tooltip content="对于缺失数据的格网，采用国外光学影像进行填补，填补过程中基于AI算法进行超分辨率重建" placement="top"
                                        effect="dark">
                                        <CircleHelp :size="14" />
                                    </el-tooltip>
                                </div>
                                <div class="config-control flex-col !items-start">
                                    <div class="flex flex-col gap-2">
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="additionalData[1]" @click="addAbroadImages"
                                                :disabled="!dataReconstruction[0]" class="w-4 h-4 rounded">
                                            国外影像填补缺失格网
                                        </label>
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="dataReconstruction[1]"
                                                @click="controlProgress(1)" :disabled="!dataReconstruction[0]"
                                                class="w-4 h-4 rounded">
                                            使用国外影像超分重建数据
                                        </label>
                                        <div v-if="showProgress[1]"
                                            class="w-full  bg-[#1e293b] rounded-lg overflow-hidden border border-[#2c3e50]">
                                            <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                                :style="{ width: `${progress[1]}%` }"></div>
                                        </div>
                                    </div>
                                    <div class="result-info-container w-full">
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">研究区国外影像</div>
                                                <div class="result-info-value">{{ internationalImages.length }}景影像</div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">累积影像覆盖率</div>
                                                <div class="result-info-value">{{ coverageRate.international != 'NaN%' ?
                                                    coverageRate.international : '待计算' }}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="config-item">
                                <div class="config-label relative">
                                    <CalendarIcon :size="16" class="config-icon" />
                                    <span>融合SAR影像</span>
                                    <el-tooltip content="勾选将使用雷达数据进行色彩变换，与光学数据配准，并补充重构。" placement="top" effect="dark">
                                        <CircleHelp :size="14" />
                                    </el-tooltip>
                                </div>
                                <div class="config-control flex-col !items-start">
                                    <div class="flex flex-col gap-2">
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="additionalData[2]" @click="addRadarImages"
                                                :disabled="!additionalData[1] || !dataReconstruction[1]"
                                                class="w-4 h-4 rounded">
                                            SAR影像填补缺失格网
                                        </label>
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="dataReconstruction[2]"
                                                @click="controlProgress(2)"
                                                :disabled="!additionalData[1] || !dataReconstruction[1]"
                                                class="w-4 h-4 rounded">
                                            使用SAR影像色彩变换重建数据
                                        </label>
                                        <div v-if="showProgress[2]"
                                            class="w-full  bg-[#1e293b] rounded-lg overflow-hidden border border-[#2c3e50]">
                                            <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                                :style="{ width: `${progress[2]}%` }"></div>
                                        </div>
                                    </div>
                                    <div class="result-info-container w-full">
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">研究区SAR影像</div>
                                                <div class="result-info-value">{{ radarImages.length }}景影像</div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">累积影像覆盖率</div>
                                                <div class="result-info-value">{{ coverageRate.addRadar != 'NaN%'
                                                    ? coverageRate.addRadar : '待计算' }}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="w-full flex flex-col">
                                <button @click="calNoClouds"
                                    class="bg-[#0d1526] w-full  cursor-pointer text-white border border-[#2c3e50] rounded-lg px-4 py-2 hover:bg-[#1a2b4c] hover:border-[#2bb2ff] transition-all duration-200 active:scale-95">
                                    一版图重构
                                </button>
                                <div v-if="showProgress[3]"
                                    class="w-full  bg-[#1e293b] rounded-lg overflow-hidden border border-[#2c3e50]">
                                    <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                        :style="{ width: `${progress[3]}%` }"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>


                <section class="panel-section" v-if="calImage.length > 0">
                    <div class="section-header">
                        <div class="section-icon">
                            <CloudIcon :size="18" />
                        </div>
                        <h2 class="section-title">重构信息</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div v-for="(image, index) in calImage" class="config-item">
                                <div>无云一版图第{{ index + 1 }}次计算完成！</div>
                                本次使用的数据包括：{{ image.dataSet }}
                            </div>
                        </div>
                    </div>
                </section>

            </div>
        </dv-border-box12>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, type PropType, type Ref } from 'vue'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import { type interactiveExplore } from '@/components/dataCenter/type'
import { formatTime } from '@/util/common'
import { getSceneGrids, getNoCloud, getCaseStatus, getCaseResult } from '@/api/http/satellite-data'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import * as MapOperation from '@/util/map/operation'
import { ElMessage } from 'element-plus'
import ezStore from '@/store/ezStore'

import {
    DatabaseIcon,
    MapPinIcon,
    CircleHelp,
    CalendarIcon,
    UploadCloudIcon,
    RefreshCwIcon,
    HexagonIcon,
    CloudIcon,
    ApertureIcon,
    ClockIcon,
    ImageIcon,
    LayersIcon,
    DownloadIcon,
    FilePlus2Icon,
    BoltIcon,
    BanIcon,
    MapIcon,
} from 'lucide-vue-next'
import { FastBackwardFilled } from '@ant-design/icons-vue'

const props = defineProps({
    regionConfig: {
        type: Object as PropType<interactiveExplore>,
        required: true
    }
})

/**
 * 国产区
 */
// 各品类影像分类,1m是亚米
const demotic1mImages: Ref<any[]> = ref([])
const demotic2mImages: Ref<any[]> = ref([])
const internationalImages: Ref<any[]> = ref([])
const radarImages: Ref<any[]> = ref([])

// 累积影像分布到各个格网的计算结果
const demotic1mGridImages: Ref<any[]> = ref([])
const demotic2mGridImages: Ref<any[]> = ref([])
const interGridImages: Ref<any[]> = ref([])
const radarGridImages: Ref<any[]> = ref([])

// 记录每一级渲染的格网FeatureCollection
const demotic1mGridFeature: Ref<FeatureCollection | null> = ref(null)
const demotic2mGridFeature: Ref<FeatureCollection | null> = ref(null)
const interGridFeature: Ref<FeatureCollection | null> = ref(null)
const radarGridFeature: Ref<FeatureCollection | null> = ref(null)

interface CoverageRate {
    demotic1m: string | null;
    demotic2m: string | null;
    international: string | null;
    addRadar: string | null;
}
const coverageRate: Ref<CoverageRate> = ref({
    demotic1m: null,
    demotic2m: null,
    international: null,
    addRadar: null
})

// 看起来是计算属性，其实已经影像分类初始化了
const demotic = computed(() => {
    let allImages = props.regionConfig.images

    allImages.forEach((image: any) => {
        if (image.tags.includes('radar')) {
            radarImages.value.push(image)
        } else if (image.tags.includes('international')) {
            // 国外非雷达数据
            internationalImages.value.push(image)
        } else if (image.resolution === '2m') {
            demotic2mImages.value.push(image)
        } else {
            demotic1mImages.value.push(image)
        }
    })
    return demotic1mImages.value.length
})

const add2mDemoticImages = () => {
    cancelCheckbox('grid', 0)

    // 逻辑与addRadarImages中的一样，可以参考
    let operateData = dataReconstruction.value[0] ? demotic1mGridImages.value : demotic2mGridImages.value

    // 清除格网图层，得放到一个请求上面，不然添加图层的时候还没销毁
    // gridStore.cleadAllGrids()
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    // 不管是否勾选，都要调用这个，因为取消勾选的进度条显示逻辑也在里面！！！
    controlProgress(0)

    // 进度条加载完毕才能进行渲染图层，但是取消勾选不需要等待，而是立刻加载上一级的图层
    setTimeout(() => {
        let gridFeature: FeatureCollection = {
            type: 'FeatureCollection',
            features: props.regionConfig.grids.map((item: any, index) => {
                return {
                    type: 'Feature',
                    geometry: item.boundary.geometry as Geometry,
                    properties: {
                        ...(item.properties || {}),
                        id: item.properties?.id ?? index, // 确保每个都有 id
                        opacity: judgeGridOpacity(index, operateData),
                        source: classifyGridSource(index, operateData, demotic1mGridFeature.value, 'demotic2m') || null
                    }
                }
            })
        }
        demotic2mGridFeature.value = gridFeature

        MapOperation.map_addGridLayer(gridFeature)
        MapOperation.draw_deleteAll()
    }, dataReconstruction.value[0] ? 100 : mockProgressTime)
}

/**
 * 欧美区
 */

const addAbroadImages = () => {
    cancelCheckbox('grid', 1)
    // 逻辑与addRadarImages中的一样，可以参考
    let operateData = additionalData.value[1] ? demotic2mGridImages.value : interGridImages.value

    // 清除格网图层，得放到一个请求上面，不然添加图层的时候还没销毁
    // gridStore.cleadAllGrids()
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    // 不管是否勾选，都要调用这个，因为取消勾选的进度条显示逻辑也在里面！！！
    // controlProgress(1)

    // 进度条加载完毕才能进行渲染图层，但是取消勾选不需要等待，而是立刻加载上一级的图层
    setTimeout(() => {
        let gridFeature: FeatureCollection = {
            type: 'FeatureCollection',
            features: props.regionConfig.grids.map((item: any, index) => {
                return {
                    type: 'Feature',
                    geometry: item.boundary.geometry as Geometry,
                    properties: {
                        ...(item.properties || {}),
                        id: item.properties?.id ?? index, // 确保每个都有 id
                        opacity: judgeGridOpacity(index, operateData),
                        source: classifyGridSource(index, operateData, demotic2mGridFeature.value, 'international') || null
                    }
                }
            })
        }

        interGridFeature.value = gridFeature
        MapOperation.map_addGridLayer(gridFeature)
        MapOperation.draw_deleteAll()
    }, dataReconstruction.value[1] ? 100 : mockProgressTime)

}

const addRadarImages = () => {
    // 这里要考虑一个问题，就是勾选的时候，渲染三合一的数据，取消勾选的时候，要渲染二合一的数据，所以渲染数据要根据
    // 勾选框的数据变化比较晚，所以勾选的时候是false，取消勾选的时候是true
    let operateData = additionalData.value[2] ? interGridImages.value : radarGridImages.value

    // 清除格网图层，得放到一个请求上面，不然添加图层的时候还没销毁
    // gridStore.cleadAllGrids()
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    // 不管是否勾选，都要调用这个，因为取消勾选的进度条显示逻辑也在里面！！！
    // controlProgress(2)

    // 进度条加载完毕才能进行渲染图层，但是取消勾选不需要等待，而是立刻加载上一级的图层
    setTimeout(() => {
        let gridFeature: FeatureCollection = {
            type: 'FeatureCollection',
            features: props.regionConfig.grids.map((item: any, index) => {
                return {
                    type: 'Feature',
                    geometry: item.boundary.geometry as Geometry,
                    properties: {
                        ...(item.properties || {}),
                        id: item.properties?.id ?? index, // 确保每个都有 id
                        opacity: judgeGridOpacity(index, operateData),
                        source: classifyGridSource(index, operateData, interGridFeature.value, 'radar') || null
                    }
                }
            })
        }
        radarGridFeature.value = gridFeature
        MapOperation.map_addGridLayer(gridFeature)
        MapOperation.draw_deleteAll()
    }, dataReconstruction.value[2] ? 100 : mockProgressTime)
}


/**
 * 快进进度条
 */

// 四个进度条的进度值
const progress = ref([0, 0, 0, 0])
// 四个进度条的显示状态
const showProgress = ref([
    false, false, false, false
])
// const showCalResult = ref(false)
const calTask: Ref<any> = ref({
    calState: 'start',
    taskId: ''
})

// 填补勾选框
const additionalData = ref([true, false, false])
// 数据重构勾选框
const dataReconstruction = ref([false, false, false])


const calImage: Ref<any[]> = ref([])
let progressTimer: ReturnType<typeof setInterval> | null = null

// 控制进度条
const progressControl = (index: number) => {

    if (calTask.value.calState === 'pending') return
    progress.value[index] = 0
    calTask.value.calState = 'pending'
    progressTimer = setInterval(() => {
        if (calTask.value.calState === 'success' || calTask.value.calState === 'failed') {
            progress.value[index] = 100
            // showCalResult.value = true
            clearInterval(progressTimer!)
            progressTimer = null
        } else if (progress.value[index] < 95) {
            progress.value[index] += 1
        } else {
            progress.value[index] = 95
        }
    }, 100)
}

// 开始计算
const calNoClouds = async () => {
    // 发送请求，计算无云一版图

    // 根据勾选情况合并影像
    // 1、国产亚米

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


    let getNoCloudParam = {
        regionId: props.regionConfig.regionCode,
        cloud: props.regionConfig.cloud,
        resolution: props.regionConfig.space,
        sceneIds: addedImages.map(image => image.sceneId)
    }

    console.log(getNoCloudParam, '发起请求');

    let startCalcRes = await getNoCloud(getNoCloudParam)
    if (startCalcRes.message !== 'success') {
        ElMessage.error('计算失败，请重试')
        console.error(startCalcRes);
        return
    }

    calTask.value.taskId = startCalcRes.data

    // 1、启动进度条
    controlProgress(3)

    // // 2、轮询运行状态，直到运行完成
    const pollStatus = async (taskId: string) => {
        const interval = 1000 // 每秒轮询一次
        return new Promise<void>((resolve, reject) => {
            const timer = setInterval(async () => {
                try {
                    const res = await getCaseStatus(taskId)
                    console.log('轮询结果:', res)

                    if (res?.data === 'COMPLETE') {
                        clearInterval(timer)
                        resolve()
                    } else if (res?.data === 'FAILED' || 'ERROR') {
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
        await pollStatus(calTask.value.taskId)
        // ✅ 成功后设置状态
        calTask.value.calState = 'success'
        console.log('成功，开始拿结果');

        let res = await getCaseResult(calTask.value.taskId)
        console.log(res, '结果')

        // 1、先预览无云一版图影像
        let data = res.data.noCloud.tiles
        previewNoCloud(data)




        // 2、补充数据
        let calResult = {
            demotic1m: true,
            demotic2m: dataReconstruction.value[0],
            international: dataReconstruction.value[1],
            radar: dataReconstruction.value[2],
            dataSet: [
                '国产亚米影像',
                dataReconstruction.value[0] ? '国产2m超分影像' : null,
                dataReconstruction.value[1] ? '国外影像超分数据' : null,
                dataReconstruction.value[2] ? 'SAR色彩转换数据' : null,
            ].filter(Boolean).join('、')
        }
        console.log(dataReconstruction.value, calResult);

        calImage.value.push(calResult)

        ElMessage.success('无云一版图计算完成')
    } catch (error) {
        calTask.value.calState = 'failed'
        ElMessage.error('无云一版图计算失败，请重试')
    }
}
// 预览无云一版图
const previewNoCloud = async (data: any) => {



}
// 假操作进度条统一时间
const mockProgressTime = 500

// 现在的问题是，国外和SAR的勾选框有两个，取消一个都要取消后面的勾选框，所以作为一个单独的方法
const cancelCheckbox = (type: string, index: number) => {
    // 第一种情况，取消勾选格网填补
    if (type === 'grid' && additionalData.value[index] === true) {
        // showProgress.value[index] = false
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
// 操控进度条
const controlProgress = (index: number) => {

    // 1、取消勾选要把后面的选项全部取消勾选。2、取消勾选隐藏进度条
    // 这里要注意，additionalData值变化是延后的，所以是变化前的值
    let overTask = cancelCheckbox('dataReconstruction', index)
    if (overTask) return

    // 只显示当前进度条
    showProgress.value = showProgress.value.map((_progress, i: number) => {
        return index === i ? true : false
    })

    progressControl(index)

    // 2、轮询运行状态，直到运行完成
    setTimeout(() => {
        calTask.value.calState = 'success'
    }, mockProgressTime)
}


onMounted(async () => {
    // 清除格网图层，得放到一个请求上面，不然添加图层的时候还没销毁
    // gridStore.cleadAllGrids()
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    // 计算四个覆盖率
    let gridCount = props.regionConfig.grids.length
    let allGrids = props.regionConfig.grids.map((item: any) => {
        return {
            rowId: item.rowId,
            columnId: item.columnId,
            resolution: item.resolution
        }
    })


    // 计算四种情况的格网分布情况
    demotic1mGridImages.value = await getSceneGrids({
        grids: allGrids,
        sceneIds: demotic1mImages.value.map(images => images.sceneId)
    })
    coverageRate.value.demotic1m = getCoverage(demotic1mGridImages.value, gridCount)

    let addDemotic1mImages = demotic1mImages.value.concat(demotic2mImages.value)
    demotic2mGridImages.value = await getSceneGrids({
        grids: allGrids,
        sceneIds: addDemotic1mImages.map(images => images.sceneId)
    })
    coverageRate.value.demotic2m = getCoverage(demotic2mGridImages.value, gridCount)

    let addInternationalImages = addDemotic1mImages.concat(internationalImages.value)
    interGridImages.value = await getSceneGrids({
        grids: allGrids,
        sceneIds: addInternationalImages.map(images => images.sceneId)
    })
    coverageRate.value.international = getCoverage(interGridImages.value, gridCount)

    let addRadarImages = addInternationalImages.concat(radarImages.value)
    radarGridImages.value = await getSceneGrids({
        grids: allGrids,
        sceneIds: addRadarImages.map(images => images.sceneId)
    })
    coverageRate.value.addRadar = getCoverage(radarGridImages.value, gridCount)

    // 国产影像渲染
    // 添加带有数据指示的格网
    let gridFeature: FeatureCollection = {
        type: 'FeatureCollection',
        features: props.regionConfig.grids.map((item: any, index) => {
            return {
                type: 'Feature',
                geometry: item.boundary.geometry as Geometry,
                properties: {
                    ...(item.properties || {}),
                    id: item.properties?.id ?? index, // 确保每个都有 id
                    opacity: judgeGridOpacity(index, demotic1mGridImages.value),
                    source: classifyGridSource(index, demotic1mGridImages.value, null) || null
                }
            }
        })
    }
    console.log(props.regionConfig.grids, 111);

    demotic1mGridFeature.value = gridFeature
    MapOperation.map_addGridLayer(gridFeature)
    MapOperation.draw_deleteAll()

    ElMessage.success(`国产亚米级影像加载完毕`)
})

// 算格网的颜色,接收的数据分别为：要上色的格网本身，累积影像分布到格网的结果，格网数量，所属层级
// 取消勾选，即回到上一级数据格网的结果也没问题，第三个传输就传递上一级（和第二个参数相同）即可
const classifyGridSource = (index: any, sceneGridsRes: any, lastGridFeature?: any, type?: string) => {
    if (lastGridFeature === null) {
        let source: string | null
        sceneGridsRes[index]?.scenes.length > 0 ? source = 'demotic1m' : source = null;
        return source
    } else if (type !== undefined) {
        let source: string | null
        let lastSource = lastGridFeature.features[index].properties.source
        lastSource ? source = lastSource : sceneGridsRes[index]?.scenes.length > 0 ? source = type : source = null;
        return source
    }
    return null
}

// 判断格网到底有没有数据，有就返回0.3
const judgeGridOpacity = (index: number, sceneGridsRes: any) => {
    let opacity = 0.01
    sceneGridsRes[index]?.scenes.length > 0 ? opacity = 0.3 : opacity = 0.01;
    return opacity
}

// 算覆盖率
const getCoverage = (gridImages: any, gridCount: number) => {
    const nonEmptyScenesCount = gridImages.filter(item => item.scenes.length > 0).length
    let coverage = (nonEmptyScenesCount * 100 / gridCount).toFixed(2) + '%';
    return coverage
}
</script>

<style scoped src="./tabStyle.css">
:deep(.border-box-content) {
    padding: 1.5rem;
}
</style>