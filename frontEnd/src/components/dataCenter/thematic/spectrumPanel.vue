<template>
    <!-- Section: 输入模块 -->
    <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <MapIcon :size="18" />
            </div>
            <h2 class="section-title">光谱分析</h2>
        </div>
        <div class="section-content">
            <div class="config-container">
                <div class="config-item">
                    <div class="config-label relative">
                        <MapIcon :size="16" class="config-icon" />
                        <span>待分析影像</span>
                    </div>
                    <div class="config-control justify-center">
                        <div class="flex items-center gap-2 mt-2 w-full">
                            <label class="text-white">影像选择：</label>
                            <select v-model="selectedSceneId" @change="showImageBBox"
                                class="bg-[#0d1526] text-[#38bdf8] border border-[#2c3e50] rounded-lg px-3 py-1 appearance-none hover:border-[#2bb2ff] focus:outline-none focus:border-[#3b82f6] max-w-[calc(100%-90px)] truncate">
                                <option disabled selected value="">请选择影像</option>
                                <option v-for="image in hyperspectralImages" :key="image.sceneName"
                                    :value="image.sceneId" :title="image.sceneName" class="truncate">
                                    {{ image.sceneName }}
                                </option>
                            </select>
                        </div>
                        <!-- <div class="w-full space-y-2">
                            <div v-if="allDsmImages.length === 0" class="flex justify-center my-6">
                                <SquareDashedMousePointer class="mr-2" />该区域暂无DSM影像
                            </div>
                            <div v-for="(image, index) in allDsmImages" :key="index" @click="showTif(image)"
                                class="flex flex-col border border-[#247699] bg-[#0d1526] text-white px-4 py-2 rounded-lg transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c]">
                                <div class="font-semibold text-base">{{ image.sceneName }}</div>
                                <div class="text-sm text-gray-400">{{ formatTime(image.sceneTime, 'minutes') }}</div>
                            </div>
                        </div> -->
                    </div>
                </div>
                <div class="config-item">
                    <div class="config-label relative">
                        <MapIcon :size="16" class="config-icon" />
                        <span>空间选择</span>
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
                                地图选点
                            </div>

                            <!-- 划线采点块 -->
                            <div @click="!true && toggleMode('line')"
                                class="w-24 h-24 flex flex-col items-center justify-center rounded-lg border cursor-pointer transition-all duration-200 text-white relative"
                                :class="[
                                    activeMode === 'false'
                                        ? 'border-[#2bb2ff] bg-[#1a2b4c]'
                                        : 'border-[#247699] bg-[#0d1526]',
                                    true
                                        ? 'opacity-50 cursor-not-allowed pointer-events-none'
                                        : 'hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95'
                                ]">
                                <LayersIcon class="mb-2" />
                                划线采点
                                <div v-if="true"
                                    class="absolute inset-0 bg-black bg-opacity-40 rounded-lg flex flex-col items-center justify-center text-xs text-white cursor-not-allowed">
                                    <LayersIcon class="mb-2" />
                                    划线采点
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <button @click="analysisSpectrum"
                    class="cursor-pointer rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95">
                    开始分析
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
            <h2 class="section-title">计算结果</h2>
        </div>
        <div class="section-content">
            <div v-if="analysisData.length === 0" class="flex justify-center my-6">
                <SquareDashedMousePointer class="mr-2" />暂无计算结果
            </div>
            <div class="config-item" v-for="(item, index) in analysisData" :key="index">
                <div>第{{ index + 1 }}次计算：{{ item.analysis }}</div>
                <div v-if="item.imageName">所选影像为：{{ item.imageName }}</div>
                <!-- <div>NDVI计算结果为：xxx</div> -->
                <!-- <div>统计数据-统计数据-统计数据-统计数据</div> -->
                <div>经纬度：（{{ item.point[0] }},{{ item.point[1] }}）</div>

                <div class="chart-wrapper flex flex-col items-end">
                    <div class="chart" :ref="el => setChartRef(el, index)" :id="`chart-${index}`"
                        style="width: 100%; height: 400px;"></div>
                    <button class="!text-[#38bdf8] cursor-pointer" @click="fullscreenChart(index)">全屏查看</button>
                </div>
            </div>
        </div>
    </section>
</template>

<script setup lang="ts">

import { ezStore, useGridStore } from '@/store';
import { ElMessage } from 'element-plus';
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
import { computed, nextTick, onMounted, onUnmounted, ref, watch, type ComponentPublicInstance, type ComputedRef, type Ref } from 'vue';
import * as echarts from 'echarts'
import * as MapOperation from '@/util/map/operation'
import { getBoundaryBySceneId, getCaseResult, getCaseStatus, getRasterScenesDes, getSpectrum } from '@/api/http/satellite-data';
import bus from '@/store/bus'
import mapboxgl from 'mapbox-gl'
import { mapManager } from '@/util/map/mapManager';



type ThematicConfig = {
    allImages: any,
    regionId: number,
    startTime: string,
    endTime: string
}
type LatLng = [number, number]

const props = defineProps<{ thematicConfig: ThematicConfig }>()


const hyperspectralImages = computed(() => {
    let filteredImages = props.thematicConfig.allImages.filter((image: any) => {
        return image.sceneName.includes('AHSI')
    })
    if (filteredImages.length === 0) {
        ElMessage.warning('该区域暂无高光谱影像')
    }

    return filteredImages
})

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
        ElMessage.info('请在地图上绘制研究点')
    } else if (mode === 'line') {
        MapOperation.draw_lineMode()
        ElMessage.info('请在地图上绘制研究线')
    }
}

const showImageBBox = async () => {
    let getDescriptionRes = await getBoundaryBySceneId(selectedSceneId.value)
    const FeatureCollectionBoundary: GeoJSON.FeatureCollection = {
        type: "FeatureCollection",
        features: [getDescriptionRes]
    }
    try {
        MapOperation.map_addPolygonLayer({
            geoJson: FeatureCollectionBoundary,
            id: 'UniqueSceneLayer',
            lineColor: '#8fffff',
            fillColor: '#a4ffff',
            fillOpacity: 0.2,
        })
        ElMessage.success('已加影像边界，请在影像与行政区的交集内选点。')
    } catch (e) {
        console.error("有错误找后端", e)
        ElMessage.error('加载影像边界失败。')
    }
}

/**
 * 计算
 */
const selectedSceneId = ref('')

const calTask: Ref<any> = ref({
    calState: 'start',
    taskId: ''
})
const analysisData = ref<any>([])

const analysisSpectrum = async () => {

    if (!pickedPoint.value[0] || !pickedPoint.value[1]) {
        ElMessage.warning('请先选择您要计算的区域')
        return
    }
    if (selectedSceneId.value === '') {
        ElMessage.warning('请先选择您要计算的影像')
        return
    }
    ElMessage.success('开始高光谱分析。')
    let spectrumParam = {
        sceneId: selectedSceneId.value,
        point: [pickedPoint.value[1], pickedPoint.value[0]]
    }
    let getSpectrumRes = await getSpectrum(spectrumParam)
    if (getSpectrumRes.message !== 'success') {
        ElMessage.error('计算失败，请重试')
        console.error(getSpectrumRes)
        return
    }
    calTask.value.taskId = getSpectrumRes.data


    // 1、启动进度条
    // showProgress.value = true
    // progressControl()

    // 2、轮询运行状态，直到运行完成
    // ✅ 轮询函数，直到 data === 'COMPLETE'
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

    // 找到影像名称
    const selectedImage = props.thematicConfig.allImages.find(image => image.sceneId = selectedSceneId.value)
    // console.log(selectedImage);


    try {
        await pollStatus(calTask.value.taskId)
        // ✅ 成功后设置状态
        calTask.value.calState = 'success'
        let res = await getCaseResult(calTask.value.taskId)
        console.log(res, '结果');
        let spectrum = res.data.spectral_profile

        let xData = spectrum.map(data => data.band + '波段')
        let yData = spectrum.map(data => data.value)

        analysisData.value.push({
            yData,
            xData,
            type: 'line',
            analysis: "定点光谱分析",
            imageName: selectedImage.sceneName,
            point: [...pickedPoint.value]
        })
        ElMessage.success('光谱分析计算完成')
    } catch (error) {
        calTask.value.calState = 'failed'
        ElMessage.error('光谱分析计算失败，请重试')
        console.error(error);
    }
}

/**
 * 图表绘制
 */
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

const ndviProjectId = ref('')
const spectrumProjectId = ref('')


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
onMounted(() => {
    bus.on('point-finished', createMarker);
    ndviProjectId.value = ezStore.get('conf').ndviProjectId
    spectrumProjectId.value = ezStore.get('conf').spectrumProjectId
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
