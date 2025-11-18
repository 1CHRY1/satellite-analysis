import { useEffect, useState } from "react";
import type { CommonResponse } from "~/apis/https/common.type";
import { getUserById } from "~/apis/https/user/user.api";

interface User {
	userId: string;
	userName: string;
	email: string;
}

const userCache = new Map<string, User>();
const pendingPromises = new Map<string, Promise<CommonResponse<User>>>();

export const useUser = (id: string) => {
	const [loading, setLoading] = useState<boolean>(true);
	const [user, setUser] = useState<User>();
	useEffect(() => {
		setLoading(true);

		if (userCache.get(id)) {
			setLoading(false);
			setUser(userCache.get(id));
			return;
		}
		let promise;
		if (pendingPromises.get(id)) {
			promise = pendingPromises.get(id);
		} else {
			promise = getUserById(id);
			pendingPromises.set(id, promise);
		}
		promise?.then((res: CommonResponse<User>) => {
			const data = res.data;
			setUser(data);
			setLoading(false);
			userCache.set(id, data);
			pendingPromises.delete(id);
		});
	}, [id]);
	return {
		loading,
		user,
	};
};
