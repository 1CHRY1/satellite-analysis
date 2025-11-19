<template>
    <div class="container mx-auto">
      <!-- Header -->
      <header
        class="flex items-center w-full gap-6 p-4 bg-white border-b border-black"
      >
        <nav class="flex items-center gap-4">
          <router-link
            to="/user"
            class="text-lg font-semibold mr-10"
            :class="[
              {
                'text-yellow-500': route.name === '概览',
                'text-gray-700': route.name !== '概览',
              },
              'hover:opacity-70',
            ]"
          >
            <font-awesome-icon :icon="['far', 'eye']" />
            {{ t("userpage.userFunction.overview") }}
          </router-link>
           <router-link
            to="/user/projects"
            class="text-lg font-semibold mr-10"
            :class="[
              {
                'text-yellow-500': route.name === '我的项目',
                'text-gray-700': route.name !== '我的项目',
              },
              'hover:opacity-70',
            ]"
          >
            <font-awesome-icon :icon="['fas', 'bars-progress']" />
            {{ t("userpage.userFunction.projects") }}
          </router-link> 
          <router-link
            to="/user/data"
            class="text-lg font-semibold"
            :class="[
              {
                'text-yellow-500': route.name === '我的数据',
                'text-gray-700': route.name !== '我的数据',
              },
              'hover:opacity-70',
            ]"
            ><font-awesome-icon :icon="['fas', 'database']" />
            {{ t("userpage.userFunction.data") }}
          </router-link>
        </nav>
      </header>
  
      <main class="p-4">
        <!-- <router-view /> -->
        <router-view :key="route.fullPath" />
      </main>
    </div>
  </template>
  
  <script setup lang="ts">
  import { useRoute } from "vue-router";
  import { useI18n } from "vue-i18n";
  
  const route = useRoute();
  const { t } = useI18n();
  </script>
  
  <style scoped>
  .container {
    width: 100%;
    max-width: 1200px;
  }
  </style>