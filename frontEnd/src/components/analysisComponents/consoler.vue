<template>
    <div class="w-full h-full overflow-auto bg-gray-50">
        <!-- 顶部栏 -->
        <div class="w-full h-12 flex items-center justify-between px-6 bg-white border-b border-gray-200">
            <div class="font-sans text-lg font-bold text-gray-800 tracking-wide uppercase">控制台</div>
            <a-button @click="clearConsole" type="primary" danger class="flex items-center justify-center"
                style="display: flex; align-items: center; justify-content: center;">
                <ClearOutlined class="my-1" />
                清空
            </a-button>
        </div>

        <!-- 信息显示区域 -->
        <div class="w-full h-[calc(100%-3rem)] p-4 overflow-y-auto bg-gray-50" ref="consoleContent">
            <div v-for="(message, index) in messages" :key="index"
                class="mb-2 text-sm text-gray-700 whitespace-pre-wrap break-words">
                {{ message }}
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, watch, defineEmits, onMounted, nextTick } from "vue";
import { ClearOutlined } from '@ant-design/icons-vue'; // 引入图标

const props = defineProps<{
    messages: string[];
}>();

const emit = defineEmits<{ (event: 'clearConsole'): void }>();

const consoleContent = ref<HTMLDivElement | null>(null);

watch(
    () => props.messages,
    () => {
        // 当消息列表变化时，滚动到底部
        scrollToBottom();
    },
    { deep: true }
);

const clearConsole = () => {
    emit('clearConsole')
};

const scrollToBottom = () => {
    if (consoleContent.value) {
        nextTick(() => {
            consoleContent.value!.scrollTop = consoleContent.value!.scrollHeight;
        });
    }
};
onMounted(() => {
    // 组件挂载完成后滚动到底部
    scrollToBottom();
});

</script>

<style scoped lang="scss"></style>