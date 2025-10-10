import React, { useContext, useEffect, useReducer } from "react";
import { UserContext, type UserContextType } from "./context";
import { userReducer } from "./reducer";
import type { UserState } from "~/types/user";

let initialState: UserState = {
	authenticated: false,
	user: {
		id: "",
		name: "",
		phone: "",
		province: "",
		city: "",
		email: "",
		title: "",
		organization: "",
		introduction: "",
		roleId: 2,
		roleName: "",
		roleDesc: "",
		maxCpu: 2,
		maxStorage: 5,
		maxJob: 3,
		isSuperAdmin: false,
	},
};

let globalUserContextRef: UserContextType | null = null;

export const useUserContext = () => {
	const context = useContext(UserContext);
	if (!context)
		throw new Error("useUserContext must be used within UserProvider");
	return context;
};

export const getUserContext = () => {
	return globalUserContextRef;
}

export const UserProvider: React.FC<{ children: React.ReactNode }> = ({
	children,
}) => {
	const [state, dispatch] = useReducer(userReducer, initialState);

	// ✅ 副作用：同步 localStorage
	useEffect(() => {
		// 不会在 SSR 阶段运行
		// 只会在浏览器完成首次渲染后执行
		if (state.authenticated) {
			localStorage.setItem("user", JSON.stringify(state.user));
			localStorage.setItem("userId", state.user.id);
		} else {
			localStorage.removeItem("user");
			localStorage.removeItem("userId");
		}
	}, [state]); 

	// 在SSR阶段运行，没有浏览器API localStorage
	useEffect(() => {
		initialState = {
			authenticated: !!localStorage.getItem("userId"),
			user: (() => {
				try {
					const storedUser = localStorage.getItem("user");
					if (storedUser) return JSON.parse(storedUser);
				} catch (e) {
					console.error("Failed to parse user:", e);
				}
				return initialState.user;
			})(),
		};
	}); // 没有依赖参数，首次挂载时执行

	useEffect(() => {
		globalUserContextRef = { state, dispatch };
	}, []); // 如果空依赖数组，挂载和更新时执行

	return (
		<UserContext.Provider value={{ state, dispatch }}>
			{children}
		</UserContext.Provider>
	);
};
