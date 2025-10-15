import { message, Button, Form } from "antd";
import { EditOutlined, PlusOutlined } from "@ant-design/icons";
import React, { useEffect, useState } from "react";
import type { User } from "~/types/user";
import { SchemaForm } from "~/components/form/schema-form";
import type { FormSchema } from "~/components/type/form.type";
import { ProFormUploadButton } from "@ant-design/pro-components";
import { updateUser } from "~/apis/https/user/user.admin.api";
import Password from "antd/es/input/Password";
const UploadButton = ProFormUploadButton as unknown as React.FC<any>;
const editUser = async (values: User) => {
    const res = await updateUser(values)
    if (res.status === 1) {
        message.success("操作成功")
        return true
    } else {
        message.warning(res.message)
        return false
    }
};

export const EditUserButton: React.FC<{
	onSuccess: () => void | Promise<any>;
	roleEnum: Record<number, { text: string }>;
    initUser: User;
}> = ({ onSuccess, roleEnum, initUser }) => {
	const roleOpts = Object.entries(roleEnum).map((role) => ({
		label: role[1].text,
		value: Number(role[0]),
	}));
    
	const schema: FormSchema = {
		groups: [
			{
				fields: [
					{
						name: "userId",
						label: "用户ID",
                        type: "custom",
                        render: () => (<></>),
					},
                    {
						name: "userName",
						label: "用户名",
						type: "text",
						rules: [{ required: true, message: "请输入用户名" }],
					},
					{
						name: "title",
						label: "称谓",
						type: "select",
						options: [
							{ label: "Mr", value: "Mr" },
							{ label: "Mrs", value: "Mrs" },
							{ label: "Ms", value: "Ms" },
							{ label: "Miss", value: "Miss" },
							{ label: "Mx", value: "Mx" },
							{ label: "Dr", value: "Dr" },
							{ label: "Prof", value: "Prof" },
						],
					},
					{
						name: "roleId",
						label: "角色",
						type: "select",
						options: roleOpts,
                        rules: [{ required: true, message: "请选择角色" }],
					},
				],
			},
			{
				fields: [
					{
						name: "password",
						label: "密码",
						type: "password",
						rules: [
							{ required: true, message: "请输入密码" },
							// {
							// 	pattern: /^[a-zA-Z0-9]{6,12}$/,
							// 	message: "密码需为6-12位字母或数字",
							// },
						],
					},
					{
						name: "confirmPassword",
						label: "确认密码",
						type: "password",
						rules: [
							{ required: true, message: "请再次输入密码" },
							({ getFieldValue }: any) => ({
								validator(_: any, value: string) {
									if (
										!value ||
										getFieldValue("password") === value
									) {
										return Promise.resolve();
									}
									return Promise.reject(
										new Error("两次输入的密码不一致"),
									);
								},
							}),
						],
					},
				],
			},
			{
				fields: [
					{
						name: "email",
						label: "邮箱",
						type: "text",
						rules: [{ type: "email", message: "邮箱格式错误" },
                            { required: true, message: "请输入邮箱" },
                        ],
					},
					{
						name: "phone",
						label: "电话",
						type: "text",
						rules: [
							{
								pattern: /^1\d{10}$/,
								message: "请输入正确手机号",
							},
						],
					},
                    {
						name: "organization",
						label: "组织",
						type: "text",
					},
				],
			},
			{
				fields: [
					{
						name: "province",
						label: "省",
						type: "text",
					},
					{
						name: "city",
						label: "市",
						type: "text",
					},
				],
			},
			{
				fields: [
					{
						name: "introduction",
						label: "介绍",
						type: "textarea",
					},
				],
			},
			/*{
				fields: [
					{
						name: "avartarPath",
						label: "头像",
						type: "custom",
						render: (form) => (
							<UploadButton
								name="avatarPath"
								label="头像"
								max={1}
								fieldProps={{
									action: "/api/upload",
									onChange: (info: any) => {
										// 你可以直接访问 form 实例
										const fileUrl = info.file.response?.url;
										if (fileUrl)
											form.setFieldValue(
												"avatarPath",
												fileUrl,
											);
									},
								}}
							/>
						),
					},
				],
			},*/
		],
	};
	return (
		<SchemaForm
			mode="modal"
			title="编辑用户"
			trigger={
				<Button type="link">
					编辑
				</Button>
			}
            initialValues={initUser}
			schema={schema}
			onFinish={async(values) => {
                values.userId = initUser.userId;
                const result = await editUser(values)
                if (result) 
                    onSuccess()
                return result
            }}
		></SchemaForm>
	);
};
