/**
 * 通用响应类型
 */
export interface CommonResponse<T> {
    status: number,
    message: string,
    data: T,
}
