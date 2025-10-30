import http from '~/apis/clients/adminClient'
import type { CommonResponse } from '../common.type'
import type { VectorPageRequest, VectorPageResponse } from './vector.type'
import type { Vector } from '~/types/vector'

export async function getVectorPage(vectorInfo: VectorPageRequest): Promise<CommonResponse<VectorPageResponse>> {
    return http.post<CommonResponse<VectorPageResponse>>(`vector/page`, vectorInfo)
}

export async function updateVector(vectorInfo: Vector): Promise<CommonResponse<any>> {
    return http.post<CommonResponse<any>>(`vector/update`, vectorInfo)
}