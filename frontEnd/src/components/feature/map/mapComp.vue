<template>
    <div class="relative">
        <div class="relative h-full w-full" id="mapContainer"></div>
        <div class="absolute top-2 right-2 flex gap-2">
            <button @click="handleFitView" class="map-button">🌏</button>
            <button @click="handleZoomIn" class="map-button">➕</button>
            <button @click="handleZoomOut" class="map-button">➖</button>
            <button @click="handleRightRotate" class="map-button">↩️</button>
            <button @click="handleLeftRotate" class="map-button">↪️</button>
            <button @click="handle3DTiles" class="map-button !text-gray-900">{{ is3DMode ? '3D' : '2D' }}</button>
            <button @click="localTian" class="map-button text-gray-900!">{{ t('datapage.mapcomp.vector') }}</button>
            <button @click="localImg" class="map-button text-gray-900!">{{ t('datapage.mapcomp.imagery') }}</button>
        </div>
        <CubeTimeline
            class="absolute right-1/2 bottom-10 flex translate-x-1/2 gap-2"
            v-model="cubeTimelineShow"
        >
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
import { ezStore } from '@/store'
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
        console.log('设置内网vec样式')
        console.log(StyleMap.local.sources)
        m.setStyle(StyleMap.localVec)
    })
}
const localImg = () => {
    mapManager.withMap((m) => {
        console.log('设置本地img样式')
        console.log(StyleMap.local.sources)
        m.setStyle(StyleMap.localImg)
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

// const handle3DTiles = () => {
//     mapManager.withMap((m) => {
//         console.log('加载3D瓦片')
//         // 添加DEM瓦片图层
//         m.addSource('dem-tiles', {
//             type: 'raster-dem',
//             tiles: [ezStore.get('conf')['dem_tiles_url']],
//             // tiles: ['https://api.mapbox.com/v4/mapbox.terrain-rgb/{z}/{x}/{y}.png?access_token='],
//             tileSize: 256,
//             encoding: 'mapbox'
//         })
        
//         m.setTerrain({source: "dem-tiles", exaggeration: 1.5})
    
//     })
// }

const handle3DTiles = () => {
    mapManager.withMap((m) => {
        if (is3DMode.value) {
            // 关闭3D模式
            console.log('关闭3D瓦片')
            m.setTerrain(null) // 移除地形
            if (m.getSource('dem-tiles')) {
                m.removeSource('dem-tiles') // 移除数据源
            }
            is3DMode.value = false
        } else {
            // 开启3D模式
            console.log('加载3D瓦片')
            // 检查数据源是否已存在，避免重复添加
            if (!m.getSource('dem-tiles')) {
                m.addSource('dem-tiles', {
                    type: 'raster-dem',
                    tiles: [ezStore.get('conf')['dem_tiles_url']],
                    tileSize: 256,
                    encoding: 'mapbox'
                })
            }
            m.setTerrain({source: "dem-tiles", exaggeration: 1.5})
            is3DMode.value = true
        }
    })
}

onMounted(() => {
    console.log(props, 1111)

    MapOperation.map_initiliaze('mapContainer', props.style, props.proj)

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
    MapOperation.map_destroy()
})
</script>

<style scoped>
@reference 'tailwindcss';

.map-button {
    @apply cursor-pointer rounded-md bg-white p-2 shadow-md shadow-gray-300 hover:bg-gray-50;
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

:deep(.vdr-container.active) {
    border: none;
}
</style>
