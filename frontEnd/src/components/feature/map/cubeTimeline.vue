<template>
    <div class="timeline" v-if="show">
        <ChevronLeftIcon :size="32" class="w-10 cursor-pointer" />
        <div class="flex-1 flex flex-row justify-evenly items-center">
            <div v-for="(item, index) in images" :key="index" class="timeline-item"
                :class="{ active: index === activeIndex }" @click="handleClick(index)">
                <div class="dot"></div>
                <div class="label">{{ item.time }}</div>
            </div>
        </div>
        <ChevronRightIcon :size="32" class="w-10 cursor-pointer" />
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ChevronLeftIcon, ChevronRightIcon } from 'lucide-vue-next'

import { getGridImage } from '@/api/http/satellite-data/visualize.api'
import { grid2Coordinates } from '@/util/map/gridMaker'
import { ezStore } from '@/store'
import bus from '@/store/bus'
import * as MapOperation from '@/util/map/operation'

type ImageInfoType = {
    sceneId: string
    time: string
    tifFullPath: string
}
type GridInfoType = {
    rowId: number
    columnId: number
    resolution: number
}

const show = defineModel<boolean>()
const images = ref<ImageInfoType[]>([{ sceneId: '', time: '', tifFullPath: '' }])
const grid = ref<GridInfoType>({ rowId: 0, columnId: 0, resolution: 0 })
const activeIndex = ref(0)

const handleClick = async (index: number) => {

    activeIndex.value = index
    const img: ImageInfoType = images.value[activeIndex.value]

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

const updateHandler = (_data: ImageInfoType[], _grid: GridInfoType) => {
    images.value = _data
    grid.value = _grid
}

onMounted(() => {

    bus.on('cubeVisualize', updateHandler)
    bus.on('closeTimeline', () => {
        activeIndex.value = 0
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
