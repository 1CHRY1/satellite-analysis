<template>
    <Vue3DraggableResizable :draggable="enableDraggable" :resizable="false" :initW="250">
        <div class="popup-content">
            <div class="grid-id">
                <p>时空立方体编号: {{ gridID }}</p>
            </div>

            <div class="tabs">
                <button
                    class="tab-btn"
                    :class="{ active: activeTab === 'RS' }"
                    @click="activeTab = 'RS'"
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
                    :class="{ active: activeTab === 'product' }"
                    @click="activeTab = 'product'"
                >
                    栅格专题
                </button>
            </div>

            <!-- 影像Tab -->
            <div v-show="activeTab === 'RS'">
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

                <!-- 拉伸增强Tab -->
                <!-- <div v-show="showBandSelector && activeMethod === 'single'" class="tab-content">
                    <div class="band-selection">
                        <label for="band-select">波段:</label>
                        <select id="band-select" v-model="selectedBand" class="band-select">
                            <option disabled value="">请选择</option>
                            <option v-for="band in bands" :key="band" :value="band">
                                {{ band }}
                            </option>
                        </select>
                    </div>
                </div> -->

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
                    <button class="visualize-btn">
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
                    @mousedown="handleMouseDown"
                    @mouseup="handleMouseUp"
                    v-show="activeMethod === 'rgb' || activeMethod === 'single'"
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
                <div class="btns" v-show="activeMethod === 'rgb' || activeMethod === 'single'">
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
                    <span class="result-info-label">共找到 {{vectors.length}} 条记录</span>
                    <a-checkable-tag
                        v-for="(item, index) in vectors"
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
            <div v-show="activeTab === 'product'">
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
                    @mousedown="handleMouseDown"
                    @mouseup="handleMouseUp"
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
import { DatabaseIcon, GalleryHorizontalIcon, RectangleEllipsisIcon, Trash2Icon } from 'lucide-vue-next'
import bus from '@/store/bus'
import { map_destroyGridRGBImageTileLayer } from '@/util/map/operation'
import Vue3DraggableResizable from 'vue3-draggable-resizable'
import 'vue3-draggable-resizable/dist/Vue3DraggableResizable.css'
import { ezStore } from '@/store'
import * as MapOperation from '@/util/map/operation'
/////// Types //////////////////////////////////
type Image = {
    bucket: string
    tifPath: string
    band: string
}

type Scene = {
    bucket: string
    cloudPath: string
    sceneId: string
    sceneTime: string
    sensorName: string
    productName: string
    resolution: string
    dataType: string
    bandMapper: {
        Red: string
        Green: string
        Blue: string
        NIR: string
    }
    images: Image[]
    noData: number
}

export type GridData = {
    rowId: number
    columnId: number
    resolution: number
    scenes: Scene[]
    vectors?: any[]
}

type ImageInfoType = {
    sceneId: string
    time: string
    tifFullPath: string
    nodata: number
}

type MultiImageInfoType = {
    sceneId: string
    time: string
    sensorName: string
    productName: string
    dataType: string
    redPath: string
    greenPath: string
    bluePath: string
    nodata: number
}

type GridInfoType = {
    rowId: number
    columnId: number
    resolution: number,
    opacity?: number
}

/////// Main //////////////////////////////////
const activeTab = ref('RS')
const canVisualize = computed(() => {
    switch (activeTab.value) {
        case 'RS':
            if (showBandSelector.value === false) return true
            if (activeMethod.value === 'single') {
                return !!selectedBand.value
            } else if (activeMethod.value === 'rgb') {
                return !!selectedRBand.value && !!selectedGBand.value && !!selectedBBand.value
            }
            break
        case 'vector':
            // return !!selectedResolution.value
            break
        case 'product':
            return !!selectedProductType.value && !!selectedProduct.value
            break
    }
})

/**
 * 影像相关
 */
const gridData = ref<GridData>({
    rowId: 0,
    columnId: 0,
    resolution: 0,
    scenes: [],
    vectors: [],
})
const activeMethod = ref('rgb')

const showBandSelector = ref(true)
const enableDraggable = ref(true)
const scaleRate = ref(0)
const scaleRateFormatter = (value: number) => {
    return `${value}级`
}
const opacity = ref(0)
const opacityFormatter = (value: number) => {
    return `${value}%`
}
const showingImageStrech = reactive({
    r_min: 0,
    r_max: 5000,
    g_min: 0,
    g_max: 5000,
    b_min: 0,
    b_max: 5000,
})

const handleMouseUp = () => (enableDraggable.value = true)

const handleMouseDown = () => (enableDraggable.value = false)

const onAfterScaleRateChange = (scale_rate: number) => {
    console.log(scale_rate)
}

const onAfterOpacityChange = (opacity: number) => {
    console.log(opacity)
}

// Select options
const gridID = computed(() => {
    const { rowId, columnId, resolution } = gridData.value
    return `${rowId}-${columnId}-${resolution}`
})

const resolutions = computed(() => {
    const result = new Set<string>()
    // result.add('all')
    gridData.value.scenes.forEach((scene: Scene) => {
        if (scene.dataType === 'satellite') {
            result.add(scene.resolution)
        }
    })
    const arr = Array.from(result)
    arr.sort((a, b) => {
        return Number(b) - Number(a)
    })

    return ['全选', ...arr]
})

const handleSensorChange = (value) => {
    if (value === '全选') {
        showBandSelector.value = false
    } else {
        showBandSelector.value = true
    }
}

const sensors = computed(() => {
    let result = new Set<string>()
    result.add('全选')
    gridData.value.scenes.forEach((scene: Scene) => {
        if (scene.dataType === 'satellite') {
            if (selectedResolution.value != '全选' && scene.resolution == selectedResolution.value) {
                result.add(scene.sensorName)
            } else if (selectedResolution.value === '全选') {
                result.add(scene.sensorName)
            }
        }
    })
    return Array.from(result)
})

const bands = computed(() => {
    let result: string[] = []

    if (selectedSensor.value != '') {
        gridData.value.scenes.forEach((scene: Scene) => {
            if (selectedSensor.value && scene.sensorName === selectedSensor.value) {
                scene.images.forEach((bandImg: Image) => {
                    if (!result.includes(bandImg.band)) {
                        result.push(bandImg.band)
                    }
                })
            }
        })
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

// Form state
const selectedResolution = ref('')
const selectedSensor = ref('')
const selectedBand = ref('')
const selectedRBand = ref('')
const selectedGBand = ref('')
const selectedBBand = ref('')

// Handle visualization
const handleRSVisualize = () => {
    const { rowId, columnId, resolution } = gridData.value
    const gridInfo: GridInfoType = {
        rowId,
        columnId,
        resolution,
    }
    // 所有的它都想看
    if (showBandSelector.value === false) {
        const rgbImageData: MultiImageInfoType[] = []

        const gridAllScenes: Scene[] = []
        // gridData.value.scenes
        if (selectedResolution.value && selectedResolution.value !== '全选') {
            for (const scene of gridData.value.scenes) {
                if (scene.resolution === selectedResolution.value) {
                    gridAllScenes.push(scene)
                }
            }
        } else {
            gridAllScenes.push(...gridData.value.scenes)
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
                if (sceneInfo.bandMapper.Red === bandImg.band) {
                    redPath = bandImg.bucket + '/' + bandImg.tifPath
                }
                if (sceneInfo.bandMapper.Green === bandImg.band) {
                    greenPath = bandImg.bucket + '/' + bandImg.tifPath
                }
                if (sceneInfo.bandMapper.Blue === bandImg.band) {
                    bluePath = bandImg.bucket + '/' + bandImg.tifPath
                }
            }
            rgbImageData.push({
                sceneId: sceneInfo.sceneId,
                sensorName: sceneInfo.sensorName,
                productName: sceneInfo.productName + '-' + sceneInfo.resolution,
                dataType: sceneInfo.dataType,
                time: sceneInfo.sceneTime,
                redPath: redPath,
                greenPath: greenPath,
                bluePath: bluePath,
                nodata: sceneInfo.noData,
            })
        }
        bus.emit('cubeVisualize', rgbImageData, gridInfo, scaleRate.value, 'rgb')
    } else {
        if (activeMethod.value === 'single') {
            // Single band visualization
            const imageData: ImageInfoType[] = []
            for (let scene of gridData.value.scenes) {
                if (scene.sensorName == selectedSensor.value) {
                    scene.images.forEach((bandImg: Image) => {
                        if (bandImg.band === selectedBand.value) {
                            imageData.push({
                                tifFullPath: bandImg.bucket + '/' + bandImg.tifPath,
                                sceneId: scene.sceneId,
                                time: scene.sceneTime,
                                nodata: scene.noData,
                            })
                        }
                    })
                }
            }

            bus.emit('cubeVisualize', imageData, gridInfo, scaleRate.value, 'single')
        } else {
            const rgbImageData: MultiImageInfoType[] = []

            const filteredScene = gridData.value.scenes.filter((scene) => {
                if (selectedResolution.value != '全选')
                    return (
                        scene.resolution === selectedResolution.value &&
                        scene.sensorName === selectedSensor.value
                    )
                else return scene.sensorName === selectedSensor.value
            })

            // Process each band (R, G, B)
            for (let scene of filteredScene) {
                let redPath = ''
                let greenPath = ''
                let bluePath = ''

                // 这里用用户选的
                scene.images.forEach((bandImg: Image) => {
                    if (bandImg.band === selectedRBand.value) {
                        redPath = bandImg.bucket + '/' + bandImg.tifPath
                    }
                    if (bandImg.band === selectedGBand.value) {
                        greenPath = bandImg.bucket + '/' + bandImg.tifPath
                    }
                    if (bandImg.band === selectedBBand.value) {
                        bluePath = bandImg.bucket + '/' + bandImg.tifPath
                    }
                })

                rgbImageData.push({
                    sceneId: scene.sceneId,
                    time: scene.sceneTime,
                    sensorName: scene.sensorName,
                    productName: scene.productName + '-' + scene.resolution,
                    dataType: scene.dataType,
                    redPath: redPath,
                    greenPath: greenPath,
                    bluePath: bluePath,
                    nodata: scene.noData,
                })
            }
            bus.emit('cubeVisualize', rgbImageData, gridInfo, scaleRate.value, 'rgb')
        }
    }

    bus.emit('openTimeline')
}

const previewList = computed<boolean[]>(() => {
    const list = Array(vectors.value.length).fill(false)
    if (previewIndex.value !== null) {
        list[previewIndex.value] = true
    }
    return list
})
const previewIndex = ref<number | null>(null)
const handleSelectVector = (index: number) => {
  previewIndex.value = previewIndex.value === index ? null : index
}
const unPreview = () => {
    
    MapOperation.map_destroyGridMVTLayer()
}
const handleVectorVisualize = (tableName: string) => {
    previewIndex.value = vectors.value.findIndex((item) => item.tableName === tableName)
    MapOperation.map_addGridMVTLayer(tableName, gridData.value.columnId, gridData.value.rowId, gridData.value.resolution)
}

const handleProductVisualize = () => {
    const { rowId, columnId, resolution } = gridData.value
    const gridInfo: GridInfoType = {
        rowId,
        columnId,
        resolution,
        opacity: opacity.value,
    }
    const imageData: MultiImageInfoType[] = []

    const gridAllScenes: Scene[] = []
    // 所有的它都想看
    if (selectedProductType.value === '全选') {
        if (selectedProduct.value === '全选') {
            gridAllScenes.push(...gridData.value.scenes)
        } else {
            for (const scene of gridData.value.scenes) {
                if (scene.sensorName === selectedProduct.value) {
                    gridAllScenes.push(scene)
                }
            }
        }
    } else {
        let dataType = ''
        switch (selectedProductType.value) {
            case 'DEM':
                dataType = 'dem'
                break
            case '红绿立体影像':
                dataType = '3d'
                break
            case '形变速率':
                dataType = 'svr'
                break
            case 'NDVI':
                dataType = 'ndvi'
                break
            default:
                dataType = 'others'
                break
        }
        for (const scene of gridData.value.scenes) {
            if (dataType === scene.dataType && selectedProduct.value === '全选') {
                gridAllScenes.push(scene)
            } else if (dataType === scene.dataType && selectedProduct.value === scene.sensorName) {
                gridAllScenes.push(scene)
            }
        }
    }
    // Process each band (R, G, B)
    for (let sceneInfo of gridAllScenes) {
        let redPath = ''
        let greenPath = ''
        let bluePath = ''

        // 这里用bandmapper, 确保bandMapper！！！！！
        // console.log(sceneInfo.bandMapper)
        for (let bandImg of sceneInfo.images) {
            // 后端返回的BandMapper如果是单波段的话，Red Green 和 Blue相同
            if (sceneInfo.bandMapper.Red === bandImg.band) {
                redPath = bandImg.bucket + '/' + bandImg.tifPath
            }
            if (sceneInfo.bandMapper.Green === bandImg.band) {
                greenPath = bandImg.bucket + '/' + bandImg.tifPath
            }
            if (sceneInfo.bandMapper.Blue === bandImg.band) {
                bluePath = bandImg.bucket + '/' + bandImg.tifPath
            }
        }
        imageData.push({
            sceneId: sceneInfo.sceneId,
            sensorName: sceneInfo.sensorName,
            productName: sceneInfo.productName + '-' + sceneInfo.resolution,
            dataType: sceneInfo.dataType,
            time: sceneInfo.sceneTime,
            redPath: redPath,
            greenPath: greenPath,
            bluePath: bluePath,
            nodata: sceneInfo.noData,
        })
    }
    bus.emit('cubeVisualize', imageData, gridInfo, scaleRate.value, 'product')

    bus.emit('openTimeline')
}

const handleVisualize = () => {
    switch (activeTab.value) {
        case 'RS':
            handleRSVisualize()
            break
        case 'vector':
            if (previewIndex.value !== null) {
                handleVectorVisualize(vectors.value[previewIndex.value].tableName)
            }
            break
        case 'product':
            handleProductVisualize()
            break
    }
}

const handleRemove = () => {
    previewIndex.value = null
    MapOperation.map_destroyGridRGBImageTileLayer(gridData.value)
    MapOperation.map_destroyGridMVTLayer()
    MapOperation.map_destroyGridOneBandColorTileLayer(gridData.value)
}

/**
 * 矢量相关
 */

/**
 * 产品相关
 */
const selectedProductType = ref('')
const selectedProduct = ref('')
const productTypes = computed(() => {
    return ['全选', /*'DEM',*/ '红绿立体影像', '形变速率', 'NDVI', '其他']
})

const products = computed(() => {
    let result = new Set<string>()
    result.add('全选')
    gridData.value.scenes.forEach((scene: Scene) => {
        if (scene.dataType !== 'satellite') {
            if (selectedProductType.value != '全选') {
                let dataType = 'others'
                switch (scene.dataType) {
                    case 'dem':
                        dataType = 'DEM'
                        break
                    case '3d':
                        dataType = '红绿立体影像'
                        break
                    case 'svr':
                        dataType = '形变速率'
                        break
                    case 'ndvi':
                        dataType = 'NDVI'
                        break
                    default:
                        dataType = '其他'
                        break
                }
                if (dataType === selectedProductType.value) {
                    result.add(scene.sensorName)
                }
            } else if (selectedProductType.value === '全选') {
                result.add(scene.sensorName)
            }
        }
    })
    return Array.from(result)
})
const vectors = ref<any[]>([])


bus.on('update:gridPopupData', (info, vectorGridsRes) => {
    // vectorGridsRes 是附带的矢量数据，只不过过滤放在了这里，不太科学
    gridData.value = info
    let gridRes = vectorGridsRes.filter((item: any) => {
        return item.rowId === info.rowId && item.columnId === info.columnId
    })
    if (gridRes.length > 0) {
        vectors.value = gridRes[0].vectors
    }
    console.log(vectors.value, 'vectors')
})

onMounted(() => {
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
