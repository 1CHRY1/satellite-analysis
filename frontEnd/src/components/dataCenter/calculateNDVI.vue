<template>
    <div class="custom-panel px-2">
        <dv-border-box12 class="!h-[calc(100vh-56px-48px-32px-8px)]">
            <div class="main-container">
                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <ChartColumn :size="18" />
                        </div>
                        <h2 class="section-title">动态计算领域：</h2>
                        <select v-model="selectedTask"
                            class="bg-[#0d1526] text-white w-40 border border-[#2c3e50] rounded-lg px-4 py-2 pr-8 appearance-none transition-all duration-200 hover:border-[#206d93] focus:outline-none focus:border-[#3b82f6]">
                            <option v-for="option in optionalTasks" :key="option" :value="option"
                                class="bg-[#0d1526] text-[#e0f2fe]">
                                {{ option }}
                            </option>
                        </select>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div class="config-item">
                                <div class="config-label relative">
                                    <MapIcon :size="16" class="config-icon" />
                                    <span>地图选点</span>
                                    <el-button link @click="startDraw" class="absolute right-1 !text-sky-300">
                                        开始选点
                                    </el-button>
                                </div>
                                <div class="config-control flex-col  gap-2">
                                    请确定您要研究NDVI时序变化的区域：

                                    <div class="result-info-container">
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <Earth :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">纬度</div>
                                                <div class="result-info-value">{{ Math.round(pickedPoint[0] * 1000000) /
                                                    1000000 }}
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <Earth :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">经度</div>
                                                <div class="result-info-value">{{ Math.round(pickedPoint[1] * 1000000) /
                                                    1000000 }} </div>
                                            </div>
                                        </div>

                                    </div>
                                    <!-- <div>
                                        lat:
                                        <input v-model="position[0]" class="input-box w-20 border-1 rounded-"
                                            step="0.0001" />
                                        lng:
                                        <input v-model="position[1]" class="input-box w-20 border-1 rounded-xs"
                                            step="0.0001" />
                                    </div> -->

                                </div>

                            </div>
                            <div class="result-info-container">
                                <div class="result-info-item">
                                    <div class="result-info-icon">
                                        <MapIcon :size="16" />
                                    </div>
                                    <div class="result-info-content">
                                        <div class="result-info-label">研究区行政区划编码</div>
                                        <div class="result-info-value">{{ props.regionConfig.regionCode }} </div>
                                    </div>
                                </div>
                                <div class="result-info-item">
                                    <div class="result-info-icon">
                                        <ImageIcon :size="16" />
                                    </div>
                                    <div class="result-info-content">
                                        <div class="result-info-label">数据源</div>
                                        <div class="result-info-value">{{ 111 }} </div>
                                    </div>
                                </div>
                                <div class="result-info-item">
                                    <div class="result-info-icon">
                                        <CalendarIcon :size="16" />
                                    </div>
                                    <div class="result-info-content">
                                        <div class="result-info-label">涵盖时间范围</div>
                                        <div class="result-info-value date-range">
                                            <div class="date-item">{{ formatTime(props.regionConfig.dataRange[0], 'day')
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
                            <button @click="calNDVI"
                                class="bg-[#0d1526] w-full  cursor-pointer text-white border border-[#2c3e50] rounded-lg px-4 py-2 hover:bg-[#1a2b4c] hover:border-[#2bb2ff] transition-all duration-200 active:scale-95">
                                开始计算
                            </button>
                            <div v-if="showProgress"
                                class="w-full  bg-[#1e293b] rounded-lg overflow-hidden border border-[#2c3e50]">
                                <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                    :style="{ width: `${progress}%` }"></div>
                            </div>

                        </div>
                    </div>
                </section>
                <section class="panel-section">
                    <div class="section-header">
                        <div class="section-icon">
                            <ChartColumn :size="18" />
                        </div>
                        <h2 class="section-title">计算结果</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div v-for="(image, index) in drawData" class="config-item">
                                第{{ index + 1 }}次计算结果为：
                                NDVI计算结果为：xxx
                                统计数据-统计数据-统计数据-统计数据
                                二维折线图/三维折线图/复式折线图
                            </div>
                        </div>
                    </div>
                </section>
            </div>

        </dv-border-box12>
    </div>
</template>
<script setup lang="ts">
import { ref, type PropType, computed, type Ref } from 'vue'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import { type interactiveExplore } from '@/components/dataCenter/type'
import { formatTime } from '@/util/common'

import * as MapOperation from '@/util/map/operation'
import { useGridStore, ezStore } from '@/store'
import {
    ChartColumn,
    Earth,
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
import { ElMessage } from 'element-plus'

const props = defineProps({
    regionConfig: {
        type: Object as PropType<interactiveExplore>,
        required: true
    }
})

const gridStore = useGridStore()
const pickedPoint = computed(() => gridStore._point)
const showProgress = ref(false)
const progress = ref(0)
const showCalResult = ref(false)
const calState = ref('start')
const selectedTask = ref('NDVI时序计算')
const optionalTasks = ref(['NDVI时序计算', '滑坡概率计算', '洪水频发风险区域计算'])

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

const startDraw = () => {
    MapOperation.draw_pointMode()
}

const calNDVI = () => {
    if (pickedPoint.value.length === 0) {
        ElMessage.warning('请先选择您要计算的区域')
        return
    }
    // 1、启动进度条
    showProgress.value = true
    progressControl()

    // 2、轮询运行状态，直到运行完成
    setTimeout(() => {
        calState.value = 'success'
    }, 4000)

    // 3、渲染运行结果
    drawData.value.push(11)
}

const drawData: Ref<number[]> = ref([])
</script>

<style scoped src="./tabStyle.css"></style>