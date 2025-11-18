import http from "~/apis/clients/adminClient";
import { clientV3 as httpClient } from "~/apis/clients/client";
import type { CommonResponse } from "../common.type";
import type {
	CaseIds,
	MethLibTaskPageResponse,
	Method,
	TaskPageRequest,
	TaskPageResponse,
} from "./task.type";

export async function getTaskPage(
	taskInfo: TaskPageRequest,
): Promise<CommonResponse<TaskPageResponse>> {
	return http.post<CommonResponse<TaskPageResponse>>(`case/page`, taskInfo);
}

export async function getMethLibTaskPage(
	taskInfo: TaskPageRequest,
): Promise<CommonResponse<MethLibTaskPageResponse>> {
	return http.post<CommonResponse<MethLibTaskPageResponse>>(
		`case/methlib/page`,
		taskInfo,
	);
}

export async function batchDelTask(
	caseIds: CaseIds,
): Promise<CommonResponse<any>> {
	return http.delete<CommonResponse<any>>(`case/delete`, { data: caseIds });
}

export async function batchDelMethLibTask(
	caseIds: CaseIds,
): Promise<CommonResponse<any>> {
	return http.delete<CommonResponse<any>>(`case/methlib/delete`, {
		data: caseIds,
	});
}

export async function getMethodById(
	id: number,
): Promise<CommonResponse<Method>> {
	return httpClient.get<CommonResponse<Method>>(`methlib/${id}`);
}
