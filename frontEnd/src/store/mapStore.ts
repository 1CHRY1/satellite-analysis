// @/store/mapStore.ts

import { defineStore } from 'pinia'
import type { Map as MapboxMap } from 'mapbox-gl'

export const useMapStore = defineStore('map', {
    state: () => ({
        // 存储 Mapbox 实例
        mapInstance: null as MapboxMap | null, 
        isInitialized: false,
    }),
    actions: {
        setMapInstance(map) {
            this.mapInstance = map
            this.isInitialized = true
        },
        // 方便在需要时销毁地图（例如，退出应用）
        destroyMapInstance() {
            if (this.mapInstance) {
                this.mapInstance.remove()
                this.mapInstance = null
                this.isInitialized = false
            }
        },
    },
})