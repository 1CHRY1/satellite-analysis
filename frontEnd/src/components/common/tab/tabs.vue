<template>
    <a-tabs v-model:activeKey="activeKey" type="card" class="custom-tabs">
        <slot :activeKey="activeKey" :setActiveKey="setActiveKey"></slot>
    </a-tabs> 
</template>

<script setup lang="ts">

const activeKey = defineModel<string>('activeKey', { required: true })

const setActiveKey = (key: string) => {
    activeKey.value = key
}

</script>

<style scoped>
.custom-tabs :deep(.ant-tabs-nav) {
    margin-bottom: 0.5rem;
    transition: 0.3s ease-in-out;
}

.custom-tabs :deep(.ant-tabs-nav-wrap) {
    display: flex;
}

.custom-tabs :deep(.ant-tabs-nav-list) {
    width: 100%;
    flex: 1;
    display: flex;
    justify-content: space-evenly;
}

.custom-tabs :deep(.ant-tabs-tab) {
    flex: 1;
    padding: 0.5rem 1rem;
    border-radius: 0.25rem 0.25rem 0 0;
    transition: all 0.2s ease;
    display: flex;
    justify-content: center;
    align-items: center;
}

.custom-tabs :deep(.ant-tabs-tab-active) {
    background-color: #e0f2fe !important;
    border-color: #7dd3fc !important;
}

.custom-tabs :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
    color: #0284c7 !important;
    font-weight: 500;
}
</style>