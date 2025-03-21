<script setup lang="ts">
import Header from './layout/Header.vue'
import Footer from './layout/Footer.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
dayjs.locale('zh-cn')
</script>

<template>
    <div class="flex flex-col min-h-screen bg-[rgb(245,244,238)]">
        <Header></Header>
        <a-config-provider :locale="zhCN">
            <RouterView class="flex-1"></RouterView>
        </a-config-provider>
        <Footer></Footer>
    </div>
</template>
