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

            <!-- GIF生成按钮 -->
            <div class="gif-generator flex flex-col items-center justify-center">
                <button
                    class="gif-btn"
                    @click="handleGenerateGif"
                    :disabled="filteredImages.length < 2 || isGeneratingGif"
                    :title="filteredImages.length < 2 ? '需要至少2张影像' : '生成GIF动画'"
                >
                    <FilmIcon v-if="!isGeneratingGif" :size="20" />
                    <LoaderIcon v-else :size="20" class="animate-spin" />
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
const scaleRate = ref(1.00)
const stretchMethod = ref<'linear' | 'gamma' | 'standard' | ''>('gamma')
const grid = ref<GridData>({ rowId: 0, columnId: 0, resolution: 0, opacity: 0, normalize_level: 0, sceneRes: { total: 0, category: [] }, vectors: [], themeRes: { total: 0, category: [] } })
const activeIndex = ref(-1)
const visualMode = ref<'rgb' | 'product'>('rgb')
const timelineTrack = ref<HTMLElement | null>(null)

// GIF生成相关状态
const isGeneratingGif = ref(false)
const gifProgress = ref(0)

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
            std_config: JSON.stringify({mean_r, mean_g, mean_b, std_r, std_g, std_b})
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
 */
const handleGenerateGif = async () => {
    if (filteredImages.value.length < 2) {
        message.warning('需要至少2张影像才能生成GIF')
        return
    }

    isGeneratingGif.value = true
    gifProgress.value = 0

    try {
        // 动态导入gif.js
        const GIF = (await import('gif.js')).default
        
        const images = filteredImages.value
        const totalImages = images.length
        
        // 创建GIF编码器，设置透明背景
        const gif = new GIF({
            workers: 2,
            quality: 10,
            width: 512,
            height: 512,
            workerScript: '/gif.worker.js', // 需要确保这个文件存在于public目录
            transparent: 0x000000 // 将黑色设为透明色
        } as any)

        // 加载每张影像并添加到GIF
        for (let i = 0; i < totalImages; i++) {
            const img = images[i] as MultiImageInfoType
            
            try {
                // 获取影像URL（与handleClick类似的逻辑）
                const imageUrl = await getImageUrlForGif(img)
                
                // 获取bbox用于裁剪
                const bbox = grid2bbox(grid.value.columnId, grid.value.rowId, grid.value.resolution)
                
                // 加载图片并裁剪到bbox区域，缩放到512x512
                const croppedCanvas = await loadAndCropImageForGif(imageUrl, bbox, 512)
                
                // 添加到GIF（每帧延迟500ms）
                gif.addFrame(croppedCanvas, { delay: 500 })
                
                // 更新进度
                gifProgress.value = Math.round(((i + 1) / totalImages) * 80)
            } catch (err) {
                console.warn(`跳过第${i + 1}张影像:`, err)
            }
        }

        // 监听GIF渲染进度
        gif.on('progress', (p: number) => {
            gifProgress.value = 80 + Math.round(p * 20)
        })

        // 渲染完成后在地图上显示GIF
        gif.on('finished', (blob: Blob) => {
            // 创建Blob URL用于显示
            const gifBlobUrl = URL.createObjectURL(blob)
            
            // 在地图格网上显示GIF动画
            GridExploreMapOps.map_addGridGifLayer(grid.value, gifBlobUrl)

            isGeneratingGif.value = false
            gifProgress.value = 0
            message.success('GIF生成成功！已在格网中显示')
        })

        gif.render()
    } catch (error) {
        console.error('GIF生成失败:', error)
        message.error('GIF生成失败，请重试')
        isGeneratingGif.value = false
        gifProgress.value = 0
    }
}

/**
 * 获取影像URL用于GIF生成
 * 使用 /rgb/box/{z}/{x}/{y}.png 端点，计算覆盖bbox的合适瓦片
 */
const getImageUrlForGif = async (img: MultiImageInfoType): Promise<string> => {
    let redPath = img.redPath
    let greenPath = img.greenPath
    let bluePath = img.bluePath

    const cache = ezStore.get('statisticCache')
    let [min_r, max_r, min_g, max_g, min_b, max_b, mean_r, mean_g, mean_b, std_r, std_g, std_b] = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

    if (cache.get(redPath) && cache.get(greenPath) && cache.get(bluePath)) {
        ;[min_r, max_r, mean_r, std_r] = cache.get(redPath)
        ;[min_g, max_g, mean_g, std_g] = cache.get(greenPath)
        ;[min_b, max_b, mean_b, std_b] = cache.get(bluePath)
    } else if (img.dataType !== 'dem' && img.dataType !== 'dsm') {
        const [stats_r, stats_g, stats_b] = await Promise.all([
            getImgStats(getMinIOUrl(redPath)),
            getImgStats(getMinIOUrl(greenPath)),
            getImgStats(getMinIOUrl(bluePath)),
        ])
        min_r = stats_r.b1.min
        max_r = stats_r.b1.max
        min_g = stats_g.b1.min
        max_g = stats_g.b1.max
        min_b = stats_b.b1.min
        max_b = stats_b.b1.max
        mean_r = stats_r.b1.mean
        std_r = stats_r.b1.std
        mean_g = stats_g.b1.mean
        std_g = stats_g.b1.std
        mean_b = stats_b.b1.mean
        std_b = stats_b.b1.std

        cache.set(redPath, [min_r, max_r, mean_r, std_r])
        cache.set(greenPath, [min_g, max_g, mean_g, std_g])
        cache.set(bluePath, [min_b, max_b, mean_b, std_b])
    }

    const bbox = grid2bbox(grid.value.columnId, grid.value.rowId, grid.value.resolution)
    
    // titiler配置可能是代理路径(如/tiler)，GIF生成需要完整URL来跨域加载图片
    const titilerEndPoint = ezStore.get('conf')['titiler']
    const minioEndPoint = ezStore.get('conf')['minioIpAndPort']
    
    // 如果是相对路径，拼接当前origin
    const fullTitilerUrl = titilerEndPoint.startsWith('http') 
        ? titilerEndPoint 
        : `${window.location.origin}${titilerEndPoint}`
    
    // 计算覆盖bbox的瓦片坐标（选择合适的zoom级别）
    const { z, x, y } = bboxToTile(bbox)
    
    const requestParams = new URLSearchParams()
    requestParams.append('bbox', bbox.join(','))
    requestParams.append('url_r', `${minioEndPoint}/${redPath}`)
    requestParams.append('url_g', `${minioEndPoint}/${greenPath}`)
    requestParams.append('url_b', `${minioEndPoint}/${bluePath}`)
    requestParams.append('min_r', min_r.toString())
    requestParams.append('max_r', max_r.toString())
    requestParams.append('min_g', min_g.toString())
    requestParams.append('max_g', max_g.toString())
    requestParams.append('min_b', min_b.toString())
    requestParams.append('max_b', max_b.toString())
    if (scaleRate.value) requestParams.append('normalize_level', scaleRate.value.toString())
    if (stretchMethod.value) requestParams.append('stretch_method', stretchMethod.value)
    if (img.nodata) requestParams.append('nodata', img.nodata.toString())
    requestParams.append('std_config', JSON.stringify({ mean_r, mean_g, mean_b, std_r, std_g, std_b }))

    // 使用 /rgb/box/{z}/{x}/{y}.png 端点
    return `${fullTitilerUrl}/rgb/box/${z}/${x}/${y}.png?${requestParams.toString()}`
}

/**
 * 将bbox转换为覆盖该区域的瓦片坐标
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
    
    // 计算合适的zoom级别（使bbox大致占满一个瓦片）
    // 每个瓦片在zoom z时覆盖 360/2^z 度
    let z = Math.floor(Math.log2(360 / maxSpan))
    z = Math.max(1, Math.min(z, 18)) // 限制在1-18之间
    
    // 计算瓦片坐标
    const x = Math.floor((centerLon + 180) / 360 * Math.pow(2, z))
    const latRad = centerLat * Math.PI / 180
    const y = Math.floor((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * Math.pow(2, z))
    
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
 * 加载瓦片图片并裁剪bbox区域，缩放到目标尺寸
 */
const loadAndCropImageForGif = async (url: string, bbox: number[], targetSize: number = 512): Promise<HTMLCanvasElement> => {
    // 加载原始瓦片图片
    const img = await loadImage(url)
    
    // 获取瓦片坐标和瓦片边界
    const { z, x, y } = bboxToTile(bbox)
    const tileBbox = tileToBbox(z, x, y)
    
    const [tMinLon, tMinLat, tMaxLon, tMaxLat] = tileBbox
    const [bMinLon, bMinLat, bMaxLon, bMaxLat] = bbox
    
    // 瓦片尺寸（通常是256）
    const tileSize = img.width || 256
    
    // 计算bbox在瓦片中的像素位置
    // 注意：y轴是从上到下的，所以lat的计算要反过来
    const srcX = Math.max(0, (bMinLon - tMinLon) / (tMaxLon - tMinLon) * tileSize)
    const srcY = Math.max(0, (tMaxLat - bMaxLat) / (tMaxLat - tMinLat) * tileSize)
    const srcW = Math.min(tileSize - srcX, (bMaxLon - bMinLon) / (tMaxLon - tMinLon) * tileSize)
    const srcH = Math.min(tileSize - srcY, (bMaxLat - bMinLat) / (tMaxLat - tMinLat) * tileSize)
    
    // 创建目标Canvas
    const canvas = document.createElement('canvas')
    canvas.width = targetSize
    canvas.height = targetSize
    const ctx = canvas.getContext('2d')!
    
    // 裁剪bbox区域并缩放到目标尺寸
    if (srcW > 0 && srcH > 0) {
        ctx.drawImage(img, srcX, srcY, srcW, srcH, 0, 0, targetSize, targetSize)
    }
    
    // 将黑色/近黑色像素转为透明（处理nodata区域）
    const imageData = ctx.getImageData(0, 0, targetSize, targetSize)
    const data = imageData.data
    const threshold = 10 // 允许一定的容差，处理近黑色像素
    
    for (let i = 0; i < data.length; i += 4) {
        const r = data[i]
        const g = data[i + 1]
        const b = data[i + 2]
        // 如果RGB值都接近0（黑色），设为完全透明
        if (r < threshold && g < threshold && b < threshold) {
            data[i] = 0     // R
            data[i + 1] = 0 // G
            data[i + 2] = 0 // B
            data[i + 3] = 0 // Alpha设为0（透明）
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
const { handleSuperResolution, isSuperRes } = useSuperResolution()

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
        handleSuperResolution()
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
}

.gif-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0.5rem 1rem;
    background: linear-gradient(135deg, rgba(108, 253, 255, 0.2), rgba(77, 171, 247, 0.2));
    border: 1px solid rgba(108, 253, 255, 0.4);
    border-radius: 8px;
    color: #6cfdff;
    font-size: 0.85rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s ease;
    min-width: 80px;
}

.gif-btn:hover:not(:disabled) {
    background: linear-gradient(135deg, rgba(108, 253, 255, 0.3), rgba(77, 171, 247, 0.3));
    border-color: rgba(108, 253, 255, 0.7);
    box-shadow: 0 0 15px rgba(108, 253, 255, 0.3);
    transform: translateY(-1px);
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
