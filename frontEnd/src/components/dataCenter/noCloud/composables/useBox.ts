import { computed, reactive, ref, watch } from 'vue'
import { calTask, exploreData, setCurrentPanel, taskStore } from './shared'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import * as DataPreparationMapOps from '@/util/map/operation/data-preparation'
import type { MapMouseEvent } from 'mapbox-gl'
import { ezStore } from '@/store'
import { getScenesInGrid } from '@/api/http/interactive-explore/grid.api'
import type { Grid } from '@/api/http/interactive-explore/grid.type'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { addCube, getCube } from '@/api/http/analytics-display/cube.api'
import type { CubeRequest } from '@/api/http/analytics-display/cube.type'
import { NumberFormat } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { getEOCube } from '@/api/http/satellite-data'
import { getGridSceneUrl, getImgStats, getMinIOUrl } from '@/api/http/interactive-explore'
import type { GridData } from '@/type/interactive-explore/grid'
import * as GridExploreMapOps from '@/util/map/operation/grid-explore'

type GridInfo = {
    columnId: number
    rowId: number
    resolution: number
}
type SceneImage = {
    bucket: string
    tifPath: string
    band: string
}
type Scene = {
    sceneId: string
    sceneTime: Dayjs
    sensorName: string
    platformName: string
    productName: string
    noData: number
    images: SceneImage[]
    bandMapper: any
    boundingBox: any
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
    console.log(e)
    console.log(e.features)
    if (!e.features || e.features.length === 0) {
        console.warn('点击事件触发，但未捕获到 Feature。可能是渲染未完成或图层遮挡。')
        return
    }

    const f = e.features[0]
    
    // 确保属性存在
    if (!f.properties) return message.warning("属性不存在")

    // 更新 Ref
    selectedGrid.value = {
        columnId: f.properties.columnId,
        rowId: f.properties.rowId,
        resolution: f.properties.resolution,
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

/**
 * 时序立方体传感器数据列表
 */
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
        strategy: 'upscale' as string | undefined,
        period: 'month' as string | undefined,
    })

    watch(selectedGrid, () => {
        formData.sensors = []
        formData.dates = []
        formData.bands = []
    })

    // 波段option
    const bandOptions = ref([
        { value: 'Red', label: 'Red', color: '#ff4d4f' },
        { value: 'Green', label: 'Green', color: '#52c41a' },
        { value: 'Blue', label: 'Blue', color: '#1890ff' },
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

    // 判断现在是否可以合成
    const canSynthesize = computed(() => {
        return (
            formData.sensors.length > 0 &&
            formData.bands.length > 0 &&
            formData.dates.length > 0 &&
            formData.strategy &&
            formData.period
        )
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

    // 生成cube缓存
    const handleGenCache = async () => {
        try {
            DataPreparationMapOps.map_destrod3DBoxLayer()
            showCubeContentDialog.value = true
            const res = await addCube(cubeContent.value)
            currentCacheKey.value = res.data
            console.log(await getCube())
            let gridGeoJson = {}
            for (const grid of exploreData.grids) {
                if (
                    selectedGrid.value?.columnId === grid.columnId &&
                    selectedGrid.value?.resolution === grid.resolution &&
                    selectedGrid.value?.rowId === grid.rowId
                ) {
                    gridGeoJson = grid.boundary
                }
            }
            // 取消高亮格网，并展示3D图层
            const id = 'grid-layer'
            const highlightId = id + '-highlight'
            ezStore.get('map').setFilter(highlightId, ['in', 'id', ''])
            DataPreparationMapOps.map_add3DBoxLayer(
                gridGeoJson,
                cubeSceneListByDate.value,
                cubeContent.value,
            )
        } catch (error) {
            console.log(error)
        } finally {
        }
    }

    // 合成时序立方体
    const handleSynthesis = async () => {
        // 因为从后端拿到taskId需要一定时间，所以先向任务store推送一个初始化任务状态
        taskStore.setIsInitialTaskPending(true)
        setCurrentPanel('history')

        let getEOCubeParam = {
            regionId: exploreData.regionCode,
            cloud: exploreData.cloud,
            resolution: exploreData.gridResolution,
            sceneIds: cubeContent.value.dimensionScenes.map((scene) => scene.sceneId),
            dataSet: { ...cubeContent.value, period: formData.period, resample: formData.strategy },
            bandList: cubeContent.value.dimensionBands,
        }

        // 发送请求
        console.log(getEOCubeParam, '发起请求')
        let startCalcRes = await getEOCube(getEOCubeParam)
        if (startCalcRes.message !== 'success') {
            message.error('时序立方体计算失败')
            console.error(startCalcRes)
            return
        }

        // 更新任务，跳转至历史panel
        calTask.value.taskId = startCalcRes.data
        taskStore.setTaskStatus(calTask.value.taskId, 'PENDING')
        taskStore.setIsInitialTaskPending(false)
    }

    const handleReset = () => {
        DataPreparationMapOps.map_destrod3DBoxLayer()
        GridExploreMapOps.map_destroyGridSceneLayer(selectedGrid.value as any)
        selectedGrid.value = undefined
        sensorOptions.value = []
    }

    const onFinish = (values: any) => {
        console.log('表单提交:', values)
        handleGenCache()
    }

    const showCubeContentDialog = ref(false)

    const currentCacheKey = ref('')

    // 缓存内容
    const cubeContent = computed(() => {
        return {
            cubeId: `${selectedGrid.value?.rowId}-${selectedGrid.value?.columnId}-${selectedGrid.value?.resolution}`,
            dimensionSensors: formData.sensors,
            dimensionDates: formData.dates,
            dimensionBands: formData.bands,
            dimensionScenes: sceneList.value.filter(
                (item) =>
                    formData.sensors.includes(item.sensorName) &&
                    formData.dates.some((d) => dayjs(d).isSame(dayjs(item.sceneTime), 'day')),
            ),
        }
    })

    const cubeSceneListByDate = computed(() => {
        return Object.fromEntries(
            cubeContent.value.dimensionDates.map((date) => {
                return [
                    date,
                    sceneList.value.filter((item) =>
                        dayjs(date).isSame(dayjs(item.sceneTime), 'day'),
                    ),
                ]
            }),
        )
    })

    /**
     * 时序立方体可视化预览
     */

    // 表单数据
    const visFormData = reactive({
        R: undefined as string | undefined,
        G: undefined as string | undefined,
        B: undefined as string | undefined,
    })

    const availableScenes = computed(() => {
        const { sensors, bands, dates } = formData
        if (!sensors.length || !bands.length || !dates.length) return []
        return sceneList.value.filter(
            (item) =>
                sensors.includes(item.sensorName) &&
                dates.some((d) => dayjs(d).isSame(dayjs(item.sceneTime), 'day')),
        )
    })

    const mapToOptions = (scenes, channel) => {
        return scenes
            .map((scene) => {
                // 从 bandMapper 动态获取对应通道的波段号 (Red/Green/Blue)
                const targetBandId = scene.bandMapper[channel]
                // 找到对应的图片对象
                const targetImg = scene.images.find((img) => img.band == targetBandId)

                return targetImg
                    ? {
                          label: `${scene.platformName}-${dayjs(scene.sceneTime).format('YYYYMMDD')}-${channel[0]}`,
                          value: targetImg, // 这里的 value 是对象
                      }
                    : null
            })
            .filter(Boolean) // 过滤掉没找到图片的无效项
    }

    const cubeRedImages = computed(() => mapToOptions(availableScenes.value, 'Red'))
    const cubeGreenImages = computed(() => mapToOptions(availableScenes.value, 'Green'))
    const cubeBlueImages = computed(() => mapToOptions(availableScenes.value, 'Blue'))

    // 判断现在是否可以合成
    const canVisualize = computed(() => {
        return (
            formData.sensors.length > 0 &&
            formData.bands.length > 0 &&
            formData.dates.length > 0 &&
            visFormData.B &&
            visFormData.G &&
            visFormData.B
        )
    })

    watch(
        () => [formData.sensors, formData.dates, formData.bands],
        () => {
            // 重置预览表单
            visFormData.R = undefined
            visFormData.G = undefined
            visFormData.B = undefined
        },
        { deep: true },
    )

    const handleVisualize = async () => {
        const currentGridKey = `${selectedGrid.value?.rowId}-${selectedGrid.value?.columnId}-${selectedGrid.value?.resolution}`

        console.log('red, green, blue', visFormData.R, visFormData.G, visFormData.B)
        let redPath = visFormData.R
        let greenPath = visFormData.G
        let bluePath = visFormData.B
        const cache = ezStore.get('statisticCache')
        const promises: any = []
        let [
            min_r,
            max_r,
            min_g,
            max_g,
            min_b,
            max_b,
            mean_r,
            mean_g,
            mean_b,
            std_r,
            std_g,
            std_b,
        ] = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

        if (cache.get(redPath) && cache.get(greenPath) && cache.get(bluePath)) {
            console.log('cache hit!')
            ;[min_r, max_r, mean_r, std_r] = cache.get(redPath)
            ;[min_g, max_g, mean_g, std_g] = cache.get(greenPath)
            ;[min_b, max_b, mean_b, std_b] = cache.get(bluePath)
        }
        promises.push(
            getImgStats(getMinIOUrl(redPath!)),
            getImgStats(getMinIOUrl(greenPath!)),
            getImgStats(getMinIOUrl(bluePath!)),
        )
        await Promise.all(promises).then((values) => {
            min_r = values[0].b1.min
            max_r = values[0].b1.max
            min_g = values[1].b1.min
            max_g = values[1].b1.max
            min_b = values[2].b1.min
            max_b = values[2].b1.max
            mean_r = values[0].b1.mean
            std_r = values[0].b1.std
            mean_g = values[1].b1.mean
            std_g = values[1].b1.std
            mean_b = values[2].b1.mean
            std_b = values[2].b1.std
        })

        cache.set(redPath, [min_r, max_r, mean_r, std_r])
        cache.set(greenPath, [min_g, max_g, mean_g, std_g])
        cache.set(bluePath, [min_b, max_b, mean_b, std_b])

        console.log(
            min_r,
            max_r,
            min_g,
            max_g,
            min_b,
            max_b,
            mean_r,
            mean_g,
            mean_b,
            std_r,
            std_g,
            std_b,
        )
        const url = getGridSceneUrl(selectedGrid.value as GridData, {
            redPath: redPath!,
            greenPath: greenPath!,
            bluePath: bluePath!,
            r_min: min_r,
            r_max: max_r,
            g_min: min_g,
            g_max: max_g,
            b_min: min_b,
            b_max: max_b,
        })
        
        GridExploreMapOps.map_addGridSceneLayer(selectedGrid.value as GridData, url)
    }

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
        handleGenCache,
        handleSynthesis,
        handleReset,
        onFinish,
        cubeContent,
        cubeSceneListByDate,
        currentCacheKey,
        showCubeContentDialog,
        cubeRedImages,
        cubeGreenImages,
        cubeBlueImages,
        visFormData,
        canVisualize,
        handleVisualize,
    }
}
