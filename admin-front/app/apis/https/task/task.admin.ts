import http from "~/apis/clients/adminClient";
import type { CommonResponse } from "../common.type";
import type { CaseIds, TaskPageRequest, TaskPageResponse } from "./task.type";

export async function getTaskPage(
	taskInfo: TaskPageRequest,
): Promise<CommonResponse<TaskPageResponse>> {
	return http.post<CommonResponse<TaskPageResponse>>(`case/page`, taskInfo);
}

export async function batchDelTask(
	caseIds: CaseIds,
): Promise<CommonResponse<any>> {
	return http.delete<CommonResponse<any>>(`case/delete`, { data: caseIds });
}
