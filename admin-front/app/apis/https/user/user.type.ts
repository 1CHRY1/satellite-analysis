export interface UserInfo {
    userId: string;
	userName: string;
    password: string;
	phone: string;
	province: string;
	city: string;
	organization: string;
	introduction: string;
	create_time: string;
	title: string;
	role: string;
	email: string;
	avataPath: string;
	roleId: number;
}

export interface UserPageRequest {
    page: number,
    pageSize: number,
    asc?: boolean,
    searchText?: string,
    sortField?: string,
    roleIds?: number[]
}

export interface UserPageResponse {
    records: UserInfo[]
    total: number,
    size: number,
    current: number
}

export interface UserIds {
    userIds: number[]
}