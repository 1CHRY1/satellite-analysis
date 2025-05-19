<template>
    <div class="bg-amber-400 flex">
        <MapComp class="flex-1" :style="'image'" :proj="'globe'" />
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref, type Ref } from 'vue'
import MapComp from '@/components/feature/map/mapComp.vue'
import { mapManager } from '@/util/map/mapManager'


onMounted(() => {

    setTimeout(() => {
        mapManager.withMap((m) => {

            m.addSource('src', {
                type: 'raster',
                tiles: [
                    // 'http://localhost:8000/rgb/tiles/{z}/{x}/{y}.png?url_r=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF&url_g=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF&url_b=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF'
                    '/hytemp/rgb/box/{z}/{x}/{y}.png?url_r=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF&url_g=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF&url_b=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF&bbox=117,25,118,35&max_r=50000&max_g=50000&max_b=50000&min_r=20000&min_g=20000&min_b=20000'
                ]
            })
            m.addLayer({
                id: 'raster-layer',
                source: 'src',
                type: 'raster',
                minzoom: 7.5,
                maxzoom: 22
            })

        })
    }, 1);

})


</script>

<style scoped></style>