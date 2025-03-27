<template>
    <div class="absolute top-0 left-0 w-screen h-screen">
        <div class="h-[92vh]" id="map"></div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, h } from 'vue';
import { mapManager } from '@/util/map/mapManager';

onMounted(() => {
    mapManager.init('map').then((map)=>{
        console.log(map)

        map.addSource("src",{
            type:"raster",
            tiles:[
                "http://127.0.0.1:5000/tiff1/{z}/{x}/{y}.png"
            ]
        })
        map.addLayer({
            id:"layer",
            type:"raster",
            source:"src",
            minzoom:0,
        })

        map.addSource("src2",{
            type:"raster",
            tiles:[
                "http://localhost:5000/tiff2/{z}/{x}/{y}.png"
            ]
        })
        map.addLayer({
            id:"layer2",
            type:"raster",
            source:"src2",
            minzoom:0,
        })


        window.addEventListener("keydown",(e)=>{
            if(e.key === "1"){
                map.setPaintProperty("layer","raster-opacity",0.1)
                map.setPaintProperty("layer2","raster-opacity",1)
            }else if(e.key === "2"){
                map.setPaintProperty("layer","raster-opacity",1)
                map.setPaintProperty("layer2","raster-opacity",0.1)
            }
        })
    })
})


</script>