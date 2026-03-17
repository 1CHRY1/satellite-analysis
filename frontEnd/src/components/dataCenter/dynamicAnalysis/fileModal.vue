<template>
    <a-modal :title="modalTitle" :visible="visible" @cancel="handleCancel" @ok="handleOk" :width="600"
        :ok-button-props="okButtonProps">
        <a-spin :spinning="loading">

            <a-alert v-if="!hasSelection" :message="selectTip" type="info" show-icon style="margin-bottom: 16px" />
            <a-alert v-else :message="selectionSummary"
                :type="mode === 'file' && isSelectionInvalid ? 'warning' : 'success'" show-icon
                style="margin-bottom: 16px" />



            <div class="file-tree-container">
                <div class="data-source-selector" style="margin: 8px; ">
                    <template v-if="mode !== 'output'">
                        <span style="margin-left: 4px; margin-right: 8px; color: white;">当前数据源:</span>
                        <a-radio-group :value="currentDataSource"
                            @change="e => handleSourceChange(e.target.value as DataSource)" button-style="solid"
                            size="small">
                            <a-radio-button value="user">我的空间</a-radio-button>
                            <a-radio-button value="platform">平台数据</a-radio-button>
                        </a-radio-group>
                    </template>
                </div>
                <a-tree :tree-data="filteredTreeData" :selected-keys="mode !== 'file' || !multiple ? selectedKeys : []"
                    @select="onSelect" :checked-keys="mode === 'file' && multiple ? checkedKeys : []"
                    :checkable="mode === 'file' && multiple" @check="onCheck" :auto-expand-parent="true" block-node
                    :default-expanded-keys="filteredTreeData.length > 0 ? [filteredTreeData[0].key] : []">
                    <template #title="{ title, dir, iconType, sizeText, lastModified }">
                        <div class="tree-node-item">
                            <Icon :type="iconType" :style="{ color: dir ? '#fadb14' : '#1890ff' }" />
                            <span class="file-name">{{ title }}</span>
                            <span class="file-meta" v-if="!dir && currentDataSource === 'user' && !loading">({{ sizeText
                                }})</span>
                            <span class="file-meta-date" v-if="currentDataSource === 'user' && !loading">修改时间: {{ new
                                Date(lastModified).toLocaleString() }}</span>
                        </div>
                    </template>
                </a-tree>
            </div>

            <a-form v-if="mode === 'output'" layout="vertical" style="margin-top: 16px">
                <a-form-item label="输出文件名">
                    <a-input v-model:value="outputFileName" placeholder="请输入输出文件名 (例如: result.tif)"
                        :disabled="!selectedNode || !selectedNode.dir" />
                </a-form-item>
            </a-form>

        </a-spin>
    </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, h } from 'vue';
// 确保这些路径和类型定义文件存在于您的项目中
import { convertToFileTree } from '@/util/file/file-helper';
import type {
    SelectorMode,
    FileTreeData,
    RawFileNode,
    SelectionResult,
} from '@/type/file';
import type { TreeProps } from 'ant-design-vue';

// 导入所有需要的图标
import {
    FileImageFilled,
    VideoCameraFilled,
    FileTextFilled,
    FolderFilled,
    FileUnknownFilled,
    FilePdfFilled,
    FileWordFilled,
    FileExcelFilled,
    FileZipFilled,
} from '@ant-design/icons-vue';
import { platformDataFile } from './shared';
import { getDataFile } from '@/api/http/user/minio.api';
import { useUserStore } from '@/store';
const userStore = useUserStore()
// --- 图标映射和封装 ---
const iconMap: { [key: string]: any } = {
    'file-image-filled': FileImageFilled,
    'file-video-filled': VideoCameraFilled,
    'file-text-filled': FileTextFilled,
    'folder-filled': FolderFilled,
    'file-unknown-filled': FileUnknownFilled,
    'file-pdf-filled': FilePdfFilled,
    'file-word-filled': FileWordFilled,
    'file-excel-filled': FileExcelFilled,
    'file-zip-filled': FileZipFilled,
};

// 封装一个 Icon 组件用于模板渲染
const Icon = (props: { type: string; style?: any }) => {
    const IconComponent = iconMap[props.type] || FileTextFilled;
    return h(IconComponent, { style: props.style });
};

// --- Props & Emits ---
interface Props {
    visible: boolean;
    mode: SelectorMode;
    multiple?: boolean;
    index: number;
}
const props = defineProps<Props>();
const emit = defineEmits<{
    (e: 'update:visible', visible: boolean): void;
    (e: 'confirm', result: SelectionResult): void;
}>();

// 数据源类型
type DataSource = 'user' | 'platform';

// --- 状态管理 ---
const loading = ref(false);
const treeData = ref<FileTreeData[]>([]);
const currentDataSource = ref<DataSource>('user'); // 默认选中用户空间

// 单选状态：用于单文件、选择文件夹、输出模式
const selectedKeys = ref<TreeProps['selectedKeys']>([]);
const selectedNode = ref<FileTreeData | null>(null);

// 多选状态：仅用于多文件选择
const checkedKeys = ref<TreeProps['checkedKeys']>([]);
const checkedNodes = ref<FileTreeData[]>([]); // 存储多选勾选的节点数据

const outputFileName = ref('');

// --- 辅助方法: 递归过滤文件节点 ---
const filterTree = (nodes: FileTreeData[]): FileTreeData[] => {
    // 文件夹/输出模式下，不渲染文件
    const shouldFilterFiles = props.mode === 'folder' || props.mode === 'output';

    return nodes.map(node => {
        // 如果是文件且需要过滤，则返回 null (表示排除)
        if (!node.dir && shouldFilterFiles) {
            return null;
        }

        // 递归处理子节点
        let filteredChildren: FileTreeData[] | undefined = undefined;
        if (node.children && node.children.length > 0) {
            const children = filterTree(node.children).filter((n): n is FileTreeData => !!n);
            if (children.length > 0) {
                filteredChildren = children;
            }
        }

        // 返回一个新节点对象 (保持原有的 key 和 title 等属性)
        return {
            ...node,
            children: filteredChildren,
        } as FileTreeData;
    }).filter((n): n is FileTreeData => !!n); // 移除被过滤的节点
};
// 优化后的文件树数据，根据 mode 过滤文件
const filteredTreeData = computed(() => {
    return filterTree(treeData.value);
});

// --- 辅助方法: 查找节点 ---
const findNodeByPath = (key: string, nodes: FileTreeData[]): FileTreeData | undefined => {
    for (const node of nodes) {
        if (node.key === key) return node;
        if (node.children) {
            const found = findNodeByPath(key, node.children);
            if (found) return found;
        }
    }
    return undefined;
};

// --- Computed 状态 ---

// 多选模式下，实际选中的（非文件夹）文件
const selectedFiles = computed(() => {
    if (props.mode === 'file' && props.multiple) {
        return checkedNodes.value.filter(node => !node.dir);
    }
    return [];
});

// 多选模式下，选中的项里是否包含文件夹
const isSelectionInvalid = computed(() => {
    if (props.mode === 'file' && props.multiple) {
        return checkedNodes.value.some(node => node.dir);
    }
    return false;
});

// 是否有任何选中项 (用于 alert 提示)
const hasSelection = computed(() => {
    if (props.mode === 'file' && props.multiple) {
        return selectedFiles.value.length > 0;
    }
    // ⚠️ 修复：检查 selectedNode 是否存在，并且确保它满足当前 mode 的要求
    if (!selectedNode.value) return false;

    if (props.mode === 'file' && !props.multiple) {
        // 单文件模式：必须是非文件夹
        return !selectedNode.value.dir;
    }
    if (props.mode === 'folder' || props.mode === 'output') {
        // 文件夹/输出模式：必须是文件夹
        return selectedNode.value.dir;
    }

    return !!selectedNode.value;
});

// 选中项总结提示
const selectionSummary = computed(() => {
    if (props.mode === 'file' && props.multiple) {
        const fileCount = selectedFiles.value.length;
        if (fileCount === 0) return selectTip.value;

        const invalidCount = checkedNodes.value.filter(node => node.dir).length;
        if (invalidCount > 0) {
            return `已选择 ${fileCount} 个文件，并忽略了 ${invalidCount} 个文件夹。`;
        }
        return `已选择 ${fileCount} 个文件。`;
    } else if (selectedNode.value) {
        return `已选择: ${selectedNode.value.path}`;
    }
    return selectTip.value;
});

const modalTitle = computed(() => {
    switch (props.mode) {
        case 'file':
            return props.multiple ? '选择文件' : '选择文件';
        case 'folder':
            return '选择文件夹';
        case 'output':
            return '选择输出路径';
        default:
            return '文件选择器';
    }
});

const selectTip = computed(() => {
    if (props.mode === 'file') {
        return props.multiple ? '请在下方目录中选择文件（支持多选）' : '请在下方目录中选择文件';
    }
    return '请在下方目录中选择文件夹';
});

const outputFolderPath = computed<string>(() => {
    if (props.mode === 'output' && selectedNode.value && selectedNode.value.dir) {
        return selectedNode.value.path;
    }
    return '';
});

// OK 按钮的禁用状态
const okButtonProps = computed(() => {
    let disabled = true;

    if (props.mode === 'file') {
        if (props.multiple) {
            // 多选文件模式：必须至少选中一个文件
            disabled = selectedFiles.value.length === 0;
        } else {
            // 单选文件模式：必须选中一个文件且不能是文件夹
            disabled = !selectedNode.value || selectedNode.value.dir;
        }
    } else if (props.mode === 'folder') {
        // 选择文件夹模式：必须选中一个文件夹
        disabled = !selectedNode.value || !selectedNode.value.dir;
    } else if (props.mode === 'output') {
        // 输出模式：必须选中一个文件夹且文件名不为空
        disabled = !selectedNode.value || !selectedNode.value.dir || !outputFileName.value.trim();
    }
    return { disabled };
});

// --- Watchers ---
// 首次打开时，根据模式确定初始数据源
watch(
    () => props.visible,
    (val) => {
        if (val) {
            // 🌟 核心修改：如果是输出模式，强制为用户个人空间
            if (props.mode === 'output') {
                currentDataSource.value = 'user';
            } else {
                // 如果是输入模式，默认使用用户个人空间
                currentDataSource.value = 'user';
            }

            // 首次打开或模式切换，确保加载数据
            fetchFileData();

            // 重置所有选择状态 (保持不变)
            selectedKeys.value = [];
            selectedNode.value = null;
            checkedKeys.value = [];
            checkedNodes.value = [];
            outputFileName.value = '';
        }
    },
);
// --- 模拟数据加载函数 ---
const getUserDataFile = async (): Promise<{ status: number; message: string; data: RawFileNode[] }> => {
    // ** 请在这里替换为您的真实异步请求 **
    let param = {
        userId: userStore.user.id,
        filePath: ""
    }
    let res = await getDataFile(param)
    res.data = [res.data] as RawFileNode[]
    return res
};

const fetchFileData = async () => {
    loading.value = true;
    try {
        let response: { status: number; message: string; data: RawFileNode[] };

        if (currentDataSource.value === 'platform') {
            response = { status: 1, message: '', data: platformDataFile.value };
        } else { // 'user'
            // 沿用您现有的 getUserData 或 getDataFile 函数
            response = await getUserDataFile();
        }

        if (response.status === 1 && response.data) {
            treeData.value = convertToFileTree(response.data);
            console.log(treeData.value)
        }
    } catch (error) {
        console.error('获取文件数据失败:', error);
    } finally {
        loading.value = false;
    }
};

// 切换数据源处理函数
const handleSourceChange = (newSource: DataSource) => {
    // 只有当模式是输入（非输出）时，才允许切换数据源
    if (props.mode !== 'output') {
        currentDataSource.value = newSource;

        // 切换数据源时，清空当前选择状态并重新加载数据
        selectedKeys.value = [];
        selectedNode.value = null;
        checkedKeys.value = [];
        checkedNodes.value = [];

        fetchFileData();
    }
};

// --- Methods ---

// 单选事件 (适用于单文件、选择文件夹、选择输出路径)
const onSelect: TreeProps['onSelect'] = (keys, info) => {
    if (props.mode === 'file' && props.multiple) {
        // 多选文件模式下忽略 select 事件
        return;
    }

    // 🌟 关键修复点：检查是否是取消选择
    if (keys.length === 0) {
        selectedKeys.value = [];
        selectedNode.value = null; // 清除选中节点
        return;
    }

    selectedKeys.value = keys;
    const node = info.node.dataRef as FileTreeData;

    // --- 核心逻辑：根据模式限制选择类型 ---

    if (props.mode === 'file' && !props.multiple) {
        // 1. 单文件选择：必须是非文件夹
        if (node.dir) {
            // 阻止选择文件夹，并清空当前选择
            selectedNode.value = null;
            selectedKeys.value = [];
            return;
        }
    } else if (props.mode === 'folder' || props.mode === 'output') {
        // 3. & 4. 文件夹/输出模式：必须是文件夹
        if (!node.dir) {
            // 阻止选择文件，并清空当前选择
            selectedNode.value = null;
            selectedKeys.value = [];
            return;
        }
    }

    // 只有通过上述检查的节点才被选中
    selectedNode.value = node;
};

// 勾选事件 (仅适用于多选文件)
const onCheck: TreeProps['onCheck'] = (checkedKeysOrEvent, info) => {
    if (props.mode !== 'file' || !props.multiple) {
        // 非多选文件模式下忽略 check 事件
        return;
    }

    // 兼容 Antdv checkedKeys 的两种类型
    const checkedKeysList = Array.isArray(checkedKeysOrEvent)
        ? checkedKeysOrEvent : checkedKeysOrEvent.checked;

    checkedKeys.value = checkedKeysList;

    // 提取对应的节点数据 (包含文件夹)，用于判断有效文件数量
    const allNodes = checkedKeysList
        .map(key => findNodeByPath(key as string, treeData.value))
        .filter((n): n is FileTreeData => !!n);
    checkedNodes.value = allNodes;
};

const handleCancel = () => {
    emit('update:visible', false);
};

const handleOk = () => {
    if (okButtonProps.value.disabled) return;

    let result: SelectionResult | null = null;

    if (props.mode === 'file') {
        if (props.multiple) {
            // 多选模式：返回路径/名称数组
            const paths = selectedFiles.value.map(node => ({
                path: node.path,
                name: node.title,
            }));
            result = { type: 'file', paths, index: props.index };
        } else if (selectedNode.value) {
            // 单选模式：返回单个路径/名称
            result = { type: 'file', path: selectedNode.value.path, name: selectedNode.value.title, index: props.index, multiBandData: selectedNode.value?.multiBandData };
        }
    } else if (props.mode === 'folder' && selectedNode.value) {
        // 选择文件夹模式
        result = { type: 'folder', path: selectedNode.value.path, name: selectedNode.value.title, index: props.index };
    } else if (props.mode === 'output' && selectedNode.value) {
        // 输出模式
        const fileName = outputFileName.value.trim();
        result = {
            type: 'output',
            folderPath: selectedNode.value.path,
            fileName: fileName,
            fullPath: selectedNode.value.path + fileName,
            index: props.index
        };
    }
    console.log(result)
    if (result) {
        emit('confirm', result);
        emit('update:visible', false);
    }
};
</script>

<style scoped>
.file-tree-container {
    max-height: 400px;
    overflow-y: auto;
    border: 1px solid rgb(21, 50, 91);
    border-radius: 4px;
    padding: 8px;
}

/* 优化树节点样式 */
.tree-node-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 2px 0;
}

.file-name {
    flex-grow: 1;
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.file-meta {
    color: #8c8c8c;
    font-size: 12px;
    margin-right: 16px;
    flex-shrink: 0;
}

.file-meta-date {
    color: #bfbfbf;
    font-size: 10px;
    flex-shrink: 0;
}
</style>