import {
    type SensorView,
    type ProductView,
    type ProductResponse,
    type SensorResponse,
} from '@/types/sensor'
import {
    getSensorList,
    getSensorDetail,
    getProductList,
    getProductDetail,
} from '@/api/rs-data/sensorApi'
import { getColorFromPalette } from '@/util'

/**
 * 将单个产品数据转换为ProductView
 */
async function adaptProductToView(
    product: ProductResponse,
    sensorId: string,
): Promise<ProductView> {
    const productDetail = await getProductDetail(product.productId)
    return {
        id: product.productId,
        satelliteId: sensorId,
        name: product.productName,
        description: productDetail.description,
        resolution: productDetail.resolution,
        period: productDetail.period,
    }
}

/**
 * 获取并转换指定传感器的所有产品数据
 */
async function getProductViewsForSensor(sensorId: string): Promise<Array<ProductView>> {
    const products = await getProductList(sensorId)
    return Promise.all(products.map((product) => adaptProductToView(product, sensorId)))
}

/**
 * 将单个传感器数据转换为SensorView
 */
async function adaptSensorToView(sensor: SensorResponse): Promise<SensorView> {
    const sensorDetail = await getSensorDetail(sensor.sensorId)
    const products = await getProductViewsForSensor(sensor.sensorId)

    return {
        id: sensor.sensorId,
        name: sensor.sensorName,
        description: sensorDetail.description,
        color: getColorFromPalette(Number(sensor.sensorId)) || '#666666',
        products,
    }
}

/**
 * 获取所有传感器视图数据
 */
export async function getSensorViewData(): Promise<Array<SensorView>> {
    const sensors = await getSensorList()
    console.log(sensors)
    return Promise.all(sensors.map(adaptSensorToView))
}

/**
 * 获取指定数量的传感器视图数据
 */
export async function getLimitedSensorViewData(limit: number): Promise<Array<SensorView>> {
    const sensors = await getSensorList()
    const limitedSensors = sensors.slice(0, limit)
    return Promise.all(limitedSensors.map(adaptSensorToView))
}

/**
 * 获取单个传感器的视图数据
 */
export async function getSingleSensorViewData(sensorId: string): Promise<SensorView | null> {
    const sensors = await getSensorList()
    const sensor = sensors.find((s) => s.sensorId === sensorId)
    if (!sensor) return null
    return adaptSensorToView(sensor)
}
