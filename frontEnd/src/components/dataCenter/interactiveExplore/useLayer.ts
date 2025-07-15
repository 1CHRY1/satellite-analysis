import { ref } from 'vue'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import type { Marker } from 'mapbox-gl'
import * as MapOperation from '@/util/map/operation'
import { mapManager } from '@/util/map/mapManager'
import mapboxgl from 'mapbox-gl'
import { getOneBandColorParamFromSceneObject, getRGBTileLayerParamFromSceneObject, getTerrainParamFromSceneObject } from '@/util/visualizeHelper'

/**
 * 图层可视化
 * @returns 
 */

export type POIInfo = {
    // adcode: string,
    // adname: string,
    gcj02Lat: string,
    gcj02Lon: string,
    geometry: any,
    id: string,
    name: string,
    address: string
    pname: string
    cityname: string
    adname: string
}

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
     * 交互式探索中用到的图层操作函数
     */

    const addPolygonLayer = (boundary: any) => {
        MapOperation.map_addPolygonLayer({
            geoJson: boundary,
            id: 'UniqueLayer',
            lineColor: '#8fffff',
            fillColor: '#a4ffff',
            fillOpacity: 0.2,
        })
    }

    const addPOIMarker = (selectedPOI: POIInfo) => {
        mapManager.withMap((m) => {
            marker.value = new mapboxgl.Marker()
                .setLngLat([Number(selectedPOI?.gcj02Lon), Number(selectedPOI?.gcj02Lat)])
                .addTo(m);
        })
    }

    // addGridLayer是初步，这里是根据景的数量更新透明度
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
    
        MapOperation.map_addGridLayer(gridFeature)
        MapOperation.draw_deleteAll()
        // fly to
        MapOperation.map_fitView([
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
    const updateFullSceneGridLayer = (allGrids: any, sceneGridsRes: any, totalImg: number) => {
        let gridFeature: FeatureCollection = {
            type: 'FeatureCollection',
            features: allGrids.map((item: any, index: number) => {
                return {
                    type: 'Feature',
                    geometry: item.boundary.geometry as Geometry,
                    properties: {
                        ...(item.properties || {}),
                        id: item.properties?.id ?? index, // 确保每个都有 id
                        opacity: judgeGridOpacity(index, sceneGridsRes, totalImg),
                        rowId: item.rowId,
                        columnId: item.columnId,
                        resolution: item.resolution,
                        flag: true, // flag means its time to trigger the visual effect
                        international: sceneGridsRes[index].international,
                        national: sceneGridsRes[index].national,
                        light: sceneGridsRes[index].light,
                        radar: sceneGridsRes[index].radar,
                        traditional: sceneGridsRes[index].traditional,
                        ard: sceneGridsRes[index].ard,
                    },
                }
            }),
        }
        MapOperation.map_destroyGridLayer()
        MapOperation.map_addGridLayer_coverOpacity(gridFeature)
        MapOperation.draw_deleteAll()
    }

    // 基于覆盖度返回opacity
    const judgeGridOpacity = (index: number, sceneGridsRes: any, totalImg: number) => {
        let opacity = 0.01
        opacity = (sceneGridsRes[index].scenes.length / totalImg) * 0.3
        return opacity
    }

    const destroyLayer = async () => {
        try {
            await MapOperation.map_destroyImagePolygon();
            await MapOperation.map_destroyImagePreviewLayer();
            await MapOperation.map_destroyGridLayer();
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
        MapOperation.map_destroyRGBImageTileLayer()
        MapOperation.map_destroySceneBoxLayer()
        MapOperation.map_destroyMultiRGBImageTileLayer()
        MapOperation.map_destroyMultiTerrainTileLayer()
        MapOperation.map_destroyMultiOneBandColorLayer()
        MapOperation.map_destroyMVTLayer()
    }

    return {
        marker,
        createGeoJSONFromBounds,
        addPolygonLayer,
        destroyLayer,
        removeUniqueLayer,
        addPOIMarker,
        addGridLayer,
        updateFullSceneGridLayer,
        clearAllShowingSensor,
        addMultiRGBImageTileLayer,
        addMultiTerrainTileLayer,
        addBaseTerrainTileLayer,
        addMulti3DImageTileLayer,
        addMultiOneBandColorLayer,
        addMVTLayer
    }
}
