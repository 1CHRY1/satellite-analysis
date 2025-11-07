import { ProCard, StatisticCard } from "@ant-design/pro-components";
import { Statistic, Progress, Table, Tag, Space, Row, Col } from "antd";
import {
	DatabaseOutlined,
	TeamOutlined,
	ProfileOutlined,
	CloudOutlined,
	CheckCircleOutlined,
	SyncOutlined,
	CloseCircleOutlined,
	RocketOutlined,
	GlobalOutlined,
	EnvironmentOutlined,
	BarChartOutlined,
	FolderOpenOutlined,
	DashboardOutlined,
	HddOutlined,
	ApiOutlined,
	ThunderboltOutlined,
	LinkOutlined,
} from "@ant-design/icons";
import { useEffect, useState } from "react";
import { getStats, getActivity } from "~/apis/https/dashboard/dashboard.admin";
import type {
	OverviewData,
	ServerData,
} from "~/apis/https/dashboard/dashboard.type";

const { Statistic: ProStatistic } = StatisticCard;

export default function Dashboard() {
	const [data, setData] = useState<OverviewData>();
	const [serverInfo, setServerInfo] = useState<ServerData>();

	useEffect(() => {
		(async () => {
			const res = await getStats();
			if (res.status === 1) setData(res.data);
		})();

		// 获取服务器信息
		const fetchServerInfo = async () => {
			const res = await getActivity();
			if (res.status === 1) setServerInfo(res.data);
		};

		fetchServerInfo();
		// 每5秒轮询一次
		const interval = setInterval(fetchServerInfo, 3000);

		return () => clearInterval(interval);
	}, []);

	if (!data)
		return (
			<div
				style={{
					display: "flex",
					justifyContent: "center",
					alignItems: "center",
					height: "100vh",
					fontSize: 16,
					color: "#999",
				}}
			>
				<SyncOutlined spin style={{ marginRight: 8 }} />
				加载中...
			</div>
		);

	const percent = (data.storage.usedSpace / data.storage.totalSpace) * 100;

	const bucketColumns = [
		{
			title: "存储桶名称",
			dataIndex: "bucketName",
			width: 200,
			render: (text: string) => (
				<Space>
					<FolderOpenOutlined style={{ color: "#1890ff" }} />
					<span style={{ fontWeight: 500 }}>{text}</span>
				</Space>
			),
		},
		{
			title: "对象数量",
			dataIndex: "objectsCount",
			width: 120,
			align: "center" as const,
			render: (val: number) => (
				<Tag color="blue">{val.toLocaleString()}</Tag>
			),
		},
		{
			title: "已用空间",
			dataIndex: "bucketUsedSize",
			align: "right" as const,
			render: (v: number) => {
				const gb = (v / 1024 / 1024 / 1024).toFixed(2);
				return <span style={{ fontWeight: 500 }}>{gb} GB</span>;
			},
		},
	];

	const getStorageColor = () => {
		if (percent < 60) return "#52c41a";
		if (percent < 80) return "#faad14";
		return "#ff4d4f";
	};

	return (
		<div style={{ padding: 24 }}>
			{/* 页面标题 */}
			<div style={{ marginBottom: 24 }}>
				<h1
					style={{
						fontSize: 24,
						fontWeight: 600,
						margin: 0,
						color: "#262626",
					}}
				>
					多源遥感应用支撑云平台总览
				</h1>
				<p
					style={{
						color: "#8c8c8c",
						margin: "8px 0 0 0",
						fontSize: 14,
					}}
				>
					实时监控系统运行状态与数据资源概况
				</p>
			</div>

			{/* 顶部核心指标卡片 */}
			<Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
				<Col xs={24} sm={8}>
					<ProCard bordered hoverable>
						<Statistic
							title={
								<span
									style={{ fontSize: 14, color: "#8c8c8c" }}
								>
									数据总量
								</span>
							}
							value={data.overall.data}
							prefix={
								<DatabaseOutlined
									style={{ color: "#1890ff", fontSize: 24 }}
								/>
							}
							valueStyle={{
								color: "#1890ff",
								fontWeight: 600,
								fontSize: 28,
							}}
							suffix="景"
						/>
						<div
							style={{
								marginTop: 16,
								paddingTop: 12,
								borderTop: "1px solid #f0f0f0",
								fontSize: 12,
								color: "#52c41a",
							}}
						>
							<CheckCircleOutlined /> 系统运行正常
						</div>
					</ProCard>
				</Col>

				<Col xs={24} sm={8}>
					<ProCard bordered hoverable>
						<Statistic
							title={
								<span
									style={{ fontSize: 14, color: "#8c8c8c" }}
								>
									注册用户
								</span>
							}
							value={data.overall.user}
							prefix={
								<TeamOutlined
									style={{ color: "#52c41a", fontSize: 24 }}
								/>
							}
							valueStyle={{
								color: "#52c41a",
								fontWeight: 600,
								fontSize: 28,
							}}
							suffix="位"
						/>
						<div
							style={{
								marginTop: 16,
								paddingTop: 12,
								borderTop: "1px solid #f0f0f0",
								fontSize: 12,
								color: "#8c8c8c",
							}}
						>
							系统已注册用户数
						</div>
					</ProCard>
				</Col>

				<Col xs={24} sm={8}>
					<ProCard bordered hoverable>
						<Statistic
							title={
								<span
									style={{ fontSize: 14, color: "#8c8c8c" }}
								>
									任务总数
								</span>
							}
							value={data.overall.task}
							prefix={
								<ProfileOutlined
									style={{ color: "#faad14", fontSize: 24 }}
								/>
							}
							valueStyle={{
								color: "#faad14",
								fontWeight: 600,
								fontSize: 28,
							}}
							suffix="个"
						/>
						<div
							style={{
								marginTop: 16,
								paddingTop: 12,
								borderTop: "1px solid #f0f0f0",
								fontSize: 12,
								color: "#8c8c8c",
							}}
						>
							系统已运行任务数
						</div>
					</ProCard>
				</Col>
			</Row>

			{/* 任务状态与存储空间 */}
			<Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
				<Col xs={24} lg={10}>
					<ProCard
						extra={
							<a href="/task" target="_blank">
								<LinkOutlined /> 查看
							</a>
						}
						title={
							<Space>
								<RocketOutlined style={{ color: "#1890ff" }} />
								<span>任务执行情况</span>
							</Space>
						}
						bordered
						style={{ height: "100%" }}
					>
						<Row gutter={16} style={{ marginBottom: 16 }}>
							<Col span={12} style={{ textAlign: "center" }}>
								<Statistic
									title="总任务数"
									value={data.task.total}
									valueStyle={{
										color: "#1890ff",
										fontSize: 32,
										fontWeight: 600,
									}}
								/>
							</Col>
							<Col span={12} style={{ textAlign: "center" }}>
								<Statistic
									title="完成率"
									value={(
										(data.task.completed /
											data.task.total) *
										100
									).toFixed(1)}
									suffix="%"
									valueStyle={{
										color: "#52c41a",
										fontSize: 32,
										fontWeight: 600,
									}}
								/>
							</Col>
						</Row>

						<div
							style={{
								marginTop: 16,
								paddingTop: 16,
								borderTop: "1px solid #f0f0f0",
							}}
						>
							<Space
								direction="vertical"
								size="middle"
								style={{ width: "100%" }}
							>
								<div
									style={{
										display: "flex",
										justifyContent: "space-between",
										alignItems: "center",
									}}
								>
									<Space>
										<CheckCircleOutlined
											style={{
												color: "#52c41a",
												fontSize: 16,
											}}
										/>
										<span>已完成</span>
									</Space>
									<Tag
										color="success"
										style={{
											minWidth: 60,
											textAlign: "center",
											fontSize: 14,
										}}
									>
										{data.task.completed}
									</Tag>
								</div>
								<div
									style={{
										display: "flex",
										justifyContent: "space-between",
										alignItems: "center",
									}}
								>
									<Space>
										<SyncOutlined
											spin
											style={{
												color: "#1890ff",
												fontSize: 16,
											}}
										/>
										<span>进行中</span>
									</Space>
									<Tag
										color="processing"
										style={{
											minWidth: 60,
											textAlign: "center",
											fontSize: 14,
										}}
									>
										{data.task.running}
									</Tag>
								</div>
								<div
									style={{
										display: "flex",
										justifyContent: "space-between",
										alignItems: "center",
									}}
								>
									<Space>
										<CloseCircleOutlined
											style={{
												color: "#ff4d4f",
												fontSize: 16,
											}}
										/>
										<span>失败</span>
									</Space>
									<Tag
										color="error"
										style={{
											minWidth: 60,
											textAlign: "center",
											fontSize: 14,
										}}
									>
										{data.task.error}
									</Tag>
								</div>
							</Space>
						</div>
					</ProCard>
				</Col>

				<Col xs={24} lg={14}>
					<ProCard
						title={
							<Space>
								<CloudOutlined style={{ color: "#1890ff" }} />
								<span>MinIO存储空间使用情况</span>
							</Space>
						}
						bordered
						style={{ height: "100%" }}
					>
						<Row gutter={24} align="middle">
							<Col
								xs={24}
								sm={10}
								style={{
									textAlign: "center",
									marginBottom: 16,
								}}
							>
								<Progress
									type="circle"
									percent={parseFloat(percent.toFixed(1))}
									strokeColor={getStorageColor()}
									width={140}
									strokeWidth={8}
									format={(percent) => (
										<div>
											<div
												style={{
													fontSize: 24,
													fontWeight: 600,
												}}
											>
												{percent}%
											</div>
											<div
												style={{
													fontSize: 12,
													color: "#8c8c8c",
													marginTop: 4,
												}}
											>
												已使用
											</div>
										</div>
									)}
								/>
							</Col>
							<Col xs={24} sm={14}>
								<Space
									direction="vertical"
									size="large"
									style={{ width: "100%" }}
								>
									<div>
										<div
											style={{
												color: "#8c8c8c",
												fontSize: 14,
												marginBottom: 8,
											}}
										>
											总容量
										</div>
										<div
											style={{
												fontSize: 24,
												fontWeight: 600,
												color: "#262626",
											}}
										>
											{(
												data.storage.totalSpace / 1e9
											).toFixed(1)}{" "}
											GB
										</div>
									</div>
									<div>
										<div
											style={{
												color: "#8c8c8c",
												fontSize: 14,
												marginBottom: 8,
											}}
										>
											已使用
										</div>
										<div
											style={{
												fontSize: 24,
												fontWeight: 600,
												color: getStorageColor(),
											}}
										>
											{(
												data.storage.usedSpace / 1e9
											).toFixed(1)}{" "}
											GB
										</div>
									</div>
									<div>
										<div
											style={{
												color: "#8c8c8c",
												fontSize: 14,
												marginBottom: 8,
											}}
										>
											剩余空间
										</div>
										<div
											style={{
												fontSize: 24,
												fontWeight: 600,
												color: "#52c41a",
											}}
										>
											{(
												data.storage.availSpace / 1e9
											).toFixed(1)}{" "}
											GB
										</div>
									</div>
								</Space>
							</Col>
						</Row>
					</ProCard>
				</Col>
			</Row>

			{/* 数据资源统计 */}
			<ProCard
				title={
					<Space>
						<BarChartOutlined style={{ color: "#1890ff" }} />
						<span>数据资源统计</span>
					</Space>
				}
				bordered
				headerBordered
				style={{ marginBottom: 16 }}
			>
				{/* 卫星遥感数据 */}
				<div style={{ marginBottom: 24 }}>
					<div
						style={{
							display: "flex",
							justifyContent: "space-between",
							alignItems: "center",
							fontSize: 16,
							fontWeight: 500,
							marginBottom: 16,
							paddingBottom: 12,
							borderBottom: "2px solid #e8f4ff",
							color: "#262626",
						}}
					>
						<div>
							<GlobalOutlined
								style={{
									marginRight: 8,
									color: "#1890ff",
									fontSize: 18,
								}}
							/>
							卫星遥感数据
							<span
								style={{
									marginLeft: 12,
									fontSize: 12,
									color: "#8c8c8c",
									fontWeight: 400,
								}}
							>
								多源卫星影像数据资源
							</span>
						</div>
						{/* 右侧链接 */}
						<a
							href="/satellite"
							target="_blank"
							style={{
								fontSize: 12,
								color: "#1890ff",
								fontWeight: 400,
							}}
						>
							查看详情 →
						</a>
					</div>

					<Row gutter={[16, 16]}>
						{data.data.satelliteList.map((item, index) => (
							<Col xs={12} sm={8} md={6} lg={4} key={item.key}>
								<ProCard
									bordered
									hoverable
									style={{
										textAlign: "center",
										background: `linear-gradient(135deg, #ffffff 0%, ${index % 2 === 0 ? "#f0f7ff" : "#f5f5f5"} 100%)`,
									}}
								>
									<div
										style={{
											fontSize: 13,
											color: "#8c8c8c",
											marginBottom: 8,
											fontWeight: 500,
										}}
									>
										{item.label}
									</div>
									<div
										style={{
											fontSize: 24,
											color: "#1890ff",
											fontWeight: 600,
										}}
									>
										{item.value.toLocaleString()}
									</div>
									<div
										style={{
											fontSize: 11,
											color: "#bfbfbf",
											marginTop: 4,
										}}
									>
										景数据
									</div>
								</ProCard>
							</Col>
						))}
					</Row>
				</div>

				{/* 矢量与地理产品 */}
				<div style={{ marginBottom: 24 }}>
					<div
						style={{
							display: "flex",
							justifyContent: "space-between",
							alignItems: "center",
							fontSize: 16,
							fontWeight: 500,
							marginBottom: 16,
							paddingBottom: 12,
							borderBottom: "2px solid #e8f4ff",
							color: "#262626",
						}}
					>
						<div>
							<EnvironmentOutlined
								style={{
									marginRight: 8,
									color: "#1890ff",
									fontSize: 18,
								}}
							/>
							矢量与地理产品
							<span
								style={{
									marginLeft: 12,
									fontSize: 12,
									color: "#8c8c8c",
									fontWeight: 400,
								}}
							>
								空间矢量与地理信息数据
							</span>
						</div>
						{/* 右侧链接 */}
						<a
							href="/vector"
							target="_blank"
							style={{
								fontSize: 12,
								color: "#1890ff",
								fontWeight: 400,
							}}
						>
							查看详情 →
						</a>
					</div>
					<Row gutter={[16, 16]}>
						{data.data.vectorList.map((item, index) => (
							<Col xs={12} sm={8} md={6} lg={4} key={item.key}>
								<ProCard
									bordered
									hoverable
									style={{
										textAlign: "center",
										background: `linear-gradient(135deg, #ffffff 0%, ${index % 2 === 0 ? "#f0fff4" : "#f5f5f5"} 100%)`,
									}}
								>
									<div
										style={{
											fontSize: 13,
											color: "#8c8c8c",
											marginBottom: 8,
											fontWeight: 500,
										}}
									>
										{item.label}
									</div>
									<div
										style={{
											fontSize: 24,
											color: "#52c41a",
											fontWeight: 600,
										}}
									>
										{item.value.toLocaleString()}
									</div>
									<div
										style={{
											fontSize: 11,
											color: "#bfbfbf",
											marginTop: 4,
										}}
									>
										个要素
									</div>
								</ProCard>
							</Col>
						))}
					</Row>
				</div>

				{/* 专题分析产品 */}
				<div>
					<div
						style={{
							display: "flex",
							justifyContent: "space-between",
							alignItems: "center",
							fontSize: 16,
							fontWeight: 500,
							marginBottom: 16,
							paddingBottom: 12,
							borderBottom: "2px solid #e8f4ff",
							color: "#262626",
						}}
					>
						<div>
							<BarChartOutlined
								style={{
									marginRight: 8,
									color: "#faad14",
									fontSize: 18,
								}}
							/>
							栅格产品
							<span
								style={{
									marginLeft: 12,
									fontSize: 12,
									color: "#8c8c8c",
									fontWeight: 400,
								}}
							>
								行业专题与深度分析产品
							</span>
						</div>
						{/* 右侧链接 */}
						<a
							href="/theme"
							target="_blank"
							style={{
								fontSize: 12,
								color: "#1890ff",
								fontWeight: 400,
							}}
						>
							查看详情 →
						</a>
					</div>
					<Row gutter={[16, 16]}>
						{data.data.themeList.map((item, index) => (
							<Col xs={12} sm={8} md={6} lg={4} key={item.key}>
								<ProCard
									bordered
									hoverable
									style={{
										textAlign: "center",
										background: `linear-gradient(135deg, #ffffff 0%, ${index % 2 === 0 ? "#fffbf0" : "#f5f5f5"} 100%)`,
									}}
								>
									<div
										style={{
											fontSize: 13,
											color: "#8c8c8c",
											marginBottom: 8,
											fontWeight: 500,
										}}
									>
										{item.label}
									</div>
									<div
										style={{
											fontSize: 24,
											color: "#faad14",
											fontWeight: 600,
										}}
									>
										{item.value.toLocaleString()}
									</div>
									<div
										style={{
											fontSize: 11,
											color: "#bfbfbf",
											marginTop: 4,
										}}
									>
										份产品
									</div>
								</ProCard>
							</Col>
						))}
					</Row>
				</div>
			</ProCard>

			{/* 存储桶信息 */}
			<ProCard
				title={
					<Space>
						<FolderOpenOutlined style={{ color: "#1890ff" }} />
						<span>存储桶详情</span>
					</Space>
				}
				bordered
				headerBordered
				style={{ marginBottom: 16 }}
			>
				<Table
					rowKey="bucketName"
					columns={bucketColumns}
					dataSource={data.storage.bucketsInfo}
					pagination={false}
					size="middle"
				/>
			</ProCard>

			{/* 服务器监控信息 */}
			{serverInfo && (
				<div>
					<ProCard
						direction="column"
						title={
							<Space>
								<DashboardOutlined
									style={{ color: "#1890ff" }}
								/>
								<span>后台服务器实时监控</span>
								<Tag
									color="success"
									icon={<SyncOutlined spin />}
								>
									实时
								</Tag>
							</Space>
						}
						bordered
						headerBordered
						style={{ marginBottom: 16 }}
					>
						{/* CPU和内存 */}
						<Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
							<Col xs={24} lg={12}>
								<ProCard
									title={
										<Space>
											<ThunderboltOutlined
												style={{ color: "#1890ff" }}
											/>
											<span>CPU 使用情况</span>
										</Space>
									}
									bordered
									type="inner"
								>
									<div style={{ marginBottom: 16 }}>
										<div
											style={{
												fontSize: 13,
												color: "#8c8c8c",
												marginBottom: 8,
											}}
										>
											处理器型号
										</div>
										<div
											style={{
												fontSize: 14,
												fontWeight: 500,
												color: "#262626",
											}}
										>
											{serverInfo.cpuInfo.cpu}
										</div>
									</div>

									<Row
										gutter={16}
										style={{ marginBottom: 16 }}
									>
										<Col span={12}>
											<div
												style={{ textAlign: "center" }}
											>
												<div
													style={{
														fontSize: 12,
														color: "#8c8c8c",
														marginBottom: 4,
													}}
												>
													物理核心
												</div>
												<div
													style={{
														fontSize: 20,
														fontWeight: 600,
														color: "#1890ff",
													}}
												>
													{
														serverInfo.cpuInfo
															.physicalProcessorCount
													}
												</div>
											</div>
										</Col>
										<Col span={12}>
											<div
												style={{ textAlign: "center" }}
											>
												<div
													style={{
														fontSize: 12,
														color: "#8c8c8c",
														marginBottom: 4,
													}}
												>
													逻辑核心
												</div>
												<div
													style={{
														fontSize: 20,
														fontWeight: 600,
														color: "#52c41a",
													}}
												>
													{
														serverInfo.cpuInfo
															.logicalProcessorCount
													}
												</div>
											</div>
										</Col>
									</Row>

									<div>
										<div
											style={{
												display: "flex",
												justifyContent: "space-between",
												marginBottom: 8,
											}}
										>
											<span
												style={{
													fontSize: 13,
													color: "#8c8c8c",
												}}
											>
												CPU 使用率
											</span>
											<span
												style={{
													fontSize: 16,
													fontWeight: 600,
													color:
														serverInfo.cpuInfo
															.cpuUsage > 0.8
															? "#ff4d4f"
															: serverInfo.cpuInfo
																		.cpuUsage >
																  0.6
																? "#faad14"
																: "#52c41a",
												}}
											>
												{(
													serverInfo.cpuInfo
														.cpuUsage * 100
												).toFixed(2)}
												%
											</span>
										</div>
										<Progress
											percent={parseFloat(
												(
													serverInfo.cpuInfo
														.cpuUsage * 100
												).toFixed(2),
											)}
											strokeColor={
												serverInfo.cpuInfo.cpuUsage >
												0.8
													? "#ff4d4f"
													: serverInfo.cpuInfo
																.cpuUsage > 0.6
														? "#faad14"
														: "#52c41a"
											}
											showInfo={false}
										/>
									</div>
								</ProCard>
							</Col>

							<Col xs={24} lg={12}>
								<ProCard
									title={
										<Space>
											<HddOutlined
												style={{ color: "#52c41a" }}
											/>
											<span>内存使用情况</span>
										</Space>
									}
									bordered
									type="inner"
								>
									<Row gutter={24} align="middle">
										<Col
											xs={24}
											sm={10}
											style={{
												textAlign: "center",
												marginBottom: 16,
											}}
										>
											<Progress
												type="circle"
												percent={parseFloat(
													serverInfo.memoryInfo.usedMemoryPercent.toFixed(
														1,
													),
												)}
												strokeColor={
													serverInfo.memoryInfo
														.usedMemoryPercent > 85
														? "#ff4d4f"
														: serverInfo.memoryInfo
																	.usedMemoryPercent >
															  70
															? "#faad14"
															: "#52c41a"
												}
												width={120}
												strokeWidth={8}
											/>
										</Col>
										<Col xs={24} sm={14}>
											<Space
												direction="vertical"
												size="middle"
												style={{ width: "100%" }}
											>
												<div>
													<div
														style={{
															color: "#8c8c8c",
															fontSize: 13,
															marginBottom: 4,
														}}
													>
														总内存
													</div>
													<div
														style={{
															fontSize: 18,
															fontWeight: 600,
															color: "#262626",
														}}
													>
														{(
															serverInfo
																.memoryInfo
																.totalMemory /
															1024 /
															1024 /
															1024
														).toFixed(2)}{" "}
														GB
													</div>
												</div>
												<div>
													<div
														style={{
															color: "#8c8c8c",
															fontSize: 13,
															marginBottom: 4,
														}}
													>
														已使用
													</div>
													<div
														style={{
															fontSize: 18,
															fontWeight: 600,
															color:
																serverInfo
																	.memoryInfo
																	.usedMemoryPercent >
																85
																	? "#ff4d4f"
																	: "#faad14",
														}}
													>
														{(
															(serverInfo
																.memoryInfo
																.totalMemory -
																serverInfo
																	.memoryInfo
																	.availableMemory) /
															1024 /
															1024 /
															1024
														).toFixed(2)}{" "}
														GB
													</div>
												</div>
												<div>
													<div
														style={{
															color: "#8c8c8c",
															fontSize: 13,
															marginBottom: 4,
														}}
													>
														可用内存
													</div>
													<div
														style={{
															fontSize: 18,
															fontWeight: 600,
															color: "#52c41a",
														}}
													>
														{(
															serverInfo
																.memoryInfo
																.availableMemory /
															1024 /
															1024 /
															1024
														).toFixed(2)}{" "}
														GB
													</div>
												</div>
											</Space>
										</Col>
									</Row>
								</ProCard>
							</Col>
						</Row>

						{/* 磁盘信息 */}
						<ProCard
							title={
								<Space>
									<HddOutlined style={{ color: "#722ed1" }} />
									<span>磁盘使用情况</span>
								</Space>
							}
							bordered
							type="inner"
							style={{ marginBottom: 16 }}
						>
							<Row gutter={[16, 16]}>
								{serverInfo.diskInfo.map((disk, index) => (
									<Col xs={24} sm={12} lg={8} key={index}>
										<ProCard bordered hoverable>
											<div style={{ marginBottom: 12 }}>
												<div
													style={{
														fontSize: 14,
														fontWeight: 500,
														color: "#262626",
														marginBottom: 4,
													}}
												>
													{disk.name}
												</div>
												<Tag color="purple">
													{disk.type}
												</Tag>
											</div>

											<div style={{ marginBottom: 12 }}>
												<div
													style={{
														display: "flex",
														justifyContent:
															"space-between",
														fontSize: 12,
														color: "#8c8c8c",
														marginBottom: 8,
													}}
												>
													<span>
														已用{" "}
														{disk.usagePercent.toFixed(
															1,
														)}
														%
													</span>
													<span>
														{disk.usableSpaceGB.toFixed(
															1,
														)}{" "}
														/{" "}
														{disk.totalSpaceGB.toFixed(
															1,
														)}{" "}
														GB
													</span>
												</div>
												<Progress
													percent={parseFloat(
														disk.usagePercent.toFixed(
															1,
														),
													)}
													strokeColor={
														disk.usagePercent > 90
															? "#ff4d4f"
															: disk.usagePercent >
																  75
																? "#faad14"
																: "#52c41a"
													}
													showInfo={false}
													size="small"
												/>
											</div>
										</ProCard>
									</Col>
								))}
							</Row>
						</ProCard>

						{/* 网络信息 */}
						<ProCard
							title={
								<Space>
									<ApiOutlined style={{ color: "#13c2c2" }} />
									<span>网络接口状态</span>
								</Space>
							}
							bordered
							type="inner"
						>
							<Table
								rowKey="name"
								columns={[
									{
										title: "接口名称",
										dataIndex: "displayName",
										width: 280,
										render: (text: string, record: any) => (
											<Space
												direction="vertical"
												size={0}
											>
												<span
													style={{ fontWeight: 500 }}
												>
													{text}
												</span>
												<span
													style={{
														fontSize: 12,
														color: "#8c8c8c",
													}}
												>
													{record.name}
												</span>
											</Space>
										),
									},
									{
										title: "IP 地址",
										dataIndex: "ipAddress",
										width: 140,
										render: (text: string) =>
											text === "N/A" ? (
												<Tag>未分配</Tag>
											) : (
												<Tag color="blue">{text}</Tag>
											),
									},
									{
										title: "MAC 地址",
										dataIndex: "macAddress",
										width: 150,
										render: (text: string) => (
											<span
												style={{
													fontFamily: "monospace",
													fontSize: 12,
												}}
											>
												{text}
											</span>
										),
									},
									{
										title: "速率",
										dataIndex: "speedMbps",
										width: 100,
										align: "right" as const,
										render: (val: number) =>
											val > 0 ? (
												<span
													style={{
														color: "#52c41a",
														fontWeight: 500,
													}}
												>
													{val >= 1000000
														? `${(val / 1000000).toFixed(0)} Gbps`
														: `${(val / 1000).toFixed(0)} Mbps`}
												</span>
											) : (
												<span
													style={{ color: "#bfbfbf" }}
												>
													-
												</span>
											),
									},
									{
										title: "下载速率",
										dataIndex: "rxKbps",
										width: 120,
										align: "right" as const,
										render: (val: number) => (
											<Space>
												<span
													style={{
														color: "#1890ff",
														fontWeight: 500,
													}}
												>
													↓{" "}
													{val > 1024
														? `${(val / 1024).toFixed(2)} Mbps`
														: `${val.toFixed(2)} Kbps`}
												</span>
											</Space>
										),
									},
									{
										title: "上传速率",
										dataIndex: "txKbps",
										width: 120,
										align: "right" as const,
										render: (val: number) => (
											<Space>
												<span
													style={{
														color: "#52c41a",
														fontWeight: 500,
													}}
												>
													↑{" "}
													{val > 1024
														? `${(val / 1024).toFixed(2)} Mbps`
														: `${val.toFixed(2)} Kbps`}
												</span>
											</Space>
										),
									},
								]}
								dataSource={serverInfo.networkInfo}
								pagination={false}
								size="small"
								scroll={{ x: 900 }}
							/>
						</ProCard>
					</ProCard>
				</div>
			)}
		</div>
	);
}
