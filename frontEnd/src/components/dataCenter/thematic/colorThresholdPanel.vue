<template>
    <!-- Section: 输入模块 -->
    <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <MapIcon :size="18" />
            </div>
            <h2 class="section-title">伪彩色阀值分割</h2>
        </div>
        <div class="section-content">
            <div class="config-container">

                <div class="config-item">
                    <div class="config-label relative flex items-center gap-2">
                        <MapIcon :size="16" class="config-icon" />
                        <span>空间选择</span>
                    </div>
                    <div class="config-control flex-col  gap-2 w-full">
                        <div class="config-item bg-[#0d1526]/50 p-3 rounded-lg">
                            <div class="config-label relative">
                                <MapIcon :size="16" class="config-icon" />
                                <span>{{ t('datapage.optional_thematic.spectrum.wait') }}</span>
                            </div>
                            <!-- <div class="config-control justify-center">
                                <div class="flex items-center gap-2 mt-2 w-full">
                                    <label class="text-white">{{ t('datapage.optional_thematic.spectrum.select') }}</label>
                                    <select v-model="selectedSceneId" @change="showImageBBox"
                                        class="bg-[#0d1526] text-[#38bdf8] border border-[#2c3e50] rounded-lg px-3 py-1 appearance-none hover:border-[#2bb2ff] focus:outline-none focus:border-[#3b82f6] max-w-[calc(100%-90px)] truncate">
                                        <option disabled selected value="">{{ t('datapage.optional_thematic.spectrum.op_select') }}</option>
                                        <option v-for="image in targetImages" :key="image.sceneName"
                                            :value="image.sceneId" :title="image.sceneName" class="truncate">
                                            {{ image.sceneName }}
                                        </option>
                                    </select>
                                </div>
                            </div> -->
                        </div>
                        <button @click=""
                            class="cursor-pointer w-full rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95">
                            {{$t('datapage.optional_thematic.NDVI.button')}}
                        </button>
                        <!-- 请确定您要研究的区域： -->
                        <!-- <div class="flex items-center gap-2 mt-2 w-full">
                            <label class="text-white">影像选择：</label>
                            <select v-model="selectedSceneId" @change="showImageBBox"
                                class="bg-[#0d1526] text-[#38bdf8] border border-[#2c3e50] rounded-lg px-3 py-1 appearance-none hover:border-[#2bb2ff] focus:outline-none focus:border-[#3b82f6] max-w-[calc(100%-90px)] truncate">
                                <option disabled selected value="">请选择影像</option>
                                <option v-for="image in props.regionConfig.images" :key="image.sceneName"
                                    :value="image.sceneId" :title="image.sceneName" class="truncate">
                                    {{ image.sceneName }}
                                </option>
                            </select>
                        </div> -->
                        <!-- <div class="result-info-container">
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <Earth :size="16" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">纬度</div>
                                    <div class="result-info-value">{{ 1 }}
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <Earth :size="16" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">经度</div>
                                    <div class="result-info-value">{{ 1 }} </div>
                                </div>
                            </div>
                        </div> -->
                    </div>

                    <div class="config-label flex items-center gap-2 mt-4">
                        <LayersIcon :size="16" />
                        <span>色彩图例</span>
                    </div>
                    <div class="config-control">
                        <!-- 图例类型选择 -->
                        <el-radio-group v-model="legendType" class="mb-4">
                        <el-radio-button label="continuous">连续渐变</el-radio-button>
                        <el-radio-button label="discrete">离散分级</el-radio-button>
                        </el-radio-group>

                        <!-- 预置色带选择 -->
                        <div class="color-palette-selector">
                            <span>选择色带</span>
                            <select
                                v-model="selectedPalette"
                                class="w-full rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white appearance-none hover:border-[#2bb2ff] hover:bg-[#1a2b4c] focus:outline-none focus:border-[#3b82f6]"
                            >
                                <option disabled value="">选择色带</option>
                                <option
                                v-for="(palette, index) in presetPalettes"
                                :key="index"
                                :value="palette.name"
                                class="bg-[#0d1526] text-white"
                                >
                                {{ palette.name }}
                                </option>
                                <option
                                value="custom"
                                class="bg-[#0d1526] text-white"
                                >
                                自定义
                                </option>
                            </select>
                        </div>
                    </div>

                        <!-- 自定义色带按钮 -->
                    <div class="custom-palette-item mt-3 cursor-pointer border border-dashed border-[#2c3e50] rounded-md p-2 flex flex-col items-center transition-all duration-200 hover:border-[#38bdf8] hover:-translate-y-0.5 " @click="">

                        <span class="palette-name text-xs mt-1 text-center text-[#94a3b8] group-hover:text-white">自定义色带</span>
                    </div>

                </div>
            </div>
        </div>
    </section>

    <!-- Section: 结果展示 -->
    <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <ChartColumn :size="18" />
            </div>
            <h2 class="section-title">{{$t('datapage.optional_thematic.NDVI.result')}}</h2>
        </div>
        <div class="section-content">
            <div v-if="analysisData.length === 0" class="flex justify-center my-6">
                <SquareDashedMousePointer class="mr-2" />{{$t('datapage.optional_thematic.NDVI.noresult')}}
            </div>

            <div class="config-item" v-for="(item, index) in analysisData" :key="index">
                <div class="config-label relative">
                    <ListFilter :size="22" class="config-icon" />
                    <span>第{{ index + 1 }}次计算：{{ item.analysis }}</span>
                    <div class="absolute right-0 cursor-pointer">
                        <ChevronDown v-if="isExpand[index]" :size="22" @click="isExpand[index] = false" />
                        <ChevronUp v-else @click="isExpand[index] = true" :size="22" />
                    </div>
                </div>

                <div v-show="isExpand[index]" >
                    <!-- <div>第{{ index + 1 }}次计算：{{ item.analysis }}</div> -->
                    <!-- <div>NDVI计算结果为：xxx</div> -->
                    <!-- <div>统计数据-统计数据-统计数据-统计数据</div> -->
                    <div>经纬度：（{{ item.point[0] }},{{ item.point[1] }}）</div>

                    <div class="chart-wrapper flex flex-col items-end">
                        <div class="chart" :ref="el => setChartRef(el, index)" :id="`chart-${index}`"
                            style="width: 100%; height: 400px;"></div>
                        <button class="!text-[#38bdf8] cursor-pointer" @click="fullscreenChart(index)">{{$t('datapage.optional_thematic.NDVI.fullview')}}</button>
                    </div>
                </div>
            </div>
        </div>
    </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch, type ComponentPublicInstance, type ComputedRef, type Ref } from 'vue'
import * as MapOperation from '@/util/map/operation'
import { getBoundaryBySceneId, getCaseResult, getCaseStatus, getNdviPoint, getRasterScenesDes } from '@/api/http/satellite-data'
import bus from '@/store/bus'
import mapboxgl from 'mapbox-gl'
import * as echarts from 'echarts'



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
    SquareDashedMousePointer,
    Bus,
    ChevronDown,
    ChevronUp
} from 'lucide-vue-next'
import { useGridStore } from '@/store'
import { mapManager } from '@/util/map/mapManager'

import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
const { t } = useI18n()
const selectedSceneId = ref('')
type ThematicConfig = {
    allImages: any,
    regionId: number,
    startTime: string,
    endTime: string
}
const props = defineProps<{ thematicConfig: ThematicConfig }>()

const dataRange = ref({
    min: 0,
    max: 1
})
const minThreshold = ref(0)
const maxThreshold = ref(1)
const isExpand = ref<boolean[]>([])
const legendType = ref('continuous');
const breakCount = ref(5);
const selectedPalette = ref('viridis');
const presetPalettes = ref([
  {
    name: 'viridis',
    gradient: 'linear-gradient(to right, #440154, #414487, #2a788e, #22a884, #7ad151, #fde725)',
    type: 'scientific',
    colors: ['#440154', '#414487', '#2a788e', '#22a884', '#7ad151', '#fde725']
  },
  {
        name: 'plasma',
        gradient: 'linear-gradient(to right, #0d0887, #6a00a8, #b12a90, #e16462, #fca636, #f0f921)',
        type: 'scientific',
        colors: ['#0d0887', '#6a00a8', '#b12a90', '#e16462', '#fca636', '#f0f921']
    },
{
        name: 'magma',
        gradient: 'linear-gradient(to right, #000004, #1b0b41, #4a0c6b, #781c6d, #b63779, #ed6925, #fbb61a)',
        type: 'scientific',
        colors: ['#000004', '#1b0b41', '#4a0c6b', '#781c6d', '#b63779', '#ed6925', '#fbb61a']
    }
]);

const analysisData = ref<Array<{
    analysis: string
    point: [number, number]
    data: number[]
}>>([])

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
        message.success(t('datapage.optional_thematic.spectrum.message.success_poin'))
    } catch (e) {
        console.error("有错误找后端", e)
        message.error(t('datapage.optional_thematic.spectrum.message.info_fail'))
    }
}

const chartInstances = ref<(echarts.ECharts | null)[]>([])

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
// const selectedImage = props.thematicConfig.allImages.find(image => image.sceneId = selectedSceneId.value)

</script>

<style>
.color-palette-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 12px;
}
.palette-item {
  cursor: pointer;
  border: 1px solid #2c3e50;
  border-radius: 4px;
  overflow: hidden;
}
.palette-preview {
  height: 24px;
  width: 100%;
}
</style>
