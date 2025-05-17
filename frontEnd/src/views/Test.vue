<template>
    <div>
        <button @click="startProcessing" class="bg-amber-400 px-3 py-2">开始处理图片</button>
        <div v-for="imageUrl in combinedImageUrls" :key="imageUrl">
            <img :src="imageUrl" alt="Combined Image">
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue';
import bandMergeHelper from '@/util/image/util'


let combinedImageUrls = ref<string[]>([]);

const startProcessing = () => {
    console.time('Combining images...')
    combinedImageUrls.value = [];

    const testUrls = allImageUrls

    const oneMergeParams = {
        redPath: testUrls[0],
        greenPath: testUrls[1],
        bluePath: testUrls[2],
    }
    bandMergeHelper.mergeOne(oneMergeParams, (url) => {
        console.timeEnd('Combining images...')
        combinedImageUrls.value.push(url)
    })
};

let allImageUrls: string[] = []

onMounted(async () => {
    allImageUrls = [
        // "http://223.2.32.166:30900/test-images/GF-1_PMS/Level_1A/tif/GF1C_PMS_E117.3_N36.9_20250419_L1A1022430485_beauty/Band_3.tif",
        // "http://223.2.32.166:30900/test-images/GF-1_PMS/Level_1A/tif/GF1C_PMS_E117.3_N36.9_20250419_L1A1022430485_beauty/Band_2.tif",
        // "http://223.2.32.166:30900/test-images/GF-1_PMS/Level_1A/tif/GF1C_PMS_E117.3_N36.9_20250419_L1A1022430485_beauty/Band_1.tif",

        "http://223.2.32.166:30900/test-images/Sentinel-1A_SAR/GRDH/tif/s1a-iw-grd-20250104-cog/Band_2.tif",
        "http://223.2.32.166:30900/test-images/Sentinel-1A_SAR/GRDH/tif/s1a-iw-grd-20250104-cog/Band_1.tif",
        "http://223.2.32.166:30900/test-images/Sentinel-1A_SAR/GRDH/tif/s1a-iw-grd-20250104-cog/Band_0.tif",

        // "http://223.2.32.166:30900/test-images/Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250321_20250327_02_T1/Band_3.tif",
        // "http://223.2.32.166:30900/test-images/Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250321_20250327_02_T1/Band_2.tif",
        // "http://223.2.32.166:30900/test-images/Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250321_20250327_02_T1/Band_1.tif",

        // "http://223.2.32.166:30900/test-images/Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250321_20250327_02_T1/Band_1.tif",
        // "http://223.2.32.166:30900/test-images/Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250321_20250327_02_T1/Band_2.tif",
        // "http://223.2.32.166:30900/test-images/Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250321_20250327_02_T1/Band_1.tif",

        // "http://223.2.32.166:30900/test-images/Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250321_20250327_02_T1/Band_4.tif",
        // "http://223.2.32.166:30900/test-images/Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250321_20250327_02_T1/Band_3.tif",
        // "http://223.2.32.166:30900/test-images/Landsat8_OLI/L2SP/tif/LC08_L2SP_120035_20250321_20250327_02_T1/Band_2.tif",
    ]

    // const promises = allImageUrls.map(async (url, index) => {
    //     const url1 = await makeUrl(url)
    //     allImageUrls[index] = url1
    // });

    // await Promise.all(promises)

    // for (let i = 0; i < allImageUrls.length; i += 3) {
    //     const group = allImageUrls.slice(i, i + 3) as [string, string, string];
    //     if (group.length === 3) {
    //         imageUrlLists.push(group);
    //     }
    // }
    // console.log(imageUrlLists[0], 'imageUrlLists')


});

onUnmounted(() => {
    // workerPool.terminateAll(); // 在组件卸载时终止所有 worker
});
</script>