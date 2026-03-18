<template>
    <div class="relative">
        <div class="relative h-full w-full" id="mapContainer"></div>
        <div class="absolute top-2 left-2 z-10">

        </div>
        <div class="absolute top-2 right-2 flex gap-2">
            <button @click="handleFitView" class="map-button">🌏</button>
            <button @click="handleZoomIn" class="map-button">➕</button>
            <button @click="handleZoomOut" class="map-button">➖</button>
            <button @click="handleRightRotate" class="map-button">↩️</button>
            <button @click="handleLeftRotate" class="map-button">↪️</button>
            <button @click="handle3DTiles" :class="['map-button', is3D ? '' : 'grayscale']">⛰️</button>
            <!-- <button @click="handle3DTiles" class="map-button !text-gray-900">{{ is3DMode ? '3D' : '2D' }}</button>-->
            <button @click="localTian" class="map-button text-gray-900!">{{ t('datapage.mapcomp.vector') }}</button>
            <button @click="localImg" class="map-button text-gray-900!">{{ t('datapage.mapcomp.imagery') }}</button>
            <a-dropdown :trigger="['click']">
                <button class="map-button text-gray-900!" @click.prevent>
                    年时序静态瓦片
                    <DownOutlined style="font-size: 12px; margin-left: 4px;" />
                </button>
                <template #overlay>
                    <a-menu @click="localStaticImgPerYear($event.key)">
                        <a-menu-item key="2022">2022年</a-menu-item>
                        <a-menu-item key="2023">2023年</a-menu-item>
                        <a-menu-item key="2024">2024年</a-menu-item>
                        <a-menu-item key="2025">2025年</a-menu-item>
                    </a-menu>
                </template>
            </a-dropdown>
            <LayerManagerComp />
        </div>
        <CubeTimeline class="absolute right-1/2 bottom-10 flex translate-x-1/2 gap-2" v-model="cubeTimelineShow">
        </CubeTimeline>
    </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, type PropType, ref } from 'vue'
import * as MapOperation from '@/util/map/operation'
import { mapManager } from '@/util/map/mapManager'
import { StyleMap } from '@/util/map/tianMapStyle'
import CubeTimeline from './cubeTimeline.vue'
import bus from '@/store/bus'
import { useI18n } from 'vue-i18n'
import type { Map as MapboxMap } from 'mapbox-gl'
import { ezStore } from '@/store'
import { useMapStore } from '@/store/mapStore'
import LayerManagerComp from './layerManagerComp.vue';
import { layerManager } from '@/util/map/layerManager';
import { DownOutlined } from '@ant-design/icons-vue'

const { t } = useI18n()

const props = defineProps({
    style: {
        type: String as PropType<'vector' | 'image' | 'local' | 'empty'>,
        default: 'local',
    },
    proj: {
        type: String as PropType<'mercator' | 'globe'>,
        default: 'mercator',
    },
    isPicking: {
        type: Boolean,
        default: false,
    },
})

const cubeTimelineShow = ref(false)
const is3DMode = ref(false)


const handleFitView = () => {
    MapOperation.map_fitViewToCN()
}
const handleZoomIn = () => {
    MapOperation.map_zoomIn()
}
const handleZoomOut = () => {
    MapOperation.map_zoomOut()
}

const localTian = () => {
    mapManager.withMap((m) => {
        m.setStyle(StyleMap.localVec)
    })
}
const localImg = () => {
    mapManager.withMap((m) => {
        m.setStyle(StyleMap.localImg)
    })
}
const localStaticImgPerYear = (key: string) => {
    console.log(key)
    mapManager.withMap((m) => {
        m.setStyle(StyleMap[key])
    })
}

const handleLeftRotate = () => {
    mapManager.withMap((m) => {
        const bare = m.getBearing()
        m.rotateTo(bare + 90, { duration: 2000 })
    })
}

const handleRightRotate = () => {

    mapManager.withMap((m) => {
        // m.rotateTo(90, { duration: 5000 })
        const bare = m.getBearing()
        m.rotateTo(bare - 90, { duration: 2000 })
    })
}

const is3D = ref(false)
const handle3DTiles = () => {
    const origin = window.location.origin;
    if (is3D.value) {
        handleDestroyDEMLayer()
        is3D.value = false
    } else {
        mapManager.withMap((m) => {
            console.log('加载3D瓦片')
            // 添加DEM瓦片图层
            m.addSource('dem-tiles', {
                type: 'raster-dem',
                // tiles: [ezStore.get('conf')['dem_tiles_url']],
                tiles: [`${origin}` + '/demtiles/{z}/{x}/{y}.png'], //注意app.conf.json中的并没有dem_tiles_url真正应用
                // tiles: ['https://api.mapbox.com/v4/mapbox.terrain-rgb/{z}/{x}/{y}.png?access_token='],
                tileSize: 256,
                encoding: 'mapbox'
            })

            m.setTerrain({ source: "dem-tiles", exaggeration: 1.5 })

        })
        is3D.value = true
    }
}

const handleDestroyDEMLayer = () => {
    const id = 'dem-tiles'
    const srcId = id
    mapManager.withMap((m) => {
        m.setTerrain(null)
        m.getLayer(id) && m.removeLayer(id)
        m.getSource(srcId) && m.removeSource(srcId)
    })
}

const store = useMapStore()
onMounted(async () => {
    // --------------------- NEW(USING PINIA) --------------------- //
    const container = document.getElementById('mapContainer')

    if (!store.isInitialized && container) {
        console.log("MapComp: 第一次加载，创建新的 Mapbox 实例。")
        const mapInstance = await MapOperation.map_initiliaze(
            'mapContainer',
            props.style,
            props.proj
        )
        store.setMapInstance(mapInstance)
        layerManager.initializeObserver()
    } else if (store.mapInstance && container) {
        console.log("MapComp: 实例已存在，重新附加 DOM。")
        // 1. 获取地图实例的原 DOM 容器
        const mapCanvas = store.mapInstance.getContainer()
        // 2. 将地图 DOM 容器移动到当前组件的挂载点
        if (mapCanvas && mapCanvas.parentElement !== container) {
            container.appendChild(mapCanvas)
        }
        // 3. 通知地图库 DOM 尺寸已变更（Mapbox 常用操作）
        store.mapInstance.resize()
        layerManager.initializeObserver()
    }

    // -------------------- OLD ---------------- //
    // MapOperation.map_initiliaze('mapContainer', props.style, props.proj)

    bus.on('openTimeline', () => {
        cubeTimelineShow.value = true
    })

    bus.on('closeTimeline', () => {
        cubeTimelineShow.value = false
    })
    // bus.on('cleanAllGridPreviewLayer', () => {
    //     MapOperation.map_removeGridPreviewLayer('all')
    // })

    bus.on('cleanAllLayer', () => {
        MapOperation.map_destroyGridLayer()
        MapOperation.map_destroyRGBImageTileLayer()
        MapOperation.map_destroyImagePreviewLayer()
        MapOperation.map_destroySceneBoxLayer()
        MapOperation.map_destroyImagePolygon()
        MapOperation.map_destroyMultiRGBImageTileLayer()
        MapOperation.map_destroyNoCloudLayer()
        MapOperation.map_destroyTerrain()
    })

    // setTimeout(() => {
    //     mapManager.withMap((m) => {
    //         m.showTileBoundaries = true
    //     })
    //     //         // '/hytemp/rgb/tiles/{z}/{x}/{y}.png?url_r=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF&url_g=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF&url_b=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF'
    //     //         '/hytemp/rgb/box/{z}/{x}/{y}.png?url_r=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF&url_g=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF&url_b=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF&bbox=117,31.5,118,32&max_r=50000&max_g=50000&max_b=50000&min_r=20000&min_g=20000&min_b=20000'
    //     //     ]
    //     // })
    //     // m.addLayer({
    //     //     id: 'raster-layer',
    //     //     source: 'src',
    //     //     type: 'raster',
    //     //     minzoom: 5,
    //     //     maxzoom: 22
    //     // })
    //     // })
    // }, 1)

})

onUnmounted(() => {
    // 在路由切换时，我们不销毁地图，只将地图 DOM 容器移出当前视图
    if (store.mapInstance) {
        const container = document.getElementById('mapContainer')
        console.log(store.mapInstance)
        const mapCanvas = store.mapInstance.getContainer()

        if (mapCanvas && mapCanvas.parentElement === container) {
            // 将地图 DOM 从 mapContainer 中移出，但不要销毁它
            // 移出后，它仍然存在于内存中，只是不再依附于任何组件的 DOM 树
            mapCanvas.remove()
            console.log("MapComp: 路由离开，地图 DOM 容器被分离。")
        }
    }
    MapOperation.map_destroy_observers()
})
</script>

<style scoped>
@reference 'tailwindcss';

.map-button {
    @apply cursor-pointer rounded-md bg-white p-2 shadow-md shadow-gray-300 hover:bg-gray-50;
}

.map-button.grayscale {
    filter: grayscale(100%);
    opacity: 0.7;
}

:deep(.mapboxgl-popup-content) {
    /* @apply  font-medium text-black; */
    background-color: transparent;
    padding: 0;
}

:deep(.mapboxgl-popup-anchor-bottom .mapboxgl-popup-tip) {
    /* border-top-color: rgba(0, 0, 0, 0); */
    border-color: transparent;
}

:deep(.mapboxgl-popup-close-button) {
    font-size: 20px;
    margin-top: 12px;
}

/* Vector Popup Styles */
:deep(.vector-popup-container .mapboxgl-popup-content) {
    background-color: #0a1929;
    border: 1px solid #1e3a5f;
    border-radius: 8px;
    padding: 0;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
}

:deep(.vector-popup-container .mapboxgl-popup-tip) {
    border-top-color: #0a1929;
}

:deep(.vector-popup-container .mapboxgl-popup-close-button) {
    color: #a5d8ff;
    font-size: 20px;
    padding: 4px 8px;
    margin: 0;
}

:deep(.vector-popup-container .mapboxgl-popup-close-button:hover) {
    background-color: rgba(165, 216, 255, 0.1);
    border-radius: 4px;
}

:deep(.vector-popup-content) {
    color: #e6f1ff;
    min-width: 200px;
}

:deep(.vector-popup-content .popup-header) {
    padding: 12px 16px;
    border-bottom: 1px solid #1e3a5f;
    background-color: #132f4c;
    border-radius: 8px 8px 0 0;
}

:deep(.vector-popup-content .popup-header h4) {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    color: #7eb3dd;
}

:deep(.vector-popup-content .popup-body) {
    padding: 12px 16px;
    max-height: 300px;
    overflow-y: auto;
}

:deep(.vector-popup-content .no-data) {
    text-align: center;
    color: #a5d8ff;
    font-style: italic;
    padding: 12px 0;
}

:deep(.vector-popup-content .attributes-table) {
    width: 100%;
    border-collapse: collapse;
}

:deep(.vector-popup-content .attributes-table tr) {
    border-bottom: 1px solid #1e3a5f;
}

:deep(.vector-popup-content .attributes-table tr:last-child) {
    border-bottom: none;
}

:deep(.vector-popup-content .attributes-table td) {
    padding: 8px 4px;
    vertical-align: top;
}

:deep(.vector-popup-content .attr-key) {
    font-weight: 600;
    color: #4dabf7;
    font-size: 12px;
    width: 40%;
    word-break: break-word;
}

:deep(.vector-popup-content .attr-value) {
    color: #e6f1ff;
    font-size: 13px;
    word-break: break-word;
}

/* Scrollbar styling for popup */
:deep(.vector-popup-content .popup-body::-webkit-scrollbar) {
    width: 6px;
}

:deep(.vector-popup-content .popup-body::-webkit-scrollbar-track) {
    background: #132f4c;
    border-radius: 3px;
}

:deep(.vector-popup-content .popup-body::-webkit-scrollbar-thumb) {
    background: #1e3a5f;
    border-radius: 3px;
}

:deep(.vector-popup-content .popup-body::-webkit-scrollbar-thumb:hover) {
    background: #4dabf7;
}

:deep(.vdr-container.active) {
    border: none;
}
</style>
