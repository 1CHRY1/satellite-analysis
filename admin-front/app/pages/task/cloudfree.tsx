import type {
	ActionType,
	ParamsType,
	ProColumns,
	RequestData,
} from "@ant-design/pro-components";
import { ProTable } from "@ant-design/pro-components";
import {
	Button,
	Cascader,
	message,
	Popconfirm,
	Popover,
	Space,
	Spin,
	Tag,
} from "antd";
import { Table } from "antd";
import type { SortOrder } from "antd/es/table/interface";
import dayjs from "dayjs";
import { useEffect, useMemo, useRef, useState } from "react";
import { batchDelTask, getTaskPage } from "~/apis/https/task/task.admin";
import RegionSelector from "~/components/region/region-selector";
import type { Task } from "~/types/task";
import { useUser } from "./hooks/useUser";
import { Link } from "react-router";
import { EyeOutlined } from "@ant-design/icons";
const colors = ["magenta", "orange", "green", "geekblue", "purple"];

const getAllTask = async (
	params: ParamsType & {
		pageSize?: number;
		current?: number;
		keyword?: string;
	},
	sort: Record<string, SortOrder>,
	filter: Record<string, (string | number)[] | null>,
): Promise<Partial<RequestData<Task>>> => {
	console.log(sort, filter);
	console.log(params);
	// await waitTime(2000);
	const res = await getTaskPage({
		page: params.current as number,
		pageSize: params.pageSize as number,
		resolution: params.resolution,
		// regionId: params.regionId
		startTime: params.createTime?.[0]
			? dayjs(params.createTime?.[0]).format("YYYY-MM-DD HH:mm:ss")
			: undefined,
		endTime: params.createTime?.[1]
			? dayjs(params.createTime?.[1]).format("YYYY-MM-DD HH:mm:ss")
			: undefined,
	});
	console.log(params);
	return {
		data: res.data.records,
		success: true,
		total: res.data.total,
	};
};

const delTask = async (caseIds: string[]) => {
	const res = await batchDelTask({ caseIds });
	if (res.status === 1) {
		message.success("删除成功");
		return true;
	} else message.warning(res.message);
	return false;
};

const PopoverJson = ({ title, params }: { title: string; params: any }) => (
	<Popover
		placement="leftTop"
		title={title}
		content={
			<div style={{ maxHeight: 300, overflowY: "auto", maxWidth: 400 }}>
				<pre style={{ margin: 0, fontSize: "12px" }}>
					{JSON.stringify(params, null, 2)}
				</pre>
			</div>
		}
		trigger="click"
	>
		<Button size="small" type="link" icon={<EyeOutlined></EyeOutlined>}>
			查看
		</Button>
	</Popover>
);

/**
 * 用户
 */
const UserName: React.FC<{ id: string }> = ({ id }) => {
	const { loading, user } = useUser(id);
	return (
		<Spin spinning={loading}>
			<Link to={`/user?userName=${user?.userName}`}>
				{user?.userName}
			</Link>
		</Spin>
	);
};

const TaskTable: React.FC = () => {
	const actionRef = useRef<ActionType>(undefined);

	const columns: ProColumns<Task>[] = [
		{
			dataIndex: "index",
			valueType: "indexBorder",
			width: 48,
		},
		{
			title: "任务名称",
			dataIndex: "address",
			ellipsis: true,
			hideInSearch: true,
			render: (_, task) => {
				return task.address + "无云一版图";
			},
		},
		{
			title: "创建用户",
			dataIndex: "userId",
			ellipsis: true,
			hideInSearch: true,
			render: (_, task) => {
				return <UserName id={task.userId}></UserName>;
			},
		},
		{
			title: "行政区",
			dataIndex: "regionId",
			hideInSearch: true,
			renderFormItem: (_, { value, onChange }) => (
				<RegionSelector value={value} onChange={onChange} />
			),
			render: (_, record) => record.regionId, // 表格显示 adcode
		},
		{
			title: "格网分辨率",
			dataIndex: "resolution",
			valueType: "select",
			valueEnum: [
				1, 2, 5, 10, 15, 20, 25, 30, 40, 50, 100, 150, 200, 500, 1000,
			].reduce(
				(acc, cur) => {
					acc[cur] = {
						text: `${cur}km`,
						value: cur,
					};
					return acc;
				},
				{} as Record<number, { text: string; value: number }>,
			),
		},
		{
			title: "波段",
			dataIndex: "bandList",
			hideInSearch: true,
			render: (_, task) => {
				// 把 "[NIR, NDVI, EVI]" 转为数组
				const bandList = task.bandList
					?.replace(/[\[\]\s]/g, "")
					.split(",")
					.filter(Boolean);

				return bandList?.map((band, index) => (
					<Tag color={colors[index % colors.length]} key={band}>
						{band}
					</Tag>
				));
			},
		},
		{
			title: "数据源",
			dataIndex: "dataSet",
			valueType: "text",
			hideInSearch: true,
		},
		{
			title: "状态",
			dataIndex: "status",
			hideInSearch: true,
			valueEnum: {
				COMPLETE: { text: "已完成", status: "Success" },
				RUNNING: { text: "运行中", status: "Processing" },
				PENDING: { text: "排队中", status: "Warning" },
				NONE: { text: "未开始", status: "Default" },
				ERROR: { text: "失败", status: "Error" },
			},
		},
		{
			title: "运行结果",
			dataIndex: "result",
			valueType: "text",
			hideInSearch: true,
			render: (_, task) => {
				return (
					<PopoverJson
						title="输出结果"
						params={task.result}
					></PopoverJson>
				);
			},
		},
		{
			title: "创建时间",
			dataIndex: "createTime",
			valueType: "dateTime",
			hideInSearch: true,
		},
		{
			title: "创建时间",
			dataIndex: "createTime",
			valueType: "dateRange",
			hideInTable: true,
			fieldProps: {
				ranges: {
					今天: [dayjs(), dayjs()],
					最近7天: [dayjs().subtract(7, "day"), dayjs()],
					最近30天: [dayjs().subtract(30, "day"), dayjs()],
					本月: [dayjs().startOf("month"), dayjs().endOf("month")],
				},
			},
		},
		{
			title: "操作",
			width: 150,
			key: "option",
			valueType: "option",
			fixed: "right",
			render: (_, record) => [
				<Popconfirm
					title="提示"
					description={"确定删除任务" + record.caseId + "吗？"}
					onConfirm={async () => {
						const result = await delTask([record.caseId]);
						if (result) actionRef.current?.reload();
					}}
					okText="确定"
					cancelText="取消"
				>
					<Button danger type="link">
						删除
					</Button>
				</Popconfirm>,
			],
		},
	];

	return (
		<ProTable<Task>
			columns={columns}
			rowKey="caseId"
			rowSelection={{
				// 自定义选择项参考: https://ant.design/components/table-cn/#components-table-demo-row-selection-custom
				// 注释该行则默认不显示下拉选项
				selections: [Table.SELECTION_ALL, Table.SELECTION_INVERT],
			}}
			tableAlertRender={({
				selectedRowKeys,
				selectedRows,
				onCleanSelected,
			}) => {
				console.log(selectedRowKeys, selectedRows);
				return (
					<Space size={24}>
						<span>
							已选 {selectedRowKeys.length} 项
							<a
								style={{ marginInlineStart: 8 }}
								onClick={onCleanSelected}
							>
								取消选择
							</a>
						</span>
					</Space>
				);
			}}
			tableAlertOptionRender={({ selectedRowKeys, onCleanSelected }) => {
				return (
					<>
						<Popconfirm
							title="提示"
							description={"确定删除吗？"}
							onConfirm={async () => {
								const result = await delTask(
									selectedRowKeys as string[],
								);
								if (result) {
									actionRef.current?.reload();
									onCleanSelected();
								}
							}}
							okText="确定"
							cancelText="取消"
						>
							<Button type="link">批量删除</Button>
						</Popconfirm>
						<Button type="link">导出数据</Button>
					</>
				);
			}}
			actionRef={actionRef}
			cardBordered
			request={getAllTask}
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
			headerTitle="任务列表"
		/>
	);
};

export default function App() {
	return (
		<>
			<TaskTable />
		</>
	);
}
