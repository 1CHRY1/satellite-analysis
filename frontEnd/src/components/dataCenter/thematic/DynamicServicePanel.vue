<template>
    <section class="panel-section">
        <div class="section-header">
            <div class="section-subtitle flex items-center gap-2 mb-4 mt-4 pb-2 border-b border-[#247699]">
                <CommandIcon :size="18" />
                <h2 class="section-title text-lg font-medium text-[#38bdf8] mt-2 ml-2">
                    {{ toolMeta.name }}
                </h2>
            </div>
        </div>

        <div class="section-content text-gray-200">
            <p class="mb-4 text-sm leading-6 text-gray-300" v-if="toolMeta.description">
                {{ toolMeta.description }}
            </p>
            <el-form
                label-position="top"
                :model="formModel"
                class="config-container"
            >
                <div
                    v-for="param in toolMeta.paramsSchema"
                    :key="param.key"
                    class="config-item mb-4 border border-[#247699] rounded p-3"
                    style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);"
                >
                    <label class="block mb-2 text-sm font-medium text-gray-100">
                        {{ param.label }}
                        <span v-if="param.required" class="text-red-400 ml-1">*</span>
                    </label>
                    <p v-if="param.description" class="mb-2 text-xs text-gray-300">
                        {{ param.description }}
                    </p>
                    <el-input
                        v-if="param.type === 'string'"
                        v-model="formModel[param.key]"
                        :placeholder="param.placeholder ?? ''"
                        clearable
                    />
                    <el-input-number
                        v-else-if="param.type === 'number'"
                        v-model="formModel[param.key]"
                        :placeholder="param.placeholder ?? ''"
                        class="w-full"
                    />
                    <el-select
                        v-else-if="param.type === 'select'"
                        v-model="formModel[param.key]"
                        class="w-full"
                        filterable
                        clearable
                        :placeholder="param.placeholder ?? ''"
                    >
                        <el-option
                            v-for="option in getOptionsForParam(param)"
                            :key="`${param.key}-${option.value}`"
                            :label="option.label"
                            :value="option.value"
                        />
                    </el-select>
                    <el-switch
                        v-else-if="param.type === 'boolean'"
                        v-model="formModel[param.key]"
                        active-color="#1677ff"
                        inactive-color="#475569"
                    />
                </div>
            </el-form>

            <el-alert
                v-if="!hasMosaicContext"
                type="warning"
                show-icon
                class="mb-4"
                title="请先从前序数据选择影像结果"
            />

            <div class="flex gap-3 mt-6">
                <el-button
                    type="primary"
                    :loading="loading"
                    :disabled="!hasMosaicContext"
                    @click="runTool"
                >
                    {{ loading ? '运行中...' : '开始运行' }}
                </el-button>
                <el-button @click="resetForm" :disabled="loading">
                    重置
                </el-button>
            </div>
        </div>
    </section>
</template>

<script setup lang="ts">
import { computed, reactive, watch, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CommandIcon } from 'lucide-vue-next'
import * as MapOperation from '@/util/map/operation'
import { getMosaicJsonUrl, getNoCloudUrl4MosaicJson } from '@/api/http/satellite-data/visualize.api'
import { useAnalysisStore } from '@/store'
import type {
    DynamicToolMeta,
    DynamicToolParamSchema,
} from '@/store/toolRegistry'

const props = defineProps<{
    thematicConfig: Record<string, any>
    toolMeta: DynamicToolMeta
}>()

const analysisStore = useAnalysisStore()
const loading = ref(false)
const formModel = reactive<Record<string, any>>({})

const bandOptions = computed(() => {
    const raw = analysisStore.bandList
    if (!raw) return []
    return raw
        .replace(/[\[\]\s]/g, '')
        .split(',')
        .filter(Boolean)
        .map((band) => ({ label: band, value: band }))
})

const hasMosaicContext = computed(() => !!mosaicUrl.value)

const defaultPayloadTemplate = computed(() => ({
    mosaicUrl: '{{mosaicUrl}}',
    params: '{{params}}',
}))

const mosaicUrl = computed(() => {
    if (analysisStore.mosaicBucket && analysisStore.mosaicPath) {
        return getMosaicJsonUrl({
            mosaicJsonPath: `${analysisStore.mosaicBucket}/${analysisStore.mosaicPath}`,
        })
    }
    const dataset = props.thematicConfig?.dataset
    if (dataset?.result?.bucket && dataset.result.object_path) {
        return getMosaicJsonUrl({
            mosaicJsonPath: `${dataset.result.bucket}/${dataset.result.object_path}`,
        })
    }
    if (dataset?.bucket && dataset?.object_path) {
        return getMosaicJsonUrl({
            mosaicJsonPath: `${dataset.bucket}/${dataset.object_path}`,
        })
    }
    return ''
})

const resetForm = () => {
    Object.keys(formModel).forEach((key) => {
        delete formModel[key]
    })
    props.toolMeta.paramsSchema.forEach((param) => {
        formModel[param.key] =
            param.default ??
            (param.type === 'number'
                ? null
                : param.type === 'boolean'
                ? false
                : '')
    })
}

watch(
    () => props.toolMeta,
    () => {
        resetForm()
    },
    { immediate: true }
)

const getOptionsForParam = (param: DynamicToolParamSchema) => {
    if (param.source === 'bands') {
        return bandOptions.value
    }
    return param.options ?? []
}

const validateRequired = () => {
    const missing = props.toolMeta.paramsSchema
        .filter((param) => param.required)
        .filter((param) => isEmptyValue(formModel[param.key], param.type))

    if (missing.length > 0) {
        ElMessage.warning(`请完善必填项：${missing.map((item) => item.label).join('、')}`)
        return false
    }
    return true
}

const isEmptyValue = (value: unknown, type: DynamicToolParamSchema['type']) => {
    if (type === 'boolean') return false
    if (value === 0) return false
    if (value === null || value === undefined) return true
    if (typeof value === 'string') return value.trim() === ''
    return false
}

const resolveTemplate = (template: unknown, context: Record<string, any>): any => {
    if (template === null || template === undefined) return template
    if (typeof template === 'string') {
        return template.replace(/{{\s*([^{}]+)\s*}}/g, (_, key) => {
            const value = getValueByPath(context, key.trim())
            return value === undefined || value === null ? '' : String(value)
        })
    }
    if (Array.isArray(template)) {
        return template.map((item) => resolveTemplate(item, context))
    }
    if (typeof template === 'object') {
        const result: Record<string, any> = {}
        Object.entries(template as Record<string, any>).forEach(([key, value]) => {
            result[key] = resolveTemplate(value, context)
        })
        return result
    }
    return template
}

const getValueByPath = (context: Record<string, any>, path: string) => {
    if (!path) return undefined
    const segments = path.split('.')
    let cursor: any = context
    for (const segment of segments) {
        if (cursor === null || cursor === undefined) return undefined
        cursor = cursor[segment]
    }
    return cursor
}

const pickResponseData = (data: any) => {
    const { responsePath } = props.toolMeta.invoke
    if (!responsePath) return data
    return getValueByPath({ data }, `data.${responsePath}`)
}

const ensureRasterLayerCleared = () => {
    MapOperation.map_destroyNoCloudLayer()
}

const addGeoJsonLayer = (featureCollection: any) => {
    const layerId = `Dynamic-Tool-${props.toolMeta.id}`
    MapOperation.map_addPolygonLayer({
        geoJson: featureCollection,
        id: layerId,
        fillColor: '#38bdf8',
        fillOpacity: 0.2,
        lineColor: '#38bdf8',
    })
}

const applyTileLayer = (tileTemplate: string) => {
    ensureRasterLayerCleared()
    MapOperation.map_addNoCloudLayer(tileTemplate)
}

const applyMosaicLayer = (bucket: string, objectPath: string) => {
    const tileTemplate = getNoCloudUrl4MosaicJson({
        mosaicJsonPath: `${bucket}/${objectPath}`,
    })
    applyTileLayer(tileTemplate)
}

const runTilerExpression = (context: Record<string, any>) => {
    const { expressionTemplate, colorMap, pixelMethod } = props.toolMeta.invoke
    const expression = resolveTemplate(expressionTemplate ?? '', context)?.toString().trim()
    if (!expression) {
        throw new Error('表达式配置为空，请检查工具设置')
    }
    const color = resolveTemplate(colorMap ?? 'rdylgn', context)
    const pixel = (pixelMethod ?? 'first').toString()
    const url = `/tiler/mosaic/analysis/{z}/{x}/{y}.png?mosaic_url=${encodeURIComponent(
        context.mosaicUrl
    )}&expression=${encodeURIComponent(expression)}&pixel_method=${pixel}&color=${encodeURIComponent(
        color
    )}`
    applyTileLayer(url)
}

const callHttpService = async (context: Record<string, any>) => {
    const { endpoint, method = 'POST', headers = {}, payloadTemplate } = props.toolMeta.invoke
    if (!endpoint) {
        throw new Error('工具未配置服务端点')
    }
    const payload = resolveTemplate(
        payloadTemplate ?? defaultPayloadTemplate.value,
        context
    )

    const fetchInit: RequestInit = {
        method,
        headers: {
            'Content-Type': 'application/json',
            ...headers,
        },
    }

    if (method.toUpperCase() === 'GET') {
        const query = new URLSearchParams(
            Object.entries(payload ?? {}).reduce<Record<string, string>>((acc, [key, value]) => {
                acc[key] = typeof value === 'object' ? JSON.stringify(value) : String(value ?? '')
                return acc
            }, {})
        )
        return fetch(`${endpoint}?${query.toString()}`, fetchInit)
    }

    fetchInit.body = JSON.stringify(payload ?? {})
    return fetch(endpoint, fetchInit)
}

const runTool = async () => {
    if (!validateRequired()) return
    const mosaic = mosaicUrl.value
    if (!mosaic) {
        ElMessage.warning('未获取到影像数据，请先选择“前序数据”')
        return
    }
    const context = {
        ...formModel,
        mosaicUrl: mosaic,
        params: { ...formModel },
    }
    loading.value = true
    try {
        switch (props.toolMeta.invoke.type) {
            case 'tiler-expression':
                runTilerExpression(context)
                ElMessage.success('工具运行成功，已叠加分析图层')
                break
            case 'http+tile': {
                const response = await callHttpService(context)
                if (!response.ok) {
                    throw new Error(`服务请求失败：${response.status}`)
                }
                const data = await response.json()
                const payload = pickResponseData(data)
                const tileTemplate =
                    payload?.tileTemplate ??
                    payload?.tile_template ??
                    data.tileTemplate ??
                    data.tile_template
                if (!tileTemplate) {
                    throw new Error('服务返回结果缺少 tileTemplate 字段')
                }
                applyTileLayer(tileTemplate)
                ElMessage.success('工具运行成功，已叠加分析图层')
                break
            }
            case 'http+mosaic': {
                const response = await callHttpService(context)
                if (!response.ok) {
                    throw new Error(`服务请求失败：${response.status}`)
                }
                const data = await response.json()
                const payload = pickResponseData(data) ?? data
                const bucket =
                    payload?.bucket ?? payload?.result?.bucket ?? payload?.data?.bucket
                const objectPath =
                    payload?.object_path ??
                    payload?.objectPath ??
                    payload?.result?.object_path ??
                    payload?.data?.object_path
                if (!bucket || !objectPath) {
                    throw new Error('服务返回结果缺少 bucket 或 object_path 信息')
                }
                applyMosaicLayer(bucket, objectPath)
                ElMessage.success('工具运行成功，已叠加分析图层')
                break
            }
            case 'http+geojson': {
                const response = await callHttpService(context)
                if (!response.ok) {
                    throw new Error(`服务请求失败：${response.status}`)
                }
                const data = await response.json()
                const payload = pickResponseData(data) ?? data
                const featureCollection =
                    payload?.featureCollection ??
                    payload?.features ??
                    payload?.geojson ??
                    payload

                if (!featureCollection) {
                    throw new Error('服务返回结果缺少 GeoJSON 数据')
                }
                addGeoJsonLayer(featureCollection)
                ElMessage.success('工具运行成功，已加载矢量结果')
                break
            }
            default:
                throw new Error(`暂不支持的工具类型：${props.toolMeta.invoke.type}`)
        }
    } catch (error: any) {
        console.error('运行动态工具失败:', error)
        ElMessage.error(error?.message ?? '工具运行失败')
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
</style>
