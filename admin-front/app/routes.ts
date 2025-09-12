import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
    // 对应文件不能用具名导出
    route("/", "routes/dashboard.tsx", [
        index("routes/home.tsx"),
        route("dashboard", "welcome/welcome.tsx"),
        route("user", "welcome/welcome.tsx", {id: "?"})
    ]),
] satisfies RouteConfig;
