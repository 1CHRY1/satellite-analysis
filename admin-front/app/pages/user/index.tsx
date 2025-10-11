import { EllipsisOutlined, PlusOutlined } from "@ant-design/icons";
import type {
	ActionType,
	ParamsType,
	ProColumns,
} from "@ant-design/pro-components";
import { ProTable, TableDropdown } from "@ant-design/pro-components";
import { Button, Dropdown, Space, Tag } from "antd";
import type { SortOrder } from "antd/es/table/interface";
import { useRef } from "react";
import { getUserPage } from "~/apis/https/user/user.admin.api";
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

type GithubIssueItem = {
	url: string;
	id: number;
	number: number;
	title: string;
	labels: {
		name: string;
		color: string;
	}[];
	state: string;
	comments: number;
	created_at: string;
	updated_at: string;
	closed_at?: string;
};

type UserItem = {
	userId: string;
	userName: string;
	phone: string;
	province: string;
	city: string;
	organization: string;
	introduction: string;
	create_time: string;
	title: string;
	role: string;
	email: string;
	avataPath: string;
	roleId: number;
};

const columns: ProColumns<UserItem>[] = [
	{
		dataIndex: "index",
		valueType: "indexBorder",
		width: 48,
	},
	{
		title: "用户名",
		dataIndex: "userName",
		ellipsis: true,
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
		title: "邮箱",
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
		dataIndex: "phone",
		ellipsis: true,
		hideInSearch: true,
	},
	{
		disable: true,
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
		render: (_, record) => <Space>{record.province}{record.city}</Space>,
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
		key: "showTime",
		dataIndex: "createTime",
		valueType: "date",
		sorter: true,
		hideInSearch: true,
	},
	{
		disable: true,
		title: "角色",
		dataIndex: "roleId",
		ellipsis: true,
		filters: true,
		onFilter: true,
		valueType: "select",
		valueEnum: {
			all: { text: "超长".repeat(50) },
			open: {
				text: "未解决",
				status: "Error",
			},
			closed: {
				text: "已解决",
				status: "Success",
				disabled: true,
			},
			processing: {
				text: "解决中",
				status: "Processing",
			},
		},
	},
];

const oldColumns: ProColumns<GithubIssueItem>[] = [
	{
		dataIndex: "index",
		valueType: "indexBorder",
		width: 48,
	},
	{
		title: "用户名",
		dataIndex: "userName",
		copyable: true,
		ellipsis: true,
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
		title: "状态",
		dataIndex: "state",
		filters: true,
		onFilter: true,
		ellipsis: true,
		valueType: "select",
		valueEnum: {
			all: { text: "超长".repeat(50) },
			open: {
				text: "未解决",
				status: "Error",
			},
			closed: {
				text: "已解决",
				status: "Success",
				disabled: true,
			},
			processing: {
				text: "解决中",
				status: "Processing",
			},
		},
	},
	{
		disable: true,
		title: "标签",
		dataIndex: "labels",
		search: false,
		renderFormItem: (_, { defaultRender }) => {
			return defaultRender(_);
		},
		render: (_, record) => (
			<Space>
				{record.labels.map(({ name, color }) => (
					<Tag color={color} key={name}>
						{name}
					</Tag>
				))}
			</Space>
		),
	},
	{
		title: "创建时间",
		key: "showTime",
		dataIndex: "created_at",
		valueType: "date",
		sorter: true,
		hideInSearch: true,
	},
	{
		title: "创建时间",
		dataIndex: "created_at",
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
		title: "操作",
		valueType: "option",
		key: "option",
		render: (text, record, _, action) => [
			<a
				key="editable"
				href="?"
				onClick={() => {
					action?.startEditable?.(record.id);
				}}
			>
				编辑
			</a>,
			<a
				href={record.url}
				target="_blank"
				rel="noopener noreferrer"
				key="view"
			>
				查看
			</a>,
			<TableDropdown
				key="actionGroup"
				onSelect={() => action?.reload()}
				menus={[
					{ key: "copy", name: "复制" },
					{ key: "delete", name: "删除" },
				]}
			/>,
		],
	},
];

const getAllUser = async (
	params: ParamsType & {
		pageSize?: number;
		current?: number;
		keyword?: string;
	},
	sort: Record<string, SortOrder>,
	filter: Record<string, (string | number)[] | null>,
) => {
	console.log(sort, filter);
	// await waitTime(2000);
	const res = await getUserPage({
		page: params.current as number,
		pageSize: params.pageSize as number,
		searchText: params.userName as string,
	})
	console.log(params)
	return {
		data: res.data.records,
		success: true,
		total: res.data.total
	}
	// return request<{
	// 	data: UserItem[];
	// }>("https://proapi.azurewebsites.net/github/issues", {
	// 	params,
	// });
};

const Table: React.FC = () => {
	const actionRef = useRef<ActionType>(undefined);
	return (
		<ProTable<UserItem>
			columns={columns}
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
			rowKey="id"
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
			dateFormatter="string"
			headerTitle="高级表格"
			toolBarRender={() => [
				<Button
					key="button"
					icon={<PlusOutlined />}
					onClick={() => {
						actionRef.current?.reload();
					}}
					type="primary"
				>
					新建
				</Button>,
				<Dropdown
					key="menu"
					menu={{
						items: [
							{
								label: "1st item",
								key: "1",
							},
							{
								label: "2nd item",
								key: "2",
							},
							{
								label: "3rd item",
								key: "3",
							},
						],
					}}
				>
					<Button>
						<EllipsisOutlined />
					</Button>
				</Dropdown>,
			]}
		/>
	);
};

export default function App() {
	return (
		<>
			<Table />
		</>
	);
}
