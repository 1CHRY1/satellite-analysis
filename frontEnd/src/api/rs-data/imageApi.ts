import client from '../clientHttp'
import ezStore from '@/util/ezStore'
import {
    type SceneSearchRequest,
    type SceneSearchResponse,
    type SceneDetailResponse,
    type SceneImageListResponse,
} from '@/types/image'

const conf = ezStore.get('conf')
const MOCK = conf['Mock']

// Mock data for scene search
const mockSceneSearch: SceneSearchResponse = {
    type: 'FeatureCollection',
    features: [
        {
            id: 'mock-scene-1',
            type: 'Feature',
            properties: {
                time: '2023-01-01',
                cloud: 0.1
            },
            geometry: {
                type: 'Polygon',
                coordinates: [[[120, 30], [121, 30], [121, 31], [120, 31], [120, 30]]]
            }
        }
    ]
}

// Search scenes by sensor, product, time and area
export async function searchScenesWithFilter(params: SceneSearchRequest): Promise<SceneSearchResponse> {
    if (MOCK) return mockSceneSearch
    return client.post<SceneSearchResponse>(
        '/data/scene/sensorId/productId/time/area',
        params
    )
}

// Get scene detail by scene id
export async function getSceneDetail(sceneId: string): Promise<SceneDetailResponse> {
    if (MOCK) return {
        "sceneTime": "2021-02-12T00:00:00",
        "tileLevelNum": 1,
        "tileLevels": [
            "40000"
        ],
        "crs": "EPSG:32650",
        "description": null,
        "bandNum": 3,
        "bands": [
            "1",
            "2",
            "3"
        ]
    }
    return client.get<SceneDetailResponse>(`/data/scene/description/sceneId/${sceneId}`)
}

// Get scene picture by scene id
export async function getScenePng(sceneId: string): Promise<Blob> {
    if (MOCK) return new Blob()
    return client.get<Blob>(`/data/scene/png/sceneId/${sceneId}`, {
        responseType: 'blob'
    })
}

// Get scene picture url by scene id
export function getScenePngUrl(sceneId: string): string {
    return conf['back_app'] + `/data/scene/png/sceneId/${sceneId}`
}

// Get scene image list by scene id
export async function getSceneImageList(sceneId: string): Promise<SceneImageListResponse> {
    if (MOCK) return [
        {
            "imageId": "I3395592835",
            "band": "1"
        },
        {
            "imageId": "I3408592859",
            "band": "2"
        },
        {
            "imageId": "I3419592882",
            "band": "3"
        }
    ]
    return client.get<SceneImageListResponse>(`/data/image/sceneId/${sceneId}`)
}
