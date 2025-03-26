<template>
    <div class="relative">
        <div class="relative bg-gray-200 w-full h-[100%]" id="mapContainer"></div>
        <div class="absolute top-2 right-2 flex gap-2">
            <button @click="handleDebug" class="map-button">🐛</button>
            <button @click="handleLocate" class="map-button">📍</button>
            <button @click="handleFitView" class="map-button">🌏</button>
            <button @click="handleZoomIn" class="map-button">➕</button>
            <button @click="handleZoomOut" class="map-button">➖</button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import * as MapOperation from '@/util/map/operation'

const handleDebug = () => { }
const handleLocate = () => { }
const handleFitView = () => { }
const handleZoomIn = () => { }
const handleZoomOut = () => { }

onMounted(() => {
    MapOperation.map_initiliaze('mapContainer')
    const logo = document.querySelector('.mapboxgl-ctrl-logo') as HTMLElement
    logo.style.display = 'none'
})

onUnmounted(() => {
    MapOperation.map_destroy()
})
</script>

<style scoped>
@reference 'tailwindcss';

.map-button {
    @apply p-2 rounded-md cursor-pointer bg-white shadow-gray-300 shadow-md hover:bg-gray-50
}

:deep(.mapboxgl-popup-content) {
    @apply bg-white font-medium text-black;
}
</style>
