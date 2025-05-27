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
                            <div class="config-item w-full">
                                <div class="config-label relative">
                                    <MapIcon :size="16" class="config-icon" />
                                    <span>研究区选择</span>
                                </div>
                                <!-- <div class="config-control justify-center">
                                    <RegionSelects v-model="region" :placeholder="['选择省份', '选择城市', '选择区县']"
                                        class="flex gap-2"
                                        select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
                                </div> -->
                                <div class="flex border-b border-[#2c3e50] mb-4">
                                    <button v-for="tab in tabs" :key="tab.value" @click="activeTab = tab.value" :class="[
                                        'flex-1 text-center px-4 py-2 text-sm font-medium ',
                                        activeTab === tab.value
                                            ? 'border-b-2 border-[#38bdf8] text-[#38bdf8]'
                                            : 'text-gray-400 hover:text-[#38bdf8]'
                                    ]">
                                        {{ tab.label }}
                                    </button>
                                </div>
                                <div v-if="activeTab === 'region'" class="config-control justify-center">
                                    <RegionSelects v-model="region" :placeholder="['选择省份', '选择城市', '选择区县']"
                                        class="flex gap-2"
                                        select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
                                </div>
                                <div v-else-if="activeTab === 'poi'" class="config-control justify-center w-full">
                                    <el-select v-model="selectedPOI" filterable remote reserve-keyword value-key="id"
                                        placeholder="请输入 POI 关键词" :remote-method="fetchPOIOptions"
                                        class="!w-[90%] bg-[#0d1526] text-white" popper-class="bg-[#0d1526] text-white">
                                        <el-option v-for="item in poiOptions" :key="item.id"
                                            :label="item.name + '(' + item.pname + item.cityname + item.adname + item.address + ')'"
                                            :value="item" />
                                    </el-select>
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
                                        <select v-model="selectedRadius"
                                            class="w-40 appearance-none rounded-lg border border-[#2c3e50] bg-[#0d1526] px-4 py-2 pr-8 text-white transition-all duration-200 hover:border-[#206d93] focus:border-[#3b82f6] focus:outline-none">
                                            <option v-for="option in radiusOptions" :key="option" :value="option"
                                                class="bg-[#0d1526] text-white">
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
                            <button @click="getAllGrid"
                                class="cursor-pointer rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95">
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
                        <h2 class="section-title">按时间筛选</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div class="config-item">
                                <div class="config-label relative">
                                    <CalendarIcon :size="16" class="config-icon" />
                                    <span>时间范围</span>
                                </div>
                                <div class="config-control">
                                    <a-range-picker class="custom-date-picker" v-model:value="tileMergeConfig.dateRange"
                                        picker="day" :allow-clear="false" :placeholder="['开始日期', '结束日期']" />
                                </div>
                            </div>
                            <!-- <div class="config-item">
                                <div class="config-label relative">
                                    <CloudIcon :size="16" class="config-icon" />
                                    <span>最大云量限度</span>
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
                            </div> -->
                            <button @click="filterByCloudAndDate" :disabled="filterByCloudAndDateLoading"
                                class="flex justify-center rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95"
                                :class="{
                                    'cursor-not-allowed': filterByCloudAndDateLoading,
                                    'cursor-pointer': !filterByCloudAndDateLoading,
                                }">
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
                                                <div class="result-info-label">格网分辨率</div>
                                                <div class="result-info-value">
                                                    {{ selectedRadius }}km
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
                        <div class="section-icon absolute right-0 cursor-pointer" @click="clearAllShowingSensor">
                            <a-tooltip>
                                <template #title>清空影像图层</template>
                                <Trash2Icon :size="18" />
                            </a-tooltip>
                        </div>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div class="config-item" v-for="([label, value], index) in resolutionType" :key="value">
                                <div class="config-label relative">
                                    <BoltIcon :size="16" class="config-icon" />
                                    <span>{{ label }}分辨率影像集</span>
                                </div>
                                <div class="config-control flex w-full flex-col gap-4">
                                    <div class="result-info-container w-full">
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">包含</div>
                                                <div class="result-info-value">
                                                    {{ getSceneCountByResolution(value) }}景影像
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
                                                        allSensorsItems[label]
                                                            ? (
                                                                (allSensorsItems[label] * 100) /
                                                                allGridCount
                                                            ).toFixed(2) + '%'
                                                            : '待计算'
                                                    }}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div v-if="Object.keys(classifiedScenes).length > 0" class="!w-full">
                                        <label class="mr-2 text-white">选择传感器：</label>
                                        <select
                                            class="max-h-[600px] w-[calc(100%-113px)] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none"
                                            v-model="resolutionPlatformSensor[label]">
                                            <option disabled selected value="">请选择</option>
                                            <!-- <option :value="'all'" class="truncate">全选</option> -->
                                            <option v-for="platformName in classifiedScenes[
                                                value + 'm'
                                            ]" :value="platformName" :key="platformName" class="truncate">
                                                {{ platformName }}
                                            </option>
                                        </select>
                                        <div class="flex flex-row items-center">
                                            <a-button class="custom-button mt-4! w-[calc(100%-50px)]!"
                                                @click="handleShowResolutionSensorImage(label)"
                                                :disabled="!resolutionPlatformSensor[label]">
                                                影像可视化
                                            </a-button>
                                            <a-tooltip>
                                                <template #title>清空影像图层</template>
                                                <Trash2Icon :size="18" class="mt-4! ml-4! cursor-pointer"
                                                    @click="clearAllShowingSensor" />
                                            </a-tooltip>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <!-- 检索后的统计信息 -->
                            <!-- <div class="config-item">
                                <div class="config-label relative">
                                    <BoltIcon :size="16" class="config-icon" />
                                    <span>统计信息</span>
                                </div>
                                <div class="config-control flex-col gap-4">
                                    <div v-for="(sensorItem, index) in filteredSensorsItems" class="config-item w-full">
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
                                            <select @change="
                                                (e) =>
                                                    showImageBySensorAndSelect(
                                                        sensorItem,
                                                        (e.target as HTMLSelectElement).value,
                                                    )
                                            "
                                                class="max-h-[600px] max-w-[calc(100%-90px)] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none">
                                                <option disabled selected value="">
                                                    请选择影像
                                                </option>
                                                <option v-for="sceneName in sensorItem.sceneNames" :key="sceneName"
                                                    :value="sceneName" class="truncate">
                                                    {{ sceneName }}
                                                </option>
                                            </select>
                                        </div>
                                        <div v-show="sensorItem.selectedSceneInfo" class="mt-4">
                                            <div class="mr-1 grid grid-cols-[1fr_2fr]">
                                                <span class="text-white">亮度拉伸:</span>
                                                <a-slider :tip-formatter="scaleRateFormatter"
                                                    v-model:value="sensorItem.scaleRate"
                                                    @afterChange="onAfterScaleRateChange" />
                                            </div>
                                            <div>
                                        
                                                <a-button class="custom-button w-full!" :loading="sensorItem.loading"
                                                    @click="handleShowImage(sensorItem)">
                                                    影像可视化
                                                </a-button>
                                            </div>
                                        </div>
                                    </div>
                             
                                </div>
                            </div> -->
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
    getCoverRegionSensorScenes,
    getPoiInfo,
    getGridByPOIAndResolution,
    getPOIPosition,
} from '@/api/http/satellite-data'
import * as MapOperation from '@/util/map/operation'
import { mapManager } from '@/util/map/mapManager'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import { ezStore } from '@/store'
import { getSceneGeojson } from '@/api/http/satellite-data/visualize.api'
import { getRGBTileLayerParamFromSceneObject } from '@/util/visualizeHelper/index'
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
import mapboxgl from 'mapbox-gl'
const emit = defineEmits(['submitConfig'])

/**
 * 行政区划选取
 */

const radiusOptions = [1, 2, 5, 10, 15, 20, 25, 30, 40, 50, 80, 100, 150]

type ResolutionItem = [label: string, value: number]
type Label = {
    label: string,
    value: string,
}
type POIInfo = {
    // adcode: string,
    // adname: string,
    gcj02Lat: string,
    gcj02Lon: string,
    geometry: any,
    id: string,
    name: string,
    address: string
    pname: string
    cityname: string
    adname: string
}

const resolutionType: ResolutionItem[] = [
    ['亚米', 1],
    ['2米', 2],
    ['10米', 10],
    ['30米', 30],
    ['其他', 500],
]
// 绑定每个select的选中项
const resolutionPlatformSensor = reactive<any>({
    亚米: '',
    '2米': '',
    '10米': '',
    '30米': '',
    其他: '',
})

const selectedRadius = ref(20)
const tileMergeConfig = ref({
    useLatestTime: false,
    useMinCloud: false,
    dateRange: [dayjs('2024-01'), dayjs('2025-05')],
    cloudRange: [0, 100],
})
const region = ref<RegionValues>({
    province: '370000',
    city: '370100',
    area: '',
})
const allGrids = ref([])
const allGridCount = ref(0)
const currentCityBounds = ref([])
// 计算到了哪一级行政单位
const displayLabel = computed(() => {
    if (activeTab.value === 'poi') {

        if (!selectedPOI.value) return '未选择'
        return selectedPOI.value?.id
    }
    let info = region.value
    if (info.area) return `${info.area}`
    if (info.city) return `${info.city}`
    if (info.province) return `${info.province}`
    return '未选择'
})

const createGeoJSONFromBounds = (bounds: number[][]) => {
    const [minLon, minLat, maxLon, maxLat] = bounds;

    const polygon = [
        [
            [minLon, minLat],
            [maxLon, minLat],
            [maxLon, maxLat],
            [minLon, maxLat],
            [minLon, minLat] // 闭合
        ]
    ];

    return {
        type: "Feature",
        geometry: {
            type: "MultiPolygon",
            coordinates: [polygon]
        }
    };
}
let marker
// 获取格网数据
const getAllGrid = async () => {
    let gridRes: any = []
    let window: any = []
    if (displayLabel.value === '未选择') {
        ElMessage.warning('请选择行政区或POI')
        return
    }
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()
    if (marker) marker.remove()

    if (activeTab.value === 'region') {
        let boundaryRes = await getBoundary(displayLabel.value)
        currentCityBounds.value = boundaryRes
        gridRes = await getGridByRegionAndResolution(displayLabel.value, selectedRadius.value)
        allGrids.value = gridRes
        allGridCount.value = gridRes.length
        console.log(boundaryRes, 445);

        // 先清除现有的矢量边界，然后再添加新的
        MapOperation.map_addPolygonLayer({
            geoJson: boundaryRes,
            id: 'UniqueLayer',
            lineColor: '#8fffff',
            fillColor: '#a4ffff',
            fillOpacity: 0.2,
        })
        window = await getRegionPosition(displayLabel.value)
    } else if (activeTab.value === 'poi') {
        gridRes = await getGridByPOIAndResolution(displayLabel.value, selectedRadius.value)
        mapManager.withMap((m) => {
            if (m.getSource('UniqueLayer-source')) m.removeSource('UniqueLayer-source')
            if (m.getLayer('UniqueLayer-line')) m.removeLayer('UniqueLayer-line')
            if (m.getLayer('UniqueLayer-fill')) m.removeLayer('UniqueLayer-fill')
        })
        // console.log(gridRes, 7474);
        allGrids.value = gridRes
        allGridCount.value = gridRes.length
        window = await getPOIPosition(displayLabel.value, selectedRadius.value)
        let geojson = createGeoJSONFromBounds(window.bounds)
        console.log(geojson, 741);
        MapOperation.map_addPolygonLayer({
            geoJson: geojson,
            id: 'UniqueLayer',
            lineColor: '#8fffff',
            fillColor: '#a4ffff',
            fillOpacity: 0.2,
        })
        mapManager.withMap((m) => {
            marker = new mapboxgl.Marker()
                .setLngLat([Number(selectedPOI.value?.gcj02Lon), Number(selectedPOI.value?.gcj02Lat)])
                .addTo(m);
        })

        // MapOperation.map_addPointLayer([Number(selectedPOI.value?.gcj02Lon), Number(selectedPOI.value?.gcj02Lat)])
    }

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

const activeTab = ref('region')
const tabs = [{
    value: 'region',
    label: '行政区'
}, {
    value: 'poi',
    label: 'POI'
}]
const selectedPOI = ref<POIInfo>()
const poiOptions = ref<POIInfo[]>([])

// 根据输入内容远程获取
const fetchPOIOptions = async (query: string) => {
    let res: POIInfo[] = await getPoiInfo(query)
    poiOptions.value = res.map(item => {
        return {
            ...item,
            address: item.address === '[]' ? '' : item.address,
        }
    })
    console.log(res, 7774);

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
    console.log('allScenes', allScenes.value)

    // 记录所有景中含有的“传感器+分辨率字段”
    // allSensorsItems.value = getSensorsAndResolutions(allScenes.value)

    // 请求每个格子的景sceneGridsRes， 计算覆盖率，添加覆盖率格网图层，以及图层右键交互事件
    await makeFullSceneGrid()

    if (allScenes.value.length === 0) {
        ElMessage.warning('未筛选出符合要求的影像，请重新设置条件')
    } else {
        ElMessage.success(`已检索到${allScenes.value.length}景影像，请进一步筛选您所需的影像`)
    }

    // 根据所有的传感器+分辨率，找出每个格网上，在特定传感器+分辨率情况下，有多少景影像
    // allSensorsItems.value = countSensorsCoverage(
    //     allSensorsItems.value,
    //     ezStore.get('sceneGridsRes'),
    // )

    // 计算各种分辨率下的格网覆盖情况
    allSensorsItems.value = countResolutionCoverage(ezStore.get('sceneGridsRes'))

    // 获取各分辨率拥有多少种传感器，用来渲染下拉框
    classifyScenesByResolution()

    // 刚拿到的时候根据默认tags先分类一次
    filterByTags()

    // 恢复状态
    filterByCloudAndDateLoading.value = false
}

// 数各种分辨率分别覆盖了多少格网
const countResolutionCoverage = (allGridScene: any[]) => {
    console.log(allGridScene)
    const result = {
        亚米: 0,
        '2米': 0,
        '10米': 0,
        '30米': 0,
        其他: 0,
    }

    allGridScene.forEach((grid) => {
        const seen = new Set<string>() // 记录当前 grid 中已统计过的 resolution 分类

        grid.scenes?.forEach((scene: any) => {
            const resStr = scene.resolution?.toString().replace('m', '')
            const res = parseFloat(resStr)

            let key

            if (res <= 1) {
                key = '亚米'
            } else if (res === 2) {
                key = '2米'
            } else if (res === 10) {
                key = '10米'
            } else if (res === 30) {
                key = '30米'
            } else {
                key = '其他'
            }

            if (!seen.has(key)) {
                seen.add(key)
                result[key] += 1
            }
        })
    })

    return result
}

// 数各种分辨率的数据集分别覆盖了多少格网
const classifiedScenes = ref({})
const classifyScenesByResolution = () => {
    const result = {
        '1m': [],
        '2m': [],
        '10m': [],
        '30m': [],
        '500m': [],
    }

    const addToCategory = (key: string, platform: string) => {
        if (!result[key].includes(platform)) {
            result[key].push(platform)
        }
    }

    for (const scene of allScenes.value) {
        const resStr = scene.resolution?.toString().replace('m', '')
        const res = parseFloat(resStr)
        const platform = scene.platformName

        if (!platform || isNaN(res)) continue

        if (res <= 1) {
            addToCategory('1m', platform)
        } else if (res === 2) {
            addToCategory('2m', platform)
        } else if (res === 10) {
            addToCategory('10m', platform)
        } else if (res === 30) {
            addToCategory('30m', platform)
        } else {
            addToCategory('500m', platform)
        }
    }
    classifiedScenes.value = result
}

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
    const sceneNodataMap = ezStore.get('sceneNodataMap') as Map<string, number>
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
            // 在这记录一下景的nodata
            sceneNodataMap.set(scene.sceneId, scene.noData)
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
}

// 根据分辨率获得影像总数的方法
const getSceneCountByResolution = (resolution: number) => {
    let count = 0
    if (resolution === 1) {
        allScenes.value.forEach((scene: any) => {
            let data = parseFloat(scene.resolution)
            if (data <= 1) {
                count++
            }
        })
        return count
    } else if (resolution === 500) {
        allScenes.value.forEach((scene: any) => {
            let data = parseFloat(scene.resolution)
            if (data > 1 && data != 2 && data != 10 && data != 30) {
                count++
            }
        })
        return count
    } else {
        allScenes.value.forEach((scene: any) => {
            let data = parseFloat(scene.resolution)
            if (data === resolution) {
                count++
            }
        })
        return count
    }
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
        console.error('搞毛啊，能选出空值来？')
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
    MapOperation.map_destroyMultiRGBImageTileLayer()
}

// 工具函数： 获取platformName名对应的所有景
const getSceneIdsByPlatformName = (platformName: string, label: string) => {
    console.log('所有景', allScenes.value)
    console.log('选中的平台名', platformName)
    let scenes = allScenes.value
    if (label === '亚米') {
        scenes = allScenes.value.filter((scene) => {
            if (scene.tags.includes('ard')) {
                return scene
            }
        })
    }
    console.log(scenes, 'allImages')

    if (platformName === 'all') return scenes.map((item) => item.sceneId)

    const res: any[] = []
    scenes.forEach((item) => {
        if (item.platformName == platformName) {
            res.push(item.sceneId)
        }
    })
    console.log(res, 'images')

    return res
}

// 工具函数： platformName -> sensorName, 接口需要sensorName
const getSensorNamebyPlatformName = (platformName: string) => {
    // if (platformName === 'all') return 'all'
    // return platformName.split('_')[1]
    // 先把全选去了，接口没留全选逻辑
    let sensorName = ''
    for (let item of allScenes.value) {
        if (item.platformName === platformName) {
            sensorName = item.sensorName
            break
        }
    }
    return sensorName
}

const handleShowResolutionSensorImage = async (label: string) => {
    const selectPlatformName = resolutionPlatformSensor[label]
    const sceneIds = getSceneIdsByPlatformName(selectPlatformName, label)
    console.log('选中的景ids', sceneIds)
    console.log('当前所有的景', allScenes.value)
    const sensorName = getSensorNamebyPlatformName(selectPlatformName)

    console.log('匹配的sensorName', sensorName)

    const params = {
        sensorName,
        sceneIds,
        regionId: displayLabel.value,
    }

    const stopLoading = message.loading('正在加载影像...')

    const coverScenes = await getCoverRegionSensorScenes(params)

    console.log('覆盖的景们', coverScenes)

    const promises: Promise<any>[] = []

    for (let scene of coverScenes) {
        promises.push(getRGBTileLayerParamFromSceneObject(scene))
    }

    const rgbTileLayerParamList = await Promise.all(promises)

    console.log('可视化参数们', rgbTileLayerParamList)

    MapOperation.map_addMultiRGBImageTileLayer(rgbTileLayerParamList, stopLoading)
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
    if (!ezStore.get('sceneNodataMap')) ezStore.set('sceneNodataMap', new Map())
})
</script>

<style scoped src="./tabStyle.css">
:deep(button.ant-picker-year-btn) {
    pointer-events: none;
    color: #aaa;
}

:deep(button.ant-picker-month-btn) {
    pointer-events: none;
    color: #aaa;
}
</style>
