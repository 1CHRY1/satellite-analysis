import { computed, ref } from "vue";
import * as MapOperation from '@/util/map/operation'
import * as InteractiveExploreMapOps from '@/util/map/operation/interactive-explore'
import { getRealtimeNoCloudUrl } from "@/api/http/satellite-data/visualize.api"
import { useI18n } from "vue-i18n";
import { ezStore } from "@/store"
import { getDEMUrl, getNDVIOrSVRUrl, getSceneUrl, getVectorUrl, get3DUrl, getLargeSceneMosaicUrl, getLargeSceneUrl, get2DDEMUrl } from "@/api/http/interactive-explore/visualize.api";
import type { Marker } from 'mapbox-gl'
import type { POIInfo, ProductType } from '@/type/interactive-explore/filter'
import type { AttrSymbology, VectorSymbology } from '@/type/interactive-explore/visualize'
import * as CommonMapOps from '@/util/map/operation/common'
import mapboxgl from 'mapbox-gl'
import { mapManager } from '@/util/map/mapManager'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import { searchedSpatialFilterMethod, finalLandId, curGridsBoundary, vectorStats, selectedGridResolution, vectorSymbology, selectedDateRange } from "./shared"
import { message } from "ant-design-vue";
import { cancelCase, getCaseResult, getVectorAttr, getVectorCustomAttr, pollStatus } from "@/api/http/satellite-data/satellite.api";
import { calTask } from "../noCloud/composables/shared";
import bus from '@/store/bus'
// 使用一个对象来存储每个 Product Item 的显示状态
const eyeStates = ref({});

export const useVisualize = () => {
    /**
     * 0. 公用变量与函数
     */
    const marker = ref<Marker>()

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

    const { t } = useI18n()

    /**
     * 1. 数据检索 - 获取格网
     */

    /**
     * 添加多边形边界图层
     * @param boundary 边界
     */
    const addPolygonLayer = (boundary: any) => {
        InteractiveExploreMapOps.map_addPolygonLayer({
            geoJson: boundary,
            id: 'UniqueLayer',
            lineColor: '#8fffff',
            fillColor: '#a4ffff',
            fillOpacity: 0.2,
        })
    }
    const destroyUniqueLayer = () => {
        InteractiveExploreMapOps.map_destroyUniqueLayer()
    }

    /**
     * 添加POI标记点
     * @param selectedPOI 选中的POI
     */
    const addPOIMarker = (selectedPOI: POIInfo) => {
        mapManager.withMap((m) => {
            marker.value = new mapboxgl.Marker()
                .setLngLat([Number(selectedPOI?.gcj02Lon), Number(selectedPOI?.gcj02Lat)])
                .addTo(m);
        })
    }

    /**
     * 添加网格图层
     * @param gridRes 网格数据
     * @param window 窗口
     */
    const addGridLayer = (gridRes: any, window: any) => {
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
    
        InteractiveExploreMapOps.map_addGridLayer(gridFeature)
        InteractiveExploreMapOps.draw_deleteAll()
        // fly to
        CommonMapOps.map_fitView([
            [window.bounds[0], window.bounds[1]],
            [window.bounds[2], window.bounds[3]],
        ])
    }

    /**
     * 2. 数据检索 - 检索后
     */
    // addGridLayer是初步，这里是根据景的数量更新透明度
    const updateGridLayer = (allGrids: any) => {
        let gridFeature: FeatureCollection = {
            type: 'FeatureCollection',
            features: allGrids.map((item: any, index: number) => {
                return {
                    type: 'Feature',
                    geometry: item.boundary.geometry as Geometry,
                    properties: {
                        ...(item.properties || {}),
                        id: item.properties?.id ?? index, // 确保每个都有 id
                        // opacity: judgeGridOpacity(index, sceneGridsRes, totalImg),
                        opacity: 0.3,
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
        destroyGridLayer()
        InteractiveExploreMapOps.map_addGridLayer(gridFeature)
        // InteractiveExploreMapOps.draw_deleteAll()
    }
    const destroyGridLayer = () => {
        InteractiveExploreMapOps.map_destroyGridLayer()
    }


    /**
     * 3. 交互探索 - 影像可视化
     */
    const curTaskId = ref<string>('')
    const selectedSensorName = ref('')
    const showSceneResult = async (sensorName: string) => {
        destroyScene()
        InteractiveExploreMapOps.map_fitViewToTargetZoom(11)
        const stopLoading = message.loading('正在生成大范围图幅，请耐心等待...', 0)
        // 1. 创建大范围可视化的任务，轮询，最后返回 mosaicJson
        let taskRes = await getLargeSceneMosaicUrl({
            startTime: selectedDateRange.value[0].format('YYYY-MM-DD'),
            endTime: selectedDateRange.value[1].format('YYYY-MM-DD'),
            sensorName,
            // TODO: regionId(POI等待兼容！！！！)
            regionId: finalLandId.value
        })
        setTimeout(() => {
            handleShowScene(sensorName)
        }, 1000)
        let taskId = taskRes.data
        if (!taskId) {
            stopLoading()
            message.warning("任务创建失败")
            return
        }
        curTaskId.value = taskId
        
        try {
            await pollStatus(taskId)
            console.log('成功，开始拿结果')
    
            let res = await getCaseResult(taskId)
            console.log(res, '结果')
    
            // 1、先预览无云一版图影像
            let data = res.data
            const getData = async (taskId: string) => {
                let res:any
                while (!(res = await getCaseResult(taskId)).data) {
                    console.log('Retrying...')
                    await new Promise(resolve => setTimeout(resolve, 1000));
                }
                return res.data;
            }
            if(!data)
                data = await getData(taskId)
            if (!data.mosaicjson_url) {
                stopLoading()
                message.warning("计算失败，请重试")
                curTaskId.value = ''
                return
            }
            handleShowLargeScene(data.mosaicjson_url)
            stopLoading()
            message.success("计算完成")
            curTaskId.value = ''
        } catch (error) {
            console.log(error)
            stopLoading()
            if (curTaskId.value !== '')
                message.warning("计算失败，请重试")
            curTaskId.value = ''
        }
        setTimeout(() => {
            stopLoading()
        }, 500000)
    }
    const handleShowScene = async (sensorName: string) => {
        const url = getSceneUrl(sensorName)
        InteractiveExploreMapOps.map_addSceneLayer(url)
    }
    const handleShowLargeScene = async (mosaicUrl: string) => {
        const url = getLargeSceneUrl(mosaicUrl)
        InteractiveExploreMapOps.map_addLargeSceneLayer(url)
    }
    const cancelLargeScaleScene = async (taskId: string) => {
        if (taskId !== '') {
            try {
                await cancelCase(taskId)
                curTaskId.value = ''
            } catch (error) {
                console.error("任务取消失败")
            }
        }
    }
    const destroyExploreLayers = () => {
        destroyScene()
        destroyVector()
        destroyProduct()
    }
    const destroyScene = () => {
        cancelLargeScaleScene(curTaskId.value)
        InteractiveExploreMapOps.map_destroySceneLayer()
        InteractiveExploreMapOps.map_destroyLargeSceneLayer()
    }

    /**
     * 4. 交互探索 - 矢量可视化
     */
    // 可视化辅助变量
    const previewVectorList = computed<boolean[]>(() => {
        const list = Array(vectorStats.value.length).fill(false)
        if (previewVectorIndex.value !== null) {
            list[previewVectorIndex.value] = true
        }
        return list
    })
    const previewVectorIndex = ref<number | null>(null)

    const predefineColors = ref([
        "rgb(255, 69, 0)",             // #ff4500
        "rgb(255, 140, 0)",            // #ff8c00
        "rgb(255, 215, 0)",            // #ffd700
        "rgb(144, 238, 144)",          // #90ee90
        "rgb(0, 206, 209)",            // #00ced1
        "rgb(30, 144, 255)",           // #1e90ff
        "rgb(199, 21, 133)",           // #c71585
        "rgba(255, 69, 0, 0.68)",      // 已经是 rgba
        "rgb(255, 120, 0)",            // 已经是 rgb
        "rgb(249, 250, 0)",            // hsv(51, 100, 98)
        "rgba(71, 240, 71, 0.5)",      // hsva(120, 40, 94, 0.5)
        "rgb(0, 189, 189)",            // hsl(181, 100%, 37%)
        "rgba(0, 115, 255, 0.73)",     // hsla(209, 100%, 56%, 0.73)
        "rgba(199, 21, 133, 0.47)"     // #c7158577
        ])

    const getVectorSymbology = async () => {
        const tasks = vectorStats.value.map(async (vector) => {
            try {
                // 初始化
                vectorSymbology.value[vector.tableName] = {
                    attrs: [],
                    checkAll: false,
                    isIndeterminate: true,
                    checkedAttrs: [],
                    // selectedField: vector.fields.slice(-1)[0],
                    selectedField: undefined,
                    isRequesting: false
                }
                // const result = await getVectorAttr(vector.tableName)
                // vectorSymbology.value[vector.tableName].attrs = result.map((item, index) => ({
                //     ...item,
                //     color: predefineColors.value[index % predefineColors.value.length],
                // }))
                // vectorSymbology.value[vector.tableName].checkedAttrs = result.map(item => item.label)
            } catch (e) {
                console.error(`[${vector.tableName}] 请求失败:`, e)
            }
        })
        await Promise.all(tasks)
        ezStore.set("vectorSymbology", vectorSymbology.value)
        console.log('vectorSymbology', vectorSymbology.value)
    }
    const getAttrs4CustomField = async (tableName: string, field: string | undefined) => {
        if (!field) {
            message.warning("请先选择字段")
            return
        }
        vectorSymbology.value[tableName].isRequesting = true
        const result = await getVectorCustomAttr(tableName, field)
        vectorSymbology.value[tableName].attrs = result.map((item, index) => ({
            type: item,
            label: item,
            color: predefineColors.value[index % predefineColors.value.length],
        }))
        vectorSymbology.value[tableName].checkedAttrs = result
        vectorSymbology.value[tableName].isRequesting = false
    }
    const handleCheckAllChange = (tableName: string, val: boolean) => {
        console.log('all:', val)
        const item = vectorSymbology.value[tableName];
        // 使用解构赋值或 Vue.set 确保响应性
        vectorSymbology.value[tableName] = {
            ...item,
            checkedAttrs: val ? item.attrs.map(attr => attr.label) : [],
            isIndeterminate: false
        };
        console.log(vectorSymbology.value[tableName])
        const attrList = vectorSymbology.value[tableName].checkedAttrs.map(item => {
            const targetAttr = vectorSymbology.value[tableName].attrs.find(i => i.label === item)
            return targetAttr
        })
        InteractiveExploreMapOps.map_updateMVTLayerStyle(tableName, attrList as any, vectorSymbology.value[tableName].selectedField)
    }
    const handleCheckedAttrsChange = (tableName: string, value: string[]) => {
        console.log(value)
        const checkedCount = value.length
        const attrs = vectorSymbology.value[tableName].attrs
        vectorSymbology.value[tableName].checkAll = checkedCount === attrs.length
        vectorSymbology.value[tableName].isIndeterminate = checkedCount > 0 && checkedCount < attrs.length
        console.log(vectorSymbology.value[tableName])
        const attrList = vectorSymbology.value[tableName].checkedAttrs.map(item => {
            const targetAttr = vectorSymbology.value[tableName].attrs.find(i => i.label === item)
            return targetAttr
        })
        InteractiveExploreMapOps.map_updateMVTLayerStyle(tableName, attrList as any, vectorSymbology.value[tableName].selectedField)
    }

    const showVectorResult = async (tableName: string, index: number) => {
        if (tableName === '') {
            message.warning(t('datapage.explore.message.filtererror_choose'))
            return
        }
        destroyVector()
        previewVectorIndex.value = index
        handleShowVector(tableName, finalLandId.value)
        // setTimeout(() => {
        //     stopLoading()
        // }, 5000)
    }
    const handleShowVector = async (source_layer: string, landId: string) => {
        const curField = vectorSymbology.value[source_layer].selectedField
        const attrList = vectorSymbology.value[source_layer].checkedAttrs.map(item => {
            const targetAttr = vectorSymbology.value[source_layer].attrs.find(i => i.label === item)
            return targetAttr
        })
        console.log(attrList)

        const types = attrList
            .map(a => a?.type)
            // .filter((v): v is number => typeof v === 'number')
        console.log(types)
        const url = getVectorUrl({
            landId,
            source_layer,
            field: vectorSymbology.value[source_layer].selectedField || 'type',
            spatialFilterMethod: searchedSpatialFilterMethod.value,
            resolution: selectedGridResolution.value,
            type: types
        })

        // 添加到地图 - 点击事件处理已经在 map_addMVTLayer 函数中实现
        InteractiveExploreMapOps.map_addMVTLayer(source_layer, url, attrList as any, curField)
    }
    const destroyVector = (index?: number) => {
        if (index !== undefined) {
            previewVectorIndex.value = null
        }
        InteractiveExploreMapOps.map_destroyMVTLayer()
    }
    

    /**
     * 5. 交互探索 - 栅格专题可视化
     */
    const showProductResult = async (dataType: ProductType, themeName: string) => {
        destroyProduct(dataType)
        handleShowProduct(themeName, dataType)
    }
    const handleShowProduct = (themeName: string, dataType: string) => {
        switch (dataType) {
            case 'dem':
                handleShowDEM(themeName)
                break
            case 'dsm':
                handleShowDEM(themeName)
                break
            case 'ndvi':
                handleShowNDVIOrSVR(themeName)
                break
            case '3d':
                handleShow3D(themeName)
                break
            case 'svr':
                handleShowNDVIOrSVR(themeName)
                break
        }
    }
    const handleShowDEM = async(themeName: string) => {
        // const url = await getDEMUrl(themeName, curGridsBoundary.value)
        // InteractiveExploreMapOps.map_addDEMLayer(url)
        const url = await get2DDEMUrl(themeName, curGridsBoundary.value)
        InteractiveExploreMapOps.map_add2DDEMLayer(url)        
    }
    const handleShowNDVIOrSVR = async(themeName: string) => {
        const url = await getNDVIOrSVRUrl(themeName, curGridsBoundary.value)
        InteractiveExploreMapOps.map_addNDVIOrSVRLayer(url)
    }
    const handleShow3D = async(themeName: string) => {
        const url = await get3DUrl(themeName, curGridsBoundary.value)
        InteractiveExploreMapOps.map_add3DLayer(url)
    }
    const destroyProduct = (dataType?: ProductType) => {
        if (dataType === undefined) {
            destroy2DDEM()

            destroy3D()
            destroyNDVIOrSVR()
            // TODO: 删除其他产品图层
        } else {
            switch (dataType) {
                case 'dem':
                    destroy2DDEM()
                    break
                case 'dsm':
                    destroy2DDEM()
                    break
                case 'ndvi':
                    destroyNDVIOrSVR()
                    break
                case '3d':
                    destroy3D()
                    break
                case 'svr': 
                    destroyNDVIOrSVR()
                    break
            }
        }
    }
    const destroyDEM = () => {
        InteractiveExploreMapOps.map_destroyDEMLayer()
    }
    const destroy2DDEM = () => {
        InteractiveExploreMapOps.map_destroy2DDEMLayer()
    }
    const destroyNDVIOrSVR = () => {
        InteractiveExploreMapOps.map_destroyNDVIOrSVRLayer()
    }
    const destroy3D = () => {
        InteractiveExploreMapOps.map_destroy3DLayer()
    }

    /**
     * 交互探索 - 栅格专题可视化 - 切换显示
     * @param category 产品类型
     * @param index 产品索引
     * @param themeName 产品名称
     */
    const toggleEye = (category: string, index: number, themeName: string) => {
        const label = category
        const key = `${label}_${index}`;
        let isShow = eyeStates.value[key];
        eyeStates.value[key] = !isShow;
        if (eyeStates.value[key]) {
            Object.keys(eyeStates.value).forEach(item => {
                if (item !== key) {
                    eyeStates.value[item] = false
                }
            })
            showProductResult(category as ProductType, themeName)
        } else {
            destroyProduct(category as ProductType)
        }
    };
    // 判断当前应该显示 Eye 还是 EyeOff
    const shouldShowEyeOff = (label: string, index: number) => {
        const key = `${label}_${index}`;
        return eyeStates.value[key];
    };


    return {
        eyeStates,
        toggleEye,
        showSceneResult,
        showProductResult,
        shouldShowEyeOff,
        previewVectorList,
        showVectorResult,
        selectedSensorName,
        destroyExploreLayers,
        destroyGridLayer,
        destroyScene,
        destroyVector,
        getAttrs4CustomField,
        destroyDEM,
        destroyUniqueLayer,
        addPOIMarker,
        addGridLayer,
        updateGridLayer,
        addPolygonLayer,
        createGeoJSONFromBounds,
        marker,
        destroyProduct,
        predefineColors,
        getVectorSymbology,
        vectorSymbology,
        handleCheckAllChange,
        handleCheckedAttrsChange
    }
}