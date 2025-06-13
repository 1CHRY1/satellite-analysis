import { getCasePage } from '@/api/http/satellite-data/satellite.api';
import { ref, computed, nextTick } from 'vue';
import type { Case } from '@/api/http/satellite-data/satellite.type';
import type { RegionValues } from 'v-region'

type HistoryValueTab = 'running' | 'complete'
type HistoryTab = {
    label: string
    value: HistoryValueTab
}
type TimeRange = {
    startTime: string,
    endTime: string
}
type TimeOption = {
    label: string,
    value: TimeRange | null
}

export function useViewHistoryModule() {
    const caseList = ref<Case.Case[]>([])
    const currentPage = ref<number>(1)
    const pageSize = ref<number>(3)
    const total = ref<number>(0)
    const sectionHeader = ref<HTMLDivElement | null>(null)
    const historyClassTabs = ref<Array<HistoryTab>>([
        {
            label: '运行中',
            value: 'running'
        },
        {
            label: '已完成',
            value: 'complete'
        }
    ])
    const activeTab = ref<HistoryValueTab>('running')
    const handleSelectTab = (value: HistoryValueTab) => {
        activeTab.value = value
    }

    /**
    * 筛选条件相关变量
    */
    const selectedRegion = ref<RegionValues>({
        province: '',
        city: '',
        area: '',
    })
    const selectedTime = ref<TimeOption>({ label: '请选择', value: null })
    const timeOptionList = ref<TimeOption[]>([
        {
            label: '请选择',
            value: null
        },
        {
            label: '7天内',
            value: {
                startTime: '',
                endTime: ''
            }
        }
    ])
    const EMPTY_RESOLUTION = 0 // 预设请选择分辨率选项
    const selectedResolution = ref<number>(EMPTY_RESOLUTION)
    const resolutionList = ref<number[]>([EMPTY_RESOLUTION, 1, 2, 5, 10, 15, 20, 25, 30, 40, 50, 80, 100, 150])
    const isExpand = ref<boolean>(false)

    const scrollToTop = () => {
        if (sectionHeader.value) {
            sectionHeader.value.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            })
        }
    }

    const getCaseList = async (currentPage: number) => {
        const res = await getCasePage({
            page: currentPage,
            pageSize: pageSize.value,
            asc: false,
            sortField: 'createTime'
        })
        caseList.value = res.data.records
        total.value = res.data.total
        // nextTick(() => {
        scrollToTop()
        // })
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
        selectedResolution,
        EMPTY_RESOLUTION,
        timeOptionList,
        resolutionList,
        isExpand
    }

}
