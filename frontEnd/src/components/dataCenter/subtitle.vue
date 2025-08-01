<template>
        <div class="button-container">
            <button class="button" style="border-radius: 0 4px 0 0;"
                @click="router.push('/explore')">
                <div class="buttontext" style="height: 140px;">
                    交互式探索
                </div>
            </button>
            <button class="button"
                @click="router.push('/nocloud')">
                <div class="buttontext">
                    数据准备
                </div>
            </button>
            <button class="button" style="border-radius: 0 0 4px 0;"
                @click="router.push('/analysis')">
                <div class="buttontext">
                    展示分析
                </div>
            </button>
        </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()

</script>

<style scoped>
.button-container {
    display: inline-block;
    width: min-content; 
    height: min-content; 
    background: transparent;
    padding: 0;
    overflow: hidden
    }

.button {
  width: 54px;
  height: 200px;
  background-color: #182944;
  border: none;
  box-shadow: inset 0 0 5px 0.5px #03fbffa6;
  padding: 0;
  margin: 0;
}

.button:hover {
  background-color: #182944;
}

.button:active {
  background-color: #128CFF;
}

.buttontext {
  width: 29px;
  height: 112px;
  color: #9BDFFF;
  text-align: left;
  font-size: 20px;
  font-weight: 700;
  font-family: "Source Han Sans";
  line-height: 28px;
  margin: 0 auto;
  display: block;
}

.buttontext:active {
  color: #ffffff;
}


</style>