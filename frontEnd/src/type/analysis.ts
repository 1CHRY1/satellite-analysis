/**
 * analysis页面的对象、数组类型
 * 写ts是真勾麻烦啊
 */

export type dockerData = {
    fileName: string
    filePath: string
    fileSize: number
    fileType: string
    serverPath: string
    updateTime: string
    view: boolean
}

export type analysisResponse = {
    status: number
    info: string
    projectId: string
}

export type project = {
    createTime: string
    createUser: string
    createUserEmail: string
    createUserName: string
    description: string
    environment: string
    joinedUsers: Array<string>
    packages: string
    projectName: string
    projectId: string
}

export type newProject = {
    projectName: string
    environment: string
    keywords: string
    description: string
    authority: string
}
