import http from '~/apis/clients/adminClient'
import type { CommonResponse } from '../common.type'
import type { ImageIds, ImageRequest, ImageResponse } from './image.type'
import type { Image } from '~/types/image'

export async function getImage(sceneId: ImageRequest): Promise<CommonResponse<ImageResponse>> {
    return http.get<CommonResponse<ImageResponse>>(`image/sceneId/${sceneId}`)
}

export async function updateImage(imageInfo: Image): Promise<CommonResponse<any>> {
    return http.post<CommonResponse<any>>(`image/update`, imageInfo)
}

export async function batchDelImage(imageIds: ImageIds): Promise<CommonResponse<any>> {
    return http.delete<CommonResponse<any>>(`image/delete`, {data: imageIds})
}