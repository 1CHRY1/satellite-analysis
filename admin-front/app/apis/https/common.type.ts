/**
 * 通用响应类型
 */
export interface CommonResponse<T> {
    status: number,
    message: string,
    data: T,
}

export interface PageRequest {
    page: number,
    pageSize: number,
    asc?: boolean,
    searchText?: string,
    sortField?: string,
}

export interface PageResponse<T> {
    records: T[]
    total: number,
    size: number,
    current: number
}