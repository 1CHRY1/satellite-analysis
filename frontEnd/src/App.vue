<script setup lang="ts">
import Header from '@/components/layout/Header.vue'
import Footer from '@/components/layout/Footer.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
</script>

<template>
    <div class="flex min-h-screen flex-col bg-[rgb(245,244,238)]">
        <Header></Header>
        <a-config-provider :locale="zhCN">
            <RouterView class="flex-1"></RouterView>
        </a-config-provider>
        <Footer></Footer>
    </div>
</template>
