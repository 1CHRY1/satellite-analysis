import http from '~/apis/clients/adminClient'
import type { CommonResponse } from '../common.type'
import type { RoleIds, RolePageRequest, RolePageResponse } from './role.type'
import type { Role } from '~/types/role'

export async function getRolePage(roleInfo: RolePageRequest): Promise<CommonResponse<RolePageResponse>> {
    return http.post<CommonResponse<RolePageResponse>>(`role/page`, roleInfo)
}

export async function addRole(roleInfo: Role): Promise<CommonResponse<any>> {
    return http.put<CommonResponse<any>>(`role/insert`, roleInfo)
}

export async function updateRole(roleInfo: Role): Promise<CommonResponse<any>> {
    return http.post<CommonResponse<any>>(`role/update`, roleInfo)
}

export async function batchDelRole(roleIds: RoleIds): Promise<CommonResponse<any>> {
    return http.delete<CommonResponse<any>>(`role/delete`, {data: roleIds})
}