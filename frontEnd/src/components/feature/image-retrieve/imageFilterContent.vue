<template>
    <div class="w-full h-full flex flex-col gap-2">
        <CommonCard class="flex-1 h-1/3 w-full">
            <template #icon>
                <DatabaseIcon />
            </template>
            <template #title>影像数据集</template>
            <template #content>
                <CommonArea>
                    <template #upload-icon>
                        <PointerIcon :size="16" />
                    </template>
                    <template #upload-intro>点击选择数据集</template>
                    <template #upload-content>
                        上传了的东西在这
                    </template>
                </CommonArea>
            </template>
        </CommonCard>
        <CommonCard class="flex-1 h-1/3 w-full">
            <template #icon>
                <MapPinIcon />
            </template>
            <template #title>空间位置</template>
            <template #content>
                <a-tabs v-model:activeKey="activeKey" type="card" class="custom-tabs">
                    <a-tab-pane key="1" tab="地图绘制">
                        <div class="tab-content">
                            <div class="button-group">
                                <CommonButton @click="handleDrawPoint">
                                    <MapPinIcon :size="16" class="button-icon" />
                                    <span>点</span>
                                </CommonButton>
                                <a-divider type="vertical" class="divider" />
                                <CommonButton @click="handleDrawPolygon">
                                    <HexagonIcon :size="16" class="button-icon" />
                                    <span>多边形</span>
                                </CommonButton>
                            </div>
                        </div>
                    </a-tab-pane>
                    <a-tab-pane key="2" tab="文件上传" force-render>
                        <div class="tab-content">
                            <div class="upload-area !m-0" @click="handleFileUpload">
                                <upload-cloud-icon :size="22" class="upload-icon" />
                                <span class="upload-text">点击上传矢量文件</span>
                            </div>
                        </div>
                    </a-tab-pane>
                    <a-tab-pane key="3" tab="行政区检索">
                        <div class="tab-content">
                            <a-cascader class="custom-cascader" v-model:value="selectedDistrict"
                                :options="districtOption" placeholder="请选择行政区域" />
                        </div>
                    </a-tab-pane>
                </a-tabs>
            </template>

        </CommonCard>

        <CommonCard class="flex-1 h-1/3 w-full">
            <template #icon>
                <CalendarIcon />
            </template>
            <template #title>时间范围</template>
            <template #content>
                <CommonRangePicker v-model:value="dateRange" />
            </template>
        </CommonCard>

        <div class="flex-1/2 flex gap-3 py-3 px-4 w-full">
            <CommonButton class="flex-2" type="primary" @click="handleSearch">
                <SearchIcon :size="14" class="button-icon" />
                <span class="text-[14px]">开始检索</span>
            </CommonButton>
            <CommonButton class="flex-1" type="default" @click="handleReset">
                <RefreshCwIcon :size="14" class="button-icon" />
                <span class="text-[14px]">重置</span>
            </CommonButton>
        </div>
    </div>

</template>

<script setup lang="ts">
import { DatabaseIcon, PointerIcon, MapPinIcon, CalendarIcon, RefreshCwIcon, SearchIcon, HexagonIcon, UploadCloudIcon } from 'lucide-vue-next'
import { CommonCard, CommonArea, CommonRangePicker, CommonButton } from '@/components/common'
import { districtOption } from './districts'
import dayjs, { type Dayjs } from 'dayjs';
import { ref } from 'vue';

const activeKey = ref('1')
const selectedDistrict = ref<string[]>([])
const dateRange = ref<[Dayjs, Dayjs]>([dayjs('2010-10'), dayjs()])

const handleDrawPoint = () => {

}
const handleDrawPolygon = () => {

}
const handleFileUpload = () => {

}
const handleDistrictSelect = () => {

}
const handleSearch = () => {
}
const handleReset = () => {
}

</script>

<style scoped>

/* custom - a - cascader */
.custom-cascader {
    width: 100%;
}

.custom-cascader :deep(.ant-select-selector) {
    border-color: #7dd3fc !important;
}

.custom-cascader:hover :deep(.ant-select-selector) {
    border-color: #0284c7 !important;
}


/* custom - a - tab */
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
.tab-content {
    padding: 0.5rem 0;
}

.button-group {
    display: flex;
    align-items: center;
    justify-content: space-evenly;
    gap: 0.5rem;
}

</style>