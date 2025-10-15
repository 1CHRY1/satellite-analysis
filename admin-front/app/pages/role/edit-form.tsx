import { message, Button, Form } from "antd";
import { EditOutlined, PlusOutlined } from "@ant-design/icons";
import React, { useEffect, useState } from "react";
import { SchemaForm } from "~/components/form/schema-form";
import type { FormSchema } from "~/components/type/form.type";
import { ProForm, ProFormSwitch, ProFormUploadButton } from "@ant-design/pro-components";
import type { Role } from "~/types/role";
import { updateRole } from "~/apis/https/role/role.admin.api";
const UploadButton = ProFormUploadButton as unknown as React.FC<any>;
const editRole = async (values: Role) => {
	values.isSuperAdmin = values.isSuperAdmin ? 1 : 0 as number
	console.log(values)
    const res = await updateRole(values)
    if (res.status === 1) {
        message.success("操作成功")
        return true
    } else {
        message.warning(res.message)
        return false
    }
};

export const EditRoleButton: React.FC<{
	onSuccess: () => void | Promise<any>;
    initRole: Role;
}> = ({ onSuccess, initRole }) => {
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
						rules: [{ required: true, message: "请输入最大可用CPU核数" }],
					},
					{
						name: "maxStorage",
						label: "最大可用存储空间（GB）",
						type: "number",
						rules: [{ required: true, message: "请输入最大可用存储空间（GB）" }],
					},
					{
						name: "maxJob",
						label: "最大可同时运行任务数",
						type: "number",
						rules: [{ required: true, message: "请输入最大可同时运行任务数" }],
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
			key={initRole.roleId}
			title="编辑用户"
			trigger={
				<Button type="link">
					编辑
				</Button>
			}
            initialValues={initRole}
			schema={schema}
			onFinish={async(values) => {
                values.roleId = initRole.roleId;
                const result = await editRole(values)
                if (result) 
                    onSuccess()
                return result
            }}
		></SchemaForm>
	);
};
