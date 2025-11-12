import { ElMessage } from 'element-plus'
import type { RegionValues } from 'v-region'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getSceneByConfig, getBoundary } from '@/api/http/satellite-data'
import * as MapOperation from '@/util/map/operation'
import { useExploreStore } from '@/store'
import {
    getNdviPoint,
    getCaseStatus,
    getCaseResult,
    getSpectrum,
    getBoundaryBySceneId,
    getRegionPosition,
    getRasterScenesDes,
} from '@/api/http/satellite-data'
import { selectedResult, thematicConfig } from '../shared'

export const useSettings = () => {
    const { t } = useI18n()

    /**
     * 控制设置面板展开
     */ 
    const isSettingExpand = ref(true)

    /**
     * 数据过滤条件设置
     */
    const region = ref<RegionValues>({
        province: '370000',
        city: '370100',
        area: '',
    })
    const startTime = '1900-01-01'
    const endTime = '2050-01-01'

    // 获取根据行政区选择的原始数据
    const originImages = ref([])

    
    const exploreData = useExploreStore()
    const displayLabel = computed(() => {
        let info = region.value
        if (info.area) return Number(`${info.area}`)
        if (info.city) return Number(`${info.city}`)
        if (info.province) return Number(`${info.province}`)
        return '未选择'
    })

    const getOriginImages = async (newRegion: number | '未选择') => {
        if (newRegion === '未选择') {
            ElMessage.warning(t('datapage.analysis.message.region'))
            return
        }
        let filterData = {
            startTime,
            endTime,
            cloud: 100,
            regionId: newRegion,
        }
        originImages.value = await getSceneByConfig(filterData)

        // MapOperation.map_destroyImagePolygon()
        // MapOperation.map_destroyImagePreviewLayer()
        // MapOperation.map_destroyGridLayer()

        // let boundaryRes = await exploreData.boundary
        // let window = await getRegionPosition(newRegion)

        // // 先清除现有的矢量边界，然后再添加新的
        // MapOperation.map_addPolygonLayer({
        //     geoJson: boundaryRes,
        //     id: 'UniqueLayer',
        //     lineColor: '#8fffff',
        //     fillColor: '#a4ffff',
        //     fillOpacity: 0.2,
        // })
        // // fly to
        // MapOperation.map_fitView([
        //     [window.bounds[0], window.bounds[1]],
        //     [window.bounds[2], window.bounds[3]],
        // ])
        thematicConfig.value = {
            allImages: originImages.value,
            regionId: displayLabel.value,
            endTime,
            startTime,
            dataset: selectedResult.value,
        }
    }

    return {
        isSettingExpand,
        region,
        thematicConfig,
        originImages,
        exploreData,
        selectedResult,
        displayLabel,
        getOriginImages
    }
}
