<template>
    <div>
        <projectsBg
            class="absolute inset-0 z-0 h-full w-full overflow-hidden bg-[radial-gradient(ellipse_at_bottom,_#1b2735_0%,_#090a0f_100%)]">
        </projectsBg>
        <div class="relative z-10 flex flex-col items-center justify-center">
            <div class="my-10 flex w-[50vw] flex-col items-center justify-center">
                <svg class="h-12 w-fit" viewBox="0 0 420 96" xmlns="http://www.w3.org/2000/svg">
                    <text class="page-heading-script" x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" style="font-size: 80px;">
                        在线编程
                    </text>
                </svg>
                <div class="searchContainer mt-6 w-[100%]">
                    <div class="model_research">
                         <input type="text" autocomplete="false" :placeholder="t('projectpage.searchbar')"
                            class="model_research_input" v-model="searchInput" style="font-size: 14px"
                            @keyup.enter="researchProjects" />

                        <el-button type="primary" color="#049f40" style="
                                border-left: none;
                                border-top-left-radius: 0px;
                                border-bottom-left-radius: 0px;
                                border-color: #ffffff;
                                font-size: 14px;
                                height: 2rem;
                            " @click="researchProjects">
                            <Search class="h-4" />
                            {{t("projectpage.search")}}
                        </el-button>
                    </div>
                    <el-button type="primary" color="#049f40" style="
                            margin-left: 10px;
                            border-color: #049f40;
                            font-size: 14px;
                            height: 2rem;
                        " @click="createProjectView = !createProjectView">
                        {{ createProjectView ? t('projectpage.canceltool') : t('projectpage.createtool')}}
                    </el-button>
                    <el-button type="primary" color="#049f40" :disabled="searchProjectsVisible" style="
                            margin-left: 10px;
                            border-color: #049f40;
                            font-size: 14px;
                            height: 2rem;
                        " @click="viewMyProjects">
                        {{ myProjectsVisible ? t('projectpage.alltool') : t('projectpage.mytool') }}
                    </el-button>
                </div>
            </div>

            <!-- 计算合适的高度，header是56px，搜索184px，即 50vh = 1/2x + 56px + 184px，x为容器高度，这样创建弹窗在容器内居中就会在视口居中 -->
            <div class="relative flex h-[calc(100vh-184px-56px)] w-full justify-center">
                <!-- 计算合适的宽度 -->
                <div v-if="!createProjectView" class="grid gap-12 overflow-y-auto p-3"
                    :style="{ gridTemplateColumns: `repeat(${columns}, ${cardWidth}px)` }">
                    <projectCard v-for="item in (searchProjectsVisible
                        ? searchedProjects
                        : (myProjectsVisible
                            ? myProjectList
                            : projectList.filter((i:any)=> Number((i as any).isTool ?? 0) !== 1)))"
                        :key="item.projectId" :project="item" :is-service="isService(item)"
                        @click="enterProject(item)" @deleteProject=afterDeleteProject>
                    </projectCard>
                </div>

                <!-- 创建工具的卡片 -->
                <div v-if="createProjectView" class="absolute top-[calc(50vh-240px-206px)] flex opacity-80">
                    <div class="w-96 rounded-xl bg-gray-700 p-6 text-white shadow-xl">
                        <h2 class="mb-4 text-center text-xl font-semibold">{{ t("projectpage.createtool") }}</h2>


                        <!-- 工具名称 -->
                        <label class="mb-1 flex text-sm">
                            <div style="color: red; margin-right: 4px">*</div>
                            {{t("projectpage.toolname")}}
                        </label>
                        <input v-model="newProject.projectName" type="text"
                            class="w-full rounded bg-gray-800 p-2 text-white focus:ring-2 focus:ring-green-400 focus:outline-none" />

                        <!-- 运行环境 -->
                        <label class="mt-3 mb-1 flex text-sm">
                            <div style="color: red; margin-right: 4px">*</div>
                            {{t("projectpage.envir")}}
                        </label>
                        <select v-model="newProject.environment"
                            class="w-full rounded bg-gray-800 p-2 text-white focus:ring-2 focus:ring-green-400 focus:outline-none">
                            <option value="Python2_7" disabled>Python 2.7 ( 暂不支持 )</option>
                            <option value="Python3_9">Python 3.9</option>
                            <option value="Python3_10" disabled>Python 3.10 ( 暂不支持 )</option>
                        </select>

                        <!-- 关键词 -->
                        <!-- <label class="block text-sm mt-3 mb-1">* 关键词</label>
                        <div class="flex">
                            <input v-model="newProject.keywords" type="text"
                                class="w-full p-2 rounded bg-gray-800 text-white focus:outline-none focus:ring-2 focus:ring-green-400" />
                            <button @click="addKeyword"
                                class="ml-2 px-4 py-2 bg-green-600 rounded hover:bg-green-500">add</button>
                        </div>
                        <div class="mt-2 flex flex-wrap">
                            <span v-for="(kw, index) in newProject.keywords" :key="index"
                                class="bg-green-700 text-white px-2 py-1 rounded-full text-xs mr-2 mb-2">
                                {{ kw }} ✕
                            </span>
                        </div> -->

                        <!-- 描述 -->
                        <label class="mt-3 mb-1 flex text-sm">
                            <div style="color: red; margin-right: 4px">*</div>
                            描述
                        </label>
                        <textarea v-model="newProject.description"
                            class="h-20 w-full rounded bg-gray-800 p-2 text-white focus:ring-2 focus:ring-green-400 focus:outline-none"></textarea>

                        <!-- 权限 -->
                        <!-- <label class="block text-sm mt-3 mb-1">* 权限</label>
                        <div class="flex items-center space-x-4">
                            <label class="flex items-center">
                                <input type="radio" v-model="newProject.authority" value="public" class="mr-1" /> Public
                            </label>
                            <label class="flex items-center">
                                <input type="radio" v-model="newProject.authority" value="private" class="mr-1" />
                                Private
                            </label>
                        </div> -->

                        <!-- 按钮 -->
                        <div class="mt-6 flex justify-center">
                            <button @click="create" class="mx-2 rounded-xl bg-green-600 px-6 py-2 hover:bg-green-500"
                                :class="{ 'cursor-pointer': !createLoading }" :disabled="createLoading">
                                <div v-if="createLoading" class="is-loading flex items-center">
                                    <Loading class="h-4 w-4 mr-1" />{{t("projectpage.button.create")}}
                                </div>
                                <div v-else>{{t("projectpage.button.create")}}</div>
                            </button>
                            <button @click="createProjectView = false"
                                class="mx-2 !ml-4 rounded-xl bg-gray-500 px-6 py-2 hover:bg-gray-400 cursor-pointer">
                                {{t("projectpage.button.cancel")}}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, onActivated, type Ref } from 'vue'
import projectsBg from '@/components/projects/projectsBg.vue'
import { Search } from 'lucide-vue-next'
import { getProjects, createProject } from '@/api/http/analysis'
import { getAllTools, getToolStatus } from '@/api/http/tool'
import projectCard from '@/components/projects/projectCard.vue'
import { Loading } from "@element-plus/icons-vue"
import type { project, newProject } from '@/type/analysis'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store'
import { useI18n } from 'vue-i18n'
import { updateRecord } from '@/api/http/user'

const { t } = useI18n()
const userStore = useUserStore()
const router = useRouter()
const userId = localStorage.getItem('userId')
const searchInput = ref('')

/**
 * 搜索功能模块
 */
const myProjectsVisible = ref(false)
const myProjectList: Ref<project[]> = ref([])
const searchedProjects: Ref<project[]> = ref([])
const searchProjectsVisible = ref(false)

// 支持搜索工具名称、创建人和工具描述
const isToolItem = (item: project) => Number((item as any).isTool ?? 0) === 1

const researchProjects = () => {
    if (searchInput.value.length > 0) {
        searchedProjects.value = projectList.value.filter(
            (item: project) =>
                (item.projectName.includes(searchInput.value) ||
                item.createUserName.includes(searchInput.value) ||
                item.description.includes(searchInput.value)) &&
                !isToolItem(item),
        )
        searchProjectsVisible.value = true
        console.log(searchedProjects.value, 1115)
    } else {
        searchProjectsVisible.value = false
    }
}

// 查看我的工具
const viewMyProjects = () => {
    myProjectsVisible.value = !myProjectsVisible.value
    myProjectList.value = projectList.value.filter((item: project) => item.createUser === userId && !isToolItem(item))
}

/**
 * 工具列表模块
 */
const projectList: Ref<project[]> = ref([])
const serviceMap = ref<Record<string, boolean>>({})
const toolIdMap = ref<Record<string, string>>({})
const createProjectView = ref(false)
const createLoading = ref(false)
const newProject = ref({
    projectName: '',
    environment: 'Python3_9',
    // keywords: [],
    description: '',
    // authority: ''
})
const cardWidth = 300
const gap = 48
const columns = ref(1)

const updateColumns = () => {
    const containerWidth = window.innerWidth - 32 // 预留 100px 余量
    columns.value = Math.max(1, Math.floor(containerWidth / (cardWidth + gap)))
}

const enterProject = (item: project) => {
    if (item.createUser === userId) {
        router.push(`/project/${item.projectId}`)
    } else {
        ElMessage.error(t("projectpage.message.error.entererr"))
    }
}

const create = async () => {
    if (!newProject.value.projectName) {
        ElMessage.error(t("projectpage.message.error.create_name_empty"))
        return
    } else if (!newProject.value.description) {
        ElMessage.error(t("projectpage.message.error.create_description_empty"))
        return
    } else if (
        projectList.value.some((item: project) => item.projectName === newProject.value.projectName)
    ) {
        ElMessage.error(t("projectpage.message.error.create_name_exist"))
        return
    }
    createLoading.value = true
    let createBody = {
        ...newProject.value,
        userId,
    }
    let createRes = await createProject(createBody)
    router.push(`/project/${createRes.projectId}`)
    ElMessage.success(t("projectpage.message.success"))
    try{
    action.value = '创建'
    uploadRecord(action)}
    catch(error){
        console.error('upload 报错:', error);
    }
    createLoading.value = false
    createProjectView.value = false
}

const afterDeleteProject = async () => {
    await getProjectsInOrder()
}

const getProjectsInOrder = async () => {
    projectList.value = await getProjects()
    projectList.value.sort((a, b) => b.createTime.localeCompare(a.createTime))
    await ensureServiceFlags()
}

const clearProjectsRefreshFlag = () => {
    try {
        localStorage.removeItem('projectsNeedRefresh')
    } catch (error) {
        console.warn('清理项目刷新标记失败', error)
    }
}

const refreshProjectsIfNeeded = async () => {
    try {
        if (localStorage.getItem('projectsNeedRefresh')) {
            await getProjectsInOrder()
            clearProjectsRefreshFlag()
        }
    } catch (error) {
        console.error('刷新项目列表失败', error)
    }
}

const action = ref()

//上传记录
const uploadRecord = async(typeParam = action) =>{
    let param = {
        userId : userStore.user.id,
        actionDetail:{
            projectName:newProject.value.projectName,
            projectType:"Project",
            description: newProject.value.description
        },
        actionType:typeParam.value,
    }

    let res = await updateRecord(param)
    console.log(res, "记录")
}

onMounted(async () => {
    await getProjectsInOrder()
    clearProjectsRefreshFlag()

    console.log("所有工具：", projectList.value)
    updateColumns();
    window.addEventListener("resize", updateColumns);


})

onActivated(async () => {
    await refreshProjectsIfNeeded()
})

onUnmounted(() => {
    window.removeEventListener('resize', updateColumns)
})

// 计算是否为服务卡：后台标志优先，其次兜底检测
const isService = (item: project) => {
    return Number((item as any).isTool ?? 0) === 1 || serviceMap.value[item.projectId] === true
}

const ensureServiceFlags = async () => {
    try {
        const userIdLocal = userId || localStorage.getItem('userId')
        if (!userIdLocal) return
        // 构建 projectId -> toolId 映射
        toolIdMap.value = {}
        try {
            const toolsRes = await getAllTools({ current: 1, size: 200, userId: userIdLocal })
            const records = toolsRes?.data?.records || []
            for (const rec of records) {
                if (rec?.projectId && rec?.toolId) toolIdMap.value[rec.projectId] = rec.toolId
            }
        } catch {}
        const promises: Promise<void>[] = []
        for (const item of projectList.value) {
            // 已标服务的卡片不重复探测
            if (Number((item as any).isTool ?? 0) === 1) continue
            if (serviceMap.value[item.projectId] === true) continue
            if (!toolIdMap.value[item.projectId]) continue
            promises.push(checkAndSetService(item.projectId, userIdLocal))
        }
        // 控制并发（简单分批）
        const batchSize = 6
        for (let i = 0; i < promises.length; i += batchSize) {
            await Promise.all(promises.slice(i, i + batchSize))
        }
    } catch (e) {
        console.warn('ensureServiceFlags error', e)
    }
}

const checkAndSetService = async (pid: string, uid: string | null) => {
    try {
        const toolId = uid ? toolIdMap.value[pid] : undefined
        if (!toolId) return
        const res = await getToolStatus({ userId: uid as string, toolId })
        if (res?.status === 1 && res.data?.status === 'running') {
            serviceMap.value[pid] = true
        }
    } catch (e) {
        // 静默失败即可
    }
}
</script>

<style scoped lang="scss">
div::-webkit-scrollbar {
    width: none !important;
}

div {
    scrollbar-width: none !important;
    scrollbar-color: rgba(37, 190, 255, 0.332) transparent !important;
}

.searchContainer {
    // height: 60px;
    // height: fit-content;
    display: flex;
    align-items: center;
    justify-content: center;
}

.model_research {
    position: relative;
    font-size: 14px;
    display: inline-flex;
    width: 100%;
}

.model_research_input {
    // -webkit-appearance: none;
    background-color: rgba(0, 0, 0, 0.6);
    background-image: none;
    border-radius: 4px 0px 0px 4px;
    border: 1px solid #ffffff;
    border-right: none;

    box-sizing: border-box;
    color: #ffffff;
    display: inline-block;
    font-size: inherit;
    height: 2rem;
    line-height: 2rem;
    outline: 0;
    padding: 0 15px;
    transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
    width: 100%;
}

.model_research_input::-webkit-input-placeholder {
    color: #c3c5ca;
}
</style>
