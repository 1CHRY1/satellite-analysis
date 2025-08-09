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
                <div class="band-selection">
                    <label for="sensor-select">数据集:</label>
                    <select
                        id="sensor-select"
                        v-model="selectedVector"
                        class="band-select"
                    >
                        <option disabled value="">请选择</option>
                        <option v-for="(item, index) in gridData.vectors" :key="item.tableName" :value="item">
                            {{ item.vectorName }}
                        </option>
                    </select>
                </div>
                <div class="config-container" v-if="gridVectorSymbology[selectedVector.tableName]">
                    <div class="flex items-center justify-between gap-2">
                        <el-checkbox
                            v-model="gridVectorSymbology[selectedVector.tableName].checkAll"
                            :indeterminate="gridVectorSymbology[selectedVector.tableName].isIndeterminate"
                            @change="(val) => handleCheckAllChange(selectedVector.tableName, val as boolean)">
                            <template #default>
                                <span class="text-white">全选</span>
                            </template>
                        </el-checkbox>
                    </div>
                    <el-checkbox-group v-model="gridVectorSymbology[selectedVector.tableName].checkedAttrs"
                        @change="(val) => handleCheckedAttrsChange(selectedVector.tableName, val as string[])" >
                        <template v-if="gridVectorSymbology[selectedVector.tableName].attrs.length">
                            <div v-for="(attr, attrIndex) in gridVectorSymbology[selectedVector.tableName].attrs"
                                :key="attrIndex"
                                class="flex items-center justify-between bg-[#01314e] px-3 mb-1.5 py-2 rounded">
                                <div class="flex items-center gap-2">
                                    <el-checkbox class="config-label mt-1" :key="attr.type" :label="attr.label" >
                                        <template default></template>
                                    </el-checkbox>
                                    <span class="config-label mt-1">{{ attr.label }}</span>
                                </div>
                                <el-color-picker v-model="attr.color" size="small"
                                    show-alpha :predefine="predefineColors" />
                            </div>
                        </template>
                    </el-checkbox-group>
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
import { ElCheckbox, ElCheckboxGroup, ElColorPicker } from 'element-plus'
import { ref, computed, onMounted, onUnmounted, type Ref, reactive } from 'vue'
import { DatabaseIcon, GalleryHorizontalIcon, RectangleEllipsisIcon, Trash2Icon,CircleOff } from 'lucide-vue-next'
import bus from '@/store/bus'
import Vue3DraggableResizable from 'vue3-draggable-resizable'
import 'vue3-draggable-resizable/dist/Vue3DraggableResizable.css'
import { ezStore } from '@/store'
import { getThemesInGrid } from '@/api/http/interactive-explore/grid.api'
import { getScenesInGrid } from '@/api/http/interactive-explore/grid.api'
import * as GridExploreMapOps from '@/util/map/operation/grid-explore'
import { activeTab, gridData, canVisualize, showBandSelector,
      activeMethod, gridID } from './shared'
import { useGridScene } from './useGridScene'
import { useGridVector } from './useGridVector'
import { useGridTheme } from './useGridTheme'
import { useSuperResolution } from './useSuperResolution'

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
    GridExploreMapOps.map_destroyGridDEMLayer(gridData.value)
    GridExploreMapOps.map_destroyGridMVTLayer()
    GridExploreMapOps.map_destroyGrid3DLayer(gridData.value)
    GridExploreMapOps.map_destroyGridNDVIOrSVRLayer(gridData.value)
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
    gridVectorSymbology.value = JSON.parse(JSON.stringify(ezStore.get("vectorSymbology"))) // 深拷贝
    console.log(gridData.value, 'gridData')
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
})

onUnmounted(() => {
    bus.off('update:gridPopupData', handleInitGrid)
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
        handleScaleMouseDown, handleScaleMouseUp, scaleRateFormatter, onAfterScaleRateChange, scaleRate, enableDraggable,
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
        handleVectorVisualize } = useGridVector()

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
const { handleSuperResolution } = useSuperResolution()

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