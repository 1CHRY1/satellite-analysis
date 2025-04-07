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
        // next('/login')
        // 首先，后端获取数据本来就需要token，没有授权就会通过请求重发的方式获得token，不然就会跳转到login页面，不需要这里跳转
        // 其次，刷新token的时候，似乎会因为这里的设置直接跳转到login页面，哪怕后面刷新成功也无法跳转到原来的页面，所以这里不拦截了
        next()
    } else if (to.meta.requiresGuest && isAuthenticated) {
        // ⚠️ 已登录用户，不能访问 login/register，跳转到主页
        // next('/home')
        next()
    } else {
        next()
    }
})

export default router
