import { useEffect, useState } from "react"
import { findMenuItemByKey, MenuContext } from "./context"
import type { MenuProps } from "antd";
import { items } from "~/layouts/scripts/menus";
import { useLocation } from "react-router";
type MenuItem = Required<MenuProps>["items"][number];

export const MenuProvider : React.FC<{children: React.ReactNode}> = ({children}) => {
    const [selectedMenus, setSelectedMenus] = useState<MenuItem[]>([])
    const location = useLocation();
    useEffect(() => {
        const path = location.pathname;
        console.log(location.pathname)
		const item = findMenuItemByKey(items, path);
		if (item) {
			setSelectedMenus([item]);
		}
    }, [location])
    return (
        <MenuContext.Provider value={{selectedMenus, setSelectedMenus}}>
            {children}
        </MenuContext.Provider>
    )
}