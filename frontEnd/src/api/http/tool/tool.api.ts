import DateBody from 'ant-design-vue/es/vc-picker/panels/DatePanel/DateBody'
import http from '../../axiosClient/clientHttp'
import type {
  ToolParameter,
  ToolData,
  ToolResponse,
  PaginatedToolResponse
} from './tool.type'


export async function publishTool(
    projectId: string,
    environment: string,
    userId: string,
    toolData: any
): Promise<ToolResponse> {
    return http.post<ToolResponse>('/tools/publish', {
        projectId,
        environment,
        userId,
        ...toolData
    })
}
//更新工具
export async function  updateTool(
    toolId: string,
    updateData: any
): Promise<ToolResponse> {
    return http.post<ToolResponse>('/tools/update', {
        toolId,
        ...updateData
    })
}

export async function deleteTool(toolId: string): Promise<ToolResponse> {
    return http.delete<ToolResponse>(`/tools/delete/${toolId}`)
}

export async function  getToolById(toolId: string): Promise<ToolData> {
    return http.get(`/tools/${toolId}`)
  }

// 获取所有工具信息

export async function getAllTools(body:any): Promise<any> {
  return http.post('/tools/all', body
  )
}

