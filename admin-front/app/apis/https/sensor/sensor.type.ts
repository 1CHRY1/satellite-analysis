import type { Sensor } from "~/types/sensor";
import type { PageRequest, PageResponse } from "../common.type";

export interface SensorPageRequest extends PageRequest {
    dataTypes?: string[]
}

export type SensorPageResponse = PageResponse<Sensor>


export interface SensorIds {
    sensorIds: string[]
}