import http from '../../axiosClient/clientHttp'

export async function getRole(roleId: any): Promise<any> {
    return http.get<any>(`role/${roleId}`)
}