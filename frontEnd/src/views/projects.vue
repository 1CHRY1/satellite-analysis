<template>
    <div>
        <projectsBg
            class="h-full w-full overflow-hidden absolute inset-0 z-0  bg-[radial-gradient(ellipse_at_bottom,_#1b2735_0%,_#090a0f_100%)] ">
        </projectsBg>
        <div class="flex justify-center items-center z-99 relative">
            <div class="flex my-10 flex-col w-[50vw] justify-center items-center">
                <img src="@/assets/image/projectsName.png" class="h-12 w-fit" alt="">
                <div class="searchContainer mt-6 w-[100%]">
                    <div class="model_research">
                        <input type="text" autocomplete="false" placeholder="输入项目名称搜索项目" class="model_research_input "
                            v-model="searchInput" style="font-size: 16px" @keyup.enter="" />

                        <el-button type="primary" color="#049f40" style="
                            border-left: none;
                            border-top-left-radius: 0px;
                            border-bottom-left-radius: 0px;
                            border-color: #ffffff;
                            font-size: 16px;
                            height: 2rem;
                        " @click="">
                            <font-awesome-icon icon="magnifying-glass" style="margin-right: 5px" />
                            搜索
                        </el-button>
                    </div>
                    <el-button type="primary" color="#049f40" style="
                        margin-left: 10px;
                        border-color: #049f40;
                        font-size: 16px;
                        height: 2rem;
                    " @click="">创建</el-button>

                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import projectsBg from '@/components/projects/projectsBg.vue';

const searchInput = ref('');

</script>

<style scoped lang="scss">
.searchContainer {
    // height: 60px;
    // height: fit-content;
    display: flex;
    align-items: center;
    justify-content: center;
}

.model_research {
    position: relative;
    font-size: 14px;
    display: inline-flex;
    width: 100%;
}

.model_research_input {
    -webkit-appearance: none;
    background-color: rgba(0, 0, 0, 0.6);
    background-image: none;
    border-radius: 4px 0px 0px 4px;
    border: 1px solid #ffffff;
    border-right: none;

    box-sizing: border-box;
    color: #ffffff;
    display: inline-block;
    font-size: inherit;
    height: 2rem;
    line-height: 2rem;
    outline: 0;
    padding: 0 15px;
    transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
    width: 100%;
}

.model_research_input::-webkit-input-placeholder {
    color: #c3c5ca;
}
</style>