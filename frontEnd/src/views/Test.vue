<template>
    <div class="bg-red-100">

    </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
// import { mapManager } from '@/util/map/mapManager';

onMounted(() => {
    // mapManager.init('map').then((map)=>{
    //     console.log(map)

    //     map.addSource("src",{
    //         type:"raster",
    //         tiles:[
    //             "http://localhost:5000/tiff1/{z}/{x}/{y}.png"
    //         ]
    //     })
    //     map.addLayer({
    //         id:"layer",
    //         type:"raster",
    //         source:"src",
    //         minzoom:0,
    //     })

    //     // map.addSource("src2",{
    //     //     type:"raster",
    //     //     tiles:[
    //     //         "http://localhost:5000/tiff2/{z}/{x}/{y}.png"
    //     //     ]
    //     // })
    //     // map.addLayer({
    //     //     id:"layer2",
    //     //     type:"raster",
    //     //     source:"src2",
    //     //     minzoom:0,
    //     // })
    // })
})


</script>