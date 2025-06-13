<template>
    <div class="relative flex flex-1 flex-row bg-black">
        <div class="w-[28vw] p-4 text-gray-200">
            <!-- 自定义选项切换按钮 -->
            <div class="mx-2 mb-2 flex justify-between rounded-xl bg-[#2a2a2a] p-1 shadow-md">
                <button v-for="item in pages" :key="item.value" @click="pageCheckout(item.value)" :class="[
                    'flex-1 rounded-lg px-2 py-2 text-center text-sm font-medium transition-all duration-200 cursor-pointer',
                    showPage === item.value
                        ? 'bg-gradient-to-r from-blue-500 to-indigo-500 text-white shadow-inner'
                        : 'text-gray-300 hover:bg-[#3a3a3a]',
                    // unlockTab ? 'cursor-not-allowed' : 'cursor-pointer',
                ]">
                    {{ item.label }}
                </button>
            </div>

            <!-- 页面内容区 -->
            <div class="">
                <!-- 这里改成v-show，就能保留预设的数据，并且不修改其他任何内容 -->
                <dataExplore @submitConfig="submitConfig" v-show="showPage === 'explore'" />
                <pictureOfNoCloud :regionConfig="regionConfig!" v-if="showPage === 'noClouds'" />
                <calculateNDVI :regionConfig="regionConfig!" v-if="showPage === 'analysis'" />
                <!-- <ImageSearcher class="" :regionConfig="regionConfig" v-if="showPage === 'analysis'" /> -->
            </div>
        </div>
        <!-- <ImageSearcher class="h-full w-[28vw] mt-10" /> -->
        <MapComp class="flex-1" :style="'image'" :proj="'globe'" :isPicking="isPicking" />
    </div>
</template>

<script setup lang="ts">
import { ref, type Ref } from 'vue'
import MapComp from '@/components/feature/map/mapComp.vue'
// import ImageSearcher from '@/components/dataCenter/imageSearcher.vue'
import dataExplore from '@/components/dataCenter/interactiveExplore.vue'
import pictureOfNoCloud from '@/components/dataCenter/noCloud/pictureOfNoCloud.vue'
import calculateNDVI from '@/components/dataCenter/dynamicAnalysis.vue'
import { type interactiveExplore } from '@/components/dataCenter/type'
// import * as MapOperation from '@/util/map/operation'
import bus from '@/store/bus'
import { ElMessage } from 'element-plus'

const showPage = ref('explore')
const regionConfig: Ref<interactiveExplore | null> = ref(null)
const unlockTab = ref(false)
const isPicking = ref(false)

const pageCheckout = (tab: string) => {
    if (tab === 'noClouds' && unlockTab.value === false) {
        ElMessage.warning('请完成影像筛选后再计算无云一版图')
        return
    }
    if (tab != 'explore') {
        // bus.emit('cleanAllGridPreviewLayer')
        // bus.emit('cleanAllSceneBoxLayer')
        bus.emit('cleanAllLayer')
    }
    showPage.value = tab as 'explore' | 'noClouds' | 'analysis'
}

const submitConfig = (config: interactiveExplore) => {
    unlockTab.value = true
    regionConfig.value = config
}

const pages = [
    { label: '交互式探索', value: 'explore' },
    { label: '无云一版图', value: 'noClouds' },
    { label: '动态展示分析', value: 'analysis' },
]
</script>
