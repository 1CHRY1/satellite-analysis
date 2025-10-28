import { defineStore } from 'pinia'
import { computed, reactive } from 'vue'

export type DynamicToolInvokeType =
    | 'tiler-expression'
    | 'http+tile'
    | 'http+mosaic'
    | 'http+geojson'

export type DynamicToolResultType = 'tile' | 'mosaic' | 'geojson'

export type DynamicToolParamType = 'string' | 'number' | 'select' | 'boolean'

export interface DynamicToolParamOption {
    label: string
    value: string | number | boolean
}

export interface DynamicToolParamSchema {
    key: string
    label: string
    type: DynamicToolParamType
    required?: boolean
    placeholder?: string
    description?: string
    default?: string | number | boolean
    options?: DynamicToolParamOption[]
    /**
     * Optional dynamic source hint (e.g. "bands" to auto-populate with current mosaic band names)
     */
    source?: 'bands'
}

export interface DynamicToolInvokeConfig {
    type: DynamicToolInvokeType
    /**
     * Used for tiler-expression tools. Supports template placeholders {{paramKey}}.
     */
    expressionTemplate?: string
    colorMap?: string
    pixelMethod?: string
    /**
     * Used for HTTP based tools.
     */
    endpoint?: string
    method?: 'GET' | 'POST'
    headers?: Record<string, string>
    payloadTemplate?: unknown
    responsePath?: string
}

export interface DynamicToolMeta {
    id: string
    name: string
    category: string
    description?: string
    tags?: string[]
    paramsSchema: DynamicToolParamSchema[]
    invoke: DynamicToolInvokeConfig
    resultType: DynamicToolResultType
    createdAt: string
    updatedAt: string
}

interface ToolRegistryState {
    tools: DynamicToolMeta[]
    currentUserId: string | null
    isInitialized: boolean
}

const STORAGE_KEY = 'dynamic_tools_registry'
const memoryStorage = reactive(new Map<string, DynamicToolMeta[]>())

const readFromStorage = (userId: string): DynamicToolMeta[] => {
    if (!userId) return []
    try {
        if (typeof window !== 'undefined' && window.localStorage) {
            const raw = window.localStorage.getItem(`${STORAGE_KEY}:${userId}`)
            if (!raw) return []
            return JSON.parse(raw) as DynamicToolMeta[]
        }
    } catch {
        // ignore and fall back to memory storage
    }
    return memoryStorage.get(userId) ?? []
}

const writeToStorage = (userId: string, tools: DynamicToolMeta[]): void => {
    if (!userId) return
    try {
        if (typeof window !== 'undefined' && window.localStorage) {
            window.localStorage.setItem(`${STORAGE_KEY}:${userId}`, JSON.stringify(tools))
            return
        }
    } catch {
        // ignore and fall back to memory storage
    }
    memoryStorage.set(userId, tools)
}

export const generateToolId = (prefix = 'tool'): string => {
    return `${prefix}-${Math.random().toString(36).slice(2, 10)}-${Date.now()}`
}

export const useToolRegistryStore = defineStore('toolRegistry', {
    state: (): ToolRegistryState => ({
        tools: [],
        currentUserId: null,
        isInitialized: false,
    }),
    getters: {
        toolbox(state): Record<string, DynamicToolMeta[]> {
            const map: Record<string, DynamicToolMeta[]> = {}
            state.tools.forEach((tool) => {
                if (!map[tool.category]) {
                    map[tool.category] = []
                }
                map[tool.category].push(tool)
            })
            return map
        },
        getToolById: (state) => (toolId: string): DynamicToolMeta | undefined => {
            return state.tools.find((tool) => tool.id === toolId)
        },
    },
    actions: {
        ensureLoaded(userId: string): void {
            if (!userId) return
            if (this.isInitialized && this.currentUserId === userId) return
            this.currentUserId = userId
            this.tools = readFromStorage(userId)
            this.isInitialized = true
        },
        registerTool(userId: string, tool: DynamicToolMeta): void {
            if (!userId) return
            this.ensureLoaded(userId)
            const existingIndex = this.tools.findIndex((item) => item.id === tool.id)
            const timestamp = new Date().toISOString()
            const record: DynamicToolMeta = {
                ...tool,
                createdAt: tool.createdAt ?? timestamp,
                updatedAt: timestamp,
            }
            if (existingIndex >= 0) {
                this.tools.splice(existingIndex, 1, {
                    ...this.tools[existingIndex],
                    ...record,
                })
            } else {
                this.tools.push(record)
            }
            writeToStorage(userId, this.tools)
        },
        removeTool(userId: string, toolId: string): void {
            if (!userId) return
            this.ensureLoaded(userId)
            this.tools = this.tools.filter((tool) => tool.id !== toolId)
            writeToStorage(userId, this.tools)
        },
        reset(userId: string): void {
            if (!userId) return
            this.tools = []
            writeToStorage(userId, [])
        },
    },
})
