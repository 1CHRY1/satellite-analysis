<template>
    <div class="relative flex flex-1 flex-row bg-black">
        <div class="w-[28vw] p-4 text-gray-200 mb-0 gap-0">
            <div class="custom-panel px-2 mb-0">
                <dv-border-box12 class="!h-[calc(100vh-56px-48px-32px-8px)]">
                    <div class="main-container">
                        <section class="panel-section">
                            <div class="section-header">
                                <div class="section-icon">
                                    <MapPinIcon :size="18" />
                                </div>
                                <h2 class="section-title">{{t('datapage.analysis.section1.subtitle')}}</h2>
                            </div>
                            <div class="section-content">
                                <div class="config-container">
                                    <div class="config-item">
                                        <div class="config-label relative">
                                            <MapIcon :size="16" class="config-icon" />
                                            <span>{{t('datapage.analysis.section1.area')}}</span>
                                        </div>
                                        <div class="config-control justify-center">
                                            <RegionSelects v-model="region" class="flex gap-2"
                                                select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </section>
                        <section class="panel-section">
                            <div class="section-header">
                                <div class="section-icon">
                                    <ChartColumn :size="18" />
                                </div>
                                <h2 class="section-title">{{t('datapage.analysis.section2.subtitle')}}</h2>
                                <select v-model="selectedTask" @change="handleThematicChange"
                                    class="bg-[#0d1526] text-[#38bdf8] border border-[#2c3e50] rounded-lg px-3 py-1 appearance-none hover:border-[#2bb2ff] focus:outline-none focus:border-[#3b82f6] max-w-[calc(100%-90px)] truncate">
                                    <option v-for="option in optionalTasks" :key="option.value" :value="option.value"
                                        :disabled="option.disabled">
                                        {{ option.label }}
                                    </option>
                                </select>
                                <!-- <div class="absolute right-6" @click="clearImages">
                                    <a-tooltip>
                                        <template #title>{{t('datapage.analysis.section2.clear')}}</template>
                                        <Trash2Icon :size="20" />
                                    </a-tooltip>
                                </div> -->

                            </div>
                        </section>

                        <component :is="currentTaskComponent" :thematicConfig="thematicConfig" />
                    </div>

                </dv-border-box12>
            </div>
        </div>
        <!-- <ImageSearcher class="h-full w-[28vw] mt-10" /> -->
        <MapComp class="flex-1" :style="'local'" :proj="'globe'" :isPicking="isPicking" />

        <div class="absolute right-0 top-0 h-full flex items-center mt-10 ">
            <!-- Toggle button -->
            <button 
                @click="isToolbarOpen = !isToolbarOpen"
                class="h-12 w-6 bg-gray-800 hover:bg-gray-700 text-white rounded-l-lg shadow-lg flex items-center justify-center transition-all z-10"
                :class="{ '!bg-blue-600': isToolbarOpen }"
            >
                <ChevronLeftIcon 
                    :size="16" 
                    class="transition-transform duration-300"
                    :class="{ 'transform rotate-180': isToolbarOpen }"
                />
            </button>
            <!-- 工具栏内容 -->
            <div v-show="isToolbarOpen" 
                 class="h-full bg-gray-800 shadow-lg transition-all duration-300 overflow-hidden flex flex-col"
                :class="isToolbarOpen ? 'w-64' : 'w-0'">
                <div class="p-4 text-white border-b border-gray-700">
                    <h3 class="font-semibold flex items-center gap-2">
                        <ToolIcon :size="18" />
                        工具目录
                    </h3>
                </div>
                            <!-- Function Title -->
                <div class="flex flex-col  flex-wrap  gap-2 mt-4 ml-6 mr-6">
                        <h3>影像分析</h3>
                        <button 
                            v-for="option in optionalTasks" 
                            :key="option.value"
                            @click="selectedTask = option.value"
                            :class="{
                            'bg-[#1e3a8a] text-white': selectedTask === option.value,
                            'bg-[#0d1526] text-gray-300 hover:bg-[#1e293b]': selectedTask !== option.value,
                            'opacity-50 cursor-not-allowed': option.disabled
                            }"
                            class="px-3 py-1 border border-[#2c3e50] rounded-lg transition-colors"
                            :disabled="option.disabled"
                            >
                                {{ option.label }}
                        </button>
                        <h3>模型分析</h3>

                   <div class="absolute right-6 " @click="clearImages">
                    <a-tooltip>
                        <template #title>{{t('datapage.analysis.section2.clear')}}</template>
                        <Trash2Icon :size="20" />
                    </a-tooltip>
                </div>
                </div>
                
            </div>
        </div>
    </div>
</template>
<script setup lang="ts">
import { ref, type PropType, computed, type Ref, nextTick, onUpdated, onMounted, reactive, onBeforeUnmount, watch, defineAsyncComponent, type ComponentPublicInstance, onUnmounted } from 'vue'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import { type interactiveExplore } from '@/components/dataCenter/type'
import { formatTime } from '@/util/common'
import { getNdviPoint, getCaseStatus, getCaseResult, getSpectrum, getBoundaryBySceneId, getRegionPosition, getRasterScenesDes } from '@/api/http/satellite-data'
import * as echarts from 'echarts'
import { getSceneGeojson } from '@/api/http/satellite-data/visualize.api'
import * as MapOperation from '@/util/map/operation'
import { useGridStore, ezStore } from '@/store'
import type { RegionValues } from 'v-region'
import { RegionSelects } from 'v-region'
import { getSceneByConfig, getBoundary } from '@/api/http/satellite-data'
import { getRGBTileLayerParamFromSceneObject } from '@/util/visualizeHelper'
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
    Trash2Icon,
    ChevronLeftIcon,
    ChevronRight
} from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { mapManager } from '@/util/map/mapManager'
import { useExploreStore } from '@/store'
const exploreData = useExploreStore()

import { useI18n } from 'vue-i18n'
const { t } = useI18n()

const isToolbarOpen = ref(false)

import MapComp from '@/components/feature/map/mapComp.vue'
const isPicking = ref(false)

const startTime = '1900-01-01'
const endTime = '2050-01-01'
const region = ref<RegionValues>({
    province: '370000',
    city: '370100',
    area: '',
})
const displayLabel = computed(() => {
    let info = region.value
    if (info.area) return Number(`${info.area}`)
    if (info.city) return Number(`${info.city}`)
    if (info.province) return Number(`${info.province}`)
    return '未选择'
})

const optionalTasks = [
    { value: 'NDVI时序计算', label: t('datapage.analysis.optionallab.task_NDVI'), disabled: false },
    { value: '光谱分析', label: t('datapage.analysis.optionallab.task_spectral'), disabled: false },
    { value: '伪彩色分割', label: '伪彩色分割', disabled: false },
    { value: '指数分析', label: '指数分析', disabled: false },
    { value: '空间分析', label: '空间分析', disabled: false },
]


const selectedTask = ref(optionalTasks[0].value)

// 专题组件映射
const taskComponentMap = {
    '伪彩色分割': defineAsyncComponent(() => import('./thematic/colorThresholdPanel.vue')),
    '指数分析': defineAsyncComponent(() => import('./thematic/indexPanel.vue')),
    'NDVI时序计算': defineAsyncComponent(() => import('./thematic/ndviPanel.vue')),
    '光谱分析': defineAsyncComponent(() => import('./thematic/spectrumPanel.vue')),
}

const currentTaskComponent = computed(() => taskComponentMap[selectedTask.value] || null)


// 获取根据行政区选择的原始数据
const originImages = ref([])
const thematicConfig = ref({})
const getOriginImages = async (newRegion: number | '未选择') => {
    if (newRegion === "未选择") {
        ElMessage.warning(t('datapage.analysis.message.region'))
        return
    }
    let filterData = {
        startTime,
        endTime,
        cloud: 100,
        regionId: newRegion,
    }
    originImages.value = await getSceneByConfig(filterData)

    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    let boundaryRes = await exploreData.boundary
    let window = await getRegionPosition(newRegion)

    // 先清除现有的矢量边界，然后再添加新的  
    MapOperation.map_addPolygonLayer({
        geoJson: boundaryRes,
        id: 'UniqueLayer',
        lineColor: '#8fffff',
        fillColor: '#a4ffff',
        fillOpacity: 0.2,
    })
    // fly to
    MapOperation.map_fitView([
        [window.bounds[0], window.bounds[1]],
        [window.bounds[2], window.bounds[3]],
    ])
    thematicConfig.value = {
        allImages: originImages.value,
        regionId: displayLabel.value,
        endTime,
        startTime
    }
}

const handleThematicChange = async () => {
    // if (selectedTask.value === '红绿立体') {
    //     // const stopLoading = message.loading('正在加载影像', 0)
    //     let rasterParam = {
    //         startTime,
    //         endTime,
    //         regionId: displayLabel.value,
    //         dataType: '3d'
    //     }
    //     const sceneObject = await getRasterScenesDes(rasterParam)
    //     console.log(sceneObject, '红绿立体');
    //     const rgbLayerParam = await getRGBTileLayerParamFromSceneObject(sceneObject)
    //     MapOperation.map_addRGBImageTileLayer(rgbLayerParam)
    // }



}

const clearImages = () => {
    MapOperation.map_destroyTerrain()
    MapOperation.map_destroyRGBImageTileLayer()
    MapOperation.map_destroyOneBandColorLayer()
    mapManager.withMap((map) => {
        if (map.getLayer('UniqueSceneLayer-fill')) map.removeLayer('UniqueSceneLayer-fill')
        if (map.getLayer('UniqueSceneLayer-line')) map.removeLayer('UniqueSceneLayer-line')
        if (map.getSource('UniqueSceneLayer-source')) map.removeSource('UniqueSceneLayer-source')
    })
}



watch(displayLabel, getOriginImages, { immediate: true })

const addLocalInternalLayer = () => {
    mapManager.withMap((map) => {
        const sourceId = 'Local-Interal-Source'
        const layerId = 'Local-Interal-Layer'

        // 防止重复添加
        if (map.getLayer(layerId)) {
            map.removeLayer(layerId)
        }
        if (map.getSource(sourceId)) {
            map.removeSource(sourceId)
        }

        // 添加 source
        map.addSource(sourceId, {
            type: 'raster',
            tiles: [
                `http://${window.location.host}${ezStore.get('conf')['fk_url']}`
            ],
            tileSize: 256,
        })

        // 添加 layer
        map.addLayer({
            id: layerId,
            type: 'raster',
            source: sourceId,
        })
    })
}
onMounted(async () => {
    addLocalInternalLayer()
})
onUnmounted(() => {
    mapManager.withMap((map) => {
        const sourceId = 'Local-Interal-Source'
        const layerId = 'Local-Interal-Layer'

        if (map.getLayer(layerId)) {
            map.removeLayer(layerId)
        }
        if (map.getSource(sourceId)) {
            map.removeSource(sourceId)
        }
    })
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