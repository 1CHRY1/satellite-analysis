<template>
    <div>
        <div class="h-[8%] w-full flex justify-between">
            <div class="w-fit ml-2 bg-[#eaeaea] rounded flex items-center my-1 shadow-md">
                <div class="toolItem" @click="configEnv">
                    <CloudServerOutlined />
                    Environment
                </div>
                <div class="toolItem" @click="runCode">
                    <CaretRightOutlined />
                    Run
                </div>
                <div class="toolItem" @click="saveCode">
                    <SaveOutlined />
                    Save
                </div>
            </div>
            <div class="w-fit ml-2  rounded flex items-center my-1 relative ">
                <div class="px-2 h-full flex items-center text-xs my-1 mr-2 bg-[#eaeaea] rounded shadow-md cursor-pointer relative "
                    @click="toggleDropdown">
                    当前环境：{{ selectedEnv }}
                </div>
                <div v-if="showDropdown"
                    class="absolute left-0 top-8 w-fit mt-1 bg-white border border-gray-300 rounded shadow-md z-10">
                    <div v-for="env in envOptions" :key="env" class="px-3 py-2 hover:bg-gray-200 cursor-pointer text-sm"
                        @click="selectEnv(env)">
                        {{ env }}
                    </div>
                </div>
            </div>

        </div>
        <div class="code-editor h-[92%] w-full  bg-gray-100 rounded-lg overflow-y-auto text-xs font-sans">
            <Codemirror class=" !text-[12px] " v-model="code" :extensions="extensions" @ready="onCmReady"
                @update:model-value="onCmInput" />
        </div>
    </div>

</template>

<script setup lang="ts">
import { CloudServerOutlined, CaretRightOutlined, SaveOutlined } from "@ant-design/icons-vue";

import { ref } from "vue";
import { Codemirror } from "vue-codemirror";
import { python } from "@codemirror/lang-python";
// import { oneDarkTheme } from "@codemirror/theme-one-dark";


/**
 * 在线编程工具条
 */

const showDropdown = ref(false)
const envOptions = ["Python2.7", "Python3.6", "Python3.9"];
const selectedEnv = ref("Python 3.6");

const configEnv = () => {
    console.log("安装依赖");

};
const runCode = () => {
    console.log("执行代码");

};
const saveCode = () => {
    // 保存代码内容
    console.log("保存代码", code.value)
};

// 切换环境选择下拉框状态
const toggleDropdown = () => {
    showDropdown.value = !showDropdown.value;
};
const selectEnv = (env: string) => {
    selectedEnv.value = env;
    showDropdown.value = false;
};


/**
 * codemirror操作
 */

// 定义代码内容
const code = ref(`import pandas as pd
  # S3
  # ExtrapolationResults--RooftopAreaConsolidation
  # 将城市名相同的行进行合并，更换单位，并将小数点修改为后两位，保存为 rooftop_area_360
  extrapolation_results = pd.read_csv("./extrapolation_results.csv")
  rooftop_area_df = extrapolation_results.groupby('City').agg({'inference': 'sum'}) / 1e6
  
  rooftop_area_df['Rooftop_area'] = rooftop_area_df['inference'].round(2)
  rooftop_area_df.to_csv("./rooftop_area_360.csv")
  
  print(rooftop_area_df["inference"].sum())`);

// CodeMirror 配置项
const extensions = [python()]; // 使用正确的 light 主题

// 当编辑器初始化完成时触发
const onCmReady = (editor: any) => {
    console.log("CodeMirror is ready!", editor);
};

// 当代码内容发生变化时触发
const onCmInput = (value: string) => {
    // console.log("Code updated:", value);
};
</script>

<style scoped>
@reference 'tailwindcss';

.code-editor {
    @apply h-[90%] w-full p-2 bg-gray-100 rounded-lg overflow-y-auto overflow-x-hidden text-sm font-sans;
}

.toolItem {
    font-size: 14px;
    padding: 0 12px;
    border-right: 1.5px dashed #5f6477;
    color: #4c5160;
}

.toolItem:hover {
    color: #1479d7;
    cursor: pointer;
}

.toolItem:last-child {
    border-right: none;
}
</style>