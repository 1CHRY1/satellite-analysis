<template>
    <div>
        <projectsBg
            class="absolute inset-0 z-0 h-full w-full overflow-hidden bg-[radial-gradient(ellipse_at_bottom,_#1b2735_0%,_#090a0f_100%)]">
        </projectsBg>
        <div class="relative z-99 flex flex-col items-center justify-center">
            <div class="my-10 flex w-[50vw] flex-col items-center justify-center">
                <img src="@/assets/image/projectsName.png" class="h-12 w-fit" alt="" />
                <div class="searchContainer mt-6 w-[100%]">
                    <div class="model_research">
                        <input type="text" autocomplete="false" placeholder="根据项目名称、创建人或项目信息进行搜索，搜索词为空则显示所有项目"
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
                    <el-button type="primary" color="#049f40"
                        style="margin-left: 10px;border-color: #049f40;font-size: 14px;height: 2rem;"
                        @click="createProjectView = !createProjectView">

                        {{ createProjectView ? '取消创建' : '创建项目' }}
                    </el-button>
                    <el-button type="primary" color="#049f40" :disabled="searchProjectsVisible" style="
                            margin-left: 10px;
                            border-color: #049f40;
                            font-size: 14px;
                            height: 2rem;
                        " @click="viewMyProjects">
                        {{ myProjectsVisible ? '所有项目' : '我的项目' }}
                    </el-button>
                </div>
            </div>

            <!-- 计算合适的高度，header是56px，搜索184px，即 50vh = 1/2x + 56px + 184px，x为容器高度，这样创建弹窗在容器内居中就会在视口居中 -->
            <div class=" flex h-[calc(100vh-184px-56px)] w-full justify-center relative">
                <!-- 计算合适的宽度 -->
                <div v-if="!createProjectView" class="p-3 grid gap-12 overflow-y-auto"
                    :style="{ gridTemplateColumns: `repeat(${columns}, ${cardWidth}px)` }">
                    <projectCard
                        v-for="item in searchProjectsVisible ? searchedProjects : myProjectsVisible ? myProjectList : projectList"
                        :key="item.projectId" :project="item" @click="enterProject(item)">
                    </projectCard>
                </div>


                <!-- 创建项目的卡片 -->
                <div v-if="createProjectView" class="flex  opacity-80 absolute top-[calc(50vh-240px-206px)]">
                    <div class="bg-gray-700 text-white p-6 rounded-xl shadow-xl w-96">
                        <h2 class="text-xl font-semibold mb-4 text-center">创建项目</h2>

                        <!-- 项目名称 -->
                        <label class="flex text-sm mb-1">
                            <div style="color: red;margin-right: 4px;">* </div> 项目名称
                        </label>
                        <input v-model="newProject.projectName" type="text"
                            class="w-full p-2 rounded bg-gray-800 text-white focus:outline-none focus:ring-2 focus:ring-green-400" />

                        <!-- 运行环境 -->
                        <label class="flex text-sm mt-3 mb-1">
                            <div style="color: red;margin-right: 4px;">* </div>运行环境
                        </label>
                        <select v-model="newProject.environment"
                            class="w-full p-2 rounded bg-gray-800 text-white focus:outline-none focus:ring-2 focus:ring-green-400">
                            <option value="Python2_7" disabled>Python 2.7 ( 暂不支持 )</option>
                            <option value="Python3_9">Python 3.9</option>
                            <option value="Python3_10" disabled>Python 3.10 ( 暂不支持 ) </option>
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
                        <label class="flex text-sm mt-3 mb-1">
                            <div style="color: red;margin-right: 4px;">* </div> 描述
                        </label>
                        <textarea v-model="newProject.description"
                            class="w-full p-2 rounded bg-gray-800 text-white h-20 focus:outline-none focus:ring-2 focus:ring-green-400"></textarea>

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
                        <div class="flex justify-center mt-6">
                            <button @click="create"
                                class="px-6 py-2 bg-green-600 rounded-xl hover:bg-green-500 mx-2">创建</button>
                            <button @click="createProjectView = false"
                                class="px-6 py-2 !ml-4 bg-gray-500 rounded-xl hover:bg-gray-400 mx-2">取消</button>
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
import { getProjects, createProject, getModels, getMethods } from '@/api/http/analysis'
import projectCard from '@/components/projects/projectCard.vue'

import type { project, newProject } from "@/type/analysis"
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router';



const router = useRouter();
const userId = localStorage.getItem('userId')
const searchInput = ref('')

/**
 * 搜索功能模块
 */
const myProjectsVisible = ref(false)
const myProjectList: Ref<project[]> = ref([])
const searchedProjects: Ref<project[]> = ref([])
const searchProjectsVisible = ref(false)

// 支持搜索项目名称、创建人和项目描述
const researchProjects = () => {
    if (searchInput.value.length > 0) {
        searchedProjects.value = projectList.value.filter((item: project) => item.projectName.includes(searchInput.value) || item.createUserName.includes(searchInput.value) || item.description.includes(searchInput.value))
        searchProjectsVisible.value = true
        console.log(searchedProjects.value, 1115);
    } else {
        searchProjectsVisible.value = false
    }

}

// 查看我的项目
const viewMyProjects = () => {
    myProjectsVisible.value = !myProjectsVisible.value
    myProjectList.value = projectList.value.filter((item: project) => item.createUser === userId)
}


/**
 * 项目列表模块
 */
const projectList: Ref<project[]> = ref([])
const createProjectView = ref(false)
const newProject = ref({
    projectName: '',
    environment: 'Python3_9',
    // keywords: [],
    description: '',
    // authority: ''
})
const cardWidth = 300;
const gap = 48;
const columns = ref(1);

const updateColumns = () => {
    const containerWidth = window.innerWidth - 32; // 预留 100px 余量
    columns.value = Math.max(1, Math.floor(containerWidth / (cardWidth + gap)));
};

const enterProject = (item: project) => {

    if (item.createUser === userId) {
        router.push(`/project/${item.projectId}`);
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
    } else if (projectList.value.some((item: project) => item.projectName === newProject.value.projectName)) {
        ElMessage.error('该项目已存在，请更换项目名称')
        return

    }
    let createBody = {
        ...newProject.value,
        userId,
    }
    // 先关闭再发起请求，起到防抖作用
    createProjectView.value = false
    await createProject(createBody)
    projectList.value = await getProjects()
    ElMessage.success('创建成功')
}

const getProjectsInOrder = async () => {
    projectList.value = await getProjects()
    projectList.value.sort((a, b) => b.createTime.localeCompare(a.createTime));
}

onMounted(async () => {
    await getProjectsInOrder()

    console.log("所有项目：", projectList.value)
    updateColumns();
    window.addEventListener("resize", updateColumns);
    console.log(await getModels({
        "asc": false,
        "page": 1,
        "pageSize": 18,
        "searchText": "",
        "sortField": "createTime",
        "tagClass": "problemTags",
        "tagNames": [
            ""
        ]
    }), 12132);
    console.log(await getMethods({
        "asc": false,
        "page": 1,
        "pageSize": 18,
        "searchText": "",
        "sortField": "createTime",
        "tagClass": "problemTags",
        "tagNames": [
            ""
        ]
    }), 13132);

})

onUnmounted(() => {
    window.removeEventListener("resize", updateColumns);
});
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
