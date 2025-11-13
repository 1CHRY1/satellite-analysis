import { number } from 'echarts'
import { defineStore } from 'pinia'

export const useExploreStore = defineStore('exploreStore', {
    state() {
        return {
            searchtab: '' as string,
            regionCode: 0 as number,
            dataRange: '' as string,
            cloud: 0 as number,
            gridResolution: 0 as number,
            coverage: '' as string,
            allCoverage: [] as any,
            // images: [] as any[],
            grids: [] as any[],
            boundary: [] as any[],
            load: true,
            // 新增🌟
            sensors: [] as {
                sensorName: string
                platformName: string
            }[],
        }
    },
    actions: {
        // 更新单字段
        updateField(field, value) {
            if (this.hasOwnProperty(field)) {
                this[field] = value
            }
        },
        // 批量更新
        updateFields(load) {
            Object.keys(load).forEach((key) => {
                if (this.hasOwnProperty(key)) {
                    this[key] = load[key]
                }
            })
        },
    },
})
