<template>
    <div class="absolute top-0 left-0 h-screen w-screen">
        <div class="h-[92vh]" id="map"></div>
        <div class="absolute top-10 right-10 z-10 flex flex-col gap-y-4">
            <button class="h-10 w-24 bg-amber-950 text-lg" @click="() => Operations.draw_polygonMode()">
                绘制多边形
            </button>
            <button class="h-10 w-24 bg-amber-950 text-lg" @click="() => Operations.map_destroyGridLayer()">
                删除格网
            </button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { mapManager } from '@/util/map/mapManager'
import * as Operations from '@/util/map/operation'
import { useGridStore } from '@/store'
import { GridMaker } from '@/util/map/gridMaker'
import { Modal } from 'ant-design-vue'
const gridStore = useGridStore()

watch(
    () => gridStore.polygon,
    (newVal) => {
        if (newVal) {
            //保证绘制后触发，清空时不触发
            showConfirm()
        }
    },
)

const showConfirm = () => {
    Modal.confirm({
        title: '确认选择该区域?',
        okText: '确认并生成格网',
        okType: 'primary',
        cancelText: '取消',
        onOk() {
            console.log('确认并生成格网')
            makeGrid()
        },
        onCancel() {
            console.log('取消')
            Operations.draw_deleteAll()
        },
    })
}
const makeGrid = () => {
    if (!gridStore.polygon) {
        console.log('😠 怎么可能呢, polygon为空!')
        return
    }

    const gridMaker = new GridMaker(1000) // 当前只有 1km分辨率
    const gridGeoJson = gridMaker.makeGrid({
        polygon: gridStore.polygon,
        startCb: () => {
            console.log('开始生成格网')
        },
        endCb: () => {
            console.log('格网生成完成')
        },
        overboundCb: () => {
            console.log('格网面积超过限制')
            Operations.draw_deleteAll()
        },
    })
    if (gridGeoJson) {
        Operations.map_addGridLayer(gridGeoJson)
        Operations.draw_deleteAll()
    }
}

onMounted(() => {
    mapManager.init('map').then((map) => {
        console.log(map)
        map.zoomTo(10)
        ///// Grid-test /////////////////////////

        ///// rio-tiler-test /////////////////////////
        if (false) {
            map.addSource('src', {
                type: 'raster',
                tiles: [
                    // no cog
                    // "http://223.2.32.242:8079/{z}/{x}/{y}.png?object=/test-images/landsat/landset7/tif/LE07_L1TP_122039_20210212_20210212_01_RT/LE07_L1TP_122039_20210212_20210212_01_RT_B1.TIF"
                    'http://223.2.32.242:8079/{z}/{x}/{y}.png?object=/project-data-bucket/rgj3/PRJMMFWjTXm1zuYm4m4q/test.tif',
                    // "http://223.2.32.242:8079/{z}/{x}/{y}.png?object=/test-images/landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20241217_20241227_02_T1/LC08_L2SP_118038_20241217_20241227_02_T1_SR_B4.TIF"
                ],
            })
            map.addLayer({
                id: 'layer',
                type: 'raster',
                source: 'src',
                minzoom: 0,
            })

            map.addSource('src2', {
                type: 'raster',
                tiles: [
                    'http://223.2.32.242:8079/{z}/{x}/{y}.png?object=/test-images/landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20241201_20241203_02_T1/LC08_L2SP_118038_20241201_20241203_02_T1_SR_B4.TIF',
                ],
            })
            map.addLayer({
                id: 'layer2',
                type: 'raster',
                source: 'src2',
                minzoom: 0,
            })

            window.addEventListener('keydown', (e) => {
                if (e.key === '1') {
                    map.setPaintProperty('layer', 'raster-opacity', 0.1)
                    map.setPaintProperty('layer2', 'raster-opacity', 1)
                } else if (e.key === '2') {
                    map.setPaintProperty('layer', 'raster-opacity', 1)
                    map.setPaintProperty('layer2', 'raster-opacity', 0.1)
                }
            })
        }
    })
})
</script>
