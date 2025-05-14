<template>
    <div class="custom-panel px-2">
        <dv-border-box12 class="!h-[calc(100vh-56px-48px-32px-8px)]">
            <div class="main-container">
                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <CloudIcon :size="18" />
                        </div>
                        <h2 class="section-title">无云一版图计算</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">

                            <div class="config-item">
                                <div class="config-label relative">
                                    <BoltIcon :size="16" class="config-icon" />
                                    <span>国产光学影像</span>
                                </div>
                                <div class="config-control flex-col !items-start">
                                    <div class="flex flex-col gap-2">
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="additionalData[0]" class="w-4 h-4 rounded">
                                            采用国外数据源（如哨兵等）作为补充
                                        </label>
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="additionalData[1]" class="w-4 h-4 rounded">
                                            采用雷达数据作为补充
                                        </label>
                                    </div>
                                    <div class="result-info-container">
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <MapIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">研究区行政区划编码</div>
                                                <div class="result-info-value">{{ props.regionConfig.regionCode }}
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <ImageIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">影像覆盖率</div>
                                                <div class="result-info-value">{{ props.regionConfig.space }}km </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CalendarIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">涵盖时间范围</div>
                                                <div class="result-info-value date-range">
                                                    <div class="date-item">{{
                                                        formatTime(props.regionConfig.dataRange[0], 'day')
                                                    }}~
                                                        {{ formatTime(props.regionConfig.dataRange[1], 'day')
                                                        }}</div>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">云量范围</div>
                                                <div class="result-info-value">0 ~ {{ props.regionConfig.cloud }}%</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                            </div>
                            <div class="config-item">
                                <div class="config-label relative">
                                    <CalendarIcon :size="16" class="config-icon" />
                                    <span>融合国外光学影像</span>

                                </div>
                                <div class="config-control">
                                    123
                                </div>
                            </div>
                            <div class="config-item">
                                <div class="config-label relative">
                                    <CalendarIcon :size="16" class="config-icon" />
                                    <span>融合SAR影像</span>

                                </div>
                                <div class="config-control">
                                    123
                                </div>
                            </div>

                            <div class="w-full flex flex-col">
                                <button @click="calNoClouds"
                                    class="bg-[#0d1526] w-full  cursor-pointer text-white border border-[#2c3e50] rounded-lg px-4 py-2 hover:bg-[#1a2b4c] hover:border-[#2bb2ff] transition-all duration-200 active:scale-95">
                                    一版图重构
                                </button>
                                <div v-if="showProgress"
                                    class="w-full  bg-[#1e293b] rounded-lg overflow-hidden border border-[#2c3e50]">
                                    <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                        :style="{ width: `${progress}%` }"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>


                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <CloudIcon :size="18" />
                        </div>
                        <h2 class="section-title">计算结果</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div v-for="(image, index) in calImage" class="config-item">
                                第{{ index + 1 }}次计算结果为：
                                无云一版图计算结果为：xxx
                                统计数据-统计数据-统计数据-统计数据
                            </div>
                        </div>
                    </div>
                </section>

            </div>
        </dv-border-box12>
    </div>

</template>

<script setup lang="ts">
import { ref, type PropType, type Ref } from 'vue'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import { type interactiveExplore } from '@/components/dataCenter/type'
import { formatTime } from '@/util/common'
import {
    DatabaseIcon,
    MapPinIcon,
    CalendarIcon,
    UploadCloudIcon,
    RefreshCwIcon,
    HexagonIcon,
    CloudIcon,
    ApertureIcon,
    ClockIcon,
    ImageIcon,
    LayersIcon,
    DownloadIcon,
    FilePlus2Icon,
    BoltIcon,
    BanIcon,
    MapIcon,
} from 'lucide-vue-next'

const props = defineProps({
    regionConfig: {
        type: Object as PropType<interactiveExplore>,
        required: true
    }
})

const progress = ref(0)
const showProgress = ref(false)
const showCalResult = ref(false)
const calState = ref('start')
const additionalData = ref([false, false])
const calImage: Ref<any[]> = ref([])
let progressTimer: ReturnType<typeof setInterval> | null = null

// 控制进度条
const progressControl = () => {
    if (calState.value === 'pending') return
    progress.value = 0
    calState.value = 'pending'
    progressTimer = setInterval(() => {
        if (calState.value === 'success' || calState.value === 'error') {
            progress.value = 100
            showCalResult.value = true
            clearInterval(progressTimer!)
            progressTimer = null
        } else if (progress.value < 95) {
            progress.value += 1
        } else {
            progress.value = 95
        }
    }, 100)
}
// 开始计算
const calNoClouds = () => {
    // 1、启动进度条
    showProgress.value = true
    progressControl()

    // 2、轮询运行状态，直到运行完成
    setTimeout(() => {
        calState.value = 'success'
    }, 6000)

    // 3、渲染运行结果
    calImage.value.push({
        name: 111
    })
}
</script>

<style scoped src="./tabStyle.css">
:deep(.border-box-content) {
    padding: 1.5rem;
}
</style>