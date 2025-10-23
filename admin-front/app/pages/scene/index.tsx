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
	Spin,
} from "antd";
import { Table } from "antd";
import type { SortOrder } from "antd/es/table/interface";
import { useEffect, useMemo, useRef, useState } from "react";
import { EditSceneButton } from "./edit-form";
import type { Scene } from "~/types/scene";
import { batchDelScene, getScenePage } from "~/apis/https/scene/scene.admin";
import { getSensorPage } from "~/apis/https/sensor/sensor.admin";
import { getProductPage } from "~/apis/https/product/product.admin";
import type { Image } from "~/types/image";
import type { ImageRequest } from "~/apis/https/image/image.type";
import { getImage } from "~/apis/https/image/image.admin";

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

const getAllScene = async (
	params: ParamsType & {
		pageSize?: number;
		current?: number;
		keyword?: string;
	},
	sort: Record<string, SortOrder>,
	filter: Record<string, (string | number)[] | null>,
): Promise<Partial<RequestData<Scene>>> => {
	console.log(sort, filter);
	console.log(params);
	// await waitTime(2000);
	const res = await getScenePage({
		page: params.current as number,
		pageSize: params.pageSize as number,
		searchText: params.sceneName,
		sensorId: params.sensorId,
		productId: params.productId,
	});
	console.log(params);
	return {
		data: res.data.records,
		success: true,
		total: res.data.total,
	};
};

const delScene = async (sceneIds: string[]) => {
	const res = await batchDelScene({ sceneIds });
	if (res.status === 1) {
		message.success("删除成功");
		return true;
	} else {
		message.warning(res.message);
		return false;
	}
};

const RoleTable: React.FC = () => {
	const actionRef = useRef<ActionType>(undefined);
	const [sensorOpts, setSensorOpts] = useState<
		{ label: string; value: string }[]
	>([]);
	const [productOpts, setProductOpts] = useState<
		{ label: string; value: string }[]
	>([]);
	const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);
	const [selectedRowsMap, setSelectedRowsMap] = useState<Record<string, React.Key[]>>({});

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

	const getAllProduct = async (sensorId: string) => {
		const res = await getProductPage({
			page: 1,
			pageSize: 999,
			sensorIds: [sensorId],
		});
		if (res.status === 1) {
			setProductOpts(
				res.data.records.map((record) => ({
					label: record.productName,
					value: record.productId,
				})),
			);
		} else {
			setProductOpts([]);
		}
	};

	const getSceneImages = async (sceneId: ImageRequest) => {
		const res = await getImage(sceneId);
		if (res.status === 1) {
			return {
				data: res.data || [],
				success: true,
				total: res.data?.length || 0,
			};
		} else {
			return {
				data: [],
				success: false,
				total: 0,
			};
		}
	};

	const columns: ProColumns<Scene>[] = [
		{
			dataIndex: "index",
			valueType: "indexBorder",
			width: 48,
		},
		{
			title: "遥感影像",
			dataIndex: "sceneName",
			ellipsis: true,
			hideInTable: true,
		},
		{
			title: "遥感影像标识",
			dataIndex: "sceneName",
			width: 400,
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
			title: "所属传感器",
			dataIndex: "sensorId",
			filters: true,
			onFilter: true,
			valueType: "text", // 告诉 ProTable 用选择型字段
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
			title: "所属产品",
			dataIndex: "productId",
			filters: true,
			onFilter: true,
			valueType: "text", // 告诉 ProTable 用选择型字段
			valueEnum: Object.fromEntries(
				productOpts.map((product, idx) => [
					product.value,
					{
						text: product.label,
						status: ["Default", "Processing", "Success", "Warning"][
							idx % 4
						],
					},
				]),
			),
		},
		{
			title: "时间范围",
			dataIndex: "dateRange",
			valueType: "dateRange",
			hideInTable: true,
			search: {
				transform: (value) => {
					return {
						startTime: value[0],
						endTime: value[1],
					};
				},
			},
		},
		{
			title: "时间",
			dataIndex: "sceneTime",
			valueType: "date",
			hideInSearch: true,
		},
		{
			title: "坐标系",
			dataIndex: "coordinateSystem",
			width: 80,
			valueType: "text",
			hideInSearch: true,
		},
		{
			title: "波段数",
			dataIndex: "bandNum",
			width: 80,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			title: "标签",
			dataIndex: "tags",
			valueType: "text",
			hideInSearch: true,
		},
		{
			title: "云量",
			dataIndex: "cloud",
			width: 80,
			valueType: "digit",
			hideInSearch: true,
		},
		{
			title: "NoData",
			dataIndex: "noData",
			width: 80,
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
				<EditSceneButton
					onSuccess={() => {
						actionRef.current?.reload();
					}}
					initScene={record}
					sensorOpts={sensorOpts}
				></EditSceneButton>,
				<Popconfirm
					title="提示"
					description={"确定删除遥感影像" + record.sceneName + "吗？"}
					onConfirm={async () => {
						const result = await delScene([record.sceneId]);
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

	const expandedRowRender = (record: Scene) => {
		// const data = expandedData[record.sceneId] || [];
		const currentSelectedKeys = selectedRowsMap[record.sceneId] || [];
		// // 如果正在加载
		// if (loadingRows.includes(record.sceneId)) {
		// 	return <Spin tip="加载中..." />;
		// }

		return (
			<ProTable<Image>
				rowKey="imageId"
				bordered={false}
				size="small"
				request={async (params) => {
					return await getSceneImages(record.sceneId)
				}}
				
				columns={[
					{ 
						title: "波段",
						 dataIndex: "band", 
						 valueType: "digit",
						 sorter: (a, b)=> a.band - b.band,
						 defaultSortOrder: "ascend"
					},
					{
						title: "链接",
						dataIndex: "tifPath",
						valueType: "text",
					},
					{
						title: "操作",
						width: 150,
						key: "option",
						valueType: "option",
						fixed: "right",
						render: (_, record) => [
							<EditSceneButton
								onSuccess={() => {
									actionRef.current?.reload();
								}}
								initScene={record}
								sensorOpts={sensorOpts}
							></EditSceneButton>,
							<Popconfirm
								title="提示"
								description={"确定删除遥感影像" + record.sceneName + "吗？"}
								onConfirm={async () => {
									const result = await delScene([record.sceneId]);
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
				]}
				headerTitle={false}
				search={false}
				options={false}
				rowSelection={{
					selectedRowKeys: currentSelectedKeys, // 绑定当前子表格的选中状态
					onChange: (selectedRowKeys) => {
						// 更新当前子表格的选中状态
						setSelectedRowsMap(prev => ({
							...prev,
							[record.sceneId]: selectedRowKeys
						}));
					},
					selections: [Table.SELECTION_ALL, Table.SELECTION_INVERT],
				}}
				tableAlertRender={({
					selectedRowKeys,
					selectedRows,
					onCleanSelected,
				}) => {
					console.log(selectedRowKeys, selectedRows);
					return (
						<Space size={20}>
							<span>
								已选 {selectedRowKeys.length} 项
								<a
									style={{ marginInlineStart: 8 }}
									onClick={() => {
										setSelectedRowsMap(prev => ({
											...prev,
											[record.sceneId]: []
										}));
									}}
								>
									取消选择
								</a>
							</span>
						</Space>
					);
				}}
				tableAlertOptionRender={({ selectedRowKeys }) => {
					return (
						<>
							<Popconfirm
								title="提示"
								description={"确定删除吗？"}
								onConfirm={async () => {
									const result = await delScene(
										selectedRowKeys as string[],
									);
									if (result) {
										actionRef.current?.reload();
										setSelectedRowsMap(prev => ({
											...prev,
											[record.sceneId]: []
										}));
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
				pagination={false}
			/>
		);
	};

	const handleExpand = async (expanded: boolean, record: any) => {
		const sceneId = record.sceneId;

		if (expanded) {
			// 设置展开状态
			setExpandedRowKeys((prev) => [...prev, sceneId]);

			// // 如果该行数据还没加载，发请求
			// if (!expandedData[sceneId]) {
			// 	setLoadingRows((prev) => [...prev, sceneId]);
			// 	const data = await getSceneImages(sceneId);
			// 	setExpandedData((prev) => ({ ...prev, [sceneId]: data }));
			// 	setLoadingRows((prev) => prev.filter((id) => id !== sceneId));
			// }
		} else {
			// 折叠该行
			setExpandedRowKeys((prev) => prev.filter((key) => key !== sceneId));
		}
	};

	return (
		<ProTable<Scene>
			columns={columns}
			rowKey="sceneId"
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
								const result = await delScene(
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
			request={getAllScene}
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
				onValuesChange: (changedVals, allVals) => {
					if (changedVals.sensorId) {
						getAllProduct(changedVals.sensorId);
					}
				},
			}}
			expandable={{
				expandedRowRender,
				onExpand: handleExpand,
				expandedRowKeys,
			}}
			pagination={{
				pageSize: 10,
				onChange: (page) => console.log(page),
			}}
			scroll={{ x: 1300 }}
			dateFormatter="string"
			headerTitle="遥感影像列表"
		/>
	);
};

export default function App() {
	return (
		<>
			<RoleTable />
		</>
	);
}
