import React, { useContext, useEffect, useReducer } from "react";
import { UserContext } from "./context";
import { userReducer } from "./reducer";
import type { UserState } from "~/types/user";

const initialState: UserState = {
	authenticated: !!localStorage.getItem("userId"),
	user: (() => {
		try {
			const storedUser = localStorage.getItem("user");
			if (storedUser) return JSON.parse(storedUser);
		} catch (e) {
			console.error("Failed to parse user:", e);
		}
		return {
			id: localStorage.getItem("userId") || "???",
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
		};
	})(),
};

export const useUserContext = () => {
	const context = useContext(UserContext);
	if (!context)
		throw new Error("useUserContext must be used within UserProvider");
	return context;
};

export const UserProvider: React.FC<{ children: React.ReactNode }> = ({
	children,
}) => {
	const [state, dispatch] = useReducer(userReducer, initialState);

	// ✅ 副作用：同步 localStorage
	useEffect(() => {
		if (state.authenticated) {
			localStorage.setItem("user", JSON.stringify(state.user));
			localStorage.setItem("userId", state.user.id);
		} else {
			localStorage.removeItem("user");
			localStorage.removeItem("userId");
		}
	}, [state]);

	return (
		<UserContext.Provider value={{ state, dispatch }}>
			{children}
		</UserContext.Provider>
	);
};
