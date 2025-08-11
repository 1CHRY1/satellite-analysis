import { ref, computed } from 'vue'
import { getImageStats } from '@/api/http/satellite-data/satellite.api3'
import { exploreData} from './shared'
import type { platformType } from './shared'

/**
 * 数据准备相关的组合式函数
 */

export const useDataPreparation = () => {
    // 各品类影像分类,1m是亚米
    const demotic1mImages = ref<any[]>([])
    const demotic2mImages = ref<any[]>([])
    const internationalImages = ref<any[]>([])
    const radarImages = ref<any[]>([])

    // 累积影像分布到各个格网的计算结果
    const demotic1mGridImages = ref<any[]>([])
    const demotic2mGridImages = ref<any[]>([])
    const interGridImages = ref<any[]>([])
    const radarGridImages = ref<any[]>([])

    // 记录每一级渲染的格网FeatureCollection
    const demotic1mGridFeature = ref<any>(null)
    const demotic2mGridFeature = ref<any>(null)
    const interGridFeature = ref<any>(null)
    const radarGridFeature = ref<any>(null)

    // 数据集
    const nation1mSet = ref()
    const nation2mSet = ref()
    const internationalLightSet = ref()
    const SARSet = ref()

    // 平台列表
    const nation1mPlatformList = ref()
    const nation2mPlatformList = ref()
    const internationalLightPlatformList = ref()
    const SARPlatformList = ref()

    // 获取筛选后的传感器
    const extractPlatformList = (sceneList: platformType[]): platformType[] => {
        return Array.from(
            sceneList.reduce((map, item: platformType) => {
                const existing = map.get(item.platformName)
                if (existing) {
                    existing.sceneId = Array.isArray(existing.sceneId)
                        ? [...existing.sceneId, item.sceneId]
                        : [existing.sceneId, item.sceneId]
                } else {
                    map.set(item.platformName, {
                        platformName: item.platformName,
                        sceneId: [item.sceneId]
                    })
                }
                return map
            }, new Map()).values()
        )
    }

    // 所有场景的计算属性
    const allScenes = computed(() => [
        ...demotic1mImages.value,
        ...demotic2mImages.value,
        ...internationalImages.value,
        ...radarImages.value
    ])

    // 数据准备主函数
    const dataPrepare = async () => {
        let nation1mData = { resolutionName: 'subMeter', source: 'national', production: 'light' }
        const nation1Para = {
            grids: exploreData.grids,
            filters: nation1mData
        }
        nation1mSet.value = await getImageStats(nation1Para)

        let nation2mData = { resolutionName: 'twoMeter', source: 'national', production: 'light' }
        const nation2Para = {
            grids: exploreData.grids,
            filters: nation2mData
        }
        nation2mSet.value = await getImageStats(nation2Para)

        let internationalLightData = { source: 'international', production: 'light' }
        const InternationalLightPara = {
            grids: exploreData.grids,
            filters: internationalLightData
        }
        internationalLightSet.value = await getImageStats(InternationalLightPara)

        let SARData = { production: 'radar' }
        const SARPara = {
            grids: exploreData.grids,
            filters: SARData
        }
        SARSet.value = await getImageStats(SARPara)

        // 对应的名称列表创建
        nation1mPlatformList.value = extractPlatformList(nation1mSet.value.scenes)
        nation2mPlatformList.value = extractPlatformList(nation2mSet.value.scenes)
        internationalLightPlatformList.value = extractPlatformList(internationalLightSet.value.scenes)
        SARPlatformList.value = extractPlatformList(SARSet.value.scenes)

        demotic1mImages.value = nation1mSet.value?.scenes || []
        demotic2mImages.value = nation2mSet.value?.scenes || []
        internationalImages.value = internationalLightSet.value?.scenes || []
        radarImages.value = SARSet.value?.scenes || []
    }

    // 根据数据类型获取对应的格网统计数据
    const getGridStatsByType = (dataType: string): any[] => {
        switch (dataType) {
            case 'demotic1m':
                return nation1mSet.value?.grids || []
            case 'demotic2m':
                return nation2mSet.value?.grids || []
            case 'international':
                return internationalLightSet.value?.grids || []
            case 'radar':
                return SARSet.value?.grids || []
            default:
                return []
        }
    }

    return {
        // 影像数据
        demotic1mImages,
        demotic2mImages,
        internationalImages,
        radarImages,
        
        // 格网影像数据
        demotic1mGridImages,
        demotic2mGridImages,
        interGridImages,
        radarGridImages,
        
        // 格网特征数据
        demotic1mGridFeature,
        demotic2mGridFeature,
        interGridFeature,
        radarGridFeature,
        
        // 数据集
        nation1mSet,
        nation2mSet,
        internationalLightSet,
        SARSet,
        
        // 平台列表
        nation1mPlatformList,
        nation2mPlatformList,
        internationalLightPlatformList,
        SARPlatformList,
        
        // 方法
        extractPlatformList,
        allScenes,
        dataPrepare,
        getGridStatsByType
    }
}