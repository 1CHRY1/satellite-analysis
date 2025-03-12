import { mapManager, initMap, type Style } from './mapManager'
import mapboxgl from 'mapbox-gl'
import { CN_Bounds } from './constant'
import { type Image } from '@/types/satellite'

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
                'line-color': '#000000',
                'line-width': 1,
            },
        })

        m.addLayer({
            id: props.id,
            type: 'fill',
            source: props.id + '-source',
            paint: {
                'fill-color': '#ff0000',
                'fill-opacity': ['case', ['boolean', ['feature-state', 'selected'], false], 0.5, 0.01],
            },
        })

        m.on('click', props.id, (e) => {
            const features = m.queryRenderedFeatures(e.point, { layers: [props.id] })
            if (features.length) {
                const feature = features[0]
                const text = `${feature.properties?.x}-${feature.properties?.y}`
                popup.setLngLat(e.lngLat).setText(text).addTo(m)
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
        })
    })
}

////////////////////////////////////////////////////////
/////// Grid Operation //////////////////////////////////
// export function grid_create() {

// }
export function grid_create(props: Image): void {
    
}
