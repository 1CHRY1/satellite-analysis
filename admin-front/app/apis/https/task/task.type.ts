import type { PageRequest, PageResponse } from "../common.type";
export type ModelStatus = "COMPLETE" | "RUNNING" | "PENDING" | "NONE" | "ERROR";

export interface Task {
	caseId: string;
	address: string;
	resolution: string;
	sceneList: string[];
	dataSet: string;
	status: ModelStatus;
	result: any; // 如果后续有明确结构可以再细化
	createTime: string; // 或 Date
	userId: string;
	regionId: number;
	bandList: string; // 因为原本是字符串，可以提前 JSON.parse
}

export interface MethLibTask {
	caseId: string;
	params: any;
	methodId: number;
	status: ModelStatus;
	userId: string;
	result: any; // 如果后续有明确结构可以再细化
	createTime: string; // 或 Date
}

export interface TaskPageRequest extends PageRequest {
	regionId?: number | null;
	// address: string,
	startTime?: string;
	endTime?: string;
	resolution?: number;
	status?: ModelStatus;
}

export type TaskPageResponse = PageResponse<Task>;
export type MethLibTaskPageResponse = PageResponse<MethLibTask>

export interface CaseIds {
	caseIds: string[];
}

export interface MethodParam {
    Name: string;
    Type: 'DataInput' | 'DataOutput' | 'ParamInput';
    Flags: string[];
    Optional: boolean;
    Description: string;
    default_value: string | null;
    parameter_type: any; 
}

export interface Method {
    id: number;
    name: string;
    description: string;
    longDesc: string;
    copyright: string;
    uuid: string;
    type: string;
    params: MethodParam[];
    execution: string;
    createTime: string;
}
