import http from '~/apis/clients/adminClient'
import type { UserIds, UserInfo, UserPageRequest, UserPageResponse } from './user.type'
import type { CommonResponse } from '../common.type'

export async function getUserPage(userInfo: UserPageRequest): Promise<CommonResponse<UserPageResponse>> {
    return http.post<CommonResponse<UserPageResponse>>(`user/page`, userInfo)
}

export async function addUser(userInfo: UserInfo): Promise<CommonResponse<any>> {
    return http.post<CommonResponse<any>>(`user/insert`, userInfo)
}

export async function updateUser(userInfo: UserInfo): Promise<CommonResponse<any>> {
    return http.post<CommonResponse<any>>(`user/update`, userInfo)
}

export async function batchDelUser(userIds: UserIds): Promise<CommonResponse<any>> {
    return http.post<CommonResponse<any>>(`user/delete`, userIds)
}