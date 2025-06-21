<template>
    <div>
        <projectsBg
            class="absolute inset-0 z-0 h-full w-full overflow-hidden bg-[radial-gradient(ellipse_at_bottom,_#1b2735_0%,_#090a0f_100%)]">
        </projectsBg>
        <div class="relative z-99 flex flex-col items-center justify-center">
            <div class="my-10 flex w-[50vw] flex-col items-center justify-center">
                <img src="@/assets/image/toolsEstablish.png" class="h-12 w-fit" alt="" />
                <div class="searchContainer mt-6 w-[100%]">
                    <div class="model_research">
                        <input type="text" autocomplete="false" placeholder="根据工具名称、创建人或工具信息进行搜索，搜索词为空则显示所有工具"
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
                            搜索
                        </el-button>
                    </div>
                    <el-button type="primary" color="#049f40" style="
                            margin-left: 10px;
                            border-color: #049f40;
                            font-size: 14px;
                            height: 2rem;
                        " @click="createProjectView = !createProjectView">
                        {{ createProjectView ? '取消创建' : '创建工具' }}
                    </el-button>
                    <el-button type="primary" color="#049f40" :disabled="searchProjectsVisible" style="
                            margin-left: 10px;
                            border-color: #049f40;
                            font-size: 14px;
                            height: 2rem;
                        " @click="viewMyProjects">
                        {{ myProjectsVisible ? '所有工具' : '我的工具' }}
                    </el-button>
                </div>
            </div>

            <!-- 计算合适的高度，header是56px，搜索184px，即 50vh = 1/2x + 56px + 184px，x为容器高度，这样创建弹窗在容器内居中就会在视口居中 -->
            <div class="relative flex h-[calc(100vh-184px-56px)] w-full justify-center">
                <!-- 计算合适的宽度 -->
                <div v-if="!createProjectView" class="grid gap-12 overflow-y-auto p-3"
                    :style="{ gridTemplateColumns: `repeat(${columns}, ${cardWidth}px)` }">
                    <projectCard v-for="item in searchProjectsVisible
                        ? searchedProjects
                        : myProjectsVisible
                            ? myProjectList
                            : projectList" :key="item.projectId" :project="item" @click="enterProject(item)"
                        @deleteProject=afterDeleteProject>
                    </projectCard>
                </div>

                <!-- 创建工具的卡片 -->
                <div v-if="createProjectView" class="absolute top-[calc(50vh-240px-206px)] flex opacity-80">
                    <div class="w-96 rounded-xl bg-gray-700 p-6 text-white shadow-xl">
                        <h2 class="mb-4 text-center text-xl font-semibold">创建工具</h2>

                        <!-- 工具名称 -->
                        <label class="mb-1 flex text-sm">
                            <div style="color: red; margin-right: 4px">*</div>
                            工具名称
                        </label>
                        <input v-model="newProject.projectName" type="text"
                            class="w-full rounded bg-gray-800 p-2 text-white focus:ring-2 focus:ring-green-400 focus:outline-none" />

                        <!-- 运行环境 -->
                        <label class="mt-3 mb-1 flex text-sm">
                            <div style="color: red; margin-right: 4px">*</div>
                            运行环境
                        </label>
                        <select v-model="newProject.environment"
                            class="w-full rounded bg-gray-800 p-2 text-white focus:ring-2 focus:ring-green-400 focus:outline-none">
                            <option value="Python2_7" disabled>Python 2.7 ( 暂不支持 )</option>
                            <option value="Python3_9">Python 3.9</option>
                            <option value="Python3_10" disabled>Python 3.10 ( 暂不支持 )</option>
                            <option value="Python3_12">Python 3.12</option>
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
                                    <Loading class="h-4 w-4 mr-1" />创建
                                </div>
                                <div v-else>创建</div>
                            </button>
                            <button @click="createProjectView = false"
                                class="mx-2 !ml-4 rounded-xl bg-gray-500 px-6 py-2 hover:bg-gray-400 cursor-pointer">
                                取消
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, type Ref } from 'vue'
import projectsBg from '@/components/projects/projectsBg.vue'
import { Search } from 'lucide-vue-next'
import { getProjects, createProject } from '@/api/http/analysis'
import projectCard from '@/components/projects/projectCard.vue'
import { Loading } from "@element-plus/icons-vue"
import type { project, newProject } from '@/type/analysis'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

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
const researchProjects = () => {
    if (searchInput.value.length > 0) {
        searchedProjects.value = projectList.value.filter(
            (item: project) =>
                item.projectName.includes(searchInput.value) ||
                item.createUserName.includes(searchInput.value) ||
                item.description.includes(searchInput.value),
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
    myProjectList.value = projectList.value.filter((item: project) => item.createUser === userId)
}

/**
 * 工具列表模块
 */
const projectList: Ref<project[]> = ref([])
const createProjectView = ref(false)
const createLoading = ref(false)
const newProject = ref({
    projectName: '',
    environment: 'Python3_12',
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
        ElMessage.error('您没有访问该工具的权限，请联系工具创建者')
    }
}

const create = async () => {
    if (!newProject.value.projectName) {
        ElMessage.error('工具名称不能为空')
        return
    } else if (!newProject.value.description) {
        ElMessage.error('描述不能为空')
        return
    } else if (
        projectList.value.some((item: project) => item.projectName === newProject.value.projectName)
    ) {
        ElMessage.error('该工具已存在，请更换工具名称')
        return
    }
    createLoading.value = true
    let createBody = {
        ...newProject.value,
        userId,
    }
    let createRes = await createProject(createBody)
    router.push(`/project/${createRes.projectId}`)
    ElMessage.success('创建成功')
    createLoading.value = false
    createProjectView.value = false
}

const afterDeleteProject = async () => {
    await getProjectsInOrder()
}

const getProjectsInOrder = async () => {
    projectList.value = await getProjects()
    projectList.value.sort((a, b) => b.createTime.localeCompare(a.createTime))
}

onMounted(async () => {
    await getProjectsInOrder()

    console.log("所有工具：", projectList.value)
    updateColumns();
    window.addEventListener("resize", updateColumns);


})

onUnmounted(() => {
    window.removeEventListener('resize', updateColumns)
})
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
