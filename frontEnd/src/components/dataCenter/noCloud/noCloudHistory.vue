<template>
    <!-- Header -->
    <div class="section-header" ref="sectionHeader">
        <div class="section-icon cursor-pointer">
            <a-tooltip>
                <template #title>{{ t('datapage.history.back') }}</template>
                <ArrowLeft :size="18" @click="$emit('toggle', 'noCloud')" />
            </a-tooltip>
        </div>
        <h2 class="section-title">历史记录</h2>
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
                    <a-button class="a-button" @click="getCaseList">{{ t('datapage.history.refresh') }}</a-button>
                </div>
            </div>
            <div v-for="item in caseList" class="config-item" :key="item.caseId">
                <a-badge-ribbon text="运行中" style="position: absolute; top: -9px; right: -20px;" color="#ffa726">
                    <div class="config-label relative">
                        <Image :size="16" class="config-icon" />
                        <span>{{ item.type == 'eoCube' ? `${item.address}时序立方体` : `${item.address}无云一版图` }}</span>
                    </div>
                    <div class="config-control flex-col !items-start">
                        <div class="flex w-full flex-col gap-2">
                            <div class="result-info-container">
                                <div class="result-info-item">
                                    <div class="result-info-icon">
                                        <Grid3x3 :size="12" />
                                    </div>
                                    <div class="result-info-content">
                                        <div class="result-info-label">{{ t('datapage.history.resolution') }}</div>
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
                                        <div class="result-info-label">{{ t('datapage.history.sta_time') }}</div>
                                        <div class="result-info-value">
                                            {{ formatTimeToText(item.createTime) }}
                                        </div>
                                    </div>
                                </div>
                                <div class="result-info-item" v-if="item.type !== 'eoCube'">
                                    <div class="result-info-icon">
                                        <DatabaseIcon :size="12" />
                                    </div>
                                    <div class="result-info-content">
                                        <div class="result-info-label">{{ t('datapage.history.data') }}</div>
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
                                        <select v-model="selectedTimeIndex" class="custom-select" style="width: 8rem;">
                                            <option v-for="(option, index) in timeOptionList" :key="option.label"
                                                :value="index" class="custom-option">
                                                {{ option.label }}
                                            </option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-content">
                                    <div class="result-info-label">{{ t('datapage.history.resolution') }}</div>
                                    <div class="result-info-value">
                                        <select v-model="selectedResolution" class="custom-select" style="width: 6rem;">
                                            <option v-for="option in resolutionList" :key="option" :value="option"
                                                class="custom-option">
                                                {{ `${option === EMPTY_RESOLUTION ? t('datapage.history.choose') :
                                                    option +
                                                    'km'}` }}
                                            </option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-content">
                                    <div class="result-info-label">{{ t('datapage.history.admin') }}</div>
                                    <div class="result-info-value">
                                        <RegionSelects v-model="selectedRegion"
                                            :placeholder="t('datapage.history.admin_choose')" class="flex gap-2"
                                            select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
                                    </div>
                                </div>
                            </div>
                            <!-- <div class="result-info-item">
                                <div class="result-info-content">
                                    <div class="result-info-label">类型</div>
                                    <div class="result-info-value">
                                        <RegionSelects v-model="selectedType"
                                            :placeholder="t('datapage.history.admin_choose')" class="flex gap-2"
                                            select-class="bg-[#0d1526] border border-[#2c3e50] text-white p-2 rounded focus:outline-none" />
                                    </div>
                                </div>
                            </div> -->
                        </div>
                    </div>
                </div>
                <div class="config-control">
                    <div class="flex justify-between gap-3 items-center w-full">
                        <span class="result-info-label">共找到 {{ total }} 条记录</span>
                        <div class="flex gap-3">
                            <a-button type="primary" class="a-button-dark" @click="reset">{{ t('datapage.history.clear')
                            }}</a-button>
                            <a-button type="primary" class="a-button" @click="getCaseList">{{
                                t('datapage.history.fliter')
                            }}</a-button>
                        </div>
                    </div>
                </div>
            </div>
            <a-modal v-model:open="isModalVisible" title="选择可视化波段" @ok="showResult" @cancel="previewIndex = null">
                <a-form :model="bandForm">
                    <a-form-item label="R" :rules="[{ required: true, message: '红波段必选' }]">
                        <a-select v-model:value="bandForm.band1">
                            <a-select-option v-for="(band, index) in currentBandList" :key="index" :value="index">{{
                                band
                            }}</a-select-option>
                        </a-select>
                    </a-form-item>

                    <a-form-item label="G" :rules="[{ required: true, message: '绿波段必选' }]">
                        <a-select v-model:value="bandForm.band2">
                            <a-select-option v-for="(band, index) in currentBandList" :key="index" :value="index">{{
                                band
                            }}</a-select-option>
                        </a-select>
                    </a-form-item>

                    <a-form-item label="B" :rules="[{ required: true, message: '蓝波段必选' }]">
                        <a-select v-model:value="bandForm.band3">
                            <a-select-option v-for="(band, index) in currentBandList" :key="index" :value="index">{{
                                band
                            }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-form>
            </a-modal>
            <div v-for="(item, index) in caseList" class="config-item" :key="item.caseId">
                <div class="config-label relative">
                    <Image :size="16" class="config-icon" />
                    <span>{{ item.type == 'eoCube' ? `${item.address}时序立方体` : `${item.address}无云一版图` }}</span>
                    <div class="absolute right-0 cursor-pointer">
                        <a-tooltip>
                            <template #title>{{ t('datapage.history.preview') }}</template>
                            <Eye v-if="previewList[index]" @click="unPreview" :size="16" class="cursor-pointer" />
                            <EyeOff v-else :size="16" @click="showModal(item.caseId, item.regionId)"
                                class="cursor-pointer" />
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
                                    <div class="result-info-label">{{ t('datapage.history.resolution') }}</div>
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
                                    <div class="result-info-label">{{ t('datapage.history.create_time') }}</div>
                                    <div class="result-info-value">
                                        {{ formatTimeToText(item.createTime) }}
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item" v-if="item.type !== 'eoCube'">
                                <div class="result-info-icon">
                                    <DatabaseIcon :size="12" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">{{ t('datapage.history.data') }}</div>
                                    <div>
                                        {{ item.dataSet }}
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item" v-else>
                                <div class="result-info-icon">
                                    <DatabaseIcon :size="12" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">时间维度</div>
                                    <div>
                                        {{ item.result.dimensions.join('、') }}
                                    </div>
                                </div>
                            </div>
                        </div>
                        <!-- 下载按钮和发布按钮 -->
                        <div class="flex gap-2 mt-2">
                            <a-button v-if="item.type !== 'eoCube'" type="primary" size="middle"
                                @click="handlePublish(item)">
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
                <el-pagination v-if="total > 0" background layout="prev, pager, next" v-model:current-page="currentPage"
                    :total="total" :page-size="pageSize" @current-change="getCaseList" @next-click="" @prev-click="">
                </el-pagination>
            </div>
        </div>
    </div>

    <!-- 发布弹窗 -->
    <a-modal v-model:visible="publishModalVisible" title="发布无云一版图" :width="600" @ok="handlePublishConfirm"
        @cancel="handlePublishCancel">
        <div class="space-y-4">

            <a-alert :message="`您正在发布: ${currentItem?.address}无云一版图`" type="info" show-icon class="status-alert">
            </a-alert>

            <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
                <a-form-item label="发布名称">
                    <a-input v-model:value="publishForm.name" placeholder="请输入发布名称" />
                </a-form-item>

                <a-form-item label="发布描述">
                    <a-textarea v-model:value="publishForm.description" placeholder="请输入发布描述" :rows="4" />
                </a-form-item>

                <a-form-item label="发布范围">
                    <a-radio-group v-model:value="publishForm.scope">
                        <a-radio value="public">公开</a-radio>
                        <a-radio value="internal">内部</a-radio>
                        <a-radio value="private">私有</a-radio>
                    </a-radio-group>
                </a-form-item>

                <a-form-item label="有效期">
                    <a-date-picker v-model:value="publishForm.expiryDate" placeholder="选择有效期" style="width: 100%" />
                </a-form-item>
            </a-form>
        </div>
    </a-modal>

    <!-- tif文件下载弹窗 -->
    <a-modal v-model:visible="tifDownloadModalVisible" title="请选择要下载的TIF文件" :width="700" :footer="null"
        @cancel="handleTifDownloadCancel">
        <div class="space-y-4">
            <div class="flex items-center gap-2 p-3 bg-[#111a2c] border border-[#15325b] rounded">
                <FileDown :size="16" class="text-green-500" />
                <span>找到{{ tifFileUrls.length }}个TIF文件</span>
            </div>
            <div class="max-h-96 overflow-y-auto">
                <div class="space-y-2">
                    <div v-for="(url, index) in tifFileUrls" :key="index" class="p-3 border border-[#15325b] rounded">
                        <a :href="url" @click.prevent="handleFileDownload(url)"
                            class="text-blue-600 hover:text-blue-800 hover:underline cursor-pointer text-xs break-all">
                            {{ url }}
                        </a>
                    </div>
                </div>
            </div>
        </div>

        <div class="flex justify-end gap-2">
            <a-button @click="handleTifDownloadCancel">
                关闭
            </a-button>
            <a-button type="primary" @click="() => tifFileUrls.forEach(url => handleFileDownload(url))">
                全部下载
            </a-button>
        </div>
    </a-modal>
    <!-- 时间线可视化时序立方体 -->
    <!-- <TimeLine :list="[
        { label: '08:00', value: 101, data: { status: 'ok', temp: 24 } },
        { label: '09:00', value: 102, data: { status: 'warning', temp: 26 } },
        { label: '10:00', value: 103, data: { status: 'ok', temp: 25 } },
        { label: '11:00', value: 104, data: { status: 'error', temp: 30 } },
        { label: '12:00', value: 105, data: { status: 'ok', temp: 24 } },
    ]" class="fixed " @change="" v-model="activeId"/> -->
</template>

<script setup lang="ts">
const activeId = ref(103);
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
import { ezStore } from '@/store'
import { getResultByCaseId } from '@/api/http/satellite-data/satellite.api'
import { message } from 'ant-design-vue'
import type { Case } from '@/api/http/satellite-data/satellite.type'


import { useI18n } from 'vue-i18n'
import TimeLine from '@/components/common/time-line.vue';
const { t } = useI18n()

const { caseList, currentPage, sectionHeader, pageSize, total, historyClassTabs, activeTab, handleSelectTab,
    selectedRegion, selectedTimeIndex, timeOptionList, selectedResolution, resolutionList, EMPTY_RESOLUTION, getCaseList,
    isExpand, reset, previewList, previewIndex, showResult, unPreview, currentCase, bandForm, isModalVisible, currentBandList, showModal } = useViewHistoryModule()
const { pendingTaskList, startPolling, stopPolling } = useTaskPollModule()
const taskStore = useTaskStore()
// 检测由noCloud跳转至history面板时（setCurrentPanel），前端是否还在初始化任务（calNoCloud处理请求参数）
const isPending = computed(() => taskStore._isInitialTaskPending)

// 发布和下载弹窗的响应式变量
const publishModalVisible = ref(false)
const downloadModalVisible = ref(false)
const tifDownloadModalVisible = ref(false)  // tif文件下载弹窗
const currentItem = ref<Case.Case | null>(null)
const tifFileUrls = ref<string[]>([])  // tif文件URL列表

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
const handlePublish = (item: Case.Case) => {
    currentItem.value = item
    publishModalVisible.value = true
}

// 发布确认
const handlePublishConfirm = async () => {
    try {
        if (!currentItem.value) {
            message.error('请选择要发布的任务')
            return
        }
        // 获取配置
        const conf = ezStore.get('conf')
        const titilerEndpoint = conf['titiler']
        const minioEndpoint = conf['minioIpAndPort']

        // 获取任务结果
        const result = await getResultByCaseId(currentItem.value.caseId)
        const resultData = result.data

        if (!resultData) {
            message.error('无法获取发布数据，请稍后重试')
            return
        }

        if (!resultData.bucket) {
            message.error('无法获取发布数据的bucket，请稍后重试')
            return
        }
        if (!resultData.object_path) {
            message.error('无法获取发布数据的bucket，请稍后重试')
            return
        }

        // 生成发布URL
        const mosaicJsonPath = `${resultData.bucket}/${resultData.object_path}`

        // 从titilerEndPoint中提取IP和端口
        let titilerBaseUrl = titilerEndpoint

        const publishUrl = `${titilerBaseUrl}/mosaic/mosaictile/{z}/{x}/{y}.png?mosaic_url=${minioEndpoint}/${mosaicJsonPath}`

        message.success('发布成功')
        message.info(`发布地址：${publishUrl}`, 10)

        if (navigator.clipboard) {
            navigator.clipboard.writeText(publishUrl).then(() => {
                message.success('发布URL已复制到剪贴板')
            }).catch(() => {
                message.error('复制失败，请手动复制')
            })
        }

        publishModalVisible.value = false

        // 重置表单
        Object.assign(publishForm, {
            name: '',
            description: '',
            scope: 'public',
            expiryDate: null
        })

    } catch (error) {
        console.error('发布失败', error)
        message.error('发布失败，请稍后重试')
    }
}

const handlePublishCancel = () => {
    publishModalVisible.value = false
}

async function fetchAllInParallel(urls: string[]): Promise<any[]> {
    // 1. 为每个 URL 创建一个包含 fetch 和 JSON 解析的 Promise
    const fetchPromises = urls.map(url =>
        fetch(`${url}?_t=${Date.now()}`).then(res => {
            if (!res.ok) {
                message.warning(`HTTP Error: ${res.status}`)
                throw new Error(`HTTP Error: ${res.status}`)
            };
            return res.json();
        })
    );

    // 2. 使用 Promise.all() 并行执行所有请求
    const results = await Promise.all(fetchPromises);

    return results; // 结果数组的顺序与 urls 列表的顺序一致
}

// 下载处理函数
const handleDownload = async (item: any) => {
    try {
        currentItem.value = item

        // 获取配置
        const conf = ezStore.get('conf')
        const minioEndpoint = conf['minioProxy']

        // 获取case结果数据
        const result = await getResultByCaseId(item.caseId)
        const resultData = result.data

        if (!resultData) {
            message.error('无法获取下载数据，请稍后重试')
            return
        }

        // 生成MosaicJSON的URL
        // let mosaicJSON = ''
        let mosaicJSONs: string[] = []

        switch (item?.type) {
            case "noCloud": {
                if (!resultData.bucket) {
                    message.error('无法获取下载数据的bucket，请稍后重试')
                    return
                }

                if (!resultData.object_path) {
                    message.error('无法获取下载数据的object_path，请稍后重试')
                    return
                }
                let mosaicJSON = `${minioEndpoint}/${resultData.bucket}/${resultData.object_path}`
                mosaicJSONs.push(mosaicJSON)
                break;
            }
            // 12.22进一步修正，modelServer根据需求把不同周期对应的tif都合并到一个tif中，等于还是一个格网对应一个tif，所以跟无云一版图下载无异
            /* case "eoCube": {
                if (!resultData?.dimensions) {
                    message.error('时序立方体数据为空')
                    return
                }
                for (let key of resultData.dimensions) {
                    let mosaicJSON = `${minioEndpoint}/${resultData.data[key].bucket}/${resultData.data[key].object_path}`
                    mosaicJSONs.push(mosaicJSON)
                }
                break;
            } */
            default: {
                if (!resultData.bucket) {
                    message.error('无法获取下载数据的bucket，请稍后重试')
                    return
                }

                if (!resultData.object_path) {
                    message.error('无法获取下载数据的object_path，请稍后重试')
                    return
                }
                let mosaicJSON = `${minioEndpoint}/${resultData.bucket}/${resultData.object_path}`
                mosaicJSONs.push(mosaicJSON)
                break;
            }
        }

        const mosaicJSONDataList = await fetchAllInParallel(mosaicJSONs)
        console.log(mosaicJSONDataList)

        const allTifUrls: string[] = []

        // 提取所有tif文件的URL
        mosaicJSONDataList.forEach(mosaicJSONData => {
            const tiles = mosaicJSONData.tiles
            Object.keys(tiles).forEach(key => {
                const urlArray = tiles[key]
                if (Array.isArray(urlArray)) {
                    allTifUrls.push(...urlArray)
                }
            })
        })

        // 存储解析出的URL数组
        tifFileUrls.value = allTifUrls

        // 直接显示tif文件下载弹窗
        tifDownloadModalVisible.value = true

    } catch (error) {
        console.error('下载失败', error)
        message.error('下载失败，请稍后重试')
    }
}

// 处理tif文件下载弹窗关闭
const handleTifDownloadCancel = () => {
    tifDownloadModalVisible.value = false
    tifFileUrls.value = []
}

// 处理单个文件下载
const handleFileDownload = (url: string) => {
    const link = document.createElement('a')
    link.href = url
    link.download = url.split('/').pop() || 'nocloud.tif'
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
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
.space-y-4>*+* {
    margin-top: 1rem;
}

.a-button {
    display: inline-flex;
    align-items: center;
}
</style>