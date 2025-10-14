import React from "react";
import { Dropdown } from "antd";
import {
	GithubFilled,
	InfoCircleFilled,
	QuestionCircleFilled,
	LogoutOutlined,
    ArrowLeftOutlined,
} from "@ant-design/icons";
import { useMenuContext } from "~/features/menu/context";
// import { css } from '@emotion/css';

const UserMenu = () => (
	<Dropdown
		menu={{
			items: [
				{
					key: "logout",
					icon: <LogoutOutlined />,
					label: "退出登录",
				},
			],
		}}
	>
		<img
			src="https://gw.alipayobjects.com/zos/antfincdn/efFD%24IOql2/weixintupian_20170331104822.jpg"
			alt="avatar"
			style={{
				width: 32,
				height: 32,
				borderRadius: "50%",
				cursor: "pointer",
			}}
		/>
	</Dropdown>
);


const AppHeader: React.FC = () => {
    const menuContext = useMenuContext()
    
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
                <ArrowLeftOutlined onClick={() => window.history.back()}/>
				
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
				<UserMenu />
			</div>
		</div>
	);
};

export default AppHeader;
