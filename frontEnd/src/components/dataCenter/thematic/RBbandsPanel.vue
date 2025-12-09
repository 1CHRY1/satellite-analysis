<template>
    <div></div>
    <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <MapIcon :size="18" />
            </div>
            <h2 class="section-title">{{t('datapage.optional_thematic.RG.title')}}</h2>
        </div>
        <div class="section-content">
            <div class="config-container">
                <div class="config-item">
                    <div class="config-label relative">
                        <MapIcon :size="16" class="config-icon" />
                        <span>{{t('datapage.optional_thematic.RG.set')}}</span>
                    </div>
                    <div class="config-control justify-center">
                        <div class="w-full space-y-2 max-h-[500px] overflow-auto">
                            <div v-if="RGImages.length === 0" class="flex justify-center my-6">
                                <SquareDashedMousePointer class="mr-2" />{{t('datapage.optional_thematic.RG.noimage')}}
                            </div>
                            <div v-for="(image, index) in RGImages" :key="index" @click="showTif(image)"
                                class="flex flex-col border cursor-pointer border-[#247699] bg-[#0d1526] text-white px-4 py-2 rounded-lg transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c]">
                                <div class="truncate font-semibold text-base" :title="image.sceneName">{{
                                    image.sceneName
                                    }}</div>
                                <div class="text-sm text-gray-400">{{ formatTime(image.sceneTime, 'minutes') }}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

        </div>
    </section>

    <!-- <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <ChartColumn :size="18" />
            </div>
            <h2 class="section-title">计算结果</h2>
        </div>
        <div class="section-content">

        </div>
    </section> -->
</template>

<script setup lang="ts">
import { getDescriptionBySceneId, getRasterScenesDes, getWindow } from '@/api/http/satellite-data';
import {
    ChartColumn,
    Earth,
    MapPinIcon,
    CalendarIcon,
    UploadCloudIcon,
    RefreshCwIcon,
    HexagonIcon,
    CloudIcon,
    ApertureIcon,
    ClockIcon,
    ImageIcon,
    LayersIcon,
    DownloadIcon,
    FilePlus2Icon,
    BoltIcon,
    BanIcon,
    MapIcon,
    SquareDashedMousePointer
} from 'lucide-vue-next'
import { onMounted, ref, type Ref } from 'vue';
import { getRGBTileLayerParamFromSceneObject } from '@/util/visualizeHelper'
import * as MapOperation from '@/util/map/operation'
import { formatTime } from '@/util/common';

import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue';
const { t } = useI18n()

/**
 * type
 */
type ThematicConfig = {
    allImages: any,
    regionId: number,
    startTime: string,
    endTime: string
}


const props = defineProps<{
    thematicConfig: ThematicConfig,
}>()

const showTif = async (image) => {
    message.success(t('datapage.optional_thematic.RG.load'))
    let sceneId = image.sceneId
    let res = await getDescriptionBySceneId(sceneId)
    console.log(res, '红绿立体');

    const rgbLayerParam = await getRGBTileLayerParamFromSceneObject(res)
    MapOperation.map_addRGBImageTileLayer(rgbLayerParam)
    let window = await getWindow(sceneId)
    MapOperation.map_fitView([
        [window.bounds[0], window.bounds[1]],
        [window.bounds[2], window.bounds[3]],
    ])
}

const RGImages: Ref<any> = ref([])
onMounted(async () => {
    let thematicConfig = props.thematicConfig;
    let rasterParam = {
        startTime: thematicConfig.startTime,
        endTime: thematicConfig.endTime,
        regionId: thematicConfig.regionId,
        dataType: '3d'
    }
    RGImages.value = await getRasterScenesDes(rasterParam)
    // console.log(sceneObject, '红绿立体');



})
</script>

<style scoped src="../tabStyle.css"></style>
