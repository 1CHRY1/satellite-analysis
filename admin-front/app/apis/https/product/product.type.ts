import type { PageRequest, PageResponse } from "../common.type";

export interface ProductInfo {
    productId: string
    sensorId: string
    productName: string
    description: string
    resolution: string
    period: string
}

export interface ProductPageRequest extends PageRequest {
    sensorIds?: string[]
}

export type ProductPageResponse = PageResponse<ProductInfo>


export interface ProductIds {
    productIds: string[]
}