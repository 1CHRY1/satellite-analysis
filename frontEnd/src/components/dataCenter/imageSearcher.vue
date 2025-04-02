<template>
    <div class="remote-sensing-panel select-none">
        <dv-border-box12 v-if="UIStage === 'filter-stage'">
            <div class="main-container">
                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <DatabaseIcon :size="18" />
                        </div>
                        <h2 class="section-title">影像数据集</h2>
                    </div>
                    <div class="upload-area" @click="handleDatasetSelect">
                        <div class="upload-t" v-if="!selectedProduct">
                            <UploadCloudIcon :size="18" class="upload-icon" />
                            <span class="upload-text">点击选择数据集</span>
                        </div>

                        <div v-else class="selected-product-info">
                            <div class="product-header">
                                <DatabaseIcon :size="16" class="product-icon" />
                                <span class="product-name">{{ selectedProduct.name }}</span>
                            </div>
                            <div class="product-tags">
                                <span class="product-tag">
                                    <ApertureIcon :size="14" class="tag-icon" />
                                    分辨率: {{ selectedProduct.resolution }}
                                </span>
                                <span class="product-tag">
                                    <ClockIcon :size="14" class="tag-icon" />
                                    周期: {{ selectedProduct.period }}
                                </span>
                            </div>
                        </div>

                        <DataSetModal v-model="datasetModalOpen" @update:selected-product-id="handleSelectedProduct" />
                    </div>
                </section>

                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <MapPinIcon :size="18" />
                        </div>
                        <h2 class="section-title">空间位置</h2>
                    </div>
                    <div class="section-content">
                        <a-tabs v-model:activeKey="spatialTabsKey" type="card" class="custom-tabs">
                            <a-tab-pane key="1" tab="地图绘制">
                                <div class="tab-content">
                                    <div class="button-group">
                                        <a-button class="custom-button" @click="handleDrawPoint">
                                            <MapPinIcon :size="16" class="button-icon" />
                                            <span>点</span>
                                        </a-button>
                                        <a-divider type="vertical" class="divider" />
                                        <a-button class="custom-button" @click="handleDrawPolygon">
                                            <HexagonIcon :size="16" class="button-icon" />
                                            <span>多边形</span>
                                        </a-button>
                                    </div>
                                </div>
                            </a-tab-pane>
                            <a-tab-pane key="2" tab="文件上传" force-render>
                                <div class="tab-content">
                                    <div class="upload-area !my-0 !h-[50px] !w-full !flex-row"
                                        @click="handleGeojsonUpload">
                                        <UploadCloudIcon :size="18" class="upload-icon !mr-2 !mb-1" />
                                        <span class="upload-text">点击上传矢量文件</span>
                                    </div>
                                </div>
                            </a-tab-pane>
                            <a-tab-pane key="3" tab="行政区检索">
                                <div class="tab-content">
                                    <a-cascader class="custom-cascader" v-model:value="selectedDistrict"
                                        :allow-clear="false" :options="districts" placeholder="请选择行政区域" />
                                </div>
                            </a-tab-pane>
                        </a-tabs>
                    </div>
                </section>

                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <CalendarIcon :size="18" />
                        </div>
                        <h2 class="section-title">时间范围</h2>
                    </div>
                    <div class="section-content">
                        <a-range-picker class="custom-date-picker" v-model:value="dateRangeValue" picker="day"
                            :allow-clear="false" :placeholder="['开始日期', '结束日期']" />
                    </div>
                </section>

                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <CloudIcon :size="18" />
                        </div>
                        <h2 class="section-title">云量筛选</h2>
                    </div>
                    <div class="section-content">
                        <div class="px-10">
                            <a-slider class="custom-slider" range :cloudMarks="cloudMarks" v-model:value="cloudRange"
                                :tipFormatter="(value: number) => value + '%'" />
                        </div>
                        <div class="mt-2 flex justify-around">
                            <div class="flex items-center px-5">
                                <span class="rounded-md bg-sky-400/20 px-2 py-1 text-sky-400">最小云量</span>
                                <span class="ml-2 w-4 text-white">{{ cloudRange[0] }}%</span>
                            </div>
                            <div class="flex items-center px-5">
                                <span class="rounded-md bg-sky-400/20 px-2 py-1 text-sky-400">最大云量</span>
                                <span class="ml-2 w-4 text-white">{{ cloudRange[1] }}%</span>
                            </div>
                        </div>
                    </div>
                </section>

                <div class="panel-footer">
                    <a-button type="primary" class="search-button" @click="handleImageFilterSearch">
                        <SearchIcon :size="18" class="button-icon" />
                        <span>开始检索</span>
                    </a-button>
                    <a-button class="reset-button" @click="handleFilterStageReset">
                        <RefreshCwIcon :size="18" class="button-icon" />
                        <span>重置</span>
                    </a-button>
                </div>
            </div>
        </dv-border-box12>

        <dv-border-box12 v-else-if="UIStage === 'tile-stage'">
            <div class="main-container">
                <!-- Image List Section with Row Layout -->
                <section class="panel-section">
                    <div class="section-header relative">
                        <div class="section-icon">
                            <ImageIcon :size="18" />
                        </div>
                        <h2 class="section-title">影像列表</h2>
                        <div class="section-actions right-4">
                            <a-tooltip title="刷新列表">
                                <a-button type="text" size="small" @click="handleImageFilterSearch">
                                    <RefreshCwIcon :size="16" :color="'#38bdf8'" />
                                </a-button>
                            </a-tooltip>
                        </div>
                        <div class="section-actions right-12">
                            <a-tooltip title="返回">
                                <a-button type="text" size="small" @click="handleBackToFilterStage">
                                    <ArrowLeftFromLineIcon :size="16" :color="'#38bdf8'" />
                                </a-button>
                            </a-tooltip>
                        </div>
                    </div>

                    <div class="section-content">
                        <div class="my-3">
                            <a-input-search v-model:value="searchText" placeholder="搜索影像" style="width: 100%"
                                @search="handleTextSearch" allow-clear />
                        </div>

                        <a-spin :spinning="searchResultLoading">
                            <div class="image-list-row">
                                <div v-if="filteredImages.length === 0" class="empty-state">
                                    <FileSearchIcon :size="32" class="empty-icon" />
                                    <p>没有找到符合条件的影像</p>
                                </div>

                                <div v-else v-for="image in filteredImages" :key="image.id" class="image-row-item"
                                    :class="{ selected: selectedImage?.id === image.id }"
                                    @click="toggleImageSelection(image.id)">
                                    <div class="image-row-preview">
                                        <img :src="image.preview_url" :alt="image.name" class="row-thumbnail" />
                                    </div>
                                    <div class="image-row-details">
                                        <h4 class="image-row-name">{{ image.name }}</h4>
                                        <div class="image-row-meta">
                                            <span class="image-row-date">
                                                <CalendarIcon :size="12" />
                                                {{ formatDate(image.date) }}
                                            </span>
                                            <span class="image-row-resolution">
                                                <MaximizeIcon :size="12" />
                                                {{ image.resolution }}
                                            </span>
                                            <span class="image-row-cloud">
                                                <CloudIcon :size="12" />
                                                {{ image.cloudCover }}%
                                            </span>
                                        </div>
                                    </div>
                                    <div class="image-row-actions">
                                        <a-radio :checked="selectedImage?.id === image.id" @click.stop
                                            @change="toggleImageSelection(image.id)" />
                                    </div>
                                </div>
                            </div>
                        </a-spin>

                        <div class="selection-summary">
                            <a-button type="link" @click="clearSelection">清除选择</a-button>
                            <a-button type="primary" @click="confirmSelection" :disabled="selectedImage === undefined">
                                确认选择 </a-button>
                        </div>
                    </div>
                </section>

                <!-- Band Selection Section (Modified to work without grid selection) -->
                <section class="panel-section" v-if="bandViews.length > 0">
                    <div class="section-header">
                        <div class="section-icon">
                            <LayersIcon :size="18" />
                        </div>
                        <h2 class="section-title">波段选择</h2>
                    </div>

                    <div class="section-content band-selection">
                        <a-radio-group v-model:value="selectedBand" button-style="solid" class="band-radio-group">
                            <a-radio-button v-for="band in bandViews" :key="band.id" :value="band.id"
                                class="band-radio-button">
                                {{ band.name }}
                            </a-radio-button>
                        </a-radio-group>
                    </div>
                </section>

                <!-- Selected Images Summary Section (New) -->
                <section class="panel-section" v-if="selectedImage && selectedBand">
                    <div class="section-header">
                        <div class="section-icon">
                            <ListIcon :size="18" />
                        </div>
                        <h2 class="section-title">切片选择</h2>
                    </div>

                    <div class="section-content">
                        <div class="block-area" v-if="selectedGridIDs.length === 0">
                            <MousePointerClickIcon :size="20" class="block-icon" />
                            <span class="block-text">点击地图以选择切片</span>
                        </div>
                        <div class="block-area" v-else>
                            <a-tag v-for="gridID in selectedGridIDs" :key="gridID" closable
                                @close="removeOneGrid(gridID)" class="product-tag">
                                {{ gridID }}
                            </a-tag>
                        </div>
                    </div>
                </section>

                <!-- Download Section (Modified to work without grid selection) -->
                <div class="panel-footer">
                    <a-button class="reset-button" @click="handleTileStageReset">
                        <RefreshCwIcon :size="18" class="button-icon" />
                        <span>重置</span>
                    </a-button>
                    <a-button type="primary" class="search-button"
                        v-if="selectedImage && selectedBand && selectedGridIDs.length > 0" @click="handleMergeDownload">
                        <DownloadIcon :size="18" class="button-icon" />
                        <span>合并下载</span>
                    </a-button>
                    <a-button type="primary" class="search-button"
                        v-if="selectedImage && selectedBand && selectedGridIDs.length > 0" @click="handleAddToProject">
                        <FilePlus2Icon :size="18" class="button-icon" />
                        <span>添加至项目</span>
                    </a-button>
                </div>
            </div>
        </dv-border-box12>
        <ProjectModal v-model="projectModalOpen" @select_project="handleSelectProject" />
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, type ComputedRef, computed } from 'vue'
import {
    DatabaseIcon,
    MapPinIcon,
    CalendarIcon,
    UploadCloudIcon,
    SearchIcon,
    RefreshCwIcon,
    HexagonIcon,
    CloudIcon,
    ApertureIcon,
    ClockIcon,
    ArrowLeftFromLineIcon,
    ImageIcon,
    FileSearchIcon,
    MaximizeIcon,
    LayersIcon,
    ListIcon,
    MousePointerClickIcon,
    DownloadIcon,
    FilePlus2Icon,
} from 'lucide-vue-next'
import type { Dayjs } from 'dayjs'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import DataSetModal from './imageDataModal.vue'
import ProjectModal from './projectModal.vue'
import { ezStore, useGridStore } from '@/store'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'

///// Local /////////////////////////
import { districts, initialFilterConditions, cloudMarks } from './constant'
import { polygonGeometryToBoxCoordinates } from '@/util/common'
import * as MapOperation from '@/util/map/operation'
import type { ImageFilterCondition, Project } from './type'
import {
    type ProductView,
    type SceneView,
    searchSceneViews,
    type BandView,
    fetchBandViews,
    getSceneGridGeoJsonURL,
    mergeGridsAndDownload,
} from './apiAdapter/adapter'

const UIStage = ref<'filter-stage' | 'tile-stage'>('filter-stage')

//////////////////////////////////////////////////////////////
/////////////// Filter Stage /////////////////////////////////
const filterConditions = reactive<ImageFilterCondition>(initialFilterConditions)
const imageFilterSearchResult = ref<SceneView[]>([])
const datasetModalOpen = ref<boolean>(false)
const projectModalOpen = ref<boolean>(true)

/// 01 Dataset Filter --> selectedProduct
const selectedProduct = ref<ProductView | undefined>()
const handleDatasetSelect = () => (datasetModalOpen.value = true)
const handleSelectedProduct = (productInfo: ProductView) => (selectedProduct.value = productInfo)

/// 02 Spatial Filter --> filterConditions
const spatialTabsKey = ref('1')
const selectedDistrict = ref<string[]>([])
const handleDrawPoint = () => MapOperation.draw_pointMode()
const handleDrawPolygon = () => MapOperation.draw_polygonMode()
const handleGeojsonUpload = () => {
    const fileInput = document.createElement('input')
    fileInput.type = 'file'
    fileInput.accept = '.geojson'
    fileInput.onchange = async (event) => {
        const file = (event.target as HTMLInputElement).files?.[0]
        if (file) {
            const reader = new FileReader()
            reader.onload = (e: ProgressEvent<FileReader>) => {
                try {
                    const geojson = JSON.parse(e.target?.result as string)
                    const polygonFeature = geojson.features[0]
                    ezStore.set('polygonFeature', polygonFeature)
                } catch (error) {
                    message.error('不支持的文件')
                    console.error('不支持的文件 GeoJSON:', error)
                }
            }
            reader.readAsText(file)
        }
    }
    fileInput.click()
}

// 03 Time Filter --> filterConditions
const dateRangeValue = ref<[Dayjs | undefined, Dayjs | undefined]>([dayjs('2010-10'), dayjs('2024-10')])

// 04 Cloud Filter --> filterConditions
const cloudRange = ref<[number, number]>([23, 41])

// 05 Image Filter Search --> imageFilterSearchResult
const handleImageFilterSearch = () => {
    // 收集筛选条件
    filterConditions.product = selectedProduct.value!
    filterConditions.dateRange = [dateRangeValue.value[0]!, dateRangeValue.value[1]!]
    filterConditions.geometry = MapOperation.getCurrentGeometry()
    filterConditions.cloudCover = cloudRange.value
    // 切换到瓦片选择面板
    UIStage.value = 'tile-stage'
    MapOperation.draw_deleteAll()
    searchResultLoading.value = true
    searchText.value = ''
    // 搜索影像
    searchSceneViews(filterConditions).then((sceneViews) => {
        console.log('imageFilterSearchResult:', sceneViews)
        imageFilterSearchResult.value = sceneViews
        searchResultLoading.value = false
    })
    //// temp mock
    // import('./constant/mock').then(({ mockFilterResult }) => {
    //     imageFilterSearchResult.value = mockFilterResult
    //     console.log('imageFilterSearchResult:', imageFilterSearchResult.value)
    //     searchResultLoading.value = false
    // })
}
// 06 Filter Stage Reset --> filterConditions
const handleFilterStageReset = () => {
    spatialTabsKey.value = '1'
    selectedDistrict.value = []
    selectedProduct.value = undefined
    dateRangeValue.value = [undefined, undefined]
    MapOperation.draw_deleteAll()
    console.log('Reset Filter Stage')
}

/////////////////////////////////////////////////////////////
/////////////// Tile Stage //////////////////////////////////

/// 01 Input Search
const searchText = ref<string>('')
const searchResultLoading = ref<boolean>(false)
const handleTextSearch = () => {
    // just tricking
    searchResultLoading.value = true
    setTimeout(() => {
        searchResultLoading.value = false
    }, 400)
}
const filteredImages: ComputedRef<SceneView[]> = computed(() => {
    // actual searching
    if (!searchText.value) return imageFilterSearchResult.value
    return imageFilterSearchResult.value.filter((image) => image.name.toLowerCase().includes(searchText.value.toLowerCase()))
})
/// 02 Select Image
const selectedImage = ref<SceneView | undefined>()
const toggleImageSelection = (imageId: string) => {
    // Just toggle selection, add the image outline to map
    selectedImage.value = imageFilterSearchResult.value.find((image) => image.id === imageId)
    selectedImage.value && MapOperation.map_showImagePolygon(selectedImage.value.geoFeature)
}
const clearSelection = () => {
    selectedImage.value = undefined
    MapOperation.map_destroyImagePolygon()
}
const confirmSelection = async () => {
    bandViews.value = await fetchBandViews(selectedImage.value!.id)

    // remove the image outline from map
    MapOperation.map_destroyImagePolygon()
    // remove the grid from map
    MapOperation.map_destroyGridLayer()

    // add the preview png to map
    const boxCoordinates = polygonGeometryToBoxCoordinates(selectedImage.value!.geoFeature)
    MapOperation.map_addImagePreviewLayer({
        imageUrl: selectedImage.value!.preview_url,
        boxCoordinates: boxCoordinates,
    })

    // add the grid to map
    const geojsonURL = getSceneGridGeoJsonURL(selectedImage.value!)
    MapOperation.map_addGridLayer(geojsonURL)
}
/// 03 Select Band
const bandViews = ref<BandView[]>([])
const selectedBand = ref<BandView | undefined>()

/// 04 Grid
const gridStore = useGridStore()
const selectedGridIDs = computed(() => gridStore.selectedGrids)
const removeOneGrid = (gridID: string) => {
    gridStore.removeGrid(gridID)
}

// 05 Bottom Buttons
const handleTileStageReset = () => {
    gridStore.clearGrids()
    selectedBand.value = undefined
    selectedImage.value = undefined
    MapOperation.map_destroyGridLayer()
    console.log('Reset Tile Stage')
}
const handleMergeDownload = () => {
    console.log('Merge download clicked !')
    // mergeGridsAndDownload(selectedGridIDs.value, selectedBand.value!.id)
}

const handleAddToProject = () => projectModalOpen.value = true

const handleSelectProject = (project: Project) => {
    console.log('selectedProject:', project)
    // TODO: add selectedGrid to project 
    console.log('add selectedGrid to project')
    // console.log('selectedGridIDs:', selectedGridIDs.value)
    // console.log('selectedBand:', selectedBand.value)
}

// 06 Back to Filter Stage
const handleBackToFilterStage = () => {
    handleTileStageReset()
    UIStage.value = 'filter-stage'
}

///// Local Helper /////////////////////////
const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.remote-sensing-panel {
    border-radius: 0.75rem;
    background-color: hsl(234, 100%, 6%);
    padding-left: 0.5rem;
    padding-right: 0.5rem;
    display: flex;
}

:deep(.border-box-content) {
    padding: 1.5rem;
}

.main-container {
    overflow-y: auto;
    max-height: calc(100vh - 12vh);
    flex: 1;
    scrollbar-width: none !important;
    scrollbar-color: rgba(37, 190, 255, 0.332) transparent !important;
}

.main-container::-webkit-scrollbar {
    width: none !important;
}

.panel-container {
    display: flex;
    flex-direction: column;
    padding: 0.5rem;
}

.action-button {
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border: none;
    color: #94a3b8;
    cursor: pointer;
    padding: 0.25rem;
    border-radius: 0.25rem;
    transition: all 0.2s ease;
}

.action-button:hover {
    color: #38bdf8;
    background-color: rgba(56, 189, 248, 0.1);
}

.panel-section {
    background-color: rgba(15, 23, 42, 0.6);
    border-radius: 0.5rem;
    margin-bottom: 1rem;
    border: 1px solid rgba(56, 189, 248, 0.15);
    overflow: hidden;
    transition: all 0.3s ease;
}

.panel-section:hover {
    box-shadow: 0 2px 8px rgba(56, 189, 248, 0.15);
    border-color: rgba(56, 189, 248, 0.3);
}

.section-header {
    display: flex;
    align-items: center;
    padding: 0.75rem 1rem;
    border-bottom: 2px solid rgba(56, 189, 248, 0.2);
    background: linear-gradient(to right, rgba(56, 189, 248, 0.1), transparent);
}

.section-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: 0.25rem;
    background-color: rgba(14, 165, 233, 0.2);
    color: #38bdf8;
    margin-right: 0.75rem;
}

.section-title {
    font-size: 1rem;
    font-weight: 600;
    color: #e0f2fe;
    margin: 0;
}

.section-actions {
    position: absolute;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
}

.section-content {
    padding: 0.75rem 1rem;
}

.band-selection {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.upload-area {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100px;
    border: 2px dashed rgba(56, 189, 248, 0.3);
    border-radius: 0.5rem;
    margin: 0.75rem 1rem;
    cursor: pointer;
    transition: all 0.2s ease;
    background-color: rgba(14, 165, 233, 0.1);
}

.upload-t {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
}

.upload-area:hover {
    border-color: #38bdf8;
    background-color: rgba(14, 165, 233, 0.15);
}

.upload-icon {
    color: #38bdf8;
    margin-bottom: 0.5rem;
}

.upload-text {
    color: #e0f2fe;
    font-size: 0.9rem;
}

.custom-tabs :deep(.ant-tabs-nav) {
    margin-bottom: 0.5rem;
    transition: 0.3s ease-in-out;
}

.custom-tabs :deep(.ant-tabs-nav-wrap) {
    display: flex;
}

.custom-tabs :deep(.ant-tabs-nav-list) {
    width: 100%;
    flex: 1;
    display: flex;
    justify-content: space-evenly;
}

.custom-tabs :deep(.ant-tabs-tab) {
    flex: 1;
    padding: 0.5rem 1rem;
    border-radius: 0.25rem 0.25rem 0 0;
    transition: all 0.2s ease;
    display: flex;
    justify-content: center;
    align-items: center;
    background-color: rgba(15, 23, 42, 0.8);
    border-color: rgba(56, 189, 248, 0.2);
    color: #247699;
}

.custom-tabs :deep(.ant-tabs-tab-active) {
    background-color: rgba(14, 165, 233, 0.15) !important;
    border-color: #38bdf8 !important;
    box-shadow: 0 0 5px 1px rgba(56, 195, 255, 0.621) inset;
}

.custom-tabs :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
    color: #38bdf8 !important;
    font-weight: 500;
}

.custom-tabs :deep(.ant-tabs-nav-operations) {
    display: none;
}

.tab-content {
    padding: 0.5rem 0;
    height: 7vh;
    display: flex;
    justify-content: center;
}

.button-group {
    display: flex;
    align-items: center;
    justify-content: space-evenly;
    gap: 0.5rem;
}

.custom-button {
    display: flex;
    align-items: center;
    gap: 0.25rem;
    border-color: rgba(56, 189, 248, 0.3);
    color: #38bdf8;
    transition: all 0.2s ease;
    background-color: rgba(14, 165, 233, 0.05);
    padding-left: 1.5rem;
    padding-right: 1.5rem;
    height: 2rem;
}

.custom-button:hover {
    background-color: rgba(14, 165, 233, 0.15);
    border-color: #38bdf8;
}

.button-icon {
    margin-right: 0.25rem;
}

.divider {
    height: 1.5rem;
    background-color: rgba(56, 189, 248, 0.3);
}

/* Cascader */
.custom-cascader {
    width: 100%;
}

.custom-cascader :deep(.ant-select-selector) {
    border-color: rgba(56, 189, 248, 0.3) !important;
    background-color: rgba(15, 23, 42, 0.6) !important;
    color: #e0f2fe !important;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 0.95rem;
    padding-left: 1rem;
}

.custom-cascader:hover :deep(.ant-select-selector) {
    border-color: #38bdf8 !important;
}

.custom-cascader :deep(.ant-select-selection-placeholder) {
    color: #ffffff !important;
}

.custom-cascader :deep(.ant-select-dropdown) {
    background-color: red !important;
}

/* Date-Picker */
.custom-date-picker {
    width: 100%;
    margin: 1rem 0;
    height: 3rem;
    background-color: rgba(56, 191, 248, 0.097);
    border-color: #247699;
    color: #3fc5ff;
}

.custom-date-picker :deep(.ant-picker) {
    border-color: rgba(56, 189, 248, 0.3);
    background-color: rgba(15, 23, 42, 0.6);
}

.custom-date-picker :deep(.ant-picker-input > input) {
    color: #e0f2fe !important;
}

.custom-date-picker :deep(.ant-picker-input > input::placeholder) {
    color: #e0f2fe !important;
}

.custom-date-picker :deep(.ant-picker-suffix) {
    color: #38bdf8;
}

.custom-date-picker:hover :deep(.ant-picker) {
    border-color: #38bdf8;
}

.custom-date-picker :deep(.anticon svg) {
    fill: #38bdf8;
}

.custom-date-picker :deep(.ant-picker-input input) {
    text-align: center;
    font-size: 1rem;
    letter-spacing: 0.1rem;
}

.custom-date-picker :deep(.ant-picker-active-bar) {
    transform: translateX(1rem);
}

/* Slider */
.custom-slider {
    /* position: relative;
    left: 5%;
    width: 90%; */
}

.custom-slider :deep(.ant-slider-mark-text) {
    color: #31a6d8;
    font-size: 0.9rem;
    padding-left: 0.5rem;
}

.custom-select {
    width: 100%;
}

.custom-select :deep(.ant-select-selector) {
    border-color: rgba(56, 189, 248, 0.3) !important;
    background-color: rgba(15, 23, 42, 0.6) !important;
    color: #e0f2fe !important;
}

.custom-select:hover :deep(.ant-select-selector) {
    border-color: #38bdf8 !important;
}

.panel-footer {
    display: flex;
    gap: 0.75rem;
    padding: 0.75rem 1rem;
    padding-top: 0rem;
    margin-top: 2rem;
}

.search-button {
    flex: 2;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgba(56, 189, 248, 0.3);
    box-shadow: 0 2px 6px rgba(2, 132, 199, 0.3);
    transition: all 0.2s ease;
    border: none;
}

.search-button:hover {
    background-color: #0369a1;
    box-shadow: 0 4px 8px rgba(2, 133, 199, 0.684);
}

.reset-button {
    flex: 1;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #94a3b8;
    background-color: rgba(15, 23, 42, 0.4);
    border-color: rgba(56, 189, 248, 0.2);
    transition: all 0.2s ease;
}

.reset-button:hover {
    color: #e0f2fe;
    border-color: rgba(56, 189, 248, 0.5);
    background-color: rgba(14, 165, 233, 0.1);
}

.selected-tags-container {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 12px;
    padding: 0 8px;
}

.product-tag {
    display: flex;
    align-items: center;
    padding: 4px 8px;
    border-radius: 4px;
    margin-right: 0;
    background-color: rgba(14, 165, 233, 0.15);
    border-color: rgba(56, 189, 248, 0.3);
    color: #e0f2fe;
}

.tag-icon {
    margin-right: 4px;
    color: #38bdf8;
}

.selected-product-info {
    padding: 1.5rem;
    background-color: rgba(15, 23, 42, 0.4);
    border-radius: 8px;
    border: 1px solid rgba(56, 189, 248, 0.2);
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
}

.product-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
}

.product-icon {
    color: #38bdf8;
}

.product-name {
    font-size: 1rem;
    font-weight: 500;
    color: #e0f2fe;
}

.product-tags {
    display: flex;
    gap: 8px;
    margin-bottom: 8px;
}

.product-tag {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 8px;
    background-color: rgba(56, 189, 248, 0.1);
    border: 1px solid rgba(56, 189, 248, 0.2);
    border-radius: 4px;
    font-size: 0.85rem;
    color: #38bdf8;
}

.tag-icon {
    color: #38bdf8;
}

.product-description {
    font-size: 0.9rem;
    color: #94a3b8;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

/* 影像列表样式 */
.image-list-row {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    max-height: 350px;
    min-height: 350px;
    overflow-y: auto;
    padding: 0.25rem;
}

.image-list-row::-webkit-scrollbar {
    width: 6px;
}

.image-list-row::-webkit-scrollbar-track {
    background-color: transparent;
}

.image-list-row::-webkit-scrollbar-thumb {
    background-color: rgba(56, 189, 248, 0.3);
    border-radius: 3px;
}

.image-list-row::-webkit-scrollbar-thumb:hover {
    background-color: rgba(56, 189, 248, 0.5);
}

.image-row-item {
    display: flex;
    align-items: center;
    padding: 0.75rem;
    border: 1px solid rgba(56, 189, 248, 0.2);
    border-radius: 0.5rem;
    cursor: pointer;
    transition: all 0.2s ease;
    background-color: rgba(15, 23, 42, 0.4);
}

.image-row-item:hover {
    border-color: rgba(56, 189, 248, 0.5);
    background-color: rgba(14, 165, 233, 0.1);
    box-shadow: 0 0 10px rgba(56, 189, 248, 0.1);
}

.image-row-item.selected {
    border-color: #38bdf8;
    background-color: rgba(14, 165, 233, 0.15);
    box-shadow: 0 0 15px rgba(56, 189, 248, 0.2);
}

.image-row-preview {
    position: relative;
    width: 70px;
    height: 70px;
    border-radius: 0.375rem;
    overflow: hidden;
    flex-shrink: 0;
    margin-right: 1rem;
    border: 1px solid rgba(56, 189, 248, 0.3);
    background-color: rgba(15, 23, 42, 0.6);
}

.row-thumbnail {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.image-row-details {
    flex: 1;
    min-width: 0;
}

.image-row-name {
    font-size: 0.95rem;
    font-weight: 500;
    margin: 0 0 0.5rem 0;
    color: #e0f2fe;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.image-row-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 1rem;
    font-size: 0.8rem;
    color: #94a3b8;
}

.image-row-date,
.image-row-resolution,
.image-row-cloud {
    display: flex;
    align-items: center;
    gap: 0.25rem;
}

.image-row-actions {
    margin-left: 0.75rem;
}

.image-row-actions :deep(.ant-radio-wrapper) {
    color: #38bdf8;
}

.image-row-actions :deep(.ant-radio-inner) {
    background-color: rgba(15, 23, 42, 0.6);
    border-color: rgba(56, 189, 248, 0.5);
}

.image-row-actions :deep(.ant-radio-checked .ant-radio-inner) {
    background-color: #38bdf8;
    border-color: #38bdf8;
}

.image-row-actions :deep(.ant-radio-wrapper:hover .ant-radio-inner) {
    border-color: #38bdf8;
}

.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 3rem 1rem;
    color: #94a3b8;
    flex: 1;
}

.empty-icon {
    margin-bottom: 1rem;
    color: rgba(56, 189, 248, 0.5);
}

.selection-summary {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.75rem 0;
    border-top: 1px solid rgba(56, 189, 248, 0.2);
    margin-top: 1rem;
}

.selection-summary :deep(.ant-btn-link) {
    color: #94a3b8;
}

.selection-summary :deep(.ant-btn-link:hover) {
    color: #38bdf8;
}

.selection-summary :deep(.ant-btn-primary) {
    background-color: rgba(56, 189, 248, 0.3);
    border-color: rgba(56, 189, 248, 0.5);
    box-shadow: 0 2px 6px rgba(56, 189, 248, 0.2);
}

.selection-summary :deep(.ant-btn-primary:hover) {
    background-color: #38bdf8;
    border-color: #38bdf8;
    box-shadow: 0 4px 8px rgba(56, 189, 248, 0.3);
}

/* 搜索框样式 */
.my-3 :deep(.ant-input-search) {
    width: 100%;
}

.my-3 :deep(.ant-input-affix-wrapper) {
    background-color: rgb(175, 226, 255);
}

.my-3 :deep(.ant-input-search .ant-input) {
    background-color: transparent;
    border-color: rgba(56, 189, 248, 0.3);
    color: #002a46;
}

.my-3 :deep(.ant-input-search .ant-input:hover) {
    border-color: rgba(56, 189, 248, 0.5);
}

.my-3 :deep(.ant-input-search .ant-input:focus) {
    border-color: #38bdf8;
    box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.2);
}

.my-3 :deep(.ant-input-search .ant-input::placeholder) {
    color: #002a467b;
}

.my-3 :deep(.ant-input-search .ant-input-search-button) {
    background-color: rgba(56, 189, 248, 0.3);
    border-color: rgba(56, 189, 248, 0.3);
}

.my-3 :deep(.ant-input-search .ant-input-search-button:hover) {
    background-color: rgba(56, 189, 248, 0.5);
    border-color: rgba(56, 189, 248, 0.5);
}

.my-3 :deep(.ant-input-search .ant-input-search-button .anticon) {
    color: #e0f2fe;
}

/* 加载状态样式 */
:deep(.ant-spin) {
    color: #38bdf8;
}

:deep(.ant-spin-dot-item) {
    background-color: #38bdf8;
}

/* 波段选择样式 */
.band-radio-group {
    display: flex;
    flex-wrap: wrap;
    /* gap: 0.5rem; */
    margin-bottom: 1rem;
}

.band-radio-button {
    min-width: 2rem;
    flex: 1;
    text-align: center;
    transition: all 0.2s ease;
}

.band-radio-group :deep(.ant-radio-button-wrapper) {
    background-color: rgba(56, 189, 248, 0.3);
    border: none;
    color: #94a3b8;
    padding: 0.25rem 0.75rem;
    height: auto;
    line-height: 1.5;
    font-size: 0.9rem;
    border-radius: 0.25rem;
    margin-right: 0;
    transition: all 0.2s ease;
}

.band-radio-group :deep(.ant-radio-button-wrapper:hover) {
    color: #e0f2fe;
    background-color: rgba(14, 165, 233, 0.1);
    border: none;
    box-shadow: 0 0 5px rgba(56, 189, 248, 0.2);
}

.band-radio-group :deep(.ant-radio-button-wrapper-checked) {
    background-color: rgba(4, 167, 242, 0.733);
    border: none;
    color: #c7eeff;
    box-shadow: 0 0 10px rgba(56, 191, 248, 0.604) inset;
    font-weight: 500;
}

.band-radio-group :deep(.ant-radio-button-wrapper-checked:hover) {
    background-color: rgba(14, 165, 233, 0.3);
    border: none;
    color: #38bdf8;
}

.band-radio-group :deep(.ant-radio-button-wrapper-checked:not(.ant-radio-button-wrapper-disabled):hover) {
    color: #38bdf8;
    background-color: rgba(14, 165, 233, 0.3);
    border: none;
}

.band-radio-group :deep(.ant-radio-button-wrapper-checked:not(.ant-radio-button-wrapper-disabled):active) {
    color: #38bdf8;
    background-color: rgba(14, 165, 233, 0.4);
    border: none;
}

.band-radio-group :deep(.ant-radio-button-wrapper-checked:not(.ant-radio-button-wrapper-disabled)::before) {
    background-color: #000000;
    border: none;
}

.block-area {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 2rem 1rem;
    background-color: rgba(15, 23, 42, 0.4);
    border-radius: 0.5rem;
    border: 1px dashed rgba(56, 189, 248, 0.3);
    transition: all 0.3s ease;
}

.block-area:hover {
    border-color: rgba(56, 189, 248, 0.5);
    background-color: rgba(14, 165, 233, 0.1);
}

.block-icon {
    color: #38bdf8;
    margin-bottom: 0.75rem;
    opacity: 0.8;
    transition: all 0.3s ease;
}

.block-area:hover .block-icon {
    opacity: 1;
    transform: scale(1.1);
}

.block-text {
    color: #94a3b8;
    font-size: 0.9rem;
    text-align: center;
    transition: all 0.3s ease;
}

.block-area:hover .block-text {
    color: #e0f2fe;
}

/* 已选择切片的标签样式 */
.block-area:not(:empty) {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 0.5rem;
    padding: 1rem;
    border-style: solid;
    border-width: 1px;
    border-color: rgba(56, 189, 248, 0.2);
    background-color: rgba(15, 23, 42, 0.5);
}

.product-tag {
    display: flex;
    align-items: center;
    padding: 0.25rem 0.75rem;
    background-color: rgba(14, 165, 233, 0.15);
    border: 1px solid rgba(56, 189, 248, 0.3);
    border-radius: 0.375rem;
    color: #e0f2fe;
    font-size: 0.85rem;
    transition: all 0.2s ease;
    margin: 0;
}

.product-tag:hover {
    background-color: rgba(14, 165, 233, 0.25);
    border-color: rgba(56, 189, 248, 0.5);
    box-shadow: 0 0 8px rgba(56, 189, 248, 0.2);
}

.product-tag :deep(.anticon) {
    margin-left: 0.5rem;
    color: #38bdf8;
    font-size: 0.8rem;
    transition: all 0.2s ease;
}

.product-tag:hover :deep(.anticon) {
    color: #e0f2fe;
    transform: scale(1.1);
}

.product-tag :deep(.anticon:hover) {
    color: #ff4d4f;
}
</style>
