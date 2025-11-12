import { useToolRegistryStore, useUserStore } from '@/store'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, defineAsyncComponent, nextTick, ref, watch } from 'vue'
const toolRegistry = useToolRegistryStore()
import { useI18n } from 'vue-i18n'
import DynamicServicePanel from '../../thematic/DynamicServicePanel.vue'
import { selectedResult, thematicConfig } from '../shared'

export const useTool = () => {
    const { t } = useI18n()
    // {
    //     name: '景级分析工具',
    //     tools: [
    //         { value: '1', label: 'Math and Stats Tools', disabled: false },
    //         { value: '2', label: 'Image Processing Tools', disabled: false },
    //         { value: '3', label: 'Filters', disabled: false },
    //         { value: '4', label: 'Data Tools', disabled: false },
    //         { value: '5', label: 'GIS Analysis', disabled: false },
    //         { value: '6', label: 'LiDAR Tools', disabled: false },
    //         { value: '7', label: 'Geomorphometric Analysis', disabled: false },
    //         { value: '8', label: 'Hydrological Analysis', disabled: false },
    //         { value: '9', label: 'Overlay Tools', disabled: false },
    //         { value: '10', label: 'Image Enhancement', disabled: false },
    //         { value: '11', label: 'Patch Shape Tools', disabled: false },
    //         { value: '12', label: 'Distance Tools', disabled: false },
    //         { value: '13', label: 'Stream Network Analysis', disabled: false },
    //         { value: '14', label: 'Whitebox Utilities', disabled: false },
    //         { value: '15', label: 'Machine Learning', disabled: false },
    //     ],
    // },
    const isToolsExpand = ref(true)
    /**
     * Category相关变量
     */
    const builtinToolCategories = [
        {
            name: '高程模型分析',
            tools: [
                {
                    value: 'DSM分析',
                    label: t('datapage.analysis.optionallab.task_DSM'),
                    disabled: false,
                },
                {
                    value: 'DEM分析',
                    label: t('datapage.analysis.optionallab.task_DEM'),
                    disabled: false,
                },
            ],
        },
        {
            name: '时序与植被指数',
            tools: [
                { value: '指数分析', label: '指数分析', disabled: false },
                {
                    value: 'NDVI时序计算',
                    label: t('datapage.analysis.optionallab.task_NDVI'),
                    disabled: false,
                },
            ],
        },
        {
            name: '光谱与色彩处理',
            tools: [
                {
                    value: '光谱分析',
                    label: t('datapage.analysis.optionallab.task_spectral'),
                    disabled: false,
                },
                { value: '伪彩色分割', label: '伪彩色分割', disabled: false },
            ],
        },
        {
            name: '三维与形变分析',
            tools: [
                {
                    value: '红绿立体',
                    label: t('datapage.analysis.optionallab.task_red_green'),
                    disabled: false,
                },
                {
                    value: '形变速率',
                    label: t('datapage.analysis.optionallab.task_rate'),
                    disabled: false,
                },
            ],
        },
    ]

    const expandedCategories = ref<string[]>(['景级分析工具'])

    // const filteredCategories = computed(() => {
    //     const categories = allToolCategories.value
    //     if (!searchQuery.value) return categories

    //     const query = searchQuery.value.toLowerCase()
    //     return categories
    //         .map(category => ({
    //             ...category,
    //             tools: category.tools.filter(tool =>
    //                 tool.label.toLowerCase().includes(query) ||
    //                 category.name.toLowerCase().includes(query)
    //             )
    //         }))
    //         .filter(category => category.tools.length > 0)
    // })

    const dynamicToolCategories = computed(() => {
        const categoryMap = new Map<
            string,
            { name: string; tools: Array<{ value: string; label: string; disabled: boolean }> }
        >()
        toolRegistry.tools.forEach((tool) => {
            const categoryName = tool.category || '自定义'
            if (!categoryMap.has(categoryName)) {
                categoryMap.set(categoryName, {
                    name: categoryName,
                    tools: [],
                })
            }
            categoryMap.get(categoryName)?.tools.push({
                value: `dynamic:${tool.id}`,
                label: tool.name,
                disabled: false,
            })
        })
        return Array.from(categoryMap.values())
    })

    const allToolCategories = computed(() => {
        const categoryMap = new Map<
            string,
            { name: string; tools: Array<{ value: string; label: string; disabled: boolean }> }
        >()
        builtinToolCategories.forEach((category) => {
            categoryMap.set(category.name, {
                name: category.name,
                tools: [...category.tools],
            })
        })
        dynamicToolCategories.value.forEach((category) => {
            if (!categoryMap.has(category.name)) {
                categoryMap.set(category.name, {
                    name: category.name,
                    tools: [],
                })
            }
            categoryMap.get(category.name)?.tools.push(...category.tools)
        })
        return Array.from(categoryMap.values())
    })
    const selectedTask = ref<string>('')

    /**
     * 用户变化，重新加载工具
     */
    const userStore = useUserStore()
    const currentUserId = computed(() => userStore.user?.id ?? '')
    const toolRegistry = useToolRegistryStore()
    watch(
        currentUserId,
        (id) => {
            if (id) {
                toolRegistry.ensureLoaded(id)
            }
        },
        { immediate: true },
    )

    // 工具目录
    const fallbackTaskValue = computed(() => {
        const firstCategory = allToolCategories.value.find((category) => category.tools.length > 0)
        return firstCategory?.tools[0]?.value ?? ''
    })

    watch(
        fallbackTaskValue,
        (value) => {
            if (!selectedTask.value && value) {
                selectedTask.value = value
            }
        },
        { immediate: true },
    )

    watch(
        () => toolRegistry.tools.map((tool) => tool.id),
        () => {
            if (selectedTask.value.startsWith('dynamic:')) {
                const toolId = selectedTask.value.replace('dynamic:', '')
                if (!toolRegistry.getToolById(toolId)) {
                    selectedTask.value = fallbackTaskValue.value
                }
            }
        },
    )

    const toggleCategory = (categoryName: string) => {
        const index = expandedCategories.value.indexOf(categoryName)
        if (index >= 0) {
            expandedCategories.value.splice(index, 1)
        } else {
            expandedCategories.value.push(categoryName)
        }
    }

    // 专题组件映射
    const taskComponentMap = {
        伪彩色分割: defineAsyncComponent(() => import('../../thematic/colorThresholdPanel.vue')),
        指数分析: defineAsyncComponent(() => import('../../thematic/indexPanel.vue')),
        NDVI时序计算: defineAsyncComponent(() => import('../../thematic/ndviPanel.vue')),
        光谱分析: defineAsyncComponent(() => import('../../thematic/spectrumPanel.vue')),
        DSM分析: defineAsyncComponent(() => import('../../thematic/dsmPanel.vue')),
        DEM分析: defineAsyncComponent(() => import('../../thematic/demPanel.vue')),
        红绿立体: defineAsyncComponent(() => import('../../thematic/RBbandsPanel.vue')),
        形变速率: defineAsyncComponent(() => import('../../thematic/deformationRate.vue')),
    }

    const dynamicSelectedTool = computed(() => {
        if (!selectedTask.value.startsWith('dynamic:')) return null
        const toolId = selectedTask.value.replace('dynamic:', '')
        return toolRegistry.getToolById(toolId) ?? null
    })

    const currentTaskComponent = computed(() => {
        if (dynamicSelectedTool.value) {
            return DynamicServicePanel
        }

        return taskComponentMap[selectedTask.value] || null
    })

    const currentTaskProps = computed(() => {
        if (dynamicSelectedTool.value) {
            return {
                thematicConfig: thematicConfig.value,
                toolMeta: dynamicSelectedTool.value,
            }
        }
        return {
            thematicConfig: thematicConfig.value,
        }
    })

    const handleResultLoaded = (result) => {
        selectedResult.value = result
    }

    const handleRemoveDynamicTool = async (toolValue: string) => {
        const toolId = toolValue.replace('dynamic:', '')
        const toolMeta = toolRegistry.getToolById(toolId)
        if (!toolMeta) return
        try {
            await ElMessageBox.confirm(
                `确定要取消发布工具“${toolMeta.name}”吗？取消发布后工具将被从工具目录中移除。`,
                '取消发布',
                {
                    confirmButtonText: '确认',
                    cancelButtonText: '保留',
                    type: 'warning',
                },
            )
        } catch {
            return
        }
        toolRegistry.removeTool(currentUserId.value, toolId)
        if (selectedTask.value === toolValue) {
            nextTick(() => {
                selectedTask.value = fallbackTaskValue.value
            })
        }
        ElMessage.success('已取消发布工具')
    }

    return {
        builtinToolCategories,
        expandedCategories,
        dynamicToolCategories,
        allToolCategories,
        selectedTask,
        isToolsExpand,
        currentTaskComponent,
        currentTaskProps,
        handleResultLoaded,
        handleRemoveDynamicTool,
        toggleCategory,
    }
}
