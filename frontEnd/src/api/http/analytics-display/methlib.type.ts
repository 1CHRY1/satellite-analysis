import type { ModelStatus } from "../satellite-data"

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
        nameZh?: string,
        descriptionZh?: string,
        longDescZh?: string,
        copyright: string,
        category: string,
        uuid: string,
        type: string,
        params: any[],
        paramsZh?: any[],
        execution: string,
        createTime: string,
    }

    export interface Tag {
        id: number,
        name: string,
        nameZh?: string,
        createTime: string,
    }

    export interface MethLibResponse extends Common.CommonResult<Method> { }

    export interface MethLibPageResponse extends Common.PageResponse<Method> {
    }
}

/**
 * MethLibCase API
 */
export namespace MethLibCase {
    export interface CasePageRequest extends Common.PageRequest {
        startTime: string | null,
        endTime: string | null,
        status: ModelStatus
    }

    export interface Case {
        methodId: number,
        caseId: string,
        status: ModelStatus,
        params: any,
        result: any,
        userId: string,
        createTime: string,
        method?: MethLib.Method
    }

    export interface CaseResponse extends Common.CommonResult<Case> { }

    export interface CasePageResponse extends Common.PageResponse<Case> {
    }
}