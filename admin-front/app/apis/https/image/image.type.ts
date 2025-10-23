export interface ImageInfo {
    imageId: string
    sceneId: string
    tifPath: string
    band: number
    bucket: string
    cloud: number
}

export type ImageRequest = string

export type ImageResponse = ImageInfo[]


export interface ImageIds {
    imageIds: string[]
}