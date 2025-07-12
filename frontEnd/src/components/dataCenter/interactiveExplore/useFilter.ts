import { computed, reactive, ref } from "vue"
import dayjs from 'dayjs'
import type { RegionValues } from 'v-region'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'

import * as MapOperation from '@/util/map/operation'
import { mapManager } from '@/util/map/mapManager'
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
    getSceneByPOIConfig,
    getCoverPOISensorScenes,
} from '@/api/http/satellite-data'
import { useLayer, type POIInfo } from './useLayer'
import { useStats } from './useStats'
import { ezStore } from "@/store"
const { createGeoJSONFromBounds, marker, addPolygonLayer, destroyLayer, removeUniqueLayer,
    addPOIMarker, addGridLayer, updateFullSceneGridLayer, addMultiRGBImageTileLayer } = useLayer()
const { countResolutionCoverage, classifyScenesByResolution, getSceneIdsByPlatformName, getSensorNamebyPlatformName } = useStats()
const coverageRSRate = ref('0.00%')
const coverageDEMRate = ref('0.00%')
import { useExploreStore } from '@/store/exploreStore'
const exploreData = useExploreStore()
import { message } from 'ant-design-vue'
/**
 * 筛选器
 */
export const useFilter = () => {
    const { t } = useI18n()

    /**
     * 筛选器变量
     */

    // 默认筛选配置
    const defaultConfig = ref({
        useLatestTime: false,
        useMinCloud: false,
        dateRange: [dayjs('2024-01'), dayjs('2025-05')],
        cloudRange: [0, 100],
    })

    interface Tab {
        value: SpatialFilterMethod
        label: string
    }

    const tabs = computed<Tab[]>(() =>  [{
        value: 'region',
        label: t('datapage.explore.section1.admin')
    }, {
        value: 'poi',
        label: 'POI'
    }])

    // 过滤出的遥感影像
    const allScenes = ref<any>([])
    // 过滤出的遥感影像中含有的传感器统计信息
    const allSensorsItems = ref<any>([])
    // 过滤出的DEM
    const allDEMs = ref<any>([])

    // 影像分辨率类型
    type ResolutionItem = [label: string, value: number]
    const resolutionType = computed<ResolutionItem[]>(()=>[
        [t('datapage.explore.section_interactive.resolutiontype.yami'), 1],
        [t('datapage.explore.section_interactive.resolutiontype.twom'), 2],
        [t('datapage.explore.section_interactive.resolutiontype.tenm'), 10],
        [t('datapage.explore.section_interactive.resolutiontype.thirtym'), 30],
        [t('datapage.explore.section_interactive.resolutiontype.others'), 500],
    ])
    // 绑定每个select的选中项
    const resolutionPlatformSensor = reactive<any>({
        [t('datapage.explore.section_interactive.resolutiontype.yami')]: '',
        [t('datapage.explore.section_interactive.resolutiontype.twom')]: '',
        [t('datapage.explore.section_interactive.resolutiontype.tenm')]: '',
        [t('datapage.explore.section_interactive.resolutiontype.thirtym')]: '',
        [t('datapage.explore.section_interactive.resolutiontype.others')]: '',
    })

    /**
     * 1.空间筛选
     */
    // 空间筛选方法
    type SpatialFilterMethod = 'region' | 'poi'
    const spatialFilterMethods = ref<SpatialFilterMethod[]>(['region', 'poi'])
    // 选中的空间筛选方法(用于实际检索)
    const searchedSpatialFilterMethod = ref<SpatialFilterMethod>('region')
    // 激活的空间筛选方法(用于展示)
    const activeSpatialFilterMethod = ref<SpatialFilterMethod>('region')
    // 获取格网阶段所用的region/poi的id，称为临时的地物id
    const tempLandId = computed(() => {
        if (activeSpatialFilterMethod.value === 'poi') {
            if (!selectedPOI.value) return '未选择'
            return selectedPOI.value?.id
        }
        let info = region.value
        if (info.area) return `${info.area}`
        if (info.city) return `${info.city}`
        if (info.province) return `${info.province}`
        return '未选择'
    })
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
            if (!selectedPOI.value) return '未选择'
            return selectedPOI.value?.id
        }
        let info = region.value
        if (info.area) return `${info.area}`
        if (info.city) return `${info.city}`
        if (info.province) return `${info.province}`
        return '未选择'
    })
    // 用户选择空间筛选方法
    const handleSelectTab = (value: SpatialFilterMethod) => {
        activeSpatialFilterMethod.value = value
    }
    
    /**
     * 1.1 Region行政区划筛选
     */
    // 行政区划筛选默认配置: 山东济南
    const region = ref<RegionValues>({
        province: '370000',
        city: '370100',
        area: '',
    })
    const curRegionBounds = ref([])

    /**
     * 1.2 POI筛选
     */
    const selectedPOI = ref<POIInfo>()
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
     * 2.格网筛选
     */
    const gridOptions = [1, 2, 5, 10, 15, 20, 25, 30, 40, 50]
    const selectedGrid = ref<number>(20)
    const allGrids = ref([])
    const allGridCount = ref(0)

    /**
     * 获取格网数据
     */
    const getAllGrid = async () => {
        let gridRes: any = []
        let window: any = []
        if (tempLandId.value === '未选择') {
             ElMessage.warning(t('datapage.explore.message.POIerror'))
            return
        }
        await destroyLayer()
    
        if (marker.value) marker.value.remove()
    
        if (activeSpatialFilterMethod.value === 'region') {
            let boundaryRes = await getBoundary(tempLandId.value)
            curRegionBounds.value = boundaryRes
            gridRes = await getGridByRegionAndResolution(tempLandId.value, selectedGrid.value)
            allGrids.value = gridRes
            allGridCount.value = gridRes.length
            console.log(boundaryRes, 445);
    
            // 先清除现有的矢量边界，然后再添加新的
            addPolygonLayer(boundaryRes)
            window = await getRegionPosition(tempLandId.value)
        } else if (activeSpatialFilterMethod.value === 'poi') {
            gridRes = await getGridByPOIAndResolution(tempLandId.value, selectedGrid.value)
            removeUniqueLayer()
            // console.log(gridRes, 7474);
            allGrids.value = gridRes
            allGridCount.value = gridRes.length
            window = await getPOIPosition(tempLandId.value, selectedGrid.value)
            let geojson = createGeoJSONFromBounds(window.bounds)
            console.log(geojson, 741);
            addPolygonLayer(geojson)
            if (selectedPOI.value) addPOIMarker(selectedPOI.value)
        }
    
        addGridLayer(gridRes, window)
        // 将tab的选择固定下来
        searchedSpatialFilterMethod.value = activeSpatialFilterMethod.value
    }

    /**
     * 3.时间筛选
     */
    

    /**
     * 4.数据类型筛选
     */

    /**
     * 5. 最终筛选函数
     */
    // 筛选loading状态
    const filterLoading = ref(false)
    // 筛选是否完成
    const isFilterDone = ref(false)
    const filter = async () => {
        if (finalLandId.value === '未选择') {
            ElMessage.warning(t('datapage.explore.message.filtererror_choose'))
            return
        } else if (allGrids.value.length === 0) {
            ElMessage.warning(t('datapage.explore.message.filtererror_grid'))
            return
        }
        // 先禁止按钮，渲染loading状态
        filterLoading.value = true
        let filterData = {
            startTime: defaultConfig.value.dateRange[0].format('YYYY-MM-DD'),
            endTime: defaultConfig.value.dateRange[1].format('YYYY-MM-DD'),
            cloud: defaultConfig.value.cloudRange[1],
            regionId: finalLandId.value,
        }
        // allFilteredImages.value = await getSceneByConfig(filterData)
        if (searchedSpatialFilterMethod.value === 'region') {
            const allScenesRes = await getSceneByConfig(filterData)
            allScenes.value = allScenesRes
                .filter((image) => image.dataType === 'satellite')
                .map((image) => ({
                    ...image,
                    tags: [image.tags.source, image.tags.production, image.tags.category],
                }))
            allDEMs.value = allScenesRes
                .filter((image) => image.dataType === 'dem')
                .map((image) => ({
                    ...image,
                    tags: [image.tags.source, image.tags.production, image.tags.category],
                }))
        } else if (searchedSpatialFilterMethod.value === 'poi') {
            const poiFilter = {
                startTime: defaultConfig.value.dateRange[0].format('YYYY-MM-DD'),
                endTime: defaultConfig.value.dateRange[1].format('YYYY-MM-DD'),
                cloud: defaultConfig.value.cloudRange[1],
                locationId: finalLandId.value,
                resolution: selectedGrid.value
            }
            const allScenesRes = await getSceneByPOIConfig(poiFilter)
            allScenes.value = allScenesRes
                .filter((image) => image.dataType === 'satellite')
                .map((image) => ({
                    ...image,
                    tags: [image.tags.source, image.tags.production, image.tags.category],
                }))
            allDEMs.value = allScenesRes
                .filter((image) => image.dataType === 'dem')
                .map((image) => ({
                    ...image,
                    tags: [image.tags.source, image.tags.production, image.tags.category],
                }))
        }
        console.log('allScenes', allScenes.value)
        console.log('allDEMs', allDEMs.value)
    
        // 记录所有景中含有的“传感器+分辨率字段”
        // allSensorsItems.value = getSensorsAndResolutions(allScenes.value)
    
        // 请求每个格子的景sceneGridsRes， 计算覆盖率，添加覆盖率格网图层，以及图层右键交互事件
        await makeFullSceneGrid()
    
        if (allScenes.value.length === 0) {
            ElMessage.warning(t('datapage.explore.message.sceneerror_recondition'))
        } else {
            ElMessage.success(t('datapage.explore.message.scene_searched',{ count: allScenes.value.length }) )
        }
    
        // 计算各种分辨率下的格网覆盖情况
        allSensorsItems.value = countResolutionCoverage(ezStore.get('sceneGridsRes'))
    
        // 获取各分辨率拥有多少种传感器，用来渲染下拉框
        classifyScenesByResolution(allScenes.value)
    
        // 恢复状态
        filterLoading.value = false
        isFilterDone.value = true
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
            sceneIds: allScenes.value.filter(image => image.tags.includes('ard') || parseFloat(image.resolution) > 1).map((image: any) => image.sceneId)
                        ,
        }
    
        // Destroy layer
        destroyLayer()
    
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
        let nonEmptyScenesCount = 0
        let nonEmptyDEMsCount = 0
        for (const item of sceneGridsRes) {
            if (item.scenes.some(scene => scene.dataType === 'satellite')) nonEmptyScenesCount++
            if (item.scenes.some(scene => scene.dataType === 'dem')) nonEmptyDEMsCount++
        }
        coverageRSRate.value = ((nonEmptyScenesCount * 100) / sceneGridsRes.length).toFixed(2) + '%'
        coverageDEMRate.value = ((nonEmptyDEMsCount * 100) / sceneGridsRes.length).toFixed(2) + '%'
    
        updateFullSceneGridLayer(allGrids.value, sceneGridsRes, allScenes.value.length)
    
        exploreData.updateFields({
            searchtab: searchedSpatialFilterMethod.value,
            regionCode: finalLandId.value,
            dataRange: [...defaultConfig.value.dateRange],
            cloud: defaultConfig.value.cloudRange[1],
            space: selectedGrid.value,
            coverage: coverageRSRate.value,
            images: allScenes.value,
            grids: allGrids.value,
            boundary: curRegionBounds.value,
            load: true
        });
    }

    const handleShowResolutionSensorImage = async (label: string) => {
        const sceneIds = getSceneIdsByPlatformName(label, resolutionPlatformSensor[label], allScenes.value)
        console.log('选中的景ids', sceneIds)
        console.log('当前所有的景', allScenes.value)
        const sensorName = getSensorNamebyPlatformName(resolutionPlatformSensor[label], allScenes.value)
    
        console.log('匹配的sensorName', sensorName)
    
        const stopLoading = message.loading(t('datapage.explore.message.load'))
    
        let coverScenes, gridsBoundary
        if (searchedSpatialFilterMethod.value === 'region') {
            const params = {
                sensorName,
                sceneIds,
                regionId: finalLandId.value,
                resolution: selectedGrid.value,
            }
            const coverScenesRes = await getCoverRegionSensorScenes(params)
            coverScenes = coverScenesRes.sceneList
            gridsBoundary = coverScenesRes.gridsBoundary
        } else if (searchedSpatialFilterMethod.value === 'poi') {
            const params = {
                sensorName,
                sceneIds,
                locationId: finalLandId.value,
                resolution: selectedGrid.value,
            }
            const coverScenesRes = await getCoverRegionSensorScenes(params)
            coverScenes = coverScenesRes.sceneList
            gridsBoundary = coverScenesRes.gridsBoundary
        }
        console.log('接口返回：覆盖的景们', coverScenes)
        await addMultiRGBImageTileLayer(coverScenes, gridsBoundary, stopLoading)
    }


    return {
        gridOptions,
        resolutionType,
        resolutionPlatformSensor,
        selectedGrid,
        defaultConfig,
        region,
        allGrids,
        allGridCount,
        selectedPOI,
        poiOptions,
        fetchPOIOptions,
        getAllGrid,
        filter,
        filterLoading,
        isFilterDone,
        makeFullSceneGrid,
        handleShowResolutionSensorImage,
        activeSpatialFilterMethod,
        tabs,
        allScenes,
        allDEMs,
        coverageDEMRate,
        allSensorsItems,
        coverageRSRate,
        handleSelectTab,
    }
}
