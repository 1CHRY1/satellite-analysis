/**
 * EO立方体合成API V3版
 */

import http from '../../axiosClient/clientHttp3'
import type { CommonResponse } from '../common.type'
import type { CubeRequest, GridRequest } from './cube.type'

export async function addCube(param: CubeRequest): Promise<CommonResponse<string>> {
    return http.post<CommonResponse<string>>(`modeling/example/cube/cache/save`, param)
}

export async function getCube(): Promise<CommonResponse<string>> {
    return http.get<CommonResponse<string>>(`modeling/example/cube/cache/get/user`)
}

export async function getGrid(param: GridRequest): Promise<CommonResponse<any>> {
    return http.post<CommonResponse<any>>(`data/grid/resolution/columnIdAndRowId`, param)
}