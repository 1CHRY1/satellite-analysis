import { message, Button, Form } from "antd";
import { EditOutlined, PlusOutlined } from "@ant-design/icons";
import React, { useEffect, useState } from "react";
import { SchemaForm } from "~/components/form/schema-form";
import type { FormSchema } from "~/components/type/form.type";
import { ProForm, ProFormSwitch, ProFormUploadButton } from "@ant-design/pro-components";
import type { Product } from "~/types/product";
import { updateProduct } from "~/apis/https/product/product.admin";
const UploadButton = ProFormUploadButton as unknown as React.FC<any>;
const editProduct = async (values: Product) => {
	console.log(values)
    const res = await updateProduct(values)
    if (res.status === 1) {
        message.success("操作成功")
        return true
    } else {
        message.warning(res.message)
        return false
    }
};

export const EditProductButton: React.FC<{
	onSuccess: () => void | Promise<any>;
    initProduct: Product;
	sensorOpts: {label: string, value: string}[];
}> = ({ onSuccess, initProduct, sensorOpts }) => {
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
			key={initProduct.productId}
			title="编辑产品"
			trigger={
				<Button type="link">
					编辑
				</Button>
			}
            initialValues={initProduct}
			schema={schema}
			onFinish={async(values) => {
				values.productId = initProduct.productId;
                const result = await editProduct(values)
                if (result) 
                    onSuccess()
                return result
            }}
		></SchemaForm>
	);
};
