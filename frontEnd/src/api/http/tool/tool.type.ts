export interface ToolParameter {
  Name?; string;
  Flags?: string;
  Type?: 'String' | 'Number' | 'Boolean';
  Description?: string;
  default_value?: any;
  Optional?: boolean; // 
}

export interface ToolData {
  toolId: string;
  projectId?: string;
  environment?: string;
  userId: string;
  toolName: string;
  description: string;
  tags?: string[];
  category: string;
  parameters: ToolParameter[];
  outputType: string[]
  createTime?: string;
}

export interface ToolResponse{
    status:number
    message:string
    date: any
    toolId:string
}

export interface PaginatedToolResponse {
  status: number;
  message: string;
  data: {
    records: ToolData[];
    total: number;
    size: number;
    current: number;
    pages: number;
  };
}