<template>
    <div class="relative">
        <div class="relative h-full w-full" id="mapContainer"></div>
        <div class="absolute top-2 right-2 flex gap-2">
            <button @click="handleFitView" class="map-button">🌏</button>
            <button @click="handleZoomIn" class="map-button">➕</button>
            <button @click="handleZoomOut" class="map-button">➖</button>
        </div>
        <CubeTimeline class="absolute bottom-10 right-1/2 flex gap-2 translate-x-1/2" v-model="cubeTimelineShow">
        </CubeTimeline>
    </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, type PropType, ref } from 'vue'
import * as MapOperation from '@/util/map/operation'
import CubeTimeline from './cubeTimeline.vue'
import bus from '@/store/bus'


const props = defineProps({
    style: {
        type: String as PropType<'vector' | 'image' | 'local'>,
        default: 'local',
    },
    proj: {
        type: String as PropType<'mercator' | 'globe'>,
        default: 'mercator',
    },
    isPicking: {
        type: Boolean,
        default: false
    }
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

onMounted(() => {
    MapOperation.map_initiliaze('mapContainer', props.style, props.proj)

    bus.on('openTimeline', () => {
        cubeTimelineShow.value = true
    })

    bus.on('closeTimeline', () => {
        cubeTimelineShow.value = false
    })


    // window.addEventListener('keydown', e => {
    //     if (e.key === '1') {
    //         cubeTimelineShow.value = !cubeTimelineShow.value
    //     }
    //     if (e.key === '2') {

    //         // debug
    //         const mockImgs = [{
    //             tifFullPath: '/a/b',
    //             sceneId: '123',
    //             time: '2022-01-01',
    //         }, {
    //             tifFullPath: '/a/b/c',
    //             sceneId: '123',
    //             time: '2022-05-01',
    //         }, {
    //             tifFullPath: '/a/b',
    //             sceneId: '213',
    //             time: '2023-01-01',
    //         }]
    //         const mockGrid = {
    //             rowId: 0,
    //             columnId: 0,
    //             resolution: 0
    //         }

    //         bus.emit('cubeVisualize', mockImgs, mockGrid)

    //     }
    // })

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
    border-top-color: rgb(1, 0, 51);
}
</style>
