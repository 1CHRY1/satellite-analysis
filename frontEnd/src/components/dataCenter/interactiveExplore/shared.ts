/**
 * Composables Shared Variables
 */

import type { SpatialFilterMethod, POIInfo } from "@/type/interactive-explore/filter"
import type { RegionValues } from "v-region"
import { ref, computed } from "vue"
import dayjs from "dayjs"
import type { SceneStats, VectorStats, ThemeStats } from '@/api/http/interactive-explore/filter.type'
import type { VectorSymbology } from "@/type/interactive-explore/visualize"


/**
 * 数据检索全局变量 - 1.空间位置
 */

// 选中的空间筛选方法(用于实际检索)
export const searchedSpatialFilterMethod = ref<SpatialFilterMethod>('region')
// 激活的空间筛选方法(用于展示)
export const activeSpatialFilterMethod = ref<SpatialFilterMethod>('region')
export const selectedRegion = ref<RegionValues>({
    province: '370000',
    city: '370100',
    area: '',
})
export const selectedPOI = ref<POIInfo>()
// 获取region/poi的id的计算属性，称为最终的地物id
export const finalLandId = computed(() => {
    let curSpatialFilterMethod: SpatialFilterMethod
    if (searchedSpatialFilterMethod.value === 'poi') {
        curSpatialFilterMethod = 'poi'
    } else if (searchedSpatialFilterMethod.value === 'region') {
        curSpatialFilterMethod = 'region'
    } else if (activeSpatialFilterMethod.value === 'poi') {
        curSpatialFilterMethod = 'poi'
    } else {
        curSpatialFilterMethod = 'region'
    }

    if (curSpatialFilterMethod === 'poi') {
        if (!selectedPOI.value) return 'None'
        return selectedPOI.value?.id
    }
    let info = selectedRegion.value
    if (info.area) return `${info.area}`
    if (info.city) return `${info.city}`
    if (info.province) return `${info.province}`
    return '100000' // 默认中国
})

/**
 * 数据检索全局变量 - 2.格网分辨率
 */
export const selectedGridResolution = ref<number>(20)
export const curGridsBoundary = ref()

/**
 * 数据检索全局变量 - 3.时间范围
 */
export const selectedDateRange = ref([dayjs('2025-05-01'), dayjs('2025-06-30')])

/**
 * 数据检索全局变量 - 4.统计信息
 */
// 统计信息
export const sceneStats = ref<SceneStats.SceneStatsResponse>({
    total: 0,
    coverage: '0.00%',
    category: []
})
export const vectorStats = ref<VectorStats.VectorStatsResponse>([])
export const themeStats = ref<ThemeStats.ThemeStatsResponse>({
    total: 0,
    category: []
})

export const vectorSymbology = ref<VectorSymbology>({})