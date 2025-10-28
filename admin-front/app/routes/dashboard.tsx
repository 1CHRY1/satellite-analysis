// 命名导入
import type { Route } from "./+types/dashboard";
import { VerticalDashboardLayout } from "../layouts/vertical-dashboard"

export function meta({}: Route.MetaArgs) {
    return [
      { title: "ARD后台管理系统" },
      { name: "description", content: "Welcome to React Router!" },
    ];
  }

export default function Dashboard() {
    return (
        <>
            <VerticalDashboardLayout />
        </>
    );
}