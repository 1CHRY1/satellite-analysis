<template>
    <div>
        <div class="flex h-[44px] w-full justify-between bg-[#161b22]">
            <div class="mx-2.5 my-1.5 flex w-fit items-center rounded bg-[#181d25] px-2 text-[14px] shadow-md">
                <div @click="handleClick('data')"
                    class="mr-2 cursor-pointer border-r-1 border-dashed border-gray-500 pr-2"
                    :class="activeDataBase === 'data' ? 'text-[#1479d7]' : 'text-[#818999]'">
                    容器数据列表
                </div>
                <div @click="handleClick('output')" 
                    class="mr-2 cursor-pointer border-r-1 border-dashed border-gray-500 pr-2"
                    :class="activeDataBase === 'output' ? 'text-[#1479d7]' : 'text-[#818999]'">
                    输出数据列表
                </div>
                <div @click="handleClick('user')" class="cursor-pointer"
                    :class="activeDataBase === 'user' ? 'text-[#1479d7]' : 'text-[#818999]'">
                    用户数据
                </div>
            </div>

            <div class="flex">
                <div v-if="activeDataBase === 'user' && selectedUserFiles.length > 0" @click="importSelectedData"
                    class="mr-2.5 my-1.5 flex w-fit cursor-pointer items-center rounded bg-[#238636] text-[#c9d1d9] px-2 text-[14px] shadow-md"
                    title="导入选中数据到代码编辑器">
                    <FileInput :size="16" class="text-primary" />
                    <span class="ml-1">导入({{ selectedUserFiles.length }})</span>
                </div>
                <div @click="refreshTableData"
                    class="mr-2.5 my-1.5 flex w-fit cursor-pointer items-center rounded bg-[#1f3d88] text-[#c9d1d9] px-2 text-[14px] shadow-md"
                    title="刷新数据">
                    <RefreshCcw :size="16" class="text-primary" />
                </div>
                <div @click="triggerFileSelect"
                    class="mr-2.5 my-1.5 flex w-fit cursor-pointer items-center rounded bg-[#1f3d88] text-[#c9d1d9] px-2 text-[14px] shadow-md"
                    title="上传geojson">
                    <Upload :size="16" class="text-primary" />
                </div>

                <!-- 隐藏文件选择框 -->
                <input ref="fileInput" type="file" accept=".json,.txt,.geojson" @change="uploadFile" class="hidden" />

                <!-- <div @click=""
                    class="mr-2.5 my-1.5 flex w-fit cursor-pointer items-center rounded bg-[#eaeaea] px-2 text-[14px] text-[#818999] shadow-md">
                    <FileUp :size="16" class="text-primary" />
                </div> -->
            </div>

        </div>
        <div class="h-[calc(100%-44px)] max-w-full overflow-x-auto bg-[#161b22]">
            <!-- 容器数据和输出数据列表 -->
            <table v-if="activeDataBase !== 'user'" class="w-full table-auto border-collapse">
                <thead>
                    <tr class="sticky top-0 bg-[#06162a] text-[#c9d1d9] border-[#262b32]">
                        <th class="w-auto min-w-[100px] px-4 py-2 text-left">文件名</th>
                        <th class="w-[100px] px-4 py-2 text-left">更新时间</th>
                        <th class="w-[60px] px-4 py-2 text-left">文件大小</th>
                        <th class="w-[60px] px-4 py-2 text-left">预览</th>
                    </tr>
                </thead>
                <tbody>
                    <tr class="text-[#818999] relative" v-for="(item, index) in tableData" :key="index">
                        <td class="ml-4 flex cursor-pointer py-2 overflow-hidden whitespace-nowrap text-ellipsis"
                           :title="item.fileName">
                            <div class="mr-1 flex h-4 w-4 items-center justify-center">
                                <img :src="'/filesImg/' + item.fileType + '.png'" alt="" />
                            </div>
                            {{ item.fileName }}
                        </td>
                        <td class="cursor-pointer px-4 py-2">
                            {{ formatTime(item.updateTime, 'minutes', -8) }}
                        </td>
                        <td class="cursor-pointer px-4 py-2" >
                            {{ sizeConversion(item.fileSize) }}
                        </td>
                        <td class="cursor-pointer relative px-4 py-2">
                            <!-- JSON 图标 -->
                            <span v-if="item.fileName.split('.')[1] === 'json'" @click="handlePreview(item)">
                                <ChartColumn :size="16" class="text-red-400 hover:text-red-600" />
                            </span>

                            <!-- Eye 图标：点击展开弹出框 -->
                            <span v-else-if="item.view" class="text-green-400 hover:text-green-600"
                                @click="handleUnPreview(item)">
                                <Eye :size="16" />
                            </span>

                            <!-- EyeOff 图标 -->
                            <span v-else class="text-red-400 hover:text-red-600" @click="handlePreview(item)">
                                <EyeOff :size="16" />
                            </span>


                        </td>
                    </tr>
                    <tr v-if="tableData.length === 0">
                        <td colspan="4" class="py-10 text-center !text-base text-gray-500">
                            <div class="flex h-full flex-col items-center justify-center">
                                <CircleSlash :size="40" class="mb-2 text-gray-500" />
                                <span class="!text-base text-gray-500">暂无数据</span>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>

            <!-- 用户数据列表 -->
            <div v-else class="flex flex-col h-full">
                <!-- 路径导航 -->
                <div v-if="userCurrentPath" class="flex items-center px-4 py-2 bg-[#0d1117] text-[#c9d1d9] text-sm border-b border-[#21262d]">
                    <span @click="goBackUserFolder" class="cursor-pointer hover:text-[#58a6ff] flex items-center mr-2">
                        <ArrowLeft :size="14" class="mr-1" />
                        返回
                    </span>
                    <span class="text-[#818999]">当前路径: /{{ userCurrentPath }}</span>
                </div>
                <table class="w-full table-auto border-collapse">
                    <thead>
                        <tr class="sticky top-0 bg-[#06162a] text-[#c9d1d9] border-[#262b32]">
                            <th class="w-[40px] px-2 py-2 text-center">
                                <input type="checkbox" 
                                    :checked="isAllUserFilesSelected" 
                                    @change="toggleSelectAllUserFiles"
                                    class="cursor-pointer accent-[#1479d7]" />
                            </th>
                            <th class="w-auto min-w-[100px] px-4 py-2 text-left">文件名</th>
                            <th class="w-[80px] px-4 py-2 text-left">类型</th>
                            <th class="w-[80px] px-4 py-2 text-left">大小</th>
                            <th class="w-[60px] px-4 py-2 text-left">操作</th>
                        </tr>
                </thead>
                <tbody>
                    <tr class="text-[#818999] relative hover:bg-[#21262d]" 
                        v-for="(item, index) in userFileList" 
                        :key="item.filePath"
                        :class="{ 'bg-[#1f3d88]/30': isUserFileSelected(item) }">
                        <td class="px-2 py-2 text-center">
                            <input type="checkbox" 
                                :checked="isUserFileSelected(item)" 
                                @change="toggleUserFileSelection(item)"
                                class="cursor-pointer accent-[#1479d7]" />
                        </td>
                        <td class="px-4 py-2 overflow-hidden whitespace-nowrap text-ellipsis"
                           :title="item.filePath">
                            <div class="flex items-center">
                                <Folder v-if="item.isDir" :size="16" class="mr-2 text-yellow-500" />
                                <File v-else :size="16" class="mr-2 text-blue-400" />
                                <span class="cursor-pointer" @click="handleUserFileClick(item)">{{ item.fileName }}</span>
                            </div>
                        </td>
                        <td class="cursor-pointer px-4 py-2">
                            {{ item.isDir ? '文件夹' : getFileExtension(item.fileName) }}
                        </td>
                        <td class="cursor-pointer px-4 py-2">
                            {{ item.size || '-' }}
                        </td>
                        <td class="cursor-pointer px-4 py-2">
                            <span v-if="!item.isDir" @click="importSingleFile(item)" 
                                class="text-green-400 hover:text-green-600" title="导入到代码">
                                <FileInput :size="16" />
                            </span>
                        </td>
                    </tr>
                    <tr v-if="userFileList.length === 0">
                        <td colspan="5" class="py-10 text-center !text-base text-gray-500">
                            <div class="flex h-full flex-col items-center justify-center">
                                <CircleSlash :size="40" class="mb-2 text-gray-500" />
                                <span class="!text-base text-gray-500">暂无数据</span>
                            </div>
                        </td>
                    </tr>
                </tbody>
                </table>
            </div>

            <!-- 栅格可视化配置对话框 -->
            <a-modal v-model:open="showModal" title="栅格可视化配置" @cancel="handleModalCancel" @ok="handleConfirm">
                <a-select v-model:value="colormapName" style="width: 100%; margin-bottom: 1rem;" placeholder="请选择可视化色带"
                    :dropdownStyle="{backgroundColor: '#ffffff',borderRadius: '4px',}">
                    <a-select-option v-for="item in colormapOptions" :key="item.value" :value="item.value">
                        <div class="color-option">
                            <span>{{ item.label }}</span>
                            <div 
                            class="color-strip" 
                            :style="{ background: item.gradient }"
                            ></div>
                        </div>
                    </a-select-option>
                </a-select>
                <a-select v-model:value="selectedBidx" style="width: 100%" placeholder="请选择可视化波段"
                    :dropdownStyle="{backgroundColor: 'black',borderRadius: '4px',}">
                    <a-select-option v-for="item in bidxOptions" :key="item" :value="item">
                        <span>{{ item }}</span>
                    </a-select-option>
                </a-select>
            </a-modal>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import type { dockerData } from '@/type/analysis'
import { getFiles, getMiniIoFiles, getTileFromMiniIo, uploadGeoJson, getJsonFileContent } from '@/api/http/analysis'
import { getDataFile } from '@/api/http/user/minio.api'
import { sizeConversion, formatTime } from '@/util/common'
import { addRasterLayerFromUrl, removeRasterLayer, map_fitView } from '@/util/map/operation'
import { RefreshCcw, CircleSlash, Upload, Eye, EyeOff, ChartColumn, View, FileInput, Folder, File, ArrowLeft } from 'lucide-vue-next'
import { ezStore } from '@/store'
import { message, type SelectProps } from 'ant-design-vue'
import { getImgStatistics } from '@/api/http/satellite-data/visualize.api'
import { getImgStats } from '@/api/http/interactive-explore'

// 获取 MinIO 端点配置（前端和容器访问地址相同）
const minioEndPoint = ezStore.get('conf')['minioIpAndPort'] || 'http://localhost:9000'

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

const showedCache = ref<string[]>([])

const emit = defineEmits(["addCharts", "removeCharts", "showMap", "importData"])

const tableData = ref<Array<dockerData>>([])
const inputData = ref<Array<dockerData>>([])
const outputData = ref<Array<dockerData>>([])
const fileInput = ref<HTMLInputElement | null>(null)
const activeDataBase = ref('data')
const activeItem = ref<string | null>(null)

/**
 * 用户数据相关变量和方法
 */
interface UserFileItem {
    id: string
    fileName: string
    isDir: boolean
    filePath: string
    size: string | null
    lastModified: string
}

const userFileList = ref<UserFileItem[]>([])
const userRootPath = ref<string>('')
const userCurrentPath = ref<string>('')
const userPathHistory = ref<string[]>([])
const selectedUserFiles = ref<UserFileItem[]>([])

// 递归函数将树形结构转换为平铺
const flattenFileTree = (node: any, parentPath: string = ""): UserFileItem[] => {
    const result: UserFileItem[] = []
    
    if (node.children && Array.isArray(node.children)) {
        for (const child of node.children) {
            const currentPath = parentPath ? `${parentPath}/${child.name}` : child.name
            
            result.push({
                id: child.name,
                fileName: child.name,
                isDir: child.dir,
                filePath: currentPath,
                size: child.size ? child.size.toString() : null,
                lastModified: child.lastModified || ""
            })
            
            // 递归处理子节点
            if (child.children && child.children.length > 0) {
                result.push(...flattenFileTree(child, currentPath))
            }
        }
    }
    
    return result
}

// 获取用户数据列表
const getUserFileList = async (filePath: string = "") => {
    try {
        let param = {
            userId: props.userId,
            filePath: filePath
        }
        let dataFile = await getDataFile(param)
        if (dataFile.status === 1) {
            userRootPath.value = dataFile.data.path
            
            if (filePath === "") {
                // 根目录
                if (dataFile.data.children && Array.isArray(dataFile.data.children)) {
                    userFileList.value = dataFile.data.children.map((child: any) => ({
                        id: child.name,
                        fileName: child.name,
                        isDir: child.dir,
                        filePath: child.name,
                        size: child.size ? child.size.toString() : null,
                        lastModified: child.lastModified || ""
                    }))
                } else {
                    userFileList.value = []
                }
            } else {
                // 子目录
                const findNodeByPath = (node: any, targetPath: string): any => {
                    if (!targetPath) return node
                    
                    const pathParts = targetPath.split('/').filter((part: string) => part !== '')
                    let currentNode = node
                    
                    for (const part of pathParts) {
                        if (!currentNode.children) return null
                        const found = currentNode.children.find((child: any) => child.name === part)
                        if (!found) return null
                        currentNode = found
                    }
                    
                    return currentNode
                }
                
                const targetNode = findNodeByPath(dataFile.data, filePath)
                if (targetNode && targetNode.children && Array.isArray(targetNode.children)) {
                    userFileList.value = targetNode.children.map((child: any) => ({
                        id: child.name,
                        fileName: child.name,
                        isDir: child.dir,
                        filePath: `${filePath}/${child.name}`,
                        size: child.size ? child.size.toString() : null,
                        lastModified: child.lastModified || ""
                    }))
                } else {
                    userFileList.value = []
                }
            }
        } else {
            message.error('获取用户数据失败')
        }
    } catch (error) {
        console.error('获取用户数据失败:', error)
        message.error('获取用户数据失败')
    }
}

// 用户文件点击处理（打开文件夹）
const handleUserFileClick = async (item: UserFileItem) => {
    if (item.isDir) {
        userPathHistory.value.push(userCurrentPath.value)
        userCurrentPath.value = item.filePath
        await getUserFileList(item.filePath)
    }
}

// 返回上一级
const goBackUserFolder = async () => {
    if (userPathHistory.value.length > 0) {
        const previousPath = userPathHistory.value.pop() || ''
        userCurrentPath.value = previousPath
        await getUserFileList(previousPath)
    }
}

// 获取文件扩展名
const getFileExtension = (fileName: string): string => {
    const parts = fileName.split('.')
    return parts.length > 1 ? parts.pop()?.toUpperCase() || '-' : '-'
}

// 检查文件是否被选中
const isUserFileSelected = (item: UserFileItem): boolean => {
    return selectedUserFiles.value.some(f => f.filePath === item.filePath)
}

// 切换文件选择状态
const toggleUserFileSelection = (item: UserFileItem) => {
    if (item.isDir) return // 不允许选择文件夹
    
    const index = selectedUserFiles.value.findIndex(f => f.filePath === item.filePath)
    if (index === -1) {
        selectedUserFiles.value.push(item)
    } else {
        selectedUserFiles.value.splice(index, 1)
    }
}

// 检查是否全选
const isAllUserFilesSelected = computed(() => {
    const selectableFiles = userFileList.value.filter(f => !f.isDir)
    return selectableFiles.length > 0 && selectableFiles.every(f => isUserFileSelected(f))
})

// 全选/取消全选
const toggleSelectAllUserFiles = () => {
    const selectableFiles = userFileList.value.filter(f => !f.isDir)
    if (isAllUserFilesSelected.value) {
        // 取消全选当前页
        selectableFiles.forEach(f => {
            const index = selectedUserFiles.value.findIndex(sf => sf.filePath === f.filePath)
            if (index !== -1) {
                selectedUserFiles.value.splice(index, 1)
            }
        })
    } else {
        // 全选当前页
        selectableFiles.forEach(f => {
            if (!isUserFileSelected(f)) {
                selectedUserFiles.value.push(f)
            }
        })
    }
}

// 生成导入代码
const generateImportCode = (files: UserFileItem[]): string => {
    if (files.length === 0) return ''
    
    const codeLines: string[] = []
    const imports = new Set<string>()
    
    // 添加通用的 requests 导入
    imports.add('import requests')
    imports.add('import io')
    
    files.forEach((file, index) => {
        const ext = getFileExtension(file.fileName).toLowerCase()
        // 构建 MinIO URL
        let rootPath = userRootPath.value || ''
        // 移除开头的 / 因为 MinIO URL 不需要
        if (rootPath.startsWith('/')) {
            rootPath = rootPath.substring(1)
        }
        // 确保 rootPath 以 / 结尾
        if (rootPath && !rootPath.endsWith('/')) {
            rootPath = rootPath + '/'
        }
        const minioUrl = `${minioEndPoint}/${rootPath}${file.filePath}`
        const varName = `data_${index + 1}`
        
        switch (ext) {
            case 'json':
            case 'geojson':
                imports.add('import json')
                codeLines.push(`# ${file.fileName}`)
                codeLines.push(`${varName}_url = "${minioUrl}"`)
                codeLines.push(`${varName}_response = requests.get(${varName}_url)`)
                codeLines.push(`${varName}_response.raise_for_status()`)
                codeLines.push(`${varName} = ${varName}_response.json()`)
                codeLines.push('')
                break
            case 'tif':
            case 'tiff':
                imports.add('import rasterio')
                codeLines.push(`# ${file.fileName}`)
                codeLines.push(`${varName}_url = "${minioUrl}"`)
                codeLines.push(`# 使用 rasterio 直接从 URL 读取 (需要 GDAL 支持)`)
                codeLines.push(`${varName} = rasterio.open(${varName}_url)`)
                codeLines.push('')
                break
            case 'csv':
                imports.add('import pandas as pd')
                codeLines.push(`# ${file.fileName}`)
                codeLines.push(`${varName}_url = "${minioUrl}"`)
                codeLines.push(`${varName} = pd.read_csv(${varName}_url)`)
                codeLines.push('')
                break
            case 'txt':
                codeLines.push(`# ${file.fileName}`)
                codeLines.push(`${varName}_url = "${minioUrl}"`)
                codeLines.push(`${varName}_response = requests.get(${varName}_url)`)
                codeLines.push(`${varName}_response.raise_for_status()`)
                codeLines.push(`${varName} = ${varName}_response.text`)
                codeLines.push('')
                break
            case 'shp':
                imports.add('import geopandas as gpd')
                imports.add('import zipfile')
                codeLines.push(`# ${file.fileName}`)
                codeLines.push(`${varName}_url = "${minioUrl}"`)
                codeLines.push(`# 注意: Shapefile 需要下载到本地后读取`)
                codeLines.push(`${varName}_response = requests.get(${varName}_url)`)
                codeLines.push(`${varName}_response.raise_for_status()`)
                codeLines.push(`with open('/tmp/${file.fileName}', 'wb') as f:`)
                codeLines.push(`    f.write(${varName}_response.content)`)
                codeLines.push(`${varName} = gpd.read_file('/tmp/${file.fileName}')`)
                codeLines.push('')
                break
            case 'npy':
                imports.add('import numpy as np')
                codeLines.push(`# ${file.fileName}`)
                codeLines.push(`${varName}_url = "${minioUrl}"`)
                codeLines.push(`${varName}_response = requests.get(${varName}_url)`)
                codeLines.push(`${varName}_response.raise_for_status()`)
                codeLines.push(`${varName} = np.load(io.BytesIO(${varName}_response.content))`)
                codeLines.push('')
                break
            case 'pkl':
            case 'pickle':
                imports.add('import pickle')
                codeLines.push(`# ${file.fileName}`)
                codeLines.push(`${varName}_url = "${minioUrl}"`)
                codeLines.push(`${varName}_response = requests.get(${varName}_url)`)
                codeLines.push(`${varName}_response.raise_for_status()`)
                codeLines.push(`${varName} = pickle.loads(${varName}_response.content)`)
                codeLines.push('')
                break
            default:
                codeLines.push(`# ${file.fileName}`)
                codeLines.push(`${varName}_url = "${minioUrl}"`)
                codeLines.push(`${varName}_response = requests.get(${varName}_url)`)
                codeLines.push(`${varName}_response.raise_for_status()`)
                codeLines.push(`${varName} = ${varName}_response.content  # 二进制内容`)
                codeLines.push(`# TODO: 根据文件类型选择合适的解析方式`)
                codeLines.push('')
        }
    })
    
    // 组合导入语句和代码
    const importLines = Array.from(imports).sort()
    const header = ['# ===== 导入的用户数据 (从 MinIO 获取) =====', ...importLines, '']
    
    return [...header, ...codeLines].join('\n')
}

// 导入单个文件
const importSingleFile = (item: UserFileItem) => {
    const code = generateImportCode([item])
    emit('importData', code)
    message.success(`已导入: ${item.fileName}`)
}

// 导入选中的多个文件
const importSelectedData = () => {
    if (selectedUserFiles.value.length === 0) {
        message.warning('请先选择要导入的文件')
        return
    }
    
    const code = generateImportCode(selectedUserFiles.value)
    emit('importData', code)
    message.success(`已导入 ${selectedUserFiles.value.length} 个文件`)
    selectedUserFiles.value = [] // 清空选择
}

/**
 * 栅格可视化配置对话框相关变量
 */
const showModal = ref<boolean>(false)
const colormapName = ref<string>('rdylgn')
const colormapOptions = ref<SelectProps['options']>([
    {
        value: 'viridis',
        label: 'Viridis',
        gradient: 'linear-gradient(to right, #440154, #482475, #414487, #355f8d, #2a788e, #21908d, #22a884, #42be71, #7ad151, #bddf26, #fde725)'
    },
    {
        value: 'plasma',
        label: 'Plasma',
        gradient: 'linear-gradient(to right, #0d0887, #46039f, #7201a8, #9c179e, #bd3786, #d8576b, #ed7953, #fb9f3a, #fdca26, #f0f921)'
    },
    {
        value: 'magma',
        label: 'Magma',
        gradient: 'linear-gradient(to right, #000004, #1b0b41, #4b0c6b, #781c6d, #a52c60, #cf4446, #ed6925, #fb9b06, #f7d13d, #fcffa4)'
    },
    {
        value: 'inferno',
        label: 'Inferno',
        gradient: 'linear-gradient(to right, #000004, #1b0c42, #4b0c6b, #781c6d, #a52c5f, #cf4446, #ed6925, #fb9b06, #f7ce3b, #fcffa4)'
    },
    {
        value: 'cividis',
        label: 'Cividis',
        gradient: 'linear-gradient(to right, #00204d, #123570, #3b496c, #575d6d, #707173, #8a8778, #a69d75, #c4b56e, #e4cf5b, #ffea46)'
    },
    {
        value: 'turbo',
        label: 'Turbo',
        gradient: 'linear-gradient(to right, #30123b, #4145ab, #4675ed, #39a2fc, #1bcfd4, #24eca6, #61fc6c, #a4fc3b, #d1e834, #f3c63a, #fe9b2d, #f36315, #d93806, #b11901, #7a0402)'
    },
    {
        value: 'rainbow',
        label: 'Rainbow',
        gradient: 'linear-gradient(to right, #ff0000, #ff8000, #ffff00, #80ff00, #00ff00, #00ff80, #00ffff, #0080ff, #0000ff, #8000ff, #ff00ff)'
    },
    {
        value: 'coolwarm',
        label: 'Cool-Warm',
        gradient: 'linear-gradient(to right, #3b4cc0, #5977e2, #8da0fa, #c5caf2, #e2e2e2, #f1b6b6, #e67b7b, #d14949, #b2182b)'
    },
    {
        value: 'rdylgn',
        label: 'Red-Yellow-Green',
        gradient: 'linear-gradient(to right, #a50026, #d73027, #f46d43, #fdae61, #fee08b, #ffffbf, #d9ef8b, #a6d96a, #66bd63, #1a9850, #006837)'
    },
    {
        value: 'bwr',
        label: 'Blue-White-Red',
        gradient: 'linear-gradient(to right, #0000ff, #4444ff, #8888ff, #ccccff, #ffffff, #ffcccc, #ff8888, #ff4444, #ff0000)'
    },
    {
        value: 'seismic',
        label: 'Seismic',
        gradient: 'linear-gradient(to right, #00004d, #0000a3, #1c1cff, #6e6eff, #b9b9ff, #ffffff, #ffb9b9, #ff6e6e, #ff1c1c, #a30000, #4d0000)'
    },
    {
        value: 'jet',
        label: 'Jet',
        gradient: 'linear-gradient(to right, #00007f, #0000ff, #007fff, #00ffff, #7fff7f, #ffff00, #ff7f00, #ff0000, #7f0000)'
    },
    {
        value: 'hot',
        label: 'Hot',
        gradient: 'linear-gradient(to right, #0b0000, #4b0000, #960000, #e10000, #ff3d00, #ff7800, #ffb600, #fff100, #ffff6d)'
    },
    {
        value: 'hsv',
        label: 'HSV',
        gradient: 'linear-gradient(to right, #ff0000, #ff00cc, #cc00ff, #6600ff, #0000ff, #0066ff, #00ccff, #00ffff, #00ffcc, #00ff66, #00ff00, #66ff00, #ccff00, #ffff00, #ffcc00, #ff6600)'
    },
    {
        value: 'blues',
        label: 'Blues',
        gradient: 'linear-gradient(to right, #f7fbff, #deebf7, #c6dbef, #9ecae1, #6baed6, #4292c6, #2171b5, #08519c, #08306b)'
    },
    {
        value: 'greens',
        label: 'Greens',
        gradient: 'linear-gradient(to right, #f7fcf5, #e5f5e0, #c7e9c0, #a1d99b, #74c476, #41ab5d, #238b45, #006d2c, #00441b)'
    },
    {
        value: 'reds',
        label: 'Reds',
        gradient: 'linear-gradient(to right, #fff5f0, #fee0d2, #fcbba1, #fc9272, #fb6a4a, #ef3b2c, #cb181d, #a50f15, #67000d)'
    }
]);
const range = ref<number[]>([-1, 1])
const bidxOptions = ref<string[]>([])
const selectedBidx = ref<string | null>(null)
const selectedBand = ref<any>(null)
const tileUrlObj = ref<any>()
const stats = ref<any>()
const itemInMinIo = ref<any>()

const handleConfirm = async() => {
    
    if (activeDataBase.value === "data") {
        message.info("目前仅支持输出数据预览")
        return
    }

    const targetItem: any = (
        activeDataBase.value === 'data' ? inputData.value : outputData.value
    ).find((data) => data.fileName === activeItem.value)

    // 默认值
    selectedBidx.value = 'b1'
    range.value = [-1, 1]
    activeItem.value = null

    // 获取瓦片服务地址
    let mapPosition = itemInMinIo.value.bbox.geometry.coordinates[0]
    let tifUrl = `${tileUrlObj.value.minioEndpoint}/${tileUrlObj.value.object}`
    const stats = await getImgStats(tifUrl)
    console.log(stats)
    const bidx = selectedBidx.value === null ? 'b1' : selectedBidx.value
    selectedBand.value = stats[bidx]
    range.value[0] = selectedBand.value.min
    range.value[1] = selectedBand.value.max
    let wholeTileUrl = `${tileUrlObj.value.tilerUrl}/tiles/WebMercatorQuad/{z}/{x}/{y}.png?scale=1&url=${tifUrl}&colormap_name=${colormapName.value}&rescale=${range.value[0]},${range.value[1]}&nodata=0`

    console.log(wholeTileUrl, 'wholeTileUrl')
    if (!tileUrlObj.value.object) {
        console.info(wholeTileUrl, '没有拿到瓦片服务的URL呢,拼接的路径参数是空的')
        message.error('瓦片服务错误')
        return
    }

    // 加载图层，图层名为"文件名+文件大小"
    addRasterLayerFromUrl(wholeTileUrl, targetItem.fileName + targetItem.fileSize)
    map_fitView(getBounds(mapPosition))
    showedCache.value.push(targetItem.fileName + targetItem.fileSize)
    targetItem.view = true

    showModal.value = false
}

const handleModalCancel = () => {
    activeItem.value = null
    showModal.value = false
}

const handleUnPreview = (item: dockerData) => {
    const fileSuffix = item.fileName.split('.').pop()?.toLowerCase()
    const targetItem: any = (
            activeDataBase.value === 'data' ? inputData.value : outputData.value
        ).find((data) => data.fileName === item.fileName)
    if (fileSuffix === 'tif' || fileSuffix === 'tiff' || fileSuffix === 'TIF' || fileSuffix === 'TIFF') {
        // 关闭时移除图层
        removeRasterLayer(item.fileName + item.fileSize)
        // 从缓存中移除
        showedCache.value = showedCache.value.filter((cache) => cache !== item.fileName + item.fileSize)
    }
    targetItem.view = !targetItem.view
}

const handlePreview = async(item: dockerData) => {
    activeItem.value = item.fileName
    const fileSuffix = item.fileName.split('.').pop()?.toLowerCase()
    if (fileSuffix === 'tif' || fileSuffix === 'tiff' || fileSuffix === 'TIF' || fileSuffix === 'TIFF') {
        handleTifPreview(item)
    } else if (fileSuffix === 'json' || fileSuffix === 'txt') {
        const targetItem = (
            activeDataBase.value === 'data' ? inputData.value : outputData.value
        ).find((data) => data.fileName === item.fileName)

        if (targetItem) {
            if (!targetItem.view) {
                // 1、拿到miniIo里面的数据列表
                let miniIoFile = await getMiniIoFiles({
                    userId: props.userId,
                    projectId: props.projectId,
                })

                // 2、根据view行所代表的数据信息，找到对应的miniIo实体
                let targetInMiniIo = miniIoFile.find(
                    (data: any) => data.dataName === targetItem.fileName,
                )
                if (!targetInMiniIo?.dataId) {
                    console.info(
                        targetItem.fileName + '没有dataId，检查miniIo上是否存在这个数据实体',
                    )
                    message.info('该数据正在上传，请稍后再预览')
                    return
                }

                let dataEntity = await getJsonFileContent(targetInMiniIo.dataId)
                console.log(dataEntity);
                emit('addCharts', dataEntity.data)
                // targetItem.view = true
            } else {
                console.log("关闭预览？");
                // targetItem.view = false
            }
        }
    } else {
        message.warning('暂不支持预览')
    }
}

const handleTifPreview = async (item: dockerData) => {
    /**
     * 校验是否能预览
     */
    if (activeDataBase.value === "data") {
        message.info("目前仅支持输出数据预览")
        return
    }
    emit('showMap')
    const targetItem = (
        activeDataBase.value === 'data' ? inputData.value : outputData.value
    ).find((data) => data.fileName === item.fileName)

    let targetInMiniIo: any = null
    if (targetItem && !targetItem.view) {
        // 1、拿到miniIo里面的数据列表
        let miniIoFile = await getMiniIoFiles({
            userId: props.userId,
            projectId: props.projectId,
        })
        // 2、根据view行所代表的数据信息，找到对应的miniIo实体
        targetInMiniIo = miniIoFile.find(
            (data: any) => data.dataName === targetItem.fileName,
        )
        if (!targetInMiniIo?.dataId) {
            console.info(
                targetItem.fileName + '没有dataId，检查miniIo上是否存在这个数据实体',
            )
            message.info('该数据正在切片，请稍后再预览')
            return
        }
    }

    /**
     * 符合预览标准
     */
    showModal.value = true
    itemInMinIo.value = targetInMiniIo
    tileUrlObj.value = await getTileFromMiniIo(targetInMiniIo.dataId)
    stats.value = await getImgStatistics(tileUrlObj.value.object)
    bidxOptions.value = Object.keys(stats.value)
}

// 数据列表切换点击事件
const handleClick = async (type: string) => {
    activeDataBase.value = type
    // 在这里处理点击事件，切换数据
    if (activeDataBase.value === 'data') {
        await getInputData()
        tableData.value = inputData.value
    } else if (activeDataBase.value === 'output') {
        await getOutputData()
        tableData.value = outputData.value
    } else if (activeDataBase.value === 'user') {
        await getUserFileList(userCurrentPath.value)
    }
}

const refreshTableData = async () => {
    if (activeDataBase.value === 'data') {
        await getInputData()
        tableData.value = inputData.value
        message.success('数据更新成功')
    } else if (activeDataBase.value === 'output') {
        await getOutputData()
        tableData.value = outputData.value
        message.success('数据更新成功')
    } else if (activeDataBase.value === 'user') {
        await getUserFileList(userCurrentPath.value)
        message.success('数据更新成功')
    }
}

const triggerFileSelect = () => {
    fileInput.value?.click()
}

// 上传geojson的方法,这里最好检查一下键名，比如feature什么的，进一步收紧限制，防止用户上传错误的geojson
const uploadFile = async (event: Event) => {
    const target = event.target as HTMLInputElement;
    if (!target.files || target.files.length === 0) return;
    const file: File = target.files[0];
    if (!file) return

    // 检查文件类型
    const allowedTypes = ['application/json', 'text/plain']
    const allowedExtensions = ['.json', '.txt', '.geojson'];

    const fileExtension = file.name.split('.').pop()?.toLowerCase() || '';
    if (!allowedTypes.includes(file.type) && !allowedExtensions.includes(`.${fileExtension}`)) {
        message.warning('只支持 .geojson, .json 或 .txt 文件')
        return
    }

    // 读取文件内容
    const reader = new FileReader()
    reader.onload = async (e: ProgressEvent<FileReader>) => {
        try {

            const fileContent = e.target?.result as string

            const jsonData = JSON.parse(fileContent)

            const res = await uploadGeoJson({
                userId: props.userId,
                projectId: props.projectId,
                uploadDataName: file.name.split('.')[0], // 文件名作为数据名
                uploadData: jsonData
            })

            if (res.status === 1) {
                await handleClick(activeDataBase.value)
                message.success('上传成功')
            } else {
                message.error('上传失败')
            }
        } catch (err) {
            message.error('文件内容不是有效的 JSON 格式')
        }
    }

    reader.readAsText(file)

    // 清空 input 的值，以便多次上传同一个文件时也能触发 change
    target.value = '';
}

// 从一组坐标点中获得左下和右上坐标点
const getBounds = (coordinates: number[][]) => {
    let minLng = Infinity; // 最小经度（西经）
    let maxLng = -Infinity; // 最大经度（东经）
    let minLat = Infinity; // 最小纬度（南纬）
    let maxLat = -Infinity; // 最大纬度（北纬）
    for (const [lng, lat] of coordinates) {
        if (lng < minLng) minLng = lng;
        if (lng > maxLng) maxLng = lng;
        if (lat < minLat) minLat = lat;
        if (lat > maxLat) maxLat = lat;
    }
    return [
        [minLng, minLat], // 左下角（西经, 南纬）
        [maxLng, maxLat]  // 右上角（东经, 北纬）
    ];
}

const getInputData = async () => {
    let tempData = await getFiles({
        userId: props.userId,
        projectId: props.projectId,
        path: '/data',
    })
    if (tempData.length === 0) {
        return []
    }

    inputData.value = tempData.map((item: any) => {
        return { ...item, view: false }
    })
}
const getOutputData = async () => {
    let tempData = await getFiles({
        userId: props.userId,
        projectId: props.projectId,
        path: '/output',
    })
    if (tempData.length === 0) {
        return []
    }
    outputData.value = tempData.map((item: any) => {
        if (showedCache.value.includes(item.fileName + item.fileSize)) item.view = true
        else item.view = false

        return item
    })
}

onMounted(async () => {
    setTimeout(async () => {
        // nextTick(async () => {
        await getInputData()
        await getOutputData()
        tableData.value = activeDataBase.value === 'data' ? inputData.value : outputData.value
        // })
    }, 1000)

    console.log(tableData.value, 'tableData.value')
})
</script>

<style scoped lang="scss">
table {
    border: 0;
}

th,
td {
    border: 0;
    font-size: 10px;
    white-space: nowrap;
    /* 防止换行 */
    overflow: hidden;
    text-overflow: ellipsis;
}

thead {
    background-color: #293f6b;
}

th {
    position: sticky;
    top: 0;
    z-index: 10;
}

.color-option {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.color-strip {
  width: 100%;
  height: 20px;
  margin-top: 4px;
  border-radius: 2px;
}

/* 调整下拉框选项宽度 */
:deep(.ant-select-dropdown) {
  min-width: 200px !important;
}

:deep(.ant-select-item) {
  padding: 8px 12px;
}
</style>
