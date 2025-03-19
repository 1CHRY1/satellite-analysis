
export type TileId = string
export type TileBBox = number[][][]

export type TilesGeoJsonRequest = {
    imageId: string
    tileLevel: number
}

export type TilesGeoJsonResponse = {
    type: 'FeatureCollection'
    features: Array<{
        id: TileId
        type: 'Feature'
        properties: Record<string, any>
        geometry: {
            type: 'Polygon'
            coordinates: TileBBox
        }
    }>
}

export type TileTifRequest = {
    imageId: string
    tileId: string
}

export type MergedTifRequest = {
    tiles: Array<TileId>
}

export type TileDetailRequest = TileTifRequest

export type TileDescriptionResponse = {
    tileLevel: string
    imageId: string 
    columnId: number
    rowId: number
}