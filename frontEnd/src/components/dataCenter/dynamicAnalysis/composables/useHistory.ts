import { ref, computed, nextTick, reactive } from 'vue'
import type { RegionValues } from 'v-region'
import { formatTime } from '@/util/common'
import {
    getImgBounds,
    getImgBoundsFromInfoGeoJson,
    getImgStatistics,
    getImgStatisticsWithUrl,
    getNoCloudUrl4MosaicJson,
} from '@/api/http/satellite-data/visualize.api'
import { message, type SelectProps } from 'ant-design-vue'
import * as MapOperation from '@/util/map/operation'
import { defineEmits } from 'vue'
import { useAnalysisStore } from '@/store'
import type { MethLib, MethLibCase } from '@/api/http/analytics-display/methlib.type'
import {
    getMethLibCaseById,
    getMethLibCasePage,
    getMethodById,
} from '@/api/http/analytics-display/methlib.api'
import type { CommonResponse } from '@/api/http/common.type'
import { getTileFromMiniIo } from '@/api/http/analysis'
import { map_fitView } from '@/util/map/operation/common'
import dayjs from 'dayjs'

const dbValue = useAnalysisStore()
type HistoryValueTab = 'RUNNING' | 'COMPLETE'
type HistoryTab = {
    label: string
    value: HistoryValueTab
}
type TimeRange = {
    startTime: string
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
    const methLibCaseList = ref<MethLibCase.Case[]>([])
    const currentPage = ref<number>(1)
    const pageSize = ref<number>(3)
    const total = ref<number>(0)

    const getMethLibCaseList = async () => {
        const res = await getMethLibCasePage({
            page: currentPage.value,
            pageSize: pageSize.value,
            asc: false,
            sortField: 'createTime',
            status: activeTab.value,
            startTime: selectedTime.value.value?.startTime as string,
            endTime: selectedTime.value.value?.endTime as string,
            // startTime: dayjs(selectedTime.value.value?.startTime).format('YYYY-MM-DDTHH:mm:ss'),
            // endTime: dayjs(selectedTime.value.value?.endTime).format('YYYY-MM-DDTHH:mm:ss'),
        })
        methLibCaseList.value = res.data.records
        // TODO 可以做一个缓存；请求所有记录对应的方法
        let promises: Promise<CommonResponse<MethLib.Method>>[] = []
        for (let methLibCase of methLibCaseList.value) {
            promises.push(getMethodById(methLibCase.methodId))
        }
        let allRes = await Promise.all(promises)
        for (let i = 0; i < methLibCaseList.value.length; i++) {
            methLibCaseList.value[i].method = allRes[i].data
        }
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
                block: 'start',
            })
        }
    }

    /**
     * Tab相关变量
     */
    const historyClassTabs = ref<Array<HistoryTab>>([
        {
            label: '运行中',
            value: 'RUNNING',
        },
        {
            label: '已完成',
            value: 'COMPLETE',
        },
    ])
    const activeTab = ref<HistoryValueTab>('RUNNING')
    const handleSelectTab = (value: HistoryValueTab) => {
        total.value = 0
        methLibCaseList.value = []
        currentPage.value = 1
        activeTab.value = value
        getMethLibCaseList()
    }

    /**
     * 筛选条件相关变量
     */
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
                startTime = new Date('1900-01-01 00:00:00')
                break
            case '1h':
                startTime = new Date(now.getTime() - 60 * 60 * 1000)
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
                startTime = new Date('1900-01-01 00:00:00')
                endTime.setMonth(new Date(now).getMonth() - 3)
                break
            default:
                startTime = now
        }

        return {
            startTime: formatTime(startTime, 'seconds', 0, true),
            endTime: formatTime(endTime, 'seconds', 0, true),
        }
    }
    const timeOptionList = computed<TimeOption[]>(() => [
        {
            label: '请选择',
            value: calculateTimeRange('all'),
        },
        {
            label: '最近1小时',
            value: calculateTimeRange('1h'),
        },
        {
            label: '今天',
            value: calculateTimeRange('today'),
        },
        {
            label: '最近7天',
            value: calculateTimeRange('7d'),
        },
        {
            label: '最近30天',
            value: calculateTimeRange('30d'),
        },
        {
            label: '最近3个月',
            value: calculateTimeRange('3m'),
        },
        {
            label: '更早之前',
            value: calculateTimeRange('earlier'),
        },
    ])
    const selectedTimeIndex = ref<number>(0)
    const selectedTime = computed<TimeOption>(() => timeOptionList.value[selectedTimeIndex.value])
    const isExpand = ref<boolean>(true)
    const reset = () => {
        selectedTimeIndex.value = 0
        getMethLibCaseList()
    }

    /**
     * 获取输入文件
     */
    const getParamKeys = (
        method: MethLib.Method | undefined,
        type: 'input' | 'output',
    ): string[] => {
        if (method == undefined || !method.params) {
            return []
        }
        let keys: string[] = []
        let targetTypes: string[] = []
        if (type === 'input') {
            targetTypes = ['ExistingFile', 'ExistingFileOrFloat', 'FileList']
        } else {
            targetTypes = ['NewFile']
        }
        for (const [index, param] of method.params.entries()) {
            if (typeof param !== 'object' || param === null) {
                continue
            }
            const paramType = param.parameter_type
            if (typeof paramType !== 'object' || paramType === null) {
                continue
            }
            const paramTypeKeys = Object.keys(paramType)
            const isInputFileParam = targetTypes.some((targetKey) =>
                paramTypeKeys.includes(targetKey),
            )
            if (isInputFileParam) {
                keys.push(`val${index}`)
            }
        }
        return keys
    }

    const getFileNameFromUrl = (url: string): string => {
        if (!url) {
            return ''
        }
        url = url.replace(/\/$/, '')
        const parts = url.split('/')
        const fileName = parts[parts.length - 1]

        return fileName || ''
    }

    const getPathFromUrl = (url: string): string => {
        if (!url) {
            return ''
        }
        try {
            const urlObj = new URL(url)
            return urlObj.pathname + urlObj.search
        } catch (e) {
            const firstSlashIndex = url.indexOf('/')
            if (firstSlashIndex === -1) {
                return ''
            }
            return url.substring(firstSlashIndex)
        }
    }

    const getInputFileNames = (item: MethLibCase.Case) => {
        let keys = getParamKeys(item.method, 'input')
        let urls: string[] = []
        for (let key of keys) {
            if (typeof item.params[key] === 'string') {
                urls.push(item.params[key])
            } else if (Array.isArray(item.params[key])) {
                urls.push(...item.params[key])
            }
        }
        let fileNames: string[] = []
        for (let url of urls) {
            fileNames.push(getFileNameFromUrl(url))
        }
        return fileNames
    }

    const getOutputFileUrls = (item: MethLibCase.Case) => {
        console.log(item)
        let keys = getParamKeys(item.method, 'output')
        let urls: string[] = []
        for (let key of keys) {
            if (typeof item.result[key] === 'string') {
                urls.push(item.result[key])
            } else if (Array.isArray(item.result[key])) {
                urls.push(...item.result[key])
            }
        }
        return urls
    }

    const getOutputFileObjs = (item: MethLibCase.Case) => {
        let urls = getOutputFileUrls(item)
        console.log(urls)
        let objs: FileObj[] = []

        for (let url of urls) {
            // TODO 当前仅筛选TIF文件
            if (
                typeof url === 'string' &&
                (url.toLowerCase().endsWith('.tif') || url.toLowerCase().endsWith('.tiff'))
            ) {
                objs.push({
                    label: getFileNameFromUrl(url),
                    value: url,
                    path: getPathFromUrl(url),
                    bidxOptions: [],
                    selectedBidx: undefined,
                })
            }
        }
        return objs
    }

    /**
     * 栅格可视化配置对话框相关变量
     */
    type FileObj = {
        label: string
        value: string
        path: string
        bidxOptions?: string[]
        stats?: any[]
        selectedBidx?: string
    }

    const showModal = ref<boolean>(false)

    // 2. 选项和信息
    const visFormRef = ref(null) // 用于表单引用和校验
    const colormapName = ref<string>('rdylgn')
    const colormapOptions = ref<SelectProps['options']>([
        {
            value: 'viridis',
            label: 'Viridis',
            gradient:
                'linear-gradient(to right, #440154, #482475, #414487, #355f8d, #2a788e, #21908d, #22a884, #42be71, #7ad151, #bddf26, #fde725)',
        },
        {
            value: 'plasma',
            label: 'Plasma',
            gradient:
                'linear-gradient(to right, #0d0887, #46039f, #7201a8, #9c179e, #bd3786, #d8576b, #ed7953, #fb9f3a, #fdca26, #f0f921)',
        },
        {
            value: 'magma',
            label: 'Magma',
            gradient:
                'linear-gradient(to right, #000004, #1b0b41, #4b0c6b, #781c6d, #a52c60, #cf4446, #ed6925, #fb9b06, #f7d13d, #fcffa4)',
        },
        {
            value: 'inferno',
            label: 'Inferno',
            gradient:
                'linear-gradient(to right, #000004, #1b0c42, #4b0c6b, #781c6d, #a52c5f, #cf4446, #ed6925, #fb9b06, #f7ce3b, #fcffa4)',
        },
        {
            value: 'cividis',
            label: 'Cividis',
            gradient:
                'linear-gradient(to right, #00204d, #123570, #3b496c, #575d6d, #707173, #8a8778, #a69d75, #c4b56e, #e4cf5b, #ffea46)',
        },
        {
            value: 'turbo',
            label: 'Turbo',
            gradient:
                'linear-gradient(to right, #30123b, #4145ab, #4675ed, #39a2fc, #1bcfd4, #24eca6, #61fc6c, #a4fc3b, #d1e834, #f3c63a, #fe9b2d, #f36315, #d93806, #b11901, #7a0402)',
        },
        {
            value: 'rainbow',
            label: 'Rainbow',
            gradient:
                'linear-gradient(to right, #ff0000, #ff8000, #ffff00, #80ff00, #00ff00, #00ff80, #00ffff, #0080ff, #0000ff, #8000ff, #ff00ff)',
        },
        {
            value: 'coolwarm',
            label: 'Cool-Warm',
            gradient:
                'linear-gradient(to right, #3b4cc0, #5977e2, #8da0fa, #c5caf2, #e2e2e2, #f1b6b6, #e67b7b, #d14949, #b2182b)',
        },
        {
            value: 'rdylgn',
            label: 'Red-Yellow-Green',
            gradient:
                'linear-gradient(to right, #a50026, #d73027, #f46d43, #fdae61, #fee08b, #ffffbf, #d9ef8b, #a6d96a, #66bd63, #1a9850, #006837)',
        },
        {
            value: 'bwr',
            label: 'Blue-White-Red',
            gradient:
                'linear-gradient(to right, #0000ff, #4444ff, #8888ff, #ccccff, #ffffff, #ffcccc, #ff8888, #ff4444, #ff0000)',
        },
        {
            value: 'seismic',
            label: 'Seismic',
            gradient:
                'linear-gradient(to right, #00004d, #0000a3, #1c1cff, #6e6eff, #b9b9ff, #ffffff, #ffb9b9, #ff6e6e, #ff1c1c, #a30000, #4d0000)',
        },
        {
            value: 'jet',
            label: 'Jet',
            gradient:
                'linear-gradient(to right, #00007f, #0000ff, #007fff, #00ffff, #7fff7f, #ffff00, #ff7f00, #ff0000, #7f0000)',
        },
        {
            value: 'hot',
            label: 'Hot',
            gradient:
                'linear-gradient(to right, #0b0000, #4b0000, #960000, #e10000, #ff3d00, #ff7800, #ffb600, #fff100, #ffff6d)',
        },
        {
            value: 'hsv',
            label: 'HSV',
            gradient:
                'linear-gradient(to right, #ff0000, #ff00cc, #cc00ff, #6600ff, #0000ff, #0066ff, #00ccff, #00ffff, #00ffcc, #00ff66, #00ff00, #66ff00, #ccff00, #ffff00, #ffcc00, #ff6600)',
        },
        {
            value: 'blues',
            label: 'Blues',
            gradient:
                'linear-gradient(to right, #f7fbff, #deebf7, #c6dbef, #9ecae1, #6baed6, #4292c6, #2171b5, #08519c, #08306b)',
        },
        {
            value: 'greens',
            label: 'Greens',
            gradient:
                'linear-gradient(to right, #f7fcf5, #e5f5e0, #c7e9c0, #a1d99b, #74c476, #41ab5d, #238b45, #006d2c, #00441b)',
        },
        {
            value: 'reds',
            label: 'Reds',
            gradient:
                'linear-gradient(to right, #fff5f0, #fee0d2, #fcbba1, #fc9272, #fb6a4a, #ef3b2c, #cb181d, #a50f15, #67000d)',
        },
    ])
    const formState = reactive({
        colormapName: colormapOptions.value![0].value, // 选择的色带名称
        selectedTif: undefined, // 选择的 TIF 文件 ID
        selectedBidx: undefined, // 选择的波段 ID
        range: {
            min: undefined, // 自定义可视化最小
            max: undefined, // 自定义可视化最大
        },
    })
    const tifOptions = ref<FileObj[]>([])
    const bidxOptions = ref<string[]>([]) // 根据 selectedTif 动态更新
    const selectedBandInfo = ref<any>()
    const formRules = {
        colormapName: [{ required: true, message: '请选择色带' }],
        selectedTif: [{ required: true, message: '请选择栅格文件' }],
        selectedBidx: [{ required: true, message: '请选择波段' }],
        // 范围校验可以放在自定义校验函数中，确保 Min < Max
        range: {
            min: [{ required: true, message: '请输入最小值' }],
            max: [{ required: true, message: '请输入最大值' }],
        },
    }

    // 3. 事件处理函数
    const handleTifChange = (tifId) => {
        bidxOptions.value = tifOptions.value[tifId].bidxOptions as string[]
        // 重置波段和范围
        selectedBandInfo.value = {}
        formState.selectedBidx = undefined
        formState.range.min = undefined
        formState.range.max = undefined
    }

    const handleBidxChange = async (bidx) => {
        selectedBandInfo.value = tifOptions.value[formState.selectedTif || 0].stats?.[bidx]
        formState.range.min = selectedBandInfo.value.min
        formState.range.max = selectedBandInfo.value.max
    }

    // 4. 表单操作
    const handleModalCancel = () => {
        (visFormRef.value as any).resetFields() // 取消时重置表单
        previewIndex.value = null
        showModal.value = false
    }

    const handleConfirm = () => {
        ;(visFormRef.value as any)
            .validate()
            .then(() => {
                previewOutputFile()
                showModal.value = false
            })
            .catch((error) => {})
    }

    /**
     * 预览输出TIF
     */
    const previewList = computed<boolean[]>(() => {
        const list = Array(total.value).fill(false)
        if (previewIndex.value !== null) {
            list[previewIndex.value] = true
        }
        return list
    })
    const previewIndex = ref<number | null>(null)
    const previewOutputFile = async () => {
        let tifUrl = tifOptions.value[formState.selectedTif || 0].value
        const bounds = await getImgBoundsFromInfoGeoJson(tifUrl)
        let wholeTileUrl = `/tiler/tiles/WebMercatorQuad/{z}/{x}/{y}.png?scale=1&url=${tifUrl}&colormap_name=${formState.colormapName}&rescale=${formState.range.min},${formState.range.max}`
        map_fitView(bounds)
        MapOperation.addRasterLayerFromUrl(wholeTileUrl, `methlib-layer`)
    }

    const onResultSelected = ref<((result: any) => void) | null>(null)

    const handleShowModal = async (item: MethLibCase.Case) => {
        // 清除变量
        unPreview()
        formState.range = {min: undefined, max: undefined}
        formState.selectedBidx = undefined
        formState.selectedTif = undefined
        bidxOptions.value = []
        selectedBandInfo.value = {}
        previewIndex.value = methLibCaseList.value.findIndex((i) => i.caseId === item.caseId)
        
        showModal.value = true
        tifOptions.value = getOutputFileObjs(item)
        let promises: Promise<any>[] = []
        for (let tif of tifOptions.value) {
            promises.push(getImgStatisticsWithUrl(tif.value))
        }
        let allRes = await Promise.all(promises)
        for (let [index, stats] of allRes.entries()) {
            console.log(tifOptions.value)
            tifOptions.value[index].stats = stats
            tifOptions.value[index].bidxOptions = Object.keys(stats)
        }
        if (onResultSelected.value) {
            onResultSelected.value(item)
        }
    }
    const unPreview = () => {
        previewIndex.value = null
        MapOperation.removeRasterLayer('methlib-layer')
    }

    return {
        methLibCaseList,
        currentPage,
        sectionHeader,
        pageSize,
        total,
        historyClassTabs,
        activeTab,
        handleSelectTab,
        getMethLibCaseList,
        selectedTime,
        selectedTimeIndex,
        timeOptionList,
        isExpand,
        reset,
        previewList,
        previewIndex,
        handleShowModal,
        unPreview,
        onResultSelected,
        getInputFileNames,
        showModal,
        handleModalCancel,
        handleConfirm,
        colormapName,
        colormapOptions,
        formRules,
        formState,
        handleTifChange,
        tifOptions,
        handleBidxChange,
        selectedBandInfo,
        bidxOptions,
        visFormRef,
    }
}
