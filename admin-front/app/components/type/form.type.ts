import type { Rule } from "antd/es/form";

export interface FormSchema {
	groups?: FormGroup[];
}

export interface FormGroup {
	title?: string;
	fields: FormField[];
}

export interface FormField {
	name: string;
	label: string;
	type: string; // text, number, select, date, custom...
	placeholder?: string;
	options?: { label: string; value: any }[];
	rules?: (Rule | RegExpRule)[];
	defaultValue?: any;
	render?: (form: any) => React.ReactNode; // for custom rendering
}

export interface RegExpRule {
	pattern: RegExp;
	message: string;
}

export interface SchemaFormProps<T> {
	schema: FormSchema;
	initialValues?: Record<string, any>;
	onFinish?: (formData: T) => Promise<any>;
	mode?: "inline" | "modal" | "drawer";
	trigger?: React.ReactNode; // 用于 modal/drawer 触发按钮
	title?: string;
	// width?: number | string;
}
