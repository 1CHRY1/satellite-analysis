import { getAddress, getCasePage } from '@/api/http/satellite-data/satellite.api';
import { ref, computed, nextTick } from 'vue';
import type { Case } from '@/api/http/satellite-data/satellite.type';
import type { RegionValues } from 'v-region'
import { formatTime } from '@/util/common';

type HistoryValueTab = 'RUNNING' | 'COMPLETE'
type HistoryTab = {
    label: string
    value: HistoryValueTab
}
type TimeRange = {
    startTime: string,
    endTime: string
}
interface TimeOption {
    label: string
    value: {
      startTime: string
      endTime: string
    } | null
}

export function useViewHistoryModule() {
    /**
    * 列表相关变量
    */
    const caseList = ref<Case.Case[]>([])
    const currentPage = ref<number>(1)
    const pageSize = ref<number>(3)
    const total = ref<number>(0)
    
    const getCaseList = async () => {
        const regionId = getRegionId()
        const res = await getCasePage({
            page: currentPage.value,
            pageSize: pageSize.value,
            asc: false,
            sortField: 'createTime',
            status: activeTab.value,
            regionId: regionId ? Number(regionId) : null,
            startTime: selectedTime.value.value?.startTime as string,
            endTime: selectedTime.value.value?.endTime as string,
            resolution: selectedResolution.value === EMPTY_RESOLUTION ? null : selectedResolution.value,
        })
        caseList.value = res.data.records
        total.value = res.data.total
        // nextTick(() => {
        scrollToTop()
        // })
    }

    /**
    * backToTop相关变量
    */
    const sectionHeader = ref<HTMLDivElement | null>(null)
    const scrollToTop = () => {
        if (sectionHeader.value) {
            sectionHeader.value.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            })
        }
    }

    /**
    * Tab相关变量
    */
    const historyClassTabs = ref<Array<HistoryTab>>([
        {
            label: '运行中',
            value: 'RUNNING'
        },
        {
            label: '已完成',
            value: 'COMPLETE'
        }
    ])
    const activeTab = ref<HistoryValueTab>('RUNNING')
    const handleSelectTab = (value: HistoryValueTab) => {
        total.value = 0
        caseList.value = []
        currentPage.value = 1
        activeTab.value = value
        getCaseList()
    }

    /**
    * 筛选条件相关变量
    */
    const selectedRegion = ref<RegionValues>({
        province: '',
        city: '',
        area: '',
    })
    const getRegionId = () => {
        if (selectedRegion.value.province) {
            if (selectedRegion.value.city) {
                if (selectedRegion.value.area) {
                    return selectedRegion.value.area
                } else {
                    return selectedRegion.value.city
                }
            } else {
                return selectedRegion.value.province
            }
        } else {
            return null
        }
    }
    // 获取今天开始时间（00:00:00）
    const getTodayStart = (): Date => {
        const now = new Date()
        return new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0)
    }
    // 获取今天结束时间（23:59:59）
    const getTodayEnd = (): Date => {
        const now = new Date()
        return new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59)
    }
    // 计算时间范围
    const calculateTimeRange = (type: string) => {
        const now = new Date()
        let startTime: Date
        let endTime: Date = new Date(now) // 当前时间作为结束时间
        
        switch (type) {
        case 'all':
            startTime = new Date("1900-01-01 00:00:00")
            break
        case '1h':
            startTime = new Date(now.getTime() - 60 * 60 * 1000) // 1小时前
            break
        case 'today':
            startTime = getTodayStart()
            endTime = getTodayEnd()
            break
        case '7d':
            startTime = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000) // 7天前
            break
        case '30d':
            startTime = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000) // 30天前
            break
        case '3m':
            startTime = new Date(now)
            startTime.setMonth(startTime.getMonth() - 3) // 3个月前
            break
        case 'earlier':
            startTime = new Date("1900-01-01 00:00:00")
            endTime.setMonth(new Date(now).getMonth() - 3)
            break
        default:
            startTime = now
        }
        
        return {
            startTime: formatTime(startTime, 'seconds', 0, true),
            endTime: formatTime(endTime, 'seconds', 0, true)
        }
    }
    const timeOptionList = computed<TimeOption[]>(() => [
        {
          label: '请选择',
          value: calculateTimeRange('all')
        },
        {
          label: '最近1小时',
          value: calculateTimeRange('1h')
        },
        {
          label: '今天',
          value: calculateTimeRange('today')
        },
        {
          label: '最近7天',
          value: calculateTimeRange('7d')
        },
        {
          label: '最近30天',
          value: calculateTimeRange('30d')
        },
        {
          label: '最近3个月',
          value: calculateTimeRange('3m')
        },
        {
          label: '更早之前',
          value: calculateTimeRange('earlier')
        }
    ])
    const selectedTimeIndex = ref<number>(0)
    const selectedTime = computed<TimeOption>(() => timeOptionList.value[selectedTimeIndex.value])
    const EMPTY_RESOLUTION = 0 // 预设请选择分辨率选项
    const selectedResolution = ref<number>(EMPTY_RESOLUTION)
    const resolutionList = ref<number[]>([EMPTY_RESOLUTION, 1, 2, 5, 10, 15, 20, 25, 30, 40, 50, 80, 100, 150])
    const isExpand = ref<boolean>(true)
    const reset = () => {
        selectedRegion.value = {
            province: '',
            city: '',
            area: ''
        }
        selectedTimeIndex.value = 0
        selectedResolution.value = EMPTY_RESOLUTION
        getCaseList()
    }

    return {
        caseList,
        currentPage,
        sectionHeader,
        pageSize,
        total,
        historyClassTabs,
        activeTab,
        handleSelectTab,
        selectedRegion,
        getCaseList,
        selectedTime,
        selectedTimeIndex,
        selectedResolution,
        EMPTY_RESOLUTION,
        timeOptionList,
        resolutionList,
        isExpand,
        reset
    }
}
