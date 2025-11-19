<template>
    <div class="bg-[#161b22]">
        <div class="flex h-[44px] w-full justify-between bg-[#161b22]">
            <a-space :size="5" style="padding-top: 5px; padding-left:5px">
                <a-button class="toolItem btHover" :icon="h(CloudServerOutlined)"
                    style="display: flex; align-items: center; justify-content: center" size="small"
                    @click="showPackageList">
                    依赖管理
                </a-button>
                <a-button class="toolItem" :class="{ 'btHover': !isRunning }"
                    style="display: flex; align-items: center; justify-content: center" :icon="h(CaretRightOutlined)"
                    size="small" @click="runCode" :disabled="isRunning">
                    运行
                </a-button>
                <a-button class="toolItem" :class="{ 'btHover': isRunning }"
                    style="display: flex; align-items: center; justify-content: center" :icon="h(StopOutlined)"
                    size="small" @click="stopCode" :disabled="!isRunning">
                    结束
                </a-button>
                <a-button class="toolItem btHover" style="display: flex; align-items: center; justify-content: center"
                    size="small" :icon="h(SaveOutlined)" @click="saveCode">
                    保存
                </a-button>
                <el-dropdown v-if="isToolProject" @command="handleTemplateCommand">
                    <a-button class="toolItem btHover"
                        style="display: flex; align-items: center; justify-content: center" :icon="h(DownOutlined)"
                        size="small">
                        模板
                    </a-button>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item command="expr">表达式（无需服务）</el-dropdown-item>
                            <el-dropdown-item command="flask">Flask（HTTP 瓦片）</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>

                <a-button v-if="isToolProject" class="toolItem btHover" style="display: flex; align-items: center; justify-content: center"
                    size="small" :icon="h(ToolOutlined)" @click="openToolWizard">
                    发布为工具
                </a-button>
            </a-space>

            <el-dialog title="发布为工具" v-model="toolWizardVisible" width="720px" class="tool-wizard-dialog"
                :close-on-click-modal="false">
                <div class="max-h-[70vh] overflow-y-auto pr-1">
                    <el-form :model="toolWizardForm" label-width="110px" label-position="left">
                        <el-form-item label="工具名称" required>
                            <el-input v-model="toolWizardForm.toolName" placeholder="请输入工具名称" />
                        </el-form-item>
                        <el-form-item label="描述" required>
                            <el-input v-model="toolWizardForm.description" type="textarea" :rows="3"
                                placeholder="请简要说明工具功能" />
                        </el-form-item>
                        <el-form-item label="分类" required>
                            <el-select v-model="toolWizardForm.category" filterable allow-create default-first-option
                                placeholder="选择或输入分类">
                                <el-option v-for="option in categoryOptions" :key="option" :label="option"
                                    :value="option" />
                            </el-select>
                        </el-form-item>
                        <el-form-item label="标签">
                            <el-select v-model="toolWizardForm.tags" multiple allow-create filterable
                                placeholder="输入标签后回车" class="w-full">
                                <el-option v-for="tag in toolWizardForm.tags" :key="tag" :label="tag" :value="tag" />
                            </el-select>
                        </el-form-item>

                        <el-divider content-position="left">执行方式</el-divider>
                        <el-form-item label="调用方式" required>
                            <el-radio-group v-model="toolWizardForm.invokeType">
                                <el-radio-button label="tiler-expression">表达式</el-radio-button>
                                <el-radio-button label="http+tile">HTTP 瓦片</el-radio-button>
                                <el-radio-button label="http+mosaic">HTTP Mosaic</el-radio-button>
                                <el-radio-button label="http+geojson">HTTP 矢量</el-radio-button>
                            </el-radio-group>
                        </el-form-item>

                        <template v-if="toolWizardForm.invokeType === 'tiler-expression'">
                            <el-form-item label="表达式模板" required>
                                <el-input v-model="toolWizardForm.expressionTemplate" type="textarea" :rows="3"
                                    placeholder="示例：(b3 - b5) / (b3 + b5)" />
                            </el-form-item>
                            <el-form-item label="颜色映射">
                                <el-input v-model="toolWizardForm.colorMap" placeholder="如 rdylgn，可在服务端扩展" />
                            </el-form-item>
                            <el-form-item label="像元方法">
                                <el-select v-model="toolWizardForm.pixelMethod">
                                    <el-option label="first" value="first" />
                                    <el-option label="mean" value="mean" />
                                    <el-option label="max" value="max" />
                                </el-select>
                            </el-form-item>
                        </template>

                        <template v-else>
                            <el-form-item label="服务地址" required>
                                <el-input v-model="toolWizardForm.serviceEndpoint"
                                    placeholder="例如 http://localhost:20080/run" />
                            </el-form-item>
                            <el-form-item label="HTTP 方法">
                                <el-select v-model="toolWizardForm.serviceMethod">
                                    <el-option label="POST" value="POST" />
                                    <el-option label="GET" value="GET" />
                                </el-select>
                            </el-form-item>
                            <el-form-item label="请求模板">
                                <el-input v-model="toolWizardForm.payloadTemplate" type="textarea" :rows="4"
                                    placeholder='默认模板: { "mosaicUrl": "{{mosaicUrl}}", "params": "{{params}}" }' />
                            </el-form-item>
                            <el-form-item label="结果路径">
                                <el-input v-model="toolWizardForm.responsePath" placeholder="可选，例如 data.result" />
                            </el-form-item>
                            <el-form-item label="服务状态">
                                <div class="rounded border border-dashed border-gray-400 px-3 py-2 text-sm">
                                    <div class="mb-2 flex items-center justify-between">
                                        <span>
                                            <span v-if="serviceStatus.running" class="text-green-500">服务运行中</span>
                                            <span v-else class="text-red-400">服务未运行</span>
                                        </span>
                                        <el-button size="small" @click="checkServiceStatus">刷新</el-button>
                                    </div>
                                    <div v-if="serviceStatus.running" class="text-xs text-gray-500">
                                        {{ serviceStatus.url || 'URL 未返回' }}
                                    </div>
                                    <div class="mt-2 flex flex-wrap items-center gap-2">
                                        <el-input v-model="toolWizardForm.servicePort" placeholder="指定端口 (可选)"
                                            class="w-32" />
                                        <el-button type="primary" size="small" :loading="servicePublishLoading"
                                            @click="startServiceForWizard">
                                            启动服务
                                        </el-button>
                                        <el-button type="danger" size="small" :loading="servicePublishLoading"
                                            @click="stopServiceForWizard">
                                            停止服务
                                        </el-button>
                                    </div>
                                </div>
                            </el-form-item>
                        </template>

                        <el-divider content-position="left">参数配置</el-divider>
                        <div class="mb-2 flex items-center justify-between">
                            <span class="text-sm text-gray-200">表单参数 (可选)</span>
                            <el-button type="primary" plain size="small" @click="addWizardParam">
                                添加参数
                            </el-button>
                        </div>
                        <div v-if="toolWizardForm.params.length === 0"
                            class="rounded bg-gray-100/10 p-3 text-xs text-gray-400">
                            暂无参数。若工具需要用户输入，请点击“添加参数”。
                        </div>
                        <div v-for="(param, index) in toolWizardForm.params" :key="index"
                            class="mb-3 rounded border border-gray-600/60 p-3">
                            <div class="flex flex-wrap gap-3">
                                <el-input v-model="param.label" placeholder="显示名称" class="w-40" />
                                <el-input v-model="param.key" placeholder="参数键" class="w-40" />
                                <el-select v-model="param.type" class="w-36">
                                    <el-option label="字符串" value="string" />
                                    <el-option label="数字" value="number" />
                                    <el-option label="布尔" value="boolean" />
                                    <el-option label="下拉" value="select" />
                                </el-select>
                                <el-checkbox v-model="param.required">必填</el-checkbox>
                                <el-select v-model="param.source" class="w-40" placeholder="数据来源">
                                    <el-option label="手动输入" value="" />
                                    <el-option label="影像波段" value="bands" />
                                </el-select>
                            </div>
                            <div class="mt-2 flex flex-wrap gap-3">
                                <el-input v-model="param.placeholder" placeholder="占位提示" class="w-64" />
                                <el-input v-model="param.optionsText"
                                    :disabled="param.source === 'bands' || param.type !== 'select'"
                                    placeholder="选项，示例: 红光:band3, 近红外:band5" class="flex-1" />
                                <el-button type="danger" text @click="removeWizardParam(index)">
                                    删除
                                </el-button>
                            </div>
                        </div>
                    </el-form>
                </div>
                <template #footer>
                    <el-button @click="toolWizardVisible = false" :disabled="servicePublishLoading">取消</el-button>
                    <el-button type="primary" @click="startServiceForWizard" :loading="servicePublishLoading">
                        发布
                    </el-button>
                </template>
            </el-dialog>

            <!-- ###################### OLD VERSION ####################### -->
            <!-- <el-dialog title="依赖管理" v-model="dialogVisible" width="400px">
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
                <template #footer>
                    <span class="dialog-footer">
                        <el-button @click="addPackageShow = !addPackageShow">安装依赖</el-button>
                        <el-button type="primary" @click="dialogVisible = false">关闭</el-button>
                    </span>
                </template>
            </el-dialog> -->

            <a-modal title="依赖管理" :visible="dialogVisible" @cancel="dialogVisible = false" width="400px">
                <template #footer>
                    <a-button @click="addPackageShow = !addPackageShow">
                        {{ addPackageShow ? '取消安装' : '安装依赖' }}
                    </a-button>
                    <a-button type="primary" @click="dialogVisible = false">关闭</a-button>
                </template>
                <a-table :data-source="packageList" :columns="[
                    {
                        title: '包名',
                        dataIndex: 'package',
                        key: 'package',
                    },
                    {
                        title: '版本',
                        dataIndex: 'version',
                        key: 'version',
                    },
                    {
                        title: '操作',
                        key: 'operation', // 使用 key: operation 来匹配 template #bodyCell 的逻辑
                        width: 80,
                    },
                ]" :pagination="false" row-key="package" style="width: 100%; margin-bottom: 16px;">
                    <template #bodyCell="{ column, record }">

                        <template v-if="column.key === 'version'">
                            {{ record.version || '-' }}
                        </template>

                        <template v-else-if="column.key === 'operation'">
                            <a-button type="link" @click="removePackage(record)">移除</a-button>
                        </template>
                    </template>
                </a-table>

                <div v-show="addPackageShow" style="margin-top: 8px; display: flex; align-items: center;">

                    <div style="display: flex; align-items: center;">
                        <label style="margin-right: 4px;">
                            <span style="color: red; margin-right: 2px;">*</span>包名:
                        </label>
                        <a-input v-model:value="addedPackageInfo.name" placeholder="package name"
                            style="width: 120px; font-size: 14px" />
                    </div>

                    <div style="margin-left: 16px; display: flex; align-items: center;">
                        <label style="margin-right: 4px;">版本: </label>
                        <a-input v-model:value="addedPackageInfo.version" placeholder="version"
                            style="width: 70px; font-size: 14px" />
                    </div>

                    <div style="margin-left: 16px;">
                        <a-button type="link" @click="installPackage()">安装</a-button>
                    </div>
                </div>
            </a-modal>

            <a-dropdown :trigger="['click']">
                <div class="my-1.5 ml-2 mr-2 flex w-fit items-center rounded"
                    style="padding-top: 5px; padding-bottom: 2px;">
                    <a-button type="text" class="ant-dropdown-link" @click.prevent style="
                        background-color: #09314c; 
                        color: white; 
                        border-radius: 4px;
                        font-size: 12px;
                        height: 24px;
                        padding: 0 8px;
                        box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05); /* 模拟 shadow-md */
                    ">
                        当前环境：{{ selectedEnv }}
                        <!-- <DownOutlined /> -->
                    </a-button>
                </div>
                <!-- ##################### OLD VERSION ##################### -->
                <!-- <template #overlay>
                    <a-menu @click="handleEnvChange">
                        <a-menu-item v-for="env in envOptions" :key="env">
                            {{ env }}
                        </a-menu-item>
                    </a-menu>
                </template> -->
            </a-dropdown>
            <!-- ##################### OLD VERSION ##################### -->
            <!-- <div class="relative my-1.5 ml-2 flex w-fit items-center rounded">
                <div class="relative my-1 mr-2 flex h-full cursor-pointer items-center rounded bg-[#09314c] px-2 text-xs shadow-md"
                    @click="">
                    当前环境：{{ selectedEnv }}
                </div>
                <div v-if="showDropdown"
                    class="absolute top-8 left-0 z-10 mt-1 w-fit rounded border border-[#0a4975]-300 bg-black shadow-md">
                    <div v-for="env in envOptions" :key="env" class="cursor-pointer px-3 py-2 text-sm hover:bg-gray-200"
                        @click="">
                        {{ env }}
                    </div>
                </div>
            </div> -->
        </div>
        <div class="code-editor">
            <Codemirror class="!p-0 !text-[12px]" v-model="code" :extensions="extensions" @ready="onCmReady"
                @update:model-value="onCmInput" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { h } from 'vue';
import {
    CloudServerOutlined,
    CaretRightOutlined,
    SaveOutlined,
    StopOutlined,
    ToolOutlined,
    DownOutlined,
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
import { publishTool, getToolStatus, unpublishTool, getAllTools } from '@/api/http/tool'
import { ref, reactive, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import { Codemirror } from 'vue-codemirror'
import { python } from '@codemirror/lang-python'
import { ElMessageBox } from 'element-plus'
import { updateRecord } from '@/api/http/user'
import { useUserStore, useToolRegistryStore, generateToolId } from '@/store'
import type {
    DynamicToolInvokeType,
    DynamicToolResultType,
    DynamicToolParamSchema,
    DynamicToolMeta,
} from '@/store/toolRegistry'
import { message } from 'ant-design-vue';
import { getProjects } from '@/api/http/analysis'

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
const userStore = useUserStore()
const toolRegistry = useToolRegistryStore()
const emit = defineEmits(['addMessage', 'servicePublished', 'serviceUnpublished'])

const currentUserId = computed(() => userStore.user?.id ?? '')

// 是否工具工程：用于控制“模板/发布为工具”按钮的显示
const isToolProject = ref(false)
const parseIsTool = (v: any): number => {
    if (typeof v === 'number') return v
    if (typeof v === 'boolean') return v ? 1 : 0
    if (typeof v === 'string') {
        const s = v.toLowerCase()
        if (s === '1' || s === 'true') return 1
        return 0
    }
    return 0
}
const refreshIsToolFlag = async () => {
    try {
        const list = await getProjects()
        const p = (list || []).find((it: any) => String(it?.projectId || '') === String(props.projectId))
        const raw = (p as any)?.isTool ?? (p as any)?.is_tool
        isToolProject.value = !!p && parseIsTool(raw) === 1
    } catch {
        isToolProject.value = false
    }
}

watch(
    currentUserId,
    (id) => {
        if (id) {
            toolRegistry.ensureLoaded(id)
        }
    },
    { immediate: true }
)

watch(
    () => props.projectId,
    async () => {
        await refreshIsToolFlag()
    }
)

type WizardParamForm = {
    label: string
    key: string
    type: 'string' | 'number' | 'select' | 'boolean'
    required: boolean
    placeholder: string
    source: '' | 'bands'
    optionsText: string
}

const builtinToolCategoryOptions = ['图像', '影像集合', '要素集合'] as const

const toolWizardVisible = ref(false)
const toolWizardSubmitting = ref(false)

const defaultPayloadTemplate = JSON.stringify(
    {
        mosaicUrl: '{{mosaicUrl}}',
        params: '{{params}}',
    },
    null,
    2
)

const toolWizardForm = reactive({
    toolName: '',
    description: '',
    category: builtinToolCategoryOptions[0],
    tags: [] as string[],
    invokeType: 'tiler-expression' as DynamicToolInvokeType,
    resultType: 'tile' as DynamicToolResultType,
    expressionTemplate: '',
    colorMap: 'rdylgn',
    pixelMethod: 'first',
    serviceEndpoint: '',
    serviceMethod: 'POST',
    servicePort: '',
    payloadTemplate: defaultPayloadTemplate,
    responsePath: '',
    params: [] as WizardParamForm[],
})

const wizardStorageKey = computed(() => `tool_wizard_last:${currentUserId.value || 'anonymous'}:${props.projectId}`)

const saveWizardDraft = () => {
    try {
        const data = JSON.stringify(toolWizardForm)
        localStorage.setItem(wizardStorageKey.value, data)
    } catch (e) {
        console.warn('保存发布表单失败:', e)
    }
}

const loadWizardDraft = () => {
    try {
        const raw = localStorage.getItem(wizardStorageKey.value)
        if (!raw) return false
        const parsed = JSON.parse(raw)
        // 合并而非替换，保留默认字段
        Object.assign(toolWizardForm, parsed || {})
        return true
    } catch (e) {
        console.warn('读取发布表单失败:', e)
        return false
    }
}

const categoryOptions = computed(() => {
    const set = new Set<string>([...builtinToolCategoryOptions])
    toolRegistry.tools.forEach((tool) => {
        if (tool.category) {
            set.add(tool.category)
        }
    })
    if (toolWizardForm.category) {
        set.add(toolWizardForm.category)
    }
    return Array.from(set)
})

watch(
    () => toolWizardForm.invokeType,
    (type) => {
        if (type === 'http+geojson') {
            toolWizardForm.resultType = 'geojson'
        } else if (type === 'http+mosaic') {
            toolWizardForm.resultType = 'mosaic'
        } else {
            toolWizardForm.resultType = 'tile'
        }
    },
    { immediate: true }
)

const addWizardParam = () => {
    toolWizardForm.params.push({
        label: '',
        key: '',
        type: 'string',
        required: false,
        placeholder: '',
        source: '',
        optionsText: '',
    })
}

const removeWizardParam = (index: number) => {
    toolWizardForm.params.splice(index, 1)
}

const resetToolWizard = () => {
    toolWizardForm.toolName = ''
    toolWizardForm.description = ''
    toolWizardForm.category = builtinToolCategoryOptions[0]
    toolWizardForm.tags = []
    toolWizardForm.invokeType = 'tiler-expression'
    toolWizardForm.resultType = 'tile'
    toolWizardForm.expressionTemplate = ''
    toolWizardForm.colorMap = 'rdylgn'
    toolWizardForm.pixelMethod = 'first'
    toolWizardForm.serviceEndpoint = ''
    toolWizardForm.serviceMethod = 'POST'
    toolWizardForm.servicePort = ''
    toolWizardForm.payloadTemplate = defaultPayloadTemplate
    toolWizardForm.responsePath = ''
    toolWizardForm.params.splice(0, toolWizardForm.params.length)
}

const openToolWizard = async () => {
    // 优先尝试加载草稿，其次使用默认值
    const loaded = loadWizardDraft()
    if (!loaded) {
        resetToolWizard()
    }
    toolWizardVisible.value = true
    await checkServiceStatus()
}

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
        message.warning('请输入要安装的依赖包名')
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
            message.success('脚本启动')
        } else {
            message.error('启动失败，请重试或者联系管理员')
        }
    } else {
        message.error('保存失败，请重试或者联系管理员')
    }
}

const stopCode = async () => {
    isRunning.value = false
    let stopResult = await stopScript({
        projectId: props.projectId,
        userId: props.userId,
    })
    console.log(stopResult, 'stopResult');

    message.info('正在停止运行')
}
const saveCode = async () => {
    // 保存代码内容
    let result = await updateScript({
        projectId: props.projectId,
        userId: props.userId,
        content: code.value,
    })
    if (result.status === 1) {
        message.success('代码保存成功')
    } else {
        message.error('代码保存失败')
    }
}

// 一键填充工具发布模板（无参函数模板）
const handleTemplateCommand = async (cmd: string) => {
    if (cmd === 'expr') {
        await applyExpressionTemplate()
    } else if (cmd === 'flask') {
        await applyFlaskTemplate()
    }
}

const applyExpressionTemplate = async () => {
    // 预置为“表达式（无需服务）”路径，100% 复用指数分析逻辑
    toolWizardForm.toolName = toolWizardForm.toolName || '表达式工具'
    toolWizardForm.description = toolWizardForm.description || '基于表达式的分析瓦片（与指数分析相同逻辑）'
    toolWizardForm.category = toolWizardForm.category || '图像'
    if (!toolWizardForm.tags || toolWizardForm.tags.length === 0) {
        toolWizardForm.tags = ['expression', 'tile']
    }
    toolWizardForm.invokeType = 'tiler-expression'
    toolWizardForm.resultType = 'tile'
    // 使用占位模板，从表单 expression 读取，默认值放在参数 default
    toolWizardForm.expressionTemplate = '{{expression}}'
    toolWizardForm.colorMap = 'rdylgn'
    toolWizardForm.pixelMethod = 'first'
    toolWizardForm.params.splice(0, toolWizardForm.params.length)
    toolWizardForm.params.push(
        { label: '表达式', key: 'expression', type: 'string', required: true, placeholder: '例如 2*b2-b1-b3', source: '', optionsText: '', default: '2*b2-b1-b3' },
        { label: '色带', key: 'color', type: 'string', required: false, placeholder: '默认 rdylgn', source: '', optionsText: '', default: 'rdylgn' },
        { label: '像元方法', key: 'pixel_method', type: 'string', required: false, placeholder: '默认 first', source: '', optionsText: '', default: 'first' },
    )
    // 可选：在编辑器中放入说明性注释，提示该模板无需后端代码
    const stub = `# 表达式工具模板（无需服务）\n# 说明：此工具直接在发布向导中配置表达式，\n# 前端会拼接 Titiler 的分析瓦片 URL 并叠加地图，\n# 无需在此处编写后端代码。\n#\n# 快速开始：点击“发布为工具”→ 选择“表达式（无需服务）”，\n# 按需修改表达式/色带/像元方法并发布即可。\n`
    const current = (code.value || '').trim()
    if (!current || current === '代码读取失败，请检查容器运行情况或联系管理员') {
        code.value = stub
    } else {
        try {
            await ElMessageBox.confirm('将覆盖当前代码为说明性注释，是否继续？', '提示', {
                confirmButtonText: '覆盖',
                cancelButtonText: '保留',
                type: 'warning',
            })
            code.value = stub
        } catch {
            // 用户取消覆盖，保持现有代码
        }
    }
    saveWizardDraft()
    message.success('已应用“表达式（无需服务）”模板，可直接发布为工具')
}

const applyFlaskTemplate = async () => {
    // 预置为“Flask（HTTP 瓦片）”路径（内置渲染版，无 Titiler 依赖）
    const template = `"""
Flask-based dynamic tile tool (no Titiler).

It renders analysis tiles directly from a MosaicJSON using rio-tiler.

Endpoints:
- POST /run -> returns {"tileTemplate": "<this-service>/tile/{z}/{x}/{y}.png?..."}
- GET  /tile/{z}/{x}/{y}.png -> serves PNG tiles

Required pip packages (install via 依赖管理):
- rasterio, numpy, mercantile, requests
- rio-tiler>=5, rio-tiler-mosaic, cogeo-mosaic
"""

from flask import Flask, jsonify, request, Response as FlaskResponse
from flask_cors import CORS
from urllib.parse import quote_plus
import json
import requests
import numpy as np

import mercantile
from rio_tiler.io import COGReader
from rio_tiler_mosaic.mosaic import mosaic_tiler
from rio_tiler_mosaic.methods import defaults
from rio_tiler.colormap import cmap
from rio_tiler.utils import render
from rio_tiler.profiles import img_profiles

app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})


def tiler(src_path: str, *args, nodata=None, **kwargs):
    with COGReader(src_path, options={"nodata": nodata}) as cog:
        return cog.tile(*args, **kwargs)


def normalize(arr, min_val=-1.0, max_val=1.0):
    arr = np.nan_to_num(arr)
    arr = np.clip((arr - min_val) / (max_val - min_val), 0, 1)
    return (arr * 255).astype("uint8")


def fetch_mosaic_definition(mosaic_url: str):
    resp = requests.get(mosaic_url, timeout=10)
    resp.raise_for_status()
    return resp.json()


@app.route("/run", methods=["POST"])
def run():
    body = request.get_json(force=True, silent=True) or {}
    mosaic_url = body.get("mosaicUrl")
    params = body.get("params") or {}

    if isinstance(params, str):
        try:
            params = json.loads(params)
        except json.JSONDecodeError:
            params = {}

    if not isinstance(params, dict):
        params = {}

    if not mosaic_url:
        return jsonify({"error": "mosaicUrl is required"}), 400

    expression = params.get("expression") or "2*b2-b1-b3"
    color = params.get("color") or "rdylgn"
    pixel_method = params.get("pixel_method") or "first"

    # 生成绝对 URL，确保前端能直接请求此服务自身的瓦片接口
    base = request.url_root.rstrip("/")
    tile_template = (
        f"{base}/tile/{{z}}/{{x}}/{{y}}.png?"
        f"mosaic_url={quote_plus(mosaic_url)}"
        f"&expression={quote_plus(expression)}"
        f"&pixel_method={quote_plus(pixel_method)}"
        f"&color={quote_plus(color)}"
    )
    return jsonify({"tileTemplate": tile_template})


@app.get("/tile/<int:z>/<int:x>/<int:y>.png")
def tile(z: int, x: int, y: int):
    try:
        mosaic_url = request.args.get("mosaic_url")
        expression = request.args.get("expression", type=str)
        pixel_method = request.args.get("pixel_method", default="first", type=str)
        color = request.args.get("color", default="rdylgn", type=str)

        if not mosaic_url or not expression:
            return FlaskResponse("Missing required params", status=400)

        # Step 1: Load MosaicJSON
        mosaic_def = fetch_mosaic_definition(mosaic_url)

        # Step 2: Resolve assets for this tile using minzoom quadkey
        quadkey_zoom = mosaic_def.get("minzoom")
        if quadkey_zoom is None:
            return FlaskResponse("Invalid mosaic: missing minzoom", status=400)

        merc_tile = mercantile.Tile(x=x, y=y, z=z)
        if merc_tile.z > quadkey_zoom:
            depth = merc_tile.z - quadkey_zoom
            for _ in range(depth):
                merc_tile = mercantile.parent(merc_tile)

        quadkey = mercantile.quadkey(*merc_tile)
        assets = mosaic_def.get("tiles", {}).get(quadkey)
        if not assets:
            return FlaskResponse(status=204)

        # Step 3: Pixel selection
        method = (pixel_method or "first").lower()
        if method == "highest":
            sel = defaults.HighestMethod()
        elif method == "lowest":
            sel = defaults.LowestMethod()
        else:
            sel = defaults.FirstMethod()

        # Step 4: Fetch tile with expression
        img, mask = mosaic_tiler(assets, x, y, z, tiler, pixel_selection=sel, expression=expression, nodata=0)
        if img is None:
            return FlaskResponse(status=204)

        # Step 5: Render single-band expression with colormap
        img_uint8 = normalize(img[0], -1, 1)
        content = render(img_uint8, mask, img_format="png", colormap=cmap.get(color), **img_profiles.get("png"))
        return FlaskResponse(content, mimetype="image/png")
    except Exception as e:
        return FlaskResponse(str(e), status=500, mimetype="text/plain")


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=20080, debug=False)
`
    // 如果已有代码，提示是否覆盖
    const current = (code.value || '').trim()
    if (current && current !== '代码读取失败，请检查容器运行情况或联系管理员') {
        try {
            await ElMessageBox.confirm('将覆盖当前代码，是否继续？', '提示', {
                confirmButtonText: '覆盖',
                cancelButtonText: '取消',
                type: 'warning',
            })
        } catch {
            return
        }
    }
    code.value = template

    // 预填基础信息，方便直接发布
    toolWizardForm.toolName = toolWizardForm.toolName || 'Flask动态指数'
    toolWizardForm.description = toolWizardForm.description || 'Flask 服务返回指数分析瓦片（与指数分析相同逻辑）'
    toolWizardForm.category = toolWizardForm.category || '图像'
    if (!toolWizardForm.tags || toolWizardForm.tags.length === 0) {
        toolWizardForm.tags = ['flask', 'tile']
    }
    toolWizardForm.invokeType = 'http+tile'
    toolWizardForm.resultType = 'tile'
    toolWizardForm.serviceMethod = 'POST'
    // 端口由发布时后端分配；服务地址在启动服务成功后复制“运行中”URL
    toolWizardForm.params.splice(0, toolWizardForm.params.length)
    toolWizardForm.params.push(
        { label: '表达式', key: 'expression', type: 'string', required: true, placeholder: '例如 2*b2-b1-b3', source: '', optionsText: '', default: '2*b2-b1-b3' },
        { label: '色带', key: 'color', type: 'string', required: false, placeholder: '默认 rdylgn', source: '', optionsText: '', default: 'rdylgn' },
        { label: '像元方法', key: 'pixel_method', type: 'string', required: false, placeholder: '默认 first', source: '', optionsText: '', default: 'first' },
    )
    saveWizardDraft()
    message.success('已填充 Flask 模板，请保存代码后启动服务再发布')
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

// 服务发布相关
type ServiceStatus = {
    isPublished: boolean
    running: boolean
    url: string
    host: string
    port: number | null
}
const servicePublishLoading = ref(false)
const currentToolId = ref<string | null>(null)
const parseHostPortFromUrl = (url: string): { host: string; port: number | null } => {
    try {
        const u = new URL(url)
        const host = u.hostname || ''
        const port = u.port ? Number(u.port) : null
        return { host, port }
    } catch {
        return { host: '', port: null }
    }
}
const serviceStatus = ref<ServiceStatus>({
    isPublished: false,
    running: false,
    url: '',
    host: '',
    port: null
})

const resolveCurrentToolId = async (): Promise<string | null> => {
    if (currentToolId.value) return currentToolId.value
    try {
        const res = await getAllTools({ current: 1, size: 100, userId: props.userId })
        const list = res?.data?.records || []
        const found = list.find((it: any) => it?.projectId === props.projectId)
        currentToolId.value = found?.toolId ?? null
    } catch {
        currentToolId.value = null
    }
    return currentToolId.value
}

const checkServiceStatus = async () => {
    try {
        const toolId = await resolveCurrentToolId()
        if (!toolId) {
            serviceStatus.value = { isPublished: false, running: false, url: '', host: '', port: null }
            return
        }
        const res = await getToolStatus({ userId: props.userId, toolId })
        if (res?.status === 1) {
            const data = res.data || {}
            const { host, port } = parseHostPortFromUrl(data.url || '')
            serviceStatus.value = {
                isPublished: true,
                running: data.status === 'running',
                url: data.url || '',
                host,
                port,
            }
        } else {
            serviceStatus.value = { isPublished: false, running: false, url: '', host: '', port: null }
        }
    } catch (error) {
        console.error('检查服务状态失败:', error)
        serviceStatus.value = { isPublished: false, running: false, url: '', host: '', port: null }
    }
}

const publishServiceFunction = async (preferredPort?: number) => {
    servicePublishLoading.value = true
    try {
        // 适配 /tools/publish（CommonResultVO 包装）
        // 构造 Code2ToolDTO 形状的请求体
        const parameters = (toolWizardForm.params || []).map((item: any) => ({
            label: item.label,
            key: item.key,
            type: item.type,
            required: item.required,
            placeholder: item.placeholder,
            source: item.source,
            optionsText: item.optionsText,
        }))

        const body: any = {
            projectId: props.projectId,
            userId: props.userId,
            toolName: (toolWizardForm.toolName || '').trim(),
            description: (toolWizardForm.description || '').trim(),
            tags: Array.isArray(toolWizardForm.tags) ? [...toolWizardForm.tags] : [],
            categoryId: null,
            parameters,
            share: false,
            outputType: toolWizardForm.resultType || 'tile',
        }
        if (preferredPort) body.servicePort = preferredPort
        const res = await publishTool(body)
        if (!res || res.status !== 1) {
            throw new Error(res?.message ?? '发布失败')
        }
        const data = res.data ?? {}
        const { host, port } = parseHostPortFromUrl(data.url || '')
        serviceStatus.value = {
            isPublished: true,
            running: true,
            url: data.url || '',
            host,
            port,
        }
        const url = data.url || ''
        currentToolId.value = data.toolId
        message.success(url ? `服务发布成功！访问地址: ${url}` : '服务发布成功！')
        emit('addMessage', url ? `Service running at: ${url}` : 'Service running')
        emit('servicePublished')
    } catch (error) {
        message.error('服务发布失败: ' + (error as Error).message)
    } finally {
        servicePublishLoading.value = false
    }
}

const unpublishServiceFunction = async () => {
    servicePublishLoading.value = true
    try {
        const toolId = await resolveCurrentToolId()
        if (!toolId) {
            message.warning('未找到已发布的工具')
            return
        }
        const res = await unpublishTool({ userId: props.userId, toolId })
        if (res?.status !== 1) throw new Error(res?.message ?? '停止服务失败')
        serviceStatus.value = { isPublished: false, running: false, url: '', host: '', port: null }
        message.success('服务已停止')
        emit('addMessage', 'Service stopped')
        emit('serviceUnpublished')
    } catch (error) {
        message.error('停止服务失败: ' + (error as Error).message)
    } finally {
        servicePublishLoading.value = false
    }
}

const startServiceForWizard = async () => {
    const port = parseInt(toolWizardForm.servicePort, 10)
    const preferredPort = Number.isNaN(port) ? undefined : port
    await publishServiceFunction(preferredPort)
}

const stopServiceForWizard = async () => {
    if (!serviceStatus.value.running) {
        message.info('服务未运行')
        return
    }
    await unpublishServiceFunction()
}

const parseSelectOptions = (text: string) => {
    return text
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean)
        .map((item) => {
            const [labelPart, valuePart] = item.split(':')
            const label = (labelPart ?? '').trim()
            const value = (valuePart ?? labelPart ?? '').trim()
            return {
                label: label || value,
                value: value || label,
            }
        })
        .filter((option) => option.label && option.value)
}

const buildParamsSchema = (): DynamicToolParamSchema[] => {
    return toolWizardForm.params
        .map((param) => {
            const key = param.key.trim()
            const label = param.label.trim() || key
            const schema: DynamicToolParamSchema = {
                key,
                label,
                type: param.type,
                required: param.required,
            }
            if (param.placeholder) {
                schema.placeholder = param.placeholder
            }
            if (param.source) {
                schema.source = param.source
            }
            if (param.type === 'select' && !param.source) {
                const options = parseSelectOptions(param.optionsText)
                schema.options = options
            }
            return schema
        })
        .filter((schema) => schema.key)
}

const logToolPublish = async (toolName: string, description: string) => {
    try {
        await updateRecord({
            userId: userStore.user.id,
            actionDetail: {
                projectName: toolName,
                projectType: 'Tool',
                description,
            },
            actionType: '发布',
        })
    } catch (error) {
        console.error('记录工具发布失败:', error)
    }
}

const publishDynamicTool = async () => {
    if (toolWizardSubmitting.value) return
    const activeUserId = currentUserId.value || props.userId
    if (!activeUserId) {
        message.error('未获取到用户信息，无法发布工具')
        return
    }
    const name = toolWizardForm.toolName.trim()
    if (!name) {
        message.error('请填写工具名称')
        return
    }
    const category = toolWizardForm.category.trim()
    if (!category) {
        message.error('请填写工具分类')
        return
    }
    const description = toolWizardForm.description.trim()
    if (!description) {
        message.error('请填写工具描述')
        return
    }
    if (toolWizardForm.invokeType === 'tiler-expression' && !toolWizardForm.expressionTemplate.trim()) {
        message.error('请填写表达式模板')
        return
    }
    if (toolWizardForm.invokeType !== 'tiler-expression' && !toolWizardForm.serviceEndpoint.trim()) {
        message.error('请填写服务地址')
        return
    }

    const keySet = new Set<string>()
    for (const param of toolWizardForm.params) {
        const key = param.key.trim()
        if (!key) {
            message.error('参数键不能为空')
            return
        }
        if (keySet.has(key)) {
            message.error(`参数键重复：${key}`)
            return
        }
        keySet.add(key)
        if (param.type === 'select' && !param.source && !param.optionsText.trim()) {
            message.error(`请选择或输入参数“${param.label || param.key}”的枚举项`)
            return
        }
    }

    let payloadTemplate: any = undefined
    if (toolWizardForm.invokeType !== 'tiler-expression') {
        const payloadText = toolWizardForm.payloadTemplate.trim()
        if (payloadText) {
            try {
                payloadTemplate = JSON.parse(payloadText)
            } catch (error) {
                message.error('请求模板需要是合法的 JSON 格式')
                return
            }
        }
    }

    const paramsSchema = buildParamsSchema()

    const invokeConfig: DynamicToolMeta['invoke'] = toolWizardForm.invokeType === 'tiler-expression'
        ? {
            type: 'tiler-expression',
            expressionTemplate: toolWizardForm.expressionTemplate,
            colorMap: toolWizardForm.colorMap || 'rdylgn',
            pixelMethod: toolWizardForm.pixelMethod || 'first',
        }
        : {
            type: toolWizardForm.invokeType,
            endpoint: toolWizardForm.serviceEndpoint.trim(),
            method: toolWizardForm.serviceMethod as 'GET' | 'POST',
            payloadTemplate: payloadTemplate ?? undefined,
            responsePath: toolWizardForm.responsePath.trim() || undefined,
        }

    const toolMeta: DynamicToolMeta = {
        id: generateToolId('dynamic'),
        name,
        category,
        description,
        tags: [...toolWizardForm.tags],
        paramsSchema,
        invoke: invokeConfig,
        resultType: toolWizardForm.resultType,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
    }

    toolWizardSubmitting.value = true
    try {
        toolRegistry.registerTool(activeUserId, toolMeta)
        saveWizardDraft()
        emit('servicePublished')
        await logToolPublish(name, description)
        message.success('工具发布成功，可前往动态分析页面使用')
        toolWizardVisible.value = false
    } catch (error) {
        console.error('注册工具失败:', error)
        message.error('注册工具失败，请重试')
    } finally {
        toolWizardSubmitting.value = false
    }
}

onMounted(async () => {
    await refreshIsToolFlag()
    try {
        const script = await getScript({
            projectId: props.projectId,
            userId: props.userId,
        })
        if (script) {
            code.value = script
        }
    } catch (error) {
        console.error('加载代码失败:', error)
        message.error('加载代码失败，请检查后端服务是否运行')
    }

    try {
        const result = await projectOperating({
            projectId: props.projectId,
            userId: props.userId,
            action: 'open',
        })

        if (result.status === 1) {
            message.success('项目启动成功')
        } else {
            message.error('启动失败，请刷新页面或联系管理员')
        }
    } catch (error) {
        console.error('项目启动失败:', error)
        message.error('项目启动失败，请检查后端服务')
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

/* 确保 Codemirror 容器和内部滚动条样式正确 */
:deep(.cm-scroller) {
    overflow-x: hidden !important;
    background-color: #161b22;
    /* Codemirror 背景色 */
}

:deep(.cm-editor) {
    background-color: #161b22;
    /* Codemirror 整体背景色 */
    height: 100%;
    border-radius: 0 !important;
}

:deep(.cm-content) {
    caret-color: #58a6ff;
    /* 光标颜色 */
    color: #c9d1d9;
    /* 代码文本颜色 */
}

:deep(.cm-gutters) {
    background-color: #161b22;
    /* 行号背景 */
    border-right: 1px solid #21262d;
    /* 行号分隔线 */
}

:deep(.cm-lineNumbers .cm-gutterElement) {
    color: #8b949e;
    /* 行号颜色 */
}

/* 代码编辑器容器 */
.code-editor {
    /* 适配 h-[90%] w-full，移除冲突的亮色背景和字体 */
    @apply h-[calc(100%-44px)] w-full overflow-x-hidden overflow-y-auto rounded-lg font-sans text-sm;
    background-color: #161b22;
    border: 1px solid #21262d;
    /* 增加轻微边框 */
}

:deep(.cm-selectionBackground) {
    background-color: #3b5074 !important;
    /* 修改为你想要的颜色 */
}

/* 兼容性补充：针对激活的行（如果启用了 line selection） */
:deep(.cm-activeLine) {
    background-color: #1a2a47 !important;
    /* 修改为活动行的背景色，通常比编辑器背景稍亮或稍暗 */
}

/* 兼容性补充：针对被选中的文本（如果 cm-selectionBackground 不生效） */
:deep(::selection),
:deep(.cm-content ::selection) {
    background-color: #3b5074 !important;
    /* Fallback for native selection */
    color: #ffffff;
    /* 可选：设置选中时的文本颜色 */
}

/* 顶部工具栏按钮样式 */
.toolItem {
    font-size: 14px;
    padding: 0 12px;
    color: #c9d1d9;
    /* 默认文本为柔和浅色 */
}

/* 运行/保存/依赖 hover 状态 */
.btHover:hover {
    color: #58a6ff;
    /* 亮蓝色强调色 */
}

/* 覆盖 Element Plus 输入框/选择框，以适应暗黑主题 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-textarea__inner) {
    background-color: #21262d !important;
    /* 组件背景深于 Pane 背景 */
    box-shadow: none !important;
}

:deep(.el-input__inner),
:deep(.el-select__placeholder),
:deep(.el-textarea__inner) {
    color: #c9d1d9 !important;
    /* 文本颜色 */
}

/* 解决 Tool Wizard 中，Element Plus Dialog 的暗黑主题问题 */
:deep(.el-dialog) {
    background-color: #161b22 !important;
    color: #c9d1d9 !important;
    border: 1px solid #21262d;

    .el-dialog__header,
    .el-dialog__title {
        color: #c9d1d9 !important;
    }

    .el-dialog__footer {
        border-top: 1px solid #21262d;
    }
}

:deep(.el-form-item__label) {
    color: #c9d1d9 !important;
}

:deep(.el-divider__text) {
    color: #58a6ff !important;
    background-color: #161b22 !important;
}

/* 修正 Tool Wizard 中的服务状态框 */
:deep(.border-gray-400) {
    border-color: #30363d !important;
}

:deep(.bg-gray-100\/10) {
    background-color: #21262d !important;
    color: #8b949e !important;
}

/* 修正 Tool Wizard 中的参数配置框 */
:deep(.border-gray-600\/60) {
    border-color: #30363d !important;
}

/* 修正表格内部的颜色 (依赖管理) */
:deep(.el-table) {
    background-color: #161b22 !important;
    color: #c9d1d9;

    .el-table__header-wrapper,
    .el-table__body tr {
        background-color: #161b22 !important;
    }

    .el-table__header-wrapper th {
        color: #c9d1d9 !important;
    }
}
</style>
