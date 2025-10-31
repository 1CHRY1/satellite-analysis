import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
    // 对应文件不能用具名导出
    route("/login", "routes/login.tsx"),
    route("/", "routes/dashboard.tsx", [
        index("routes/home.tsx"),
        route("dashboard", "pages/dashboard/index.tsx"),
        route("user", "pages/user/index.tsx"),
        route("role", "pages/role/index.tsx"),
        route("sensor", "pages/sensor/index.tsx"),
        route("product", "pages/product/index.tsx"),
        route("satellite", "pages/scene/index.tsx"),
        route("vector", "pages/vector/index.tsx"),
        route("theme", "pages/theme/index.tsx"),
        // ✅ 缓存管理模块
        route("cache", "layouts/cache-layout.tsx", [
            index("pages/cache/index.tsx"), // 默认进入后端缓存
            route("backend", "pages/cache/backend.tsx"),
            route("redis", "pages/cache/redis.tsx"),
        ]),
    ]),
] satisfies RouteConfig;
