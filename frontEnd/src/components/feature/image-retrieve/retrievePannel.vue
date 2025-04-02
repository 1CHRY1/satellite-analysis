<template>
    <div class="pannel-container cuz-scroll">
        <div class="pannel-content" v-if="stage === 'image-filter'">
            <ImageFilterContent />
        </div>
        <div class="pannel-content" v-else-if="stage === 'image-operation'"></div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ImageFilterContent from './imageFilterContent.vue'

const stage = ref<'image-filter' | 'image-operation'>('image-filter')
</script>

<style scoped>
@reference 'tailwindcss';

.pannel-container {
    @apply absolute top-4 left-4 z-10 max-h-[88vh] w-[24vw] overflow-y-auto rounded-xl bg-white shadow-gray-500 backdrop-blur-lg;
}

.pannel-content {
    @apply flex h-full w-full flex-col items-center justify-center px-4 py-3;
}
</style>
