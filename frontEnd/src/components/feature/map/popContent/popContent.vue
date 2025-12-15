<template>
    <Vue3DraggableResizable :draggable="true" :resizable="true" :parent="false" v-model:x="popX" v-model:y="popY"
        v-model:w="popW" v-model:h="popH" :minW="260" :minH="364" :initW="popW" :initH="popH" :lockAspectRatio="true"
        :handles="['br']" classNameHandle="custom-handle" classNameDraggable="draggable-popup" classNameActive="">
        <div class="popup-content" :style="popupContentStyle">
            <button class="popup-cancel-btn" @click="() => bus.emit('gridPopup:closeByUser')">
                <CircleOff :size="16" />
            </button>
            <div class="grid-id">
                <p>时空立方体编号: {{ gridID }}</p>
            </div>

            <div class="tabs">
                <button class="tab-btn" :class="{ active: activeTab === 'scene' }" @click="activeTab = 'scene'">
                    遥感影像
                </button>
                <button class="tab-btn" :class="{ active: activeTab === 'vector' }" @click="activeTab = 'vector'">
                    矢量专题
                </button>
                <button class="tab-btn" :class="{ active: activeTab === 'theme' }" @click="activeTab = 'theme'">
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
                    <select id="sensor-select" v-model="selectedSensor" class="band-select"
                        @change="handleSensorChange(selectedSensor)">
                        <option disabled value="">请选择</option>
                        <option v-for="sensor in sensors" :key="sensor" :value="sensor">
                            {{ sensor }}
                        </option>
                    </select>
                </div>

                <!-- 增强方法Tab -->
                <div class="tabs" v-show="showBandSelector">
                    <button class="tab-btn" :class="{ active: activeMethod === 'rgb' }" @click="activeMethod = 'rgb'">
                        波段增强
                    </button>
                    <button class="tab-btn" :class="{ active: activeMethod === 'superresolution' }"
                        @click="activeMethod = 'superresolution'">
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
                    <button class="visualize-btn" @click="handleSuperResolution">
                        <span class="btn-icon">
                            <GalleryHorizontalIcon :size="18" />
                        </span>
                        超分增强
                    </button>
                    <button class="delete-btn" @click="handleRemove">
                        <span class="btn-icon">
                            <Trash2Icon :size="18" />
                        </span>
                    </button>
                </div>
                <!-- <div class="btns flex justify-center" v-show="showBandSelector && activeMethod === 'superresolution'">
                    <button class="visualize-btn">
                        <span class="btn-icon">
                            <GalleryHorizontalIcon :size="18" />
                        </span>
                        指数增强
                    </button>
                    <button class="delete-btn" @click="handleRemove">
                        <span class="btn-icon">
                            <Trash2Icon :size="18" />
                        </span>
                    </button>
                </div> -->

                <!-- 原亮度拉伸，现为拉伸增强 -->
                <div @mousedown="handleScaleMouseDown" @mouseup="handleScaleMouseUp" v-show="activeMethod === 'rgb'">
                    <div class="band-selection">
                        <label for="r-band-select">拉伸方法:</label>
                        <select id="r-band-select" v-model="selectedStretchMethod" class="band-select">
                            <option disabled value="">请选择</option>
                            <option v-for="method in stretchMethods" :key="method.value" :value="method.value">
                                {{ method.label }}
                            </option>
                        </select>
                    </div>
                    <div class="mr-1 grid grid-cols-[2fr_3fr]">
                        <span class="sp text-white">拉伸增强:</span>
                        <a-slider v-if="selectedStretchMethod === 'linear'" :tip-formatter="(value) => `${value}级`"
                            v-model:value="scaleRate" :min="0" :max="10" :step="1"
                            @afterChange="onAfterScaleRateChange" />
                        <a-slider v-if="selectedStretchMethod === 'gamma'" :tip-formatter="scaleRateFormatter"
                            v-model:value="scaleRate" :min="0.10" :max="10" :step="0.01"
                            @afterChange="onAfterScaleRateChange" />
                        <a-slider v-if="selectedStretchMethod === 'standard'" :tip-formatter="(value) => `${value}σ`"
                            v-model:value="scaleRate" :min="0" :max="3" :step="1"
                            @afterChange="onAfterScaleRateChange" />
                    </div>
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
                <div class="band-selection">
                    <label for="sensor-select">数据集:</label>
                    <select id="sensor-select" v-model="selectedVector" class="band-select">
                        <option disabled value="">请选择</option>
                        <option v-for="(item, index) in gridData.vectors" :key="item.tableName" :value="item">
                            {{ item.vectorName }}
                        </option>
                    </select>
                </div>
                <div class="config-container" v-if="gridVectorSymbology[selectedVector.tableName]">
                    <div class="flex justify-between">
                        <div>
                            <label class="mr-2 text-white">字段：</label>
                            <select
                                class="max-h-[600px] w-[120px] appearance-none truncate rounded-lg border border-[#2c3e50] bg-[#0d1526] px-3 py-1 text-[#38bdf8] hover:border-[#2bb2ff] focus:border-[#3b82f6] focus:outline-none"
                                v-model="gridVectorSymbology[selectedVector.tableName].selectedField">
                                <option disabled selected value="">
                                    请选择
                                </option>
                                <!-- <option :value="'all'" class="truncate">全选</option> -->
                                <option v-for="f in selectedVector.fields" :value="f" :key="f" class="truncate">
                                    {{ f }}
                                </option>
                            </select>
                        </div>
                        <a-button type="primary"
                            @click="getAttrs4CustomField(selectedVector.tableName, gridVectorSymbology[selectedVector.tableName].selectedField)">获取</a-button>
                    </div>
                    <div class="flex items-center justify-between gap-2">
                        <el-checkbox v-model="gridVectorSymbology[selectedVector.tableName].checkAll"
                            :indeterminate="gridVectorSymbology[selectedVector.tableName].isIndeterminate"
                            @change="(val) => handleCheckAllChange(selectedVector.tableName, val as boolean)">
                            <template #default>
                                <span class="text-white">全选</span>
                            </template>
                        </el-checkbox>
                    </div>
                    <div class="w-full max-h-[248px] overflow-y-auto">
                        <el-checkbox-group v-model="gridVectorSymbology[selectedVector.tableName].checkedAttrs"
                            @change="(val) => handleCheckedAttrsChange(selectedVector.tableName, val as string[])">
                            <template v-if="gridVectorSymbology[selectedVector.tableName].attrs.length">
                                <div v-for="(attr, attrIndex) in gridVectorSymbology[selectedVector.tableName].attrs"
                                    :key="attrIndex"
                                    class="flex items-center justify-between bg-[#01314e] px-3 mb-1.5 py-2 rounded">
                                    <div class="flex items-center gap-2">
                                        <el-checkbox class="config-label mt-1" :key="attr.type" :label="attr.label">
                                            <template default></template>
                                        </el-checkbox>
                                        <span class="config-label mt-1">{{ attr.label }}</span>
                                    </div>
                                    <el-color-picker v-model="attr.color" size="small" show-alpha
                                        :predefine="predefineColors" />
                                </div>
                            </template>
                        </el-checkbox-group>
                    </div>
                    <!-- <span class="result-info-label">共找到 {{gridData.vectors.length}} 条记录</span>
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
                    </a-checkable-tag> -->
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
                    <select id="sensor-select" v-model="selectedProduct" class="band-select"
                        @change="handleSensorChange(selectedProduct)">
                        <option disabled value="">请选择</option>
                        <option v-for="product in products" :key="product" :value="product">
                            {{ product }}
                        </option>
                    </select>
                </div>

                <!-- 透明度 -->
                <div class="mr-1 grid grid-cols-[2fr_3fr]" @mousedown="handleScaleMouseDown"
                    @mouseup="handleScaleMouseUp">
                    <span class="sp text-white">透明度:</span>
                    <a-slider :tip-formatter="opacityFormatter" v-model:value="opacity"
                        @afterChange="onAfterOpacityChange" />
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
import { ElCheckbox, ElCheckboxGroup, ElColorPicker } from 'element-plus'
import { ref, computed, onMounted, onUnmounted, type Ref, reactive } from 'vue'
import { DatabaseIcon, GalleryHorizontalIcon, RectangleEllipsisIcon, Trash2Icon, CircleOff } from 'lucide-vue-next'
import bus from '@/store/bus'
import Vue3DraggableResizable from 'vue3-draggable-resizable'
import 'vue3-draggable-resizable/dist/Vue3DraggableResizable.css'
import { ezStore } from '@/store'
import { getThemesInGrid } from '@/api/http/interactive-explore/grid.api'
import { getScenesInGrid } from '@/api/http/interactive-explore/grid.api'
import * as GridExploreMapOps from '@/util/map/operation/grid-explore'
import {
    activeTab, gridData, canVisualize, showBandSelector,
    activeMethod, gridID
} from './shared'
import { useGridScene } from './useGridScene'
import { useGridVector } from './useGridVector'
import { useGridTheme } from './useGridTheme'
import { useSuperResolution } from './useSuperResolution'

// Popup position and size state
const popX = ref(0)
const popY = ref(0)
const popW = ref(300)
const popH = ref(420)
const VIEW_MARGIN = 16

// Base dimensions for the popup content
const baseWidth = 300
const baseHeight = 420

// Calculate zoom factor based on current dimensions
const zoomFactor = computed(() => {
    // Use width for scaling since we have lockAspectRatio=true
    return popW.value / baseWidth
})

// Computed style using zoom for true proportional scaling
const popupContentStyle = computed(() => ({
    width: '100%',
    height: '100%',
    zoom: zoomFactor.value
}))

function resetToBottomRightFullyVisible() {
    const vw = window.innerWidth
    const vh = window.innerHeight
    const w = Math.min(popW.value || 300, vw - VIEW_MARGIN * 2)
    const h = Math.min(popH.value || 420, vh - VIEW_MARGIN * 2)
    popW.value = w
    popH.value = h
    popX.value = Math.max(0, vw - w - VIEW_MARGIN)
    popY.value = Math.max(0, vh - h - VIEW_MARGIN)
}

// Set initial position before first paint so it does not flash at (0,0)
if (typeof window !== 'undefined') {
    // Use default popW/popH to compute a safe initial bottom-right position
    const vw = window.innerWidth
    const vh = window.innerHeight
    const w = Math.min(popW.value || 300, vw - VIEW_MARGIN * 2)
    const h = Math.min(popH.value || 420, vh - VIEW_MARGIN * 2)
    popW.value = w
    popH.value = h
    popX.value = Math.max(0, vw - w - VIEW_MARGIN)
    popY.value = Math.max(0, vh - h - VIEW_MARGIN)
}

function measureAndReset() {
    try {
        const el = document.querySelector('.popup-content') as HTMLElement
        if (el) {
            popW.value = Math.min(
                Math.max(el.offsetWidth || 300, 260),
                window.innerWidth - VIEW_MARGIN * 2
            )
            const desiredH = Math.min(
                Math.max((el.scrollHeight || el.offsetHeight || 420), 200),
                window.innerHeight - VIEW_MARGIN * 2
            )
            popH.value = desiredH
        }
    } catch { }
    resetToBottomRightFullyVisible()
}

// Window resize handler
function handleWindowResize() {
    const vw = window.innerWidth
    const vh = window.innerHeight
    popW.value = Math.min(popW.value, vw - VIEW_MARGIN * 2)
    popH.value = Math.min(popH.value, vh - VIEW_MARGIN * 2)
    popX.value = Math.min(Math.max(0, popX.value), vw - popW.value)
    popY.value = Math.min(Math.max(0, popY.value), vh - popH.value)
}

/**
 * 1-1.可视化函数
 */
const handleVisualize = () => {
    switch (activeTab.value) {
        case 'scene':
            handleSceneVisualize()
            break
        case 'vector':
            handleVectorVisualize()
            break
        case 'theme':
            handleProductVisualize()
            break
    }
}
/**
 * 1-2.删除可视化
 */
const handleRemove = () => {
    previewIndex.value = null
    // GridExploreMapOps.map_destroyGridDEMLayer(gridData.value)
    GridExploreMapOps.map_destroyGrid2DDEMLayer(gridData.value)
    GridExploreMapOps.map_destroyGridMVTLayerByGrid(gridData.value)
    // 清理矢量图层的事件监听器
    cleanupGridVectorEvents()
    GridExploreMapOps.map_destroyGrid3DLayer(gridData.value)
    GridExploreMapOps.map_destroyGridNDVIOrSVRLayer(gridData.value)
    // 清除超分状态，确保不会重新加载
    if (isSuperRes.value) {
        isSuperRes.value = false
    }
    GridExploreMapOps.map_destroySuperResolution(gridData.value)
    bus.emit('gridPopup:visible', false)
}
/**
 * 1-3.初始化网格
 */
// 请求锁
let themeResLoading = false
const handleInitGrid = async (info) => {
    if (themeResLoading) return
    themeResLoading = true
    try {
        gridData.value = {
            ...info,  // 拷贝原有属性
            vectors: ezStore.get('vectorStats'),
            themeRes: await getThemesInGrid(info),
            sceneRes: await getScenesInGrid(info)
        }
        let timer = setInterval(() => {
            const data = ezStore.get("vectorSymbology");
            if (data) {
                gridVectorSymbology.value = JSON.parse(JSON.stringify(data)); // 深拷贝
                console.log("✅ vectorSymbology 初始化成功");
                clearInterval(timer); // 成功后停止重试
            } else {
                console.log("⏳ vectorSymbology 未准备好，继续重试...");
            }
        }, 500);
        // gridVectorSymbology.value = JSON.parse(JSON.stringify(ezStore.get("vectorSymbology"))) // 深拷贝
        console.log(gridData.value, 'gridData')
        // after console.log(gridData.value, 'gridData') and before themeResLoading=false:
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                measureAndReset()
            })
        })
    } finally {
        themeResLoading = false
    }
}
bus.on('update:gridPopupData', handleInitGrid)

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

    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            measureAndReset()
        })
    })
    bus.on('gridPopup:reset-position', () => {
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                measureAndReset()
            })
        })
    })

    // Listen for visibility changes and reset position
    bus.on('gridPopup:visible', (visible: boolean) => {
        if (!visible) return
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                measureAndReset()
            })
        })
    })

    // Add window resize listener
    window.addEventListener('resize', handleWindowResize)
})

onUnmounted(() => {
    bus.off('update:gridPopupData', handleInitGrid)
    // Note: bus.off() requires both event and handler, so we only remove those we have handlers for
    window.removeEventListener('resize', handleWindowResize)
})

/**
 * 2. 遥感影像Tab
 */
const { // ------------------------------ 1. 分辨率选项 ------------------------------//
    selectedResolution, resolutions,
    // ------------------------------ 2. 传感器选项 ------------------------------//
    selectedSensor, sensors, handleSensorChange,
    // ------------------------------ 3. 波段选项 ------------------------------//
    selectedBand, selectedRBand, selectedGBand, selectedBBand, bands,
    // ------------------------------ 4. 拉伸增强选项 ------------------------------//
    handleScaleMouseDown, handleScaleMouseUp, scaleRateFormatter, onAfterScaleRateChange, scaleRate, enableDraggable, stretchMethods, selectedStretchMethod,
    // ------------------------------ 5. 立方体可视化 ------------------------------//
    handleSceneVisualize } = useGridScene()

/**
 * 3. 矢量Tab
 */
const { // ------------------------------ 1. 矢量符号化 ------------------------------//
    handleCheckAllChange, handleCheckedAttrsChange, predefineColors, gridVectorSymbology,
    // ------------------------------ 2. 矢量选择 -------------------------------//
    previewIndex, selectedVector,
    // ------------------------------ 3. 立方体可视化 ------------------------------//
    handleVectorVisualize, cleanupGridVectorEvents, getAttrs4CustomField } = useGridVector()

/**
 * 4. 栅格专题Tab
 */
const { // ------------------------------ 1. 产品选项 ------------------------------//
    selectedProductType, selectedProduct, productTypes, products,
    // ------------------------------ 2. 透明度选项 ------------------------------//
    opacityFormatter, onAfterOpacityChange, opacity,
    // ------------------------------ 3. 立方体可视化 ------------------------------//
    handleProductVisualize } = useGridTheme()


/**
 * 5. 超分Tab
 */
const { handleSuperResolution, isSuperRes } = useSuperResolution()

</script>

<style scoped>
.popup-content {
    background-color: #0a1929;
    color: #e6f1ff;
    padding: 0.75rem;
    border-radius: 0.5rem;
    overflow: auto;
    user-select: none;
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    box-sizing: border-box;
}

.popup-cancel-btn {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.2);
    color: #ffffff;
    cursor: pointer;
    padding: 0.25rem;
    border-radius: 0.25rem;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
    z-index: 10;
}

.popup-cancel-btn:hover {
    background-color: rgba(255, 59, 48, 0.2);
    border-color: rgba(255, 59, 48, 0.4);
    color: #ff3b30;
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

<style>
/* Global styles for Vue3DraggableResizable */
.vdr,
.draggable-popup {
    border: none !important;
    outline: none !important;
}

.vdr:before,
.vdr:after,
.draggable-popup:before,
.draggable-popup:after {
    display: none !important;
}

/* Hide all resize handles except bottom-right */
.vdr-handle {
    display: none !important;
}

.vdr-handle-br {
    display: block !important;
    background: none !important;
    border: none !important;
    width: 20px !important;
    height: 20px !important;
    right: 0 !important;
    bottom: 0 !important;
    cursor: nwse-resize !important;
    z-index: 100 !important;
}

/* Custom resize handle appearance */
.vdr-handle-br::after {
    content: '';
    position: absolute;
    bottom: 3px;
    right: 3px;
    width: 0;
    height: 0;
    border-style: solid;
    border-width: 0 0 10px 10px;
    border-color: transparent transparent #4dabf7 transparent;
}

/* Remove all selection borders and outlines */
.vdr.active,
.vdr.active:before {
    border: none !important;
    outline: none !important;
}

/* Remove focus and active state styles */
.vdr:focus {
    outline: none !important;
    border: none !important;
}
</style>