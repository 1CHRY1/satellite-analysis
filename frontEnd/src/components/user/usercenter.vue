<template>  
    <div class="flex flex-1 flex-row min-h-screen">
      <div class="flex flex-col items-center w-full h-screen pt-4 mt-10">
        <main
          class="flex flex-col w-full gap-6 p-4 bg-white md:flex-row justify-center h-full"
        >
          <aside
            class="relative flex flex-col items-center w-full p-4 space-y-4 bg-white md:w-1/6"
            style="padding-left: 5%; padding-top: 5%; flex-basis: 25%"
          >
            <el-upload
              v-model:file-list="fileList_ava"
              :show-file-list="false"
              class="upload-demo relative"
              accept="image/jpg,image/jpeg,image/png"
              :limit="1"
              :before-upload="beforeUploadAvatar"
              :auto-upload="true"
              :http-request="uploadAvatar"
            >
              <div class="relative w-48 h-48">
                <img
                  v-if="data.avatar"
                  :src="data.avatar"
                  width="200"
                  height="200"
                  style="aspect-ratio: 200 / 200; object-fit: cover"
                  alt="Profile"
                  class="w-48 h-48 rounded-full"
                />
                <img
                  v-else
                  :src="userAvatar"
                  width="200"
                  height="200"
                  style="aspect-ratio: 200 / 200; object-fit: cover"
                  alt="Profile"
                  class="w-48 h-48 rounded-full"
                />
                <div
                  class="absolute inset-0 flex items-center justify-center bg-black bg-opacity-50 rounded-full opacity-0 hover:opacity-100 transition-opacity"
                >
                  <span class="text-white text-lg">
                    {{ t("userpage.confirm") }}
                  </span>
                </div>
              </div>
            </el-upload>
            <h1 class="text-2xl font-bold text-black">{{ data.name }}</h1>
            <h2 class="text-lg text-gray-600">
              {{ data.email }}
            </h2>
            <h2 class="text-lg text-gray-600">
            组织： {{ data.organization }}
            </h2>
            <p class="text-center text-gray-600" v-if="data.introduction">
              {{ data.introduction }}
            </p>
            <p class="text-center text-gray-600" v-else>
              {{ t("userpage.introduction") }}
            </p>
            <div class="button-container">
              <button
                class="mr-4 px-4 py-2 text-white bg-blue-500 hover:bg-blue-400 rounded "
                @click="opendialog"
              >
                <font-awesome-icon :icon="['far', 'pen-to-square']" />
                {{ t("userpage.edit") }}
              </button>

              <button 
                class="mr-4 px-4 py-2 text-white bg-blue-500 hover:bg-blue-400 rounded "
                @click="openReset"
                >
                 修改密码 
              </button>

              <button
                class="mr-4 px-4 py-2 text-white bg-blue-500 hover:bg-blue-400 rounded "
                @click="logout"
              >
                <font-awesome-icon :icon="['fas', 'arrow-right-from-bracket']" />
                {{ t("userpage.logout") }}
              </button>
            </div>
          </aside>
          <section class="w-full p-4 space-y-4 bg-white md:w-3/5">
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
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { userUpdate, getUsers , changePassword} from '@/api/http/user';
import { RegionSelects } from 'v-region'
import { avaterUpdate, getAvatar } from '@/api/http/user'

const router = useRouter();
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
    ElMessage.error('上传头像图片只能是 JPG/PNG 格式!');
    return false;
  }
  if (!isLt1M) {
    ElMessage.error('上传头像图片大小不能超过 1MB!');
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
      ElMessage.success("头像上传成功");

      // 更新头像展示路径
      data.avatar = "http://223.2.34.8:30900/" + res.data.avatarPath;

      // 更新 Store（可选）
      userStore.updateUser({
        ...userStore.user,
        avatar: res.data.avatarPath,
      });
    } else {
      ElMessage.error(res.message || "上传失败");
    }
  } catch (err) {
    console.error(err);
    ElMessage.error("上传异常");
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
      ElMessage.success("更新成功");
      let newData = await getUsers(userStore.user.id)
      resetVisible.value = false;
    } else {
      ElMessage.error(passwordRes.message);
      resetVisible.value = false;
    }
  ;
}

// 退出登录
function logout() {
  ElMessage.success("已退出登录");
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
      ElMessage.success("更新成功");
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
        })
      dialogFormVisible.value = false;
    } else {
      ElMessage.error(res.message);
      dialogFormVisible.value = false;
    }
  ;
};

onMounted(async () => {
  await fetchAvatar(userStore.user.id);
});
</script>

<style scoped>
.button-container {
  display: flex;
  flex-direction: column; 
  gap: 8px
    }
:deep(.el-input__wrapper) {
  background-color: white !important;
}
:deep(.el-input__inner) {
  color: black !important;
}
:deep(.region-selects button) {
  background-color: white !important;
  color:black !important
}
</style>