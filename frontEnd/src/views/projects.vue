<template>
    <div>
        <projectsBg
            class="absolute inset-0 z-0 h-full w-full overflow-hidden bg-[radial-gradient(ellipse_at_bottom,_#1b2735_0%,_#090a0f_100%)]"
        >
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
                        style="
                            margin-left: 10px;
                            border-color: #049f40;
                            font-size: 14px;
                            height: 2rem;
                        "
                        @click=""
                    >
                        创建
                    </el-button>
                    <el-button
                        type="primary"
                        color="#049f40"
                        style="
                            margin-left: 10px;
                            border-color: #049f40;
                            font-size: 14px;
                            height: 2rem;
                        "
                        @click=""
                    >
                        我的
                    </el-button>
                </div>
            </div>

            <div class="my-10 flex h-[calc(100vh-264px-56px)] w-full justify-around">
                <projectCard
                    v-for="item in projectList"
                    :project="item"
                    @click="enterProject(item)"
                ></projectCard>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import projectsBg from '@/components/projects/projectsBg.vue'
import { Search } from 'lucide-vue-next'
import { getProjects } from '@/api/http/analysis'
import projectCard from '@/components/projects/projectCard.vue'
import type { project } from '@/type/analysis'

const searchInput = ref('')

const projectList = ref([])

const enterProject = (item: project) => {
    console.log(item)
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
