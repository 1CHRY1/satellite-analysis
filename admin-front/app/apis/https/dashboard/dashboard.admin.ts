import http from "~/apis/clients/adminClient";
import type { CommonResponse } from "../common.type";
import type { OverviewData, ServerData } from "./dashboard.type";

export async function getStats(): Promise<CommonResponse<OverviewData>> {
	return http.get<CommonResponse<OverviewData>>(`dashboard/stats`);
}

export async function getActivity(): Promise<CommonResponse<ServerData>> {
	return http.get<CommonResponse<ServerData>>(`dashboard/activity`);
}
