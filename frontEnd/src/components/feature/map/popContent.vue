<template>
    <Vue3DraggableResizable :draggable="enableDraggable" :resizable="false" :initW="250">
        <div class="popup-content">
            <div class="grid-id">
                <p>时空立方体编号: {{ gridID }}</p>
            </div>

            <div class="tabs">
                <button
                    class="tab-btn"
                    :class="{ active: activeTab === 'scene' }"
                    @click="activeTab = 'scene'"
                >
                    遥感影像
                </button>
                <button
                    class="tab-btn"
                    :class="{ active: activeTab === 'vector' }"
                    @click="activeTab = 'vector'"
                >
                    矢量专题
                </button>
                <button
                    class="tab-btn"
                    :class="{ active: activeTab === 'theme' }"
                    @click="activeTab = 'theme'"
                >
                    栅格专题
                </button>
            </div>

            <!-- 影像Tab -->
            <div v-show="activeTab === 'scene'">
                <!-- 分辨率选择 -->
                <div class="band-selection">
                    <label for="resolution-select">分辨率:</label>
                    <select id="resolution-select" v-model="selectedResolution" class="band-select">
                        <option disabled value="">请选择</option>
                        <option v-for="reso in resolutions" :key="reso" :value="reso">
                            {{ reso }}
                        </option>
                    </select>
                </div>
                
                <!-- 传感器选择 -->
                <div class="band-selection">
                    <label for="sensor-select">传感器:</label>
                    <select
                        id="sensor-select"
                        v-model="selectedSensor"
                        class="band-select"
                        @change="handleSensorChange(selectedSensor)"
                    >
                        <option disabled value="">请选择</option>
                        <option v-for="sensor in sensors" :key="sensor" :value="sensor">
                            {{ sensor }}
                        </option>
                    </select>
                </div>

                <!-- 增强方法Tab -->
                <div class="tabs" v-show="showBandSelector">
                    <button
                        class="tab-btn"
                        :class="{ active: activeMethod === 'rgb' }"
                        @click="activeMethod = 'rgb'"
                    >
                        波段增强
                    </button>
                    <button
                        class="tab-btn"
                        :class="{ active: activeMethod === 'superresolution' }"
                        @click="activeMethod = 'superresolution'"
                    >
                        信息增强
                    </button>
                </div>

                <!-- 波段增强Tab -->
                <div v-show="showBandSelector && activeMethod === 'rgb'" class="tab-content">
                    <div class="band-selection">
                        <label for="r-band-select">R波段:</label>
                        <select id="r-band-select" v-model="selectedRBand" class="band-select">
                            <option disabled value="">请选择</option>
                            <option v-for="band in bands" :key="band" :value="band">
                                {{ band }}
                            </option>
                        </select>
                    </div>
                    <div class="band-selection">
                        <label for="g-band-select">G波段:</label>
                        <select id="g-band-select" v-model="selectedGBand" class="band-select">
                            <option disabled value="">请选择</option>
                            <option v-for="band in bands" :key="band" :value="band">
                                {{ band }}
                            </option>
                        </select>
                    </div>
                    <div class="band-selection">
                        <label for="b-band-select">B波段:</label>
                        <select id="b-band-select" v-model="selectedBBand" class="band-select">
                            <option disabled value="">请选择</option>
                            <option v-for="band in bands" :key="band" :value="band">
                                {{ band }}
                            </option>
                        </select>
                    </div>
                </div>

                <!-- 超分增强Tab -->
                <div class="btns flex justify-center" v-show="showBandSelector && activeMethod === 'superresolution'">
                    <button class="visualize-btn" 
                        @click="handleSuperResolution">
                        <span class="btn-icon">
                            <GalleryHorizontalIcon :size="18" />
                        </span>
                        超分增强
                    </button>
                </div>
                <div class="btns flex justify-center" v-show="showBandSelector && activeMethod === 'superresolution'">
                    <button class="visualize-btn">
                        <span class="btn-icon">
                            <GalleryHorizontalIcon :size="18" />
                        </span>
                        指数增强
                    </button>
                </div>

                <!-- 原亮度拉伸，现为拉伸增强 -->
                <div
                    class="mr-1 grid grid-cols-[2fr_3fr]"
                    @mousedown="handleScaleMouseDown"
                    @mouseup="handleScaleMouseUp"
                    v-show="activeMethod === 'rgb'"
                >
                    <span class="sp text-white">拉伸增强:</span>
                    <a-slider
                        :tip-formatter="scaleRateFormatter"
                        v-model:value="scaleRate"
                        :min="0"
                        :max="10"
                        @afterChange="onAfterScaleRateChange"
                    />
                </div>

                <!-- 可视化按钮 -->
                <div class="btns" v-show="activeMethod === 'rgb'">
                    <button class="visualize-btn" @click="handleVisualize" :disabled="!canVisualize">
                        <span class="btn-icon">
                            <GalleryHorizontalIcon :size="18" />
                        </span>
                        立方体可视化
                    </button>
                    <button class="delete-btn" @click="handleRemove">
                        <span class="btn-icon">
                            <Trash2Icon :size="18" />
                        </span>
                    </button>
                </div>
            </div>

            <!-- 矢量Tab -->
            <div v-show="activeTab === 'vector'">
                <div class="config-container">
                    <span class="result-info-label">共找到 {{gridData.vectors.length}} 条记录</span>
                    <a-checkable-tag
                        v-for="(item, index) in gridData.vectors"
                        :key="item.tableName"
                        :checked="previewIndex === index"
                        @click="handleSelectVector(index)"
                        class="vector-tag"
                    >
                        <span class="btn-icon">
                            <DatabaseIcon :size="18" />&nbsp;{{ item.vectorName }}
                        </span>
                    </a-checkable-tag>
                </div>

                <div class="btns">
                    <button class="visualize-btn" @click="handleVisualize">
                        <span class="btn-icon">
                            <GalleryHorizontalIcon :size="18" />
                        </span>
                        立方体可视化
                    </button>
                    <button class="delete-btn" @click="handleRemove">
                        <span class="btn-icon">
                            <Trash2Icon :size="18" />
                        </span>
                    </button>
                </div>
            </div>

            <!-- 产品Tab -->
            <div v-show="activeTab === 'theme'">
                <!-- 类别选择 -->
                <div class="band-selection">
                    <label for="product-select">专题:</label>
                    <select id="product-select" v-model="selectedProductType" class="band-select">
                        <option disabled value="">请选择</option>
                        <option v-for="productType in productTypes" :key="productType" :value="productType">
                            {{ productType }}
                        </option>
                    </select>
                </div>

                <!-- 产品选择 -->
                <div class="band-selection">
                    <label for="sensor-select">产品名:</label>
                    <select
                        id="sensor-select"
                        v-model="selectedProduct"
                        class="band-select"
                        @change="handleSensorChange(selectedProduct)"
                    >
                        <option disabled value="">请选择</option>
                        <option v-for="product in products" :key="product" :value="product">
                            {{ product }}
                        </option>
                    </select>
                </div>

                <!-- 透明度 -->
                <div
                    class="mr-1 grid grid-cols-[2fr_3fr]"
                    @mousedown="handleScaleMouseDown"
                    @mouseup="handleScaleMouseUp"
                >
                    <span class="sp text-white">透明度:</span>
                    <a-slider
                        :tip-formatter="opacityFormatter"
                        v-model:value="opacity"
                        @afterChange="onAfterOpacityChange"
                    />
                </div>

                <!-- 可视化按钮 -->
                <div class="btns">
                    <button class="visualize-btn" @click="handleVisualize" :disabled="!canVisualize">
                        <span class="btn-icon">
                            <GalleryHorizontalIcon :size="18" />
                        </span>
                        立方体可视化
                    </button>
                    <button class="delete-btn" @click="handleRemove">
                        <span class="btn-icon">
                            <Trash2Icon :size="18" />
                        </span>
                    </button>
                </div>
            </div>
        </div>
    </Vue3DraggableResizable>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, type Ref, reactive } from 'vue'
import { DatabaseIcon, GalleryHorizontalIcon, RectangleEllipsisIcon, Trash2Icon,CircleOff } from 'lucide-vue-next'
import bus from '@/store/bus'
import Vue3DraggableResizable from 'vue3-draggable-resizable'
import 'vue3-draggable-resizable/dist/Vue3DraggableResizable.css'
import { ezStore } from '@/store'
import { GetSuperResolution } from '@/api/http/satellite-data/visualize.api'
import { ElMessage } from 'element-plus'
import { getCaseStatus, getCaseBandsResult} from '@/api/http/satellite-data'
import type { GridData, MultiImageInfoType, PopupTab, SceneMethod } from '@/type/interactive-explore/grid'
import type { Grid } from '@/api/http/interactive-explore/grid.type'
import { getThemesInGrid } from '@/api/http/interactive-explore/grid.api'
import { getScenesInGrid } from '@/api/http/interactive-explore/grid.api'
import * as GridExploreMapOps from '@/util/map/operation/grid-explore'
import { getGridVectorUrl } from '@/api/http/interactive-explore/visualize.api'
import { message } from 'ant-design-vue'
/**
 * 1. 公共变量
 */
/**
 * 当前tab
 */
const activeTab = ref<PopupTab>('scene')
    const gridID = computed(() => {
    const { rowId, columnId, resolution } = gridData.value
    return `${rowId}-${columnId}-${resolution}`
})
/**
 * 网格数据
 */
 const gridData = ref<GridData>({
    rowId: 0,
    columnId: 0,
    resolution: 0,
    sceneRes: {
        total: 0,
        category: [],
    },
    vectors: [],
    themeRes: {
        total: 0,
        category: [],
    },
})
/**
 * 判断是否可以可视化
 */
const canVisualize = computed(() => {
    switch (activeTab.value) {
        case 'scene':
            if (showBandSelector.value === false) return true
            if (activeMethod.value === 'rgb') {
                return !!selectedRBand.value && !!selectedGBand.value && !!selectedBBand.value
            }
            break
        case 'vector':
            // return !!selectedResolution.value
            break
        case 'theme':
            return !!selectedProductType.value && !!selectedProduct.value
            break
    }
})
/**
 * 可视化函数
 */
const handleVisualize = () => {
    switch (activeTab.value) {
        case 'scene':
            handleSceneVisualize()
            break
        case 'vector':
            if (previewIndex.value !== null) {
                handleVectorVisualize(gridData.value.vectors[previewIndex.value].tableName)
            }
            break
        case 'theme':
            handleProductVisualize()
            break
    }
}
/**
 * 删除可视化
 */
const handleRemove = () => {
    previewIndex.value = null
    GridExploreMapOps.map_destroyGridDEMLayer(gridData.value)
    GridExploreMapOps.map_destroyGridMVTLayer()
    GridExploreMapOps.map_destroyGrid3DLayer(gridData.value)
    GridExploreMapOps.map_destroyGridNDVIOrSVRLayer(gridData.value)
}
/**
 * 初始化网格
 */
bus.on('update:gridPopupData', async (info) => {
    gridData.value = info
    gridData.value.vectors = ezStore.get('vectorStats')
    gridData.value.themeRes = await getThemesInGrid(info)
    gridData.value.sceneRes = await getScenesInGrid(info)
    console.log(gridData.value, 'gridData')
})

onMounted(async () => {
    if (!ezStore.get('statisticCache')) {
        ezStore.set('statisticCache', new Map())
        console.log('cache init')
    }
    bus.on('closeTimeline', () => {
        selectedBand.value = ''
        selectedRBand.value = ''
        selectedGBand.value = ''
        selectedBBand.value = ''
        selectedSensor.value = ''
    })
})

/**
 * 2. 遥感影像Tab
 */
// 当前遥感影像可视化方法
const activeMethod = ref<SceneMethod>('rgb')
/**
 * 分辨率选项
 */
const selectedResolution = ref('')
const resolutions = computed(() => {
    const result = new Set<string>()
    if (gridData.value.sceneRes?.dataset) {
        console.log(gridData.value, '111')
        for(const category of gridData.value.sceneRes.category) {
            
            if (gridData.value.sceneRes.dataset[category] === undefined) {
                console.log(category, '222')
                console.log(gridData.value.sceneRes.dataset, '222')
                console.log(gridData.value, '222')
            }
            if (gridData.value.sceneRes.dataset[category].dataList.length > 0) {
                result.add(gridData.value.sceneRes.dataset[category].label)
            }
        }
    }
    const arr = Array.from(result)
    arr.sort((a, b) => {
        return Number(b) - Number(a)
    })

    return ['全选', ...arr]
})
/**
 * 传感器选项
 */
const selectedSensor = ref('')
const sensors = computed(() => {
    let result = new Set<string>()
    result.add('全选')
    if (gridData.value.sceneRes?.dataset) {
        for(const category of gridData.value.sceneRes.category) {
            for(const scene of gridData.value.sceneRes.dataset[category].dataList) {
                if (selectedResolution.value != '全选' && gridData.value.sceneRes.dataset[category].label == selectedResolution.value) {
                    result.add(scene.sensorName)
                } else if (selectedResolution.value === '全选') {
                    result.add(scene.sensorName)
                }
            }
        }
    }
    return Array.from(result)
})
const handleSensorChange = (value) => {
    if (value === '全选') {
        showBandSelector.value = false
    } else {
        showBandSelector.value = true
    }
}
/**
 * 波段选项
 */
const selectedBand = ref('')
const selectedRBand = ref('')
const selectedGBand = ref('')
const selectedBBand = ref('')
const bands = computed(() => {
    let result: string[] = []

    if (selectedSensor.value != '' && gridData.value.sceneRes?.dataset) {
        for (const category of gridData.value.sceneRes.category) {
            for (const scene of gridData.value.sceneRes.dataset[category].dataList) {
                if (selectedSensor.value && scene.sensorName === selectedSensor.value) {
                    scene.images.forEach((bandImg) => {
                        if (!result.includes(bandImg.band.toString())) {
                            result.push(bandImg.band.toString())
                        }
                    })
                }
            }
        }
    }

    result.sort((a, b) => {
        return parseInt(a, 10) - parseInt(b, 10)
    })

    if (result.length > 0) {
        selectedBand.value = result[0]
    }
    if (result.length > 2) {
        selectedBBand.value = result[2]
        selectedGBand.value = result[1]
        selectedRBand.value = result[0]
    }

    return result
})
const showBandSelector = ref(true)

/**
 * 拉伸增强选项
 */
const enableDraggable = ref(true)
const scaleRate = ref(0)
const scaleRateFormatter = (value: number) => {
    return `${value}级`
}

const handleScaleMouseUp = () => (enableDraggable.value = true)

const handleScaleMouseDown = () => (enableDraggable.value = false)

const onAfterScaleRateChange = (scale_rate: number) => {
    console.log(scale_rate)
}

/**
 * 遥感影像可视化函数
 */
const visualLoad = ref(false)
const handleSceneVisualize = () => {
    const { rowId, columnId, resolution } = gridData.value
    const gridInfo = {
        rowId,
        columnId,
        resolution,
    }
    if (!gridData.value.sceneRes?.dataset) {
        return
    }
    // 所有的它都想看
    if (showBandSelector.value === false) {
        const rgbImageData: MultiImageInfoType[] = []

        const gridAllScenes:Grid.SceneDetail[] = []
        // gridData.value.scenes
        // 分辨率指定、传感器全选在上面
        if (selectedResolution.value && selectedResolution.value !== '全选') {
            for (const category of gridData.value.sceneRes.category) {
                for (const scene of gridData.value.sceneRes.dataset[category].dataList) {
                    if (gridData.value.sceneRes.dataset[category].label === selectedResolution.value) {
                        gridAllScenes.push(scene)
                    }
                }
            }
        } else {
            // 分辨率全选，传感器全选
            for (const category of gridData.value.sceneRes.category) {
                for (const scene of gridData.value.sceneRes.dataset[category].dataList) {
                    gridAllScenes.push(scene)
                }
            }
        }

        // Process each band (R, G, B)
        for (let sceneInfo of gridAllScenes) {
            let redPath = ''
            let greenPath = ''
            let bluePath = ''

            // 这里用bandmapper
            // console.log(sceneInfo.bandMapper)
            for (let bandImg of sceneInfo.images) {
                // 后端返回的BandMapper如果是单波段的话，Red Green 和 Blue相同
                if (sceneInfo.bandMapper.Red == bandImg.band) {
                    redPath = bandImg.bucket + '/' + bandImg.tifPath
                }
                if (sceneInfo.bandMapper.Green == bandImg.band) {
                    greenPath = bandImg.bucket + '/' + bandImg.tifPath
                }
                if (sceneInfo.bandMapper.Blue == bandImg.band) {
                    bluePath = bandImg.bucket + '/' + bandImg.tifPath
                }
            }
            rgbImageData.push({
                sceneId: sceneInfo.sceneId,
                sensorName: sceneInfo.sensorName,
                productName: sceneInfo.sensorName + '-' +sceneInfo.productName,
                dataType: 'satellite',
                time: sceneInfo.sceneTime,
                redPath: redPath,
                greenPath: greenPath,
                bluePath: bluePath,
                nodata: sceneInfo.noData,
            })
        }
        
        bus.emit('cubeVisualize', rgbImageData, gridInfo, scaleRate.value, 'rgb')
    } else {
        // 针对具体传感器、具体波段组合
        const rgbImageData: MultiImageInfoType[] = []
        const filteredScene: Grid.SceneDetail[] = []
        for (const category of gridData.value.sceneRes.category) {
            for (const scene of gridData.value.sceneRes.dataset[category].dataList) {
                // 分辨率全选，传感器全选和分辨率指定、传感器全选在上面
                if (selectedResolution.value == '全选' && selectedSensor.value != '全选') {
                    // 分辨率全选，传感器指定
                    if (scene.sensorName === selectedSensor.value) {
                        filteredScene.push(scene)
                    }
                } else if (selectedResolution.value != '全选' && selectedSensor.value != '全选') {
                    // 分辨率指定，传感器指定
                    if (gridData.value.sceneRes.dataset[category].label === selectedResolution.value && scene.sensorName === selectedSensor.value) {
                        filteredScene.push(scene)
                    }
                }
            }
        }

        // Process each band (R, G, B)
        for (let scene of filteredScene) {
            let redPath = ''
            let greenPath = ''
            let bluePath = ''

            // 这里用用户选的
            scene.images.forEach((bandImg) => {
                console.log(bandImg, 'bandImg')
                if (bandImg.band == Number(selectedRBand.value)) {
                    redPath = bandImg.bucket + '/' + bandImg.tifPath
                }
                if (bandImg.band == Number(selectedGBand.value)) {
                    greenPath = bandImg.bucket + '/' + bandImg.tifPath
                }
                if (bandImg.band == Number(selectedBBand.value)) {
                    bluePath = bandImg.bucket + '/' + bandImg.tifPath
                }
            })

            rgbImageData.push({
                sceneId: scene.sceneId,
                time: scene.sceneTime,
                sensorName: scene.sensorName,
                productName: scene.sensorName + '-' + scene.productName,
                dataType: 'satellite',
                redPath: redPath,
                greenPath: greenPath,
                bluePath: bluePath,
                nodata: scene.noData,
            })
        }
        bus.emit('cubeVisualize', rgbImageData, gridInfo, scaleRate.value, 'rgb')
    }

    bus.emit('openTimeline')
    visualLoad.value = true
}


/**
 * 3. 矢量Tab
 */
const previewIndex = ref<number | null>(null)
const handleSelectVector = (index: number) => {
  previewIndex.value = previewIndex.value === index ? null : index
}
const handleVectorVisualize = (tableName: string) => {
    previewIndex.value = gridData.value.vectors.findIndex((item) => item.tableName === tableName)
    const url = getGridVectorUrl(gridData.value, tableName)
    GridExploreMapOps.map_addGridMVTLayer(tableName, url)
}

/**
 * 4. 栅格专题Tab
 */
const selectedProductType = ref('')
const selectedProduct = ref('')
const productTypes = computed(() => {
    if (!gridData.value.themeRes?.dataset)
        return []
    const result = new Set<string>()
    result.add('全选')
    for (const category of gridData.value.themeRes.category) {
        result.add(gridData.value.themeRes.dataset[category].label)
    }
    return Array.from(result)
})

const products = computed(() => {
    let result = new Set<string>()
    if (!gridData.value.themeRes?.dataset)
        return []
    result.add('全选')
    for (const category of gridData.value.themeRes.category) {
        for (const theme of gridData.value.themeRes.dataset[category].dataList) {
            if (selectedProductType.value != '全选') {
                let dataType = gridData.value.themeRes.dataset[category].label
                if (dataType === selectedProductType.value) {
                    result.add(theme.sensorName)
                }
            } else if (selectedProductType.value === '全选') {
                result.add(theme.sensorName)
            }
        }
    }
    return Array.from(result)
})
const handleProductVisualize = () => {
    const gridInfo = gridData.value
    gridInfo.opacity = opacity.value
    const imageData: MultiImageInfoType[] = []
    const gridAllThemes: Grid.ThemeDetail[] = []
    if (!gridData.value.themeRes.dataset) {
        return
    }

    // 所有的它都想看
    if (selectedProduct.value === '全选') {
        // 所有专题、所有产品
        if (selectedProductType.value === '全选') {
            for (const category of gridData.value.themeRes.category) {
                for (const theme of gridData.value.themeRes.dataset[category].dataList) {
                    theme.dataType = category
                    gridAllThemes.push(theme)
                }
            }
        } else {
            // 指定专题、所有产品
            for (const category of gridData.value.themeRes.category) {
                for (const theme of gridData.value.themeRes.dataset[category].dataList) {
                    theme.dataType = category
                    if (gridData.value.themeRes.dataset[category].label === selectedProductType.value) {
                        gridAllThemes.push(theme)
                    }
                }
            }
        }
    } else {
        // 所有专题、指定产品
        if (selectedProductType.value === '全选') {
            for (const category of gridData.value.themeRes.category) {
                for (const theme of gridData.value.themeRes.dataset[category].dataList) {
                    theme.dataType = category
                    if (theme.sensorName === selectedProduct.value) {
                        gridAllThemes.push(theme)
                    }
                }
            }
        } else {
            // 指定专题、指定产品
            for (const category of gridData.value.themeRes.category) {
                for (const theme of gridData.value.themeRes.dataset[category].dataList) {
                    theme.dataType = category
                    if (theme.sensorName === selectedProduct.value) {
                        gridAllThemes.push(theme)
                    }
                }
            }
        }
    }
    // Process each band (R, G, B)
    for (let themeInfo of gridAllThemes) {
        let redPath = ''
        let greenPath = ''
        let bluePath = ''

        // 这里用bandmapper, 确保bandMapper！！！！！
        // console.log(sceneInfo.bandMapper)
        for (let bandImg of themeInfo.images) {
            // 后端返回的BandMapper如果是单波段的话，Red Green 和 Blue相同
            if (themeInfo.bandMapper.Red == bandImg.band) {
                redPath = bandImg.bucket + '/' + bandImg.tifPath
            }
            if (themeInfo.bandMapper.Green == bandImg.band) {
                greenPath = bandImg.bucket + '/' + bandImg.tifPath
            }
            if (themeInfo.bandMapper.Blue == bandImg.band) {
                bluePath = bandImg.bucket + '/' + bandImg.tifPath
            }
        }
        imageData.push({
            sceneId: themeInfo.sceneId,
            sensorName: themeInfo.sensorName,
            productName: themeInfo.sensorName + '-' + themeInfo.productName,
            dataType: themeInfo.dataType,
            time: themeInfo.sceneTime,
            redPath: redPath,
            greenPath: greenPath,
            bluePath: bluePath,
            nodata: themeInfo.noData,
        })
    }
    bus.emit('cubeVisualize', imageData, gridInfo, scaleRate.value, 'product')

    bus.emit('openTimeline')
}

/**
 * 透明度变量
 */
const opacity = ref(0)
const opacityFormatter = (value: number) => {
    return `${value}%`
}
const onAfterOpacityChange = (opacity: number) => {
    console.log(opacity)
}

/**
 * 5. 超分Tab
 */
//超分
type band_path={
    R: string,
    G: string,
    B: string
}
const calTask: Ref<any> = ref({
    calState: 'start',
    taskId: ''
})

const isSuperRes = ref(false)

const handleSuperResolution = async ()=> {
    if (!gridData.value.sceneRes.dataset) {
        return
    }
    if (visualLoad.value){
        isSuperRes.value = !isSuperRes.value 
        try{
            handleRemove()
            let currentScene
            for (const category of gridData.value.sceneRes.category) {
                for (const scene of gridData.value.sceneRes.dataset[category].dataList) {
                    if (scene.sensorName === selectedSensor.value) {
                        currentScene = scene
                    }
                }
            }

            const bands : band_path ={
                R:'',
                G:'',
                B:''
            }
            if (!currentScene) {
                ElMessage.error('未找到对应的数据');
                return;
            }

            currentScene.images.forEach((bandImg) => {
                        if (bandImg.band === selectedRBand.value) {
                            bands.R = bandImg.bucket + '/' + bandImg.tifPath
                        }
                        if (bandImg.band === selectedGBand.value) {
                            bands.G = bandImg.bucket + '/' + bandImg.tifPath
                        }
                        if (bandImg.band === selectedBBand.value) {
                            bands.B = bandImg.bucket + '/' + bandImg.tifPath
                        }
                    })
            const result = await GetSuperResolution({
                columnId: gridData.value.columnId,
                rowId: gridData.value.rowId,
                resolution: gridData.value.resolution,
                band: bands
            });
            
            calTask.value.taskId = result.data
            // 2、轮询运行状态，直到运行完成
            // ✅ 轮询函数，直到 data === 'COMPLETE'
            const pollStatus = async (taskId: string) => {
                console.log('查询报错')
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
                console.log("开始")
                await pollStatus(calTask.value.taskId)
                // ✅ 成功后设置状态
                calTask.value.calState = 'success'
                let bandres = await getCaseBandsResult(calTask.value.taskId)
                console.log(bandres, '结果');
                console.log('超分返回数据',bandres.data)
            // console.log(result.value)
                bus.emit('SuperResTimeLine', bandres.data,isSuperRes.value)
                
            } catch (error) {
                calTask.value.calState = 'failed'

                console.error('有问题');
                console.error('问题double')
            }
            ElMessage.success('超分处理成功')
                
        }catch{
            ElMessage.error('超分处理失败');
        }
    } else{
        ElMessage.error('请先完成立方体可视化')
    }
}

</script>

<style scoped>
.popup-content {
    background-color: #0a1929;
    color: #e6f1ff;
    padding: 0.75rem;
    border-radius: 0.5rem;
    width: 100%;
    max-width: 250px;
    user-select: none;
}

.grid-id {
    /* margin-bottom: 1rem; */
    padding-bottom: 0.5rem;
    /* border-bottom: 1px solid #1e3a5f; */
    text-align: center;
    cursor: move;
}

.grid-id p {
    font-size: 0.9rem;
    font-weight: 600;
    margin: 0;
    color: #7eb3dd;
}

.band-selection {
    display: grid;
    grid-template-columns: 40% 60%;
    margin-bottom: 1rem;
}

.band-selection label {
    display: inline-block;
    margin-right: 1rem;
    height: 2.75rem;
    line-height: 2.75rem;
    font-size: 1rem;
    color: #a5d8ff;
}

.band-select {
    width: 100%;
    padding: 0.625rem;
    background-color: #132f4c;
    color: #e6f1ff;
    border: 1px solid #1e3a5f;
    border-radius: 0.25rem;
    font-size: 0.875rem;
    appearance: none;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%234dabf7' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 0.625rem center;
    padding-right: 2rem;
    max-height: 350px;
}

.band-select:focus {
    outline: none;
    border-color: #4dabf7;
    box-shadow: 0 0 0 2px rgba(77, 171, 247, 0.25);
}

/* Tab styles */
.tabs {
    display: flex;
    margin-bottom: 1rem;
    border-bottom: 1px solid #1e3a5f;
}

.tab-btn {
    flex: 1;
    background: transparent;
    color: #a5d8ff;
    border: none;
    padding: 0.75rem 0;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s ease;
    position: relative;
}

.tab-btn.active {
    color: #4dabf7;
    font-weight: 600;
}

.tab-btn.active::after {
    content: '';
    position: absolute;
    bottom: -1px;
    left: 0;
    width: 100%;
    height: 2px;
    background-color: #4dabf7;
}

.tab-content {
    margin-top: 1rem;
}

.sp {
    height: 2.5rem;
    line-height: 2.5rem;
    font-size: 1rem;
    color: #a5d8ff;
}

.btns {
    display: flex;
    margin-top: 1rem;
}

.visualize-btn {
    width: 80%;
    padding: 0.75rem 0.5rem;
    padding-right: 0;
    background-color: #0c4a6e;
    color: #e6f1ff;
    border: none;
    border-radius: 0.25rem;
    font-size: 0.875rem;
    font-weight: 600;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background-color 0.2s ease;
}

.visualize-btn:hover:not(:disabled) {
    background-color: #075985;
}

.visualize-btn:disabled,
.delete-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.delete-btn {
    width: 20%;
    color: #e6f1ff;
    padding-left: 0.75rem;
    border: none;
    border-radius: 0.25rem;
    font-size: 0.875rem;
    font-weight: 600;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background-color 0.2s ease;
}

.btn-icon {
    margin-right: 0.5rem;
    display: flex;
    align-items: center;
}

.config-container {
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
    padding: 0.5rem;
}

.config-item {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    background-color: rgba(15, 23, 42, 0.3);
    border-radius: 0.5rem;
    padding: 0.75rem;
    border: 1px solid rgba(56, 189, 248, 0.15);
    transition: all 0.3s ease;
}

.config-item:hover {
    background-color: rgba(14, 165, 233, 0.1);
    border-color: rgba(56, 189, 248, 0.3);
    box-shadow: 0 0 10px rgba(56, 189, 248, 0.1);
}

.config-item-no-hover {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    background-color: rgba(15, 23, 42, 0.3);
    border-radius: 0.5rem;
    padding: 0.75rem;
    border: 1px solid rgba(56, 189, 248, 0.15);
    transition: all 0.3s ease;
}

.config-label {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.9rem;
    font-weight: 600;
    color: #e0f2fe;
    margin-bottom: 0.25rem;
}

.vector-tag {
    background-color: #132f4c !important;
    color: #e6f1ff !important;
    border: 1px solid #1e3a5f !important;
    margin-bottom: 6px;
    padding: 0.75rem;
    transition: all 0.2s;
}
:deep(.vector-tag.ant-tag-checkable-checked) {
    background-color: #075985 !important;
    color: #4dabf7 !important;
    border-color: #4dabf7 !important;
}

@media (max-width: 640px) {
    .popup-content {
        padding: 1rem;
    }
}
</style>
