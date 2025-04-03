<template>
    <div>
        <div class="flex h-[44px] w-full justify-between">
            <div class="mx-2.5 my-1.5 flex w-fit items-center rounded bg-[#eaeaea] px-2 text-[14px] shadow-md">
                <div @click="handleClick('data')"
                    class="mr-2 cursor-pointer border-r-1 border-dashed border-gray-500 pr-2"
                    :class="activeDataBase === 'data' ? 'text-[#1479d7]' : 'text-[#818999]'">
                    容器数据列表
                </div>
                <div @click="handleClick('output')" class="cursor-pointer"
                    :class="activeDataBase === 'output' ? 'text-[#1479d7]' : 'text-[#818999]'">
                    输出数据列表
                </div>
            </div>

            <div @click="refreshTableData"
                class="mx-2.5 my-1 flex w-fit cursor-pointer items-center rounded bg-[#eaeaea] px-2 text-[14px] text-[#818999] shadow-md">
                <RefreshCcw :size="16" class="text-primary" />
            </div>
        </div>
        <div class="h-[calc(100vh-44px)] max-w-full overflow-x-auto">
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
                        <td class="ml-4 flex cursor-pointer py-2" @click="handleCellClick(item, ' name')">
                            <div class="mr-1 flex h-4 w-4 items-center justify-center">
                                <img :src="'/filesImg/' + item.fileType + '.png'" alt="" />
                            </div>
                            {{ item.fileName }}
                        </td>
                        <td class="cursor-pointer px-4 py-2" @click="handleCellClick(item, 'updateTime')">
                            {{ formatTime(item.updateTime, 'minutes', 0) }}
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
    if (activeDataBase.value === "data") {
        ElMessage.info("目前仅支持输出数据预览")
        return
    }
    if (column === 'view') {
        if (item.fileType === 'tif' || item.fileType === 'tiff' || item.fileType === 'TIF') {
            const targetItem = (
                activeDataBase.value === 'data' ? inputData.value : outputData.value
            ).find(
                (data) =>
                    data.updateTime === item.updateTime &&
                    data.fileSize === item.fileSize &&
                    data.fileName === item.fileName,
            )
            if (targetItem) {
                // false变true才需要展示
                if (!targetItem.view) {
                    // 1、拿到miniIo里面的数据列表
                    let miniIoFile = await getMiniIoFiles({
                        userId: props.userId,
                        projectId: props.projectId,
                    })
                    // 2、根据view行所代表的数据信息，找到对应的miniIo实体
                    let targetInMiniIo = miniIoFile.find(
                        (data: any) => data.dataName === targetItem.fileName,
                    )
                    if (!targetInMiniIo?.dataId) {
                        console.info(
                            targetItem.fileName + '没有dataId，检查miniIo上是否存在这个数据实体',
                        )
                        ElMessage.info("该数据正在切片，请稍后再预览")
                        return
                    }
                    console.log(targetInMiniIo.dataId, 18156)

                    // 3、拿到数据实体的瓦片url
                    let tileUrlObj = await getTileFromMiniIo(targetInMiniIo.dataId)
                    let wholeTileUrl =
                        tileUrlObj.tilerUrl + '/{z}/{x}/{y}.png?object=/' + tileUrlObj.object
                    console.log(tileUrlObj, wholeTileUrl, 'wholeTileUrl')
                    if (!tileUrlObj.object) {
                        console.info(wholeTileUrl, '没有拿到瓦片服务的URL呢,拼接的路径参数是空的')
                        return
                    }
                    // addRasterLayerFromUrl("http://223.2.32.242:8079/{z}/{x}/{y}.png?object=/test-images/landset8_test/landset8_L2SP_test/tif/LC08_L2SP_118038_20241201_20241203_02_T1/LC08_L2SP_118038_20241201_20241203_02_T1_SR_B4.TIF", item.fileName + item.fileSize)
                    // 图层名为“文件名+文件大小”
                    addRasterLayerFromUrl(wholeTileUrl, item.fileName + item.fileSize)
                    // flyTo
                    if (0) {
                        map_flyTo([114.305542, 30.592807])
                    }
                    targetItem.view = !targetItem.view
                } else {
                    // 关闭时移除图层
                    removeRasterLayer(item.fileName + item.fileSize)
                    targetItem.view = !targetItem.view
                }
            }
        } else {
            ElMessage.warning('暂不支持预览')
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
