import { Tabs } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router";

export default function SQLLayout() {
    const navigate = useNavigate();
    const location = useLocation();

    const activeKey = location.pathname.split("/").pop() || "datasource";

    return (
        <div style={{ padding: 16 }}>
            <Tabs
                activeKey={activeKey}
                onChange={(key) => navigate(`/sql/${key}`)}
                items={[
                    { key: "datasource", label: "数据源" },
                    { key: "monitor", label: "SQL监控" },
                    { key: "wall", label: "SQL防火墙" },
                    { key: "webapp", label: "Web应用" },
                    { key: "weburi", label: "URI监控" },
                    { key: "websession", label: "Session监控" },
                ]}
            />
            <div style={{ marginTop: 16 }}>
                <Outlet />
            </div>
        </div>
    );
}
