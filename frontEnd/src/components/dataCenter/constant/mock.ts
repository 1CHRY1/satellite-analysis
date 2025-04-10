import type { SceneView } from '../apiAdapter/adapter'
import type { Project } from '../type'

export const mockFilterResult = [
    {
        id: 'SC607111488',
        productId: 'P15303208',
        sensorId: 'SE15303',
        name: 'LC08_L2SP_118038_20240811_20240815_02_T1',
        date: '2024-08-11T00:00:00',
        cloudCover: 13.4587,
        resolution: '30.0m',
        preview_url: 'blob:http://localhost:5173/609c94c0-c70a-40b2-93cd-b69481172055',
        geoFeature: {
            type: 'Polygon',
            coordinates: [
                [
                    [120.694525, 32.784185],
                    [123.164654, 32.805302],
                    [123.160932, 30.67761],
                    [120.746586, 30.658167],
                    [120.694525, 32.784185],
                ],
            ],
        },
        bands: ['1', '2', '3', '4', '5', '6', '7'],
        description: '',
        tileLevelNum: 1,
        tileLevels: ['40031*20016'],
        crs: 'EPSG:32651',
    },
    {
        id: 'SC155541969',
        productId: 'P15303208',
        sensorId: 'SE15303',
        name: 'LC08_L2SP_118038_20240928_20241005_02_T1',
        date: '2024-09-28T00:00:00',
        cloudCover: 13.3009,
        resolution: '30.0m',
        preview_url: 'blob:http://localhost:5173/2d1b9d15-d200-4146-99c7-304094bb7141',
        geoFeature: {
            type: 'Polygon',
            coordinates: [
                [
                    [120.707329, 32.78442],
                    [123.180676, 32.80528],
                    [123.176592, 30.67759],
                    [120.759102, 30.658383],
                    [120.707329, 32.78442],
                ],
            ],
        },
        bands: ['1', '2', '3', '4', '5', '6', '7'],
        description: '',
        tileLevelNum: 1,
        tileLevels: ['40031*20016'],
        crs: 'EPSG:32651',
    },
    {
        id: 'SC153032082',
        productId: 'P15303208',
        sensorId: 'SE15303',
        name: 'LC08_L2SP_118038_20240320_20240402_02_T1',
        date: '2024-03-20T00:00:00',
        cloudCover: 0.924727,
        resolution: '30.0m',
        preview_url: 'blob:http://localhost:5173/c27b87e9-9e10-4080-afd5-5aec97a149a5',
        geoFeature: {
            type: 'Polygon',
            coordinates: [
                [
                    [120.659313, 32.783531],
                    [123.129405, 32.805343],
                    [123.126484, 30.680356],
                    [120.712105, 30.66027],
                    [120.659313, 32.783531],
                ],
            ],
        },
        bands: ['1', '2', '3', '4', '5', '6', '7'],
        description: '',
        tileLevelNum: 1,
        tileLevels: ['40031*20016'],
        crs: 'EPSG:32651',
    },
] as SceneView[]

export const myTestProject: Project = {
    projectId: 'PRJzYdhkbc9SAgmrM4yP',
    projectName: 'huanyuTesting',
    environment: 'Python3_9',
    createTime: '2025-04-03T11:19:10',
    packages: '[numpy]',
    createUser: 'USR22DjM7sfWVlJeT3m2',
    createUserName: 'huanyu',
    createUserEmail: '1923606858@qq.com',
    joinedUsers: [],
    description: '测试项目',
}
