import { ref, reactive } from 'vue'
import { useExploreStore } from '@/store/exploreStore'
import { useTaskStore } from '@/store'

/**
 * pictureOfNoCloud 各功能的公共变量
 */

// 探索数据存储
export const exploreData = useExploreStore()

// 任务存储
export const taskStore = useTaskStore()

// 面板显示控制
export const showPanel = ref(true)
export const currentPanel = ref('noCloud')
export const setCurrentPanel = (panel: string) => {
    currentPanel.value = panel
}

// 控制无云一版图内容的折叠状态
export const isNoCloudExpand = ref<boolean>(true)
export const isComplexExpand = ref<boolean>(false)

// 地图展示
export const isPicking = ref(false)

// 无云一版图加载状态
export const noCloudLoading = ref(false)

// 计算任务
export const calTask = ref({
    calState: 'start',
    taskId: '',
})

// 填补勾选框
export const additionalData = ref([false, false, false])

// 数据重构勾选框
export const dataReconstruction = ref([false, false, false])

// 是否合并
export const isMerging = ref(false)

// 进度条相关
export const progress = ref([0, 0, 0, 0])
export const showProgress = ref([false, false, false, false])

// 复杂合成进度控制
export const complexProgress = ref([0, 0, 0, 0])
export const showComplexProgress = ref([false, false, false, false])
export const complexSynthesisLoading = ref(false)
export const hasComplexResult = ref(false)

// 覆盖率
export interface CoverageRate {
    demotic1m: string | null
    demotic2m: string | null
    international: string | null
    addRadar: string | null
}

export const coverageRate = ref<CoverageRate>({
    demotic1m: null,
    demotic2m: null,
    international: null,
    addRadar: null,
})

// 平台类型接口
export interface platformType {
    platformName: string,
    tags?: string[]
    resolution: string,
    sceneId: string[],
    sensorName: string
}

// 传感器选择
export const selectnation = ref<platformType | null>(null)
export const selectinternation = ref<platformType | null>(null)
export const selectsar = ref<platformType | null>(null)

// 格网渲染相关数据
export const gridRenderingData = reactive({
    demotic1m: {
        rendered: false,
        grids: [] as any[],
        color: '#00FFFF' // 青色
    },
    demotic2m: {
        rendered: false,
        grids: [] as any[],
        color: '#00FF00' // 绿色
    },
    international: {
        rendered: false,
        grids: [] as any[],
        color: '#FFA500' // 橙色
    },
    radar: {
        rendered: false,
        grids: [] as any[],
        color: '#FF0000' // 红色
    }
})

// 格网渲染状态跟踪
export const renderedGrids = ref<Set<string>>(new Set()) // 记录已渲染的格网ID
export const gridRenderingStatus = ref<Map<string, string>>(new Map()) // 记录格网渲染类型