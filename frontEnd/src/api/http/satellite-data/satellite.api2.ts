import http from '../clientHttp2'

export async function getSceneByConfig(param: any): Promise<any> {
    return http.post<any>(`data/scene/time/cloud/region`, param)
}
export async function getSceneByPOIConfig(param: any): Promise<any> {
    return http.post<any>(`data/scene/time/cloud/resolution/location`, param)
}

export async function getDescriptionBySceneId(sceneId: string): Promise<any> {
    return http.get<any>(`data/scene/sceneId/${sceneId}`)
}

export async function getBoundaryBySceneId(sceneId: string): Promise<any> {
    return http.get<any>(`data/scene/boundary/sceneId/${sceneId}`)
}

export async function getRasterScenesDes(param: any): Promise<any> {
    return http.post<any>(`data/scene/raster/time/region`, param)
}
export async function getCoverRegionSensorScenes(param: any): Promise<any> {
    return http.post<any>(`data/scene/cover/region/sceneIds`, param)
}
