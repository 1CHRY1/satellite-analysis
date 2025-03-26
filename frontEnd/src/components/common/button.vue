<template>
    <a-tooltip :title="tooltip">
        <a-button class="cuz-buttom" :type="type" :size="size" :shape="shape" :loading="loading" :disabled="disabled"
            @click="handleClick">
            <div class="flex flex-row items-center justify-center gap-2">
                <slot></slot>
            </div>
        </a-button>
    </a-tooltip>
</template>

<script setup lang="ts">

defineProps<{
    type?: 'primary' | 'default' | 'dashed' | 'text' | 'link'
    size?: 'small' | 'middle' | 'large'
    shape?: 'default' | 'circle' | 'round'
    loading?: boolean
    disabled?: boolean
    tooltip?: string
}>()

const emit = defineEmits(['click'])
const handleClick = () => {
    emit('click')
}
</script>
<style scoped>
@reference 'tailwindcss';

.custom-button {
    @apply flex items-center gap-2 border-2 border-sky-500 text-sky-500 transition-all duration-200 ease-in-out;
}

.custom-button:hover {
    @apply border-sky-400 bg-sky-400;
}
</style>
