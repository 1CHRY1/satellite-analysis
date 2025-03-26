<template>
    <div>
        <div class="h-[8%] w-full flex justify-between">
            <div class="w-fit px-2 my-1 mx-2.5 shadow-md text-xs flex items-center bg-[#eaeaea] rounded">
                <div @click="handleClick('data')"
                    class="cursor-pointer pr-2 mr-2 border-r border-dashed border-gray-500 border-r-1"
                    :class="activeDataBase === 'data' ? 'text-[#1479d7]' : ''">
                    容器数据列表

                </div>
                <div @click="handleClick('output')" class="cursor-pointer"
                    :class="activeDataBase === 'output' ? 'text-[#1479d7]' : ''">
                    输出数据列表
                </div>
            </div>

            <div class="w-fit px-2 my-1 mx-2.5 shadow-md text-xs flex items-center bg-[#eaeaea] rounded">
                工具列表
            </div>

        </div>
        <div class="overflow-x-auto max-w-full h-[92%]">
            <table class="min-w-full table-auto border-collapse">
                <thead>
                    <tr class="bg-gray-200 sticky top-0">
                        <th class="py-2 px-4 text-left w-2/5">Name</th>
                        <th class="py-2 px-4 text-left w-3/10">Update time</th>
                        <th class="py-2 px-4 text-left w-1/5">Size</th>
                        <th class="py-2 px-4 text-left w-1/10">View</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="(item, index) in data" :key="index">
                        <td class="py-2 px-4 cursor-pointer" @click="handleCellClick(item, ' name')">
                            {{ item.name }}
                        </td>
                        <td class=" py-2 px-4 cursor-pointer" @click="handleCellClick(item, 'updateTime')">
                            {{ item.updateTime }}
                        </td>
                        <td class="py-2 px-4 cursor-pointer" @click="handleCellClick(item, 'size')">
                            {{ item.size }}
                        </td>
                        <td class="py-2 px-4 cursor-pointer" @click="handleCellClick(item, 'view')">
                            <span v-if="item.view" class="text-green-500">✔️</span>
                            <span v-else class="text-red-500">❌</span>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

</template>

<script setup lang="ts">
import { ref, defineEmits } from 'vue';
import type { PropType } from 'vue';
import type { dockerData } from '@/type/analysis';



defineProps({
    data: {
        type: Array as PropType<Array<dockerData>>,
        required: true,
    },
});
const emit = defineEmits<{ (event: 'changeView', item: dockerData): void }>();

const activeDataBase = ref('data');

// 数据列表切换点击事件
const handleClick = (type: string) => {
    activeDataBase.value = type
    // 在这里处理点击事件，切换数据
    if (type === 'data') {

    } else if (type === 'output') {

    }
    console.log("别忘了这里还要切换数据，没写完", type);
};

// 单元格点击事件处理
const handleCellClick = (item: dockerData, column: string) => {
    console.log(item, column);
    if (column === 'view') {
        emit('changeView', item)
    }
}
</script>

<style scoped lang="scss">
table {
    border: 0;
}

th,
td {
    border: 0;
    font-size: 10px;
    white-space: nowrap;
    /* 防止换行 */
    overflow: hidden;
    text-overflow: ellipsis;
}

thead {
    background-color: #f3f4f6;
}

th {
    position: sticky;
    top: 0;
    z-index: 10;
}
</style>