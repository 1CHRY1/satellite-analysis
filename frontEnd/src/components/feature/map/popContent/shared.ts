import { computed, ref } from "vue"
import type { GridData, PopupTab, SceneMethod } from '@/type/interactive-explore/grid'
import { selectedRBand, selectedGBand, selectedBBand } from "./useGridScene"
import { selectedProductType, selectedProduct } from "./useGridTheme"

/**
 * 一些PopContent各功能的公共变量
 */


/**
 * 当前tab
 */
export const activeTab = ref<PopupTab>('scene')

// 网格ID
export const gridID = computed(() => {
    const { rowId, columnId, resolution } = gridData.value
    return `${rowId}-${columnId}-${resolution}`
})

/**
 * 网格数据
 */
export const gridData = ref<GridData>({
    rowId: 0,
    columnId: 0,
    resolution: 0,
    sceneRes: {
        total: 0,
        category: [],
    },
    vectors: [],
    themeRes: {
        total: 0,
        category: [],
    },
})

/**
 * 判断是否可以可视化
 */
export const showBandSelector = ref(true)
export const canVisualize = computed(() => {
    switch (activeTab.value) {
        case 'scene':
            if (showBandSelector.value === false) return true
            if (activeMethod.value === 'rgb') {
                return !!selectedRBand.value && !!selectedGBand.value && !!selectedBBand.value
            }
            break
        case 'vector':
            // return !!selectedResolution.value
            break
        case 'theme':
            return !!selectedProductType.value && !!selectedProduct.value
            break
    }
})

// 当前遥感影像可视化方法
export const activeMethod = ref<SceneMethod>('rgb')