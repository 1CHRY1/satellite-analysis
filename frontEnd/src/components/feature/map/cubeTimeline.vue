<template>
    <div class="timeline-container" v-if="show">
        <div class="timeline">
            <div class="date-filter start-filter flex flex-col items-center justify-center text-center">
                <div class="filter-label mb-1">年份</div>
                <div class="date-selector">
                    <select
                    v-model="selectedYear"
                    @change="applyDateFilter"
                    class="bg-transparent border border-gray-300 rounded px-2 py-1 text-center"
                    >
                    <option disabled value="" class="!bg-[#1f282f]">请选择年份</option>
                    <option v-for="year in yearOptions" :key="year" :value="year" class="!bg-[#1f282f]">{{ year }}</option>
                    </select>
                </div>
            </div>

            <div class="timeline-wrapper" @click="console.log(filteredImages)">
                <button
                    class="nav-button"
                    @click="handleClick(activeIndex - 1)"
                    :disabled="activeIndex <= 0"
                >
                    <ChevronLeftIcon :size="24" />
                </button>

                <div class="timeline-track" ref="timelineTrack">
                    <div
                        v-for="(item, index) in filteredImages"
                        :key="index"
                        class="timeline-item"
                        :class="{ active: index === activeIndex }"
                        @click="handleClick(index)"
                    >
                        <div class="label mb-1" @click="console.log(item, 1221)">
                            {{ item.productName }}
                        </div>
                        <div class="dot-container">
                            <div class="dot"></div>
                            <div class="connector" v-if="index < filteredImages.length - 1"></div>
                        </div>
                        <div class="label">{{ timeFormat(item.time) }}</div>
                    </div>
                </div>

                <button
                    class="nav-button"
                    @click="handleClick(activeIndex + 1)"
                    :disabled="activeIndex >= filteredImages.length - 1"
                >
                    <ChevronRightIcon :size="24" />
                </button>
            </div>

            <div class="date-filter end-filter flex flex-col items-center justify-center text-center">
                <div class="filter-label mb-1">月份</div>
                <div class="date-selector">
                    <select
                    v-model="selectedMonth"
                    :disabled="!selectedYear"
                    @change="applyDateFilter"
                    class="bg-transparent border border-gray-300 rounded px-2 py-1 text-center"
                    >
                        <option value="" class="!bg-[#1f282f]">全部月份</option>
                        <option v-for="month in 12" :key="month" :value="month" class="!bg-[#1f282f]">{{ month }} 月</option>
                    </select>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, reactive, type ComputedRef } from 'vue'
import { ChevronLeftIcon, ChevronRightIcon } from 'lucide-vue-next'
import { getGrid3DUrl, getGridDEMUrl, getGridNDVIOrSVRUrl, getGridSceneUrl, getImgStats, getMinIOUrl } from '@/api/http/interactive-explore/visualize.api'
import bus from '@/store/bus'
import { ezStore } from '@/store'
import * as GridExploreMapOps from '@/util/map/operation/grid-explore'
import { message } from 'ant-design-vue'
import type { GridData } from '@/type/interactive-explore/grid'

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

const show = defineModel<boolean>()
const multiImages = ref<MultiImageInfoType[]>([])
const productImages = ref<MultiImageInfoType[]>([])
const scaleRate = ref(0)
const grid = ref<GridData>({ rowId: 0, columnId: 0, resolution: 0, opacity: 0, normalize_level: 0, sceneRes: { total: 0, category: [] }, vectors: [], themeRes: { total: 0, category: [] } })
const activeIndex = ref(-1)
const visualMode = ref<'rgb' | 'product'>('rgb')
const timelineTrack = ref<HTMLElement | null>(null)

const showingImageStrech = reactive({
    r_min: 0,
    r_max: 5000,
    g_min: 0,
    g_max: 5000,
    b_min: 0,
    b_max: 5000,
})

/**
 * 日期筛选
 */
const startDateFilter = ref('')
const endDateFilter = ref('')
const minDate = ref('')
const maxDate = ref('')
const selectedYear = ref('')
const selectedMonth = ref('')

const yearOptions:ComputedRef<string[]> = computed(() => {
    const years = new Set<string>()
    // 假设你有 minDate 和 maxDate 形式为 'YYYY-MM-DD'
    const minYear = new Date(minDate.value).getFullYear()
    const maxYear = new Date(maxDate.value).getFullYear()
    for (let y = minYear; y <= maxYear; y++) {
        years.add(String(y))
    }
    return Array.from(years)
})
const handleDataRange = () => {
    if (selectedYear.value && !selectedMonth.value) {
    // 年份已选，月份未选：整年
    startDateFilter.value = `${selectedYear.value}-01-01`
    endDateFilter.value = `${selectedYear.value}-12-31`
  } else if (selectedYear.value && selectedMonth.value) {
    // 年份和月份都选了：该月
    const year = selectedYear.value
    const month = String(selectedMonth.value).padStart(2, '0')
    const lastDay = new Date(Number(year), Number(selectedMonth.value), 0).getDate()
    startDateFilter.value = `${year}-${month}-01`
    endDateFilter.value = `${year}-${month}-${lastDay}`
  } else {
    // 都未选，那就全选吧
    startDateFilter.value = '2001-01-01'
    endDateFilter.value = '2030-12-31'
  } 
}
// 应用日期筛选
const applyDateFilter = () => {
    handleDataRange()
    // 如果活动索引超出了筛选后的范围，重置为第一个
    if (activeIndex.value >= filteredImages.value.length || activeIndex.value < 0) {
        // activeIndex.value = filteredImages.value.length > 0 ? 0 : -1
        activeIndex.value = -1

        // 如果有有效的活动索引，触发点击事件以显示对应的图像
        if (activeIndex.value >= 0) {
            handleClick(activeIndex.value)
        }
    }
}

// 设置日期范围
const setDateRange = () => {
    if (showingImages.value.length > 0) {
        const dates = showingImages.value
            .map((item) => new Date(item.time))
            .sort((a, b) => a.getTime() - b.getTime())

        if (dates.length > 0 && !isNaN(dates[0].getTime())) {
            const firstDate = dates[0]
            const lastDate = dates[dates.length - 1]

            const nextDay = new Date(lastDate)
            nextDay.setDate(nextDay.getDate() + 2)

            minDate.value = firstDate.toISOString().split('T')[0]
            maxDate.value = nextDay.toISOString().split('T')[0]
        
            console.log(firstDate, lastDate, minDate.value, maxDate.value, 157)

            // 只在初始化时设置筛选器的默认值
            if (!startDateFilter.value) {
                startDateFilter.value = minDate.value
            }
            if (!endDateFilter.value) {
                endDateFilter.value = maxDate.value
            }
        }
    }
}

/**
 * 筛选后的图像数据
 */
const showingImages = computed(() => {
    if (visualMode.value === 'rgb') {
        return multiImages.value
    } else {
        return productImages.value
    }
})
const filteredImages = computed(() => {
    let images
    if (visualMode.value === 'rgb') {
        images = multiImages.value as MultiImageInfoType[]
    } else {
        images = productImages.value as MultiImageInfoType[]
    }
    // console.log('all image', images)

    if (startDateFilter.value) {
        images = images.filter((item) => new Date(item.time) >= new Date(startDateFilter.value))
    }

    if (endDateFilter.value) {
        images = images.filter((item) => new Date(item.time) <= new Date(endDateFilter.value))
    }

    // console.log('filteredImages', images, images[0].time)
    images.sort((a, b) => a.time.localeCompare(b.time))

    return images
})

// 监听筛选后的数据变化，重置活动索引
watch(
    filteredImages,
    () => {
        if (activeIndex.value >= filteredImages.value.length) {
            activeIndex.value = filteredImages.value.length > 0 ? 0 : -1
        }
    },
    { deep: true },
)

const timeFormat = (timeString: string) => {
    const date = new Date(timeString)

    if (isNaN(date.getTime())) {
        console.error(`Invalid date string: ${timeString}`)
        return 'unknown'
    }

    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')

    return `${year}-${month}-${day}`
}


/**
 * 影像可视化
 */
const handleClick = async (index: number) => {
    console.log(filteredImages.value, 'filteredImages')
    if (index < 0 || index >= filteredImages.value.length) return

    const stopLoading = message.loading('正在加载影像...')

    activeIndex.value = index

    const currentImage = filteredImages.value[index]

    if (visualMode.value === 'rgb') {
        const img = currentImage as MultiImageInfoType

        let redPath,greenPath,bluePath

        if(superResOverride.value?.load == true){
             redPath = superResOverride.value?.redPath 
             greenPath = superResOverride.value?.greenPath 
             bluePath = superResOverride.value?.bluePath 
        } else {
            redPath =  img.redPath
            greenPath =  img.greenPath
            bluePath = img.bluePath
        }
        
        console.log('red, green, blue', redPath, greenPath, bluePath)

        const cache = ezStore.get('statisticCache')
        const promises: any = []
        let [min_r, max_r, min_g, max_g, min_b, max_b] = [0, 0, 0, 0, 0, 0]

        if (cache.get(redPath) && cache.get(greenPath) && cache.get(bluePath)) {
            console.log('cache hit!')
            ;[min_r, max_r] = cache.get(redPath)
            ;[min_g, max_g] = cache.get(greenPath)
            ;[min_b, max_b] = cache.get(bluePath)
        } else if (img.dataType !== 'dem' && img.dataType !== 'dsm') {
            promises.push(
                getImgStats(getMinIOUrl(redPath)),
                getImgStats(getMinIOUrl(greenPath)),
                getImgStats(getMinIOUrl(bluePath)),
            )
            await Promise.all(promises).then((values) => {
                min_r = values[0].b1.min
                max_r = values[0].b1.max
                min_g = values[1].b1.min
                max_g = values[1].b1.max
                min_b = values[2].b1.min
                max_b = values[2].b1.max
            })

            cache.set(redPath, [min_r, max_r])
            cache.set(greenPath, [min_g, max_g])
            cache.set(bluePath, [min_b, max_b])
        }

        console.log(min_r, max_r, min_g, max_g, min_b, max_b)
        const url = getGridSceneUrl(grid.value, {
            redPath,
            greenPath,
            bluePath,
            r_min: min_r,
            r_max: max_r,
            g_min: min_g,
            g_max: max_g,
            b_min: min_b,   
            b_max: max_b,
            normalize_level: scaleRate.value,
            nodata: img.nodata
        })
        GridExploreMapOps.map_addGridSceneLayer(
            grid.value,
            url,
            stopLoading,
        )
    } else if (visualMode.value === 'product') {
        const img = currentImage as MultiImageInfoType

        let redPath = img.redPath
        let greenPath = img.greenPath
        let bluePath = img.bluePath
        
        console.log('red, green, blue', redPath, greenPath, bluePath)

        const cache = ezStore.get('statisticCache')
        const promises: any = []
        let [min_r, max_r, min_g, max_g, min_b, max_b] = [0, 0, 0, 0, 0, 0]

        if (cache.get(redPath) && cache.get(greenPath) && cache.get(bluePath)) {
            console.log('cache hit!')
            ;[min_r, max_r] = cache.get(redPath)
            ;[min_g, max_g] = cache.get(greenPath)
            ;[min_b, max_b] = cache.get(bluePath)
        } else if (img.dataType !== 'dem' && img.dataType !== 'dsm') {
            promises.push(
                getImgStats(getMinIOUrl(redPath)),
                getImgStats(getMinIOUrl(greenPath)),
                getImgStats(getMinIOUrl(bluePath)),
            )
            await Promise.all(promises).then((values) => {
                min_r = values[0].b1.min
                max_r = values[0].b1.max
                min_g = values[1].b1.min
                max_g = values[1].b1.max
                min_b = values[2].b1.min
                max_b = values[2].b1.max
            })

            cache.set(redPath, [min_r, max_r])
            cache.set(greenPath, [min_g, max_g])
            cache.set(bluePath, [min_b, max_b])
        }

        console.log(min_r, max_r, min_g, max_g, min_b, max_b)

        
        if (img.dataType === 'dem') {
            const url = getGridDEMUrl(grid.value, redPath)
            GridExploreMapOps.map_addGridDEMLayer(
                grid.value,
                url,
                stopLoading,
            )

        } else if (img.dataType === '3d') {
            const url = getGrid3DUrl(grid.value, {
                redPath,
                greenPath,
                bluePath,
                r_min: min_r,
                r_max: max_r,
                g_min: min_g,
                g_max: max_g,
                b_min: min_b,
                b_max: max_b,
                normalize_level: scaleRate.value,
                nodata: img.nodata
            })
            GridExploreMapOps.map_addGrid3DLayer(
                grid.value,
                url,
                stopLoading,
            )
        } else if (img.dataType === 'svr' || img.dataType === 'ndvi') {
            const url = getGridNDVIOrSVRUrl(grid.value, {
                fullTifPath: redPath,
                // min: min_r,
                // max: max_r,
                min: -1,
                max: 1,
                normalize_level: scaleRate.value,
                nodata: img.nodata
            })
            GridExploreMapOps.map_addGridNDVIOrSVRLayer(
                grid.value,
                url,
                stopLoading,
            )
            console.log('nodata',img.nodata,img)
        }
    }
}

/**
 * 更新数据
 */
const updateHandler = (
    _data: ImageInfoType[] | MultiImageInfoType[],
    _grid: GridData,
    _scaleRate: number,
    mode: 'rgb' | 'product',

) => {

    activeIndex.value = -1
    grid.value = _grid
    visualMode.value = mode

    if (mode === 'rgb') {
        multiImages.value = _data as MultiImageInfoType[]
    } else {
        productImages.value = _data as MultiImageInfoType[]
    }
    console.log('multi', multiImages.value)
    console.log('product', productImages.value)
    console.log('scalerate', _scaleRate)
    scaleRate.value = _scaleRate

    startDateFilter.value = ''
    endDateFilter.value = ''

    setDateRange()
}

// let runningSource: 'SuperResTimeLine' | 'cubeVisualize' | null = null;
const clearState = () => {
  activeIndex.value = -1;
  multiImages.value = [];
  productImages.value = [];
  scaleRate.value = 0;
}

const superResOverride = ref<{
  redPath: string
  greenPath: string
  bluePath: string
  load: boolean
} | null>(null)

onMounted(() => {
    clearState()
    bus.on('cubeVisualize', updateHandler)

    bus.on('SuperResTimeLine', (SuperData: { R: string, G: string, B: string }, loadSuper : boolean) => {
    console.log('收到 SuperResTimeLine 数据:', SuperData,loadSuper)
    superResOverride.value = {
        redPath: SuperData.R,
        greenPath: SuperData.G,
        bluePath: SuperData.B,
        load: loadSuper
  }

  // 强制重新加载当前图像（如果有）
  if (activeIndex.value >= 0) {
    handleClick(activeIndex.value)
  }
})

    bus.on('closeTimeline', () => {
        activeIndex.value = -1
        multiImages.value = []
        productImages.value = []
        scaleRate.value = 0
    })
})
</script>

<style scoped>
.timeline-container {
    display: flex;
    justify-content: center;
    padding: 0.5rem;
}

.timeline {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    min-width: 30vw;
    max-width: 55vw;
    background: rgba(18, 25, 38, 0.75);
    backdrop-filter: blur(10px);
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
    overflow: hidden;
    padding: 1rem;
    border: 1px solid rgba(255, 255, 255, 0.1);
    gap: 1rem;
}

.timeline-wrapper {
    display: flex;
    flex: 1;
    align-items: center;
    height: 100px;
    min-width: 0;
    /* 允许子元素收缩 */
}

.timeline-track {
    display: flex;
    flex: 1;
    align-items: center;
    overflow-x: auto;
    scrollbar-width: thin;
    scrollbar-color: rgba(108, 253, 255, 0.3) rgba(18, 25, 38, 0.5);
    padding: 0 0.5rem;
    min-width: 0;
    /* 允许子元素收缩 */
}

.timeline-track::-webkit-scrollbar {
    height: 6px;
}

.timeline-track::-webkit-scrollbar-track {
    background: rgba(18, 25, 38, 0.5);
    border-radius: 3px;
}

.timeline-track::-webkit-scrollbar-thumb {
    background: rgba(108, 253, 255, 0.3);
    border-radius: 3px;
}

.timeline-item {
    cursor: pointer;
    text-align: center;
    height: 90px;
    min-width: 80px;
    /* 确保每个项目有足够的宽度 */
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    position: relative;
    transition: transform 0.2s ease;
    flex-shrink: 0;
    /* 防止项目被压缩 */
    margin: 0 5px;
    /* 增加项目间距 */
}

.timeline-item:hover {
    transform: translateY(-2px);
}

.dot-container {
    display: flex;
    align-items: center;
    position: relative;
    width: 100%;
    height: 20px;
}

.dot {
    width: 14px;
    height: 14px;
    background-color: rgba(108, 253, 255, 0.3);
    border: 2px solid #6cfdff;
    border-radius: 50%;
    margin: 0 auto;
    transition: all 0.3s ease;
    position: relative;
    z-index: 2;
}

.connector {
    position: absolute;
    height: 2px;
    background: linear-gradient(to right, #6cfdff, rgba(108, 253, 255, 0.3));
    width: calc(100% - 14px);
    left: 60%;
    top: 50%;
    transform: translateY(-50%);
    z-index: 1;
}

.label {
    margin-top: 10px;
    font-size: 0.8rem;
    color: rgba(255, 255, 255, 0.7);
    transition: color 0.3s ease;
    white-space: nowrap;
}

.active .dot {
    background-color: #6cfdff;
    box-shadow: 0 0 10px rgba(108, 253, 255, 0.7);
    transform: scale(1.2);
}

.active .label {
    color: #ffffff;
    font-weight: 500;
}

.nav-button {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 36px;
    height: 36px;
    border-radius: 50%;
    background: rgba(108, 253, 255, 0.1);
    border: 1px solid rgba(108, 253, 255, 0.3);
    color: #6cfdff;
    cursor: pointer;
    transition: all 0.2s ease;
    z-index: 3;
    flex-shrink: 0;
}

.nav-button:hover:not(:disabled) {
    background: rgba(108, 253, 255, 0.2);
    transform: scale(1.05);
}

.nav-button:disabled {
    opacity: 0.4;
    cursor: not-allowed;
}

/* 日期筛选样式 */
.date-filter {
    display: flex;
    flex-direction: column;
    min-width: 140px;
    padding: 0.5rem;
    background: rgba(18, 25, 38, 0.5);
    border-radius: 8px;
    border: 1px solid rgba(108, 253, 255, 0.2);
    flex-shrink: 0;
}

.filter-label {
    font-size: 0.8rem;
    color: rgba(255, 255, 255, 0.8);
    margin-bottom: 0.5rem;
    text-align: center;
}

.date-selector input {
    width: 100%;
    padding: 0.5rem;
    background: rgba(18, 25, 38, 0.7);
    border: 1px solid rgba(108, 253, 255, 0.3);
    border-radius: 4px;
    color: #fff;
    font-size: 0.8rem;
    outline: none;
}

.date-selector input:focus {
    border-color: rgba(108, 253, 255, 0.7);
    box-shadow: 0 0 5px rgba(108, 253, 255, 0.3);
}

/* 自定义日期选择器样式 */
.date-selector input::-webkit-calendar-picker-indicator {
    filter: invert(1);
    opacity: 0.7;
}

@media (max-width: 768px) {
    .timeline {
        flex-direction: column;
        gap: 0.5rem;
        padding: 0.5rem;
    }

    .date-filter {
        flex-direction: row;
        align-items: center;
        width: 100%;
        gap: 0.5rem;
    }

    .filter-label {
        margin-bottom: 0;
        min-width: 60px;
    }
}
</style>
