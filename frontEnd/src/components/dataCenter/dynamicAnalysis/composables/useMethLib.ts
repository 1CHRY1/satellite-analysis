import { getAllTag, getMethLibPage, invokeMethod } from '@/api/http/analytics-display/methlib.api'
import type { MethLib } from '@/api/http/analytics-display/methlib.type'
import { computed, reactive, ref } from 'vue'

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
    const selectTags = reactive<Boolean[]>([]);
    const selectTagIds = computed(() => {
        return tagList.value.filter((_, index) => selectTags[index]).map(tag => tag.id)
    })
    const getTagList = async () => {
        const res = await getAllTag()
        // selectTags = Array(res.data.length).fill(false) // 相当于直接整个替换响应式变量
        selectTags.length = 0
        selectTags.push(...Array(res.data.length).fill(false))
        if (selectTags.length > 0)
            selectTags[0] = true
        tagList.value = res.data
    }

    const handleInvoke = async (params: any) => {
        for (let [key, val] of Object.entries(params.formData)) {
            if (params.formData[key].value !== undefined) {
                params.formData[key] = params.formData[key].value
            }
        }
        console.log(params.formData)
        await invokeMethod(params.method.id, params.formData)
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
    }
}
