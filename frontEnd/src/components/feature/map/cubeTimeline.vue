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
                        @mousedown="handleInteraction"
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

            <!-- GIF生成区域 -->
            <div class="gif-generator flex flex-col items-center justify-center">
                <!-- 分辨率选择按钮组 -->
                <div class="resolution-toggle-group">
                    <button
                        v-for="res in [256, 512, 2048]"
                        :key="res"
                        class="resolution-toggle-btn"
                        :class="{ active: gifResolution === res }"
                        :disabled="isGeneratingGif"
                        @click="gifResolution = res as 256 | 512 | 2048"
                        :title="res === 256 ? '快速' : res === 512 ? '均衡' : '高清'"
                    >
                        {{ res }}
                    </button>
                </div>
                <!-- GIF生成按钮 -->
                <button
                    class="gif-btn"
                    @click="handleGenerateGif"
                    :disabled="filteredImages.length < 2 || isGeneratingGif"
                    :title="filteredImages.length < 2 ? '需要至少2张影像' : '生成GIF动画'"
                >
                    <FilmIcon v-if="!isGeneratingGif" :size="18" />
                    <LoaderIcon v-else :size="18" class="animate-spin" />
                    <span class="ml-1">{{ isGeneratingGif ? `${gifProgress}%` : 'GIF' }}</span>
                </button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, reactive, type ComputedRef, nextTick, onUnmounted } from 'vue'
import { ChevronLeftIcon, ChevronRightIcon, FilmIcon, LoaderIcon } from 'lucide-vue-next'
import { getGrid2DDEMUrl, getGrid3DUrl, getGridDEMUrl, getGridNDVIOrSVRUrl, getGridOneBandUrl, getGridSceneUrl, getImgStats, getMinIOUrl } from '@/api/http/interactive-explore/visualize.api'
import { grid2bbox } from '@/util/map/gridMaker'
import bus from '@/store/bus'
import { ezStore } from '@/store'
import * as GridExploreMapOps from '@/util/map/operation/grid-explore'
import { message } from 'ant-design-vue'
import type { GridData } from '@/type/interactive-explore/grid'
import { currentScene } from '@/components/feature/map/popContent/shared.ts'
import { useSuperResolution } from './popContent/useSuperResolution'
import bandMergeHelper from '@/util/image/util'

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
    cloudPath?: string
}

const show = defineModel<boolean>()
const multiImages = ref<MultiImageInfoType[]>([])
const productImages = ref<MultiImageInfoType[]>([])
const scaleRate = ref(1.00)
const stretchMethod = ref<'linear' | 'gamma' | 'standard' | ''>('gamma')
const grid = ref<GridData>({ rowId: 0, columnId: 0, resolution: 0, opacity: 0, normalize_level: 0, sceneRes: { total: 0, category: [] }, vectors: [], themeRes: { total: 0, category: [] } })
const activeIndex = ref(-1)
const visualMode = ref<'rgb' | 'product'>('rgb')
const timelineTrack = ref<HTMLElement | null>(null)

// GIF生成相关状态
const isGeneratingGif = ref(false)
const gifProgress = ref(0)
const gifResolution = ref<256 | 512 | 2048>(512)

// 支持去云的传感器
const CLOUD_SUPPORT_SENSORS = [
    'CB04_WPM', 'GF-1_PMS', 'GF-2_PMS', 'GF-6_PMS',
    'GF-7_PMS', 'ZY-1_VNIC', 'ZY-3_MUX', 'MTFC_PMS'
]

const scrollToCenter = (index: number) => {
    if (!timelineTrack.value) return
    
    const track = timelineTrack.value
    const items = track.children
    
    if (index < 0 || index >= items.length) return
    
    const item = items[index] as HTMLElement
    const trackWidth = track.offsetWidth
    const itemLeft = item.offsetLeft
    const itemWidth = item.offsetWidth
    
    // 计算需要滚动的位置，使项目居中
    const scrollPosition = itemLeft - (trackWidth / 2) + (itemWidth / 2)
    
    track.scrollTo({
        left: scrollPosition,
        behavior: 'smooth'
    })
}

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
    } else if (activeIndex.value >= 0) {
        scrollToCenter(activeIndex.value)
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
watch(activeIndex, (newIndex) => {
    if (newIndex >= 0) {
        nextTick(() => {
            scrollToCenter(newIndex)
        })
    }
})

/**
 * 影像可视化
 */
const handleClick = async (index: number) => {
    console.log(filteredImages.value, 'filteredImages')
    if (index < 0 || index >= filteredImages.value.length) return

    activeIndex.value = index

    // 【新增】滚动到居中位置
    scrollToCenter(index)
    
    const currentImage = filteredImages.value[index]
    currentScene.value = currentImage
    // console.log(currentImage)

    if (visualMode.value === 'rgb') {
        // 三层叠加？
        const img = currentImage as MultiImageInfoType

        let redPath, greenPath, bluePath

        const currentGridKey = `${grid.value.rowId}-${grid.value.columnId}-${grid.value.resolution}`
        const gridSuperRes = superResOverride.value.get(currentGridKey)

        if (gridSuperRes?.load === true && gridSuperRes.sceneId === img.sceneId) {
            redPath = gridSuperRes.redPath
            greenPath = gridSuperRes.greenPath
            bluePath = gridSuperRes.bluePath
        } else {
            redPath = img.redPath
            greenPath = img.greenPath
            bluePath = img.bluePath
        }
        
        console.log('red, green, blue', redPath, greenPath, bluePath)

        const cache = ezStore.get('statisticCache')
        const promises: any = []
        let [min_r, max_r, min_g, max_g, min_b, max_b, mean_r, mean_g, mean_b, std_r, std_g, std_b] = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

        if (cache.get(redPath) && cache.get(greenPath) && cache.get(bluePath)) {
            console.log('cache hit!')
            ;[min_r, max_r, mean_r, std_r] = cache.get(redPath)
            ;[min_g, max_g, mean_g, std_g] = cache.get(greenPath)
            ;[min_b, max_b, mean_b, std_b] = cache.get(bluePath)
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
                mean_r = values[0].b1.mean
                std_r = values[0].b1.std
                mean_g = values[1].b1.mean
                std_g = values[1].b1.std
                mean_b = values[2].b1.mean
                std_b = values[2].b1.std
            })

            cache.set(redPath, [min_r, max_r, mean_r, std_r])
            cache.set(greenPath, [min_g, max_g, mean_g, std_g])
            cache.set(bluePath, [min_b, max_b, mean_b, std_b])
        }

        console.log(min_r, max_r, min_g, max_g, min_b, max_b, mean_r, mean_g, mean_b, std_r, std_g, std_b)

        // 默认启用去云功能（如果有云掩膜且传感器支持）
        let cloudPath: string | undefined = undefined
        if (img.cloudPath && CLOUD_SUPPORT_SENSORS.includes(img.sensorName)) {
            cloudPath = img.cloudPath
        }

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
            stretch_method: stretchMethod.value,
            nodata: img.nodata,
            std_config: JSON.stringify({mean_r, mean_g, mean_b, std_r, std_g, std_b}),
            cloudPath: cloudPath
        })
        GridExploreMapOps.map_addGridSceneLayer(
            grid.value,
            url,
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

        
        if (img.dataType === 'dem' || img.dataType === 'dsm') {
            // const url = getGridDEMUrl(grid.value, redPath)
            // GridExploreMapOps.map_addGridDEMLayer(
            //     grid.value,
            //     url,
            //     stopLoading,
            // )
            const url = getGrid2DDEMUrl(grid.value, redPath)
            GridExploreMapOps.map_addGrid2DDEMLayer(
                grid.value,
                url,
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
            )
            console.log('nodata',img.nodata,img)
        } else if (['lai' , 'fvc' , 'fpar' , 'lst' , 'lse' , 'npp' , 'gpp' , 'et' , 'wue' , 'cue' , 'esi' , 'apar' , 'bba' , 'aridity_index' , 'vcf'].includes(img.dataType)) {
            const url = getGridOneBandUrl(grid.value, {
                fullTifPath: redPath,
                min: min_r,
                max: max_r,
                normalize_level: scaleRate.value,
                nodata: img.nodata
            })
            GridExploreMapOps.map_addGridOneBandLayer(
                grid.value,
                url,
            )
            console.log('nodata',img.nodata,img)
        }
    }
}

/**
 * 生成GIF动画
 * 使用 gifenc 库实现精确的透明色控制，避免颜色量化导致的透明误判
 */
const handleGenerateGif = async () => {
    if (filteredImages.value.length < 2) {
        message.warning('需要至少2张影像才能生成GIF')
        return
    }

    isGeneratingGif.value = true
    gifProgress.value = 0

    try {
        const { GIFEncoder, quantize, applyPalette } = await import('gifenc')

        const images = filteredImages.value
        const totalImages = images.length
        const resolution = gifResolution.value

        // 创建 GIF 编码器
        const gif = GIFEncoder()

        // 并行加载所有帧
        const bbox = grid2bbox(grid.value.columnId, grid.value.rowId, grid.value.resolution)
        let loadedCount = 0

        const loadTasks = images.map(async (img, index) => {
            try {
                const imageUrl = await getImageUrlForGif(img as MultiImageInfoType)
                const { rgba, transparentPixels } = await loadImageDataForGif(imageUrl, resolution)

                loadedCount++
                gifProgress.value = Math.round((loadedCount / totalImages) * 60)

                return { index, rgba, transparentPixels, success: true }
            } catch (err) {
                console.warn(`跳过第${index + 1}张影像:`, err)
                loadedCount++
                gifProgress.value = Math.round((loadedCount / totalImages) * 60)
                return { index, rgba: null, transparentPixels: null, success: false }
            }
        })

        const results = await Promise.all(loadTasks)

        const successfulFrames = results
            .filter(r => r.success && r.rgba)
            .sort((a, b) => a.index - b.index)

        if (successfulFrames.length === 0) {
            message.error('所有影像加载失败，无法生成GIF')
            isGeneratingGif.value = false
            gifProgress.value = 0
            return
        }

        if (successfulFrames.length < 2) {
            message.warning('成功加载的影像少于2张，无法生成GIF')
            isGeneratingGif.value = false
            gifProgress.value = 0
            return
        }

        // 透明色索引（调色板的最后一个位置）
        const TRANSPARENT_INDEX = 255

        // 处理每一帧
        successfulFrames.forEach((frame, i) => {
            const { rgba, transparentPixels } = frame

            // 量化颜色到 255 色（保留 1 个位置给透明色）
            // 不指定 format，使用默认的 8 位精度 (RGB888 = 16,777,216 色空间)
            // format 选项会降低精度：rgb565 (65K色)、rgb444 (4K色)
            const palette = quantize(rgba!, 255)

            // 添加透明色到调色板末尾
            palette.push([255, 0, 255])

            // 将 RGBA 数据转换为调色板索引（使用默认的最高精度）
            const indexedPixels = applyPalette(rgba!, palette)

            // 将透明像素的索引设为专用的透明索引
            for (const pixelIdx of transparentPixels!) {
                indexedPixels[pixelIdx] = TRANSPARENT_INDEX
            }

            // 写入帧，精确指定透明索引（不是颜色匹配！）
            gif.writeFrame(indexedPixels, resolution, resolution, {
                palette,
                transparent: true,
                transparentIndex: TRANSPARENT_INDEX,
                delay: 500,
                dispose: 2,  // 恢复到背景
            })

            gifProgress.value = 60 + Math.round(((i + 1) / successfulFrames.length) * 35)
        })

        // 完成编码
        gif.finish()

        // 获取 GIF 数据并创建 Blob
        const gifData = gif.bytes()
        const blob = new Blob([gifData], { type: 'image/gif' })
        const gifBlobUrl = URL.createObjectURL(blob)

        // 在地图格网上显示GIF动画
        GridExploreMapOps.map_addGridGifLayer(grid.value, gifBlobUrl)

        isGeneratingGif.value = false
        gifProgress.value = 0
        message.success('GIF生成成功！已在格网中显示')

    } catch (error) {
        console.error('GIF生成失败:', error)
        message.error('GIF生成失败，请重试')
        isGeneratingGif.value = false
        gifProgress.value = 0
    }
}

/**
 * 加载图片并返回 RGBA 数据及透明像素位置
 * 这个函数将透明像素的位置记录下来，而不是修改其颜色
 */
const loadImageDataForGif = async (url: string, targetSize: number): Promise<{
    rgba: Uint8ClampedArray,
    transparentPixels: number[]
}> => {
    const img = await loadImage(url)

    const canvas = document.createElement('canvas')
    canvas.width = targetSize
    canvas.height = targetSize
    const ctx = canvas.getContext('2d')!

    // 绘制影像
    ctx.drawImage(img, 0, 0, targetSize, targetSize)

    // 获取像素数据
    const imageData = ctx.getImageData(0, 0, targetSize, targetSize)
    const data = imageData.data

    // 记录透明像素的索引位置
    const transparentPixels: number[] = []

    for (let i = 0; i < data.length; i += 4) {
        const pixelIndex = i / 4  // 像素在索引图像中的位置

        if (data[i + 3] < 128) {
            // 透明像素：记录位置，设置为不透明（避免量化时出问题）
            transparentPixels.push(pixelIndex)
            // 设置一个不会影响量化的颜色（黑色）
            data[i] = 0
            data[i + 1] = 0
            data[i + 2] = 0
            data[i + 3] = 255
        }
    }

    return { rgba: data, transparentPixels }
}

/**
 * 获取影像URL用于GIF生成
 * 使用 /tiler/gif/{z}/{x}/{y}.png 端点，计算覆盖bbox的合适瓦片
 */
const getImageUrlForGif = async (img: MultiImageInfoType): Promise<string> => {
    let redPath = img.redPath
    let greenPath = img.greenPath
    let bluePath = img.bluePath

    const currentGridKey = `${grid.value.rowId}-${grid.value.columnId}-${grid.value.resolution}`
    const gridSuperRes = superResOverride.value.get(currentGridKey)
    if (gridSuperRes?.load === true && gridSuperRes.sceneId === img.sceneId) {
        redPath = gridSuperRes.redPath
        greenPath = gridSuperRes.greenPath
        bluePath = gridSuperRes.bluePath
    }

    // 1. 获取统计信息 (参考 handleClick 逻辑)
    const cache = ezStore.get('statisticCache')
    let [min_r, max_r, min_g, max_g, min_b, max_b, mean_r, mean_g, mean_b, std_r, std_g, std_b] = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

    if (cache.get(redPath) && cache.get(greenPath) && cache.get(bluePath)) {
        // console.log('GIF generation: cache hit!')
        ;[min_r, max_r, mean_r, std_r] = cache.get(redPath)
        ;[min_g, max_g, mean_g, std_g] = cache.get(greenPath)
        ;[min_b, max_b, mean_b, std_b] = cache.get(bluePath)
    } else if (img.dataType !== 'dem' && img.dataType !== 'dsm') {
        try {
            const values = await Promise.all([
                getImgStats(getMinIOUrl(redPath)),
                getImgStats(getMinIOUrl(greenPath)),
                getImgStats(getMinIOUrl(bluePath)),
            ])
            min_r = values[0].b1.min
            max_r = values[0].b1.max
            min_g = values[1].b1.min
            max_g = values[1].b1.max
            min_b = values[2].b1.min
            max_b = values[2].b1.max
            mean_r = values[0].b1.mean
            std_r = values[0].b1.std
            mean_g = values[1].b1.mean
            std_g = values[1].b1.std
            mean_b = values[2].b1.mean
            std_b = values[2].b1.std

            cache.set(redPath, [min_r, max_r, mean_r, std_r])
            cache.set(greenPath, [min_g, max_g, mean_g, std_g])
            cache.set(bluePath, [min_b, max_b, mean_b, std_b])
        } catch (e) {
            console.warn('Stats fetch failed for GIF frame', e)
        }
    }

    // 2. 计算瓦片坐标
    const bbox = grid2bbox(grid.value.columnId, grid.value.rowId, grid.value.resolution)
    const bboxStr = bbox.join(',')
    const tile = bboxToTile(bbox)

    // 3. 构建 URL
    // 注意：这里使用新创建的后端接口 /tiler/gif/...
    const titilerProxyEndPoint = ezStore.get('conf')['titiler']

    const baseUrl = `${titilerProxyEndPoint}/gif/${tile.z}/${tile.x}/${tile.y}.png`
    const params = new URLSearchParams()
    
    // 必须使用 getMinIOUrl 转换路径，确保后端 rio_tiler 能访问
    params.append('url_r', getMinIOUrl(redPath))
    params.append('url_g', getMinIOUrl(greenPath))
    params.append('url_b', getMinIOUrl(bluePath))
    
    params.append('min_r', min_r.toString())
    params.append('max_r', max_r.toString())
    params.append('min_g', min_g.toString())
    params.append('max_g', max_g.toString())
    params.append('min_b', min_b.toString())
    params.append('max_b', max_b.toString())
    
    params.append('nodata', (img.nodata || 0).toString())
    params.append('stretch_method', stretchMethod.value || 'gamma')
    params.append('normalize_level', scaleRate.value.toString())
    
    // 添加 bbox 参数，确保 GIF 帧位置正确
    params.append('bbox', bboxStr)
    // 请求用户选择的分辨率
    params.append('resolution', gifResolution.value.toString())

    if (stretchMethod.value === 'standard') {
        params.append('std_config', JSON.stringify({
            mean_r, mean_g, mean_b, 
            std_r, std_g, std_b
        }))
    }

    return `${baseUrl}?${params.toString()}`
}

/**
 * 将bbox转换为能完全覆盖该区域的瓦片坐标
 * 确保返回的瓦片完全包含整个bbox
 */
const bboxToTile = (bbox: number[]): { z: number, x: number, y: number } => {
    const [minLon, minLat, maxLon, maxLat] = bbox
    
    // 计算bbox的中心点
    const centerLon = (minLon + maxLon) / 2
    const centerLat = (minLat + maxLat) / 2
    
    // 根据bbox大小选择合适的zoom级别
    const lonSpan = maxLon - minLon
    const latSpan = maxLat - minLat
    const maxSpan = Math.max(lonSpan, latSpan)
    
    // 计算合适的zoom级别
    // 使用 floor - 1 来确保瓦片足够大，能完全包含bbox
    let z = Math.floor(Math.log2(360 / maxSpan)) - 1
    z = Math.max(1, Math.min(z, 18)) // 限制在1-18之间
    
    // 计算瓦片坐标
    const n = Math.pow(2, z)
    const x = Math.floor((centerLon + 180) / 360 * n)
    const latRad = centerLat * Math.PI / 180
    const y = Math.floor((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * n)
    
    // 验证瓦片是否完全包含bbox，如果不包含，降低zoom级别
    let tileBbox = tileToBbox(z, x, y)
    let iterations = 0
    while (iterations < 5 && (
        tileBbox[0] > minLon || tileBbox[1] > minLat || 
        tileBbox[2] < maxLon || tileBbox[3] < maxLat
    )) {
        z = Math.max(1, z - 1)
        const newN = Math.pow(2, z)
        const newX = Math.floor((centerLon + 180) / 360 * newN)
        const newLatRad = centerLat * Math.PI / 180
        const newY = Math.floor((1 - Math.log(Math.tan(newLatRad) + 1 / Math.cos(newLatRad)) / Math.PI) / 2 * newN)
        tileBbox = tileToBbox(z, newX, newY)
        iterations++
        if (tileBbox[0] <= minLon && tileBbox[1] <= minLat && 
            tileBbox[2] >= maxLon && tileBbox[3] >= maxLat) {
            return { z, x: newX, y: newY }
        }
    }
    
    return { z, x, y }
}

/**
 * 根据瓦片坐标计算瓦片的地理边界
 */
const tileToBbox = (z: number, x: number, y: number): number[] => {
    const n = Math.pow(2, z)
    const minLon = x / n * 360 - 180
    const maxLon = (x + 1) / n * 360 - 180
    const maxLatRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * y / n)))
    const minLatRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * (y + 1) / n)))
    const maxLat = maxLatRad * 180 / Math.PI
    const minLat = minLatRad * 180 / Math.PI
    return [minLon, minLat, maxLon, maxLat]
}

/**
 * 加载图片为HTMLImageElement
 */
const loadImage = (url: string): Promise<HTMLImageElement> => {
    return new Promise((resolve, reject) => {
        const img = new Image()
        img.crossOrigin = 'anonymous'
        img.onload = () => resolve(img)
        img.onerror = reject
        img.src = url
    })
}

/**
 * 加载瓦片图片并缩放到目标尺寸
 * 使用双重保护策略确保 GIF 透明正确：
 * 1. nodata 像素（alpha=0）设为品红色
 * 2. 有效像素如果接近品红，稍微调整避免被误判为透明
 */
const loadAndCropImageForGif = async (url: string, _bbox: number[], targetSize: number = 512): Promise<HTMLCanvasElement> => {
    const img = await loadImage(url)

    const canvas = document.createElement('canvas')
    canvas.width = targetSize
    canvas.height = targetSize
    const ctx = canvas.getContext('2d')!

    // 绘制影像到 canvas
    ctx.drawImage(img, 0, 0, targetSize, targetSize)

    // 读取像素数据进行处理
    const imageData = ctx.getImageData(0, 0, targetSize, targetSize)
    const data = imageData.data

    for (let i = 0; i < data.length; i += 4) {
        const r = data[i]
        const g = data[i + 1]
        const b = data[i + 2]
        const a = data[i + 3]

        if (a < 128) {
            // nodata 像素：设为纯品红作为透明标记
            data[i] = 255      // R
            data[i + 1] = 0    // G
            data[i + 2] = 255  // B
            data[i + 3] = 255  // A
        } else {
            // 有效像素：如果颜色接近品红，稍微调整 G 分量
            // 避免在 gif.js 量化时被错误地映射到透明色
            // 品红是 (255, 0, 255)，我们检查是否接近这个值
            if (r > 240 && g < 30 && b > 240) {
                data[i + 1] = 50  // 将 G 分量提高到 30，远离纯品红
            }
        }
    }

    ctx.putImageData(imageData, 0, 0)
    return canvas
}

/**
 * 更新数据
 */
const updateHandler = (
    _data: ImageInfoType[] | MultiImageInfoType[],
    _grid: GridData,
    _strechMethod: 'linear' | 'gamma' | 'standard' | '',
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
    stretchMethod.value = _strechMethod

    startDateFilter.value = ''
    endDateFilter.value = ''

    setDateRange()
}

// let runningSource: 'SuperResTimeLine' | 'cubeVisualize' | null = null;
const clearState = () => {
  activeIndex.value = -1;
  multiImages.value = [];
  productImages.value = [];
  scaleRate.value = 1.00;
  stretchMethod.value = 'gamma'
}

const superResOverride = ref<Map<string, {
  redPath: string
  greenPath: string
  bluePath: string
  load: boolean
  sceneId?: string
}>>(new Map())

// 彩蛋：按Enter直接超分
const { handleSuperResolution, handleSuperResolutionV2, handlePreSuperResolution, isSuperRes } = useSuperResolution()

const handleInteraction = (e) => {
  // 逻辑：如果是键盘的回车键 (Enter) 或者 鼠标的中键 (button === 1)
  // 注意：e.key 在鼠标事件中是 undefined，e.button 在键盘事件中通常也是 undefined 或不相关，所以用 || 连接没问题
  if (e.key === 'Enter' || e.button === 1) {
    
    // 【重要】鼠标中键通常会触发浏览器的“自动滚动”模式，建议阻止默认行为
    if (e.button === 1) {
        e.preventDefault(); 
    }

    console.log('触发成功（Enter 或 中键），当前索引是:', activeIndex.value)
    
    if (activeIndex.value !== -1) {
        handlePreSuperResolution()
    }
  }
}

onMounted(() => {
    clearState()
    bus.on('cubeVisualize', updateHandler)

    bus.on('SuperResTimeLine', (
      SuperData: { data: { R: string, G: string, B: string }, gridInfo: { rowId: number, columnId: number, resolution: number }, sceneId?: string },
      loadSuper: boolean
    ) => {
      const gridKey = `${SuperData.gridInfo.rowId}-${SuperData.gridInfo.columnId}-${SuperData.gridInfo.resolution}`
      const currentGridKey = `${grid.value.rowId}-${grid.value.columnId}-${grid.value.resolution}`

      if (gridKey === currentGridKey) {
        if (!loadSuper) {
          superResOverride.value.delete(gridKey)
          return
        }
        superResOverride.value.set(gridKey, {
          redPath: SuperData.data.R,
          greenPath: SuperData.data.G,
          bluePath: SuperData.data.B,
          load: true,
          sceneId: SuperData.sceneId
        })
        if (activeIndex.value >= 0) {
          handleClick(activeIndex.value)
        }
      }
    })

    bus.on('closeTimeline', () => {
        activeIndex.value = -1
        multiImages.value = []
        productImages.value = []
        scaleRate.value = 1.00
        stretchMethod.value = 'gamma'
        // 清除当前格网的超分状态
        const currentGridKey = `${grid.value.rowId}-${grid.value.columnId}-${grid.value.resolution}`
        superResOverride.value.delete(currentGridKey)
    })
    window.addEventListener('keydown', handleInteraction)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleInteraction)
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

/* GIF生成按钮样式 */
.gif-generator {
    padding: 0 0.5rem;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.35rem;
}

.resolution-toggle-group {
    display: flex;
    border-radius: 4px;
    overflow: hidden;
    border: 1px solid rgba(108, 253, 255, 0.3);
}

.resolution-toggle-btn {
    padding: 0.2rem 0.4rem;
    background: transparent;
    border: none;
    color: rgba(255, 255, 255, 0.6);
    font-size: 0.65rem;
    cursor: pointer;
    transition: all 0.2s ease;
    min-width: 32px;
}

.resolution-toggle-btn:not(:last-child) {
    border-right: 1px solid rgba(108, 253, 255, 0.2);
}

.resolution-toggle-btn:hover:not(:disabled):not(.active) {
    background: rgba(108, 253, 255, 0.1);
    color: rgba(255, 255, 255, 0.8);
}

.resolution-toggle-btn.active {
    background: rgba(108, 253, 255, 0.25);
    color: #6cfdff;
}

.resolution-toggle-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.gif-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0.4rem 0.8rem;
    background: linear-gradient(135deg, rgba(108, 253, 255, 0.2), rgba(77, 171, 247, 0.2));
    border: 1px solid rgba(108, 253, 255, 0.4);
    border-radius: 6px;
    color: #6cfdff;
    font-size: 0.8rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s ease;
    min-width: 70px;
}

.gif-btn:hover:not(:disabled) {
    background: linear-gradient(135deg, rgba(108, 253, 255, 0.3), rgba(77, 171, 247, 0.3));
    border-color: rgba(108, 253, 255, 0.7);
    box-shadow: 0 0 10px rgba(108, 253, 255, 0.3);
}

.gif-btn:active:not(:disabled) {
    transform: translateY(0);
}

.gif-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background: rgba(100, 100, 100, 0.2);
    border-color: rgba(100, 100, 100, 0.3);
    color: rgba(255, 255, 255, 0.5);
}

.gif-btn .animate-spin {
    animation: spin 1s linear infinite;
}

@keyframes spin {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
    }
}
</style>
