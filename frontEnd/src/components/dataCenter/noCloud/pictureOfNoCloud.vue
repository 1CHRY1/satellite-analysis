<template>
    <div class="custom-panel px-2">
        <dv-border-box12 class="!h-[calc(100vh-56px-48px-32px-8px)]">
            <div class="main-container">
                <section class="panel-section" v-show="currentPanel === 'noCloud'" key="noCloud">
                    <div class="section-header">
                        <div class="section-icon">
                            <CloudIcon :size="18" />
                        </div>
                        <h2 class="section-title">{{t('datapage.nocloud.title')}}</h2>
                        <div class="section-icon absolute right-0 cursor-pointer">
                            <a-tooltip>
                                <template #title>{{t('datapage.history.his_recon')}}</template>
                                <History :size="18" @click="setCurrentPanel('history')"/>
                            </a-tooltip>
                        </div>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div class="config-item">
                                <div class="config-label relative">
                                    <BoltIcon :size="16" class="config-icon" />
                                    <span>{{t('datapage.nocloud.section_chinese.subtitle')}}</span>
                                </div>
                                <div class="config-control flex-col !items-start">
                                    <div class="flex w-full flex-col gap-2">
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="additionalData[0]" 
                                                class="h-4 w-4 rounded" 
                                                @click="add1mDemoticImage"/>
                                            {{t('datapage.nocloud.section_chinese.text_national_image')}}
                                        </label>
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="dataReconstruction[0]"
                                                @click="add2mDemoticImages" class="h-4 w-4 rounded" />
                                            {{t('datapage.nocloud.section_chinese.text_national2m')}}
                                        </label>
                                        <!-- 传感器选择 -->
                                        <label >
                                            {{ t('datapage.nocloud.choose') }}
                                        </label>
                                        <select class="max-h-[600px] w-[calc(100%-113px)] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none"
                                         v-model="selectnation"
                                         >
                                            <option disabled selected value="">{{ t('datapage.explore.section_interactive.choose') }}</option>
                                            <option v-for="platform in groupedLists.national" 
                                            :key="platform.platformName" 
                                            :value="platform"
                                            >
                                            {{ platform.platformName }}
                                            <span v-if="platform.tags?.includes('national')" style="color: red; margin-left: 5px;">
                                                (推荐)
                                            </span>
                                                </option>
                                        </select>

                                        <a-button class="custom-button mt-4! w-[calc(100%-50px)]!"
                                                @click="handleShowSensorImage(selectnation)">
                                                 {{ t('datapage.explore.section_interactive.button') }}
                                        </a-button>

                                        <div v-if="showProgress[0]"
                                            class="w-full overflow-hidden rounded-lg border border-[#2c3e50] bg-[#1e293b]">
                                            <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                                :style="{ width: `${progress[0]}%` }"></div>
                                        </div>
                                    </div>
                                    <div class="result-info-container">
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <ImageIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">{{t('datapage.nocloud.section_chinese.resolution')}}</div>
                                                <div class="result-info-value">
                                                    {{ props.regionConfig.space }}km
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CalendarIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">{{t('datapage.nocloud.section_chinese.timerange')}}</div>
                                                <div class="result-info-value date-range">
                                                    <div class="date-item">
                                                        {{
                                                            formatTime(
                                                                props.regionConfig.dataRange[0],
                                                                'day',
                                                            )
                                                        }}~
                                                        {{
                                                            formatTime(
                                                                props.regionConfig.dataRange[1],
                                                                'day',
                                                            )
                                                        }}
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">
                                                    {{t('datapage.nocloud.section_chinese.text_national_research')}}
                                                </div>
                                                <div class="result-info-value">
                                                    {{ demotic }}{{ t('datapage.explore.scene') }}
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">
                                                    {{t('datapage.nocloud.section_chinese.text_national_coverage')}}
                                                </div>
                                                <div class="result-info-value">
                                                    {{
                                                        coverageRate.demotic1m != 'NaN%'
                                                            ? coverageRate.demotic1m
                                                            : '待计算'
                                                    }}
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">
                                                    {{t('datapage.nocloud.section_chinese.text_research2m')}}
                                                </div>
                                                <div class="result-info-value">
                                                    {{ demotic2mImages.length }}{{ t('datapage.explore.scene') }}
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">
                                                    {{t('datapage.nocloud.section_chinese.text_coverage2m')}}
                                                </div>
                                                <div class="result-info-value">
                                                    {{
                                                        coverageRate.demotic2m != 'NaN%'
                                                            ? coverageRate.demotic2m
                                                            : '待计算'
                                                    }}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="config-item">
                                <div class="config-label relative">
                                    <CalendarIcon :size="16" class="config-icon" />
                                    <span>{{t('datapage.nocloud.section_international.subtitle')}}</span>
                                    <el-tooltip content="对于缺失数据的格网，采用国外光学影像进行填补，填补过程中基于AI算法进行超分辨率重建" placement="top"
                                        effect="dark">
                                        <CircleHelp :size="14" />
                                    </el-tooltip>
                                </div>
                                <div class="config-control flex-col !items-start">
                                    <div class="flex flex-col gap-2">
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="additionalData[1]" @click="addAbroadImages"
                                                :disabled="!dataReconstruction[0]" class="h-4 w-4 rounded" />
                                            {{t('datapage.nocloud.section_international.text_preview')}}
                                        </label>
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="dataReconstruction[1]"
                                                @click="controlProgress(1)" :disabled="!dataReconstruction[0]"
                                                class="h-4 w-4 rounded" />
                                            {{t('datapage.nocloud.section_international.text_overseaimage')}}
                                        </label>
                                         <!-- 传感器选择 -->
                                        <label >
                                            {{ t('datapage.nocloud.choose') }}
                                        </label>
                                        <select class="max-h-[600px] w-[calc(100%-113px)] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none"
                                         v-model="selectinternation"
                                         >
                                            <option disabled selected value="">{{ t('datapage.explore.section_interactive.choose') }}</option>
                                            <option v-for="platform in groupedLists.international" 
                                            :key="platform.platformName" 
                                            :value="platform"
                                            >
                                            {{ platform.platformName }}
                                            <span v-if="platform.tags?.includes('international')" style="color: red; margin-left: 5px;">
                                                (推荐)
                                            </span>
                                            </option>
                                        </select>
                                        <a-button class="custom-button mt-4! w-[calc(100%-50px)]!"
                                                @click="handleShowSensorImage(selectinternation)">
                                                 {{ t('datapage.explore.section_interactive.button') }}
                                        </a-button>

                                        <div v-if="showProgress[1]"
                                            class="w-full overflow-hidden rounded-lg border border-[#2c3e50] bg-[#1e293b]">
                                            <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                                :style="{ width: `${progress[1]}%` }"></div>
                                        </div>
                                    </div>
                                    <div class="result-info-container w-full">
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">{{t('datapage.nocloud.section_international.text_research')}}</div>
                                                <div class="result-info-value">
                                                    {{ internationalImages.length }}{{ t('datapage.explore.scene') }}
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">{{t('datapage.nocloud.section_international.text_coverage')}}</div>
                                                <div class="result-info-value">
                                                    {{
                                                        coverageRate.international != 'NaN%'
                                                            ? coverageRate.international
                                                            : '待计算'
                                                    }}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="config-item">
                                <div class="config-label relative">
                                    <CalendarIcon :size="16" class="config-icon" />
                                    <span>{{t('datapage.nocloud.section_SAR.subtitle')}}</span>
                                    <el-tooltip content="勾选将使用雷达数据进行色彩变换，与光学数据配准，并补充重构。" placement="top" effect="dark">
                                        <CircleHelp :size="14" />
                                    </el-tooltip>
                                </div>
                                <div class="config-control flex-col !items-start">
                                    <div class="flex flex-col gap-2">
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="additionalData[2]" @click="addRadarImages"
                                                :disabled="!additionalData[1] || !dataReconstruction[1]
                                                    " class="h-4 w-4 rounded" />
                                             {{t('datapage.nocloud.section_SAR.text_preview')}}
                                        </label>
                                        <label class="flex items-center gap-2">
                                            <input type="checkbox" v-model="dataReconstruction[2]"
                                                @click="controlProgress(2)" :disabled="!additionalData[1] || !dataReconstruction[1]
                                                    " class="h-4 w-4 rounded" />
                                            {{t('datapage.nocloud.section_SAR.text_SARtrans')}}
                                        </label>

                                         <!-- 传感器选择 -->
                                        <label >
                                            {{ t('datapage.nocloud.choose') }}
                                        </label>
                                       <select class="max-h-[600px] w-[calc(100%-113px)] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none"
                                         v-model="selectsar"
                                         >
                                            <option disabled selected value="">{{ t('datapage.explore.section_interactive.choose') }}</option>
                                            <option v-for="platform in groupedLists.sar" 
                                            :key="platform.platformName" 
                                            :value="platform"
                                            >
                                            {{ platform.platformName }}
                                            <span v-if="platform.tags?.includes('radar')" style="color: red; margin-left: 5px;">
                                                (推荐)
                                            </span>
                                            </option>
                                        </select>
                                        <a-button class="custom-button mt-4! w-[calc(100%-50px)]!"
                                                @click="handleShowSensorImage(selectsar)">
                                                 {{ t('datapage.explore.section_interactive.button') }}
                                        </a-button>

                                        <div v-if="showProgress[2]"
                                            class="w-full overflow-hidden rounded-lg border border-[#2c3e50] bg-[#1e293b]">
                                            <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                                :style="{ width: `${progress[2]}%` }"></div>
                                        </div>
                                    </div>
                                    <div class="result-info-container w-full">
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">{{t('datapage.nocloud.section_SAR.text_SARresearch')}}</div>
                                                <div class="result-info-value">
                                                    {{ radarImages.length }}{{ t('datapage.explore.scene') }}
                                                </div>
                                            </div>
                                        </div>
                                        <div class="result-info-item">
                                            <div class="result-info-icon">
                                                <CloudIcon :size="16" />
                                            </div>
                                            <div class="result-info-content">
                                                <div class="result-info-label">{{t('datapage.nocloud.section_SAR.text_coverage')}}</div>
                                                <div class="result-info-value">
                                                    {{
                                                        coverageRate.addRadar != 'NaN%'
                                                            ? coverageRate.addRadar
                                                            : '待计算'
                                                    }}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="flex w-full flex-col justify-center">
                                <!-- <div class="flex flex-row gap-2 my-4 ml-[13px] items-center">
                                    <label class="flex items-center gap-2">
                                        <input type="checkbox" v-model="isMerging" class="h-4 w-4 rounded" />
                                        合并一版图
                                    </label>
                                    <div>
                                        <el-tooltip content="需要输出时勾选，会大幅增加计算时间。" placement="top" effect="dark">
                                            <CircleHelp :size="14" />
                                        </el-tooltip>
                                    </div>
                                </div> -->

                                <button @click="calNoClouds" :disabled="noCloudLoading"
                                    class="flex justify-center w-full rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95"
                                    :class="{
                                        'cursor-not-allowed': noCloudLoading,
                                        'cursor-pointer': !noCloudLoading,
                                    }">
                                    <span>{{t('datapage.nocloud.section4.button')}} </span>
                                    <Loader v-if="noCloudLoading" class="ml-2" />
                                </button>
                                <div v-if="showProgress[3]"
                                    class="w-full overflow-hidden rounded-lg border border-[#2c3e50] bg-[#1e293b]">
                                    <div class="h-4 bg-gradient-to-r from-[#3b82f6] to-[#06b6d4] transition-all duration-300"
                                        :style="{ width: `${progress[3]}%` }"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>

                <section class="panel-section" v-if="currentPanel === 'history'" key="history">
                    <noCloudHistory @toggle="setCurrentPanel" />
                </section>

                <!-- <section class="panel-section" v-if="calImage.length > 0">
                    <div class="section-header">
                        <div class="section-icon">
                            <CloudIcon :size="18" />
                        </div>
                        <h2 class="section-title">重构信息</h2>
                    </div>
                    <div class="section-content">
                        <div class="config-container">
                            <div v-for="(image, index) in calImage" :key="index" class="config-item">
                                <div>无云一版图第{{ index + 1 }}次计算完成！</div>
                                本次使用的数据包括：{{ image.dataSet }}
                                <p>{{image}}</p>
                            </div>
                        </div>
                    </div>
                </section> -->
            </div>
        </dv-border-box12>
    </div>
</template>

<script setup lang="ts">
import { inject, computed, onMounted, ref, type PropType, type Ref, reactive } from 'vue'
import { BorderBox12 as DvBorderBox12 } from '@kjgl77/datav-vue3'
import { type interactiveExplore } from '@/components/dataCenter/type'
import noCloudHistory from '@/components/dataCenter/noCloud/noCloudHistory.vue'
import { formatTime } from '@/util/common'
import { getSceneGrids, getNoCloud, getCaseStatus, getCaseResult, pollStatus } from '@/api/http/satellite-data'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import * as MapOperation from '@/util/map/operation'
import { ElMessage } from 'element-plus'
import ezStore from '@/store/ezStore'
import { useTaskStore } from '@/store'
import {
    getGridImage,
    getGridPreviewUrl,
    getTifbandMinMax,
} from '@/api/http/satellite-data/visualize.api'
import { grid2Coordinates } from '@/util/map/gridMaker'
import { getNoCloudScaleParam, getNoCloudUrl, getNoCloudUrl4MosaicJson } from '@/api/http/satellite-data/visualize.api'

import {
    Loader,
    DatabaseIcon,
    MapPinIcon,
    CircleHelp,
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
    History,
} from 'lucide-vue-next'
import { FastBackwardFilled } from '@ant-design/icons-vue'
import bandMergeHelper from '@/util/image/util'
import { message } from 'ant-design-vue'
import { usePanelSwitchModule } from './panelSwitch'
import {
    getCoverRegionSensorScenes,
    getCoverPOISensorScenes,
} from '@/api/http/satellite-data'
import { getRGBTileLayerParamFromSceneObject } from '@/util/visualizeHelper/index'

import { useI18n } from 'vue-i18n'
const { t } = useI18n()

const props = defineProps({
    regionConfig: {
        type: Object as PropType<interactiveExplore>,
        required: true,
    },
})
console.log( props.regionConfig)
console.log(props.regionConfig.images)

/**
 * 面板显示控制区
 */
const { currentPanel, setCurrentPanel } = usePanelSwitchModule()

/**
 * 国产区
 */
// 各品类影像分类,1m是亚米
const demotic1mImages: Ref<any[]> = ref([])
const demotic2mImages: Ref<any[]> = ref([])
const internationalImages: Ref<any[]> = ref([])
const radarImages: Ref<any[]> = ref([])

// 累积影像分布到各个格网的计算结果
const demotic1mGridImages: Ref<any[]> = ref([])
const demotic2mGridImages: Ref<any[]> = ref([])
const interGridImages: Ref<any[]> = ref([])
const radarGridImages: Ref<any[]> = ref([])

// 记录每一级渲染的格网FeatureCollection
const demotic1mGridFeature: Ref<FeatureCollection | null> = ref(null)
const demotic2mGridFeature: Ref<FeatureCollection | null> = ref(null)
const interGridFeature: Ref<FeatureCollection | null> = ref(null)
const radarGridFeature: Ref<FeatureCollection | null> = ref(null)

interface CoverageRate {
    demotic1m: string | null
    demotic2m: string | null
    international: string | null
    addRadar: string | null
}
const coverageRate: Ref<CoverageRate> = ref({
    demotic1m: null,
    demotic2m: null,
    international: null,
    addRadar: null,
})

const noCloudLoading = ref(false)

interface platformType {
    platformName: string,
    tags?: string[]
    resolution: string,
    sceneId:string[],
    sensorName: string
}


// 获取筛选后的传感器和tags
const platformList: platformType[] = Array.from(
  props.regionConfig.images.reduce((map, item) => {
    const existing = map.get(item.platformName);
    if (existing) {
      // 如果已存在，将 sceneId 合并为数组
      existing.sceneId = Array.isArray(existing.sceneId)
        ? [...existing.sceneId, item.sceneId]  // 已是数组，追加
        : [existing.sceneId, item.sceneId];    // 原为单值，转为数组
    } else {
        // 如果不存在，初始化条目（sceneId 直接存为数组）
      map.set(item.platformName, {
        platformName: item.platformName,
        tags: item.tags ,
        resolution: item.resolution,
        sceneId: [item.sceneId],
        sensorName: item.sensorName
      })
    }
  return map;
  }, new Map())
  .values()
);

console.log('传感器和类别',platformList)
// 优先级选项排序
const groupedLists = computed(() => ({
  national: [
    ...platformList.filter(item => item.tags?.includes('national')),
    ...platformList.filter(item => !item.tags?.includes('national')),
  ],
  international: [
    ...platformList.filter(item => ['international', 'light'].every(tag => item.tags?.includes(tag))),
    ...platformList.filter(item => !item.tags?.includes('international')),
  ],
  sar: [
    ...platformList.filter(item => item.tags?.includes('radar')),
    ...platformList.filter(item => !item.tags?.includes('radar')),
  ],
}));

// const prioritized = [];
// const nonPrioritized = [];

// platformList.forEach(item => {
//   // Assuming tags is an array - adjust condition as needed
//   if (item.tags && item.tags.includes('national')) {
//     prioritized.push(item);
//   } else {
//     nonPrioritized.push(item);
//   }
// });
// // Merge them with prioritized first
// const platformList_natioan = [...prioritized, ...nonPrioritized];





// const getSceneIdsByPlatformName = (platformName: string, label: string) => {
//     console.log('所有景', props.regionConfig.images.value)
//     console.log('选中的平台名', platformName)
//     let scenes = props.regionConfig.images.value
//     if (label === '亚米') {
//         scenes = props.regionConfig.images.value.filter((scene) => {
//             if (scene.tags.includes('ard')) {
//                 return scene
//             }
//         })
//     }
//     console.log(scenes, 'Images')

//     if (platformName === 'all') return scenes.map((item) => item.sceneId)

//     const res: any[] = []
//     scenes.forEach((item) => {
//         if (item.platformName == platformName) {
//             res.push(item.sceneId)
//         }
//     })
//     console.log(res, 'images')

//     return res
// }
const landId = props.regionConfig.regionCode
const space = props.regionConfig.space 
const searchtab = props.regionConfig.searchtab
// const selectedOption  = ref<platformType | null>(null);
const selectnation = ref<platformType | null>(null);
const selectinternation = ref<platformType | null>(null);
const selectsar = ref<platformType | null>(null);

const handleShowSensorImage = async (selectedSensor: platformType | null)  => {
    console.log(selectedSensor,'选择')
    const sceneIds = selectedSensor?.sceneId || []
    console.log('选中的景ids', sceneIds)
    console.log('当前所有的景', props.regionConfig.images)
    const sensorName = selectedSensor?.sensorName || []

    console.log('匹配的sensorName', sensorName)

    console.log('对应LandId',landId)
    console.log(searchtab)

    const stopLoading = message.loading(t('datapage.explore.message.load'))

    let coverScenes
    if (searchtab === 'region') {
        const params = {
            sensorName,
            sceneIds,
            regionId: landId,
        }
        coverScenes = await getCoverRegionSensorScenes(params)
    } else if (searchtab === 'poi') {
        const params = {
            sensorName,
            sceneIds,
            locationId: landId,
            resolution: space,
        }
        coverScenes = await getCoverPOISensorScenes(params)
    }
    console.log(coverScenes, 1476);


    console.log('接口返回：覆盖的景们', coverScenes)

    const promises: Promise<any>[] = []

    for (let scene of coverScenes) {
        promises.push(getRGBTileLayerParamFromSceneObject(scene))
    }

    const rgbTileLayerParamList = await Promise.all(promises)

    console.log('可视化参数们', rgbTileLayerParamList)

    MapOperation.map_addMultiRGBImageTileLayer(rgbTileLayerParamList, stopLoading)
}


// 看起来是计算属性，其实已经影像分类初始化了
const demotic = computed(() => {
    let allImages = props.regionConfig.images

    allImages.forEach((image: any) => {
        if (image.tags.includes('radar')) {
            radarImages.value.push(image)
        } else if (image.tags.includes('international')) {
            // 国外非雷达数据
            internationalImages.value.push(image)
        } else if (image.tags.includes('ard') && image.resolution === '2m') {
            demotic2mImages.value.push(image)
        } else if (image.tags.includes('ard')) {
            let imageResolution = parseFloat(image.resolution)
            if (imageResolution <= 1) {
                demotic1mImages.value.push(image)
            }
        }
    })

    return demotic1mImages.value.length
})

const add1mDemoticImage = async () => {
    const isChecked = additionalData.value[0];
    // 清除格网图层，得放到一个请求上面，不然添加图层的时候还没销毁
    // gridStore.cleadAllGrids()
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    // 不管是否勾选，都要调用这个，因为取消勾选的进度条显示逻辑也在里面！！！
    controlProgress(0)

    if (isChecked) {
        return;
    }
    // 计算国产1m影像的格网分布和覆盖率
    const gridCount = props.regionConfig.grids.length;
    const allGrids = props.regionConfig.grids.map((item: any) => ({
        rowId: item.rowId,
        columnId: item.columnId,
        resolution: item.resolution,
    }));

    demotic1mGridImages.value = await getSceneGrids({
        grids: allGrids,
        sceneIds: demotic1mImages.value.map((image) => image.sceneId),
    });
    coverageRate.value.demotic1m = getCoverage(demotic1mGridImages.value, gridCount);

    // 国产影像渲染
    // 添加带有数据指示的格网
    let gridFeature: FeatureCollection = {
        type: 'FeatureCollection',
        features: props.regionConfig.grids.map((item: any, index) => {
            return {
                type: 'Feature',
                geometry: item.boundary.geometry as Geometry,
                properties: {
                    ...(item.properties || {}),
                    id: item.properties?.id ?? index, // 确保每个都有 id
                    opacity: judgeGridOpacity(index, demotic1mGridImages.value),
                    source: classifyGridSource(index, demotic1mGridImages.value, null) || null,
                },
            }
        }),
    }
    console.log(props.regionConfig.grids, 111)

    demotic1mGridFeature.value = gridFeature
    MapOperation.map_addGridLayer(gridFeature)
    MapOperation.draw_deleteAll()

    ElMessage.success(t('datapage.nocloud.message.guochanload'))
}

const add2mDemoticImages = () => {
    cancelCheckbox('grid', 0)

    // 逻辑与addRadarImages中的一样，可以参考
    let operateData = dataReconstruction.value[0]
        ? demotic1mGridImages.value
        : demotic2mGridImages.value

    // 清除格网图层，得放到一个请求上面，不然添加图层的时候还没销毁
    // gridStore.cleadAllGrids()
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    // 不管是否勾选，都要调用这个，因为取消勾选的进度条显示逻辑也在里面！！！
    controlProgress(0)

    // 进度条加载完毕才能进行渲染图层，但是取消勾选不需要等待，而是立刻加载上一级的图层
    setTimeout(
        () => {
            let gridFeature: FeatureCollection = {
                type: 'FeatureCollection',
                features: props.regionConfig.grids.map((item: any, index) => {
                    return {
                        type: 'Feature',
                        geometry: item.boundary.geometry as Geometry,
                        properties: {
                            ...(item.properties || {}),
                            id: item.properties?.id ?? index, // 确保每个都有 id
                            opacity: judgeGridOpacity(index, operateData),
                            source:
                                classifyGridSource(
                                    index,
                                    operateData,
                                    demotic1mGridFeature.value,
                                    'demotic2m',
                                ) || null,
                        },
                    }
                }),
            }
            demotic2mGridFeature.value = gridFeature

            MapOperation.map_addGridLayer(gridFeature)
            MapOperation.draw_deleteAll()
        },
        dataReconstruction.value[0] ? 100 : mockProgressTime,
    )
}

/**
 * 欧美区
 */

const addAbroadImages = () => {
    cancelCheckbox('grid', 1)
    // 逻辑与addRadarImages中的一样，可以参考
    let operateData = additionalData.value[1] ? demotic2mGridImages.value : interGridImages.value

    // 清除格网图层，得放到一个请求上面，不然添加图层的时候还没销毁
    // gridStore.cleadAllGrids()
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    // 不管是否勾选，都要调用这个，因为取消勾选的进度条显示逻辑也在里面！！！
    // controlProgress(1)

    // 进度条加载完毕才能进行渲染图层，但是取消勾选不需要等待，而是立刻加载上一级的图层
    setTimeout(
        () => {
            let gridFeature: FeatureCollection = {
                type: 'FeatureCollection',
                features: props.regionConfig.grids.map((item: any, index) => {
                    return {
                        type: 'Feature',
                        geometry: item.boundary.geometry as Geometry,
                        properties: {
                            ...(item.properties || {}),
                            id: item.properties?.id ?? index, // 确保每个都有 id
                            opacity: judgeGridOpacity(index, operateData),
                            source:
                                classifyGridSource(
                                    index,
                                    operateData,
                                    demotic2mGridFeature.value,
                                    'international',
                                ) || null,
                        },
                    }
                }),
            }

            interGridFeature.value = gridFeature
            MapOperation.map_addGridLayer(gridFeature)
            MapOperation.draw_deleteAll()
        },
        dataReconstruction.value[1] ? 100 : mockProgressTime,
    )
}

const addRadarImages = () => {
    // 这里要考虑一个问题，就是勾选的时候，渲染三合一的数据，取消勾选的时候，要渲染二合一的数据，所以渲染数据要根据
    // 勾选框的数据变化比较晚，所以勾选的时候是false，取消勾选的时候是true
    let operateData = additionalData.value[2] ? interGridImages.value : radarGridImages.value

    // 清除格网图层，得放到一个请求上面，不然添加图层的时候还没销毁
    // gridStore.cleadAllGrids()
    MapOperation.map_destroyImagePolygon()
    MapOperation.map_destroyImagePreviewLayer()
    MapOperation.map_destroyGridLayer()

    // 不管是否勾选，都要调用这个，因为取消勾选的进度条显示逻辑也在里面！！！
    // controlProgress(2)

    // 进度条加载完毕才能进行渲染图层，但是取消勾选不需要等待，而是立刻加载上一级的图层
    setTimeout(
        () => {
            let gridFeature: FeatureCollection = {
                type: 'FeatureCollection',
                features: props.regionConfig.grids.map((item: any, index) => {
                    return {
                        type: 'Feature',
                        geometry: item.boundary.geometry as Geometry,
                        properties: {
                            ...(item.properties || {}),
                            id: item.properties?.id ?? index, // 确保每个都有 id
                            opacity: judgeGridOpacity(index, operateData),
                            source:
                                classifyGridSource(
                                    index,
                                    operateData,
                                    interGridFeature.value,
                                    'radar',
                                ) || null,
                        },
                    }
                }),
            }
            console.log(gridFeature)
            radarGridFeature.value = gridFeature
            MapOperation.map_addGridLayer(gridFeature)
            MapOperation.draw_deleteAll()
        },
        dataReconstruction.value[2] ? 100 : mockProgressTime,
    )
}

/**
 * 快进进度条
 */

// 四个进度条的进度值
const progress = ref([0, 0, 0, 0])
// 四个进度条的显示状态
const showProgress = ref([false, false, false, false])
// const showCalResult = ref(false)

const calTask: Ref<any> = ref({
    calState: 'start',
    taskId: '',
})
const taskStore = useTaskStore()

// 填补勾选框
const additionalData = ref([false, false, false])
// 数据重构勾选框
const dataReconstruction = ref([false, false, false])
// 是否合并
const isMerging = ref(false)

const calImage: Ref<any[]> = ref([])
let progressTimer: ReturnType<typeof setInterval> | null = null

// 控制进度条
const progressControl = (index: number) => {
    if (calTask.value.calState === 'pending') return
    progress.value[index] = 0
    calTask.value.calState = 'pending'
    progressTimer = setInterval(() => {
        if (calTask.value.calState === 'success' || calTask.value.calState === 'failed') {
            progress.value[index] = 100
            // showCalResult.value = true
            clearInterval(progressTimer!)
            progressTimer = null
        } else if (progress.value[index] < 95) {
            progress.value[index] += 1
        } else {
            progress.value[index] = 95
        }
    }, 100)
}

// 开始计算
const calNoClouds = async () => {
    // noCloudLoading.value = true
    // const stopLoading = message.loading("正在重构无云一版图...", 0)

    // 因为从后端拿到taskId需要一定时间，所以先向任务store推送一个初始化任务状态
    taskStore.setIsInitialTaskPending(true)
    setCurrentPanel('history')

    // 根据勾选情况合并影像
    // 1、国产亚米

    let addedImages = [...demotic1mImages.value]
    if (dataReconstruction.value[0] === true) {
        addedImages = addedImages.concat(demotic2mImages.value)
    }
    if (dataReconstruction.value[1] === true) {
        addedImages = addedImages.concat(internationalImages.value)
    }
    if (dataReconstruction.value[2] === true) {
        addedImages = addedImages.concat(radarImages.value)
    }
    let dataSet = [
                '国产亚米影像',
                dataReconstruction.value[0] ? '国产2m超分影像' : null,
                dataReconstruction.value[1] ? '国外影像超分数据' : null,
                dataReconstruction.value[2] ? 'SAR色彩转换数据' : null,
    ].filter(Boolean).join('、')

    let getNoCloudParam = {
        regionId: props.regionConfig.regionCode,
        cloud: props.regionConfig.cloud,
        resolution: props.regionConfig.space,
        sceneIds: addedImages.map((image) => image.sceneId),
        dataSet: dataSet,
    }

    // 发送请求
    console.log(getNoCloudParam, '发起请求')
    let startCalcRes = await getNoCloud(getNoCloudParam)
    if (startCalcRes.message !== 'success') {
        ElMessage.error(t('datapage.nocloud.message.calerror'))
        console.error(startCalcRes)
        return
    }
    // 更新任务，跳转至历史panel
    calTask.value.taskId = startCalcRes.data
    taskStore.setTaskStatus(calTask.value.taskId, 'PENDING')
    taskStore.setIsInitialTaskPending(false)

    // 1、启动进度条
    // controlProgress(3)

    // 这里不再轮询
    /** 
    // 轮询运行状态，直到运行完成
    try {
        await pollStatus(calTask.value.taskId)
        // ✅ 成功后设置状态
        calTask.value.calState = 'success'
        console.log('成功，开始拿结果')

        let res = await getCaseResult(calTask.value.taskId)
        console.log(res, '结果')

        // 1、先预览无云一版图影像
        let data = res.data
        const getData = async (taskId: string) => {
            let res:any
            while (!(res = await getCaseResult(taskId)).data) {
                console.log('Retrying...')
                await new Promise(resolve => setTimeout(resolve, 1000));
            }
            return res.data;
        }
        if(!data)
            data = await getData(calTask.value.taskId)
        
        previewNoCloud(data)

        // 2、补充数据
        let calResult = {
            demotic1m: true,
            demotic2m: dataReconstruction.value[0],
            international: dataReconstruction.value[1],
            radar: dataReconstruction.value[2],
            dataSet: dataSet,
        }
        console.log(dataReconstruction.value, calResult)

        calImage.value.push(calResult)
        noCloudLoading.value = false
        stopLoading()
        ElMessage.success('无云一版图计算完成')
    } catch (error) {
        console.log(error)
        calTask.value.calState = 'failed'
        noCloudLoading.value = false
        stopLoading()
        ElMessage.error('无云一版图计算失败，请重试')
    } */
}

const showingImageStrech = reactive({
    r_min: 0,
    r_max: 5000,
    g_min: 0,
    g_max: 5000,
    b_min: 0,
    b_max: 5000,
})
// 预览无云一版图
const previewNoCloud = async (data: any) => {

    const stopLoading = message.loading(t('datapage.nocloud.message.load'), 0)
    // 清除旧图层
    MapOperation.map_removeNocloudGridPreviewLayer()
    MapOperation.map_destroyNoCloudLayer()
    // -------- 旧版无云一版图（合并版）展示逻辑 ------------------------------
    /* const nocloudTifPath = data.bucket + '/' + data.tifPath
    const band123Scale = await getNoCloudScaleParam(nocloudTifPath)
    const url = getNoCloudUrl({
        fullTifPath: nocloudTifPath,
        ...band123Scale
    })
    MapOperation.map_addNoCloudLayer(url) */

    // -------- 新版无云一版图（MosaicJson）展示逻辑 --------------------------
    const mosaicJsonPath = data.bucket + '/' + data.object_path
    const url4MosaicJson = getNoCloudUrl4MosaicJson({
        mosaicJsonPath: mosaicJsonPath
    })
    MapOperation.map_addNoCloudLayer(url4MosaicJson)

    // 清除旧图层
    // MapOperation.map_destroyMultiNoCloudLayer()
    // console.log(data)

    // MapOperation.map_addMultiNoCloudLayer(data.grids, data.statistic)



    // const gridResolution = props.regionConfig.space

    // for (let i = 0; i < data.length; i++) {
    //     const gridInfo = {
    //         columnId: data[i].colId, // 注意这里返回的是colID，其他接口都是columnId
    //         rowId: data[i].rowId,
    //         resolution: gridResolution,
    //         redPath: data[i].bucket + '/' + data[i].redPath,
    //         greenPath: data[i].bucket + '/' + data[i].greenPath,
    //         bluePath: data[i].bucket + '/' + data[i].bluePath,
    //     }
    //     // // console.log('gridInfo', gridInfo)
    //     // bandMergeHelper.mergeGrid(gridInfo, (url) => {
    //     //     const imgUrl = url
    //     //     const gridCoords = grid2Coordinates(data[i].colId, data[i].rowId, gridResolution)
    //     //     MapOperation.map_addGridPreviewLayer(imgUrl, gridCoords, 'nocloud')
    //     // })
    //     let redPath = gridInfo.redPath
    //     let greenPath = gridInfo.greenPath
    //     let bluePath = gridInfo.bluePath

    //     const cache = ezStore.get('statisticCache')
    //     const promises: any = []
    //     let [min_r, max_r, min_g, max_g, min_b, max_b] = [0, 0, 0, 0, 0, 0]

    //     if (cache.get(redPath) && cache.get(greenPath) && cache.get(bluePath)) {
    //         console.log('cache hit!')
    //             ;[min_r, max_r] = cache.get(redPath)
    //             ;[min_g, max_g] = cache.get(greenPath)
    //             ;[min_b, max_b] = cache.get(bluePath)
    //     } else {
    //         promises.push(
    //             getTifbandMinMax(redPath),
    //             getTifbandMinMax(greenPath),
    //             getTifbandMinMax(bluePath),
    //         )
    //         await Promise.all(promises).then((values) => {
    //             min_r = values[0][0]
    //             max_r = values[0][1]
    //             min_g = values[1][0]
    //             max_g = values[1][1]
    //             min_b = values[2][0]
    //             max_b = values[2][1]
    //         })

    //         cache.set(redPath, [min_r, max_r])
    //         cache.set(greenPath, [min_g, max_g])
    //         cache.set(bluePath, [min_b, max_b])
    //     }

    //     console.log(min_r, max_r, min_g, max_g, min_b, max_b)

    //     const defaultScaleRate = 50
    //     const scale = 1.0 - defaultScaleRate / 100
    //     // 基于 scale rate 进行拉伸
    //     showingImageStrech.r_min = Math.round(min_r)
    //     showingImageStrech.r_max = Math.round(min_r + (max_r - min_r) * scale)
    //     showingImageStrech.g_min = Math.round(min_g)
    //     showingImageStrech.g_max = Math.round(min_g + (max_g - min_g) * scale)
    //     showingImageStrech.b_min = Math.round(min_b)
    //     showingImageStrech.b_max = Math.round(min_b + (max_b - min_b) * scale)
    //     MapOperation.map_addGridRGBImageTileLayer({
    //         ...gridInfo,
    //     }, {
    //         redPath,
    //         greenPath,
    //         bluePath,
    //         ...showingImageStrech,
    //     })
    // }

    setTimeout(() => {
        stopLoading()
    }, 5000);
    // console.log('一下加几十个图层，等着吃好果子')
}
// 假操作进度条统一时间
const mockProgressTime = 500

// 现在的问题是，国外和SAR的勾选框有两个，取消一个都要取消后面的勾选框，所以作为一个单独的方法
const cancelCheckbox = (type: string, index: number) => {
    // 第一种情况，取消勾选格网填补
    if (type === 'grid' && additionalData.value[index] === true) {
        // showProgress.value[index] = false
        while (index < additionalData.value.length - 1) {
            additionalData.value[index + 1] = false
            dataReconstruction.value[index + 1] = false
            showProgress.value[index + 1] = false
            index++
        }
        return true
    }
    // 第二种情况，取消勾选数据重构
    if (type === 'dataReconstruction' && dataReconstruction.value[index] === true) {
        showProgress.value[index] = false
        while (index < dataReconstruction.value.length - 1) {
            additionalData.value[index + 1] = false
            dataReconstruction.value[index + 1] = false
            showProgress.value[index + 1] = false
            index++
        }
        return true
    }
}
// 操控进度条
const controlProgress = (index: number) => {
    // 1、取消勾选要把后面的选项全部取消勾选。2、取消勾选隐藏进度条
    // 这里要注意，additionalData值变化是延后的，所以是变化前的值
    let overTask = cancelCheckbox('dataReconstruction', index)
    if (overTask) return

    // 只显示当前进度条
    showProgress.value = showProgress.value.map((_progress, i: number) => {
        return index === i ? true : false
    })

    progressControl(index)

    // 2、轮询运行状态，直到运行完成
    setTimeout(() => {
        calTask.value.calState = 'success'
    }, mockProgressTime)
}

onMounted(async () => {
    // 清除格网图层，得放到一个请求上面，不然添加图层的时候还没销毁
    // gridStore.cleadAllGrids()
    // MapOperation.map_destroyImagePolygon()
    // MapOperation.map_destroyImagePreviewLayer()
    // MapOperation.map_destroyGridLayer()

    // 计算四个覆盖率
    let gridCount = props.regionConfig.grids.length
    let allGrids = props.regionConfig.grids.map((item: any) => {
        return {
            rowId: item.rowId,
            columnId: item.columnId,
            resolution: item.resolution,
        }
    })

    // 计算四种情况的格网分布情况
    demotic1mGridImages.value = await getSceneGrids({
        grids: allGrids,
        sceneIds: demotic1mImages.value.map((images) => images.sceneId),
    })
    console.log('国产亚米级影像分布情况', demotic1mGridImages.value)
    coverageRate.value.demotic1m = getCoverage(demotic1mGridImages.value, gridCount)

    let addDemotic1mImages = demotic1mImages.value.concat(demotic2mImages.value)
    demotic2mGridImages.value = await getSceneGrids({
        grids: allGrids,
        sceneIds: addDemotic1mImages.map((images) => images.sceneId),
    })
    console.log('国产2m超分影像分布情况', demotic2mGridImages.value)
    coverageRate.value.demotic2m = getCoverage(demotic2mGridImages.value, gridCount)

    let addInternationalImages = addDemotic1mImages.concat(internationalImages.value)
    interGridImages.value = await getSceneGrids({
        grids: allGrids,
        sceneIds: addInternationalImages.map((images) => images.sceneId),
    })
    coverageRate.value.international = getCoverage(interGridImages.value, gridCount)

    let addRadarImages = addInternationalImages.concat(radarImages.value)
    radarGridImages.value = await getSceneGrids({
        grids: allGrids,
        sceneIds: addRadarImages.map((images) => images.sceneId),
    })
    coverageRate.value.addRadar = getCoverage(radarGridImages.value, gridCount)

    // // 国产影像渲染
    // // 添加带有数据指示的格网
    // let gridFeature: FeatureCollection = {
    //     type: 'FeatureCollection',
    //     features: props.regionConfig.grids.map((item: any, index) => {
    //         return {
    //             type: 'Feature',
    //             geometry: item.boundary.geometry as Geometry,
    //             properties: {
    //                 ...(item.properties || {}),
    //                 id: item.properties?.id ?? index, // 确保每个都有 id
    //                 opacity: judgeGridOpacity(index, demotic1mGridImages.value),
    //                 source: classifyGridSource(index, demotic1mGridImages.value, null) || null,
    //             },
    //         }
    //     }),
    // }
    // console.log(props.regionConfig.grids, 111)

    // demotic1mGridFeature.value = gridFeature
    // MapOperation.map_addGridLayer(gridFeature)
    // MapOperation.draw_deleteAll()

    // ElMessage.success(t('datapage.nocloud.message.guochanload'))
})

// 算格网的颜色,接收的数据分别为：要上色的格网本身，累积影像分布到格网的结果，格网数量，所属层级
// 取消勾选，即回到上一级数据格网的结果也没问题，第三个传输就传递上一级（和第二个参数相同）即可
const classifyGridSource = (
    index: any,
    sceneGridsRes: any,
    lastGridFeature?: any,
    type?: string,
) => {
    if (lastGridFeature === null) {
        let source: string | null
        sceneGridsRes[index]?.scenes.length > 0 ? (source = 'demotic1m') : (source = null)
        return source
    } else if (type !== undefined) {
        let source: string | null
        let lastSource = lastGridFeature.features[index].properties.source
        lastSource
            ? (source = lastSource)
            : sceneGridsRes[index]?.scenes.length > 0
                ? (source = type)
                : (source = null)
        return source
    }
    return null
}

// 判断格网到底有没有数据，有就返回0.3
const judgeGridOpacity = (index: number, sceneGridsRes: any) => {
    let opacity = 0.01
    sceneGridsRes[index]?.scenes.length > 0 ? (opacity = 0.3) : (opacity = 0.01)
    return opacity
}

// 算覆盖率
const getCoverage = (gridImages: any, gridCount: number) => {
    const nonEmptyScenesCount = gridImages.filter((item) => item.scenes.length > 0).length
    let coverage = ((nonEmptyScenesCount * 100) / gridCount).toFixed(2) + '%'
    return coverage
}
</script>

<style scoped src="../tabStyle.css">
:deep(.border-box-content) {
    padding: 1.5rem;
}

</style>
