<template>
    <div
        class="group relative flex h-[160px] w-[280px] cursor-pointer flex-col justify-between overflow-hidden rounded-lg bg-slate-900/80 px-4 py-3 transition-all duration-300 ease-in-out hover:shadow-lg hover:shadow-blue-500/10"
        @click="handleClick"
        :class="{ checked: checked }"
    >
        <!-- 头部 -->
        <div class="flex items-center justify-between">
            <div class="flex items-center">
                <div class="mr-2.5 flex h-8 w-8 items-center justify-center rounded-md bg-blue-500/10 text-blue-400">
                    <Satellite class="h-4 w-4" />
                </div>
                <div class="overflow-hidden text-base font-bold text-blue-400">
                    {{ project.projectName }}
                </div>
            </div>
            <a-radio :checked="props.checked" @click.stop />
        </div>

        <!-- 项目信息 -->
        <div class="my-1.5 flex-grow">
            <div class="line-clamp-2 text-xs text-slate-300">
                {{ project.description || '暂无描述' }}
            </div>
        </div>

        <!-- 人员信息 -->
        <div class="flex flex-col space-y-1">
            <div class="flex items-center text-xs text-slate-300">
                <User class="mr-1.5 h-3.5 w-3.5 text-blue-400" />
                <span>{{ project.createUserName }}</span>
            </div>
            <div class="flex items-center text-xs text-slate-300">
                <Mail class="mr-1.5 h-3.5 w-3.5 text-blue-400" />
                <span>{{ project.createUserEmail }}</span>
            </div>
            <div class="flex items-center text-xs text-slate-300">
                <Clock3 class="mr-1.5 h-3.5 w-3.5 text-blue-400" />
                <span>{{ formatTime(project.createTime, 'minutes', 0) }}</span>
            </div>
        </div>
    </div>
</template>

<script lang="ts" setup>
import { Satellite, User, Mail, Clock3 } from 'lucide-vue-next'
import { formatTime } from '@/util/common.ts'
import type { Project } from './type'
import type { PropType } from 'vue'

const props = defineProps({
    project: {
        type: Object as PropType<Project>,
        default: () => ({}),
    },
    checked: {
        type: Boolean,
        default: false,
    },
})

const emit = defineEmits(['select'])

const handleClick = () => {
    emit('select', props.project)
}
</script>

<style scoped>
.checked {
    box-shadow: 0 0 0 2px #165cffae inset;
}
</style>
