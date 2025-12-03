<template>
    <a-popover
        trigger="click"
        placement="bottomRight"
        :overlay-inner-style="{ padding: '0px' }"
        :overlay-style="{ zIndex: 101 }"
        :get-popup-container="getMapContainer"
        v-model:open="visible"
    >
        <template #content>
            <div class="layer-manager-popover-content" @click.stop>
                
                <div class="popover-header">
                    <span class="text-base font-semibold">图层管理</span>
                    <a-button type="text" size="small" @click="visible = false" title="关闭" class="text-white-btn">
                        <template #icon><CloseOutlined /></template>
                    </a-button>
                </div>

                <a-list 
                    item-layout="horizontal" 
                    :data-source="layers"
                    size="small"
                    class="layer-list-dense"
                >
                    <template #renderItem="{ item, index }">
                        <a-list-item class="layer-item-antdv-dense dark-list-item">
                            <a-list-item-meta class="flex-1">
                                <template #avatar>
                                    <component :is="getLayerIcon(item.type)" class="text-base text-cyan-400 flex-shrink-0" />
                                </template>
                                <template #title>
                                    <a-tooltip :title="item.name" placement="top">
                                        <span 
                                            class="text-sm truncate font-medium max-w-[180px]" :style="{ color: item.visibility === 'none' ? '#555' : '#ddd' }"
                                        >
                                            {{ item.name }}
                                        </span>
                                    </a-tooltip>
                                </template>
                                <template #description>
                                    <div class="flex items-center space-x-2 mt-1 -ml-1">
                                        <span class="text-xs text-gray-400 flex-shrink-0">透明度:</span>
                                        <a-slider
                                            :min="0"
                                            :max="100"
                                            :step="1"
                                            :value="item.opacity * 100"
                                            @change="setOpacity(item.id, $event as number)"
                                            class="flex-grow my-0 dark-slider"
                                            :tip-formatter="value => `${value}%`"
                                            :tooltipOpen="false"
                                        />
                                    </div>
                                </template>
                            </a-list-item-meta>

                            <a-space :size="2" class="flex-shrink-0 ml-4"> <a-switch 
                                    :checked="item.visibility === 'visible'" 
                                    @change="toggleVisibility(item.id)" 
                                    size="small"
                                    title="显示/隐藏"
                                />

                                <a-button 
                                    type="text" 
                                    size="small" 
                                    @click="moveLayer(item.id, 'up')" 
                                    :disabled="index === 0"
                                    title="上移"
                                    class="p-0 px-1 h-4 w-4 dark-control-btn"
                                >
                                    <template #icon><CaretUpOutlined /></template>
                                </a-button>
                                <a-button 
                                    type="text" 
                                    size="small" 
                                    @click="moveLayer(item.id, 'down')" 
                                    :disabled="index === layers.length - 1"
                                    title="下移"
                                    class="p-0 px-1 h-4 w-4 dark-control-btn"
                                >
                                    <template #icon><CaretDownOutlined /></template>
                                </a-button>

                                <a-popconfirm
                                    title="确定删除该图层吗?"
                                    ok-text="是"
                                    cancel-text="否"
                                    @confirm="removeLayer(item.id)"
                                    placement="left"
                                >
                                    <a-button 
                                        type="text" 
                                        danger 
                                        size="small" 
                                        title="删除"
                                        class="p-0 px-1 h-4 w-4"
                                    >
                                        <template #icon><DeleteOutlined /></template>
                                    </a-button>
                                </a-popconfirm>
                            </a-space>
                        </a-list-item>
                    </template>
                </a-list>
            </div>
        </template>

        <button @click="visible = true" class="map-button text-gray-900!"><Layers /></button>
    </a-popover>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, type Component } from 'vue';
// 请确保这里的路径是正确的
import { layerManager, type ManagedLayer } from '@/util/map/layerManager'; 
import { useI18n } from 'vue-i18n';
import { 
    List as AList, Switch as ASwitch, Slider as ASlider, 
    Button as AButton, Space as ASpace, Empty as AEmpty, 
    Popconfirm as APopconfirm, Popover as APopover, Tooltip as ATooltip
} from 'ant-design-vue';

// 引入 Antdv 图标
import { 
    DeleteOutlined, BarsOutlined, CloseOutlined,
    PictureOutlined, AppstoreOutlined, LineOutlined, EnvironmentOutlined,
    CaretUpOutlined, CaretDownOutlined
} from '@ant-design/icons-vue';
import { Layers } from 'lucide-vue-next';

const { t } = useI18n(); // 假设您已经设置了 i18n
// 确保 t('layerManager.title') 等键存在，如果不需要国际化，请直接写中文
if (!t) {
    // 假设 t 函数不存在时，提供一个占位符
    // @ts-ignore
    const t = (key: string) => key.split('.').pop() === 'title' ? '图层管理' : '暂无图层';
}

const layers = ref<ManagedLayer[]>([]);
const visible = ref(false); // 控制 Popover 显示/隐藏
const simpleImage = AEmpty.PRESENTED_IMAGE_SIMPLE;

// --- Mapbox/LayerManager 逻辑 ---

const getMapContainer = (): HTMLElement => {
    // 确保 Popover 挂载在地图容器内部，以便于定位
    return document.getElementById('mapContainer') || document.body;
}

const updateLayerList = () => {
    layers.value = layerManager.getLayers().slice().reverse(); 
};

const toggleVisibility = (id: string) => {
    layerManager.toggleVisibility(id); 
};

const setOpacity = (id: string, value: number) => {
    const opacity = value / 100;
    layerManager.setOpacity(id, opacity);
};

const moveLayer = (id: string, direction: 'up' | 'down') => {
    layerManager.moveLayer(id, direction); 
};

const removeLayer = (id: string) => {
    layerManager.removeLayer(id);
};

// --- 辅助函数：根据图层类型选择图标 ---
const getLayerIcon = (type: string): Component => {
    switch (type) {
        case 'raster':
        case 'heatmap':
        case 'dem':
            return PictureOutlined;
        case 'fill':
        case 'fill-extrusion':
            return AppstoreOutlined;
        case 'line':
            return LineOutlined;
        case 'symbol':
        case 'circle':
        default:
            return EnvironmentOutlined;
    }
}

onMounted(() => {
    updateLayerList();
    // 移除不正确的外部点击监听
    layerManager.registerUpdateCallback(updateLayerList);
});

onUnmounted(() => {
    // 实际生产中需要 LayerManager 提供 unregisterUpdateCallback
});
</script>

<style scoped>
@reference 'tailwindcss';

.map-button {
    @apply cursor-pointer rounded-md bg-white p-2 shadow-md shadow-gray-300 hover:bg-gray-50;
}

.map-button.grayscale {
    filter: grayscale(100%);
    opacity: 0.7;
}
/* Popover 内容区域样式 - 暗色背景 */
.layer-manager-popover-content {
    width: 350px; /* **【修改点1】** 增加宽度 */
    max-height: 400px;
    overflow-y: auto;
    padding: 0;
    background-color: #222; 
    color: #fff; 
}
/* ... (Header, List 样式保持不变) ... */

/* Popover 头部样式 */
.popover-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    border-bottom: 1px solid #333; 
    background-color: #333; 
}
.popover-header .font-semibold { color: #eee; }
.text-white-btn { color: #eee !important; }
.text-white-btn:hover { color: #40a9ff !important; background-color: transparent !important; }

/* 覆盖 Antdv 样式以实现高密度列表 */
.layer-list-dense { padding: 0 8px; }

/* 列表项样式 - 暗色背景 */
.layer-list-dense :deep(.ant-list-item) {
    padding: 8px 0 !important;
    border-bottom: 1px solid #333 !important; 
}
/* 列表项内容 */
.layer-item-antdv-dense :deep(.ant-list-item-meta-title) {
    color: #ddd; 
    margin-bottom: 0px !important;
    line-height: 1.2;
    display: flex;
    align-items: center;
    justify-content: space-between; 
}
/* 列表项的描述（透明度文字） */
.layer-item-antdv-dense :deep(.ant-list-item-meta-description) {
    color: #999; 
    padding-top: 2px;
}

/* 统一控制按钮颜色 */
.dark-control-btn { color: #ddd !important; }
.dark-control-btn:hover { color: #40a9ff !important; }
.dark-list-item { background: transparent !important; }

/* Slider 样式 */
.dark-slider :deep(.ant-slider-track) { background-color: #40a9ff !important; }
.dark-slider :deep(.ant-slider-rail) { background-color: #555 !important; }
.dark-slider :deep(.ant-slider-handle) { border-color: #40a9ff !important; background-color: #111 !important; }

/* Empty 组件 */
.dark-empty :deep(.ant-empty-description) { color: #aaa !important; }
.dark-empty :deep(.ant-empty-image svg) { fill: #555 !important; }

/* --- 其他高密度布局样式 (保持不变) --- */
.layer-item-antdv-dense { align-items: flex-start !important; }
.layer-item-antdv-dense :deep(.ant-list-item-meta-avatar) { margin-right: 6px; }
.layer-item-antdv-dense :deep(.ant-list-item-meta-description) { padding-top: 2px; }
.ant-slider { margin: 0px !important; }
.ant-space-item { line-height: 1; }
.layer-item-antdv-dense .ant-btn { min-width: 20px; }
</style>

<style>
/* 必须使用非 scoped style 来修改 Popover 的样式 */
.ant-popover-placement-bottomRight > .ant-popover-arrow { display: none; }
.ant-popover-inner {
    background-color: #222 !important; 
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5) !important; 
    border: 1px solid #333; 
}
</style>