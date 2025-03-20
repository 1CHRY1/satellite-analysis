import { type SceneView, type BandView, type SceneSearchRequest } from '@/types/image'
import {
    searchScenesWithFilter,
    getSceneDetail,
    getScenePngUrl,
    getSceneImageList, //band list
} from '@/api/rs-data/imageApi'
import type { polygonGeometry } from '@/types/sharing'

/**
 * 将一个Scene结果转换为SceneView
 */
export async function getSingleSceneViewData(feature: {
    id: string
    type: 'Feature'
    properties: Record<string, any>
    geometry: polygonGeometry
}): Promise<SceneView> {
    const sceneId = feature.id
    const _sceneDetail = await getSceneDetail(sceneId)
    const _imageList = await getSceneImageList(sceneId)

    // 构建波段信息
    const bands: Array<BandView> = _imageList.map((image, index) => {
        return {
            id: image.imageId,
            name: `Band-${image.band}`,
            description: '暂无', // !API中未提供波段描述信息
            wavelength: '暂无', // !API中未提供波段波长信息
            resolution: '暂无', // !API中未提供波段分辨率信息
        }
    })

    return {
        id: sceneId,
        name: `Scene-${sceneId}`, // !API中未提供该景影像名称
        date: _sceneDetail.sceneTime,
        resolution: '暂无',
        cloudCover: 18, // !API中未提供该景影像云量
        thumbnail: getScenePngUrl(sceneId),
        bands: bands,
        geometry: feature.geometry,
    }
}

/**
 * 将所有Scene搜索结果转换为SceneView数组
 */
export async function getSceneViewData(params: SceneSearchRequest): Promise<Array<SceneView>> {
    const response = await searchScenesWithFilter(params)
    console.log('response', response)
    return Promise.all(
        response.features.map(async (feature: any) => {
            return getSingleSceneViewData(feature)
        }),
    )
}
