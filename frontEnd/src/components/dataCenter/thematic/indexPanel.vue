<template>
    <!-- Section: 输入模块 -->
    <!-- <section class="panel-section"> -->
                <div class="section-header">
            <div class="section-subtitle flex items-center gap-2 mb-4 mt-4 pb-2 border-b border-[#247699]">
                <MapIcon :size="18" />
                <h2 class="section-title text-lg font-medium text-[#38bdf8] mt-2 ml-2">指数分析</h2>
            </div>
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

                    <div class="config-label flex items-center gap-2 mt-4 mb-4  border border-[#247699] rounded" style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);" >
                        <!-- <LayersIcon :size="16" /> -->
                        <span class="ml-4">预制指数</span>
                        <button type="button" @click="togglePresetIndexes" class="text-blue-400 hover:text-blue-300">
                            {{ showPresetIndexes ? '隐藏' : '显示' }}
                        </button>
 
                    </div>

                    <div class="config-control" >
                        <!-- 指数类型选择 -->
                        <div v-if="showPresetIndexes" class="section-content mb-6 ml-8">
                            <div class="max-h-48 overflow-y-auto border border-[#247699] rounded-lg p-2 bg-[#0d1526]">
                                <div class="space-y-2">
                                    <button
                                        v-for="(item, index) in presetIndex"
                                        :key="index"
                                        @click="selectIndexDetail(item)"
                                        class="w-full text-left px-3 py-2 rounded transition-colors duration-200 hover:bg-[#1a2b4c] hover:border-[#2bb2ff] hover:text-white border border-transparent"
                                        :class="{ 'bg-[#1a2b4c] border-[#2bb2ff]': item.isAble === false }"
                                    >
                                        {{ item.name }}
                                    </button>
                                </div>
                            </div>
                        </div>
                        <el-dialog v-model="showDetail"
                                    class="max-w-[90vw] md:max-w-[80vw] lg:max-w-[70vw] xl:max-w-[60vw]"
                                    style="background-color: #111827; color: white;">
                                    <div class="text-blue-500">指数详情</div>
                                    <p class="text-blue-300" v-if="selectedItem">指数名称 : {{ selectedItem.name }}</p>
                                    <p class="text-blue-300" v-if="selectedItem">公式详情 : {{ selectedItem.expression }}</p>
                                    <p class="text-blue-300" v-if="selectedItem">描述：</p>
                                    <p class="text-blue-300" v-if="selectedItem">{{ selectedItem.description || '暂无描述' }}</p>
                                </el-dialog>
                                                        <el-dialog v-model="showDefine"
                                    class="max-w-[90vw] md:max-w-[80vw] lg:max-w-[70vw] xl:max-w-[60vw]"
                                    style="background-color: #111827; color: white;">
                                    <div class="text-blue-500">
                                        {{ selectedItem && selectedItem.basisIndex === false ? '编辑自定义指数' : '新建自定义指数' }}
                                    </div>
                                    <el-form v-model="form">
                                        <p class="text-blue-300" >
                                            指数名称 : 
                                            <input v-model="form.name"  placeholder="请输入指数名称">
                                        </p>
                                        
                                        <div class="text-blue-300 flex items-center gap-2" >
                                            公式详情 : 
                                            <input v-model="form.expression"  placeholder="请输入具体公式" readonly @focus="showInput = true">
                                            <div class="text-red-400 cursor-pointer hover:underline" @click="undoLast">删除</div>
                                        </div>
                                        <div v-if="showInput" class="option-panel mb-2" >
                                            <el-button
                                                v-for="(item,index) in reBands"
                                                :key="item"
                                                size="small"
                                                @click="selectLabel(item)"
                                                :disabled="isBandDisabled(item)"
                                                class="w-16 h-10 bg-blue-500 text-white text-center rounded hover:bg-blue-600"
                                                :style="{ backgroundColor: '#1e3a8a', color: 'white' }"
                                                :class="getBandButtonClass(item)"
                                            >
                                                {{ item }}
                                            </el-button>
                                        </div>
                                        <div v-if="showInput" class="option-panel mb-2">
                                            <el-button
                                                v-for="item in calcu"
                                                :key="item"
                                                size="small"
                                                @click="selectLabel(item)"
                                                class="w-16 h-10 bg-blue-500 text-white text-center rounded hover:bg-blue-600"
                                                :style="{ backgroundColor: '#1e3a8a', color: 'white' }"
                                            >
                                                {{ item }}
                                            </el-button>
                                        </div>
                                        <div v-if="showInput" class="option-panel mb-2">
                                            <el-button
                                                v-for="item in numList"
                                                :key="item"
                                                size="small"
                                                @click="selectLabel(item)"
                                                class="w-16 h-10 bg-blue-500 text-white text-center rounded hover:bg-blue-600"
                                                :style="{ backgroundColor: '#1e3a8a', color: 'white' }"
                                            >
                                                {{ item }}
                                            </el-button>
                                        </div>
                                        
                                        <p class="text-blue-300" >
                                            描述：
                                            <input v-model="form.description"  placeholder="请输入公式的描述信息">
                                        </p>

                                        <p class="text-blue-300"
                                            :hidden="isDisabled">
                                            展示初始化（默认）：
                                            <input v-model="form.isAble" disabled>
                                        </p>
                                        <input
                                             v-model="form.basisIndex"
                                             :hidden="isDisabled"
                                            />
                                        
                                        <div class="flex gap-2 mt-4">
                                            <button type="button" @click="saveLocal"
                                                class="text-blue-500 px-4 py-2 border border-blue-500 rounded hover:bg-blue-500 hover:text-white">
                                                {{ selectedItem && selectedItem.basisIndex === false ? '更新' : '保存' }}
                                            </button>
                                            <button type="button" @click="cancelEdit"
                                                class="text-gray-400 px-4 py-2 border border-gray-400 rounded hover:bg-gray-400 hover:text-white">
                                                取消
                                            </button>
                                            <button type="button" @click="showCustomIndexes = true"
                                                class="text-green-500 px-4 py-2 border border-green-500 rounded hover:bg-green-500 hover:text-white">
                                                管理已保存
                                            </button>
                                        </div>
                                    </el-form>
                                </el-dialog>
                                
                                <!-- 管理自定义指数对话框 -->
                                <el-dialog v-model="showCustomIndexes"
                                    class="max-w-[90vw] md:max-w-[80vw] lg:max-w-[70vw] xl:max-w-[60vw]"
                                    style="background-color: #111827; color: white;">
                                    <div class="text-blue-500 mb-4">管理自定义指数</div>
                                    <div v-if="customIndexesList.length === 0" class="text-gray-400 text-center py-8">
                                        暂无自定义指数
                                    </div>
                                    <div v-else class="space-y-3">
                                        <div v-for="(index, idx) in customIndexesList" :key="idx" 
                                             class="border border-gray-600 rounded p-3">
                                            <div class="flex justify-between items-start">
                                                <div class="flex-1">
                                                    <h4 class="text-white font-medium">{{ index.name }}</h4>
                                                    <p class="text-gray-300 text-sm mt-1">公式: {{ index.expression }}</p>
                                                    <p class="text-gray-400 text-sm mt-1" v-if="index.description">
                                                        描述: {{ index.description }}
                                                    </p>
                                                </div>
                                                <button type="button" @click="deleteCustomIndex(idx)"
                                                        class="text-red-400 hover:text-red-300 ml-2">
                                                    删除
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </el-dialog>
                        
                        <div>
                            <div class="color-palette-selector mb-6 border border-[#247699] rounded mt-2 mb-4" style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                <span class="ml-4">选择指数</span>
                                <select
                                    v-model="selectedIndex"
                                    class="w-full rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white appearance-none hover:border-[#2bb2ff] hover:bg-[#1a2b4c] focus:outline-none focus:border-[#3b82f6]"
                                >
                                    <option disabled value="">选择指数</option>
                                    <option
                                    v-for="(palette, index) in presetIndex"
                                    :key="index"
                                    :value="palette.name"
                                    :disabled="palette.isAble === false"
                                    class="bg-[#0d1526] text-white mb-4"
                                    >
                                    {{ palette.name }}
                                    </option>

                                </select>
                            </div>
                        </div>
                        <div class="color-palette-selector  mb-6  border border-[#247699] rounded mt-2 mb-4" style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                                <label class="ml-4">配色方案:</label>
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
                <button type="button" @click="handleCloudTiles"
                            class="cursor-pointer w-full rounded-lg border border-[#247699] bg-[#069FFF] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95">
                            开始运行
                </button>
            </div>
        </div>
    <!-- </section> -->
    
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch, type ComponentPublicInstance, type ComputedRef, type Ref, reactive } from 'vue'
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
import { useAnalysisStore, useGridStore, useUserStore } from '@/store'
import { mapManager } from '@/util/map/mapManager'
import { getNoCloudUrl4MosaicJson, getMosaicJsonUrl } from '@/api/http/satellite-data/visualize.api';
import { useI18n } from 'vue-i18n'
import { Item } from 'ant-design-vue/es/menu'
const { t } = useI18n()
const dbData = useAnalysisStore()
const selectedSceneId = ref('')
const userStore = useUserStore()
const userId = computed(() => userStore.user?.id || '')

interface IndexItem {
  name: string
  expression: string
  description?: string
  basisIndex?: boolean
  isAble?: boolean
}

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
const isDisabled = ref(true)

//指数目录
const selectedIndex = ref();
const presetIndex = ref([
    {name:'超绿指数', expression: '2*Green-Red-Blue', description:'超绿色提取绿色植物图像效果较好，阴影、枯草和土壤图像均能较明显的被压制，植物图像更为突出', isAble: false, basisIndex: true},
    {name:'自定义', expression:'', description:'', isAble:false, basisIndex:false}
])

const showDetail = ref(false)

//色带选择
const selectedColorMap = ref('rdylgn')
const colorMaps = {
    '红-黄-绿':'rdylgn',
    '自定义（即将提供）': 'self-define'
}

//detail显示
const selectedItem = ref<IndexItem | null>(null)
const selectIndexDetail = (item) => {
    if (item.name == '自定义') {
        
        form.name = ''
        form.expression = ''
        form.description = ''
        inputHistory.value = []
        selectedItem.value = null
        showDefine.value = true
    } else if (item.basisIndex === false) {
        // 允许修改
        selectedItem.value = item

        Object.assign(form, item)
        
        nextTick(() => {
            initInputHistory()
        })
        showDefine.value = true
    } else {
        
        selectedItem.value = item
        showDetail.value = true
    }
}


const togglePresetIndexes = () => {
    showPresetIndexes.value = !showPresetIndexes.value
}

const showDefine = ref(false)
const showCustomIndexes = ref(false)
const customIndexesList = ref<any[]>([])
const showPresetIndexes = ref(true)

const STORAGE_KEY = 'index_form'

const form = reactive({
    name: '',
    expression: '',
    description:'',
    isAble: false,
    basisIndex: false
})
//本地缓存
const saveLocal = async() => {
    // 获取现有的自定义指数列表
    const customIndexesStr = localStorage.getItem('custom_indexes')
    const existingCustomIndexes = customIndexesStr 
        ? JSON.parse(customIndexesStr) 
        : []
    
    // 检查是否为编辑模式
    const existingIndex = existingCustomIndexes.find(item => item.name === form.name && item.userId === userId.value)
    const isEditing = selectedItem.value && selectedItem.value.basisIndex === false
    
    if (existingIndex && !isEditing) {
        ElMessage.warning('已存在同名指数，请使用不同的名称')
        return
    }

    if (isEditing) {
        // 更新现有指数
        const indexToUpdate = existingCustomIndexes.findIndex(item => item.name === selectedItem.value?.name && item.userId === userId.value)
        if (indexToUpdate !== -1) {
            existingCustomIndexes[indexToUpdate] = { ...form, userId: userId.value }
            ElMessage.success('已成功更新自定义指数')
        }
    } else {
        const newCustomIndex = { ...form, userId: userId.value }
        existingCustomIndexes.push(newCustomIndex)
        ElMessage.success('已成功保存自定义指数')
    }
    
    localStorage.setItem('custom_indexes', JSON.stringify(existingCustomIndexes))
    localStorage.setItem(STORAGE_KEY, JSON.stringify(form))
    
    loadAllCustomIndexes()
    
    showDefine.value = false
    selectedItem.value = null
}


const mergeList = async() => {
    loadAllCustomIndexes()
}

// 加载所有自定义指数
const loadAllCustomIndexes = () => {
    const customIndexesStr = localStorage.getItem('custom_indexes')
    const customIndexes = customIndexesStr 
        ? JSON.parse(customIndexesStr) 
        : []

    // 只显示当前用户的自定义指数
    const userCustomIndexes = customIndexes.filter((item: any) => item.userId === userId.value)
    customIndexesList.value = userCustomIndexes
    
    // 获取单个
    const cached = localStorage.getItem(STORAGE_KEY)
    if (cached) {
        const parsed = JSON.parse(cached)
        Object.assign(form, parsed)
    }

    presetIndex.value = presetIndex.value.filter(item => item.basisIndex === true)
    
    // 只添加当前用户的自定义指数
    userCustomIndexes.forEach((customIndex: any) => {
        presetIndex.value.splice(presetIndex.value.length - 1, 0, { 
            ...customIndex,
            isAble: false,
            basisIndex: false  
        })
    })
    
    presetIndex.value.push({name:'自定义', expression:'', description:'', isAble:false, basisIndex:false})
    
    if (!selectedItem.value || selectedItem.value.basisIndex !== false) {
        form.description = ''
        form.expression = ''
        form.name = ''
        inputHistory.value = []
    }
}


const deleteCustomIndex = (index: number) => {
    const customIndexesStr = localStorage.getItem('custom_indexes')
    const customIndexes = customIndexesStr 
        ? JSON.parse(customIndexesStr) 
        : []
    
    // 只删除当前用户的指定指数
    const userCustomIndexes = customIndexes.filter((item: any) => item.userId === userId.value)
    userCustomIndexes.splice(index, 1)

    // 重新合并所有用户的指数
    const otherUsersIndexes = customIndexes.filter((item: any) => item.userId !== userId.value)
    const newIndexes = [...otherUsersIndexes, ...userCustomIndexes]

    localStorage.setItem('custom_indexes', JSON.stringify(newIndexes))
    
    loadAllCustomIndexes()
    
    ElMessage.success('已删除自定义指数')
}


const cancelEdit = () => {
     
    form.description = ''
    form.expression = ''
    form.name = ''
    
    
    inputHistory.value = []
    
     
    showDefine.value = false
    selectedItem.value = null
}

// 公式操作
const calcu = ['+','-','*','/']
const numList = ['1','2','3','4','5','6','7','8','9','0']
const inputHistory = ref<string[]>([])

const showInput = ref(false)
const bandMap = {}
const reBands = ref()

//对导入波段监视
watch(() => dbData.bandList, (newVal) => {
  if (newVal&& newVal.length > 0) {
    //原始波段格式转换
     reBands.value = dbData.bandList.replace(/[\[\]\s]/g, '').split(',')
    const options = ref({
        b1:reBands[0],
    })

    console.log('原始 bandList:', dbData.bandList)
    console.log('清洗后 reBands:', reBands)

    //波段重索引

    reBands.value.forEach((val, index) => {
    bandMap[val] = `b${index + 1}`
    })
    console.log(bandMap,'对应波段')

    // 正则替换
    function replaceExpressionVars(expression, map) {
        const unmatched = new Set()

        const replaced = expression.replace(/\b[A-Za-z_][A-Za-z0-9_]*\b/g, (match) => {
            if (map[match]) {
            return map[match]
            } else {
            unmatched.add(match)
            return match
            }
        })

        const allMatched = unmatched.size === 0

        return {
            replaced,
            allMatched,
            unmatched: Array.from(unmatched)
        }
        }


    // 替换所有表达式
    presetIndex.value.forEach(item => {
        const { replaced, allMatched, unmatched } = replaceExpressionVars(item.expression, bandMap)

        item.expression = replaced
        item.isAble = allMatched

        if (!allMatched) {
            console.warn(` ${item.name} 中未识别字段:`, unmatched)
        }
        })


    console.log(presetIndex.value)

  }
}, { immediate: true })

const selectLabel  = async(item:string ) => {
    // 检查是否为波段选择
    if (reBands.value && reBands.value.includes(item)) {

        const selectedBands = inputHistory.value.filter(char => reBands.value.includes(char))
        if (selectedBands.length >= 3) {
            ElMessage.warning('最多只能选择3个波段')
            return
        }
        

        if (selectedBands.includes(item)) {
            ElMessage.warning('不能重复选择相同波段')
            return
        }
    }
    
    inputHistory.value.push(item)               
    form.expression = inputHistory.value.join('')
}

// 检查波段按钮是否应该禁用
const isBandDisabled = (band: string) => {
    if (!reBands.value || !reBands.value.includes(band)) return false
    
    const selectedBands = inputHistory.value.filter(char => reBands.value.includes(char))
    
    // 如果已经选择了3个波段，禁用所有未选择的波段
    if (selectedBands.length >= 3) {
        return !selectedBands.includes(band)
    }
    
    // 如果已经选择了这个波段，禁用它
    return selectedBands.includes(band)
}

// 获取波段按钮的样式类
const getBandButtonClass = (band: string) => {
    if (!reBands.value || !reBands.value.includes(band)) {
        return 'bg-blue-500 text-white hover:bg-blue-600'
    }
    
    const selectedBands = inputHistory.value.filter(char => reBands.value.includes(char))
    
    if (selectedBands.includes(band)) {

        return 'bg-green-600 text-white cursor-not-allowed'
    } else if (selectedBands.length >= 3) {

        return 'bg-gray-500 text-gray-300 cursor-not-allowed'
    } else {
        
        return 'bg-blue-500 text-white hover:bg-blue-600'
    }
}


const initInputHistory = () => {
    if (form.expression) {
        // 将现有表达式分解为输入历史
        inputHistory.value = form.expression.split('').filter(char => 
            [...calcu, ...numList, ...reBands.value].includes(char)
        )
    } else {
        inputHistory.value = []
    }
}
const undoLast = () => {
    inputHistory.value.pop()
    form.expression = inputHistory.value.join('')
}


const handleCloudTiles = async () => {

    try {
        console.log('props',props.thematicConfig)
        console.log('来自store的', dbData)
        // if(!props.thematicConfig.dataset){
        //     ElMessage.warning('请先从"前序数据"中选择一个数据集')
        //     return
        // }
        if (!dbData){
            ElMessage.warning('请先从"前序数据"中选择一个数据集')
        //     return
        }
        let bucket, object_path
        // old version
        // if (props.thematicConfig.dataset?.data) {
        //     bucket = props.thematicConfig.dataset?.data.bucket
        //     object_path = props.thematicConfig.dataset?.data.object_path
        // } else if (props.thematicConfig.dataset?.bucket) {
        //     bucket = props.thematicConfig.dataset?.bucket
        //     object_path = props.thematicConfig.dataset?.object_path
        // }

        bucket = dbData.mosaicBucket
        object_path = dbData.mosaicPath
        // console.log('props.thematicConfig.dataset.result', props.thematicConfig.dataset.result)
        let mosaicUrl = getMosaicJsonUrl({
            mosaicJsonPath: bucket + '/' + object_path
        })
        console.log('mosaicUrl', mosaicUrl)

        const target:IndexItem | undefined = presetIndex.value.find(item => item.name === selectedIndex.value )

        let encodedExpr
        if(target){
             encodedExpr = encodeURIComponent(target.expression)
        }else{
            ElMessage.error("没有指数")
            return
        }

        const selectedColor = colorMaps[selectedColorMap.value]
        
        const tileUrl = `/tiler/mosaic/analysis/{z}/{x}/{y}.png?mosaic_url=${mosaicUrl}&expression=${encodedExpr}&pixel_method=first&color=${selectedColor}`
        console.log('tileUrl', tileUrl)
        // const tileUrl = `http://localhost:8000/mosaic/analysis/{z}/{x}/{y}.png?mosaic_url=http://192.168.1.135:30900/temp-files/mosaicjson/hello.json&expression=${encodedExpr}&pixel_method=first&color=rdylgn`
        // const tileUrl = `http://localhost:8000/mosaic/analysis/{z}/{x}/{y}.png?mosaic_url=${mosaicUrl}&expression=${encodedExpr}&pixel_method=first&color=rdylgn`
        // const tileUrl = `http://localhost:8000/mosaic/analysis/13/6834/3215.png?mosaic_url=http://192.168.1.135:30900/temp-files/mosaicjson/hello.json&expression=${encodedExpr}&pixel_method=first&color=rdylgn`
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
onMounted(() => {
    loadAllCustomIndexes()
    console.log(presetIndex)
})

// 显示模态框
const showModal = (type) => {
  selectedIndex.value = type
  
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
