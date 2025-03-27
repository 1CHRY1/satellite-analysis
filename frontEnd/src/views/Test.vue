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
                "http://localhost:5000/tile/{z}/{x}/{y}.png"
            ]
        })
        map.addLayer({
            id:"layer",
            type:"raster",
            source:"src",
            minzoom:0,
            paint:{
                
            }
        })
    })

    

})


</script>