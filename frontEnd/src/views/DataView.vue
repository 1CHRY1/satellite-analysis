<template>
    <div class="relative flex flex-1 flex-row bg-black">
        <ImageSearcher class="w-[28vw]" />
        <MapComp class="flex-1" :style="'local'" :proj="'globe'" />
    </div>
</template>

<script setup lang="ts">
import MapComp from '@/components/feature/map/mapComp.vue'
import ImageSearcher from '@/components/dataCenter/imageSearcher.vue'
</script>
