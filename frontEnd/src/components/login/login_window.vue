<template>
    <teleport to="body">
        <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div class="login-container bg-[#1a1a1a] w-full max-w-md rounded-lg shadow-2xl border border-gray-800 overflow-hidden">
                <!-- 头部 -->
                <div class="flex justify-between items-center p-6 border-b border-gray-800">
                    <h2 class="text-2xl font-semibold text-white">欢迎回来</h2>
                    <a-button 
                        type="text" 
                        class="text-gray-400 hover:text-white"
                        @click="$emit('close')"
                    >
                        <template #icon>
                            <CloseOutlined class="text-xl" />
                        </template>
                    </a-button>
                </div>

                <!-- 内容区 -->
                <div class="p-6">
                    <p class="text-sm text-gray-400 mb-6">
                        如果您已经拥有账户，可以使用您的邮箱（或用户名）和密码登录
                    </p>

                    <a-form layout="vertical" class="login-form">
                        <!-- 邮箱/用户名 -->
                        <a-form-item :label="$t('login.username')">
                            <a-input
                                v-model:value="email"
                                size="large"
                                :placeholder="t('login.intext_email')"
                                autocomplete="off"
                            >
                                <template #prefix>
                                    <UserOutlined class="text-gray-500" />
                                </template>
                            </a-input>
                        </a-form-item>

                        <!-- 密码 -->
                        <a-form-item :label="$t('login.password')">
                            <a-input-password
                                v-model:value="password"
                                size="large"
                                :placeholder="t('login.intext_password')"
                                autocomplete="new-password"
                            >
                                <template #prefix>
                                    <LockOutlined class="text-gray-500" />
                                </template>
                            </a-input-password>
                        </a-form-item>

                        <!-- 登录按钮 -->
                        <a-form-item class="mb-4">
                            <a-button
                                type="primary"
                                size="large"
                                block
                                @click="handleLogin"
                                class="login-btn"
                            >
                                {{ $t('login.button.submit') }}
                            </a-button>
                        </a-form-item>

                        <!-- 底部链接 -->
                        <div class="text-center space-y-3">
                            <div class="text-sm">
                                <span class="text-gray-400">还没有账户？</span>
                                <a
                                    @click="$emit('switch')"
                                    class="text-blue-500 hover:text-blue-400 cursor-pointer ml-1"
                                >
                                    {{ $t('login.button.register') }}
                                </a>
                            </div>
                            <div>
                                <a
                                    @click=""
                                    class="text-sm text-blue-500 hover:text-blue-400 cursor-pointer"
                                >
                                    忘记密码
                                </a>
                            </div>
                        </div>
                    </a-form>
                </div>
            </div>
        </div>
    </teleport>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store'
import { login, getUsers } from '@/api/http/user'
import { useI18n } from 'vue-i18n'
import { getRole } from '@/api/http/user/role.api'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, CloseOutlined } from '@ant-design/icons-vue'

const { t } = useI18n()

const emit = defineEmits(['close', 'switch'])

const router = useRouter()
const userStore = useUserStore()

const email = ref('')
const password = ref('')

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
            province: userRes.province,
            city: userRes.city,
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
</script>

<style scoped lang="scss">
.login-container {
    max-height: 90vh;
    overflow-y: auto;
}

// Ant Design Vue 暗黑主题定制
:deep(.ant-form-item-label > label) {
    color: #d1d5db;
    font-size: 14px;
}

:deep(.ant-input) {
    background-color: #0f0f0f;
    border-color: #374151;
    color: #ffffff;
    
    &:hover {
        border-color: #4b5563;
    }
    
    &:focus {
        border-color: #3b82f6;
        box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
    }
}

:deep(.ant-input-password) {
    background-color: #0f0f0f;
    border-color: #374151;
    
    &:hover {
        border-color: #4b5563;
    }
    
    input {
        background-color: transparent;
        color: #ffffff;
    }
}

:deep(.ant-input-affix-wrapper-focused) {
    border-color: #3b82f6;
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

:deep(.anticon) {
    color: #9ca3af;
}

.login-btn {
    height: 44px;
    font-size: 16px;
    font-weight: 500;
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    border: none;
    
    &:hover {
        background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    }
}

// 自动填充样式
input:-webkit-autofill {
    box-shadow: 0 0 0px 1000px #0f0f0f inset !important;
    -webkit-text-fill-color: white !important;
}

// 选中文本样式
::selection {
    background: #374151;
}

::-webkit-selection {
    background: #374151;
}

::-moz-selection {
    background: #374151;
}

// 响应式优化
@media (max-width: 640px) {
    .login-container {
        margin: 1rem;
    }
}

// 滚动条样式
.login-container::-webkit-scrollbar {
    width: 6px;
}

.login-container::-webkit-scrollbar-track {
    background: #1a1a1a;
}

.login-container::-webkit-scrollbar-thumb {
    background: #374151;
    border-radius: 3px;
    
    &:hover {
        background: #4b5563;
    }
}
</style>