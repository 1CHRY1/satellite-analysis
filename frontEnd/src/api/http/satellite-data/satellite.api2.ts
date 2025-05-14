import http from '../clientHttp2'

export async function getSceneByConfig(param: any): Promise<any> {
    return http.post<any>(`data/scene/time/cloud/region`, param)
}
