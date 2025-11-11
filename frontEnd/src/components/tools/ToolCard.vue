<template>
  <div :class="cardClass" :title="displayName" @click="$emit('click')">
    <div v-if="isOnline" class="service-ribbon">已发布服务</div>
    <div v-if="isOnline" class="service-accent"></div>

    <!-- 头部，与项目卡一致风格 -->
    <div class="card-section my-2 flex flex-col items-center justify-center">
      <Package class="mt-0 mb-1 h-16 w-16 icon" />
      <div class="title-wrapper">
        <span class="title-text">{{ displayName }}</span>
        <span v-if="isOnline" class="service-badge">Online</span>
        <span v-else class="offline-badge">Offline</span>
      </div>
    </div>

    <!-- 描述 -->
    <div class="card-section flex flex-grow flex-col">
      <div class="subtitle my-2 text-[16px]">信息</div>
      <div class="relative flex-grow">
        <div class="absolute inset-0 overflow-auto text-sm text-white desc">
          {{ tool.description || '暂无描述' }}
        </div>
      </div>
    </div>

    <!-- 元信息 -->
    <div class="card-section flex flex-col space-y-1">
      <div class="subtitle my-2 text-[16px]">创建者</div>
      <div class="flex meta-row"><User class="mr-1.5" />{{ tool.userId || '-' }}</div>
      <div class="flex meta-row" v-if="tool.updateTime || tool.createTime">
        <Clock3 class="mr-1.5" />{{ tool.updateTime || tool.createTime }}
      </div>
    </div>
  </div>
</template>

  <script setup lang="ts">
  import { computed } from 'vue'
  import { Package, User, Clock3 } from 'lucide-vue-next'

  defineEmits(['click'])
  const props = defineProps<{ tool: any }>()

  const displayName = computed(() => props.tool?.toolName || props.tool?.name || '未命名工具')
  const isOnline = computed(() => Number((props.tool as any)?.is_publish ?? 0) === 1)

  const cardClass = computed(() => [
    'card box-border flex h-90 relative w-70 cursor-pointer flex-col justify-between rounded-lg',
    'border-t border-l border-solid border-t-[rgba(255,255,255,.5)] border-l-[rgba(255,255,255,.5)]',
    'bg-black px-6 py-1 opacity-80 overflow-hidden transition-all duration-200',
    { 'service-card': isOnline.value, 'offline-dim': !isOnline.value },
  ])
  </script>

  <style scoped>
  .card {
    min-height: 360px;
  }
  .card:hover {
    scale: 1.05;
    border-width: 1px 2px 2px 1px;
  }
  .icon { color: white }
  .subtitle {
    border-bottom: solid 2px;
    border-image: linear-gradient(to right, rgba(81, 162, 255, 0.6), transparent 25%) 1;
  }
  .title-wrapper {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    width: 100%;
    max-width: 100%;
    font-size: 1.75rem;
    font-weight: 700;
    color: #60a5fa;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    text-align: center;
  }
  .title-text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .service-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 2px 8px;
    font-size: 0.75rem;
    font-weight: 600;
    color: #0f172a;
    background: linear-gradient(135deg, rgba(96, 165, 250, 0.95), rgba(37, 99, 235, 0.95));
    border-radius: 9999px;
    text-transform: uppercase;
    letter-spacing: 0.02em;
    box-shadow: 0 2px 6px rgba(59, 130, 246, 0.35);
  }
  .offline-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 2px 8px;
    font-size: 0.75rem;
    font-weight: 600;
    color: #e2e8f0;
    background: rgba(148,163,184,0.25);
    border-radius: 9999px;
  }
  .service-card { border: 1px solid rgba(20, 121, 215, 0.75); box-shadow: 0 0 18px rgba(59, 130, 246, 0.28); }
  .service-card:hover { box-shadow: 0 0 24px rgba(59, 130, 246, 0.42); }
  .service-card::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(14, 165, 233, 0.05));
    pointer-events: none;
    z-index: 0;
  }
  .service-ribbon {
    position: absolute;
    top: 16px;
    right: -38px;
    width: 140px;
    line-height: 1.6;
    text-align: center;
    font-size: 0.75rem;
    font-weight: 600;
    color: #0f172a;
    background: linear-gradient(135deg, rgba(96, 165, 250, 0.9), rgba(59, 130, 246, 0.9));
    transform: rotate(45deg);
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.35);
    pointer-events: none;
    z-index: 15;
  }
  .service-accent {
    position: absolute;
    top: 0;
    left: 0;
    width: 6px;
    height: 100%;
    background: linear-gradient(180deg, rgba(59, 130, 246, 0.95), rgba(14, 165, 233, 0.75));
    pointer-events: none;
    z-index: 12;
    border-radius: 0 6px 6px 0;
  }
  .offline-dim { filter: grayscale(0.15); opacity: 0.85; }
  .desc::-webkit-scrollbar { width: 0; height: 0; }
  </style>
