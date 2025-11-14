import { getAllTag, getMethLibPage, invokeMethod } from '@/api/http/analytics-display/methlib.api'
import type { MethLib } from '@/api/http/analytics-display/methlib.type'
import { message, type TourProps } from 'ant-design-vue'
import { computed, reactive, ref, createVNode } from 'vue'
import banner from "@/assets/image/home/banner.jpeg"
import { calTask, setCurrentPanel, taskStore } from '../shared'

export const useMethLib = () => {
    const isMethLibExpand = ref(true)
    /**
     * 列表相关变量
     */
    const searchQuery = ref('')
    const methLibList = ref<MethLib.Method[]>([])
    const currentPage = ref<number>(1)
    const pageSize = ref<number>(3)
    const total = ref<number>(1)
    const getMethLibList = async () => {
        // TEMP
        //
        const res = await getMethLibPage({
            page: currentPage.value,
            pageSize: pageSize.value,
            asc: true,
            sortField: 'name',
            searchText: searchQuery.value,
            // tags: [selectedTask.value],
            tags: selectTagIds.value,
        })
        methLibList.value = res.data.records
        total.value = res.data.total
    }

    const tagList = ref<MethLib.Tag[]>([])
    const selectTags = reactive<Boolean[]>([])
    const selectTagIds = computed(() => {
        return tagList.value.filter((_, index) => selectTags[index]).map((tag) => tag.id)
    })
    const getTagList = async () => {
        const res = await getAllTag()
        // selectTags = Array(res.data.length).fill(false) // 相当于直接整个替换响应式变量
        selectTags.length = 0
        selectTags.push(...Array(res.data.length).fill(false))
        if (selectTags.length > 0) selectTags[0] = true
        tagList.value = res.data
    }

    /**
     * 控制方法调用
     */
    const showModal = ref(false)
    const selectedItem = ref(null)

    function openModal(item) {
        selectedItem.value = item
        showModal.value = true
    }

    const handleInvoke = async (params: any) => {
        taskStore.setIsInitialTaskPending(true)
        setCurrentPanel('history')
        // 转换参数，将 {label, value} 展示性的对象转换成纯value值
        for (let [key, val] of Object.entries(params.formData)) {
            if (params.formData[key].value !== undefined) {
                params.formData[key] = params.formData[key].value
            }
        }
        console.log(params.formData)
        // 调用方法
        let calcRes = await invokeMethod(params.method.id, params.formData)
        message.success("调用成功，您可以在历史记录当中查看")
         // 更新任务，跳转至历史panel
        calTask.value.taskId = calcRes.data
        taskStore.setTaskStatus(calTask.value.taskId, 'PENDING')
        taskStore.setIsInitialTaskPending(false)
    }

    /**
     * 方法调用Guide
     */
    const ref1 = ref(null)
    const ref2 = ref(null)
    const ref3 = ref(null)
    const current = ref(0)
    const openTour = ref<boolean>(false)

    const steps: TourProps['steps'] = [
        {
            title: '筛选数据',
            description: '在这里筛选您将要分析的平台数据',
            cover: createVNode('img', {
                alt: 'tour.png',
                src: banner,
                style: "width: 240px; height: auto; border-radius: 8px;"
            }),
            target: () => ref1.value && (ref1.value as any).$el,
        },
        {
            title: '调用工具',
            description: '在这里调用平台的景级分析工具',
            target: () => ref2.value && (ref2.value as any).$el,
        },
        {
            title: '查看记录',
            description: '调用完成后您可以在这里查看调用记录',
            target: () => ref3.value && (ref3.value as any).$el,
        },
    ]
    const handleOpenTour = (val: boolean): void => {
        openTour.value = val
    }

    return {
        getMethLibList,
        currentPage,
        pageSize,
        total,
        methLibList,
        searchQuery,
        isMethLibExpand,
        tagList,
        getTagList,
        selectTags,
        handleInvoke,
        openModal,
        showModal,
        selectedItem,
        ref1,
        ref2,
        ref3,
        current,
        steps,
        handleOpenTour,
        openTour,
    }
}
