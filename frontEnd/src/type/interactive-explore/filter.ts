import type { RegionValues } from "v-region"

export type SpatialFilterMethod = 'region' | 'poi' | 'None'
import type { Dayjs } from 'dayjs'

export type POIInfo = {
    // adcode: string,
    // adname: string,
    gcj02Lat: string,
    gcj02Lon: string,
    geometry: any,
    id: string,
    name: string,
    address: string
    pname: string
    cityname: string
    adname: string
}

export type FilterConfig = {
    dateRange: [Dayjs, Dayjs],
    gridResolution: number,
    region: RegionValues,
}

export interface FilterTab {
    value: SpatialFilterMethod
    label: string
}