<template>
    <div class="timeline-container" v-if="show">
        <div class="timeline">
            <!-- 左侧日期筛选 -->
            <div class="date-filter start-filter">
                <div class="filter-label">起始日期</div>
                <div class="date-selector">
                    <input type="date" v-model="startDateFilter" :min="minDate" :max="endDateFilter || maxDate"
                        @change="applyDateFilter" />
                </div>
            </div>

            <div class="timeline-wrapper">
                <button class="nav-button" @click="handleClick(activeIndex - 1)" :disabled="activeIndex <= 0">
                    <ChevronLeftIcon :size="24" />
                </button>

                <div class="timeline-track" ref="timelineTrack">
                    <div v-for="(item, index) in filteredImages" :key="index" class="timeline-item"
                        :class="{ active: index === activeIndex }" @click="handleClick(index)">
                        <div class="dot-container">
                            <div class="dot"></div>
                            <div class="connector" v-if="index < filteredImages.length - 1"></div>
                        </div>
                        <div class="label">{{ timeFormat(item.time) }}</div>
                    </div>
                </div>

                <button class="nav-button" @click="handleClick(activeIndex + 1)"
                    :disabled="activeIndex >= filteredImages.length - 1">
                    <ChevronRightIcon :size="24" />
                </button>
            </div>

            <!-- 右侧日期筛选 -->
            <div class="date-filter end-filter">
                <div class="filter-label">结束日期</div>
                <div class="date-selector">
                    <input type="date" v-model="endDateFilter" :min="startDateFilter || minDate" :max="maxDate"
                        @change="applyDateFilter" />
                </div>
            </div>
        </div>
    </div>
</template>
  
<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { ChevronLeftIcon, ChevronRightIcon } from 'lucide-vue-next'

import { getGridImage } from '@/api/http/satellite-data/visualize.api'
import { grid2Coordinates } from '@/util/map/gridMaker'
import bus from '@/store/bus'
import * as MapOperation from '@/util/map/operation'
import bandMergeHelper from '@/util/image/util'

type ImageInfoType = {
    sceneId: string
    time: string
    tifFullPath: string
}
type MultiImageInfoType = {
    sceneId: string
    time: string
    redPath: string
    greenPath: string
    bluePath: string
}
type GridInfoType = {
    rowId: number
    columnId: number
    resolution: number
}

const show = defineModel<boolean>()
const singleImages = ref<ImageInfoType[]>([{ sceneId: '', time: '', tifFullPath: '' }])
const multiImages = ref<MultiImageInfoType[]>([{ sceneId: '', time: '', redPath: '', greenPath: '', bluePath: '' }])
const grid = ref<GridInfoType>({ rowId: 0, columnId: 0, resolution: 0 })
const activeIndex = ref(-1)
const visualMode = ref<'single' | 'rgb'>('single')
const timelineTrack = ref<HTMLElement | null>(null)

// 日期筛选
const startDateFilter = ref('')
const endDateFilter = ref('')
const minDate = ref('')
const maxDate = ref('')

const showingImages = computed(() => {
    if (visualMode.value === 'single') {
        return singleImages.value
    } else {
        return multiImages.value
    }
})

// 筛选后的图像数据
const filteredImages = computed(() => {

    let images
    if (visualMode.value === 'single') {
        images = singleImages.value as ImageInfoType[]
    } else {
        images = multiImages.value as MultiImageInfoType[]
    }
    console.log('all image', images)

    if (startDateFilter.value) {
        images = images.filter(item => new Date(item.time) >= new Date(startDateFilter.value));
    }

    if (endDateFilter.value) {
        images = images.filter(item => new Date(item.time) <= new Date(endDateFilter.value));
    }

    console.log('filteredImages', images);

    return images;
})

// 监听筛选后的数据变化，重置活动索引
watch(filteredImages, () => {
    if (activeIndex.value >= filteredImages.value.length) {
        activeIndex.value = filteredImages.value.length > 0 ? 0 : -1;
    }
}, { deep: true })

const timeFormat = (timeString: string) => {
    const date = new Date(timeString);

    if (isNaN(date.getTime())) {
        console.error(`Invalid date string: ${timeString}`);
        return 'unknown';
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}

// 应用日期筛选
const applyDateFilter = () => {
    // 如果活动索引超出了筛选后的范围，重置为第一个
    if (activeIndex.value >= filteredImages.value.length || activeIndex.value < 0) {
        activeIndex.value = filteredImages.value.length > 0 ? 0 : -1;

        // 如果有有效的活动索引，触发点击事件以显示对应的图像
        if (activeIndex.value >= 0) {
            handleClick(activeIndex.value);
        }
    }
}

// 设置日期范围
const setDateRange = () => {
    if (showingImages.value.length > 0) {
        const dates = showingImages.value.map(item => new Date(item.time)).sort((a, b) => a.getTime() - b.getTime());

        if (dates.length > 0 && !isNaN(dates[0].getTime())) {
            const firstDate = dates[0];
            const lastDate = dates[dates.length - 1];

            minDate.value = firstDate.toISOString().split('T')[0];
            maxDate.value = lastDate.toISOString().split('T')[0];

            // 只在初始化时设置筛选器的默认值
            if (!startDateFilter.value) {
                startDateFilter.value = minDate.value;
            }
            if (!endDateFilter.value) {
                endDateFilter.value = maxDate.value;
            }
        }
    }
}

const handleClick = async (index: number) => {
    if (index < 0 || index >= filteredImages.value.length) return;

    activeIndex.value = index;

    // 确保选中的点在视图中居中
    if (timelineTrack.value) {
        const items = timelineTrack.value.querySelectorAll('.timeline-item');
        if (items[index]) {
            const itemWidth = items[index].clientWidth;
            const trackWidth = timelineTrack.value.clientWidth;
            // @ts-ignore Property 'offsetLeft' does not exist on type 'Element'.ts(2339
            const scrollPosition = items[index].offsetLeft - (trackWidth / 2) + (itemWidth / 2);

            timelineTrack.value.scrollTo({
                left: scrollPosition,
                behavior: 'smooth'
            });
        }
    }

    const currentImage = filteredImages.value[index];

    if (visualMode.value === 'single') {
        const img = currentImage as ImageInfoType;

        const imgB64Path = await getGridImage({
            rowId: grid.value.rowId,
            columnId: grid.value.columnId,
            resolution: grid.value.resolution,
            tifFullPath: img.tifFullPath
        });
        const gridCoords = grid2Coordinates(grid.value.columnId, grid.value.rowId, grid.value.resolution);
        const prefix = grid.value.rowId + '' + grid.value.columnId;
        MapOperation.map_addGridPreviewLayer(imgB64Path, gridCoords, prefix);
    }
    else if (visualMode.value === 'rgb') {
        const img = currentImage as MultiImageInfoType;

        const mergeGridBandParam = {
            rowId: grid.value.rowId,
            columnId: grid.value.columnId,
            resolution: grid.value.resolution,
            redPath: img.redPath,
            greenPath: img.greenPath,
            bluePath: img.bluePath
        };
        bandMergeHelper.mergeGrid(mergeGridBandParam, (mergedImgUrl: string) => {
            const gridCoords = grid2Coordinates(grid.value.columnId, grid.value.rowId, grid.value.resolution);
            const prefix = grid.value.rowId + '' + grid.value.columnId;
            MapOperation.map_addGridPreviewLayer(mergedImgUrl, gridCoords, prefix);
        });
    }
}

const updateHandler = (_data: ImageInfoType[] | MultiImageInfoType[], _grid: GridInfoType, mode: 'single' | 'rgb') => {
    activeIndex.value = -1;
    grid.value = _grid;
    visualMode.value = mode;

    if (mode === 'single') {
        singleImages.value = _data as ImageInfoType[];
    } else {
        multiImages.value = _data as MultiImageInfoType[];
    }

    console.log('updateHandler', singleImages.value, multiImages.value)

    startDateFilter.value = '';
    endDateFilter.value = '';

    setDateRange();
}

onMounted(() => {
    bus.on('cubeVisualize', updateHandler);
    bus.on('closeTimeline', () => {
        activeIndex.value = -1;
    });
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
    height: 60px;
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
  