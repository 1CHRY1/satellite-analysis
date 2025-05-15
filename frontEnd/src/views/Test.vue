<template>
    <div class="absolute top-0 left-0 h-screen w-screen">
        <div class="h-[92vh]" id="map"></div>
        <!-- <div class="absolute top-10 right-10 z-10 flex flex-col gap-y-4">
            
        </div> -->
        <!-- <PopoverContent class="absolute top-10 left-10" :scenes="mockScenes" :grid="mockGrid"></PopoverContent> -->
    </div>
</template>

<script setup lang="ts">
import { onMounted, createApp, type ComponentInstance, ref } from 'vue'
import { mapManager } from '@/util/map/mapManager'
import * as Operations from '@/util/map/operation'
import PopoverContent from '@/components/feature/map/popoverContent.vue'

type Scene = {
    sceneId: string;
    sceneName: string;
    bands: string[];
    [key: string]: any;
}
type Grid = {
    rowId: number
    columnId: number
    resolution: number
}
const mockScenes: Scene[] = [
    {
        sceneId: '1',
        sceneName: 'scene1',
        bands: ['B1', 'B2', 'B3'],
    },
    {
        sceneId: '2',
        sceneName: 'scene2',
        bands: ['B1', 'B2', 'B3'],
    },
    {
        sceneId: '3',
        sceneName: 'scene3',
        bands: ['B1', 'B2', 'B3'],
    },
    {
        sceneId: '4',
        sceneName: 'scene4',
        bands: ['B1', 'B2', 'B3'],
    }
]
const mockScenes2: Scene[] = [
    {
        sceneId: '1',
        sceneName: 'scene1',
        bands: ['b1', 'b2'],
    },
    {
        sceneId: '2',
        sceneName: 'scene2',
        bands: ['b1', 'b2'],
    }
]

const mockGrid: Grid = {
    rowId: 1,
    columnId: 1,
    resolution: 30
}

const mockGrid2: Grid = {
    rowId: 1,
    columnId: 1,
    resolution: 10
}

const scenesRef = ref<Scene[]>(mockScenes)
const gridRef = ref<Grid>(mockGrid)


let popoverContentIns: null | ComponentInstance<typeof PopoverContent> = null
function createPopoverContent() {
    const div = document.createElement('div')
    div.id = 'popover-content'
    document.body.appendChild(div)

    const app = createApp(PopoverContent, {
        scenes: scenesRef,
        grid: gridRef
    })
    popoverContentIns = app.mount('#popover-content') as ComponentInstance<typeof PopoverContent>
    console.log(popoverContentIns)

}

function updatePopoverContent(scenes: Scene[], grid: Grid) {
    scenesRef.value = scenes
    gridRef.value = grid
}

onMounted(() => {

    createPopoverContent()

    window.addEventListener('keydown', e => {
        if (e.key === '1') {
            updatePopoverContent(mockScenes2, mockGrid2)
        }else if (e.key === '2') {
            updatePopoverContent(mockScenes, mockGrid)
        }
    })

    mapManager.init('map', 'vector', 'mercator').then((map) => {
        // map.zoomTo(10)

        // map.showTileBoundaries = true
        ///// rio-tiler-test /////////////////////////
        // if (true) {
        //     map.addSource('src', {
        //         type: 'raster',
        //         tiles: [
        //             // /tiles/WebMercatorQuad/9/428/208?scale=1&format=tif&url=http%3A%2F%2F223.2.43.228%3A30900%2Ftest-images%2Flandset8_test%252Flandset8_L2SP_test%252Ftif%252FLC08_L2SP_118038_20241201_20241203_02_T1%252FLC08_L2SP_118038_20241201_20241203_02_T1_SR_B6.TIF&bidx=1&unscale=false&resampling=nearest&reproject=nearest&return_mask=true
        //             // no cog
        //             // "http://223.2.32.242:8079/{z}/{x}/{y}.png?object=/test-images/landsat/landset7/tif/LE07_L1TP_122039_20210212_20210212_01_RT/LE07_L1TP_122039_20210212_20210212_01_RT_B1.TIF"
        //             'http://localhost:8000/tiles/WebMercatorQuad/{z}/{x}/{y}?scale=1&format=png&url=http://223.2.43.228:30900/test-images/qa/LC08_L2SP_120035_20250116_20250127_02_T1_SR_B1.TIF&bidx=1&unscale=false&resampling=nearest&reproject=nearest&return_mask=true',
        //             // "http://223.2.32.242:8079/{z}/{x}/{y}.png?object=/test-images/landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20241217_20241227_02_T1/LC08_L2SP_118038_20241217_20241227_02_T1_SR_B4.TIF"
        //         ],
        //     })
        //     map.addLayer({
        //         id: 'layer',
        //         type: 'raster',
        //         source: 'src',
        //         minzoom: 0,
        //     })
        // }
    })
})
</script>
