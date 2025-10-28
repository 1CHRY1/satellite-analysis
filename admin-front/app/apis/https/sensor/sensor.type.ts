import type { Sensor } from "~/types/sensor";
import type { PageRequest, PageResponse } from "../common.type";

export type SensorPageRequest = PageRequest

export type SensorPageResponse = PageResponse<Sensor>


export interface SensorIds {
    sensorIds: string[]
}