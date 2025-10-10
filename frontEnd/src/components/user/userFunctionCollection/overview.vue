<template>
    <div class="satellite-overview">
      <div class="overview-header">
        <h2 class="section-title">
          <div class="title-decoration"></div>
          {{ t("userpage.userFunction.dynamic") }}
          <div class="satellite-icon">🛰️</div>
        </h2>
      </div>

      <section
        ref="scrollContainer"
        class="orbital-timeline-container"
        @scroll="handleScroll"
      >
        <div class="orbital-path">
          <div class="orbital-timeline" v-if="historyData.length > 0">
            <div
              v-for="(item, index) in historyData"
              :key="item.actionTime"
              class="timeline-node"
              :class="`node-${index % 4}`"
            >
              <div class="satellite-marker">
                <div class="pulse-ring"></div>
                <div class="satellite-dot"></div>
              </div>

              <div class="mission-card">
                <div class="card-header">
                  <div class="mission-type">{{ item.actionType }}</div>
                  <div class="timestamp">{{ formatTime(item.actionTime) }}</div>
                </div>

                <div class="mission-details">
                  <h3 class="project-name">{{ item.actionDetail.projectName }}</h3>
                  <div class="mission-info">
                    <div class="info-item">
                      <span class="label">类型：</span>
                      <span class="value">{{ item.actionDetail.projectType }}</span>
                    </div>
                    <div class="info-item description">
                      <span class="label">描述：</span>
                      <span class="value">{{ item.actionDetail.description }}</span>
                    </div>
                  </div>
                </div>

                <div class="mission-status">
                  <div class="status-indicator active"></div>
                  <span>Mission Active</span>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="empty-space">
            <div class="floating-satellite">🛰️</div>
            <h3 class="empty-title">{{ t("userpage.userFunction.emptyState") }}</h3>
            <p class="empty-subtitle">暂无卫星任务数据</p>
          </div>
        </div>
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

  // 格式化时间显示
function formatTime(timeString: string) {
  const date = new Date(timeString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
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
/* Satellite Overview Styling */
.satellite-overview {
  padding: 2rem;
  background: transparent;
  min-height: 100%;
}

.overview-header {
  margin-bottom: 2rem;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 2rem;
  font-weight: 700;
  color: #F8FAFC;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  position: relative;
}

.title-decoration {
  width: 4px;
  height: 2rem;
  background: linear-gradient(45deg, #6366F1, #10B981);
  border-radius: 2px;
  box-shadow: 0 0 10px rgba(99, 102, 241, 0.5);
}

.satellite-icon {
  font-size: 1.5rem;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-10px) rotate(5deg); }
}

/* Orbital Timeline Container */
.orbital-timeline-container {
  position: relative;
  max-height: 600px;
  overflow-y: auto;
  border: 1px solid rgba(99, 102, 241, 0.3);
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.3);
  backdrop-filter: blur(20px);
  padding: 2rem;
  scrollbar-width: thin;
  scrollbar-color: rgba(99, 102, 241, 0.5) transparent;
}

.orbital-timeline-container::-webkit-scrollbar {
  width: 8px;
}

.orbital-timeline-container::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.3);
  border-radius: 4px;
}

.orbital-timeline-container::-webkit-scrollbar-thumb {
  background: rgba(99, 102, 241, 0.5);
  border-radius: 4px;
}

.orbital-timeline-container::-webkit-scrollbar-thumb:hover {
  background: rgba(99, 102, 241, 0.7);
}

/* Orbital Path */
.orbital-path {
  position: relative;
  min-height: 400px;
}

.orbital-timeline {
  position: relative;
}

.orbital-timeline::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(99, 102, 241, 0.5),
    rgba(16, 185, 129, 0.5),
    rgba(245, 158, 11, 0.5),
    transparent
  );
  transform: translateX(-50%);
  z-index: 0;
}

/* Timeline Nodes */
.timeline-node {
  position: relative;
  display: flex;
  align-items: center;
  margin-bottom: 3rem;
  z-index: 1;
}

.timeline-node:nth-child(odd) {
  flex-direction: row;
}

.timeline-node:nth-child(even) {
  flex-direction: row-reverse;
}

/* Satellite Markers */
.satellite-marker {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 2rem;
  z-index: 2;
}

.pulse-ring {
  position: absolute;
  width: 30px;
  height: 30px;
  border: 2px solid rgba(99, 102, 241, 0.4);
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

.satellite-dot {
  position: relative;
  width: 16px;
  height: 16px;
  background: linear-gradient(45deg, #6366F1, #10B981);
  border-radius: 50%;
  box-shadow:
    0 0 20px rgba(99, 102, 241, 0.6),
    inset 0 2px 4px rgba(255, 255, 255, 0.2);
  z-index: 3;
}

/* Mission Cards */
.mission-card {
  flex: 1;
  max-width: 400px;
  background: rgba(30, 41, 59, 0.7);
  backdrop-filter: blur(15px);
  border: 1px solid rgba(99, 102, 241, 0.3);
  border-radius: 16px;
  padding: 1.5rem;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.mission-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.8), transparent);
  animation: scan 3s linear infinite;
}

@keyframes scan {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.mission-card:hover {
  transform: translateY(-5px);
  box-shadow:
    0 20px 40px rgba(0, 0, 0, 0.3),
    0 0 30px rgba(99, 102, 241, 0.2);
  border-color: rgba(99, 102, 241, 0.5);
}

/* Card Header */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.mission-type {
  padding: 0.25rem 0.75rem;
  background: linear-gradient(45deg, rgba(99, 102, 241, 0.8), rgba(16, 185, 129, 0.8));
  color: white;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.3);
}

.timestamp {
  font-size: 0.8rem;
  color: #94A3B8;
  font-family: 'Courier New', monospace;
  background: rgba(15, 23, 42, 0.5);
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  border: 1px solid rgba(99, 102, 241, 0.2);
}

/* Mission Details */
.mission-details {
  margin-bottom: 1rem;
}

.project-name {
  font-size: 1.2rem;
  font-weight: 700;
  color: #F8FAFC;
  margin-bottom: 0.75rem;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.mission-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-item {
  display: flex;
  gap: 0.5rem;
  font-size: 0.9rem;
}

.info-item .label {
  color: #6366F1;
  font-weight: 600;
  min-width: 3rem;
}

.info-item .value {
  color: #CBD5E1;
  flex: 1;
}

.info-item.description .value {
  color: #94A3B8;
  font-style: italic;
}

/* Mission Status */
.mission-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(99, 102, 241, 0.2);
  font-size: 0.8rem;
  color: #10B981;
  font-weight: 600;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10B981;
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.6);
  animation: heartbeat 2s infinite;
}

@keyframes heartbeat {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

/* Empty State */
.empty-space {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
  color: #64748B;
}

.floating-satellite {
  font-size: 4rem;
  margin-bottom: 1rem;
  animation: float 4s ease-in-out infinite;
  filter: drop-shadow(0 0 20px rgba(99, 102, 241, 0.3));
}

.empty-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #CBD5E1;
  margin-bottom: 0.5rem;
}

.empty-subtitle {
  font-size: 1rem;
  color: #64748B;
}

/* Node Colors */
.timeline-node.node-0 .satellite-dot {
  background: linear-gradient(45deg, #6366F1, #8B5CF6);
}

.timeline-node.node-1 .satellite-dot {
  background: linear-gradient(45deg, #10B981, #06B6D4);
}

.timeline-node.node-2 .satellite-dot {
  background: linear-gradient(45deg, #F59E0B, #F97316);
}

.timeline-node.node-3 .satellite-dot {
  background: linear-gradient(45deg, #EF4444, #EC4899);
}

/* Responsive Design */
@media (max-width: 768px) {
  .timeline-node {
    flex-direction: column !important;
    text-align: center;
  }

  .satellite-marker {
    margin: 1rem 0;
  }

  .mission-card {
    max-width: 100%;
    margin: 0;
  }

  .orbital-timeline::before {
    display: none;
  }
}
</style>
  