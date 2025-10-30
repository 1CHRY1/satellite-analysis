import {
	CloudOutlined,
	DeleteOutlined,
	EllipsisOutlined,
	SearchOutlined,
} from "@ant-design/icons";
import type {
	ActionType,
	ParamsType,
	ProColumns,
	ProFormInstance,
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
	Tag,
	Tooltip,
} from "antd";
import { Table } from "antd";
import type { SortOrder } from "antd/es/table/interface";
import React, { useEffect, useMemo, useRef, useState } from "react";
import { EditSceneButton } from "./edit-form";
import type { Scene } from "~/types/scene";
import {
	batchDelScene,
	getScenePage,
	updateScene,
} from "~/apis/https/scene/scene.admin";
import { getSensorPage } from "~/apis/https/sensor/sensor.admin";
import { getProductPage } from "~/apis/https/product/product.admin";
import type { Image } from "~/types/image";
import type { ImageRequest } from "~/apis/https/image/image.type";
import { batchDelImage, getImage } from "~/apis/https/image/image.admin";
import { useSearchParams } from "react-router";
import styles from "./scene.module.css";
import dayjs from "dayjs";
import { BandEdit } from "./edit-image-form";

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
		sensorIds:
			typeof params.sensorId === "string"
				? [params.sensorId]
				: params.sensorId,
		productId: params.productId,
		...(params.startTime && {
			startTime: dayjs(params.startTime).format("YYYY-MM-DDTHH:mm:ss"),
		}),
		...(params.endTime && {
			endTime: dayjs(params.endTime).format("YYYY-MM-DDTHH:mm:ss"),
		}),
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

const delImage = async (imageIds: string[], scene: Scene) => {
	const res = await batchDelImage({ imageIds });
	if (res.status !== 1) {
		return false;
	}
	const {data} = await getSceneImages(scene.sceneId);
    
    scene.bands = data.map(d => d.band.toString());
    scene.bandNum = data.length;
	
	const sceneRes = await updateScene(scene);
	if (sceneRes.status === 1) {
		message.success("删除成功");
		return true;
	} else {
		message.warning(sceneRes.message);
		return false;
	}
};

const handleImageDownload = async (record: Image) => {
	const url = `/minio/console/${record.bucket}/${record.tifPath}`;
	window.open(url, "_blank", "noopener,noreferrer");
};

const handleCloudDownload = async (record: Scene) => {
	if (record.cloudPath) {
		const url = `/minio/console/${record.bucket}/${record.cloudPath}`;
		window.open(url, "_blank", "noopener,noreferrer");
	}
};

const SceneTable: React.FC = () => {
	const actionRef = useRef<ActionType>(undefined);
	const rowActionRefs = useRef<Record<string, React.RefObject<ActionType | null>>>({});
	const formRef = useRef<ProFormInstance>(undefined);
	const [sensorOpts, setSensorOpts] = useState<
		{ label: string; value: string }[]
	>([]);
	const [productOpts, setProductOpts] = useState<
		{ label: string; value: string }[]
	>([]);
	const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);
	const [selectedRowsMap, setSelectedRowsMap] = useState<
		Record<string, React.Key[]>
	>({});
	const [searchParams] = useSearchParams();

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
		const sensorIds = searchParams.getAll("sensorId");
		getAllProduct(sensorIds);
	}, []); // 去掉中括号请求爆炸

	const getAllProduct = async (sensorIds: string[]) => {
		const res = await getProductPage({
			page: 1,
			pageSize: 999,
			sensorIds: sensorIds,
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
			hideInSearch: true,
			width: 180,
			render: (_, record) => {
				if (!record.tags) return "-";

				const tags =
					typeof record.tags === "string"
						? JSON.parse(record.tags)
						: record.tags;

				const { source, category, production } = tags;

				const tagConfig = [
					{
						key: "source",
						text: source === "national" ? "国内" : "国外",
						color: source === "national" ? "red" : "blue",
					},
					{
						key: "category",
						text: category === "ard" ? "ARD" : "传统",
						color: category === "ard" ? "orange" : "green",
					},
					{
						key: "production",
						text: production === "radar" ? "雷达" : "光学",
						color: production === "radar" ? "purple" : "cyan",
					},
				];

				return (
					<Space wrap>
						{tagConfig.map((t) => (
							<Tag key={t.key} color={t.color}>
								{t.text}
							</Tag>
						))}
					</Space>
				);
			},
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
			width: 130,
			key: "option",
			valueType: "option",
			fixed: "right",
			render: (_, record) => [
				record.cloudPath && (
					<Tooltip title="云掩膜">
						<Button
							type="dashed"
							shape="circle"
							icon={<CloudOutlined />}
							onClick={() => {
								handleCloudDownload(record);
							}}
						/>
					</Tooltip>
				),
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
					<Tooltip title="删除">
						<Button
							type="dashed"
							shape="circle"
							danger
							icon={<DeleteOutlined />}
						/>
					</Tooltip>
				</Popconfirm>,
			],
		},
	];

	const expandedRowRender = (record: Scene) => {
		// const data = expandedData[record.sceneId] || [];
		const currentSelectedKeys = selectedRowsMap[record.sceneId] || [];
		if (!rowActionRefs.current[record.sceneId]) {
			rowActionRefs.current[record.sceneId] = React.createRef<ActionType>();
		}
		const currentRowRef = rowActionRefs.current[record.sceneId];
		// // 如果正在加载
		// if (loadingRows.includes(record.sceneId)) {
		// 	return <Spin tip="加载中..." />;
		// }

		return (
			<div className={styles.noExpandTable}>
				<ProTable<Image>
					rowKey="imageId"
					bordered={false}
					size="small"
					scroll={{ y: 360 }}
					request={async (params) => {
						return await getSceneImages(record.sceneId);
					}}
					columns={[
						{
							title: "波段",
							dataIndex: "band",
							valueType: "digit",
							width: 100,
							sorter: (a, b) => a.band - b.band,
							defaultSortOrder: "ascend",
						},
						{
							title: "桶",
							width: 200,
							dataIndex: "bucket",
							valueType: "text",
						},
						{
							title: "路径",
							dataIndex: "tifPath",
							valueType: "text",
						},
						{
							title: "操作",
							width: 220,
							key: "option",
							valueType: "option",
							fixed: "right",
							render: (_, image_record) => [
								<Button
									type="link"
									onClick={() => {
										handleImageDownload(image_record);
									}}
								>
									下载
								</Button>,
								<BandEdit
									record={image_record}
									scene={record}
									onSuccess={() => {
										currentRowRef.current?.reload()
										actionRef.current?.reload()
									}}
								></BandEdit>,
								<Popconfirm
									title="提示"
									description={
										"确定删除波段" +
										image_record.band +
										"吗？"
									}
									onConfirm={async () => {
										const result = await delImage(
											[image_record.imageId],
											record,
										);
										if (result) {
											currentRowRef.current?.reload();
											actionRef.current?.reload()
										}
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
							setSelectedRowsMap((prev) => ({
								...prev,
								[record.sceneId]: selectedRowKeys,
							}));
						},
						selections: [
							Table.SELECTION_ALL,
							Table.SELECTION_INVERT,
						],
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
											setSelectedRowsMap((prev) => ({
												...prev,
												[record.sceneId]: [],
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
										const result = await delImage(
											selectedRowKeys as string[],
											record,
										);
										if (result) {
											currentRowRef.current?.reload();
											setSelectedRowsMap((prev) => ({
												...prev,
												[record.sceneId]: [],
											}));
											currentRowRef.current?.clearSelected?.()
											actionRef.current?.reload()
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
					actionRef={currentRowRef}
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
			</div>
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
			formRef={formRef}
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
						// 传感器选择变化，所选产品清空
						formRef.current?.setFieldsValue({
							productId: undefined,
						});
						const sensorIds =
							typeof changedVals.sensorId === "string"
								? [changedVals.sensorId]
								: changedVals.sensorId;
						getAllProduct(sensorIds);
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
			<SceneTable />
		</>
	);
}
