<template>
    <teleport to="body">
        <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div class="register-container bg-[#1a1a1a] w-full max-w-md rounded-lg shadow-2xl border border-gray-800 overflow-hidden">
                <!-- 头部 -->
                <div class="flex justify-between items-center p-6 border-b border-gray-800">
                    <h2 class="text-2xl font-semibold text-white">注册</h2>
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
                        {{ $t('signup.descript') }}
                    </p>

                    <a-form layout="vertical" class="register-form">
                        <!-- 邮箱 -->
                        <a-form-item :label="$t('signup.email')">
                            <a-input
                                v-model:value="email"
                                size="large"
                                type="email"
                                :placeholder="t('signup.intext_email')"
                            >
                                <template #prefix>
                                    <MailOutlined class="text-gray-500" />
                                </template>
                            </a-input>
                        </a-form-item>

                        <!-- 密码 -->
                        <a-form-item :label="$t('signup.password')">
                            <a-input-password
                                v-model:value="password"
                                size="large"
                                :placeholder="t('signup.intext_password')"
                            >
                                <template #prefix>
                                    <LockOutlined class="text-gray-500" />
                                </template>
                            </a-input-password>
                        </a-form-item>

                        <!-- 确认密码 -->
                        <a-form-item :label="$t('signup.repassword')">
                            <a-input-password
                                v-model:value="confirmPassword"
                                size="large"
                                :placeholder="t('signup.intext_repassword')"
                            >
                                <template #prefix>
                                    <LockOutlined class="text-gray-500" />
                                </template>
                            </a-input-password>
                        </a-form-item>

                        <!-- 用户名 -->
                        <a-form-item :label="$t('signup.username')">
                            <a-input
                                v-model:value="name"
                                size="large"
                                :placeholder="t('signup.intext_name')"
                            >
                                <template #prefix>
                                    <UserOutlined class="text-gray-500" />
                                </template>
                            </a-input>
                        </a-form-item>

                        <!-- 头衔 -->
                        <a-form-item :label="$t('signup.title')">
                            <a-select
                                v-model:value="title"
                                size="large"
                                :placeholder="t('signup.intext_title')"
                            >
                                <a-select-option value="Professor">Professor</a-select-option>
                                <a-select-option value="Dr">Dr</a-select-option>
                                <a-select-option value="Mr">Mr</a-select-option>
                                <a-select-option value="Mrs">Mrs</a-select-option>
                                <a-select-option value="Miss">Miss</a-select-option>
                                <a-select-option value="Ms">Ms</a-select-option>
                                <a-select-option value="Mx">Mx</a-select-option>
                            </a-select>
                        </a-form-item>

                        <!-- 组织 -->
                        <a-form-item :label="$t('signup.organization')">
                            <a-input
                                v-model:value="organization"
                                size="large"
                                :placeholder="t('signup.intext_organization')"
                            >
                                <template #prefix>
                                    <BankOutlined class="text-gray-500" />
                                </template>
                            </a-input>
                        </a-form-item>

                        <!-- 按钮组 -->
                        <div class="flex gap-3 mt-6">
                            <a-button
                                type="primary"
                                size="large"
                                block
                                @click="handleRegister"
                                class="register-btn"
                            >
                                {{ $t('signup.button.register') }}
                            </a-button>
                            <a-button
                                size="large"
                                block
                                @click="$emit('switch')"
                                class="back-btn"
                            >
                                {{ $t('signup.button.back') }}
                            </a-button>
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
import { createUser } from '@/api/http/user'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { 
    UserOutlined, 
    LockOutlined, 
    CloseOutlined, 
    MailOutlined,
    BankOutlined
} from '@ant-design/icons-vue'

const { t } = useI18n()

const emit = defineEmits(['close', 'switch'])

const router = useRouter()

const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const name = ref('')
const title = ref('')
const organization = ref('')

const handleRegister = async () => {
    if (password.value !== confirmPassword.value) {
        message.error(t('signup.message.error.unmatch'))
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
        router.push('/home')
        message.success(t('signup.message.success'))
    } else {
        message.error(t('signup.message.error.fail'))
    }
}
</script>

<style scoped lang="scss">
.register-container {
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

:deep(.ant-select) {
    .ant-select-selector {
        background-color: #0f0f0f !important;
        border-color: #374151 !important;
        color: #ffffff;
        
        &:hover {
            border-color: #4b5563 !important;
        }
    }
    
    &.ant-select-focused .ant-select-selector {
        border-color: #3b82f6 !important;
        box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1) !important;
    }
    
    .ant-select-selection-placeholder {
        color: #9ca3af;
    }
}

:deep(.anticon) {
    color: #9ca3af;
}

.register-btn {
    height: 44px;
    font-size: 16px;
    font-weight: 500;
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    border: none;
    
    &:hover {
        background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    }
}

.back-btn {
    height: 44px;
    font-size: 16px;
    font-weight: 500;
    background-color: #374151;
    border-color: #4b5563;
    color: #ffffff;
    
    &:hover {
        background-color: #4b5563;
        border-color: #6b7280;
        color: #ffffff;
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
    .register-container {
        margin: 1rem;
    }
    
    .flex.gap-3 {
        flex-direction: column;
    }
}

// 滚动条样式
.register-container::-webkit-scrollbar {
    width: 6px;
}

.register-container::-webkit-scrollbar-track {
    background: #1a1a1a;
}

.register-container::-webkit-scrollbar-thumb {
    background: #374151;
    border-radius: 3px;
    
    &:hover {
        background: #4b5563;
    }
}
</style>