<template>
    <div>
        <projectsBg
            class="absolute inset-0 z-0 h-full w-full overflow-hidden bg-[radial-gradient(ellipse_at_bottom,_#1b2735_0%,_#090a0f_100%)]">
        </projectsBg>
        <el-row class="relative z-10 flex h-[calc(100vh-56px)] text-white" style="flex: none">
            <el-col :span="5" class="h-full">
                <div class="cardShadow border-box mx-[3vw] my-[3vh] opacity-80 p-6 h-[calc(100%-6vh)] overflow-auto">
                    <!-- 标题行（添加切换按钮） -->
                    <div class="flex justify-between items-center mb-4">
                        
                        <el-switch
                            v-model="showTools"
                            active-text="工具"
                            inactive-text="模型"
                            @change="handleViewSwitch"
                            class="ml-2"
                        />
                    </div>

                    <!-- 树形菜单 -->
                    <el-tree :data="toolTreeData" :props="defaultProps" default-expand-all :expand-on-click-node="false"
                        :highlight-current="false" class="readonly-tree" node-key="label" />
                </div>
            </el-col>

            <el-col :span="18" class="h-full">
                <main class="border-box mx-[2vw] mt-[3vh] h-[calc(100%-3vh)] px-6">
                    <el-row class="mb-[30px] h-[60px]">
                        <div class="mb-4 flex space-x-2">
                            <button @click="showMode='models'" :class="{'bg-blue-600': showMode==='models'}">
                                全部工具
                            </button>
                            <button @click="showMyTools" :class="{'bg-green-600': showMode==='myTools'}">
                                我的工具
                            </button>
                        </div>
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

                    <!-- 全部工具视图 -->
                    <el-row class="flex flex-wrap gap-4">
                    <el-col 
                         v-for="tool in showMode === 'models' ? response : myTools"  
                        :key="tool.toolId" 
                        :span="6"
                        class="h-[200px] max-h-[200px] w-full mt-6">
                        <div class="relative box-border h-full w-full cursor-pointer rounded-lg border border-solid border-[#fff9] bg-black p-4 opacity-80 shadow-lg"
                            @click="showToolDetail(tool)">
                            <div class="flex w-full items-center">
                                <Package :size="50" class="mr-1 text-blue-600" />
                                <div class="ml-2 flex w-[calc(100%-56px)] flex-col">
                                    <div class="w-full overflow-hidden text-lg font-bold text-ellipsis whitespace-nowrap">
                                        {{ tool.name }}
                                    </div>
                                    <span class="w-full overflow-hidden text-sm text-ellipsis whitespace-nowrap text-gray-400">
                                        作者: {{ tool.userId }}
                                    </span>
                                </div>
                            </div>
                            <div class="mt-1.5 h-[calc(100%-50px-14px-16px)] overflow-auto text-sm text-gray-400">
                                {{ tool.description || '暂无描述' }}
                            </div>
                            <div v-if="tool.userId === userStore.user.id.value"
                                class="px-4 py-2 text-[12px] rounded hover:text-blue-500 absolute bottom-2 font-bold right-2 flex items-center">
                                <button @click.stop="showEditDialog(tool)">编辑</button>
                            </div>
                        </div>
                    </el-col>
                    </el-row>


                    <!-- 工具详情弹窗 -->
                    <el-dialog v-model="detailDialogVisible" :title="selectedTool?.toolName" width="50%">
                        <div v-if="selectedTool">
                            <div class="mb-4">
                                <h3 class="font-bold">描述</h3>
                                <p>{{ selectedTool.description || '暂无描述' }}</p>
                            </div>
                            <div class="mb-4">
                                <h3 class="font-bold">参数</h3>
                                <el-table :data="selectedTool.parameters || []" style="width: 100%">
                                    <el-table-column prop="name" label="参数名" />
                                    <!-- <el-table-column prop="type" label="类型" />
                                    <el-table-column prop="description" label="描述" /> -->
                                </el-table>
                            </div>
                            <div class="flex justify-end space-x-4 mt-4">
                                <button class="px-4 py-2 bg-blue-500 rounded hover:bg-blue-600"
                                    @click="showEditDialog(selectedTool)">
                                    编辑
                                </button>
                                <button class="px-4 py-2 bg-red-500 rounded hover:bg-red-600"
                                    @click="confirmDelete(selectedTool.toolId)">
                                    删除
                                </button>
                            </div>
                        </div>
                    </el-dialog>

                    <!-- 发布/编辑工具弹窗 -->
                    <el-dialog v-model="editDialogVisible" :title="isEditing ? '编辑工具' : '添加工具'" width="50%">
                        <el-form :model="toolForm" label-width="120px">
                            <el-form-item label="工具名称">
                                <el-input v-model="toolForm.name" />
                            </el-form-item>
                            <el-form-item label="描述">
                                <el-input v-model="toolForm.description" type="textarea" />
                            </el-form-item>
                            <el-form-item label="参数">
                                <div v-for="(param, index) in toolForm.parameters" :key="index" class="mb-2 flex items-center">
                                    <el-input v-model="param.Name" placeholder="参数名" class="mr-2" />
                                    <!-- <el-select v-model="param.type" placeholder="类型" class="mr-2">
                                        <el-option label="字符串" value="string" />
                                        <el-option label="数字" value="number" />
                                        <el-option label="布尔值" value="boolean" />
                                        <el-option label="对象" value="object" />
                                        <el-option label="数组" value="array" />
                                    </el-select>
                                    <el-input v-model="param.description" placeholder="描述" class="mr-2" /> -->
                                </div>
                            </el-form-item>
                        </el-form>
                        <template #footer>
                            <span class="dialog-footer">
                                <el-button @click="editDialogVisible = false">取消</el-button>
                                <el-button type="primary" @click="submitToolForm">确认</el-button>
                            </span>
                        </template>
                    </el-dialog>
                    <!-- <div class="flex h-[60px] justify-around">
                        <el-pagination background layout="prev, pager, next" v-model="currentPage" :total="5189"
                            :page-size="15" @current-change="pageChange" @next-click="" @prev-click="">
                        </el-pagination>
                    </div> -->
                </main>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, type Ref } from 'vue'
import projectsBg from '@/components/projects/projectsBg.vue'
// import { getModels } from '@/api/http/analysis'
import { FilePlus2, Mail, Package } from 'lucide-vue-next'
import type { modelsOrMethods } from '@/type/modelCentral'
import { getAllTools,type ToolData, updateTool, deleteTool, getToolById, type ToolParameter, type PaginatedToolResponse} from '@/api/http/tool'
import { ElMessage,ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store'

import { useI18n } from 'vue-i18n'
import router from '@/router'
const { t } = useI18n()

const showTools = ref(true) 
const userStore = useUserStore()
const userId = computed(() => userStore.user.id?.value || '')


// 工具分类数据
const toolTreeData = [
    {
        label: '工具分类',
        children: [
            { label: '' },
            { label: '' },
            { label: '' }
        ]
    }
]

// 切换处理
const handleViewSwitch = (isTools: false) => {
    // 可以在这里添加切换后的逻辑
    if (!isTools){
        router.push('/models')
    }
}



const defaultProps = {
    children: 'children',
    label: 'label',
};

const searchQuery = ref<string>('')
const models: Ref<modelsOrMethods[]> = ref([])
const currentPage: Ref<number> = ref(1)
const showMode = ref<'models' | 'myTools'>('models'); 
const myTools = ref<ToolData[]>([]); 
const selectedTool = ref<ToolData | null>(null)
const detailDialogVisible = ref(false)
const columns = ref<number>(3)

const editDialogVisible = ref(false)
const isEditing = ref(false)
const tools =ref([])

const toolForm = ref({
    name: '',
    description: '',
    parameters: [] as ToolParameter[],
    category:'',
    tags:'' as any,
    toolId: ''
})

const updateColumns = () => {
    const containerWidth = window.innerWidth - 80;
    columns.value = Math.max(1, Math.floor(containerWidth / 350));
};

// const pageChange = async (page: number) => {
//     models.value = (await getModels({
//         asc: false,
//         page: page === 1 ? 3 : page === 3 ? 1 : page,
//         pageSize: 12,
//         searchText: "",
//         sortField: "createTime",
//         tagClass: "problemTags",
//         tagNames: [""],
//     })).data;
// }

const response = ref()

const fetchAllTools = async () => {
  try {
    const res = await getAllTools({
        current: currentPage.value,
        size: 12
  }); 
    // 验证响应数据是否存
    if (!res || !res.data || !Array.isArray(res.data.records)) {
      console.error("无效的工具数据格式:", response);
      throw new Error("服务器返回无效的工具数据格式");
    }
    response.value = res.data?.records || []
    console.log("list", tools)
    // 访问 toolId 
    
    const toolIds = response.value  
      .filter(tool => tool?.toolId) 
      .map(tool => tool.toolId);    
    
    console.log("有效工具ID:", toolIds);
    console.log("所有", response)
    
    return response.value; 
  } catch (error) {
    ElMessage.error("获取工具列表失败");
    return [];
  }
};

// 获取我的工具
const fetchMyTools = async () => {
  const res = await getAllTools({
    current: currentPage.value,
    size: 12
});
  myTools.value = res.data.records.filter(tool => tool.userId === userStore.user.id.value );
}

const showMyTools = async () => {
    showMode.value = 'myTools'
    await fetchMyTools()
}


// 显示工具详情
const showToolDetail = (tool) => {
  selectedTool.value = tool;
  detailDialogVisible.value = true;
}

const showEditDialog = (tool: ToolData) => {
    isEditing.value = true
    selectedTool.value = tool
    toolForm.value = {
        name: tool.toolName,
        description: tool.description || '',
        parameters: tool.parameters ? [...tool.parameters] : [],
        toolId: tool.toolId,
        category: tool.category || '', 
        tags: tool.tags || []
    }
    editDialogVisible.value = true
}

const submitToolForm = async () => {
    try {
        const toolData = {
        toolId: toolForm.value.toolId,
        toolName: toolForm.value.name,
        description: toolForm.value.description,
        parameters: toolForm.value.parameters,
        environment: "production", 
        outputType: "", 
        tags: Array.isArray(toolForm.value.tags) ? toolForm.value.tags : [],
        category: toolForm.value.category 
        };

        if (!toolForm.value.name) {
            ElMessage.error("工具名称不能为空")
            return
        }

        if (isEditing.value) {
            // 更新工具
            await updateTool(
               toolForm.value.toolId, toolData)
            ElMessage.success("更新成功")
        }

        editDialogVisible.value = false
        await fetchMyTools() // 刷新我的工具列表
        await fetchAllTools()
    } catch (error) {
        ElMessage.error("更新失败")
    }
}

// 删除工具（带确认）
const confirmDelete = async (toolId) => {
  await ElMessageBox.confirm('确定删除？');
  await deleteTool(toolId);
  await fetchMyTools(); // 刷新列表
  await fetchAllTools()
}

onMounted(async () => {
    fetchAllTools();
    updateColumns();
    window.addEventListener('resize', updateColumns);
    // models.value = (await getModels({
    //     "asc": false,
    //     "page": 3,
    //     "pageSize": 12,
    //     "searchText": "",
    //     "sortField": "createTime",
    //     "tagClass": "problemTags",
    //     "tagNames": [
    //         ""
    //     ]
    // })).data
    // console.log(models.value, 'models.value');

});

// const filteredModels = computed(() => {
//     return models.value.filter((m) =>
//         m.name.toLowerCase().includes(searchQuery.value.toLowerCase()),
//     )
// })

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
