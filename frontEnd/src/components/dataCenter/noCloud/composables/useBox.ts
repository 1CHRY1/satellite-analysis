import { computed, reactive, ref } from 'vue'
import { exploreData } from './shared'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import * as DataPreparationMapOps from '@/util/map/operation/data-preparation'
import type { MapMouseEvent } from 'mapbox-gl'
import { ezStore } from '@/store'
import { getScenesInGrid } from '@/api/http/interactive-explore/grid.api'
import type { Grid } from '@/api/http/interactive-explore/grid.type'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'

type GridInfo = {
    columnId: number
    rowId: number
    resolution: number
}
type Scene = {
    sceneId: string
    sceneTime: Dayjs
    sensorName: string
    platformName: string
    productName: string
    noData: number
    images: any[]
    bandMapper: any
}
// 选中的格网数据信息
export const selectedGrid = ref<GridInfo>()
const sceneGridRes = ref() // 格网请求返回内容
const sceneList = ref<Scene[]>([]) // 格网内景列表
const gridCache = new Map<string, any>() // 请求缓存

// 传感器选项数据
const sensorOptions = ref([])

// 单击格网响应函数
export const clickHandler = async (e: MapMouseEvent): Promise<void> => {
    // 通知响应式变量
    if (e.features) {
        const f = e.features[0]
        selectedGrid.value = {
            columnId: f.properties?.columnId,
            rowId: f.properties?.rowId,
            resolution: f.properties?.resolution,
        }
    }
    console.log('当前已选择格网', selectedGrid.value)

    // 高亮对应格网
    const id = 'grid-layer'
    const highlightId = id + '-highlight'
    ezStore.get('map').setFilter(highlightId, ['in', 'id', e.features![0].properties!.id])

    // 请求格网内的数据
    const key = `${selectedGrid.value?.rowId}-${selectedGrid.value?.columnId}-${selectedGrid.value?.resolution}`
    if (gridCache.has(key)) {
        sceneGridRes.value = gridCache.get(key)
    } else {
        sceneGridRes.value = await getScenesInGrid(selectedGrid.value as Grid.GridRequest)
        gridCache.set(key, sceneGridRes.value)
    }
    getSensorOptions()
}

// 获取格网传感器options
const getSensorOptions = () => {
    // 先map后平铺成scene的列表，为避免重复数据，所以用Map和迭代器做了去重
    sceneList.value = [
        ...new Map(
            sceneGridRes.value.category
                .flatMap((item) => sceneGridRes.value.dataset[item].dataList)
                .map((item) => [item.sceneId, item]),
        ).values(),
    ] as Scene[]
    // 先去重，保留包含唯一sensorName的数组，再映射为需要的sensorOptions
    sensorOptions.value = [
        ...new Map(sceneList.value.map((item) => [item.sensorName, item])).values(),
    ].map((item) => {
        return { label: item.platformName, value: item.sensorName }
    }) as []
}

export const useBox = () => {
    // 点击格网后更新图层
    const updateGridLayer = (allGrids: any) => {
        console.log(allGrids)
        let gridFeature: FeatureCollection = {
            type: 'FeatureCollection',
            features: allGrids.map((item: any, index: number) => {
                return {
                    type: 'Feature',
                    geometry: item.boundary.geometry as Geometry,
                    properties: {
                        ...(item.properties || {}),
                        id: `${item.rowId}-${item.columnId}-${item.resolution}`, // 确保每个都有 id
                        // opacity: judgeGridOpacity(index, sceneGridsRes, totalImg),
                        opacity: 0.1,
                        rowId: item.rowId,
                        columnId: item.columnId,
                        resolution: item.resolution,
                        flag: true, // flag means its time to trigger the visual effect
                        // international: sceneGridsRes[index].international,
                        // national: sceneGridsRes[index].national,
                        // light: sceneGridsRes[index].light,
                        // radar: sceneGridsRes[index].radar,
                        // traditional: sceneGridsRes[index].traditional,
                        // ard: sceneGridsRes[index].ard,
                    },
                }
            }),
        }
        DataPreparationMapOps.map_destroyGridLayer()
        DataPreparationMapOps.map_addGridLayer(gridFeature)
        // InteractiveExploreMapOps.draw_deleteAll()
    }

    // 表单数据
    const formData = reactive({
        sensors: [] as string[],
        bands: [] as string[],
        dates: [] as Dayjs[],
    })

    // 波段option
    const bandOptions = ref([
        { value: 'R', label: 'Red', color: '#ff4d4f' },
        { value: 'G', label: 'Green', color: '#52c41a' },
        { value: 'B', label: 'Blue', color: '#1890ff' },
        { value: 'NIR', label: 'NIR', color: '#722ed1' },
    ])

    // 日期option，随传感器变化
    const dateOptions = computed(() => {
        let scenes = sceneList.value.filter((item) =>
            formData.sensors.includes(item.sensorName),
        ) as Scene[]
        console.log(scenes)
        return [...new Set(scenes.map((item) => dayjs(item.sceneTime).format('YYYY-MM-DD')))].sort(
            (a, b) => dayjs(a).valueOf() - dayjs(b).valueOf(),
        )
    })

    const canSynthesize = computed(() => {
        return formData.sensors.length > 0 && formData.bands.length > 0 && formData.dates.length > 0
    })

    // 事件处理
    const handleSensorChange = (value: string[]) => {
        console.log('传感器选择变化:', value)
        // 默认全选
        formData.dates = dateOptions.value as []
    }

    const handleBandChange = (value: string[]) => {
        console.log('波段选择变化:', value)
    }

    const handleDateChange = (dates: [Dayjs, Dayjs] | null) => {
        console.log('时间范围变化:', dates)
    }

    const handleSynthesis = async () => {
        try {
            showCubeContentDialog.value = true
        } catch (error) {
        } finally {
        }
    }

    const onFinish = (values: any) => {
        console.log('表单提交:', values)
        handleSynthesis()
    }
    const showCubeContentDialog = ref(false)
    const cubeContent = computed(() => {
        return {
            ...formData,
            scenes: sceneList.value.filter(item => formData.sensors.includes(item.sensorName) && formData.dates.some(d => dayjs(d).isSame(dayjs(item.sceneTime), 'day')))
        }
    })
    return {
        selectedGrid,
        updateGridLayer,
        formData,
        sensorOptions,
        bandOptions,
        dateOptions,
        canSynthesize,
        handleSensorChange,
        handleBandChange,
        handleDateChange,
        handleSynthesis,
        onFinish,
        cubeContent,
        showCubeContentDialog
    }
}
