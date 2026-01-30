import mapboxgl from 'mapbox-gl'
import 'mapbox-gl/dist/mapbox-gl.css'
import MapboxDraw from '@mapbox/mapbox-gl-draw'
import '@mapbox/mapbox-gl-draw/dist/mapbox-gl-draw.css'
import { StyleMap, type Style } from './tianMapStyle'
import { ezStore } from '@/store'
import { useGridStore } from '@/store/gridStore'
import type { polygonGeometry } from '../share.type'
import Bus from '@/store/bus'
import { CN_Bounds } from './constant'
import { message } from 'ant-design-vue'
import { amapStyle } from './amapStyle'

class MapManager {
    private static instance: MapManager | null
    private map: mapboxgl.Map | null = null
    private draw: MapboxDraw | null = null
    private initPromise: Promise<mapboxgl.Map> | null = null

    private _isInitialized = false
    private _isDrawInitialized = false

    // [新增 1] 全局 Loading 状态管理
    private _isLayerLoading = false // 标记：是否正在因为 addLayer 等待
    private _loadingTimer: any = null // 防抖定时器

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
        if (this._isInitialized && this.map) return this.map

        this._isInitialized = false
        this._isDrawInitialized = false

        this.initPromise = new Promise((resolve) => {
            const conf = ezStore.get('conf')
            this.map = new mapboxgl.Map({
                container,
                projection: proj,
                center: [117, 36],
                zoom: 2,
                maxZoom: 22,
                // important: 第一行注释打开，则使用本地底图影像，否则使用mapbox默认在线底图或高德地图
                style: StyleMap[style],
                // style: 'mapbox://styles/mapbox/satellite-streets-v12',
                // 高德配置
                // style: amapStyle as any,
                transformRequest: (url) => {
                    // if (url.indexOf(conf['back_app']) > -1) {
                    //     const token = localStorage.getItem('token')
                    //     return {
                    //         url: url,
                    //         headers: { Authorization: `Bearer ${token}` },
                    //         credentials: 'include',
                    //     }
                    // }
                    // 高德在线地图配置
                    if (url.includes('autonavi.com') || url.includes('amap.com')) {
                        return {
                            url: url,
                            // 强制清空 headers，防止 Mapbox 自动加上 Authorization 头
                            headers: {},
                        }
                    }
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
            // [新增 2] 在地图实例创建后，立即注入全局 Loading 监控
            this.setupGlobalLoadingMonitor()
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

    // [新增 3] 核心逻辑：劫持 addLayer 并监听 idle
    private setupGlobalLoadingMonitor() {
        if (!this.map) return
        const map = this.map

        // 1. 保存原始的 addLayer 方法
        // 这里必须 bind(map)，否则 this 指向会丢失
        const originalAddLayer = map.addLayer.bind(map)

        // 2. 重写/劫持 addLayer
        // @ts-ignore: 忽略 TS 对覆盖原有方法的警告，或者扩展类型定义
        map.addLayer = (layer: mapboxgl.AnyLayer, beforeId?: string) => {
            // A. 只要调用了 addLayer，就开启 Loading
            this.handleStartLoading()

            // B. 执行原始逻辑，保证功能正常
            return originalAddLayer(layer, beforeId)
        }

        // 3. 监听全局 idle 事件
        map.on('idle', () => {
            // 只有当是因为“加图层”引起的 Loading，才允许由 idle 关闭
            // 这样就过滤掉了普通平移引起的 idle
            if (this._isLayerLoading) {
                this.handleStopLoading()
            }
        })
    }

    // [新增 4] 开始 Loading (带防抖，体验优化)
    private handleStartLoading() {
        this._isLayerLoading = true

        // 如果已经在倒计时显示 loading，清除它，重新计时
        if (this._loadingTimer) clearTimeout(this._loadingTimer)

        // 延迟 100ms 显示。如果数据加载极快(比如缓存)，用户甚至不需要看到 loading 圈，体验更好
        this._loadingTimer = setTimeout(() => {
            // 注意：这里使用 message.loading 且 duration: 0 表示一直显示直到手动销毁
            // key: 'global_map_loader' 保证全局只有一个 loading 实例，不会重复弹出
            message.loading({
                content: '正在加载，请稍后...',
                key: 'global_map_loader',
                duration: 0,
            })
        }, 100)
    }

    // [新增 5] 停止 Loading
    private handleStopLoading() {
        if (!this._isLayerLoading) return

        this._isLayerLoading = false

        if (this._loadingTimer) {
            clearTimeout(this._loadingTimer)
            this._loadingTimer = null
        }

        message.destroy('global_map_loader')
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
                Bus.emit('point-finished', {
                    lng,
                    lat,
                })
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

        // 销毁时清理定时器，防止内存泄漏
        if (this._loadingTimer) {
            clearTimeout(this._loadingTimer)
            this._loadingTimer = null
            // 强制关闭可能残留的 message
            message.destroy('global_map_loader')
        }

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

    public async waitForInit(): Promise<void> {
        if (this.initPromise) {
            await this.initPromise
        }
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
