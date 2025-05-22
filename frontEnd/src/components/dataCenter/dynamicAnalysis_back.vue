<template>
    <div class="custom-panel px-2">
        <dv-border-box12 class="!h-[calc(100vh-56px-48px-32px-8px)]">
            <div class="main-container">
                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <ChartColumn :size="18" />
                        </div>
                        <h2 class="section-title">计算领域：</h2>
                        <select v-model="selectedTask"
                            class=" max-w-full truncate bg-[#0d1526] text-white w-40 border border-[#2c3e50] rounded-lg px-4 py-2 pr-8 appearance-none transition-all duration-200 hover:border-[#206d93] focus:outline-none focus:border-[#3b82f6]">
                            <option v-for="option in optionalTasks" :key="option.value" :value="option.value"
                                :disabled="option.disabled" class="bg-[#0d1526] "
                                :class="option.disabled ? 'text-gray-500 italic' : 'text-[#e0f2fe]'">
                                {{ option.label }}
                            </option>
                        </select>
                        <router-link v-if="selectedTask === 'NDVI时序计算'" :to="`/project/${ndviProjectId}`"
                            class="absolute right-6 bg-[#0d1526] cursor-pointer text-white border border-[#2c3e50] rounded-lg px-4 py-2 hover:bg-[#1a2b4c] hover:border-[#2bb2ff] transition-all duration-200 active:scale-95">
                            自定义
                        </router-link>
                        <router-link v-if="selectedTask === '光谱分析'" :to="`/project/${spectrumProjectId}`"
                            class="absolute right-6 bg-[#0d1526] cursor-pointer text-white border border-[#2c3e50] rounded-lg px-4 py-2 hover:bg-[#1a2b4c] hover:border-[#2bb2ff] transition-all duration-200 active:scale-95">
                            自定义
                        </router-link>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div class="config-item">
                                <div class="config-label relative">
                                    <MapIcon :size="16" class="config-icon" />
                                    <span>地图选点</span>
                                    <el-button link @click="startDraw" class="absolute right-1 !text-sky-300">
                                        开始选点
                                    </el-button>
                                </div>
                                <div class="config-control flex-col  gap-2 w-full">
                                    请确定您要研究{{ selectedTask }}的区域：
                                    <div v-if="selectedTask === '光谱分析'" class="flex items-center gap-2 mt-2 w-full">
                                        <label class="text-white">影像选择：</label>
                                        <select v-model="selectedSceneId" @change="showImageBBox"
                                            class="bg-[#0d1526] text-[#38bdf8] border border-[#2c3e50] rounded-lg px-3 py-1 appearance-none hover:border-[#2bb2ff] focus:outline-none focus:border-[#3b82f6] max-w-[calc(100%-90px)] truncate">
                                            <option disabled selected value="">请选择影像</option>
                                            <option v-for="image in props.regionConfig.images" :key="image.sceneName"
                                                :value="image.sceneId" :title="image.sceneName" class="truncate">
                                                {{ image.sceneName }}
                                            </option>
                                        </select>
                                    </div>
                                    <div class="result-info-container">
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <Earth :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">纬度</div>
                                                <div class="result-info-value">{{ pickedPoint[0] }}
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <Earth :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">经度</div>
                                                <div class="result-info-value">{{ pickedPoint[1] }} </div>
                                            </div>
                                        </div>

                                    </div>


                                </div>

                            </div>
                            <div class="result-info-container">
                                <div class="result-info-item">
                                    <div class="result-info-icon">
                                        <CloudIcon :size="16" />
                                    </div>
                                    <div class="result-info-content">
                                        <div class="result-info-label">格网分辨率</div>
                                        <div class="result-info-value">{{ props.regionConfig.space }}km</div>
                                    </div>
                                </div>
                                <div class="result-info-item">
                                    <div class="result-info-icon">
                                        <CalendarIcon :size="16" />
                                    </div>
                                    <div class="result-info-content">
                                        <div class="result-info-label">涵盖时间范围</div>
                                        <div class="result-info-value date-range">
                                            <div class="date-item">{{ formatTime(props.regionConfig.dataRange[0], 'day')
                                            }}~
                                                {{ formatTime(props.regionConfig.dataRange[1], 'day')
                                                }}</div>
                                        </div>
                                    </div>
                                </div>

                                <div class="result-info-item">
                                    <div class="result-info-icon">
                                        <ImageIcon :size="16" />
                                    </div>
                                    <div class="result-info-content">
                                        <div class="result-info-label">研究区影像</div>
                                        <div class="result-info-value">{{ props.regionConfig.images.length }}景 </div>
                                    </div>
                                </div>
                                <div class="result-info-item">
                                    <div class="result-info-icon">
                                        <MapIcon :size="16" />
                                    </div>
                                    <div class="result-info-content">
                                        <div class="result-info-label">影像覆盖率</div>
                                        <div class="result-info-value">{{ props.regionConfig.coverage != 'NaN%'
                                            ? props.regionConfig.coverage
                                            : '待计算' }} </div>
                                    </div>
                                </div>


                            </div>
                            <button @click="selectCal"
                                class="bg-[#0d1526] w-full  cursor-pointer text-white border border-[#247699] rounded-lg px-4 py-2 hover:bg-[#1a2b4c] hover:border-[#2bb2ff] transition-all duration-200 active:scale-95">
                                开始计算
                            </button>
                            <div v-if="showProgress"
                                class="w-full  bg-[#1e293b] rounded-lg overflow-hidden border border-[#2c3e50]">
                                <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                    :style="{ width: `${progress}%` }"></div>
                            </div>

                        </div>
                    </div>
                </section>
                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <ChartColumn :size="18" />
                        </div>
                        <h2 class="section-title">计算结果</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div class="config-item" v-for="(item, index) in drawData" :key="index">
                                <div>第{{ index + 1 }}次计算：{{ item.analysis }}</div>
                                <div v-if="item.imageName">所选影像为：{{ item.imageName }}</div>
                                <!-- <div>NDVI计算结果为：xxx</div> -->
                                <!-- <div>统计数据-统计数据-统计数据-统计数据</div> -->
                                <div>经纬度：（{{ item.point[0] }},{{ item.point[1] }}）</div>

                                <div class="chart-wrapper flex flex-col items-end">
                                    <div class="chart" :ref="el => setChartRef(el, index)" :id="`chart-${index}`"
                                        style="width: 100%; height: 400px;"></div>
                                    <button class="!text-[#38bdf8] cursor-pointer"
                                        @click="fullscreenChart(index)">全屏查看</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>
            </div>

        </dv-border-box12>
    </div>
</template>
<script setup lang="ts">
import { ref, type PropType, computed, type Ref, nextTick, onUpdated, onMounted, reactive, onBeforeUnmount, watch, type ComponentPublicInstance } from 'vue'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import { type interactiveExplore } from '@/components/dataCenter/type'
import { formatTime } from '@/util/common'
import { getNdviPoint, getCaseStatus, getCaseResult, getSpectrum, getBoundaryBySceneId } from '@/api/http/satellite-data'
import * as echarts from 'echarts'
import { getSceneGeojson } from '@/api/http/satellite-data/visualize.api'
import * as MapOperation from '@/util/map/operation'
import { useGridStore, ezStore } from '@/store'
import {
    ChartColumn,
    Earth,
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
import { ElMessage } from 'element-plus'

const props = defineProps({
    regionConfig: {
        type: Object as PropType<interactiveExplore>,
        required: true
    }
})


const gridStore = useGridStore()
const pickedPoint = computed(() => {
    return [
        Math.round(gridStore._point[0] * 1000000) / 1000000,
        Math.round(gridStore._point[1] * 1000000) / 1000000
    ];
})
const showProgress = ref(false)
const progress = ref(0)
const showCalResult = ref(false)
const calTask: Ref<any> = ref({
    calState: 'start',
    taskId: ''
})
const selectedTask = ref('NDVI时序计算')
const optionalTasks = ref([
    { label: 'NDVI时序计算', value: 'NDVI时序计算', disabled: false },
    { label: '光谱分析', value: '光谱分析', disabled: false },
    { label: '滑坡概率计算', value: '滑坡概率计算', disabled: true },
    { label: '洪水频发风险区域计算', value: '洪水频发风险区域计算', disabled: true }
])

let progressTimer: ReturnType<typeof setInterval> | null = null


// 控制进度条
const progressControl = () => {
    if (calTask.value.calState === 'pending') return
    progress.value = 0
    calTask.value.calState = 'pending'
    progressTimer = setInterval(() => {
        if (calTask.value.calState === 'success' || calTask.value.calState === 'failed') {
            progress.value = 100
            showCalResult.value = true
            clearInterval(progressTimer!)
            progressTimer = null
        } else if (progress.value < 95) {
            progress.value += 1
        } else {
            progress.value = 95
        }
    }, 100)
}

const startDraw = () => {
    MapOperation.draw_pointMode()
}

const selectCal = async () => {
    if (selectedTask.value === optionalTasks.value[0].value) {
        await calNDVI()
    } else if (selectedTask.value === optionalTasks.value[1].value) {
        await calSpectrum()
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

const selectedSceneId = ref('')

const calSpectrum = async () => {
    if (pickedPoint.value.length === 0) {
        ElMessage.warning('请先选择您要计算的区域')
        return
    }
    if (selectedSceneId.value === '') {
        ElMessage.warning('请先选择您要计算的影像')
        return
    }
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
    showProgress.value = true
    progressControl()

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
    const selectedImage = props.regionConfig.images.find(image => image.sceneId = selectedSceneId.value)
    console.log(selectedImage);


    try {
        await pollStatus(calTask.value.taskId)
        // ✅ 成功后设置状态
        calTask.value.calState = 'success'
        let res = await getCaseResult(calTask.value.taskId)
        console.log(res, '结果');
        let spectrum = res.data.spectral_profile

        let xData = spectrum.map(data => data.band + '波段')
        let yData = spectrum.map(data => data.value)

        drawData.value.push({
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


const calNDVI = async () => {

    if (pickedPoint.value.length === 0) {
        ElMessage.warning('请先选择您要计算的区域')
        return
    }
    let getNdviPointParam = {
        sceneIds: props.regionConfig.images.map(image => image.sceneId),
        point: [pickedPoint.value[1], pickedPoint.value[0]]
    }
    console.log(getNdviPointParam, '开始计算ndvi');

    let getNdviRes = await getNdviPoint(getNdviPointParam)
    if (getNdviRes.message !== 'success') {
        ElMessage.error('计算失败，请重试')
        console.error(getNdviRes)
        return
    }

    calTask.value.taskId = getNdviRes.data

    // 1、启动进度条
    showProgress.value = true
    progressControl()

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

    try {
        await pollStatus(calTask.value.taskId)
        // ✅ 成功后设置状态
        calTask.value.calState = 'success'
        let res = await getCaseResult(calTask.value.taskId)
        console.log(res, '结果');
        let NDVIData = res.data.NDVI
        let xData = NDVIData.map(data => data.sceneTime)
        let yData = NDVIData.map(data => data.value)

        drawData.value.push({
            yData,
            xData,
            type: 'line',
            analysis: "定点NDVI时序计算",
            point: [...pickedPoint.value]
        })
        ElMessage.success('NDVI计算完成')
    } catch (error) {
        calTask.value.calState = 'failed'
        ElMessage.error('NDVI计算失败，请重试')
        console.error(error);
    }

}

/**
 * 结果展示
 */
const drawData: Ref<any> = ref([])

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

            initChart(el, drawData.value[index], index)
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

// 响应式监听 drawData 变化并重新渲染
watch(drawData, (newData) => {
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
onMounted(() => {

    ndviProjectId.value = ezStore.get('conf').ndviProjectId
    spectrumProjectId.value = ezStore.get('conf').spectrumProjectId
    nextTick(() => {
        drawData.value.forEach((item, index) => {
            const el = document.getElementById(`chart-${index}`)
            if (el) {
                initChart(el, item, index)
            }
        })
    })
})
onBeforeUnmount(() => {
    const map = ezStore.get('map')

    const id = 'UniqueSceneLayer'
    const fillId = `${id}-fill`
    const lineId = `${id}-line`
    const sourceId = `${id}-source`

    if (map.getLayer(fillId)) map.removeLayer(fillId)
    if (map.getLayer(lineId)) map.removeLayer(lineId)
    if (map.getSource(sourceId)) map.removeSource(sourceId)
})

</script>

<style scoped src="./tabStyle.css">
.chart-wrapper {
    margin-top: 10px;
}

.chart {
    width: 100%;
    height: 400px !important;
    border: 1px solid #ddd;
    border-radius: 6px;
}

:fullscreen .chart {
    width: 100vw !important;
    height: 100vh !important;
}

button {
    margin-top: 5px;
}
</style>