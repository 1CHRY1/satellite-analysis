import http from '../../axiosClient/clientHttp3'
import type { CommonResponse } from '../common.type'
import type { MethLib } from './methlib.type'

export async function getMethLibPage(
    param: MethLib.MethLibPageRequest,
): Promise<MethLib.MethLibPageResponse> {
    return http.post<MethLib.MethLibPageResponse>(`methlib/page`, param)
}

export async function getAllTag(): Promise<CommonResponse<MethLib.Tag[]>> {
    return http.get<CommonResponse<MethLib.Tag[]>>(`methlib/tag/all`)
}

export async function invokeMethod(id: number, params: any): Promise<CommonResponse<string>> {
    return http.post<CommonResponse<string>>(`methlib/invoke/${id}`, params)
}
