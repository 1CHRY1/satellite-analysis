import { mapManager, initMap, type Style } from './mapManager'
import mapboxgl from 'mapbox-gl'
import { CN_Bounds } from './constant'
import { type Image } from '@/types/satellite'
import { useGridStore } from '@/store'
import { type polygonGeometry } from '@/types/sharing'
import { watch } from 'vue'

////////////////////////////////////////////////////////
/////// Map Operation //////////////////////////////////
export async function map_initiliaze(id: string) {
    // return initMap(id)
    initMap(id).then((m) => {
        m.resize()
    })
}

export async function map_destroy() {
    mapManager.destroy()
}

export function map_checkoutStyle(s: Style): void {
    mapManager.withMap((m) => {
        m.setStyle(s)
    })
}

export function map_fitViewToCN(): void {
    mapManager.withMap((m) => {
        m.fitBounds(CN_Bounds)
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

export async function getCurrentGeometry(): Promise<polygonGeometry> {
    return new Promise((resolve, reject) => {
        mapManager.withDraw((d) => {
            const features = d.getAll().features
            if (features.length) {
                console.log('current geom', features[0].geometry)
                resolve(features[0].geometry as polygonGeometry)
            }
            // 默认检索范围
            resolve({
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
            })
        })
    })
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

export function map_addGridLayer(props: GridLayerProp): void {
    const gridStore = useGridStore()

    mapManager.withMap((m) => {
        const popup = new mapboxgl.Popup({
            closeButton: false,
        })

        m.addSource(props.id + '-source', {
            type: 'geojson',
            data: props.url,
        })

        m.addLayer({
            id: props.id + '-line',
            type: 'line',
            source: props.id + '-source',
            paint: {
                'line-color': '#F00000',
                'line-width': 1,
                'line-opacity': 0.4,
            },
        })

        m.addLayer({
            id: props.id,
            type: 'fill',
            source: props.id + '-source',
            paint: {
                'fill-color': '#FF0000',
                'fill-opacity': 0.01,
            },
        })

        // Add a highlight layer
        m.addLayer({
            id: props.id + '-highlight',
            type: 'fill',
            source: props.id + '-source',
            paint: {
                'fill-color': '#FFFFFF', // Highlight color
                'fill-opacity': 0.5,
            },
            filter: ['in', 'id', ''],
        })

        watch(
            () => gridStore.selectedGrids,
            () => {
                console.log('watch gridStore.selectedGrid')
                m.setFilter(props.id + '-highlight', ['in', 'id', ...gridStore.selectedGrids])
            },
        )

        m.on('click', props.id, (e) => {
            const features = m.queryRenderedFeatures(e.point, { layers: [props.id] })
            if (features.length) {
                const featureId = features[0].properties?.id
                // console.log(features[0], featureId)
                const text = `GridID : ${featureId}`
                popup.setLngLat(e.lngLat).setText(text).addTo(m)

                // Toggle highlight
                if (gridStore.selectedGrids.includes(featureId)) {
                    gridStore.removeGrid(featureId)
                } else {
                    gridStore.addGrid(featureId)
                }
            }
        })
    })
}

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

export function grid_create(props: Image): void {
    const gridImgUrl = 'http://127.0.0.1:5000/png'
    map_addImageLayer({
        id: 'gridImage',
        url: gridImgUrl,
        boxCoordinates: [
            [119.0494136861107535, 32.7542374796797588],
            [121.6247280289205861, 32.7542374796797588],
            [121.6247280289205861, 30.7365359708879033],
            [119.0494136861107535, 30.7365359708879033],
        ],
    })

    setTimeout(() => {
        const gridGeoJsonUrl = 'http://127.0.0.1:5000/geojson'
        map_addGridLayer({
            id: 'grid',
            url: gridGeoJsonUrl,
        })
    }, 500)
}
