import { message, Button, Form } from "antd";
import { EditOutlined, PlusOutlined } from "@ant-design/icons";
import React, { useEffect, useState } from "react";
import { SchemaForm } from "~/components/form/schema-form";
import type { FormSchema } from "~/components/type/form.type";
import { ProForm, ProFormSwitch, ProFormUploadButton } from "@ant-design/pro-components";
import type { Scene } from "~/types/scene";
import { updateScene } from "~/apis/https/scene/scene.admin";
const UploadButton = ProFormUploadButton as unknown as React.FC<any>;
const editScene = async (values: Scene) => {
	console.log(values)
    const res = await updateScene(values)
    if (res.status === 1) {
        message.success("操作成功")
        return true
    } else {
        message.warning(res.message)
        return false
    }
};

export const EditSceneButton: React.FC<{
	onSuccess: () => void | Promise<any>;
    initScene: Scene;
	sensorOpts: {label: string, value: string}[];
}> = ({ onSuccess, initScene, sensorOpts }) => {
	const schema: FormSchema = {
		groups: [
			{
				fields: [
					{
						name: "sceneName",
						label: "遥感影像标识",
						type: "text",
						rules: [{ required: true, message: "请输入遥感影像标识" }],
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
		<SchemaForm<Scene>
			mode="modal"
			key={initScene.sceneId}
			title="编辑产品"
			trigger={
				<Button type="link">
					编辑
				</Button>
			}
            initialValues={initScene}
			schema={schema}
			onFinish={async(values) => {
				values.sceneId = initScene.sceneId;
                const result = await editScene(values)
                if (result) 
                    onSuccess()
                return result
            }}
		></SchemaForm>
	);
};
