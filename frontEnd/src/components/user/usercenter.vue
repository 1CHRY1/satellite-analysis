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
              action=""
              accept="image/jpg,image/jpeg,image/png"
              :limit="1"
              :before-upload="beforeUploadAvatar"
              :auto-upload="true"
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
            <div>
              <button
                class="mr-4 px-4 py-2 text-white bg-blue-500 hover:bg-blue-400 rounded "
                @click="opendialog"
              >
                <font-awesome-icon :icon="['far', 'pen-to-square']" />
                {{ t("userpage.edit") }}
              </button>

              <button
                class="px-4 py-2 text-white bg-red-500 hover:bg-red-400 rounded"
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
        <el-input v-model="updateForm.name" autocomplete="off" />
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
</template>

<script setup lang="ts">    
import { useI18n } from 'vue-i18n';
const {t} = useI18n()

import { useUserStore } from '@/store';
import userAvatar from "@/assets/image/avator.png";
import { ref, reactive } from "vue";
import userFunction from "@/components/user/userFunction.vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getUserUpdate } from '@/api/http/user';

const router = useRouter();
const useUser = useUserStore()
// 用户数据
const data = reactive({
  avatar: "",
  name: useUser.user.name,
  email: useUser.user.email,
  title: useUser.user.title,
  organization:useUser.user.organization,
  introduction: "",
});

// 更新表单数据
const updateForm = reactive({
  name: "",
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
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isJPG) {
    ElMessage.error('上传头像图片只能是 JPG/PNG 格式!');
    return false;
  }
  if (!isLt2M) {
    ElMessage.error('上传头像图片大小不能超过 2MB!');
    return false;
  }
  return true;
}

// 打开编辑对话框
function opendialog() {
  dialogFormVisible.value = true;
  updateForm.name = data.name;
  updateForm.email = data.email;
  updateForm.title = data.title;
  updateForm.organization = data.organization;
  updateForm.introduction = data.introduction;
}

// 退出登录
function logout() {
  ElMessage.success("已退出登录");
  router.push('/login');
}

// 更新用户信息
const updateUserInfo = async() => {
  let updata = {
    name: useUser.user.name,
    email: useUser.user.email,
    title: useUser.user.title,
    organization:useUser.user.organization,
    introduction: updateForm.introduction,
  };
  console.log(updata);
  let res = await getUserUpdate(updata)
    if (res.code == 0) {
      ElMessage.success("更新成功");
      data.name = res.data.name;
      data.introduction = res.data.introduction;
      dialogFormVisible.value = false;
    } else {
      ElMessage.error(res.message);
      dialogFormVisible.value = false;
    }
  ;
};
</script>

<style>

</style>