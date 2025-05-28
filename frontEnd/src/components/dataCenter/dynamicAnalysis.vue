<template>
    <div class="custom-panel px-2">
        <dv-border-box12 class="!h-[calc(100vh-56px-48px-32px-8px)]">
            <div class="main-container">
                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <MapPinIcon :size="18" />
                        </div>
                        <h2 class="section-title">研究区域选择</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div class="config-item">
                                <div class="config-label relative">
                                    <MapIcon :size="16" class="config-icon" />
                                    <span>行政区</span>
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
                        <h2 class="section-title">专题选择：</h2>
                        <select v-model="selectedTask" @change="handleThematicChange"
                            class="bg-[#0d1526] text-[#38bdf8] border border-[#2c3e50] rounded-lg px-3 py-1 appearance-none hover:border-[#2bb2ff] focus:outline-none focus:border-[#3b82f6] max-w-[calc(100%-90px)] truncate">
                            <option v-for="option in optionalTasks" :key="option.value" :value="option.value"
                                :disabled="option.disabled">
                                {{ option.label }}
                            </option>
                        </select>
                        <div class="absolute right-6" @click="clearImages">
                            <a-tooltip>
                                <template #title>清空影像图层</template>
                                <Trash2Icon :size="20" />
                            </a-tooltip>
                        </div>

                    </div>
                </section>

                <component :is="currentTaskComponent" :thematicConfig="thematicConfig" />
            </div>

        </dv-border-box12>
    </div>
</template>
<script setup lang="ts">
import { ref, type PropType, computed, type Ref, nextTick, onUpdated, onMounted, reactive, onBeforeUnmount, watch, defineAsyncComponent, type ComponentPublicInstance } from 'vue'
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
    Trash2Icon
} from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { mapManager } from '@/util/map/mapManager'

const startTime = '2001-01-01'
const endTime = '2030-01-01'
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
    { value: 'DSM分析', label: 'DSM分析', disabled: false },
    { value: 'DEM分析', label: 'DEM分析', disabled: false },
    { value: '红绿立体', label: '红绿立体', disabled: false },
    { value: '形变速率', label: '形变速率', disabled: false },
    { value: 'NDVI时序计算', label: 'NDVI时序计算', disabled: false },
    { value: '光谱分析', label: '光谱分析', disabled: false },
]

const selectedTask = ref(optionalTasks[0].value)

// 专题组件映射
const taskComponentMap = {
    'DSM分析': defineAsyncComponent(() => import('./thematic/dsmPanel.vue')),
    'DEM分析': defineAsyncComponent(() => import('./thematic/demPanel.vue')),
    '红绿立体': defineAsyncComponent(() => import('./thematic/RBbands.vue')),
    '形变速率': defineAsyncComponent(() => import('./thematic/deformationRate.vue')),
    'NDVI时序计算': defineAsyncComponent(() => import('./thematic/ndviPanel.vue')),
    '光谱分析': defineAsyncComponent(() => import('./thematic/spectrumPanel.vue')),
}

const currentTaskComponent = computed(() => taskComponentMap[selectedTask.value] || null)


// 获取根据行政区选择的原始数据
const originImages = ref([])
const thematicConfig = ref({})
const getOriginImages = async (newRegion: number | '未选择') => {
    if (newRegion === "未选择") {
        ElMessage.warning('请选择正确的研究区域')
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

    let boundaryRes = await getBoundary(newRegion)
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
onMounted(async () => {
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