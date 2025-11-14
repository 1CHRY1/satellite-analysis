<template>
    <a-modal v-model:open="innerVisible" :title="methodItem?.name" @cancel="close" @ok="handleOk"
        :confirm-loading="isExecuting" :width="1200" wrapClassName="wide-modal">

        <a-spin :spinning="!isReady" tip="正在加载方法参数...">
            <a-row :gutter="24">

                <a-col :span="15">
                    <a-divider orientation="left">参数配置</a-divider>
                    <a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical" class="param-form">
                        <a-form-item class="form-item" :name="'val' + index"
                            v-for="(param, index) in methodItem?.params" :key="index">
                            <template #label>
                                <div class="label-content">
                                    <a-tooltip :title="param.Description">
                                        <span style="font-weight: 600;">{{ param.Description }}</span>
                                    </a-tooltip>
                                    <a-tag :color="getTypeTagColor(param.parameter_type)" style="margin-left: 8px;">
                                        {{ formatJson(param.parameter_type) }}
                                    </a-tag>
                                </div>
                                <div style="font-size: 12px; color: #8c8c8c;">
                                    默认值：{{ param.default_value === null ? 'Null' : param.default_value }}
                                </div>
                            </template>

                            <template
                                v-if="param.parameter_type?.ExistingFile !== undefined || param.parameter_type?.FileList !== undefined">
                                <a-input-group compact style="display: flex;">
                                    <a-input class="file-input-field" :value="formData['val' + index]?.label || ''"
                                        :placeholder="param.parameter_type?.FileList !== undefined ? '请选择多个文件' : '请选择文件'"
                                        @change="updateIsChangedList(index)" readonly />
                                    <a-button type="default"
                                        @click="openModal(index, 'file', param.parameter_type?.FileList !== undefined)">
                                        <template #icon>
                                            <UploadOutlined />
                                        </template>
                                        选择
                                    </a-button>
                                </a-input-group>
                            </template>

                            <template v-else-if="param.parameter_type?.ExistingFileOrFloat !== undefined">
                                <div style="margin-bottom: 8px;">
                                    <a-switch v-model:checked="switchStates[index]" checked-children="数值 (Number)"
                                        un-checked-children="文件 (File)" @change="handleSwitchChange(index)" />
                                </div>

                                <a-input-group compact style="display: flex;" v-if="!switchStates[index]">
                                    <a-input class="file-input-field" :value="formData['val' + index]?.label || ''"
                                        placeholder="请选择文件" @change="updateIsChangedList(index)" readonly />
                                    <a-button type="default" @click="openModal(index, 'file', false)">
                                        <template #icon>
                                            <UploadOutlined />
                                        </template>
                                        选择
                                    </a-button>
                                </a-input-group>

                                <a-input v-else v-model:value="formData['val' + index]" :placeholder="param.description"
                                    style="width: 40%" type="number" @change="updateIsChangedList(index)" />
                            </template>

                            <template v-else-if="
                                param.parameter_type?.NewFile !== undefined
                            ">
                                <a-input-group compact style="display: flex;">
                                    <a-input class="file-input-field" :value="formData['val' + index]?.label || ''"
                                        placeholder="请选择输出路径" @change="updateIsChangedList(index)" readonly />
                                    <a-button type="default" @click="openModal(index, 'output', false)">
                                        <template #icon>
                                            <FolderOpenOutlined />
                                        </template>
                                        选择
                                    </a-button>
                                </a-input-group>
                            </template>

                            <template v-else-if="param.parameter_type === 'Directory'">
                                <a-input-group compact style="display: flex;">
                                    <a-input class="file-input-field" :value="formData['val' + index]?.label"
                                        placeholder="请选择输出文件夹" @change="updateIsChangedList(index)" readonly />
                                    <a-button type="default" @click="openModal(index, 'folder', false)">
                                        <template #icon>
                                            <FolderOpenOutlined />
                                        </template>
                                        选择
                                    </a-button>
                                </a-input-group>
                            </template>

                            <template v-else-if="
                                param.parameter_type?.VectorAttributeField !== undefined ||
                                param.parameter_type === 'String' ||
                                param.parameter_type === 'StringOrNumber'
                            ">
                                <a-input v-model:value="formData['val' + index]" :placeholder="param.description"
                                    style="width: 100%" @change="updateIsChangedList(index)" />
                            </template>

                            <template v-else-if="param.parameter_type === 'Boolean'">
                                <a-switch v-model:checked="formData['val' + index]" checked-children="是"
                                    un-checked-children="否" @change="updateIsChangedList(index)" />
                            </template>

                            <template v-else-if="
                                param.parameter_type === 'Float' ||
                                param.parameter_type === 'Integer'
                            ">
                                <a-input-number v-model:value="formData['val' + index]" :placeholder="param.description"
                                    :step="param.parameter_type === 'Float' ? 0.1 : 1" style="width: 40%"
                                    @change="updateIsChangedList(index)" />
                            </template>

                            <template v-else-if="param.parameter_type?.OptionList !== undefined">
                                <a-select v-model:value="formData['val' + index]" style="width: 60%"
                                    @change="updateIsChangedList(index)" :placeholder="param.description">
                                    <a-select-option v-for="option in param.parameter_type.OptionList" :key="option"
                                        :value="option">
                                        {{ option }}
                                    </a-select-option>
                                </a-select>
                            </template>

                            <template v-else>
                                <a-textarea v-model:value="formData['val' + index]" :placeholder="param.description"
                                    :rows="3" style="width: 100%" />
                            </template>
                        </a-form-item>
                        <a-typography-paragraph style="margin: 0;">
                            <blockquote style="margin: 0; padding-left: 10px; border-left: 4px solid #1890ff; ">
                                方法调用后，您可以在「历史记录」中查看执行进度和结果
                            </blockquote>
                        </a-typography-paragraph>
                    </a-form>
                </a-col>

                <a-col :span="9">
                    <a-divider orientation="left">方法详细信息</a-divider>
                    <a-card :title="methodItem?.name" size="small" style="min-height: 500px;"
                        :headStyle="{ borderBottom: '1px solid #f0f0f0' }"
                        :bodyStyle="{ padding: '12px', maxHeight: '450px', overflowY: 'auto' }">
                        <p v-if="methodItem?.longDesc" style="white-space: pre-wrap; margin: 0;">
                            {{ methodItem.longDesc }}
                        </p>
                        <a-empty v-else description="暂无详细描述信息" :image="simpleImage" />
                    </a-card>
                </a-col>
            </a-row>
        </a-spin>

        <template #footer>
            <a-button key="back" @click="close">取消</a-button>
            <a-button key="submit" type="primary" :loading="isExecuting" @click="handleOk">
                <template #icon>
                    <RocketOutlined />
                </template>
                执行方法
            </a-button>
        </template>
    </a-modal>
    <file-modal :visible="modalVisible" :mode="currentMode" :multiple="isMultiple" :index="currentIndex"
        @update:visible="modalVisible = $event" @confirm="handleConfirm" />
</template>

<script setup lang="ts">
import { ref, watch, reactive } from "vue"
import { UploadOutlined, FolderOpenOutlined, RocketOutlined } from '@ant-design/icons-vue';
import { Empty, message, type FormInstance } from 'ant-design-vue';
import fileModal from "./fileModal.vue";

// 导入 Antdv 组件
import {
    Modal as AModal,
    Form as AForm,
    FormItem as AFormItem,
    Input as AInput,
    InputGroup as AInputGroup,
    InputNumber as AInputNumber, // 引入数字输入框
    Button as AButton,
    Switch as ASwitch,
    Select as ASelect,
    SelectOption as ASelectOption,
    Textarea as ATextarea,
    Row as ARow, // 引入栅格布局
    Col as ACol,
    Divider as ADivider,
    Card as ACard,
    Tag as ATag,
    Tooltip as ATooltip,
    Spin as ASpin, // 引入加载动画
} from 'ant-design-vue';
import type { SelectorMode } from "@/type/file";
import type { SelectionResult } from "@/type/file";
import { ezStore } from "@/store";

const minioEndPoint = ezStore.get('conf')['minioIpAndPort']
// Antd Empty 简化图片
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;

// --- Props & Emits & 基础状态管理 ---

const props = defineProps({
    modelValue: Boolean,
    methodItem: Object, // 对应于原 Element 代码中的 method 对象，现在假设包含 longDesc
})

const emit = defineEmits(["update:modelValue", "invokeMethod"])

// 创建一个内部可写变量（避免直接改 props）
const innerVisible = ref(props.modelValue)
const isExecuting = ref(false)
const isReady = ref(false)

// 表单核心数据
const formRef = ref<FormInstance | null>(null)
const formData = reactive<Record<string, any>>({})
const formRules = reactive({})
const switchStates = reactive({})

// 同步父子状态
watch(
    () => props.modelValue,
    (val) => {
        innerVisible.value = val;
        if (val) {
            isReady.value = false; // 重新打开时设置加载中
            initFormData();
            setTimeout(() => {
                formRef.value?.clearValidate();
            }, 100);
        }
    }
)

function close() {
    emit("update:modelValue", false)
}

// --- 辅助函数 ---

function getTypeTagColor(type) {
    const typeStr = formatJson(type);
    switch (typeStr) {
        case 'ExistingFile':
        case 'FileList':
            return 'green';
        case 'NewFile':
        case 'Directory':
            return 'blue';
        case 'Float':
        case 'Integer':
            return 'orange';
        case 'Boolean':
            return 'cyan';
        case 'String':
            return 'purple';
        default:
            return 'default';
    }
}

/**
* 根据 methodItem.params 的 Optional 字段动态生成 Antdv 表单校验规则。
*/
async function generateFormRules() {
    if (!props.methodItem?.params) {
        Object.assign(formRules, {});
        return;
    }

    // 使用 reduce 动态生成规则对象
    const rules = props.methodItem.params.reduce((acc, param, index) => {
        // 如果 Optional 为 false 或未定义（默认必填）
        if (!param.Optional) {
            const key = 'val' + index;
            acc[key] = [
                {
                    required: true,
                    // 使用 Name 字段作为提示信息
                    message: `${param.Name || param.Description} 是必填项`,
                    trigger: ['change', 'blur']
                }
            ];
        }
        return acc;
    }, {});

    // 将生成的规则赋给响应式对象
    Object.assign(formRules, rules);
    console.log("生成的表单校验规则:", formRules);
}

/**
 * 初始化表单数据 (formData) 并调用规则生成。
 */
function initFormData() {
    if (!props.methodItem?.params) {
        // ... 重置逻辑
        isReady.value = true;
        return;
    }

    const initialData = {};
    const initialSwitchStates = {};

    props.methodItem.params.forEach((param, index) => {
        const key = 'val' + index;
        let defaultValue = param.default_value !== null ? param.default_value : null;

        // 根据类型设置初始值（保持上一版逻辑）
        if (param.parameter_type === 'Boolean') {
            defaultValue = param.default_value !== null ? !!param.default_value : false;
        } else if (param.parameter_type === 'Float' || param.parameter_type === 'Integer') {
            defaultValue = param.default_value !== null ? Number(param.default_value) : null;
        } else if (param.parameter_type?.OptionList !== undefined) {
            defaultValue = param.parameter_type.OptionList[0] || null;
        }

        initialData[key] = defaultValue;

        // 初始化 ExistingFileOrFloat 的开关状态
        if (param.parameter_type?.ExistingFileOrFloat !== undefined) {
            initialSwitchStates[index] = false;
        }
    });

    // 清空现有数据
    Object.keys(formData).forEach(key => delete formData[key]);
    Object.keys(switchStates).forEach(key => delete switchStates[key]);

    // 赋值给响应式对象
    Object.assign(formData, initialData);
    Object.assign(switchStates, initialSwitchStates);

    // 调用新的规则生成函数
    generateFormRules();

    // 确保数据设置完成后，再显示表单
    isReady.value = true;
}

// ExistingFileOrFloat 开关变化处理
function handleSwitchChange(index) {
    const key = 'val' + index;
    formData[key] = null;
    formRef.value?.clearValidate(key);
}

// --- 占位函数（保持一致） ---

async function handleOk() {
    if (!formRef.value) return;
    isExecuting.value = true;
    try {
        await formRef.value.validate();

        console.log("表单验证通过，准备执行方法:", formData);

        emit("invokeMethod", {
            method: props.methodItem,
            formData: formData,
        });

        close();

    } catch (error) {
        console.error("表单验证失败:", error);
    } finally {
        isExecuting.value = false;
    }
}

function updateIsChangedList(index) {
    console.log(`参数 val${index} 已修改`);
}

function formatJson(type) {
    if (typeof type === 'object' && type !== null) {
        return Object.keys(type)[0] || 'Unknown';
    }
    return type;
}

// --- 状态 ---
const modalVisible = ref(false);
const currentMode = ref<SelectorMode>('file');
const currentIndex = ref<number>(0)
const isMultiple = ref(false); // 新增状态

// --- 方法 ---
const openModal = (index: number, mode: SelectorMode, multiple: boolean) => {
    currentIndex.value = index
    currentMode.value = mode;
    isMultiple.value = multiple;
    modalVisible.value = true;
};

const handleConfirm = (selection: SelectionResult) => {
    console.log('选择结果:', selection);
    let result = { label: '', value: '' } as { label: string, value: any }
    switch (selection.type) {
        case "file":
            if (selection.paths !== undefined) {
                result.label = selection.paths?.map(path => path.name).join(', ')
                result.value = selection.paths?.map(path => `${minioEndPoint}/${path.path}`.replace(/\/$/, ""))
            } else if (selection.path !== undefined) {
                result.label = selection.name as string
                result.value = `${minioEndPoint}/${selection.path}`.replace(/\/$/, "")
            }
            break
        case "folder":
            result.label = selection.path as string
            result.value = `${minioEndPoint}/${selection.path}`.replace(/\/$/, "")
            break
        case "output":
            result.label = selection.fullPath as string
            result.value = `${minioEndPoint}/${selection.fullPath}`.replace(/\/$/, "")
            break
    }
    formData['val' + selection.index] = result
};

initFormData();

</script>

<style>
/* 调整模态框宽度，以便容纳左右两栏 */
.wide-modal .ant-modal {
    max-width: 90%;
}

/* 调整 Modal Body 间距 */
.wide-modal .ant-modal-body {
    padding-top: 0;
    padding-bottom: 0;
}
</style>

<style scoped>
/* 标题和标签的样式 */
.label-content {
    display: flex;
    align-items: center;
    line-height: 1;
    margin-bottom: 2px;
}

/* 调整表单项间距 */
.ant-form-item {
    margin-bottom: 16px;
}

/* 隐藏 Spin 内部的边框，让内容更贴合 */
.ant-spin-nested-loading>div>.ant-spin {
    max-height: none !important;
}

/* 确保描述卡片的滚动条美观 */
.ant-card-body {
    scrollbar-width: thin;
    /* Firefox */
    scrollbar-color: #d9d9d9 transparent;
    /* Firefox */
}

.ant-card-body::-webkit-scrollbar {
    width: 6px;
}

.ant-card-body::-webkit-scrollbar-thumb {
    background-color: #d9d9d9;
    border-radius: 3px;
}
</style>