<template>
    <teleport to="body">
        <div class=" fixed inset-0 bg-black/40 flex items-center justify-center z-50 pointer-events-none">
            <div class="bg-black w-[20%] h-[50%] p-6 rounded-lg overflow-auto pointer-events-auto ">
                <svg viewBox="0 0 320 300">
                    <path
                        d="m 40,120.00016 239.99984,-3.2e-4 c 0,0 24.99263,0.79932 25.00016,35.00016 0.008,34.20084 -25.00016,35 -25.00016,35 h -239.99984 c 0,-0.0205 -25,4.01348 -25,38.5 0,34.48652 25,38.5 25,38.5 h 215 c 0,0 20,-0.99604 20,-25 0,-24.00396 -20,-25 -20,-25 h -190 c 0,0 -20,1.71033 -20,25 0,24.00396 20,25 20,25 h 168.57143"
                    />
                </svg>
                <div class="m-10 relative">
                    <div class="flex justify-between items-center mb-6">
                        <div class="text-xl">
                            登录
                        </div >
                        <span class="text-2xl text-white cursor-pointer" @click="$emit('close')">&times;</span>
                    </div>
                    <div class="text-sm">如果您已经拥有账户，可以使用您的邮箱（或用户名）和密码登录</div>
                    <hr class="my-4 border-t border-gray-300" />

                    <div class="form">
                        <label for="email" >{{$t('login.username')}}</label>
                        <input
                            type="email"
                            :placeholder="t('login.intext_email')"
                            id="email"
                            autocomplete="off"
                            v-model="email"
                            class="w-full mb-4 p-3 rounded bg-[#181a20] text-white border border-gray-600 focus:border-blue-500 outline-none"
                        />
                        <label for="password">{{ $t('login.password') }}</label>
                        <input
                            type="password"
                            :placeholder="t('login.intext_password')"
                            id="password"
                            autocomplete="new-password"
                            v-model="password"
                            class="w-full mb-4 p-3 rounded bg-[#181a20] text-white border border-gray-600 focus:border-blue-500 outline-none"
                        />
                        <div class="flex-row items-center justify-center bg-blue-500 text-white px-4 py-0.5 rounded mt-4" >
                            <button
                                type="submit"
                                id="submit"
                                @click="handleLogin"
                                class="w-full h-[50%] bg-blue-500 hover:bg-blue-600 text-white py-3 rounded-lg text-lg font-semibold transition"
                            >
                                {{$t('login.button.submit')}}
                        </button>
                        </div>
                        <div class="mt-6 justify-center">
                            <span>还没有账户？</span>
                            <span
                                type="register"
                                id="register"
                                @click="$emit('switch')"
                                class="submit cursor-pointer text-blue"
                            >
                                {{$t('login.button.register')}}
                        </span>
                        </div>
                        <div>
                            <span
                                type="reset"
                                id="reset"
                                @click=""
                                class="submit justify-center item-center cursor-pointer text-blue"
                            >
                             忘记密码
                        </span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </teleport>
</template>

<script setup >
import anime from 'animejs/lib/anime.es.js'
import { onMounted, ref ,reactive} from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store'
import { login, getUsers } from '@/api/http/user'

import { useI18n } from 'vue-i18n'
import { getRole } from '@/api/http/user/role.api'
import { message } from 'ant-design-vue'
const { t } = useI18n()

const emit= defineEmits(['close', 'switch']);

const router = useRouter()
const userStore = useUserStore()

const gotoRegister = () => {
    router.push('/register')
}

const handleLogin = async () => {
    let loginRes = await login({
        email: email.value,
        password: password.value,
    })
    if (loginRes.status === 1) {
        localStorage.setItem('token', loginRes.data.accessToken)
        localStorage.setItem('refreshToken', loginRes.data.refreshToken)
        localStorage.setItem('userId', loginRes.data.userId)
        console.log('用户信息', loginRes.data.userId)

        let userRes = await getUsers(loginRes.data.userId)
        let roleRes = await getRole(userRes.roleId)
        console.log('用户信息', userRes)
        console.log('角色信息', roleRes)

        userStore.login({
            id: loginRes.data.userId,
            phone: userRes.phone,
            province:userRes.province,
            city:userRes.city,
            email: userRes.email,
            name: userRes.userName,
            title: userRes.title,
            organization: userRes.organization,
            introduction: userRes.introduction,
            roleId: userRes.roleId,
            roleName: roleRes.data.name,
            roleDesc: roleRes.data.description,
            maxCpu: roleRes.data.maxCpu,
            maxStorage: roleRes.data.maxStorage,
            maxJob: roleRes.data.maxJob,
            isSuperAdmin: roleRes.data.isSuperAdmin
        })
        message.success(t('login.message.success'))
        router.push('/home')
        emit('close')
    } else if (loginRes.status === -1) {
        message.error(t('login.message.error_wrongdetail'))
    } else {
        message.error(t('login.message.error_fail'))
    }
}

const email = ref('')
const password = ref('')

onMounted(() => {
    let current = null
    document.querySelector('#email').addEventListener('focus', () => {
        if (current) current.pause()
        current = anime({
            targets: 'path',
            strokeDashoffset: {
                value: 0,
                duration: 700,
                easing: 'easeOutQuart',
            },
            strokeDasharray: {
                value: '240 1386',
                duration: 700,
                easing: 'easeOutQuart',
            },
        })
    })

    document.querySelector('#password').addEventListener('focus', () => {
        if (current) current.pause()
        current = anime({
            targets: 'path',
            strokeDashoffset: {
                value: -336,
                duration: 700,
                easing: 'easeOutQuart',
            },
            strokeDasharray: {
                value: '240 1386',
                duration: 700,
                easing: 'easeOutQuart',
            },
        })
    })

    document.querySelector('#submit').addEventListener('mouseover', () => {
        if (current) current.pause()
        current = anime({
            targets: 'path',
            strokeDashoffset: {
                value: -730,
                duration: 700,
                easing: 'easeOutQuart',
            },
            strokeDasharray: {
                value: '530 1386',
                duration: 700,
                easing: 'easeOutQuart',
            },
        })
    })
    document.querySelector('#register').addEventListener('mouseover', () => {
        if (current) current.pause()
        current = anime({
            targets: 'path',
            strokeDashoffset: {
                value: -730,
                duration: 700,
                easing: 'easeOutQuart',
            },
            strokeDasharray: {
                value: '530 1386',
                duration: 700,
                easing: 'easeOutQuart',
            },
        })
    })
})
</script>

<style scoped lang="scss">
// 修改自动填充的样式
input:-webkit-autofill {
    // background-color: #474a59 !important; /* 让背景色保持透明 */
    box-shadow: 0 0 0px 1000px #474a59 inset !important;
    /* 强制覆盖背景 */
    -webkit-text-fill-color: white !important;
    /* 让文本颜色保持不变 */
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
    top: 56px;
    width: 100%;
    display: flex;
    flex-direction: column;
    // height: calc(100% - 40px);
    position: absolute;
    place-content: center;
    // width: calc(100% - 40px);
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
    height: 320px;
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
    height: calc(100% - 40px);
    top: 20px;
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
 //   margin: 40px;
    width: 100%;
    position: absolute;
}

label {
    color: #c2c2c5;
    display: block;
    font-size: 14px;
    height: 16px;
    margin-top: 20px;
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
    //height: 30px;
    //line-height: 30px;
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
