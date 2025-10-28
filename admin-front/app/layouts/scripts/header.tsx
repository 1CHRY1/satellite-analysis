import React, { useEffect } from "react";
import { Avatar, Dropdown, Space, type MenuProps } from "antd";
import {
	GithubFilled,
	InfoCircleFilled,
	QuestionCircleFilled,
	LogoutOutlined,
	ArrowLeftOutlined,
	UserOutlined,
} from "@ant-design/icons";
import { useMenuContext } from "~/features/menu/context";
import { useUserContext } from "~/features/user/provider";
// import { css } from '@emotion/css';

const AppHeader: React.FC = () => {
	const menuContext = useMenuContext();
	const userContext = useUserContext();

	useEffect(() => {
		console.log(111, userContext.state);
	});

	const items: MenuProps["items"] = [
		{
			key: "profile",
			icon: <UserOutlined />,
			label: "个人中心",
			onClick: () => console.log("跳转到个人中心"),
		},
		{
			type: "divider",
		},
		{
			key: "logout",
			icon: <LogoutOutlined />,
			label: "退出登录",
			onClick: () => {
				userContext.dispatch({ type: "LOGOUT" });
				localStorage.removeItem("token");
				localStorage.removeItem("refreshToken");
				localStorage.removeItem("userId");
				window.location.href = "/login"
			},
		},
	];

	return (
		<div className="flex space-between">
			{/* 左侧 Logo + 标题 */}

			<div
				style={{
					display: "flex",
					alignItems: "center",
					gap: 8,
					marginLeft: 21,
					fontSize: 18,
					fontWeight: 500,
				}}
			>
				<ArrowLeftOutlined onClick={() => window.history.back()} />

				<span>{(menuContext.selectedMenus[0] as any)?.label}</span>
			</div>

			{/* 中间区域可放 MenuCard */}
			<div style={{ flex: 1, textAlign: "center" }}>
				{/* 这里可以插入 MenuCard */}
			</div>

			{/* 右侧操作按钮 */}
			<div
				style={{
					display: "flex",
					alignItems: "center",
					gap: 16,
					marginRight: 21,
				}}
			>
				<InfoCircleFilled />
				<QuestionCircleFilled />
				<GithubFilled />
				<Dropdown menu={{ items }} placement="bottomRight">
					<Space style={{ cursor: "pointer", userSelect: "none" }}>
						<Avatar
							src="https://gw.alipayobjects.com/zos/antfincdn/efFD%24IOql2/weixintupian_20170331104822.jpg"
						/>
						<span style={{ fontWeight: 500 }}>
							{userContext.state.user.name}
						</span>
					</Space>
				</Dropdown>
			</div>
		</div>
	);
};

export default AppHeader;
