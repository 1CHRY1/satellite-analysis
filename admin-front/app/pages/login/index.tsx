import {
	LockOutlined,
	UserOutlined,
} from "@ant-design/icons";
import {
	LoginFormPage,
	ProConfigProvider,
	ProFormCheckbox,
	ProFormText,
} from "@ant-design/pro-components";
import { theme } from "antd";

const Page = () => {
	const { token } = theme.useToken();
	return (
		<div
			style={{
				backgroundColor: "white",
				height: "100vh",
			}}
		>
			<LoginFormPage
				logo="/satellite.svg"
				backgroundVideoUrl="/bgVideo.mp4"
				title="ARD后台管理"
				containerStyle={{
					backgroundColor: "rgba(0, 0, 0,0.65)",
					backdropFilter: "blur(4px)",
				}}
                onFinish={(values) => {console.log(values)}}
				subTitle="专注多源遥感应用支撑云平台后台"
				activityConfig={{
					style: {
						// boxShadow: "0px 0px 8px rgba(0, 0, 0, 0.2)",
						color: token.colorTextHeading,
						borderRadius: 8,
						// backgroundColor: "rgba(255,255,255,0.25)",
						// backdropFilter: "blur(4px)",
                        position: 'absolute',
                        top: '30%',
                        left: '5%',
                        right: '40%'
					},
					title: (
                        <>
                            <span className="text-5xl">
                                全链路遥感应用管理中枢
                            </span>
                        </>
                    ),
					subTitle: (
                        <>
                            <span className="text-sm">
                            本系统作为多源遥感应用支撑云平台的后台管理核心，覆盖从卫星数据接入、处理、分析到成果发布的完整业务链路，实现数据、模型、工具和用户操作的统一管控与调度。通过集中化管理和可视化监控，平台不仅提升了遥感数据处理效率，也确保了各环节的协同与安全，为科研、决策和应用提供稳定、高效的支撑。
                            </span>
                        </>
                    ),
                    
				}}
			>
				<>
                    <ProFormText
                        name="username"
                        fieldProps={{
                            size: "large",
                            prefix: (
                                <UserOutlined
                                    style={{
                                        color: token.colorText,
                                    }}
                                    className={"prefixIcon"}
                                />
                            ),
                        }}
                        placeholder={"邮箱"}
                        rules={[
                            {
                                required: true,
                                message: "请输入邮箱!",
                            },
                        ]}
                    />
                    <ProFormText.Password
                        name="password"
                        fieldProps={{
                            size: "large",
                            prefix: (
                                <LockOutlined
                                    style={{
                                        color: token.colorText,
                                    }}
                                    className={"prefixIcon"}
                                />
                            ),
                        }}
                        placeholder={"密码"}
                        rules={[
                            {
                                required: true,
                                message: "请输入密码！",
                            },
                        ]}
                    />
                </>
				<div
					style={{
						marginBlockEnd: 24,
					}}
				>
					<ProFormCheckbox noStyle name="autoLogin">
						自动登录
					</ProFormCheckbox>
					<a
                        href="dashboard"
						style={{
							float: "right",
						}}
					>
						忘记密码
					</a>
				</div>
			</LoginFormPage>
		</div>
	);
};

export default function App () {
	return (
		<ProConfigProvider dark>
			<Page />
		</ProConfigProvider>
	);
};
