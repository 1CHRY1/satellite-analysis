import { computed, ref } from "vue";
import * as MapOperation from '@/util/map/operation'
import * as InteractiveExploreMapOps from '@/util/map/operation/interactive-explore'
import { getRealtimeNoCloudUrl } from "@/api/http/satellite-data/visualize.api"
import { ElMessage } from 'element-plus'
import { useI18n } from "vue-i18n";
import { ezStore } from "@/store"
import { getDEMUrl, getSceneUrl, getVectorUrl } from "@/api/http/interactive-explore/visualize.api";
import type { Marker } from 'mapbox-gl'
import type { POIInfo } from '@/type/interactive-explore/filter'
import * as CommonMapOps from '@/util/map/operation/common'
import mapboxgl from 'mapbox-gl'
import { mapManager } from '@/util/map/mapManager'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import { searchedSpatialFilterMethod, finalLandId, curGridsBoundary, vectorStats } from "./shared"
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
        InteractiveExploreMapOps.map_destroyGridLayer()
        InteractiveExploreMapOps.map_addGridLayer(gridFeature)
        // InteractiveExploreMapOps.draw_deleteAll()
    }


    /**
     * 3. 交互探索 - 影像可视化
     */
    const selectedSensorName = ref('')
    const showSceneResult = async (sensorName: string) => {
        destroyScene()
        handleShowScene(sensorName)
    }
    const handleShowScene = async (sensorName: string) => {
        const url = getSceneUrl(sensorName)
        InteractiveExploreMapOps.map_addSceneLayer(url)
    }
    const destroyExploreLayers = () => {
        destroyScene()
        destroyVector()
        destroyDEM()
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

    const showVectorResult = async (tableName: string, index: number) => {
        if (tableName === '') {
            ElMessage.warning(t('datapage.explore.message.filtererror_choose'))
            return
        }
        previewVectorIndex.value = index
        previewVectorList.value[index] = true
        handleShowVector(tableName, finalLandId.value)
    }
    const handleShowVector = async(source_layer: string, landId: string) => {
        const url = getVectorUrl({
            landId,
            source_layer,
            spatialFilterMethod: searchedSpatialFilterMethod.value,
        })
        InteractiveExploreMapOps.map_addMVTLayer(source_layer, url)
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
    const showProductResult = async (dataType: string, themeName: string) => {
        destroyProduct()
        handleShowProduct(themeName, dataType)
    }
    const handleShowProduct = (themeName: string, dataType: string) => {
        switch (dataType) {
            case 'dem':
                handleShowDEM(themeName)
                break
            case 'dsm':
                break
        }
    }
    const handleShowDEM = async(themeName: string) => {
        const url = getDEMUrl(themeName, curGridsBoundary.value)
        InteractiveExploreMapOps.map_addDEMLayer(url)
    }
    const destroyProduct = () => {
        destroyDEM()
        // TODO: 删除其他产品图层
    }
    const destroyDEM = () => {
        InteractiveExploreMapOps.map_destroyDEMLayer()
    }

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
            showProductResult(category, themeName)
        } else {
            destroyProduct()
        }
    };
    // 判断当前应该显示 Eye 还是 EyeOff
    const shouldShowEyeOff = (label: string, index: number) => {
        const key = `${label}_${index}`;
        return eyeStates.value[key];
    };



    /**
     * 1.遥感影像可视化
     */
    // 创建无云一版图瓦片
    // const handleCreateNoCloudTiles = async (sceneIds: any[] | Event) => {
    //     try {
    //         let finalSceneIds;

    //         // 如果没有传入sceneIds，则自己构建
    //         if (!sceneIds || 
    //             sceneIds instanceof Event || 
    //             sceneIds instanceof PointerEvent || 
    //             !Array.isArray(sceneIds) || 
    //             sceneIds.length === 0) {
    //             // 如果不是有效数组，则自己构建
    //             finalSceneIds = allScenes.value.map((item: any) => item.sceneId);
    //         } else {
    //             // 如果是有效数组，直接使用
    //             finalSceneIds = sceneIds;
    //         }
    //         const param = {
    //             sceneIds: finalSceneIds,
    //         }
    //         console.log(param)

    //         console.log('创建无云一版图配置参数:', param)

    //         // 2. 创建配置
    //         const response = await fetch('/api/modeling/example/noCloud/createNoCloudConfig', {
    //             method: 'POST',
    //             body: JSON.stringify(param),
    //             headers: {
    //                 'Content-Type': 'application/json',
    //                 'Authorization': 'Bearer ' + localStorage.getItem('token'),
    //             },
    //         })
    //         const result = await response.json()
    //         const jsonUrl = result.data  // 从CommonResultVO中获取data字段

    //         console.log('获取到的jsonUrl:', jsonUrl)

    //         // 3. 添加瓦片图层

    //         // 清除旧的无云图层
    //         MapOperation.map_destroyNoCloudLayer()

    //         // 添加新的瓦片图层
    //         const url = getOnTheFlyUrl(jsonUrl)
    //         MapOperation.map_addNoCloudLayer(url)

    //         console.log('无云一版图瓦片图层已添加到地图')

    //     } catch (error) {
    //         console.error('创建无云一版图瓦片失败:', error)
    //     }
    // }

    const handleCreateNoCloudTiles = async (sensorName: string, startTime: string, endTime: string) => {
        // 清除旧的无云图层
        MapOperation.map_destroyNoCloudLayer()

        // 添加新的瓦片图层
        const url = getRealtimeNoCloudUrl({ sensorName, startTime, endTime })
        MapOperation.map_addNoCloudLayer(url)
    }
    const handleShowImageInBoundary = async (sensorName: string, dateRange: Array<any>) => {
        const startTime = dateRange[0].format('YYYY-MM-DD')
        const endTime = dateRange[1].format('YYYY-MM-DD')
        handleCreateNoCloudTiles(sensorName, startTime, endTime)

        ////////////// OLD START: Load Image On the Fly //////////////
        // const sceneIds = getSceneIdsByPlatformName(label, platformName, allScenes.value)
        // handleCreateNoCloudTiles(sceneIds)
        ////////////// OLD END //////////////

        ////////////// OLD START: Load Image One By One //////////////
        // const sceneIds = getSceneIdsByPlatformName(label, platformName, allScenes.value)
        // console.log('选中的景ids', sceneIds)
        // console.log('当前所有的景', allScenes.value)
        // const sensorName = getSensorNamebyPlatformName(resolutionPlatformSensor[label], allScenes.value)

        // console.log('匹配的sensorName', sensorName)

        // const stopLoading = message.loading(t('datapage.explore.message.load'))

        // let coverScenes, gridsBoundary
        // if (searchedSpatialFilterMethod.value === 'region') {
        //     const params = {
        //         sensorName,
        //         sceneIds,
        //         regionId: finalLandId.value,
        //         resolution: selectedGrid.value,
        //     }
        //     const coverScenesRes = await getCoverRegionSensorScenes(params)
        //     coverScenes = coverScenesRes.sceneList
        //     gridsBoundary = coverScenesRes.gridsBoundary
        // } else if (searchedSpatialFilterMethod.value === 'poi') {
        //     const params = {
        //         sensorName,
        //         sceneIds,
        //         locationId: finalLandId.value,
        //         resolution: selectedGrid.value,
        //     }
        //     const coverScenesRes = await getCoverRegionSensorScenes(params)
        //     coverScenes = coverScenesRes.sceneList
        //     gridsBoundary = coverScenesRes.gridsBoundary
        // }
        // console.log('接口返回：覆盖的景们', coverScenes)
        // // 临时做法！！！！！！！！！！！！！！！
        // // 获取DEM的产品作为底图加上来
        // let demProducts
        // let productSceneIds: any[] = []
        // let productSensorName = ''
        // allProducts.value.forEach(product => {
        //     if (product.dataType === 'dem') {
        //         productSceneIds.push(product.sceneId)
        //         productSensorName = product.sensorName
        //     }
        // })
        // if (searchedSpatialFilterMethod.value === 'region') {
        //     const params = {
        //         sensorName: productSensorName,
        //         sceneIds: productSceneIds,
        //         regionId: finalLandId.value,
        //         resolution: selectedGrid.value,
        //     }
        //     const coverProductsRes = await getCoverRegionSensorScenes(params)
        //     demProducts = coverProductsRes.sceneList
        //     gridsBoundary = coverProductsRes.gridsBoundary
        // } else if (searchedSpatialFilterMethod.value === 'poi') {
        //     const params = {
        //         sensorName: productSensorName,
        //         sceneIds: productSceneIds,
        //         locationId: finalLandId.value,
        //         resolution: selectedGrid.value,
        //     }
        //     const coverProductsRes = await getCoverRegionSensorScenes(params)
        //     demProducts = coverProductsRes.sceneList
        // }
        // console.log('scene:demProducts', demProducts)
        // await addTerrainBaseMap(demProducts, gridsBoundary)
        // await addMultiRGBImageTileLayer(coverScenes, gridsBoundary, stopLoading)
        ////////////////// OLD END //////////////////////
    }

    /**
     * 2.产品可视化
     */

    

    // const handleShowProductInBoundary = async (label: string, platformName: string) => {
        // console.log('label', label)
        // const sceneIds = getSceneIdsByPlatformName(label, platformName, allProducts.value)
        // console.log('选中的景ids', sceneIds)
        // console.log('当前所有的产品', allProducts.value)
        // const sensorName = getSensorNamebyPlatformName(platformName, allProducts.value)
        // 之所以是sensorName，因为后台统一存在sensor表
        // console.log('匹配的sensorName', sensorName)

        // const stopLoading = message.loading(t('datapage.explore.message.load'))

        // let coverProducts, gridsBoundary
        // if (searchedSpatialFilterMethod.value === 'region') {
        //     const params = {
        //         sensorName,
        //         sceneIds,
        //         regionId: finalLandId.value,
        //         resolution: selectedGridResolution.value,
        //     }
        //     const coverProductsRes = await getCoverRegionSensorScenes(params)
        //     coverProducts = coverProductsRes.sceneList
        //     gridsBoundary = coverProductsRes.gridsBoundary
        // } else if (searchedSpatialFilterMethod.value === 'poi') {
        //     const params = {
        //         sensorName,
        //         sceneIds,
        //         locationId: finalLandId.value,
        //         resolution: selectedGridResolution.value,
        //     }
        //     const coverProductsRes = await getCoverRegionSensorScenes(params)
        //     coverProducts = coverProductsRes.sceneList
        //     gridsBoundary = coverProductsRes.gridsBoundary
        // }
        // console.log('接口返回：覆盖的产品们', coverProducts)

        // let demProducts
        // let productSceneIds: any[] = []
        // let productSensorName = ''
        // allProducts.value.forEach(product => {
        //     if (product.dataType === 'dem') {
        //         productSceneIds.push(product.sceneId)
        //         productSensorName = product.sensorName
        //     }
        // })
        // if (searchedSpatialFilterMethod.value === 'region') {
        //     const params = {
        //         sensorName: productSensorName,
        //         sceneIds: productSceneIds,
        //         regionId: finalLandId.value,
        //         resolution: selectedGrid.value,
        //     }
        //     const coverProductsRes = await getCoverRegionSensorScenes(params)
        //     demProducts = coverProductsRes.sceneList
        //     gridsBoundary = coverProductsRes.gridsBoundary
        // } else if (searchedSpatialFilterMethod.value === 'poi') {
        //     const params = {
        //         sensorName: productSensorName,
        //         sceneIds: productSceneIds,
        //         locationId: finalLandId.value,
        //         resolution: selectedGrid.value,
        //     }
        //     const coverProductsRes = await getCoverRegionSensorScenes(params)
        //     demProducts = coverProductsRes.sceneList
        // }

        // await addTerrainBaseMap(demProducts, gridsBoundary)
        // switch (label) {
        //     case 'DEM':
        //         await addMultiTerrainTileLayer(coverProducts, gridsBoundary, stopLoading)
        //         break
        //     case '红绿立体':
        //         await addMulti3DImageTileLayer(coverProducts, gridsBoundary, stopLoading)
        //         break
        //     case '形变速率':
        //         await addMultiOneBandColorLayer(coverProducts, gridsBoundary, stopLoading)
        //         break
        //     case 'NDVI':
        //         await addMultiOneBandColorLayer(coverProducts, gridsBoundary, stopLoading)
        //         break
        //     default:
        //         await addMultiRGBImageTileLayer(coverProducts, gridsBoundary, stopLoading)
        //         break
        // }
    // }

    /**
     * 3.矢量可视化
     */
    


    /**
     * 4.DEM底图可视化
     */
    const addTerrainBaseMap = async (demProducts: any[], gridsBoundary: any) => {
        if (demProducts.length === 0) {
            return
        }
        let targetProduct = demProducts[0]
        if (!targetProduct)
            return
        if (ezStore.get('currentTerrainBaseMap')) {
            let terrainType = ezStore.get('currentTerrainBaseMap')
            switch (terrainType) {
                case 'DEM':
                    // await addBaseTerrainTileLayer(targetProduct, gridsBoundary)
                    break
                case 'DSM':
                    break
            }
        }
    }

    return {
        eyeStates,
        addTerrainBaseMap,
        toggleEye,
        showSceneResult,
        showProductResult,
        shouldShowEyeOff,
        previewVectorList,
        showVectorResult,
        selectedSensorName,
        destroyExploreLayers,
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
    }
}