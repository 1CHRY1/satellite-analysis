<template>
    <div class="flex bg-amber-50">
        <MapComp class="flex-1" :style="'image'" :proj="'globe'" />

        <div class="absolute top-10 right-10 flex h-fit w-fit flex-col gap-5">
            <button class="bg-amber-300 p-5" @click="localMvt">本地MVT</button>
            <button class="bg-amber-300 p-5" @click="localImg">本地影像瓦片</button>
            <button class="bg-amber-300 p-5" @click="localTian">内网老版天地图</button>
            <button class="bg-amber-300 p-5" @click="locate">定位</button>

            <button class="bg-amber-500 p-5" @click="addFK">加他们的影像底图</button>

            <button class="bg-red-500 p-5" @click="addMVTLayer">矢量瓦片测试</button>

            <button class="bg-red-500 p-5" @click="addMosaicJsonLayer">MOSAICJSON测试</button>

            <button class="bg-red-500 p-5" @click="handleCreateNoCloudTiles">无云一版图瓦片测试</button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref, type Ref } from 'vue'
import MapComp from '@/components/feature/map/mapComp.vue'
import { mapManager } from '@/util/map/mapManager'
import { StyleMap } from '@/util/map/tianMapStyle'
import { ezStore } from '@/store'
import http from '@/api/http/clientHttp'
import * as MapOperation from '@/util/map/operation'

const addMVTLayer = () => {
    // const baseUrl = '/chry'
    // const url = baseUrl + '/patch/{z}/{x}/{y}'

    // const url = `http://${window.location.host}/chry/patch/{z}/{x}/{y}`
    const url = 'http://223.2.47.202:9888/api/v1/geo/vector/tiles/patch/region/370100/type/grass/{z}/{x}/{y}'
    // const url = 'http://127.0.0.1:8000/tiles/{z}/{x}/{y}'

    console.log(import.meta.env.VITE)

    mapManager.withMap((map) => {
        console.log('add layer')
        map.addSource('t-source', {
            type: 'vector',
            tiles: [url],
        })
        setTimeout(() => {
            const source = map.getSource('t-source')
            console.log(source)
        }, 1000)
        map.addLayer({
            id: 'test-layer',
            type: 'fill',
            source: 't-source',
            'source-layer': 'patch', //这个地方要注意,
            paint: {
                'fill-color': '#ffffff',
            },
        })
        map.on('click','test-layer',(e)=>{
            // console.log(e.features[0] )
        })
    })
}

const mockSceneIds = [
  "SCrmtcmrcgp",
  "SCwaxjagmrv",
  "SC825032809",
  "SCl4ad8ul91",
  "SCa6c4bossr",
  "SC04u521n84",
  "SCaj9c7exoq",
  "SCrsk2g1b1g"
]

// 创建无云一版图瓦片
const handleCreateNoCloudTiles = async () => {
    try {
        // 1. 准备参数
        const param = {
            sceneIds: mockSceneIds,
        }

        console.log('创建无云一版图配置参数:', param)

        // 2. 创建配置
        const response = await fetch('/api/modeling/example/noCloud/createNoCloudConfig', {
            method: 'POST',
            body: JSON.stringify(param),
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + localStorage.getItem('token'),
            },
        })
        const result = await response.json()
        const jsonUrl = result.data  // 从CommonResultVO中获取data字段
        
        console.log('获取到的jsonUrl:', jsonUrl)
        
        // 3. 添加瓦片图层
        const tileUrl = `http://192.168.1.100:8000/no_cloud/{z}/{x}/{y}?jsonUrl=${encodeURIComponent(jsonUrl)}`
        //const tileUrl = `http://192.168.1.100:8000/no_cloud/{z}/{x}/{y}.png?jsonUrl=${encodeURIComponent(jsonUrl)}`
        
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

const localMvt = () => {
    console.log('1')
    mapManager.withMap((m) => {
        console.log('设置本地mvt样式')
        console.log(StyleMap.local.sources)
        m.setStyle(StyleMap.localMvt)
    })
}

const localImg = () => {
    console.log('2')
    mapManager.withMap((m) => {
        console.log('设置本地img样式')
        console.log(StyleMap.local.sources)
        m.setStyle(StyleMap.localImg)
    })
}

const localTian = () => {
    console.log('3')
    mapManager.withMap((m) => {
        console.log('设置内网vec样式')
        console.log(StyleMap.local.sources)
        m.setStyle(StyleMap.localVec)
    })
}

const locate = () => {
    mapManager.withMap((m) => {
        m.flyTo({
            center: [121.42859, 28.66138],
            zoom: 8,
        })
    })
}

const addFK = () => {
    const url = ezStore.get('conf')['fk_url']
    console.log(url)

    mapManager.withMap((map) => {
        map.addSource('wms-test-source', {
            type: 'raster',
            tiles: [url],
            tileSize: 256,
        })
        map.addLayer({
            id: 'wms-test-layer',
            type: 'raster',
            source: 'wms-test-source',
            paint: {},
        })
    })
}

const addMosaicJsonLayer = () => {
    // TEMP
    let titilerEndPoint = 'http://223.2.43.228:31800'
    let baseUrl = `${titilerEndPoint}/mosaic/mosaictile/{z}/{x}/{y}.png`
    const requestParams = new URLSearchParams()
    requestParams.append('mosaic_url', 'http://223.2.43.228:30900/' + 'temp-files' + '/' + 'mosaicjson/b0e75625-7224-495e-b875-c9b3e1493c9b.json')
    const fullUrl = baseUrl + '?' + requestParams.toString()
    MapOperation.map_removeNocloudGridPreviewLayer()
    MapOperation.map_destroyNoCloudLayer()
    MapOperation.map_addNoCloudLayer(fullUrl)
}

onMounted(() => {
 
    setTimeout(() => {
        mapManager.withMap((m) => {
            m.showTileBoundaries = true
        })
        //         // '/hytemp/rgb/tiles/{z}/{x}/{y}.png?url_r=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF&url_g=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF&url_b=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF'
        //         '/hytemp/rgb/box/{z}/{x}/{y}.png?url_r=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B4.TIF&url_g=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B3.TIF&url_b=D%3A%5Cedgedownload%5CLC08_L2SP_121038_20200922_20201006_02_T2%5CLC08_L2SP_121038_20200922_20201006_02_T2_SR_B2.TIF&bbox=117,31.5,118,32&max_r=50000&max_g=50000&max_b=50000&min_r=20000&min_g=20000&min_b=20000'
        //     ]
        // })
        // m.addLayer({
        //     id: 'raster-layer',
        //     source: 'src',
        //     type: 'raster',
        //     minzoom: 5,
        //     maxzoom: 22
        // })
        // })
    }, 1)
})
</script>

<style scoped></style>
