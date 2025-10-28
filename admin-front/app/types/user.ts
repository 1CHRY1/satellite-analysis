export interface User {
	userId: string;
	userName: string;
	password: string,
	phone: string;
	province: string;
	city: string;
	email: string;
	title: string;
	organization: string;
	createTime: string,
	avatarPath: string,
	introduction: string;
	roleId: number;
}

export interface UserInfo {
	name: string;
	phone: string;
	province: string;
	city: string;
	email: string;
	id: string;
	title: string;
	organization: string;
	introduction: string;
	isSuperAdmin: boolean;
	roleName: string;
	roleDesc: string;
	maxCpu: number;
	maxStorage: number;
	maxJob: number;
	roleId: number;
}

export interface UserState {
	authenticated: boolean;
	user: UserInfo;
}

export type UserAction = { type: "LOGIN"; payload: UserInfo } | { type: "LOGOUT" };
