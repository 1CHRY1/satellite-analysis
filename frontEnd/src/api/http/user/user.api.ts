import http from '../clientHttp'

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
