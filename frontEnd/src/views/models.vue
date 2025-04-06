<template>
    <div>
        <projectsBg
            class="absolute inset-0 z-0 h-full w-full overflow-hidden bg-[radial-gradient(ellipse_at_bottom,_#1b2735_0%,_#090a0f_100%)]"
        >
        </projectsBg>
        <el-row class="relative z-10 flex h-[calc(100vh-56px)] text-white" style="flex: none">
            <el-col :span="5" class="h-full">
                <div class="cardShadow border-box mx-[3vw] my-[3vh] p-6 opacity-80">
                    <h2 class="text-lg font-bold">Model Center</h2>
                    <ul class="mt-4 space-y-2">
                        <li
                            v-for="category in categories"
                            :key="category"
                            class="cursor-pointer hover:text-blue-400"
                        >
                            {{ category }}
                        </li>
                    </ul>
                </div>
            </el-col>
            <el-col :span="18" class="h-full">
                <main class="border-box mx-[2vw] mt-[3vh] h-[calc(100%-3vh)] px-6">
                    <el-row class="mb-[20px] h-[60px]">
                        <!-- 搜索栏 -->
                        <div class="cardShadow mb-6 flex w-full items-center space-x-2">
                            <input
                                v-model="searchQuery"
                                type="text"
                                placeholder="Search models..."
                                class="w-[100%] rounded bg-white p-2 text-white focus:ring-2 focus:ring-blue-400 focus:outline-none"
                            />
                            <button
                                class="w-20 cursor-pointer rounded bg-blue-600 px-4 py-2 hover:bg-blue-500"
                                @click=""
                            >
                                搜索
                            </button>
                        </div>
                    </el-row>
                    <el-row :gutter="20" class="h-[calc(100%-60px-20px-60px)] overflow-auto">
                        <el-col
                            v-for="model in filteredModels"
                            :key="model.createTime + model.name"
                            :span="8"
                            class="h-[23%] max-h-[200px] w-full"
                        >
                            <!-- 模型卡片 -->
                            <div
                                class="relative box-border h-full w-full cursor-pointer rounded-lg border border-solid border-[#fff9] bg-black p-4 opacity-80 shadow-lg"
                            >
                                <!-- <div class="text-lg font-bold flex items-center">
                                    <Package :size="50" class="text-blue-600 mr-1" />{{ model.name }}Create Time:{{
                                        model.createTime }}
                                </div> -->
                                <div class="flex w-full items-center">
                                    <!-- Package 图标 -->
                                    <div class="flex-shrink-0">
                                        <Package :size="50" class="mr-1 text-blue-600" />
                                    </div>

                                    <!-- 文本内容 -->
                                    <div class="ml-2 flex w-[calc(100%-56px)] flex-col">
                                        <!-- model.name -->
                                        <div
                                            class="w-full overflow-hidden text-lg font-bold text-ellipsis whitespace-nowrap"
                                        >
                                            {{ model.name }}
                                        </div>

                                        <!-- model.createTime -->
                                        <span
                                            class="w-full overflow-hidden text-sm text-ellipsis whitespace-nowrap text-gray-400"
                                            >Create Time: {{ model.createTime }}</span
                                        >
                                    </div>
                                </div>
                                <div
                                    class="mt-1.5 h-[calc(100%-50px-14px-16px)] overflow-auto text-sm text-gray-400"
                                >
                                    {{ model.description }}
                                </div>
                                <div
                                    class="absolute bottom-2 left-2 flex items-center px-2 py-2 text-[12px] text-white"
                                >
                                    <Mail :size="16" class="mr-1 text-green-600" />{{
                                        model.author
                                    }}
                                </div>
                                <div
                                    class="absolute right-2 bottom-2 flex items-center rounded px-4 py-2 text-[12px] font-bold hover:text-blue-500"
                                >
                                    Add to project
                                    <FilePlus2 :size="16" class="ml-1" />
                                </div>
                            </div>
                        </el-col>
                    </el-row>
                    <div class="flex h-[60px] justify-around">
                        <el-pagination
                            background
                            layout="prev, pager, next"
                            v-model="currentPage"
                            :total="5189"
                            :page-size="15"
                            @current-change="pageChange"
                            @next-click=""
                            @prev-click=""
                        >
                        </el-pagination>
                    </div>
                </main>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, type Ref, watch } from 'vue'
import projectsBg from '@/components/projects/projectsBg.vue'
import { getModels, getMethods } from '@/api/http/analysis'
import { FilePlus2, Mail, Package } from 'lucide-vue-next'
import type { modelsOrMethods } from '@/type/modelCentral'

const categories = ref<string[]>([
    'Natural Perspective',
    'Human Perspective',
    'Global Scale',
    'Regional Scale',
    'Data Analysis',
    'Machine Learning',
])

const searchQuery = ref<string>('')
const models: Ref<modelsOrMethods[]> = ref([])
const currentPage: Ref<number> = ref(1)

const columns = ref<number>(3)

const updateColumns = () => {
    const containerWidth = window.innerWidth - 80
    columns.value = Math.max(1, Math.floor(containerWidth / 350))
}

const pageChange = async (page: number) => {
    models.value = (
        await getModels({
            asc: false,
            page: page,
            pageSize: 12,
            searchText: '',
            sortField: 'createTime',
            tagClass: 'problemTags',
            tagNames: [''],
        })
    ).data
}

onMounted(async () => {
    updateColumns()
    window.addEventListener('resize', updateColumns)
    models.value = (
        await getModels({
            asc: false,
            page: 1,
            pageSize: 12,
            searchText: '',
            sortField: 'createTime',
            tagClass: 'problemTags',
            tagNames: [''],
        })
    ).data
    console.log(models.value, 'models.value')
})

const filteredModels = computed(() => {
    return models.value.filter((m) =>
        m.name.toLowerCase().includes(searchQuery.value.toLowerCase()),
    )
})
</script>

<style scoped lang="scss">
::-webkit-scrollbar {
    width: 8px;
}

::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.2);
    border-radius: 4px;
}

.cardShadow {
    box-shadow: 2px 2px 6px #fff8;
    border-radius: 5px;
}

:deep(.el-pagination ul li) {
    padding: 0 4px !important;
}
</style>
