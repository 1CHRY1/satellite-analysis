<template>
    <div class="timeline" v-if="show">
        <ChevronLeftIcon :size="32" class="w-10 cursor-pointer" @click="handleClick(activeIndex - 1)" />
        <div class="flex-1 flex flex-row justify-evenly items-center">
            <div v-for="(item, index) in showingImages" :key="index" class="timeline-item"
                :class="{ active: index === activeIndex }" @click="handleClick(index)">
                <div class="dot"></div>
                <div class="label">{{ item.time }}</div>
            </div>
        </div>
        <ChevronRightIcon :size="32" class="w-10 cursor-pointer" @click="handleClick(activeIndex + 1)" />
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
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

const showingImages = computed(() => {
    if (visualMode.value === 'single') {
        return singleImages.value
    } else {
        return multiImages.value
    }
})


const handleClick = async (index: number) => {

    if (index < 0) return
    if (visualMode.value === 'single' && index > singleImages.value.length - 1) return
    if (visualMode.value === 'rgb' && index > multiImages.value.length - 1) return

    activeIndex.value = index

    if (visualMode.value === 'single') {
        const img: ImageInfoType = showingImages[index]

        const imgB64Path = await getGridImage({
            rowId: grid.value.rowId,
            columnId: grid.value.columnId,
            resolution: grid.value.resolution,
            tifFullPath: img.tifFullPath
        })

        const gridCoords = grid2Coordinates(grid.value.columnId, grid.value.rowId, grid.value.resolution)
        const prefix = grid.value.rowId + '' + grid.value.columnId
        MapOperation.map_addGridPreviewLayer(imgB64Path, gridCoords, prefix)
    }
    else if (visualMode.value === 'rgb') {

        const img: MultiImageInfoType = showingImages[index]

        const mergeGridBandParam = {
            rowId: grid.value.rowId,
            columnId: grid.value.columnId,
            resolution: grid.value.resolution,
            redPath: img.redPath,
            greenPath: img.greenPath,
            bluePath: img.bluePath
        }

        bandMergeHelper.mergeGrid(mergeGridBandParam, (mergedImgUrl: string) => {
            const gridCoords = grid2Coordinates(grid.value.columnId, grid.value.rowId, grid.value.resolution)
            const prefix = grid.value.rowId + '' + grid.value.columnId
            MapOperation.map_addGridPreviewLayer(mergedImgUrl, gridCoords, prefix)
        })
    }
}

const updateHandler = (_data: ImageInfoType[] | MultiImageInfoType[], _grid: GridInfoType, mode: 'single' | 'rgb') => {

    grid.value = _grid
    visualMode.value = mode
    if (mode === 'single') {
        singleImages.value = _data as ImageInfoType[]
    } else {
        multiImages.value = _data as MultiImageInfoType[]
    }

}

onMounted(() => {

    bus.on('cubeVisualize', updateHandler)
    bus.on('closeTimeline', () => {
        activeIndex.value = -1
    })

})

</script>

<style scoped>
.timeline {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    gap: 10px;
    padding: 20px;
    padding-left: 5px;
    padding-right: 5px;
    min-width: 30vw;
    background-color: rgba(164, 213, 197, 0.5);
    backdrop-filter: blur(10px);
    border-radius: 10px;
}

.timeline-item {
    cursor: pointer;
    text-align: center;
    height: 30px;
}

.dot {
    width: 16px;
    height: 16px;
    background-color: rgb(0, 22, 63);
    border-radius: 50%;
    margin: 0 auto;
    transition: background-color 0.3s;
}

.label {
    margin-top: 8px;
    font-size: 0.75rem;
}

.active .dot {
    background-color: #6cfdff;
}
</style>
