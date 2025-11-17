<template>
    <div>
        <projectsBg
            class="absolute inset-0 z-0 h-full w-full overflow-hidden bg-[radial-gradient(ellipse_at_bottom,_#1b2735_0%,_#090a0f_100%)]">
        </projectsBg>
        <div class="relative z-10 flex flex-col items-center justify-center">
            <div class="my-10 flex w-[50vw] flex-col items-center justify-center">
                <img src="@/assets/image/toolsEstablish.png" class="h-12 w-fit" alt="" />
                <div class="searchContainer mt-6 w-[100%]">
                    <div class="model_research">
                         <input type="text" autocomplete="false" :placeholder="t('toolpage.searchbar')"
                            class="model_research_input" v-model="searchInput" style="font-size: 14px"
                            @keyup.enter="researchTools" />

                        <el-button type="primary" color="#049f40" style="
                                border-left: none;
                                border-top-left-radius: 0px;
                                border-bottom-left-radius: 0px;
                                border-color: #ffffff;
                                font-size: 14px;
                                height: 2rem;
                            " @click="researchTools">
                            <Search class="h-4" />
                            {{t("toolpage.search")}}
                        </el-button>
                    </div>
                    <el-button type="primary" color="#049f40" style="
                            margin-left: 10px;
                            border-color: #049f40;
                            font-size: 14px;
                            height: 2rem;
                        " @click="createToolView = !createToolView">
                        {{ createToolView ? t('toolpage.canceltool') : t('toolpage.createtool')}}
                    </el-button>
                    <el-button type="primary" color="#049f40" :disabled="searchVisible" style="
                            margin-left: 10px;
                            border-color: #049f40;
                            font-size: 14px;
                            height: 2rem;
                        " @click="toggleMyOnly">
                        {{ myOnly ? t('toolpage.alltool') : t('toolpage.mytool') }}
                    </el-button>
                </div>
            </div>

            <!-- 计算合适的高度，header是56px，搜索184px，即 50vh = 1/2x + 56px + 184px，x为容器高度，这样创建弹窗在容器内居中就会在视口居中 -->
            <div class="relative flex h-[calc(100vh-184px-56px)] w-full justify-center">
                <!-- 计算合适的宽度 -->
                <div v-if="!createToolView" class="grid gap-12 overflow-y-auto p-3"
                     :style="{ gridTemplateColumns: `repeat(${columns}, ${cardWidth}px)` }">
                  <ToolCard v-for="tool in (searchVisible ? searchedTools : (myOnly ? toolList.filter(isMyTool) : toolList))"
                            :key="tool.toolId || tool.projectId || tool.name"
                            :tool="tool"
                            :owner-name="userNameMap[(tool.userId||'').toString()] || tool.createUserName || tool.userName || ''"
                            :owner-email="userEmailMap[(tool.userId||'').toString()] || tool.createUserEmail || ''"
                            @click="enterTool(tool)"
                            @deleted="reloadTools" />
                </div>

                <!-- 创建工具的卡片（与 projects.vue 一致样式） -->
                <div v-if="createToolView" class="absolute top-[calc(50vh-240px-206px)] flex opacity-80">
                    <div class="w-96 rounded-xl bg-gray-700 p-6 text-white shadow-xl">
                        <h2 class="mb-4 text-center text-xl font-semibold">{{ t("toolpage.createtool") }}</h2>

                        <!-- 工具名称 -->
                        <label class="mb-1 flex text-sm">
                            <div style="color: red; margin-right: 4px">*</div>
                            {{t("toolpage.toolname")}}
                        </label>
                        <input v-model="newProject.projectName" type="text"
                            class="w-full rounded bg-gray-800 p-2 text-white focus:ring-2 focus:ring-green-400 focus:outline-none" />

                        <!-- 运行环境 -->
                        <label class="mt-3 mb-1 flex text-sm">
                            <div style="color: red; margin-right: 4px">*</div>
                            {{t("toolpage.envir")}}
                        </label>
                        <select v-model="newProject.environment"
                            class="w-full rounded bg-gray-800 p-2 text-white focus:ring-2 focus:ring-green-400 focus:outline-none">
                            <option value="Python2_7" disabled>Python 2.7 ( 暂不支持 )</option>
                            <option value="Python3_9">Python 3.9</option>
                            <option value="Python3_10" disabled>Python 3.10 ( 暂不支持 )</option>
                        </select>

                        <!-- 描述 -->
                        <label class="mt-3 mb-1 flex text-sm">
                            <div style="color: red; margin-right: 4px">*</div>
                            描述
                        </label>
                        <textarea v-model="newProject.description"
                            class="h-20 w-full rounded bg-gray-800 p-2 text-white focus:ring-2 focus:ring-green-400 focus:outline-none"></textarea>

                        <!-- 按钮 -->
                        <div class="mt-6 flex justify-center">
                            <button @click="create" class="mx-2 rounded-xl bg-green-600 px-6 py-2 hover:bg-green-500"
                                :class="{ 'cursor-pointer': !createLoading }" :disabled="createLoading">
                                <div v-if="createLoading" class="is-loading flex items-center">
                                    <Loading class="h-4 w-4 mr-1" />{{t("toolpage.button.create")}}
                                </div>
                                <div v-else>{{t("toolpage.button.create")}}</div>
                            </button>
                            <button @click="createToolView = false"
                                class="mx-2 !ml-4 rounded-xl bg-gray-500 px-6 py-2 hover:bg-gray-400 cursor-pointer">
                                {{t("toolpage.button.cancel")}}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, type Ref, computed } from 'vue'
import projectsBg from '@/components/projects/projectsBg.vue'
import { Search } from 'lucide-vue-next'
import { getAllTools } from '@/api/http/tool/tool.api'
import { getProjects } from '@/api/http/analysis'
import ToolCard from '@/components/tools/ToolCard.vue'
import { ElMessage } from 'element-plus'
import { getUsers, updateRecord } from '@/api/http/user'
import { useUserStore } from '@/store'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { createProject } from '@/api/http/analysis'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const userId = computed(() => userStore?.user?.id?.value || localStorage.getItem('userId'))
const searchInput = ref('')

/**
 * 搜索功能模块
 */
const myOnly = ref(false)
const searchedTools: Ref<any[]> = ref([])
const searchVisible = ref(false)

// 支持搜索工具名称、创建人和工具描述
const researchTools = () => {
  const q = searchInput.value?.trim()
  if (q && q.length > 0) {
    searchedTools.value = toolList.value.filter((item: any) =>
      // 兼容项目表字段：projectName -> name
      (item.toolName || item.name || item.projectName || '').includes(q) ||
      String(item.userId ?? item.createUser ?? '').includes(q) ||
      (item.description || '').includes(q)
    )
    searchVisible.value = true
  } else {
    searchVisible.value = false
  }
}

// 查看我的工具
const toggleMyOnly = () => {
  myOnly.value = !myOnly.value
}

/**
 * 工具列表模块
 */
const toolList: Ref<any[]> = ref([])
const userNameMap = ref<Record<string, string>>({})
const userEmailMap = ref<Record<string, string>>({})
const cardWidth = 300
const gap = 48
const columns = ref(1)
const createToolView = ref(false)
const createLoading = ref(false)
const newProject = ref({
  projectName: '',
  environment: 'Python3_9',
  description: '',
})

const updateColumns = () => {
    const containerWidth = window.innerWidth - 32 // 预留 100px 余量
    columns.value = Math.max(1, Math.floor(containerWidth / (cardWidth + gap)))
}

// 判定是否为当前用户的工具（统一转字符串避免类型不一致导致的筛选失败）
const isMyTool = (t: any) => {
  return String(t?.userId ?? t?.createUser ?? '') === String(userId.value ?? '')
}

// 进入编辑器（与在线编程行为一致）
const enterTool = (tool: any) => {
  const uid = (userId.value || '').toString()
  const owner = (tool?.userId || '').toString()
  if (owner === uid) {
    if (tool?.projectId) {
      router.push(`/project/${tool.projectId}`)
    } else {
      ElMessage.error('该工具缺少关联工程，无法打开')
    }
  } else {
    ElMessage.error(t('toolpage.message.error.entererr'))
  }
}

// creation flow removed for tools page per requirement
// 创建工具（与项目页一致，实际创建一个工程以进入在线编程）
const create = async () => {
  if (!newProject.value.projectName) {
    ElMessage.error(t('toolpage.message.error.create_name_empty'))
    return
  } else if (!newProject.value.description) {
    ElMessage.error(t('toolpage.message.error.create_description_empty'))
    return
  } else if (
    toolList.value.some((item: any) => (item.toolName || item.name) === newProject.value.projectName)
  ) {
    // 工具页无法直接校验工程重名，这里仅与工具名作弱校验
    ElMessage.error(t('toolpage.message.error.createrr_name_exist'))
    return
  }
  createLoading.value = true
  try {
    const createBody = {
      ...newProject.value,
      userId: userId.value,
      tool: true,
    }
    const createRes: any = await createProject(createBody)
    // 记录创建行为
    try {
      await updateRecord({
        userId: (userStore.user.id as any),
        actionDetail: {
          projectName: newProject.value.projectName,
          projectType: 'Project',
          description: newProject.value.description,
        },
        actionType: '创建',
      })
    } catch {}
    router.push(`/project/${createRes.projectId}`)
    ElMessage.success(t('toolpage.message.success'))
    createToolView.value = false
  } catch (e) {
    // 后端会做更严格校验
  } finally {
    createLoading.value = false
  }
}

const reloadTools = async () => {
  await fetchTools()
}

const fetchTools = async () => {
  try {
    const res = await getAllTools({ current: 1, size: 200, userId: userId.value })
    const records = res?.data?.records || []
    toolList.value = records
    await ensureUserNames()
  } catch (e) {
    ElMessage.error('获取工具列表失败')
  }
}

// 为工具作者补全昵称/邮箱映射
const ensureUserNames = async () => {
  const ids = Array.from(new Set((toolList.value || [])
    .map((t: any) => (t?.userId || '').toString())
    .filter(Boolean)))
  const pending: Promise<void>[] = []
  for (const id of ids) {
    if (!userNameMap.value[id]) {
      pending.push(
        getUsers(id)
          .then((u: any) => {
            userNameMap.value[id] = u?.userName || u?.name || u?.email || id
            if (u?.email) userEmailMap.value[id] = u.email
          })
          .catch(() => {
            userNameMap.value[id] = id
            if (!userEmailMap.value[id]) userEmailMap.value[id] = ''
          })
      )
    }
  }
  if (pending.length) await Promise.all(pending)
}

// not used in tools page

// not used in tools page

// no action log for listing

onMounted(async () => {
  await fetchTools()
  updateColumns()
  window.addEventListener('resize', updateColumns)  
})

onUnmounted(() => {
    window.removeEventListener('resize', updateColumns)
})

// online/offline style is handled per-card by is_publish
</script>

<style scoped lang="scss">
div::-webkit-scrollbar {
    width: none !important;
}

div {
    scrollbar-width: none !important;
    scrollbar-color: rgba(37, 190, 255, 0.332) transparent !important;
}

.searchContainer {
    // height: 60px;
    // height: fit-content;
    display: flex;
    align-items: center;
    justify-content: center;
}

.model_research {
    position: relative;
    font-size: 14px;
    display: inline-flex;
    width: 100%;
}

.model_research_input {
    // -webkit-appearance: none;
    background-color: rgba(0, 0, 0, 0.6);
    background-image: none;
    border-radius: 4px 0px 0px 4px;
    border: 1px solid #ffffff;
    border-right: none;

    box-sizing: border-box;
    color: #ffffff;
    display: inline-block;
    font-size: inherit;
    height: 2rem;
    line-height: 2rem;
    outline: 0;
    padding: 0 15px;
    transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
    width: 100%;
}

.model_research_input::-webkit-input-placeholder {
    color: #c3c5ca;
}
</style>
