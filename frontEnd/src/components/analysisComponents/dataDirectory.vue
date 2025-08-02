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

            <div class="flex">
                <div @click="refreshTableData"
                    class="mr-2.5 my-1.5 flex w-fit cursor-pointer items-center rounded bg-[#eaeaea] px-2 text-[14px] text-[#818999] shadow-md"
                    title="刷新数据">
                    <RefreshCcw :size="16" class="text-primary" />
                </div>
                <div @click="triggerFileSelect"
                    class="mr-2.5 my-1.5 flex w-fit cursor-pointer items-center rounded bg-[#eaeaea] px-2 text-[14px] text-[#818999] shadow-md"
                    title="上传geojson">
                    <Upload :size="16" class="text-primary" />
                </div>

                <!-- 隐藏文件选择框 -->
                <input ref="fileInput" type="file" accept=".json,.txt,.geojson" @change="uploadFile" class="hidden" />

                <!-- <div @click=""
                    class="mr-2.5 my-1.5 flex w-fit cursor-pointer items-center rounded bg-[#eaeaea] px-2 text-[14px] text-[#818999] shadow-md">
                    <FileUp :size="16" class="text-primary" />
                </div> -->
            </div>

        </div>
        <div class="h-[calc(100%-44px)] max-w-full overflow-x-auto">
            <table class="w-full table-auto border-collapse">
                <thead>
                    <tr class="sticky top-0 bg-gray-200 text-[#818999]">
                        <th class="w-auto min-w-[100px] px-4 py-2 text-left">文件名</th>
                        <th class="w-[100px] px-4 py-2 text-left">更新时间</th>
                        <th class="w-[60px] px-4 py-2 text-left">文件大小</th>
                        <th class="w-[60px] px-4 py-2 text-left">预览</th>
                    </tr>
                </thead>
                <tbody>
                    <tr class="text-[#818999] relative" v-for="(item, index) in tableData" :key="index">
                        <td class="ml-4 flex cursor-pointer py-2 overflow-hidden whitespace-nowrap text-ellipsis"
                           :title="item.fileName">
                            <div class="mr-1 flex h-4 w-4 items-center justify-center">
                                <img :src="'/filesImg/' + item.fileType + '.png'" alt="" />
                            </div>
                            {{ item.fileName }}
                        </td>
                        <td class="cursor-pointer px-4 py-2">
                            {{ formatTime(item.updateTime, 'minutes', -8) }}
                        </td>
                        <td class="cursor-pointer px-4 py-2" >
                            {{ sizeConversion(item.fileSize) }}
                        </td>
                        <td class="cursor-pointer relative px-4 py-2">
                            <!-- JSON 图标 -->
                            <span v-if="item.fileName.split('.')[1] === 'json'" @click="handlePreview(item)">
                                <ChartColumn :size="16" class="text-red-400 hover:text-red-600" />
                            </span>

                            <!-- Eye 图标：点击展开弹出框 -->
                            <span v-else-if="item.view" class="text-green-400 hover:text-green-600"
                                @click="handleUnPreview(item)">
                                <Eye :size="16" />
                            </span>

                            <!-- EyeOff 图标 -->
                            <span v-else class="text-red-400 hover:text-red-600" @click="handlePreview(item)">
                                <EyeOff :size="16" />
                            </span>


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
                <a-modal v-model:open="showModal" title="栅格可视化配置" @cancel="handleModalCancel" @ok="handleConfirm">
                    <a-select v-model:value="colormapName" style="width: 100%; margin-bottom: 1rem;" placeholder="请选择可视化色带"
                        :dropdownStyle="{backgroundColor: '#ffffff',borderRadius: '4px',}">
                        <a-select-option v-for="item in colormapOptions" :key="item.value" :value="item.value">
                            <div class="color-option">
                                <span>{{ item.label }}</span>
                                <div 
                                class="color-strip" 
                                :style="{ background: item.gradient }"
                                ></div>
                            </div>
                        </a-select-option>
                    </a-select>
                    <a-select v-model:value="selectedBidx" style="width: 100%" placeholder="请选择可视化波段"
                        :dropdownStyle="{backgroundColor: '#ffffff',borderRadius: '4px',}">
                        <a-select-option v-for="item in bidxOptions" :key="item" :value="item">
                            <span>{{ item }}</span>
                        </a-select-option>
                    </a-select>
                </a-modal>
            </table>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import type { dockerData } from '@/type/analysis'
import { getFiles, getMiniIoFiles, getTileFromMiniIo, uploadGeoJson, getJsonFileContent } from '@/api/http/analysis'
import { sizeConversion, formatTime } from '@/util/common'
import { ElMessage } from 'element-plus'
import { addRasterLayerFromUrl, removeRasterLayer, map_fitView } from '@/util/map/operation'
import { RefreshCcw, CircleSlash, Upload, Eye, EyeOff, ChartColumn, View } from 'lucide-vue-next'
import type { SelectProps } from 'ant-design-vue'
import { getImgStatistics } from '@/api/http/satellite-data/visualize.api'

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

const showedCache = ref<string[]>([])

const emit = defineEmits(["addCharts", "removeCharts", "showMap"])

const tableData = ref<Array<dockerData>>([])
const inputData = ref<Array<dockerData>>([])
const outputData = ref<Array<dockerData>>([])
const fileInput = ref<HTMLInputElement | null>(null)
const activeDataBase = ref('data')
const activeItem = ref<string | null>(null)

/**
 * 栅格可视化配置对话框相关变量
 */
const showModal = ref<boolean>(false)
const colormapName = ref<string>('rdylgn')
const colormapOptions = ref<SelectProps['options']>([
    {
        value: 'viridis',
        label: 'Viridis',
        gradient: 'linear-gradient(to right, #440154, #482475, #414487, #355f8d, #2a788e, #21908d, #22a884, #42be71, #7ad151, #bddf26, #fde725)'
    },
    {
        value: 'plasma',
        label: 'Plasma',
        gradient: 'linear-gradient(to right, #0d0887, #46039f, #7201a8, #9c179e, #bd3786, #d8576b, #ed7953, #fb9f3a, #fdca26, #f0f921)'
    },
    {
        value: 'magma',
        label: 'Magma',
        gradient: 'linear-gradient(to right, #000004, #1b0b41, #4b0c6b, #781c6d, #a52c60, #cf4446, #ed6925, #fb9b06, #f7d13d, #fcffa4)'
    },
    {
        value: 'inferno',
        label: 'Inferno',
        gradient: 'linear-gradient(to right, #000004, #1b0c42, #4b0c6b, #781c6d, #a52c5f, #cf4446, #ed6925, #fb9b06, #f7ce3b, #fcffa4)'
    },
    {
        value: 'cividis',
        label: 'Cividis',
        gradient: 'linear-gradient(to right, #00204d, #123570, #3b496c, #575d6d, #707173, #8a8778, #a69d75, #c4b56e, #e4cf5b, #ffea46)'
    },
    {
        value: 'turbo',
        label: 'Turbo',
        gradient: 'linear-gradient(to right, #30123b, #4145ab, #4675ed, #39a2fc, #1bcfd4, #24eca6, #61fc6c, #a4fc3b, #d1e834, #f3c63a, #fe9b2d, #f36315, #d93806, #b11901, #7a0402)'
    },
    {
        value: 'rainbow',
        label: 'Rainbow',
        gradient: 'linear-gradient(to right, #ff0000, #ff8000, #ffff00, #80ff00, #00ff00, #00ff80, #00ffff, #0080ff, #0000ff, #8000ff, #ff00ff)'
    },
    {
        value: 'coolwarm',
        label: 'Cool-Warm',
        gradient: 'linear-gradient(to right, #3b4cc0, #5977e2, #8da0fa, #c5caf2, #e2e2e2, #f1b6b6, #e67b7b, #d14949, #b2182b)'
    },
    {
        value: 'rdylgn',
        label: 'Red-Yellow-Green',
        gradient: 'linear-gradient(to right, #a50026, #d73027, #f46d43, #fdae61, #fee08b, #ffffbf, #d9ef8b, #a6d96a, #66bd63, #1a9850, #006837)'
    },
    {
        value: 'bwr',
        label: 'Blue-White-Red',
        gradient: 'linear-gradient(to right, #0000ff, #4444ff, #8888ff, #ccccff, #ffffff, #ffcccc, #ff8888, #ff4444, #ff0000)'
    },
    {
        value: 'seismic',
        label: 'Seismic',
        gradient: 'linear-gradient(to right, #00004d, #0000a3, #1c1cff, #6e6eff, #b9b9ff, #ffffff, #ffb9b9, #ff6e6e, #ff1c1c, #a30000, #4d0000)'
    },
    {
        value: 'jet',
        label: 'Jet',
        gradient: 'linear-gradient(to right, #00007f, #0000ff, #007fff, #00ffff, #7fff7f, #ffff00, #ff7f00, #ff0000, #7f0000)'
    },
    {
        value: 'hot',
        label: 'Hot',
        gradient: 'linear-gradient(to right, #0b0000, #4b0000, #960000, #e10000, #ff3d00, #ff7800, #ffb600, #fff100, #ffff6d)'
    },
    {
        value: 'hsv',
        label: 'HSV',
        gradient: 'linear-gradient(to right, #ff0000, #ff00cc, #cc00ff, #6600ff, #0000ff, #0066ff, #00ccff, #00ffff, #00ffcc, #00ff66, #00ff00, #66ff00, #ccff00, #ffff00, #ffcc00, #ff6600)'
    },
    {
        value: 'blues',
        label: 'Blues',
        gradient: 'linear-gradient(to right, #f7fbff, #deebf7, #c6dbef, #9ecae1, #6baed6, #4292c6, #2171b5, #08519c, #08306b)'
    },
    {
        value: 'greens',
        label: 'Greens',
        gradient: 'linear-gradient(to right, #f7fcf5, #e5f5e0, #c7e9c0, #a1d99b, #74c476, #41ab5d, #238b45, #006d2c, #00441b)'
    },
    {
        value: 'reds',
        label: 'Reds',
        gradient: 'linear-gradient(to right, #fff5f0, #fee0d2, #fcbba1, #fc9272, #fb6a4a, #ef3b2c, #cb181d, #a50f15, #67000d)'
    }
]);
const range = ref<number[]>([-1, 1])
const bidxOptions = ref<string[]>([])
const selectedBidx = ref<string | null>(null)
const tileUrlObj = ref<any>()
const stats = ref<any>()
const itemInMinIo = ref<any>()

const handleConfirm = async() => {
    
    if (activeDataBase.value === "data") {
        ElMessage.info("目前仅支持输出数据预览")
        return
    }

    const targetItem: any = (
        activeDataBase.value === 'data' ? inputData.value : outputData.value
    ).find((data) => data.fileName === activeItem.value)

    // 默认值
    selectedBidx.value = 'b1'
    range.value = [-1, 1]
    activeItem.value = null

    // 获取瓦片服务地址
    let mapPosition = itemInMinIo.value.bbox.geometry.coordinates[0]
    let tifUrl = `${tileUrlObj.value.minioEndpoint}/${tileUrlObj.value.object}`
    const bidx = selectedBidx.value === null ? 'b1' : selectedBidx.value
    range.value[0] = -1
    range.value[1] = 1
    let wholeTileUrl = `${tileUrlObj.value.tilerUrl}/tiles/WebMercatorQuad/{z}/{x}/{y}.png?scale=1&url=${tifUrl}&colormap_name=${colormapName.value}&rescale=${range.value[0]},${range.value[1]}`

    console.log(wholeTileUrl, 'wholeTileUrl')
    if (!tileUrlObj.value.object) {
        console.info(wholeTileUrl, '没有拿到瓦片服务的URL呢,拼接的路径参数是空的')
        ElMessage.error('瓦片服务错误')
        return
    }

    // 加载图层，图层名为"文件名+文件大小"
    addRasterLayerFromUrl(wholeTileUrl, targetItem.fileName + targetItem.fileSize)
    map_fitView(getBounds(mapPosition))
    showedCache.value.push(targetItem.fileName + targetItem.fileSize)
    targetItem.view = true

    showModal.value = false
}

const handleModalCancel = () => {
    activeItem.value = null
    showModal.value = false
}

const handleUnPreview = (item: dockerData) => {
    const fileSuffix = item.fileName.split('.').pop()?.toLowerCase()
    const targetItem: any = (
            activeDataBase.value === 'data' ? inputData.value : outputData.value
        ).find((data) => data.fileName === item.fileName)
    if (fileSuffix === 'tif' || fileSuffix === 'tiff' || fileSuffix === 'TIF' || fileSuffix === 'TIFF') {
        // 关闭时移除图层
        removeRasterLayer(item.fileName + item.fileSize)
        // 从缓存中移除
        showedCache.value = showedCache.value.filter((cache) => cache !== item.fileName + item.fileSize)
    }
    targetItem.view = !targetItem.view
}

const handlePreview = async(item: dockerData) => {
    activeItem.value = item.fileName
    const fileSuffix = item.fileName.split('.').pop()?.toLowerCase()
    if (fileSuffix === 'tif' || fileSuffix === 'tiff' || fileSuffix === 'TIF' || fileSuffix === 'TIFF') {
        handleTifPreview(item)
    } else if (fileSuffix === 'json' || fileSuffix === 'txt') {
        const targetItem = (
            activeDataBase.value === 'data' ? inputData.value : outputData.value
        ).find((data) => data.fileName === item.fileName)

        if (targetItem) {
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
                    ElMessage.info('该数据正在上传，请稍后再预览')
                    return
                }

                let dataEntity = await getJsonFileContent(targetInMiniIo.dataId)
                console.log(dataEntity);
                emit('addCharts', dataEntity.data)
                // targetItem.view = true
            } else {
                console.log("关闭预览？");
                // targetItem.view = false
            }
        }
    } else {
        ElMessage.warning('暂不支持预览')
    }
}

const handleTifPreview = async (item: dockerData) => {
    /**
     * 校验是否能预览
     */
    if (activeDataBase.value === "data") {
        ElMessage.info("目前仅支持输出数据预览")
        return
    }
    emit('showMap')
    const targetItem = (
        activeDataBase.value === 'data' ? inputData.value : outputData.value
    ).find((data) => data.fileName === item.fileName)

    let targetInMiniIo: any = null
    if (targetItem && !targetItem.view) {
        // 1、拿到miniIo里面的数据列表
        let miniIoFile = await getMiniIoFiles({
            userId: props.userId,
            projectId: props.projectId,
        })
        // 2、根据view行所代表的数据信息，找到对应的miniIo实体
        targetInMiniIo = miniIoFile.find(
            (data: any) => data.dataName === targetItem.fileName,
        )
        if (!targetInMiniIo?.dataId) {
            console.info(
                targetItem.fileName + '没有dataId，检查miniIo上是否存在这个数据实体',
            )
            ElMessage.info('该数据正在切片，请稍后再预览')
            return
        }
    }

    /**
     * 符合预览标准
     */
    showModal.value = true
    itemInMinIo.value = targetInMiniIo
    tileUrlObj.value = await getTileFromMiniIo(targetInMiniIo.dataId)
    stats.value = await getImgStatistics(tileUrlObj.value.object)
    bidxOptions.value = Object.keys(stats.value)
}

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

const triggerFileSelect = () => {
    fileInput.value?.click()
}

// 上传geojson的方法,这里最好检查一下键名，比如feature什么的，进一步收紧限制，防止用户上传错误的geojson
const uploadFile = async (event: Event) => {
    const target = event.target as HTMLInputElement;
    if (!target.files || target.files.length === 0) return;
    const file: File = target.files[0];
    if (!file) return

    // 检查文件类型
    const allowedTypes = ['application/json', 'text/plain']
    const allowedExtensions = ['.json', '.txt', '.geojson'];

    const fileExtension = file.name.split('.').pop()?.toLowerCase() || '';
    if (!allowedTypes.includes(file.type) && !allowedExtensions.includes(`.${fileExtension}`)) {
        ElMessage.warning('只支持 .geojson, .json 或 .txt 文件')
        return
    }

    // 读取文件内容
    const reader = new FileReader()
    reader.onload = async (e: ProgressEvent<FileReader>) => {
        try {

            const fileContent = e.target?.result as string

            const jsonData = JSON.parse(fileContent)

            const res = await uploadGeoJson({
                userId: props.userId,
                projectId: props.projectId,
                uploadDataName: file.name.split('.')[0], // 文件名作为数据名
                uploadData: jsonData
            })

            if (res.status === 1) {
                await handleClick(activeDataBase.value)
                ElMessage.success('上传成功')
            } else {
                ElMessage.error('上传失败')
            }
        } catch (err) {
            ElMessage.error('文件内容不是有效的 JSON 格式')
        }
    }

    reader.readAsText(file)

    // 清空 input 的值，以便多次上传同一个文件时也能触发 change
    target.value = '';
}

// 从一组坐标点中获得左下和右上坐标点
const getBounds = (coordinates: number[][]) => {
    let minLng = Infinity; // 最小经度（西经）
    let maxLng = -Infinity; // 最大经度（东经）
    let minLat = Infinity; // 最小纬度（南纬）
    let maxLat = -Infinity; // 最大纬度（北纬）
    for (const [lng, lat] of coordinates) {
        if (lng < minLng) minLng = lng;
        if (lng > maxLng) maxLng = lng;
        if (lat < minLat) minLat = lat;
        if (lat > maxLat) maxLat = lat;
    }
    return [
        [minLng, minLat], // 左下角（西经, 南纬）
        [maxLng, maxLat]  // 右上角（东经, 北纬）
    ];
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
        if (showedCache.value.includes(item.fileName + item.fileSize)) item.view = true
        else item.view = false

        return item
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

.color-option {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.color-strip {
  width: 100%;
  height: 20px;
  margin-top: 4px;
  border-radius: 2px;
}

/* 调整下拉框选项宽度 */
:deep(.ant-select-dropdown) {
  min-width: 200px !important;
}

:deep(.ant-select-item) {
  padding: 8px 12px;
}
</style>
