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
                <div class="grid gap-12 overflow-y-auto p-3"
                     :style="{ gridTemplateColumns: `repeat(${columns}, ${cardWidth}px)` }">
                  <ToolCard v-for="tool in (searchVisible ? searchedTools : (myOnly ? toolList.filter(t => (t.userId||'') === (userId.value || '').toString()) : toolList))"
                            :key="tool.toolId || tool.projectId || tool.name"
                            :tool="tool"
                            @click="enterTool(tool)" />
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
import ToolCard from '@/components/tools/ToolCard.vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

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
      (item.toolName || item.name || '').includes(q) ||
      (item.userId || '').includes(q) ||
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
const cardWidth = 300
const gap = 48
const columns = ref(1)

const updateColumns = () => {
    const containerWidth = window.innerWidth - 32 // 预留 100px 余量
    columns.value = Math.max(1, Math.floor(containerWidth / (cardWidth + gap)))
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

const reloadTools = async () => {
  await fetchTools()
}

const fetchTools = async () => {
  try {
    const res = await getAllTools({ current: 1, size: 200, userId: userId.value })
    const records = res?.data?.records || []
    toolList.value = records
  } catch (e) {
    ElMessage.error('获取工具列表失败')
  }
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
