import { ref, computed } from 'vue'
import { type SpatialFilterMethod, type POIInfo, type FilterTab } from '@/type/interactive-explore/filter'
import { useExploreStore } from '@/store/exploreStore'
import { useI18n } from 'vue-i18n'
import { dayjs, ElMessage } from 'element-plus'
import { message } from 'ant-design-vue'
import {
    // ------------------------ V1/V2版本API ------------------------ //
    getGridByRegionAndResolution,
    getBoundary,
    getRegionPosition,
    getPoiInfo,
    getGridByPOIAndResolution,
    getPOIPosition,
    getVectorsByRegionFilter,
    getVectorsByPOIFilter,
} from '@/api/http/satellite-data'
import {
    // ------------------------ V3版本API ------------------------ //
    getSceneStatsByRegionFilter,
    getSceneStatsByPOIFilter,
    getThemeStatsByRegionFilter,
    getThemeStatsByPOIFilter
} from '@/api/http/interactive-explore/filter.api'
import { useVisualize } from './useVisualize'
import { ezStore } from "@/store"
import {
    searchedSpatialFilterMethod,
    activeSpatialFilterMethod,
    selectedRegion,
    selectedPOI,
    finalLandId,
    selectedGridResolution,
    selectedDateRange,
    curGridsBoundary,
    sceneStats,
    vectorStats,
    themeStats
} from './shared'

const exploreData = useExploreStore()


/**
 * 筛选器
 */
export const useFilter = () => {
    const { t } = useI18n()
    const { createGeoJSONFromBounds, marker, addPolygonLayer, addPOIMarker, addGridLayer, updateGridLayer, destroyUniqueLayer, destroyGridLayer, getVectorSymbology } = useVisualize()

    
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
        return '100000' // 默认中国
    })

    /**
     * 数据检索变量 - 2.格网分辨率
     */
    const gridOptions = [1, 2, 5, 10, 15, 20, 25, 30, 40, 50, 100, 200, 500, 1000]
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
        const stopLoading = message.loading('正在获取格网，请稍后...', 100)
        destroyGridLayer()
    
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
            destroyUniqueLayer()
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
        stopLoading()
        message.success('格网获取成功')
    }

    /**
     * 数据检索变量 - 3.时间预设范围
     */
    const dateRangePresets = ref([
        { label: '7 天内', value: [dayjs().add(-7, 'd'), dayjs()] },
        { label: '14 天内', value: [dayjs().add(-14, 'd'), dayjs()] },
        { label: '最近一个月', value: [dayjs().add(-30, 'd'), dayjs()] },
        { label: '最近三个月', value: [dayjs().add(-90, 'd'), dayjs()] },
        { label: '最近半年', value: [dayjs().add(-180, 'd'), dayjs()] },
        { label: '最近一年', value: [dayjs().add(-365, 'd'), dayjs()] },
    ]);


    /**
     * 数据检索函数及统计信息获取
     */
    // 筛选loading状态
    const filterLoading = ref(false)
    // 筛选是否完成
    const isFilterDone = ref(false)
    const doFilter = async () => {
        if (finalLandId.value === 'None') {
            ElMessage.warning(t('datapage.explore.message.filtererror_choose'))
            return
        } else if (allGrids.value.length === 0) {
            ElMessage.warning(t('datapage.explore.message.filtererror_grid'))
            return
        }
        const stopLoading = message.loading('正在检索数据，请稍后...', 500)
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
            themeStatsRes = await getThemeStatsByRegionFilter(regionFilter)
        } else if (searchedSpatialFilterMethod.value === 'poi') {
            sceneStatsRes = await getSceneStatsByPOIFilter(poiFilter)
            vectorsRes = await getVectorsByPOIFilter(poiFilter)
            themeStatsRes = await getThemeStatsByPOIFilter(poiFilter)
        }
        sceneStats.value = sceneStatsRes
        vectorStats.value = vectorsRes
        themeStats.value = themeStatsRes
        try {
            await getVectorSymbology()
        } catch (e) {
            console.error('获取矢量属性类型失败:', e)
        }

        syncToGridExplore()
        syncToDataPrepare()

        if (sceneStats.value.total === 0) {
            message.warning('未检索到数据')
        } else {
            message.success('数据检索成功')
        }
        stopLoading()
    
        // 恢复状态
        filterLoading.value = false
        isFilterDone.value = true
    }

    /**
     * 同步到网格图层: 添加格网图层，同步格网探查所需的变量，以及图层右键交互事件
     */
    const syncToGridExplore = () => {
        updateGridLayer(allGrids.value)
        ezStore.set('sceneStats', sceneStats.value)
        ezStore.set('vectorStats', vectorStats.value)
        ezStore.set('themeStats', themeStats.value)
        ezStore.set('curGridsBoundary', curGridsBoundary.value)
    }
    
    /**
     * 同步到数据准备: 同步数据准备所需的变量
     */
    const syncToDataPrepare = () => {
        exploreData.updateFields({
            searchtab: searchedSpatialFilterMethod.value,
            regionCode: finalLandId.value,
            dataRange: [...selectedDateRange.value],
            gridResolution: selectedGridResolution.value, // 原space
            coverage: sceneStats.value.coverage,
            allCoverage: sceneStats.value.dataset,
            // images: allScenes.value,
            grids: allGrids.value,
            boundary: curRegionBounds.value,
            load: true
        });
    }

    return {
        gridOptions,
        allGrids,
        allGridCount,
        poiOptions,
        fetchPOIOptions,
        getAllGrid,
        tabs,
        handleSelectTab,
        doFilter,
        filterLoading,
        isFilterDone,
        dateRangePresets
    }
}