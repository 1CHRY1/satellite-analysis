<template>
  <div class="constructionContainer" id="container" @mousemove="handleMousemove($event)"
    @mousedown="handleMousedown($event)" @mouseup="handleMouseup($event)">
    <div v-show="showCodeContainer" class="codeContainer" id="codeContainerId">
      <div class="dataPaneArea" id="dataPaneAreaId"></div>
      <div class="splitHandleVertical" id="splitHandleVertical2Id" style="left: 30%;"></div>
      <div class="codeEditArea" id="codeEditAreaId"></div>

      <div class="splitHandleVertical" id="splitHandleVertical3Id" style="left: 70%;"></div>
      <div class="consolerArea" id="consolerAreaId">
        <consolerComponent> </consolerComponent>
      </div>

    </div>
    <div class="splitHandleHorizontal" id="splitPaneHorizontal1Id"></div>
    <div v-show="showMapContainer" class="mapContainer" id="mapContainerId">
      <mapComp style="height: inherit;"> </mapComp>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
// import mapComp from "@/components/feature/map/mapComp.vue";
import consolerComponent from "@/components/analysisComponents/consoler.vue";

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
    if (percentageValue > 90) {
      percentageValue = 90;
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
      document.getElementById("codeContainerId") && (document.getElementById("codeContainerId")!.style.flexGrow = "0");
      showCodeContainer.value = false;
      percentageValue = 0;
    } else {
      showCodeContainer.value = true;
      document.getElementById("codeContainerId") && (document.getElementById("codeContainerId")!.style.flexGrow = "1");
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
    document.getElementById("mapContainerId")!.style.flexGrow = ((100 - percentageValue) / percentageValue).toString();
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
  flex-direction: column;
  position: relative;

  .codeContainer {
    color: black;
    flex-grow: 1;
    // height: 50%;
    display: flex;

  }

  .mapContainer {
    color: black;
    flex-grow: 1;
    display: block;
    // height: 50%;
    // background: red;

    .modelContent {
      height: 100%;
    }
  }

  .dataPaneArea {
    width: 30%;
  }

  .codeEditArea {
    width: 40%;
  }

  .consolerArea {
    width: 30%;
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