import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
})

router.beforeEach((to, from, next) => {
    const isAuthenticated = !!localStorage.getItem('token') // 判断用户是否登录

    if (to.meta.requiresAuth && !isAuthenticated) {
        //  未登录，跳转到登录页
        next('/login')
    } else if (to.meta.requiresGuest && isAuthenticated) {
        // ⚠️ 已登录用户，不能访问 login/register，跳转到主页
        next('/home')
        // next()
    } else {
        next()
    }
})

export default router
