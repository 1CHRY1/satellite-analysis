<template>
    <div
        class="bg-black opacity-80 card box-border flex h-[360px] w-[280px] cursor-pointer flex-col justify-between rounded-lg border-t border-l border-solid border-t-[rgba(255,255,255,.5)] border-l-[rgba(255,255,255,.5)] px-6 py-1">
        <!-- 头部 -->
        <div class="my-2 flex flex-col items-center justify-center">
            <Satellite class="mt-4 mb-1 h-16 w-16" color="white" />
            <div class="mb-1 overflow-hidden text-3xl font-bold text-blue-400 whitespace-nowrap">
                {{ project.projectName }}
            </div>
        </div>
        <!-- 项目信息 -->
        <div class="flex flex-grow flex-col  ">
            <div class="subtitle my-2 text-[16px]">信息</div>
            <div class="relative flex-grow ">
                <div class="absolute inset-0 overflow-auto text-sm text-white">
                    {{ project.description }}
                </div>
            </div>
        </div>


        <!-- 人员信息 -->
        <div class="flex flex-col space-y-1 ">
            <div class="subtitle my-2 text-[16px]">创建者</div>
            <div class="flex">
                <User color="white" class="mr-1.5" />{{ project.createUserName }}
            </div>
            <div class="flex">
                <Mail color="white" class="mr-1.5" />{{ project.createUserEmail }}
            </div>
            <div class="flex">
                <Clock3 color="white" class="mr-1.5" />{{ formatTime(project.createTime, 'minutes', 0) }}
            </div>
        </div>
    </div>
</template>

<script lang="ts" setup>
import { Satellite, User, Mail, Clock3 } from 'lucide-vue-next'
import { formatTime } from "@/util/common.ts"

defineProps({
    project: {
        type: Object,
        default: () => ({}),
    },
})
</script>

<style lang="scss" scoped>
.card {
    transition: all 0.2s ease-in-out;
}

.card:hover {
    scale: 1.05;
    border-width: 1px 2px 2px 1px;
    // border-right: 1px solid rgba(255, 255, 255, .5);
    // border-bottom: 1px solid rgba(255, 255, 255, .5);

    // transition: 0.5s;
}

.subtitle {
    border-bottom: solid 2px;
    border-image: linear-gradient(to right, rgba(81, 162, 255, 0.6), transparent 25%) 1;
}

div::-webkit-scrollbar {
    width: none !important;
}

div {
    scrollbar-width: none !important;
    scrollbar-color: rgba(37, 190, 255, 0.332) transparent !important;
}
</style>
