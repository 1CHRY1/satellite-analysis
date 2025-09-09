import http from '../../axiosClient/clientHttp'
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
    return http.get<any>(`modeling/project/all`)
}

export async function getUserProjects(): Promise<any> {
    const userId = localStorage.getItem('userId')
    const allProjects = await getProjects()
    const userProjects = allProjects.filter((project: any) => project.createUser === userId)
    return userProjects
}

export async function createProject(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/new`, param)
}

// 项目的启动、关闭和删除
export async function projectOperating(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/operating`, param)
}

//----------------------------- code online API -----------------------------//

export async function getScript(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/file/script`, param)
}

export async function updateScript(param: any): Promise<analysisResponse> {
    return http.put<any>(`modeling/coding/project/file/script`, param)
}

export async function runScript(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/executing`, param)
}

export async function stopScript(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/canceling`, param)
}

export async function operatePackage(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/package`, param)
}

export async function getPackages(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/package/list`, param)
}

//----------------------------- data directory API -----------------------------//

export async function getFiles(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/file`, param)
}

// 获取miniIo里面的文件信息列表
export async function getMiniIoFiles(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/results`, param)
}

// 获取miniIo里面的文件信息列表
export async function getTileFromMiniIo(dataId: any): Promise<any> {
    return http.get<any>(`modeling/coding/project/result/tif/${dataId}`)
}

export async function getJsonFileContent(dataId: any): Promise<any> {
    return http.get<any>(`modeling/coding/project/result/json/${dataId}`)
}

export async function uploadGeoJson(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/file/geojson`, param)
}

//----------------------------- extra API -----------------------------//
// 后门指令：命令行操作
export async function cmdOperation(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/environment`, param)
}

//-----------------------------  -----------------------------//
export async function publishService(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/service/publish`, param)
}

export async function unpublishService(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/service/unpublish`, param)
}

export async function getServiceStatus(param: any): Promise<any> {
    return http.post<any>(`modeling/coding/project/service/status`, param)
}
