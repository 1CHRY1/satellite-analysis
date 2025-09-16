<template>
    <div class="p-4">
      <!-- <section class="mb-4">
        <h2 class="text-xl font-semibold mb-4">
          {{ t("userpage.userFunction.recent") }}
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div
            v-for="(project, index) in projectList"
            :key="index"
            class="h-48 rounded-lg border bg-card text-card-foreground shadow-sm relative"
            data-v0-t="card"
          >
            <div class="p-6 h-full">
              <a
                @click="toProject(project.projectId)"
                class="text-blue-800 hover:text-blue-500 font-bold"
                rel="ugc"
                style="cursor: pointer; display: inline"
              >
                {{ project.projectName }}
              </a>
              <h1 class="text-muted-foreground mt-1 text-black">{{ project.description }}</h1>
  
              <div
                class="flex justify-between items-center mt-8 absolute bottom-4 right-4"
              >
                <div class="flex items-center space-x-2">
                  <font-awesome-icon
                    class="relative top-0 text-xs"
                    :icon="['fas', 'eye']"
                  />
                  <h1 class="text-xs text-black">{{ project.createTime }}</h1>
                  <font-awesome-icon
                    class="relative top-0 pl-2 text-xs"
                    :icon="['fas', 'code-compare']"
                  />
                  <h1 class="text-xs">{{ project.memberList.length }}</h1> 
                </div>
              </div>
            </div>
          </div>
          <div v-if="projectList.length === 0">
            <h3 class="text-sl font-semibold pl-8">
              {{ t("userpage.userFunction.emptyItem") }}
            </h3>
          </div>
        </div>
      </section> -->
  
      <section
        ref="scrollContainer"
        class="max-h-[600px] overflow-y-auto border border-gray-200 rounded-lg p-4"
        @scroll="handleScroll"
      >
        <h2 class="text-xl font-semibold mb-4 text-black sticky top-0 bg-white z-10">
          {{ t("userpage.userFunction.dynamic") }}
        </h2>
        <div class="block">
          <el-timeline>
            <el-timeline-item
              v-for="item in historyData"
              :timestamp="item.actionTime"
              placement="top"
              :key="item.actionTime"
            >
              <el-card>
                <h1 class="font-bold">{{ item.actionType }} {{ item.actionDetail.projectName }}</h1>
                <div style="display: flex; flex-direction: column; justify-content: space-between;">
                  <p class="mt-2">类型： {{ item.actionDetail.projectType }}</p>
                  <p class="mt-2">描述： {{ item.actionDetail.description}}</p>
                  <!-- <div v-show="!resolvedList.includes(item.actionId) && item.ActionType === '项目邀请' " class="mt-2">
                    <el-link class="mr-2" :underline="false" @click="handleInvite('同意', item.id)">同意</el-link>
                    <el-link :underline="false" @click="handleInvite('拒绝', item.id)">拒绝</el-link>
                  </div> -->
                </div>
                
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <div v-if="historyData.length === 0">
            <h3 class="text-sl font-semibold pl-8">
              {{ t("userpage.userFunction.emptyState") }}
            </h3>
          </div>
        </div>
        <!-- Loading indicator -->
        <!-- <div v-if="isLoading" class="text-center py-4">
          <el-loading-spinner />
          <p class="text-gray-500 mt-2">{{ "加载中..." }}</p>
        </div> -->

        <!-- End of data indicator -->
        <!-- <div v-else-if="historyData.length >= TotalElement && TotalElement > 0" class="text-center py-4">
          <p class="text-gray-500">{{ "没有更多数据了" }}</p>
        </div> -->
      </section>
    </div>
  </template>
  
<script setup lang="ts">
import { onMounted, ref, nextTick } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { getUserProjects } from "@/api/http/analysis";
import { getHistoryData } from "@/api/http/user";
import { useUserStore } from "@/store";

const userStore = useUserStore()
const router = useRouter();
const { t } = useI18n();
  
interface projectSet {
  projectId:any,
  name:string,
  description:string,
  viewTimes:number,
  memberList: string[]
}
type Project = {
    createTime: string
    createUser: string
    createUserEmail: string
    createUserName: string
    description: string
    environment: string
    joinedUsers: Array<string>
    packages: string
    projectName: string
    projectId: any
}

interface historyType  {
    actionId: number,
    actionType: string,            
    actionDetail:{
      projectName: string,
      projectType: string,
      description?: string 
    },
    actionTime: string
}

const projectList = ref<Project[]>([])
const scrollContainer = ref<HTMLElement>()
const isLoading = ref(false)

  // 历史数据demo
const historyData = ref<historyType[]>([]);


const page = ref(1)
const loadHistroy = async(pagePara = page ) => {
  if (isLoading.value) return;

  isLoading.value = true;
  let param = {
      userId : userStore.user.id,
      page: pagePara.value,
      pageSize: "5",
      asc: false,
      sortField: "actionTime",
      };
  try{
    let res = await getHistoryData(param)
    console.log(res);

    if (res.status == 1){
      res.data.records.sort((a,b) => b.actionTime.localeCompare(a.actionTime))
      historyData.value.push(...res.data.records)
      console.log(historyData.value)
      page.value += 1
      // Update total count if available
      if (res.data.total) {
        TotalElement.value = res.data.total;
      }
    }
  } catch(error){
    console.error('loadHistroy 报错:', error);
  } finally {
    isLoading.value = false;
  }
}

  // 已处理的项目ID列表
const resolvedList = ref<number[]>([]);
  
  // 总元素数量
const TotalElement = ref(10);
  
  // 跳转到项目详情
function toProject(id: number) {
    router.push(`/project/${id}`);
  }
  
  // 处理邀请
function handleInvite(action: string, id: number) {
    resolvedList.value.push(id);
    console.log(`处理邀请: ${action}, ID: ${id}`);
  }
  
  // 处理滚动事件，实现无限滚动
function handleScroll(event: Event) {
  const target = event.target as HTMLElement;
  const { scrollTop, scrollHeight, clientHeight } = target;

  // 当滚动到距离底部 50px 时触发加载
  if (scrollHeight - scrollTop - clientHeight < 50) {
    // 检查是否还有更多数据要加载
    if (historyData.value.length < TotalElement.value && !isLoading.value) {
      loadHistroy();
    }
  }
}

onMounted(async () => {
  try {
    // projectList.value = await getUserProjects()
    await loadHistroy()
    console.log('historyData:', historyData.value)
  } catch (err) {
    console.error('onMounted 里异步流程出错:', err)
  }
})
  </script>
  
  <style scoped>
  /* Custom scrollbar styling */
  section {
    scrollbar-width: thin;
    scrollbar-color: #cbd5e0 #f7fafc;
  }

  section::-webkit-scrollbar {
    width: 6px;
  }

  section::-webkit-scrollbar-track {
    background: #f7fafc;
    border-radius: 3px;
  }

  section::-webkit-scrollbar-thumb {
    background: #cbd5e0;
    border-radius: 3px;
  }

  section::-webkit-scrollbar-thumb:hover {
    background: #a0aec0;
  }
  </style>
  