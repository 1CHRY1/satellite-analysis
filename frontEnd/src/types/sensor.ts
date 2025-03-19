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
