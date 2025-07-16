<template>
    <!-- Section: 输入模块 -->
    <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <MapIcon :size="18" />
            </div>
            <h2 class="section-title">指数分析</h2>
        </div>
        <div class="section-content">
            <div class="config-container">

                <div class="config-item">
                    <div class="config-label relative flex items-center gap-2">
                        <MapIcon :size="16" class="config-icon" />
                        <span>空间选择</span>
                    </div>
                    <div class="config-control flex-col  gap-2 w-full">
                        <div class="config-item bg-[#0d1526]/50 p-3 rounded-lg">
                            <div class="config-label relative">
                                <MapIcon :size="16" class="config-icon" />
                                <span>{{ t('datapage.optional_thematic.spectrum.wait') }}</span>
                            </div>
                            <div class="config-control justify-center">
                                <div class="flex items-center gap-2 mt-2 w-full">
                                    <label class="text-white">{{ t('datapage.optional_thematic.spectrum.select') }}</label>
                                    <select v-model="selectedSceneId" @change="showImageBBox"
                                        class="bg-[#0d1526] text-[#38bdf8] border border-[#2c3e50] rounded-lg px-3 py-1 appearance-none hover:border-[#2bb2ff] focus:outline-none focus:border-[#3b82f6] max-w-[calc(100%-90px)] truncate">
                                        <option disabled selected value="">{{ t('datapage.optional_thematic.spectrum.op_select') }}</option>
                                        <!-- <option v-for="image in targetImage" :key="image.sceneName"
                                            :value="image.sceneId" :title="image.sceneName" class="truncate">
                                            {{ image.sceneName }}
                                        </option> -->
                                    </select>
                                </div>
                            </div>
                        </div>
                        
                        <!-- 请确定您要研究的区域： -->
                        <!-- <div class="flex items-center gap-2 mt-2 w-full">
                            <label class="text-white">影像选择：</label>
                            <select v-model="selectedSceneId" @change="showImageBBox"
                                class="bg-[#0d1526] text-[#38bdf8] border border-[#2c3e50] rounded-lg px-3 py-1 appearance-none hover:border-[#2bb2ff] focus:outline-none focus:border-[#3b82f6] max-w-[calc(100%-90px)] truncate">
                                <option disabled selected value="">请选择影像</option>
                                <option v-for="image in props.regionConfig.images" :key="image.sceneName"
                                    :value="image.sceneId" :title="image.sceneName" class="truncate">
                                    {{ image.sceneName }}
                                </option>
                            </select>
                        </div> -->
                        <!-- <div class="result-info-container">
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <Earth :size="16" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">纬度</div>
                                    <div class="result-info-value">{{ 1 }}
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <Earth :size="16" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">经度</div>
                                    <div class="result-info-value">{{ 1 }} </div>
                                </div>
                            </div>
                        </div> -->
                    </div>

                    <div class="config-label flex items-center gap-2 mt-4">
                        <LayersIcon :size="16" />
                        <span>预制指数</span>
                    </div>
                    <div class="config-control">
                        <!-- 指数类型选择 -->
                        <div class="section-content space-y-3 ">
                            <button 
                            v-for="(item, index) in presetIndex" 
                            :key="index"
                            @click="showModal(getModalType(item.name))"
                            class="config-button transition-colors duration-200 hover:bg-[#1a2b4c] hover:border-[#2bb2ff] hover:text-white"
                            >
                            {{ item.name }}
                            </button>
                        </div>
                        <div class="color-palette-selector">
                            <span>选择指数</span>
                            <select 
                                v-model="selectedIndex"
                                class="w-full rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white appearance-none hover:border-[#2bb2ff] hover:bg-[#1a2b4c] focus:outline-none focus:border-[#3b82f6]"
                            >
                                <option disabled value="">选择指数</option>
                                <option 
                                v-for="(palette, index) in presetIndex" 
                                :key="index"
                                :value="palette.name"
                                class="bg-[#0d1526] text-white"
                                >
                                {{ palette.name }}
                                </option>
                                
                            </select>
                        </div>
                    </div>
                </div>
                <button @click=""
                            class="cursor-pointer w-full rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95">
                            {{$t('datapage.optional_thematic.NDVI.button')}}
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
            <h2 class="section-title">{{$t('datapage.optional_thematic.NDVI.result')}}</h2>
        </div>
        <div class="section-content">
            <div v-if="analysisData.length === 0" class="flex justify-center my-6">
                <SquareDashedMousePointer class="mr-2" />{{$t('datapage.optional_thematic.NDVI.noresult')}}
            </div>

            <div class="config-item" v-for="(item, index) in analysisData" :key="index">
                <div class="config-label relative">
                    <ListFilter :size="22" class="config-icon" />
                    <span>第{{ index + 1 }}次计算：{{ item.analysis }}</span>
                    <div class="absolute right-0 cursor-pointer">
                        <ChevronDown v-if="isExpand[index]" :size="22" @click="isExpand[index] = false" />
                        <ChevronUp v-else @click="isExpand[index] = true" :size="22" />
                    </div>
                </div>

                <div v-show="isExpand[index]" > 
                    <!-- <div>第{{ index + 1 }}次计算：{{ item.analysis }}</div> -->
                    <!-- <div>NDVI计算结果为：xxx</div> -->
                    <!-- <div>统计数据-统计数据-统计数据-统计数据</div> -->
                    <div>经纬度：（{{ item.point[0] }},{{ item.point[1] }}）</div>

                    <!-- <div class="chart-wrapper flex flex-col items-end">
                        <div class="chart" :ref="el => setChartRef(el, index)" :id="`chart-${index}`"
                            style="width: 100%; height: 400px;"></div>
                        <button class="!text-[#38bdf8] cursor-pointer" @click="fullscreenChart(index)">{{$t('datapage.optional_thematic.NDVI.fullview')}}</button>
                    </div> -->
                </div>
            </div>
        </div>
    </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch, type ComponentPublicInstance, type ComputedRef, type Ref } from 'vue'
import * as MapOperation from '@/util/map/operation'
import { getBoundaryBySceneId, getCaseResult, getCaseStatus, getNdviPoint, getRasterScenesDes } from '@/api/http/satellite-data'
import { ElMessage } from 'element-plus'
import bus from '@/store/bus'
import mapboxgl from 'mapbox-gl'
import * as echarts from 'echarts'



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
    SquareDashedMousePointer,
    Bus,
    ChevronDown,
    ChevronUp
} from 'lucide-vue-next'
import { useGridStore } from '@/store'
import { mapManager } from '@/util/map/mapManager'

import { useI18n } from 'vue-i18n'
const { t } = useI18n()
const selectedSceneId = ref('')
type ThematicConfig = {
    allImages: any,
    regionId: number,
    startTime: string,
    endTime: string
}
const props = defineProps<{ thematicConfig: ThematicConfig }>()

const isExpand = ref<boolean[]>([])

const selectedIndex = ref();
const presetIndex = ref([
    {name:'二波段指数'},
    {name:'三波段指数'},
    {name:'典型光谱指数'},
    {name:'自定义'},
])
// 获取对应的图标组件


// 获取对应的模态框类型
const getModalType = (name) => {
  const modalMap = {
    '二波段指数': 'dualBand',
    '三波段指数': 'tripleBand',
    '典型光谱指数': 'typical'
  }
  return modalMap[name] || 'custom'
}

// 显示模态框
const showModal = (type) => {
  selectedIndex.value = type
  // 这里添加你的模态框显示逻辑
}
const analysisData = ref<Array<{
    analysis: string
    point: [number, number]
    data: number[]
}>>([])

const showImageBBox = async () => {
    let getDescriptionRes = await getBoundaryBySceneId(selectedSceneId.value)
    const FeatureCollectionBoundary: GeoJSON.FeatureCollection = {
        type: "FeatureCollection",
        features: [getDescriptionRes]
    }
    try {
        MapOperation.map_addPolygonLayer({
            geoJson: FeatureCollectionBoundary,
            id: 'UniqueSceneLayer',
            lineColor: '#8fffff',
            fillColor: '#a4ffff',
            fillOpacity: 0.2,
        })
        ElMessage.success(t('datapage.optional_thematic.spectrum.message.success_poin'))
    } catch (e) {
        console.error("有错误找后端", e)
        ElMessage.error(t('datapage.optional_thematic.spectrum.message.info_fail'))
    }
}

// const selectedImage = props.thematicConfig.allImages.find(image => image.sceneId = selectedSceneId.value)

</script>

<style>
.color-palette-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 12px;
}
.palette-item {
  cursor: pointer;
  border: 1px solid #2c3e50;
  border-radius: 4px;
  overflow: hidden;
}
.palette-preview {
  height: 24px;
  width: 100%;
}
</style>