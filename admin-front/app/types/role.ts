export interface Role {
	roleId: number;
	name: string;
	description: string,
    maxCpu: number,
    maxStorage: number,
    maxJob: number,
	isSuperAdmin: number;
}
