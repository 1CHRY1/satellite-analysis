import mapboxgl from 'mapbox-gl'
import 'mapbox-gl/dist/mapbox-gl.css'
import MapboxDraw from '@mapbox/mapbox-gl-draw'
import '@mapbox/mapbox-gl-draw/dist/mapbox-gl-draw.css'
import { StyleMap, type Style } from './tianMapStyle'
import { ezStore } from '@/store'
import { useGridStore } from '@/store/gridStore'
import type { polygonGeometry } from '../share.type'

class MapManager {
    private static instance: MapManager | null
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

    async init(
        container: string | HTMLDivElement,
        // style: Style = 'vector',
        style: Style = 'local', // 默认用本地影像
        proj: 'mercator' | 'globe' = 'mercator', // 默认用Mercortor平面
    ): Promise<mapboxgl.Map> {
        if (this.map) return this.map
        this.initPromise = new Promise((resolve) => {
            const conf = ezStore.get('conf')
            this.map = new mapboxgl.Map({
                container,
                projection: proj,
                center: [117, 36],
                zoom: 2,
                maxZoom: 22,
                style: StyleMap[style],
                transformRequest: (url) => {
                    // if (url.indexOf(conf['back_app']) > -1) {
                    //     const token = localStorage.getItem('token')
                    //     return {
                    //         url: url,
                    //         headers: { Authorization: `Bearer ${token}` },
                    //         credentials: 'include',
                    //     }
                    // }
                    if (url.includes('bbox=') && url.includes('temporaryMap')) {
                        const match = url.match(
                            /bbox=([0-9\.\-]+),([0-9\.\-]+),([0-9\.\-]+),([0-9\.\-]+)/,
                        )
                        if (match) {
                            const [, minX, minY, maxX, maxY] = match.map(Number)
                            // console.log(minX, minY, maxX, maxY, '3857')

                            // Web Mercator to WGS84
                            const project = (x, y) => {
                                const lon = (x * 180) / 20037508.34
                                const lat =
                                    (Math.atan(Math.exp((y * Math.PI) / 20037508.34)) * 360) /
                                        Math.PI -
                                    90
                                return [lon, lat]
                            }

                            const [lon1, lat1] = project(minX, minY)
                            const [lon2, lat2] = project(maxX, maxY)

                            const wgs84bbox = `${lon1},${lat1},${lon2},${lat2}`
                            // url = url.replace(/{bbox-epsg-3857}/g, wgs84bbox)
                            url = url.replace(/bbox=[^&]+/, `bbox=${wgs84bbox}`)
                        }
                    }

                    if (url.includes('tianditu')) {
                        return {
                            url,
                        }
                    }

                    const token = localStorage.getItem('token')
                    return {
                        url: url,
                        headers: { Authorization: `Bearer ${token}` },
                        // credentials: 'include',
                    }
                },
            })
            const logo = document.querySelector('.mapboxgl-ctrl-logo') as HTMLElement
            logo.style.display = 'none'
            const scale = new mapboxgl.ScaleControl({
                maxWidth: 100,
                unit: 'metric',
            })
            this.map.addControl(scale, 'bottom-left')

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
            // const features = this.draw?.getAll().features
            // if (features?.length) {
            //     ezStore.set('polygonFeature', features[0].geometry)
            //     useGridStore().setPolygon(features[0].geometry as polygonGeometry)
            // }
            const feature = this.draw?.getAll().features[0]
            if (!feature) return

            if (feature.geometry.type === 'Point') {
                const [lng, lat] = feature.geometry.coordinates
                useGridStore().setPickedPoint([lat, lng])
            } else if (feature.geometry.type === 'Polygon') {
                ezStore.set('polygonFeature', feature.geometry)
                useGridStore().setPolygon(feature.geometry as polygonGeometry)
            } else if (feature.geometry.type === 'LineString') {
                // 这里是新增的逻辑
                // 坐标是 [lng, lat][] 格式，我们通常按 [lat, lng] 存
                const latlngLine = feature.geometry.coordinates
                useGridStore().setPickedLine(latlngLine)
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
        }
        MapManager.instance = null
        this.initPromise = null
        this.map = null
    }
}

// 辅助函数：获取瓦片的 EPSG:3857 坐标范围
function getTileBBox(x, y, z) {
    const tileSize = 20037508.34
    const res = tileSize / Math.pow(2, z)
    const minx = x * tileSize - tileSize / 2
    const miny = y * tileSize - tileSize / 2
    const maxx = (x + 1) * tileSize - tileSize / 2
    const maxy = (y + 1) * tileSize - tileSize / 2
    return [minx, miny, maxx, maxy]
}

/////// 单例 //////////////////////////////////
export const mapManager = MapManager.getInstance()

/////// 外部简单调用 //////////////////////////////////
export const initMap = (
    container: string | HTMLDivElement,
    style: Style = 'local',
    proj: 'mercator' | 'globe' = 'mercator',
) => mapManager.init(container, style, proj)

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
