export interface OverviewData {
	overall: {
		data: number; // 数据量
		user: number; // 用户数
		task: number; // 任务数
	};
	data: {
		satelliteList: DatasetItem[];
		vectorList: DatasetItem[];
		themeList: DatasetItem[];
	};
	task: {
		total: number;
		completed: number;
		error: number;
		running: number;
	};
	storage: {
		totalSpace: number;
		availSpace: number;
		usedSpace: number;
		bucketsInfo: BucketInfo[];
	};
}

// 通用数据项
export interface DatasetItem {
	key: string;
	label: string;
	value: number;
}

// 存储桶信息
export interface BucketInfo {
	bucketName: string;
	bucketUsedSize: number;
	objectsCount: number;
}

export interface ServerData {
	cpuInfo: CpuInfo;
	memoryInfo: MemoryInfo;
	diskInfo: DiskInfo[];
	networkInfo: NetworkInfo[];
}

export interface CpuInfo {
	cpu: string;
	physicalProcessorCount: number;
	logicalProcessorCount: number;
	cpuUsage: number; // 百分比 0~1
}

export interface MemoryInfo {
	totalMemory: number; // 单位字节
	availableMemory: number; // 单位字节
	usedMemoryPercent: number; // 百分比 0~100
}

export interface DiskInfo {
	name: string;
	type: string;
	totalSpaceGB: number;
	usableSpaceGB: number;
	usagePercent: number; // 百分比 0~100
}

export interface NetworkInfo {
	name: string;
	displayName: string;
	ipAddress: string;
	macAddress: string;
	speedMbps: number;
	rxBytes: number;
	txBytes: number;
	rxKbps: number;
	txKbps: number;
}
