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
import { getRolePage } from "~/apis/https/role/role.admin.api";
import { batchDelUser, getUserPage } from "~/apis/https/user/user.admin.api";
import { CreateUserButton } from "./insert-form";
import { EditUserButton } from "./edit-form";
import type { UserInfo } from "~/apis/https/user/user.type";
import type { User } from "~/types/user";
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
	// return request<{
	// 	data: UserItem[];
	// }>("https://proapi.azurewebsites.net/github/issues", {
	// 	params,
	// });
};

const delUser = async (userIds: string[]) => {
	const res = await batchDelUser({ userIds });
	if (res.status === 1) {
		message.success("删除成功");
		return true;
	} else message.warning(res.message);
	return false;
};

const UserTable: React.FC = () => {
	const actionRef = useRef<ActionType>(undefined);
	const [roleEnum, setRoleEnum] = useState<Record<number, { text: string }>>(
		{},
	);

	useEffect(() => {
		const getAllRole = async () => {
			const res = await getRolePage({
				page: 1,
				pageSize: 999,
			});
			const roles = res.data.records;
			const roleEnumMap = roles.reduce(
				(acc, cur) => {
					acc[cur.roleId] = { text: cur.name };
					return acc;
				},
				{} as Record<number, { text: string }>,
			);
			setRoleEnum(roleEnumMap);
		};
		getAllRole();
	}, []);

	const columns: ProColumns<UserInfo>[] = useMemo(
		() => [
			{
				dataIndex: "index",
				valueType: "indexBorder",
				width: 48,
			},
			{
				title: "用户名",
				dataIndex: "userName",
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
				title: "用户",
				dataIndex: "userName",
				ellipsis: true,
				hideInTable: true,
			},
			{
				title: "邮箱",
				width: 220,
				dataIndex: "email",
				copyable: true,
				ellipsis: true,
				hideInSearch: true,
				formItemProps: {
					rules: [
						{
							required: true,
							message: "此项为必填项",
						},
					],
				},
			},
			{
				disable: true,
				title: "电话",
				width: 120,
				dataIndex: "phone",
				ellipsis: true,
				hideInSearch: true,
			},
			{
				disable: true,
				width: 80,
				title: "称谓",
				dataIndex: "title",
				ellipsis: true,
				hideInSearch: true,
			},
			{
				disable: true,
				title: "组织",
				dataIndex: "organization",
				ellipsis: true,
				hideInSearch: true,
			},
			{
				title: "地址",
				key: "address",
				hideInSearch: true,
				renderFormItem: (_, { defaultRender }) => {
					return defaultRender(_);
				},
				render: (_, record) => (
					<Space>
						{record.province}
						{record.city}
					</Space>
				),
			},
			{
				disable: true,
				title: "头像",
				dataIndex: "avartarPath",
				ellipsis: true,
				hideInSearch: true,
				hideInTable: true,
			},
			{
				title: "创建时间",
				width: 120,
				key: "showTime",
				dataIndex: "createTime",
				valueType: "date",
				sorter: true,
				hideInSearch: true,
			},
			{
				disable: true,
				title: "角色",
				width: 120,
				dataIndex: "roleId",
				ellipsis: true,
				filters: true,
				onFilter: true,
				valueType: "select",
				fieldProps: {
					mode: "multiple", // ✅ 支持多选
					allowClear: true, // 可选：允许清空
					showSearch: true, // 可选：支持搜索
				},
				valueEnum: roleEnum,
			},
			{
				title: "介绍",
				dataIndex: "introduction",
				valueType: "text",
				hideInSearch: true,
			},
			{
				title: "操作",
				width: 150,
				key: "option",
				valueType: "option",
				fixed: "right",
				render: (_, record) => [
					<EditUserButton
						onSuccess={() => {
							actionRef.current?.reload();
						}}
						roleEnum={roleEnum}
						initUser={{ ...record, password: "" } as User}
					></EditUserButton>,
					<Popconfirm
						title="提示"
						description={"确定删除用户" + record.userName + "吗？"}
						onConfirm={async () => {
							const result = await delUser([record.userId]);
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
		],
		[roleEnum],
	);

	return (
		<ProTable<UserInfo>
			columns={columns}
			rowKey="userId"
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
			tableAlertOptionRender={({selectedRowKeys, onCleanSelected}) => {
				return (
					<>
						<Popconfirm
							title="提示"
							description={
								"确定删除吗？"
							}
							onConfirm={async () => {
								const result = await delUser(selectedRowKeys as string[]);
								if (result) {
									actionRef.current?.reload();
									onCleanSelected()
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
			request={getAllUser}
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
				<CreateUserButton
					onSuccess={() => {
						actionRef.current?.reload();
					}}
					roleEnum={roleEnum}
				></CreateUserButton>,
			]}
		/>
	);
};

export default function App() {
	return (
		<>
			<UserTable />
		</>
	);
}
