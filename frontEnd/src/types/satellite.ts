export interface SatelliteProduct {
    id: number
    satelliteId: number
    name: string
    description: string
    resolution: string
    period: string
}
export interface Satellite {
    id: number
    name: string
    description: string
    color: string
    products: Array<SatelliteProduct>
}

export interface Band {
    id: string
    name: string
    description: string
    wavelength: string
    resolution: string
}

export interface Image {
    id: number
    name: string
    date: string
    resolution: string
    cloudCover: number
    thumbnail: string
    color: string
    bands: Array<Band>
}
