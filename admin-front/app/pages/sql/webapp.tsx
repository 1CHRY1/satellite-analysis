import { ProDescriptions } from "@ant-design/pro-components";
import { WEBAPP_INFO_FIELDS } from "~/apis/https/sql/sql.type";
import { getAllWebAppStatInfo } from "~/apis/https/sql/sql.admin";
import type { WebAppStat } from "~/apis/https/sql/sql.type";

const DBInfoDisplay = () => {
	// 格式化值
	const formatValue = (key: string, value: any) => {
		if (value === null || value === undefined) return "null";
		if (Array.isArray(value)) return value.length ? value.join(", ") : "[]";
		if (typeof value === "boolean") return value ? "true" : "false";
		if (
			key.includes("Time") &&
			typeof value === "string" &&
			value.includes("T")
		) {
			return new Date(value).toLocaleString("zh-CN");
		}
		return String(value);
	};

	const getWebAppStat = async () => {
		const res = await getAllWebAppStatInfo();
        console.log(res)
		return {
			data: res[0], // TODO
			success: true,
		};
	};

	// 列配置
	const columns = Object.entries(WEBAPP_INFO_FIELDS).map(([key, label]) => {
		return {
			title: label ? label : key,
			dataIndex: key,
			render: (_: any, record: WebAppStat) =>
				formatValue(key, record?.[key as keyof WebAppStat]),
		};
	});

	return (
		<>
			<h2
				style={{
					marginBottom: "24px",
					fontSize: "20px",
					fontWeight: 600,
				}}
			>
				Web应用统计信息
			</h2>
			<ProDescriptions<WebAppStat>
				column={3}
				request={getWebAppStat}
				columns={columns}
				bordered
				size="small"
				emptyText="-"
			/>
		</>
	);
};

export default DBInfoDisplay;
