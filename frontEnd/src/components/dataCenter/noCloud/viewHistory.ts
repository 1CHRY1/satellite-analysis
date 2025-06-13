import { getCasePage } from '@/api/http/satellite-data/satellite.api';
import { ref, computed, nextTick } from 'vue';
import type { Case } from '@/api/http/satellite-data/satellite.type';

type HistoryValueTab = 'running' | 'complete'
type HistoryTab = {
    label: string
    value: HistoryValueTab
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
        getCaseList
    }

}
