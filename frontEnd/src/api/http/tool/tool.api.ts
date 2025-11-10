import DateBody from 'ant-design-vue/es/vc-picker/panels/DatePanel/DateBody'
import http from '../../axiosClient/clientHttp'
import type {
  ToolParameter,
  ToolData,
  ToolResponse,
  PaginatedToolResponse
} from './tool.type'


// 发布工具（与后端 /api/v1/tools/publish 对齐，传入 Code2ToolDTO 形状的 body）
export async function publishTool(body: any): Promise<ToolResponse> {
    return http.post<ToolResponse>('/tools/publish', body)
}
// 停止工具服务（ToolBasicDTO：{ userId, toolId }）
export async function unpublishTool(body: { userId: string; toolId: string }): Promise<ToolResponse> {
    return http.post<ToolResponse>('/tools/unpublish', body)
}

// 查询工具服务状态（ToolBasicDTO：{ userId, toolId }）
export async function getToolStatus(body: { userId: string; toolId: string }): Promise<ToolResponse> {
    return http.post<ToolResponse>('/tools/status', body)
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

