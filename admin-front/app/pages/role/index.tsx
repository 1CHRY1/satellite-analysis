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
import { getRolePage, batchDelRole } from "~/apis/https/role/role.admin.api";
import { CreateRoleButton } from "./insert-form";
import { EditRoleButton } from "./edit-form";
import type { Role } from "~/types/role";
import { getUserPage } from "~/apis/https/user/user.admin.api";
import type { UserInfo } from "~/apis/https/user/user.type";

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

const getAllRole = async (
	params: ParamsType & {
		pageSize?: number;
		current?: number;
		keyword?: string;
	},
	sort: Record<string, SortOrder>,
	filter: Record<string, (string | number)[] | null>,
): Promise<Partial<RequestData<Role>>> => {
	console.log(sort, filter);
	console.log(params);
	// await waitTime(2000);
	const res = await getRolePage({
		page: params.current as number,
		pageSize: params.pageSize as number,
		searchText: params.name,
	});
	console.log(params);
	return {
		data: res.data.records,
		success: true,
		total: res.data.total,
	};
};

const getAllUser = async (
	params: ParamsType & {
		pageSize?: number;
		current?: number;
		keyword?: string;
		roleId?: number[];
	},
	sort: Record<string, SortOrder>,
	filter: Record<string, (string | number)[] | null>,
): Promise<Partial<RequestData<UserInfo>>> => {
	console.log(sort, filter);
	console.log(params);
	// await waitTime(2000);
	const res = await getUserPage({
		page: params.current as number,
		pageSize: params.pageSize as number,
		searchText: params.userName,
		roleIds: params.roleId,
	});
	console.log(params);
	return {
		data: res.data.records,
		success: true,
		total: res.data.total,
	};
};

const delRole = async (roleIds: number[]) => {
	const userRes = await getAllUser(
		{ pageSize: 999, current: 1, roleId: roleIds },
		{},
		{},
	);
	if (userRes.total !== undefined && userRes.total === 0) {
		const res = await batchDelRole({ roleIds });
		if (res.status === 1) {
			message.success("删除成功");
			return true;
		} else message.warning(res.message);
		return false;
	} else {
        message.warning("请先取消角色关联的用户");
        return false;
    }
};

const RoleTable: React.FC = () => {
	const actionRef = useRef<ActionType>(undefined);

	const columns: ProColumns<Role>[] = [
		{
			dataIndex: "index",
			valueType: "indexBorder",
			width: 48,
		},
		{
			title: "角色名",
			dataIndex: "name",
			width: 100,
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
			title: "角色",
			dataIndex: "name",
			ellipsis: true,
			hideInTable: true,
		},
		{
			title: "描述信息",
			dataIndex: "description",
			valueType: "text",
			hideInSearch: true,
		},
		{
			title: "最大可用CPU核数",
			dataIndex: "maxCpu",
			valueType: "digit",
			hideInSearch: true,
		},
		{
			title: "最大可用存储空间（GB）",
			dataIndex: "maxStorage",
			valueType: "digit",
			hideInSearch: true,
		},
		{
			title: "最大可同时运行任务数",
			dataIndex: "maxJob",
			valueType: "digit",
			hideInSearch: true,
		},
		{
			title: "是否为超级管理员",
			dataIndex: "isSuperAdmin",
			valueType: "select", // 告诉 ProTable 用选择型字段
			valueEnum: {
				0: { text: "否", status: "Default" },
				1: { text: "是", status: "Success" },
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
				<EditRoleButton
					onSuccess={() => {
						actionRef.current?.reload();
					}}
					initRole={record}
				></EditRoleButton>,
				<Popconfirm
					title="提示"
					description={"确定删除角色" + record.name + "吗？"}
					onConfirm={async () => {
						const result = await delRole([record.roleId]);
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
		<ProTable<Role>
			columns={columns}
			rowKey="roleId"
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
								const result = await delRole(
									selectedRowKeys as number[],
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
			request={getAllRole}
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
			headerTitle="用户列表"
			toolBarRender={() => [
				<CreateRoleButton
					onSuccess={() => {
						actionRef.current?.reload();
					}}
				></CreateRoleButton>,
			]}
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
