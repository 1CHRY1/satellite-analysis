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
  padding: 2rem;
  background: transparent;
  min-height: 100%;
}

/* Header */
.projects-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(99, 102, 241, 0.2);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 2rem;
  font-weight: 700;
  color: #F8FAFC;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.title-decoration {
  width: 4px;
  height: 2rem;
  background: linear-gradient(45deg, #6366F1, #10B981);
  border-radius: 2px;
  box-shadow: 0 0 10px rgba(99, 102, 241, 0.5);
}

.mission-badge {
  font-size: 1.5rem;
  animation: rocket-float 3s ease-in-out infinite;
}

@keyframes rocket-float {
  0%, 100% { transform: translateY(0) rotate(-10deg); }
  50% { transform: translateY(-8px) rotate(5deg); }
}

.mission-stats {
  display: flex;
  gap: 2rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem;
  background: rgba(15, 23, 42, 0.5);
  border-radius: 12px;
  border: 1px solid rgba(99, 102, 241, 0.3);
  backdrop-filter: blur(10px);
}

.stat-number {
  font-size: 2rem;
  font-weight: 700;
  color: #6366F1;
  text-shadow: 0 0 10px rgba(99, 102, 241, 0.5);
}

.stat-label {
  font-size: 0.8rem;
  color: #CBD5E1;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Missions Grid */
.missions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 2rem;
  margin-top: 2rem;
}

/* Mission Cards */
.mission-card-container {
  cursor: pointer;
  transition: transform 0.3s ease;
}

.mission-card-container:hover {
  transform: translateY(-8px);
}

.mission-card {
  position: relative;
  background: rgba(30, 41, 59, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(99, 102, 241, 0.3);
  border-radius: 20px;
  padding: 2rem;
  height: 320px;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
}

.mission-card:hover {
  box-shadow:
    0 25px 50px rgba(0, 0, 0, 0.3),
    0 0 50px rgba(99, 102, 241, 0.2);
  border-color: rgba(99, 102, 241, 0.6);
}

/* Mission Card Variants */
.mission-card.mission-0 {
  border-image: linear-gradient(45deg, #6366F1, #8B5CF6) 1;
}

.mission-card.mission-1 {
  border-image: linear-gradient(45deg, #10B981, #06B6D4) 1;
}

.mission-card.mission-2 {
  border-image: linear-gradient(45deg, #F59E0B, #F97316) 1;
}

.mission-card.mission-3 {
  border-image: linear-gradient(45deg, #EF4444, #EC4899) 1;
}

/* Card Header */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  position: relative;
  z-index: 2;
}

.mission-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0.75rem;
  background: rgba(16, 185, 129, 0.2);
  border: 1px solid rgba(16, 185, 129, 0.4);
  border-radius: 20px;
  backdrop-filter: blur(10px);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10B981;
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.6);
  animation: pulse-status 2s infinite;
}

@keyframes pulse-status {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.2); }
}

.status-text {
  font-size: 0.7rem;
  font-weight: 600;
  color: #10B981;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.mission-id {
  font-family: 'Courier New', monospace;
  font-size: 0.9rem;
  color: #94A3B8;
  background: rgba(15, 23, 42, 0.7);
  padding: 0.25rem 0.75rem;
  border-radius: 8px;
  border: 1px solid rgba(99, 102, 241, 0.2);
}

/* Mission Content */
.mission-content {
  position: relative;
  z-index: 2;
  margin-bottom: 1.5rem;
}

.mission-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #F8FAFC;
  margin-bottom: 0.75rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  line-height: 1.2;
}

.mission-description {
  font-size: 0.95rem;
  color: #94A3B8;
  line-height: 1.5;
  margin-bottom: 1.5rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mission-details {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
}

.detail-icon {
  font-size: 1rem;
  width: 1.2rem;
  text-align: center;
}

.detail-text {
  color: #CBD5E1;
  font-weight: 500;
}

/* Mission Footer */
.mission-footer {
  position: absolute;
  bottom: 2rem;
  left: 2rem;
  right: 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 2;
}

.mission-tech {
  display: flex;
  gap: 0.5rem;
}

.tech-badge {
  padding: 0.25rem 0.5rem;
  background: rgba(99, 102, 241, 0.2);
  border: 1px solid rgba(99, 102, 241, 0.4);
  border-radius: 6px;
  font-size: 0.7rem;
  color: #6366F1;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.launch-button {
  position: relative;
  padding: 0.5rem 1rem;
  background: linear-gradient(45deg, #6366F1, #8B5CF6);
  border: none;
  border-radius: 10px;
  color: white;
  font-weight: 600;
  font-size: 0.8rem;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.launch-button:hover {
  transform: scale(1.05);
  box-shadow: 0 10px 25px rgba(99, 102, 241, 0.4);
}

.button-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  transition: left 0.6s ease;
}

.launch-button:hover .button-glow {
  left: 100%;
}

/* Card Background Effects */
.card-background {
  position: absolute;
  inset: 0;
  opacity: 0.1;
  z-index: 1;
}

.satellite-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.1) 1px, transparent 1px);
  background-size: 20px 20px;
  animation: grid-move 20s linear infinite;
}

@keyframes grid-move {
  0% { transform: translate(0, 0); }
  100% { transform: translate(20px, 20px); }
}

.orbital-lines {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 200px 100px at 50% 0%, rgba(99, 102, 241, 0.1), transparent),
    radial-gradient(ellipse 150px 75px at 100% 100%, rgba(16, 185, 129, 0.1), transparent);
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
  background: rgba(15, 23, 42, 0.3);
  border: 2px dashed rgba(99, 102, 241, 0.3);
  border-radius: 20px;
  backdrop-filter: blur(10px);
}

.empty-icon {
  font-size: 5rem;
  margin-bottom: 1.5rem;
  animation: float 4s ease-in-out infinite;
  filter: drop-shadow(0 0 30px rgba(99, 102, 241, 0.4));
}

.empty-title {
  font-size: 1.8rem;
  font-weight: 700;
  color: #CBD5E1;
  margin-bottom: 0.5rem;
}

.empty-subtitle {
  font-size: 1.1rem;
  color: #64748B;
  margin-bottom: 2rem;
}

.create-mission-btn {
  position: relative;
  padding: 1rem 2rem;
  background: linear-gradient(45deg, #6366F1, #8B5CF6);
  border: none;
  border-radius: 12px;
  color: white;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.create-mission-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 35px rgba(99, 102, 241, 0.4);
}

.btn-particles {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 30% 40%, rgba(255, 255, 255, 0.2) 1px, transparent 1px),
    radial-gradient(circle at 70% 70%, rgba(255, 255, 255, 0.2) 1px, transparent 1px);
  background-size: 20px 20px;
  animation: particles 3s linear infinite;
}

@keyframes particles {
  0% { transform: translate(0, 0); }
  100% { transform: translate(-20px, -20px); }
}

/* Responsive Design */
@media (max-width: 768px) {
  .missions-grid {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }

  .mission-card {
    height: auto;
    min-height: 280px;
  }

  .projects-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }

  .section-title {
    font-size: 1.5rem;
  }
}

@media (max-width: 480px) {
  .satellite-projects {
    padding: 1rem;
  }

  .mission-card {
    padding: 1.5rem;
  }

  .mission-footer {
    bottom: 1.5rem;
    left: 1.5rem;
    right: 1.5rem;
  }
}
</style>