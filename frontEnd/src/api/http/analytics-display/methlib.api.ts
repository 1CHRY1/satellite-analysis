import http from '../../axiosClient/clientHttp3'
import type { CommonResponse } from '../common.type'
import type { MethLib, MethLibCase } from './methlib.type'

export async function getMethLibPage(
    param: MethLib.MethLibPageRequest,
): Promise<MethLib.MethLibPageResponse> {
    return http.post<MethLib.MethLibPageResponse>(`methlib/page`, param)
}

export async function getMethodById(id: number): Promise<CommonResponse<MethLib.Method>> {
    return http.get<CommonResponse<MethLib.Method>>(`methlib/${id}`)
}

export async function getAllTag(): Promise<CommonResponse<MethLib.Tag[]>> {
    return http.get<CommonResponse<MethLib.Tag[]>>(`methlib/tag/all`)
}

export async function invokeMethod(id: number, params: any): Promise<CommonResponse<string>> {
    return http.post<CommonResponse<string>>(`methlib/invoke/${id}`, params)
}

/**
 * MethLibCase历史记录
 */
export async function getMethLibCasePage(param: MethLibCase.CasePageRequest): Promise<MethLibCase.CasePageResponse> {
    return http.post<MethLibCase.CasePageResponse>(`methlib/case/page`, param)
}

export async function getMethLibCaseById(caseId: string): Promise<MethLibCase.CaseResponse> {
    return http.get<MethLibCase.CaseResponse>(`methlib/case/${caseId}`)
}
// 注意与getMethLibCaseResult的区别
export async function getResultByMethLibCaseId(caseId: string): Promise<any> {
    return http.get<any>(`methlib/case/result/${caseId}`)
}