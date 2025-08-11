import { computed, ref } from "vue"
import { gridData } from "./shared"
import type { MultiImageInfoType } from "@/type/interactive-explore/grid"
import type { Grid } from "@/api/http/interactive-explore/grid.type"
import bus from "@/store/bus"
import { scaleRate } from "./useGridScene"

/**
 * 一些其他功能可能用到的公共变量
 */
export const selectedProductType = ref('')
export const selectedProduct = ref('')

/**
 * 3. 栅格专题Tab
 */
export const useGridTheme = () => {

    /**
     * 产品类型选项
     */
    const productTypes = computed(() => {
        if (!gridData.value.themeRes?.dataset)
            return []
        const result = new Set<string>()
        result.add('全选')
        for (const category of gridData.value.themeRes.category) {
            result.add(gridData.value.themeRes.dataset[category].label)
        }
        return Array.from(result)
    })

    const products = computed(() => {
        let result = new Set<string>()
        if (!gridData.value.themeRes?.dataset)
            return []
        result.add('全选')
        for (const category of gridData.value.themeRes.category) {
            for (const theme of gridData.value.themeRes.dataset[category].dataList) {
                if (selectedProductType.value != '全选') {
                    let dataType = gridData.value.themeRes.dataset[category].label
                    if (dataType === selectedProductType.value) {
                        result.add(theme.sensorName)
                    }
                } else if (selectedProductType.value === '全选') {
                    result.add(theme.sensorName)
                }
            }
        }
        return Array.from(result)
    })

    /**
     * 透明度变量
     */
    const opacity = ref(0)
    const opacityFormatter = (value: number) => {
        return `${value}%`
    }
    const onAfterOpacityChange = (opacity: number) => {
        console.log(opacity)
    }

    /**
     * 栅格专题数据可视化
     */
    const handleProductVisualize = () => {
        const gridInfo = gridData.value
        gridInfo.opacity = opacity.value
        const imageData: MultiImageInfoType[] = []
        const gridAllThemes: Grid.ThemeDetail[] = []
        if (!gridData.value.themeRes.dataset) {
            return
        }
    
        // 所有的它都想看
        if (selectedProduct.value === '全选') {
            // 所有专题、所有产品
            if (selectedProductType.value === '全选') {
                for (const category of gridData.value.themeRes.category) {
                    for (const theme of gridData.value.themeRes.dataset[category].dataList) {
                        theme.dataType = category
                        gridAllThemes.push(theme)
                    }
                }
            } else {
                // 指定专题、所有产品
                for (const category of gridData.value.themeRes.category) {
                    for (const theme of gridData.value.themeRes.dataset[category].dataList) {
                        theme.dataType = category
                        if (gridData.value.themeRes.dataset[category].label === selectedProductType.value) {
                            gridAllThemes.push(theme)
                        }
                    }
                }
            }
        } else {
            // 所有专题、指定产品
            if (selectedProductType.value === '全选') {
                for (const category of gridData.value.themeRes.category) {
                    for (const theme of gridData.value.themeRes.dataset[category].dataList) {
                        theme.dataType = category
                        if (theme.sensorName === selectedProduct.value) {
                            gridAllThemes.push(theme)
                        }
                    }
                }
            } else {
                // 指定专题、指定产品
                for (const category of gridData.value.themeRes.category) {
                    for (const theme of gridData.value.themeRes.dataset[category].dataList) {
                        theme.dataType = category
                        if (theme.sensorName === selectedProduct.value) {
                            gridAllThemes.push(theme)
                        }
                    }
                }
            }
        }
        // Process each band (R, G, B)
        for (let themeInfo of gridAllThemes) {
            let redPath = ''
            let greenPath = ''
            let bluePath = ''
    
            // 这里用bandmapper, 确保bandMapper！！！！！
            // console.log(sceneInfo.bandMapper)
            for (let bandImg of themeInfo.images) {
                // 后端返回的BandMapper如果是单波段的话，Red Green 和 Blue相同
                if (themeInfo.bandMapper.Red == bandImg.band) {
                    redPath = bandImg.bucket + '/' + bandImg.tifPath
                }
                if (themeInfo.bandMapper.Green == bandImg.band) {
                    greenPath = bandImg.bucket + '/' + bandImg.tifPath
                }
                if (themeInfo.bandMapper.Blue == bandImg.band) {
                    bluePath = bandImg.bucket + '/' + bandImg.tifPath
                }
            }
            imageData.push({
                sceneId: themeInfo.sceneId,
                sensorName: themeInfo.sensorName,
                productName: themeInfo.sensorName + '-' + themeInfo.productName,
                dataType: themeInfo.dataType,
                time: themeInfo.sceneTime,
                redPath: redPath,
                greenPath: greenPath,
                bluePath: bluePath,
                nodata: themeInfo.noData,
            })
        }
        bus.emit('cubeVisualize', imageData, gridInfo, scaleRate.value, 'product')
    
        bus.emit('openTimeline')
    }
    
    return {
        handleProductVisualize,
        opacityFormatter,
        onAfterOpacityChange,
        opacity,
        selectedProductType,
        selectedProduct,
        productTypes,
        products
    }
}