import type { PageRequest, PageResponse } from "../common.type";

export interface RoleInfo {
    roleId: number
    name: string
    description: string
    maxCpu: number
    maxStorage: number
    maxJob: number
    isSuperAdmin: number
}

export type RolePageRequest = PageRequest

export type RolePageResponse = PageResponse<RoleInfo>