<template>
    <!-- Header -->
    <div class="section-header" ref="sectionHeader">
        <div class="section-icon cursor-pointer">
            <a-tooltip>
                <template #title>{{ t('datapage.history.back') }}</template>
                <ArrowLeft :size="18" @click="$emit('toggle', 'analysis')" />
            </a-tooltip>
        </div>
        <h2 class="section-title">历史调用记录</h2>
    </div>

    <!-- Content -->
    <div class="section-content">
        <!-- Content-1 Tab -->
        <div class="config-container">
            <segmented :options="historyClassTabs" :active-tab="activeTab" @change="handleSelectTab" />
        </div>

        <!-- Content-2 Running cases -->
        <div class="config-container" v-if="activeTab === 'RUNNING'">
            <div class="config-item">
                <div class="flex items-center justify-between">
                    <div class="config-label relative">
                        <span v-if="pendingTaskList.length > 0 || isPending" class="flex items-center gap-2">
                            <loadingIcon />
                            {{ (pendingTaskList.length > 0 || isPending) ? t("datapage.history.wait") : '' }}
                        </span>
                        <span v-else class="flex items-center gap-2">
                            <loadingIcon v-if="total > 0" />
                            {{ total > 0 ? `共 ${total} 个任务运行中` : t('datapage.history.no_task') }}
                        </span>
                    </div>
                    <a-button class="a-button" @click="getMethLibCaseList">{{ t('datapage.history.refresh')
                    }}</a-button>
                </div>
            </div>
            <div v-for="item in methLibCaseList" class="config-item" :key="item.caseId">
                <a-badge-ribbon text="运行中" style="position: absolute; top: -9px; right: -20px;" color="#ffa726">
                    <div class="config-label relative">
                        <Image :size="16" class="config-icon" />
                        <span>{{ `${item.caseId}` }}</span>
                    </div>
                    <div class="config-control flex-col !items-start">
                        <div class="flex w-full flex-col gap-2">
                            <div class="result-info-container">
                                <div class="result-info-item">
                                    <div class="result-info-icon">
                                        <CalendarIcon :size="12" />
                                    </div>
                                    <div class="result-info-content">
                                        <div class="result-info-label">{{ t('datapage.history.sta_time') }}</div>
                                        <div class="result-info-value">
                                            {{ formatTimeToText(item.createTime) }}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </a-badge-ribbon>

            </div>
            <a-empty v-if="total === 0" />
        </div>

        <!-- Content-3 Complete cases -->
        <div class="config-container" v-else>
            <div class="config-item">
                <div class="config-label relative">
                    <ListFilter :size="22" class="config-icon" />
                    <span>{{ t('datapage.history.condition') }}</span>
                    <div class="absolute right-0 cursor-pointer">
                        <ChevronDown v-if="isExpand" :size="22" @click="isExpand = false" />
                        <ChevronUp v-else @click="isExpand = true" :size="22" />
                    </div>
                </div>
                <div v-show="isExpand" class="config-control flex-col !items-start">
                    <div class="flex w-full flex-col gap-2">
                        <div class="result-info-container">
                            <div class="result-info-item">
                                <div class="result-info-content">
                                    <div class="result-info-label">{{ t('datapage.history.create_time') }}</div>
                                    <div class="result-info-value">
                                        <select v-model="selectedTimeIndex" class="custom-select" style="width: 15rem;">
                                            <option v-for="(option, index) in timeOptionList" :key="option.label"
                                                :value="index" class="custom-option">
                                                {{ option.label }}
                                            </option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="config-control">
                    <div class="flex justify-between gap-3 items-center w-full">
                        <span class="result-info-label">共找到 {{ total }} 条记录</span>
                        <div class="flex gap-3">
                            <a-button type="primary" class="a-button-dark" @click="reset">{{ t('datapage.history.clear')
                            }}</a-button>
                            <a-button type="primary" class="a-button" @click="getMethLibCaseList">{{
                                t('datapage.history.fliter') }}</a-button>
                        </div>
                    </div>
                </div>
            </div>
            <div v-for="(item, index) in methLibCaseList" class="config-item" :key="item.caseId">
                <div class="config-label relative">
                    <Image :size="16" class="config-icon" />
                    <span>{{ `${item.method?.name}` }}</span>
                    <div class="absolute right-0 cursor-pointer">
                        <Eye v-if="previewList[index]" @click="unPreview" :size="16" class="cursor-pointer" />
                        <EyeOff v-else :size="16" @click="handleShowModal(item)" class="cursor-pointer" />
                    </div>
                </div>
                <div class="config-control flex-col !items-start">
                    <div class="flex w-full flex-col gap-2">
                        <div class="result-info-container">
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <CalendarIcon :size="12" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">{{ t('datapage.history.create_time') }}</div>
                                    <div class="result-info-value">
                                        {{ formatTimeToText(item.createTime) }}
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item" v-if="item.method">
                                <div class="result-info-icon">
                                    <FileIcon :size="12" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">处理文件</div>
                                    <div class="result-info-value">
                                        {{ getInputFileNames(item).join(", ") }}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <a-modal v-model:open="showModal" title="栅格可视化配置" @cancel="handleModalCancel" @ok="handleConfirm">
                <a-form :model="formState" :rules="formRules" ref="visFormRef" layout="vertical">
                    <a-form-item label="选择可视化色带" name="colormapName">
                        <a-select v-model:value="formState.colormapName" style="width: 100%" placeholder="请选择可视化色带">
                            <a-select-option v-for="item in colormapOptions" :key="item.value" :value="item.value">
                                <div class="color-option">
                                    <span>{{ item.label }}</span>
                                    <div class="color-strip" :style="{ background: item.gradient }"></div>
                                </div>
                            </a-select-option>
                        </a-select>
                    </a-form-item>

                    <a-form-item label="选择输出文件(*.tif)" name="selectedTif">
                        <a-select v-model:value="formState.selectedTif" style="width: 100%" placeholder="请选择要可视化的输出栅格文件"
                            @change="handleTifChange">
                            <a-select-option v-for="(file, index) in tifOptions" :key="file.value" :value="index">
                                {{ file.label }}
                            </a-select-option>
                        </a-select>
                    </a-form-item>

                    <a-form-item label="选择可视化波段" name="selectedBidx">
                        <a-select v-model:value="formState.selectedBidx" style="width: 100%" placeholder="请选择可视化波段"
                            @change="handleBidxChange">
                            <a-select-option v-for="bidx in bidxOptions" :key="bidx" :value="bidx">
                                {{ bidx }}
                            </a-select-option>
                        </a-select>
                    </a-form-item>

                    <a-row :gutter="16" v-if="selectedBandInfo">
                        <a-col :span="24">
                            <a-divider orientation="left">波段详细信息</a-divider>
                            <a-descriptions bordered size="small" :column="2">
                                <a-descriptions-item label="统计 Min">{{ selectedBandInfo.min }}</a-descriptions-item>
                                <a-descriptions-item label="统计 Max">{{ selectedBandInfo.max }}</a-descriptions-item>
                                <a-descriptions-item label="平均值">{{ selectedBandInfo.mean }}</a-descriptions-item>
                                <a-descriptions-item label="标准差">{{ selectedBandInfo.std }}</a-descriptions-item>
                            </a-descriptions>
                        </a-col>
                    </a-row>

                    <a-form-item label="自定义可视化范围 (Min/Max)" :name="['range', 'min']" style="margin-top: 16px;">
                        <a-row :gutter="8">
                            <a-col :span="12">
                                <a-input-number v-model:value="formState.range.min"
                                    :min="selectedBandInfo ? selectedBandInfo.min : null" :max="formState.range.max"
                                    style="width: 100%" placeholder="最小值 (Min)" />
                            </a-col>
                            <a-col :span="12">
                                <a-input-number v-model:value="formState.range.max" :min="formState.range.min"
                                    :max="selectedBandInfo ? selectedBandInfo.max : null" style="width: 100%"
                                    placeholder="最大值 (Max)" />
                            </a-col>
                        </a-row>
                        <template #extra>
                            建议范围: {{ selectedBandInfo ? `${selectedBandInfo.min} ~ ${selectedBandInfo.max}` : '请先选择波段'
                            }}
                        </template>
                    </a-form-item>
                </a-form>
            </a-modal>
            <a-empty v-if="total === 0" />
        </div>

        <!-- Content-4 Pagination -->
        <div class="config-container">
            <div class="flex h-[60px] justify-around">
                <el-pagination v-if="total > 0" background layout="prev, pager, next" v-model:current-page="currentPage"
                    :total="total" :page-size="pageSize" @current-change="getMethLibCaseList" @next-click=""
                    @prev-click="">
                </el-pagination>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ArrowBigDown, ArrowLeft, CalendarIcon, ChevronDown, ChevronDownCircle, ChevronDownIcon, ChevronDownSquare, ChevronUp, DatabaseIcon, ExpandIcon, Eye, EyeOff, Grid3x3, Image, ListFilter, TimerIcon, Upload, Download, Info, FileDown, FileIcon } from 'lucide-vue-next';
import { useViewHistoryModule } from './composables/useHistory';
import { onMounted, onUnmounted, computed, watch, ref, reactive } from 'vue';
import { formatTimeToText } from '@/util/common';
import segmented from '@/components/common/segmented.vue';
import loadingIcon from '@/components/common/loadingIcon.vue'
import { RegionSelects } from 'v-region'
import { useTaskPollModule } from './taskPoll.ts'
import bus from '@/store/bus'
import { useTaskStore } from '@/store'
import { ezStore } from '@/store'
import { message } from 'ant-design-vue'


import { useI18n } from 'vue-i18n'
import type { MethLib } from '@/api/http/analytics-display/methlib.type.ts';
import { MehTwoTone } from '@ant-design/icons-vue';
const { t } = useI18n()

const { methLibCaseList, currentPage, sectionHeader, pageSize, total, historyClassTabs, activeTab, handleSelectTab,
    selectedTimeIndex, timeOptionList, getMethLibCaseList, isExpand, reset, previewList, handleShowModal, unPreview,
    getInputFileNames, showModal, handleModalCancel,
    handleConfirm,
    colormapName,
    colormapOptions, formRules,
    formState,
    handleTifChange,
    tifOptions,
    handleBidxChange,
    selectedBandInfo,
    bidxOptions, visFormRef
} = useViewHistoryModule()
const { pendingTaskList, startPolling, stopPolling } = useTaskPollModule()
const taskStore = useTaskStore()
// 检测由noCloud跳转至history面板时（setCurrentPanel），前端是否还在初始化任务（calNoCloud处理请求参数）
const isPending = computed(() => taskStore._isInitialTaskPending)

onMounted(() => {
    bus.on('methlib-case-list-refresh', getMethLibCaseList)
    console.log('methLibHistory mounted')
    getMethLibCaseList()
    console.log('MehLibcaseList obtained')
    startPolling()
    console.log('polling started')
})

watch(activeTab, (newVal) => {
    if (newVal === 'RUNNING') {
        startPolling();
    } else {
        stopPolling();
    }
});

watch(activeTab, (newVal) => {
    if (newVal === 'RUNNING') {
        startPolling();
    } else {
        stopPolling();
    }
});

onUnmounted(() => {
    bus.off('methlib-case-list-refresh', getMethLibCaseList)
    stopPolling()
    unPreview()
})
</script>

<style scoped src="../tabStyle.css" lang="scss">
:deep(.border-box-content) {
    padding: 1.5rem;
}

/* 添加一些自定义样式 */
.space-y-4>*+* {
    margin-top: 1rem;
}

.a-button {
    display: inline-flex;
    align-items: center;
}

.color-option {
    display: flex;
    flex-direction: column;
    width: 100%;
}

.color-strip {
    width: 100%;
    height: 20px;
    margin-top: 4px;
    border-radius: 2px;
}

/* 调整下拉框选项宽度 */
:deep(.ant-select-dropdown) {
    min-width: 200px !important;
}

:deep(.ant-select-item) {
    padding: 8px 12px;
}
</style>