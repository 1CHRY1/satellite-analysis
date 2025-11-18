import { useEffect, useState } from "react";
import type { CommonResponse } from "~/apis/https/common.type";
import { getMethodById } from "~/apis/https/task/task.admin";
import type { Method } from "~/apis/https/task/task.type";

/**
 * 1、设立全局方法缓存
 * 2、全局pending
 */
const methodCache = new Map<number, Method>();
const pendingPromises = new Map<number, Promise<CommonResponse<Method>>>();

export const useMethod = (id: number) => {
	const [method, setMethod] = useState<Method>();
	const [loading, setLoading] = useState<boolean>(true);

	useEffect(() => {
		// 返回清理函数或undefined
		// 1. 检查是否有缓存
		if (methodCache.get(id)) {
			setMethod(methodCache.get(id));
			setLoading(false);
		} else {
			// 2. 检查是否有进行中的请求
			let promise;
			if (pendingPromises.get(id)) {
				promise = pendingPromises.get(id);
			} else {
				// 3. 请求
				promise = getMethodById(id);
				pendingPromises.set(id, promise);
			}
			promise?.then(({ data }: { data: Method }) => {
				setMethod(data);
                methodCache.set(id, data);
				setLoading(false);
				pendingPromises.delete(id);
			});
		}
	}, [id]);
	return { method, loading };
};
