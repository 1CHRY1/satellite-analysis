import http from '../../axiosClient/clientHttp3'
import type { MethLib } from './methlib.type'

export async function getMethLibPage(param: MethLib.MethLibPageRequest): Promise<MethLib.MethLibPageResponse> {
    return http.post<MethLib.MethLibPageResponse>(`methlib/page`, param)
}