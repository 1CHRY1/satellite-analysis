import type {
	ActionType,
	ParamsType,
	ProColumns,
} from "@ant-design/pro-components";
import { ProTable } from "@ant-design/pro-components";
import { useRef } from "react";
import { getAllWebSessionStatInfo } from "~/apis/https/sql/sql.admin";
import type { WebSessionStat } from "~/apis/https/sql/sql.type";

const WebSessionStatTable: React.FC = () => {
	const actionRef = useRef<ActionType>(undefined);

	const columns: ProColumns<WebSessionStat>[] = [
		{
			dataIndex: "index",
			valueType: "indexBorder",
			width: 48,
		},
		{
			dataIndex: "SESSIONID",
			title: "SESSIONID",
			valueType: "text",
			hideInSearch: true,
		},
		{
			dataIndex: "Principal",
			title: "Principal",
			valueType: "text",
			hideInSearch: true,
		},
		{
			dataIndex: "CreateTime",
			title: "创建时间",
			valueType: "dateTime",
			hideInSearch: true,
		},
		{
			dataIndex: "LastAccessTime",
			title: "最后访问时间",
			valueType: "dateTime",
			hideInSearch: true,
		},
		{
			dataIndex: "RemoteAddress",
			title: "访问IP地址",
			valueType: "text",
			hideInSearch: true,
		},
		{
			dataIndex: "RequestCount",
			title: "请求次数",
			width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "RequestTimeMillisTotal",
			title: "请求时间（和）",
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
			dataIndex: "JdbcExecuteCount",
			title: "Jdbc执行数",
			width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "JdbcExecuteTimeMillis",
			title: "Jdbc时间",
			width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "JdbcCommitCount",
			title: "事务提交数",
			width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "JdbcRollbackCount",
			title: "事务回滚数",
			width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "JdbcFetchRowCount",
			title: "读取行数",
			width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			dataIndex: "JdbcUpdateCount",
			title: "更新行数",
			width: 70,
			valueType: "digit",
			hideInSearch: true,
		},
	];

	const getAllWebSessionStat = async (
		params: ParamsType & {
			pageSize?: number;
			current?: number;
			keyword?: string;
		},
	) => {
		const res = await getAllWebSessionStatInfo();
		console.log(params);
		return {
			data: res,
			success: true,
			total: res.length,
		};
	};

	return (
		<ProTable<WebSessionStat>
			columns={columns}
			rowKey="SESSIONID"
			actionRef={actionRef}
			cardBordered
			request={getAllWebSessionStat}
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
			search={false}
			scroll={{ x: 1300 }}
			dateFormatter="string"
			headerTitle="Web Session 统计列表"
		/>
	);
};

export default function App() {
	return (
		<>
			<WebSessionStatTable />
		</>
	);
}
