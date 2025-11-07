import http from "~/apis/clients/client";
import type { Region } from "./region.type";

// 获取指定 level 的行政区（省/市/区）
export async function getRegionsByLevel(
	level: string,
): Promise<Region[]> {
	const res = await http.get<Region[]>(
		`/data/region/level/${level}`,
	);
	return res; // 返回实际数组
}

// 获取指定 parent 下的子行政区
export async function getRegionsByParent(
	parentAdcode: number,
): Promise<Region[]> {
	const res = await http.get<Region[]>(
		`/data/region/parent/${parentAdcode}`,
	);
	return res;
}
