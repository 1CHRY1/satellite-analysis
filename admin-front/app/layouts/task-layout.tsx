import { Tabs } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router";

export default function CacheLayout() {
    const navigate = useNavigate();
    const location = useLocation();

    const activeKey = location.pathname.split("/").pop() || "cloudfree";

    return (
        <div style={{ padding: 16 }}>
            <Tabs
                activeKey={activeKey}
                onChange={(key) => navigate(`/task/${key}`)}
                items={[
                    { key: "cloudfree", label: "数据准备任务" },
                    { key: "methlib", label: "展示分析任务" },
                ]}
            />
            <div style={{ marginTop: 16 }}>
                <Outlet />
            </div>
        </div>
    );
}
