import * as SatelliteDataApi from '@/api/http/satellite-data'

///// View /////////////////////////
export interface ProductView {
    id: string
    name: string
    description: string
    resolution: string
    period: string
    sensorId: string
}

export interface SensorView {
    id: string
    name: string
    description: string
    products: ProductView[]
}

///// Adapter /////////////////////////

export async function fetchProductViews(sensorId: string): Promise<ProductView[]> {
    const productViews: ProductView[] = []

    const baseInfo = await SatelliteDataApi.getProductList(sensorId)

    baseInfo.forEach(async (pBaseInfo) => {
        const pDetailInfo = await SatelliteDataApi.getProductDetail(pBaseInfo.productId)
        const pView: ProductView = {
            id: pBaseInfo.productId,
            name: pBaseInfo.productName,
            description: pDetailInfo.description,
            resolution: pDetailInfo.resolution,
            period: pDetailInfo.period,
            sensorId: sensorId,
        }
        productViews.push(pView)
    })

    return productViews
}

// TODO: 分页查询
export async function fetchAllSensorViews(): Promise<SensorView[]> {
    const sensorsView: SensorView[] = []

    const sensorBaseInfoList = await SatelliteDataApi.getSensorList()

    sensorBaseInfoList.forEach(async (sensorBaseInfo) => {
        const detailInfo = await SatelliteDataApi.getSensorDetail(sensorBaseInfo.sensorId)

        const producs = await fetchProductViews(sensorBaseInfo.sensorId)

        let sensor: SensorView = {
            id: sensorBaseInfo.sensorId,
            name: sensorBaseInfo.sensorName,
            description: detailInfo.description,
            products: producs,
        }

        sensorsView.push(sensor)
    })

    sensorsView.sort((a, b) => a.products.length - b.products.length)

    return sensorsView

    // Testing
    // return new Promise((resolve) => {
    //     setTimeout(() => {
    //         resolve(sensorsView)
    //     }, 1000)
    // })
}
