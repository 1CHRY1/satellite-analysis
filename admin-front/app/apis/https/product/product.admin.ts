import http from '~/apis/clients/adminClient'
import type { CommonResponse } from '../common.type'
import type { ProductIds, ProductPageRequest, ProductPageResponse } from './product.type'
import type { Product } from '~/types/product'

export async function getProductPage(productInfo: ProductPageRequest): Promise<CommonResponse<ProductPageResponse>> {
    return http.post<CommonResponse<ProductPageResponse>>(`product/page`, productInfo)
}

export async function addProduct(productInfo: Product): Promise<CommonResponse<any>> {
    return http.put<CommonResponse<any>>(`product/insert`, productInfo)
}

export async function updateProduct(productInfo: Product): Promise<CommonResponse<any>> {
    return http.post<CommonResponse<any>>(`product/update`, productInfo)
}

export async function batchDelProduct(productIds: ProductIds): Promise<CommonResponse<any>> {
    return http.delete<CommonResponse<any>>(`product/delete`, {data: productIds})
}