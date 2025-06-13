<template>
    <div class="custom-segmented">
        <div 
            v-for="(option, index) in options" 
            :key="option.value || index"
            class="segment-item"
            :class="{ active: activeTab === (option.value || option) }"
            @click="handleSelectTab(option.value || option)">
            <span class="segment-text">{{ option.label || option }}</span>
        </div>
    </div>
</template>

<script setup lang="ts">
import type { PropType } from 'vue'

const props = defineProps({
    options: {
        type: Array as PropType<any[]>,
        required: true
    },
    activeTab: {
        type: String,
        required: true
    },
})

const emit = defineEmits(['update:modelValue', 'change'])

const handleSelectTab = (value: any) => {
    emit('update:modelValue', value)
    emit('change', value)
}

</script>

<style scoped src="./commonCom.css">

</style>
