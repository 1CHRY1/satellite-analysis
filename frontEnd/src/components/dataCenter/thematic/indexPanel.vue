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
                    <!-- <div class="config-label relative flex items-center gap-2">
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
                                        <option v-for="image in targetImage" :key="image.sceneName"
                                            :value="image.sceneId" :title="image.sceneName" class="truncate">
                                            {{ image.sceneName }}
                                        </option>
                                    </select>
                                </div>
                            </div>
                        </div> -->

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
                        </div>
                    </div> -->

                    <div class="config-label flex items-center gap-2 mt-4">
                        <LayersIcon :size="16" />
                        <span>预制指数</span>
                    </div>

                    <div class="config-control">
                        <!-- 指数类型选择 -->
                        <div class="section-content space-y-3 mb-6 ml-8">
                            <button
                            v-for="(item, index) in presetIndex"
                            :key="index"
                            @click="selectIndex(item)"
                            class="config-button block transition-colors duration-200 hover:bg-[#1a2b4c] hover:border-[#2bb2ff] hover:text-white"
                            >
                            {{ item.name }}
                            </button>
                        </div>
                        <div class="color-palette-selector mb-6">
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
                        <div class="flex items-center gap-2 mt-2 mb-6">
                                <label class="text-white">配色方案:</label>
                                <select
                                    v-model="selectedColorMap"
                                    class="w-full rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white appearance-none hover:border-[#2bb2ff] hover:bg-[#1a2b4c] focus:outline-none focus:border-[#3b82f6]"
                                >
                                    <option v-for="(color, name) in colorMaps" :key="name" :value="name">
                                        {{ name }}
                                    </option>
                                </select>
                            </div>
                    </div>
                </div>
                <button @click="handleCloudTiles"
                            class="cursor-pointer w-full rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95">
                            开始运行
                </button>
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
import http from '@/api/http/clientHttp'


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
import { getNoCloudUrl4MosaicJson } from '@/api/http/satellite-data/visualize.api';
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
const selectedSceneId = ref('')
type ThematicConfig = {
    allImages: any,
    regionId: number,
    startTime: string,
    endTime: string,
    dataset: any
}
const props = defineProps<{ thematicConfig: ThematicConfig }>()

interface analysisTileParams{

    mosaic_url: string,
    expression:string,
    pixel_method:string,
    color: string
}

const isExpand = ref<boolean[]>([])

const selectedIndex = ref();
const presetIndex = ref([
    {name:'超绿指数', expression: '2*b1-b2+b3'},
    {name:'自定义（即将上线）', expression:''}
])

//色带选择
const selectedColorMap = ref('rdylgn')
const colorMaps = {
    '红-黄-绿':'rdylgn',
    '自定义（即将提供）': 'self-define'
}

const selectIndex = (item) => {
    selectedIndex.value = item.name
    if (item.name !== '自定义') {
        ElMessage.error('暂不支持')
    }
}

 //上传参数获取图片

// 获取对应的模态框类型
// const getModalType = (name) => {
//   const modalMap = {
//     '二波段指数': 'dualBand',
//     '三波段指数': 'tripleBand',
//     '典型光谱指数': 'typical',
//   }
//   return modalMap[name] || 'custom'
// }

const handleCloudTiles = async () => {

    try {
        const mosaicUrl = getNoCloudUrl4MosaicJson({mosaicJsonPath : props.thematicConfig.dataset.result.bucket + '/' + props.thematicConfig.dataset.result.object_path})
        const encodedExpr = encodeURIComponent('2*b1-b2+b3')
        const tileUrl = `http://localhost:8000/analysis/{z}/{x}/{y}.png?mosaic_url=${mosaicUrl}&expression=${encodedExpr}&pixel_method=first&color=rdylgn`
        console.log('瓦片URL模板:', tileUrl)

        // 清除旧的无云图层
        MapOperation.map_destroyNoCloudLayer()

        // 添加新的瓦片图层
        MapOperation.map_addNoCloudLayer(tileUrl)

        console.log('无云一版图瓦片图层已添加到地图')

    } catch (error) {
        console.error('创建无云一版图瓦片失败:', error)
    }
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
