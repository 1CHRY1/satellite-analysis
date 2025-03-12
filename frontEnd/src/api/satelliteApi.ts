import { type Satellite } from '@/types/satellite'

// Mock data for satellites
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

export const fetchSatellites = (): Promise<Array<Satellite>> => {
    return new Promise((resolve) => {
        // Simulate API delay
        setTimeout(() => {
            resolve(mockSatellites)
        }, 500)
    })
}
