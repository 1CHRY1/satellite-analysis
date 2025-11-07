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
import { CreateProductButton } from "./insert-form";
import { EditProductButton } from "./edit-form";
import type { Product } from "~/types/product";
import {
	batchDelProduct,
	getProductPage,
} from "~/apis/https/product/product.admin";
import { getScenePage } from "~/apis/https/scene/scene.admin";
import { getSensorPage } from "~/apis/https/sensor/sensor.admin";

// import request from 'umi-request';
export const waitTimePromise = async (time: number = 100) => {
	return new Promise((resolve) => {
		setTimeout(() => {
			resolve(true);
		}, time);
	});
};

export const waitTime = async (time: number = 100) => {
	await waitTimePromise(time);
};

const getAllProduct = async (
	params: ParamsType & {
		pageSize?: number;
		current?: number;
		keyword?: string;
	},
	sort: Record<string, SortOrder>,
	filter: Record<string, (string | number)[] | null>,
): Promise<Partial<RequestData<Product>>> => {
	console.log(sort, filter);
	console.log(params);
	// await waitTime(2000);
	const res = await getProductPage({
		page: params.current as number,
		pageSize: params.pageSize as number,
		searchText: params.productName,
		sensorIds: (typeof params.sensorId === 'string') ? [params.sensorId] : params.sensorId,
	});
	console.log(params);
	return {
		data: res.data.records,
		success: true,
		total: res.data.total,
	};
};

const delProduct = async (productIds: string[]) => {
	const batchSize = 3;
	let flag = true;
	for (let i = 0; i < productIds.length; i += batchSize) {
		const ids = productIds.slice(i, i + batchSize);
		const results = await Promise.all(
			// 这里返回的是异步函数对象，而不是Promise对象，只有async函数执行时才返回包装后的Promise，否则他就只是异步函数对象
			// ids.map(id => async () => {
			//     let res = await getScenePage({
			//         page: 1,
			//         pageSize: 1,
			//         productId: id,
			//     })
			//     return res
			// })

			// 这里返回的也是异步函数对象数组
			ids.map((id) =>
				getScenePage({
					page: 1,
					pageSize: 1,
					productId: id,
				}),
			),
		);
		const hit = results.some(
			(res) => res && res.status === 1 && res.data?.total > 0,
		);
		if (hit) {
			flag = false;
			console.log("命中条件，中断后续请求。");
			break; // 中断外层 for 循环
		}
	}
	if (flag) {
		const res = await batchDelProduct({ productIds });
		if (res.status === 1) {
			message.success("删除成功");
			return true;
		} else {
			message.warning(res.message);
			return false;
		}
	} else {
		message.warning("请先删除产品关联的遥感影像");
		return false;
	}
};

const ProductTable: React.FC = () => {
	const actionRef = useRef<ActionType>(undefined);
	const [sensorOpts, setSensorOpts] = useState<
		{ label: string; value: string }[]
	>([]);
	useEffect(() => {
		const getAllSensor = async () => {
			const res = await getSensorPage({
				page: 1,
				pageSize: 999,
			});
			if (res.status === 1) {
				setSensorOpts(
					res.data.records.map((record) => ({
						label: record.platformName,
						value: record.sensorId,
					})),
				);
			} else {
				setSensorOpts([]);
			}
		};
		getAllSensor();
	}, []); // 去掉中括号请求爆炸

	const columns: ProColumns<Product>[] = [
		{
			dataIndex: "index",
			valueType: "indexBorder",
			width: 48,
		},
		{
			title: "产品名",
			dataIndex: "productName",
			ellipsis: true,
			hideInTable: true,
		},
		{
			title: "产品名",
			dataIndex: "productName",
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
			title: "描述信息",
			dataIndex: "description",
			valueType: "text",
			hideInSearch: true,
		},
		{
			title: "所属传感器",
			dataIndex: "sensorId",
			filters: true,
			onFilter: true,
			valueType: "text", // 告诉 ProTable 用选择型字段
			fieldProps: {
				mode: "multiple", // ✅ 支持多选
				allowClear: true, // 可选：允许清空
				showSearch: true, // 可选：支持搜索
			},
			valueEnum: Object.fromEntries(
				sensorOpts.map((sensor, idx) => [
					sensor.value,
					{
						text: sensor.label,
						status: ["Default", "Processing", "Success", "Warning"][
							idx % 4
						],
					},
				]),
			),
		},
		{
			title: "操作",
			width: 150,
			key: "option",
			valueType: "option",
			fixed: "right",
			render: (_, record) => [
				<EditProductButton
					onSuccess={() => {
						actionRef.current?.reload();
					}}
					initProduct={record}
					sensorOpts={sensorOpts}
				></EditProductButton>,
				<Popconfirm
					title="提示"
					description={"确定删除产品" + record.productName + "吗？"}
					onConfirm={async () => {
						const result = await delProduct([record.productId]);
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
		<ProTable<Product>
			columns={columns}
			rowKey="productId"
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
								const result = await delProduct(
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
			request={getAllProduct}
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
			headerTitle="产品列表"
			toolBarRender={() => [
				<CreateProductButton
					onSuccess={() => {
						actionRef.current?.reload();
					}}
					sensorOpts={sensorOpts}
				></CreateProductButton>,
			]}
		/>
	);
};

export default function App() {
	return (
		<>
			<ProductTable />
		</>
	);
}
