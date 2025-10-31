import type {
	ActionType,
	ParamsType,
	ProColumns,
	RequestData,
} from "@ant-design/pro-components";
import { ProTable } from "@ant-design/pro-components";
import {
	Button,
	message,
	Popconfirm,
	Space,
} from "antd";
import { Table } from "antd";
import { useRef } from "react";
import type { RedisCache } from "~/types/cache";
import {
	batchDelRedisCache,
	getRedisCache,
} from "~/apis/https/cache/cache.admin";

const getAllRedisCache = async (
	params: ParamsType & {
		pageSize?: number;
		current?: number;
		keyword?: string;
	},
): Promise<Partial<RequestData<RedisCache>>> => {
	// await waitTime(2000);
	const res = await getRedisCache();
	console.log(params);
	let redisCacheList: RedisCache[] = res.data;
    redisCacheList = redisCacheList.filter(cache => cache.key.includes(params.key))
	return {
		data: redisCacheList,
		success: true,
		total: redisCacheList.length,
	};
};

const delRedisCache = async (caches: string[]) => {
	const res = await batchDelRedisCache({ keys: caches });
	if (res.status === 1) {
		message.success("删除成功");
		return true;
	} else message.warning(res.message);
	return false;
};

const RedisCacheTable: React.FC = () => {
	const actionRef = useRef<ActionType>(undefined);

	const columns: ProColumns<RedisCache>[] = [
		{
			dataIndex: "index",
			valueType: "indexBorder",
			width: 48,
		},
		{
			title: "缓存键",
			dataIndex: "key",
			ellipsis: true,
		},
		{
			title: "缓存类型",
			dataIndex: "type",
			valueType: "text", // 告诉 ProTable 用选择型字段
			hideInSearch: true,
		},
		{
			title: "Size",
			dataIndex: "size",
			valueType: "digit",
			hideInSearch: true,
		},
		{
			title: "ttl",
			dataIndex: "ttl",
			valueType: "digit",
			hideInSearch: true,
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
					description={"确定删除缓存" + record.key + "吗？"}
					onConfirm={async () => {
						const result = await delRedisCache([record.key]);
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
		<ProTable<RedisCache>
			columns={columns}
			rowKey="key"
			rowSelection={{
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
								const result = await delRedisCache(
									selectedRowKeys as string[]
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
			request={getAllRedisCache}
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
			headerTitle="缓存列表"
		/>
	);
};

export default function App() {
	return (
		<>
			<RedisCacheTable />
		</>
	);
}
