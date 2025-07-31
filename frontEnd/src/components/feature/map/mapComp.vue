<template>
    <div class="relative">
        <div class="relative h-full w-full" id="mapContainer"></div>
        <div class="absolute top-2 right-2 flex gap-2">
            <button @click="handleFitView" class="map-button">🌏</button>
            <button @click="handleZoomIn" class="map-button">➕</button>
            <button @click="handleZoomOut" class="map-button">➖</button>
            <button @click="handleRightRotate" class="map-button">↩️</button>
            <button @click="handleLeftRotate" class="map-button">↪️</button>
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
