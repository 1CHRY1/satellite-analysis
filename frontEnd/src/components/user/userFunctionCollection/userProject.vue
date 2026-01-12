<template>
    <div class="satellite-projects">
      <div class="projects-header">
        <h2 class="section-title">
          <div class="title-decoration"></div>
          {{ t("userpage.userFunction.recent") }}
          <div class="mission-badge">🚀</div>
        </h2>
        <div class="mission-stats">
          <div class="stat-item">
            <span class="stat-number">{{ projectList.length }}</span>
            <span class="stat-label">活跃任务</span>
          </div>
        </div>
      </div>

      <section class="missions-grid">
        <div
          v-for="(project, index) in projectList"
          :key="project.projectId"
          class="mission-card-container"
          @click="toProject(project.projectId)"
        >
          <div class="mission-card" :class="`mission-${index % 4}`">
            <div class="card-header">
              <div class="mission-status">
                <div class="status-dot active"></div>
                <span class="status-text">ACTIVE</span>
              </div>
              <div class="mission-id">#{{ String(project.projectId).padStart(4, '0') }}</div>
            </div>

            <div class="mission-content">
              <h3 class="mission-title">{{ project.projectName }}</h3>
              <p class="mission-description">{{ project.description || '暂无任务描述' }}</p>

              <div class="mission-details">
                <div class="detail-item">
                  <span class="detail-icon">👨‍🚀</span>
                  <span class="detail-text">{{ project.createUserName || 'Unknown' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-icon">📅</span>
                  <span class="detail-text">{{ formatDate(project.createTime) }}</span>
                </div>
                <div class="detail-item" v-if="project.joinedUsers && project.joinedUsers.length > 0">
                  <span class="detail-icon">👥</span>
                  <span class="detail-text">{{ project.joinedUsers.length }} 成员</span>
                </div>
              </div>
            </div>

            <div class="mission-footer">
              <div class="mission-tech">
                <div class="tech-badge" v-if="project.environment">
                  {{ project.environment }}
                </div>
                <div class="tech-badge" v-if="project.packages">
                  {{ getPackageCount(project.packages) }} 包
                </div>
              </div>
              <div class="launch-button">
                <span>LAUNCH</span>
                <div class="button-glow"></div>
              </div>
            </div>

            <div class="card-background">
              <div class="satellite-grid"></div>
              <div class="orbital-lines"></div>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div v-if="projectList.length === 0" class="empty-missions">
          <div class="empty-icon">🛰️</div>
          <h3 class="empty-title">{{ t("userpage.userFunction.emptyItem") }}</h3>
          <p class="empty-subtitle">开始您的第一个卫星分析任务</p>
          <button class="create-mission-btn">
            <span>创建新任务</span>
            <div class="btn-particles"></div>
          </button>
        </div>
      </section>
    </div>
</template>

<script setup   lang="ts">
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
  
  
  // 已处理的项目ID列表
  const resolvedList = ref<number[]>([]);
  
  // 总元素数量
  const TotalElement = ref(10);
  
  // 跳转到项目详情
  function toProject(id: number) {
    router.push(`/project/${id}`);
  }

  // 格式化日期
  function formatDate(dateString: string) {
    if (!dateString) return '--'
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN', {
      month: 'short',
      day: 'numeric'
    })
  }

  // 获取包数量
  function getPackageCount(packages: string) {
    if (!packages) return 0
    try {
      return packages.split(',').length
    } catch {
      return 0
    }
  }
  

  onMounted(async () => {
    projectList.value = await getUserProjects()
})
</script>

<style scoped>
/* Satellite Projects Styling */
.satellite-projects {
  padding: 1.5rem;
  background: transparent;
  min-height: 100%;
  position: relative;
}

/* Header */
.projects-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 1rem 1.5rem;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(15px);
  border: 1px solid rgba(14, 165, 233, 0.3);
  border-radius: 16px;
  position: relative;
  overflow: hidden;
}

.projects-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(14, 165, 233, 0.8), rgba(16, 185, 129, 0.8), transparent);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 1.5rem;
  font-weight: 700;
  color: #F8FAFC;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
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

.mission-badge {
  font-size: 1.5rem;
  animation: rocket-launch 3s ease-in-out infinite;
  filter: drop-shadow(0 0 10px rgba(245, 158, 11, 0.4));
}

@keyframes rocket-launch {
  0%, 100% { 
    transform: translateY(0) rotate(-15deg); 
    filter: drop-shadow(0 0 10px rgba(245, 158, 11, 0.4));
  }
  25% { 
    transform: translateY(-8px) rotate(-5deg) translateX(3px); 
    filter: drop-shadow(0 0 15px rgba(239, 68, 68, 0.6));
  }
  50% { 
    transform: translateY(-12px) rotate(5deg); 
    filter: drop-shadow(0 0 20px rgba(245, 158, 11, 0.8));
  }
  75% { 
    transform: translateY(-8px) rotate(0deg) translateX(-3px); 
    filter: drop-shadow(0 0 15px rgba(14, 165, 233, 0.6));
  }
}

.mission-stats {
  display: flex;
  gap: 1rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.75rem 1.25rem;
  background: rgba(14, 165, 233, 0.1);
  border-radius: 12px;
  border: 1px solid rgba(14, 165, 233, 0.3);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
}

.stat-number {
  font-size: 1.8rem;
  font-weight: 700;
  color: #0EA5E9;
  text-shadow: 0 0 15px rgba(14, 165, 233, 0.6);
  line-height: 1;
  position: relative;
  z-index: 1;
}

.stat-label {
  font-size: 0.7rem;
  color: #94A3B8;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-top: 0.25rem;
  position: relative;
  z-index: 1;
}

/* Missions Grid */
.missions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 1.5rem;
}

/* Mission Cards */
.mission-card-container {
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  animation: card-appear 0.5s ease-out forwards;
}

.mission-card-container:nth-child(1) { animation-delay: 0.1s; }
.mission-card-container:nth-child(2) { animation-delay: 0.15s; }
.mission-card-container:nth-child(3) { animation-delay: 0.2s; }
.mission-card-container:nth-child(4) { animation-delay: 0.25s; }
.mission-card-container:nth-child(5) { animation-delay: 0.3s; }
.mission-card-container:nth-child(6) { animation-delay: 0.35s; }
.mission-card-container:nth-child(n+7) { animation-delay: 0.4s; }

@keyframes card-appear {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.mission-card-container:hover {
  transform: translateY(-10px);
}

.mission-card {
  position: relative;
  background: rgba(30, 41, 59, 0.8);
  backdrop-filter: blur(25px);
  border: 1px solid rgba(14, 165, 233, 0.25);
  border-radius: 20px;
  padding: 1.5rem;
  height: 300px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  box-shadow: 
    0 4px 20px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.mission-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--card-color-1, #0EA5E9), var(--card-color-2, #0284C7));
  opacity: 0.8;
  transition: opacity 0.3s ease;
}

.mission-card:hover::before {
  opacity: 1;
}

.mission-card:hover {
  box-shadow:
    0 30px 60px rgba(0, 0, 0, 0.35),
    0 0 50px rgba(14, 165, 233, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border-color: rgba(14, 165, 233, 0.5);
}

/* Mission Card Variants */
.mission-card.mission-0 {
  --card-color-1: #0EA5E9;
  --card-color-2: #0284C7;
}

.mission-card.mission-0:hover {
  box-shadow:
    0 30px 60px rgba(0, 0, 0, 0.35),
    0 0 50px rgba(14, 165, 233, 0.3);
}

.mission-card.mission-1 {
  --card-color-1: #10B981;
  --card-color-2: #06B6D4;
}

.mission-card.mission-1:hover {
  box-shadow:
    0 30px 60px rgba(0, 0, 0, 0.35),
    0 0 50px rgba(16, 185, 129, 0.3);
}

.mission-card.mission-2 {
  --card-color-1: #F59E0B;
  --card-color-2: #F97316;
}

.mission-card.mission-2:hover {
  box-shadow:
    0 30px 60px rgba(0, 0, 0, 0.35),
    0 0 50px rgba(245, 158, 11, 0.3);
}

.mission-card.mission-3 {
  --card-color-1: #EF4444;
  --card-color-2: #EC4899;
}

.mission-card.mission-3:hover {
  box-shadow:
    0 30px 60px rgba(0, 0, 0, 0.35),
    0 0 50px rgba(239, 68, 68, 0.3);
}

/* Card Header */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
  position: relative;
  z-index: 2;
}

.mission-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.3rem 0.85rem;
  background: rgba(16, 185, 129, 0.15);
  border: 1px solid rgba(16, 185, 129, 0.4);
  border-radius: 20px;
  backdrop-filter: blur(10px);
}

.status-dot {
  position: relative;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10B981;
  box-shadow: 0 0 12px rgba(16, 185, 129, 0.7);
  animation: status-pulse 2s ease-in-out infinite;
}

.status-dot::before {
  content: '';
  position: absolute;
  inset: -3px;
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 50%;
  animation: status-ring 2s ease-in-out infinite;
}

@keyframes status-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(0.85); }
}

@keyframes status-ring {
  0%, 100% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.8); opacity: 0; }
}

.status-text {
  font-size: 0.65rem;
  font-weight: 700;
  color: #10B981;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.mission-id {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 0.8rem;
  color: #94A3B8;
  background: rgba(15, 23, 42, 0.6);
  padding: 0.3rem 0.75rem;
  border-radius: 8px;
  border: 1px solid rgba(14, 165, 233, 0.2);
}

/* Mission Content */
.mission-content {
  position: relative;
  z-index: 2;
  margin-bottom: 1rem;
}

.mission-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #F8FAFC;
  margin-bottom: 0.6rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mission-description {
  font-size: 0.85rem;
  color: #94A3B8;
  line-height: 1.5;
  margin-bottom: 1rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.5rem;
}

.mission-details {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.8rem;
  padding: 0.35rem 0.65rem;
  background: rgba(15, 23, 42, 0.5);
  border-radius: 8px;
  border: 1px solid rgba(14, 165, 233, 0.15);
}

.detail-icon {
  font-size: 0.85rem;
  width: 1rem;
  text-align: center;
}

.detail-text {
  color: #CBD5E1;
  font-weight: 500;
}

/* Mission Footer */
.mission-footer {
  position: absolute;
  bottom: 1.5rem;
  left: 1.5rem;
  right: 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 2;
}

.mission-tech {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.tech-badge {
  padding: 0.25rem 0.6rem;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.2), rgba(2, 132, 199, 0.2));
  border: 1px solid rgba(14, 165, 233, 0.35);
  border-radius: 6px;
  font-size: 0.65rem;
  color: #7DD3FC;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.launch-button {
  position: relative;
  padding: 0.5rem 1.25rem;
  background: linear-gradient(135deg, #0EA5E9, #0284C7);
  border: none;
  border-radius: 10px;
  color: white;
  font-weight: 700;
  font-size: 0.75rem;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 1px;
  box-shadow: 0 4px 15px rgba(14, 165, 233, 0.3);
}

.launch-button:hover {
  transform: scale(1.08);
  box-shadow: 0 8px 25px rgba(14, 165, 233, 0.5);
}

.button-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  transition: left 0.5s ease;
}

.mission-card:hover .button-glow {
  left: 100%;
}

/* Card Background Effects */
.card-background {
  position: absolute;
  inset: 0;
  opacity: 0.08;
  z-index: 1;
  pointer-events: none;
}

.satellite-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(14, 165, 233, 0.15) 1px, transparent 1px),
    linear-gradient(90deg, rgba(14, 165, 233, 0.15) 1px, transparent 1px);
  background-size: 25px 25px;
  animation: grid-drift 25s linear infinite;
}

@keyframes grid-drift {
  0% { transform: translate(0, 0); }
  100% { transform: translate(25px, 25px); }
}

.orbital-lines {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 180px 90px at 80% -10%, rgba(14, 165, 233, 0.15), transparent),
    radial-gradient(ellipse 120px 60px at 100% 100%, rgba(16, 185, 129, 0.1), transparent),
    radial-gradient(ellipse 100px 50px at 0% 50%, rgba(245, 158, 11, 0.08), transparent);
}

/* Empty State */
.empty-missions {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
  padding: 3rem;
  background: rgba(15, 23, 42, 0.4);
  border: 2px dashed rgba(14, 165, 233, 0.3);
  border-radius: 24px;
  backdrop-filter: blur(15px);
  position: relative;
  overflow: hidden;
}

.empty-missions::before {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  animation: empty-glow 5s ease-in-out infinite;
}

@keyframes empty-glow {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.3); opacity: 0.8; }
}

.empty-icon {
  font-size: 5rem;
  margin-bottom: 1.5rem;
  animation: satellite-orbit 6s ease-in-out infinite;
  filter: drop-shadow(0 0 40px rgba(14, 165, 233, 0.5));
  position: relative;
  z-index: 1;
}

@keyframes satellite-orbit {
  0%, 100% { 
    transform: translateY(0) rotate(-10deg); 
    filter: drop-shadow(0 0 40px rgba(14, 165, 233, 0.5));
  }
  25% { 
    transform: translateY(-20px) rotate(0deg) translateX(15px); 
    filter: drop-shadow(0 0 50px rgba(16, 185, 129, 0.6));
  }
  50% { 
    transform: translateY(-25px) rotate(10deg); 
    filter: drop-shadow(0 0 60px rgba(14, 165, 233, 0.7));
  }
  75% { 
    transform: translateY(-20px) rotate(0deg) translateX(-15px); 
    filter: drop-shadow(0 0 50px rgba(245, 158, 11, 0.6));
  }
}

.empty-title {
  font-size: 1.6rem;
  font-weight: 700;
  color: #CBD5E1;
  margin-bottom: 0.5rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 1;
}

.empty-subtitle {
  font-size: 1rem;
  color: #64748B;
  margin-bottom: 2rem;
  position: relative;
  z-index: 1;
}

.create-mission-btn {
  position: relative;
  padding: 1rem 2rem;
  background: linear-gradient(135deg, #0EA5E9, #0284C7);
  border: none;
  border-radius: 14px;
  color: white;
  font-weight: 700;
  font-size: 0.95rem;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 1px;
  box-shadow: 0 8px 25px rgba(14, 165, 233, 0.4);
  z-index: 1;
}

.create-mission-btn:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 15px 40px rgba(14, 165, 233, 0.5);
}

.btn-particles {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.3) 1px, transparent 1px),
    radial-gradient(circle at 60% 50%, rgba(255, 255, 255, 0.2) 1px, transparent 1px),
    radial-gradient(circle at 80% 80%, rgba(255, 255, 255, 0.3) 1px, transparent 1px);
  background-size: 25px 25px;
  animation: particles-float 4s linear infinite;
}

@keyframes particles-float {
  0% { transform: translate(0, 0); }
  100% { transform: translate(-25px, -25px); }
}

/* Responsive Design */
@media (max-width: 900px) {
  .missions-grid {
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 1.25rem;
  }
}

@media (max-width: 768px) {
  .missions-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .mission-card {
    height: auto;
    min-height: 260px;
  }

  .projects-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
    padding: 1rem;
  }

  .section-title {
    font-size: 1.25rem;
  }

  .mission-stats {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 480px) {
  .satellite-projects {
    padding: 1rem;
  }

  .mission-card {
    padding: 1.25rem;
    min-height: 240px;
  }

  .mission-footer {
    bottom: 1.25rem;
    left: 1.25rem;
    right: 1.25rem;
    flex-direction: column;
    gap: 0.75rem;
    align-items: flex-start;
  }

  .launch-button {
    width: 100%;
    text-align: center;
  }

  .mission-title {
    font-size: 1.1rem;
  }

  .card-header {
    flex-wrap: wrap;
    gap: 0.5rem;
  }
}
</style>