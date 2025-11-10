import type {
	ActionType,
	ParamsType,
	ProColumns,
	RequestData,
} from "@ant-design/pro-components";
import { ProTable } from "@ant-design/pro-components";
import { useRef, useState } from "react";
import {
	getAllDBInfo,
	getAllSqlStatInfo,
	getSqlStatInfo,
} from "~/apis/https/sql/sql.admin";
import type { SqlStatInfo } from "~/apis/https/sql/sql.type";

const SqlStatTable: React.FC = () => {
	const actionRef = useRef<ActionType>(undefined);
	const [dbOpts, setDbOpts] = useState<{ label: string; value: number }[]>(
		[],
	);

	const columns: ProColumns<SqlStatInfo>[] = [
		{
			dataIndex: "index",
			valueType: "indexBorder",
			width: 48,
		},
        {
			dataIndex: "SQL",
			title: "SQL",
			valueType: "text",
            width: 240,
            ellipsis: true,
            copyable: true,
			hideInSearch: true,
		},
		{
			dataIndex: "ExecuteCount",
			title: "执行数",
            width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "Name",
			title: "所属数据源",
			filters: true,
			onFilter: true,
			valueType: "text",
			fieldProps: {
				allowClear: true,
				showSearch: true,
			},
			valueEnum: Object.fromEntries(
				dbOpts.map((db, idx) => [
					db.value,
					{
						text: db.label,
						status: ["Default", "Processing", "Success", "Warning"][
							idx % 4
						],
					},
				]),
			),
		},
		{
			dataIndex: "TotalTime",
			title: "执行时间",
            width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "MaxTimespan",
			title: "最慢",
            width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "InTransactionCount",
			title: "事务执行",
            width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "EffectedRowCount",
			title: "更新行数",
            width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "FetchRowCount",
			title: "读取行数",
            width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "RunningCount",
			title: "执行中",
            width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "ConcurrentMax",
			title: "最大并发",
            width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "Histogram",
			title: "执行时间分布",
			render: (_, record) => `[${record.Histogram.join(",")}]`,
			valueType: "text",
			hideInSearch: true,
		},
		{
			dataIndex: "ExecuteAndResultHoldTimeHistogram",
			title: "执行+RS时分布",
			render: (_, record) => `[${record.ExecuteAndResultHoldTimeHistogram.join(",")}]`,
			valueType: "text",
			hideInSearch: true,
		},
		{
			dataIndex: "FetchRowCountHistogram",
			title: "读取行分布",
			valueType: "text",
			render: (_, record) => `[${record.FetchRowCountHistogram.join(",")}]`,
			hideInSearch: true,
		},
		{
			dataIndex: "EffectedRowCountHistogram",
			title: "更新行分布",
			valueType: "text",
			render: (_, record) => `[${record.EffectedRowCountHistogram.join(",")}]`,
			hideInSearch: true,
		},
	];

	const getAllSqlStat = async (
		params: ParamsType & {
			pageSize?: number;
			current?: number;
			keyword?: string;
		},
	): Promise<Partial<RequestData<SqlStatInfo>>> => {
		const dbRes = await getAllDBInfo();
		setDbOpts(dbRes.map((db) => ({ label: db.Name, value: db.Identity })));
		let res;
		if (params.Name) {
			res = await getSqlStatInfo(params.Name);
		} else {
			res = await getAllSqlStatInfo();
		}
		console.log(params);
		return {
			data: res,
			success: true,
			total: res.length,
		};
	};

	return (
		<ProTable<SqlStatInfo>
			columns={columns}
			rowKey="ID"
			actionRef={actionRef}
			cardBordered
			request={getAllSqlStat}
			editable={{
				type: "multiple",
			}}
			columnsState={{
				persistenceKey: "pro-table-singe-demos",
				persistenceType: "localStorage",
				defaultValue: {
					option: { fixed: "right", disable: true },
				},
				onChange(value) {
					console.log("value: ", value);
				},
			}}
			search={{
				labelWidth: "auto",
			}}
			options={{
				setting: {
					listsHeight: 400,
				},
			}}
			form={{
				// 由于配置了 transform，提交的参数与定义的不同这里需要转化一下
				syncToUrl: (values, type) => {
					if (type === "get") {
						return {
							...values,
							created_at: [values.startTime, values.endTime],
						};
					}
					return values;
				},
			}}
			pagination={{
				pageSize: 10,
				onChange: (page) => console.log(page),
			}}
			scroll={{ x: 1300 }}
			dateFormatter="string"
			headerTitle="SQL列表"
		/>
	);
};

export default function App() {
	return (
		<>
			<SqlStatTable />
		</>
	);
}
