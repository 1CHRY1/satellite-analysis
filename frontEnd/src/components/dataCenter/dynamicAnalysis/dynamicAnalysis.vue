<template>
    <div class="relative flex flex-1 h-full flex-row bg-black ">
        <subtitle class="z-10 absolute" style="margin-top: 60px; " />
        <div class="absolute left-16 z-10 h-[calc(100vh-100px)] p-4 text-gray-200"
            :class="isToolbarOpen ? 'w-[545px]' : 'w-16 transition-all duration-300'">
            <button @click="isToolbarOpen = !isToolbarOpen" class="absolute top-1/2 right-0 -translate-y-1/2 h-12 w-6 text-white rounded-l-lg shadow-lg
                 items-center justify-center transition-all z-10"
                :class="isToolbarOpen ? 'bg-blue-600 hover:bg-blue-500' : 'bg-gray-800 hover:bg-gray-700'">
                <ChevronRightIcon :size="16" class="transition-transform duration-300"
                    :class="{ 'transform rotate-180': isToolbarOpen }" />
            </button>
            <div v-if="isToolbarOpen">
                <!--顶部标题+历史记录图标-->
                <section class="panel-section ml-2 mr-2" style="margin-top: 0rem; margin-bottom: 0.5rem;">
                    <div class="section-header">
                        <div class="section-icon">
                            📈
                        </div>
                        <span class="page-title">展示分析</span>
                    </div>
                </section>
                <!-- 内容区域 -->
                <div class="custom-panel px-2">
                    <dv-border-box12 class="!h-[calc(100vh-56px-48px-32px-8px)]">
                        <!--主容器-->
                        <div class="main-container">
                            <!-- 设置部分 -->
                            <section class="panel-section">
                                <!--设置标题-->
                                <div class="section-header">
                                    <div class="section-icon">
                                        <Settings :size="18" />
                                    </div>
                                    <h2 class="section-title">设置</h2>
                                    <div class="absolute right-2 cursor-pointer">
                                        <ChevronDown v-if="isSettingExpand" :size="22"
                                            @click="isSettingExpand = false" />
                                        <ChevronUp v-else @click="isSettingExpand = true" :size="22" />
                                    </div>
                                </div>

                                <!--设置内容区域-->
                                <div v-show="isSettingExpand" class="section-content">
                                    <div class="config-container">
                                        <!-- 空间位置配置 -->
                                        <div class="config-item"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <MapIcon :size="16" class="config-icon" />
                                                <span>{{ t('datapage.analysis.section1.area') }}</span>
                                            </div>
                                            <div class="config-control justify-center">
                                                <RegionSelects v-model="region" class="flex gap-2"
                                                    select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
                                            </div>
                                        </div>

                                        <!-- 数据集配置 -->
                                        <div class="config-item"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <ChartColumn :size="16" class="config-icon" />
                                                <span>数据集</span>
                                            </div>
                                            <div class="config-control">
                                                <button @click="showHistory = !showHistory"
                                                    class="bg-[#0d1526] text-[#38bdf8] border border-[#2c3e50] rounded-lg px-4 py-1 appearance-none hover:border-[#2bb2ff] focus:outline-none focus:border-[#3b82f6] truncate">
                                                    前序数据
                                                </button>
                                                <el-dialog v-model="showHistory"
                                                    class="max-w-[90vw] md:max-w-[80vw] lg:max-w-[70vw] xl:max-w-[60vw]"
                                                    style="background-color: #111827; color: white;">
                                                    <div class="mb-6 text-gray-100">前序数据集</div>

                                                    <div v-if="completedCases.length > 0"
                                                        class="max-h-[500px] overflow-y-auto">
                                                        <div v-for="item in completedCases" :key="item.caseId" class="p-4 mb-3 border border-gray-200 rounded-md
                                                            cursor-pointer transition-all duration-300
                                                            hover:bg-gray-50 hover:shadow-md"
                                                            @click="showResult(item.caseId, item.regionId)">
                                                            <h3 class="mt-0 text-blue-500">{{ item.address }}无云一版图</h3>
                                                            <p class="my-1 text-blue-300">分辨率: {{ item.resolution }}km
                                                            </p>
                                                            <p class="my-1 text-blue-300">创建时间: {{
                                                                formatTimeToText(item.createTime) }}</p>
                                                            <p class="my-1 text-blue-300">数据集: {{ item.dataSet }}</p>
                                                        </div>
                                                    </div>
                                                    <div v-else>
                                                        <p class="item-center text-center text-gray-100">暂无数据</p>
                                                    </div>
                                                </el-dialog>
                                                <div class="absolute right-2 cursor-pointer" @click="clearImages">
                                                    <a-tooltip>
                                                        <template #title>{{ t('datapage.analysis.section2.clear')
                                                            }}</template>
                                                        <Trash2Icon :size="20" />
                                                    </a-tooltip>
                                                </div>
                                            </div>
                                        </div>

                                        <!-- 立方体配置 -->
                                        <div class="config-item"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <BoxIcon :size="16" class="config-icon" />
                                                <span>时序立方体</span>
                                            </div>
                                            <a-alert v-if="exploreData.grids.length === 0"
                                                :message="`已选择${cubeList.filter(item => item.isSelect).length}个时序立方体`"
                                                type="info" show-icon class="status-alert">
                                            </a-alert>
                                            <a-form layout="inline">
                                                <a-form-item class="w-full">
                                                    <a-input v-model:value="inputCacheKey"
                                                        placeholder="键入CacheKey（按Enter以选择）"
                                                        @keyup.enter="handleSelectCube(inputCacheKey)">
                                                        <template #prefix>
                                                            <CommandIcon :size="14"
                                                                style="color: rgba(255, 255, 255, 0.25)" />
                                                        </template>
                                                    </a-input>
                                                </a-form-item>
                                            </a-form>
                                            <div class="max-h-[268px] overflow-y-auto">
                                                <a-modal v-model:open="currentCacheKey" title="时序立方体"
                                                    @ok="() => { cubeList.filter(cube => cube.cacheKey === currentCacheKey)[0].isShow = false; currentCacheKey = undefined }"
                                                    @cancel="() => { cubeList.filter(cube => cube.cacheKey === currentCacheKey)[0].isShow = false; currentCacheKey = undefined }">
                                                    <a-card
                                                        style="max-height: 400px; overflow: auto; position: relative;">
                                                        <pre
                                                            style="white-space: pre-wrap; word-break: break-word; user-select: text;">
                    {{cubeList.filter(cube => cube.cacheKey === currentCacheKey)[0]}}
                </pre>
                                                    </a-card>
                                                </a-modal>
                                                <a-list item-layout="horizontal" class="w-full" :data-source="cubeList">
                                                    <template #renderItem="{ item }">
                                                        <a-list-item>
                                                            <template #actions>
                                                                <div>
                                                                    <Eye v-if="item.isShow" :size="16"
                                                                        class="cursor-pointer"
                                                                        @click="currentCacheKey = undefined; item.isShow = false">
                                                                    </Eye>
                                                                    <EyeOff v-else :size="16" class="cursor-pointer"
                                                                        @click="currentCacheKey = item.cacheKey; item.isShow = true">
                                                                    </EyeOff>
                                                                </div>
                                                                <div>
                                                                    <Square v-if="!item.isSelect" :size="16"
                                                                        class="cursor-pointer"
                                                                        @click="handleSelectCube(item.cacheKey)">
                                                                    </Square>
                                                                    <SquareCheck v-else :size="16"
                                                                        class="cursor-pointer"
                                                                        @click="handleSelectCube(item.cacheKey)">
                                                                    </SquareCheck>
                                                                </div>
                                                            </template>
                                                            <a-list-item-meta
                                                                :description="`${item.dimensionDates.length}维时序立方体, 包含${item.dimensionSensors.length}类传感器, ${item.dimensionScenes.length}景影像`">
                                                                <template #title>
                                                                    {{ formatTimeToText(item.cacheTime) }}
                                                                </template>
                                                                <template #avatar>
                                                                    <div class="section-icon">
                                                                        <BoxIcon :size="14" />
                                                                    </div>
                                                                </template>
                                                            </a-list-item-meta>
                                                        </a-list-item>
                                                    </template>
                                                </a-list>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </section>

                            <!-- 工具目录部分 -->
                            <section class="panel-section">
                                <!--工具目录标题-->
                                <div class="section-header">
                                    <div class="section-icon">
                                        <LayersIcon :size="18" />
                                    </div>
                                    <h2 class="section-title">工具目录</h2>
                                    <div class="absolute right-2 cursor-pointer">
                                        <ChevronDown v-if="isToolsExpand" :size="22" @click="isToolsExpand = false" />
                                        <ChevronUp v-else @click="isToolsExpand = true" :size="22" />
                                    </div>
                                </div>

                                <!--工具目录内容区域-->
                                <div v-show="isToolsExpand" class="section-content">
                                    <div class="config-container">
                                        <div class="config-item"
                                            style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                            <div class="config-label relative">
                                                <SearchIcon :size="16" class="config-icon" />
                                                <span>搜索工具</span>
                                            </div>
                                            <a-input-search v-model:value="searchQuery" placeholder="输入关键词..."
                                                enter-button="搜索" size="large" @search="getCaseList" />
                                            <div class="config-control relative">
                                                <!-- 分类工具列表 -->
                                                <div class="mt-4 w-full mr-4">
                                                    <div v-for="category in filteredCategories" :key="category.name"
                                                        class="mb-4">
                                                        <div class="flex items-center cursor-pointer px-2 py-1 hover:bg-gray-800 rounded"
                                                            @click="toggleCategory(category.name)">
                                                            <ChevronRightIcon :size="16"
                                                                class="mr-2 transition-transform duration-200"
                                                                :class="{ 'transform rotate-90': expandedCategories.includes(category.name) }" />
                                                            <span class="text-gray-300 font-medium">{{ category.name
                                                                }}</span>
                                                        </div>

                                                        <div v-show="expandedCategories.includes(category.name) || searchQuery"
                                                            class="ml-6 mt-2 grid grid-cols-2 gap-2">
                                                            <div v-for="tool in category.tools" :key="tool.value"
                                                                @click="selectedTask = tool.value" :class="{
                                                                    'bg-[#1e3a8a] text-white': selectedTask === tool.value,
                                                                    'bg-[#0d1526] text-gray-300 hover:bg-[#1e293b]': selectedTask !== tool.value && !tool.disabled,
                                                                    'opacity-50 cursor-not-allowed': tool.disabled,
                                                                    'cursor-pointer': !tool.disabled
                                                                }" class="px-3 py-1 rounded-lg transition-colors w-full text-left flex items-center justify-between"
                                                                :disabled="tool.disabled">

                                                                <div class="flex-grow min-w-0" :title="tool.label">
                                                                    <span class="truncate block">{{ tool.label }}</span>
                                                                </div>

                                                                <CircleX v-if="tool.value.startsWith('dynamic:')"
                                                                    :size="16"
                                                                    class="text-gray-400 hover:text-gray-300 flex-shrink-0 ml-1"
                                                                    @click.stop="handleRemoveDynamicTool(tool.value)" />
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="config-control">
                                                <div class="flex justify-between gap-3 items-center w-full">
                                                    <span class="result-info-label">共找到 {{ total }} 条记录</span>
                                                    <div class="flex gap-3">
                                                        <a-button type="primary" class="a-button"
                                                            @click="getCaseList">{{
                                                                t('datapage.history.fliter') }}</a-button>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="config-container">
                                        <div v-for="(item, index) in caseList" class="config-item" :key="item.caseId">
                                            <div class="config-label relative">
                                                <Image :size="16" class="config-icon" />
                                                <span>{{ `${item.address}无云一版图` }}</span>
                                                <div class="absolute right-0 cursor-pointer">
                                                    <a-tooltip>
                                                        <template #title>{{ t('datapage.history.preview') }}</template>
                                                        <Eye v-if="previewList[index]" @click="unPreview" :size="16"
                                                            class="cursor-pointer" />
                                                        <EyeOff v-else :size="16"
                                                            @click="showResult(item.caseId, item.regionId)"
                                                            class="cursor-pointer" />
                                                    </a-tooltip>
                                                </div>
                                            </div>
                                            <div class="config-control flex-col !items-start">
                                                <div class="flex w-full flex-col gap-2">
                                                    <div class="result-info-container">
                                                        <div class="result-info-item">
                                                            <div class="result-info-icon">
                                                                <Grid3x3 :size="12" />
                                                            </div>
                                                            <div class="result-info-content">
                                                                <div class="result-info-label">
                                                                    {{ t('datapage.history.resolution') }}</div>
                                                                <div class="result-info-value">
                                                                    {{ item.resolution }}km
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div class="result-info-item">
                                                            <div class="result-info-icon">
                                                                <CalendarIcon :size="12" />
                                                            </div>
                                                            <div class="result-info-content">
                                                                <div class="result-info-label">
                                                                    {{ t('datapage.history.create_time') }}</div>
                                                                <div class="result-info-value">
                                                                    {{ formatTimeToText(item.createTime) }}
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div class="result-info-item">
                                                            <div class="result-info-icon">
                                                                <DatabaseIcon :size="12" />
                                                            </div>
                                                            <div class="result-info-content">
                                                                <div class="result-info-label">
                                                                    {{ t('datapage.history.data') }}</div>
                                                                <div>
                                                                    {{ item.dataSet }}
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <!-- 下载按钮和发布按钮 -->
                                                    <div class="flex gap-2 mt-2">
                                                        <a-button type="primary" size="middle"
                                                            @click="handlePublish(item)">
                                                            <Upload :size="14" class="mr-1" />
                                                        </a-button>
                                                        <a-button size="middle" @click="handleDownload(item)">
                                                            <Download :size="14" class="mr-1" />
                                                        </a-button>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                        <a-empty v-if="total === 0" />
                                    </div>

                                    <!-- Content-4 Pagination -->
                                    <div class="config-container">
                                        <div class="flex h-[60px] justify-around">
                                            <el-pagination v-if="total > 0" background layout="prev, pager, next"
                                                v-model:current-page="currentPage" :total="total" :page-size="pageSize"
                                                @current-change="getCaseList" @next-click="" @prev-click="">
                                            </el-pagination>
                                        </div>
                                    </div>


                                </div>
                            </section>

                        </div>
                    </dv-border-box12>
                </div>
            </div>
        </div>
        <!-- <ImageSearcher class="h-full w-[28vw] mt-10" /> -->

        <!-- 展示页面 -->
        <div class="absolute right-0 top-0 h-full flex items-center z-10 ">
            <!-- Toggle button -->
            <button @click="showPanel = !showPanel" class="mt-10 h-12 w-6 bg-gray-800 hover:bg-gray-700 text-white rounded-l-lg shadow-lg 
                flex items-center justify-center transition-all z-10" :class="{ '!bg-blue-600': showPanel }">
                <ChevronLeftIcon :size="16" class="transition-transform duration-300"
                    :class="{ 'transform rotate-180': showPanel }" />
            </button>
            <!-- <section class="panel-section ml-2 mr-2" style="margin-top: 0rem; margin-bottom: 0.5rem;">
                    <div class="section-header">
                        <div class="section-icon">
                            📈
                        </div>
                        <span class="page-title">展示分析</span>
                    </div>
                </section> -->
            <div v-if="showPanel">
                <div class="custom-panel px-2 mt-20">
                    <!-- <dv-border-box12 class=" !h-full"> -->
                    <dv-border-box12 class="!h-[calc(100vh-56px-48px-32px-8px)]"
                        style="width:426px; background-color: rgba(20, 20, 21, 0.6);">

                        <component v-if="currentTaskComponent" :is="currentTaskComponent" v-bind="currentTaskProps" />
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
import { ezStore, useExploreStore, useTaskStore, useToolRegistryStore, useUserStore } from '@/store'
import type { RegionValues } from 'v-region'
import { RegionSelects } from 'v-region'
import { getSceneByConfig, getBoundary } from '@/api/http/satellite-data'
import { getRGBTileLayerParamFromSceneObject } from '@/util/visualizeHelper'
import { useViewHistoryModule } from '../noCloud/viewHistory'
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
    ChevronDown,
    ChevronUp,
    SearchIcon,
    ChevronDownIcon,
    Settings,
    BoxIcon,
    Eye,
    EyeOff,
    Circle,
    CircleOff,
    Square,
    SquareCheck,
    CommandIcon,
    CircleX
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { mapManager } from '@/util/map/mapManager'
import { formatTimeToText } from '@/util/common';
import { ElDialog } from 'element-plus'
import { type Case } from '@/api/http/satellite-data'
import subtitle from '../subtitle.vue'
import DynamicServicePanel from '../thematic/DynamicServicePanel.vue'
const exploreData = useExploreStore()
const taskStore = useTaskStore()
const toolRegistry = useToolRegistryStore()
const userStore = useUserStore()

const currentUserId = computed(() => userStore.user?.id ?? '')

watch(
    currentUserId,
    (id) => {
        if (id) {
            toolRegistry.ensureLoaded(id)
        }
    },
    { immediate: true }
)

import { useI18n } from 'vue-i18n'
const { t } = useI18n()

const isToolbarOpen = ref(true)
const isSettingExpand = ref(true)
const isToolsExpand = ref(true)

import MapComp from '@/components/feature/map/mapComp.vue'
import { getCube } from '@/api/http/analytics-display'
import { useCube } from './composables/useCube'
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

const builtinToolCategories = [
    {
        name: '图像',
        tools: [
            { value: '指数分析', label: 'Math and Stats Tools', disabled: false },
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

const dynamicToolCategories = computed(() => {
    const categoryMap = new Map<string, { name: string; tools: Array<{ value: string; label: string; disabled: boolean }> }>()
    toolRegistry.tools.forEach((tool) => {
        const categoryName = tool.category || '自定义'
        if (!categoryMap.has(categoryName)) {
            categoryMap.set(categoryName, {
                name: categoryName,
                tools: [],
            })
        }
        categoryMap.get(categoryName)?.tools.push({
            value: `dynamic:${tool.id}`,
            label: tool.name,
            disabled: false,
        })
    })
    return Array.from(categoryMap.values())
})

const allToolCategories = computed(() => {
    const categoryMap = new Map<string, { name: string; tools: Array<{ value: string; label: string; disabled: boolean }> }>()
    builtinToolCategories.forEach((category) => {
        categoryMap.set(category.name, {
            name: category.name,
            tools: [...category.tools],
        })
    })
    dynamicToolCategories.value.forEach((category) => {
        if (!categoryMap.has(category.name)) {
            categoryMap.set(category.name, {
                name: category.name,
                tools: [],
            })
        }
        categoryMap.get(category.name)?.tools.push(...category.tools)
    })
    return Array.from(categoryMap.values())
})

const selectedTask = ref<string>('')

const fallbackTaskValue = computed(() => {
    const firstCategory = allToolCategories.value.find((category) => category.tools.length > 0)
    return firstCategory?.tools[0]?.value ?? ''
})

watch(
    fallbackTaskValue,
    (value) => {
        if (!selectedTask.value && value) {
            selectedTask.value = value
        }
    },
    { immediate: true }
)

watch(
    () => toolRegistry.tools.map((tool) => tool.id),
    () => {
        if (selectedTask.value.startsWith('dynamic:')) {
            const toolId = selectedTask.value.replace('dynamic:', '')
            if (!toolRegistry.getToolById(toolId)) {
                selectedTask.value = fallbackTaskValue.value
            }
        }
    }
)

const filteredCategories = computed(() => {
    const categories = allToolCategories.value
    if (!searchQuery.value) return categories

    const query = searchQuery.value.toLowerCase()
    return categories
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

// 专题组件映射
const taskComponentMap = {
    '伪彩色分割': defineAsyncComponent(() => import('../thematic/colorThresholdPanel.vue')),
    '指数分析': defineAsyncComponent(() => import('../thematic/indexPanel.vue')),
    'NDVI时序计算': defineAsyncComponent(() => import('../thematic/ndviPanel.vue')),
    '光谱分析': defineAsyncComponent(() => import('../thematic/spectrumPanel.vue')),
    'DSM分析': defineAsyncComponent(() => import('../thematic/dsmPanel.vue')),
    'DEM分析': defineAsyncComponent(() => import('../thematic/demPanel.vue')),
    '红绿立体': defineAsyncComponent(() => import('../thematic/RBbandsPanel.vue')),
    '形变速率': defineAsyncComponent(() => import('../thematic/deformationRate.vue')),
}

const dynamicSelectedTool = computed(() => {
    if (!selectedTask.value.startsWith('dynamic:')) return null
    const toolId = selectedTask.value.replace('dynamic:', '')
    return toolRegistry.getToolById(toolId) ?? null
})

const currentTaskComponent = computed(() => {
    if (dynamicSelectedTool.value) {
        return DynamicServicePanel
    }
    return taskComponentMap[selectedTask.value] || null
})

const thematicConfig = ref({})

const currentTaskProps = computed(() => {
    if (dynamicSelectedTool.value) {
        return {
            thematicConfig: thematicConfig.value,
            toolMeta: dynamicSelectedTool.value,
        }
    }
    return {
        thematicConfig: thematicConfig.value,
    }
})

const selectedResult = ref(null);

const handleResultLoaded = (result) => {
    selectedResult.value = result;
}

const handleRemoveDynamicTool = async (toolValue: string) => {
    const toolId = toolValue.replace('dynamic:', '')
    const toolMeta = toolRegistry.getToolById(toolId)
    if (!toolMeta) return
    try {
        await ElMessageBox.confirm(
            `确定要取消发布工具“${toolMeta.name}”吗？取消发布后工具将被从工具目录中移除。`,
            '取消发布',
            {
                confirmButtonText: '确认',
                cancelButtonText: '保留',
                type: 'warning',
            },
        )
    } catch {
        return
    }
    toolRegistry.removeTool(currentUserId.value, toolId)
    if (selectedTask.value === toolValue) {
        nextTick(() => {
            selectedTask.value = fallbackTaskValue.value
        })
    }
    ElMessage.success('已取消发布工具')
}
// 获取根据行政区选择的原始数据
const originImages = ref([])
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

const { cubeObj, cubeList, inputCacheKey, handleSelectCube, updateGridLayer, currentCacheKey, getCubeObj } = useCube()
onMounted(async () => {
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
    await getCubeObj()
    updateGridLayer(cubeList.value)
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

<style scoped src="../tabStyle.css">
html,
body,
#app {
    margin: 0;
    padding: 0;
    height: 100vh;
    /* 确保根元素占满视口 */
    overflow: hidden;
    /* 防止滚动条导致异常 */
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
