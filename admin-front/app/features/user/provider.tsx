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
};

export const UserProvider: React.FC<{ children: React.ReactNode }> = ({
	children,
}) => {
	const [state, dispatch] = useReducer(userReducer, initialState, (init) => {
		// 读取 localStorage 初始化 state（LAZY INITIALIZER）
		try {
			const storedUser = localStorage.getItem("user");
			const userId = localStorage.getItem("userId");
			if (userId && storedUser) {
				return {
					authenticated: true,
					user: JSON.parse(storedUser),
				};
			}
		} catch (e) {
			console.error(e);
		}
		return init; // 默认 state
	});

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

	useEffect(() => {
		globalUserContextRef = { state, dispatch };
	}, [state, dispatch]);

	return (
		<UserContext.Provider value={{ state, dispatch }}>
			{children}
		</UserContext.Provider>
	);
};
