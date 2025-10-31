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
import type { BackendCache, CacheType } from "~/types/cache";
import {
	batchDelBackendCache,
	delExpiredBackendCache,
	getBackendCache,
} from "~/apis/https/cache/cache.admin";

const getAllBackendCache = async (
	params: ParamsType & {
		pageSize?: number;
		current?: number;
		keyword?: string;
	},
): Promise<Partial<RequestData<BackendCache>>> => {
	// await waitTime(2000);
	const res = await getBackendCache();
	console.log(params);
	const backendCacheList: BackendCache[] = [];
	let idx = 0;
	for (let key in res.data) {
		const cacheType = key as CacheType;
		const cacheObj = res.data[cacheType];
		const cacheTypeParams = params.type
			? Array.isArray(params.type)
				? params.type
				: [params.type]
			: [];
		console.log("cacheTypeParams", cacheTypeParams);
		if (
			cacheTypeParams.includes(cacheType) ||
			cacheTypeParams.length === 0
		) {
			for (let cacheKey in cacheObj) {
				backendCacheList.push({
					...cacheObj[cacheKey],
					cacheKey,
					type: cacheType,
					id: idx,
				});
				idx++;
			}
		}
	}
	return {
		data: backendCacheList,
		success: true,
		total: backendCacheList.length,
	};
};

const delBackendCache = async (
	caches: { cacheKey: string; type: CacheType }[],
) => {
	const res = await batchDelBackendCache({ cacheKeys: caches });
	if (res.status === 1) {
		message.success("删除成功");
		return true;
	} else message.warning(res.message);
	return false;
};

const delAllExpiredBackendCache = async () => {
	const res = await delExpiredBackendCache();
	if (res.status === 1) {
		message.success("删除成功");
		return true;
	} else message.warning(res.message);
	return false;
};

const BackendCacheTable: React.FC = () => {
	const actionRef = useRef<ActionType>(undefined);

	const columns: ProColumns<BackendCache>[] = [
		{
			dataIndex: "index",
			valueType: "indexBorder",
			width: 48,
		},
		{
			title: "缓存键",
			dataIndex: "cacheKey",
			ellipsis: true,
			hideInSearch: true,
		},
		{
			title: "缓存类型",
			dataIndex: "type",
			ellipsis: true,
			filters: true,
			onFilter: true,
			valueType: "text", // 告诉 ProTable 用选择型字段
			fieldProps: {
				mode: "multiple", // ✅ 支持多选
				allowClear: true, // 可选：允许清空
				showSearch: true, // 可选：支持搜索
			},
			valueEnum: Object.fromEntries(
				[
					{ label: "时序立方体缓存", value: "eOCubeCache" },
					{ label: "遥感影像检索缓存", value: "sceneCache" },
					{ label: "栅格产品检索缓存", value: "themeCache" },
					{ label: "行政区缓存", value: "regionInfoCache" },
				].map((cache, idx) => [
					cache.value,
					{
						text: cache.label,
						status: ["Default", "Processing", "Success", "Warning"][
							idx % 4
						],
					},
				]),
			),
		},
		{
			title: "缓存时间",
			dataIndex: "cacheTime",
			valueType: "dateTime",
			hideInSearch: true,
		},
		{
			title: "过期时间",
			dataIndex: "cacheExpiry",
			valueType: "dateTime",
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
					description={"确定删除缓存" + record.cacheKey + "吗？"}
					onConfirm={async () => {
						const result = await delBackendCache([
							{ cacheKey: record.cacheKey!, type: record.type! },
						]);
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
		<ProTable<BackendCache>
			columns={columns}
			rowKey="id"
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
			tableAlertOptionRender={({ selectedRows, onCleanSelected }) => {
				return (
					<>
						<Popconfirm
							title="提示"
							description={"确定删除吗？"}
							onConfirm={async () => {
								const result = await delBackendCache(
									selectedRows.map((row) => ({
										cacheKey: row.cacheKey!,
										type: row.type!,
									})),
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
			request={getAllBackendCache}
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
			toolBarRender={() => [
				<Button
					variant="outlined"
					color="orange"
					onClick={async () => {
						const result = await delAllExpiredBackendCache();
						if (result) actionRef.current?.reload();
					}}
				>
					一键删除过期缓存
				</Button>,
			]}
		/>
	);
};

export default function App() {
	return (
		<>
			<BackendCacheTable />
		</>
	);
}
