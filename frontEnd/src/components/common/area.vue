<template>
    <div class="upload-area" @click="handleClick">
        <div class="upload-intro" v-if="showIntro">
            <span class="upload-intro-icon">
                <slot name="upload-icon"> </slot>
            </span>
            <span class="upload-intro-text">
                <slot name="upload-intro"> 上传引导 </slot>
            </span>
        </div>
        <div class="upload-content" v-else>
            <slot name="upload-content"> 上传后要显示的内容 </slot>
        </div>
    </div>
</template>

<script setup lang="ts">
const props = defineProps({
    showIntro: {
        type: Boolean,
        default: true,
    },
})
const emit = defineEmits(['click'])

const handleClick = () => {
    emit('click')
}
</script>

<style scoped>
@reference 'tailwindcss';

.upload-area {
    @apply mx-4 my-3 flex h-20 cursor-pointer flex-col items-center justify-center rounded-md border-2 border-dashed border-gray-300 bg-gray-50 transition-all duration-200 ease-in-out hover:border-sky-200 hover:bg-sky-50;
}

.upload-intro {
    @apply flex flex-row items-center justify-center;
}

.upload-intro-icon {
    @apply mr-2 text-sm text-sky-700;
}

.upload-intro-text {
    @apply text-sm text-gray-700;
}

.upload-content {
    @apply flex flex-col items-center justify-center;
}
</style>
