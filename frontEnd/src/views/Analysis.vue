<template>
    <div class="constructionContainer" id="container">
        <Splitpanes class="default-theme" horizontal style="height: 100%;">
            <Pane :size="50" min-size="0" class="codeContainer">
                <Splitpanes class="default-theme">
                    <Pane :size="25" min-size="10" class="dataPaneArea">
                        <dataDirectory :projectId="projectId" :userId="userId" @removeCharts="removeCharts"
                            @showMap="changeMapState" @addCharts="addCharts" @importData="handleImportData" class="h-[100%] w-full rounded" />
                    </Pane>
                    <Pane :size="50" min-size="20" class="codeEditArea">
                        <codeEditor ref="codeEditorRef" :projectId="projectId" :userId="userId" @addMessage="addMessage"
                            @servicePublished="markProjectsStale" @serviceUnpublished="markProjectsStale"
                            class="h-[100%] w-full" />
                    </Pane>
                    <Pane :size="25" min-size="10" class="consolerArea">
                        <consolerComponent :messages="messages" @clearConsole="clearConsole">
                        </consolerComponent>
                    </Pane>
                </Splitpanes>
            </Pane>

            <Pane :size="50" min-size="0" class="mapContainer">
                <div class="absolute z-99 top-3 left-2 opacity-80">
                    <div class="mx-2 my-1 px-2 py-1 flex w-fit items-center rounded bg-[#eaeaea]  text-[14px] shadow-md">
                        <div @click="showMap = true"
                            class="mr-2 cursor-pointer border-r-1 border-dashed border-gray-500 pr-2"
                            :class="showMap === true ? 'text-[#1479d7]' : 'text-[#818999]'">
                            地图
                        </div>
                        <div @click="showMap = false" class="cursor-pointer"
                            :class="showMap === false ? 'text-[#1479d7]' : 'text-[#818999]'">
                            图表
                        </div>
                    </div>
                </div>

                <mapComp v-show="showMap" class="h-[100%]" :style="'local'" :proj="'mercator'">
                </mapComp>
                <charts ref="chartsRef" v-show="!showMap"></charts>
            </Pane>
        </Splitpanes>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
// 导入 splitpanes 组件
import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css' // 导入 splitpanes 样式

import mapComp from '@/components/feature/map/mapComp.vue'
import charts from '@/components/analysisComponents/charts.vue'
import consolerComponent from '@/components/analysisComponents/consoler.vue'
import codeEditor from '@/components/analysisComponents/codeEditor.vue'
import dataDirectory from '@/components/analysisComponents/dataDirectory.vue'
import { createWebSocket } from '@/api/websocket/websocketApi'
import { useUserStore } from '@/store'
import { useRoute } from 'vue-router'

// --- 业务逻辑和数据保持不变 ---
const userStore = useUserStore()
const route = useRoute()
const userId = userStore.user.id
const projectId = route.params.projectId as string

onMounted(() => {
    ws.connect()
    window.addEventListener('beforeunload', handleBeforeUnload)
})

onUnmounted(() => {
    window.removeEventListener('beforeunload', handleBeforeUnload)
    ws.close() // 关闭连接
})

// ... (addMessage, markProjectsStale, messages, ws, handleBeforeUnload, clearConsole 保持不变) ...
const addMessage = (messageContent: string = 'code') => {
    if (messageContent === 'code') {
        messages.value.push('开始执行代码,请稍候...')
    } else {
        messages.value.push(messageContent)
    }
}

const markProjectsStale = () => {
    try {
        localStorage.setItem('projectsNeedRefresh', Date.now().toString())
    } catch (error) {
        console.warn('标记项目刷新失败', error)
    }
}

const messages = ref<string[]>(['Response and execution information will be displayed here .'])
const ws = createWebSocket(userId, projectId)

ws.on('message', (data: any) => {
    messages.value.push(data)
})

ws.on('close', () => {
    console.log('WebSocket 连接已关闭')
})

const handleBeforeUnload = () => {
    ws.close() // 当页面即将关闭时，手动关闭 WebSocket
}

const clearConsole = () => {
    messages.value = ['Response and execution information will be displayed here .']
}

// ... (下半可视化模块逻辑保持不变) ...
const showMap = ref(true)
interface ChartInstance {
    addChart: (type: string, config: { labels: string[]; values: number[] }) => void;
    removeChart: (index: number) => void;
}
const chartsRef = ref<ChartInstance | null>(null)

const addCharts = (config: { labels: string[]; values: number[], type: string }) => {
    if (showMap.value === true) {
        showMap.value = false
    }
    if (chartsRef.value) {
        chartsRef.value.addChart(config.type, config)
    }
}

const removeCharts = () => {
    // 逻辑保持不变
}

const changeMapState = () => {
    showMap.value = true
}

// 处理数据导入到代码编辑器
interface CodeEditorInstance {
    insertCode: (code: string) => void;
}
const codeEditorRef = ref<CodeEditorInstance | null>(null)

const handleImportData = (code: string) => {
    if (codeEditorRef.value) {
        codeEditorRef.value.insertCode(code)
    }
}

// --- 移除所有手动拖拽相关的逻辑 ---
// activeSplitPane, containerHeight, containerWidth, mouseActTag, 
// showCodeContainer, showMapContainer, 
// handleMousedown, handleMouseup, handleMousemove, refreshContainerSize 全部移除

</script>

<style scoped lang="scss">
// 覆盖主容器的背景，确保内部没有透明的缝隙
:deep(.splitpanes) {
  // 移除官方给的彩虹背景，使用统一的深色背景
  background-color: #0d1117; 
}

// 覆盖 Pane 样式
:deep(.splitpanes__pane) {
  // 清除默认的 box-shadow，使用统一的 Pane 背景色
  box-shadow: none !important;
  justify-content: unset;
  align-items: unset;
  display: block;
  background-color: #161b22;
  border: 1px solid #21262d; 
}

// 覆盖默认主题分隔条，保持暗黑风格
:deep(.default-theme) {
    .splitpanes__splitter {
        // 分隔条颜色：比组件背景稍亮，易于识别
        background-color: #21262d; 
        transition: background-color 0.3s;
        
        // 拖动时的 hover 效果，使用亮蓝色高亮
        &:hover {
            background-color: #58a6ff;
        }
        &::before, &::after {
            background-color: #c9d1d9; // 分隔条上的小点颜色
        }
    }

    /* ----------------------------------------------------- */
    /* 💥 新增/修改：覆盖所有分隔条的亮色边框为 #0a2e49 */
    /* ----------------------------------------------------- */

    // 覆盖水平分隔条的顶部边框
    &.splitpanes--horizontal > .splitpanes__splitter,
    .splitpanes--horizontal > .splitpanes__splitter {
        border-top: 1px solid #262b32 !important;
        border-bottom: 1px solid #262b32 !important; /* 确保底部也被覆盖 */
    }

    // 覆盖垂直分隔条的左/右边框
    &.splitpanes--vertical > .splitpanes__splitter,
    .splitpanes--vertical > .splitpanes__splitter {
        border-left: 1px solid #262b32 !important;
        border-right: 1px solid #262b32 !important; /* 确保右侧也被覆盖 */
    }
}

.constructionContainer {
    width: 100vw;
    // height: calc(100vh - 56px);
    height: 100vh;
    display: flex;
    flex: none;
    flex-direction: column;
    position: relative;
    background-color: #0d1117;
    // background-color: #f9fafb;

    .codeContainer {
        // color: black;
        color: #c9d1d9; // 主文本颜色
        // flex-grow: 1;
        height: 50%;
        display: flex;

        .dataPaneArea {
            width: 25%;
        }

        .codeEditArea {
            width: 50%;
            height: 100%;
        }

        .consolerArea {
            // max-height: 100%;
            width: 25%;
            overflow: auto;
        }
    }

    .mapContainer {
        // color: black;
        color: #c9d1d9;
        // flex-grow: 1;
        display: block;
        height: 50%;
        position: relative;
        // background: red;

        .modelContent {
            height: 100%;
        }
    }
}
</style>
