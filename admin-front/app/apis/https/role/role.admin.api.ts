import http from '~/apis/clients/adminClient'
import type { CommonResponse } from '../common.type'
import type { RolePageRequest, RolePageResponse } from './role.type'

export async function getRolePage(roleInfo: RolePageRequest): Promise<CommonResponse<RolePageResponse>> {
    return http.post<CommonResponse<RolePageResponse>>(`role/page`, roleInfo)
}