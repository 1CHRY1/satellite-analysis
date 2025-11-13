// types/file.d.ts

/**
 * 后端返回的原始文件节点结构
 */
export interface RawFileNode {
    name: string
    path: string
    size: number // 单位 B
    lastModified: string // ISO 8601 格式时间
    children: RawFileNode[] | null
    dir: boolean
}

/**
 * Antdv Tree 节点所需的扩展数据结构
 */
export interface FileTreeData extends RawFileNode {
    key: string // 使用 path 作为 key
    title: string
    isLeaf: boolean
    iconType: string
    sizeText: string
    children?: FileTreeData[]
}

/**
 * 文件选择器的操作模式
 */
export type SelectorMode = 'file' | 'folder' | 'output'

/**
 * 最终确认结果的结构
 */
export interface SelectionResult {
    type: SelectorMode
    // 单选结果 (文件/文件夹)
    index: number,
    path?: string
    name?: string
    // 多选结果 (文件/文件夹数组)
    paths?: { path: string; name: string }[]

    // output 模式
    folderPath?: string
    fileName?: string
    fullPath?: string
}
