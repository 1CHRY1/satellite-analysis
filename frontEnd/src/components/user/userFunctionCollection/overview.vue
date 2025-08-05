<template>
    <div class="p-4">
      <section class="mb-4">
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
                  <!-- <h1 class="text-xs">{{ project.memberList.length }}</h1> -->
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
      </section>
  
      <section>
        <h2 class="text-xl font-semibold mb-4">
          {{ t("userpage.userFunction.dynamic") }}
        </h2>
        <div class="block">
          <el-timeline>
            <el-timeline-item
              v-for="item in historyData"
              :timestamp="item.createTime"
              placement="top"
              :key="item.createTime"
            >
              <el-card>
                <h4 class="font-bold">{{ item.type }}</h4>
                <div style="display: flex; flex-direction: row; justify-content: space-between;">
                  <p class="mt-2">{{ item.content }}</p>
                  <div v-show="!resolvedList.includes(item.id) && item.type === '项目邀请' && item.state === '未读'" class="mt-2">
                    <el-link class="mr-2" :underline="false" @click="handleInvite('同意', item.id)">同意</el-link>
                    <el-link :underline="false" @click="handleInvite('拒绝', item.id)">拒绝</el-link>
                  </div>
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
        <el-button
          style="
            width: 100%;
            height: 30px;
            margin-top: 1%;
            border-color: white;
            background-color: white;
          "
          @click="loadmoreAll"
          v-if="TotalElement > 5 && historyData.length < TotalElement"
        >
          {{ t("userpage.userFunction.load") }}
        </el-button>
      </section>
    </div>
  </template>
  
<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { getUserProjects } from "@/api/http/analysis";

  
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

const projectList = ref<Project[]>([])

  // // 项目数据
  // const topProjects = ref<projectSet[]>([
  //   {
  //     projectId: projectList.value.projectId,
  //     name: "示例项目A",
  //     description: "这是一个示例项目描述",
  //     viewTimes: 10,
  //     memberList:[]
  //   },
  //   {
  //     projectId: 2,
  //     name: "示例项目B", 
  //     description: "另一个示例项目描述",
  //     viewTimes: 5,
  //     memberList:[]
  //   }
  // ]);
  
  // 历史数据
  const historyData = ref([

    {
      id: 2,
      type: "数据上传",
      content: "成功上传了新的卫星数据",
      createTime: "2024-05-30 15:30:00",
      state: "已读"
    }
  ]);
  
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
  
  // 加载更多
  function loadmoreAll() {
    console.log("加载更多数据");
    // 这里可以添加加载更多数据的逻辑
  }

  onMounted(async () => {
    projectList.value = await getUserProjects()
})
  </script>
  
  <style lang="less" scoped></style>
  