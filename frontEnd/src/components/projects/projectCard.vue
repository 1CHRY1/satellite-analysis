<template>
    <div
        class="card box-border flex h-[360px] relative w-[280px] cursor-pointer flex-col justify-between rounded-lg border-t border-l border-solid border-t-[rgba(255,255,255,.5)] border-l-[rgba(255,255,255,.5)] bg-black px-6 py-1 opacity-80">
        <!-- 头部 -->
        <div class="my-2 flex flex-col items-center justify-center">
            <Satellite class="mt-0 mb-1 h-16 w-16" color="white" />
            <div class="mb-1 w-full overflow-hidden text-3xl font-bold whitespace-nowrap text-blue-400 text-center"
                :title="project.projectName">
                {{ project.projectName }}
            </div>
        </div>
        <div class="absolute top-4 right-4" @click.stop="deleteConfirm">
            <CircleX />
        </div>

        <!-- 项目信息 -->
        <div class="flex flex-grow flex-col">
            <div class="subtitle my-2 text-[16px]">信息</div>
            <div class="relative flex-grow">
                <div class="absolute inset-0 overflow-auto text-sm text-white">
                    {{ project.description }}
                </div>
            </div>
        </div>

        <!-- 人员信息 -->
        <div class="flex flex-col space-y-1">
            <div class="subtitle my-2 text-[16px]">创建者</div>
            <div class="flex">
                <User color="white" class="mr-1.5" />{{ project.createUserName }}
            </div>
            <div class="flex">
                <Mail color="white" class="mr-1.5" />{{ project.createUserEmail }}
            </div>
            <div class="flex">
                <Clock3 color="white" class="mr-1.5" />{{
                    formatTime(project.createTime, 'minutes', 0)
                }}
            </div>
        </div>
    </div>
</template>

<script lang="ts" setup>
import { Satellite, User, Mail, Clock3, CircleX } from 'lucide-vue-next'
import { formatTime } from '@/util/common.ts'
import { ElMessage, ElMessageBox } from 'element-plus';
import { projectOperating } from "@/api/http/analysis"

const props = defineProps({
    project: {
        type: Object,
        default: () => ({}),
    },
})
const emit = defineEmits(['deleteProject'])

const userId = localStorage.getItem('userId')

const deleteConfirm = async () => {
    if (props.project.createUser !== userId) {
        ElMessage.error('只有创建者才能删除项目');
        return;
    }
    ElMessageBox.confirm(
        '确定要删除该项目吗？', // 提示内容
        '警告', // 标题
        {
            confirmButtonText: '确定', // 确认按钮文本
            cancelButtonText: '取消', // 取消按钮文本
            type: 'warning', // 对话框类型
        }
    )
        .then(async () => {
            // 用户点击了“确定”
            let params = {
                projectId: props.project.projectId, // 项目ID 
                userId: userId,
                action: "delete"
            }
            await projectOperating(params);
            ElMessage.success('项目删除成功');
            emit('deleteProject'); // 触发删除事件
        })
        .catch(() => {
            // 用户点击了“取消”
            ElMessage.info('已取消删除');
        });
};
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
