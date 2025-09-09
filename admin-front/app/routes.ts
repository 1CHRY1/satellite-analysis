import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
    index("routes/dashboard.tsx", {id : 'dashboard'}),
    route("dashboard", "routes/dashboard.tsx")
] satisfies RouteConfig;
