<template>
    <div class="flex bg-amber-50">
        <MapComp class="flex-1" :style="'image'" :proj="'globe'" />

        <div class="absolute top-10 right-10 flex h-fit w-fit flex-col gap-5">
            <button class="bg-amber-300 p-5" @click="localMvt">本地MVT</button>
            <button class="bg-amber-300 p-5" @click="localImg">本地影像瓦片</button>
            <button class="bg-amber-300 p-5" @click="localTian">内网老版天地图</button>

            <button class="bg-amber-800 p-5" @click="addterrain">添加地形</button>
            <button class="bg-amber-800 p-5" @click="locate(-119.81307, 33.99333)">定位</button>

            <button class="bg-amber-600 p-5" @click="addColorBand">
                单波段彩色产品(形变速率， NDVI)
            </button>
            <button class="bg-amber-600 p-5" @click="locate(120.6141, 36.6215)">定位</button>

            <button class="bg-amber-600 p-5" @click="addNocloud">无云一版图</button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref, type Ref } from 'vue'
import MapComp from '@/components/feature/map/mapComp.vue'
import { mapManager } from '@/util/map/mapManager'
import { StyleMap } from '@/util/map/tianMapStyle'
import * as MapOperation from '@/util/map/operation'
import mapboxgl from 'mapbox-gl'
import { getNoCloudScaleParam, getNoCloudUrl } from '@/api/http/satellite-data/visualize.api'
const addterrain = () => {
    MapOperation.map_addTerrain({
        fullTifPath: 'test-images/DEM/ChannelIslands_Topobathy_DEM_v1_47.TIF',
        // http://223.2.32.166:30900/test-images/DEM/ChannelIslands_Topobathy_DEM_v1_47.TIF
    })
}

const addNocloud = async () => {
    const tifpath = 'temp-files/aa0594b6-f6a1-4c08-a148-21dfb5ed193e/noCloud_merge.tif'

    const band123Scale: any = await getNoCloudScaleParam(tifpath)
    console.log(band123Scale)


    const url = getNoCloudUrl({
        fullTifPath: tifpath,
        ...band123Scale
    })

    MapOperation.map_addNoCloudLayer(url)

    // MapOperation.map_addNoCloudLayer(tifpath)
}

const addColorBand = () => {
    MapOperation.map_addOneBandColorLayer({
        fullTifPath: 'test-images/DEM/ASTGTMV003_N36E120_dem.tif',
    })
}

const localMvt = () => {
    console.log('1')
    mapManager.withMap((m) => {
        console.log('设置本地mvt样式')
        console.log(StyleMap.local.sources)
        m.setStyle(StyleMap.localMvt)
    })
}

const localImg = () => {
    console.log('2')
    mapManager.withMap((m) => {
        console.log('设置本地img样式')
        console.log(StyleMap.local.sources)
        m.setStyle(StyleMap.localImg)
    })
}

const localTian = () => {
    console.log('3')
    mapManager.withMap((m) => {
        console.log('设置内网vec样式')
        console.log(StyleMap.local.sources)
        m.setStyle(StyleMap.localVec)
    })
}

const locate = (lng, lat) => {
    mapManager.withMap((m) => {
        m.flyTo({
            center: [lng, lat],
            zoom: 8,
        })
    })
}

onMounted(() => {
    setTimeout(() => {
        mapManager.withMap((m: mapboxgl.Map) => {
            // console.log(m)
            // m.addSource('src', {
            //     type: 'raster',
            //     tiles: [
            //         // '/hytemp/rgb/tiles/{z}/{x}/{y}.png?url_r=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF&url_g=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF&url_b=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF'
            //         '/hytemp/rgb/box/{z}/{x}/{y}.png?url_r=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF&url_g=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF&url_b=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF&bbox=117,31.5,118,32&max_r=50000&max_g=50000&max_b=50000&min_r=20000&min_g=20000&min_b=20000'
            //     ]
            // })
            // m.addLayer({
            //     id: 'raster-layer',
            //     source: 'src',
            //     type: 'raster',
            //     minzoom: 5,
            //     maxzoom: 22
            // })
        })
    }, 1)
})
</script>

<style scoped></style>
