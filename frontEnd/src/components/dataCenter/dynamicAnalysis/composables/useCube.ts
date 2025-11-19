import { getCube, getGrid, type CubeDisplayItem, type CubeObj } from '@/api/http/analytics-display'
import { computed, ref } from 'vue'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import * as DynamicAnalysisMapOps from '@/util/map/operation/dynamic-analysis'
import * as CommonMapOps from '@/util/map/operation/common'
import { ezStore } from '@/store'
import { bbox } from '@turf/turf'

export const useCube = () => {
    const cubeObj = ref<CubeObj>({})
    const inputCacheKey = ref<string>('')
    const cubeList = ref<CubeDisplayItem[]>([])
    const currentCacheKey = ref<string>()

    /**
     * 获取用于放在列表展示的cube列表
     */
    const getCubeObj = async () => {
        const cubeObjRes = await getCube()
        cubeObj.value = cubeObjRes.data

        cubeList.value = await Promise.all(
            Object.entries(cubeObj.value).map(async ([key, value]) => {
                const [rowId, columnId, resolution] = value.cubeId.split('-').map(Number)
                const gridGeoJsonRes = await getGrid({ rowId, columnId, resolution })
                return {
                    ...value,
                    cacheKey: key,
                    isSelect: false, // 控制用户是否勾选
                    isShow: false, // 控制是否显示（Eye图标）
                    gridGeoJson: gridGeoJsonRes.boundary // 空间属性
                }
            })
        )
    }

    /**
     * 勾选cube时，自动fitview到视图范围并高亮格网
     * @param cacheKey cube的key
     */
    const handleSelectCube = (cacheKey: string) => {
        cubeList.value.map(async (item) => {
            if (item.cacheKey === cacheKey) {
                item.isSelect = !item.isSelect
                if(item.isSelect) {
                    const boundsArray = bbox(item.gridGeoJson)
                    const bounds = [
                        [boundsArray[0], boundsArray[1]],
                        [boundsArray[2], boundsArray[3]]
                    ]
                    CommonMapOps.map_fitView(bounds)
                }
            }
            return item
        })
        const selectedIds = cubeList.value
            .filter((item) => item.isSelect)
            .map((item) => item.cacheKey)

        const map = ezStore.get('map')
        const highlightId = 'grid-layer-highlight'

        if (selectedIds.length === 0) {
            // 没有选中的话清除高亮
            map.setFilter(highlightId, ['in', 'id', ''])
        } else {
            map.setFilter(highlightId, ['in', 'id', ...selectedIds])
        }
    }

    /**
     * 点击格网后更新图层
     * @param allGrids 所有格网
     */
    const updateGridLayer = (allGrids: any) => {
        console.log(allGrids)
        let gridFeature: FeatureCollection = {
            type: 'FeatureCollection',
            features: allGrids.map((item: any, index: number) => {
                return {
                    type: 'Feature',
                    geometry: item.gridGeoJson.geometry as Geometry,
                    properties: {
                        ...(item.properties || {}),
                        id: item.cacheKey, // 确保每个都有 id
                        // opacity: judgeGridOpacity(index, sceneGridsRes, totalImg),
                        opacity: 0.1,
                        rowId: item.cubeId.split('-')[0],
                        columnId: item.cubeId.split('-')[1],
                        resolution: item.cubeId.split('-')[2],
                        flag: true, // flag means its time to trigger the visual effect
                        // international: sceneGridsRes[index].international,
                        // national: sceneGridsRes[index].national,
                        // light: sceneGridsRes[index].light,
                        // radar: sceneGridsRes[index].radar,
                        // traditional: sceneGridsRes[index].traditional,
                        // ard: sceneGridsRes[index].ard,
                    },
                }
            }),
        }
        DynamicAnalysisMapOps.map_destroyGridLayer()
        DynamicAnalysisMapOps.map_addGridLayer(gridFeature)
        // InteractiveExploreMapOps.draw_deleteAll()
    }

    return {
        getCubeObj,
        cubeObj,
        inputCacheKey,
        handleSelectCube,
        updateGridLayer,
        cubeList,
        currentCacheKey,
    }
}
