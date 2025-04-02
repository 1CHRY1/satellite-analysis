<template>
    <div>
        <projectsBg class="absolute inset-0 z-0 h-full w-full overflow-hidden bg-[radial-gradient(ellipse_at_bottom,_#1b2735_0%,_#090a0f_100%)]">
        </projectsBg>
        <div class="relative z-99 flex flex-col items-center justify-center">
            <div class="my-10 flex w-[50vw] flex-col items-center justify-center">
                <img src="@/assets/image/projectsName.png" class="h-12 w-fit" alt="" />
                <div class="searchContainer mt-6 w-[100%]">
                    <div class="model_research">
                        <input
                            type="text"
                            autocomplete="false"
                            placeholder="输入项目名称搜索项目"
                            class="model_research_input"
                            v-model="searchInput"
                            style="font-size: 14px"
                            @keyup.enter=""
                        />

                        <el-button
                            type="primary"
                            color="#049f40"
                            style="
                                border-left: none;
                                border-top-left-radius: 0px;
                                border-bottom-left-radius: 0px;
                                border-color: #ffffff;
                                font-size: 14px;
                                height: 2rem;
                            "
                            @click=""
                        >
                            <Search class="h-4" />

                            搜索
                        </el-button>
                    </div>
                    <el-button
                        type="primary"
                        color="#049f40"
                        style="margin-left: 10px; border-color: #049f40; font-size: 14px; height: 2rem"
                        @click="createProjectView = true"
                    >
                        创建
                    </el-button>
                    <el-button
                        type="primary"
                        color="#049f40"
                        style="margin-left: 10px; border-color: #049f40; font-size: 14px; height: 2rem"
                        @click=""
                    >
                        我的
                    </el-button>
                </div>
            </div>

            <div class="my-10 h-[calc(100vh-264px-56px)] w-full justify-center">
                <div class="inline-flex flex-wrap gap-12">
                    <projectCard v-if="!createProjectView" v-for="item in projectList" :project="item" @click="enterProject(item)"></projectCard>
                </div>

                <!-- 创建项目的卡片 -->
                <div v-if="createProjectView" class="fixed flex items-center justify-center opacity-80">
                    <div class="w-96 rounded-xl bg-gray-600 p-6 text-white shadow-xl">
                        <h2 class="mb-4 text-center text-xl font-semibold">创建项目</h2>

                        <!-- 项目名称 -->
                        <label class="mb-1 flex text-sm">
                            <div style="color: red; margin-right: 4px">*</div>
                            项目名称
                        </label>
                        <input
                            v-model="newProject.projectName"
                            type="text"
                            class="w-full rounded bg-gray-800 p-2 text-white focus:ring-2 focus:ring-green-400 focus:outline-none"
                        />

                        <!-- 运行环境 -->
                        <label class="mt-3 mb-1 flex text-sm">
                            <div style="color: red; margin-right: 4px">*</div>
                            运行环境
                        </label>
                        <select
                            v-model="newProject.environment"
                            class="w-full rounded bg-gray-800 p-2 text-white focus:ring-2 focus:ring-green-400 focus:outline-none"
                        >
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
                        <textarea
                            v-model="newProject.description"
                            class="h-20 w-full rounded bg-gray-800 p-2 text-white focus:ring-2 focus:ring-green-400 focus:outline-none"
                        ></textarea>

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
                            <button @click="create" class="mx-2 rounded-xl bg-green-600 px-6 py-2 hover:bg-green-500">创建</button>
                            <button @click="createProjectView = false" class="mx-2 !ml-4 rounded-xl bg-gray-500 px-6 py-2 hover:bg-gray-400">
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
import { ref, onMounted } from 'vue'
import projectsBg from '@/components/projects/projectsBg.vue'
import { Search } from 'lucide-vue-next'
import { getProjects, createProject } from '@/api/http/analysis'
import projectCard from '@/components/projects/projectCard.vue'

import type { project, newProject } from '@/type/analysis'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const userId = localStorage.getItem('userId')
const searchInput = ref('')
const projectList = ref([])
const createProjectView = ref(false)
const newProject = ref({
    projectName: '',
    environment: 'Python3_9',
    // keywords: [],
    description: '',
    // authority: ''
})

const enterProject = (item: project) => {
    console.log(item)
    if (item.createUser === userId) {
        router.push(`/project/${item.projectId}`)
    } else {
        ElMessage.error('您没有访问该项目的权限，请联系项目创建者')
    }
}

const create = async () => {
    if (!newProject.value.projectName) {
        ElMessage.error('项目名称不能为空')
        return
    } else if (!newProject.value.description) {
        ElMessage.error('描述不能为空')
        return
    }
    let createBody = {
        ...newProject.value,
        userId,
    }
    console.log(createBody, 11)

    await createProject(createBody)
    createProjectView.value = false
    projectList.value = await getProjects()
    ElMessage.success('创建成功')
}

onMounted(async () => {
    projectList.value = await getProjects()
    console.log(projectList.value)
})
</script>

<style scoped lang="scss">
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
    -webkit-appearance: none;
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
