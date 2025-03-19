import client from '../clientHttp'
import { blobDownload } from '@/util'
import {
    type TilesGeoJsonResponse,
    type TilesGeoJsonRequest,
    type TileTifRequest,
    type MergedTifRequest,
    type TileDetailRequest,
    type TileDescriptionResponse
} from '@/types/imageTile'

// Get tiles-geojson by imageId and tile-resolution
export async function getTilesGeoJson(params: TilesGeoJsonRequest): Promise<TilesGeoJsonResponse> {
    return client.get<TilesGeoJsonResponse>(`/data/tile/imageId/${params.imageId}/tileLevel/${params.tileLevel}`)
}

// Get tile tif by imageId and tileId
export async function getTileTif(params: TileTifRequest): Promise<Blob> {
    return client.get<Blob>(`/data/tile/tif/imageId/${params.imageId}/tileId/${params.tileId}`, {
        responseType: 'blob'
    })
}
export async function getTileTifandDownload(params: TileTifRequest & { name?: string }) {
    const blob = await getTileTif(params)
    blobDownload(blob, params.name || params.tileId + '.tif')
}

// Get merged tif by tiles
export async function getMergedTif(params: MergedTifRequest): Promise<Blob> {
    return client.post<Blob>(`/data/tile/tif/tileIds`, params, {
        responseType: 'blob'
    })
}
export async function getMergedTifandDownload(params: MergedTifRequest & { name?: string }) {
    const blob = await getMergedTif(params)
    blobDownload(blob, params.name || 'merge' + '.tif')
}

// Get tile description by imageId and tileId
export async function getTileDescription(params: TileDetailRequest): Promise<TileDescriptionResponse> {
    return client.get<TileDescriptionResponse>(`/data/tile/description/imageId/${params.imageId}/tileId/${params.tileId}`)
}
