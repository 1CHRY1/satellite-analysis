<template>
    <div class="mission-control">
        <header class="control-header">
            <div class="navigation-section">
                <button @click="back" class="nav-button back-btn">
                    <span class="nav-icon">←</span>
                    {{ $t("userpage.userFunction.back") }}
                </button>
                <div class="location-display">
                    <span class="location-label">🗺️ 当前位置：</span>
                    <div class="breadcrumb-nav">
                        <span
                            v-for="(item, index) in breadcrumbs"
                            :key="item"
                            class="breadcrumb-item"
                            :class="{ active: index === breadcrumbs.length - 1 }"
                        >
                            {{ item }}
                            <span v-if="index < breadcrumbs.length - 1" class="breadcrumb-separator">▶</span>
                        </span>
                    </div>
                </div>
            </div>
            <div class="divider-line"></div>
            
            <div class="controls-section">
                <div class="search-control">
                    <div class="search-wrapper">
                        <span class="search-icon">🔍</span>
                        <input
                            v-model="searchKeyword"
                            placeholder="搜索卫星数据文件..."
                            @input="handleSearch"
                            class="search-input"
                        />
                        <button v-if="isSearching" @click="clearSearch" class="clear-search">
                            ✕
                        </button>
                    </div>
                </div>

                <div class="action-controls">
                    <button @click="refresh" class="control-btn refresh-btn">
                        <span class="btn-icon">🔄</span>
                        <span>{{ $t("userpage.userFunction.refresh") }}</span>
                        <div class="btn-glow"></div>
                    </button>

                    <button @click="uploadVisible = true" class="control-btn upload-btn">
                        <span class="btn-icon">📡</span>
                        <span>{{ $t("userpage.userFunction.upload") }}</span>
                        <div class="btn-glow"></div>
                    </button>

                    <button @click="newFolderVisible=true" class="control-btn folder-btn">
                        <span class="btn-icon">📁</span>
                        <span>{{ $t("userpage.userFunction.newfolder") }}</span>
                        <div class="btn-glow"></div>
                    </button>
                </div>
            </div>
            <div class="divider-line"></div>
        </header>
   
        <main class="control-main">
            <div class="data-grid-section">
                <div class="grid-container">
                    <div class="satellite-data-grid">
                        <div 
                            v-for="file in fileList" 
                            @click="selectFile(file)"
                            @dblclick="openFolder()"
                            @contextmenu.prevent="showContextMenu($event, file)"
                            class="flex flex-col items-center p-4 bg-white rounded-lg shadow hover:shadow-md transition-shadow cursor-pointer border-2"
                            :class="{
                            }"
                        >
                            <img
                                v-if="file.isDir"
                                src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAKgAAACoCAMAAABDlVWGAAAAwFBMVEUAAAD/2Ur/20z/203/40X//3j/yiX/zCv/3lD/2kv/yib/yiX/yib/2kr/2kr/yiX/2kv/2kv/yyb/2UT/1Tv/20H/yib/2kr/yib/2kr/yiX/20v/2kr/3Uz/5Vj/2kr/ySX/ySX/yiX/2Ur/20v/2Uz/ySj/zSf/20z/ySb/2kv/2Ur/2Ur/2Uv/yiX/0DX/yib/2Uz/yiX/yyf/20v/ySf/2kr/ySf/20z/2Uz/yyb/4k7/////2Ur/ySX/zS9+lhVZAAAAPXRSTlMAr1RGBQKWFw2o9PHt69/YiYBpKyIT+/bLwIFfWDMJ8OPHv7ePbEdBP+DU0tG8uKmnopCEd3duVU02NhoBsiz/2wAAAaxJREFUeNrt1GdSIzEQQOE2nuhsnCNhgWVzDoQW978V8J/CowkSqnrfCV5J3S0AAAAAAAAAXhVdJefDjjlg3vuw/L7+l4ovadI1NoY/b8WDaN0x1oZ/MnEsPTOldH/NxKXJqSmrdxyLM+mpqWA5FUeiM1NJ7724sTYVzY/FhbRjKksiaV5iavC1+dKoa+qQSNOuTD1emNM3+PNP5k3v/rmpSW8qjRqauixjadLzcQpiTE19uqk06KFG39oHXU8yKUedO8m3WRChTxbjLIxQ1f42kFDVURxIqLbiQEJ1FEqobkIJ7WeBhOoolNDFLJBQ3YQS2goldBEHEqp3oYTuQgn9H0roEaGEEkoooYQSSiihhBZHKKGEEkoooYQSSqgNQgkllFBCCSWUUEJtEEoooYQSSiihhBJqg9DD+urVtRT1Wb2aSlHv1Ku9FDVWnz5JYRP1aSzFDdSjnRR3qf6sxELk8UlvxMaR+pKLnQv142MmdmI/t7Q/FVv7XN0bTMTe/VhdW82klN0XdWmwiaWsdn6ijqwu91JF1P47+tFqVn7xezsTAAAAAAAAAK97BM+evOtMrXyJAAAAAElFTkSuQmCC"
                                alt=""
                                style="width: 100%;"
                            />
                            <div class="mb-2">
                                <!-- <font-awesome-icon  
                                    v-if="file.isDir"
                                    :icon="['fas', 'folder']"
                                    class="text-4xl text-yellow-500"
                                />
                                <font-awesome-icon 
                                    v-else
                                    :icon="['fas', 'file']"
                                    class="text-4xl text-gray-500"
                                /> -->
                            </div>
                            
                            <div class="text-sm text-center text-gray-700 break-words w-full">
                                {{ file.fileName }}
                            </div>
                            
                            <!-- 搜索状态下显示完整路径 -->
                            <div v-if="isSearching" class="text-xs text-gray-500 mt-1 text-center break-words w-full">
                                路径: {{ file.filePath }}
                            </div>
                            
                            <div v-if="!file.isDir && file.size" class="text-xs text-gray-500 mt-1">
                                {{ file.size }}
                            </div>
                        </div>
                    </div>
                    <!-- 空状态 -->
                    <div v-if="!fileList || fileList.length === 0" class="flex flex-col items-center justify-center h-full text-gray-500">
                        <!-- <font-awesome-icon :icon="['fas', 'folder-open']" class="text-6xl mb-4" /> -->
                        <p class="text-lg" v-if="!isSearching">{{ $t("userpage.data.emptyFolder") }}</p>
                        <p class="text-lg" v-else>未找到匹配的文件或文件夹</p>
                        <p class="text-sm mt-2" v-if="isSearching">尝试使用不同的关键词搜索</p>
                    </div>
                </div>
                <!-- 右键菜单 -->
                <div class="add-folder-9 bg-white border border-gray-300 rounded-md shadow-lg !text-black" v-show="folderShow" :style="{ position: 'fixed', left: menuPosition.x + 'px', top: menuPosition.y + 'px', zIndex: 1000, }">
                    <div class="add-folder-1">
                        <div v-if="show_file.fileType === 'folder'" class="add-folder-2" @click="openFolder">
                        打开文件夹
                        </div>
                        <div style="border: 2px solid rgba(18,17,42,.07)"></div>
                        <div class="add-folder-2" @click="handleFileOperation">    
                        移动
                        </div>
                        <div style="border: 2px solid rgba(18,17,42,.07)"></div>
                        <div class="add-folder-2" @click="rename">
                        重命名
                        </div>
                        <div style="border: 2px solid rgba(18,17,42,.07)"></div>
                        <div class="add-folder-6" @click="deleteFolder">
                        删 除
                        </div>
                    </div>
                </div>
            </div>
            <aside class="w-84 p-4 bg-gray-50">
                <div class="space-y-4">
                    <h2 class="text-lg font-bold " style="color: black;">
                        {{ $t("userpage.userFunction.details") }}
                    </h2>
                    <h3 class="text-xl font-bold text-blue-600">
                        {{ show_file === null ? $t("userpage.userFunction.noSelect") : show_file.fileName }}
                    </h3>
                    <!-- <div class="text-sm text-gray-500">
                        {{ $t("userpage.userFunction.id") }}:
                        {{ show_file === null ? "" : show_file.id }}
                    </div> -->
                    <div class="space-y-2">
                        <div class="flex items-center">
                        
                        </div>

                        <div class="text-sm text-gray-500">
                        {{ $t("userpage.data.modifyTime") }}: 
                        {{ show_file === null ? "" : show_file.lastModified }}
                        </div>
                        <div  class="text-sm text-gray-500">
                        {{ $t("userpage.userFunction.type") }}:
                        {{
                            show_file === null ? "" : show_file.fileType
                        }}
                        </div>
                        <div class="text-sm text-gray-500">
                        {{ $t("userpage.userFunction.size") }}: 
                        {{ show_file === null ? "" : show_file.size }}
                        </div>
                    </div>
                    <div class="space-y-2">

                        <button
                        v-if="dataSwitch"
                        @click="movePanelShow = true"
                        class="move_button inline-flex hover:bg-blue-100 items-center justify-center whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 border border-input bg-background hover:bg-accent hover:text-accent-foreground h-10 px-4 py-2 w-full"
                        style="color: black; "
                        >
                        
                        {{ $t("userpage.userFunction.move") }}
                        <!-- ({{ choose_num }}) -->
                        </button>
                        <button
                        @click=""
                        class="inline-flex bg-blue-500 hover:bg-blue-400 text-white items-center justify-center whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 border border-input bg-background hover:bg-accent hover:text-accent-foreground h-10 px-4 py-2 w-full"
                        >
                        <!-- <font-awesome-icon
                            class="relative right-2"
                            :icon="['fas', 'plus']"
                        /> -->
                    预览数据
                        <!-- ({{ choose_num }}) -->
                        </button>
                        <button
                        @click="download"
                        class="inline-flex bg-green-500 hover:bg-green-400 text-white items-center justify-center whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 border border-input bg-background hover:bg-accent hover:text-accent-foreground h-10 px-4 py-2 w-full"
                        >
                        <!-- <font-awesome-icon
                            class="relative right-2"
                            :icon="['fas', 'download']"
                        /> -->
                        {{ $t("userpage.userFunction.down") }}
                        ({{ choose_num }})
                        </button>
                        <button
                        v-if="dataSwitch"
                        @click="deleteFolder"
                        class="inline-flex bg-red-500 hover:bg-red-400 text-white items-center justify-center whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 border border-input bg-background hover:bg-accent hover:text-accent-foreground h-10 px-4 py-2 w-full"
                        >
                        <!-- <font-awesome-icon
                            class="relative right-2"
                            :icon="['fas', 'trash-can']"
                        /> -->
                        {{ $t("userpage.userFunction.delete") }}
                        ({{ choose_num }})
                        </button>
                        <button
                        v-if="!dataSwitch"
                        @click=""
                        class="inline-flex bg-blue-500 hover:bg-blue-400 text-white items-center justify-center whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 border border-input bg-background hover:bg-accent hover:text-accent-foreground h-10 px-4 py-2 w-full"
                        >
                        <!-- <font-awesome-icon
                            class="relative right-2"
                            :icon="['fas', 'plus']"
                        /> -->
                        {{ $t("userpage.userFunction.addData") }}
                        ({{ choose_num }})
                        </button>
                    </div>
                </div>
            </aside>
        </main>
    </div>
    <el-dialog title="上传数据" v-model="uploadVisible" width="500px">
        <el-form>
            <el-form-item label="数据选取">
                        <el-upload 
                            ref="uploadRef"
                            style="" 
                            accept=""
                            :before-upload="beforeUpload"
                            :on-success="handleUploadSuccess"
                            :on-error="handleUploadError"
                            :on-change="handleFileChange"
                            :limit="1"
                            :on-exceed="handleExceed"
                            action="#"
                            :auto-upload="false"
                        >
                            <el-button type="primary">选择文件</el-button>
                        </el-upload>
                   </el-form-item>
                    <el-form-item label="文件路径">
                        <el-input placeholder="请输入文件名称" v-model="uploadFilePath" />  
                    </el-form-item>
            <!-- <el-form-item label="文件名称">
                <el-input placeholder="请输入文件名称" />
                
            </el-form-item> -->
        </el-form>

        <template #footer>
            <span class="dialog-footer">
                    <el-button @click="uploadVisible = false">
                        {{ $t("userpage.cancel") }}
                    </el-button>
                    <el-button type="primary" @click="upload">上传</el-button>
            </span>
        </template>
    </el-dialog>

    <el-dialog
        v-model="movePanelShow"
        :title="t('userpage.data.move')"
        width="40%"
  >
        <el-button 
        v-for="folder in allFileList.filter(item => item.isDir)" 
        :key="folder.filePath"
        style="margin: 5px;background-color: white !important;"
        @click="moveFile(folder)"
        >
            <img
                src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAKgAAACoCAMAAABDlVWGAAAAwFBMVEUAAAD/2Ur/20z/203/40X//3j/yiX/zCv/3lD/2kv/yib/yiX/yib/2kr/2kr/yiX/2kv/2kv/yyb/2UT/1Tv/20H/yib/2kr/yib/2kr/yiX/20v/2kr/3Uz/5Vj/2kr/ySX/ySX/yiX/2Ur/20v/2Uz/ySj/zSf/20z/ySb/2kv/2Ur/2Ur/2Uv/yiX/0DX/yib/2Uz/yiX/yyf/20v/ySf/2kr/ySf/20z/2Uz/yyb/4k7/////2Ur/ySX/zS9+lhVZAAAAPXRSTlMAr1RGBQKWFw2o9PHt69/YiYBpKyIT+/bLwIFfWDMJ8OPHv7ePbEdBP+DU0tG8uKmnopCEd3duVU02NhoBsiz/2wAAAaxJREFUeNrt1GdSIzEQQOE2nuhsnCNhgWVzDoQW978V8J/CowkSqnrfCV5J3S0AAAAAAAAAXhVdJefDjjlg3vuw/L7+l4ovadI1NoY/b8WDaN0x1oZ/MnEsPTOldH/NxKXJqSmrdxyLM+mpqWA5FUeiM1NJ7724sTYVzY/FhbRjKksiaV5iavC1+dKoa+qQSNOuTD1emNM3+PNP5k3v/rmpSW8qjRqauixjadLzcQpiTE19uqk06KFG39oHXU8yKUedO8m3WRChTxbjLIxQ1f42kFDVURxIqLbiQEJ1FEqobkIJ7WeBhOoolNDFLJBQ3YQS2goldBEHEqp3oYTuQgn9H0roEaGEEkoooYQSSiihhBZHKKGEEkoooYQSSqgNQgkllFBCCSWUUEJtEEoooYQSSiihhBJqg9DD+urVtRT1Wb2aSlHv1Ku9FDVWnz5JYRP1aSzFDdSjnRR3qf6sxELk8UlvxMaR+pKLnQv142MmdmI/t7Q/FVv7XN0bTMTe/VhdW82klN0XdWmwiaWsdn6ijqwu91JF1P47+tFqVn7xezsTAAAAAAAAAK97BM+evOtMrXyJAAAAAElFTkSuQmCC"
                alt=""
                style="width: 20px;"
            />
            <div class="text-sm text-center text-gray-700 break-words w-full">
                {{ folder.fileName }}
            </div>
        </el-button>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="movePanelShow = false">
          >{{ $t("userpage.cancel") }}
        </el-button>
      </span>
    </template>
  </el-dialog>

  <el-dialog
        v-model="renameVisible"
        title="重命名文件"
        width="400px"
  >
    <el-form>
      <el-form-item label="新文件名">
        <el-input v-model="newFileName" placeholder="请输入新的文件名" />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="renameVisible = false">
          {{ $t("userpage.cancel") }}
        </el-button>
        <el-button type="primary" @click="confirmRename">
          确认重命名
        </el-button>
      </span>
    </template>
  </el-dialog>
  <el-dialog
        v-model="newFolderVisible"
        title="创建文件夹"
        width="400px"
  >
    <el-form>
      <el-form-item label="文件夹名称">
        <el-input v-model="newFolderName" placeholder="请输入文件夹名称" />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="newFolderVisible = false">
          {{ $t("userpage.cancel") }}
        </el-button>
        <el-button type="primary" @click="createFolder">
          确认创建
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref,computed,reactive,onMounted, nextTick, watch  } from 'vue'
import { Axios } from 'axios'
import { ArrowRight } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { 
    getDataFile,
    deleteFile,
    uploadFile,
    updateFile,
    downloadFile
 } from '@/api/http/user/minio.api.ts'
import { useUserStore } from '@/store/userStore'
import {
    Upload,
    FileDown,
    FolderPlus,
    Trash2,
    CircleChevronLeft
 } from 'lucide-vue-next'
import { message } from 'ant-design-vue'

const userStore = useUserStore()
const { t } = useI18n()
const dataSwitch = ref(true)
const prePath = ref()
const last_path: string[] = []

const back = async () => {
    
    if (isSearching.value) {
        clearSearch()
        return
    }
    
    if (last_path.length == 0) {
        message.info("已是最上级");
        return;
    }
    
    const previousPath = last_path.pop();
    
    try {
        await getFileList(previousPath || "");
        
        // 更新UI状态
        if (breadcrumbs.length > 1) {
            breadcrumbs.pop();
        }
        
        show_file.value = {
            id: null,
            fileName: "",
            fileType: "",
            filePath: "",
            size: null,
            createTime: "",
            lastModified: "",
        };
        
        message.success('返回上一级');
    } catch (error) {
        console.error('返回上一级失败:', error)
        message.error('返回上一级失败，请重试')
        // 失败把路径放回
        if (previousPath !== undefined) {
            last_path.push(previousPath);
        }
    }
}

const refresh = async () => {
    try {
        await getFileList()

        show_file.value = {
            id: null,
            fileName: "",
            fileType: "",
            filePath: "",
            size: null,
            createTime: "",
            lastModified: "",
        }
        choose_num.value = 0
        message.success('刷新成功')
    } catch (error) {
        console.error('刷新失败:', error)
        message.error('刷新失败，请重试')
    }
}

const newFolderVisible = ref(false)
const newFolderName = ref("")
const createFolder = () => {
    if (!newFolderName.value) {
        message.error('请输入文件夹名称')
        return
    }
    
    // 创建新的文件夹对象
    const newFolder: FileItem = {
        id: newFolderName.value,
        fileName: newFolderName.value,
        isDir: true,
        filePath: newFolderName.value,
        size: null,
        lastModified: new Date().toISOString()
    }
    
    fileList.value.unshift(newFolder)
    
    newFolderName.value = ''
    newFolderVisible.value = false
    
    message.success(`文件夹 "${newFolder.fileName}" 创建成功`)
}

const uploadFilePath = ref("") 
const uploadRef = ref()
const selectedFile = ref<any>(null)

const handleFileChange = (file, fileList) => {
    console.log('文件选择变化:', file, fileList)
    selectedFile.value = file
    console.log('已选择文件:', selectedFile.value)
}

const handleExceed = (files: any, fileList: any) => {
    message.warning(`最多只能上传1个文件`)
}

const upload = async () =>{
    // 验证是否选择了文件
    if (!selectedFile.value) {
        message.error('请先选择要上传的文件')
        return
    }
    
    // 验证文件路径
    if (!uploadFilePath.value || !uploadFilePath.value.trim()) {
        message.error('请输入文件路径')
        return
    }
    
    // 检查当前文件夹中是否已存在同名文件
    const fileName = selectedFile.value.name || (selectedFile.value.raw && selectedFile.value.raw.name)
    const existingFile = fileList.value.find(file => file.fileName === fileName && !file.isDir)
    
    if (existingFile) {
        message.error(`文件 "${fileName}" 已存在，请选择其他文件或重命名后上传`)
        return
    }
    
    try {
        let formdata = new FormData()
        let fullpath = rootPath.value+show_file.value.filePath+ uploadFilePath.value
        // let fullpath = rootPath.value + uploadFilePath.value.trim()
        console.log('fullpath',fullpath)
        const file = selectedFile.value.raw || selectedFile.value
        console.log('准备上传的文件:', file)
        formdata.append('file', file) 
        formdata.append('filePath', fullpath.trim())    


        let res = await uploadFile(formdata)
        if(res.status === 1) {
            await getFileList()
            message.success('上传成功')
            uploadVisible.value = false 
            uploadFilePath.value = '' 
            selectedFile.value = null 
        } else {
            message.error(res.message || '上传失败')
        }
    } catch (error) {
        console.error('上传异常:', error)
        message.error('上传过程中发生错误，请重试')
    }
}

const breadcrumbs = reactive(["./ROOT"]);

// 文件列表类型定义
interface FileItem {
    id: string;
    fileName: string;
    isDir: boolean;
    filePath: string;
    size: string | null;
    lastModified: string;
}

//数据列表获取
const fileList = ref<FileItem[]>([])
const allFileList = ref<FileItem[]>([]) 

// 搜索相关变量
const searchKeyword = ref('')
const isSearching = ref(false)
const originalFileList = ref<FileItem[]>([]) // 保存原始文件列表
const rootPath = ref("")

// 递归函数将树形结构转换为平铺,用于搜索
const flattenFileTree = (node: any, parentPath: string = ""): FileItem[] => {
    const result: FileItem[] = []
    
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

            console.log('节点:', result)
        }
    }
    
    return result
}

const getFileList = async (filePath: string = "") => {
    try {
        let param = {
            userId: userStore.user.id,
            filePath: "" 
        }
        let dataFile = await getDataFile(param)
        if(dataFile.status === 1) {
            
            allFileList.value = flattenFileTree(dataFile.data)
                     
            if (filePath === "") {
                // 根目录
                if (dataFile.data.children && Array.isArray(dataFile.data.children)) {
                    fileList.value = dataFile.data.children.map(child => ({
                        id: child.name,
                        fileName: child.name,
                        isDir: child.dir,
                        filePath: child.name,
                        size: child.size ? child.size.toString() : null,
                        createTime: child.createTime || "",
                        lastModified: child.lastModified || ""
                    }))
                } else {
                    fileList.value = []
                }
            } else {
                // 子目录
                const findNodeByPath = (node: any, targetPath: string): any => {
                    if (!targetPath) return node
                    
                    const pathParts = targetPath.split('/').filter(part => part !== '')
                    console.log('路径部分:', pathParts)

                    let currentNode = node
                    
                    for (const part of pathParts) {
                        if (!currentNode.children) return null
                        
                        const found = currentNode.children.find(child => child.name === part)
                        if (!found) return null
                        
                        currentNode = found
                    }
                    
                    return currentNode
                }
                
                const targetNode = findNodeByPath(dataFile.data, filePath)
                if (targetNode && targetNode.children && Array.isArray(targetNode.children)) {
                    fileList.value = targetNode.children.map(child => ({
                        id: child.name,
                        fileName: child.name,
                        isDir: child.dir,
                        filePath: `${filePath}/${child.name}`,
                        size: child.size ? child.size.toString() : null,
                        createTime: child.createTime || "",
                        lastModified: child.lastModified || ""
                    }))
                } else {
                    fileList.value = []
                }
            }
            rootPath.value = dataFile.data.path
            console.log('API响应数据:', dataFile.data)
            console.log('完整文件列表:', allFileList.value)
            console.log('当前显示文件列表:', fileList.value)
        } else {
            message.error('数据加载失败')
            throw new Error('数据加载失败')
        }
    } catch (error) {
        console.error('获取文件列表失败:', error)
        message.error('获取文件列表失败，请重试')
        throw error // 重新抛出异常，让调用者能够捕获
    }
}

//文件选择
const choosing_file = ref([])
const choosing_file_index = []

interface fileType {
    id: string | null;
    fileName: string ;
    fileType: string;
    filePath: string;
    size: string | null;    
    createTime: string;
    lastModified: string;
}
const show_file = ref<fileType>({
    id: null,
    fileName: "",
    fileType: "",
    filePath: "",
    size: null,
    createTime: "",
    lastModified: "",
});

// 选择文件函数
const selectFile = (file: any) => {
    show_file.value = {
        id: file.id,
        fileName: file.fileName,
        fileType: file.isDir ? 'folder' : file.fileName.split('.').pop() || '',
        filePath: file.filePath,
        size: file.size,
        createTime: file.createTime,
        lastModified: file.lastModified,
    };
    console.log(show_file.value)
    choose_num.value = 1;
};
const choose_num = ref(0);

// 搜索相关函数
const handleSearch = () => {
    const keyword = searchKeyword.value.trim().toLowerCase()
    
    if (keyword === '') {
        clearSearch()
        return
    }
    
    if (!isSearching.value) {
    
        originalFileList.value = [...fileList.value]
        isSearching.value = true
    }
    
    const searchResults = allFileList.value.filter(file => 
        file.fileName.toLowerCase().includes(keyword) ||
        file.filePath.toLowerCase().includes(keyword)
    )
    
    fileList.value = searchResults
}

const clearSearch = () => {
    searchKeyword.value = ''
    isSearching.value = false
    
    if (originalFileList.value.length > 0) {
        // 恢复原始文件列表
        fileList.value = [...originalFileList.value]
        originalFileList.value = []
    }
}

const uploadVisible = ref(false)
const renameVisible = ref(false)
const newFileName = ref('')

// 文件上传前的验证
const beforeUpload = (file) => {
    const isLimit = file.size / 1024 / 1024 < 50
    if (!isLimit) {
        message.error('文件大小不能超过50MB！')
        return false
    }
    return true
}

// 存储上传的文件信息
const uploadedFile = ref(null)

// 文件上传成功处理
const handleUploadSuccess = (response, file) => {
    uploadedFile.value = file
    message.success(`文件 ${file.name} 上传成功！`)
    console.log('上传的文件信息:', file)
}

// 文件上传失败处理
const handleUploadError = (error, file) => {
    message.error(`文件 ${file.name} 上传失败！`)
    console.error('上传失败:', error)
}

const detailsVisible = ref(false)
const dataForm = ref({
    name: "",
    lastModified: "",
})

const movePanelShow = ref(false)

//右键菜单
const folderShow = ref(false)
const menuPosition = ref({ x: 0, y: 0 })


// 显示右键菜单
const showContextMenu = (event: MouseEvent, file: any) => {
    event.preventDefault()
    menuPosition.value = { x: event.clientX, y: event.clientY }
    
    
    show_file.value = {
        id: file.id,
        fileName: file.fileName,
        fileType: file.isDir ? 'folder' : file.fileName.split('.').pop() || '',
        filePath: file.filePath,
        size: file.size,
        createTime: file.createTime,
        lastModified: file.lastModified,
    };
    
    folderShow.value = true
    
    const closeMenu = () => {
        folderShow.value = false
        document.removeEventListener('click', closeMenu)
    }
    setTimeout(() => {
        document.addEventListener('click', closeMenu)
    }, 0)
}

const openFolder = async () => {
    if (!show_file.value || show_file.value.fileType !== 'folder') {
        message.warning('请选择一个文件夹');
        folderShow.value = false;
        return;
    }

    // 如果正在搜索，先清除搜索状态
    if (isSearching.value) {
        clearSearch()
    }

    const folderName = show_file.value.fileName;
    const folderPath = show_file.value.filePath;
    const currentPath = show_file.value.filePath.substring(0, show_file.value.filePath.lastIndexOf('/')) || "";
    
    try {
        last_path.push(currentPath);
        
        await getFileList(folderPath);
        
        breadcrumbs.push(folderName);
        
        show_file.value = {
            id: null,
            fileName: "",
            fileType: "",
            filePath: "",
            size: null,
            createTime: "",
            lastModified: "",
        };
        
        console.log('打开文件夹:', folderName);
        message.success(`打开文件夹: ${folderName}`);
    } catch (error) {
        last_path.pop();
        console.error('打开文件夹失败:', error);
        message.error('打开文件夹失败，请重试');
    } finally {
        folderShow.value = false;
    }
}

const handleFileOperation = () => {
    if (show_file.value) {
        console.log('操作文件:', show_file.value.fileName)
        message.info(`操作文件: ${show_file.value.fileName}`)
    }
    folderShow.value = false
}

const rename = () => {
    if (show_file.value) {
        renameVisible.value = true
        newFileName.value = show_file.value.fileName
    }
    folderShow.value = false
}

const confirmRename = async () => {
    if (!show_file.value || !newFileName.value.trim()) {
        message.error('请输入有效的文件名')
        return
    }
    
    if (newFileName.value === show_file.value.fileName) {
        message.info('文件名未发生变化')
        renameVisible.value = false
        return
    }
    
    try {
        const renameData = {
            oldFilePath: rootPath.value+show_file.value.filePath,
            newFilePath: rootPath.value+show_file.value.filePath.replace(show_file.value.fileName, newFileName.value),
        }
        
        const result = await updateFile(renameData)
        
        if (result.status === 1) {
            message.success(`文件重命名成功: ${newFileName.value}`)
            renameVisible.value = false
            // 刷新文件列表
            await refresh()
        } else {
            message.error('重命名失败，请重试')
        }
    } catch (error) {
        console.error('重命名文件失败:', error)
        message.error('重命名失败，请重试')
    }
}

const moveFile = async(folder) => {
    try {
        const newFolder = {
            oldFilePath: rootPath.value + show_file.value.filePath,
            newFilePath: rootPath.value + folder.filePath + '/' + show_file.value.fileName,
        }
        
        const result = await updateFile(newFolder)
        
        if (result.status === 1) {
            message.success(`文件移动成功: ${show_file.value.fileName}`)
            movePanelShow.value = false
            // 刷新文件列表
            await refresh()
        } else {
            message.error('移动文件失败，请重试')
        }
    } catch (error) {
        console.error('移动文件失败:', error)
        message.error('移动文件失败，请重试')
    }
}

const deleteFolder = async() => {
    if (show_file.value) {
        const confirmed = confirm(`确定要删除 "${show_file.value.fileName}" 吗？`)
        if (confirmed) {
            try{
                let fullpath = rootPath.value + show_file.value.filePath
                let delate = await deleteFile(fullpath)
                if(delate.status === 1) {
                    console.log('删除文件:', show_file.value.fileName)
                    message.success(`已删除: ${show_file.value.fileName}`)
                }
                refresh()
            }catch (error) {
                console.error('删除文件失败:', error)
                message.error('删除文件失败，请重试')
            }
            
        }
    }
    folderShow.value = false
}

//下载函数
const download = async() =>{
    try {
        // let downLoadInterval = setInterval(async() => {
            let file = show_file.value;
            console.log(file)
            console.log(file.filePath)
            let fullpath = rootPath.value + file.filePath
            console.log(fullpath)

            if (file.fileType == "folder") {
                message.error("请选择文件而非文件夹");
            } else {
                let  res = await downloadFile(fullpath)
                const blob = new Blob([res], { type: 'application/octet-stream' })
                const link = document.createElement('a')
                link.href = URL.createObjectURL(blob)
                link.download = file.fileName || 'downloaded-file'
                link.click()
                URL.revokeObjectURL(link.href)

                message.success(`文件 ${file.fileName} 下载成功！`)
                // clearInterval(downLoadInterval);
            }
        // }, 1000);
    } catch (error) {
        console.error('下载文件失败:', error)
        message.error('下载文件失败，请重试')
    }
}

// 文件图标映射
const getFileIcon = (fileName: string) => {
  if (!fileName) return '📄'
  const extension = fileName.split('.').pop()?.toLowerCase()

  const iconMap: Record<string, string> = {
    'pdf': '📄',
    'doc': '📝',
    'docx': '📝',
    'txt': '📃',
    'csv': '📊',
    'xlsx': '📊',
    'xls': '📊',
    'ppt': '📽️',
    'pptx': '📽️',
    'jpg': '🖼️',
    'jpeg': '🖼️',
    'png': '🖼️',
    'gif': '🖼️',
    'tif': '🛰️',
    'tiff': '🛰️',
    'geotiff': '🛰️',
    'shp': '🗺️',
    'kml': '🗺️',
    'gpx': '🗺️',
    'json': '⚙️',
    'xml': '⚙️',
    'zip': '📦',
    'rar': '📦',
    '7z': '📦',
    'tar': '📦'
  }

  return iconMap[extension || ''] || '📄'
}

// 格式化文件日期
const formatFileDate = (dateString: string) => {
  if (!dateString) return '--'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const downFile  = (data) => {

    return new Promise<void>((resolve, reject) => {
        try {
            const dataServer = 'http://223.2.34.8:30901'
            window.location.href = dataServer + '/userRes/downloadDataItem/' + data.filePath
            resolve()
        } catch (error) {
            reject(error)
        }
    })
}


//demo，可删除
const fileLeve1 = {
  "status": 1,
  "message": "成功获取数据列表",
  "data": {
    "files": [
      {
        "fileName": "doc",
        "isDir": true,
        "filePath":"/doc",
        "size":null,
        "lastModified": "2025-09-04T10:25:00Z",
        },
      {
        "fileName": "file.pdf",
        "isDir": false,
        "filePath": "/file.pdf",
        "size": "12MB",
        "lastModified": "2025-09-04T10:25:00Z"
      } 
    ]
  }
}

const fileLeve2 = {
  "status": 1,
  "message": "成功获取数据列表",
  "data": {
    "files": [
      {
        "fileName": "file.pdf",
        "id":"",
        "isDir": false,
        "filePath": "/docs/report.pdf",
        "size": "12MB",
        "createTime": "2025-09-04T10:25:00Z",
        "lastModified": "2025-09-04T10:25:00Z"
      }
    ]
  }
}

onMounted(async () => {
    // 初始化时加载文件列表
    let filePath = ref("")
    await getFileList(filePath.value)
})
</script>

<style scoped>
/* Mission Control Interface Styling */
.mission-control {
  background: transparent;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  color: #F8FAFC;
}

/* Control Header */
.control-header {
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(99, 102, 241, 0.3);
  border-radius: 16px 16px 0 0;
  padding: 1.5rem;
  margin-bottom: 1rem;
}

.navigation-section {
  display: flex;
  align-items: center;
  gap: 2rem;
  margin-bottom: 1rem;
}

.nav-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.8), rgba(139, 92, 246, 0.8));
  border: none;
  border-radius: 12px;
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.nav-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(99, 102, 241, 0.4);
}

.nav-icon {
  font-size: 1.2rem;
}

.location-display {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.location-label {
  font-weight: 600;
  color: #CBD5E1;
  font-size: 1rem;
}

.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.breadcrumb-item {
  padding: 0.25rem 0.75rem;
  background: rgba(30, 41, 59, 0.6);
  border-radius: 8px;
  font-size: 0.9rem;
  color: #94A3B8;
  transition: all 0.2s ease;
}

.breadcrumb-item.active {
  background: rgba(99, 102, 241, 0.3);
  color: #6366F1;
  font-weight: 600;
}

.breadcrumb-separator {
  color: #64748B;
  font-size: 0.8rem;
  margin: 0 0.25rem;
}

.divider-line {
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.5), transparent);
  margin: 1rem 0;
}

.controls-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 2rem;
}

/* Search Control */
.search-control {
  flex: 1;
  max-width: 400px;
}

.search-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(99, 102, 241, 0.3);
  border-radius: 12px;
  padding: 0.75rem;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.search-wrapper:focus-within {
  border-color: rgba(99, 102, 241, 0.6);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.search-icon {
  margin-right: 0.75rem;
  font-size: 1.1rem;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #F8FAFC;
  font-size: 0.95rem;
  font-weight: 500;
}

.search-input::placeholder {
  color: #64748B;
}

.clear-search {
  background: rgba(239, 68, 68, 0.2);
  border: 1px solid rgba(239, 68, 68, 0.4);
  border-radius: 6px;
  color: #EF4444;
  padding: 0.25rem 0.5rem;
  cursor: pointer;
  font-size: 0.8rem;
  margin-left: 0.5rem;
  transition: all 0.2s ease;
}

.clear-search:hover {
  background: rgba(239, 68, 68, 0.3);
}

/* Action Controls */
.action-controls {
  display: flex;
  gap: 1rem;
}

.control-btn {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
  backdrop-filter: blur(10px);
}

.control-btn:hover {
  transform: translateY(-2px);
}

.refresh-btn {
  background: linear-gradient(135deg, #10B981, #06B6D4);
  color: white;
}

.refresh-btn:hover {
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.4);
}

.upload-btn {
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  color: white;
}

.upload-btn:hover {
  box-shadow: 0 8px 25px rgba(99, 102, 241, 0.4);
}

.folder-btn {
  background: linear-gradient(135deg, #F59E0B, #F97316);
  color: white;
}

.folder-btn:hover {
  box-shadow: 0 8px 25px rgba(245, 158, 11, 0.4);
}

.btn-icon {
  font-size: 1rem;
}

.btn-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.6s ease;
}

.control-btn:hover .btn-glow {
  left: 100%;
}

/* Main Control Area */
.control-main {
  display: flex;
  flex: 1;
  gap: 1rem;
}

.data-grid-section {
  flex: 1;
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 16px;
  overflow: hidden;
  backdrop-filter: blur(20px);
}

.grid-container {
  padding: 2rem;
  height: 100%;
}

.satellite-data-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.5rem;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(99, 102, 241, 0.5) transparent;
}

.satellite-data-grid::-webkit-scrollbar {
  width: 8px;
}

.satellite-data-grid::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.3);
  border-radius: 4px;
}

.satellite-data-grid::-webkit-scrollbar-thumb {
  background: rgba(99, 102, 241, 0.5);
  border-radius: 4px;
}

/* Data File Cards */
.data-file-card {
  position: relative;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 16px;
  padding: 1.5rem;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
  backdrop-filter: blur(15px);
}

.data-file-card:hover {
  transform: translateY(-3px);
  border-color: rgba(99, 102, 241, 0.4);
  box-shadow:
    0 15px 35px rgba(0, 0, 0, 0.2),
    0 0 25px rgba(99, 102, 241, 0.15);
}

.data-file-card.selected {
  border-color: rgba(99, 102, 241, 0.8);
  background: rgba(99, 102, 241, 0.1);
  box-shadow: 0 0 20px rgba(99, 102, 241, 0.3);
}

.data-file-card.is-folder {
  border-color: rgba(245, 158, 11, 0.3);
}

.data-file-card.is-folder:hover {
  border-color: rgba(245, 158, 11, 0.5);
  box-shadow:
    0 15px 35px rgba(0, 0, 0, 0.2),
    0 0 25px rgba(245, 158, 11, 0.15);
}

.file-type-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 4rem;
  height: 4rem;
  margin: 0 auto 1rem;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(99, 102, 241, 0.2);
}

.folder-icon,
.file-icon {
  font-size: 2rem;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
}

.file-info {
  text-align: center;
  margin-bottom: 1rem;
}

.file-name {
  font-size: 1rem;
  font-weight: 600;
  color: #F8FAFC;
  margin-bottom: 0.5rem;
  word-break: break-word;
  line-height: 1.3;
}

.file-path {
  font-size: 0.75rem;
  color: #64748B;
  margin-bottom: 0.5rem;
  word-break: break-all;
  line-height: 1.3;
}

.path-label {
  color: #6366F1;
  font-weight: 600;
}

.file-size {
  font-size: 0.8rem;
  color: #94A3B8;
  font-weight: 500;
  margin-bottom: 0.5rem;
}

.file-meta {
  border-top: 1px solid rgba(99, 102, 241, 0.1);
  padding-top: 0.75rem;
}

.meta-item {
  font-size: 0.75rem;
  color: #64748B;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
}

.file-status {
  position: absolute;
  top: 1rem;
  right: 1rem;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  animation: pulse-dot 2s infinite;
}

.folder-status {
  background: #F59E0B;
  box-shadow: 0 0 10px rgba(245, 158, 11, 0.6);
}

.file-status {
  background: #10B981;
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.6);
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.2); }
}

.card-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(45deg, transparent, rgba(99, 102, 241, 0.1), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.data-file-card:hover .card-glow {
  opacity: 1;
}

/* Side Panel (keeping original structure but with new styling) */
aside {
  width: 300px;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 0 16px 16px 0;
  padding: 2rem;
  backdrop-filter: blur(15px);
}

/* Responsive Design */
@media (max-width: 1024px) {
  .control-main {
    flex-direction: column;
  }

  aside {
    width: 100%;
    border-radius: 16px;
  }

  .satellite-data-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  }
}

@media (max-width: 768px) {
  .controls-section {
    flex-direction: column;
    gap: 1rem;
  }

  .action-controls {
    width: 100%;
    justify-content: space-between;
  }

  .control-btn {
    flex: 1;
    justify-content: center;
  }

  .satellite-data-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 1rem;
  }

  .navigation-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
}

/* Legacy button styles for compatibility */
.move_button {
  color: #64748B;
  border: 1px solid rgba(99, 102, 241, 0.3);
  background: rgba(30, 41, 59, 0.6);
  border-radius: 12px;
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.move_button:hover {
  color: white;
  background: rgba(99, 102, 241, 0.3);
  border-color: rgba(99, 102, 241, 0.5);
  transform: translateY(-1px);
}

.back-button {
  color: #64748B;
  border: 1px solid rgba(99, 102, 241, 0.3);
  background: rgba(30, 41, 59, 0.6);
  border-radius: 12px;
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.back-button:hover {
  background: rgba(99, 102, 241, 0.2);
  border-color: rgba(99, 102, 241, 0.4);
  color: #F8FAFC;
  transform: translateY(-1px);
}
</style>