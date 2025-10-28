import http from '~/apis/clients/adminClient'
import type { CommonResponse } from '../common.type'
import type { SensorIds, SensorPageRequest, SensorPageResponse } from './sensor.type'
import type { Sensor } from '~/types/sensor'

export async function getSensorPage(sensorInfo: SensorPageRequest): Promise<CommonResponse<SensorPageResponse>> {
    return http.post<CommonResponse<SensorPageResponse>>(`sensor/page`, sensorInfo)
}

export async function addSensor(sensorInfo: Sensor): Promise<CommonResponse<any>> {
    return http.put<CommonResponse<any>>(`sensor/insert`, sensorInfo)
}

export async function updateSensor(sensorInfo: Sensor): Promise<CommonResponse<any>> {
    return http.post<CommonResponse<any>>(`sensor/update`, sensorInfo)
}

export async function batchDelSensor(sensorIds: SensorIds): Promise<CommonResponse<any>> {
    return http.delete<CommonResponse<any>>(`sensor/delete`, {data: sensorIds})
}