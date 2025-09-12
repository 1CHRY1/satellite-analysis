import {
	SettingOutlined,
	TeamOutlined,
	AuditOutlined,
	UserOutlined,
	DatabaseOutlined,
	ConsoleSqlOutlined,
	ControlOutlined,
	FileProtectOutlined,
	GlobalOutlined,
	BlockOutlined,
	ClearOutlined,
	ProductOutlined,
	DashboardOutlined,
	VerticalRightOutlined,
	InboxOutlined,
} from "@ant-design/icons";
import type { MenuProps } from "antd";
type MenuItem = Required<MenuProps>["items"][number];

// 具名导出
export const items: MenuItem[] = [
	{
		key: "/dashboard", // 如果不加/就会按相对路径解析
		label: "工作台",
		icon: <DashboardOutlined />,
	},
	{
		key: "1",
		label: "用户",
		type: "group",
		children: [
			{
				key: "/user",
				label: "用户管理",
				icon: <UserOutlined />,
			},
			{
				key: "/role",
				label: "角色管理",
				icon: <TeamOutlined />,
			},
			{
				key: "/permission",
				label: "权限配置",
				icon: <AuditOutlined />,
			},
		],
	},
	{
		key: "2",
		label: "数据",
		type: "group",
		children: [
			{
				key: "/sensor",
				label: "传感器信息管理",
				icon: <GlobalOutlined />,
			},
			{
				key: "/product",
				label: "产品信息管理",
				icon: <InboxOutlined />,
			},
			{
				key: "/satellite",
				label: "遥感数据管理",
				icon: <DatabaseOutlined />,
			},
			{
				key: "/vector",
				label: "矢量数据管理",
				icon: <BlockOutlined />,
			},
			{
				key: "/theme",
				label: "栅格产品管理",
				icon: <ProductOutlined />,
			},
		],
	},
	{
		key: "3",
		label: "运维",
		type: "group",
		children: [
			{
				key: "/cache",
				label: "缓存管理",
				icon: <ClearOutlined />,
			},
			{
				key: "/sql",
				label: "SQL监控",
				icon: <ConsoleSqlOutlined />,
			},
			{
				key: "/audit",
				label: "日志审查",
				icon: <FileProtectOutlined />,
			},
			{
				key: "/task",
				label: "任务管理",
				icon: <ControlOutlined />,
			},
		],
	},
	{
		key: "4",
		label: "配置",
		type: "group",
		children: [
			{
				key: "/config",
				label: "系统参数",
				icon: <SettingOutlined />,
			},
		],
	},
];