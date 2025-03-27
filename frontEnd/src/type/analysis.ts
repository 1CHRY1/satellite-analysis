/**
 * analysis页面的对象、数组类型
 * 写ts是真勾麻烦啊
 */

export type dockerData = {
    fileName: string
    fileSize: number
    fileType: string
    updateTime: string
    view: boolean
}

export type analysisResponse = {
    status: number
    info: string
    projectId: string
}
