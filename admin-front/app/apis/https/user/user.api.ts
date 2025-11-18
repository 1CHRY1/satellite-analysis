import {clientV1 as http} from '~/apis/clients/client'
import adminClient from '~/apis/clients/adminClient'

export async function login(userInfo: any): Promise<any> {
    return http.post<any>(`user/login`, userInfo)
}

export async function getUsers(userId: any): Promise<any> {
    return http.get<any>(`user/description/userId/${userId}`)
}

export async function getRole(roleId: any): Promise<any> {
    return http.get<any>(`role/${roleId}`)
}

export async function getUserById(userId: any): Promise<any> {
    return adminClient.get<any>(`user/${userId}`)
}