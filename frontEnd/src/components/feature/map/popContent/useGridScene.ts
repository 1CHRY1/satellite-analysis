import { computed, ref } from "vue"
import { showBandSelector, gridData } from "./shared"
import type { GridData, MultiImageInfoType, PopupTab, SceneMethod } from '@/type/interactive-explore/grid'
import type { Grid } from '@/api/http/interactive-explore/grid.type'
import bus from '@/store/bus'

/**
 * 1. 遥感影像Tab
 */

/**
 * 一些共用变量，其他功能也可能用到的
 */
export const scaleRate = ref(1.00)
export const visualLoad = ref(false)
export const selectedBand = ref('')
export const selectedRBand = ref('')
export const selectedGBand = ref('')
export const selectedBBand = ref('')
export const selectedSensor = ref('')

export const useGridScene = () => {

    /**
     * 分辨率选项
     */
    const selectedResolution = ref('')
    const resolutions = computed(() => {
        const result = new Set<string>()
        if (gridData.value.sceneRes?.dataset) {
            console.log(gridData.value, '111')
            for(const category of gridData.value.sceneRes.category) {
                
                if (gridData.value.sceneRes.dataset[category] === undefined) {
                    console.log(category, '222')
                    console.log(gridData.value.sceneRes.dataset, '222')
                    console.log(gridData.value, '222')
                }
                if (gridData.value.sceneRes.dataset[category].dataList.length > 0) {
                    result.add(gridData.value.sceneRes.dataset[category].label)
                }
            }
        }
        const arr = Array.from(result)
        arr.sort((a, b) => {
            return Number(b) - Number(a)
        })

        return ['全选', ...arr]
    })
    
    /**
     * 传感器选项
     */
    const sensors = computed(() => {
        let result = new Set<string>()
        result.add('全选')
        if (gridData.value.sceneRes?.dataset) {
            for(const category of gridData.value.sceneRes.category) {
                for(const scene of gridData.value.sceneRes.dataset[category].dataList) {
                    if (selectedResolution.value != '全选' && gridData.value.sceneRes.dataset[category].label == selectedResolution.value) {
                        result.add(scene.sensorName)
                    } else if (selectedResolution.value === '全选') {
                        result.add(scene.sensorName)
                    }
                }
            }
        }
        return Array.from(result)
    })
    const handleSensorChange = (value) => {
        if (value === '全选') {
            showBandSelector.value = false
        } else {
            showBandSelector.value = true
        }
    }

    /**
     * 波段选项
     */
    const bands = computed(() => {
        let result: string[] = []

        if (selectedSensor.value != '' && gridData.value.sceneRes?.dataset) {
            for (const category of gridData.value.sceneRes.category) {
                for (const scene of gridData.value.sceneRes.dataset[category].dataList) {
                    if (selectedSensor.value && scene.sensorName === selectedSensor.value) {
                        scene.images.forEach((bandImg) => {
                            if (!result.includes(bandImg.band.toString())) {
                                result.push(bandImg.band.toString())
                            }
                        })
                    }
                }
            }
        }

        result.sort((a, b) => {
            return parseInt(a, 10) - parseInt(b, 10)
        })

        if (result.length > 0) {
            selectedBand.value = result[0]
        }
        if (result.length > 2) {
            selectedBBand.value = result[0]
            selectedGBand.value = result[1]
            selectedRBand.value = result[2]
        }

        return result
    })
    
    /**
     * 拉伸增强选项
     */
    const stretchMethods = [
        {label: "线性拉伸", value: "linear"},
        {label: "γ 拉伸", value: "gamma"},
        {label: "标准差拉伸", value: "standard"},
    ]
    type StretchMethod = "linear" | "gamma" | "standard"
    const selectedStretchMethod = ref<StretchMethod>("gamma")
    const enableDraggable = ref(true)

    const handleStretchMethodChange = () => {
        switch (selectedStretchMethod.value) {
            case "linear":
                scaleRate.value = 0
                break
            case "gamma":
                scaleRate.value = 1
                break
            case "standard":
                scaleRate.value = 0
                break
            default:
                scaleRate.value = 1
        }
    }
    
    const scaleRateFormatter = (value: number) => {
        // return `${value}级`
        return `${value}`
    }
    
    const handleScaleMouseUp = () => (enableDraggable.value = true)
    
    const handleScaleMouseDown = () => (enableDraggable.value = false)
    
    const onAfterScaleRateChange = (scale_rate: number) => {
        console.log(scale_rate)
    }
    
    /**
     * 遥感影像可视化函数
     */
    const handleSceneVisualize = () => {
        const { rowId, columnId, resolution } = gridData.value
        const gridInfo = {
            rowId,
            columnId,
            resolution,
        }
        if (!gridData.value.sceneRes?.dataset) {
            return
        }
        // 所有的它都想看
        if (showBandSelector.value === false) {
            const rgbImageData: MultiImageInfoType[] = []
    
            const gridAllScenes:Grid.SceneDetail[] = []
            // gridData.value.scenes
            // 分辨率指定、传感器全选在上面
            if (selectedResolution.value && selectedResolution.value !== '全选') {
                for (const category of gridData.value.sceneRes.category) {
                    for (const scene of gridData.value.sceneRes.dataset[category].dataList) {
                        if (gridData.value.sceneRes.dataset[category].label === selectedResolution.value) {
                            gridAllScenes.push(scene)
                        }
                    }
                }
            } else {
                // 分辨率全选，传感器全选
                for (const category of gridData.value.sceneRes.category) {
                    for (const scene of gridData.value.sceneRes.dataset[category].dataList) {
                        gridAllScenes.push(scene)
                    }
                }
            }
    
            // Process each band (R, G, B)
            for (let sceneInfo of gridAllScenes) {
                let redPath = ''
                let greenPath = ''
                let bluePath = ''
    
                // 这里用bandmapper
                // console.log(sceneInfo.bandMapper)
                for (let bandImg of sceneInfo.images) {
                    // 后端返回的BandMapper如果是单波段的话，Red Green 和 Blue相同
                    if (sceneInfo.bandMapper.Red == bandImg.band) {
                        redPath = bandImg.bucket + '/' + bandImg.tifPath
                    }
                    if (sceneInfo.bandMapper.Green == bandImg.band) {
                        greenPath = bandImg.bucket + '/' + bandImg.tifPath
                    }
                    if (sceneInfo.bandMapper.Blue == bandImg.band) {
                        bluePath = bandImg.bucket + '/' + bandImg.tifPath
                    }
                }
                rgbImageData.push({
                    sceneId: sceneInfo.sceneId,
                    sensorName: sceneInfo.sensorName,
                    productName: sceneInfo.sensorName + '-' +sceneInfo.productName,
                    dataType: 'satellite',
                    time: sceneInfo.sceneTime,
                    redPath: redPath,
                    greenPath: greenPath,
                    bluePath: bluePath,
                    nodata: sceneInfo.noData,
                    bandMapper: sceneInfo.bandMapper,
                    images: sceneInfo.images,
                    cloudPath: sceneInfo.cloudPath && sceneInfo.bucket
                        ? `${sceneInfo.bucket}/${sceneInfo.cloudPath}`
                        : undefined
                })
            }
            
            bus.emit('cubeVisualize', rgbImageData, gridInfo, selectedStretchMethod.value, scaleRate.value, 'rgb')
        } else {
            // 针对具体传感器、具体波段组合
            const rgbImageData: MultiImageInfoType[] = []
            const filteredScene: Grid.SceneDetail[] = []
            for (const category of gridData.value.sceneRes.category) {
                for (const scene of gridData.value.sceneRes.dataset[category].dataList) {
                    // 分辨率全选，传感器全选和分辨率指定、传感器全选在上面
                    if (selectedResolution.value == '全选' && selectedSensor.value != '全选') {
                        // 分辨率全选，传感器指定
                        if (scene.sensorName === selectedSensor.value) {
                            filteredScene.push(scene)
                        }
                    } else if (selectedResolution.value != '全选' && selectedSensor.value != '全选') {
                        // 分辨率指定，传感器指定
                        if (gridData.value.sceneRes.dataset[category].label === selectedResolution.value && scene.sensorName === selectedSensor.value) {
                            filteredScene.push(scene)
                        }
                    }
                }
            }
    
            // Process each band (R, G, B)
            for (let scene of filteredScene) {
                let redPath = ''
                let greenPath = ''
                let bluePath = ''
    
                // 这里用用户选的
                scene.images.forEach((bandImg) => {
                    console.log(bandImg, 'bandImg')
                    if (bandImg.band == Number(selectedRBand.value)) {
                        redPath = bandImg.bucket + '/' + bandImg.tifPath
                    }
                    if (bandImg.band == Number(selectedGBand.value)) {
                        greenPath = bandImg.bucket + '/' + bandImg.tifPath
                    }
                    if (bandImg.band == Number(selectedBBand.value)) {
                        bluePath = bandImg.bucket + '/' + bandImg.tifPath
                    }
                })
    
                rgbImageData.push({
                    sceneId: scene.sceneId,
                    time: scene.sceneTime,
                    sensorName: scene.sensorName,
                    productName: scene.sensorName + '-' + scene.productName,
                    dataType: 'satellite',
                    redPath: redPath,
                    greenPath: greenPath,
                    bluePath: bluePath,
                    nodata: scene.noData,
                    bandMapper: scene.bandMapper,
                    images: scene.images,
                    cloudPath: scene.cloudPath && scene.bucket
                        ? `${scene.bucket}/${scene.cloudPath}`
                        : undefined
                })
            }
            bus.emit('cubeVisualize', rgbImageData, gridInfo, selectedStretchMethod.value, scaleRate.value, 'rgb')
        }
    
        bus.emit('openTimeline')
        visualLoad.value = true
    }

    return {
        handleSceneVisualize,
        handleScaleMouseDown,
        handleScaleMouseUp,
        scaleRateFormatter,
        onAfterScaleRateChange,
        handleSensorChange,
        scaleRate,
        visualLoad,
        enableDraggable,
        selectedResolution,
        resolutions,
        selectedSensor,
        sensors,
        selectedBand,
        selectedRBand,
        selectedGBand,
        selectedBBand,
        bands,
        stretchMethods,
        selectedStretchMethod,
        handleStretchMethodChange
    }
}



