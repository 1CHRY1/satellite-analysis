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
		label: "Identity",
		remark: "",
	},
	Name: {
		label: "名称",
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
		label: "等待线程数量",
		remark: "当前等待获取连接的线程数",
	},
	NotEmptyWaitCount: {
		label: "累计总次数",
		remark: "获取连接时累计等待多少次",
	},
	NotEmptyWaitMillis: {
		label: "等待总时长",
		remark: "获取连接时累计等待多长时间",
	},
	PoolingCount: {
		label: "池中连接数",
		remark: "当前连接池中的数目",
	},
	PoolingPeak: {
		label: "池中连接数峰值",
		remark: "连接池中数目的峰值",
	},
	PoolingPeakTime: {
		label: "池中连接数峰值时间",
		remark: "连接池数目峰值出现的时间",
	},
	ActiveCount: {
		label: "活跃连接数",
		remark: "当前连接池中活跃连接数",
	},
	ActivePeak: {
		label: "活跃连接数峰值",
		remark: "连接池中活跃连接数峰值",
	},
	ActivePeakTime: {
		label: "活跃连接数峰值时间",
		remark: "活跃连接池峰值出现的时间",
	},
	LogicConnectCount: {
		label: "逻辑连接打开次数",
		remark: "产生的逻辑连接建立总数",
	},
	LogicCloseCount: {
		label: "逻辑连接关闭次数",
		remark: "产生的逻辑连接关闭总数",
	},
	LogicConnectErrorCount: {
		label: "逻辑连接错误次数",
		remark: "产生的逻辑连接出错总数",
	},
	PhysicalConnectCount: {
		label: "物理连接打开次数",
		remark: "产生的物理连接建立总数",
	},
	PhysicalCloseCount: {
		label: "物理关闭数量",
		remark: "产生的物理关闭总数",
	},
	PhysicalConnectErrorCount: {
		label: "物理连接错误次数",
		remark: "产生的物理连接失败总数",
	},
	DiscardCount: {
		label: "校验失败废弃连接数",
		remark: "校验连接失败丢弃连接次数",
	},
	ExecuteCount: {
		label: "执行数",
		remark: "",
	},
	ExecuteUpdateCount: {
		label: "ExecuteUpdateCount",
		remark: "",
	},
	ExecuteQueryCount: {
		label: "ExecuteQueryCount",
		remark: "",
	},
	ExecuteBatchCount: {
		label: "ExecuteBatchCount",
		remark: "",
	},
	ErrorCount: {
		label: "错误数",
		remark: "",
	},
	CommitCount: {
		label: "提交数",
		remark: "事务提交次数",
	},
	RollbackCount: {
		label: "回滚数",
		remark: "事务回滚次数",
	},
	PSCacheAccessCount: {
		label: "PSCache访问次数",
		remark: "PSCache访问次数",
	},
	PSCacheHitCount: {
		label: "PSCache命中次数",
		remark: "PSCache命中次数",
	},
	PSCacheMissCount: {
		label: "PSCache不命中次数",
		remark: "PSCache不命中次数",
	},
	StartTransactionCount: {
		label: "事务启动数",
		remark: "事务开始的个数",
	},
	TransactionHistogram: {
		label: "事务时间分布",
		remark: "事务运行时间分布，分布区间为[0-1 ms, 1-10 ms, 10-100 ms, 100-1 s, 1-10 s, 10-100 s, >100 s]",
	},
	ConnectionHoldTimeHistogram: {
		label: "连接持有时间分布",
		remark: "连接持有时间分布，分布区间为[0-1 ms, 1-10 ms, 10-100 ms, 100ms-1s, 1-10 s, 10-100 s, 100-1000 s, >1000 s]",
	},
	// RemoveAbandoned: {
	// 	label: "Blob打开数",
	// 	remark: "Blob打开数",
	// },
	ClobOpenCount: {
		label: "Clob打开次数",
		remark: "Clob打开次数",
	},
	BlobOpenCount: {
		label: "Blob打开次数",
		remark: "Blob打开次数",
	},
	KeepAliveCheckCount: {
		label: "KeepAlive检测次数",
		remark: "KeepAlive检测次数",
	},
	RecycleErrorCount: {
		label: "逻辑连接回收重用次数",
		remark: "逻辑连接回收重用次数",
	},
	PreparedStatementOpenCount: {
		label: "真实PreparedStatement打开次数",
		remark: "真实PreparedStatement打开次数",
	},
	PreparedStatementClosedCount: {
		label: "真实PreparedStatement关闭次数",
		remark: "真实PreparedStatement关闭次数",
	},
} as const;

export interface SqlStatInfo {
	ExecuteAndResultSetHoldTime: number;
	EffectedRowCountHistogram: number[];
	LastErrorMessage: string | null;
	Histogram: number[];
	InputStreamOpenCount: number;
	BatchSizeTotal: number;
	FetchRowCountMax: number;
	ErrorCount: number;
	BatchSizeMax: number;
	URL: string | null;
	Name: string;
	LastErrorTime: string | null;
	ReaderOpenCount: number;
	EffectedRowCountMax: number;
	LastErrorClass: string | null;
	InTransactionCount: number;
	LastErrorStackTrace: string | null;
	ResultSetHoldTime: number;
	TotalTime: number;
	ID: number;
	ConcurrentMax: number;
	RunningCount: number;
	FetchRowCount: number;
	MaxTimespanOccurTime: string | null;
	LastSlowParameters: string | null;
	ReadBytesLength: number;
	DbType: string;
	DataSource: string | null;
	SQL: string;
	HASH: number;
	LastError: string | null;
	MaxTimespan: number;
	BlobOpenCount: number;
	ExecuteCount: number;
	EffectedRowCount: number;
	ReadStringLength: number;
	ExecuteAndResultHoldTimeHistogram: number[];
	File: string | null;
	ClobOpenCount: number;
	LastTime: string | null;
	FetchRowCountHistogram: number[];
}

/** 单个函数的统计信息 */
export interface FunctionStat {
	name: string;
	invokeCount: number;
}

/** 表操作统计 */
export interface TableStat {
	name: string;
	selectCount: number;
	selectIntoCount: number;
	insertCount: number;
	updateCount: number;
	deleteCount: number;
	truncateCount: number;
	createCount: number;
	alterCount: number;
	dropCount: number;
	replaceCount: number;
	deleteDataCount: number;
	updateHistogram: number[];
	updateDataCount: number;
	fetchRowCount: number;
	fetchRowCountHistogram: number[];
}

/** 白名单 SQL 统计 */
export interface WhiteListItem {
	sql: string;
	executeCount: number;
	fetchRowCount: number;
	updateCount: number;
	sample: string;
	executeErrorCount: number;
}

/** 黑名单 SQL 统计（与白名单类似，但多 violationMessage，少 executeErrorCount） */
export interface BlackListItem {
	sql: string;
	executeCount: number;
	fetchRowCount: number;
	updateCount: number;
	sample: string;
	violationMessage: string;
}

/** 顶层监控统计结构 */
export interface WallInfo {
	checkCount: number;
	hardCheckCount: number;
	violationCount: number;
	violationEffectRowCount: number;
	blackListHitCount: number;
	blackListSize: number;
	whiteListHitCount: number;
	whiteListSize: number;
	syntaxErrorCount: number;

	tables: TableStat[];
	functions: FunctionStat[] | null;
	blackList: BlackListItem[];
	whiteList: WhiteListItem[];
}

export interface WebAppStat {
	ContextPath: string;
	RunningCount: number;
	ConcurrentMax: number;
	RequestCount: number;
	SessionCount: number;
	JdbcCommitCount: number;
	JdbcRollbackCount: number;
	JdbcExecuteCount: number;
	JdbcExecuteTimeMillis: number;
	JdbcFetchRowCount: number;
	JdbcUpdateCount: number;

	// OS 统计
	OSMacOSXCount: number;
	OSWindowsCount: number;
	OSLinuxCount: number;
	OSSymbianCount: number;
	OSFreeBSDCount: number;
	OSOpenBSDCount: number;
	OSAndroidCount: number;
	OSWindows98Count: number;
	OSWindowsXPCount: number;
	OSWindows2000Count: number;
	OSWindowsVistaCount: number;
	OSWindows7Count: number;
	OSWindows8Count: number;

	// Android 版本统计
	OSAndroid15Count: number;
	OSAndroid16Count: number;
	OSAndroid20Count: number;
	OSAndroid21Count: number;
	OSAndroid22Count: number;
	OSAndroid23Count: number;
	OSAndroid30Count: number;
	OSAndroid31Count: number;
	OSAndroid32Count: number;
	OSAndroid40Count: number;
	OSAndroid41Count: number;
	OSAndroid42Count: number;
	OSAndroid43Count: number;
	OSLinuxUbuntuCount: number;

	// 浏览器统计
	BrowserIECount: number;
	BrowserFirefoxCount: number;
	BrowserChromeCount: number;
	BrowserSafariCount: number;
	BrowserOperaCount: number;
	BrowserIE5Count: number;
	BrowserIE6Count: number;
	BrowserIE7Count: number;
	BrowserIE8Count: number;
	BrowserIE9Count: number;
	BrowserIE10Count: number;
	Browser360SECount: number;

	// 设备统计
	DeviceAndroidCount: number;
	DeviceIpadCount: number;
	DeviceIphoneCount: number;
	DeviceWindowsPhoneCount: number;

	// 爬虫统计
	BotCount: number;
	BotBaiduCount: number;
	BotYoudaoCount: number;
	BotGoogleCount: number;
	BotMsnCount: number;
	BotBingCount: number;
	BotSosoCount: number;
	BotSogouCount: number;
	BotYahooCount: number;
}

export const WEBAPP_INFO_FIELDS = {
	ContextPath: "",
	RunningCount: "执行中",
	ConcurrentMax: "最大并发",
	RequestCount: "请求次数",
	JdbcCommitCount: "事务提交数",
	JdbcRollbackCount: "事务回滚数",
	JdbcExecuteCount: "Jdbc执行数",
	JdbcExecuteTimeMillis: "Jdbc时间",
	JdbcFetchRowCount: "Jdbc读取行数",
	JdbcUpdateCount: "Jdbc更新行数",
	SessionCount: "",
	OSMacOSXCount: "",
	OSWindowsCount: "",
	OSLinuxCount: "",
	OSSymbianCount: "",
	OSFreeBSDCount: "",
	OSOpenBSDCount: "",
	OSAndroidCount: "",
	OSWindows98Count: "",
	OSWindowsXPCount: "",
	OSWindows2000Count: "",
	OSWindowsVistaCount: "",
	OSWindows7Count: "",
	OSWindows8Count: "",
	OSAndroid15Count: "",
	OSAndroid16Count: "",
	OSAndroid20Count: "",
	OSAndroid21Count: "",
	OSAndroid22Count: "",
	OSAndroid23Count: "",
	OSAndroid30Count: "",
	OSAndroid31Count: "",
	OSAndroid32Count: "",
	OSAndroid40Count: "",
	OSAndroid41Count: "",
	OSAndroid42Count: "",
	OSAndroid43Count: "",
	OSLinuxUbuntuCount: "",
	BrowserIECount: "",
	BrowserFirefoxCount: "",
	BrowserChromeCount: "",
	BrowserSafariCount: "",
	BrowserOperaCount: "",
	BrowserIE5Count: "",
	BrowserIE6Count: "",
	BrowserIE7Count: "",
	BrowserIE8Count: "",
	BrowserIE9Count: "",
	BrowserIE10Count: "",
	Browser360SECount: "",
	DeviceAndroidCount: "",
	DeviceIpadCount: "",
	DeviceIphoneCount: "",
	DeviceWindowsPhoneCount: "",
	BotCount: "",
	BotBaiduCount: "",
	BotYoudaoCount: "",
	BotGoogleCount: "",
	BotMsnCount: "",
	BotBingCount: "",
	BotSosoCount: "",
	BotSogouCount: "",
	BotYahooCount: "",
} as const;

export interface WebURIStat {
	URI: string;
	RunningCount: number;
	ConcurrentMax: number;
	RequestCount: number;
	RequestTimeMillis: number;
	ErrorCount: number;
	LastAccessTime: string;

	JdbcCommitCount: number;
	JdbcRollbackCount: number;
	JdbcExecuteCount: number;
	JdbcExecuteErrorCount: number;
	JdbcExecutePeak: number;
	JdbcExecuteTimeMillis: number;
	JdbcFetchRowCount: number;
	JdbcFetchRowPeak: number;
	JdbcUpdateCount: number;
	JdbcUpdatePeak: number;
	JdbcPoolConnectionOpenCount: number;
	JdbcPoolConnectionCloseCount: number;
	JdbcResultSetOpenCount: number;
	JdbcResultSetCloseCount: number;

	Histogram: number[];
	Profiles: any[]; // 若有具体结构，可再细化
	RequestTimeMillisMax: number;
	RequestTimeMillisMaxOccurTime: string;
}

export interface WebSessionStat {
    SESSIONID: string;
    Principal: string | null; // 当前会话的用户主体，可能为空
    RunningCount: number;
    ConcurrentMax: number;
    RequestCount: number;
    RequestTimeMillisTotal: number;
  
    CreateTime: string;
    LastAccessTime: string;
    RemoteAddress: string;
  
    JdbcCommitCount: number;
    JdbcRollbackCount: number;
    JdbcExecuteCount: number;
    JdbcExecuteTimeMillis: number;
    JdbcFetchRowCount: number;
    JdbcUpdateCount: number;
  
    UserAgent: string;
    RequestInterval: number[];
  }
  
