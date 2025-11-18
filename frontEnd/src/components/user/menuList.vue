<template>
    <div class="relative" ref="menuContainer">
        <!-- User Button with Avatar and Dropdown Indicator -->
        <button
            @click="toggleMenu"
            class="group relative flex cursor-pointer items-center gap-3 rounded-full bg-gradient-to-r from-gray-800 to-gray-900 px-4 py-2.5 transition-all duration-300 hover:from-gray-700 hover:to-gray-800 hover:shadow-lg"
        >
            <!-- User Avatar -->
            <div class="flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 text-white text-sm font-semibold">
                <User :size="18" />
            </div>

            <!-- Username -->
            <span class="font-semibold text-white transition-all duration-300 hover:text-blue-100 sm:text-base">
                {{ userStore.user.name }}
            </span>

            <!-- Dropdown Indicator -->
            <ChevronDown
                :size="16"
                class="text-gray-400 transition-transform duration-200 group-hover:text-gray-300"
                :class="{ 'rotate-180': showMenu }"
            />
        </button>

        <!-- Dropdown Menu with Animation -->
        <Transition
            enter-active-class="transition ease-out duration-200"
            enter-from-class="transform opacity-0 scale-95 translate-y-1"
            enter-to-class="transform opacity-100 scale-100 translate-y-0"
            leave-active-class="transition ease-in duration-150"
            leave-from-class="transform opacity-100 scale-100 translate-y-0"
            leave-to-class="transform opacity-0 scale-95 translate-y-1"
        >
            <div
                v-show="showMenu"
                class="absolute right-0 mt-3 min-w-64 rounded-xl bg-gray-900/95 backdrop-blur-sm shadow-xl border border-gray-700/50 z-50 overflow-hidden"
            >
                <!-- Menu Items -->
                <div class="py-2">
                    <div
                        v-for="(item, index) in menuOption"
                        :key="item.path || index"
                    >
                        <!-- Items with children -->
                        <div v-if="item.children" class="px-2">
                            <div class="px-3 py-2 text-sm font-semibold text-gray-300 border-b border-gray-700">
                                {{ item.name }}
                            </div>
                            <button
                                v-for="option in item.children"
                                :key="option.name"
                                @click="handleMenuClick(option)"
                                class="group flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-gray-300 transition-all duration-200 hover:bg-gray-700 hover:text-white"
                            >
                                <div class="flex h-6 w-6 items-center justify-center rounded-md bg-gray-700 group-hover:bg-gray-600">
                                    <ChevronRight :size="14" class="text-gray-400 group-hover:text-gray-200" />
                                </div>
                                {{ option.name }}
                            </button>
                        </div>

                        <!-- Regular menu items -->
                        <div v-else class="px-2">
                            <button
                                @click="handleMenuClick(item)"
                                class="group flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-gray-300 transition-all duration-200 hover:bg-gray-700 hover:text-white"
                            >
                                <div class="flex h-6 w-6 items-center justify-center rounded-md bg-gray-700 group-hover:bg-gray-600 transition-colors duration-200">
                                    <Settings :size="14" class="text-gray-400 group-hover:text-gray-200" />
                                </div>
                                {{ item.name }}
                            </button>
                        </div>

                        <!-- Divider (except for last item) -->
                        <div v-if="index < menuOption.length - 1" class="mx-2 my-1 border-t border-gray-700"></div>
                    </div>

                    <!-- Logout Button -->
                    <div class="border-t border-gray-700 mt-2 pt-2 px-2">
                        <button
                            @click="handleLogout"
                            class="group flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-red-400 transition-all duration-200 hover:bg-red-900/30 hover:text-red-300"
                        >
                            <div class="flex h-6 w-6 items-center justify-center rounded-md bg-red-900/40 group-hover:bg-red-800/50 transition-colors duration-200">
                                <LogOut :size="14" class="text-red-400 group-hover:text-red-300" />
                            </div>
                            {{ t('nav.button.logout') }}
                        </button>
                    </div>
                </div>
            </div>
        </Transition>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/store';
import { User, ChevronDown, ChevronRight, Settings, LogOut } from 'lucide-vue-next';
import * as MapOperation from '@/util/map/operation'

const userStore = useUserStore()
const { t } = useI18n()
const showMenu = ref(false )


const menuContainer = ref<HTMLElement | null>(null)

interface optionChild {
    name: string,
    path?: string
}

interface menuItem {
    name: string, 
    path?: string
    // action?:() => void
    children?: optionChild[]
}

const menuOption = ref<menuItem[]>([
    {name: '个人中心', path:'/user',},
    // {name: '安全设置', path:'',},
    // {name : '个性化', path:'',
    // children:[
    //     {name:'数据浏览查询', path:''},
    //     {name:'数据管理', path:''},
    //     {name:'数据可视化', path:''}
    // ]
    // },
    // {name: '设置', path:''},
    
])

const toggleMenu = () => {
    showMenu.value = !showMenu.value;
};

const handleMenuClick = (item: menuItem) => {
    if (item.path) {
        router.push(item.path);
    } 
    // else if (item.action) {
    //     item.action();
    // }
    showMenu.value = false;
};

const router = useRouter()
const handleLogout = () => {
    userStore.logout()
    // MapOperation.map_destroy_full();
    router.push('/home')
}

const handleClickOutside = (event: MouseEvent) => {
    if (menuContainer.value && !menuContainer.value.contains(event.target as Node)) {
        showMenu.value = false;
    }
};

// 添加/移除事件监听器
onMounted(() => {
    document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
/* Additional custom styles for enhanced visual effects */
.group:hover .transition-transform {
    transform: scale(1.02);
}

/* Smooth backdrop blur effect */
.backdrop-blur-sm {
    backdrop-filter: blur(8px);
}

/* Enhanced shadow for depth */
.shadow-xl {
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

/* Custom gradient background animation */
.bg-gradient-to-r:hover {
    background-size: 200% 200%;
    animation: gradient-shift 0.3s ease-in-out;
}

@keyframes gradient-shift {
    0% { background-position: 0% 50%; }
    100% { background-position: 100% 50%; }
}

/* Ripple effect on button press */
.group:active {
    transform: scale(0.98);
    transition: transform 0.1s ease-in-out;
}

/* Icon rotation animation */
.rotate-180 {
    transform: rotate(180deg);
}
</style>
