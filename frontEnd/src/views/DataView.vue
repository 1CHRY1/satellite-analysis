<template>
    <div class="relative bg-sky-200 flex">
        <MapComp class="flex-1" />
        <ImageRetrievalPanel />
    </div>
</template>

<script setup lang="ts">
import MapComp from '@/components/data/MapComp.vue'
import ImageRetrievalPanel from '@/components/data/ImageRetrievalPanel.vue'
import TileSelectionPanel from '@/components/data/TileSelectionPanel.vue'
</script>
