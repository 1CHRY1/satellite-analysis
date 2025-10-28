import { message, Button, Form } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import React, { useState } from "react";
import { SchemaForm } from "~/components/form/schema-form";
import type { FormSchema } from "~/components/type/form.type";
import { ProFormSwitch, ProFormUploadButton } from "@ant-design/pro-components";
import { addRole } from "~/apis/https/role/role.admin.api";
import type { Role } from "~/types/role";
const UploadButton = ProFormUploadButton as unknown as React.FC<any>;
const insertRole = async (values: Role) => {
	values.isSuperAdmin = values.isSuperAdmin ? 1 : (0 as number);
	const res = await addRole(values);
	if (res.status === 1) {
		message.success("操作成功");
		return true;
	} else {
		message.warning(res.message);
		return false;
	}
};

export const CreateRoleButton: React.FC<{
	onSuccess: () => void | Promise<any>;
}> = ({ onSuccess }) => {
	const schema: FormSchema = {
		groups: [
			{
				fields: [
					{
						name: "name",
						label: "角色名",
						type: "text",
						rules: [{ required: true, message: "请输入角色名" }],
					},
				],
			},
			{
				fields: [
					{
						name: "maxCpu",
						label: "最大可用CPU核数",
						type: "number",
						rules: [
							{
								required: true,
								message: "请输入最大可用CPU核数",
							},
						],
					},
					{
						name: "maxStorage",
						label: "最大可用存储空间（GB）",
						type: "number",
						rules: [
							{ required: true, message: "请输入最大可用存储空间（GB）" },
						],
					},
					{
						name: "maxJob",
						label: "最大可同时运行任务数",
						type: "number",
						rules: [
							{
								required: true,
								message: "请输入最大可同时运行任务数",
							},
						],
					},
				],
			},
			{
				fields: [
					{
						name: "description",
						label: "描述信息",
						type: "textarea",
					},
					{
						name: "isSuperAdmin",
						label: "是否为超级管理员",
						type: "custom",
						render: () => (
							<>
								<ProFormSwitch
									checkedChildren="是"
									unCheckedChildren="否"
									name="isSuperAdmin"
									label="是否为超级管理员"
								></ProFormSwitch>
							</>
						),
					},
				],
			},
		],
	};
	return (
		<SchemaForm<Role>
			mode="modal"
			title="新建角色"
			trigger={
				<Button type="primary" icon={<PlusOutlined></PlusOutlined>}>
					新建
				</Button>
			}
			schema={schema}
			onFinish={async (values) => {
				const result = await insertRole(values);
				if (result) onSuccess();
				return result;
			}}
		></SchemaForm>
	);
};
