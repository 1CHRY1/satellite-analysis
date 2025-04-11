<template>
    <div class="remote-sensing-panel select-none">
        <dv-border-box12>
            <div class="main-container">
                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <DatabaseIcon :size="18" />
                        </div>
                        <h2 class="section-title">影像产品选择</h2>
                        <div class="section-actions right-4">
                            <a-tooltip title="重置">
                                <a-button type="text" size="small" @click=handleDatasetReset>
                                    <RefreshCwIcon :size="16" :color="'#38bdf8'" />
                                </a-button>
                            </a-tooltip>
                        </div>
                    </div>
                    <div class="upload-area" @click="handleDatasetSelect">
                        <div class="upload-t" v-if="!selectedProduct">
                            <UploadCloudIcon :size="18" class="upload-icon" />
                            <span class="upload-text">点击选择影像产品</span>
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
                        <h2 class="section-title">区域影像检索</h2>
                        <div class="section-actions right-4">
                            <a-tooltip title="重置">
                                <a-button type="text" size="small" @click=handleRegionGridReset>
                                    <RefreshCwIcon :size="16" :color="'#38bdf8'" />
                                </a-button>
                            </a-tooltip>
                        </div>
                    </div>
                    <div class="section-content">
                        <a-tabs v-model:activeKey="spatialTabsKey" type="card" class="custom-tabs">
                            <a-tab-pane key="1" tab="研究区绘制">
                                <div class="tab-content">
                                    <div class="button-group !gap-x-5">
                                        <a-button class="custom-button !h-16 flex-col justify-center"
                                            @click="handleDrawPolygon">
                                            <HexagonIcon :size="16" class="button-icon" />
                                            <span>开始绘制</span>
                                        </a-button>
                                        <a-button class="custom-button !h-16 flex-col justify-center"
                                            @click="handleCancelDraw">
                                            <BanIcon :size="16" class="button-icon" />
                                            <span>取消绘制</span>
                                        </a-button>
                                    </div>
                                </div>
                            </a-tab-pane>
                            <a-tab-pane key="2" tab="瓦片筛选">
                                <div class="tab-content flex flex-col justify-start gap-1">
                                    <div class="block-area min-h-[100px] max-h-[150px] !flex-row overflow-y-scroll">
                                        <a-tag v-for="gridID in selectedGridIDs" :key="gridID" closable
                                            @close="handleRemoveOneGrid(gridID)" class="product-tag">
                                            {{ gridID.slice(0, 10) }}
                                        </a-tag>
                                    </div>
                                    <div class="button-group">
                                        <a-button class="custom-button !px-2" @click=handleSelectAllGrids>全选</a-button>
                                        <a-button class="custom-button !px-2"
                                            @click=handleSelectNoneGrids>全不选</a-button>
                                        <a-button class="custom-button !w-fit !bg-sky-800" :loading="gridSearchLoading" @click=handleStartGridSearch>
                                            开始检索
                                        </a-button>
                                    </div>
                                </div>
                            </a-tab-pane>
                        </a-tabs>
                    </div>
                </section>

                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <BoltIcon :size="18" />
                        </div>
                        <h2 class="section-title">影像检索结果信息</h2>
                    </div>
                    <div class="section-content">
                        <div class="result-info-container">
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <MapIcon :size="16" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">研究区面积</div>
                                    <div class="result-info-value">{{ searchResultInfo.area }} km²</div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <ImageIcon :size="16" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">当前已检索到</div>
                                    <div class="result-info-value">{{ searchResultInfo.imageCount }} 景影像</div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <CalendarIcon :size="16" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">涵盖时间范围</div>
                                    <div class="result-info-value date-range">
                                        <div class="date-item">{{ searchResultInfo.dateRange[0] }}</div>
                                        <div class="date-item">{{ searchResultInfo.dateRange[1] }}</div>
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <CloudIcon :size="16" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">云量范围</div>
                                    <div class="result-info-value">{{ searchResultInfo.cloudRange[0] }} ~ {{
                                        searchResultInfo.cloudRange[1] }}%</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>

                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <BoltIcon :size="18" />
                        </div>
                        <h2 class="section-title">区域影像镶嵌配置</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div class="config-item">
                                <div class="config-label relative">
                                    <CalendarIcon :size="16" class="config-icon" />
                                    <span>时间</span>
                                    <a-checkbox v-model:checked="tileMergeConfig.useLatestTime"
                                        class="absolute right-1 !text-sky-300">
                                        时间最近优先
                                    </a-checkbox>
                                </div>
                                <div class="config-control">
                                    <a-range-picker class="custom-date-picker" v-model:value="tileMergeConfig.dateRange"
                                        picker="day" :allow-clear="false" :placeholder="['开始日期', '结束日期']" />
                                </div>
                            </div>
                            <div class="config-item">
                                <div class="config-label relative">
                                    <CloudIcon :size="16" class="config-icon" />
                                    <span>云量</span>
                                    <a-checkbox v-model:checked="tileMergeConfig.useMinCloud"
                                        class="absolute right-1 !text-sky-300">
                                        云量最小优先
                                    </a-checkbox>
                                </div>
                                <div class="config-control">
                                    <div class="cloud-slider-container">
                                        <span class="cloud-value">{{ tileMergeConfig.cloudRange[0] }}%</span>
                                        <div class="slider-wrapper">
                                            <a-slider class="custom-slider" range
                                                v-model:value="tileMergeConfig.cloudRange"
                                                :tipFormatter="(value: number) => value + '%'" />
                                        </div>
                                        <span class="cloud-value">{{ tileMergeConfig.cloudRange[1] }}%</span>
                                    </div>
                                </div>
                            </div>
                            <div class="config-item">
                                <div class="config-label">
                                    <LayersIcon :size="16" class="config-icon" />
                                    <span>波段</span>
                                </div>
                                <div class="config-control">
                                    <a-checkbox-group v-model:value="tileMergeConfig.bands" class="band-radio-group">
                                        <a-checkbox v-for="band in bandViews" :key="band.id" :value="band"
                                            class="band-radio-button">
                                            {{ band.name }}
                                        </a-checkbox>
                                    </a-checkbox-group>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="section-footer">
                        <a-button class="reset-button" @click="handleAllReset">
                            <RefreshCwIcon :size="18" class="button-icon" />
                            <span>重置</span>
                        </a-button>
                        <a-button type="primary" class="search-button" v-if="
                            selectedProduct &&
                            tileMergeConfig.bands.length > 0 &&
                            selectedGridIDs.length > 0
                        " @click="handleMergeDownload" :loading="mergingLoading">
                            <DownloadIcon :size="18" class="button-icon" />
                            <span>合并下载</span>
                        </a-button>
                        <!-- <a-button type="primary" class="search-button" v-if="
                            selectedProduct &&
                            tileMergeConfig.bands.length > 0 &&
                            selectedGridIDs.length > 0
                        " @click="handleAddToProject" :loading="projectUploadLoading">
                            <FilePlus2Icon :size="18" class="button-icon" />
                            <span>添加至项目</span>
                        </a-button> -->
                    </div>
                </section>
            </div>
        </dv-border-box12>
        <ProjectModal v-model="projectModalOpen" @select_project="handleConfirmSelectProject" />
    </div>
</template>

<script setup lang="ts">
import { ref, watch, reactive, computed } from 'vue'
import {
    DatabaseIcon,
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
} from 'lucide-vue-next'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import DataSetModal from './imageDataModal.vue'
import ProjectModal from './projectModal.vue'
import { useGridStore, ezStore } from '@/store'
import dayjs from 'dayjs'
import { message, Modal, Alert } from 'ant-design-vue'

///// Local /////////////////////////
import { polygonGeometryToBoxCoordinates } from '@/util/common'
import * as MapOperation from '@/util/map/operation'
import { GridMaker } from '@/util/map/gridMaker'
import type { Project } from './type'
import {
    type ProductView,
    type SceneView,
    type GridInfoView,
    type OverlapTileInfoView,
    searchSceneViews,
    type BandView,
    fetchBandViews,
    startMergeTiles,
    downloadMergeTiles,
    queryOverlapTilesMap,
    intervalQueryMergingStatus,
    uploadTilesToProject,
    statisticsOverlapTileInfoView,
} from './apiAdapter/adapter'
const projectModalOpen = ref<boolean>(false)

//////////////////////////////////////////////////////////////
/////////////// 影像产品选择 //////////////////////////////////
const datasetModalOpen = ref<boolean>(false)

const selectedProduct = ref<ProductView | undefined>()
const handleDatasetSelect = () => (datasetModalOpen.value = true)
const handleSelectedProduct = (productInfo: ProductView) => (selectedProduct.value = productInfo)
const handleDatasetReset = () => {
    selectedProduct.value = undefined
    datasetModalOpen.value = false
}

//////////////////////////////////////////////////////////////
/////////////// 区域影像检索 //////////////////////////////////
const gridStore = useGridStore()
const gridMaker = new GridMaker(1, 20) // 1km分辨率, 25km2的限制
/// 01 绘制研究区
const spatialTabsKey = ref('1')
const handleDrawPolygon = () => { handleRegionGridReset(); MapOperation.draw_polygonMode() }
const handleCancelDraw = () => MapOperation.draw_deleteAll()
// 绘制完了就弹对话框
watch(() => gridStore.polygon, (newVal, _) => {
    if (newVal) { // 新多边形时trigger
        Modal.confirm({
            title: '确认选择该区域?',
            okText: '确认并生成格网',
            okType: 'primary',
            cancelText: '取消',
            onOk() {
                MapOperation.map_fitViewToFeature(newVal)
                makeGrid()
            },
            onCancel() {
                handleCancelDraw()
            },
        })
    }
})
// 确认了就添加格网
const makeGrid = () => {
    if (!gridStore.polygon) {
        console.log('😠 怎么可能呢,你们知道吗? polygon居然为空!')
        return
    }
    const gridGeoJson = gridMaker.makeGrid({
        polygon: gridStore.polygon,
        startCb: () => {
            console.log('开始生成格网')
        },
        endCb: () => {
            console.log('格网生成完成')
        },
        overboundCb: () => {
            message.error('格网面积超过限制, 请缩小区域范围')
            MapOperation.draw_deleteAll()
        }
    })
    if (gridGeoJson) {
        MapOperation.map_addGridLayer(gridGeoJson)
        MapOperation.draw_deleteAll()
        message.success('区域格网已加载')
        // 加载了就到tab2
        spatialTabsKey.value = '2'
    }
}
const handleRegionGridReset = () => {
    MapOperation.map_destroyGridLayer()
    MapOperation.draw_deleteAll()
    spatialTabsKey.value = '1'
    gridSearchLoading.value = false
}
/// 02 格网选择和开始检索
const gridSearchLoading = ref<boolean>(false)
const selectedGridIDs = computed(() => gridStore.selectedGrids)

const handleRemoveOneGrid = (gridID: string) => { gridStore.removeGrid(gridID) }
const handleSelectAllGrids = () => gridStore.addAllGrids()
const handleSelectNoneGrids = () => gridStore.cleadAllGrids()
const handleStartGridSearch = async () => {
    gridSearchLoading.value = true
    const gridOverlapTileMap = await queryOverlapTilesMap(selectedProduct.value!, selectedGridIDs.value)
    ezStore.set('gridOverlapTileMap', gridOverlapTileMap)
    console.log(gridOverlapTileMap)
    await calculateSearchResultInfo(gridOverlapTileMap)
    gridSearchLoading.value = false
}

//////////////////////////////////////////////////////////////
/////////////// 检索信息面板 //////////////////////////////////
const searchResultInfo = reactive({
    area: 0,
    imageCount: 0,
    dateRange: ['', ''],
    cloudRange: ['n/a', 'n/a'],
    bands: ['1', '2', '3', '4', '5', '6'],
})
const calculateSearchResultInfo = async (gridOverlapTileMap: Map<string, OverlapTileInfoView[]>) => {
    searchResultInfo.area = gridOverlapTileMap.size * 1
    let tempKey = gridOverlapTileMap.keys().next().value!
    if (!tempKey) {
        message.info('该区域没有检索到影像')
        return
    }
    let tempTileInfoViews = gridOverlapTileMap.get(tempKey)!
    let { timeRange, cloudRange } = await statisticsOverlapTileInfoView(tempTileInfoViews)
    searchResultInfo.imageCount = gridOverlapTileMap.get(tempKey)!.length
    searchResultInfo.dateRange[0] = timeRange[0]
    searchResultInfo.dateRange[1] = timeRange[1]
    searchResultInfo.cloudRange[0] = cloudRange[0].toFixed(2)
    searchResultInfo.cloudRange[1] = cloudRange[1].toFixed(2)

    tileMergeConfig.dateRange[0] = dayjs(timeRange[0])
    tileMergeConfig.dateRange[1] = dayjs(timeRange[1])
    tileMergeConfig.cloudRange[0] = Number(cloudRange[0].toFixed(2))
    tileMergeConfig.cloudRange[1] = Number(cloudRange[1].toFixed(2))

    //从一景找波段信息
    let oneSceneID = tempTileInfoViews[0].sceneId
    let bands = await fetchBandViews(oneSceneID)
    console.log('找到bands', bands)
    bandViews.value = bands

}

//////////////////////////////////////////////////////////////
/////////////// 影像聚合配置 //////////////////////////////////
const bandViews = ref<BandView[]>([])
const tileMergeConfig = reactive({
    useLatestTime: false,
    useMinCloud: false,
    dateRange: [dayjs('2010-10'), dayjs('2024-10')],
    cloudRange: [0, 100],
    bands: [] as BandView[], //基于景id拿bands
})



/////////////////////////////////////////////////////////////
/////////////// Tile Stage //////////////////////////////////


// 05 Bottom Buttons
const handleAllReset = () => {

    // reset config
    tileMergeConfig.bands = []
    tileMergeConfig.dateRange = [dayjs('2010-10'), dayjs('2024-10')]
    tileMergeConfig.cloudRange = [0, 100]
    // reset grid
    gridStore.cleadAllGrids()
    // reset map
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()
    console.log('Reset Tile Stage')
}
const mergingLoading = ref<boolean>(false)

const filterFinalGrids = (gridOverlapTileMap: Map<string, OverlapTileInfoView[]>): GridInfoView[] => {
    console.log('gridOverlapTileMap', gridOverlapTileMap)
    let finalGrids: GridInfoView[] = []
    for (let gid of gridOverlapTileMap.keys()) {
        const [columnId, rowId] = gid.split('-')
        let tiles = gridOverlapTileMap.get(gid)!
        let minCloudTile = tiles[0]
        let latestTimeTile = tiles[0]

        for (let tile of tiles) {
            // 检查时间范围是否在配置范围内
            if (tile.sceneTime) {
                let tileTime = new Date(tile.sceneTime)
                let startTime = tileMergeConfig.dateRange[0].toDate()
                let endTime = tileMergeConfig.dateRange[1].toDate()
                if (tileTime < startTime || tileTime > endTime) {
                    continue
                }
            }

            // 检查云量是否在配置范围内
            let cloudValue = parseFloat(tile.cloud)
            if (cloudValue < tileMergeConfig.cloudRange[0] || cloudValue > tileMergeConfig.cloudRange[1]) {
                continue
            }

            // 查找云量最小的tile
            if (parseFloat(tile.cloud) < parseFloat(minCloudTile.cloud)) {
                minCloudTile = tile
            }

            // 查找时间最新的tile
            if (tile.sceneTime && minCloudTile.sceneTime) {
                let tileTime = new Date(tile.sceneTime)
                let latestTime = new Date(latestTimeTile.sceneTime!)
                if (tileTime > latestTime) {
                    latestTimeTile = tile
                }
            }
        }

        // 优先使用云量最小的tile， 啥都没选就选时间最新的
        let finalTile = tileMergeConfig.useMinCloud ? minCloudTile : latestTimeTile

        finalGrids.push({
            columnId: columnId,
            rowId: rowId,
            sceneId: finalTile.sceneId,
        })
    }
    return finalGrids
}

const handleMergeDownload = async () => {
    console.log('Merge download clicked !', tileMergeConfig.bands)
    mergingLoading.value = true
    const gridOverlapTileMap = ezStore.get('gridOverlapTileMap') as Map<string, OverlapTileInfoView[]>
    const finalGrids = filterFinalGrids(gridOverlapTileMap)
    const outputFileName = 'merge_result.tif'
    const caseId = await startMergeTiles(
        tileMergeConfig.bands.map((band) => band.name),
        finalGrids,
    )

    const statusCallback = (status: string) => {
        console.log('merging status:', status)
    }
    const completeCallback = () => {
        // 合并成功后， 下载tif
        mergingLoading.value = false
        downloadMergeTiles(caseId, outputFileName)
    }
    const errorCallback = () => {
        mergingLoading.value = false
        message.error('合并tif失败, 请检查后台服务')
    }
    await intervalQueryMergingStatus(caseId, statusCallback, completeCallback, errorCallback)
}

const handleAddToProject = () => (projectModalOpen.value = true)

const projectUploadLoading = ref<boolean>(false)
const handleConfirmSelectProject = async (project: Project) => {
    projectUploadLoading.value = true
    const successCallback = () => {
        message.success('上传成功')
        projectUploadLoading.value = false
    }
    const errorCallback = () => {
        message.error('上传失败')
        projectUploadLoading.value = false
    }
    // NEED TO DO
    // await uploadTilesToProject(
    //     selectedImage.value!.id,
    //     selectedGridIDs.value,
    //     project,
    //     successCallback,
    //     errorCallback,
    // )
}


</script>

<style scoped>
.remote-sensing-panel {
    border-radius: 0.75rem;
    background-color: hsl(234, 100%, 6%);
    padding-left: 0.5rem;
    padding-right: 0.5rem;
    display: flex;
    overflow: hidden;
    transition: all 0.3s ease-in-out;
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
    position: relative;
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

.section-footer {
    display: flex;
    gap: 0.75rem;
    padding: 0.75rem 1rem;
    background-color: rgba(34, 69, 96, 0.263);
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
    transition: all 0.5s ease-in-out;
    display: flex;
    height: 125px;
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
    height: 2rem;
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
    letter-spacing: 0.05rem;
}

.custom-date-picker :deep(.ant-picker-active-bar) {
    transform: translateX(1rem);
}

/* Slider */
.custom-slider :deep(.ant-slider-mark-text) {
    color: #31a6d8;
    font-size: 0.9rem;
    padding-left: 0.5rem;
}

.custom-slider :deep(.ant-slider-rail) {
    background-color: rgba(56, 191, 248, 0.395);
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
    padding: 4px 4px;
    border-radius: 4px;
    margin-right: 0;
    font-size: 0.5rem;
    background-color: rgba(14, 165, 233, 0.15);
    border-color: rgba(56, 189, 248, 0.3);
    color: #e0f2fe;
    text-overflow: ellipsis;
    white-space: nowrap;
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
    gap: 0.5rem;
    margin-bottom: 1rem;
}

.band-radio-button {
    min-width: 3.5rem;
    max-width: 3.5rem;
    flex: 1;
    text-align: center;
    transition: all 0.2s ease;
}

.band-radio-group :deep(.ant-checkbox-wrapper) {
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

.band-radio-group :deep(.ant-checkbox-wrapper:hover) {
    color: #e0f2fe;
    background-color: rgba(14, 165, 233, 0.1);
    border: none;
    box-shadow: 0 0 5px rgba(56, 189, 248, 0.2);
}

.band-radio-group :deep(.ant-checkbox-wrapper-checked) {
    background-color: rgba(4, 167, 242, 0.733);
    border: none;
    color: #c7eeff;
    box-shadow: 0 0 10px rgba(56, 191, 248, 0.604) inset;
    font-weight: 500;
}

.band-radio-group :deep(.ant-checkbox-wrapper-checked:hover) {
    background-color: rgba(14, 165, 233, 0.3);
    border: none;
    color: #38bdf8;
}

.band-radio-group :deep(.ant-checkbox-wrapper-checked:not(.ant-checkbox-wrapper-disabled):hover) {
    color: #38bdf8;
    background-color: rgba(14, 165, 233, 0.3);
    border: none;
}

.band-radio-group :deep(.ant-checkbox-wrapper-checked:not(.ant-checkbox-wrapper-disabled):active) {
    color: #38bdf8;
    background-color: rgba(14, 165, 233, 0.4);
    border: none;
}

.band-radio-group :deep(.ant-checkbox-wrapper-checked:not(.ant-checkbox-wrapper-disabled)::before) {
    background-color: #000000;
    border: none;
}

.block-area {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    justify-content: center;
    column-gap: 0.8rem;
    padding: 1rem 1rem;
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

/* 影像检索结果信息样式 */
.result-info-container {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 1rem;
    padding: 0.5rem;
}

.result-info-item {
    display: flex;
    align-items: center;
    background-color: rgba(15, 23, 42, 0.4);
    border-radius: 0.5rem;
    padding: 0.75rem;
    border: 1px solid rgba(56, 189, 248, 0.2);
    transition: all 0.3s ease;
}

.result-info-item:hover {
    background-color: rgba(14, 165, 233, 0.1);
    border-color: rgba(56, 189, 248, 0.4);
    box-shadow: 0 0 10px rgba(56, 189, 248, 0.15);
    transform: translateY(-2px);
}

.result-info-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background-color: rgba(56, 189, 248, 0.15);
    margin-right: 0.75rem;
    color: #38bdf8;
    flex-shrink: 0;
}

.result-info-content {
    display: flex;
    flex-direction: column;
}

.result-info-label {
    font-size: 0.8rem;
    color: #94a3b8;
    margin-bottom: 0.25rem;
}

.result-info-value {
    font-size: 1rem;
    font-weight: 600;
    color: #e0f2fe;
}

.date-range {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
}

.date-item {
    font-size: 0.9rem;
    color: #e0f2fe;
    display: flex;
    align-items: center;
}

.config-container {
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
    padding: 0.5rem;
}

.config-item {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    background-color: rgba(15, 23, 42, 0.3);
    border-radius: 0.5rem;
    padding: 0.75rem;
    border: 1px solid rgba(56, 189, 248, 0.15);
    transition: all 0.3s ease;
}

.config-item:hover {
    background-color: rgba(14, 165, 233, 0.1);
    border-color: rgba(56, 189, 248, 0.3);
    box-shadow: 0 0 10px rgba(56, 189, 248, 0.1);
}

.config-label {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.9rem;
    font-weight: 600;
    color: #e0f2fe;
    margin-bottom: 0.25rem;
}

.config-control {
    display: flex;
    align-items: center;
    width: 100%;
}

.cloud-slider-container {
    display: flex;
    align-items: center;
    width: 100%;
    gap: 0.75rem;
}

.cloud-value {
    font-size: 0.85rem;
    color: #94a3b8;
    min-width: 2.5rem;
    text-align: center;
}

.slider-wrapper {
    flex: 1;
    padding: 0 0.5rem;
}

.config-icon {
    color: #38bdf8;
    background-color: rgba(56, 189, 248, 0.1);
    padding: 0.25rem;
    border-radius: 0.25rem;
}
</style>
