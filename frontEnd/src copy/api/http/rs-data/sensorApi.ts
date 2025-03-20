import client from '../clientHttp'
import ezStore from '@/util/ezStore'
import {
    type SensorListResponse,
    type SensorDetailResponse,
    type ProductListResponse,
    type ProductDetailResponse,
} from '@/types/sensor'
const conf = ezStore.get('conf')
const MOCK = conf['Mock']

//////// Sensor API //////////////////////////////////

// Mock data for sensors
const mockSensors: SensorListResponse = [
    {
        sensorId: '4821456017',
        sensorName: 'Landset Demo',
    },
    {
        sensorId: 'SE33955',
        sensorName: 'Landset',
    },
]

// Get sensor list
export async function getSensorList(): Promise<SensorListResponse> {
    if (MOCK) return mockSensors
    return client.get<SensorListResponse>('/data/sensor')
}

// Get sensor detail
export async function getSensorDetail(sensorId: string): Promise<SensorDetailResponse> {
    if (MOCK) {
        return {
            platFormName: null,
            description: 'XX陆地卫星系列, 提供中等分辨率多光谱影像',
        }
    }
    return client.get(`/data/sensor/description/sensorId/${sensorId}`)
}

//////// Product API //////////////////////////////////

// Mock data for products
const mockProducts = [
    {
        productId: 'sdjkngierkj',
        productName: 'Landsat 8 OLI',
    },
]

// Get product list by sensor id
export async function getProductList(sensorId: string): Promise<ProductListResponse> {
    if (MOCK) return mockProducts
    return client.get(`/data/product/sensorId/${sensorId}`)
}

// Get product detail
export async function getProductDetail(productId: string): Promise<ProductDetailResponse> {
    if (MOCK) {
        return {
            resolution: '30米',
            period: '16 天',
            description: '陆地卫星8号操作性陆地成像仪',
        }
    }
    return client.get(`/data/product/description/productId/${productId}`)
}
