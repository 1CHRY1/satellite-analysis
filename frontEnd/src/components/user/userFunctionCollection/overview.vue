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
const canLoadMore = ref(true) // 控制是否允许加载更多
const hasMoreData = ref(true) // 是否还有更多数据

  // 历史数据demo
const historyData = ref<historyType[]>([]);


const page = ref(1)
const loadHistroy = async(pagePara = page ) => {
  if (isLoading.value || !canLoadMore.value || !hasMoreData.value) return;

  isLoading.value = true;
  canLoadMore.value = false; // 加载时禁止触发新的加载
  
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
      // 检查是否还有更多数据
      if (historyData.value.length >= TotalElement.value || res.data.records.length === 0) {
        hasMoreData.value = false;
      }
    }
  } catch(error){
    console.error('loadHistroy 报错:', error);
  } finally {
    isLoading.value = false;
    // 延迟重新启用加载，防止内容变化后立即触发
    setTimeout(() => {
      canLoadMore.value = true;
    }, 500);
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
  // 如果正在加载或不允许加载更多，直接返回
  if (isLoading.value || !canLoadMore.value || !hasMoreData.value) return;
  
  const target = event.target as HTMLElement;
  const { scrollTop, scrollHeight, clientHeight } = target;

  // 当滚动到距离底部 100px 时触发加载（增加阈值）
  if (scrollHeight - scrollTop - clientHeight < 100) {
    loadHistroy();
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
  padding: 1.5rem;
  background: transparent;
  min-height: 100%;
  position: relative;
}

/* Header Section */
.overview-header {
  margin-bottom: 1.5rem;
  position: relative;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 1.5rem;
  font-weight: 700;
  color: #F8FAFC;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  position: relative;
  padding: 1rem 1.5rem;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(15px);
  border: 1px solid rgba(14, 165, 233, 0.3);
  border-radius: 16px;
}

.section-title::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(14, 165, 233, 0.8), rgba(16, 185, 129, 0.8), transparent);
}

.title-decoration {
  width: 4px;
  height: 1.8rem;
  background: linear-gradient(180deg, #0EA5E9, #10B981);
  border-radius: 2px;
  box-shadow: 0 0 15px rgba(14, 165, 233, 0.6);
  animation: title-pulse 2s ease-in-out infinite;
}

@keyframes title-pulse {
  0%, 100% { box-shadow: 0 0 15px rgba(14, 165, 233, 0.6); }
  50% { box-shadow: 0 0 25px rgba(14, 165, 233, 0.9), 0 0 35px rgba(16, 185, 129, 0.5); }
}

.satellite-icon {
  font-size: 1.5rem;
  animation: satellite-orbit 4s ease-in-out infinite;
  filter: drop-shadow(0 0 10px rgba(14, 165, 233, 0.4));
}

@keyframes satellite-orbit {
  0%, 100% { 
    transform: translateY(0) rotate(0deg); 
    filter: drop-shadow(0 0 10px rgba(14, 165, 233, 0.4));
  }
  25% { 
    transform: translateY(-5px) rotate(10deg) translateX(3px); 
    filter: drop-shadow(0 0 15px rgba(16, 185, 129, 0.6));
  }
  50% { 
    transform: translateY(-8px) rotate(0deg); 
    filter: drop-shadow(0 0 20px rgba(14, 165, 233, 0.8));
  }
  75% { 
    transform: translateY(-5px) rotate(-10deg) translateX(-3px); 
    filter: drop-shadow(0 0 15px rgba(245, 158, 11, 0.6));
  }
}

/* Orbital Timeline Container */
.orbital-timeline-container {
  position: relative;
  max-height: calc(100vh - 280px);
  min-height: 400px;
  overflow-y: auto;
  overflow-anchor: none; /* 禁用滚动锚定 */
  border: 1px solid rgba(14, 165, 233, 0.3);
  border-radius: 20px;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(25px);
  padding: 2rem;
  scrollbar-width: thin;
  scrollbar-color: rgba(14, 165, 233, 0.5) transparent;
  box-shadow: 
    0 10px 40px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.orbital-timeline-container * {
  scroll-behavior: auto !important; /* 禁用平滑滚动 */
}

.orbital-timeline-container .orbital-path {
  contain: layout style; /* 隔离内部布局变化 */
}

.orbital-timeline-container::-webkit-scrollbar {
  width: 6px;
  transition: none; /* 禁用滚动条过渡动画 */
}

.orbital-timeline-container::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.3);
  border-radius: 3px;
  margin: 10px 0;
  transition: none;
}

.orbital-timeline-container::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(14, 165, 233, 0.6), rgba(16, 185, 129, 0.6));
  border-radius: 3px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: none; /* 禁用滚动条滑块过渡 */
}

.orbital-timeline-container::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(14, 165, 233, 0.8), rgba(16, 185, 129, 0.8));
}

/* Orbital Path */
.orbital-path {
  position: relative;
  min-height: 350px;
  padding: 1rem 0;
}

.orbital-timeline {
  position: relative;
  padding: 0 1rem;
}

.orbital-timeline::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(
    to bottom,
    transparent 0%,
    rgba(14, 165, 233, 0.6) 10%,
    rgba(16, 185, 129, 0.6) 30%,
    rgba(245, 158, 11, 0.6) 50%,
    rgba(239, 68, 68, 0.6) 70%,
    rgba(14, 165, 233, 0.6) 90%,
    transparent 100%
  );
  transform: translateX(-50%);
  z-index: 0;
  border-radius: 2px;
  box-shadow: 0 0 20px rgba(14, 165, 233, 0.3);
}

.orbital-timeline::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(to bottom, transparent, rgba(255, 255, 255, 0.1), transparent);
  transform: translateX(-50%);
  z-index: 0;
  animation: timeline-glow 3s ease-in-out infinite;
}

@keyframes timeline-glow {
  0%, 100% { opacity: 0; transform: translateX(-50%) translateY(-100%); }
  50% { opacity: 1; }
  100% { transform: translateX(-50%) translateY(100%); }
}

/* Timeline Nodes */
.timeline-node {
  position: relative;
  display: flex;
  align-items: center;
  margin-bottom: 2.5rem;
  z-index: 1;
  opacity: 0;
  animation: node-appear 0.6s ease-out forwards;
}

.timeline-node:nth-child(1) { animation-delay: 0.1s; }
.timeline-node:nth-child(2) { animation-delay: 0.2s; }
.timeline-node:nth-child(3) { animation-delay: 0.3s; }
.timeline-node:nth-child(4) { animation-delay: 0.4s; }
.timeline-node:nth-child(5) { animation-delay: 0.5s; }
.timeline-node:nth-child(n+6) { animation-delay: 0.6s; }

@keyframes node-appear {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.timeline-node:nth-child(odd) {
  flex-direction: row;
}

.timeline-node:nth-child(even) {
  flex-direction: row-reverse;
}

.timeline-node:last-child {
  margin-bottom: 1rem;
}

/* Satellite Markers */
.satellite-marker {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 1.5rem;
  z-index: 2;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  contain: layout style; /* 隔离布局，但不创建可见边界 */
  background: transparent;
  border: none;
  outline: none;
  box-shadow: none;
}

.pulse-ring {
  position: absolute;
  width: 36px;
  height: 36px;
  border: 2px solid rgba(14, 165, 233, 0.5);
  border-radius: 50%;
  animation: marker-pulse 2.5s ease-out infinite;
  will-change: transform, opacity;
  pointer-events: none;
}

.pulse-ring::before {
  content: '';
  position: absolute;
  inset: -6px;
  border: 1px solid rgba(14, 165, 233, 0.2);
  border-radius: 50%;
  animation: marker-pulse 2.5s ease-out infinite 0.5s;
  will-change: transform, opacity;
}

@keyframes marker-pulse {
  0% {
    transform: scale(1);
    opacity: 0.8;
  }
  100% {
    transform: scale(2.2);
    opacity: 0;
  }
}

.satellite-dot {
  position: relative;
  width: 18px;
  height: 18px;
  background: linear-gradient(135deg, #0EA5E9, #10B981);
  border-radius: 50%;
  box-shadow:
    0 0 25px rgba(14, 165, 233, 0.7),
    0 0 50px rgba(14, 165, 233, 0.3),
    inset 0 2px 4px rgba(255, 255, 255, 0.3);
  z-index: 3;
  border: 2px solid rgba(255, 255, 255, 0.2);
}

.satellite-dot::after {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  width: 6px;
  height: 6px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 50%;
}

/* Mission Cards */
.mission-card {
  flex: 1;
  max-width: 420px;
  background: rgba(30, 41, 59, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(14, 165, 233, 0.25);
  border-radius: 18px;
  padding: 1.25rem 1.5rem;
  transition: box-shadow 0.4s, border-color 0.4s; /* 只过渡不影响布局的属性 */
  position: relative;
  overflow: hidden;
  box-shadow: 
    0 4px 20px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  contain: content; /* 隔离内部变化 */
}

.mission-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(14, 165, 233, 0.8), rgba(16, 185, 129, 0.6), transparent);
}

.mission-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 80px;
  height: 80px;
  background: radial-gradient(circle at top right, rgba(14, 165, 233, 0.1) 0%, transparent 70%);
  pointer-events: none;
}

.mission-card:hover {
  box-shadow:
    0 25px 50px rgba(0, 0, 0, 0.35),
    0 0 40px rgba(14, 165, 233, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border-color: rgba(14, 165, 233, 0.5);
}

/* Card Header */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  gap: 0.75rem;
}

.mission-type {
  padding: 0.35rem 0.9rem;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.9), rgba(2, 132, 199, 0.9));
  color: white;
  border-radius: 20px;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  box-shadow: 
    0 2px 10px rgba(14, 165, 233, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.timestamp {
  font-size: 0.75rem;
  color: #94A3B8;
  font-family: 'SF Mono', 'Consolas', monospace;
  background: rgba(15, 23, 42, 0.6);
  padding: 0.3rem 0.6rem;
  border-radius: 8px;
  border: 1px solid rgba(14, 165, 233, 0.2);
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.timestamp::before {
  content: '⏱';
  font-size: 0.65rem;
}

/* Mission Details */
.mission-details {
  margin-bottom: 1rem;
}

.project-name {
  font-size: 1.15rem;
  font-weight: 700;
  color: #F8FAFC;
  margin-bottom: 0.75rem;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mission-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-item {
  display: flex;
  gap: 0.5rem;
  font-size: 0.85rem;
  align-items: flex-start;
}

.info-item .label {
  color: #0EA5E9;
  font-weight: 600;
  min-width: 3rem;
  flex-shrink: 0;
}

.info-item .value {
  color: #CBD5E1;
  flex: 1;
  line-height: 1.4;
}

.info-item.description {
  padding: 0.5rem;
  background: rgba(15, 23, 42, 0.4);
  border-radius: 8px;
  border-left: 2px solid rgba(14, 165, 233, 0.4);
}

.info-item.description .value {
  color: #94A3B8;
  font-style: italic;
  font-size: 0.8rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Mission Status */
.mission-status {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding-top: 0.75rem;
  border-top: 1px solid rgba(14, 165, 233, 0.15);
  font-size: 0.75rem;
  color: #10B981;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-indicator {
  position: relative;
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #10B981;
  box-shadow: 0 0 12px rgba(16, 185, 129, 0.7);
  animation: status-pulse 2s ease-in-out infinite;
  will-change: transform, opacity;
  contain: strict;
}

.status-indicator::before {
  content: '';
  position: absolute;
  inset: -3px;
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 50%;
  animation: status-ring 2s ease-in-out infinite;
  will-change: transform, opacity;
}

@keyframes status-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(0.85); opacity: 0.8; }
}

@keyframes status-ring {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.5); opacity: 0; }
}

/* Empty State */
.empty-space {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 350px;
  text-align: center;
  color: #64748B;
  padding: 2rem;
  position: relative;
}

.empty-space::before {
  content: '';
  position: absolute;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  animation: empty-glow 4s ease-in-out infinite;
}

@keyframes empty-glow {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.2); opacity: 0.8; }
}

.floating-satellite {
  font-size: 4.5rem;
  margin-bottom: 1.5rem;
  animation: satellite-float 5s ease-in-out infinite;
  filter: drop-shadow(0 0 30px rgba(14, 165, 233, 0.4));
  position: relative;
  z-index: 1;
}

@keyframes satellite-float {
  0%, 100% { 
    transform: translateY(0) rotate(-5deg); 
    filter: drop-shadow(0 0 30px rgba(14, 165, 233, 0.4));
  }
  25% { 
    transform: translateY(-15px) rotate(0deg) translateX(10px); 
    filter: drop-shadow(0 0 40px rgba(16, 185, 129, 0.5));
  }
  50% { 
    transform: translateY(-20px) rotate(5deg); 
    filter: drop-shadow(0 0 50px rgba(14, 165, 233, 0.6));
  }
  75% { 
    transform: translateY(-15px) rotate(0deg) translateX(-10px); 
    filter: drop-shadow(0 0 40px rgba(245, 158, 11, 0.5));
  }
}

.empty-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #CBD5E1;
  margin-bottom: 0.5rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 1;
}

.empty-subtitle {
  font-size: 0.95rem;
  color: #64748B;
  position: relative;
  z-index: 1;
}

/* Node Colors - Enhanced */
.timeline-node.node-0 .satellite-dot {
  background: linear-gradient(135deg, #0EA5E9, #0284C7);
  box-shadow: 0 0 25px rgba(14, 165, 233, 0.7), 0 0 50px rgba(2, 132, 199, 0.3);
}

.timeline-node.node-0 .pulse-ring {
  border-color: rgba(14, 165, 233, 0.5);
}

.timeline-node.node-1 .satellite-dot {
  background: linear-gradient(135deg, #10B981, #06B6D4);
  box-shadow: 0 0 25px rgba(16, 185, 129, 0.7), 0 0 50px rgba(6, 182, 212, 0.3);
}

.timeline-node.node-1 .pulse-ring {
  border-color: rgba(16, 185, 129, 0.5);
}

.timeline-node.node-2 .satellite-dot {
  background: linear-gradient(135deg, #F59E0B, #F97316);
  box-shadow: 0 0 25px rgba(245, 158, 11, 0.7), 0 0 50px rgba(249, 115, 22, 0.3);
}

.timeline-node.node-2 .pulse-ring {
  border-color: rgba(245, 158, 11, 0.5);
}

.timeline-node.node-3 .satellite-dot {
  background: linear-gradient(135deg, #EF4444, #EC4899);
  box-shadow: 0 0 25px rgba(239, 68, 68, 0.7), 0 0 50px rgba(236, 72, 153, 0.3);
}

.timeline-node.node-3 .pulse-ring {
  border-color: rgba(239, 68, 68, 0.5);
}

/* Responsive Design */
@media (max-width: 900px) {
  .satellite-overview {
    padding: 1rem;
  }

  .orbital-timeline-container {
    padding: 1.5rem;
  }

  .mission-card {
    max-width: 350px;
  }

  .satellite-marker {
    margin: 0 1rem;
  }
}

@media (max-width: 768px) {
  .timeline-node {
    flex-direction: column !important;
    text-align: center;
    margin-bottom: 2rem;
  }

  .satellite-marker {
    margin: 0.75rem 0;
    order: -1;
  }

  .mission-card {
    max-width: 100%;
    margin: 0;
  }

  .orbital-timeline::before,
  .orbital-timeline::after {
    display: none;
  }

  .section-title {
    font-size: 1.2rem;
    padding: 0.75rem 1rem;
  }

  .title-decoration {
    height: 1.5rem;
  }
}

@media (max-width: 480px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .mission-type {
    font-size: 0.65rem;
  }

  .timestamp {
    font-size: 0.7rem;
  }

  .project-name {
    font-size: 1rem;
  }
}
</style>
  