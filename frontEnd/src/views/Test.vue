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
                    // '/hytemp/rgb/box/{z}/{x}/{y}.png?url_r=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF&url_g=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF&url_b=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF&bbox=117,25,118,35&max_r=50000&max_g=50000&max_b=50000&min_r=20000&min_g=20000&min_b=20000'
                    '/tiler/rgb/tiles/9/422/199.png?bbox=117.12574850299404%2C36.773226773226774%2C117.30538922155688%2C36.95304695304695&url_r=http%3A%2F%2F223.2.32.166%3A30900%2Ftest-images%2FMODIS_MOD02%2FLevel_1B%2Ftif%2FMOD02_MOD35_20250106%2FBand_1.tif&url_g=http%3A%2F%2F223.2.32.166%3A30900%2Ftest-images%2FMODIS_MOD02%2FLevel_1B%2Ftif%2FMOD02_MOD35_20250106%2FBand_4.tif&url_b=http%3A%2F%2F223.2.32.166%3A30900%2Ftest-images%2FMODIS_MOD02%2FLevel_1B%2Ftif%2FMOD02_MOD35_20250106%2FBand_3.tif&min_r=0&max_r=4975&min_g=0&max_g=4975&min_b=0&max_b=4975'
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