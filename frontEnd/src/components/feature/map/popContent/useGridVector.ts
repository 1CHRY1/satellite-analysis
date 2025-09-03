import { ref } from "vue"
import type { VectorSymbology } from "@/type/interactive-explore/visualize"
import type { VectorStats } from '@/api/http/interactive-explore'
import { getGridVectorUrl } from '@/api/http/interactive-explore/visualize.api'
import * as GridExploreMapOps from '@/util/map/operation/grid-explore'
import * as InteractiveExploreMapOps from '@/util/map/operation/interactive-explore'
import { mapManager } from '@/util/map/mapManager'
import bus from '@/store/bus'
import { gridData } from "./shared"

/**
 * 2. 矢量Tab
 */
export const useGridVector = () => {
    /**
     * 矢量符号化
     */
    const gridVectorSymbology = ref<VectorSymbology>({})
    const predefineColors = ref([
        '#ff4500',
        '#ff8c00',
        '#ffd700',
        '#90ee90',
        '#00ced1',
        '#1e90ff',
        '#c71585',
        'rgba(255, 69, 0, 0.68)',
        'rgb(255, 120, 0)',
        'hsv(51, 100, 98)',
        'hsva(120, 40, 94, 0.5)',
        'hsl(181, 100%, 37%)',
        'hsla(209, 100%, 56%, 0.73)',
        '#c7158577',
    ])
    const handleCheckAllChange = (tableName: string, val: boolean) => {
        console.log('all:', val)
        const item = gridVectorSymbology.value[tableName];
        // 使用解构赋值或 Vue.set 确保响应性
        gridVectorSymbology.value[tableName] = {
            ...item,
            checkedAttrs: val ? item.attrs.map(attr => attr.label) : [],
            isIndeterminate: false
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
        time: ''
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
        for (const attr of attrList) {
            const url = getGridVectorUrl(gridData.value, tableName, attr?.type)
            console.log(url)
            console.log(tableName)
            GridExploreMapOps.map_addGridMVTLayer(tableName, url, attr?.color || '#0066cc', attr?.type)

            // 生成 baseId，要和 map_addMVTLayer 内保持一致
            const baseId = `${tableName}-${attr?.type || 0}-mvt-layer`

            InteractiveExploreMapOps.map_addMVTLayer(tableName, url, attr?.color || '#0066cc', attr?.type)
    
            mapManager.withMap((map) => {

                // console.log(map.getStyle())
                map.on('click', (e) => {
                    //注意传真正的 layer.id而不是 source_layer
                    const features = map.queryRenderedFeatures(e.point, { 
                        layers: [
                            `${baseId}-fill`, 
                            `${baseId}-line`, 
                            `${baseId}-point`
                        ]
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
                        
                        // 保留控制台输出用于调试
                        console.log('Mapbox Layer ID:', feature.layer?.id)    
                        console.log('MVT Source Layer:', feature.sourceLayer)
                        console.log('Feature properties:', properties)
                    }
                })
            })
        }
    }

    return {
        handleCheckAllChange,
        handleCheckedAttrsChange,
        previewIndex,
        handleSelectVector,
        handleVectorVisualize,
        gridVectorSymbology,
        selectedVector,
        predefineColors
    }
}