<template>
    <div class="image-item" :class="{ selected: isSelected }" @click="toggleImageSelectChange">
        <div class="image-preview">
            <img :src="image.preview" alt="preview-image" class="row-preview" />
        </div>
        <div class="image-details">
            <h4 class="image-name">{{ image.name }}</h4>
            <div class="image-meta">
                <span class="image-property">
                    <CalendarIcon :size="12" />
                    {{ formatDate(image.date) }}
                </span>
                <span class="image-property">
                    <CloudIcon :size="12" />
                    {{ image.cloud }}%
                </span>
                <span class="image-property">
                    <Move3DIcon :size="12" />
                    {{ image.crs }}
                </span>
            </div>
        </div>
        <div class="image-actions">
            <a-checkbox :checked="isSelected" @click.stop @change="handleCheckboxChange" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Move3DIcon, CloudIcon, CalendarIcon } from 'lucide-vue-next'
import type { SensorImage } from '@/api/adapter/satellite-data'

const props = defineProps<{
    image: SensorImage.SensorImageView
    selected: boolean
}>()

const emit = defineEmits(['select-change'])

const isSelected = ref(props.selected)

const handleCheckboxChange = () => {
    // Check-State has been changed
    emit('select-change', isSelected.value, props.image.id)
}

const toggleImageSelectChange = () => {
    // Modify check-state
    isSelected.value = !isSelected.value
    // Trigger after checkbox change
    emit('select-change', isSelected.value, props.image.id)
}

const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
@reference 'tailwindcss';

.image-item {
    @apply flex cursor-pointer flex-row items-center rounded-md border border-gray-200 p-2 transition-all duration-200 ease-in-out;
}

.image-item:hover {
    @apply border-sky-200 bg-gray-50;
}

.image-item.selected {
    @apply border-sky-200 bg-sky-50;
}

.image-preview {
    @apply mr-2 h-15 w-15 flex-shrink-0 overflow-hidden rounded-md;
}

.picture-preview {
    /* object-fit：保持图片比例缩放 铺满 */
    @apply h-full w-full object-cover;
}

.check-icon {
    @apply rounded-full bg-white p-1 text-sky-500;
}

.image-details {
    flex: 1;
    min-width: 0;
    @apply min-w-0 flex-1;
}

.image-name {
    @apply text-sm font-medium text-gray-800;
}

.image-meta {
    @apply mb-0.5 flex flex-wrap gap-2 text-sm text-gray-600;
}

.image-property {
    @apply flex items-center gap-1;
}

.image-actions {
    @apply ml-2;
}
</style>
