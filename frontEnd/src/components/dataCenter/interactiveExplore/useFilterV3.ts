import { ref, computed } from 'vue'
import { type SpatialFilterMethod, type POIInfo, type FilterTab } from '@/type/interactive-explore/filter'
import type { RegionValues } from 'v-region'
import dayjs, { Dayjs } from 'dayjs'
import { useExploreStore } from '@/store/exploreStore'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
    // ------------------------ V1/V2版本API ------------------------ //
    getGridByRegionAndResolution,
    getBoundary,
    getRegionPosition,
    getPoiInfo,
    getGridByPOIAndResolution,
    getPOIPosition,
    getVectorsByRegionFilter,
    getVectorsByPOIFilter
} from '@/api/http/satellite-data'
import {
    // ------------------------ V3版本API ------------------------ //
    getSceneStatsByRegionFilter,
    getSceneStatsByPOIFilter,
    getThemeStatsByRegionFilter,
    getThemeStatsByPOIFilter
} from '@/api/http/interactive-explore/filter.api'
import type { SceneStats, VectorStats, ThemeStats } from '@/api/http/interactive-explore/filter.type'
import { useLayer } from './useLayer'
const { createGeoJSONFromBounds, marker, addPolygonLayer, destroyLayer, removeUniqueLayer,
    addPOIMarker, addGridLayer } = useLayer()


const exploreData = useExploreStore()

/**
 * 数据检索全局变量 - 1.空间位置
 */
// 选中的空间筛选方法(用于实际检索)
const searchedSpatialFilterMethod = ref<SpatialFilterMethod>('region')
// 激活的空间筛选方法(用于展示)
const activeSpatialFilterMethod = ref<SpatialFilterMethod>('region')
const selectedRegion = ref<RegionValues>({
    province: '370000',
    city: '370100',
    area: '',
})
const selectedPOI = ref<POIInfo>()
// 获取region/poi的id的计算属性，称为最终的地物id
const finalLandId = computed(() => {
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
    return 'None'
})

/**
 * 数据检索全局变量 - 2.格网分辨率
 */
const selectedGridResolution = ref<number>(20)

/**
 * 数据检索全局变量 - 3.时间范围
 */
const selectedDateRange = ref([dayjs('2025-05-01'), dayjs('2025-06-30')])


/**
 * 筛选器
 */
export const useFilter = () => {
    const { t } = useI18n()
    
    /**
     * 数据检索变量 - 1.空间位置
     */

    /**
     * 1.1 Tab变量
     */
    const tabs = computed<FilterTab[]>(() =>  [{
        value: 'region',
        label: t('datapage.explore.section1.admin')
    }, {
        value: 'poi',
        label: 'POI'
    }])
    
    // 用户选择空间筛选方法
    const handleSelectTab = (value: SpatialFilterMethod) => {
        activeSpatialFilterMethod.value = value
    }

    /**
     * 1.2 Region行政区划筛选
     */
    
    const curRegionBounds = ref([])

    /**
     * 1.3 POI筛选
     */
    
    const poiOptions = ref<POIInfo[]>([])
    // 根据输入内容远程获取
    const fetchPOIOptions = async (query: string) => {
        if (query === '') return
        let res: POIInfo[] = await getPoiInfo(query)
        poiOptions.value = res.map(item => {
            return {
                ...item,
                address: item.address === '[]' ? '' : item.address,
            }
        })
    }

    /**
     * 1.4 Region ｜ POI 临时选择
     */
    // 获取格网阶段所用的region/poi的id，称为临时的地物id
    const tempLandId = computed(() => {
        if (activeSpatialFilterMethod.value === 'poi') {
            if (!selectedPOI.value) return 'None'
            return selectedPOI.value?.id
        }
        let info = selectedRegion.value
        if (info.area) return `${info.area}`
        if (info.city) return `${info.city}`
        if (info.province) return `${info.province}`
        return 'None'
    })

    /**
     * 数据检索变量 - 2.格网分辨率
     */
    const gridOptions = [1, 2, 5, 10, 15, 20, 25, 30, 40, 50, 100]
    const curGridsBoundary = ref()
    const allGrids = ref([])
    const allGridCount = ref(0)

    // 获取格网数据
    const getAllGrid = async () => {
        let gridRes: any = []
        let window: any = []
        if (tempLandId.value === 'None') {
             ElMessage.warning(t('datapage.explore.message.POIerror'))
            return
        }
        await destroyLayer()
    
        if (marker.value) marker.value.remove()
    
        if (activeSpatialFilterMethod.value === 'region') {
            let boundaryRes = await getBoundary(tempLandId.value)
            curRegionBounds.value = boundaryRes
            gridRes = await getGridByRegionAndResolution(tempLandId.value, selectedGridResolution.value)
            allGrids.value = gridRes.grids
            allGridCount.value = gridRes.grids.length
            curGridsBoundary.value = gridRes.geoJson
            // 先清除现有的矢量边界，然后再添加新的
            addPolygonLayer(boundaryRes)
            window = await getRegionPosition(tempLandId.value)
        } else if (activeSpatialFilterMethod.value === 'poi') {
            gridRes = await getGridByPOIAndResolution(tempLandId.value, selectedGridResolution.value)
            removeUniqueLayer()
            allGrids.value = gridRes.grids
            allGridCount.value = gridRes.grids.length
            curGridsBoundary.value = gridRes.geoJson
            window = await getPOIPosition(tempLandId.value, selectedGridResolution.value)
            let geojson = createGeoJSONFromBounds(window.bounds)
            addPolygonLayer(geojson)
            if (selectedPOI.value) addPOIMarker(selectedPOI.value)
        }
    
        addGridLayer(gridRes.grids, window)
        // 将tab的选择固定下来
        searchedSpatialFilterMethod.value = activeSpatialFilterMethod.value
    }

    /**
     * 数据检索函数及统计信息获取
     */
    // 统计信息
    const sceneStats = ref<SceneStats.SceneStatsResponse>({
        total: 0,
        coverage: '0.00%',
        category: []
    })
    const vectorStats = ref<VectorStats.VectorStatsResponse>([])
    const themeStats = ref<ThemeStats.ThemeStatsResponse>({
        total: 0,
        category: []
    })
    // 筛选loading状态
    const filterLoading = ref(false)
    // 筛选是否完成
    const isFilterDone = ref(false)
    const filter = async () => {
        if (finalLandId.value === 'None') {
            ElMessage.warning(t('datapage.explore.message.filtererror_choose'))
            return
        } else if (allGrids.value.length === 0) {
            ElMessage.warning(t('datapage.explore.message.filtererror_grid'))
            return
        }
        // 先禁止按钮，渲染loading状态
        filterLoading.value = true
        const regionFilter = {
            startTime: selectedDateRange.value[0].format('YYYY-MM-DD'),
            endTime: selectedDateRange.value[1].format('YYYY-MM-DD'),
            regionId: finalLandId.value,
            resolution:selectedGridResolution.value
        }
        const poiFilter = {
            startTime: selectedDateRange.value[0].format('YYYY-MM-DD'),
            endTime: selectedDateRange.value[1].format('YYYY-MM-DD'),
            locationId: finalLandId.value,
            resolution: selectedGridResolution.value
        }
        let sceneStatsRes, vectorsRes, themeStatsRes
        if (searchedSpatialFilterMethod.value === 'region') {
            sceneStatsRes = await getSceneStatsByRegionFilter(regionFilter)
            vectorsRes = await getVectorsByRegionFilter(regionFilter)
            // themeStatsRes = await getThemeStatsByRegionFilter(regionFilter)
        } else if (searchedSpatialFilterMethod.value === 'poi') {
            sceneStatsRes = await getSceneStatsByPOIFilter(poiFilter)
            vectorsRes = await getVectorsByPOIFilter(poiFilter)
            themeStatsRes = await getThemeStatsByPOIFilter(poiFilter)
        }
        sceneStats.value = sceneStatsRes
        vectorStats.value = vectorsRes
        // themeStats.value = themeStatsRes

        // 请求每个格子的景sceneGridsRes， 计算覆盖率，添加覆盖率格网图层，以及图层右键交互事件
        // await makeFullSceneGrid()
    
        if (sceneStats.value.total === 0) {
            ElMessage.warning(t('datapage.explore.message.sceneerror_recondition'))
        } else {
            ElMessage.success(t('datapage.explore.message.scene_searched',{ count: sceneStats.value.total }) )
        }
    
        // 恢复状态
        filterLoading.value = false
        isFilterDone.value = true
    }

    return {
        gridOptions,
        selectedGridResolution,
        selectedRegion,
        selectedDateRange,
        allGrids,
        allGridCount,
        selectedPOI,
        poiOptions,
        fetchPOIOptions,
        getAllGrid,
        activeSpatialFilterMethod,
        tabs,
        handleSelectTab,
        searchedSpatialFilterMethod,
        finalLandId,
        filter,
        filterLoading,
        isFilterDone,
        sceneStats,
        vectorStats,
        themeStats
    }
}