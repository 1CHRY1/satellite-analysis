<template>
    <header
        class="flex items-center justify-between rounded-2xl border-b-2 border-sky-700 bg-gray-50 px-10 py-4 shadow-sm select-none"
    >
        <div class="flex flex-3/5 items-center justify-start">
            <div class="flex items-center">
                <img
                    :src="logo"
                    alt="Logo"
                    class="h-10 w-auto -translate-x-3 cursor-pointer"
                    @click="jumpToOGMS"
                />
                <span
                    class="ml-3 bg-gradient-to-r from-sky-950 to-sky-500 bg-clip-text font-semibold whitespace-nowrap text-transparent sm:text-lg md:text-xl lg:text-3xl"
                    >多源遥感应用支撑云平台</span
                >
            </div>

            <nav class="ml-[5vw] flex items-center space-x-8">
                <div v-for="(item, index) in navItems" class="flex flex-row space-x-8">
                    <router-link
                        :key="item.path"
                        :to="item.path"
                        class="text-shadow-deepbluewhitespace-nowrap relative rounded-md px-2 py-1 font-semibold text-sky-800 transition-all duration-300 hover:text-sky-600 sm:text-lg md:text-xl lg:text-2xl"
                        :class="{
                            'bg-sky-50 font-bold text-sky-600': currentRoute === item.path,
                            'nav-link': true,
                            'nav-link-active': currentRoute === item.path,
                        }"
                        >{{ item.name }}
                    </router-link>
                    <div
                        class="h-8 w-0 border-r-2 border-gray-500"
                        v-if="index < navItems.length - 1"
                    ></div>
                </div>
            </nav>
        </div>

        <!-- user authenticated -->
        <div
            v-if="userStore.authenticated"
            class="flex cursor-pointer items-center justify-between rounded-4xl bg-gray-200"
        >
            <span class="px-4 text-gray-500">{{ userStore.user.name }}</span>
            <div class="py-1">
                <img :src="avator" alt="user-avator" class="h-8 w-auto" />
            </div>
        </div>

        <!-- login or register -->
        <div v-else class="flex items-center gap-4">
            <router-link
                to="/login"
                class="text-md cursor-pointer rounded-md border-2 px-4 py-1 text-black transition-colors hover:font-bold hover:text-sky-400"
            >
                登录
            </router-link>
            <button
                class="text-md cursor-pointer rounded-md border-2 bg-sky-950 px-4 py-1 text-white transition-colors hover:bg-sky-300 hover:font-bold"
            >
                注册
            </button>
        </div>
    </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/index.ts'
import logo from '@/assets/image/logo.png'
import avator from '@/assets/image/avator.png'

//////// Router //////////////////////////////////
const route = useRoute()
const currentRoute = computed(() => route.path)

const navItems = [
    { name: '首页', path: '/home' },
    { name: '影像检索', path: '/data' },
    { name: '分析中心', path: '/analysis' },
    // { name: '分析中心', path: '/projects' },
]

const jumpToOGMS = () => {
    const OGMS_URL = 'https://geomodeling.njnu.edu.cn/'
    window.open(OGMS_URL, '_blank')
}

/////// User //////////////////////////////////
const userStore = useUserStore()
</script>

<style scoped lang="css">
@reference 'tailwindcss';

.nav-link::after {
    @apply absolute bottom-0 left-0 h-0.5 w-full scale-x-0 transform bg-sky-500 transition-transform duration-300 ease-in-out content-[''];
}

.nav-link:hover::after {
    @apply scale-x-100;
}

.nav-link-active::after {
    @apply scale-x-100;
}
</style>
