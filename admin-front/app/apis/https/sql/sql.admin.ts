import http from '~/apis/clients/adminClient'
import type { DBInfo } from './sql.type'

export async function getAllDBInfo(): Promise<DBInfo> {
    return http.get<DBInfo>(`monitor/datasource`)
}

