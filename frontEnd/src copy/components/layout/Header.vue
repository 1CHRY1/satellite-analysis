<template>
    <header
        class="flex items-center justify-between px-10 py-4 bg-gray-50 shadow-sm select-none border-b-2 border-sky-700 rounded-2xl"
    >
        <div class="flex-3/5 flex items-center justify-start">
            <div class="flex items-center">
                <img
                    src="/images/logo2.png"
                    alt="Logo"
                    class="h-10 w-auto -translate-x-3 cursor-pointer"
                    @click="jumpToOGMS"
                />
                <span
                    class="whitespace-nowrap ml-3 lg:text-3xl md:text-xl sm:text-lg font-semibold text-transparent bg-clip-text bg-gradient-to-r from-sky-950 to-sky-500"
                    >遥感ARD平台</span
                >
            </div>

            <nav class="flex items-center space-x-8 ml-[5vw]">
                <div v-for="(item, index) in navItems" class="flex flex-row space-x-8">
                    <router-link
                        :key="item.path"
                        :to="item.path"
                        class="relative text-shadow-deepbluewhitespace-nowrap lg:text-2xl md:text-xl sm:text-lg text-sky-800 font-semibold hover:text-sky-600 transition-all duration-300 px-2 py-1 rounded-md"
                        :class="{
                            'text-sky-600 font-bold bg-sky-50': currentRoute === item.path,
                            'nav-link': true,
                            'nav-link-active': currentRoute === item.path,
                        }"
                        >{{ item.name }}
                    </router-link>
                    <div
                        class="border-r-2 border-gray-500 h-8 w-0"
                        v-if="index < navItems.length - 1"
                    ></div>
                </div>
            </nav>
        </div>

        <!-- user authenticated -->
        <div
            v-if="userStore.authenticated"
            class="flex items-center justify-between rounded-4xl bg-gray-200 cursor-pointer"
        >
            <span class="px-4 text-gray-500">{{ userStore.user.name }}</span>
            <div class="py-1">
                <img src="/images/avator.png" alt="user-avator" class="h-8 w-auto" />
            </div>
        </div>

        <!-- login or register -->
        <div v-else class="flex items-center gap-4">
            <router-link
                to="/login"
                class="text-black text-md hover:text-sky-400 hover:font-bold transition-colors cursor-pointer rounded-md px-4 py-1 border-2"
            >
                登录
            </router-link>
            <button
                class="bg-sky-950 border-2 text-white hover:bg-sky-300 hover:font-bold rounded-md px-4 py-1 text-md transition-colors cursor-pointer"
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

//////// Router //////////////////////////////////
const route = useRoute()
const currentRoute = computed(() => route.path)

const navItems = [
    { name: '首页', path: '/home' },
    { name: '影像检索', path: '/data' },
    { name: '分析中心', path: '/analysis' },
]

const jumpToOGMS = () => {
    const OGMS_URL = 'https://geomodeling.njnu.edu.cn/'
    window.open(OGMS_URL, '_blank')
}

/////// User //////////////////////////////////
const userStore = useUserStore()
</script>

<style scoped>
.nav-link::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 2px;
    background-color: rgb(2 132 199);
    transform: scaleX(0);
    transition: transform 0.3s ease;
}

.nav-link:hover::after {
    transform: scaleX(1);
}

.nav-link-active::after {
    transform: scaleX(1);
}
</style>
