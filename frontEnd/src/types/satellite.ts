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
