import { message, Button, Form, Tooltip } from "antd";
import { EditOutlined, PlusOutlined } from "@ant-design/icons";
import React, { useEffect, useMemo, useRef, useState } from "react";
import { SchemaForm } from "~/components/form/schema-form";
import type { FormSchema } from "~/components/type/form.type";
import {
	ProForm,
	ProFormSelect,
	ProFormSwitch,
	ProFormUploadButton,
	type ProFormInstance,
} from "@ant-design/pro-components";
import type { Scene } from "~/types/scene";
import { updateScene } from "~/apis/https/scene/scene.admin";
import dayjs from "dayjs";
import { getProductPage } from "~/apis/https/product/product.admin";
import type { Image } from "~/types/image";
import { updateImage } from "~/apis/https/image/image.admin";
const UploadButton = ProFormUploadButton as unknown as React.FC<any>;
const editImage = async (values: Image) => {
	console.log(values);
	const res = await updateImage(values);
	if (res.status === 1) {
		message.success("操作成功");
		return true;
	} else {
		message.warning(res.message);
		return false;
	}
};

export const EditImageButton: React.FC<{
	onSuccess: () => void | Promise<any>;
	initImage: Image;
}> = ({ onSuccess, initImage }) => {
	const [productOpts, setProductOpts] = useState<
		{ label: string; value: string }[]
	>([]);
	const formRef = useRef<ProFormInstance>(undefined);

	const schema: FormSchema = {
		groups: [
			{
				fields: [
					{
						name: "band",
						label: "波段",
						type: "digit",
						rules: [
							{ required: true, message: "请输入波段" },
						],
					},
					{
						name: "bucket",
						label: "桶",
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
						rules: [{ required: true, message: "请选择时间" }],
					},
					{
						name: "coordinateSystem",
						label: "坐标系",
						type: "text",
						rules: [
							{ required: true, message: "请输入坐标系（EPSG）" },
						],
					},
					{
						name: "cloud",
						label: "云量",
						type: "number",
						rules: [
							{ required: true, message: "请输入云量值" },
							{
								validator: (_, value) => {
									if (
										value === undefined ||
										value === null ||
										value === ""
									) {
										return Promise.resolve(); // 交给 required 去处理
									}
									if (
										Number(value) < 0 ||
										Number(value) > 100
									) {
										return Promise.reject(
											new Error("云量值需在 0~100 之间"),
										);
									}
									return Promise.resolve();
								},
							},
						],
					},
					{
						name: "noData",
						label: "NoData",
						type: "number",
						rules: [{ required: true, message: "请输入NoData值" }],
					},
					{
						name: "tags",
						label: "数据标签",
						type: "custom",
						render: () => (
							<>
								<ProFormSelect
									name={["tags", "source"]}
									label="来源"
									options={[
										{ label: "国内", value: "national" },
										{
											label: "国外",
											value: "international",
										},
									]}
									rules={[
										{
											required: true,
											message: "请选择来源",
										},
									]}
								/>
								<ProFormSelect
									name={["tags", "category"]}
									label="类别"
									options={[
										{ label: "ARD", value: "ard" },
										{ label: "传统", value: "traditional" },
									]}
									rules={[
										{
											required: true,
											message: "请选择类别",
										},
									]}
								/>
								<ProFormSelect
									name={["tags", "production"]}
									label="成像类型"
									options={[
										{ label: "光学", value: "light" },
										{ label: "雷达", value: "radar" },
									]}
									rules={[
										{
											required: true,
											message: "请选择成像类型",
										},
									]}
								/>
							</>
						),
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
				],
			},
		],
	};
	// const schema: FormSchema = {
	// 	groups: [
	// 		{
	// 			fields: [
	// 				{
	// 					name: "sceneName",
	// 					label: "遥感影像标识",
	// 					type: "text",
	// 					rules: [
	// 						{ required: true, message: "请输入遥感影像标识" },
	// 					],
	// 				},
	// 				{
	// 					name: "sensorId",
	// 					label: "所属传感器",
	// 					type: "select",
	// 					rules: [
	// 						{ required: true, message: "请选择所属传感器" },
	// 					],
	// 					options: sensorOpts,
	// 				},
	// 				{
	// 					name: "productId",
	// 					label: "所属产品",
	// 					type: "select",
	// 					rules: [
	// 						{ required: true, message: "请选择所属传感器" },
	// 					],
	// 					options: productOpts,
	// 				},
	// 			],
	// 		},
	// 		{
	// 			fields: [
	// 				{
	// 					name: "sceneTime",
	// 					label: "时间",
	// 					type: "date",
	// 					rules: [{ required: true, message: "请选择时间" }],
	// 				},
	// 				{
	// 					name: "coordinateSystem",
	// 					label: "坐标系",
	// 					type: "text",
	// 					rules: [
	// 						{ required: true, message: "请输入坐标系（EPSG）" },
	// 					],
	// 				},
	// 				{
	// 					name: "cloud",
	// 					label: "云量",
	// 					type: "number",
	// 					rules: [
	// 						{ required: true, message: "请输入云量值" },
	// 						{
	// 							validator: (_, value) => {
	// 								if (
	// 									value === undefined ||
	// 									value === null ||
	// 									value === ""
	// 								) {
	// 									return Promise.resolve(); // 交给 required 去处理
	// 								}
	// 								if (
	// 									Number(value) < 0 ||
	// 									Number(value) > 100
	// 								) {
	// 									return Promise.reject(
	// 										new Error("云量值需在 0~100 之间"),
	// 									);
	// 								}
	// 								return Promise.resolve();
	// 							},
	// 						},
	// 					],
	// 				},
	// 				{
	// 					name: "noData",
	// 					label: "NoData",
	// 					type: "number",
	// 					rules: [{ required: true, message: "请输入NoData值" }],
	// 				},
	// 				{
	// 					name: "tags",
	// 					label: "数据标签",
	// 					type: "custom",
	// 					render: () => (
	// 						<>
	// 							<ProFormSelect
	// 								name={["tags", "source"]}
	// 								label="来源"
	// 								options={[
	// 									{ label: "国内", value: "national" },
	// 									{
	// 										label: "国外",
	// 										value: "international",
	// 									},
	// 								]}
	// 								rules={[
	// 									{
	// 										required: true,
	// 										message: "请选择来源",
	// 									},
	// 								]}
	// 							/>
	// 							<ProFormSelect
	// 								name={["tags", "category"]}
	// 								label="类别"
	// 								options={[
	// 									{ label: "ARD", value: "ard" },
	// 									{ label: "传统", value: "traditional" },
	// 								]}
	// 								rules={[
	// 									{
	// 										required: true,
	// 										message: "请选择类别",
	// 									},
	// 								]}
	// 							/>
	// 							<ProFormSelect
	// 								name={["tags", "production"]}
	// 								label="成像类型"
	// 								options={[
	// 									{ label: "光学", value: "light" },
	// 									{ label: "雷达", value: "radar" },
	// 								]}
	// 								rules={[
	// 									{
	// 										required: true,
	// 										message: "请选择成像类型",
	// 									},
	// 								]}
	// 							/>
	// 						</>
	// 					),
	// 				},
	// 			],
	// 		},
	// 		{
	// 			fields: [
	// 				{
	// 					name: "description",
	// 					label: "描述信息",
	// 					type: "textarea",
	// 				},
	// 			],
	// 		},
	// 	],
	// };
	return (
		<SchemaForm<Scene>
			formRef={formRef}
			mode="modal"
			key={initScene.sceneId}
			title="编辑"
			trigger={
				<Tooltip title="编辑">
					<Button
						type="dashed"
						shape="circle"
						onClick={() => {
							getAllProduct([initScene.sensorId]);
						}} // onClick可以同时作为初始化和trigger
						icon={<EditOutlined />}
					/>
				</Tooltip>
			}
			initialValues={initScene}
			schema={schema}
			onFinish={async (values) => {
				values.sceneId = initScene.sceneId;
				values.sceneTime = dayjs(values.sceneTime).format(
					"YYYY-MM-DDTHH:mm:ss",
				);
				console.log(values);
				const result = await editScene(values);
				if (result) onSuccess();
				return result;
			}}
			onValuesChange={(changedVals, vals) => {
				if (changedVals.sensorId) {
					formRef.current?.setFieldValue("productId", undefined);
					// 有值时才请求产品
					getAllProduct([changedVals.sensorId]);
				}
			}}
		></SchemaForm>
	);
};
