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
    
    <div class="section-content">
        <div class="config-container">
            <segmented :options="historyClassTabs" :active-tab="activeTab" @change="handleSelectTab" />
        </div>
        <div class="config-container" v-if="activeTab === 'running'">
            <div class="config-item">
                <div class="flex items-center justify-between">
                    <div class="config-label relative">
                        <loadingIcon /> 共 2 个任务运行中
                    </div>
                    <a-button class="a-button">刷新全部</a-button>
                </div>
            </div>
        </div>
        <div class="config-container" v-else>
            <div class="config-item">
                <div class="config-label relative">
                    <ListFilter :size="22" class="config-icon" />
                    <span>筛选条件</span>
                    <div class="absolute right-0 cursor-pointer">
                        <ChevronDown v-if="!isExpand" :size="22" @click="isExpand = true" />
                        <ChevronUp v-else @click="isExpand = false" :size="22" />
                    </div>
                </div>
                <div v-show="isExpand" class="config-control flex-col !items-start">
                    <div class="flex w-full flex-col gap-2">
                        <div class="result-info-container">
                            <div class="result-info-item">
                                <div class="result-info-content">
                                    <div class="result-info-label">生成时间</div>
                                    <div class="result-info-value">
                                        <select v-model="selectedTime"
                                            class="custom-select" style="width: 8rem;">
                                            <option v-for="option in timeOptionList" :key="option" :value="option"
                                                class="custom-option">
                                                {{ option.label }}
                                            </option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-content">
                                    <div class="result-info-label">格网分辨率</div>
                                    <div class="result-info-value">
                                        <select v-model="selectedResolution"
                                            class="custom-select" style="width: 6rem;">
                                            <option v-for="option in resolutionList" :key="option" :value="option"
                                                class="custom-option">
                                                {{ `${option === EMPTY_RESOLUTION ? '请选择' : option + 'km'}` }}
                                            </option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-content">
                                    <div class="result-info-label">行政区划</div>
                                    <div class="result-info-value">
                                        <RegionSelects v-model="selectedRegion" :placeholder="['选择省份', '选择城市', '选择区县']"
                                            class="flex gap-2"
                                            select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="config-control !items-start justify-end relative">
                    <div class="flex w-1/4 flex-col gap-2">
                        <a-button type="primary" class="a-button">应用</a-button>  
                    </div>
                </div>
            </div>
        </div>
        <div class="config-container">
            <div v-for="item in caseList" class="config-item" :key="item.caseId">
                <div class="config-label relative">
                    <Image :size="16" class="config-icon" />
                    <span>{{ `${item.address}无云一版图` }}</span>
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
import { ArrowBigDown, ArrowLeft, CalendarIcon, ChevronDown, ChevronDownCircle, ChevronDownIcon, ChevronDownSquare, ChevronUp, DatabaseIcon, ExpandIcon, Eye, EyeOff, Grid3x3, Image, ListFilter, TimerIcon } from 'lucide-vue-next';
import { useViewHistoryModule } from './viewHistory';
import { onMounted } from 'vue';
import { formatTimeToText } from '@/util/common';
import segmented from '@/components/common/segmented.vue';
import loadingIcon from '@/components/common/loadingIcon.vue'
import { RegionSelects } from 'v-region'

const { caseList, currentPage, sectionHeader, pageSize, total, historyClassTabs, activeTab, handleSelectTab,
     selectedRegion, selectedTime, timeOptionList, selectedResolution, resolutionList, EMPTY_RESOLUTION, getCaseList,
    isExpand } = useViewHistoryModule()

onMounted(() => {
    getCaseList(currentPage.value)
})
</script>

<style scoped src="../tabStyle.css">
:deep(.border-box-content) {
    padding: 1.5rem;
}
</style>