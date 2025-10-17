import { message, Button, Form } from "antd";
import { EditOutlined, PlusOutlined } from "@ant-design/icons";
import React, { useEffect, useState } from "react";
import { SchemaForm } from "~/components/form/schema-form";
import type { FormSchema } from "~/components/type/form.type";
import { ProForm, ProFormSwitch, ProFormUploadButton } from "@ant-design/pro-components";
import type { Sensor } from "~/types/sensor";
import { updateSensor } from "~/apis/https/sensor/sensor.admin";
const UploadButton = ProFormUploadButton as unknown as React.FC<any>;
const editSensor = async (values: Sensor) => {
	console.log(values)
    const res = await updateSensor(values)
    if (res.status === 1) {
        message.success("操作成功")
        return true
    } else {
        message.warning(res.message)
        return false
    }
};

export const EditSensorButton: React.FC<{
	onSuccess: () => void | Promise<any>;
    initSensor: Sensor;
}> = ({ onSuccess, initSensor }) => {
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
			key={initSensor.sensorId}
			title="编辑传感器"
			trigger={
				<Button type="link">
					编辑
				</Button>
			}
            initialValues={initSensor}
			schema={schema}
			onFinish={async(values) => {
                const result = await editSensor(values)
                if (result) 
                    onSuccess()
                return result
            }}
		></SchemaForm>
	);
};
