import { message, Button, Form } from "antd";
import { EditOutlined, PlusOutlined } from "@ant-design/icons";
import React, { useEffect, useState } from "react";
import { SchemaForm } from "~/components/form/schema-form";
import type { FormSchema } from "~/components/type/form.type";
import { ProForm, ProFormSwitch, ProFormUploadButton } from "@ant-design/pro-components";
import type { Vector } from "~/types/vector";
import { updateVector } from "~/apis/https/vector/vector.admin";
const UploadButton = ProFormUploadButton as unknown as React.FC<any>;
const editVector = async (values: Vector) => {
	console.log(values)
    const res = await updateVector(values)
    if (res.status === 1) {
        message.success("操作成功")
        return true
    } else {
        message.warning(res.message)
        return false
    }
};

export const EditVectorButton: React.FC<{
	onSuccess: () => void | Promise<any>;
    initVector: Vector;
}> = ({ onSuccess, initVector }) => {
	const schema: FormSchema = {
		groups: [
			{
				fields: [
					{
						name: "vectorName",
						label: "矢量名称",
						type: "text",
						rules: [{ required: true, message: "请输入矢量名称" }],
					},
                    {
						name: "srid",
						label: "坐标系（EPSG）",
						type: "text",
						rules: [{ required: true, message: "请输入坐标系（EPSG）" }],
					},
				],
			},
		],
	};
	return (
		<SchemaForm<Vector>
			mode="modal"
			key={initVector.id}
			title="编辑矢量数据集"
			trigger={
				<Button type="link">
					编辑
				</Button>
			}
            initialValues={initVector}
			schema={schema}
			onFinish={async(values) => {
				values.id = initVector.id;
                const result = await editVector(values)
                if (result) 
                    onSuccess()
                return result
            }}
		></SchemaForm>
	);
};
