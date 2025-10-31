import http from '~/apis/clients/adminClient'
import type { DBInfo, SqlStatInfo, WallInfo } from './sql.type'

export async function getAllDBInfo(): Promise<DBInfo[]> {
    return http.get<DBInfo[]>(`monitor/datasource`)
}

export async function getAllSqlStatInfo(): Promise<SqlStatInfo[]> {
    return http.get<SqlStatInfo[]>(`monitor/sql`)
}

export async function getSqlStatInfo(datasourceId: number): Promise<SqlStatInfo[]> {
    return http.get<SqlStatInfo[]>(`monitor/sql/${datasourceId}`)
}

export async function getWallInfo(datasourceId: number): Promise<WallInfo> {
    return http.get<WallInfo>(`monitor/sql/${datasourceId}`)
}
