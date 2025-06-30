<template>
    <header class="flex items-center justify-between px-10 py-2 shadow-md select-none">
        <div class="flex flex-3/5 items-center justify-start">
            <div class="flex items-center">
                <!-- <img :src="logo" alt="Logo" class="h-10 w-auto -translate-x-3 cursor-pointer" @click="jumpToOGMS" /> -->
                <satellite class="!text-[#3b82f6] h-8 w-8 mr-2" />

                <span
                    class="bg-gradient-to-r from-sky-100 to-white bg-clip-text font-semibold whitespace-nowrap text-transparent sm:text-lg md:text-xl lg:text-2xl">{{ t('nav.title') }}</span>
            </div>

            <nav class="ml-[5vw] flex items-center space-x-[1.5vw]">
                <div v-for="(item, index) in navItems" class="flex flex-row space-x-[1.5vw]">
                    <!-- 判断是否为外部链接 -->
                    <template v-if="item.external">
                        <a :href="item.path" target="_blank" rel="noopener noreferrer"
                            class="text-shadow-deepblue relative rounded-md px-2 py-1 font-semibold whitespace-nowrap text-white transition-all duration-300 hover:text-sky-100 sm:text-lg md:text-lg lg:text-xl"
                            :class="{
                                'nav-link': true,
                                'nav-link-active': currentRoute === item.path,
                            }">
                            {{ item.name }}
                        </a>
                    </template>
                    <template v-else>
                        <router-link :to="item.path"
                            class="text-shadow-deepblue relative rounded-md px-2 py-1 font-semibold whitespace-nowrap text-white transition-all duration-300 hover:text-sky-100 sm:text-lg md:text-lg lg:text-xl"
                            :class="{
                                'nav-link': true,
                                'nav-link-active': currentRoute === item.path,
                            }">
                            {{ item.name }}
                        </router-link>
                    </template>

                    <!-- 分隔线 -->
                    <div class="h-8 w-0 border-r-2 border-gray-600" v-if="index < navItems.length - 1"></div>
                </div>
            </nav>
        </div>

        <!-- Switch language -->
         <div class="flex items-center mr-4">
            <SwitcheLanguage />
         </div>

        <!-- user authenticated -->
        <div v-if="userStore.authenticated"
            class="group relative flex cursor-pointer items-center justify-between rounded-4xl bg-gray-800 transition-colors hover:bg-gray-700">
            <span class="px-4 text-gray-300">{{ userStore.user.name }}</span>
            <div class="py-1">
                <img :src="avator" alt="user-avator" class="h-8 w-auto rounded-full" />
            </div>
            <div class="absolute top-8 right-0 z-10 hidden w-24 rounded-lg bg-white text-center text-black shadow-lg group-hover:block"
                @click="handleLogout">
                {{t('nav.button.logout')}}
            </div>
        </div>

        <!-- login or register -->
        <div v-else class="flex items-center gap-4">
            <router-link to="/login"
                class="text-md cursor-pointer rounded-md border-2 border-sky-600 px-4 py-1 text-sky-300 transition-colors hover:bg-gray-800 hover:font-bold hover:text-sky-200">
                {{ t('nav.button.login') }}
            </router-link>
            <button @click="router.push('/register')"
                class="text-md cursor-pointer rounded-md border-2 border-sky-600 bg-sky-800 px-4 py-1 text-white transition-colors hover:bg-sky-700 hover:font-bold">
                {{ t('nav.button.signup') }}
            </button>
        </div>
    </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store'
import { useRouter } from 'vue-router'
import SwitcheLanguage from './SwitchLanguage.vue';

// import logo from '@/assets/image/logo2.png'
import avator from '@/assets/image/avator.png'
import { Satellite } from 'lucide-vue-next'

import { useI18n } from 'vue-i18n'
const { t } = useI18n()

//////// Router //////////////////////////////////
const route = useRoute()
const router = useRouter()
const currentRoute = computed(() => route.path)

const navItems = computed(() =>[
    { external: false, name: t('nav.home'), path: '/home' },
    { external: false, name: t('nav.models'), path: '/models' },
    { external: false, name: t('nav.data'), path: '/data' },
    { external: false, name: t('nav.tools'), path: '/projects' },
    { external: true, name: t('nav.about'), path: 'http://opengmsteam.com/' },
    ]
)

// const jumpToOGMS = () => {
//     const OGMS_URL = 'https://geomodeling.njnu.edu.cn/'
//     window.open(OGMS_URL, '_blank')
// }

/////// User //////////////////////////////////
const userStore = useUserStore()

const handleLogout = () => {
    userStore.logout()
    router.push('/login')
}
</script>

<style scoped lang="css">
@reference 'tailwindcss';

.nav-link::after {
    @apply absolute bottom-0 left-0 h-0.5 w-full scale-x-0 transform bg-sky-400 transition-transform duration-300 ease-in-out content-[''];
}

.nav-link:hover::after {
    @apply scale-x-100;
}

.nav-link-active::after {
    @apply scale-x-100;
}

/* Add subtle gradient background */
header {
    background-image: linear-gradient(to right, #1a202c, #000000, #1a202c);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
}

/* Responsive adjustments */
@media (max-width: 768px) {
    header {
        @apply px-4;
    }

    .nav-link {
        @apply text-sm;
    }
}
</style>
