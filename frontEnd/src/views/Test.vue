<template>
    <div class="absolute top-0 left-0 h-screen w-screen">
        <div class="h-[92vh]" id="map"></div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, h } from 'vue'
import { mapManager } from '@/util/map/mapManager'

onMounted(() => {
    mapManager.init('map').then((map) => {
        console.log(map)



        ///// rio-tiler-test /////////////////////////
        if (false) {
            map.addSource('src', {
                type: 'raster',
                tiles: [
                    // no cog
                    // "http://223.2.32.242:8079/{z}/{x}/{y}.png?object=/test-images/landsat/landset7/tif/LE07_L1TP_122039_20210212_20210212_01_RT/LE07_L1TP_122039_20210212_20210212_01_RT_B1.TIF"
                    'http://223.2.32.242:8079/{z}/{x}/{y}.png?object=/project-data-bucket/rgj3/PRJMMFWjTXm1zuYm4m4q/test.tif',
                    // "http://223.2.32.242:8079/{z}/{x}/{y}.png?object=/test-images/landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20241217_20241227_02_T1/LC08_L2SP_118038_20241217_20241227_02_T1_SR_B4.TIF"
                ],
            })
            map.addLayer({
                id: 'layer',
                type: 'raster',
                source: 'src',
                minzoom: 0,
            })

            map.addSource("src2", {
                type: "raster",
                tiles: [
                    "http://223.2.32.242:8079/{z}/{x}/{y}.png?object=/test-images/landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20241201_20241203_02_T1/LC08_L2SP_118038_20241201_20241203_02_T1_SR_B4.TIF"
                ]
            })
            map.addLayer({
                id: "layer2",
                type: "raster",
                source: "src2",
                minzoom: 0,
            })

            window.addEventListener('keydown', (e) => {
                if (e.key === '1') {
                    map.setPaintProperty('layer', 'raster-opacity', 0.1)
                    map.setPaintProperty('layer2', 'raster-opacity', 1)
                } else if (e.key === '2') {
                    map.setPaintProperty('layer', 'raster-opacity', 1)
                    map.setPaintProperty('layer2', 'raster-opacity', 0.1)
                }
            })
        }
    })
})

</script>
