<template>
    <div class="relative" ref="menuContainer">
        <button
        @click="toggleMenu"
        class="group relative flex cursor-pointer items-center justify-between rounded-4xl bg-gray-800 transition-colors hover:bg-gray-700">
            <span class="px-2 py-1 font-semibold whitespace-nowrap text-white transition-all duration-300 hover:text-sky-100 sm:text-lg">
                {{ userStore.user.name }}
            </span>
        </button>
        <div v-show="showMenu"
        class="absolute right-0 mt-2 w-64 rounded-md bg-gray-800 shadow-lg z-50 border border-gray-700">
            
            <div 
            v-for="(item,index) in menuOption"
            :key="item.path || index"
            class=""
            >
                <div v-if="item.children" class="relative group">
                    <span class="px-2 py-1 font-semibold whitespace-nowrap text-white">{{ item.name }}</span>
                    <button 
                    v-for=" option in item.children"
                    :key="option.name"
                    class="block w-full px-2 py-1 text-sm text-centre text-gray-300 hover:bg-gray-700 cursor-pointer flex items-center justify-between whitespace-nowrap"
                    @click="handleMenuClick(option)">
                        {{ option.name }}
                    </button>
                <div class="border-t border-gray-400 my-4"></div>   
                </div>

                <div v-else>
                    <button 
                    class="block w-full px-2 py-1 font-semibold whitespace-nowrap text-white text-centre  hover:bg-gray-700 cursor-pointer flex items-center justify-between whitespace-nowrap"
                    @click="handleMenuClick(item)">
                        {{ item.name }}
                    </button>
                    <div class="border-t border-gray-400 my-4"></div>
                </div>
            </div>
            <div class="hover:bg-gray-700 cursor-pointer"
                    @click="handleLogout">
                    {{t('nav.button.logout')}}
            </div>
        </div> 
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/store';

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


.menu-enter-active,
.menu-leave-active {
    transition: all 0.2s ease;
}

.menu-enter-from,
.menu-leave-to {
    opacity: 0;
    transform: translateY(-10px);
}
</style>
