import mapboxgl from 'mapbox-gl'
import 'mapbox-gl/dist/mapbox-gl.css'
import MapboxDraw from '@mapbox/mapbox-gl-draw'
import '@mapbox/mapbox-gl-draw/dist/mapbox-gl-draw.css'
import { StyleMap, type Style } from './tianMapStyle'

class MapManager {
    private static instance: MapManager
    private map: mapboxgl.Map | null = null
    private draw: MapboxDraw | null = null
    private initPromise: Promise<mapboxgl.Map> | null = null

    private constructor() {}

    public static getInstance(): MapManager {
        if (!MapManager.instance) {
            MapManager.instance = new MapManager()
        }
        return MapManager.instance
    }

    async init(container: string | HTMLDivElement): Promise<mapboxgl.Map> {
        if (this.map) return this.map

        this.initPromise = new Promise((resolve) => {
            this.map = new mapboxgl.Map({
                container,
                projection: 'mercator',
                center: [117, 36],
                zoom: 2,
                maxZoom: 22,
                style: StyleMap.vector,
            })

            this.map.once('load', () => {
                this.initDrawControl()
                resolve(this.map!)
            })
        })
        return this.initPromise
    }

    public registerDrawCallback(): void {}

    private initDrawControl(): void {
        if (!this.map) return

        this.draw = new MapboxDraw({
            keybindings: false,
            touchEnabled: false,
            boxSelect: false,
            clickBuffer: 4,
            displayControlsDefault: false,
            modes: {
                ...MapboxDraw.modes,
            },
        })

        this.map.addControl(this.draw)
        this.setupDrawEvents()
    }

    private setupDrawEvents(): void {
        if (!this.map || !this.draw) return

        this.map.on('draw.create', () => {
            const features = this.draw?.getAll().features
            if (features?.length) {
                console.log('绘制的图形:', features[0])
            }
        })
    }

    async withMap<T>(callback: (map: mapboxgl.Map) => T): Promise<T | null> {
        if (!this.map) {
            console.warn('Map not initialized')
            return null
        }

        await this.initPromise
        return callback(this.map)
    }

    async withDraw<T>(callback: (draw: MapboxDraw) => T): Promise<T | null> {
        if (!this.draw) {
            console.warn('Draw not initialized')
            return null
        }

        await this.initPromise
        return callback(this.draw)
    }

    public async destroy(): Promise<void> {
        await this.initPromise

        if (this.draw) {
            this.map?.removeControl(this.draw)
            this.draw = null
        }

        if (this.map) {
            this.map.remove()
            this.map = null
        }

        this.initPromise = null
    }
}

/////// 单例 //////////////////////////////////
export const mapManager = MapManager.getInstance()

/////// 外部简单调用 //////////////////////////////////
export const initMap = (container: string | HTMLDivElement) => mapManager.init(container)

export { type Style }
//////// Example //////////////////////////////////
// export const changeDrawMode = async (mode: 'draw_polygon' | 'simple_select') => {
//     await mapManager.withDraw(draw => {
//         draw.deleteAll()
//         draw.changeMode(mode)
//     })
// }

// export const addCustomLayer = async (layerConfig: any) => {
//     await mapManager.withMap(map => {
//         map.addLayer(layerConfig)
//     })
// }
