// 命名导入
import type { Route } from "./+types/login";
import LoginPage from "../pages/login/index"

export function meta({}: Route.MetaArgs) {
    return [
      { title: "登录" },
      { name: "description", content: "Welcome to React Router!" },
    ];
  }

export default function Login() {
    return (
        <>
            <LoginPage />
        </>
    );
}