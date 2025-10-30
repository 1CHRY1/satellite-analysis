import { useState } from "react";
import { Popover, InputNumber, Button, Form, message, Space } from "antd";
import type { Image } from "~/types/image";
import { updateImage } from "~/apis/https/image/image.admin";
import type { Scene } from "~/types/scene";
import { updateScene } from "~/apis/https/scene/scene.admin";
import { getSceneImages } from "~/pages/scene/common";

const editImage = async (values: Image, scene: Scene) => {
	console.log(values);
	const res = await updateImage(values);
	if (res.status !== 1) {
		return false;
	}
    const {data: images} = await getSceneImages(scene.sceneId);
    
    scene.bands = images.map(image => image.band.toString());
    scene.bandNum = images.length;

	const sceneRes = await updateScene(scene);
	if (sceneRes.status === 1) {
		message.success("编辑成功");
		return true;
	} else {
		message.warning(sceneRes.message);
		return false;
	}
};

export const BandEdit: React.FC<{
	record: Image;
    scene: Scene;
    onSuccess: () => void;
}> = ({ record, scene, onSuccess }) => {
	const [visible, setVisible] = useState(false);
	const [form] = Form.useForm();

	return (
		<Popover
			title="修改波段"
			trigger="click"
			open={visible}
			onOpenChange={(v) => {
				setVisible(v);
				if (!v) form.resetFields();
			}}
			content={
				<Form
					form={form}
					initialValues={{ band: record.band }}
					layout="inline"
					onFinish={async ({band}) => {
                        record.band = band
						await editImage(record, scene);
						setVisible(false);
                        onSuccess()
					}}
					style={{ margin: 0 }}
				>
					<Form.Item
						name="band"
						style={{ marginBottom: 0 }}
						rules={[{ required: true, message: "请输入波段号" }]}
					>
						<InputNumber
							size="small"
                            precision={0}
							min={1}
							style={{ width: 80 }}
						/>
					</Form.Item>
					<Form.Item style={{ marginBottom: 0 }}>
						<Space size={4}>
							<Button
								type="primary"
								size="small"
								htmlType="submit"
							>
								确定
							</Button>
							<Button
								size="small"
								onClick={() => {
									setVisible(false);
									form.resetFields();
								}}
							>
								取消
							</Button>
						</Space>
					</Form.Item>
				</Form>
			}
			overlayInnerStyle={{
				padding: 8,
				borderRadius: 6,
			}}
		>
			<Button type="link" size="small">
				编辑
			</Button>
		</Popover>
	);
};
