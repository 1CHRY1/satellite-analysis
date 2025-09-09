<template>
  <div v-if="visible" class="mvt-popup-overlay" @click="handleOverlayClick">
    <div class="mvt-popup-content" @click.stop>
      <div class="popup-header flex">
        <h4 class="!ml-4">要素属性信息</h4>
        <button class="close-btn" @click="closePopup">×</button>
      </div>
      <div class="popup-body">
        <div v-if="Object.keys(properties).length === 0" class="no-data ml-5">
          无属性信息
        </div>
        <div v-else class="properties-list">
          <div 
            v-for="[key, value] in Object.entries(properties)" 
            :key="key" 
            class="property-item flex gap-5 !item-center"
          >
            <span class="property-key ml-5">{{ key }}:</span>
            <span class="property-value">{{ value }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import bus from '@/store/bus'

interface Props {
  visible?: boolean
  properties?: Record<string, any>
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  properties: () => ({})
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  close: []
}>()

const visible = ref(props.visible)
const properties = ref<Record<string, any>>(props.properties)

const closePopup = () => {
  visible.value = false
  emit('update:visible', false)
  emit('close')
}

const handleOverlayClick = () => {
  closePopup()
}

// 监听MVT点击事件
const handleMVTClick = (data: any) => {
  if (data && data.properties) {
    properties.value = data.properties
    visible.value = true
    emit('update:visible', true)
  }
}

// 监听ESC键关闭弹窗
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && visible.value) {
    closePopup()
  }
}

onMounted(() => {
  bus.on('mvt:feature:click', handleMVTClick)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  bus.off('mvt:feature:click', handleMVTClick)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.mvt-popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.mvt-popup-content {
  background-color: #0a1929;
  color: #e6f1ff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  max-width: 400px;
  max-height: 500px;
  width: 90%;
  overflow: hidden;
  border: 1px solid #1e3a5f;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #1e3a5f;
  background-color: #132f4c;
}

.popup-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #7eb3dd;
}

.close-btn {
  background: none;
  border: none;
  color: #a5d8ff;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.close-btn:hover {
  background-color: rgba(165, 216, 255, 0.1);
}

.popup-body {
  padding: 16px 20px;
  max-height: 400px;
  overflow-y: auto;
}

.no-data {
  text-align: center;
  color: #a5d8ff;
  font-style: italic;
  padding: 20px 0;
}

.properties-list {
  flex-direction: column;
  gap: 8px;
}

.property-item {
  padding: 8px 0;
  border-bottom: 1px solid #1e3a5f;

}

.property-item:last-child {
  border-bottom: none;
}

.property-key {
  font-weight: 600;
  color: #4dabf7;
  font-size: 12px;
  margin-bottom: 4px;
}

.property-value {
  color: #e6f1ff;
  font-size: 14px;
  word-break: break-all;
}

/* 滚动条样式 */
.popup-body::-webkit-scrollbar {
  width: 6px;
}

.popup-body::-webkit-scrollbar-track {
  background: #132f4c;
  border-radius: 3px;
}

.popup-body::-webkit-scrollbar-thumb {
  background: #1e3a5f;
  border-radius: 3px;
}

.popup-body::-webkit-scrollbar-thumb:hover {
  background: #4dabf7;
}
</style>