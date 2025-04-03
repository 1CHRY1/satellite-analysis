<template>
    <div>
        <projectsBg
            class="absolute inset-0 z-0 h-full w-full overflow-hidden bg-[radial-gradient(ellipse_at_bottom,_#1b2735_0%,_#090a0f_100%)]">
        </projectsBg>
        <el-row class="flex  h-[calc(100vh-56px)]  text-white z-10 relative " style="flex:none">

            <el-col :span="6" class="h-full">
                <div class="cardShadow  border-box mx-[2vw] my-[3vh] opacity-80">
                    <h2 class="text-lg font-bold">Model Center</h2>
                    <ul class="mt-4 space-y-2">
                        <li v-for="category in categories" :key="category" class="cursor-pointer hover:text-blue-400">
                            {{ category }}
                        </li>
                    </ul>
                </div>
            </el-col>
            <el-col :span="18" class="h-full">
                <main class="mx-[2vw] mt-[3vh] px-6 h-[calc(100%-3vh)] border-box">
                    <el-row class="h-[60px] mb-[20px]">
                        <!-- 搜索栏 -->
                        <div class="cardShadow flex items-center space-x-2 mb-6 w-full ">
                            <input v-model="searchQuery" type="text" placeholder="Search models..."
                                class="w-full p-2 bg-white rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-400">
                            <button class="px-4 py-2 bg-blue-600 rounded hover:bg-blue-500 cursor-pointer"
                                @click="">Search</button>
                        </div>
                    </el-row>
                    <el-row :gutter="20" class="h-[calc(100%-60px-20px-60px)] overflow-auto ">
                        <el-col v-for="model in filteredModels" :key="model.createTime + model.name" :span="8"
                            class="h-[23%]   w-full">
                            <!-- 模型卡片 -->
                            <div
                                class=" p-4 bg-black rounded-lg shadow-lg opacity-80 border border-[#fff9] border-solid cursor-pointer relative box-border h-full  w-full">
                                <!-- <div class="text-lg font-bold flex items-center">
                                    <Package :size="50" class="text-blue-600 mr-1" />{{ model.name }}Create Time:{{
                                        model.createTime }}
                                </div> -->
                                <div class="flex items-center w-full">
                                    <!-- Package 图标 -->
                                    <div class="flex-shrink-0">
                                        <Package :size="50" class="text-blue-600 mr-1" />
                                    </div>

                                    <!-- 文本内容 -->
                                    <div class="ml-2 flex flex-col w-[calc(100%-56px)]">
                                        <!-- model.name -->
                                        <div
                                            class="text-lg font-bold w-full overflow-hidden text-ellipsis whitespace-nowrap ">
                                            {{ model.name }}
                                        </div>

                                        <!-- model.createTime -->
                                        <span
                                            class="text-sm text-gray-400 w-full text-ellipsis whitespace-nowrap overflow-hidden  ">Create
                                            Time: {{ model.createTime }}</span>
                                    </div>
                                </div>
                                <div class="text-sm text-gray-400 mt-1.5  relative w-full">
                                    <div class="absolute overflow-auto">
                                        {{ model.description }}

                                    </div>
                                </div>
                                <div
                                    class="text-[12px] text-white flex items-center absolute bottom-2 left-2 px-2 py-2">
                                    <Mail :size="16" class="mr-1 text-green-600" />{{ model.author }}
                                </div>
                                <div
                                    class="px-4 py-2 text-[12px] rounded hover:text-blue-500 absolute bottom-2 font-bold right-2 flex items-center">
                                    Add to project
                                    <FilePlus2 :size="16" class="ml-1" />
                                </div>
                            </div>
                        </el-col>
                    </el-row>
                    <div class="flex justify-around">
                        <!-- <el-pagination background layout="prev, pager, next" v-model:currentPage
                            :total="props.dataNum" :page-size="15" @current-change="pageChange" @next-click="pageNext"
                            @prev-click="pagePrev">
                        </el-pagination> -->
                    </div>
                </main>

            </el-col>


        </el-row>

    </div>

</template>

<script setup lang="ts">
import { ref, computed, onMounted, type Ref } from 'vue';
import projectsBg from '@/components/projects/projectsBg.vue'
import { getModels, getMethods } from '@/api/http/analysis'
import { FilePlus2, Mail, Package } from 'lucide-vue-next';
import type { modelsOrMethods } from '@/type/modelCentral'


const categories = ref<string[]>([
    'Natural Perspective',
    'Human Perspective',
    'Global Scale',
    'Regional Scale',
    'Data Analysis',
    'Machine Learning'
]);

const searchQuery = ref<string>('');
const models: Ref<modelsOrMethods[]> = ref([]);
// const currentPagea

const columns = ref<number>(3);

const updateColumns = () => {
    const containerWidth = window.innerWidth - 80;
    columns.value = Math.max(1, Math.floor(containerWidth / 350));
};

const handlePageChange = (page: number) => {
    console.log(page, 'page');
}

onMounted(async () => {
    updateColumns();
    window.addEventListener('resize', updateColumns);
    models.value = (await getModels({
        "asc": false,
        "page": 1,
        "pageSize": 12,
        "searchText": "",
        "sortField": "createTime",
        "tagClass": "problemTags",
        "tagNames": [
            ""
        ]
    })).data
    console.log(models.value, 'models.value');

});

const filteredModels = computed(() => {
    return models.value.filter(m => m.name.toLowerCase().includes(searchQuery.value.toLowerCase()));
});
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