<template>
    <div class="relative flex flex-1 flex-row bg-black">
        <div class=" w-[28vw] p-4 text-gray-200">
            <!-- 自定义选项切换按钮 -->
            <div class="flex justify-between bg-[#2a2a2a] rounded-xl p-1 mb-2 shadow-md mx-2">
                <button v-for="item in pages" :key="item.value" @click="showPage = item.value" :disabled="unlockTab"
                    :class="[
                        'flex-1 px-2 py-2 text-sm font-medium text-center rounded-lg transition-all duration-200 ',
                        showPage === item.value
                            ? 'bg-gradient-to-r from-blue-500 to-indigo-500 text-white shadow-inner'
                            : 'text-gray-300 hover:bg-[#3a3a3a]',
                        unlockTab ? 'cursor-not-allowed' : 'cursor-pointer'
                    ]">
                    {{ item.label }}
                </button>
            </div>

            <!-- 页面内容区 -->
            <div class="">
                <dataExplore @submitConfig="submitConfig" v-if="showPage === 'explore'" />
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
import ImageSearcher from '@/components/dataCenter/imageSearcher.vue'
import dataExplore from '@/components/dataCenter/interactiveExplore.vue'
import pictureOfNoCloud from '@/components/dataCenter/pictureOfNoCloud.vue'
import calculateNDVI from '@/components/dataCenter/calculateNDVI.vue'
import { type interactiveExplore } from '@/components/dataCenter/type'
import * as MapOperation from '@/util/map/operation'


const showPage = ref('explore')
const regionConfig: Ref<interactiveExplore | null> = ref(null)
const unlockTab = ref(true)
const isPicking = ref(false)

const submitConfig = (config: interactiveExplore) => {
    unlockTab.value = false
    regionConfig.value = config
}



const pages = [
    { label: '交互式探索', value: 'explore' },
    { label: '无云一版图', value: 'noClouds' },
    { label: '动态分析', value: 'analysis' }
]
</script>
