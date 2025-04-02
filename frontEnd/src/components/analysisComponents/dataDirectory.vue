<template>
    <div>
        <div class="h-[8%] w-full flex justify-between">
            <div class="w-fit px-2 my-1 mx-2.5 shadow-md text-xs flex items-center bg-[#eaeaea] rounded">
                <div @click="handleClick('data')"
                    class="cursor-pointer pr-2 mr-2 border-r border-dashed border-gray-500 border-r-1 "
                    :class="activeDataBase === 'data' ? 'text-[#1479d7]' : 'text-[#818999]'">
                    容器数据列表
                </div>
                <div @click="handleClick('output')" class="cursor-pointer"
                    :class="activeDataBase === 'output' ? 'text-[#1479d7]' : 'text-[#818999]'">
                    输出数据列表
                </div>
            </div>

            <div class="w-fit px-2 my-1 mx-2.5 shadow-md text-xs flex items-center bg-[#eaeaea] rounded text-[#818999]">
                工具列表
            </div>
        </div>
        <div class="overflow-x-auto max-w-full ">

            <table class="min-w-full table-auto border-collapse">
                <thead>
                    <tr class="sticky top-0 bg-gray-200 text-[#818999]">
                        <th class="w-2/5 px-4 py-2 text-left">文件名</th>
                        <th class="w-3/10 px-4 py-2 text-left">更新时间</th>
                        <th class="w-1/5 px-4 py-2 text-left">文件大小</th>
                        <th class="w-1/10 px-4 py-2 text-left">预览</th>
                    </tr>
                </thead>
                <tbody>
                    <tr class="text-[#818999]" v-for="(item, index) in tableData" :key="index">
                        <td class="py-2 ml-4 cursor-pointer flex " @click="handleCellClick(item, ' name')">
                            <div class="w-4 h-4 flex justify-center items-center mr-1">
                                <img :src="'/filesImg/' + item.fileType + '.png'" alt="" />
                            </div>
                            {{ item.fileName }}
                        </td>
                        <td class=" py-2 px-4 cursor-pointer" @click="handleCellClick(item, 'updateTime')">
                            {{ formatTime(item.updateTime) }}
                        </td>
                        <td class="cursor-pointer px-4 py-2" @click="handleCellClick(item, 'size')">
                            {{ sizeConversion(item.fileSize) }}
                        </td>
                        <td class="cursor-pointer px-4 py-2" @click="handleCellClick(item, 'view')">
                            <span v-if="item.view" class="text-green-500">✔️</span>
                            <span v-else class="text-red-500">❌</span>
                        </td>
                    </tr>
                    <tr v-if="tableData.length === 0">
                        <td colspan="4" class="py-10 text-center !text-base text-gray-500">
                            <div class="flex h-full flex-col items-center justify-center">
                                <CircleSlash :size="40" class="mb-2 text-gray-500" />
                                <span class="!text-base text-gray-500">暂无数据</span>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { dockerData } from '@/type/analysis'
import { getFiles, getMiniIoFiles, getTileFromMiniIo } from '@/api/http/analysis'
import { sizeConversion, formatTime } from '@/util/common'
import { ElMessage } from 'element-plus'
import { map_flyTo, addRasterLayerFromUrl, removeRasterLayer } from '@/util/map/operation'
import { RefreshCcw, CircleSlash } from 'lucide-vue-next'

const props = defineProps({
    projectId: {
        type: String,
        required: true,
    },
    userId: {
        type: String,
        required: true,
    },
})

const tableData = ref<Array<dockerData>>([])
const inputData = ref<Array<dockerData>>([])
const outputData = ref<Array<dockerData>>([])

const activeDataBase = ref('data')

// 数据列表切换点击事件
const handleClick = async (type: string) => {
    activeDataBase.value = type
    // 在这里处理点击事件，切换数据
    if (activeDataBase.value === 'data') {
        await getInputData()
        tableData.value = inputData.value
    } else if (activeDataBase.value === 'output') {
        await getOutputData()
        tableData.value = outputData.value
    }
}

const refreshTableData = async () => {
    if (activeDataBase.value === 'data') {
        await getInputData()
        tableData.value = inputData.value
        ElMessage.success('数据更新成功')
    } else if (activeDataBase.value === 'output') {
        await getOutputData()
        tableData.value = outputData.value
        ElMessage.success('数据更新成功')
    }
}

// 单元格点击事件处理
const handleCellClick = async (item: dockerData, column: string) => {
    console.log(item)

    if (column === 'view') {
        if (item.fileType === "tif") {
            console.log(item.filePath);

            const targetItem = (activeDataBase.value === "data" ? inputData.value : outputData.value).find((data) => data.updateTime === item.updateTime && data.fileSize === item.fileSize && data.fileName === item.fileName);
            if (targetItem) {
                targetItem.view = !targetItem.view;
            }
        } else {
            ElMessage.warning("暂不支持预览")
        }

    }
}

const getInputData = async () => {
    let tempData = await getFiles({
        userId: props.userId,
        projectId: props.projectId,
        path: '/data',
    })
    if (tempData.length === 0) {
        return []
    }

    if (tempData.length === 0) {
        setTimeout(async () => {
            tempData = await getFiles({
                "userId": "rgj",
                "projectId": props.projectId,
                "path": "/data"
            })
        }, 1000);
    }

    inputData.value = tempData.map((item: any) => {
        return { ...item, view: false }
    })

}
const getOutputData = async () => {
    let tempData = await getFiles({
        userId: props.userId,
        projectId: props.projectId,
        path: '/output',
    })
    if (tempData.length === 0) {
        return []
    }
    if (tempData.length === 0) {
        setTimeout(async () => {
            tempData = await getFiles({
                "userId": "rgj",
                "projectId": props.projectId,
                "path": "/output"
            })
        }, 1000);
    }
    outputData.value = tempData.map((item: any) => {
        return { ...item, view: false }
    })
}

onMounted(async () => {
    setTimeout(async () => {
        // nextTick(async () => {
        await getInputData()
        await getOutputData()
        tableData.value = activeDataBase.value === 'data' ? inputData.value : outputData.value
        // })
    }, 1000)
    // setTimeout(async () => {
    // nextTick(async () => {
    //     await getInputData()
    //     await getOutputData()
    //     tableData.value = activeDataBase.value === 'data' ? inputData.value : outputData.value;
    // })


    // }, 300);

    console.log(tableData.value, 'tableData.value');

    console.log(tableData.value, 'tableData.value')
})
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
