import { ref } from "vue"
import type { VectorSymbology } from "@/type/interactive-explore/visualize"
import type { VectorStats } from '@/api/http/interactive-explore'
import { getGridVectorUrl } from '@/api/http/interactive-explore/visualize.api'
import * as GridExploreMapOps from '@/util/map/operation/grid-explore'
import * as InteractiveExploreMapOps from '@/util/map/operation/interactive-explore'
import { mapManager } from '@/util/map/mapManager'
import bus from '@/store/bus'
import { ezStore } from '@/store'
import { gridData } from "./shared"
import { message } from "ant-design-vue"
import { getVectorCustomAttr } from "@/api/http/satellite-data"

/**
 * 2. 矢量Tab
 */
export const useGridVector = () => {
    /**
     * 矢量符号化
     */
    const gridVectorSymbology = ref<VectorSymbology>({})
    const predefineColors = ref([
        "rgb(255, 69, 0)",             // #ff4500
        "rgb(255, 140, 0)",            // #ff8c00
        "rgb(255, 215, 0)",            // #ffd700
        "rgb(144, 238, 144)",          // #90ee90
        "rgb(0, 206, 209)",            // #00ced1
        "rgb(30, 144, 255)",           // #1e90ff
        "rgb(199, 21, 133)",           // #c71585
        "rgba(255, 69, 0, 0.68)",      // 已经是 rgba
        "rgb(255, 120, 0)",            // 已经是 rgb
        "rgb(249, 250, 0)",            // hsv(51, 100, 98)
        "rgba(71, 240, 71, 0.5)",      // hsva(120, 40, 94, 0.5)
        "rgb(0, 189, 189)",            // hsl(181, 100%, 37%)
        "rgba(0, 115, 255, 0.73)",     // hsla(209, 100%, 56%, 0.73)
        "rgba(199, 21, 133, 0.47)"     // #c7158577
        ])

    const handleCheckAllChange = (tableName: string, val: boolean) => {
        console.log('all:', val)
        const item = gridVectorSymbology.value[tableName];
        // 使用解构赋值或 Vue.set 确保响应性
        gridVectorSymbology.value[tableName] = {
            ...item,
            checkedAttrs: val ? item.attrs.map(attr => attr.label) : [],
            isIndeterminate: false,
            isRequesting: false
        };
        console.log(gridVectorSymbology.value[tableName])
    }
    const handleCheckedAttrsChange = (tableName: string, value: string[]) => {
        console.log(value)
        const checkedCount = value.length
        const attrs = gridVectorSymbology.value[tableName].attrs
        gridVectorSymbology.value[tableName].checkAll = checkedCount === attrs.length
        gridVectorSymbology.value[tableName].isIndeterminate = checkedCount > 0 && checkedCount < attrs.length
        console.log(gridVectorSymbology.value[tableName])
    }
    const selectedVector = ref<VectorStats.Vector>({
        tableName: '',
        vectorName: '',
        time: '',
        fields: []
    })
    const previewIndex = ref<number | null>(null)
    const handleSelectVector = (index: number) => {
        previewIndex.value = previewIndex.value === index ? null : index
    }

    /**
     * 矢量可视化
     */
    const handleVectorVisualize = () => {
        const tableName = selectedVector.value.tableName
        previewIndex.value = gridData.value.vectors.findIndex((item) => item.tableName === tableName)
        const attrList = gridVectorSymbology.value[tableName].checkedAttrs.map(item => {
            const targetAttr = gridVectorSymbology.value[tableName].attrs.find(i => i.label === item)
            return targetAttr
        })
        console.log(attrList)
        let type = attrList.map((attr) => attr?.type) as number[]
        const url = getGridVectorUrl(gridData.value, tableName, gridVectorSymbology.value[tableName].selectedField, type)
        console.log(url)
        console.log(tableName)
        GridExploreMapOps.map_addGridMVTLayer(tableName, url, attrList as any, gridVectorSymbology.value[tableName].selectedField, undefined, gridData.value)

        // 添加一次性的点击事件监听器
        mapManager.withMap((map) => {
            
            map.off('click', handleGridVectorClick)
            
            map.on('click', handleGridVectorClick)
        })
    }

    const getAttrs4CustomField = async (tableName: string, field: string | undefined) => {
        if (!field) {
            message.warning("请先选择字段")
            return
        }
        gridVectorSymbology.value[tableName].isRequesting = true
        const result = await getVectorCustomAttr(tableName, field)
        gridVectorSymbology.value[tableName].attrs = result.map((item, index) => ({
            type: item,
            label: item,
            color: predefineColors.value[index % predefineColors.value.length],
        }))
        gridVectorSymbology.value[tableName].checkedAttrs = result
        gridVectorSymbology.value[tableName].isRequesting = false
    }

    // 定义点击事件处理函数
    const handleGridVectorClick = (e: any) => {
        const layeridStore = ezStore.get('GridMVTLayerIds') || []
        
        mapManager.withMap((map) => {
            const features = map.queryRenderedFeatures(e.point, { 
                layers: layeridStore
            })
            if (features.length > 0) {
                const feature = features[0]
                const properties = feature.properties || {}
                
                // 通过事件总线触发弹窗显示
                bus.emit('mvt:feature:click', {
                    feature,
                    properties,
                    lngLat: e.lngLat
                })
                          
                console.log('Mapbox Layer ID:', feature.layer?.id)    
                console.log('MVT Source Layer:', feature.sourceLayer)
                console.log('Feature properties:', properties)
            }
        })
    }

    // 用于移除事件监听器
    const cleanupGridVectorEvents = () => {
        mapManager.withMap((map) => {
            map.off('click', handleGridVectorClick)
        })
    }

    return {
        handleCheckAllChange,
        handleCheckedAttrsChange,
        previewIndex,
        handleSelectVector,
        handleVectorVisualize,
        gridVectorSymbology,
        selectedVector,
        predefineColors,
        cleanupGridVectorEvents,
        getAttrs4CustomField
    }
}