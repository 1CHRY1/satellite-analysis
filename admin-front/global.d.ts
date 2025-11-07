// 如果你使用 CSS Modules
declare module "*.module.css" {
	const classes: { readonly [key: string]: string };
	export default classes;
}
