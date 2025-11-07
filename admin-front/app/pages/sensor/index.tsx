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
import { CreateSensorButton } from "./insert-form";
import { EditSensorButton } from "./edit-form";
import type { Sensor } from "~/types/sensor";
import {
	batchDelSensor,
	getSensorPage,
} from "~/apis/https/sensor/sensor.admin";
import { getProductPage } from "~/apis/https/product/product.admin";

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

const getAllSensor = async (
	params: ParamsType & {
		pageSize?: number;
		current?: number;
		keyword?: string;
	},
	sort: Record<string, SortOrder>,
	filter: Record<string, (string | number)[] | null>,
): Promise<Partial<RequestData<Sensor>>> => {
	console.log(sort, filter);
	console.log(params);
	// await waitTime(2000);
	const res = await getSensorPage({
		page: params.current as number,
		pageSize: params.pageSize as number,
		searchText: params.sensorName,
	});
	console.log(params);
	return {
		data: res.data.records,
		success: true,
		total: res.data.total,
	};
};

const delSensor = async (sensorIds: string[]) => {
	const productRes = await getProductPage({
		page: 1,
		pageSize: 999,
		sensorIds,
	});
	if (productRes.status === 1 && productRes.data.total === 0) {
		const res = await batchDelSensor({ sensorIds });
		if (res.status === 1) {
			message.success("删除成功");
			return true;
		} else {
			message.warning(res.message);
			return false;
		}
	} else if (productRes.status === 1 && productRes.data.total !== 0) {
        message.warning("请先删除传感器所关联的产品");
        return false;
    } else {
        message.warning("查询传感器关联产品失败");
        return false;
    }
};

const SensorTable: React.FC = () => {
	const actionRef = useRef<ActionType>(undefined);

	const columns: ProColumns<Sensor>[] = [
		{
			dataIndex: "index",
			valueType: "indexBorder",
			width: 48,
		},
		{
			title: "传感器ID",
			dataIndex: "sensorName",
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
			title: "传感器",
			dataIndex: "sensorName",
			ellipsis: true,
			hideInTable: true,
		},
		{
			title: "传感器名称",
			dataIndex: "platformName",
			ellipsis: true,
			hideInSearch: true,
		},
		{
			title: "描述信息",
			dataIndex: "description",
			valueType: "text",
			hideInSearch: true,
		},
		{
			title: "数据类型",
			dataIndex: "dataType",
			valueType: "text", // 告诉 ProTable 用选择型字段
			valueEnum: {
				"3d": { text: "红绿立体影像", status: "Default" },
				satellite: { text: "遥感影像", status: "Processing" },
				svr: { text: "形变速率产品", status: "Success" },
				dem: { text: "DEM产品", status: "Warning" },
				dsm: { text: "DSM产品", status: "Warning" },
				ndvi: { text: "NDVI产品", status: "Success" },
			},
			hideInSearch: true,
		},
		{
			title: "操作",
			width: 150,
			key: "option",
			valueType: "option",
			fixed: "right",
			render: (_, record) => [
				<EditSensorButton
					onSuccess={() => {
						actionRef.current?.reload();
					}}
					initSensor={record}
				></EditSensorButton>,
				<Popconfirm
					title="提示"
					description={
						"确定删除传感器" + record.platformName + "吗？"
					}
					onConfirm={async () => {
						const result = await delSensor([record.sensorId]);
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
		<ProTable<Sensor>
			columns={columns}
			rowKey="sensorId"
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
								const result = await delSensor(
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
			request={getAllSensor}
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
			headerTitle="传感器列表"
			toolBarRender={() => [
				<CreateSensorButton
					onSuccess={() => {
						actionRef.current?.reload();
					}}
				></CreateSensorButton>,
			]}
		/>
	);
};

export default function App() {
	return (
		<>
			<SensorTable />
		</>
	);
}
