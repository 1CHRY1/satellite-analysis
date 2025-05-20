<template>
    <div class="custom-panel px-2">
        <dv-border-box12 class="!h-[calc(100vh-56px-48px-32px-8px)]">
            <div class="main-container">
                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <MapPinIcon :size="18" />
                        </div>
                        <h2 class="section-title">行政区划与格网分辨率</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div class="config-item">
                                <div class="config-label relative">
                                    <MapIcon :size="16" class="config-icon" />
                                    <span>行政区</span>
                                </div>
                                <div class="config-control justify-center">
                                    <RegionSelects
                                        v-model="region"
                                        :placeholder="['选择省份', '选择城市', '选择区县']"
                                        class="flex gap-2"
                                        select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none"
                                    />
                                </div>
                            </div>
                            <div class="config-item">
                                <div class="config-label relative">
                                    <BoltIcon :size="16" class="config-icon" />
                                    <span>格网分辨率</span>
                                </div>
                                <div class="config-control flex-col !items-start">
                                    <div>
                                        格网分辨率选择：
                                        <select
                                            v-model="selectedRadius"
                                            class="w-40 appearance-none rounded-lg border border-[#2c3e50] bg-[#0d1526] px-4 py-2 pr-8 text-white transition-all duration-200 hover:border-[#206d93] focus:border-[#3b82f6] focus:outline-none"
                                        >
                                            <option
                                                v-for="option in radiusOptions"
                                                :key="option"
                                                :value="option"
                                                class="bg-[#0d1526] text-white"
                                            >
                                                {{ option }}km
                                            </option>
                                        </select>
                                    </div>
                                    <div class="flex flex-row">
                                        <div class="text-red-500">*</div>
                                        建议省级行政单位格网分辨率不小于20km
                                    </div>
                                </div>
                            </div>
                            <button
                                @click="getAllGrid"
                                class="cursor-pointer rounded-lg border border-[#2c3e50] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95"
                            >
                                获取格网
                            </button>
                        </div>
                    </div>
                </section>
                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <MapPinIcon :size="18" />
                        </div>
                        <h2 class="section-title">时间与最大云量</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div class="config-item">
                                <div class="config-label relative">
                                    <CalendarIcon :size="16" class="config-icon" />
                                    <span>时间范围</span>
                                    <a-checkbox
                                        v-model:checked="tileMergeConfig.useLatestTime"
                                        class="absolute right-1 !text-sky-300"
                                    >
                                        时间最近优先
                                    </a-checkbox>
                                </div>
                                <div class="config-control">
                                    <a-range-picker
                                        class="custom-date-picker"
                                        v-model:value="tileMergeConfig.dateRange"
                                        picker="day"
                                        :allow-clear="false"
                                        :placeholder="['开始日期', '结束日期']"
                                    />
                                </div>
                            </div>
                            <div class="config-item">
                                <div class="config-label relative">
                                    <CloudIcon :size="16" class="config-icon" />
                                    <span>最大云量限度</span>
                                    <a-checkbox
                                        v-model:checked="tileMergeConfig.useMinCloud"
                                        class="absolute right-1 !text-sky-300"
                                    >
                                        云量最小优先
                                    </a-checkbox>
                                </div>
                                <div class="config-control">
                                    <div class="cloud-slider-container">
                                        <span class="cloud-value"
                                            >{{ tileMergeConfig.cloudRange[0] }}%</span
                                        >
                                        <div class="slider-wrapper">
                                            <a-slider
                                                class="custom-slider"
                                                range
                                                v-model:value="tileMergeConfig.cloudRange"
                                                :tipFormatter="(value: number) => value + '%'"
                                            />
                                        </div>
                                        <span class="cloud-value"
                                            >{{ tileMergeConfig.cloudRange[1] }}%</span
                                        >
                                    </div>
                                </div>
                            </div>
                            <button
                                @click="filterByCloudAndDate"
                                :disabled="filterByCloudAndDateLoading"
                                class="flex justify-center rounded-lg border border-[#2c3e50] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95"
                                :class="{
                                    'cursor-not-allowed': filterByCloudAndDateLoading,
                                    'cursor-pointer': !filterByCloudAndDateLoading,
                                }"
                            >
                                <span>影像筛选 </span>
                                <Loader v-if="filterByCloudAndDateLoading" class="ml-2" />
                            </button>
                            <div class="config-item">
                                <div class="config-label relative">
                                    <BoltIcon :size="16" class="config-icon" />
                                    <span>统计信息</span>
                                </div>
                                <div class="config-control flex-col gap-4">
                                    <div class="result-info-container">
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <MapIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">行政区划编码</div>
                                                <div class="result-info-value">
                                                    {{ displayLabel }}
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <MapIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">格网分辨率</div>
                                                <div class="result-info-value">
                                                    {{ selectedRadius }}km
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <ImageIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">云量</div>
                                                <div class="result-info-value">
                                                    {{ tileMergeConfig.cloudRange[0] }}% ~
                                                    {{ tileMergeConfig.cloudRange[1] }}%
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CalendarIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">涵盖时间范围</div>
                                                <div class="result-info-value date-range">
                                                    <div class="date-item">
                                                        {{
                                                            formatTime(
                                                                tileMergeConfig.dateRange[0],
                                                                'day',
                                                            )
                                                        }}~
                                                        {{
                                                            formatTime(
                                                                tileMergeConfig.dateRange[1],
                                                                'day',
                                                            )
                                                        }}
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">当前已检索到</div>
                                                <div class="result-info-value">
                                                    {{ allScenes.length }}景影像
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">影像覆盖率</div>
                                                <div class="result-info-value">
                                                    {{
                                                        coverageRate != 'NaN%'
                                                            ? coverageRate
                                                            : '待计算'
                                                    }}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>
                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <DatabaseIcon :size="18" />
                        </div>
                        <h2 class="section-title">交互探索</h2>
                        <div class="section-icon absolute right-0" @click="clearAllShowingSensor">
                            <a-tooltip>
                                <template #title>清空影像图层</template>
                                <Trash2Icon :size="18" />
                            </a-tooltip>
                        </div>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <!-- 三级筛选 -->
                            <div class="config-item">
                                <div class="config-label relative">
                                    <BoltIcon :size="16" class="config-icon" />
                                    <span>数据来源</span>
                                </div>
                                <div class="config-control gap-6">
                                    <button
                                        v-for="label in buttonGroups[0]"
                                        :key="label"
                                        @click="(toggleButton(label), filterByTags())"
                                        class="cursor-pointer rounded-lg border px-4 py-1 transition-all duration-200 active:scale-90"
                                        :class="[
                                            isActive(label)
                                                ? 'border-[#2bb2ff] bg-[#0d2e4b] text-white'
                                                : 'border-[#475569] bg-transparent text-[#94a3b8] hover:border-[#2bb2ff] hover:bg-[#1e293b]',
                                        ]"
                                    >
                                        {{ label }}
                                    </button>
                                </div>
                            </div>
                            <div class="config-item">
                                <div class="config-label relative">
                                    <BoltIcon :size="16" class="config-icon" />
                                    <span>传感器类型</span>
                                </div>
                                <div class="config-control gap-4">
                                    <button
                                        v-for="label in buttonGroups[1]"
                                        :key="label"
                                        @click="(toggleButton(label), filterByTags())"
                                        class="cursor-pointer rounded-lg border px-4 py-1 transition-all duration-200 active:scale-90"
                                        :class="[
                                            isActive(label)
                                                ? 'border-[#2bb2ff] bg-[#0d2e4b] text-white'
                                                : 'border-[#475569] bg-transparent text-[#94a3b8] hover:border-[#2bb2ff] hover:bg-[#1e293b]',
                                        ]"
                                    >
                                        {{ label }}
                                    </button>
                                </div>
                            </div>
                            <!-- 检索后的统计信息 -->
                            <div class="config-item">
                                <div class="config-label relative">
                                    <BoltIcon :size="16" class="config-icon" />
                                    <span>统计信息</span>
                                </div>
                                <div class="config-control flex-col gap-4">
                                    <div
                                        v-for="(sensorItem, index) in filteredSensorsItems"
                                        class="config-item w-full"
                                    >
                                        <div>传感器名称及分辨率：{{ sensorItem.key }}</div>
                                        <div>类型：{{ imageType(sensorItem.tags) }}</div>
                                        <div class="result-info-container">
                                            <div class="result-info-item">
                                                <div class="result-info-icon">
                                                    <CloudIcon :size="16" />
                                                </div>
                                                <div class="result-info-content">
                                                    <div class="result-info-label">包含</div>
                                                    <div class="result-info-value">
                                                        {{ sensorItem.sceneCount }}景影像
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="result-info-item">
                                                <div class="result-info-icon">
                                                    <CloudIcon :size="16" />
                                                </div>
                                                <div class="result-info-content">
                                                    <div class="result-info-label">影像覆盖率</div>
                                                    <div class="result-info-value">
                                                        {{
                                                            sensorItem.coveredCount != 'NaN%'
                                                                ? (
                                                                      (sensorItem.coveredCount *
                                                                          100) /
                                                                      allGridCount
                                                                  ).toFixed(2) + '%'
                                                                : '待计算'
                                                        }}
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                        <div>
                                            <label class="mr-2 text-white">选择影像：</label>
                                            <select
                                                @change="
                                                    (e) =>
                                                        showImageBySensorAndSelect(
                                                            sensorItem,
                                                            (e.target as HTMLSelectElement).value,
                                                        )
                                                "
                                                class="max-h-[600px] max-w-[calc(100%-90px)] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none"
                                            >
                                                <option disabled selected value="">
                                                    请选择影像
                                                </option>
                                                <option
                                                    v-for="sceneName in sensorItem.sceneNames"
                                                    :key="sceneName"
                                                    :value="sceneName"
                                                    class="truncate"
                                                >
                                                    {{ sceneName }}
                                                </option>
                                            </select>
                                        </div>
                                        <!-- Band Stretch Inputs -->
                                        <div v-show="sensorItem.selectedSceneInfo" class="mt-4">
                                            <div class="mr-1 grid grid-cols-[1fr_2fr]">
                                                <span class="text-white">亮度拉伸:</span>
                                                <a-slider
                                                    :tip-formatter="scaleRateFormatter"
                                                    v-model:value="sensorItem.scaleRate"
                                                    @afterChange="onAfterScaleRateChange"
                                                />
                                            </div>
                                            <div>
                                                <!-- <div class="bg-[#1b42528a] cursor-pointer text-white border border-[#2c3e50] rounded-lg px-4 py-1 hover:bg-[#1a2b4c] hover:border-[#2bb2ff] transition-all duration-200 active:scale-95 text-center mt-4"
                                                    @click="handleShowImage(sensorItem)">
                                                    影像可视化</div> -->
                                                <a-button
                                                    class="custom-button w-full!"
                                                    :loading="sensorItem.loading"
                                                    @click="handleShowImage(sensorItem)"
                                                >
                                                    影像可视化
                                                </a-button>
                                            </div>
                                        </div>
                                    </div>
                                    <!-- <div class="result-info-container">
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <MapIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">行政区划编码</div>
                                                <div class="result-info-value">{{ displayLabel }}
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <MapIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">格网分辨率</div>
                                                <div class="result-info-value">{{ selectedRadius }}km
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <ImageIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">云量</div>
                                                <div class="result-info-value"> {{ tileMergeConfig.cloudRange[0] }}% ~
                                                    {{ tileMergeConfig.cloudRange[1] }}%
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CalendarIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">涵盖时间范围</div>
                                                <div class="result-info-value date-range">
                                                    <div class="date-item">{{ formatTime(tileMergeConfig.dateRange[0],
                                                        'day')
                                                        }}~
                                                        {{ formatTime(tileMergeConfig.dateRange[1], 'day')
                                                        }}</div>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">当前已检索到</div>
                                                <div class="result-info-value">{{ filteredImages.length }}景影像</div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">影像覆盖率</div>
                                                <div class="result-info-value">{{ coverageRate }}</div>
                                            </div>
                                        </div>
                                    </div> -->
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
import { ref, computed, type Ref, watch, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import { RegionSelects } from 'v-region'
import type { RegionValues } from 'v-region'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import { formatTime } from '@/util/common'
import {
    getGridByRegionAndResolution,
    getBoundary,
    getRegionPosition,
    getSceneByConfig,
    getSceneGrids,
} from '@/api/http/satellite-data'
import * as MapOperation from '@/util/map/operation'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import { ezStore } from '@/store'
import { getSceneGeojson, getTifbandMinMax } from '@/api/http/satellite-data/visualize.api'

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
    Cloud,
    Images,
    Loader,
    Trash2Icon,
} from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { message } from 'ant-design-vue'
const emit = defineEmits(['submitConfig'])

/**
 * 行政区划选取
 */

const radiusOptions = [2, 5, 10, 15, 20, 25, 30, 40, 50, 80, 100, 150]
const selectedRadius = ref(20)
const tileMergeConfig = ref({
    useLatestTime: false,
    useMinCloud: false,
    dateRange: [dayjs('2010-10'), dayjs('2025-05')],
    cloudRange: [0, 100],
})
const region = ref<RegionValues>({
    province: '110000',
    city: '',
    area: '',
})
const allGrids = ref([])
const allGridCount = ref(0)
const currentCityBounds = ref([])
// 计算到了哪一级行政单位
const displayLabel = computed(() => {
    let info = region.value
    if (info.area) return Number(`${info.area}`)
    if (info.city) return Number(`${info.city}`)
    if (info.province) return Number(`${info.province}`)
    return '未选择'
})
// 获取格网数据
const getAllGrid = async () => {
    if (displayLabel.value === '未选择') {
        ElMessage.warning('请选择行政区')
        return
    }

    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    let boundaryRes = await getBoundary(displayLabel.value)
    let gridRes = await getGridByRegionAndResolution(displayLabel.value, selectedRadius.value)
    allGrids.value = gridRes
    allGridCount.value = gridRes.length

    let window = await getRegionPosition(displayLabel.value)
    currentCityBounds.value = boundaryRes
    // 先清除现有的矢量边界，然后再添加新的
    MapOperation.map_addPolygonLayer({
        geoJson: boundaryRes,
        id: 'UniqueLayer',
        lineColor: '#8fffff',
        fillColor: '#a4ffff',
        fillOpacity: 0.2,
    })
    // 渲染网格数据
    let gridFeature: FeatureCollection = {
        type: 'FeatureCollection',
        features: gridRes.map((item, index) => {
            return {
                type: 'Feature',
                geometry: item.boundary.geometry as Geometry,
                properties: {
                    ...(item.properties || {}),
                    id: item.properties?.id ?? index, // 确保每个都有 id
                },
            }
        }),
    }

    MapOperation.map_addGridLayer(gridFeature)
    MapOperation.draw_deleteAll()
    // fly to
    MapOperation.map_fitView([
        [window.bounds[0], window.bounds[1]],
        [window.bounds[2], window.bounds[3]],
    ])
}

/**
 * 用云量和日期初步获取影像数据
 */
const allScenes = ref<any>([])
const allSensorsItems = ref<any>([])
const filterByCloudAndDateLoading = ref(false)
// const allFilteredImages = ref<any>([])
const filterByCloudAndDate = async () => {
    if (displayLabel.value === '未选择') {
        ElMessage.warning('请先选择行政区并获取格网')
        return
    } else if (allGrids.value.length === 0) {
        ElMessage.warning('请先获取格网')
        return
    }
    // 先禁止按钮，渲染loading状态
    filterByCloudAndDateLoading.value = true
    let filterData = {
        startTime: tileMergeConfig.value.dateRange[0].format('YYYY-MM-DD'),
        endTime: tileMergeConfig.value.dateRange[1].format('YYYY-MM-DD'),
        cloud: tileMergeConfig.value.cloudRange[1],
        regionId: displayLabel.value,
    }
    // allFilteredImages.value = await getSceneByConfig(filterData)
    allScenes.value = (await getSceneByConfig(filterData)).map((image) => {
        return {
            ...image,
            tags: [image.tags.source, image.tags.production, image.tags.category],
        }
    })

    // 记录所有景中含有的“传感器+分辨率字段”
    allSensorsItems.value = getSensorsAndResolutions(allScenes.value)

    await makeFullSceneGrid()

    if (allScenes.value.length === 0) {
        ElMessage.warning('未筛选出符合要求的影像，请重新设置条件')
    } else {
        ElMessage.success(`已检索到${allScenes.value.length}景影像，请进一步筛选您所需的影像`)
    }

    // 根据所有的传感器+分辨率，找出每个格网上，在特定传感器+分辨率情况下，有多少景影像
    allSensorsItems.value = countSensorsCoverage(
        allSensorsItems.value,
        ezStore.get('sceneGridsRes'),
    )
    // 刚拿到的时候根据默认tags先分类一次
    filterByTags()

    // 恢复状态
    filterByCloudAndDateLoading.value = false
}

// 数各种传感器分别覆盖了多少格网
const countSensorsCoverage = (
    allSensorsItems: { key: string; tags: string[] }[],
    allGridScene: any[],
) => {
    // 初始化统计对象
    const coverageMap: Record<string, number> = {}

    // 遍历每个 grid，提取唯一 sensorName-resolution 的组合
    for (const grid of allGridScene) {
        if (!grid.scenes || grid.scenes.length === 0) continue

        const sensorResSet = new Set<string>()
        for (const scene of grid.scenes) {
            const key = `${scene.sensorName}-${scene.resolution}`
            sensorResSet.add(key)
        }

        // 每种 sensor-res 只计一次
        for (const key of sensorResSet) {
            coverageMap[key] = (coverageMap[key] || 0) + 1
        }
    }

    // 将 coveredCount 添加到 allSensorsItems 中
    const updatedSensors = allSensorsItems.map((item) => ({
        ...item,
        coveredCount: coverageMap[item.key] || 0,
    }))

    return updatedSensors
}

// 给按标签分类用的方法，在获取allScene的时候就要按传感器和分辨率分类，先知道有哪些分类。
const getSensorsAndResolutions = (scenes: any[]) => {
    const map = new Map<
        string,
        { tags: string[]; sceneCount: number; sceneNames: string[]; sceneIds: string[] }
    >()

    scenes.forEach((scene) => {
        const key = `${scene.sensorName}-${scene.resolution}`
        if (!map.has(key)) {
            map.set(key, {
                tags: scene.tags.slice(0, 2),
                sceneCount: 1,
                sceneNames: [scene.sceneName],
                sceneIds: [scene.sceneId],
            })
        } else {
            map.get(key)!.sceneCount += 1
            map.get(key)!.sceneNames.push(scene.sceneName)
            map.get(key)!.sceneIds.push(scene.sceneId)
        }
    })
    // 转换为数组
    return Array.from(map.entries()).map(([key, value]) => ({
        key,
        tags: value.tags,
        sceneCount: value.sceneCount,
        sceneNames: value.sceneNames,
        sceneIds: value.sceneIds,
    }))
}

const makeFullSceneGrid = async () => {
    let sceneGridParam = {
        grids: allGrids.value.map((item: any) => {
            return {
                rowId: item.rowId,
                columnId: item.columnId,
                resolution: item.resolution,
            }
        }),
        sceneIds: allScenes.value.map((image: any) => image.sceneId),
    }

    // Destroy layer
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    // Get scene grids
    let sceneGridsRes = await getSceneGrids(sceneGridParam)
    let scenes = allScenes.value

    const sceneTagMap = new Map<string, string[]>()
    for (let sc of scenes) {
        sceneTagMap.set(sc.sceneId, sc.tags)
    }
    ezStore.set('sceneTagMap', sceneTagMap)

    console.log(scenes)

    // Plus sceneGridsRes
    for (let i = 0; i < sceneGridsRes.length; i++) {
        let grid = sceneGridsRes[i]
        grid.international = 0
        grid.national = 0
        grid.light = 0
        grid.radar = 0
        grid.traditional = 0
        grid.ard = 0

        let scenes = sceneGridsRes[i].scenes
        for (let scene of scenes) {
            const scTag = sceneTagMap.get(scene.sceneId) as string[]
            for (let tag of scTag) {
                if (grid[tag] === undefined) console.log('未知tag: ', tag)
                grid[tag] = grid[tag] + 1
            }
        }
    }
    ezStore.set('sceneGridsRes', sceneGridsRes)

    // 算覆盖率
    const nonEmptyScenesCount = sceneGridsRes.filter((item) => item.scenes.length > 0).length
    coverageRate.value = ((nonEmptyScenesCount * 100) / sceneGridsRes.length).toFixed(2) + '%'

    let gridFeature: FeatureCollection = {
        type: 'FeatureCollection',
        features: allGrids.value.map((item: any, index: number) => {
            return {
                type: 'Feature',
                geometry: item.boundary.geometry as Geometry,
                properties: {
                    ...(item.properties || {}),
                    id: item.properties?.id ?? index, // 确保每个都有 id
                    opacity: judgeGridOpacity(index, sceneGridsRes),
                    rowId: item.rowId,
                    columnId: item.columnId,
                    resolution: item.resolution,
                    flag: true, // flag means its time to trigger the visual effect
                    international: sceneGridsRes[index].international,
                    national: sceneGridsRes[index].national,
                    light: sceneGridsRes[index].light,
                    radar: sceneGridsRes[index].radar,
                    traditional: sceneGridsRes[index].traditional,
                    ard: sceneGridsRes[index].ard,
                },
            }
        }),
    }
    MapOperation.map_destroyGridLayer()
    MapOperation.map_addGridLayer_coverOpacity(gridFeature)
    MapOperation.draw_deleteAll()

    emit('submitConfig', {
        regionCode: displayLabel.value,
        dataRange: [...tileMergeConfig.value.dateRange],
        cloud: tileMergeConfig.value.cloudRange[1],
        space: selectedRadius.value,
        coverage: coverageRate.value,
        images: allScenes.value,
        grids: allGrids.value,
        boundary: currentCityBounds.value,
    })
}

/**
 * 标签筛选
 */

const buttonGroups = [
    ['国产影像', '国外影像'],
    ['光学影像', 'SAR影像'],
    ['原始影像', 'ARD影像'],
]
// 存储已激活的按钮标签
const activeImgTags = ref<Set<string>>(new Set(['国产影像', '光学影像']))
// 切换按钮选中状态
const toggleButton = (label: string) => {
    if (activeImgTags.value.has(label)) {
        activeImgTags.value.delete(label)
    } else {
        activeImgTags.value.add(label)
    }
}

// 判断按钮是否被选中
const isActive = (label: string) => activeImgTags.value.has(label)

const tagMap: Record<string, string> = {
    国产影像: 'national',
    国外影像: 'international',
    光学影像: 'light',
    SAR影像: 'radar',
    原始影像: 'traditional',
    ARD影像: 'ard',
}

// const filteredImages: Ref<any[]> = ref([])
const coverageRate = ref('0.00%')
const filteredSensorsItems = ref<any>([])

// 点击标签自动分类
const filterByTags = async () => {
    // 1. 将 activeImgTags 映射成英文 tag Set
    const mappedTags = new Set([...activeImgTags.value].map((tag) => tagMap[tag]).filter(Boolean))

    let res: any = []
    // 2.筛选：每个 item 的 tags 都在 mappedTags 中, 并且给每个加个初始scaleRate，不改结构只能这么做了
    allSensorsItems.value.forEach((item: any) => {
        if (item.tags.every((tag: string) => mappedTags.has(tag))) {
            res.push({
                ...item,
                scaleRate: 50,
            })
        }
    })
    filteredSensorsItems.value = res

    // if (!verifyFilterByTags()) {
    //     return
    // }
}

// 根据tags返回类型
const imageType = (tags: string[]) => {
    let type = ''
    if (tags.includes('national')) {
        type += '国产'
    } else {
        type += '国外'
    }
    if (tags.includes('light')) {
        type += '光学影像'
    } else {
        type += 'SAR影像'
    }
    return type
}

/**
 * 左下的影像可视化
 */

const scaleRateFormatter = (value: number) => {
    return `${value}%`
}
const showingImageStrech = reactive({
    r_min: 0,
    r_max: 5000,
    g_min: 0,
    g_max: 5000,
    b_min: 0,
    b_max: 5000,
})
const onAfterScaleRateChange = (scale_rate: number) => {
    console.log(scale_rate)
}
const showImageBySensorAndSelect = async (image: any, imageName: string) => {
    if (imageName === '') {
        console.log('搞毛啊，能选出空值来？')
        ElMessage.warning('请选择有效影像')
        return
    }
    console.log('传入的', image)
    let allGridScene = ezStore.get('sceneGridsRes')
    console.log('所有格子的景', allGridScene)
    const imageId = image.sceneIds[image.sceneNames.indexOf(imageName)]
    const sceneInfo = allGridScene
        .flatMap((grid) => grid.scenes || [])
        .find((scene) => scene.sceneId === imageId)
    console.log('selectedSceneInfo ', sceneInfo)
    image.selectedSceneInfo = sceneInfo

    let geojson = await getSceneGeojson(sceneInfo)
    MapOperation.map_addSceneBoxLayer(geojson)
}
const clearAllShowingSensor = () => {
    MapOperation.map_destroyRGBImageTileLayer()
    MapOperation.map_destroySceneBoxLayer()
}
const handleShowImage = async (sensorItem) => {
    const stopLoading = message.loading('正在加载影像')
    let redPath, greenPath, bluePath
    const sceneInfo = sensorItem.selectedSceneInfo
    console.log(sceneInfo.bandMapper)
    sensorItem.loading = true
    for (let bandImg of sceneInfo.images) {
        // 后端返回的BandMapper如果是单波段的话，Red Green 和 Blue相同
        if (sceneInfo.bandMapper.Red === bandImg.band) {
            redPath = bandImg.bucket + '/' + bandImg.tifPath
        }
        if (sceneInfo.bandMapper.Green === bandImg.band) {
            greenPath = bandImg.bucket + '/' + bandImg.tifPath
        }
        if (sceneInfo.bandMapper.Blue === bandImg.band) {
            bluePath = bandImg.bucket + '/' + bandImg.tifPath
        }
    }

    const cache = ezStore.get('statisticCache')
    const promises: any = []
    let [min_r, max_r, min_g, max_g, min_b, max_b] = [0, 0, 0, 0, 0, 0]

    if (cache.get(redPath) && cache.get(greenPath) && cache.get(bluePath)) {
        console.log('cache hit!')
        ;[min_r, max_r] = cache.get(redPath)
        ;[min_g, max_g] = cache.get(greenPath)
        ;[min_b, max_b] = cache.get(bluePath)
    } else {
        promises.push(
            getTifbandMinMax(redPath),
            getTifbandMinMax(greenPath),
            getTifbandMinMax(bluePath),
        )
        await Promise.all(promises).then((values) => {
            min_r = values[0][0]
            max_r = values[0][1]
            min_g = values[1][0]
            max_g = values[1][1]
            min_b = values[2][0]
            max_b = values[2][1]
        })

        cache.set(redPath, [min_r, max_r])
        cache.set(greenPath, [min_g, max_g])
        cache.set(bluePath, [min_b, max_b])
    }

    console.log(min_r, max_r, min_g, max_g, min_b, max_b)
    console.log(sensorItem.scaleRate)

    const scaleRate = 1.0 - sensorItem.scaleRate / 100
    // 基于 scale rate 进行拉伸
    showingImageStrech.r_min = Math.round(min_r)
    showingImageStrech.r_max = Math.round(min_r + (max_r - min_r) * scaleRate)
    showingImageStrech.g_min = Math.round(min_g)
    showingImageStrech.g_max = Math.round(min_g + (max_g - min_g) * scaleRate)
    showingImageStrech.b_min = Math.round(min_b)
    showingImageStrech.b_max = Math.round(min_b + (max_b - min_b) * scaleRate)

    // 添加图层, 如果存在会先删除再添加
    MapOperation.map_addRGBImageTileLayer(
        {
            redPath,
            greenPath,
            bluePath,
            ...showingImageStrech,
        },
        stopLoading,
    )

    setTimeout(() => {
        sensorItem.loading = false
    }, 1000)
}

// 基于覆盖度返回opacity
const judgeGridOpacity = (index: number, sceneGridsRes: any) => {
    let opacity = 0.01
    const totalImgLenght = allScenes.value.length
    opacity = (sceneGridsRes[index].scenes.length / totalImgLenght) * 0.3
    return opacity
}
// 卫语句
const verifyFilterByTags = () => {
    let buttons = activeImgTags.value
    if (buttons.size === 0) {
        ElMessage.warning('请设置筛选条件')
        return false
    }
    if (allScenes.value.length === 0) {
        ElMessage.warning('请先进行影像筛选')
        return false
    }
    if (!buttons.has('国产影像') && !buttons.has('国外影像')) {
        ElMessage.warning('请选择您需要的数据来源')
        return false
    }
    if (!buttons.has('光学影像') && !buttons.has('SAR影像')) {
        ElMessage.warning('请选择您需要的传感器类型')
        return false
    }
    if (!buttons.has('原始影像') && !buttons.has('ARD影像')) {
        ElMessage.warning('请选择您需要的数据级别')
        return false
    }
    return true
}

onMounted(() => {
    if (!ezStore.get('statisticCache')) ezStore.set('statisticCache', new Map())
})
</script>

<style scoped src="./tabStyle.css"></style>
