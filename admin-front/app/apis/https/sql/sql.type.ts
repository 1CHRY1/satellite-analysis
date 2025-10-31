export interface DBInfo {
	Identity: number;
	Name: string;
	DbType: string;
	DriverClassName: string;
	URL: string;
	UserName: string;
	FilterClassNames: string[];

	WaitThreadCount: number;
	NotEmptyWaitCount: number;
	NotEmptyWaitMillis: number;

	PoolingCount: number;
	PoolingPeak: number;
	PoolingPeakTime: string; // ISO 时间字符串

	ActiveCount: number;
	ActivePeak: number;
	ActivePeakTime: string | null;

	InitialSize: number;
	MinIdle: number;
	MaxActive: number;

	QueryTimeout: number;
	TransactionQueryTimeout: number;
	LoginTimeout: number;

	ValidConnectionCheckerClassName: string;
	ExceptionSorterClassName: string;

	TestOnBorrow: boolean;
	TestOnReturn: boolean;
	TestWhileIdle: boolean;
	DefaultAutoCommit: boolean;
	DefaultReadOnly: boolean | null;
	DefaultTransactionIsolation: number | null;

	LogicConnectCount: number;
	LogicCloseCount: number;
	LogicConnectErrorCount: number;
	PhysicalConnectCount: number;
	PhysicalCloseCount: number;
	PhysicalConnectErrorCount: number;

	DiscardCount: number;
	ExecuteCount: number;
	ExecuteUpdateCount: number;
	ExecuteQueryCount: number;
	ExecuteBatchCount: number;
	ErrorCount: number;
	CommitCount: number;
	RollbackCount: number;

	PSCacheAccessCount: number;
	PSCacheHitCount: number;
	PSCacheMissCount: number;

	StartTransactionCount: number;
	TransactionHistogram: number[];
	ConnectionHoldTimeHistogram: number[];

	RemoveAbandoned: boolean;
	ClobOpenCount: number;
	BlobOpenCount: number;
	KeepAliveCheckCount: number;
	KeepAlive: boolean;
	FailFast: boolean;

	MaxWait: number;
	MaxWaitThreadCount: number;

	PoolPreparedStatements: boolean;
	MaxPoolPreparedStatementPerConnectionSize: number;

	MinEvictableIdleTimeMillis: number;
	MaxEvictableIdleTimeMillis: number;

	LogDifferentThread: boolean;
	RecycleErrorCount: number;
	PreparedStatementOpenCount: number;
	PreparedStatementClosedCount: number;

	UseUnfairLock: boolean;
	InitGlobalVariants: boolean;
	InitVariants: boolean;
}

export const DB_INFO_FIELDS = {
    Identity: {
		label: "",
		remark: "",
	},
	Name: {
		label: "",
		remark: "",
	},
	UserName: {
		label: "用户名",
		remark: "指定建立连接时使用的用户名",
	},
	URL: {
		label: "连接地址",
		remark: "JDBC连接字符串",
	},
	DbType: {
		label: "数据库类型",
		remark: "数据库类型",
	},
	DriverClassName: {
		label: "驱动类名",
		remark: "JDBC驱动的类名",
	},
	FilterClassNames: {
		label: "filter类名",
		remark: "filter的类名",
	},
	TestOnBorrow: {
		label: "获取连接时检测",
		remark: "是否在获得连接后检测其可用性",
	},
	TestWhileIdle: {
		label: "空闲时检测",
		remark: "是否在连接空闲一段时间后检测其可用性",
	},
	TestOnReturn: {
		label: "连接放回连接池时检测",
		remark: "是否在连接放回连接池后检测其可用性",
	},
	InitialSize: {
		label: "初始化连接大小",
		remark: "连接池建立时创建的初始化连接数",
	},
	MinIdle: {
		label: "最小空闲连接数",
		remark: "连接池中最小的活跃连接数",
	},
	MaxActive: {
		label: "最大连接数",
		remark: "连接池中最大的活跃连接数",
	},
	QueryTimeout: {
		label: "查询超时时间",
		remark: "查询超时时间",
	},
	TransactionQueryTimeout: {
		label: "事务查询超时时间",
		remark: "事务查询超时时间",
	},
	LoginTimeout: {
		label: "登录超时时间",
		remark: "登录超时时间",
	},
	ValidConnectionCheckerClassName: {
		label: "连接有效性检查类名",
		remark: "",
	},
	ExceptionSorterClassName: {
		label: "ExceptionSorter类名",
		remark: "",
	},
	DefaultAutoCommit: {
		label: "默认autocommit设置",
		remark: "",
	},
	DefaultReadOnly: {
		label: "默认只读设置",
		remark: "",
	},
	DefaultTransactionIsolation: {
		label: "默认事务隔离",
		remark: "",
	},
	MinEvictableIdleTimeMillis: {
		label: "MinEvictableIdleTimeMillis",
		remark: "",
	},
	MaxEvictableIdleTimeMillis: {
		label: "MaxEvictableIdleTimeMillis",
		remark: "",
	},
	KeepAlive: {
		label: "KeepAlive",
		remark: "",
	},
	FailFast: {
		label: "FailFast",
		remark: "",
	},
	PoolPreparedStatements: {
		label: "PoolPreparedStatements",
		remark: "",
	},
	MaxPoolPreparedStatementPerConnectionSize: {
		label: "MaxPoolPreparedStatementPerConnectionSize",
		remark: "",
	},
	MaxWait: {
		label: "MaxWait",
		remark: "",
	},
	MaxWaitThreadCount: {
		label: "MaxWaitThreadCount",
		remark: "",
	},
	LogDifferentThread: {
		label: "LogDifferentThread",
		remark: "",
	},
	UseUnfairLock: {
		label: "UseUnfairLock",
		remark: "",
	},
	InitGlobalVariants: {
		label: "InitGlobalVariants",
		remark: "",
	},
	InitVariants: {
		label: "InitVariants",
		remark: "",
	},
	WaitThreadCount: {
		label: "累计总次数",
		remark: "获取连接时累计等待多少次",
	},
	NotEmptyWaitCount: {
		label: "等待总时长",
		remark: "获取连接时累计等待多长时间",
	},
	NotEmptyWaitMillis: {
		label: "等待线程数量",
		remark: "当前等待获取连接的线程数",
	},
	PoolingCount: {
		label: "事务启动数",
		remark: "事务开始的个数",
	},
	PoolingPeak: {
		label: "事务时间分布",
		remark: "事务运行时间分布，分布区间为[0-1 ms, 1-10 ms, 10-100 ms, 100-1 s, 1-10 s, 10-100 s, >100 s]",
	},
	PoolingPeakTime: {
		label: "池中连接数",
		remark: "当前连接池中的数目",
	},
	ActiveCount: {
		label: "池中连接数峰值",
		remark: "连接池中数目的峰值",
	},
	ActivePeak: {
		label: "池中连接数峰值时间",
		remark: "连接池数目峰值出现的时间",
	},
	ActivePeakTime: {
		label: "活跃连接数",
		remark: "当前连接池中活跃连接数",
	},
	LogicConnectCount: {
		label: "活跃连接数峰值",
		remark: "连接池中活跃连接数峰值",
	},
	LogicCloseCount: {
		label: "活跃连接数峰值时间",
		remark: "活跃连接池数峰值出现的时间",
	},
	LogicConnectErrorCount: {
		label: "逻辑连接打开次数",
		remark: "产生的逻辑连接建立总数",
	},
	PhysicalConnectCount: {
		label: "逻辑连接关闭次数",
		remark: "产生的逻辑连接关闭总数",
	},
	PhysicalCloseCount: {
		label: "逻辑连接错误次数",
		remark: "产生的逻辑连接出错总数",
	},
	PhysicalConnectErrorCount: {
		label: "校验失败数并关闭连接数",
		remark: "校验连接失败数并关闭连接次数",
	},
	DiscardCount: {
		label: "逻辑连接回收重用次数",
		remark: "逻辑连接返回收重用次数",
	},
	ExecuteCount: {
		label: "物理连接打开次数",
		remark: "产生的物理连接建立总数",
	},
	ExecuteUpdateCount: {
		label: "物理关闭数量",
		remark: "产生的物理关闭总数",
	},
	ExecuteQueryCount: {
		label: "物理连接错误次数",
		remark: "产生的物理连接失败总数",
	},
	ExecuteBatchCount: {
		label: "执行数",
		remark: "",
	},
	ErrorCount: {
		label: "错误数",
		remark: "事务提交次数",
	},
	CommitCount: {
		label: "回滚数",
		remark: "事务回滚次数",
	},
	RollbackCount: {
		label: "真实PreparedStatement打开次数",
		remark: "真实PreparedStatement打开次数",
	},
	PSCacheAccessCount: {
		label: "真实PreparedStatement关闭次数",
		remark: "真实PreparedStatement关闭次数",
	},
	PSCacheHitCount: {
		label: "PSCache访问次数",
		remark: "PSCache访问总数",
	},
	PSCacheMissCount: {
		label: "PSCache命中次数",
		remark: "PSCache命中次数",
	},
	StartTransactionCount: {
		label: "PSCache不命中次数",
		remark: "PSCache不命中次数",
	},
	TransactionHistogram: {
		label: "连接持有时间分布",
		remark: "连接持有时间分布，分布区间为[0-1 ms, 1-10 ms, 10-100 ms, 100ms-1s, 1-10 s, 10-100 s, 100-1000 s, >1000 s]",
	},
	ConnectionHoldTimeHistogram: {
		label: "Clob打开数",
		remark: "Clob打开数",
	},
	RemoveAbandoned: {
		label: "Blob打开数",
		remark: "Blob打开数",
	},
	ClobOpenCount: {
		label: "KeepAlive检测次数",
		remark: "KeepAlive检测次数",
	},
	BlobOpenCount: {
		label: "活跃连接堆栈查看",
		remark: "StackTrace for active Connection. [View JSON API]",
	},
	KeepAliveCheckCount: {
		label: "连接池中连接信息",
		remark: "Info for polling connection. [View JSON API]",
	},
	RecycleErrorCount: {
		label: "sql列表",
		remark: "Info for SQL. [View JSON API]",
	},
	PreparedStatementOpenCount: {
		label: "",
		remark: "",
	},
	PreparedStatementClosedCount: {
		label: "",
		remark: "",
	},
} as const;
