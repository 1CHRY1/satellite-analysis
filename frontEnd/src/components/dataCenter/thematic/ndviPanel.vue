<template>
    <!-- Section: 输入模块 -->
    <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <MapIcon :size="18" />
            </div>
            <h2 class="section-title">NDVI时序计算</h2>
        </div>
        <div class="section-content">
            <div class="config-container">
                <div class="config-item">
                    <div class="config-label relative">
                        <MapIcon :size="16" class="config-icon" />
                        <span>空间选择</span>
                    </div>
                    <div class="config-control flex-col  gap-2 w-full">
                        <div class="flex gap-10">
                            <!-- 地图选点块 -->
                            <div @click="toggleMode('point')"
                                class="w-24 h-24 flex flex-col items-center justify-center rounded-lg border cursor-pointer transition-all duration-200 text-white"
                                :class="[
                                    activeMode === 'false'
                                        ? 'border-[#2bb2ff] bg-[#1a2b4c]'
                                        : 'border-[#247699] bg-[#0d1526]',
                                    'hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95'
                                ]">
                                <MapPinIcon class="mb-2" />
                                地图选点
                            </div>

                            <!-- 划线采点块 -->
                            <div @click="!true && toggleMode('line')"
                                class="w-24 h-24 flex flex-col items-center justify-center rounded-lg border cursor-pointer transition-all duration-200 text-white relative"
                                :class="[
                                    activeMode === 'false'
                                        ? 'border-[#2bb2ff] bg-[#1a2b4c]'
                                        : 'border-[#247699] bg-[#0d1526]',
                                    true
                                        ? 'opacity-50 cursor-not-allowed pointer-events-none'
                                        : 'hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95'
                                ]">
                                <LayersIcon class="mb-2" />
                                划线采点
                                <div v-if="true"
                                    class="absolute inset-0 bg-black bg-opacity-40 rounded-lg flex flex-col items-center justify-center text-xs text-white cursor-not-allowed">
                                    <LayersIcon class="mb-2" />
                                    划线采点
                                </div>
                            </div>
                        </div>
                        <div class="flex gap-4 my-4 items-center mb-4">
                            添加辅助图斑:
                            <div class="  relative">
                                <el-select v-model="selectedDimension" multiple placeholder="Select" class="w-[190px]"
                                    @change="showMVTLayers(selectedDimension)" popper-class="ndviSelect">
                                    <el-option v-for="item in allOptions" :key="item.type" :label="item.label"
                                        :value="item.type" class="!bg-transparent">
                                        <div class="flex items-center">
                                            <el-tag :color="item.color" class="mr-2 aspect-square border-none"
                                                size="small" />
                                            <span class="text-white">{{ item.label }}</span>
                                        </div>
                                    </el-option>

                                    <template #tag>
                                        <el-tag v-for="type in selectedDimension" :key="type"
                                            :color="getColorByType(type)" class="aspect-square border-none" />
                                    </template>
                                </el-select>
                            </div>
                        </div>
                        <button @click="analysisNDVI"
                            class="cursor-pointer w-full rounded-lg border border-[#247699] bg-[#0d1526] px-4 py-2 text-white transition-all duration-200 hover:border-[#2bb2ff] hover:bg-[#1a2b4c] active:scale-95">
                            开始分析
                        </button>
                        <!-- 请确定您要研究的区域： -->
                        <!-- <div class="flex items-center gap-2 mt-2 w-full">
                            <label class="text-white">影像选择：</label>
                            <select v-model="selectedSceneId" @change="showImageBBox"
                                class="bg-[#0d1526] text-[#38bdf8] border border-[#2c3e50] rounded-lg px-3 py-1 appearance-none hover:border-[#2bb2ff] focus:outline-none focus:border-[#3b82f6] max-w-[calc(100%-90px)] truncate">
                                <option disabled selected value="">请选择影像</option>
                                <option v-for="image in props.regionConfig.images" :key="image.sceneName"
                                    :value="image.sceneId" :title="image.sceneName" class="truncate">
                                    {{ image.sceneName }}
                                </option>
                            </select>
                        </div> -->
                        <!-- <div class="result-info-container">
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <Earth :size="16" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">纬度</div>
                                    <div class="result-info-value">{{ 1 }}
                                    </div>
                                </div>
                            </div>
                            <div class="result-info-item">
                                <div class="result-info-icon">
                                    <Earth :size="16" />
                                </div>
                                <div class="result-info-content">
                                    <div class="result-info-label">经度</div>
                                    <div class="result-info-value">{{ 1 }} </div>
                                </div>
                            </div>
                        </div> -->
                    </div>

                </div>
            </div>

        </div>
    </section>

    <!-- Section: 结果展示 -->
    <section class="panel-section">
        <div class="section-header">
            <div class="section-icon">
                <ChartColumn :size="18" />
            </div>
            <h2 class="section-title">计算结果</h2>
        </div>
        <div class="section-content">
            <div v-if="analysisData.length === 0" class="flex justify-center my-6">
                <SquareDashedMousePointer class="mr-2" />暂无计算结果
            </div>
            <div class="config-item" v-for="(item, index) in analysisData" :key="index">
                <div>第{{ index + 1 }}次计算：{{ item.analysis }}</div>
                <!-- <div>NDVI计算结果为：xxx</div> -->
                <!-- <div>统计数据-统计数据-统计数据-统计数据</div> -->
                <div>经纬度：（{{ item.point[0] }},{{ item.point[1] }}）</div>

                <div class="chart-wrapper flex flex-col items-end">
                    <div class="chart" :ref="el => setChartRef(el, index)" :id="`chart-${index}`"
                        style="width: 100%; height: 400px;"></div>
                    <button class="!text-[#38bdf8] cursor-pointer" @click="fullscreenChart(index)">全屏查看</button>
                </div>
            </div>
        </div>
    </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch, type ComponentPublicInstance, type ComputedRef, type Ref } from 'vue'
import * as MapOperation from '@/util/map/operation'
import { getBoundaryBySceneId, getCaseResult, getCaseStatus, getNdviPoint, getRasterScenesDes } from '@/api/http/satellite-data'
import { ElMessage } from 'element-plus'
import bus from '@/store/bus'
import mapboxgl from 'mapbox-gl'
import * as echarts from 'echarts'


import {
    ChartColumn,
    Earth,
    MapPinIcon,
    CalendarIcon,
    UploadCloudIcon,
    RefreshCwIcon,
    HexagonIcon,
    CloudIcon,
    ApertureIcon,
    ClockIcon,
    ImageIcon,
    LayersIcon,
    DownloadIcon,
    FilePlus2Icon,
    BoltIcon,
    BanIcon,
    MapIcon,
    SquareDashedMousePointer,
    Bus
} from 'lucide-vue-next'
import { useGridStore } from '@/store'
import { mapManager } from '@/util/map/mapManager'

type ThematicConfig = {
    allImages: any,
    regionId: number,
    startTime: string,
    endTime: string
}
type LatLng = [number, number]

type tag = {
    value: string,
    label: string,
}

const props = defineProps<{ thematicConfig: ThematicConfig }>()
const allNdviRateImages = ref<any>([])
const initNdviRatePanel = async () => {
    let thematicConfig = props.thematicConfig
    if (!thematicConfig.regionId) return
    let rasterParam = {
        startTime: thematicConfig.startTime,
        endTime: thematicConfig.endTime,
        regionId: thematicConfig.regionId,
        dataType: 'ndvi'
    }
    allNdviRateImages.value = await getRasterScenesDes(rasterParam)
    console.log(allNdviRateImages.value, 57);
}




const selectedSceneId = ref('')
const activeMode = ref<'point' | 'line' | 'false' | null>(null)
const gridStore = useGridStore()
const pickedPoint: ComputedRef<LatLng> = computed(() => {
    return [
        Math.round(gridStore._point[0] * 1000000) / 1000000,
        Math.round(gridStore._point[1] * 1000000) / 1000000
    ];
})
const pickedLine: ComputedRef<LatLng[]> = computed(() => {
    return gridStore._line.map(([lat, lng]) => [
        Math.round(lat * 1000000) / 1000000,
        Math.round(lng * 1000000) / 1000000
    ])
})

const allOptions = [
    { label: '耕地', type: 'farm', color: '#FFA07A' },   // 浅橙红（salmon）- 农田温暖色
    { label: '林地', type: 'forest', color: '#228B22' }, // 森林绿（forest green）- 森林直观
    { label: '草地', type: 'grass', color: '#7CFC00' },  // 草绿色（lawn green）- 明亮活力
    { label: '水体', type: 'water', color: '#1E90FF' },  // 道奇蓝（dodger blue）- 水体专属
    { label: '城市', type: 'city', color: '#CD5C5C' },    // 印度红（更贴近城市建筑）
    { label: '未利用', type: 'inuse', color: '#D3D3D3' },// 浅灰（light gray）- 表示未使用
    { label: '海洋', type: 'ocean', color: '#20B2AA' },  // 浅海蓝（light sea green）- 海洋专属
];
const selectedDimension = ref<string[]>([]);
// 选中的维度

// 将所有维度颜色添加到初始框中
// allOptions.forEach((option) => {
//     selectedDimension.value.push(option.type);
// });

const initMVTLayers = (options: { type: string; color: string }[]) => {
    const regionId = props.thematicConfig.regionId;

    mapManager.withMap((map) => {
        options.forEach(({ type, color }) => {

            const sourceId = `${type}-source`;
            const layerId = `${type}-layer`;
            const tileUrl = `http://${window.location.host}/api/geo/vector/tiles/patch/region/${regionId}/type/${type}/{z}/{x}/{y}`;
            // 添加 Source
            if (!map.getSource(sourceId)) {
                map.addSource(sourceId, {
                    type: 'vector',
                    tiles: [tileUrl],
                });
            }
            // 添加 Layer（不可见）
            if (!map.getLayer(layerId)) {
                map.addLayer({
                    id: layerId,
                    type: 'fill',
                    source: sourceId,
                    'source-layer': 'patch',
                    paint: {
                        'fill-color': color,
                        'fill-opacity': 0.5,
                    },
                    layout: {
                        visibility: 'none', // 初始隐藏
                    },
                });
            }
        });
    });
};

// 控制哪些图斑可见
const showMVTLayers = (visibleOptions: string[]) => {

    const visibleTypes = new Set(visibleOptions);

    mapManager.withMap((map) => {
        // 遍历所有当前图层
        map.getStyle()?.layers.forEach((layer) => {
            if (layer.id.endsWith('-layer')) {
                const type = layer.id.replace('-layer', '');
                const visibility = visibleTypes.has(type) ? 'visible' : 'none';
                map.setLayoutProperty(layer.id, 'visibility', visibility);
            }
        });
    });
};
const getColorByType = (type: string) => {
    return allOptions.find(opt => opt.type === type)?.color || '#ccc';
};


const toggleMode = (mode: 'point' | 'line' | 'false') => {
    // activeMode.value = activeMode.value === mode ? null : mode
    activeMode.value = mode
    if (mode === 'point') {
        MapOperation.draw_pointMode()
        ElMessage.info('请在地图上绘制研究点')
    } else if (mode === 'line') {
        MapOperation.draw_lineMode()
        ElMessage.info('请在地图上绘制研究线')
    }
}
/**
 * 计算NDVI
 */
const calTask: Ref<any> = ref({
    calState: 'start',
    taskId: ''
})
const analysisData = ref<any>([])

const analysisNDVI = async () => {
    if (!pickedPoint.value[0] || !pickedPoint.value[1]) {
        ElMessage.warning('请先选择您要计算的区域')
        return
    }
    let getNdviPointParam = {
        sceneIds: allNdviRateImages.value.map(image => image.sceneId),
        point: [pickedPoint.value[1], pickedPoint.value[0]]
    }
    ElMessage.success('开始ndvi时序分析。')

    let getNdviRes = await getNdviPoint(getNdviPointParam)
    if (getNdviRes.message !== 'success') {
        ElMessage.error('计算失败，请重试')
        console.error(getNdviRes)
        return
    }

    calTask.value.taskId = getNdviRes.data

    // 1、启动进度条
    // showProgress.value = true
    // progressControl()

    // 2、轮询运行状态，直到运行完成
    // ✅ 轮询函数，直到 data === 'COMPLETE'
    const pollStatus = async (taskId: string) => {
        const interval = 1000 // 每秒轮询一次
        return new Promise<void>((resolve, reject) => {
            const timer = setInterval(async () => {
                try {
                    const res = await getCaseStatus(taskId)
                    console.log('轮询结果:', res)

                    if (res?.data === 'COMPLETE') {
                        clearInterval(timer)
                        resolve()
                    } else if (res?.data === 'ERROR') {
                        console.log(res, res.data, 15616);

                        clearInterval(timer)
                        reject(new Error('任务失败'))
                    }
                } catch (err) {
                    clearInterval(timer)
                    reject(err)
                }
            }, interval)
        })
    }

    try {
        await pollStatus(calTask.value.taskId)
        // ✅ 成功后设置状态
        calTask.value.calState = 'success'
        let res = await getCaseResult(calTask.value.taskId)
        console.log(res, '结果');
        let NDVIData = res.data.NDVI
        let xData = NDVIData.map(data => data.sceneTime)
        let yData = NDVIData.map(data => data.value)

        analysisData.value.push({
            yData,
            xData,
            type: 'line',
            analysis: "定点NDVI时序计算",
            point: [...pickedPoint.value]
        })
        console.log(analysisData.value, '结果');

        ElMessage.success('NDVI计算完成')
    } catch (error) {
        calTask.value.calState = 'failed'
        ElMessage.error('NDVI计算失败，请重试')
        console.error(error);
    }

}

const chartInstances = ref<(echarts.ECharts | null)[]>([])

// 初始化图表
const initChart = (el: HTMLElement, data: any, index: number) => {
    if (!el) return
    const existingInstance = echarts.getInstanceByDom(el)
    if (existingInstance) {
        existingInstance.dispose()
    }
    let chart = echarts.init(el)
    chart.setOption({
        title: {
            text: `图表 ${index + 1}`
        },
        xAxis: {
            type: 'category',
            data: data.xData
        },
        yAxis: {
            type: 'value'
        },
        series: [
            {
                data: data.yData,
                type: data.type
            }
        ],
        dataZoom: [{  // 关键配置：时间轴滑动条
            type: 'slider',  // 滑块型
            xAxisIndex: 0,   // 控制第一个X轴
            start: 50,       // 初始显示范围的起始百分比（50%）
            end: 100         // 初始显示范围的结束百分比（100%）
        }],
        tooltip: {
            trigger: 'axis'
        },
        responsive: true
    })
    chart.resize()
    chartInstances.value[index] = chart
}

// 设置 ref 并初始化图表
const setChartRef = (el: Element | ComponentPublicInstance | null, index: number) => {
    if (el instanceof HTMLElement) {
        nextTick(() => {

            initChart(el, analysisData.value[index], index)
        })
    }
}

// 全屏查看功能
const fullscreenChart = (index: number) => {
    const dom = document.getElementById(`chart-${index}`)
    if (dom?.requestFullscreen) {
        dom.requestFullscreen()
    } else if ((dom as any).webkitRequestFullScreen) {
        (dom as any).webkitRequestFullScreen()
    } else if ((dom as any).mozRequestFullScreen) {
        (dom as any).mozRequestFullScreen()
    } else if ((dom as any).msRequestFullscreen) {
        (dom as any).msRequestFullscreen()
    }
}

// 图表自适应
window.addEventListener('resize', () => {
    chartInstances.value.forEach(chart => {
        if (chart) chart.resize()
    })
})

// 响应式监听 drawData 变化并重新渲染
watch(analysisData, (newData) => {
    nextTick(() => {
        newData.forEach((item, index) => {
            const el = document.getElementById(`chart-${index}`)
            if (el) {
                initChart(el, item, index)
            }
        })
    })
}, { deep: true })

const showImageBBox = async () => {
    let getDescriptionRes = await getBoundaryBySceneId(selectedSceneId.value)
    const FeatureCollectionBoundary: GeoJSON.FeatureCollection = {
        type: "FeatureCollection",
        features: [getDescriptionRes]
    }
    try {
        MapOperation.map_addPolygonLayer({
            geoJson: FeatureCollectionBoundary,
            id: 'UniqueSceneLayer',
            lineColor: '#8fffff',
            fillColor: '#a4ffff',
            fillOpacity: 0.2,
        })
        ElMessage.success('已加影像边界，请在影像与行政区的交集内选点。')
    } catch (e) {
        console.error("有错误找后端", e)
        ElMessage.error('加载影像边界失败。')
    }
}
watch(() => props.thematicConfig.regionId, initNdviRatePanel)

const markerRef = ref<mapboxgl.Marker | null>(null);
const createMarker = ({ lng, lat }) => {

    mapManager.withMap((map) => {
        if (markerRef.value) {
            markerRef.value.remove(); // 移除之前的标记
        }
        markerRef.value = new mapboxgl.Marker() // 创建一个新的标记
            .setLngLat([lng, lat]) // 设置标记的位置
            .addTo(map); // 将标记添加到地图上
    })
}
onMounted(async () => {
    await initNdviRatePanel()
    initMVTLayers(allOptions);
    bus.on('point-finished', createMarker);
})

onUnmounted(() => {
    gridStore.clearPicked()
    bus.off('point-finished', createMarker);
    if (markerRef.value) markerRef.value.remove()
    mapManager.withMap((map) => {
        allOptions.forEach(({ type }) => {
            const layerId = `${type}-layer`;
            const sourceId = `${type}-source`;

            if (map.getLayer(layerId)) {
                map.removeLayer(layerId);
            }

            if (map.getSource(sourceId)) {
                map.removeSource(sourceId);
            }
        });
    });
})
</script>
<!-- src="../tabStyle.css" -->
<style scoped>
:deep(.el-select__wrapper) {
    background-color: transparent;
    box-shadow: 0 0 0 1px rgba(36, 118, 153, 0.3) inset;
}

:deep(.el-select-dropdown__item) {
    background-color: rgba(67, 130, 57, 0.95);
    border-color: #2c504c;
    color: #38bdf8;
}

:deep(.el-select-dropdown.is-multiple .el-select-dropdown__item.is-selected:after) {
    background: white;
}
</style>
<style src="../tabStyle.css"></style>