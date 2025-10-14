import { createContext, useContext } from "react";
import type { MenuProps } from "antd";

interface MenuContextType {
    selectedMenus: MenuItem[]
    setSelectedMenus: React.Dispatch<React.SetStateAction<MenuItem[]>>
}

type MenuItem = Required<MenuProps>["items"][number];

/**
 * 根据 key 查找菜单项
 * @param menuItems 菜单数组
 * @param key 要查找的 key
 * @returns 找到的菜单项对象或 undefined
 */
export function findMenuItemByKey(menuItems: any[], key: string): any {
    console.log(menuItems)
	for (const item of menuItems) {
		if (!item) continue; // TS里 item 可能是 null

		// 当前项匹配
		if (item.key === key) return {label: item.label, key: item.key};

		// 如果有 children，则递归查找
		if ("children" in item && Array.isArray(item.children)) {
			const found = findMenuItemByKey(item.children, key);
			if (found) return {label: found.label, key: found.key};
		}
	}

	return undefined;
}

export const MenuContext = createContext<MenuContextType | undefined>(undefined)

export const useMenuContext = () => {
    const context = useContext(MenuContext)
    if (!context)
        throw new Error("useMenuContext must be used within a MenuProvider")
    return context
}