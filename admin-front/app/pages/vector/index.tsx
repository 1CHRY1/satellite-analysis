import { EllipsisOutlined } from "@ant-design/icons";
import type {
	ActionType,
	ParamsType,
	ProColumns,
	RequestData,
} from "@ant-design/pro-components";
import { ProTable } from "@ant-design/pro-components";
import {
	Button,
	Dropdown,
	message,
	Popconfirm,
	type PopconfirmProps,
	Space,
} from "antd";
import { Table } from "antd";
import type { SortOrder } from "antd/es/table/interface";
import { useEffect, useMemo, useRef, useState } from "react";
import { EditVectorButton } from "./edit-form";
import {
	getVectorPage,
} from "~/apis/https/vector/vector.admin";
import type { Vector } from "~/types/vector";

const getAllVector = async (
	params: ParamsType & {
		pageSize?: number;
		current?: number;
		keyword?: string;
	},
	sort: Record<string, SortOrder>,
	filter: Record<string, (string | number)[] | null>,
): Promise<Partial<RequestData<Vector>>> => {
	console.log(sort, filter);
	console.log(params);
	const res = await getVectorPage({
		page: params.current as number,
		pageSize: params.pageSize as number,
		searchText: params.vectorName,
	});
	console.log(params);
	return {
		data: res.data.records,
		success: true,
		total: res.data.total,
	};
};

const VectorTable: React.FC = () => {
	const actionRef = useRef<ActionType>(undefined);

	const columns: ProColumns<Vector>[] = [
		{
			dataIndex: "index",
			valueType: "indexBorder",
			width: 48,
		},
		{
			title: "矢量名称",
			dataIndex: "vectorName",
			ellipsis: true,
			formItemProps: {
				rules: [
					{
						required: true,
						message: "此项为必填项",
					},
				],
			},
			hideInSearch: true,
		},
		{
			title: "矢量",
			dataIndex: "vectorName",
			ellipsis: true,
			hideInTable: true,
		},
		{
			title: "表名",
			dataIndex: "tableName",
			ellipsis: true,
			hideInSearch: true,
		},
		{
			title: "坐标系（EPSG）",
			dataIndex: "srid",
			valueType: "text",
			hideInSearch: true,
		},
		{
			title: "条目数量",
			dataIndex: "count",
			valueType: "digit",
			hideInSearch: true,
		},
		{
			title: "创建时间",
			dataIndex: "time",
			valueType: "date",
			hideInSearch: true,
		},
		{
			title: "操作",
			width: 120,
			key: "option",
			valueType: "option",
			fixed: "right",
			render: (_, record) => [
				<EditVectorButton
					onSuccess={() => {
						actionRef.current?.reload();
					}}
					initVector={record}
				></EditVectorButton>,
			],
		},
	];

	return (
		<ProTable<Vector>
			columns={columns}
			rowKey="id"
			actionRef={actionRef}
			cardBordered
			request={getAllVector}
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
			headerTitle="矢量数据集列表"
		/>
	);
};

export default function App() {
	return (
		<>
			<VectorTable />
		</>
	);
}
