<template>
  <header class="flex items-center justify-between  px-10 py-2 shadow-md select-none">
    <div class="flex flex-3/5 items-center justify-start">
      <div class="flex items-center">
        <img :src="logo" alt="Logo" class="h-10 w-auto -translate-x-3 cursor-pointer" @click="jumpToOGMS" />
        <satellite class="h-8 w-8 text-primary" />

        <span
          class=" bg-gradient-to-r from-sky-100 to-white bg-clip-text font-semibold whitespace-nowrap text-transparent sm:text-lg md:text-xl lg:text-2xl">卫星中心分析平台</span>
      </div>

      <nav class="ml-[5vw] flex items-center space-x-8">
        <div v-for="(item, index) in navItems" class="flex flex-row space-x-8">
          <router-link :key="item.path" :to="item.path"
            class="text-shadow-deepblue whitespace-nowrap relative rounded-md px-2 py-1 font-semibold text-white transition-all duration-300 hover:text-sky-100 sm:text-lg md:text-lg lg:text-xl"
            :class="{

              'nav-link': true,
              'nav-link-active': currentRoute === item.path,
            }">{{ item.name }}
          </router-link>
          <div class="h-8 w-0 border-r-2 border-gray-600" v-if="index < navItems.length - 1"></div>
        </div>
      </nav>
    </div>

    <!-- user authenticated -->
    <div v-if="userStore.authenticated"
      class="flex cursor-pointer items-center justify-between rounded-4xl bg-gray-800 hover:bg-gray-700 transition-colors">
      <span class="px-4 text-gray-300">{{ userStore.user.name }}</span>
      <div class="py-1">
        <img :src="avator" alt="user-avator" class="h-8 w-auto rounded-full" />
      </div>
    </div>

    <!-- login or register -->
    <div v-else class="flex items-center gap-4">
      <router-link to="/login"
        class="text-md cursor-pointer rounded-md border-2 border-sky-600 px-4 py-1 text-sky-300 transition-colors hover:bg-gray-800 hover:font-bold hover:text-sky-200">
        登录
      </router-link>
      <button
        class="text-md cursor-pointer rounded-md border-2 border-sky-600 bg-sky-800 px-4 py-1 text-white transition-colors hover:bg-sky-700 hover:font-bold">
        注册
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store'
import logo from '@/assets/image/logo2.png'
import avator from '@/assets/image/avator.png'
import { Satellite } from 'lucide-vue-next'


//////// Router //////////////////////////////////
const route = useRoute()
const currentRoute = computed(() => route.path)

const navItems = [
  { name: '首页', path: '/home' },
  { name: '模型库', path: '/models' },
  { name: '影像检索', path: '/data' },
  { name: '项目中心', path: '/projects' },
  { name: '分析中心', path: '/analysis' },
  { name: '关于我们', path: '/about' },
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