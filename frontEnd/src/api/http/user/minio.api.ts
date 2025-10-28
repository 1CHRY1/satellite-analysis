import http from '../../axiosClient/clientHttp'

export async function getDataFile(param: any):Promise<any>{
    return http.get<any>(`minIO/files/tree/${param.userId}`, { params: { filePath: param.filePath } })
}

export async function deleteFile(filePath: string): Promise<any> {
    return http.delete<any>(`minIO/file/delete`, { params: { filePath } })
}

export async function uploadFile(param: any): Promise<any> {
    return http.post<any>(`minIO/file/upload`, param)
}

export async function updateFile(param: any): Promise<any> {
    return http.post<any>(`minIO/file/edit`, param)
}

export async function downloadFile(filePath: string): Promise<any> {
    return http.get<any>(`minIO/file/download`, { params: { filePath } , responseType: 'blob'})
}   
