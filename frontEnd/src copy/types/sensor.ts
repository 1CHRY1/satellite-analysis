/////// Sensor API //////////////////////////////////
export interface SensorResponse {
    sensorId: string
    sensorName: string
}

export interface SensorListResponse extends Array<SensorResponse> {}

export interface SensorDetailResponse {
    platFormName: string | null
    description: string
}

/////// Product API //////////////////////////////////
export interface ProductResponse {
    productId: string
    productName: string
}

export interface ProductListResponse extends Array<ProductResponse> {}

export interface ProductDetailResponse {
    resolution: string
    period: string
    description: string
}

/////// Sensor View //////////////////////////////////
export interface ProductView {
    id: string
    satelliteId: string
    name: string
    description: string
    resolution: string
    period: string
}

export interface SensorView {
    id: string
    name: string
    description: string
    color: string
    products: Array<ProductView>
}

export interface BandView {
    id: string
    name: string
    description: string
    wavelength: string
    resolution: string
}

export interface SensorImageView {
    id: string
    name: string
    date: string
    resolution: string
    cloudCover: number
    thumbnail: string
    color: string
    bands: Array<BandView>
}
