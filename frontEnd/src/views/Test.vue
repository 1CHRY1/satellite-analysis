<template>
    <div class="h-full bg-white flex">
        <div id='map' class="flex-1"></div>
    </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
// import * as MapOperation from '@/util/map/operation'
import { initMap, mapManager } from '@/util/map/mapManager'

onMounted(() => {
    initMap('map')

    mapManager.withMap((map) => {
        map.addSource('test', {
            type: 'raster',
            tiles: [
                'http://127.0.0.1:5000/tile/{z}/{x}/{y}.png'
            ]
        })
        map.addLayer({
            id: 'test',
            type: 'raster',
            source: 'test',
            paint: {
                'raster-opacity': 1.0,
                'raster-color-mix': [
                    0.1126,
                    0.7152,
                    0.0722,
                    1
                ]
            }
        })
    })

})


</script>

<style scoped></style>