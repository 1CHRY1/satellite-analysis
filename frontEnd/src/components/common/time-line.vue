<template>
    <div class="fixed-timeline-bar">
        <div class="timeline-scroll-wrapper">
            <a-steps :current="-1" progress-dot size="small" class="custom-timeline">
                <a-step v-for="item in list" :key="item.value" :title="item.label"
                    :status="item.value === modelValue ? 'process' : 'wait'" @click="onItemClick(item)"
                    class="clickable-step" />
            </a-steps>
        </div>
    </div>
</template>

<script setup lang="ts">
/**
 * 定义数据结构接口
 */
export interface TimelineItem {
    label: string;  // 展示的文字（时间）
    value: string | number; // 唯一标识
    data?: any;     // 携带的原始业务数据
}

// 定义 Props
const props = defineProps<{
    // 数据列表
    list: TimelineItem[];
    // 当前选中的 value (支持 v-model)
    modelValue: string | number;
}>();

// 定义 Events
const emit = defineEmits<{
    (e: 'update:modelValue', value: string | number): void;
    (e: 'change', item: TimelineItem): void;
}>();

// 点击事件处理
const onItemClick = (item: TimelineItem) => {
    // 如果点击的是当前已选中的，可以选择不触发，这里默认允许重复触发
    emit('update:modelValue', item.value);
    emit('change', item);
};
</script>

<style scoped>
.fixed-timeline-bar {
    position: fixed;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 30%;
    max-width: 1200px;
    /* 移除背景颜色设置，保持透明或默认 */
    /* background: transparent; */
    backdrop-filter: blur(5px); /* 毛玻璃效果 */
    padding: 20px 0;
    z-index: 0;

    /* 让内容居中 */
    display: flex;
    justify-content: center;
    /* 防止点击穿透：如果你希望未点击到点的时候能点击到底下的页面内容，可以将 pointer-events 设为 none */
    /* pointer-events: none; */
}

.timeline-scroll-wrapper {
    /* 恢复点击响应 */
    pointer-events: auto;

    max-width: 95%;
    /* 左右留一点边距 */
    overflow-x: auto;
    overflow-y: hidden;

    /* 隐藏滚动条 */
    scrollbar-width: none;
    -ms-overflow-style: none;
}

.timeline-scroll-wrapper::-webkit-scrollbar {
    display: none;
}

.custom-timeline {
    /* width: auto 不会强制撑满，数据少时会居中 */
    width: auto;
    min-width: 300px;
    /* 设置一个最小宽度防止太窄 */
    white-space: nowrap;
    /* 防止换行 */
    padding: 0 10px;
}

/* 增加鼠标悬停手势，提升交互感 */
.clickable-step {
    cursor: pointer;
    transition: all 0.3s;
}

/* 深度选择器：微调样式 */
:deep(.ant-steps-item-title) {
    font-size: 13px;
    /* 字体稍微调小一点，更精致 */
    line-height: 1.5;
    user-select: none;
    /* 防止双击选中文字 */
}

/* 强制覆盖 Antdv 的默认点击行为样式，确保只有 process 状态是亮的 */
:deep(.ant-steps-item-wait .ant-steps-item-icon > .ant-steps-icon-dot) {
    background-color: rgba(0, 0, 0, 0.15) !important;
    /* 未选中点的颜色 */
}
</style>