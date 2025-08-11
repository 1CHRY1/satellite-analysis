import { computed, ref } from "vue";
import * as MapOperation from '@/util/map/operation'
import * as InteractiveExploreMapOps from '@/util/map/operation/interactive-explore'
import { getRealtimeNoCloudUrl } from "@/api/http/satellite-data/visualize.api"
import { ElMessage } from 'element-plus'
import { useI18n } from "vue-i18n";
import { ezStore } from "@/store"
import { getDEMUrl, getNDVIOrSVRUrl, getSceneUrl, getVectorUrl, get3DUrl } from "@/api/http/interactive-explore/visualize.api";
import type { Marker } from 'mapbox-gl'
import type { POIInfo, ProductType } from '@/type/interactive-explore/filter'
import type { AttrSymbology, VectorSymbology } from '@/type/interactive-explore/visualize'
import * as CommonMapOps from '@/util/map/operation/common'
import mapboxgl from 'mapbox-gl'
import { mapManager } from '@/util/map/mapManager'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import { searchedSpatialFilterMethod, finalLandId, curGridsBoundary, vectorStats, selectedGridResolution, vectorSymbology } from "./shared"
import { message } from "ant-design-vue";
import { getVectorAttr } from "@/api/http/satellite-data/satellite.api";
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
    const selectedSensorName = ref('')
    const showSceneResult = async (sensorName: string) => {
        destroyScene()
        InteractiveExploreMapOps.map_fitViewToTargetZoom(11)
        const stopLoading = message.loading('正在加载，请稍后...', 0)
        setTimeout(() => {
            handleShowScene(sensorName)
        }, 1000)
        setTimeout(() => {
            stopLoading()
        }, 5000)
    }
    const handleShowScene = async (sensorName: string) => {
        const url = getSceneUrl(sensorName)
        InteractiveExploreMapOps.map_addSceneLayer(url)
    }
    const destroyExploreLayers = () => {
        destroyScene()
        destroyVector()
        destroyProduct()
    }
    const destroyScene = () => {
        InteractiveExploreMapOps.map_destroySceneLayer()
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
        '#ff4500',
        '#ff8c00',
        '#ffd700',
        '#90ee90',
        '#00ced1',
        '#1e90ff',
        '#c71585',
        'rgba(255, 69, 0, 0.68)',
        'rgb(255, 120, 0)',
        'hsv(51, 100, 98)',
        'hsva(120, 40, 94, 0.5)',
        'hsl(181, 100%, 37%)',
        'hsla(209, 100%, 56%, 0.73)',
        '#c7158577',
    ])
    const getVectorSymbology = async () => {
        for (const vector of vectorStats.value) {
            let result: Array<AttrSymbology> = []
            try {
                // 初始化
                vectorSymbology.value[vector.tableName] = {
                    attrs: [],
                    checkAll: false,
                    isIndeterminate: true,
                    checkedAttrs: []
                }
                result = await getVectorAttr(vector.tableName)
                console.log('result', result)
                vectorSymbology.value[vector.tableName].attrs = result.map((item, index) => {
                    return {
                        ...item,
                        color: predefineColors.value[index % predefineColors.value.length],
                    }
                })
                vectorSymbology.value[vector.tableName].checkedAttrs = result.map(item => {
                    return item.label
                })
            } catch (e) {
                console.error(`[${vector.tableName}] 请求失败:`, e);
            }
        }
        ezStore.set("vectorSymbology", vectorSymbology.value)
        console.log('vectorSymbology', vectorSymbology.value)
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
    }
    const handleCheckedAttrsChange = (tableName: string, value: string[]) => {
        console.log(value)
        const checkedCount = value.length
        const attrs = vectorSymbology.value[tableName].attrs
        vectorSymbology.value[tableName].checkAll = checkedCount === attrs.length
        vectorSymbology.value[tableName].isIndeterminate = checkedCount > 0 && checkedCount < attrs.length
        console.log(vectorSymbology.value[tableName])
    }

    const showVectorResult = async (tableName: string, index: number) => {
        if (tableName === '') {
            ElMessage.warning(t('datapage.explore.message.filtererror_choose'))
            return
        }
        const stopLoading = message.loading('正在加载，请稍后...', 0)
        previewVectorIndex.value = index
        previewVectorList.value[index] = true
        handleShowVector(tableName, finalLandId.value)
        setTimeout(() => {
            stopLoading()
        }, 5000)
    }
    const handleShowVector = async(source_layer: string, landId: string) => {
        const attrList = vectorSymbology.value[source_layer].checkedAttrs.map(item => {
            const targetAttr = vectorSymbology.value[source_layer].attrs.find(i => i.label === item)
            return targetAttr
        })
        for (const attr of attrList) {
            const url = getVectorUrl({
                landId,
                source_layer,
                spatialFilterMethod: searchedSpatialFilterMethod.value,
                resolution: selectedGridResolution.value,
                type: attr?.type
            })
            InteractiveExploreMapOps.map_addMVTLayer(source_layer, url, attr?.color || '#0066cc', attr?.type)
        }
    }
    const destroyVector = (index?: number) => {
        if (index !== undefined) {
            previewVectorIndex.value = null
            previewVectorList.value[index] = false
        }
        InteractiveExploreMapOps.map_destroyMVTLayer()
    }
    

    /**
     * 5. 交互探索 - 栅格专题可视化
     */
    const showProductResult = async (dataType: ProductType, themeName: string) => {
        const stopLoading = message.loading('正在加载，请稍后...', 0)
        destroyProduct(dataType)
        handleShowProduct(themeName, dataType)
        setTimeout(() => {
            stopLoading()
        }, 5000)
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
        const url = await getDEMUrl(themeName, curGridsBoundary.value)
        InteractiveExploreMapOps.map_addDEMLayer(url)
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
            destroyDEM()
            destroy3D()
            destroyNDVIOrSVR()
            // TODO: 删除其他产品图层
        } else {
            switch (dataType) {
                case 'dem':
                    destroyDEM()
                    break
                case 'dsm':
                    destroyDEM()
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