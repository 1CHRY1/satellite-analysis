import http from '../clientHttp'
import type { analysisResponse } from './analysis.type'
// import type { Sensor, Product, SensorImage, ImageTile } from './satellite.type'

//----------------------------- model API -----------------------------//
export async function getModels(body: any): Promise<any> {
    return http.post<any>(`model/models`, body)
}

export async function getMethods(body: any): Promise<any> {
    return http.post<any>(`model/methods`, body)
}

//----------------------------- projects API -----------------------------//
export async function getProjects(): Promise<any> {
    return http.get<any>(`project/all`)
}

export async function createProject(param: any): Promise<any> {
    return http.post<any>(`coding/project/new`, param)
}

//----------------------------- code online API -----------------------------//
// 项目的启动、关闭、创建和删除
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

export async function getPackages(param: any): Promise<any> {
    return http.post<any>(`coding/project/package/list`, param)
}

//----------------------------- data directory API -----------------------------//

export async function getFiles(param: any): Promise<any> {
    return http.post<any>(`coding/project/file`, param)
}

// 获取miniIo里面的文件信息列表
export async function getMiniIoFiles(param: any): Promise<any> {
    return http.post<any>(`coding/project/results`, param)
}

// 获取miniIo里面的文件信息列表
export async function getTileFromMiniIo(dataId: any): Promise<any> {
    return http.get<any>(`coding/project/result/tif/${dataId}`)
}

//----------------------------- extra API -----------------------------//
// 后门指令：命令行操作
export async function cmdOperation(param: any): Promise<any> {
    return http.post<any>(`coding/project/environment`, param)
}
