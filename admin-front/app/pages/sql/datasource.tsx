import { ProDescriptions } from "@ant-design/pro-components";
import { Collapse, Divider, Tooltip } from "antd";
import { QuestionCircleOutlined } from "@ant-design/icons";
import { useEffect, useState } from "react";
import { DB_INFO_FIELDS } from "~/apis/https/sql/sql.type";
import { getAllDBInfo } from "~/apis/https/sql/sql.admin";
import type { DBInfo } from "~/apis/https/sql/sql.type";

const DBInfoDisplay = () => {
	const [dbList, setDbList] = useState<DBInfo[]>([]);

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

	// 获取所有 DB
	useEffect(() => {
		getAllDBInfo().then((res) => {
			if (Array.isArray(res)) {
				setDbList(res);
			}
		});
	}, []);

	// 列配置
	const columns = Object.entries(DB_INFO_FIELDS).map(([key, config]) => {
		const { label, remark } = config;

		return {
			title: remark ? (
				<span>
					{label}{" "}
					<Tooltip title={remark}>
						<QuestionCircleOutlined
							style={{ color: "#1890ff", cursor: "help" }}
						/>
					</Tooltip>
				</span>
			) : (
				label
			),
			dataIndex: key,
			render: (_: any, record: DBInfo) =>
				formatValue(key, record?.[key as keyof DBInfo]),
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
				数据库连接池信息
			</h2>
			{dbList.map((db, index) => (
				<div key={index}>
					<Divider orientation="left">{db.Name}</Divider>
					<Collapse
						items={[
							{
								key: String(index),
								label: `数据源${index + 1} - ${db.URL}`,
								children: (
									<ProDescriptions<DBInfo>
										column={2}
										dataSource={db}
										columns={columns}
										bordered
										size="small"
										emptyText="-"
									/>
								),
							},
						]}
					/>
				</div>
			))}
		</>
	);
};

export default DBInfoDisplay;
