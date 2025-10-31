import {
	ProDescriptions,
	ProTable,
	type RequestData,
} from "@ant-design/pro-components";
import { Collapse, Divider, Tooltip } from "antd";
import { QuestionCircleOutlined } from "@ant-design/icons";
import { useEffect, useState } from "react";
import { DB_INFO_FIELDS } from "~/apis/https/sql/sql.type";
import { getAllDBInfo, getWallInfo } from "~/apis/https/sql/sql.admin";
import type {
	BlackListItem,
	DBInfo,
	FunctionStat,
	TableStat,
	WallInfo,
	WhiteListItem,
} from "~/apis/https/sql/sql.type";

const DBInfoDisplay = () => {
	const [dbList, setDbList] = useState<DBInfo[]>([]);
	const [dbWall, setDbWall] = useState<Record<string, WallInfo>>({});

	// 格式化值
	const formatValue = (key: string, value: any) => {
		if (value === null || value === undefined) return "null";
		if (Array.isArray(value)) return value.length ? value.join(", ") : "[]";
		if (typeof value === "boolean") return value ? "true" : "false";
		if (
			key.includes("Time") &&
			typeof value === "string" &&
			value.includes("T")
		) {
			return new Date(value).toLocaleString("zh-CN");
		}
		return String(value);
	};

	const getWall = async (datasourceId: number) => {
		const res = await getWallInfo(datasourceId);
		return res;
	};

	// 获取所有 DB
	useEffect(() => {
		getAllDBInfo().then(async (res) => {
			if (Array.isArray(res)) {
				setDbList(res);

				const wallPromises = res.map((db) =>
					getWall(db.Identity).then(
						(wall) =>
							({ [db.Name]: wall }) as Record<string, WallInfo>,
					),
				);

				const wallsArray = await Promise.all(wallPromises);

				// 合并所有数据库的 WallInfo
				const mergedWalls = Object.assign({}, ...wallsArray);

				// 一次性更新状态
				setDbWall((prev) => ({ ...prev, ...mergedWalls }));

				console.log(mergedWalls);
				console.log(dbWall);
			}
		});
	}, []);

	return (
		<>
			<h2
				style={{
					marginBottom: "24px",
					fontSize: "20px",
					fontWeight: 600,
				}}
			>
				数据库SQL防火墙信息
			</h2>
			{dbList.map((db, index) => (
				<div key={index}>
					<Divider orientation="left">{db.Name}</Divider>
					<Collapse
						items={[
							{
								key: String(index),
								label: `数据源${index + 1} - ${db.URL}`,
								children: (
									<>
										<h2
											style={{
												marginTop: "12px",
												marginBottom: "12px",
												fontSize: "18px",
												fontWeight: 600,
											}}
										>
											防御统计
										</h2>
										<ProDescriptions<WallInfo>
											column={3}
											dataSource={dbWall[db.Name]}
											columns={[
												{
													title: "检查次数",
													dataIndex: "checkCount",
													valueType: "digit",
												},
												{
													title: "硬检查次数",
													dataIndex: "hardCheckCount",
													valueType: "digit",
												},
												{
													title: "非法次数",
													dataIndex: "violationCount",
													valueType: "digit",
												},
												{
													title: "非法影响行数",
													dataIndex:
														"violationEffectRowCount",
													valueType: "digit",
												},
												{
													title: "黑名单命中次数",
													dataIndex:
														"blackListHitCount",
													valueType: "digit",
												},
												{
													title: "黑名单长度",
													dataIndex: "blackListSize",
													valueType: "digit",
												},
												{
													title: "白名单命中次数",
													dataIndex:
														"whiteListHitCount",
													valueType: "digit",
												},
												{
													title: "白名单长度",
													dataIndex: "whiteListSize",
													valueType: "digit",
												},
												{
													title: "语法错误次数",
													dataIndex:
														"syntaxErrorCount",
													valueType: "digit",
												},
											]}
											bordered
											size="small"
											emptyText="-"
										/>
										<h2
											style={{
												marginTop: "12px",
												marginBottom: "12px",
												fontSize: "18px",
												fontWeight: 600,
											}}
										>
											表访问统计
										</h2>
										<ProTable<TableStat>
											columns={[
												{
													title: "表名",
													dataIndex: "name",
													valueType: "text",
												},
												{
													title: "Select数",
													dataIndex: "selectCount",
													valueType: "digit",
												},
												{
													title: "SelectInto数",
													dataIndex:
														"selectIntoCount",
													valueType: "digit",
												},
												{
													title: "Insert数",
													dataIndex: "insertCount",
													valueType: "digit",
												},
												{
													title: "Update数",
													dataIndex: "updateCount",
													valueType: "digit",
												},
												{
													title: "Delete数",
													dataIndex: "deleteCount",
													valueType: "digit",
												},
												{
													title: "Truncate数",
													dataIndex: "truncateCount",
													valueType: "digit",
												},
												{
													title: "Create数",
													dataIndex: "createCount",
													valueType: "digit",
												},
												{
													title: "Alter数",
													dataIndex: "alterCount",
													valueType: "digit",
												},
												{
													title: "Drop数",
													dataIndex: "dropCount",
													valueType: "digit",
												},
												{
													title: "Replace数",
													dataIndex: "replaceCount",
													valueType: "digit",
												},
												{
													title: "删除数据行数",
													dataIndex:
														"deleteDataCount",
													valueType: "digit",
												},
												{
													title: "更新行分布",
													dataIndex:
														"updateHistogram",
													valueType: "text",
													render: (_, record) =>
														record.updateHistogram
															? `[${record.updateHistogram.join(",")}]`
															: "-",
												},
												{
													title: "更新数据行数",
													dataIndex:
														"updateDataCount",
													valueType: "digit",
												},
												{
													title: "读取行数",
													dataIndex: "fetchRowCount",
													valueType: "digit",
												},
												{
													title: "读取行分布",
													dataIndex:
														"fetchRowHistogram",
													valueType: "text",
													render: (_, record) =>
														record.fetchRowCountHistogram
															? `[${record.fetchRowCountHistogram.join(",")}]`
															: "-",
												},
											]}
											rowKey="ID"
											cardBordered
											dataSource={dbWall[db.Name]?.tables}
											options={false}
											scroll={{ x: 1300 }}
											search={false}
											pagination={false}
											dateFormatter="string"
										/>
										<h2
											style={{
												marginTop: "12px",
												marginBottom: "12px",
												fontSize: "18px",
												fontWeight: 600,
											}}
										>
											函数调用统计
										</h2>
										<ProTable<FunctionStat>
											columns={[
												{
													title: "函数名",
													dataIndex: "name",
													valueType: "text",
												},
												{
													title: "调用次数",
													dataIndex: "invokeCount",
													valueType: "digit",
												},
											]}
											rowKey="ID"
											cardBordered
											dataSource={
												dbWall[db.Name]?.functions || []
											}
											options={false}
											scroll={{ x: 1300 }}
											search={false}
											pagination={{
												pageSize: 10, // 每页显示 10 条
											}}
											dateFormatter="string"
										/>
										<h2
											style={{
												marginTop: "12px",
												marginBottom: "12px",
												fontSize: "18px",
												fontWeight: 600,
											}}
										>
											SQL防御统计 - 白名单
										</h2>
										<ProTable<WhiteListItem>
											columns={[
												{
													dataIndex: "index",
													valueType: "indexBorder",
													width: 48,
												},
												{
													title: "SQL",
													dataIndex: "sql",
													valueType: "text",
													ellipsis: true,
													copyable: true,
												},
												{
													title: "样本",
													dataIndex: "sample",
													valueType: "text",
													ellipsis: true,
													copyable: true,
												},
												{
													title: "执行数",
													dataIndex: "executeCount",
													valueType: "digit",
													width: 100,
												},
												{
													title: "执行出错数",
													dataIndex:
														"executeErrorCount",
													valueType: "digit",
													width: 100,
												},
												{
													title: "读取行数",
													dataIndex: "fetchRowCount",
													valueType: "digit",
													width: 100,
												},
												{
													title: "更新行数",
													dataIndex: "updateCount",
													valueType: "digit",
													width: 100,
												},
											]}
											rowKey="ID"
											cardBordered
											dataSource={
												dbWall[db.Name]?.whiteList || []
											}
											options={false}
											scroll={{ x: 1300 }}
											search={false}
											pagination={{
												pageSize: 10, // 每页显示 10 条
											}}
											dateFormatter="string"
										/>
										<h2
											style={{
												marginTop: "12px",
												marginBottom: "12px",
												fontSize: "18px",
												fontWeight: 600,
											}}
										>
											SQL防御统计 - 黑名单
										</h2>
										<ProTable<BlackListItem>
											columns={[
												{
													dataIndex: "index",
													valueType: "indexBorder",
													width: 48,
												},
												{
													title: "SQL",
													dataIndex: "sql",
													valueType: "text",
													ellipsis: true,
													copyable: true,
												},
												{
													title: "样本",
													dataIndex: "sample",
													valueType: "text",
													ellipsis: true,
													copyable: true,
												},
												{
													title: "violationMessage",
													dataIndex:
														"violationMessage",
													valueType: "text",
												},
												{
													title: "执行数",
													dataIndex: "executeCount",
													valueType: "digit",
													width: 100,
												},
												{
													title: "读取行数",
													dataIndex: "fetchRowCount",
													valueType: "digit",
													width: 100,
												},
												{
													title: "更新行数",
													dataIndex: "updateCount",
													valueType: "digit",
													width: 100,
												},
											]}
											rowKey="ID"
											cardBordered
											dataSource={
												dbWall[db.Name]?.blackList || []
											}
											options={false}
											scroll={{ x: 1300 }}
											search={false}
											pagination={{
												pageSize: 10, // 每页显示 10 条
											}}
											dateFormatter="string"
										/>
									</>
								),
							},
						]}
					/>
				</div>
			))}
		</>
	);
};

export default DBInfoDisplay;
