<template>
    <!-- Section: 输入模块 -->
    <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <MapIcon :size="18" />
            </div>
            <h2 class="section-title">DEM分析</h2>
        </div>
        <div class="section-content">
            <div class="config-container">
                <div class="config-item">
                    <div class="config-label relative">
                        <MapIcon :size="16" class="config-icon" />
                        <span>DEM影像</span>
                    </div>
                    <div class="config-control justify-center">
                        <div class="w-full space-y-2">
                            <div v-for="(image, index) in allDemImages" :key="index"
                                class="flex flex-col border border-[#247699] bg-[#0d1526] text-white px-4 py-2 rounded-lg transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c]">
                                <div class="font-semibold text-base">{{ image.sceneName }}</div>
                                <div class="text-sm text-gray-400">{{ formatTime(image.sceneTime, 'minutes') }}</div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="config-item">
                    <div class="config-label relative">
                        <MapIcon :size="16" class="config-icon" />
                        <span>空间选择</span>
                    </div>
                    <div class="config-control justify-center">
                        <div class="flex gap-10">
                            <!-- 地图选点块 -->
                            <div @click="toggleMode('point')"
                                class="w-24 h-24 flex flex-col items-center justify-center rounded-lg border cursor-pointer transition-all duration-200 text-white"
                                :class="[
                                    activeMode === 'false'
                                        ? 'border-[#2bb2ff] bg-[#1a2b4c]'
                                        : 'border-[#247699] bg-[#0d1526]',
                                    'hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95'
                                ]">
                                <MapPinIcon class="mb-2" />
                                地图选点
                            </div>

                            <!-- 划线采点块 -->
                            <div @click="toggleMode('line')"
                                class="w-24 h-24  flex flex-col items-center justify-center rounded-lg border cursor-pointer transition-all duration-200 text-white"
                                :class="[
                                    activeMode === 'false'
                                        ? 'border-[#2bb2ff] bg-[#1a2b4c]'
                                        : 'border-[#247699] bg-[#0d1526]',
                                    'hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95'
                                ]">
                                <LayersIcon class="mb-2" />
                                划线采点
                            </div>
                        </div>

                    </div>
                </div>
                <button @click="analysisDem"
                    class="cursor-pointer rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95">
                    开始分析
                </button>
            </div>
        </div>
    </section>

    <!-- Section: 结果展示 -->
    <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <ChartColumn :size="18" />
            </div>
            <h2 class="section-title">计算结果</h2>
        </div>
        <div class="section-content">
            <div class="config-container">
                <div v-for="(item, index) in analysisData" :key="index" class="config-item">
                    <div class="config-label relative">
                        <MapIcon :size="16" class="config-icon" />
                        <span>第{{ index + 1 }}次分析结果：</span>
                    </div>
                    <div class="config-control justify-center">
                        <div v-if="item.type === 'singleValue'">
                            经纬度（{{ item.point[1] }},{{ item.point[0] }}）的DEM值为:{{ item.value }}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch, watchEffect, type Ref } from 'vue';
import { getRasterScenesDes, getRasterPoints, getBoundaryBySceneId, getCaseStatus, getCaseResult } from '@/api/http/satellite-data';
import * as MapOperation from '@/util/map/operation'
import { useGridStore, ezStore } from '@/store'
import { formatTime } from '@/util/common';

import {
    ChartColumn,
    Earth,
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
import { ElMessage } from 'element-plus';

type ThematicConfig = {
    allImages: any,
    regionId: number,
    startTime: string,
    endTime: string
}

const props = defineProps<{ thematicConfig: ThematicConfig }>()
const allDemImages = ref<any>([])
const initDemPanel = async () => {
    let thematicConfig = props.thematicConfig
    if (!thematicConfig.regionId) return
    let rasterParam = {
        startTime: thematicConfig.startTime,
        endTime: thematicConfig.endTime,
        regionId: thematicConfig.regionId,
        dataType: 'dem'
    }
    allDemImages.value = await getRasterScenesDes(rasterParam)
    console.log(allDemImages.value, 57);
}


const activeMode = ref<'point' | 'line' | 'false' | null>(null)
const gridStore = useGridStore()
const pickedPoint = computed(() => {
    return [
        Math.round(gridStore._point[0] * 1000000) / 1000000,
        Math.round(gridStore._point[1] * 1000000) / 1000000
    ];
})

const toggleMode = (mode: 'point' | 'line' | 'false') => {
    // activeMode.value = activeMode.value === mode ? null : mode
    activeMode.value = mode
    if (mode === 'point') {
        startDrawPoint()
    }
}
const startDrawPoint = () => {
    MapOperation.draw_pointMode()
}

const calTask: Ref<any> = ref({
    calState: 'start',
    taskId: ''
})
const analysisData = ref<any>([])
const analysisDem = async () => {
    let boundary = await getBoundaryBySceneId(allDemImages.value[0].sceneId)
    MapOperation.map_addPolygonLayer({
        geoJson: boundary,
        id: 'UniqueLayer',
        lineColor: '#8fffff',
        fillColor: '#a4ffff',
        fillOpacity: 0.2,
    })
    console.log(pickedPoint.value, 447);

    if (activeMode.value === 'point') {
        let pointParam = {
            point: [pickedPoint.value[1], pickedPoint.value[0]],
            sceneIds: allDemImages.value.map((image) => image.sceneId)
        }
        let res = await getRasterPoints(pointParam)
        if (res?.message != 'success') {
            ElMessage.warning('计算服务出错')
        }
        calTask.value.taskId = res.data
        const pollStatus = async (taskId: string) => {
            const interval = 1000 // 每秒轮询一次
            return new Promise<void>((resolve, reject) => {
                const timer = setInterval(async () => {
                    try {
                        const res = await getCaseStatus(taskId)
                        console.log('轮询结果:', res)

                        if (res?.data === 'COMPLETE') {
                            clearInterval(timer)
                            resolve()
                        } else if (res?.data === 'ERROR') {
                            console.log(res, res.data, 15616);

                            clearInterval(timer)
                            reject(new Error('任务失败'))
                        }
                    } catch (err) {
                        clearInterval(timer)
                        reject(err)
                    }
                }, interval)
            })
        }
        try {
            await pollStatus(calTask.value.taskId)
            // ✅ 成功后设置状态
            calTask.value.calState = 'success'
            let res = await getCaseResult(calTask.value.taskId)
            console.log(res, '结果');
            let demValue = res.data.value

            // let xData = spectrum.map(data => data.band + '波段')
            // let yData = spectrum.map(data => data.value)

            analysisData.value.push({
                type: 'singleValue',
                analysis: "单点DEM求值",
                value: demValue,
                point: [...pickedPoint.value]
            })
            ElMessage.success('DEM计算完成')
        } catch (error) {
            calTask.value.calState = 'failed'
            ElMessage.error('DEM计算失败，请重试')
            console.error(error);
        }
    }
}

watch(() => props.thematicConfig.regionId, initDemPanel)
onMounted(async () => {
    await initDemPanel()
})
</script>

<style scoped src="../tabStyle.css"></style>
