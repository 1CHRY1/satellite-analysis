export interface Task {
	caseId: string;
	address: string;
	resolution: string;
	sceneList: string[];
	dataSet: string;
	status: "ERROR" | "SUCCESS" | "PENDING" | string; // 可根据实际枚举补充
	result: any; // 如果后续有明确结构可以再细化
	createTime: string; // 或 Date
	regionId: number;
	userId: string;
	bandList: string; // 因为原本是字符串，可以提前 JSON.parse
}
