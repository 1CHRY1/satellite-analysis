<template>
    <div class="section-header" ref="sectionHeader">
        <div class="section-icon cursor-pointer">
            <a-tooltip>
                <template #title>返回</template>
                <ArrowLeft :size="18" @click="$emit('toggle', 'noCloud')"/>
            </a-tooltip>
        </div>
        <h2 class="section-title">历史无云一版图</h2>
    </div>
    <div class="flex border-b border-[#2c3e50] mb-4 justify-center">
        <a-segmented v-model:value="activeTab" :options="historyClassTabs" />
    </div>
    <div class="gap-5 p-2 flex border-b border-[#2c3e50] mb-4">
        <button v-for="tab in historyClassTabs" :key="tab.value" @click="activeTab = tab.value" :class="[
            'flex-1 text-center px-4 py-2 text-sm font-medium ',
            activeTab === tab.value
                ? 'border-b-2 border-[#38bdf8] text-[#38bdf8]'
                : 'text-gray-400 hover:text-[#38bdf8]'
        ]">
            {{ tab.label }}
        </button>
    </div>
    <div class="section-content">
        <div class="config-container">
            <div class="config-item w-full">
                <div class="config-label relative">
                    <MapIcon :size="16" class="config-icon" />
                    <span>研究区选择</span>
                </div>
                <!-- <div class="config-control justify-center">
                    <RegionSelects v-model="region" :placeholder="['选择省份', '选择城市', '选择区县']"
                        class="flex gap-2"
                        select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
                </div> -->
                
                <div v-if="activeTab === 'region'" class="config-control justify-center">
                    <RegionSelects v-model="region" :placeholder="['选择省份', '选择城市', '选择区县']"
                        class="flex gap-2"
                        select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
                </div>
                <div v-else-if="activeTab === 'poi'" class="config-control justify-center w-full">
                    <el-select v-model="selectedPOI" filterable remote reserve-keyword value-key="id"
                        placeholder="请输入 POI 关键词" :remote-method="fetchPOIOptions"
                        class="!w-[90%] bg-[#0d1526] text-white" popper-class="bg-[#0d1526] text-white">
                        <el-option v-for="item in poiOptions" :key="item.id"
                            :label="item.name + '(' + item.pname + item.cityname + item.adname + item.address + ')'"
                            :value="item" />
                    </el-select>
                </div>
            </div>
        </div>
        <div class="config-container">
            <div v-for="item in caseList" class="config-item" :key="item.caseId">
                <div class="config-label relative">
                    <Image :size="16" class="config-icon" />
                    <span>{{ item.caseName }}</span>
                    <div class="absolute right-0 cursor-pointer">
                        <a-tooltip>
                            <template #title>预览</template>
                            <Eye v-if="!item.status" :size="16" class="cursor-pointer"/>
                            <EyeOff v-else :size="16" class="cursor-pointer"/>
                        </a-tooltip>
                    </div>
                </div>
                <div class="config-control flex-col !items-start">
                    <div class="flex w-full flex-col gap-2">
                        <div class="result-info-container">
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <Grid3x3 :size="12" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">格网分辨率</div>
                                    <div class="result-info-value">
                                        {{ item.resolution }}km
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <CalendarIcon :size="12" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">生成时间</div>
                                    <div class="result-info-value">
                                        {{ formatTimeToText(item.createTime) }}
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <DatabaseIcon :size="12" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">使用数据</div>
                                    <div>
                                        {{ item.dataSet }}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="flex h-[60px] justify-around">
                <el-pagination background layout="prev, pager, next" v-model:current-page="currentPage" :total="total"
                    :page-size="pageSize" @current-change="getCaseList" @next-click="" @prev-click="">
                </el-pagination>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ArrowLeft, CalendarIcon, DatabaseIcon, Eye, EyeOff, Grid3x3, Image, TimerIcon } from 'lucide-vue-next';
import { useViewHistoryModule } from './viewHistory';
import { onMounted } from 'vue';
import { formatTimeToText } from '@/util/common';

const { caseList, currentPage, sectionHeader, pageSize, total, historyClassTabs, activeTab, getCaseList } = useViewHistoryModule()

onMounted(() => {
    getCaseList(currentPage.value)
})
</script>

<style scoped src="../tabStyle.css">
:deep(.border-box-content) {
    padding: 1.5rem;
}
</style>