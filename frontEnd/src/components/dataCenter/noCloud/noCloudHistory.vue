<template>
    <!-- Header -->
    <div class="section-header" ref="sectionHeader">
        <div class="section-icon cursor-pointer">
            <a-tooltip>
                <template #title>{{t('datapage.history.back')}}</template>
                <ArrowLeft :size="18" @click="$emit('toggle', 'noCloud')"/>
            </a-tooltip>
        </div>
        <h2 class="section-title">{{t('datapage.history.his_noclo')}}</h2>
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
                            <loadingIcon/>
                            {{ (pendingTaskList.length > 0 || isPending) ? t("datapage.history.wait") : '' }}
                        </span>
                        <span v-else class="flex items-center gap-2">
                            <loadingIcon v-if="total > 0"/>
                            {{ total > 0 ? `共 ${total} 个任务运行中` : t('datapage.history.no_task') }}
                        </span>
                    </div>
                    <a-button class="a-button" @click="getCaseList">{{t('datapage.history.refresh')}}</a-button>
                </div>
            </div>
            <div v-for="item in caseList" class="config-item" :key="item.caseId">
                <a-badge-ribbon text="运行中" style="position: absolute; top: -9px; right: -20px;" color="#ffa726">
                    <div class="config-label relative">
                        <Image :size="16" class="config-icon" />
                        <span>{{ `${item.address}无云一版图` }}</span>
                    </div>
                    <div class="config-control flex-col !items-start">
                        <div class="flex w-full flex-col gap-2">
                            <div class="result-info-container">
                                <div class="result-info-item">
                                    <div class="result-info-icon">
                                        <Grid3x3 :size="12" />
                                    </div>
                                    <div class="result-info-content">
                                        <div class="result-info-label">{{t('datapage.history.resolution')}}</div>
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
                                        <div class="result-info-label">{{t('datapage.history.sta_time')}}</div>
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
                                        <div class="result-info-label">{{t('datapage.history.data')}}</div>
                                        <div>
                                            {{ item.dataSet }}
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
                    <span>{{t('datapage.history.condition')}}</span>
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
                                    <div class="result-info-label">{{t('datapage.history.create_time')}}</div>
                                    <div class="result-info-value">
                                        <select v-model="selectedTimeIndex"
                                            class="custom-select" style="width: 8rem;">
                                            <option v-for="(option, index) in timeOptionList" :key="option.label" :value="index"
                                                class="custom-option">
                                                {{ option.label }}
                                            </option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-content">
                                    <div class="result-info-label">{{t('datapage.history.resolution')}}</div>
                                    <div class="result-info-value">
                                        <select v-model="selectedResolution"
                                            class="custom-select" style="width: 6rem;">
                                            <option v-for="option in resolutionList" :key="option" :value="option"
                                                class="custom-option">
                                                {{ `${option === EMPTY_RESOLUTION ? t('datapage.history.choose') : option + 'km'}` }}
                                            </option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-content">
                                    <div class="result-info-label">{{t('datapage.history.admin')}}</div>
                                    <div class="result-info-value">
                                        <RegionSelects v-model="selectedRegion" :placeholder="t('datapage.history.admin_choose')"
                                            class="flex gap-2"
                                            select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
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
                            <a-button type="primary" class="a-button-dark" @click="reset">{{t('datapage.history.clear')}}</a-button>  
                            <a-button type="primary" class="a-button" @click="getCaseList">{{t('datapage.history.fliter')}}</a-button>
                        </div>
                    </div>
                </div>
            </div>
            <div v-for="(item, index) in caseList" class="config-item" :key="item.caseId">
                <div class="config-label relative">
                    <Image :size="16" class="config-icon" />
                    <span>{{ `${item.address}无云一版图` }}</span>
                    <div class="absolute right-0 cursor-pointer">
                        <a-tooltip>
                            <template #title>{{t('datapage.history.preview')}}</template>
                            <Eye v-if="previewList[index]" @click="unPreview" :size="16" class="cursor-pointer"/>
                            <EyeOff v-else :size="16" @click="showResult(item.caseId, item.regionId)" class="cursor-pointer"/>
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
                                    <div class="result-info-label">{{t('datapage.history.resolution')}}</div>
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
                                    <div class="result-info-label">{{t('datapage.history.create_time')}}</div>
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
                                    <div class="result-info-label">{{t('datapage.history.data')}}</div>
                                    <div>
                                        {{ item.dataSet }}
                                    </div>
                                </div>
                            </div>
                        </div>
                        <!-- 新增按钮区域 -->
                        <div class="flex gap-2 mt-2">
                            <a-button type="primary" size="middle" @click="handlePublish(item)">
                                <Upload :size="14" class="mr-1" />
                            </a-button>
                            <a-button size="middle" @click="handleDownload(item)">
                                <Download :size="14" class="mr-1" />
                            </a-button>
                        </div>
                    </div>
                </div>
            </div>
            <a-empty v-if="total === 0" />
        </div>

        <!-- Content-4 Pagination -->
        <div class="config-container">
            <div class="flex h-[60px] justify-around">
                <el-pagination v-if="total > 0" background layout="prev, pager, next" v-model:current-page="currentPage" :total="total"
                    :page-size="pageSize" @current-change="getCaseList" @next-click="" @prev-click="">
                </el-pagination>
            </div>
        </div>
    </div>

    <!-- 发布弹窗 -->
    <a-modal 
        v-model:visible="publishModalVisible" 
        title="发布无云一版图"
        :width="600"
        @ok="handlePublishConfirm"
        @cancel="handlePublishCancel"
    >
        <div class="space-y-4">
            <div class="flex items-center gap-2 p-3 bg-gray-50 rounded">
                <Info :size="16" class="text-blue-500" />
                <span>您正在发布: {{ currentItem?.address }}无云一版图</span>
            </div>
            
            <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
                <a-form-item label="发布名称">
                    <a-input v-model:value="publishForm.name" placeholder="请输入发布名称" />
                </a-form-item>
                
                <a-form-item label="发布描述">
                    <a-textarea 
                        v-model:value="publishForm.description" 
                        placeholder="请输入发布描述"
                        :rows="4"
                    />
                </a-form-item>
                
                <a-form-item label="发布范围">
                    <a-radio-group v-model:value="publishForm.scope">
                        <a-radio value="public">公开</a-radio>
                        <a-radio value="internal">内部</a-radio>
                        <a-radio value="private">私有</a-radio>
                    </a-radio-group>
                </a-form-item>
                
                <a-form-item label="有效期">
                    <a-date-picker 
                        v-model:value="publishForm.expiryDate" 
                        placeholder="选择有效期"
                        style="width: 100%"
                    />
                </a-form-item>
            </a-form>
        </div>
    </a-modal>

    <!-- 下载弹窗 -->
    <a-modal 
        v-model:visible="downloadModalVisible" 
        title="下载无云一版图"
        :width="500"
        @ok="handleDownloadConfirm"
        @cancel="handleDownloadCancel"
    >
        <div class="space-y-4">
            <div class="flex items-center gap-2 p-3 bg-gray-50 rounded">
                <FileDown :size="16" class="text-green-500" />
                <span>准备下载: {{ currentItem?.address }}无云一版图</span>
            </div>
            
            <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
                <a-form-item label="文件格式">
                    <a-select v-model:value="downloadForm.format" placeholder="请选择文件格式">
                        <a-select-option value="tiff">TIFF</a-select-option>
                        <a-select-option value="png">PNG</a-select-option>
                        <a-select-option value="jpg">JPG</a-select-option>
                        <a-select-option value="geotiff">GeoTIFF</a-select-option>
                    </a-select>
                </a-form-item>
                
                <a-form-item label="图像质量">
                    <a-slider 
                        v-model:value="downloadForm.quality" 
                        :min="1" 
                        :max="100"
                        :marks="{ 1: '低', 50: '中', 100: '高' }"
                    />
                </a-form-item>
                
                <a-form-item label="包含元数据">
                    <a-switch v-model:checked="downloadForm.includeMetadata" />
                </a-form-item>
                
                <a-form-item label="坐标系">
                    <a-select v-model:value="downloadForm.coordinateSystem" placeholder="请选择坐标系">
                        <a-select-option value="wgs84">WGS84</a-select-option>
                        <a-select-option value="gcj02">GCJ02</a-select-option>
                        <a-select-option value="bd09">BD09</a-select-option>
                    </a-select>
                </a-form-item>
            </a-form>
            
            <div class="mt-4 p-3 bg-blue-50 rounded">
                <div class="text-sm text-gray-600">
                    预计文件大小: <span class="font-bold">~{{ estimatedFileSize }} MB</span>
                </div>
            </div>
        </div>
    </a-modal>
</template>

<script setup lang="ts">
import { ArrowBigDown, ArrowLeft, CalendarIcon, ChevronDown, ChevronDownCircle, ChevronDownIcon, ChevronDownSquare, ChevronUp, DatabaseIcon, ExpandIcon, Eye, EyeOff, Grid3x3, Image, ListFilter, TimerIcon, Upload, Download, Info, FileDown } from 'lucide-vue-next';
import { useViewHistoryModule } from './viewHistory';
import { onMounted, onUnmounted, computed, watch, ref, reactive } from 'vue';
import { formatTimeToText } from '@/util/common';
import segmented from '@/components/common/segmented.vue';
import loadingIcon from '@/components/common/loadingIcon.vue'
import { RegionSelects } from 'v-region'
import { useTaskPollModule } from './taskPoll'
import bus from '@/store/bus'
import { useTaskStore } from '@/store'

import { useI18n } from 'vue-i18n'
const { t } = useI18n()

const { caseList, currentPage, sectionHeader, pageSize, total, historyClassTabs, activeTab, handleSelectTab,
     selectedRegion, selectedTimeIndex, timeOptionList, selectedResolution, resolutionList, EMPTY_RESOLUTION, getCaseList,
    isExpand, reset, previewList, previewIndex, showResult, unPreview } = useViewHistoryModule()
const { pendingTaskList, startPolling, stopPolling } = useTaskPollModule()
const taskStore = useTaskStore()
// 检测由noCloud跳转至history面板时（setCurrentPanel），前端是否还在初始化任务（calNoCloud处理请求参数）
const isPending = computed(() => taskStore._isInitialTaskPending)

// 新增的响应式变量
const publishModalVisible = ref(false)
const downloadModalVisible = ref(false)
const currentItem = ref(null)

// 发布表单数据
const publishForm = reactive({
    name: '',
    description: '',
    scope: 'public',
    expiryDate: null
})

// 下载表单数据
const downloadForm = reactive({
    format: 'tiff',
    quality: 90,
    includeMetadata: true,
    coordinateSystem: 'wgs84'
})

// 估算文件大小
const estimatedFileSize = computed(() => {
    // 这里可以根据实际情况计算
    const baseSize = 50
    const qualityFactor = downloadForm.quality / 100
    return Math.round(baseSize * qualityFactor)
})

// 发布处理函数
const handlePublish = (item: any) => {
    currentItem.value = item
    publishModalVisible.value = true
}

const handlePublishConfirm = () => {
    console.log('发布确认', publishForm)
    // TODO: 实现发布逻辑
    publishModalVisible.value = false
    // 重置表单
    Object.assign(publishForm, {
        name: '',
        description: '',
        scope: 'public',
        expiryDate: null
    })
}

const handlePublishCancel = () => {
    publishModalVisible.value = false
}

// 下载处理函数
const handleDownload = (item: any) => {
    currentItem.value = item
    downloadModalVisible.value = true
}

const handleDownloadConfirm = () => {
    console.log('下载确认', downloadForm)
    // TODO: 实现下载逻辑
    downloadModalVisible.value = false
}

const handleDownloadCancel = () => {
    downloadModalVisible.value = false
}

onMounted(() => { 
    bus.on('case-list-refresh', getCaseList)
    console.log('noCloudHistory mounted')
    getCaseList()
    console.log('caseList obtained')
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
    bus.off('case-list-refresh', getCaseList)
    stopPolling()
    unPreview()
})
</script>

<style scoped src="../tabStyle.css">
:deep(.border-box-content) {
    padding: 1.5rem;
}

/* 添加一些自定义样式 */
.space-y-4 > * + * {
    margin-top: 1rem;
}

.a-button {
    display: inline-flex;
    align-items: center;
}
</style>