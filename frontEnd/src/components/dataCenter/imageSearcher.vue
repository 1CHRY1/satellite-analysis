<template>
    <div class="remote-sensing-panel select-none">
        <dv-border-box12 v-if="UIStage === 'filter-stage'">
            <section class="panel-section">
                <div class="section-header">
                    <div class="section-icon"><database-icon :size="18" /></div>
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

                    <DataSetModal v-model="modalOpen" @update:selected-product-id="handleSelectedProduct" />
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
                    <a-tabs v-model:activeKey="activeKey" type="card" class="custom-tabs">
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
                                <div class="upload-area !my-0 !h-[50px] !w-full !flex-row" @click="handleFileUpload">
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
                        <calendar-icon :size="18" />
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
                        <a-slider class="custom-slider" range :marks="marks" v-model:value="cloudRange"
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
                    <search-icon :size="18" class="button-icon" />
                    <span>开始检索</span>
                </a-button>
                <a-button class="reset-button" @click="handleReset">
                    <refresh-cw-icon :size="18" class="button-icon" />
                    <span>重置</span>
                </a-button>
            </div>
        </dv-border-box12>

        <dv-border-box12 v-if="UIStage === 'tile-stage'">
            <!-- Image List Section with Row Layout -->
            <section class="panel-section">
                <div class="section-header relative">
                    <div class="section-icon">
                        <ImageIcon :size="18" />
                    </div>
                    <h2 class="section-title">影像列表</h2>
                    <div class="section-actions right-4">
                        <a-tooltip title="刷新列表">
                            <a-button type="text" size="small" @click="handleRefreshImageList">
                                <RefreshCwIcon :size="16" :color="'#38bdf8'" />
                            </a-button>
                        </a-tooltip>
                    </div>
                    <div class="section-actions right-12">
                        <a-tooltip title="返回">
                            <a-button type="text" size="small" @click="UIStage = 'filter-stage'">
                                <ArrowLeftFromLineIcon :size="16" :color="'#38bdf8'" />
                            </a-button>
                        </a-tooltip>
                    </div>
                </div>

                <div class="section-content">
                    <div class="my-3">
                        <a-input-search v-model:value="searchQuery" placeholder="搜索影像" style="width: 100%"
                            @search="handleInputSearch" allow-clear />
                    </div>

                    <!-- <a-spin :spinning="loading">
                        <div class="image-list-row">
                            <div v-for="image in filteredImages" :key="image.id" class="image-row-item"
                                :class="{ selected: selectedImages.includes(image.id) }"
                                @click="toggleImageSelection(image.id)">
                                <div class="image-row-preview">
                                    <img :src="image.thumbnail" :alt="image.name" class="row-thumbnail" />
                                    <div class="image-row-overlay">
                                        <check-circle-icon v-if="selectedImages.includes(image.id)" :size="16"
                                            class="check-icon" />
                                    </div>
                                </div>
                                <div class="image-row-details">
                                    <h4 class="image-row-name">{{ image.name }}</h4>
                                    <div class="image-row-meta">
                                        <span class="image-row-date">
                                            <calendar-icon :size="12" />
                                            {{ formatDate(image.date) }}
                                        </span>
                                        <span class="image-row-resolution">
                                            <maximize-icon :size="12" />
                                            {{ image.resolution }}
                                        </span>
                                        <span class="image-row-cloud">
                                            <cloud-icon :size="12" />
                                            {{ image.cloudCover }}%
                                        </span>
                                    </div>
                                </div>
                                <div class="image-row-actions">
                                    <a-checkbox :checked="selectedImages.includes(image.id)" @click.stop
                                        @change="(e: any) => handleCheckboxChange(e, image.id)" />
                                </div>
                            </div>

                            <div v-if="filteredImages.length === 0" class="empty-state">
                                <file-search-icon :size="32" class="empty-icon" />
                                <p>没有找到符合条件的影像</p>
                            </div>
                        </div>
                    </a-spin> -->

                    <!-- <div class="selection-summary" v-if="selectedImages.length > 0">
                        <span>已选择 {{ selectedImages.length }} 个影像</span>
                        <div>
                            <a-button type="link" @click="clearSelection">清除选择</a-button>
                            <a-button type="primary" @click="loadImage">加载影像</a-button>
                        </div>
                    </div> -->
                </div>
            </section>

            <!-- Band Selection Section (Modified to work without grid selection) -->
            <!-- <section class="panel-section" v-if="selectedImages.length > 0">
                <div class="section-header">
                    <div class="section-icon">
                        <layers-icon :size="18" />
                    </div>
                    <h2 class="section-title">波段选择</h2>
                </div>

                <div class="section-content">
                    <div class="band-selection">
                        <a-radio-group v-model:value="selectedBand" button-style="solid" class="band-radio-group">
                            <a-radio-button v-for="band in availableBands" :key="band.id" :value="band.id"
                                class="band-radio-button">
                                {{ band.name }}
                            </a-radio-button>
                        </a-radio-group>

                        <div class="band-preview" v-if="selectedBand">
                            <div class="band-info">
                                <h4>{{ getBandDetails.name }}</h4>
                                <p>{{ getBandDetails.description }}</p>
                                <div class="band-specs">
                                    <span>波长: {{ getBandDetails.wavelength }}</span>
                                    <span>分辨率: {{ getBandDetails.resolution }}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section> -->

            <!-- Selected Images Summary Section (New) -->
            <!-- <section class="panel-section" v-if="selectedImages.length > 0">
                <div class="section-header">
                    <div class="section-icon">
                        <list-icon :size="18" />
                    </div>
                    <h2 class="section-title">切片选择</h2>
                </div>

                <div class="section-content">
                    <div class="block-area" v-if="selectedGridIDs.length === 0">
                        <mouse-pointer-click-icon :size="22" class="block-icon" />
                        <span class="block-text">点击地图以选择切片</span>
                    </div>
                    <div class="block-area" v-else>
                        <a-tag v-for="gridID in selectedGridIDs" :key="gridID" closable @close="removeGrid(gridID)"
                            class="product-tag">
                            {{ gridID }}
                        </a-tag>
                    </div>
                </div>
            </section> -->

            <!-- Download Section (Modified to work without grid selection) -->
            <!-- <section class="panel-section" v-if="selectedImages.length > 0 && selectedBand">
                <div class="section-header">
                    <div class="section-icon">
                        <download-icon :size="18" />
                    </div>
                    <h2 class="section-title">下载选项</h2>
                </div>

                <div class="section-content">
                    <div class="download-options">
                        <div class="download-summary">
                            <div class="summary-item">
                                <layers-icon :size="14" />
                                <span>波段 {{ getBandDetails.name }}</span>
                            </div>
                        </div>

                        <a-button type="primary" block class="download-button" @click="downloadMergedImage"
                            :loading="downloading" :disabled="!selectedBand || selectedImages.length === 0">
                            <download-icon :size="16" />
                            <span>合并下载</span>
                        </a-button>
                    </div>
                </div>
            </section> -->
        </dv-border-box12>


        <!-- <TileSelectionPanel
            v-if="UIStage === 'tile-stage'"
            :filter-conditions="filterConditions"
            @back-stage="
                () => {
                    UIStage = 'filter-stage'
                }
            "
        >
        </TileSelectionPanel> -->
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
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
} from 'lucide-vue-next'
import type { Dayjs } from 'dayjs'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import DataSetModal from './imageDataModal.vue'
// import TileSelectionPanel from './TileSelectionPanel.vue'
import { type ProductView } from './apiAdapter/adapter'
import { districts } from './constant/districts'
import * as MapOperation from '@/util/map/operation'
import dayjs from 'dayjs'
import { ezStore } from '@/store'

type RangeValue = [Dayjs, Dayjs] | undefined

const UIStage = ref<'filter-stage' | 'tile-stage'>('tile-stage')
const modalOpen = ref<boolean>(false)
const activeKey = ref('1')
const selectedDistrict = ref<string[]>([])
const dateRangeValue = ref<RangeValue>([dayjs('2010-10'), dayjs('2024-10')])
const selectedGrid = ref<string>()
const filterConditions = ref({
    products: [],
    dateRange: ['', ''],
    geometry: {
        type: 'Polygon',
        coordinates: [
            [
                [-180, 90],
                [-180, -90],
                [180, -90],
                [180, 90],
                [-180, 90],
            ],
        ],
    },
})
// 搜索框
const searchQuery = ref<string>('')
// const filteredImages: ComputedRef<SceneView[]> = computed(() => {
//     if (!searchQuery.value) {
//         return images.value
//     }
//     return images.value.filter((image) =>
//         image.name.toLowerCase().includes(searchQuery.value.toLowerCase()),
//     )
// })
const handleInputSearch = () => {
    console.log('searchQuery:', searchQuery.value)
}

// Cloud
const marks = ref<Record<number, string>>({
    0: '0%',
    50: '50%',
    100: '100%',
})
const cloudRange = ref<number[]>([23, 41])

const handleDatasetSelect = () => {
    console.log('Select dataset clicked')
    modalOpen.value = true
}

const handleSelectedProduct = (productInfo: ProductView) => {
    console.log('Selected product:', productInfo)
    selectedProduct.value = productInfo
}

const handleFileUpload = async () => {
    console.log('File upload clicked')

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
                    console.log(' ezStore set polygonFeature:', polygonFeature)
                } catch (error) {
                    console.error('Error parsing GeoJSON:', error)
                }
            }
            reader.readAsText(file)
        }
    }
    fileInput.click()
}

const handleDrawPoint = () => {
    console.log('Draw point clicked')
    MapOperation.draw_pointMode()
}

const handleDrawPolygon = () => {
    console.log('Draw polygon clicked')
    MapOperation.draw_polygonMode()
}

const handleImageFilterSearch = () => {
    // 收集筛选条件
    filterConditions.value = {
        products: [selectedProduct.value] as any,
        dateRange: dateRangeValue.value
            ? [
                dateRangeValue.value[0].format('YYYY-MM-DD HH:mm:ss'),
                dateRangeValue.value[1].format('YYYY-MM-DD HH:mm:ss'),
            ]
            : [
                dayjs('2010-10').format('YYYY-MM-DD HH:mm:ss'),
                dayjs('2024-10').format('YYYY-MM-DD HH:mm:ss'),
            ],
        geometry: MapOperation.getCurrentGeometry(),
    }
    // 切换到瓦片选择面板
    UIStage.value = 'tile-stage'
    setTimeout(() => {
        MapOperation.draw_deleteAll()
    }, 0);
}

const handleRefreshImageList = () => {
    console.log('Refresh image list clicked')
}


const handleReset = () => {
    activeKey.value = '1'
    selectedDistrict.value = []
    selectedProduct.value = undefined
    dateRangeValue.value = undefined
    selectedGrid.value = undefined
    MapOperation.draw_deleteAll()
    console.log('Reset clicked')
}

const selectedProduct = ref<ProductView | undefined>()
</script>

<style scoped>
.remote-sensing-panel {
    overflow-y: auto;
    border-radius: 0.75rem;
    background-color: hsl(234, 100%, 6%);
    padding-left: 0.5rem;
    padding-right: 0.5rem;
}

:deep(.border-box-content) {
    padding: 1.5rem;
}

/* 滚动条 */
.remote-sensing-panel::-webkit-scrollbar {
    width: 8px;
}

.remote-sensing-panel::-webkit-scrollbar-thumb {
    background-color: rgba(37, 190, 255, 0.5);
    border-radius: 4px;
}

.remote-sensing-panel::-webkit-scrollbar-thumb:hover {
    background-color: rgba(37, 190, 255, 0.7);
}

.remote-sensing-panel::-webkit-scrollbar-track {
    background-color: transparent;
}

.remote-sensing-panel {
    scrollbar-width: thin;
    scrollbar-color: rgba(37, 190, 255, 0.332) transparent;
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
</style>
