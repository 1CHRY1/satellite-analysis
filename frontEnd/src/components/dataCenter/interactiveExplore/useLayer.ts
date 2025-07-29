import { ref } from 'vue'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import type { Marker } from 'mapbox-gl'
import * as MapOperation from '@/util/map/operation'
import * as InteractiveExploreMapOps from '@/util/map/operation/interactive-explore'
import { mapManager } from '@/util/map/mapManager'
import mapboxgl from 'mapbox-gl'
import { getOneBandColorParamFromSceneObject, getRGBTileLayerParamFromSceneObject, getTerrainParamFromSceneObject } from '@/util/visualizeHelper'
import type { POIInfo } from '@/type/interactive-explore/filter'
import * as CommonMapOps from '@/util/map/operation/common'

/**
 * 图层可视化
 * @returns 
 */

export const useLayer = () => {
    
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

    const addMultiRGBImageTileLayer = async (coverScenes: any, gridsBoundary: any, stopLoading) => {
        const promises: Promise<any>[] = []
    
        for (let scene of coverScenes) {
            promises.push(getRGBTileLayerParamFromSceneObject(scene, gridsBoundary))
        } 
        const rgbTileLayerParamList = await Promise.all(promises)
    
        console.log('可视化参数们', rgbTileLayerParamList)
    
        MapOperation.map_addMultiRGBImageTileLayer(rgbTileLayerParamList, stopLoading)
    }

    const addMultiTerrainTileLayer = async (coverProducts: any, gridsBoundary: any, stopLoading) => {
        const promises: Promise<any>[] = []
        for (let scene of coverProducts) {
            promises.push(getTerrainParamFromSceneObject(scene, gridsBoundary))
        }
        const terrainTileLayerParamList = await Promise.all(promises)
        console.log('可视化参数们', terrainTileLayerParamList)

        MapOperation.map_addMultiTerrainTileLayer(terrainTileLayerParamList, stopLoading)
    }

    const addBaseTerrainTileLayer = async(product: any, gridsBoundary: any) => {
        const promises: Promise<any>[] = []
        console.log('product', product)
        promises.push(getTerrainParamFromSceneObject(product, gridsBoundary))
        const terrainTileLayerParamList = await Promise.all(promises)
        console.log('可视化参数们', terrainTileLayerParamList)

        MapOperation.map_addBaseTerrainTileLayer(terrainTileLayerParamList)
        // MapOperation.map_addMultiTerrainTileLayer(terrainTileLayerParamList)
    }

    const addMulti3DImageTileLayer = async (coverProducts: any, gridsBoundary: any, stopLoading) => {
        const promises: Promise<any>[] = []
    
        for (let scene of coverProducts) {
            promises.push(getRGBTileLayerParamFromSceneObject(scene, gridsBoundary))
        } 
        const rgbTileLayerParamList = await Promise.all(promises)
    
        console.log('可视化参数们', rgbTileLayerParamList)
    
        MapOperation.map_addMultiRGBImageTileLayer(rgbTileLayerParamList, stopLoading)
    }

    const addMultiOneBandColorLayer = async (coverProducts: any, gridsBoundary: any, stopLoading) => {
        const promises: Promise<any>[] = []
    
        for (let scene of coverProducts) {
            promises.push(getOneBandColorParamFromSceneObject(scene, gridsBoundary))
        } 
        const oneBandColorTileLayerParamList = await Promise.all(promises)
    
        console.log('可视化参数们', oneBandColorTileLayerParamList)
    
        MapOperation.map_addMultiOneBandColorLayer(oneBandColorTileLayerParamList, stopLoading)
    }

    const addMVTLayer = async(source_layer: string, landId: string) => {
        MapOperation.map_addMVTLayer(source_layer, landId)
    }

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

    const destroyLayer = async () => {
        try {
            // await MapOperation.map_destroyImagePolygon();
            // await MapOperation.map_destroyImagePreviewLayer();
            await InteractiveExploreMapOps.map_destroyGridLayer();
        } catch (error) {
            console.warn('清理操作遇到问题:', error);
        }
    }

    const removeUniqueLayer = () => {
        mapManager.withMap((m) => {
            if (m.getSource('UniqueLayer-source')) m.removeSource('UniqueLayer-source')
            if (m.getLayer('UniqueLayer-line')) m.removeLayer('UniqueLayer-line')
            if (m.getLayer('UniqueLayer-fill')) m.removeLayer('UniqueLayer-fill')
        })
    }

    const clearAllShowingSensor = () => {
        // MapOperation.map_destroyRGBImageTileLayer()
        // MapOperation.map_destroySceneBoxLayer()
        // MapOperation.map_destroyMultiRGBImageTileLayer()
        // MapOperation.map_destroyMultiTerrainTileLayer()
        // MapOperation.map_destroyMultiOneBandColorLayer()
        InteractiveExploreMapOps.map_destroyImageLayer()
        InteractiveExploreMapOps.map_destroyMVTLayer()
        // MapOperation.map_destroyNoCloudLayer()
    }

    return {
        marker,
        createGeoJSONFromBounds,
        addPolygonLayer,
        destroyLayer,
        removeUniqueLayer,
        addPOIMarker,
        addGridLayer,
        updateGridLayer,
        clearAllShowingSensor,
        addMultiRGBImageTileLayer,
        addMultiTerrainTileLayer,
        addBaseTerrainTileLayer,
        addMulti3DImageTileLayer,
        addMultiOneBandColorLayer,
        addMVTLayer
    }
}
