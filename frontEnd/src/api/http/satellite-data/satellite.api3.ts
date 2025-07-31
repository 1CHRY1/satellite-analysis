import http from '../../axiosClient/clientHttp3'

export async function getImageStats (Param:any) : Promise<any> {
    return http.post<any>('data/grid/scene/contain', Param)
}