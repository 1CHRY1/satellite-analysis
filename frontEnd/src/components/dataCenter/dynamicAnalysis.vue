<template>
    <div class="relative flex flex-1 h-full flex-row bg-black ">
        <subtitle class="z-10 absolute" style="margin-top: 60px; "/>
        <div class="absolute  left-16  top-0  p-4 text-gray-200 z-10 h-[calc(100vh-300px)] " :class="isToolbarOpen? 'w-[20vw]' : 'w-16'" style="margin-top: 100px; ">
            <button 
                @click="isToolbarOpen = !isToolbarOpen"
                class="absolute top-1/2 right-0 -translate-y-1/2 h-12 w-6 bg-gray-800 hover:bg-gray-700 text-white rounded-r-lg shadow-lg 
                 items-center justify-center transition-all z-10"
                :class="{ '!bg-blue-600': isToolbarOpen }"
            >
                <ChevronRightIcon 
                    :size="16" 
                    class="transition-transform duration-300"
                    :class="{ 'transform rotate-180': isToolbarOpen }"
                />
            </button>
                          
            <div v-show="isToolbarOpen" 
                 class="h-full
                 "
                 style="background-color: rgba(20, 20, 21, 0.6);"
                :class="isToolbarOpen ? 'w-[20vw]' : 'w-16'">

                <dv-border-box12 class="h-full overflow-y-auto box-border " style="height: calc(100vh-300px);"> 
                <!-- 工具栏内容 -->
                    <div class="p-4 text-white border-b border-gray-700">
                        <h3 class="font-semibold flex items-center gap-2">
                            <ToolIcon :size="18" />
                            设置
                        </h3>
                    </div>
                    <div>
                        <section class="panel-section">
                            <div class="section-header" style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                <div class="section-icon">
                                    <MapPinIcon :size="18" />
                                </div>
                                <h2 class="section-title">{{t('datapage.analysis.section1.subtitle')}}</h2>
                            </div>
                            <div class="section-content">
                                <div class="config-container" style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
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
                            <div class="section-header" style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                <div class="section-icon">
                                    <ChartColumn :size="18" />
                                </div>
                                <h2 class="section-title mr-6">数据集  </h2>
                                <div class="ml-4">
                                    <button @click="showHistory = !showHistory"
                                    class=" bg-[#0d1526] text-[#38bdf8] border border-[#2c3e50] rounded-lg px-4 py-1 appearance-none hover:border-[#2bb2ff] focus:outline-none focus:border-[#3b82f6] truncate"
                                    >
                                    前序数据
                                    </button>
                                    <el-dialog v-model="showHistory"
                                                class="max-w-[90vw] md:max-w-[80vw] lg:max-w-[70vw] xl:max-w-[60vw]
                                                "
                                                style="background-color: #111827; color: white;">
                                        <div class="mb-6 text-gray-100">前序数据集</div>
                                        
                                        <div v-if="completedCases.length > 0" class="max-h-[500px] overflow-y-auto">
                                            <div v-for="item in completedCases" 
                                            :key="item.caseId" 
                                            class="p-4 mb-3 border border-gray-200 rounded-md 
                                                    cursor-pointer transition-all duration-300
                                                    hover:bg-gray-50 hover:shadow-md"
                                            @click="showResult(item.caseId, item.regionId)">
                                                <h3 class="mt-0 text-blue-500">{{ item.address }}无云一版图</h3>
                                                <p class="my-1 text-blue-300">分辨率: {{ item.resolution }}km</p>
                                                <p class="my-1 text-blue-300">创建时间: {{ formatTimeToText(item.createTime) }}</p>
                                                <p class="my-1 text-blue-300">数据集: {{ item.dataSet }}</p>
                                            </div>
                                        </div>
                                        <div v-else>
                                            <p class="item-center text-center text-gray-100">暂无数据</p>
                                        </div>
                                    </el-dialog>
                                </div>
                                <!-- <h2 class="section-title">{{t('datapage.analysis.section2.subtitle')}}</h2>
                                <select v-model="selectedTask" @change="handleThematicChange"
                                    class="bg-[#0d1526] text-[#38bdf8] border border-[#2c3e50] rounded-lg px-3 py-1 appearance-none hover:border-[#2bb2ff] focus:outline-none focus:border-[#3b82f6] max-w-[calc(100%-90px)] truncate">
                                    <option v-for="option in optionalTasks" :key="option.value" :value="option.value"
                                        :disabled="option.disabled">
                                        {{ option.label }}
                                    </option>
                                </select> -->
                                <div class="absolute right-6" @click="clearImages">
                                    <a-tooltip>
                                        <template #title>{{t('datapage.analysis.section2.clear')}}</template>
                                        <Trash2Icon :size="20" />
                                    </a-tooltip>
                                </div>
                            </div>
                        </section>
                    </div>

                                <!-- Function Title -->
                    <div class="flex flex-col flex-1  mt-4 ml-6 mr-6 max-h-[calc((100vh-140px)/2)]"  >
                                <!-- <h3>影像分析</h3>
                                <div class="absolute right-6 " @click="clearImages">
                                    <a-tooltip>
                                        <template #title>{{t('datapage.analysis.section2.clear')}}</template>
                                        <Trash2Icon :size="20" />
                                    </a-tooltip>
                                </div>
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
                                </button> -->
                                <!-- 数据集 -->
                                
                            <h3>目录</h3>
                            <div class="mt-2 relative mb-2">
                                <input
                                    v-model="searchQuery"
                                    placeholder="搜索工具..."
                                    class="w-full bg-gray-700 text-white px-3 py-1 rounded border border-gray-600 
                                    focus:outline-none focus:border-blue-500"
                                />
                                <SearchIcon :size="16" class="absolute right-3 top-2 text-gray-400" />
                            </div>
                            
                            
                            <!-- 分类工具列表 -->
                            <div class=" p-2 mb-6 min-h-0 overflow-y-auto" style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                <div v-for="category in filteredCategories" :key="category.name" class="mb-4">
                                    <h3 class="text-gray-300 font-medium px-2 py-1 flex items-center flex-1">
                                        <ChevronDownIcon 
                                            :size="16" 
                                            class="mr-1 transition-transform duration-200"
                                            :class="{ 'transform rotate-180': expandedCategories.includes(category.name) }"
                                            @click="toggleCategory(category.name)"
                                        />
                                        {{ category.name }}
                                    </h3>
                                    
                                    <div 
                                        v-show="expandedCategories.includes(category.name) || searchQuery"
                                        class="ml-6 mt-1 space-y-1 flex-1"
                                    >
                                        <div
                                            v-for="tool in category.tools" 
                                            :key="tool.value"
                                            
                                            :class="{
                                                'bg-[#1e3a8a] text-white': selectedTask === tool.value,
                                                // 'bg-[#0d1526] text-gray-300 hover:bg-[#1e293b]': selectedTask !== tool.value,
                                                'opacity-50 cursor-not-allowed': tool.disabled
                                            }"
                                            class="px-3 py-1  rounded-lg transition-colors w-full text-left truncate"
                                            :disabled="tool.disabled"
                                            @click="selectedTask = tool.value"
                                        >
                                            {{ tool.label }}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                </dv-border-box12>
            </div>
        </div>
        <!-- <ImageSearcher class="h-full w-[28vw] mt-10" /> -->
        
        <!-- 展示页面 -->
        <div class="absolute right-0 top-0 h-full flex items-center z-10 " >
            <!-- Toggle button -->
            <button 
                @click="showPanel = !showPanel"
                class="mt-10 h-12 w-6 bg-gray-800 hover:bg-gray-700 text-white rounded-l-lg shadow-lg 
                flex items-center justify-center transition-all z-10"
                :class="{ '!bg-blue-600': showPanel }"
            >
                <ChevronLeftIcon 
                    :size="16" 
                    class="transition-transform duration-300"
                    :class="{ 'transform rotate-180': showPanel }"
                />
            </button>
            <!-- <section class="panel-section ml-2 mr-2" style="margin-top: 0rem; margin-bottom: 0.5rem;">
                    <div class="section-header">
                        <div class="section-icon">
                            📈
                        </div>
                        <span class="page-title">展示分析</span>
                    </div>
                </section> -->
            <div v-if="showPanel" >
                <div class="custom-panel px-2 mt-20">
                    <!-- <dv-border-box12 class=" !h-full"> -->
                    <dv-border-box12 style="width: 426px; height: 1189px; background-color: rgba(20, 20, 21, 0.6);">
                        
                            <component :is="currentTaskComponent" :thematicConfig="thematicConfig" />
                            <ResultComponent @response="handleResultLoaded" />
                        
                    </dv-border-box12>
                </div>
            </div>
        </div>
        <MapComp class="!flex-1" :style="'local'" :proj="'globe'" :isPicking="isPicking" />
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
import { useViewHistoryModule } from './noCloud/viewHistory'
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
    ChevronRightIcon,
    ChevronRight,
    SearchIcon,
    ChevronDownIcon 
} from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { mapManager } from '@/util/map/mapManager'
import { formatTimeToText } from '@/util/common'; 
import { ElDialog } from 'element-plus'
import { type Case } from '@/api/http/satellite-data'
import subtitle from './subtitle.vue'
import { useExploreStore } from '@/store'
import { useTaskStore } from '@/store'
const exploreData = useExploreStore()
const taskStore = useTaskStore()

import { useI18n } from 'vue-i18n'
const { t } = useI18n()

const isToolbarOpen = ref(true)

import MapComp from '@/components/feature/map/mapComp.vue'
const isPicking = ref(false)

//左模块显示
const showPanel = ref(false)

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

//工具目录
const searchQuery = ref('')
const expandedCategories = ref<string[]>(['图像', '影像集合', '要素集合'])

const toolCategories = [
    {
        name: '图像',
        tools: [
            { value: '指数分析', label: '指数分析', disabled: false },
            { value: 'NDVI时序计算', label: t('datapage.analysis.optionallab.task_NDVI'), disabled: false },
            { value: '光谱分析', label: t('datapage.analysis.optionallab.task_spectral'), disabled: false },
            { value: 'DSM分析', label: t('datapage.analysis.optionallab.task_DSM'), disabled: false },
            { value: 'DEM分析', label: t('datapage.analysis.optionallab.task_DEM'), disabled: false },
            { value: '红绿立体', label: t('datapage.analysis.optionallab.task_red_green'), disabled: false },
            { value: '形变速率', label: t('datapage.analysis.optionallab.task_rate'), disabled: false },
            { value: '伪彩色分割', label: '伪彩色分割', disabled: false },
            { value: '空间分析', label: '空间分析', disabled: false },
            { value: 'boxSelect', label: '归一化差异', disabled: false },
            { value: 'hillShade', label: '地形渲染', disabled: false },
            { value: 'landcoverClean', label: '地表覆盖数据清洗', disabled: false },
            { value: 'ReduceReg', label: '区域归约', disabled: false },
            { value: 'PixelArea', label: '像元面积', disabled: false },
            { value: 'PixelLonLat', label: '像元经纬度坐标', disabled: false }
        ]
    },
    {
        name: '影像集合',
        tools: [
            { value: 'Clipped Composite', label: '裁剪合成影像', disabled: false },
            { value: 'Filtered Composite', label: '滤波合成影像', disabled: false },
            { value: 'Linear Fit', label: '线性拟合', disabled: false },
            { value: 'Simple Cloud Score', label: '简易云量评分', disabled: false }
        ]
    },
    {
        name: '要素集合',
        tools: [
            { value: 'Buffer', label: '缓冲区分析', disabled: false },
            { value: 'Distance', label: '距离计算', disabled: false },
            { value: 'Join', label: '空间连接', disabled: false },
            { value: 'Computed Area Filter', label: '基于计算面积的筛选', disabled: false }
        ]
    },
]

const filteredCategories = computed(() => {
    if (!searchQuery.value) return toolCategories
    
    const query = searchQuery.value.toLowerCase()
    return toolCategories
        .map(category => ({
            ...category,
            tools: category.tools.filter(tool => 
                tool.label.toLowerCase().includes(query) || 
                category.name.toLowerCase().includes(query)
            )
        }))
        .filter(category => category.tools.length > 0)
})

const toggleCategory = (categoryName: string) => {
    const index = expandedCategories.value.indexOf(categoryName)
    if (index >= 0) {
        expandedCategories.value.splice(index, 1)
    } else {
        expandedCategories.value.push(categoryName)
    }
}

// const optionalTasks = [
//     { value: 'NDVI时序计算', label: t('datapage.analysis.optionallab.task_NDVI'), disabled: false },
//     { value: '光谱分析', label: t('datapage.analysis.optionallab.task_spectral'), disabled: false },
//     { value: 'DSM分析', label: t('datapage.analysis.optionallab.task_DSM'), disabled: false },
//     { value: 'DEM分析', label: t('datapage.analysis.optionallab.task_DEM'), disabled: false },
//     { value: '红绿立体', label: t('datapage.analysis.optionallab.task_red_green'), disabled: false },
//     { value: '形变速率', label: t('datapage.analysis.optionallab.task_rate'), disabled: false },
//     { value: '伪彩色分割', label: '伪彩色分割', disabled: false },
//     { value: '指数分析', label: '指数分析', disabled: false },
//     { value: '空间分析', label: '空间分析', disabled: false },
// ]


const selectedTask = ref(toolCategories[0].tools[0].value)

// 专题组件映射
const taskComponentMap = {
    '伪彩色分割': defineAsyncComponent(() => import('./thematic/colorThresholdPanel.vue')),
    '指数分析': defineAsyncComponent(() => import('./thematic/indexPanel.vue')),
    'NDVI时序计算': defineAsyncComponent(() => import('./thematic/ndviPanel.vue')),
    '光谱分析': defineAsyncComponent(() => import('./thematic/spectrumPanel.vue')),
    'DSM分析': defineAsyncComponent(() => import('./thematic/dsmPanel.vue')),
    'DEM分析': defineAsyncComponent(() => import('./thematic/demPanel.vue')),
    '红绿立体': defineAsyncComponent(() => import('./thematic/RBbandsPanel.vue')),
    '形变速率': defineAsyncComponent(() => import('./thematic/deformationRate.vue')),
}

const currentTaskComponent = computed(() => taskComponentMap[selectedTask.value] || null)

const selectedResult = ref(null);

const handleResultLoaded = (result) => {
  selectedResult.value = result;
}
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
        startTime,
        dataset: selectedResult.value
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


// 数据集
const historyComponent = ref(null)
const showHistory = ref(false)
interface Case {
        caseId: string,
        address: string,
        regionId: number,
        resolution: string,
        sceneList: Array<string>,
        dataSet: string,
        status: string,
        result: {
            bucket: string,
            object_path: string
        },
        createTime: string
    }

const { 
  caseList, 
  currentPage, 
  pageSize, 
  total, 
  getCaseList,
  activeTab,
  handleSelectTab,
  showResult,
  onResultSelected
} = useViewHistoryModule();

const completedCases = ref<any[]>([]); // 仅存储已完成的任务
const isLoading = ref(false);

// 加载已完成任务
const loadCompletedCases = async () => {
  isLoading.value = true;
  activeTab.value = 'COMPLETE';
  

  await getCaseList();
  
  completedCases.value = caseList.value;
  
  isLoading.value = false;
};

// 效果测试
const mockCompletedCases = ref([
    {
    caseId: 'mock_003',
    address: '广州市天河区',
    resolution: '10',
    createTime: '2023-10-17 16:45:33',
    dataSet: 'MODIS'
  },
  {
    caseId: 'mock_002',
    address: '上海市浦东新区',
    resolution: '5',
    createTime: '2023-10-16 09:15:47',
    dataSet: 'Landsat-8'
  },
])


onMounted(() => {
    
    // 设置结果选择的回调
    onResultSelected.value = (result) => {
        selectedResult.value = result
        // 立即更新 thematicConfig
        thematicConfig.value = {
            ...thematicConfig.value,
            dataset: result.data  // 注意这里用 result.data
        }
        // 关闭弹窗
        showHistory.value = false
        ElMessage.success('已选择数据集')
    }
    
    loadCompletedCases();
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
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100vh; /* 确保根元素占满视口 */
  overflow: hidden; /* 防止滚动条导致异常 */
  overscroll-behavior-y: none; 
}

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