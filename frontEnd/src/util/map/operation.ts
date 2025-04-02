import { mapManager, initMap, type Style } from './mapManager'
import mapboxgl from 'mapbox-gl'
import { CN_Bounds } from './constant'
// import { type Image } from '@/types/satellite'
// import { useGridStore } from '@/store'
// import { type polygonGeometry } from '@/types/sharing'
import { watch } from 'vue'
import type { polygonGeometry } from '../share.type'
import { ezStore } from '@/store'

////////////////////////////////////////////////////////
/////// Map Operation //////////////////////////////////

let resizeObserver: ResizeObserver | null = null

export async function map_initiliaze(
    id: string,
    style: Style = 'vector',
    proj: 'mercator' | 'globe' = 'mercator',
) {
    // return initMap(id)
    setTimeout(() => {
        initMap(id, style, proj).then((m) => {
            m.resize()
            const container = document.getElementById(id)
            if (container) {
                resizeObserver = new ResizeObserver(() => {
                    m.resize()
                })
                resizeObserver.observe(container)
            }
            m.fitBounds(CN_Bounds, {
                linear: true,
                animate: true,
                duration: 1000,
            })
        })
    }, 0)
}

export async function map_destroy() {
    if (resizeObserver) {
        resizeObserver.disconnect()
        resizeObserver = null
    }
    mapManager.destroy()
}

export function map_checkoutStyle(s: Style): void {
    mapManager.withMap((m) => {
        m.setStyle(s)
    })
}

export function map_fitViewToCN(): void {
    mapManager.withMap((m) => {
        m.fitBounds(CN_Bounds, {
            duration: 700,
        })
    })
}

export function map_zoomIn(): void {
    mapManager.withMap((m) => {
        m.zoomIn()
    })
}

export function map_zoomOut(): void {
    mapManager.withMap((m) => {
        m.zoomOut()
    })
}

export function map_flyTo([lng, lat]: [number, number]): void {
    mapManager.withMap((m) => {
        m.flyTo({
            center: [lng, lat],
            zoom: 8,
            animate: true,
        })
    })
}

export function addRasterLayerFromUrl(url: string, layerId: string = 'raster-layer'): void {
    mapManager.withMap((m) => {
        // 检查是否已经存在同名图层，避免重复添加
        if (m.getLayer(layerId)) {
            console.warn(`图层 "${layerId}" 已存在，跳过添加。`)
            return
        }

        // 添加栅格数据源
        m.addSource(layerId, {
            type: 'raster',
            tiles: [url], // 瓦片服务的 URL 模板
            tileSize: 256, // 瓦片尺寸，默认为 256x256
            crossOrigin: 'anonymous',
        })

        // 添加栅格图层
        m.addLayer({
            id: layerId,
            type: 'raster',
            source: layerId,
            paint: {}, // 可以在这里自定义渲染样式
        })

        console.log(`图层 "${layerId}" 已成功添加到地图。`)
    })
}

export function removeRasterLayer(layerId: string = 'raster-layer'): void {
    mapManager.withMap((m) => {
        if (m.getLayer(layerId)) {
            m.removeLayer(layerId)
        }
        if (m.getSource(layerId)) {
            m.removeSource(layerId)
        }
    })
}

////////////////////////////////////////////////////////
/////// Draw Operation //////////////////////////////////
export function draw_deleteAll(): void {
    mapManager.withDraw((d) => {
        d.deleteAll()
    })
}

export function draw_polygonMode(): void {
    mapManager.withDraw((d) => {
        d.deleteAll()
        d.changeMode('draw_polygon')
    })
}

export function draw_pointMode(): void {
    mapManager.withDraw((d) => {
        d.deleteAll()
        d.changeMode('draw_point')
    })
}

export function getCurrentGeometry(): polygonGeometry {
    if (ezStore.get('polygonFeature')) {
        return ezStore.get('polygonFeature') as polygonGeometry
    }
    return {
        type: 'Polygon',
        coordinates: [
            [
                [0, 85],
                [0, -85],
                [180, -85],
                [180, 85],
                [0, 85],
            ],
        ],
    }
}

////////////////////////////////////////////////////////
/////// Layer Operation //////////////////////////////////
type RasterLayerProp = {
    id: string
    url: string
    tileSize: number
}

export function map_addRasterLayer(props: RasterLayerProp): void {
    mapManager.withMap((m) => {
        m.addSource(props.id + '-source', {
            type: 'raster',
            tiles: [props.url],
            tileSize: props.tileSize ?? 256,
        })
        m.addLayer({
            id: props.id,
            type: 'raster',
            source: props.id + '-source',
            paint: {},
        })
    })
}

type GridLayerProp = {
    id: string
    url: string
}

// export function map_addGridLayer(props: GridLayerProp): void {
//     const gridStore = useGridStore()

//     mapManager.withMap((m) => {
//         const popup = new mapboxgl.Popup({
//             closeButton: false,
//         })

//         m.addSource(props.id + '-source', {
//             type: 'geojson',
//             data: props.url,
//         })

//         m.addLayer({
//             id: props.id + '-line',
//             type: 'line',
//             source: props.id + '-source',
//             paint: {
//                 'line-color': '#F00000',
//                 'line-width': 1,
//                 'line-opacity': 0.4,
//             },
//         })

//         m.addLayer({
//             id: props.id,
//             type: 'fill',
//             source: props.id + '-source',
//             paint: {
//                 'fill-color': '#FF0000',
//                 'fill-opacity': 0.01,
//             },
//         })

//         // Add a highlight layer
//         m.addLayer({
//             id: props.id + '-highlight',
//             type: 'fill',
//             source: props.id + '-source',
//             paint: {
//                 'fill-color': '#FFFFFF', // Highlight color
//                 'fill-opacity': 0.5,
//             },
//             filter: ['in', 'id', ''],
//         })

//         watch(
//             () => gridStore.selectedGrids,
//             () => {
//                 console.log('watch gridStore.selectedGrid')
//                 m.setFilter(props.id + '-highlight', ['in', 'id', ...gridStore.selectedGrids])
//             },
//         )

//         m.on('click', props.id, (e) => {
//             const features = m.queryRenderedFeatures(e.point, { layers: [props.id] })
//             if (features.length) {
//                 const featureId = features[0].properties?.id
//                 // console.log(features[0], featureId)
//                 const text = `GridID : ${featureId}`
//                 popup.setLngLat(e.lngLat).setText(text).addTo(m)

//                 // Toggle highlight
//                 if (gridStore.selectedGrids.includes(featureId)) {
//                     gridStore.removeGrid(featureId)
//                 } else {
//                     gridStore.addGrid(featureId)
//                 }
//             }
//         })
//     })
// }

type ImageLayerProp = {
    id: string
    url: string
    boxCoordinates: [[number, number], [number, number], [number, number], [number, number]]
}

export function map_addImageLayer(props: ImageLayerProp): void {
    mapManager.withMap((m) => {
        m.addSource(props.id + '-source', {
            type: 'image',
            url: props.url,
            coordinates: props.boxCoordinates,
        })
        m.addLayer({
            id: props.id,
            type: 'raster',
            source: props.id + '-source',
            paint: {
                'raster-opacity': 0.9,
            },
        })
    })
}

////////////////////////////////////////////////////////
/////// Grid Operation //////////////////////////////////

// export function grid_create(props: Image): void {
//     const gridImgUrl = 'http://127.0.0.1:5000/png'
//     map_addImageLayer({
//         id: 'gridImage',
//         url: gridImgUrl,
//         boxCoordinates: [
//             [119.0494136861107535, 32.7542374796797588],
//             [121.6247280289205861, 32.7542374796797588],
//             [121.6247280289205861, 30.7365359708879033],
//             [119.0494136861107535, 30.7365359708879033],
//         ],
//     })

//     setTimeout(() => {
//         const gridGeoJsonUrl = 'http://127.0.0.1:5000/geojson'
//         map_addGridLayer({
//             id: 'grid',
//             url: gridGeoJsonUrl,
//         })
//     }, 500)
// }
