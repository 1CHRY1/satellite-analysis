import http from '~/apis/clients/adminClient'
import type { CommonResponse } from '../common.type'
import type { SceneIds, ScenePageRequest, ScenePageResponse } from './scene.type'
import type { Scene } from '~/types/scene'

export async function getScenePage(sceneInfo: ScenePageRequest): Promise<CommonResponse<ScenePageResponse>> {
    return http.post<CommonResponse<ScenePageResponse>>(`scene/page`, sceneInfo)
}

export async function updateScene(sceneInfo: Scene): Promise<CommonResponse<any>> {
    return http.post<CommonResponse<any>>(`scene/update`, sceneInfo)
}

export async function batchDelScene(sceneIds: SceneIds): Promise<CommonResponse<any>> {
    return http.delete<CommonResponse<any>>(`scene/delete`, {data: sceneIds})
}