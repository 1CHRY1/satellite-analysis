import { computed, ref } from "vue";
import { useLayer } from "./useLayer";
import { useFilter } from "./useFilter";
import { useStats } from "./useStats";
import * as MapOperation from '@/util/map/operation'
import { getOnTheFlyUrl, getRealtimeNoCloudUrl } from "@/api/http/satellite-data/visualize.api"
import { ElMessage } from 'element-plus'
import { message } from 'ant-design-vue'
import { useI18n } from "vue-i18n";
import { getCoverRegionSensorScenes } from "@/api/http/satellite-data";
import { ezStore } from "@/store"


// 使用一个对象来存储每个 Product Item 的显示状态
const eyeStates = ref({});
const { clearAllShowingSensor } = useLayer()

const { getSceneIdsByPlatformName, getSensorNamebyPlatformName } = useStats()
const { addMultiTerrainTileLayer, addMulti3DImageTileLayer, addMultiOneBandColorLayer, addMultiRGBImageTileLayer, addMVTLayer, addBaseTerrainTileLayer } = useLayer()

export const useVisualize = () => {
    // 使用useFilter中的模块作用域变量，注意避免循环依赖
    const { allScenes, allProducts, searchedSpatialFilterMethod, finalLandId, selectedGrid, allVectors } = useFilter()
    const { t } = useI18n()

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

    const toggleEye = (label: string, index: number, platformName: string) => {
        const key = `${label}_${index}`;
        let isShow = eyeStates.value[key];
        eyeStates.value[key] = !isShow;
        if (eyeStates.value[key]) {
            Object.keys(eyeStates.value).forEach(item => {
                if (item !== key) {
                    eyeStates.value[item] = false
                }
            })
            showProductResult(label, platformName)
        } else {
            unPreviewProduct()
        }
    };
    // 判断当前应该显示 Eye 还是 EyeOff
    const shouldShowEyeOff = (label: string, index: number) => {
        const key = `${label}_${index}`;
        return eyeStates.value[key];
    };

    const showProductResult = async (label: string, platformName: string) => {
        clearAllShowingSensor()
        handleShowProductInBoundary(label, platformName)
    }

    const unPreviewProduct = () => {
        clearAllShowingSensor()
    }

    const handleShowProductInBoundary = async (label: string, platformName: string) => {
        console.log('label', label)
        const sceneIds = getSceneIdsByPlatformName(label, platformName, allProducts.value)
        console.log('选中的景ids', sceneIds)
        console.log('当前所有的产品', allProducts.value)
        const sensorName = getSensorNamebyPlatformName(platformName, allProducts.value)
        // 之所以是sensorName，因为后台统一存在sensor表
        console.log('匹配的sensorName', sensorName)

        const stopLoading = message.loading(t('datapage.explore.message.load'))

        let coverProducts, gridsBoundary
        if (searchedSpatialFilterMethod.value === 'region') {
            const params = {
                sensorName,
                sceneIds,
                regionId: finalLandId.value,
                resolution: selectedGrid.value,
            }
            const coverProductsRes = await getCoverRegionSensorScenes(params)
            coverProducts = coverProductsRes.sceneList
            gridsBoundary = coverProductsRes.gridsBoundary
        } else if (searchedSpatialFilterMethod.value === 'poi') {
            const params = {
                sensorName,
                sceneIds,
                locationId: finalLandId.value,
                resolution: selectedGrid.value,
            }
            const coverProductsRes = await getCoverRegionSensorScenes(params)
            coverProducts = coverProductsRes.sceneList
            gridsBoundary = coverProductsRes.gridsBoundary
        }
        console.log('接口返回：覆盖的产品们', coverProducts)

        let demProducts
        let productSceneIds: any[] = []
        let productSensorName = ''
        allProducts.value.forEach(product => {
            if (product.dataType === 'dem') {
                productSceneIds.push(product.sceneId)
                productSensorName = product.sensorName
            }
        })
        if (searchedSpatialFilterMethod.value === 'region') {
            const params = {
                sensorName: productSensorName,
                sceneIds: productSceneIds,
                regionId: finalLandId.value,
                resolution: selectedGrid.value,
            }
            const coverProductsRes = await getCoverRegionSensorScenes(params)
            demProducts = coverProductsRes.sceneList
            gridsBoundary = coverProductsRes.gridsBoundary
        } else if (searchedSpatialFilterMethod.value === 'poi') {
            const params = {
                sensorName: productSensorName,
                sceneIds: productSceneIds,
                locationId: finalLandId.value,
                resolution: selectedGrid.value,
            }
            const coverProductsRes = await getCoverRegionSensorScenes(params)
            demProducts = coverProductsRes.sceneList
        }

        await addTerrainBaseMap(demProducts, gridsBoundary)
        switch (label) {
            case 'DEM':
                await addMultiTerrainTileLayer(coverProducts, gridsBoundary, stopLoading)
                break
            case '红绿立体':
                await addMulti3DImageTileLayer(coverProducts, gridsBoundary, stopLoading)
                break
            case '形变速率':
                await addMultiOneBandColorLayer(coverProducts, gridsBoundary, stopLoading)
                break
            case 'NDVI':
                await addMultiOneBandColorLayer(coverProducts, gridsBoundary, stopLoading)
                break
            default:
                await addMultiRGBImageTileLayer(coverProducts, gridsBoundary, stopLoading)
                break
        }
    }

    /**
     * 3.矢量可视化
     */
    const previewVectorList = computed<boolean[]>(() => {
        const list = Array(allVectors.value.length).fill(false)
        if (previewVectorIndex.value !== null) {
            list[previewVectorIndex.value] = true
        }
        return list
    })
    const previewVectorIndex = ref<number | null>(null)
    const showVectorResult = async (tableName: string) => {
        previewVectorIndex.value = allVectors.value.findIndex(item => item.tableName === tableName)
        handleShowVectorInBoundary(tableName)
    }
    const unPreviewVector = () => {
        previewVectorIndex.value = null
        MapOperation.map_destroyMVTLayer()
    }

    const handleShowVectorInBoundary = async (source_layer: string) => {
        if (source_layer === '') {
            ElMessage.warning(t('datapage.explore.message.filtererror_choose'))
            return
        }
        console.log('source_layer', source_layer)
        await addMVTLayer(source_layer, finalLandId.value)
    }


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
                    await addBaseTerrainTileLayer(targetProduct, gridsBoundary)
                    break
                case 'DSM':
                    break
            }
        }
    }

    return {
        eyeStates,
        addTerrainBaseMap,
        handleShowImageInBoundary,
        handleShowProductInBoundary,
        handleShowVectorInBoundary,
        handleCreateNoCloudTiles,
        toggleEye,
        unPreviewProduct,
        showProductResult,
        shouldShowEyeOff,
        previewVectorList,
        showVectorResult,
        unPreviewVector
    }
}