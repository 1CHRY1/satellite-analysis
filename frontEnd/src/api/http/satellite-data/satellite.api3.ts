import http from '../../axiosClient/clientHttp3'

export async function getImageStats (Param:any) : Promise<any> {
    return http.post<any>('data/grid/scene/contain', Param)
}

export async function getEOCube(param: any): Promise<any> {
    return http.post<any>(`modeling/example/cube/calc`, param)
}