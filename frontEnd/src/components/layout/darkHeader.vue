<template>
    <header class="flex items-center justify-between px-10 py-2 shadow-md select-none">
        <div class="flex flex-3/5 items-center justify-start">
            <div class="flex items-center">
                <!-- <img :src="logo" alt="Logo" class="h-10 w-auto -translate-x-3 cursor-pointer" @click="jumpToOGMS" /> -->
                <satellite class="!text-[#3b82f6] h-8 w-8 mr-2" />

                <a
                    class="bg-gradient-to-r from-sky-100 to-white bg-clip-text font-semibold whitespace-nowrap text-transparent sm:text-lg md:text-xl lg:text-2xl"
                    href="home"
                    >
                    {{ t('nav.title') }}</a>
            </div>

            <nav class="ml-[5vw] flex items-center justify-center">
                <template v-for="(item, index) in navItems" :key="item.path || index" class="space-x-[1.5vw]">
                <!-- 分隔线 (移到前面) -->
                <div 
                    v-if="index > 0"
                    class="h-8 w-0 border-r-2 border-gray-600 mx-[1.5vw]">
                </div>
                
                <!-- 有子菜单的项 -->
                <div v-if="item.children" class="relative group">
                    <div
                    class="text-shadow-deepblue relative rounded-md px-2 py-1 font-semibold whitespace-nowrap text-white transition-all duration-300 hover:text-sky-100 sm:text-lg md:text-lg lg:text-xl cursor-pointer flex items-center"
                    @mouseenter="openSubMenu(index)"
                    @mouseleave="closeSubMenu"
                    
                    >
                    <a :href="item.path">
                        {{ item.name }}
                    </a>
                    <!-- <svg class="w-4 h-4 ml-1 transition-transform" :class="{ 'rotate-180': activeSubMenu === index }">
                        <path d="M5 8l5 5 5-5" stroke="currentColor" fill="none"/>
                    </svg> -->
                    </div>

                    <!-- 下拉菜单 -->
                    <transition name="menu-fade">
                    <ul
                        v-show="activeSubMenu === index"
                        class="submenu absolute left-0 mt-2 w-56 bg-slate-900/90 backdrop-blur-md border border-white/10 rounded-xl shadow-2xl ring-1 ring-black/5 z-50 p-2 space-y-1"
                        @mouseenter="openSubMenu(index)"
                        @mouseleave="closeSubMenu"
                    >
                        <li v-for="child in item.children" :key="child.path">
                        <router-link
                            v-if="child.path && !child.disabled"
                            :to="child.path"
                            class="block px-3 py-2 rounded-lg text-slate-100/90 hover:text-white hover:bg-white/10 transition-colors duration-200"
                            @click="closeSubMenu"
                        >
                            {{ child.name }}
                        </router-link>
                        <span
                            v-else
                            class="block px-3 py-2 rounded-lg text-slate-400/70 cursor-not-allowed bg-white/5"
                            @click="showDisabledMessage"
                        >
                            {{ child.name }}
                        </span>
                        </li>
                    </ul>
                    </transition>
                </div>

                <!-- 普通菜单项 -->
                <template v-else>
                    <a
                    v-if="item.external"
                    :href="item.path"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="text-shadow-deepblue relative rounded-md px-2 py-1 font-semibold whitespace-nowrap text-white transition-all duration-300 hover:text-sky-100 sm:text-lg md:text-lg lg:text-xl"
                    >
                    {{ item.name }}
                    </a>
                    <router-link
                    v-else
                    :to ="item.path|| '/'"
                    class="text-shadow-deepblue relative rounded-md px-2 py-1 font-semibold whitespace-nowrap text-white transition-all duration-300 hover:text-sky-100 sm:text-lg md:text-lg lg:text-xl"
                    >
                    {{ item.name }}
                    </router-link>
                </template>
                </template>
            </nav>
    </div>

        <!-- Switch language -->
         <div class="flex items-center mr-4">
            <SwitcheLanguage />
         </div>

        <!-- user authenticated -->
        <div v-if="userStore.authenticated"
            class="group relative flex cursor-pointer items-center justify-between rounded-4xl bg-gray-800 transition-colors hover:bg-gray-700">
            <menuList />
        </div>

        <!-- login or register -->
        <div v-else class="flex items-center gap-4">
            <!-- <router-link to="homePath"
                class="text-md cursor-pointer rounded-md border-2 border-sky-600 px-4 py-1 text-sky-300 transition-colors hover:bg-gray-800 hover:font-bold hover:text-sky-200">
                {{ t('nav.button.login') }}
            </router-link>
            <button @click="router.push('/home')"
                class="text-md cursor-pointer rounded-md border-2 border-sky-600 bg-sky-800 px-4 py-1 text-white transition-colors hover:bg-sky-700 hover:font-bold">
                {{ t('nav.button.signup') }}
            </button> -->
            <a class="text-md cursor-pointer rounded-md border-2 border-sky-600 px-4 py-1 text-sky-300 transition-colors hover:bg-gray-800 hover:font-bold hover:text-sky-200"
            href="home">
            {{ t('nav.button.login') }}
            </a>
            <a class="text-md cursor-pointer rounded-md border-2 border-sky-600 px-4 py-1 text-sky-300 transition-colors hover:bg-gray-800 hover:font-bold hover:text-sky-200"
            href="home">
            {{ t('nav.button.signup') }}
            </a>
        </div>
    </header>
</template>

<script setup lang="ts">
import { computed,ref, type ComputedRef, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store'
import { useRouter } from 'vue-router'
import SwitcheLanguage from './SwitchLanguage.vue';
import menuList from '@/components/user/menuList.vue'

// import logo from '@/assets/image/logo2.png'
import avator from '@/assets/image/avator.png'
import { Satellite } from 'lucide-vue-next'

import { useI18n } from 'vue-i18n'
const { t } = useI18n()

import { useExploreStore } from '@/store';
import type { AnyPaint } from 'mapbox-gl';
import { message } from 'ant-design-vue';
const exploreData = useExploreStore()
const load = exploreData.load 

const activeSubMenu = ref<number | null>(null);

const openSubMenu = (index: number) => {
  activeSubMenu.value = index;
};

const closeSubMenu = () => {
  activeSubMenu.value = null;
};

interface NavChild {
  name: string
  path?: any  
  disabled?: boolean
}

interface NavItem {
  external?: boolean;
  name: string;
  path?: string ;
  disabled?: boolean
  children?: NavChild[]; 
}
//////// Router //////////////////////////////////
const route = useRoute()
const router = useRouter()
const currentRoute = computed(() => route.path)

const homePath = '/home'
const navItems: ComputedRef<NavItem[]> = computed(() =>[
    //{ external: false, name: t('nav.home'), path: '/home' },
    { external: false, name: t('nav.source'), path: '/models', 
    children : [
        {name: t('nav.models'), path: '/models'},
        {name: '工具中心', path:'/tool'}
    ]
    },
    { external: false, name: t('nav.data'), path: '/explore',
    children : [
        {name: t('datapage.title_explore'), path: '/explore'},
        {
            name: t('datapage.title_nocloud'), 
            path: exploreData.load ? '/nocloud' : null,
            disabled: !exploreData.load
        },
        {name: t('datapage.title_analysis'), path: '/analysis'}
    ] 
    },
    { external: false, name: t('nav.tools'), path: '/projects',
      children: [
        { name: '在线编程', path: '/projects' },
        { name: '工具发布', path: '/tools' }
      ]
    },
    { external: true, name: t('nav.about'), path: 'http://opengmsteam.com/' },
    ]
)

// const jumpToOGMS = () => {
//     const OGMS_URL = 'https://geomodeling.njnu.edu.cn/'
//     window.open(OGMS_URL, '_blank')
// }
const showDisabledMessage = () => {
    message.warning(t('nav.disabled_message')) // 使用国际化消息
}
/////// User //////////////////////////////////
const userStore = useUserStore()

const handleLogout = () => {
    userStore.logout()
    router.push('/home')
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

/* Dropdown animation */
.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 180ms ease, transform 220ms cubic-bezier(0.2, 0.8, 0.2, 1);
}
.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(6px) scale(0.98);
}

/* Dropdown pointer */
.submenu::before {
  content: '';
  position: absolute;
  top: -8px;
  left: 18px;
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-bottom: 8px solid rgba(15, 23, 42, 0.9); /* matches bg-slate-900/90 */
}
.submenu::after {
  content: '';
  position: absolute;
  top: -9px;
  left: 18px;
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-bottom: 8px solid rgba(255,255,255,0.1); /* subtle border */
  filter: blur(0.2px);
}
</style>
