import type { Dayjs } from 'dayjs'
import { type ProductView } from './apiAdapter/adapter'
import type { polygonGeometry } from '@/util/share.type'

export interface ImageFilterCondition {
    product: ProductView
    dateRange: [Dayjs, Dayjs]
    geometry: polygonGeometry
    cloudCover: [number, number]
}

export interface Project {
    projectId: string
    projectName: string
    environment: string
    createTime: string
    packages: string
    createUser: string
    createUserName: string
    createUserEmail: string
    joinedUsers: string[]
    description: string
}

export interface interactiveExplore {
    regionCode: number
    dataRange: string[]
    cloud: number
    space: number
}
