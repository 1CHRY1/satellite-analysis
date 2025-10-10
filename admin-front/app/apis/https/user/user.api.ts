import http from '~/apis/clients/client'

export async function login(userInfo: any): Promise<any> {
    return http.post<any>(`user/login`, userInfo)
}

export async function getUsers(userId: any): Promise<any> {
    return http.get<any>(`user/description/userId/${userId}`)
}

export async function getRole(roleId: any): Promise<any> {
    return http.get<any>(`role/${roleId}`)
}