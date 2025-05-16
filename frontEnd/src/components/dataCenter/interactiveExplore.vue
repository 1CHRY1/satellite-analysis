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
                                    <RegionSelects v-model="region" :placeholder="['选择省份', '选择城市', '选择区县']"
                                        class="flex gap-2"
                                        select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
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
                                            class="bg-[#0d1526] text-white w-40 border border-[#2c3e50] rounded-lg px-4 py-2 pr-8 appearance-none transition-all duration-200 hover:border-[#206d93] focus:outline-none focus:border-[#3b82f6]">
                                            <option v-for="option in radiusOptions" :key="option" :value="option"
                                                class="bg-[#0d1526] text-white">
                                                {{ option }}km
                                            </option>
                                        </select>
                                    </div>
                                    <div class="flex flex-row">
                                        <div class="text-red-500">*</div>建议省级行政单位格网分辨率不小于20km
                                    </div>
                                </div>
                            </div>
                            <button @click="getAllGrid"
                                class="bg-[#0d1526] cursor-pointer text-white border border-[#2c3e50] rounded-lg px-4 py-2 hover:bg-[#1a2b4c] hover:border-[#2bb2ff] transition-all duration-200 active:scale-95">
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
                            </div>
                            <button @click="filterByCloudAndDate"
                                class="bg-[#0d1526] cursor-pointer text-white border border-[#2c3e50] rounded-lg px-4 py-2 hover:bg-[#1a2b4c] hover:border-[#2bb2ff] transition-all duration-200 active:scale-95">
                                影像初筛
                            </button>

                        </div>
                    </div>
                </section>
                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <DatabaseIcon :size="18" />
                        </div>
                        <h2 class="section-title">筛选影像</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <!-- 三级筛选 -->
                            <div class="config-item">
                                <div class="config-label relative">
                                    <BoltIcon :size="16" class="config-icon" />
                                    <span>数据来源</span>
                                </div>
                                <div class="config-control gap-4">
                                    <button v-for="label in buttonGroups[0]" :key="label" @click="toggleButton(label)"
                                        class="cursor-pointer border rounded-lg px-4 py-1 transition-all duration-200 active:scale-90"
                                        :class="[
                                            isActive(label)
                                                ? 'bg-[#0d2e4b] text-white border-[#2bb2ff]'
                                                : 'bg-transparent text-[#94a3b8] border-[#475569] hover:bg-[#1e293b] hover:border-[#2bb2ff]',
                                        ]">
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
                                    <button v-for="label in buttonGroups[1]" :key="label" @click="toggleButton(label)"
                                        class="cursor-pointer border rounded-lg px-4 py-1 transition-all duration-200 active:scale-90"
                                        :class="[
                                            isActive(label)
                                                ? 'bg-[#0d2e4b] text-white border-[#2bb2ff]'
                                                : 'bg-transparent text-[#94a3b8] border-[#475569] hover:bg-[#1e293b] hover:border-[#2bb2ff]',
                                        ]">
                                        {{ label }}
                                    </button>
                                </div>
                            </div>
                            <div class="config-item">
                                <div class="config-label relative">
                                    <BoltIcon :size="16" class="config-icon" />
                                    <span>数据级别</span>
                                </div>
                                <div class="config-control gap-4">
                                    <button v-for="label in buttonGroups[2]" :key="label" @click="toggleButton(label)"
                                        class="cursor-pointer border rounded-lg px-4 py-1 transition-all duration-200 active:scale-90"
                                        :class="[
                                            isActive(label)
                                                ? 'bg-[#0d2e4b] text-white border-[#2bb2ff]'
                                                : 'bg-transparent text-[#94a3b8] border-[#475569] hover:bg-[#1e293b] hover:border-[#2bb2ff]',
                                        ]">
                                        {{ label }}
                                    </button>
                                </div>
                            </div>
                            <button @click="filterByTags"
                                class="bg-[#0d1526] cursor-pointer text-white border border-[#2c3e50] rounded-lg px-4 py-2 hover:bg-[#1a2b4c] hover:border-[#2bb2ff] transition-all duration-200 active:scale-95">
                                检索影像
                            </button>
                            <!-- 检索后的统计信息 -->
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
                                    </div>
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
import { ref, computed, type Ref } from 'vue'
import dayjs from 'dayjs'
import { RegionSelects } from 'v-region'
import type { RegionValues } from 'v-region'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import { formatTime } from '@/util/common'
import { getGridByRegionAndResolution, getBoundary, getRegionPosition, getSceneByConfig, getSceneGrids } from '@/api/http/satellite-data'
import * as MapOperation from '@/util/map/operation'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import { useGridStore, ezStore } from '@/store'

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
} from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { all } from 'axios'

const emit = defineEmits(['submitConfig'])
const gridStore = useGridStore()

/**
 * 行政区划选取
 */

const radiusOptions = [2, 5, 10, 15, 20, 25, 30, 40, 50]
const selectedRadius = ref(25)
const tileMergeConfig = ref({
    useLatestTime: false,
    useMinCloud: false,
    dateRange: [dayjs('2010-10'), dayjs('2025-05')],
    cloudRange: [0, 100],
})
const region = ref<RegionValues>({
    province: '140000',
    city: '',
    area: '',
})
const allGrids = ref([])
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

    gridStore.cleadAllGrids()
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    let boundaryRes = await getBoundary(displayLabel.value)
    let gridRes = await getGridByRegionAndResolution(displayLabel.value, selectedRadius.value)
    allGrids.value = gridRes
    let window = await getRegionPosition(displayLabel.value)
    currentCityBounds.value = boundaryRes
    // 先清除现有的矢量边界，然后再添加新的
    MapOperation.map_addPolygonLayer({
        geoJson: boundaryRes,
        id: 'UniqueLayer',
        lineColor: '#00FFFF',
        fillColor: '#00FFFF',
        fillOpacity: 0.2,
        // onClick: (f) => {
        //     console.log('点击的边界要素:', f)
        // },
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
                    id: item.properties?.id ?? index  // 确保每个都有 id
                }
            }
        })
    }

    MapOperation.map_addGridLayer(gridFeature)
    MapOperation.draw_deleteAll()
    // fly to 
    MapOperation.map_fitView([[window.bounds[0], window.bounds[1]], [window.bounds[2], window.bounds[3]]])
}


/**
 * 用云量和日期初步获取影像数据
 */
const allFilteredImages = ref([])

const filterByCloudAndDate = async () => {
    if (displayLabel.value === '未选择') {
        ElMessage.warning('请先选择行政区并获取格网')
        return
    }
    let filterData = {
        startTime: tileMergeConfig.value.dateRange[0].format('YYYY-MM-DD'),
        endTime: tileMergeConfig.value.dateRange[1].format('YYYY-MM-DD'),
        cloud: tileMergeConfig.value.cloudRange[1],
        regionId: displayLabel.value
    }
    allFilteredImages.value = (await getSceneByConfig(filterData)).map((image) => {
        return {
            ...image,
            tags: [image.tags.source, image.tags.production, image.tags.category]
        }
    })
    if (allFilteredImages.value.length === 0) {
        ElMessage.warning('未筛选出符合要求的影像，请重新设置条件')
    } else {
        ElMessage.success(`已检索到${allFilteredImages.value.length}景影像，请进一步筛选您所需的影像`)
    }
    console.log(allFilteredImages.value);

}

/**
 * 标签筛选
 */

const buttonGroups = [
    ['国产影像', '国外影像'],
    ['光学影像', 'SAR影像'],
    ['原始影像', 'ARD影像']
]
// 存储已激活的按钮标签
const activeButtons = ref<Set<string>>(new Set(['国产影像', '光学影像', '原始影像']))
// 切换按钮选中状态
const toggleButton = (label: string) => {
    if (activeButtons.value.has(label)) {
        activeButtons.value.delete(label)
    } else {
        activeButtons.value.add(label)
    }
}

// 判断按钮是否被选中
const isActive = (label: string) => activeButtons.value.has(label)

const tagMap: Record<string, string> = {
    '国产影像': 'national',
    '国外影像': 'international',
    '光学影像': 'light',
    'SAR影像': 'radar',
    '原始影像': 'traditional',
    'ARD影像': 'ard',
}

const filteredImages: Ref<any[]> = ref([])
const coverageRate = ref('0.00%')
const filterByTags = async () => {
    emit('submitConfig', {
        regionCode: displayLabel.value,
        dataRange: [...tileMergeConfig.value.dateRange],
        cloud: tileMergeConfig.value.cloudRange[1],
        space: selectedRadius.value,
        coverage: coverageRate.value,
        images: allFilteredImages.value,
        grids: allGrids.value,
        boundary: currentCityBounds.value
    })
    if (!verifyFilterByTags()) {
        return
    }
    // 1、根据tags进行筛选
    const flattenedButtons = buttonGroups.flat()

    filteredImages.value = allFilteredImages.value.filter((img: any) => {
        return flattenedButtons.every(btn => {
            const tagKey = tagMap[btn] // 将中文映射为英文
            // 如果某个按钮未激活，图像不能包含这个按钮对应的标签
            return activeButtons.value.has(btn) || !img.tags.includes(tagKey)
        })
    })

    // 2、获取格网覆盖信息
    let sceneGridParam = {
        grids: allGrids.value.map((item: any) => {
            return {
                rowId: item.rowId,
                columnId: item.columnId,
                resolution: item.resolution
            }
        }),
        sceneIds: filteredImages.value.map(images => images.sceneId)
    }

    // 清除格网图层，得放到一个请求上面，不然添加图层的时候还没销毁
    gridStore.cleadAllGrids()
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()
    // 这个是用网格去切割景，看各个网格里面有哪些景的数据
    let sceneGridsRes = await getSceneGrids(sceneGridParam)

    // console.log("sceneGridsRes", sceneGridsRes)
    // console.log("all grids ", allGrids.value)
    // {
    //     for (let i = 0; i < allGrids.value.length; i++) {
    //         if (allGrids.value[i].rowId === sceneGridsRes[i].rowId && allGrids.value[i].colId === sceneGridsRes[i].colId) {
    //             console.log('1')
    //         } else {
    //             console.log("not match")
    //         }
    //     }
    // }
    ezStore.set('sceneGridsRes', sceneGridsRes)


    // 算覆盖率
    const nonEmptyScenesCount = sceneGridsRes.filter(item => item.scenes.length > 0).length
    coverageRate.value = (nonEmptyScenesCount * 100 / sceneGridsRes.length).toFixed(2) + '%';

    // emit('submitConfig', {
    //     regionCode: displayLabel.value,
    //     dataRange: [...tileMergeConfig.value.dateRange],
    //     cloud: tileMergeConfig.value.cloudRange[1],
    //     space: selectedRadius.value,
    //     coverage: coverageRate.value,
    //     images: allFilteredImages.value,
    //     grids: allGrids.value,
    // })

    // 添加带有数据指示的格网
    let gridFeature: FeatureCollection = {
        type: 'FeatureCollection',
        features: allGrids.value.map((item: any, index) => {
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
                }
            }
        })
    }
    MapOperation.map_addGridLayer(gridFeature)
    MapOperation.draw_deleteAll()

    ElMessage.success(`检索完毕，请查看统计信息并进行下一步操作`)
}

// 判断格网到底有没有数据，有就返回0.3
const judgeGridOpacity = (index: number, sceneGridsRes: any) => {
    let opacity = 0.01
    sceneGridsRes[index]?.scenes.length > 0 ? opacity = 0.3 : opacity = 0.01;
    return opacity
}
// 卫语句
const verifyFilterByTags = () => {
    let buttons = activeButtons.value
    if (buttons.size === 0) {
        ElMessage.warning('请设置筛选条件')
        return false
    }
    if (allFilteredImages.value.length === 0) {
        ElMessage.warning('请先进行影像初筛')
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



</script>

<style scoped src="./tabStyle.css"></style>