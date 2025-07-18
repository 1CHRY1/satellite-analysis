<template>
    <div>
        <projectsBg
            class="absolute inset-0 z-0 h-full w-full overflow-hidden bg-[radial-gradient(ellipse_at_bottom,_#1b2735_0%,_#090a0f_100%)]">
        </projectsBg>
        <el-row class="relative z-10 flex h-[calc(100vh-56px)] text-white" style="flex: none">
            <el-col :span="5" class="h-full">
                <div class="cardShadow border-box mx-[3vw] my-[3vh] opacity-80 p-6 h-[calc(100%-6vh)] overflow-auto">
                    <div class="flex justify-between items-center mb-4">
                        
                        <el-switch
                            v-model="showTools"
                            active-text="工具"
                            inactive-text="模型"
                            @change="handleViewSwitch"
                            class="ml-2"
                        />
                    </div>
                    
                    <h2 class="text-lg font-bold">{{t("modelpage.title")}}</h2>
                    <!-- <ul class="mt-4 space-y-2">
                        <li v-for="category in categories" :key="category" class="cursor-pointer hover:text-blue-400">
                            {{ category }}
                        </li>
                    </ul> -->
                    <el-tree :data="treeData" :props="defaultProps" default-expand-all :expand-on-click-node="false"
                        :highlight-current="false" class="readonly-tree" node-key="label" />
                </div>
            </el-col>
            <el-col :span="18" class="h-full">
                <main class="border-box mx-[2vw] mt-[3vh] h-[calc(100%-3vh)] px-6">
                    <el-row class="mb-[20px] h-[60px]">
                        <!-- 搜索栏 -->
                        <div class="cardShadow mb-6 flex w-full items-center space-x-2">
                            <input v-model="searchQuery" type="text" placeholder="Search models..."
                                class="w-[100%] rounded bg-white p-2 !text-black focus:ring-2 focus:ring-blue-400 focus:outline-none" />
                            <button class="w-20 cursor-pointer rounded bg-blue-600 px-4 py-2 hover:bg-blue-500"
                                @click="">
                                 {{t("modelpage.search")}}
                            </button>
                        </div>
                    </el-row>
                    <el-row :gutter="20" class="h-[calc(100%-60px-20px-60px)] overflow-auto">
                        <el-col v-for="model in filteredModels" :key="model.createTime + model.name" :span="8"
                            class="h-[23%] max-h-[200px] w-full">
                            <!-- 模型卡片 -->
                            <div
                                class="relative box-border h-full w-full cursor-pointer rounded-lg border border-solid border-[#fff9] bg-black p-4 opacity-80 shadow-lg">
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
                                            class="w-full overflow-hidden text-lg font-bold text-ellipsis whitespace-nowrap">
                                            {{ model.name }}
                                        </div>

                                        <!-- model.createTime -->
                                        <span
                                            class="w-full overflow-hidden text-sm text-ellipsis whitespace-nowrap text-gray-400">Create
                                            Time: {{ model.createTime }}</span>
                                    </div>
                                </div>
                                <div class="mt-1.5 h-[calc(100%-50px-14px-16px)] overflow-auto text-sm text-gray-400">
                                    {{ model.description }}
                                </div>
                                <div
                                    class="absolute bottom-2 left-2 flex items-center px-2 py-2 text-[12px] text-white">
                                    <Mail :size="16" class="mr-1 text-green-600" />{{
                                        model.author
                                    }}
                                </div>
                                <div
                                    class="px-4 py-2 text-[12px] rounded hover:text-blue-500 absolute bottom-2 font-bold right-2 flex items-center">
                                    <!-- Add to project -->
                                    <a href="https://geomodeling.njnu.edu.cn/modelItem/repository" target="_blank">
                                        {{t("modelpage.detail")}}
                                    </a>
                                    <FilePlus2 v-if="0" :size="16" class="ml-1" />
                                </div>
                            </div>
                        </el-col>
                    </el-row>
                    <div class="flex h-[60px] justify-around">
                        <el-pagination background layout="prev, pager, next" v-model="currentPage" :total="5189"
                            :page-size="15" @current-change="pageChange" @next-click="" @prev-click="">
                        </el-pagination>
                    </div>
                </main>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, type Ref } from 'vue'
import projectsBg from '@/components/projects/projectsBg.vue'
import { getModels } from '@/api/http/analysis'
import { FilePlus2, Mail, Package } from 'lucide-vue-next'
import type { modelsOrMethods } from '@/type/modelCentral'

import { useI18n } from 'vue-i18n'
import router from '@/router'
const { t } = useI18n()


const treeData = computed(()=> [
    {
        label: t("modelpage.treedata.label_appli"),
        children: [
            {
                label: t("modelpage.treedata.subleb_nature"),
                children: [
                    { label: t("modelpage.treedata.nature_land") },
                    { label: t("modelpage.treedata.nature_sea") },
                    { label: t("modelpage.treedata.nature_ice") },
                    { label: t("modelpage.treedata.nature_atmosphere") },
                    { label: t("modelpage.treedata.nature_space") },
                    { label: t("modelpage.treedata.nature_earth") },
                ],
            },
            {
                label: t("modelpage.treedata.subleb_human"),
                children: [
                    { label: t("modelpage.treedata.human_dev")},
                    { label: t("modelpage.treedata.human_soc")},
                    { label: t("modelpage.treedata.human_eco") },
                ],
            },
            {
                label: t("modelpage.treedata.subleb_comprehensive"),
                children: [
                    { label: t("modelpage.treedata.general_glo") },
                    { label: t("modelpage.treedata.general_reg")},
                ],
            },
        ],
    },
    {
        label: t("modelpage.treedata.label_method"),
        children: [
            {
                label: t("modelpage.treedata.subleb_data"),
                children: [
                    { label: t("modelpage.treedata.data_geo") },
                    { label: t("modelpage.treedata.data_rem")},
                    { label: t("modelpage.treedata.data_geostat")},
                    { label: t("modelpage.treedata.data_sma") },
                ],
            },
            {
                label:t("modelpage.treedata.subleb_process"),
                children: [
                    { label: t("modelpage.treedata.process_phy") },
                    { label: t("modelpage.treedata.process_chm") },
                    { label: t("modelpage.treedata.process_bio")},
                    { label:  t("modelpage.treedata.process_hun")},
                ],
            },
        ],
    },
]);

const defaultProps = {
    children: 'children',
    label: 'label',
};

const showTools = ref(false) 
const searchQuery = ref<string>('')
const models: Ref<modelsOrMethods[]> = ref([])
const currentPage: Ref<number> = ref(1)

const columns = ref<number>(3)

const updateColumns = () => {
    const containerWidth = window.innerWidth - 80;
    columns.value = Math.max(1, Math.floor(containerWidth / 350));
};

const pageChange = async (page: number) => {
    models.value = (await getModels({
        asc: false,
        page: page === 1 ? 3 : page === 3 ? 1 : page,
        pageSize: 12,
        searchText: "",
        sortField: "createTime",
        tagClass: "problemTags",
        tagNames: [""],
    })).data;
}

const handleViewSwitch = (isTools: false) => {
    // 可以在这里添加切换后的逻辑
    if (!isTools){
        router.push('/models')
    } 
    if (isTools){
        router.push('/tool')
    }
}

onMounted(async () => {
    updateColumns();
    window.addEventListener('resize', updateColumns);
    models.value = (await getModels({
        "asc": false,
        "page": 3,
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
    return models.value.filter((m) =>
        m.name.toLowerCase().includes(searchQuery.value.toLowerCase()),
    )
})
</script>

<style scoped lang="scss">
:deep(.readonly-tree .el-tree-node__content) {
    pointer-events: none;
    /* 禁用所有交互 */
    user-select: none;
    /* 禁止文本选中（可选） */
}



:deep(.readonly-tree .el-tree-node__expand-icon) {
    display: none;
    /* 隐藏展开收起图标 */
}

:deep(.el-tree) {
    background-color: transparent;
    color: white;
}

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
