<template>
    <div>
        <div class="flex h-[44px] w-full justify-between">
            <div class="my-1.5 ml-2 flex w-fit items-center rounded bg-[#eaeaea] shadow-md">
                <el-button link class="toolItem btHover" @click="showPackageList">
                    <CloudServerOutlined class="mr-1" />
                    依赖管理
                </el-button>
                <div style="border-right: 1.5px dashed #5f6477; height: 20px;"></div>

                <el-button link class="toolItem" :class="{ 'btHover': !isRunning }" @click="runCode"
                    :disabled="isRunning">
                    <CaretRightOutlined class="mr-1" />
                    运行
                </el-button>
                <div style="border-right: 1.5px dashed #5f6477; height: 20px;"></div>

                <el-button link class="toolItem" :class="{ 'btHover': isRunning }" @click="stopCode"
                    :disabled="!isRunning">
                    <StopOutlined class="mr-1" />
                    结束
                </el-button>
                <div style="border-right: 1.5px dashed #5f6477; height: 20px;"></div>
                <el-button link class="toolItem btHover" @click="saveCode">
                    <SaveOutlined class="mr-1" />
                    保存
                </el-button>

                <div style="border-right: 1.5px dashed #5f6477; height: 20px;"></div>

                <el-button link class="toolItem btHover" @click="publishOpen">
                    <SendOutlined class="mr-1" />
                    发布
                </el-button>
            </div>

            <el-dialog title="工具发布" v-model="publishView" width="400px"> 
                <el-form :model="publishToolData" label-width="100px">
                    <el-form-item label="工具名称" required>
                        <el-input v-model="publishToolData.toolName" placeholder="请输入工具名称" />
                    </el-form-item>
                    <el-form-item label="运行环境">
                        <el-select v-model="publishToolData.environment" disabled>
                            <el-option label="Python 3.9" value="python3.9" />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="分类" required>
                        <el-input v-model="publishToolData.category" placeholder="请输入工具分类" />
                    </el-form-item>
                    <el-form-item label="描述" required>
                        <el-input 
                            v-model="publishToolData.description" 
                            type="textarea" 
                            :rows="3" 
                            placeholder="请输入工具功能描述" 
                        />
                    </el-form-item>
                    <el-form-item label="标签">
                        <el-tag
                            v-for="tag in publishToolData.tags"
                            :key="tag"
                            closable
                            @close="removeTag(tag)"
                            style="margin-right: 8px; margin-bottom: 8px"
                        >
                            {{ tag }}
                        </el-tag>
                        <el-input
                            v-if="tagsInput !== undefined"
                            v-model="tagsInput"
                            ref="tagInputRef"
                            size="small"
                            style="width: 120px"
                            @keyup.enter="addTag"
                            @blur="addTag"
                        />
                        <el-button 
                            v-else 
                            size="small" 
                            @click="showTagInput"
                            style="margin-bottom: 8px"
                        >
                            + 添加标签
                        </el-button>
                    </el-form-item>
                    
                    <el-form-item label="参数配置">
                        <div v-for="(param, index) in publishToolData.parameters" :key="index" class="param-item">
                            <el-input v-model="param.Name" placeholder="参数名" style="width: 100px" />
                            <el-input v-model="param.Flags" placeholder="Flags" style="width: 120px; margin-left: 8px" />
                            <el-select v-model="param.Type" style="width: 100px; margin-left: 8px">
                                <el-option label="String" value="String" />
                                <el-option label="Number" value="Number" />
                                <el-option label="Boolean" value="Boolean" />
                            </el-select>
                            <el-input v-model="param.Description" placeholder="描述" style="width: 150px; margin-left: 8px" />
                            <el-input 
                                v-model="param.default_value" 
                                placeholder="默认值" 
                                style="width: 120px; margin-left: 8px" 
                                :disabled="param.Type === 'Boolean'"
                            />
                            <el-button type="danger" @click="removeParameter(index)" style="margin-left: 8px">删除</el-button>
                        </div>
                        <el-button type="primary" @click="addParameter" style="margin-top: 10px">添加参数</el-button>
                    </el-form-item>
                </el-form>
            
                <template #footer>
                    <el-button @click="publishView = false">取消</el-button>
                    <el-button type="primary" @click="publishFunction" :loading="publishLoading">发布</el-button>
                </template>
            </el-dialog>

            <el-dialog title="依赖管理" v-model="dialogVisible" width="400px">
                <!-- 表格 -->
                <el-table :data="packageList" style="width: 100%">
                    <el-table-column prop="package" label="包名" />
                    <el-table-column prop="version" label="版本">
                        <template #default="scope">
                            {{ scope.row.version || '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column label="操作">
                        <template #default="scope">
                            <el-button link type="primary" @click="removePackage(scope.row)">移除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="mt-1 flex items-center" v-show="addPackageShow">
                    <div class="">
                        <!-- <font-awesome-icon style="margin-left: 2px; font-size: 10px; color: red" icon="star-of-life" /> -->
                        <label><span style="color: red">*</span>包名: </label>
                        <el-input v-model="addedPackageInfo.name" placeholder="package name"
                            style="width: 120px; font-size: 14px" />
                    </div>
                    <div class="ml-4">
                        <label>版本: </label>
                        <el-input v-model="addedPackageInfo.version" placeholder="version"
                            style="width: 70px; font-size: 14px" />
                    </div>
                    <div class="ml-4">
                        <el-button link type="primary" @click="installPackage()">安装</el-button>
                    </div>
                </div>
                <!-- 底部按钮 -->
                <template #footer>
                    <span class="dialog-footer">
                        <el-button @click="addPackageShow = !addPackageShow">安装依赖</el-button>
                        <el-button type="primary" @click="dialogVisible = false">关闭</el-button>
                    </span>
                </template>
            </el-dialog>

            <div class="relative my-1.5 ml-2 flex w-fit items-center rounded">
                <div class="relative my-1 mr-2 flex h-full cursor-pointer items-center rounded bg-[#eaeaea] px-2 text-xs shadow-md"
                    @click="">
                    当前环境：{{ selectedEnv }}
                </div>
                <div v-if="showDropdown"
                    class="absolute top-8 left-0 z-10 mt-1 w-fit rounded border border-gray-300 bg-white shadow-md">
                    <div v-for="env in envOptions" :key="env" class="cursor-pointer px-3 py-2 text-sm hover:bg-gray-200"
                        @click="">
                        {{ env }}
                    </div>
                </div>
            </div>
        </div>
        <div class="code-editor !bg-[#f9fafb]">
            <Codemirror class="!p-0 !text-[12px]" v-model="code" :extensions="extensions" @ready="onCmReady"
                @update:model-value="onCmInput" />
        </div>
    </div>
</template>

<script setup lang="ts">
import {
    CloudServerOutlined,
    CaretRightOutlined,
    SaveOutlined,
    StopOutlined,
    SendOutlined,
} from '@ant-design/icons-vue'
import {
    projectOperating,
    getScript,
    updateScript,
    runScript,
    stopScript,
    operatePackage,
    getPackages,
} from '@/api/http/analysis'
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Codemirror } from 'vue-codemirror'
import { python } from '@codemirror/lang-python'
import { ElMessage } from 'element-plus'
import { publishTool} from '@/api/http/tool' 

// import type { analysisResponse } from "@/type/analysis";
// import { oneDarkTheme } from "@codemirror/theme-one-dark";

const props = defineProps({
    projectId: {
        type: String,
        required: true,
    },
    userId: {
        type: String,
        required: true,
    },
})

const emit = defineEmits(['addMessage'])

/**
 * 在线编程工具条
 */

const showDropdown = ref(false)
const envOptions = ['Python 2.7', 'Python 3.6', 'Python 3.9']
const selectedEnv = ref('Python 3.9')
const dialogVisible = ref(false)
const addPackageShow = ref(false)
const addedPackageInfo = ref({
    name: '',
    version: '',
})
const packageList = ref([])
const isRunning = ref(false)


const showPackageList = async () => {
    dialogVisible.value = true
    await getPackageList()
}

const installPackage = async () => {
    let requestJson = {}
    if (addedPackageInfo.value.name) {
        dialogVisible.value = false
        requestJson = addedPackageInfo.value.version
            ? {
                projectId: props.projectId,
                userId: props.userId,
                action: 'add',
                name: addedPackageInfo.value.name,
                version: addedPackageInfo.value.version,
            }
            : {
                projectId: props.projectId,
                userId: props.userId,
                action: 'add',
                name: addedPackageInfo.value.name,
            }
    } else {
        ElMessage.warning('请输入要安装的依赖包名')
    }
    emit('addMessage', '正在安装依赖：' + addedPackageInfo.value.name + '，请等待并关注安装信息')
    await operatePackage(requestJson)
}

const removePackage = async (row: any) => {
    dialogVisible.value = false

    await operatePackage({
        projectId: props.projectId,
        userId: props.userId,
        action: 'remove',
        name: row.package,
    })
    console.log('正在卸载：', row.package)
}

const getPackageList = async () => {
    let result = await getPackages({
        projectId: props.projectId,
        userId: props.userId,
    })
    packageList.value = result.map((item: any) => {
        let temp = item.split(' ')
        return { package: temp[0], version: temp[1] ?? '' }
    })
}

const runCode = async () => {
    isRunning.value = true
    // 1、先更新代码
    let saveResult = await updateScript({
        projectId: props.projectId,
        userId: props.userId,
        content: code.value,
    })
    if (saveResult.status === 1) {
        // 2、再执行代码

        emit('addMessage', 'code')

        let runResult = await runScript({
            projectId: props.projectId,
            userId: props.userId,
        })
        if (runResult.status === 1) {
            ElMessage.success('脚本启动')
        } else {
            ElMessage.error('启动失败，请重试或者联系管理员')
        }
    } else {
        ElMessage.error('保存失败，请重试或者联系管理员')
    }
}

const stopCode = async () => {
    isRunning.value = false
    let stopResult = await stopScript({
        projectId: props.projectId,
        userId: props.userId,
    })
    console.log(stopResult, 'stopResult');

    ElMessage.info('正在停止运行')
}
const saveCode = async () => {
    // 保存代码内容
    let result = await updateScript({
        projectId: props.projectId,
        userId: props.userId,
        content: code.value,
    })
    if (result.status === 1) {
        ElMessage.success('代码保存成功')
    } else {
        ElMessage.error('代码保存失败')
    }
}

const keyboardSaveCode = (event: KeyboardEvent) => {
    if (event.ctrlKey && event.key === 's') {
        event.preventDefault(); // 阻止浏览器默认的保存页面行为
        saveCode(); // 调用保存逻辑
    }
};

// 切换环境选择下拉框状态

// const toggleDropdown = () => {
//     showDropdown.value = !showDropdown.value;
// };
// const selectEnv = (env: string) => {
//     selectedEnv.value = env;
//     showDropdown.value = false;
// };

/**
 * codemirror操作
 */

// 定义代码内容

const code = ref(`代码读取失败，请检查容器运行情况或联系管理员`)

// CodeMirror 配置项
const extensions = [python()] // 使用正确的 light 主题

// 当编辑器初始化完成时触发
const onCmReady = (editor: any) => {
    if (0) {
        console.log('CodeMirror is ready!', editor)
    }
}

// 当代码内容发生变化时触发
const onCmInput = (value: string) => {
    if (0) {
        console.log('Code updated:', value)
    }
}


const publishView = ref(false)

const publishOpen = async() =>{
    publishView.value = true
}


const publishLoading = ref(false)
// 发布结构
const publishToolData = ref({
    toolName: '',
    environment: 'python3.9',
    description: '',
    category: '',
    tags: [] as string[],
    parameters: [] as Array<{
        Name: string
        Flags: string
        Type: string
        Description: string
        default_value: any
        Optional: boolean
    }>
})

const tagsInput = ref<string>()
const addParameter = () => {
    publishToolData.value.parameters.push({
        Name: '',
        Flags: '',
        Type: 'String',
        Description: '',
        default_value: null,
        Optional: false
    })
}

const showTagInput = () => {
    tagsInput.value = ''
}

const addTag = () => {
    if (tagsInput.value && tagsInput.value.trim()) {
        if (!publishToolData.value.tags.includes(tagsInput.value.trim())) {
            publishToolData.value.tags.push(tagsInput.value.trim())
        }
        tagsInput.value = undefined
    }
}

const removeTag = (tag: string) => {
    publishToolData.value.tags = publishToolData.value.tags.filter(t => t !== tag)
}

const removeParameter = (index) => {
    publishToolData.value.parameters.splice(index, 1)
}


// 发布工具
const publishFunction = async () => {
    if (!publishToolData.value.toolName) {
        ElMessage.error('工具名称不能为空')
        return
    }
    if (!publishToolData.value.description) {
        ElMessage.error('工具描述不能为空')
        return
    }
    if (!publishToolData.value.category) {
        ElMessage.error('工具分类不能为空')
        return
    }

    // 验证参数
    for (const param of publishToolData.value.parameters) {
        if (!param.Name || !param.Flags) {
            ElMessage.error('参数名称和Flags不能为空')
            return
        }
    }

    publishLoading.value = true
    try {
        
        const response = await publishTool(
            props.projectId,
            publishToolData.value.environment,
            props.userId,
            {
                toolName: publishToolData.value.toolName,
                description: publishToolData.value.description,
                category: publishToolData.value.category,
                tags: publishToolData.value.tags,
                parameters: publishToolData.value.parameters
            }
        )
        // 提取toolId
        const toolId = response.toolId
        console.log('工具发布成功,ID:', toolId)

        ElMessage.success('工具发布成功')
        publishView.value = false
        // 重置表单
        publishToolData.value = {
            toolName: '',
            environment: 'python3.9',
            description: '',
            category: '',
            tags: [],
            parameters: []
        }
    } catch (error) {
        ElMessage.error('工具发布失败: ' + (error as Error).message)
    } finally {
        publishLoading.value = false
    }
}

onMounted(async () => {
    code.value = await getScript({
        projectId: props.projectId,
        userId: props.userId,
    })
    let result = await projectOperating({
        projectId: props.projectId,
        userId: props.userId,
        action: 'open',
    })

    if (result.status === 1) {
        ElMessage.success('项目启动成功')
    } else {
        ElMessage.error('启动失败，请刷新页面或联系管理员')
    }
    window.addEventListener('keydown', keyboardSaveCode);
})
onBeforeUnmount(async () => {
    let result = await projectOperating({
        projectId: props.projectId,
        userId: props.userId,
        action: 'close',
    })
    if (result.status === 1) {
        console.log('关闭竟然成功了')
    } else {
        console.error('关闭果然失败了')
    }
    window.removeEventListener('keydown', keyboardSaveCode);
})
</script>

<style scoped>
@reference 'tailwindcss';

:deep(.cm-scroller) {
    overflow-x: hidden !important;
}

.code-editor {
    @apply h-[90%] w-full overflow-x-hidden overflow-y-auto rounded-lg bg-gray-100 font-sans text-sm;
}



.toolItem {
    font-size: 14px;
    padding: 0 12px;
    /* border-right: 1.5px dashed #5f6477; */
    color: #4c5160;
}

.btHover:hover {
    color: #1479d7;
    /* cursor: pointer; */
}

.toolItem:last-child {
    border-right: none;
}
</style>
