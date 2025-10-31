import { Tabs } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router";

export default function CacheLayout() {
    const navigate = useNavigate();
    const location = useLocation();

    const activeKey = location.pathname.split("/").pop() || "backend";

    return (
        <div style={{ padding: 16 }}>
            <Tabs
                activeKey={activeKey}
                onChange={(key) => navigate(`/cache/${key}`)}
                items={[
                    { key: "backend", label: "后端缓存" },
                    { key: "redis", label: "Redis缓存" },
                ]}
            />
            <div style={{ marginTop: 16 }}>
                <Outlet />
            </div>
        </div>
    );
}
