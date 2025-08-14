import http from '../../axiosClient/clientHttp'

//----------------------------- user API -----------------------------//
export async function createUser(userInfo: any): Promise<any> {
    return http.post<any>(`user/register`, userInfo)
}

export async function login(userInfo: any): Promise<any> {
    return http.post<any>(`user/login`, userInfo)
}

export async function getUsers(userId: any): Promise<any> {
    return http.get<any>(`user/description/userId/${userId}`)
}

export async function userUpdate(userId: any, param: any ): Promise<any> {
    return http.put<any>(`user/description/userId/${userId}`,param)
}

export async function changePassword(userId: any, param:any): Promise<any> {
    return http.put<any>(`user/password/userId/${userId}`,param)
} 

export async function avaterUpdate(param: any):Promise<any> {
    return http.post<any>(`user/avatar/upload`, param)
}

export async function getAvatar(userId:any): Promise<any> {
    return http.get<any>(`user/avatar/${userId}`)
}

export async function getHistoryData(param:any):Promise<any>{
    return http.post<any>(`user/record/page`,param)
}

export async function updateRecord( param:any):Promise<any>{
    return http.put<any>(`user/record/add`,param)
}