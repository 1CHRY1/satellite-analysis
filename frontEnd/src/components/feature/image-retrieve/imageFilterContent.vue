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
                <CommonTabs v-model:activeKey="activeKey">
                   
                </CommonTabs>
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
import { CommonCard, CommonArea, CommonRangePicker, CommonButton, CommonTabs, CommonTabPane } from '@/components/common'
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

.custom-cascader {
    width: 100%;
}

.custom-cascader :deep(.ant-select-selector) {
    border-color: #7dd3fc !important;
}

.custom-cascader:hover :deep(.ant-select-selector) {
    border-color: #0284c7 !important;
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