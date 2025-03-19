import { type Satellite, type Image } from '@/types/satellite'
import client from './clientHttp'

const MOCK = true

// Mock data for satellites and procducts
const mockSatellites: Array<Satellite> = [
    {
        id: 1,
        name: 'Landsat',
        description: '美国陆地卫星系列，提供中等分辨率多光谱影像',
        color: '#0284c7',
        products: [
            {
                id: 101,
                satelliteId: 1,
                name: 'Landsat 8 OLI',
                description: '陆地卫星8号操作性陆地成像仪',
                resolution: '30米',
                period: '16天',
            },
            {
                id: 102,
                satelliteId: 1,
                name: 'Landsat 9 OLI-2',
                description: '陆地卫星9号操作性陆地成像仪-2',
                resolution: '30米',
                period: '16天',
            },
            {
                id: 103,
                satelliteId: 1,
                name: 'Landsat 5 TM',
                description: '陆地卫星5号全地形多光光谱影像',
                resolution: '30米',
                period: '16天',
            }
        ],
    },
    {
        id: 2,
        name: 'Sentinel',
        description: '欧空局哨兵卫星系列，提供多种类型的地球观测数据',
        color: '#16a34a',
        products: [
            {
                id: 201,
                satelliteId: 2,
                name: 'Sentinel-2 MSI',
                description: '哨兵2号多光谱成像仪',
                resolution: '10米',
                period: '5天',
            },
            {
                id: 202,
                satelliteId: 2,
                name: 'Sentinel-1 SAR',
                description: '哨兵1号合成孔径雷达',
                resolution: '5米',
                period: '6天',
            },
        ],
    },
    {
        id: 3,
        name: 'MODIS',
        description: 'NASA中分辨率成像光谱仪，提供全球覆盖的多光谱数据',
        color: '#dc2626',
        products: [
            {
                id: 301,
                satelliteId: 3,
                name: 'Terra MODIS',
                description: 'Terra卫星搭载的MODIS传感器',
                resolution: '250米-1000米',
                period: '1-2天',
            },
            {
                id: 302,
                satelliteId: 3,
                name: 'Aqua MODIS',
                description: 'Aqua卫星搭载的MODIS传感器',
                resolution: '250米-1000米',
                period: '1-2天',
            },
        ],
    },
    {
        id: 4,
        name: 'GaoFen',
        description: '中国高分辨率对地观测系统卫星系列',
        color: '#eab308',
        products: [
            {
                id: 401,
                satelliteId: 4,
                name: 'GaoFen-1',
                description: '高分一号光学遥感卫星',
                resolution: '2米',
                period: '4天',
            },
            {
                id: 402,
                satelliteId: 4,
                name: 'GaoFen-2',
                description: '高分二号光学遥感卫星',
                resolution: '1米',
                period: '5天',
            },
        ],
    },
    {
        id: 5,
        name: 'WorldView',
        description: 'DigitalGlobe公司的高分辨率商业遥感卫星系列',
        color: '#6366f1',
        products: [
            {
                id: 501,
                satelliteId: 5,
                name: 'WorldView-3',
                description: 'WorldView-3高分辨率多光谱卫星',
                resolution: '0.31米',
                period: '1-4天',
            },
            {
                id: 502,
                satelliteId: 5,
                name: 'WorldView-2',
                description: 'WorldView-2高分辨率多光谱卫星',
                resolution: '0.46米',
                period: '1-4天',
            },
        ],
    },
]
// Mock data which
export const fetchSatellites = (): Promise<Array<Satellite>> => {
    if (MOCK) {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve(mockSatellites)
            }, 500)
        })
    } else {
        return client.get<Array<Satellite>>('/satellites')
    }
}

// Mock data for images
const images: Array<Image> = [
    {
        id: 0,
        name: 'Landsat 5 - 长江下游',
        date: '2012-05-15',
        resolution: '30m',
        cloudCover: 5,
        thumbnail: 'https://placehold.co/60',
        color: '#0284c7',
        bands: [
            { id: 'b1', name: 'B1', description: '蓝色', wavelength: '0.45-0.52 µm', resolution: '30m' },
            { id: 'b2', name: 'B2', description: '绿色', wavelength: '0.52-0.60 µm', resolution: '30m' },
            { id: 'b3', name: 'B3', description: '红色', wavelength: '0.63-0.69 µm', resolution: '30m' },
            { id: 'b4', name: 'B4', description: '近红外', wavelength: '0.76-0.90 µm', resolution: '30m' },
            { id: 'b5', name: 'B5', description: '中红外', wavelength: '1.55-1.75 µm', resolution: '30m' },
            { id: 'b6', name: 'B6', description: '热红外', wavelength: '1.55-1.75 µm', resolution: '120m' },
            { id: 'b7', name: 'B7', description: '远红外', wavelength: '1.55-1.75 µm', resolution: '30m' },
        ],
    },
    // {
    //     id: 1,
    //     name: 'Landsat 8 - 北京市',
    //     date: '2023-05-15',
    //     resolution: '30m',
    //     cloudCover: 5,
    //     thumbnail: 'https://placehold.co/60',
    //     color: '#0284c7',
    //     bands: [
    //         { id: 'b1', name: 'B1', description: '沿海/气溶胶', wavelength: '0.43-0.45 µm', resolution: '30m' },
    //         { id: 'b2', name: 'B2', description: '蓝', wavelength: '0.45-0.51 µm', resolution: '30m' },
    //         { id: 'b3', name: 'B3', description: '绿', wavelength: '0.53-0.59 µm', resolution: '30m' },
    //         { id: 'b4', name: 'B4', description: '红', wavelength: '0.64-0.67 µm', resolution: '30m' },
    //         { id: 'b5', name: 'B5', description: '近红外', wavelength: '0.85-0.88 µm', resolution: '30m' },
    //     ],
    // },
    // {
    //     id: 2,
    //     name: 'Sentinel-2 - 上海市',
    //     date: '2023-06-20',
    //     resolution: '10m',
    //     cloudCover: 10,
    //     thumbnail: 'https://placehold.co/60',
    //     color: '#0891b2',
    //     bands: [
    //         { id: 'b1', name: 'B1', description: '沿海气溶胶', wavelength: '0.443 µm', resolution: '60m' },
    //         { id: 'b2', name: 'B2', description: '蓝', wavelength: '0.490 µm', resolution: '10m' },
    //         { id: 'b3', name: 'B3', description: '绿', wavelength: '0.560 µm', resolution: '10m' },
    //         { id: 'b4', name: 'B4', description: '红', wavelength: '0.665 µm', resolution: '10m' },
    //         { id: 'b8', name: 'B8', description: '近红外', wavelength: '0.842 µm', resolution: '10m' },
    //     ],
    // },
    // {
    //     id: 3,
    //     name: 'GF-1 - 广州市',
    //     date: '2023-04-10',
    //     resolution: '2m',
    //     cloudCover: 15,
    //     thumbnail: 'https://placehold.co/60',
    //     color: '#059669',
    //     bands: [
    //         { id: 'b1', name: 'B1', description: '蓝', wavelength: '0.45-0.52 µm', resolution: '2m' },
    //         { id: 'b2', name: 'B2', description: '绿', wavelength: '0.52-0.59 µm', resolution: '2m' },
    //         { id: 'b3', name: 'B3', description: '红', wavelength: '0.63-0.69 µm', resolution: '2m' },
    //         { id: 'b4', name: 'B4', description: '近红外', wavelength: '0.77-0.89 µm', resolution: '2m' },
    //     ],
    // },
    // {
    //     id: 4,
    //     name: 'MODIS - 华北地区',
    //     date: '2023-07-05',
    //     resolution: '250m',
    //     cloudCover: 20,
    //     thumbnail: 'https://placehold.co/60',
    //     color: '#0d9488',
    //     bands: [
    //         { id: 'b1', name: 'B1', description: '红', wavelength: '0.62-0.67 µm', resolution: '250m' },
    //         { id: 'b2', name: 'B2', description: '近红外', wavelength: '0.841-0.876 µm', resolution: '250m' },
    //         { id: 'b3', name: 'B3', description: '蓝/绿', wavelength: '0.459-0.479 µm', resolution: '500m' },
    //         { id: 'b4', name: 'B4', description: '绿', wavelength: '0.545-0.565 µm', resolution: '500m' },
    //     ],
    // },
    // {
    //     id: 5,
    //     name: 'Landsat 9 - 成都市',
    //     date: '2023-03-25',
    //     resolution: '30m',
    //     cloudCover: 8,
    //     thumbnail: 'https://placehold.co/60',
    //     color: '#0284c7',
    //     bands: [
    //         { id: 'b1', name: 'B1', description: '沿海/气溶胶', wavelength: '0.43-0.45 µm', resolution: '30m' },
    //         { id: 'b2', name: 'B2', description: '蓝', wavelength: '0.45-0.51 µm', resolution: '30m' },
    //         { id: 'b3', name: 'B3', description: '绿', wavelength: '0.53-0.59 µm', resolution: '30m' },
    //         { id: 'b4', name: 'B4', description: '红', wavelength: '0.64-0.67 µm', resolution: '30m' },
    //         { id: 'b5', name: 'B5', description: '近红外', wavelength: '0.85-0.88 µm', resolution: '30m' },
    //     ],
    // },
]
export const fetchImages = (): Promise<Image[]> => {
    if (MOCK) {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve(images)
            }, 500)
        })
    } else {
        return client.get<Image[]>('/images')
    }
}
