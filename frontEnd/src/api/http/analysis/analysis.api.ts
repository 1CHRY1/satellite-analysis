import http from '../clientHttp'
import type { analysisResponse } from './analysis.type'
// import type { Sensor, Product, SensorImage, ImageTile } from './satellite.type'

//----------------------------- code online API-----------------------------//
export async function projectOperating(param: any): Promise<any> {
    return http.post<any>(`coding/project/operating`, param)
}

export async function getScript(param: any): Promise<any> {
    return http.post<any>(`coding/project/file/script`, param)
}

export async function updateScript(param: any): Promise<analysisResponse> {
    return http.put<any>(`coding/project/file/script`, param)
}

export async function runScript(param: any): Promise<any> {
    return http.post<any>(`coding/project/executing`, param)
}

export async function stopScript(param: any): Promise<any> {
    return http.post<any>(`coding/project/canceling`, param)
}

export async function operatePackage(param: any): Promise<any> {
    return http.post<any>(`coding/project/package`, param)
}

export async function getFiles(param: any): Promise<any> {
    return http.post<any>(`coding/project/file`, param)
}

//----------------------------- extra API-----------------------------//
// 后门指令：命令行操作
export async function cmdOperation(param: any): Promise<any> {
    return http.post<any>(`coding/project/environment`, param)
}
