export namespace Common {
    export interface CommonResult<T> {
        status: number
        message: string
        data: T
    }

    export interface PageRequest {
        page: number
        pageSize: number,
        asc?: boolean,
        searchText?: string,
        sortField?: string
    }

    export interface Page<T> {
        records: Array<T>,
        total: number,
        size: number,
        current: number,
        pages: number
    }

    export interface PageResponse<T> extends CommonResult<Page<T>> { }
}

/**
 * MethLib API
 */
export namespace MethLib {
    export interface MethLibPageRequest extends Common.PageRequest {
        tags: string[] | number[],
    }

    export interface Method {
        id: number,
        name: string,
        description: string,
        longDesc: string,
        copyright: string,
        category: string,
        uuid: string,
        type: string,
        params: any[],
        execution: string,
        createTime: string,
    }

    export interface MethLibResponse extends Common.CommonResult<Method> { }

    export interface MethLibPageResponse extends Common.PageResponse<Method> {
    }
}