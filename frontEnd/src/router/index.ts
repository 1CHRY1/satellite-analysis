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
            // component: () => import('@/views/HomeView.vue'),
            // @ts-ignore
            component: () => import('@/views/home.vue'),
        },
        {
            path: '/data',
            component: () => import('@/views/DataView.vue'),
        },
        {
            path: '/projects',
            component: () => import('@/views/projects.vue'),
        },
        {
            path: '/analysis',
            component: () => import('@/views/Analysis.vue'),
        },
        {
            path: '/login',
            component: () => import('@/views/LoginView.vue'),
        },
        {
            path: '/test',
            component: () => import('@/views/Test.vue'),
        },
    ],
})

export default router
