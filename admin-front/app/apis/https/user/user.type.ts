import type { PageRequest, PageResponse } from "../common.type";

export interface UserInfo {
    userId: string;
	userName: string;
    password: string;
	phone?: string;
	province?: string;
	city?: string;
	organization?: string;
	introduction?: string;
	createTime?: string;
	title?: string;
	role?: string;
	email: string;
	avatarPath?: string;
	roleId: number;
}

export interface UserPageRequest extends PageRequest{
    roleIds?: number[]
}

export type UserPageResponse = PageResponse<UserInfo>

export interface UserIds {
    userIds: string[]
}