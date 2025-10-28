import React, { useMemo, useState } from "react";
import {
	VerticalRightOutlined,
} from "@ant-design/icons";
import type { MenuProps } from "antd";
import { Breadcrumb, Layout, Menu, theme, Tooltip } from "antd";
import menuLogo from "~/assets/menu-logo.svg";
import menuLogoSlim from "~/assets/menu-logo-slim.svg";
import { Outlet, useLocation, useNavigate } from "react-router";
import { items } from "./scripts/menus";
import AppHeader from "./scripts/header";
import { findMenuItemByKey, useMenuContext } from "~/features/menu/context";
import type { ItemType } from "antd/es/menu/interface";
const { Header, Content, Footer, Sider } = Layout;


export const VerticalDashboardLayout: React.FC = () => {
	const [collapsed, setCollapsed] = useState(false);
	const {
		token: { colorBgContainer, borderRadiusLG },
	} = theme.useToken();
	const location = useLocation();

	const selectedMenuItems: string[] = useMemo(() => {
		return [location.pathname];
	}, [location.pathname]);
	const navigate = useNavigate();
	const menuContext = useMenuContext()


	const handleMenuClick: MenuProps["onClick"] = ({ key }) => {
		navigate(key)
		menuContext.setSelectedMenus([findMenuItemByKey(items, key)] as ItemType[])
	};

	return (
		<Layout className="h-screen">
			<Sider
				trigger={null}
				collapsible
				width={250}
				collapsed={collapsed}
				className="h-screen overflow-y-auto scrollbar-hide"
			>
				<div className="demo-logo-vertical relative pb-1 pt-1">
					<div
						className={`absolute top-0.5 right-0.5 text-[#e8e8e8] cursor-pointer ${collapsed && "hidden"}`}
					>
						<VerticalRightOutlined
							onClick={() => setCollapsed(true)}
						/>
					</div>
					<img
						src={menuLogo}
						alt="React Router"
						className={`block w-full ${collapsed && "hidden"}`}
					/>
					<Tooltip title="Expand Menu">
						<img
							src={menuLogoSlim}
							alt="React Router"
							className={`block w-full ${collapsed || "hidden"} cursor-pointer`}
							onClick={() => setCollapsed(false)}
						/>
					</Tooltip>
				</div>
				<Menu
					theme="dark"
					selectedKeys={selectedMenuItems}
					defaultSelectedKeys={["/dashboard"]}
					mode="inline"
					items={items}
					onClick={handleMenuClick}
				/>
			</Sider>
			<Layout className="h-screen overflow-auto">
				<Header style={{ padding: 0, background: colorBgContainer }}>
					<AppHeader />
				</Header>
				<Content style={{ margin: "0 16px", paddingTop: "16px" }}>
					<div
						style={{
							padding: 24,
							minHeight: 360,
							background: colorBgContainer,
							borderRadius: borderRadiusLG,
						}}
					>
						<Outlet />
					</div>
				</Content>
				<Footer style={{ textAlign: "center" }}>
					Copyright ©{new Date().getFullYear()} Created by OpenGMS
				</Footer>
			</Layout>
		</Layout>
	);
};
