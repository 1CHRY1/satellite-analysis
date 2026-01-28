import type { RegionValues } from 'v-region'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getSceneByConfig, getBoundary } from '@/api/http/satellite-data'
import * as MapOperation from '@/util/map/operation'
import { useExploreStore } from '@/store'
import { getScenesInfo } from '@/api/http/interactive-explore'
import type { RawFileNode } from '@/type/file'
import {
    getNdviPoint,
    getCaseStatus,
    getCaseResult,
    getSpectrum,
    getBoundaryBySceneId,
    getRegionPosition,
    getRasterScenesDes,
} from '@/api/http/satellite-data'
import { platformDataFile, selectedResult, thematicConfig } from '../shared'
import { bbox } from '@turf/turf'
import { message } from 'ant-design-vue'

export const useSettings = () => {
    const { t } = useI18n()
    const exploreData = useExploreStore()

    /**
     * 控制设置面板展开
     */
    const isSettingExpand = ref(true)

    /**
     * 数据过滤条件设置
     */
    const selectedSensorName = ref<string>()
    const region = ref<RegionValues>({
        province: '370000',
        city: '370100',
        area: '',
    })
    const startTime = '1900-01-01T00:00:00'
    const endTime = '2050-01-01T00:00:00'

    // 获取根据行政区选择的原始数据
    const originImages = ref([])

    const displayLabel = computed(() => {
        let info = region.value
        if (info.area) return Number(`${info.area}`)
        if (info.city) return Number(`${info.city}`)
        if (info.province) return Number(`${info.province}`)
        return '未选择'
    })

    const getOriginImages = async (newRegion: number | '未选择') => {
        if (newRegion === '未选择') {
            message.warning(t('datapage.analysis.message.region'))
            return
        }
        let filterData = {
            startTime,
            endTime,
            cloud: 100,
            regionId: newRegion,
            resolution: "15",
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

    const getPlatformDataFile = async (sensorName: string) => {
        function buildSceneTree(res): RawFileNode[] {
            const tree: RawFileNode[] = []

            for (const scene of res.data.scenesConfig) {
                const files: string[] = Object.values(scene.path)
                if (files.length === 0) continue

                // 提取景目录名与路径
                const firstPath = files[0] as string
                const pathParts = firstPath.split('/')
                const sceneName = pathParts[pathParts.length - 2]
                const sceneDir = pathParts.slice(0, -1).join('/')

                // 构建景节点（文件夹）
                const sceneNode: RawFileNode = {
                    name: sceneName,
                    dir: true,
                    path: `${scene.bucket}/${sceneDir}`,
                    size: 0,
                    lastModified: '',
                    children: [],
                }

                // 构建文件节点
                for (let filePath of files) {
                    const fileName = filePath.split('/').pop()!
                    sceneNode.children!.push({
                        name: fileName,
                        dir: false,
                        path: `${scene.bucket}/${filePath}`,
                        size: 0,
                        lastModified: '',
                        children: []
                    })
                }

                tree.push(sceneNode)
            }

            return tree
        }
        if (sensorName) {
            const res = await getScenesInfo(sensorName, bbox(exploreData.boundary as any))
            platformDataFile.value = buildSceneTree(res)
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
        getOriginImages,
        getPlatformDataFile,
        selectedSensorName,
    }
}
