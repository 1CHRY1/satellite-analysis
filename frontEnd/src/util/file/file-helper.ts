// utils/file-helper.ts
import type { RawFileNode, FileTreeData } from '@/type/file' // 假设类型定义在这个路径

/**
 * 格式化文件大小 (B -> KB/MB/GB)
 */
export function formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 根据文件/文件夹名称获取对应的 Antdv 图标类型
 */
function getIconType(name: string, isDir: boolean): string {
    if (isDir) {
        return 'folder-filled'
    }

    const extension = name.split('.').pop()?.toLowerCase() ?? ''
    switch (extension) {
        case 'jpg':
        case 'jpeg':
        case 'png':
        case 'gif':
            return 'file-image-filled' // 图片
        case 'mp4':
        case 'mov':
        case 'avi':
            return 'file-video-filled' // 视频
        case 'tif':
        case 'tiff':
            return 'file-unknown-filled' // TIFF 等专业格式
        case 'pdf':
            return 'file-pdf-filled'
        case 'doc':
        case 'docx':
            return 'file-word-filled'
        case 'xls':
        case 'xlsx':
            return 'file-excel-filled'
        case 'zip':
        case 'rar':
            return 'file-zip-filled'
        default:
            return 'file-text-filled'
    }
}

/**
 * 递归转换文件数据为 Antdv Tree 结构
 */
function convertSingleNode(fileNode: RawFileNode): FileTreeData {
    const isDir = fileNode.dir
    const iconType = getIconType(fileNode.name, isDir)
    const sizeText = formatFileSize(fileNode.size)

    const treeNode: FileTreeData | any = {
        ...fileNode,
        key: fileNode.path,
        title: fileNode.name,
        isLeaf: !isDir,
        iconType: iconType,
        sizeText: sizeText,
    }

    if (isDir && fileNode.children) {
        // 递归调用自身处理子节点
        treeNode.children = fileNode.children.map((child) => convertSingleNode(child))
    } else if (isDir && !fileNode.children) {
        treeNode.children = []
    }

    return treeNode
}
export function convertToFileTree(fileNodes: RawFileNode[]): FileTreeData[] {
    // 简单地对数组中的每个根节点调用单节点转换函数
    return fileNodes.map(convertSingleNode)
}
