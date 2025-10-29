import React, { useEffect } from "react";
import {
	ProForm,
	ProFormText,
	ProFormSelect,
	ProFormDigit,
	ProFormDatePicker,
	ProFormGroup,
	ModalForm,
	DrawerForm,
    ProFormUploadButton,
    ProFormTextArea,
} from "@ant-design/pro-components";
import type { User } from "~/types/user";
import type {
	FormSchema,
	FormField,
	SchemaFormProps,
} from "~/components/type/form.type";

const componentMap: Record<string, any> = {
	text: ProFormText,
    password: (props: any) => <ProFormText.Password {...props} />,
	number: ProFormDigit,
	select: ProFormSelect,
	date: ProFormDatePicker,
    textarea: ProFormTextArea,
};

export const SchemaForm = <T,>({
	schema,
	formRef,
	initialValues,
	onFinish,
	onValuesChange,
	mode = "inline",
	trigger,
	title,
}: SchemaFormProps<T>) => {
	const [form] = ProForm.useForm();

	useEffect(() => {
		if (formRef) {
		  formRef.current = form;
		}
	}, [form, formRef]);

	const renderField = (field: FormField): React.ReactNode => {
		const {
			name,
			label,
			type,
			placeholder,
			rules,
			options,
			render,
			defaultValue,
		} = field;
		const commonProps = { name, label, placeholder, rules };
		if (type === "custom" && render) {
			return <React.Fragment key={name}>{render(form)}</React.Fragment>;
		}
		const Comp = componentMap[type];
		if (!Comp) return null;
		return (
			<Comp
				key={name}
				{...commonProps}
				initialValues={defaultValue}
				options={options}
			></Comp>
		);
	};

	const renderContent = () => (
		<>
			{schema.groups?.map((group, idx) => (
				<ProFormGroup key={idx} title={group.title}>
					{group.fields.map(renderField)}
				</ProFormGroup>
			))}
		</>
	);
	if (mode === "inline") {
		return (
			<ProForm
				form={form}
				initialValues={initialValues}
				onFinish={onFinish}
				onValuesChange={onValuesChange}
				submitter={{
					searchConfig: { submitText: "提交" },
				}}
			>
				{renderContent()}
			</ProForm>
		);
	} else if (mode === "modal") {
		return (
			<ModalForm
				form={form}
				title={title}
				trigger={trigger}
				// width={width}
				onFinish={onFinish}
				onValuesChange={onValuesChange}
				initialValues={initialValues}
                onOpenChange={(visible) => {
                    if (visible) {
                        // 每次打开弹窗时都更新表单值
                        form.setFieldsValue(initialValues);
                    }
                }}
				modalProps={{
					destroyOnClose: true,
				}}
			>
				{renderContent()}
			</ModalForm>
		);
	} else if (mode === "drawer") {
		return (
			<DrawerForm
				form={form}
				title={title}
				trigger={trigger}
				// width={width}
				onFinish={onFinish}
				onValuesChange={onValuesChange}
				initialValues={initialValues}
                onOpenChange={(visible) => {
                    if (visible) {
                        // 每次打开弹窗时都更新表单值
                        form.setFieldsValue(initialValues);
                    }
                }}
				drawerProps={{
					destroyOnClose: true,
				}}
			>
				{renderContent()}
			</DrawerForm>
		);
	}
};
