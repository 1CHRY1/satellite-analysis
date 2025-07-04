<template>
    <Vue3DraggableResizable :draggable="enableDraggable" :resizable="false" :initW="250">
        <div class="popup-content">
            <div class="grid-id">
                <p>时空立方体编号: {{ gridID }}</p>
            </div>

            <div class="band-selection">
                <label for="resolution-select">分辨率:</label>
                <select id="resolution-select" v-model="selectedResolution" class="band-select">
                    <option disabled value="">请选择</option>
                    <option v-for="reso in resolutions" :key="reso" :value="reso">
                        {{ reso }}
                    </option>
                </select>
            </div>

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

            <div class="tabs" v-show="showBandSelector">
                <button
                    class="tab-btn"
                    :class="{ active: activeTab === 'single' }"
                    @click="activeTab = 'single'"
                >
                    单波段
                </button>
                <button
                    class="tab-btn"
                    :class="{ active: activeTab === 'rgb' }"
                    @click="activeTab = 'rgb'"
                >
                    三波段合成
                </button>
            </div>

            <div v-show="showBandSelector && activeTab === 'single'" class="tab-content">
                <div class="band-selection">
                    <label for="band-select">波段:</label>
                    <select id="band-select" v-model="selectedBand" class="band-select">
                        <option disabled value="">请选择</option>
                        <option v-for="band in bands" :key="band" :value="band">
                            {{ band }}
                        </option>
                    </select>
                </div>
            </div>

            <div v-show="showBandSelector && activeTab === 'rgb'" class="tab-content">
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
            <div
                class="mr-1 grid grid-cols-[2fr_3fr]"
                @mousedown="handleMouseDown"
                @mouseup="handleMouseUp"
            >
                <span class="sp text-white">亮度拉伸:</span>
                <a-slider
                    :tip-formatter="scaleRateFormatter"
                    v-model:value="scaleRate"
                    @afterChange="onAfterScaleRateChange"
                />
            </div>

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
    </Vue3DraggableResizable>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, type Ref, reactive } from 'vue'
import { GalleryHorizontalIcon, Trash2Icon } from 'lucide-vue-next'
import bus from '@/store/bus'
import { map_destroyGridRGBImageTileLayer } from '@/util/map/operation'
import Vue3DraggableResizable from 'vue3-draggable-resizable'
import 'vue3-draggable-resizable/dist/Vue3DraggableResizable.css'
import { ezStore } from '@/store'

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
    productName: string
    redPath: string
    greenPath: string
    bluePath: string
    nodata: number
}

type GridInfoType = {
    rowId: number
    columnId: number
    resolution: number
}

/////// Main //////////////////////////////////

const gridData = ref<GridData>({
    rowId: 0,
    columnId: 0,
    resolution: 0,
    scenes: [],
})

const activeTab = ref('single')
const showBandSelector = ref(true)
const enableDraggable = ref(true)
const scaleRate = ref(50)
const scaleRateFormatter = (value: number) => {
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

// Select options
const gridID = computed(() => {
    const { rowId, columnId, resolution } = gridData.value
    return `${rowId}-${columnId}-${resolution}`
})

const resolutions = computed(() => {
    const result = new Set<string>()
    // result.add('all')
    gridData.value.scenes.forEach((scene: Scene) => {
        result.add(scene.resolution)
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
        if (selectedResolution.value != '全选' && scene.resolution == selectedResolution.value) {
            result.add(scene.sensorName)
        } else if (selectedResolution.value === '全选') {
            result.add(scene.sensorName)
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

const canVisualize = computed(() => {
    if (showBandSelector.value === false) return true
    if (activeTab.value === 'single') {
        return !!selectedBand.value
    } else if (activeTab.value === 'rgb') {
        return !!selectedRBand.value && !!selectedGBand.value && !!selectedBBand.value
    }
})

// Form state
const selectedResolution = ref('')
const selectedSensor = ref('')
const selectedBand = ref('')
const selectedRBand = ref('')
const selectedGBand = ref('')
const selectedBBand = ref('')

// Handle visualization
const handleVisualize = () => {
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
                productName: sceneInfo.productName + '-' + sceneInfo.resolution,
                time: sceneInfo.sceneTime,
                redPath: redPath,
                greenPath: greenPath,
                bluePath: bluePath,
                nodata: sceneInfo.noData,
            })
        }
        bus.emit('cubeVisualize', rgbImageData, gridInfo, scaleRate.value, 'rgb')
    } else {
        if (activeTab.value === 'single') {
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
                    productName: scene.productName + '-' + scene.resolution,
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

const handleRemove = () => {
    map_destroyGridRGBImageTileLayer(gridData.value)
}

bus.on('update:gridPopupData', (info) => {
    gridData.value = info
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
    margin-bottom: 1rem;
    padding-bottom: 0.75rem;
    border-bottom: 1px solid #1e3a5f;
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

@media (max-width: 640px) {
    .popup-content {
        padding: 1rem;
    }
}
</style>
