import { message, Button, Form, Tooltip } from "antd";
import { EditOutlined, PlusOutlined } from "@ant-design/icons";
import React, { useEffect, useState } from "react";
import { SchemaForm } from "~/components/form/schema-form";
import type { FormSchema } from "~/components/type/form.type";
import {
	ProForm,
	ProFormSwitch,
	ProFormUploadButton,
} from "@ant-design/pro-components";
import type { Scene } from "~/types/scene";
import { updateScene } from "~/apis/https/scene/scene.admin";
const UploadButton = ProFormUploadButton as unknown as React.FC<any>;
const editScene = async (values: Scene) => {
	console.log(values);
	const res = await updateScene(values);
	if (res.status === 1) {
		message.success("操作成功");
		return true;
	} else {
		message.warning(res.message);
		return false;
	}
};

export const EditSceneButton: React.FC<{
	onSuccess: () => void | Promise<any>;
	initScene: Scene;
	sensorOpts: { label: string; value: string }[];
	productOpts: { label: string; value: string }[];
}> = ({ onSuccess, initScene, sensorOpts, productOpts }) => {
	const schema: FormSchema = {
		groups: [
			{
				fields: [
					{
						name: "sceneName",
						label: "遥感影像标识",
						type: "text",
						rules: [
							{ required: true, message: "请输入遥感影像标识" },
						],
					},
					{
						name: "sensorId",
						label: "所属传感器",
						type: "select",
						rules: [
							{ required: true, message: "请选择所属传感器" },
						],
						options: sensorOpts,
					},
					{
						name: "productId",
						label: "所属产品",
						type: "select",
						rules: [
							{ required: true, message: "请选择所属传感器" },
						],
						options: productOpts,
					},
				],
			},
			{
				fields: [
					{
						name: "sceneTime",
						label: "时间",
						type: "date",
					},
					{
						name: "coordinateSystem",
						label: "坐标系",
						type: "text",
					},
					{
						name: "cloud",
						label: "云量",
						type: "digit",
					},
					{
						name: "noData",
						label: "NoData",
						type: "digit",
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
				]
			}
		],
	};
	return (
		<SchemaForm<Scene>
			mode="modal"
			key={initScene.sceneId}
			title="编辑"
			trigger={
				<Tooltip title="编辑">
					<Button
						type="dashed"
						shape="circle"
						icon={<EditOutlined />}
					/>
				</Tooltip>
			}
			initialValues={initScene}
			schema={schema}
			onFinish={async (values) => {
				values.sceneId = initScene.sceneId;
				const result = await editScene(values);
				if (result) onSuccess();
				return result;
			}}
		></SchemaForm>
	);
};
