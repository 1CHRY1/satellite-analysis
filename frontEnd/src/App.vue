<script setup lang="ts">
import DarkHeader from './components/layout/darkHeader.vue'
// import Header from '@/components/layout/Header.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
</script>

<template>
    <div class="flex min-h-screen flex-col bg-[rgb(245,244,238)]">
        <DarkHeader class="fixed top-0 left-0 right-0 z-50 h-[56px]"></DarkHeader>
        <!-- <Header></Header> -->
        <a-config-provider :locale="zhCN">
            <RouterView class="relative flex-1 pt-[56px]"></RouterView>
        </a-config-provider>
        <!-- <Footer></Footer> -->
    </div>
</template>
