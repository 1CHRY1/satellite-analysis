<template>
    <!-- Section: 输入模块 -->
    <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <MapIcon :size="18" />
            </div>
            <h2 class="section-title">{{ t('datapage.optional_thematic.rate.title') }}</h2>
        </div>
        <div class="section-content">
            <div class="config-container">
                <div class="config-item">
                    <div class="config-label relative">
                        <MapIcon :size="16" class="config-icon" />
                        <span>{{ t('datapage.optional_thematic.rate.set') }}</span>
                    </div>
                    <div class="config-control justify-center">
                        <div class="w-full space-y-2">
                            <div v-if="allDeforRateImages.length === 0" class="flex justify-center my-6">
                                <SquareDashedMousePointer class="mr-2" />{{ t('datapage.optional_thematic.rate.no_ima') }}
                            </div>
                            <div v-for="(image, index) in allDeforRateImages" :key="index" @click="showTif(image)"
                                class="flex flex-col border cursor-pointer border-[#247699] bg-[#0d1526] text-white px-4 py-2 rounded-lg transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c]">
                                <div class="font-semibold text-base">{{ image.sceneName }}</div>
                                <div class="text-sm text-gray-400">{{ formatTime(image.sceneTime, 'minutes') }}</div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="config-item">
                    <div class="config-label relative">
                        <MapIcon :size="16" class="config-icon" />
                        <span>{{ t('datapage.optional_thematic.rate.space') }}</span>
                    </div>
                    <div class="config-control justify-center">
                        <div class="flex gap-10">
                            <!-- 地图选点块 -->
                            <div @click="toggleMode('point')"
                                class="w-24 h-24 flex flex-col items-center justify-center rounded-lg border cursor-pointer transition-all duration-200 text-white"
                                :class="[
                                    activeMode === 'false'
                                        ? 'border-[#2bb2ff] bg-[#1a2b4c]'
                                        : 'border-[#247699] bg-[#0d1526]',
                                    'hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95'
                                ]">
                                <MapPinIcon class="mb-2" />
                                {{ t('datapage.optional_thematic.rate.map') }}
                            </div>

                            <!-- 划线采点块 -->
                            <div @click="toggleMode('line')"
                                class="w-24 h-24  flex flex-col items-center justify-center rounded-lg border cursor-pointer transition-all duration-200 text-white"
                                :class="[
                                    activeMode === 'false'
                                        ? 'border-[#2bb2ff] bg-[#1a2b4c]'
                                        : 'border-[#247699] bg-[#0d1526]',
                                    'hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95'
                                ]">
                                <LayersIcon class="mb-2" />
                                {{ t('datapage.optional_thematic.rate.point') }}
                            </div>
                        </div>

                    </div>
                </div>
                <button @click="analysisDeforRate"
                    class="cursor-pointer rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95">
                    {{ t('datapage.optional_thematic.rate.button') }}
                </button>
            </div>
        </div>
    </section>

    <!-- Section: 结果展示 -->
    <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <ChartColumn :size="18" />
            </div>
            <h2 class="section-title" @click="test">{{ t('datapage.optional_thematic.rate.result') }}</h2>
        </div>
        <div class="section-content">
            <div class="config-container">
                <div v-if="analysisData.length === 0" class="flex justify-center my-6">
                    <SquareDashedMousePointer class="mr-2" />{{ t('datapage.optional_thematic.rate.no_result') }}
                </div>
                <div v-for="(item, index) in analysisData" :key="index" class="config-item">
                    <div class="config-label relative">
                        <MapIcon :size="16" class="config-icon" />
                        <span>第{{ index + 1 }}次分析结果：</span>
                    </div>
                    <div class="config-control justify-center">
                        <div v-if="item.type === 'singleValue'">
                            经纬度（{{ item.point[1] }},{{ item.point[0] }}）的形变速率值为:{{ item.value }}
                        </div>
                        <div v-if="item.type === 'line'">
                            在空间位置：{{item.line.map(([lng, lat]) => `(${lng}, ${lat})`).join(' -> ')}} 上的形变速率变化趋势为：
                            <div class="chart-wrapper flex flex-col items-end">
                                <div class="chart" :ref="el => setChartRef(el, index)" :id="`chart-${index}`"
                                    style="width: 100%; height: 400px;"></div>
                                <button class="!text-[#38bdf8] cursor-pointer"
                                    @click="fullscreenChart(index)">{{ t('datapage.optional_thematic.rate.fullview') }}</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch, watchEffect, type ComponentPublicInstance, type ComputedRef, type Ref } from 'vue';
import { getRasterScenesDes, getRasterPoints, getBoundaryBySceneId, getCaseStatus, getCaseResult, getRasterLine, getDescriptionBySceneId, getWindow } from '@/api/http/satellite-data';
import * as MapOperation from '@/util/map/operation'
import { useGridStore, ezStore } from '@/store'
import { formatTime } from '@/util/common';
import * as echarts from 'echarts'
import bus from '@/store/bus'
import mapboxgl from 'mapbox-gl'
import { mapManager } from '@/util/map/mapManager';

import {
    ChartColumn,
    Earth,
    MapPinIcon,
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
    SquareDashedMousePointer
} from 'lucide-vue-next'

import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue';
const { t } = useI18n()



const test = () => {
    console.log(pickedLine.value);

}

/**
 * type
 */
type ThematicConfig = {
    allImages: any,
    regionId: number,
    startTime: string,
    endTime: string
}
type LatLng = [number, number]


const props = defineProps<{ thematicConfig: ThematicConfig }>()
const allDeforRateImages = ref<any>([])
const initDeforRatePanel = async () => {
    let thematicConfig = props.thematicConfig
    if (!thematicConfig.regionId) return
    let rasterParam = {
        startTime: thematicConfig.startTime,
        endTime: thematicConfig.endTime,
        regionId: thematicConfig.regionId,
        dataType: 'svr'
    }
    allDeforRateImages.value = await getRasterScenesDes(rasterParam)
    console.log(allDeforRateImages.value, 57);
}


const activeMode = ref<'point' | 'line' | 'false' | null>(null)
const gridStore = useGridStore()
const pickedPoint = computed(() => {
    return [
        Math.round(gridStore._point[0] * 1000000) / 1000000,
        Math.round(gridStore._point[1] * 1000000) / 1000000
    ];
})
const pickedLine: ComputedRef<LatLng[]> = computed(() => {
    return gridStore._line.map(([lat, lng]) => [
        Math.round(lat * 1000000) / 1000000,
        Math.round(lng * 1000000) / 1000000
    ])
})

const toggleMode = (mode: 'point' | 'line' | 'false') => {
    // activeMode.value = activeMode.value === mode ? null : mode
    activeMode.value = mode
    if (mode === 'point') {
        MapOperation.draw_pointMode()
        message.info(t('datapage.optional_thematic.rate.message.info_point'))
    } else if (mode === 'line') {
        MapOperation.draw_lineMode()
        message.info(t('datapage.optional_thematic.rate.message.info_line'))
    }
}

const showTif = async (image) => {
    message.success(t('datapage.optional_thematic.rate.message.load'))
    let sceneId = image.sceneId
    let res = await getDescriptionBySceneId(sceneId)
    console.log(res, sceneId, 7575);
    let url = res.images[0].bucket + '/' + res.images[0].tifPath
    MapOperation.map_addOneBandColorLayer({
        fullTifPath: url
    })
    let window = await getWindow(sceneId)
    MapOperation.map_fitView([
        [window.bounds[0], window.bounds[1]],
        [window.bounds[2], window.bounds[3]],
    ])
}

const calTask: Ref<any> = ref({
    calState: 'start',
    taskId: ''
})
const analysisData = ref<any>([])

const analysisDeforRate = async () => {
    // let boundary = await getBoundaryBySceneId(allDeforRateImages.value[0].sceneId)
    // MapOperation.map_addPolygonLayer({
    //     geoJson: boundary,
    //     id: 'UniqueLayer',
    //     lineColor: '#8fffff',
    //     fillColor: '#a4ffff',
    //     fillOpacity: 0.2,
    // })
    if (!verifyAnalysis()) {
        return
    }
    message.success(t('datapage.optional_thematic.rate.message.info_start'))
    if (activeMode.value === 'point') {
        let pointParam = {
            point: [pickedPoint.value[1], pickedPoint.value[0]],
            sceneIds: allDeforRateImages.value.map((image) => image.sceneId)
        }
        let res = await getRasterPoints(pointParam)
        if (res?.message != 'success') {
            message.warning(t('datapage.optional_thematic.rate.message.calerror'))
        }
        calTask.value.taskId = res.data
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
                        } else if (res?.data === 'ERROR') {
                            console.log(res, res.data, 15616);

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
            let deforRateValue = res.data.value

            // let xData = spectrum.map(data => data.band + '波段')
            // let yData = spectrum.map(data => data.value)

            analysisData.value.push({
                type: 'singleValue',
                analysis: "单点形变速率求值",
                value: deforRateValue,
                point: [...pickedPoint.value]
            })
            message.success(t('datapage.optional_thematic.rate.message.success'))
        } catch (error) {
            calTask.value.calState = 'failed'
            message.error(t('datapage.optional_thematic.rate.message.info_retry'))
            console.error(error);
        }
    } else if (activeMode.value === 'line') {
        let points = samplePointsOnLine(pickedLine.value, 10)
        let lineParam = {
            points: points,
            sceneIds: allDeforRateImages.value.map((image) => image.sceneId)
        }
        let lineRes = await getRasterLine(lineParam)

        if (lineRes?.message != 'success') {
            message.warning(t('datapage.optional_thematic.rate.message.calerror'))
        }
        calTask.value.taskId = lineRes.data
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
                        } else if (res?.data === 'ERROR') {
                            console.log(res, res.data, 15616);

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
            let deforRateValue = res.data.values

            let xData = Array.from({ length: 10 }, (_, i) => i + 1)
            let yData = deforRateValue

            analysisData.value.push({
                xData,
                yData,
                type: 'line',
                analysis: "空间采点形变速率变化趋势",
                line: pickedLine.value
            })
            message.success(t('datapage.optional_thematic.rate.message.success'))
        } catch (error) {
            calTask.value.calState = 'failed'
            message.error(t('datapage.optional_thematic.rate.message.info_retry'))
            console.error(error);
        }
    }
}
// 卫语句
const verifyAnalysis = () => {
    if (activeMode.value != 'point' && activeMode.value != 'line') {
        message.warning(t('datapage.optional_thematic.rate.message.info_space'))
        return false
    }
    if (allDeforRateImages.value.length === 0) {
        message.warning(t('datapage.optional_thematic.rate.message.info_noima'))
        return false
    }

    return true
}

// 等距采样方法
const samplePointsOnLine = (points: LatLng[], count: number): LatLng[] => {
    if (points.length < 2 || count < 2) return points

    // 计算每一段的欧氏距离和总长度
    const distances: number[] = []
    let totalLength = 0
    for (let i = 0; i < points.length - 1; i++) {
        const [x1, y1] = points[i]
        const [x2, y2] = points[i + 1]
        const dist = Math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2)
        distances.push(dist)
        totalLength += dist
    }

    // 计算等间距目标距离
    const segmentLength = totalLength / (count - 1)
    const sampledPoints: LatLng[] = [points[0]]

    let currentSegment = 0
    let accumulatedDist = 0

    for (let i = 1; i < count - 1; i++) {
        const targetDist = i * segmentLength

        // 在当前累积距离内移动到目标距离所在的线段
        while (accumulatedDist + distances[currentSegment] < targetDist) {
            accumulatedDist += distances[currentSegment]
            currentSegment++
        }

        const segmentStart = points[currentSegment]
        const segmentEnd = points[currentSegment + 1]
        const segmentDist = distances[currentSegment]
        const remain = targetDist - accumulatedDist
        const ratio = remain / segmentDist

        const [x1, y1] = segmentStart
        const [x2, y2] = segmentEnd
        const x = x1 + (x2 - x1) * ratio
        const y = y1 + (y2 - y1) * ratio

        sampledPoints.push([x, y])
    }

    sampledPoints.push(points[points.length - 1])
    return sampledPoints
}

const chartInstances = ref<(echarts.ECharts | null)[]>([])

// 初始化图表
const initChart = (el: HTMLElement, data: any, index: number) => {
    if (!el) return
    const existingInstance = echarts.getInstanceByDom(el)
    if (existingInstance) {
        existingInstance.dispose()
    }
    let chart = echarts.init(el)
    chart.setOption({
        title: {
            text: `图表 ${index + 1}`
        },
        xAxis: {
            type: 'category',
            data: data.xData
        },
        yAxis: {
            type: 'value'
        },
        series: [
            {
                data: data.yData,
                type: data.type
            }
        ],
        tooltip: {
            trigger: 'axis'
        },
        responsive: true
    })
    chart.resize()
    chartInstances.value[index] = chart
}

// 设置 ref 并初始化图表
const setChartRef = (el: Element | ComponentPublicInstance | null, index: number) => {
    if (el instanceof HTMLElement) {
        nextTick(() => {

            initChart(el, analysisData.value[index], index)
        })
    }
}

// 全屏查看功能
const fullscreenChart = (index: number) => {
    const dom = document.getElementById(`chart-${index}`)
    if (dom?.requestFullscreen) {
        dom.requestFullscreen()
    } else if ((dom as any).webkitRequestFullScreen) {
        (dom as any).webkitRequestFullScreen()
    } else if ((dom as any).mozRequestFullScreen) {
        (dom as any).mozRequestFullScreen()
    } else if ((dom as any).msRequestFullscreen) {
        (dom as any).msRequestFullscreen()
    }
}

// 图表自适应
window.addEventListener('resize', () => {
    chartInstances.value.forEach(chart => {
        if (chart) chart.resize()
    })
})

// 响应式监听 analysisData 变化并重新渲染
watch(analysisData, (newData) => {
    nextTick(() => {
        newData.forEach((item, index) => {
            const el = document.getElementById(`chart-${index}`)
            if (el) {
                initChart(el, item, index)
            }
        })
    })
}, { deep: true })

watch(() => props.thematicConfig.regionId, initDeforRatePanel)

const markerRef = ref<mapboxgl.Marker | null>(null);
const createMarker = ({ lng, lat }) => {

    mapManager.withMap((map) => {
        if (markerRef.value) {
            markerRef.value.remove(); // 移除之前的标记
        }
        markerRef.value = new mapboxgl.Marker() // 创建一个新的标记
            .setLngLat([lng, lat]) // 设置标记的位置
            .addTo(map); // 将标记添加到地图上
    })
}

onMounted(async () => {
    await initDeforRatePanel()
    bus.on('point-finished', createMarker);
    nextTick(() => {
        analysisData.value.forEach((item, index) => {
            const el = document.getElementById(`chart-${index}`)
            if (el) {
                initChart(el, item, index)
            }
        })
    })
})
onUnmounted(() => {
    gridStore.clearPicked()
    bus.off('point-finished', createMarker);
    if (markerRef.value) markerRef.value.remove()
})
</script>

<style scoped src="../tabStyle.css"></style>
