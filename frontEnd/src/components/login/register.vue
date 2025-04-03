<template>
    <div class="main">
        <div class="container">
            <div class="left">
                <div class="login">注 册</div>
                <div class="eula">请填写以下信息完成注册！</div>
            </div>
            <div class="right">
                <!-- <svg viewBox="0 0 320 300">
                    <defs>
                        <linearGradient
                            id="linearGradient"
                            x1="13"
                            y1="193.5"
                            x2="307"
                            y2="193.5"
                            gradientUnits="userSpaceOnUse"
                        >
                            <stop style="stop-color: #ff00ff" offset="0" />
                            <stop style="stop-color: #ff0000" offset="1" />
                        </linearGradient>
                    </defs>
                    <path
                        d="m 40,120 h 240 c 0,0 25,1 25,35s-25,35-25,35h-240c 0,0-25,4-25,38s25,38 25,38h215c 0,0 20-1 20-25s-20-25-20-25h-190c 0,0-20,2-20,25s20,25 20,25h168.57"
                    />
                </svg> -->
                <div class="form">
                    <label for="email">邮 箱</label>
                    <input type="email" placeholder="请输入您的邮箱" id="email" v-model="email" />
                    <label for="password">密 码</label>
                    <input type="password" placeholder="请输入密码" id="password" v-model="password" />
                    <label for="confirm-password">确认密码</label>
                    <input type="password" placeholder="请再次输入密码" id="confirm-password" v-model="confirmPassword" />
                    <label for="name">用户名</label>
                    <input type="text" placeholder="请输入您的用户名" id="name" v-model="name" />
                    <label for="title">称 谓</label>
                    <!-- <input type="text" placeholder="请输入您的头衔" id="title" v-model="title" /> -->
                    <el-select v-model="title" class="!p-0" placeholder="请选择您的称谓">
                        <el-option label="Professor" value="Professor"></el-option>
                        <el-option label="Dr" value="Dr"></el-option>
                        <el-option label="Mr" value="Mr"></el-option>
                        <el-option label="Mrs" value="Mrs"></el-option>
                        <el-option label="Miss" value="Miss"></el-option>
                        <el-option label="Ms" value="Ms"></el-option>
                        <el-option label="Mx" value="Mx"></el-option>
                    </el-select>
                    <label for="organization">组 织</label>
                    <input type="text" placeholder="请输入您的组织" id="organization" v-model="organization" />
                    <div class="flex h-[80px] w-[245px] flex-row items-center justify-center">
                        <button type="button" class="submit cursor-pointer" @click="handleRegister">
                            注 册
                        </button>
                        <button type="button" class="submit cursor-pointer" @click="router.push('/login')">
                            返 回
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createUser } from '@/api/http/user'

const router = useRouter()

const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const name = ref('')
const title = ref('')
const organization = ref('')

const handleRegister = async () => {
    if (password.value !== confirmPassword.value) {
        ElMessage.error('密码和确认密码不匹配！')
        return
    }
    let result = await createUser({
        userName: name.value,
        email: email.value,
        password: password.value,
        title: title.value,
        organization: organization.value,
    })
    if (result?.status === 1) {
        router.push('/login')
        ElMessage.success('注册成功！')
    } else {
        ElMessage.error('注册失败，请重试')
    }
}
</script>

<style scoped lang="scss">
:deep(.el-select__wrapper) {
    background-color: #474a59;
    border: none;
    padding-left: 0;
    box-shadow: none;
    color: white;
}

:deep(.el-select__wrapper.is-hovering:not(.is-focused)) {
    box-shadow: none;
}

:deep(.el-select__placeholder.is-transparent) {
    color: #a8abb2;
}

:deep(.el-select__placeholder) {
    color: white;
}

input:-webkit-autofill {
    box-shadow: 0 0 0px 1000px #474a59 inset !important;
    /* 强制覆盖背景 */
    -webkit-text-fill-color: white !important;
}

::selection {
    background: #2d2f36;
}

::-webkit-selection {
    background: #2d2f36;
}

::-moz-selection {
    background: #2d2f36;
}

body {
    background: white;
    font-family: 'Inter UI', sans-serif;
    margin: 0;
    padding: 20px;
}

.main {
    height: calc(100% - 56px);
    width: 100%;
    display: flex;
    flex-direction: column;
    top: 56px;
    /* // height: calc(100% - 40px); */
    position: absolute;
    place-content: center;
    /* // width: calc(100% - 40px); */
}

@media (max-width: 767px) {
    .main {
        height: auto;
        margin-bottom: 20px;
        padding-bottom: 20px;
    }
}

.container {
    display: flex;
    height: 520px;
    margin: 0 auto;
    width: 640px;
}

@media (max-width: 767px) {
    .container {
        flex-direction: column;
        height: 630px;
        width: 320px;
    }
}

.left {
    background: white;
    height: calc(100% - 240px);
    top: 120px;
    position: relative;
    width: 50%;
}

@media (max-width: 767px) {
    .left {
        height: 100%;
        left: 20px;
        width: calc(100% - 40px);
        max-height: 270px;
    }
}

.login {
    color: black;
    font-size: 38px;
    font-weight: 600;
    margin: 50px 40px 40px;
}

.eula {
    color: #999;
    font-size: 14px;
    line-height: 1.5;
    margin: 40px;
}

.right {
    background: #474a59;
    box-shadow: 0px 0px 40px 16px rgba(0, 0, 0, 0.22);
    color: #f1f1f2;
    position: relative;
    width: 50%;
    height: 520px;
}

@media (max-width: 767px) {
    .right {
        flex-shrink: 0;
        height: 100%;
        width: 100%;
        max-height: 350px;
    }
}

svg {
    position: absolute;
    width: 320px;
}

path {
    fill: none;
    stroke: url(#linearGradient);
    stroke-width: 4;
    stroke-dasharray: 240 1386;
}

.form {
    margin: 40px;
    position: absolute;
}

label {
    color: #c2c2c5;
    display: block;
    font-size: 14px;
    height: 16px;
    margin-top: 10px;
    margin-bottom: 5px;
}

input {
    background: transparent;
    border: 0;
    color: #f2f2f2;
    font-size: 13px;
    height: 30px;
    line-height: 30px;
    outline: none !important;
    width: 100%;
}

button {
    background: transparent;
    border: 0;
    color: #f2f2f2;
    font-size: 24px;
    height: 30px;
    line-height: 30px;
    outline: none !important;
    width: 100%;
}

input::-moz-focus-inner {
    border: 0;
}

.submit {
    color: #707075;
    margin-top: 40px;
    transition: color 300ms;
}

.submit:hover {
    color: #fff;
}

.submit:focus {
    color: #f2f2f2;
}

.submit:active {
    color: #d0d0d2;
}
</style>
