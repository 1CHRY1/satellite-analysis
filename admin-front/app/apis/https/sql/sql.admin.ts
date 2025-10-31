import http from '~/apis/clients/adminClient'
import type { DBInfo, SqlStatInfo, WallInfo, WebAppStat, WebSessionStat, WebURIStat } from './sql.type'

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
    return http.get<WallInfo>(`monitor/wall/${datasourceId}`)
}

export async function getAllWebAppStatInfo(): Promise<WebAppStat[]> {
    return http.get<WebAppStat[]>(`monitor/web`)
}

export async function getAllWebURIStatInfo(): Promise<WebURIStat[]> {
    return http.get<WebURIStat[]>(`monitor/uri`)
}

export async function getAllWebSessionStatInfo(): Promise<WebSessionStat[]> {
    return http.get<WebSessionStat[]>(`monitor/session`)
}

export async function addTestSession()  {
    return http.get(`monitor/test-session`)
}