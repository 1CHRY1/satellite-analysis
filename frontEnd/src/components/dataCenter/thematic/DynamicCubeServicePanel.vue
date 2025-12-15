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

            <el-alert
                v-if="!hasCubeContext"
                type="warning"
                show-icon
                class="mb-4"
                title="请先在 Cube 面板中选择或合成立方体"
            />
            <div
                v-else
                class="mb-4 rounded border border-[#247699] bg-[#021525] p-3 text-xs text-gray-300"
            >
                <p class="m-0">
                    当前使用立方体：
                    <strong>{{ selectedCube?.cubeId }}</strong>
                </p>
                <p class="m-0 mt-1 text-[11px] text-gray-400">
                    CacheKey: {{ selectedCube?.cacheKey }} · 影像 {{ selectedCube?.dimensionScenes?.length ?? 0 }} 景 · 传感器
                    {{ selectedCube?.dimensionSensors?.length ?? 0 }}
                </p>
            </div>

            <!-- 场景选择（Cube 特有） -->
            <div v-if="hasCubeContext && sceneOptions.length > 0" class="config-item mb-4 border border-[#247699] rounded p-3"
                style="background: radial-gradient(50% 337.6% at 50% 50%, #065e96 0%, #0a456a94 97%);">
                <label class="block mb-2 text-sm font-medium text-gray-100">
                    选择场景
                    <span class="text-gray-400 text-xs ml-2">(共 {{ sceneOptions.length }} 景)</span>
                </label>
                <el-select
                    v-model="selectedSceneId"
                    class="w-full"
                    filterable
                    clearable
                    placeholder="选择要分析的场景（默认使用第一景）"
                >
                    <el-option
                        v-for="scene in sceneOptions"
                        :key="scene.value"
                        :label="scene.label"
                        :value="scene.value"
                    />
                </el-select>
                <p class="mt-2 text-xs text-gray-400" v-if="currentScene">
                    已选: {{ currentScene.sensorName }} - {{ currentScene.sceneTime?.split('T')[0] }}
                </p>
            </div>
            
            <el-form label-position="top" :model="formModel" class="config-container">
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

            <div class="flex gap-3 mt-6">
                <el-button
                    type="primary"
                    :loading="loading"
                    :disabled="!hasCubeContext"
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
import { withDefaults, defineProps, reactive, watch, ref, computed } from 'vue'
import { CommandIcon } from 'lucide-vue-next'
import * as MapOperation from '@/util/map/operation'
import type { DynamicToolMeta, DynamicToolParamSchema } from '@/store/toolRegistry'
import type { CubeDisplayItem } from '@/api/http/analytics-display/cube.type'
import { message } from 'ant-design-vue'

const props = withDefaults(
    defineProps<{
        toolMeta: DynamicToolMeta
        selectedCubes?: CubeDisplayItem[]
    }>(),
    {
        selectedCubes: () => [],
    },
)

const selectedCube = computed(() => props.selectedCubes.find((cube) => cube.isSelect) ?? props.selectedCubes[0] ?? null)
const hasCubeContext = computed(() => !!selectedCube.value)
const loading = ref(false)
const formModel = reactive<Record<string, any>>({})
const selectedSceneId = ref<string>('')

// 场景选项列表
const sceneOptions = computed(() => {
    const cube = selectedCube.value
    if (!cube || !cube.dimensionScenes) return []
    return cube.dimensionScenes.map((scene: any, index: number) => ({
        value: scene.sceneId || `scene-${index}`,
        label: `${scene.sensorName || '未知传感器'} - ${scene.sceneTime?.split('T')[0] || '未知日期'} (${index + 1}/${cube.dimensionScenes.length})`,
        scene,
    }))
})

// 当前选中的场景
const currentScene = computed(() => {
    if (!selectedSceneId.value) {
        // 默认使用第一个场景
        return selectedCube.value?.dimensionScenes?.[0] ?? null
    }
    const found = sceneOptions.value.find(opt => opt.value === selectedSceneId.value)
    return found?.scene ?? null
})

// 从场景中获取 MosaicJSON 需要的 COG 文件列表
const getSceneCogUrls = (scene: any): string[] => {
    if (!scene || !scene.images) return []
    return scene.images.map((img: any) => {
        // 构建 MinIO COG 访问 URL
        const bucket = img.bucket || 'test-images'
        const tifPath = img.tifPath || ''
        // 假设 MinIO 端点可通过环境配置获取
        return `/minio/${bucket}/${tifPath}`
    })
}

const defaultPayloadTemplate = computed(() => ({
    cube: '{{cube}}',
    cubeKey: '{{cubeKey}}',
    scene: '{{scene}}',
    params: '{{params}}',
}))

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
    selectedSceneId.value = ''
}

watch(
    () => props.toolMeta,
    () => {
        resetForm()
    },
    { immediate: true },
)

// 当选中的 Cube 变化时，重置场景选择
watch(
    () => selectedCube.value?.cacheKey,
    () => {
        selectedSceneId.value = ''
    },
)

const getOptionsForParam = (param: DynamicToolParamSchema) => {
    return param.options ?? []
}

const validateRequired = () => {
    const missing = props.toolMeta.paramsSchema
        .filter((param) => param.required)
        .filter((param) => isEmptyValue(formModel[param.key], param.type))

    if (missing.length > 0) {
        message.warning(`请完善必填项：${missing.map((item) => item.label).join('、')}`)
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
        const fullMatch = template.match(/^{{\s*([^{}]+)\s*}}$/)
        if (fullMatch) {
            const directValue = getValueByPath(context, fullMatch[1].trim())
            return directValue === undefined || directValue === null ? '' : directValue
        }
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
    const layerId = `Dynamic-Cube-Tool-${props.toolMeta.id}`
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

/**
 * Cube 级 tiler-expression 模式：
 * 需要先从 Cube 的场景中生成 MosaicJSON，然后调用分析瓦片接口
 */
const runCubeTilerExpression = async (context: Record<string, any>) => {
    const { expressionTemplate, colorMap, pixelMethod } = props.toolMeta.invoke
    const scene = currentScene.value

    if (!scene) {
        throw new Error('未选择有效场景')
    }

    // 获取表达式参数
    const formExpr = (formModel['expression'] !== undefined && String(formModel['expression']).trim()) || ''
    const formColor = (formModel['color'] !== undefined && String(formModel['color']).trim()) || ''
    const formPixel = (formModel['pixel_method'] !== undefined && String(formModel['pixel_method']).trim()) || ''

    const exprSchema = props.toolMeta.paramsSchema.find(p => p.key === 'expression')
    const exprDefault = exprSchema && (exprSchema as any).default ? String((exprSchema as any).default) : ''
    const colorSchema = props.toolMeta.paramsSchema.find(p => p.key === 'color')
    const colorDefault = colorSchema && (colorSchema as any).default ? String((colorSchema as any).default) : ''
    const pixelSchema = props.toolMeta.paramsSchema.find(p => p.key === 'pixel_method')
    const pixelDefault = pixelSchema && (pixelSchema as any).default ? String((pixelSchema as any).default) : ''

    let expression = formExpr || exprDefault
    if (!expression) {
        const resolved = resolveTemplate(expressionTemplate ?? '', context)
        expression = (resolved && String(resolved).trim()) || ''
    }
    if (!expression) {
        if (exprSchema?.required) {
            throw new Error('表达式为空，请在表单中填写')
        }
        expression = '2*b2-b1-b3'
        message.info('未填写表达式，已使用默认表达式 2*b2-b1-b3')
    }

    const color = formColor || colorDefault || (resolveTemplate(colorMap ?? 'rdylgn', context) as string)
    const pixel = formPixel || pixelDefault || ((pixelMethod ?? 'first').toString())

    // 从场景中提取 COG 文件列表
    const cogFiles = getSceneCogUrls(scene)
    if (cogFiles.length === 0) {
        throw new Error('场景中没有可用的影像文件')
    }

    // 先创建 MosaicJSON
    message.info('正在为场景创建 MosaicJSON...')
    const createMosaicResponse = await fetch('/tiler/mosaic/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            files: cogFiles,
            minzoom: 4,
            maxzoom: 20,
        }),
    })

    if (!createMosaicResponse.ok) {
        throw new Error(`创建 MosaicJSON 失败: ${createMosaicResponse.status}`)
    }

    const mosaicResult = await createMosaicResponse.json()
    const mosaicJsonPath = `${mosaicResult.bucket}/${mosaicResult.object_path}`
    // 构建 MosaicJSON 完整 URL
    const mosaicUrl = `/minio/${mosaicJsonPath}`

    // 构建分析瓦片 URL
    const url = `/tiler/mosaic/analysis/{z}/{x}/{y}.png?mosaic_url=${encodeURIComponent(
        mosaicUrl
    )}&expression=${encodeURIComponent(expression)}&pixel_method=${pixel}&color=${encodeURIComponent(color)}`

    applyTileLayer(url)
}

const callHttpService = async (context: Record<string, any>) => {
    const { endpoint, method = 'POST', headers = {}, payloadTemplate } = props.toolMeta.invoke
    if (!endpoint) {
        throw new Error('工具未配置服务端点')
    }
    const payload = resolveTemplate(payloadTemplate ?? defaultPayloadTemplate.value, context)

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
            }, {}),
        )
        return fetch(`${endpoint}?${query.toString()}`, fetchInit)
    }

    fetchInit.body = JSON.stringify(payload ?? {})
    return fetch(endpoint, fetchInit)
}

const runTool = async () => {
    if (!validateRequired()) return
    const cube = selectedCube.value
    if (!cube) {
        message.warning('请先在左侧选择一个 Cube')
        return
    }
    const scene = currentScene.value
    const context = {
        ...formModel,
        cube,
        cubeKey: cube.cacheKey,
        cubeList: props.selectedCubes,
        scene,
        sceneId: scene?.sceneId,
        params: { ...formModel },
    }
    loading.value = true
    try {
        switch (props.toolMeta.invoke.type) {
            case 'tiler-expression': {
                await runCubeTilerExpression(context)
                message.success('工具运行成功，已叠加分析图层')
                break
            }
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
                message.success('工具运行成功，已叠加分析图层')
                break
            }
            case 'http+geojson': {
                const response = await callHttpService(context)
                if (!response.ok) {
                    throw new Error(`服务请求失败：${response.status}`)
                }
                const data = await response.json()
                const payload = pickResponseData(data) ?? data
                let featureCollection: any = null
                if (payload && payload.type === 'FeatureCollection' && Array.isArray(payload.features)) {
                    featureCollection = payload
                } else if (payload?.featureCollection) {
                    featureCollection = payload.featureCollection
                } else if (payload?.geojson) {
                    featureCollection = payload.geojson
                } else if (Array.isArray(payload?.features)) {
                    featureCollection = { type: 'FeatureCollection', features: payload.features }
                } else if (payload) {
                    featureCollection = payload
                }
                if (!featureCollection) {
                    throw new Error('服务返回结果缺少 GeoJSON 数据')
                }
                addGeoJsonLayer(featureCollection)
                message.success('工具运行成功，已加载矢量结果')
                break
            }
            default:
                throw new Error(`暂不支持的工具类型：${props.toolMeta.invoke.type}`)
        }
    } catch (error: any) {
        console.error('运行 Cube 动态工具失败:', error)
        message.error(error?.message ?? '工具运行失败')
    } finally {
        loading.value = false
    }
}
</script>
