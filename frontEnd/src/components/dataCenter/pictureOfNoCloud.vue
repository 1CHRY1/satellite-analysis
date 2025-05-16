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
                                                <div class="result-info-label">研究区国产影像</div>
                                                <div class="result-info-value">{{ demotic }}景影像</div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">影像覆盖率</div>
                                                <div class="result-info-value">{{ coverageRate.demotic }}</div>
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
                                            <input type="checkbox" v-model="additionalData[0]" @click="addAbroadImages"
                                                class="w-4 h-4 rounded">
                                            国外影像填补缺失格网
                                        </label>
                                        <div v-if="showProgress[0]"
                                            class="w-full  bg-[#1e293b] rounded-lg overflow-hidden border border-[#2c3e50]">
                                            <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                                :style="{ width: `${progress[0]}%` }"></div>
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
                                                <div class="result-info-value">{{ coverageRate.international || '待计算' }}
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
                                            <input type="checkbox" v-model="additionalData[1]" @click="addRadarImages"
                                                :disabled="!additionalData[0]" class="w-4 h-4 rounded">
                                            SAR影像填补缺失格网
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
                                                <div class="result-info-value">{{ coverageRate.addRadar || '待计算' }}
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
                                <div v-if="showProgress[2]"
                                    class="w-full  bg-[#1e293b] rounded-lg overflow-hidden border border-[#2c3e50]">
                                    <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                        :style="{ width: `${progress[2]}%` }"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>


                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <CloudIcon :size="18" />
                        </div>
                        <h2 class="section-title">重构结果</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div v-for="(image, index) in calImage" class="config-item">
                                第{{ index + 1 }}次计算结果为：
                                无云一版图计算结果为：xxx
                                统计数据-统计数据-统计数据-统计数据
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

const props = defineProps({
    regionConfig: {
        type: Object as PropType<interactiveExplore>,
        required: true
    }
})

/**
 * 国产区
 */
const demoticImages: Ref<any[]> = ref([])
const internationalImages: Ref<any[]> = ref([])
const radarImages: Ref<any[]> = ref([])
const demoticGridImages: Ref<any[]> = ref([])
const interGridImages: Ref<any[]> = ref([])
const radarGridImages: Ref<any[]> = ref([])
interface CoverageRate {
    demotic: string | null;
    international: string | null;
    addRadar: string | null;
}
const coverageRate: Ref<CoverageRate> = ref({
    demotic: null,
    international: null,
    addRadar: null
})

// 看起来是计算属性，其实已经影像分类初始化了
const demotic = computed(() => {
    let allImages = props.regionConfig.images
    allImages.forEach((image: any) => {
        if (image.tags.includes('radar')) {
            radarImages.value.push(image)
        } else if (image.tags.includes('national')) {
            // 国内非雷达数据
            demoticImages.value.push(image)
        } else {
            internationalImages.value.push(image)
        }
    })
    return demoticImages.value.length
})

/**
 * 欧美区
 */

const addAbroadImages = () => {
    // 逻辑与addRadarImages中的一样，可以参考
    let operateData = additionalData.value[0] ? demoticGridImages.value : interGridImages.value

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
                        opacity: judgeGridOpacity(item, operateData)
                    }
                }
            })
        }
        MapOperation.map_addGridLayer(gridFeature)
        MapOperation.draw_deleteAll()
    }, additionalData.value[0] ? 100 : mockProgressTime)

}

const addRadarImages = () => {
    // 这里要考虑一个问题，就是勾选的时候，渲染三合一的数据，取消勾选的时候，要渲染二合一的数据，所以渲染数据要根据
    // 勾选框的数据变化比较晚，所以勾选的时候是false，取消勾选的时候是true
    let operateData = additionalData.value[1] ? interGridImages.value : radarGridImages.value

    // 清除格网图层，得放到一个请求上面，不然添加图层的时候还没销毁
    // gridStore.cleadAllGrids()
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    // 不管是否勾选，都要调用这个，因为取消勾选的进度条显示逻辑也在里面！！！
    controlProgress(1)

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
                        opacity: judgeGridOpacity(item, operateData)
                    }
                }
            })
        }
        MapOperation.map_addGridLayer(gridFeature)
        MapOperation.draw_deleteAll()
    }, additionalData.value[1] ? 100 : mockProgressTime)

}


/**
 * 快进进度条
 */

const progress = ref([0, 0, 0])
const showProgress = ref([
    false, false, false
])


const showCalResult = ref(false)
const calTask: Ref<any> = ref({
    calState: 'start',
    taskId: ''
})
const additionalData = ref([false, false])
const calImage: Ref<any[]> = ref([{
    tifPath: "",
    bucket: ""
}])
let progressTimer: ReturnType<typeof setInterval> | null = null

// 控制进度条
const progressControl = (index: number) => {

    if (calTask.value.calState === 'pending') return
    progress.value[index] = 0
    calTask.value.calState = 'pending'
    progressTimer = setInterval(() => {
        if (calTask.value.calState === 'success' || calTask.value.calState === 'failed') {
            progress.value[index] = 100
            showCalResult.value = true
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
    let addedImages = [...demoticImages.value]
    if (additionalData.value[0] === true) {
        addedImages = addedImages.concat(internationalImages.value)
    }
    if (additionalData.value[1] === true) {
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
    controlProgress(2)

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
        let res = await getCaseResult(calTask.value.taskId)
        console.log(res, '结果');
        // drawData.value.push(drawData.value[0])
        ElMessage.success('无云一版图计算完成')
    } catch (error) {
        calTask.value.calState = 'failed'
        ElMessage.error('NDVI计算失败，请重试')
    }
    console.log(getNoCloudParam, startCalcRes, 1111);



    // 3、渲染运行结果
    let imageUrl = 'http://223.2.32.166:30900/test-images/landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20240320_20240402_02_T1/LC08_L2SP_118038_20240320_20240402_02_T1_SR_B2.TIF'
    previewNoCloud(imageUrl)
}
// 预览无云一版图
const previewNoCloud = async (imageUrl: string) => {
    console.log(imageUrl);

    const res = await fetch('/app.conf.json')
    let conf = await res.json()
    let minioIpAndPort = conf.minioIpAndPort
    let requestUrl = conf.titiler +
        console.log(conf.minioIpAndPort, props.regionConfig.boundary);

}
// 假操作进度条统一时间
const mockProgressTime = 6000
// 操控进度条
const controlProgress = (index: number) => {

    // 1、第一个要取消勾选要把第二个改成false。2、取消勾选隐藏进度条
    // 这里要注意，additionalData值变化是延后的，所以是变化前的值
    if (additionalData.value[index] === true) {
        showProgress.value[index] = false
        if (index === 0) {
            additionalData.value[1] = false
            showProgress.value[1] = false
        }
        return
    }
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

// 判断格网到底有没有数据，有就返回0.3
const judgeGridOpacity = (item: any, sceneGridsRes: any) => {

    let opacity = 0.01
    sceneGridsRes.forEach(element => {
        if (element.columnId === item.columnId && element.rowId === item.rowId) {
            element.scenes.length > 0 ? opacity = 0.3 : opacity = 0.01;
        }
    });
    return opacity
}

onMounted(async () => {
    // 清除格网图层，得放到一个请求上面，不然添加图层的时候还没销毁
    // gridStore.cleadAllGrids()
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    // 计算三个覆盖率
    let gridCount = props.regionConfig.grids.length
    let allGrids = props.regionConfig.grids.map((item: any) => {
        return {
            rowId: item.rowId,
            columnId: item.columnId,
            resolution: item.resolution
        }
    })

    demoticGridImages.value = await getSceneGrids({
        grids: allGrids,
        sceneIds: demoticImages.value.map(images => images.sceneId)
    })
    coverageRate.value.demotic = getCoverage(demoticGridImages.value, gridCount)

    let addInternationalImages = internationalImages.value.concat(demoticImages.value)
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
                    opacity: judgeGridOpacity(item, demoticGridImages.value)
                }
            }
        })
    }

    MapOperation.map_addGridLayer(gridFeature)
    MapOperation.draw_deleteAll()

    ElMessage.success(`国产影像加载完毕`)
})

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