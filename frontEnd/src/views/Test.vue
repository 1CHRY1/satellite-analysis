<template>
    <div class="h-full bg-white flex items-center justify-center">
        <!-- <button class="t">click</button> -->
        <!-- <a-button class="bg-red-500 w-25 h-25"> AHHA </a-button> -->

        <div class="w-[500px] h-[500px] bg-amber-200">
            <div class="h-[200px] bg-amber-400">Lorem ipsum dolor, sit amet consectetur adipisicing elit. Minima, sit!</div>
            <div class="h-[100px] bg-amber-950">Lorem ipsum dolor, sit amet consectetur adipisicing elit. Minima, sit!</div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, h } from 'vue';
// import * as MapOperation from '@/util/map/operation'

onMounted(() => {

})


</script>

<style>
@reference 'tailwindcss';

.aaaaaaaaaa {
    @apply bg-red-500 w-25 h-25;
}

.aaa {
    background-color: red;
    width: 50px;
    height: 50px;
}
</style>