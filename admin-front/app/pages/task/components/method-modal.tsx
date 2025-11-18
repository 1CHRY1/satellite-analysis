import {
	ProCard,
	ProDescriptions,
	ProTable,
	type ProColumns,
	type ProDescriptionsItemProps,
} from "@ant-design/pro-components";
import { Alert, Button, Modal, Spin, Tag, Typography } from "antd";
import { useState } from "react";
import type { Method, MethodParam } from "~/apis/https/task/task.type";
import { useMethod } from "../hooks/useMethod";
import {
	CodeOutlined,
	InfoCircleOutlined,
	QuestionCircleOutlined,
	SettingOutlined,
    EyeOutlined
} from "@ant-design/icons";

const renderParamsTable = (params: MethodParam[]) => {
	const columns: ProColumns<MethodParam>[] = [
		{
			title: "参数名称",
			dataIndex: "Name",
			key: "Name",
			width: 150,
			render: (text, record) => (
				<Typography.Text strong>
					{text}
					<Tag
						style={{ marginLeft: 8 }}
						color={record.Optional ? "default" : "red"}
					>
						{record.Optional ? "可选" : "必填"}
					</Tag>
				</Typography.Text>
			),
		},
		{
			title: "描述",
			dataIndex: "Description",
			key: "Description",
			ellipsis: true,
			// 允许复制描述
			copyable: true,
			width: 300,
		},
		{
			title: "类型",
			dataIndex: "Type",
			key: "Type",
			width: 100,
			render: (text) => <Tag color="blue">{text}</Tag>,
		},
		{
			title: "默认值",
			dataIndex: "default_value",
			key: "default_value",
			width: 100,
			render: (text) =>
				text === null ? (
					<Typography.Text type="secondary">无</Typography.Text>
				) : (
					<Tag color="geekblue">{text}</Tag>
				),
		},
	];

	return (
		<ProTable<MethodParam>
			headerTitle="方法参数列表"
			columns={columns}
			dataSource={params}
			rowKey="Name"
			search={false}
			options={{
				density: true,
				setting: false,
				fullScreen: true,
			}}
			size="small"
			pagination={false}
		/>
	);
};

/**
 * 方法详情模态框
 */
export const MethodModal: React.FC<{ id: number }> = ({ id }) => {
	const [open, setOpen] = useState(false);
	const { method, loading } = useMethod(id);

	const handleOpen = () => setOpen(true);
	const handleClose = () => setOpen(false);

	// ProDescriptions 配置
	const baseInfoColumns: ProDescriptionsItemProps<Method>[] = [
		{
			title: "方法 ID",
			dataIndex: "id",
			valueType: "text",
		},
		{
			title: "名称",
			dataIndex: "name",
			valueType: "text",
			span: 2, // 占据两个单元格
		},
		{
			title: "类型",
			dataIndex: "type",
			render: (_, record) => <Tag color="blue">{record.type}</Tag>,
		},
		{
			title: "创建时间",
			dataIndex: "createTime",
			valueType: "dateTime",
		},
		{
			title: "UUID",
			dataIndex: "uuid",
			copyable: true,
			span: 2,
		},
		{
			title: "简述",
			dataIndex: "description",
			span: 3, // 占据全部宽度
		},
		{
			title: "版权信息",
			dataIndex: "copyright",
			span: 3,
		},
	];

	// 渲染 Modal 内容
	const renderModalContent = () => {
		if (loading) {
			return (
				<div style={{ padding: "40px 0", textAlign: "center" }}>
					<Spin tip="加载方法详情..." />
				</div>
			);
		}

		if (!method) {
			return <Alert message="无法加载方法信息" type="error" showIcon />;
		}

		// 使用 ProDescriptions 和 ProCard 组织内容
		return (
			<div
				style={{
					maxHeight: "70vh",
					overflowY: "auto",
					paddingRight: 8, // 留出滚动条空间
				}}
			>
				{/* 1. 基本信息 ProCard */}
				<ProCard
					title={
						<>
							<InfoCircleOutlined style={{ marginRight: 8 }} />
							基本信息
						</>
					}
					headerBordered
					collapsible
					defaultCollapsed={false}
					style={{ marginBottom: 16 }}
				>
					<ProDescriptions
						column={3} // 3列布局
						dataSource={method}
						columns={baseInfoColumns}
						size="small"
						// 隐藏 Descriptions 的标题，因为它已经在 ProCard 中
						title={false}
					/>
				</ProCard>

				{/* 2. 详细描述 ProCard */}
				<ProCard
					title={
						<>
							<CodeOutlined style={{ marginRight: 8 }} />
							详细描述
						</>
					}
					headerBordered
					collapsible
					defaultCollapsed={false}
					style={{ marginBottom: 16 }}
				>
					<Typography.Paragraph>
						{method.longDesc || (
							<Typography.Text type="secondary">
								暂无详细描述。
							</Typography.Text>
						)}
					</Typography.Paragraph>
				</ProCard>

				{/* 3. 参数列表 ProCard (使用表格展示) */}
				<ProCard
					title={
						<>
							<SettingOutlined style={{ marginRight: 8 }} />
							参数列表
						</>
					}
					headerBordered
					collapsible
					defaultCollapsed={false} // 默认展开参数列表
					style={{ marginTop: 16 }}
					bodyStyle={{ padding: 0 }} // ProTable 自身有 padding，Card Body 去掉
				>
					{/* 使用 Table 展示参数，信息更集中、更标准 */}
					{renderParamsTable(method.params)}
				</ProCard>
			</div>
		);
	};

	return (
		<>
			{/* 触发按钮：更好的图标和展示名称 */}
			<Button
				size="small"
				type="link"
				onClick={handleOpen}
				icon={<SettingOutlined />}
			>
				{method?.name || `方法详情 (ID: ${id})`}
			</Button>

			<Modal
				title={`方法详情: ${method?.name || "加载中..."} (ID: ${id})`}
				open={open}
				onCancel={handleClose}
				footer={null}
				width={900} // 比原来更宽一点，给表格更多空间
				destroyOnClose={true}
			>
				{renderModalContent()}
			</Modal>
		</>
	);
};