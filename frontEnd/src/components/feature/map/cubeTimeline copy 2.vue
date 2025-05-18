<template>
    <div class="timeline-container" v-if="show">
      <div class="timeline">
        <button class="nav-button" @click="handleClick(activeIndex - 1)" :disabled="activeIndex <= 0">
          <ChevronLeftIcon :size="24" />
        </button>
        
        <div class="timeline-track">
          <div v-for="(item, index) in showingImages" 
               :key="index" 
               class="timeline-item"
               :class="{ active: index === activeIndex }" 
               @click="handleClick(index)">
            <div class="dot-container">
              <div class="dot"></div>
              <div class="connector" v-if="index < showingImages.length - 1"></div>
            </div>
            <div class="label">{{ timeFormat(item.time) }}</div>
          </div>
        </div>
        
        <button class="nav-button" @click="handleClick(activeIndex + 1)" 
                :disabled="(visualMode === 'single' && activeIndex >= singleImages.length - 1) || 
                           (visualMode === 'rgb' && activeIndex >= multiImages.length - 1)">
          <ChevronRightIcon :size="24" />
        </button>
      </div>
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
      return [...singleImages.value, ...singleImages.value, ...singleImages.value]
    } else {
      return multiImages.value
    }
  })
  
  const timeFormat = (timeString: string) => {
    const date = new Date(timeString);
  
    if (isNaN(date.getTime())) {
      console.error(`Invalid date string: ${timeString}`);
      return 'unknown';
    }
  
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0'); // Month is 0-indexed, pad with leading zero if needed
    const day = String(date.getDate()).padStart(2, '0');       // Pad with leading zero if needed
  
    return `${year}-${month}-${day}`;
  }
  
  const handleClick = async (index: number) => {
    if (index < 0) return
    if (visualMode.value === 'single' && index > singleImages.value.length - 1) return
    if (visualMode.value === 'rgb' && index > multiImages.value.length - 1) return
  
    activeIndex.value = index
  
    if (visualMode.value === 'single') {
      const img = showingImages.value[index] as ImageInfoType
  
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
      const img = showingImages.value[index] as MultiImageInfoType
  
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
    activeIndex.value = -1
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
    height: 80px;
    min-width: 30vw;
    max-width: 600px;
    background: rgba(18, 25, 38, 0.75);
    backdrop-filter: blur(10px);
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
    overflow: hidden;
    padding: 0 0.5rem;
    border: 1px solid rgba(255, 255, 255, 0.1);
  }
  
  .timeline-track {
    display: flex;
    flex: 1;
    justify-content: space-evenly;
    align-items: center;
    overflow-x: auto;
    scrollbar-width: none; /* Firefox */
    padding: 0 0.5rem;
  }
  
  .timeline-track::-webkit-scrollbar {
    display: none; /* Chrome, Safari, Edge */
  }
  
  .timeline-item {
    cursor: pointer;
    text-align: center;
    height: 60px;
    min-width: 70px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    position: relative;
    transition: transform 0.2s ease;
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
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: rgba(108, 253, 255, 0.1);
    border: 1px solid rgba(108, 253, 255, 0.3);
    color: #6cfdff;
    cursor: pointer;
    transition: all 0.2s ease;
    z-index: 3;
  }
  
  .nav-button:hover:not(:disabled) {
    background: rgba(108, 253, 255, 0.2);
    transform: scale(1.05);
  }
  
  .nav-button:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
  </style>
  