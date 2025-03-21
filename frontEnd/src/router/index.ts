import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            redirect: '/home',
        },
        {
            path: '/home',
            component: () => import('@/views/HomeView.vue'),
        },
        {
            path: '/data',
            component: () => import('@/views/DataView.vue'),
        },
        {
            path: '/analysis',
            component: () => import('@/views/Analysis.vue'),
        },
        {
            path: '/login',
            component: () => import('@/views/LoginView.vue'),
        },
    ],
})

export default router
