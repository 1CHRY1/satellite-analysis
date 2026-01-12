<template>
    <div class="satellite-user-center">
      <div class="space-background"></div>
      <div class="content-wrapper">
        <main class="main-container">
          <aside class="profile-section">
            <div class="avatar-container">
              <el-upload
                v-model:file-list="fileList_ava"
                :show-file-list="false"
                class="avatar-upload"
                accept="image/jpg,image/jpeg,image/png"
                :limit="1"
                :before-upload="beforeUploadAvatar"
                :auto-upload="true"
                :http-request="uploadAvatar"
              >
                <div class="avatar-wrapper">
                  <div class="avatar-glow"></div>
                  <div class="avatar-image-container">
                    <img
                      v-if="data.avatar"
                      :src="data.avatar"
                      alt="Profile"
                      class="avatar-image"
                    />
                    <img
                      v-else
                      :src="userAvatar"
                      alt="Profile"
                      class="avatar-image"
                    />
                    <div class="avatar-overlay">
                      <span class="upload-text">
                        {{ t("userpage.confirm") }}
                      </span>
                    </div>
                  </div>
                </div>
              </el-upload>
            </div>
            <div class="user-info">
              <h1 class="user-name">{{ data.name }}</h1>
              <h2 class="user-email">{{ data.email }}</h2>
              <h2 class="user-organization">
                <span class="label">组织：</span>{{ data.organization }}
              </h2>
              <p class="user-introduction" v-if="data.introduction">
                {{ data.introduction }}
              </p>
              <p class="user-introduction placeholder" v-else>
                {{ t("userpage.introduction") }}
              </p>
            </div>

            <div class="action-buttons" v-if="route.meta.showUserActions">
              <button class="space-button primary" @click="opendialog">
                <div class="button-content">
                  <font-awesome-icon :icon="['far', 'pen-to-square']" class="button-icon" />
                  <span>{{ t("userpage.edit") }}</span>
                </div>
              </button>

              <button class="space-button secondary" @click="openReset">
                <div class="button-content">
                  <span>修改密码</span>
                </div>
              </button>

              <button class="space-button danger" @click="logout">
                <div class="button-content">
                  <font-awesome-icon :icon="['fas', 'arrow-right-from-bracket']" class="button-icon" />
                  <span>{{ t("userpage.logout") }}</span>
                </div>
              </button>
            </div>
          </aside>
          <section class="function-section">
            <userFunction></userFunction>
          </section>
        </main>
      </div>
    </div>
  <el-dialog v-model="dialogFormVisible" title="编辑" width="400">
    <el-form :model="data">
      <el-form-item label="姓名" :label-width="formLabelWidth">
        <el-input v-model="updateForm.userName" autocomplete="off" />
      </el-form-item>
      <el-form-item label="电话" :label-width="formLabelWidth">
        <el-input v-model="updateForm.phone" autocomplete="off" />
      </el-form-item>
      <el-form-item label="地址" :label-width="formLabelWidth">
        <div class="flex flex-col" >
        <p class="flex items-center gap-2">
          <span>{{ data.province }} {{ data.city }}</span>
          <span
            @click="showSelection"
            
            class="text-blue-500 cursor-pointer select-none active:text-red-500"
          >
            修改
          </span>
        </p>
        <div v-if="isShow">
          <RegionSelects
              v-model="regionValue"
              class="region-selects"
              :defaultRegion="{
              province: { key: '', value: data.province },
              city: { key: '', value: data.city }
              }"
              :area="false"
              @change = "regionUpdate"
              style="background-color: white !important; color: black !important;"
            />
        </div>
        </div>
      </el-form-item>
      <el-form-item label="邮箱" :label-width="formLabelWidth">
          <el-input v-model="data.email" autocomplete="off" />
        </el-form-item>
      <el-form-item label="称呼" :label-width="formLabelWidth">
          <el-input v-model="data.title" autocomplete="off" />
        </el-form-item>
      <el-form-item label="组织" :label-width="formLabelWidth">
          <el-input v-model="data.organization" autocomplete="off" />
        </el-form-item>
      <el-form-item label="自我介绍" :label-width="formLabelWidth">
        <el-input
          v-model="updateForm.introduction"
          autocomplete="off"
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogFormVisible = false">{{
          t("userpage.cancel")
        }}</el-button>
        <el-button type="primary" @click="updateUserInfo()">{{
          t("userpage.confirm")
        }}</el-button>
      </span>
    </template>
  </el-dialog>
  <el-dialog v-model="resetVisible" title="修改密码" width="400">
      <el-form>
        <el-form-item label="旧密码" :label-width="formLabelWidth">
          <el-input v-model="oldPassword" autocomplete="off" />
        </el-form-item>
        <el-form-item label="新密码" :label-width="formLabelWidth">
          <el-input v-model="newPassword" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogFormVisible = false">{{
          t("userpage.cancel")
        }}</el-button>
        <el-button type="primary" @click="updatePassword()">{{
          t("userpage.confirm")
        }}</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">    
import { useI18n } from 'vue-i18n';
const {t} = useI18n()

import { useUserStore } from '@/store';
import userAvatar from "@/assets/image/avator.png";
import { ref, reactive , onMounted,watch } from "vue";
import userFunction from "@/components/user/userFunction.vue";
import { useRouter, useRoute } from "vue-router";
import { userUpdate, getUsers , changePassword} from '@/api/http/user';
import { RegionSelects } from 'v-region'
import { avaterUpdate, getAvatar } from '@/api/http/user'
import { message } from 'ant-design-vue';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore()

// 用户数据
const data = reactive({
  avatar: "",
  id: "",
  name: "",
  phone: "",
  province: "",
  city: "",
  email: "",
  title: "",
  organization: "",
  introduction: "",
});

// 初始赋值
Object.assign(data, userStore.user);

// 监听 userStore.user 改变，实时同步到 data
watch(() => userStore.user, (newUser) => {
  Object.assign(data, newUser);
}, { deep: true });

// 更新表单数据
const updateForm = reactive({
  userId:'',
  userName: "",
  phone: '',
  province:'',
  city:'',
  email: "",
  title: "",
  organization:"",
  introduction: "",
});

// 文件列表
const fileList_ava = ref([]);
const dialogFormVisible = ref(false);
const formLabelWidth = "80px";

// 头像上传前校验
function beforeUploadAvatar(file: File) {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png';
  const isLt1M = file.size / 1024 / 1024 < 1;

  if (!isJPG) {
    message.error('上传头像图片只能是 JPG/PNG 格式!');
    return false;
  }
  if (!isLt1M) {
    message.error('上传头像图片大小不能超过 1MB!');
    return false;
  }
  return true;
}

//头像更新
const uploadAvatar = async (option: any) => {
  const formData = new FormData();
  formData.append("userId", userStore.user.id);
  formData.append("userName", userStore.user.name);
  formData.append("file", option.file); // binary file

  try {
    const res = await avaterUpdate(formData);
    if (res.status === 1) {
      message.success("头像上传成功");

      // 更新头像展示路径
      data.avatar = "http://223.2.34.8:30900/" + res.data.avatarPath;

      // 更新 Store（可选）
      userStore.updateUser({
        ...userStore.user,
        avatar: res.data.avatarPath,
      });
    } else {
      message.error(res.message || "上传失败");
    }
  } catch (err) {
    console.error(err);
    message.error("上传异常");
  }
};

//获取头像

async function fetchAvatar(userId: string) {
  try {
    const res = await getAvatar(userId);
    if (res.status === 1 && res.data.avatarPath) {
      data.avatar = `http://223.2.34.8:30900/${res.data.avatarPath}`
    } else {
      data.avatar = '';
    }
  } catch (error) {
    console.error('获取头像失败', error);
    data.avatar = '';
  }
}


// 打开编辑对话框
function opendialog() {
  dialogFormVisible.value = true;
  updateForm.userId = data.id
  updateForm.userName = data.name;
  updateForm.phone = data.phone;
  updateForm.email = data.email;
  updateForm.title = data.title;
  updateForm.organization = data.organization;
  updateForm.introduction = data.introduction;
  regionValue.province = data.province;
  regionValue.city = data.city;
}

// 更新密码
const resetVisible = ref(false)
const oldPassword = ref()
const newPassword = ref()
const openReset = ()=>{
  resetVisible.value = true 
}

const updatePassword =  async() => {
  let passwordData ={
    userId :userStore.user.id,
    userName : userStore.user.name,
    oldPassword: oldPassword.value,
    newPassword: newPassword.value
  }
  let passwordRes = await changePassword(userStore.user.id,passwordData )
    if (passwordRes.status == 1) {
      message.success("更新成功");
      let newData = await getUsers(userStore.user.id)
      resetVisible.value = false;
    } else {
      message.error(passwordRes.message);
      resetVisible.value = false;
    }
  ;
}

// 退出登录
function logout() {
  message.success("已退出登录");
  userStore.logout()
  router.push('/home');
}

const regionValue = reactive({
  province: data.province,
  city: data.city,
})


const regionUpdate = (value) => {
  console.log("RegionSelects返回值：", value);


  regionValue.province = value.province;
  regionValue.city = value.city;

  updateForm.province = value.province.value;
  updateForm.city = value.city.value;

  console.log("省份", updateForm.province);
  // isShow.value=false 

}

const isShow = ref(false)
const showSelection = () =>{
  isShow.value = !isShow.value
}

// 更新用户信息
const updateUserInfo = async() => {
  let userInfo = {
    name: userStore.user.name,
    email: userStore.user.email,
    title: userStore.user.title,
    organization:userStore.user.organization,
    introduction: updateForm.introduction,
  };
  console.log(userInfo);
  let res = await userUpdate(userStore.user.id,updateForm)
    if (res.status == 1) {
      message.success("更新成功");
      let newData = await getUsers(userStore.user.id)
      userStore.updateUser({
            id: userStore.user.id,
            phone: newData.phone,
            province:newData.province,
            city:newData.city,
            email: newData.email,
            name: newData.userName,
            title: newData.title,
            organization: newData.organization,
            introduction:newData.introduction 
        } as any)
      dialogFormVisible.value = false;
    } else {
      message.error(res.message);
      dialogFormVisible.value = false;
    }
  ;
};

onMounted(async () => {
  await fetchAvatar(userStore.user.id);
});
</script>

<style scoped>
/* Satellite Theme CSS Variables */
:root {
  --space-primary: #0B1426;
  --space-secondary: #1E293B;
  --space-accent: #0EA5E9;
  --space-orange: #F59E0B;
  --space-green: #10B981;
  --space-silver: #E2E8F0;
  --space-white: #F8FAFC;
  --space-text: #CBD5E1;
  --space-text-muted: #64748B;
}

/* Main Container */
.satellite-user-center {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 50%, #334155 100%);
}

.space-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background:
    radial-gradient(circle at 20% 20%, rgba(14, 165, 233, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(16, 185, 129, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 40% 60%, rgba(245, 158, 11, 0.05) 0%, transparent 50%);
  z-index: 0;
}

.space-background::before {
  content: '';
  position: absolute;
  width: 100%;
  height: 100%;
  background-image:
    radial-gradient(circle at 25% 25%, white 1px, transparent 1px),
    radial-gradient(circle at 75% 75%, white 0.5px, transparent 0.5px);
  background-size: 100px 100px, 50px 50px;
  opacity: 0.1;
  animation: starfield 20s linear infinite;
}

@keyframes starfield {
  0% { transform: translateY(0) translateX(0); }
  100% { transform: translateY(-100px) translateX(-100px); }
}

.content-wrapper {
  position: relative;
  z-index: 1;
  padding: 2rem;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-container {
  display: flex;
  gap: 2rem;
  width: 100%;
  max-width: 1400px;
  background: rgba(30, 41, 59, 0.3);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(226, 232, 240, 0.1);
  border-radius: 24px;
  padding: 2rem;
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.05);
}

/* Profile Section */
.profile-section {
  flex: 0 0 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rem;
  padding: 3rem 2rem;
  background: rgba(30, 41, 59, 0.2);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(10px);
}

/* Avatar Styling */
.avatar-container {
  position: relative;
  margin-bottom: 0.5rem;
}

.avatar-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-glow {
  position: absolute;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.2) 0%, transparent 70%);
  filter: blur(20px);
  animation: pulse-glow 4s ease-in-out infinite;
  z-index: 0;
}

@keyframes pulse-glow {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(1.1); }
}

.avatar-image-container {
  position: relative;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
  z-index: 1;
  transition: all 0.3s ease;
}

.avatar-wrapper:hover .avatar-image-container {
  border-color: var(--space-accent);
  box-shadow: 0 0 20px rgba(14, 165, 233, 0.3);
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.avatar-wrapper:hover .avatar-image {
  transform: scale(1.1);
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.6);
  opacity: 0;
  transition: opacity 0.3s ease;
  backdrop-filter: blur(2px);
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.upload-text {
  color: var(--space-white);
  font-size: 0.9rem;
  font-weight: 500;
  letter-spacing: 0.5px;
}

/* User Info */
.user-info {
  text-align: center;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.user-name {
  font-size: 1.75rem;
  font-weight: 600;
  color: var(--space-white);
  margin-bottom: 0.25rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-email {
  font-size: 0.95rem;
  color: var(--space-text-muted);
  font-weight: 400;
  margin-bottom: 1.5rem;
}

.user-organization {
  font-size: 0.9rem;
  color: var(--space-text);
  padding: 0.5rem 1rem;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  margin-bottom: 1rem;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.user-organization .label {
  color: var(--space-accent);
  font-size: 0.85rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.user-introduction {
  font-size: 0.9rem;
  color: var(--space-text);
  line-height: 1.6;
  width: 100%;
  text-align: center;
  margin-top: 0.5rem;
  opacity: 0.8;
}

.user-introduction.placeholder {
  font-style: italic;
  opacity: 0.5;
}

/* --- Action Buttons (美化版) --- */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  width: 100%;
  margin-top: auto;
  padding-top: 2rem;
  /* 增加一条极淡的分割线 */
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

/* Base Button Style - 基础样式 */
.space-button {
  position: relative;
  width: 100%;
  padding: 0.9rem 1.5rem;
  border: none;
  border-radius: 12px;
  font-weight: 500;
  font-size: 0.95rem;
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  
  /* 默认状态：低调的深色磨砂玻璃 */
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: var(--space-text);
  
  /* 关键：顶部微弱高光，制造厚度感，不刺眼 */
  box-shadow: 
    0 4px 6px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.button-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.8rem;
  position: relative;
  z-index: 2;
}

.button-icon {
  font-size: 1rem;
  opacity: 0.8;
  transition: transform 0.3s ease;
}

/* Hover Effects - 通用悬停效果 */
.space-button:hover {
  transform: translateY(-2px);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.12) 0%, rgba(255, 255, 255, 0.04) 100%);
  border-color: rgba(255, 255, 255, 0.15);
  box-shadow: 
    0 8px 15px rgba(0, 0, 0, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
}

.space-button:hover .button-icon {
  transform: scale(1.1) rotate(5deg);
  opacity: 1;
}

.space-button:active {
  transform: translateY(0);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* Specific Button Styles */

/* 1. Primary (Edit) - 深空蓝，半透明 */
.space-button.primary {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.25) 0%, rgba(14, 165, 233, 0.1) 100%);
  border: 1px solid rgba(14, 165, 233, 0.3);
  color: #e0f2fe;
}

.space-button.primary:hover {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.35) 0%, rgba(14, 165, 233, 0.15) 100%);
  border-color: rgba(14, 165, 233, 0.5);
  box-shadow: 
    0 8px 20px rgba(14, 165, 233, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
}

/* 2. Secondary (Password) - 低调金属感 */
.space-button.secondary {
  border-color: rgba(255, 255, 255, 0.1);
}
.space-button.secondary:hover {
  border-color: rgba(255, 255, 255, 0.25);
}

/* 3. Danger (Logout) - 隐忍的红色，仅在Hover时明显 */
.space-button.danger {
  background: linear-gradient(145deg, rgba(239, 68, 68, 0.05) 0%, transparent 100%);
  border: 1px solid rgba(239, 68, 68, 0.15);
  color: #fca5a5;
}

.space-button.danger:hover {
  background: linear-gradient(145deg, rgba(239, 68, 68, 0.15) 0%, rgba(239, 68, 68, 0.05) 100%);
  border-color: rgba(239, 68, 68, 0.4);
  color: #fecaca;
  box-shadow: 
    0 8px 15px rgba(239, 68, 68, 0.1), 
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

/* Function Section */
.function-section {
  flex: 1;
  background: rgba(15, 23, 42, 0.2);
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.1);
  overflow: hidden;
  backdrop-filter: blur(10px);
}

/* Dialog Enhancements */
:deep(.el-dialog) {
  background: linear-gradient(135deg, var(--space-secondary), var(--space-primary));
  border: 1px solid rgba(14, 165, 233, 0.3);
  border-radius: 16px;
  backdrop-filter: blur(20px);
}

:deep(.el-dialog__header) {
  background: rgba(14, 165, 233, 0.1);
  color: var(--space-white);
}

:deep(.el-input__wrapper) {
  background: rgba(15, 23, 42, 0.5) !important;
  border: 1px solid rgba(14, 165, 233, 0.3);
  border-radius: 8px;
}

:deep(.el-input__inner) {
  color: var(--space-white) !important;
}

:deep(.el-form-item__label) {
  color: var(--space-text) !important;
}

:deep(.region-selects button) {
  background: rgba(15, 23, 42, 0.5) !important;
  color: var(--space-text) !important;
  border: 1px solid rgba(14, 165, 233, 0.3) !important;
}

/* Responsive Design */
@media (max-width: 768px) {
  .main-container {
    flex-direction: column;
    padding: 1rem;
  }

  .profile-section {
    flex: none;
    width: 100%;
  }

  .avatar-glow {
    width: 140px;
    height: 140px;
  }

  .avatar-image-container {
    width: 120px;
    height: 120px;
  }
}
</style>