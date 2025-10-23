import { message, Button, Form } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import React, { useEffect, useState } from "react";
import { SchemaForm } from "~/components/form/schema-form";
import type { FormSchema } from "~/components/type/form.type";
import { ProFormSwitch, ProFormUploadButton } from "@ant-design/pro-components";
import { addProduct } from "~/apis/https/product/product.admin";
import type { Product } from "~/types/product";
const UploadButton = ProFormUploadButton as unknown as React.FC<any>;
const insertProduct = async (values: Product) => {
	const res = await addProduct(values);
	if (res.status === 1) {
		message.success("操作成功");
		return true;
	} else {
		message.warning(res.message);
		return false;
	}
};

export const CreateProductButton: React.FC<{
	onSuccess: () => void | Promise<any>;
    sensorOpts: {label: string, value: string}[]
}> = ({ onSuccess, sensorOpts }) => {
	useEffect(() => {
		console.log(sensorOpts)
	})
	const schema: FormSchema = {
		groups: [
			{
				fields: [
					{
						name: "productName",
						label: "产品名",
						type: "text",
						rules: [{ required: true, message: "请输入产品名" }],
					},
					{
						name: "sensorId",
						label: "所属传感器",
						type: "select",
						rules: [{ required: true, message: "请选择所属传感器" }],
						options: sensorOpts
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
						name: "resolution",
						label: "像元分辨率",
						type: "text",
						rules: [{ required: true, message: "请输入像元分辨率" },
							{ pattern: /^\d+m$/, message: "请输入正确格式，例如 30m" }]
					},
					{
						name: "period",
						label: "周期",
						type: "text",
						rules: [{ pattern: /^\d+d$/, message: "请输入正确格式，例如 30d" }]
					},
				],
			},
		],
	};
	return (
		<SchemaForm<Product>
			mode="modal"
			title="新建产品"
			trigger={
				<Button type="primary" icon={<PlusOutlined></PlusOutlined>}>
					新建
				</Button>
			}
			schema={schema}
			onFinish={async (values) => {
				const result = await insertProduct(values);
				if (result) onSuccess();
				return result;
			}}
		></SchemaForm>
	);
};
