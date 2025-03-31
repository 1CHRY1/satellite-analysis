<template>
  <div class="constructionContainer" id="container" @mousemove="handleMousemove($event)"
    @mousedown="handleMousedown($event)" @mouseup="handleMouseup($event)">
    <div v-show="showCodeContainer" class="codeContainer" id="codeContainerId">
      <!-- 上左数据模块 -->
      <div class="dataPaneArea" id="dataPaneAreaId">
        <dataDirectory :projectId="projectId" class="h-[100%] w-full  rounded" />
      </div>
      <div class="splitHandleVertical" id="splitHandleVertical2Id" style="left: 25%;"></div>
      <!-- 上中在线编程 -->
      <div class="codeEditArea pl-2" id="codeEditAreaId">
        <codeEditor :projectId="projectId" class="h-[100%] w-full" />
      </div>

      <div class="splitHandleVertical" id="splitHandleVertical3Id" style="left: 75%;"></div>
      <!-- 上右控制台 -->
      <div class="consolerArea" id="consolerAreaId">
        <consolerComponent :messages="messages" @clearConsole="clearConsole"> </consolerComponent>
      </div>

    </div>
    <div class="splitHandleHorizontal" id="splitPaneHorizontal1Id"></div>
    <!-- 下方map控件 -->
    <div v-show="showMapContainer" class="mapContainer" id="mapContainerId">
      <mapComp class="h-[100%]"> </mapComp>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";

import mapComp from "@/components/feature/map/mapComp.vue";
import consolerComponent from "@/components/analysisComponents/consoler.vue";
import codeEditor from "@/components/analysisComponents/codeEditor.vue";
import dataDirectory from "@/components/analysisComponents/dataDirectory.vue";
import { createWebSocket } from "@/api/websocket/websocketApi"

// const userId = ref("rgj")
const projectId = ref("PRJL1EOibshGObEcPFHc")
// const projectName = ref("rgj_test0326")

onMounted(() => {
  ws.connect()
})

onUnmounted(() => {
  ws.close(); // 关闭连接
});

/**
 * dataDirectoryData模块
 */






/**
 * codeOnline模块
 * 
 */




/**
 * consoler子组件
 * 添加、清空、自动滚动
 */


const messages = ref<string[]>(['Response and execution information will be displayed here .']);
// 创建websocket实例
const ws = createWebSocket("rgj", projectId.value)

ws.on("message", (data: any) => {
  messages.value.push(data);
});

ws.on("close", () => {
  console.log("WebSocket 连接已关闭");
});

const clearConsole = () => {
  messages.value = ['Response and execution information will be displayed here .'];
};

// const addMessage = (msg: string) => {
//   messages.value.push(msg);
// };

// setInterval(() => {
//   addMessage(`Log: ${new Date().toLocaleTimeString()}`);
// }, 500);


/**
 * 页面模块大小分割模块
 * 用来移动横杆改变各模块大小
 * @param e 
 */

const activeSplitPane = ref<HTMLElement | null>(null);
const containerHeight = ref(0);
const containerWidth = ref(0);
const mouseActTag = ref(false);
const showCodeContainer = ref(true);
const showMapContainer = ref(true);

// 检测是否保持按下状态，并且分辨按下的是哪个分割条
const handleMousedown = (e: MouseEvent) => {
  refreshContainerSize();
  mouseActTag.value = true;
  if (e.target instanceof HTMLElement) {
    activeSplitPane.value = e.target;
  }
};

// 松开鼠标锁定容器大小
const handleMouseup = (_e: MouseEvent) => {
  mouseActTag.value = false;
};

// 移动分割条变化div宽高
const handleMousemove = (e: MouseEvent) => {
  const dataPaneArea = document.getElementById("dataPaneAreaId");
  const codeEditArea = document.getElementById("codeEditAreaId");
  const consolerArea = document.getElementById("consolerAreaId");
  const splitHandleVertical2 = document.getElementById("splitHandleVertical2Id");
  const splitHandleVertical3 = document.getElementById("splitHandleVertical3Id");
  // 移动左竖杆
  if (mouseActTag.value &&
    activeSplitPane.value?.id === "splitHandleVertical2Id") {
    // 减去的是左右固定内容的高度
    let percentageValue = ((e.x - 0) * 100) / containerWidth.value;


    // 限制最小和最大拖动范围
    let maxWidth = splitHandleVertical3!.style.left
    if (percentageValue >= Number(maxWidth.split("%")[0]) - 20) {
      percentageValue = Number(maxWidth.split("%")[0]) - 20;
    }
    if (percentageValue < 10) {
      percentageValue = 10;
    }

    // 修改容器大小与竖杆位置
    dataPaneArea!.style.width = percentageValue + "%";
    splitHandleVertical2!.style.left = percentageValue + "%";
    let totalWidthRate = Number(splitHandleVertical3!.style.left.split("%")[0])
    codeEditArea!.style.width = (totalWidthRate - percentageValue) + "%";


  }

  // 移动右竖杆
  if (mouseActTag.value &&
    activeSplitPane.value?.id === "splitHandleVertical3Id") {
    // 减去的是左右固定内容的高度
    let percentageValue = ((e.x - 0) * 100) / containerWidth.value;

    // 限制最小和最大拖动范围
    let minWidth = splitHandleVertical2!.style.left
    if (percentageValue <= Number(minWidth.split("%")[0]) + 20) {
      percentageValue = Number(minWidth.split("%")[0]) + 20;
    }
    if (percentageValue > 86.5) {
      percentageValue = 86.5;
    }

    // 修改容器大小与竖杆位置
    consolerArea!.style.width = (100 - percentageValue) + "%";
    splitHandleVertical3!.style.left = percentageValue + "%";
    let totalWidthRate = Number(splitHandleVertical2!.style.left.split("%")[0])
    codeEditArea!.style.width = (percentageValue - totalWidthRate) + "%";

  }


  // 移动横杆
  if (
    mouseActTag.value &&
    activeSplitPane.value?.id === "splitPaneHorizontal1Id"
  ) {
    // 减去的是容器上方固定内容的高度
    let percentageValue = ((e.y - 73.53) * 100) / containerHeight.value;

    showMapContainer.value = true;
    // 限制最小和最大拖动范围
    if (percentageValue < 0.1) {
      // document.getElementById("codeContainerId") && (document.getElementById("codeContainerId")!.style.flexGrow = "0");
      showCodeContainer.value = false;
      percentageValue = 0;
    } else {
      showCodeContainer.value = true;
      // document.getElementById("codeContainerId") && (document.getElementById("codeContainerId")!.style.flexGrow = "1");
    }
    if (percentageValue > 98.9) {
      percentageValue = 99;
      showMapContainer.value = false;
      activeSplitPane.value.style.top = percentageValue + "%";
      return
    } else {
      showMapContainer.value = true;
    }
    activeSplitPane.value.style.top = percentageValue + "%";
    document.getElementById("splitHandleVertical2Id")!.style.height = percentageValue + "%";
    document.getElementById("splitHandleVertical3Id")!.style.height = percentageValue + "%";
    document.getElementById("codeContainerId")!.style.height = percentageValue + "%";
    document.getElementById("mapContainerId")!.style.height = (100 - percentageValue) + "%";
    // document.getElementById("mapContainerId")!.style.flexGrow = ((100 - percentageValue) / percentageValue).toString();
  }
};


const refreshContainerSize = () => {
  const divElement = document.getElementById("container")!;
  containerHeight.value = divElement.offsetHeight;
  containerWidth.value = divElement.offsetWidth;
};
</script>

<style scoped lang="scss">
.constructionContainer {
  width: 100vw;
  height: calc(100vh - 74px);
  display: flex;
  flex: none;
  flex-direction: column;
  position: relative;
  background-color: #f9fafb;

  .codeContainer {
    color: black;
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
    color: black;
    // flex-grow: 1;
    display: block;
    height: 50%;
    // background: red;

    .modelContent {
      height: 100%;
    }
  }



  .splitHandleHorizontal {
    background-image: url("../assets/image/AnalysisHandle.png");
    background-repeat: no-repeat;
    background-position: center;
    background-color: #f5f5f5;
    border-color: #dcdcdc;
    border-style: solid;
    border-width: 0;
    border-top-width: 1px;
    border-bottom-width: 1px;
    cursor: row-resize;
    width: 100%;
    height: 8px;
    position: absolute;
    z-index: 10;
    left: 0;
    top: 50%;
  }

  .splitHandleVertical {
    background-image: url("../assets/image/analysisHandle_vertical.png");
    background-repeat: no-repeat;
    background-position: center;
    background-color: #f5f5f5;
    border-color: #dcdcdc;
    border-style: solid;
    border-width: 0;
    border-left-width: 1px;
    border-right-width: 1px;
    cursor: row-resize;
    width: 8px;
    height: 50%;
    position: absolute;
    z-index: 10;
  }
}
</style>