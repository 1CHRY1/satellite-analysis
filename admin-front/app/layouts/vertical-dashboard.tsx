import React, { useState } from "react";
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
} from "@ant-design/icons";
import type { MenuProps } from "antd";
import { Breadcrumb, Layout, Menu, theme } from "antd";
import menuLogo from "~/assets/menu-logo.svg";
import menuLogoSlim from "~/assets/menu-logo-slim.svg";

const { Header, Content, Footer, Sider } = Layout;

type MenuItem = Required<MenuProps>["items"][number];

const getItem = (
	label: React.ReactNode,
	key: React.Key,
	icon?: React.ReactNode,
	children?: MenuItem[],
): MenuItem => {
	return {
		key,
		icon,
		children,
		label,
	} as MenuItem;
};

const items: MenuItem[] = [
	{
		key: 'dashboard',
		label: '工作台',
		icon: <DashboardOutlined />
	},
	{
		key: '1',
		label: '用户',
		type: 'group',
		children: [
			{
				key: 'u1',
				label: '用户管理',
				icon: <UserOutlined />,
			},
			{
				key: 'u2',
				label: '角色管理',
				icon: <TeamOutlined />
			},
			{
				key: 'u3',
				label: '权限配置',
				icon: <AuditOutlined />
			}
		]
	},
	{
		key: '2',
		label: '资源',
		type: 'group',
		children: [
			{
				key: 'r1',
				label: '数据管理',
				icon: <DatabaseOutlined />,
				children: [
					{
						key: 'r1-1',
						label: '遥感数据管理',
						icon: <GlobalOutlined />
					},
					{
						key: 'r1-2',
						label: '矢量数据管理',
						icon: <BlockOutlined />
					},
					{
						key: 'r1-3',
						label: '栅格产品管理',
						icon: <ProductOutlined />
					}
				]
			},
			{
				key: 'r2',
				label: '任务管理',
				icon: <ControlOutlined />
			},
			{
				key: 'r3',
				label: '权限配置',
				icon: <AuditOutlined />
			}
		]
	},
	{
		key: '3',
		label: '运维',
		type: 'group',
		children: [
			{
				key: 'm1',
				label: '缓存管理',
				icon: <ClearOutlined />,
			},
			{
				key: 'm2',
				label: 'SQL监控',
				icon: <ConsoleSqlOutlined />
			},
			{
				key: 'm3',
				label: '日志审查',
				icon: <FileProtectOutlined />
			}
		]
	},
	{
		key: '4',
		label: '配置',
		type: 'group',
		children: [
			{
				key: 'c1',
				label: '系统参数',
				icon: <SettingOutlined />,
			}
		]
	},
];

export const VerticalDashboardLayout: React.FC = () => {
	const [collapsed, setCollapsed] = useState(false);
	const {
		token: { colorBgContainer, borderRadiusLG },
	} = theme.useToken();

	return (
		<Layout>
			<Sider
				collapsible
				width={250}
				collapsed={collapsed}
				onCollapse={(value) => {
					setCollapsed(value);
				}}
				className="min-h-screen"
			>
				<div className="demo-logo-vertical pb-1 pt-1">
					<img
						src={menuLogo}
						alt="React Router"
						className={`block w-full ${collapsed && "hidden"}`}
					/>
					<img
						src={menuLogoSlim}
						alt="React Router"
						className={`block w-full ${collapsed || "hidden"}`}
					/>
				</div>
				<Menu
					theme="dark"
					defaultSelectedKeys={["1"]}
					mode="inline"
					items={items}
				/>
			</Sider>
			<Layout>
				<Header style={{ padding: 0, background: colorBgContainer }} />
				<Content style={{ margin: "0 16px", paddingTop: '16px' }}>
					<div
						style={{
							padding: 24,
							minHeight: 360,
							background: colorBgContainer,
							borderRadius: borderRadiusLG,
						}}
					>
						Bill is a cat.<div className="min-h-screen"></div>
					</div>
				</Content>
				<Footer style={{ textAlign: "center" }}>
					Copyright ©{new Date().getFullYear()} Created by OpenGMS
				</Footer>
			</Layout>
		</Layout>
	);
};
