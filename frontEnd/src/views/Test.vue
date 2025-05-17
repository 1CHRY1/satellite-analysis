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
import WorkerPool from '@/util/worker/workerPool';

const workerPool = WorkerPool;
// const imageUrlLists = ref<[string, string, string][]>([]); // 你的四五十个图片 URL 分组
let combinedImageUrls = ref<string[]>([]);
let imageUrlLists: [string, string, string][] = ([]);

watch(combinedImageUrls, () => {
    console.timeEnd('Combining images...')
});
function makeUrl(url: string) {
    const titler = 'http://localhost:8000/preview'
    const params = new URLSearchParams()
    params.append('format', 'png')
    params.append('url', url)
    params.append('max_size', '512')
    return `${titler}?${params.toString()}`
}
const handleTaskCompleted = (result: { id: number | string; combinedPngBuffer: ArrayBuffer | null }) => {
    if (result.combinedPngBuffer) {
        const blob = new Blob([result.combinedPngBuffer], { type: 'image/png' });
        combinedImageUrls.value.push(URL.createObjectURL(blob));
    } else {
        console.error(`Task ${result.id} failed to combine images.`);
    }
}

const handleError = (error: Error | string) => {
    console.error('Worker Pool Error:', error);
}

const startProcessing = () => {
    console.time('Combining images...')
    combinedImageUrls.value = [];
    workerPool.setTaskCompletedCallback(handleTaskCompleted);
    workerPool.setErrorCallback(handleError);

    // 假设你的四五十个 URL 已经分组为若干个 [string, string, string][]
    imageUrlLists.forEach(urls => {
        workerPool.enqueueTask(urls);
    });

};


onMounted(() => {
    const allImageUrls: string[] = [
        'D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF',
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF",
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF",

        'D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF',
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B1.TIF",
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF",

        'D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF',
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF",
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B1.TIF",

        'D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B1.TIF',
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF",
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF",

        'D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B1.TIF',
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF",
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF",

        'D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B1.TIF',
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF",
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF",

        'D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B5.TIF',
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF",
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B1.TIF",

        'D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF',
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF",
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF",


        'D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF',
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF",
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF",


        'D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF',
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF",
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF",


        'D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B1.TIF',
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF",
        "D:/edgedownload/LC08_L2SP_121038_20200922_20201006_02_T2/LC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF",
    ]

    allImageUrls.forEach((url, index) => {
        allImageUrls[index] = makeUrl(url)
    })

    for (let i = 0; i < allImageUrls.length; i += 3) {
        const group = allImageUrls.slice(i, i + 3) as [string, string, string];
        if (group.length === 3) {
            imageUrlLists.push(group);
        }
    }
});

onUnmounted(() => {
    workerPool.terminateAll(); // 在组件卸载时终止所有 worker
});
</script>