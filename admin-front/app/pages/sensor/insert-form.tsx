import { message, Button, Form } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import React, { useState } from "react";
import { SchemaForm } from "~/components/form/schema-form";
import type { FormSchema } from "~/components/type/form.type";
import { ProFormSwitch, ProFormUploadButton } from "@ant-design/pro-components";
import { addSensor } from "~/apis/https/sensor/sensor.admin";
import type { Sensor } from "~/types/sensor";
const UploadButton = ProFormUploadButton as unknown as React.FC<any>;
const insertSensor = async (values: Sensor) => {
	const res = await addSensor(values);
	if (res.status === 1) {
		message.success("操作成功");
		return true;
	} else {
		message.warning(res.message);
		return false;
	}
};

export const CreateSensorButton: React.FC<{
	onSuccess: () => void | Promise<any>;
}> = ({ onSuccess }) => {
	const schema: FormSchema = {
		groups: [
			{
				fields: [
					{
						name: "sensorName",
						label: "传感器ID",
						type: "text",
						rules: [{ required: true, message: "请输入传感器ID" }],
					},
                    {
						name: "platformName",
						label: "传感器名称",
						type: "text",
						rules: [{ required: true, message: "请输入传感器名称" }],
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
						name: "dataType",
						label: "数据类型",
						type: "select",
						options: [
                            {label: '遥感影像', value: 'satellite'},
                            {label: '红绿立体影像', value: '3d'},
                            {label: 'DEM产品', value: 'dem'},
                            {label: 'DSM产品', value: 'dsm'},
                            {label: '形变速率产品', value: 'svr'},
                            {label: 'NDVI产品', value: 'ndvi'},
                        ]
					},
				],
			},
		],
	};
	return (
		<SchemaForm<Sensor>
			mode="modal"
			title="新建传感器"
			trigger={
				<Button type="primary" icon={<PlusOutlined></PlusOutlined>}>
					新建
				</Button>
			}
			schema={schema}
			onFinish={async (values) => {
				const result = await insertSensor(values);
				if (result) onSuccess();
				return result;
			}}
		></SchemaForm>
	);
};
