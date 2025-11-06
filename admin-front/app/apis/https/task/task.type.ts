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
	regionId: number;
	bandList: string; // 因为原本是字符串，可以提前 JSON.parse
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

export interface CaseIds {
	caseIds: string[];
}
