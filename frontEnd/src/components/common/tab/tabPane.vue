<template>
    <a-tab-pane :key="key" :tab="tab" :force-render="forceRender" :active="isActive">
        <div class="tab-content">
            <slot></slot>
        </div>
    </a-tab-pane>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
    key: string
    tab: string
    forceRender?: boolean
    activeKey: string
}>()

const isActive = computed(() => props.activeKey === props.key)


</script>

<style scoped>
.tab-content {
    padding: 0.5rem 0;
}
</style>