import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
    // 对应文件不能用具名导出
    route("/login", "routes/login.tsx"),
    route("/", "routes/dashboard.tsx", [
        index("routes/home.tsx"),
        route("dashboard", "pages/dashboard/index.tsx"),
        route("user", "welcome/welcome.tsx", {id: "?"})
    ]),
] satisfies RouteConfig;
